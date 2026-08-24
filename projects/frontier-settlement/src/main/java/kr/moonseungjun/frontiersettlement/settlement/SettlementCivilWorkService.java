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
 * Alpha.49-51 bounded selected-area civil works.
 *
 * Alpha.49 established project-local cut-to-fill earth relocation. Alpha.50 added physical imported
 * dirt/coarse-dirt hauling. Alpha.51 expands the bounded envelope and adds retaining-heavy terraces
 * whose exact cobblestone is physically hauled by the same shared construction worker.
 */
public final class SettlementCivilWorkService {
    public static final int MAX_WIDTH = 17;
    public static final int MAX_DEPTH = 17;
    public static final int MAX_AREA = MAX_WIDTH * MAX_DEPTH;
    public static final int MAX_CUT_DEPTH = 7;
    public static final int MAX_FILL_DEPTH = 7;
    private static final int MAX_PLAYER_DISTANCE = 44;
    private static final int MAX_SETTLEMENT_RADIUS = 112;
    private static final int WORK_INTERVAL_TICKS = 8;
    private static final int BLOCK_UPDATE = 2;
    private static final double WORK_REACHED_SQR = 4.0D;

    private SettlementCivilWorkService() {}

    public record Check(boolean valid, int minX, int maxX, int minZ, int maxZ, int gradeY,
                        int cutBlocks, int fillBlocks, int retainingBlocks, String message) {
        public int width() { return maxX - minX + 1; }
        public int depth() { return maxZ - minZ + 1; }
        public int importedFillBlocks() {
            return SettlementCivilFillSupplyService.importedFillRequired(cutBlocks, fillBlocks);
        }
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
            return invalid("토목 1회 범위는 최대 17×17입니다.");
        }

