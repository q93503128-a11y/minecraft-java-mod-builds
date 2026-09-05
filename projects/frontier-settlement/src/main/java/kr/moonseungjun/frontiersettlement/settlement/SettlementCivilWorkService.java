package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/** Bounded selected-area civil works using one project state and the shared construction worker. */
public final class SettlementCivilWorkService {
    public static final int MAX_WIDTH = 17;
    public static final int MAX_DEPTH = 17;
    public static final int MAX_AREA = MAX_WIDTH * MAX_DEPTH;
    public static final int MAX_CUT_DEPTH = 32;
    public static final int MAX_FILL_DEPTH = 16;
    private static final int MAX_PLAYER_DISTANCE = 44;
    private static final int MAX_SETTLEMENT_RADIUS = 112;
    // SettlementService already schedules civil work every five ticks. Matching that cadence
    // avoids the old 5-vs-8 LCM bug that reduced real work to one block every 40 ticks.
    private static final int WORK_INTERVAL_TICKS = 5;
    private static final int BLOCK_UPDATE = 2;
    private static final double WORK_REACHED_SQR = 4.0D;
    private static final int MAX_CIVIL_APPROACH_PATH_TRIES = 64;

    private SettlementCivilWorkService() {}

    public record Check(boolean valid, int minX, int maxX, int minZ, int maxZ, int gradeY,
                        int cutBlocks, int reusableCutBlocks, int fillBlocks, int retainingBlocks, String message) {
        public int width() { return maxX - minX + 1; }
        public int depth() { return maxZ - minZ + 1; }
        public int importedFillBlocks() {
            return SettlementCivilFillSupplyService.importedFillRequired(reusableCutBlocks, fillBlocks);
        }
    }
    private record ColumnPlan(boolean valid, int cutBlocks, int reusableCutBlocks, int fillBlocks, String message) {
        static ColumnPlan invalid(String message) { return new ColumnPlan(false, 0, 0, 0, message); }
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
        if (player.level() != server.overworld()) return invalid("선택영역 평탄화는 오버월드에서만 가능합니다.");
        String locked = lockedReason(settlement);
        if (locked != null) return invalid(locked);
        if (SettlementProjectAuthority.anyActive(server, settlement)) {
            return invalid("현재 공동 공사가 끝난 뒤 평탄화를 계획해 주세요.");
        }
        if (first == null || second == null) return invalid("두 모서리를 선택해 주세요.");

        int minX = Math.min(first.getX(), second.getX());
        int maxX = Math.max(first.getX(), second.getX());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxZ = Math.max(first.getZ(), second.getZ());
        long widthLong = (long) maxX - minX + 1L;
        long depthLong = (long) maxZ - minZ + 1L;
        if (widthLong <= 0L || depthLong <= 0L
                || widthLong > MAX_WIDTH || depthLong > MAX_DEPTH || widthLong * depthLong > MAX_AREA) {
            return invalid("평탄화 1회 범위는 최대 " + MAX_WIDTH + "×" + MAX_DEPTH + "입니다.");
        }
        int width = (int) widthLong;
        int depth = (int) depthLong;

        if (!withinHorizontalDistance(player.blockPosition(), first, MAX_PLAYER_DISTANCE)
                || !withinHorizontalDistance(player.blockPosition(), second, MAX_PLAYER_DISTANCE)) {
            return invalid("평탄화 영역은 플레이어 " + MAX_PLAYER_DISTANCE + "블록 안에서 지정해 주세요.");
        }
        int centerX = (int) ((long) minX + ((long) maxX - minX) / 2L);
        int centerZ = (int) ((long) minZ + ((long) maxZ - minZ) / 2L);
        int gradeY = first.getY();
        BlockPos center = new BlockPos(centerX, gradeY, centerZ);
        if (!withinHorizontalDistance(settlement.centerPos(), center, MAX_SETTLEMENT_RADIUS)) {
            return invalid("본진 평탄화 영역은 마을 중심 " + MAX_SETTLEMENT_RADIUS + "블록 안에서 지정해 주세요.");
        }
        if (overlapsInfrastructure(settlement, minX - 1, maxX + 1, minZ - 1, maxZ + 1)) {
            return invalid("선택영역이 기존 건물·도로·전초기지·공동 창고와 겹칩니다. 마을 시설은 불도저 평탄화에서 보호됩니다.");
        }

