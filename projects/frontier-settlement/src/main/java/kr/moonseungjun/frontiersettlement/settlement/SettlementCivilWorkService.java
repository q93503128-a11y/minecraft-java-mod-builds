package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Alpha.49 bounded selected-area civil works.
 *
 * The first selected corner fixes the target surface Y. Up to 9x9 loaded natural terrain may be
 * flattened when every column needs at most four blocks of cut/fill and the initial cut volume is
 * at least the fill volume. Cut terrain never drops items; each actually removed natural block adds
 * one project-local earth unit, and each coarse-dirt fill consumes one. That earth bank can never be
 * spent outside this one project, so the feature is terrain relocation rather than a second economy.
 */
public final class SettlementCivilWorkService {
    public static final int MAX_WIDTH = 9;
    public static final int MAX_DEPTH = 9;
    public static final int MAX_AREA = MAX_WIDTH * MAX_DEPTH;
    public static final int MAX_CUT_DEPTH = 4;
    public static final int MAX_FILL_DEPTH = 4;
    private static final int MAX_PLAYER_DISTANCE = 28;
    private static final int MAX_SETTLEMENT_RADIUS = 80;
    private static final int WORK_INTERVAL_TICKS = 8;
    private static final int BLOCK_UPDATE = 2;
    private static final double WORK_REACHED_SQR = 4.0D;

    private SettlementCivilWorkService() {}

    public record Check(boolean valid, int minX, int maxX, int minZ, int maxZ, int gradeY,
                        int cutBlocks, int fillBlocks, String message) {
        public int width() { return maxX - minX + 1; }
        public int depth() { return maxZ - minZ + 1; }
    }
    public record StartResult(boolean started, String message) {}

    public static String lockedReason(SettlementData data) {
        if (SettlementTier.current(data).ordinal() < SettlementTier.DOMAIN.ordinal()) {
            return "선택영역 토목은 영지 단계에서 열립니다.";
        }
        if (data.buildingCount(BuildingType.CONSTRUCTION_OFFICE) < 1) {
            return "선택영역 토목은 건설소 1곳이 필요합니다.";
        }
        return null;
    }