        if (horizontalDistanceSqr(player.blockPosition(), first) > (long) MAX_PLAYER_DISTANCE * MAX_PLAYER_DISTANCE
                || horizontalDistanceSqr(player.blockPosition(), second) > (long) MAX_PLAYER_DISTANCE * MAX_PLAYER_DISTANCE) {
            return invalid("토목 영역은 플레이어 44블록 안에서 지정해 주세요.");
        }
        BlockPos center = new BlockPos((minX + maxX) / 2, first.getY(), (minZ + maxZ) / 2);
        if (horizontalDistanceSqr(settlement.centerPos(), center) > (long) MAX_SETTLEMENT_RADIUS * MAX_SETTLEMENT_RADIUS) {
            return invalid("본진 토목 영역은 마을 중심 112블록 안에서 지정해 주세요.");
        }
        if (overlapsInfrastructure(settlement, minX - 1, maxX + 1, minZ - 1, maxZ + 1)) {
            return invalid("선택영역 또는 옹벽 보호 1칸 범위가 기존 건물·도로·전초기지·공동 창고와 겹칩니다.");
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
                    return invalid("각 지점의 절토·성토 높이 차는 최대 7블록입니다.");
                }
                String safety = validateColumn(level, x, z, surfaceY, gradeY);
                if (safety != null) return invalid(safety);
                if (delta > 0) cut += delta;
                else if (delta < 0) fill += -delta;
            }
        }
        if (cut == 0 && fill == 0) return invalid("이미 선택 높이로 평탄한 영역입니다.");

        SettlementCivilRetainingService.Plan retainingPlan = SettlementCivilRetainingService.checkPlan(
                level, minX, maxX, minZ, maxZ, gradeY);
        if (!retainingPlan.valid()) return invalid(retainingPlan.message());
        int retaining = retainingPlan.requiredBlocks();

        int importedFill = SettlementCivilFillSupplyService.importedFillRequired(cut, fill);
        if (importedFill > 0) {
            int available = SettlementCivilFillSupplyService.availableFill(level, settlement);
            if (available < 0) return invalid("공동 창고가 모두 로드된 상태에서 외부 성토 자재를 검사해 주세요.");
            if (available < importedFill) {
                return invalid("외부 성토 흙 부족 · 필요 " + importedFill + " / 공동 창고 " + available
                        + " · 흙/거친 흙 ItemStack을 실제로 넣어 주세요.");
            }
        }
        if (retaining > 0) {
            int availableRetaining = SettlementCivilRetainingService.availableRetaining(level, settlement);
            if (availableRetaining < 0) return invalid("공동 창고가 모두 로드된 상태에서 옹벽 조약돌을 검사해 주세요.");
            if (availableRetaining < retaining) {
                return invalid("옹벽 조약돌 부족 · 필요 " + retaining + " / 공동 창고 " + availableRetaining
                        + " · COBBLESTONE ItemStack을 실제로 넣어 주세요.");
            }
        }
        return new Check(true, minX, maxX, minZ, maxZ, gradeY, cut, fill, retaining,
                "토목 가능 · " + width + "×" + depth + " · 절토 " + cut + " · 성토 " + fill
                        + (importedFill > 0 ? " · 실제 창고 흙 " + importedFill : " · 현장 토사만 사용")
                        + (retaining > 0 ? " · 옹벽 조약돌 " + retaining : " · 옹벽 불필요")
                        + " · 가상 토사 생성 0");
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
                CivilWorkState.PHASE_CUT, 0, 0, check.cutBlocks(), check.fillBlocks(), check.retainingBlocks()));
        Villager builder = SettlementConstructionService.ensureBuilder(server.overworld(), settlement.centerPos());
        if (builder != null) {
            builder.setInvulnerable(true);
            builder.setCustomName(Component.literal("건설 주민 · 토목"));
        }
        SettlementService.broadcast(server, settlement);
        return new StartResult(true, "선택영역 토목 착공 · 절토 " + check.cutBlocks() + " / 성토 " + check.fillBlocks()
                + (check.retainingBlocks() > 0 ? " / 옹벽 조약돌 " + check.retainingBlocks() : "")
                + (check.importedFillBlocks() > 0 ? " / 창고 흙 운반 " + check.importedFillBlocks() : "")
                + " · 건설 주민이 현장 토사와 실제 창고 자재를 사용합니다.");
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

        if (project.phase() == CivilWorkState.PHASE_RETURN) {
            if (!SettlementCivilFillSupplyService.returnCarriedToStorage(level, settlement, builder)) return false;
            finish(server, settlement, data, builder);
            return true;
        }

        if (!areaLoaded(level, project)) return false;
        if (project.phase() == CivilWorkState.PHASE_CUT) {
            if (!builder.getMainHandItem().isEmpty()) {
                SettlementCivilFillSupplyService.returnCarriedToStorage(level, settlement, builder);
                return false;
            }
            BlockPos target = findCutTarget(level, project);
            if (target == null) {
                data.replace(project.initialRetainingBlocks() > 0 ? project.beginRetaining() : project.beginFill());
                return false;
            }
            if (!safeNaturalTarget(level, target)) return false;
            if (!moveBuilder(level, builder, target)) return false;
            if (server.getTickCount() % WORK_INTERVAL_TICKS != 0) return false;
            if (!level.setBlock(target, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE)) return false;
            builder.swing(InteractionHand.MAIN_HAND);
            data.replace(project.afterCut());
            return false;
        }

        if (project.phase() == CivilWorkState.PHASE_RETAIN) {
            if (!SettlementCivilRetainingService.isRetainingStack(builder.getMainHandItem())
                    && !builder.getMainHandItem().isEmpty()) {
                SettlementCivilFillSupplyService.returnCarriedToStorage(level, settlement, builder);
                return false;
            }
            SettlementCivilRetainingService.Plan retainingPlan = SettlementCivilRetainingService.plan(level, project);
            if (!retainingPlan.valid()) return false;
            BlockPos retainingTarget = retainingPlan.nextMissing(level);
            if (retainingTarget == null) {
                if (!builder.getMainHandItem().isEmpty()) {
                    SettlementCivilFillSupplyService.returnCarriedToStorage(level, settlement, builder);
                    return false;
                }
                data.replace(project.beginFill());
                return false;
            }
            if (!SettlementCivilRetainingService.ensureCarriedRetaining(level, settlement, builder, project)) return false;
            BlockState retainingCurrent = level.getBlockState(retainingTarget);
            if (level.getBlockEntity(retainingTarget) != null || !retainingCurrent.getFluidState().isEmpty()
                    || (!retainingCurrent.isAir() && !retainingCurrent.canBeReplaced())) return false;
            if (!moveBuilder(level, builder, retainingTarget)) return false;
            if (server.getTickCount() % WORK_INTERVAL_TICKS != 0) return false;
            if (!level.setBlock(retainingTarget, Blocks.COBBLESTONE.defaultBlockState(), BLOCK_UPDATE)) return false;
            SettlementCivilRetainingService.consumeOne(builder);
            builder.swing(InteractionHand.MAIN_HAND);
            data.replace(project.afterRetaining());
            return false;
        }

        BlockPos target = findFillTarget(level, project);
        if (target == null) {
            if (!builder.getMainHandItem().isEmpty()) {
                data.replace(project.beginReturn());
                return false;
            }
            finish(server, settlement, data, builder);
            return true;
        }

        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ()) - 1;
        if (validateColumn(level, target.getX(), target.getZ(), surfaceY, project.gradeY()) != null) return false;

        boolean importedFill = project.earthBank() <= 0;
        if (!importedFill && !builder.getMainHandItem().isEmpty()) {
            SettlementCivilFillSupplyService.returnCarriedToStorage(level, settlement, builder);
            return false;
        }
        if (importedFill && !SettlementCivilFillSupplyService.isFillStack(builder.getMainHandItem())
                && !builder.getMainHandItem().isEmpty()) {
            SettlementCivilFillSupplyService.returnCarriedToStorage(level, settlement, builder);
            return false;
        }
        if (importedFill && !SettlementCivilFillSupplyService.ensureCarriedFill(level, settlement, builder, project)) return false;

        BlockState current = level.getBlockState(target);
        if (level.getBlockEntity(target) != null || !current.getFluidState().isEmpty()
                || (!current.isAir() && !current.canBeReplaced())) return false;
        if (!moveBuilder(level, builder, target)) return false;
        if (server.getTickCount() % WORK_INTERVAL_TICKS != 0) return false;

        BlockState fillState = importedFill
                ? SettlementCivilFillSupplyService.carriedFillState(builder)
                : Blocks.COARSE_DIRT.defaultBlockState();
        if (!level.setBlock(target, fillState, BLOCK_UPDATE)) return false;
        if (importedFill) SettlementCivilFillSupplyService.consumeOne(builder);
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
        if (state.phase() == CivilWorkState.PHASE_CUT) return "선택영역 절토";
        if (state.phase() == CivilWorkState.PHASE_RETAIN) return "선택영역 테라스 옹벽 시공";
        if (state.phase() == CivilWorkState.PHASE_RETURN) return "선택영역 토목 · 잔여 자재 복귀";
        return state.earthBank() > 0 ? "선택영역 성토" : "선택영역 성토 · 창고 흙 운반";
    }

    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (level != server.overworld()) return;
        CivilWorkState state = SettlementCivilWorkData.get(server).project();
        if (!state.active()) return;
        BlockPos pos = event.getPos();
        if (pos.getX() < state.minX() - 1 || pos.getX() > state.maxX() + 1
                || pos.getZ() < state.minZ() - 1 || pos.getZ() > state.maxZ() + 1) return;
        if (pos.getY() < state.gradeY() - SettlementCivilRetainingService.MAX_RETAINING_HEIGHT
                || pos.getY() > state.gradeY() + MAX_CUT_DEPTH) return;
        event.setCanceled(true);
        event.setNotifyClient(true);
    }

    private static void finish(MinecraftServer server, SettlementData settlement,
                               SettlementCivilWorkData data, Villager builder) {
        if (!builder.getMainHandItem().isEmpty()) return;
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
        for (int x = state.minX() - 1; x <= state.maxX() + 1; x += 8) {
            for (int z = state.minZ() - 1; z <= state.maxZ() + 1; z += 8) {
                if (!level.hasChunkAt(new BlockPos(x, state.gradeY(), z))) return false;
            }
        }
        return level.hasChunkAt(new BlockPos(state.maxX() + 1, state.gradeY(), state.maxZ() + 1));
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

    static boolean isNaturalGround(BlockState state) {
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
        return new Check(false, 0, 0, 0, 0, 0, 0, 0, 0, message);
    }
}