        ServerLevel level = server.overworld();
        int cut = 0;
        int reusableCut = 0;
        int fill = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!level.hasChunkAt(new BlockPos(x, gradeY, z))) {
                    return invalid("영역 전체가 로드된 상태에서 평탄화를 시작해 주세요.");
                }
                ColumnPlan plan = analyzeColumn(level, x, z, gradeY);
                if (!plan.valid()) return invalid(plan.message());
                cut += plan.cutBlocks();
                reusableCut += plan.reusableCutBlocks();
                fill += plan.fillBlocks();
            }
        }
        if (cut == 0 && fill == 0) return invalid("이미 선택 높이로 완전히 평탄한 영역입니다.");

        int importedFill = SettlementCivilFillSupplyService.importedFillRequired(reusableCut, fill);
        if (importedFill > 0) {
            int available = SettlementCivilFillSupplyService.availableFill(level, settlement);
            if (available < 0) return invalid("공동 창고가 모두 로드된 상태에서 외부 성토 자재를 검사해 주세요.");
            if (available < importedFill) {
                return invalid("외부 성토 흙 부족 · 필요 " + importedFill + " / 공동 창고 " + available
                        + " · 흙/거친 흙 ItemStack을 실제로 넣어 주세요.");
            }
        }
        return new Check(true, minX, maxX, minZ, maxZ, gradeY, cut, reusableCut, fill, 0,
                "완전 평탄화 가능 · " + width + "×" + depth + " · 철거 " + cut + " · 성토 " + fill
                        + (importedFill > 0 ? " · 실제 창고 흙 " + importedFill : " · 현장 토사로 충당")
                        + " · 나무/잎/일반 블록도 기준면 위에서 제거");
    }

    public static StartResult start(ServerPlayer player, BlockPos first, BlockPos second) {
        MinecraftServer server = player.level().getServer();
        SettlementData settlement = SettlementData.get(server);
        SettlementCivilWorkData data = SettlementCivilWorkData.get(server);
        if (SettlementProjectAuthority.anyActive(server, settlement)) {
            return new StartResult(false, "현재 공동 공사가 끝난 뒤 평탄화를 시작해 주세요.");
        }
        Check check = check(player, first, second);
        if (!check.valid()) return new StartResult(false, check.message());

        data.begin(new CivilWorkState(true, check.minX(), check.maxX(), check.minZ(), check.maxZ(), check.gradeY(),
                CivilWorkState.PHASE_CUT, 0, 0, check.cutBlocks(), check.fillBlocks(), 0));
        List<FrontierWorkerEntity> builders = SettlementConstructionService.ensureProjectBuilders(server.overworld(), settlement);
        if (builders.isEmpty()) {
            data.clear();
            SettlementService.broadcast(server, settlement);
            return new StartResult(false, "건설 작업자를 안전하게 확보할 수 없어 평탄화 착공을 취소했습니다. 주변 마을·공동 창고 청크를 로드한 뒤 다시 시도해 주세요. 자재는 차감되지 않았습니다.");
        }
        for (FrontierWorkerEntity builder : builders) builder.setCustomName(Component.literal("건설 주민 · 평탄화"));
        SettlementService.broadcast(server, settlement);
        return new StartResult(true, "완전 평탄화 착공 · 철거 " + check.cutBlocks() + " / 성토 " + check.fillBlocks()
                + (check.importedFillBlocks() > 0 ? " / 창고 흙 운반 " + check.importedFillBlocks() : "")
                + " · 건설 주민 " + builders.size() + "명이 중앙 공사 권위 아래 순차 분담합니다.");
    }

    public static boolean tick(MinecraftServer server, SettlementData settlement) {
        SettlementCivilWorkData data = SettlementCivilWorkData.get(server);
        if (!data.project().active()) return false;
        ServerLevel level = server.overworld();
        List<FrontierWorkerEntity> builders = SettlementConstructionService.ensureProjectBuilders(level, settlement);
        if (builders.isEmpty()) return false;

        for (int i = 0; i < builders.size(); i++) {
            if (!data.project().active()) return true;
            FrontierWorkerEntity builder = builders.get(i);
            if (builder.isNoAi()) builder.setNoAi(false);
            builder.setInvulnerable(false);
            builder.setCustomName(Component.literal("건설 주민 · 평탄화"));
            tickBuilder(server, settlement, data, builder, i == 0);
        }
        return !data.project().active();
    }

    private static boolean tickBuilder(MinecraftServer server, SettlementData settlement,
                                       SettlementCivilWorkData data, FrontierWorkerEntity builder,
                                       boolean coordinator) {
        ServerLevel level = server.overworld();
        CivilWorkState project = data.project();
        if (!project.active()) return true;
        if (project.initialRetainingBlocks() > 0) {
            data.replace(project.withoutRetaining());
            project = data.project();
        }

        if (project.phase() == CivilWorkState.PHASE_RETURN) {
            if (!coordinator) return false;
            if (!SettlementCivilFillSupplyService.returnCarriedToStorage(level, settlement, builder)) return false;
            return finish(server, settlement, data, builder);
        }
        if (!areaLoaded(level, project)) return false;

        if (project.phase() == CivilWorkState.PHASE_CUT) {
            if (!builder.getMainHandItem().isEmpty()) {
                SettlementCivilFillSupplyService.returnCarriedToStorage(level, settlement, builder);
                return false;
            }
            BlockPos target = findCutTarget(level, project);
            if (target == null) {
                data.replace(project.beginFill());
                return false;
            }
            if (!safeDemolitionTarget(level, target)) return false;
            if (!moveBuilderToCivilSite(level, builder, project)) return false;
            if (server.getTickCount() % WORK_INTERVAL_TICKS != 0) return false;
            BlockState removed = level.getBlockState(target);
            if (!level.setBlock(target, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE)) return false;
            builder.swing(InteractionHand.MAIN_HAND);
            data.replace(project.afterCut(isReusableCut(removed)));
            return false;
        }

        if (project.phase() == CivilWorkState.PHASE_RETAIN) {
            data.replace(project.withoutRetaining().beginFill());
            return false;
        }

        BlockPos target = findFillTarget(level, project);
        if (target == null) {
            if (!coordinator) return false;
            if (!builder.getMainHandItem().isEmpty()) {
                data.replace(project.beginReturn());
                return false;
            }
            return finish(server, settlement, data, builder);
        }

        boolean importedFill = project.earthBank() <= 0;
        if (importedFill && !coordinator) {
            moveBuilderToCivilSite(level, builder, project);
            return false;
        }
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
        if (!moveBuilderToCivilSite(level, builder, project)) return false;
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
        if (state.phase() == CivilWorkState.PHASE_CUT) return "선택영역 완전 평탄화 · 철거";
        if (state.phase() == CivilWorkState.PHASE_RETAIN) return "선택영역 테라스 옹벽 시공";
        if (state.phase() == CivilWorkState.PHASE_RETURN) return "선택영역 토목 · 잔여 자재 복귀";
        return state.earthBank() > 0 ? "선택영역 완전 평탄화 · 성토" : "선택영역 완전 평탄화 · 창고 흙 운반";
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

    private static boolean finish(MinecraftServer server, SettlementData settlement,
                                  SettlementCivilWorkData data, FrontierWorkerEntity builder) {
        if (!builder.getMainHandItem().isEmpty()) return false;
        if (!SettlementConstructionService.returnBuilderHome(server.overworld(), settlement, builder)) return false;
        data.clear();
        builder.getNavigation().stop();
        builder.setInvulnerable(false);
        builder.setCustomName(Component.literal("건설 주민"));
        SettlementService.broadcast(server, settlement);
        return true;
    }

    private static BlockPos findCutTarget(ServerLevel level, CivilWorkState state) {
        for (int x = state.minX(); x <= state.maxX(); x++) {
            for (int z = state.minZ(); z <= state.maxZ(); z++) {
                int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                for (int y = topY; y > state.gradeY(); y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).isAir()) return pos;
                }
            }
        }
        return null;
    }

    private static BlockPos findFillTarget(ServerLevel level, CivilWorkState state) {
        for (int x = state.minX(); x <= state.maxX(); x++) {
            for (int z = state.minZ(); z <= state.maxZ(); z++) {
                int supportY = findFillSupportY(level, x, z, state.gradeY());
                if (supportY < state.gradeY()) return new BlockPos(x, supportY + 1, z);
            }
        }
        return null;
    }

    static int remainingFillBlocks(ServerLevel level, CivilWorkState state) {
        if (state == null || !state.active()) return 0;
        int total = 0;
        for (int x = state.minX(); x <= state.maxX(); x++) {
            for (int z = state.minZ(); z <= state.maxZ(); z++) {
                if (!level.hasChunkAt(new BlockPos(x, state.gradeY(), z))) return -1;
                int supportY = findFillSupportY(level, x, z, state.gradeY());
                if (supportY < state.gradeY() - MAX_FILL_DEPTH) return -1;
                total += Math.max(0, state.gradeY() - supportY);
            }
        }
        return total;
    }

    private static int findFillSupportY(ServerLevel level, int x, int z, int gradeY) {
        for (int y = gradeY; y >= gradeY - MAX_FILL_DEPTH; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) != null || !state.getFluidState().isEmpty()) return gradeY - MAX_FILL_DEPTH - 1;
            if (!state.isAir() && !state.canBeReplaced()) return y;
        }
        return gradeY - MAX_FILL_DEPTH - 1;
    }

    private static boolean moveBuilderToCivilSite(ServerLevel level, FrontierWorkerEntity builder,
                                                  CivilWorkState project) {
        if (builderInsideCivilEnvelope(builder, project)) {
            builder.getNavigation().stop();
            return true;
        }
        List<BlockPos> approaches = civilApproachPositions(level, builder, project);
        int tried = 0;
        for (BlockPos approach : approaches) {
            if (++tried > MAX_CIVIL_APPROACH_PATH_TRIES) break;
            if (builder.distanceToSqr(approach.getX() + 0.5D, approach.getY(), approach.getZ() + 0.5D) <= WORK_REACHED_SQR) {
                builder.getNavigation().stop();
                return true;
            }
            Path path = builder.getNavigation().createPath(approach, 0);
            if (path == null || !path.canReach() || path.getEndNode() == null) continue;
            BlockPos end = path.getEndNode().asBlockPos();
            if (Math.abs(end.getX() - approach.getX()) > 1 || Math.abs(end.getY() - approach.getY()) > 1
                    || Math.abs(end.getZ() - approach.getZ()) > 1) continue;
            if (builder.getNavigation().moveTo(path, 0.90D)) return false;
        }
        builder.getNavigation().stop();
        return false;
    }

    private static boolean builderInsideCivilEnvelope(FrontierWorkerEntity builder, CivilWorkState project) {
        double margin = 1.5D;
        if (builder.getX() < project.minX() - margin || builder.getX() > project.maxX() + 1.0D + margin
                || builder.getZ() < project.minZ() - margin || builder.getZ() > project.maxZ() + 1.0D + margin) return false;
        return Math.abs(builder.getY() - project.gradeY()) <= MAX_CUT_DEPTH + 4.0D;
    }

    private static List<BlockPos> civilApproachPositions(ServerLevel level, FrontierWorkerEntity builder,
                                                          CivilWorkState project) {
        List<BlockPos> result = new ArrayList<>();
        for (int x = project.minX() - 1; x <= project.maxX() + 1; x++) {
            for (int z = project.minZ() - 1; z <= project.maxZ() + 1; z++) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos feet = new BlockPos(x, y, z);
                if (isCivilWalkable(level, feet)) result.add(feet);
            }
        }
        result.sort(Comparator.comparingDouble(pos -> builder.distanceToSqr(
                pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D)));
        return List.copyOf(result);
    }

    private static boolean isCivilWalkable(ServerLevel level, BlockPos feet) {
        BlockPos head = feet.above();
        BlockPos below = feet.below();
        if (!level.hasChunkAt(feet) || !level.hasChunkAt(head) || !level.hasChunkAt(below)) return false;
        if (level.getBlockEntity(feet) != null || level.getBlockEntity(head) != null) return false;
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(head);
        BlockState belowState = level.getBlockState(below);
        if (!feetState.getFluidState().isEmpty() || !headState.getFluidState().isEmpty()
                || !belowState.getFluidState().isEmpty()) return false;
        if ((!feetState.isAir() && !feetState.canBeReplaced())
                || (!headState.isAir() && !headState.canBeReplaced())) return false;
        return !belowState.isAir() && !belowState.canBeReplaced();
    }

    private static boolean moveBuilder(ServerLevel level, FrontierWorkerEntity builder, BlockPos target) {
        if (!level.hasChunkAt(target)) return false;
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

    private static ColumnPlan analyzeColumn(ServerLevel level, int x, int z, int gradeY) {
        int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        if ((long) topY - gradeY > MAX_CUT_DEPTH) {
            return ColumnPlan.invalid("기준면 위 철거 높이는 최대 " + MAX_CUT_DEPTH + "블록입니다.");
        }
        int cut = 0;
        int reusable = 0;
        for (int y = topY; y > gradeY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (level.getBlockEntity(pos) != null) {
                return ColumnPlan.invalid("상자·기계 등 블록 엔티티가 포함된 영역은 자동 삭제하지 않습니다. 해당 블록을 옮긴 뒤 다시 평탄화해 주세요.");
            }
            if (!state.getFluidState().isEmpty()) {
                return ColumnPlan.invalid("물·용암이 포함된 열은 반복 유입을 막기 위해 평탄화 전에 먼저 배수해 주세요.");
            }
            if (state.getDestroySpeed(level, pos) < 0.0F) {
                return ColumnPlan.invalid("파괴할 수 없는 블록이 포함된 영역은 평탄화할 수 없습니다.");
            }
            cut++;
            if (isReusableCut(state)) reusable++;
        }

        int fill = 0;
        boolean supportFound = false;
        for (int y = gradeY; y >= gradeY - MAX_FILL_DEPTH; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) != null) return ColumnPlan.invalid("성토 열에 블록 엔티티가 있어 자동 평탄화하지 않습니다.");
            if (!state.getFluidState().isEmpty()) return ColumnPlan.invalid("성토 열에 물·용암이 있어 먼저 배수해야 합니다.");
            if (state.isAir() || state.canBeReplaced()) {
                fill++;
                continue;
            }
            supportFound = true;
            break;
        }
        if (!supportFound) {
            return ColumnPlan.invalid("성토 지지면이 너무 낮습니다. 성토 깊이는 최대 " + MAX_FILL_DEPTH + "블록입니다.");
        }
        return new ColumnPlan(true, cut, reusable, fill, "");
    }

    private static boolean safeDemolitionTarget(ServerLevel level, BlockPos target) {
        BlockState state = level.getBlockState(target);
        return !state.isAir() && level.getBlockEntity(target) == null
                && state.getFluidState().isEmpty() && state.getDestroySpeed(level, target) >= 0.0F;
    }

    private static boolean isReusableCut(BlockState state) {
        return isNaturalGround(state);
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

    private static boolean withinHorizontalDistance(BlockPos a, BlockPos b, int maxDistance) {
        long dx = Math.abs((long) a.getX() - b.getX());
        long dz = Math.abs((long) a.getZ() - b.getZ());
        if (dx > maxDistance || dz > maxDistance) return false;
        return dx * dx + dz * dz <= (long) maxDistance * maxDistance;
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
        return new Check(false, 0, 0, 0, 0, 0, 0, 0, 0, 0, message);
    }
}