    public static Check check(ServerPlayer player, BlockPos first, BlockPos second) {
        MinecraftServer server = player.level().getServer();
        SettlementData settlement = SettlementData.get(server);
        if (!settlement.founded()) return invalid("공동 마을이 없습니다.");
        if (player.level() != server.overworld()) return invalid("선택영역 토목은 오버월드에서만 가능합니다.");
        String locked = lockedReason(settlement);
        if (locked != null) return invalid(locked);
        if (first == null || second == null) return invalid("두 모서리를 선택해 주세요.");

        int minX = Math.min(first.getX(), second.getX());
        int maxX = Math.max(first.getX(), second.getX());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxZ = Math.max(first.getZ(), second.getZ());
        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        if (width > MAX_WIDTH || depth > MAX_DEPTH || width * depth > MAX_AREA) {
            return invalid("토목 1회 범위는 최대 9×9입니다.");
        }

        if (horizontalDistanceSqr(player.blockPosition(), first) > (long) MAX_PLAYER_DISTANCE * MAX_PLAYER_DISTANCE
                || horizontalDistanceSqr(player.blockPosition(), second) > (long) MAX_PLAYER_DISTANCE * MAX_PLAYER_DISTANCE) {
            return invalid("토목 영역은 플레이어 28블록 안에서 지정해 주세요.");
        }
        BlockPos center = new BlockPos((minX + maxX) / 2, first.getY(), (minZ + maxZ) / 2);
        if (horizontalDistanceSqr(settlement.centerPos(), center) > (long) MAX_SETTLEMENT_RADIUS * MAX_SETTLEMENT_RADIUS) {
            return invalid("본진 토목 영역은 마을 중심 80블록 안에서 지정해 주세요.");
        }
        if (overlapsInfrastructure(settlement, minX, maxX, minZ, maxZ)) {
            return invalid("기존 건물·도로·전초기지·공동 창고와 겹치는 영역은 토목할 수 없습니다.");
        }

        ServerLevel level = server.overworld();
        int gradeY = first.getY();
        int cut = 0;
        int fill = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos column = new BlockPos(x, gradeY, z);
                if (!level.hasChunkAt(column)) return invalid("영역 전체가 로드된 상태에서 토목을 시작해 주세요.");
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                int delta = surfaceY - gradeY;
                if (delta > MAX_CUT_DEPTH || delta < -MAX_FILL_DEPTH) {
                    return invalid("각 지점의 절토·성토 높이 차는 최대 4블록입니다.");
                }
                String safety = validateColumn(level, x, z, surfaceY, gradeY);
                if (safety != null) return invalid(safety);
                if (delta > 0) cut += delta;
                else if (delta < 0) fill += -delta;
            }
        }
        if (cut == 0 && fill == 0) return invalid("이미 선택 높이로 평탄한 영역입니다.");
        if (fill > cut) {
            return invalid("성토량 " + fill + " > 절토량 " + cut + " · 토목 1차는 현장 절토량 안에서만 성토합니다.");
        }
        return new Check(true, minX, maxX, minZ, maxZ, gradeY, cut, fill,
                "토목 가능 · " + width + "×" + depth + " · 절토 " + cut + " · 성토 " + fill
                        + " · 외부 자원/가상 토사 생성 0");
    }

    public static StartResult start(ServerPlayer player, BlockPos first, BlockPos second) {
        MinecraftServer server = player.level().getServer();
        SettlementData settlement = SettlementData.get(server);
        SettlementCivilWorkData data = SettlementCivilWorkData.get(server);
        if (settlement.construction().active() || settlement.roadConstruction().active() || settlement.outpostConstruction().active()) {
            return new StartResult(false, "현재 건물·도로·전초 공사가 끝난 뒤 토목을 시작해 주세요.");
        }
        if (data.project().active()) return new StartResult(false, "이미 선택영역 토목이 진행 중입니다.");
        Check check = check(player, first, second);
        if (!check.valid()) return new StartResult(false, check.message());

        data.begin(new CivilWorkState(true, check.minX(), check.maxX(), check.minZ(), check.maxZ(), check.gradeY(),
                CivilWorkState.PHASE_CUT, 0, 0, check.cutBlocks(), check.fillBlocks()));
        Villager builder = SettlementConstructionService.ensureBuilder(server.overworld(), settlement.centerPos());
        if (builder != null) {
            builder.setInvulnerable(true);
            builder.setCustomName(Component.literal("건설 주민 · 토목"));
        }
        SettlementService.broadcast(server, settlement);
        return new StartResult(true, "선택영역 토목 착공 · 절토 " + check.cutBlocks() + " / 성토 " + check.fillBlocks()
                + " · 건설 주민이 현장 자연지형을 직접 재배치합니다.");
    }

    public static boolean tick(MinecraftServer server, SettlementData settlement) {
        SettlementCivilWorkData data = SettlementCivilWorkData.get(server);
        CivilWorkState project = data.project();
        if (!project.active()) return false;
        ServerLevel level = server.overworld();
        Villager builder = SettlementConstructionService.ensureBuilder(level, settlement.centerPos());
        if (builder == null) return false;
        if (builder.isNoAi()) builder.setNoAi(false);
        builder.setInvulnerable(true);
        builder.setCustomName(Component.literal("건설 주민 · 토목"));

        if (!areaLoaded(level, project)) return false;
        if (project.phase() == CivilWorkState.PHASE_CUT) {
            BlockPos target = findCutTarget(level, project);
            if (target == null) {
                data.replace(project.beginFill());
                return false;
            }
            if (!safeNaturalTarget(level, target)) return false;
            if (!moveBuilder(level, builder, target)) return false;
            if (server.getTickCount() % WORK_INTERVAL_TICKS != 0) return false;
            level.setBlock(target, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE);
            builder.swing(InteractionHand.MAIN_HAND);
            data.replace(project.afterCut());
            return false;
        }

        BlockPos target = findFillTarget(level, project);
        if (target == null) {
            finish(server, settlement, data, builder);
            return true;
        }
        if (project.earthBank() <= 0) return false;
        BlockState current = level.getBlockState(target);
        if (level.getBlockEntity(target) != null || !current.getFluidState().isEmpty()
                || (!current.isAir() && !current.canBeReplaced())) return false;
        if (!moveBuilder(level, builder, target)) return false;
        if (server.getTickCount() % WORK_INTERVAL_TICKS != 0) return false;
        level.setBlock(target, Blocks.COARSE_DIRT.defaultBlockState(), BLOCK_UPDATE);
        builder.swing(InteractionHand.MAIN_HAND);
        data.replace(project.afterFill());
        return false;
    }

    public static int progressPercent(MinecraftServer server) {
        return SettlementCivilWorkData.get(server).project().progressPercent();
    }

    public static String phaseLabel(MinecraftServer server) {
        CivilWorkState state = SettlementCivilWorkData.get(server).project();
        if (!state.active()) return "";
        return state.phase() == CivilWorkState.PHASE_CUT ? "선택영역 절토" : "선택영역 성토";
    }

    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (level != server.overworld()) return;
        CivilWorkState state = SettlementCivilWorkData.get(server).project();
        if (!state.active()) return;
        BlockPos pos = event.getPos();
        if (pos.getX() < state.minX() || pos.getX() > state.maxX()
                || pos.getZ() < state.minZ() || pos.getZ() > state.maxZ()) return;
        if (pos.getY() < state.gradeY() - MAX_FILL_DEPTH || pos.getY() > state.gradeY() + MAX_CUT_DEPTH) return;
        event.setCanceled(true);
        event.setNotifyClient(true);
    }

    private static void finish(MinecraftServer server, SettlementData settlement,
                               SettlementCivilWorkData data, Villager builder) {
        data.clear();
        builder.getNavigation().stop();
        builder.setInvulnerable(false);
        builder.setCustomName(Component.literal("건설 주민"));
        SettlementService.broadcast(server, settlement);
    }

    private static BlockPos findCutTarget(ServerLevel level, CivilWorkState state) {
        for (int x = state.minX(); x <= state.maxX(); x++) {
            for (int z = state.minZ(); z <= state.maxZ(); z++) {
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (surfaceY > state.gradeY()) return new BlockPos(x, surfaceY, z);
            }
        }
        return null;
    }

    private static BlockPos findFillTarget(ServerLevel level, CivilWorkState state) {
        for (int x = state.minX(); x <= state.maxX(); x++) {
            for (int z = state.minZ(); z <= state.maxZ(); z++) {
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (surfaceY < state.gradeY()) return new BlockPos(x, surfaceY + 1, z);
            }
        }
        return null;
    }

    private static boolean moveBuilder(ServerLevel level, Villager builder, BlockPos target) {
        int workY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
        BlockPos work = new BlockPos(target.getX(), workY, target.getZ());
        if (builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D) <= WORK_REACHED_SQR) return true;
        builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 0.82D);
        return false;
    }

    private static boolean areaLoaded(ServerLevel level, CivilWorkState state) {
        for (int x = state.minX(); x <= state.maxX(); x += 8) {
            for (int z = state.minZ(); z <= state.maxZ(); z += 8) {
                if (!level.hasChunkAt(new BlockPos(x, state.gradeY(), z))) return false;
            }
        }
        return level.hasChunkAt(new BlockPos(state.maxX(), state.gradeY(), state.maxZ()));
    }

    private static String validateColumn(ServerLevel level, int x, int z, int surfaceY, int gradeY) {
        BlockPos support = new BlockPos(x, surfaceY, z);
        BlockState supportState = level.getBlockState(support);
        if (level.getBlockEntity(support) != null || !supportState.getFluidState().isEmpty() || !isNaturalGround(supportState)) {
            return "플레이어 건축물·컨테이너·유체·비자연 지형이 포함된 영역은 토목하지 않습니다.";
        }
        int low = Math.min(surfaceY, gradeY);
        int high = Math.max(surfaceY, gradeY);
        for (int y = low; y <= high; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) != null || !state.getFluidState().isEmpty()) {
                return "컨테이너나 유체가 있는 열은 토목할 수 없습니다.";
            }
            if (y <= surfaceY) {
                if (!isNaturalGround(state)) return "광석·플레이어 블록·구조물이 포함된 열은 토목할 수 없습니다.";
            } else if (!state.isAir() && !state.canBeReplaced()) {
                return "성토 공간에 기존 구조물이 있습니다.";
            }
        }
        return null;
    }

    private static boolean safeNaturalTarget(ServerLevel level, BlockPos target) {
        BlockState state = level.getBlockState(target);
        return level.getBlockEntity(target) == null && state.getFluidState().isEmpty() && isNaturalGround(state);
    }

    private static boolean overlapsInfrastructure(SettlementData data, int minX, int maxX, int minZ, int maxZ) {
        BlockPos stock = data.stockpilePos();
        if (insideXZ(stock, minX, maxX, minZ, maxZ)) return true;
        for (BuildingRecord building : data.buildings()) {
            int bx0 = building.originX() - 1;
            int bx1 = building.originX() + building.rotatedWidth();
            int bz0 = building.originZ() - 1;
            int bz1 = building.originZ() + building.rotatedDepth();
            if (minX <= bx1 && maxX >= bx0 && minZ <= bz1 && maxZ >= bz0) return true;
        }
        for (RoadSegment road : data.roads()) {
            for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
                if (road.containsXZ(new BlockPos(x, 0, z))) return true;
            }
        }
        for (OutpostRecord outpost : data.outposts()) {
            if (outpost.centerX() + 6 >= minX && outpost.centerX() - 6 <= maxX
                    && outpost.centerZ() + 6 >= minZ && outpost.centerZ() - 6 <= maxZ) return true;
        }
        return false;
    }

    private static boolean insideXZ(BlockPos pos, int minX, int maxX, int minZ, int maxZ) {
        return pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    private static long horizontalDistanceSqr(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private static boolean isNaturalGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.STONE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(BlockTags.DIRT);
    }

    private static Check invalid(String message) {
        return new Check(false, 0, 0, 0, 0, 0, 0, 0, message);
    }
}
