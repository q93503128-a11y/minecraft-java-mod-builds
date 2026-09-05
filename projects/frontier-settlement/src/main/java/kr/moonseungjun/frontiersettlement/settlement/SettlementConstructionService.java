package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.content.FrontierContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class SettlementConstructionService {
    static final String BUILDER_TAG = "frontier_settlement_builder";
    private static final String BUILDER_NAME = "건설 주민";
    private static final int DIRECT_BLOCK_UPDATE = 2;
    private static final int NORMAL_BLOCK_UPDATE = 3;
    private static final double WORK_POSITION_REACHED_SQR = 9.0D;
    private static final int SITE_WORK_MARGIN = 12;
    // Direct hand reach is deliberately much smaller than scaffold coverage. Reusing the 14-block
    // scaffold coverage radius here let a builder stand on the ground and place an entire tower roof.
    private static final double SUPPLY_INTERACTION_RANGE_SQR = 9.0D;
    private static final int HAUL_BATCH_SIZE = 64;
    private static final long SITE_RESERVE_TARGET_PER_CATEGORY = 64L;
    private static final long SITE_RESERVE_LOW_WATER = 8L;
    private static final int GRADE_INTERVAL_TICKS = 1;
    private static final double GRADE_WORK_RANGE_SQR = 110.25D;
    private static final int BUILD_INTERVAL_TICKS = 2;
    private static final int MAX_GRADE_FILL_DEPTH = 3;
    private static final int SMALL_TERRAIN_SPAN = 2;
    private static final int MAX_TERRAIN_WORK_SPAN = 4;
    private static final int MAX_TERRAIN_CUT_HEIGHT = 3;
    private static final int MAX_TERRAIN_RETAINING_STONE = 96;
    private static final int TREE_CANOPY_SEARCH_HEIGHT = 10;
    private static final int TREE_CANOPY_SEARCH_RADIUS = 2;
    private static final int COMMAND_PLACEMENT_DISTANCE = 10;
    private static final int MAX_MAIN_SETTLEMENT_RADIUS = 72;
    private static final int MAX_PLAYER_PLACEMENT_DISTANCE = 24;
    private static final int MAX_SCAFFOLD_STEP = 7;
    private static final int BUILDER_ROUTE_MARGIN = 32;
    private static final int BASE_BUILDER_CREW = 2;
    private static final int BUILDERS_PER_CONSTRUCTION_OFFICE = 2;
    private static final int OUTPOST_BUILDER_BONUS_CAP = 6;
    private static final int MAX_BUILDER_CREW = 12;

    private SettlementConstructionService() {}

    public record StartResult(boolean started, String message) {}
    public record PlacementCheck(boolean valid, BlockPos origin, String message,
                                 boolean terrainWork, int terrainStoneCost) {}
    public record NormalizeConstructionResult(boolean active, boolean completed, int repairedBlocks, String message) {}
    private record Site(BlockPos origin, int terrainSpan, int terrainStoneCost) {
        boolean terrainWork() {
            return terrainSpan > SMALL_TERRAIN_SPAN || terrainStoneCost > 0;
        }
    }
    private record GradeCell(BlockPos floor, boolean foundation, int retainingStone) {}
    private record BlockSnapshot(BlockPos pos, BlockState state) {}
    private record ScaffoldPiece(BlockPos pos, BlockState state) {}
    private record ScaffoldTower(List<ScaffoldPiece> pieces) {}

    public static StartResult start(ServerPlayer player, BuildingType type) {
        Direction facing = player.getDirection();
        BlockPos selectedCenter = player.blockPosition().relative(facing, COMMAND_PLACEMENT_DISTANCE);
        BuildingRotation rotation = BuildingRotation.facingPlayerFrom(facing);
        return startAt(player, type, selectedCenter, rotation.id());
    }

    public static String lockedReason(SettlementData data, BuildingType type) {
        if (type == BuildingType.FARM && data.houseCount() < 1) return "농장은 주택 1채를 먼저 완성하면 열립니다.";
        if (type == BuildingType.QUARRY && data.lumberCampCount() < 1) return "채석장은 벌목소 1곳을 먼저 완성하면 열립니다.";
        if (type == BuildingType.MINE && (data.buildingCount(BuildingType.QUARRY) < 1 || data.outposts().isEmpty())) {
            return "광산은 채석장 1곳과 연결된 전초기지 1곳을 만든 뒤 열립니다.";
        }
        if (type == BuildingType.WAREHOUSE && data.buildingCount(BuildingType.FARM) < 1) return "창고는 농장 1곳을 완성하면 열립니다.";
        if (type == BuildingType.BLACKSMITH && data.buildingCount(BuildingType.MINE) < 1) {
            return "대장간은 광산 1곳을 완성하면 열립니다.";
        }
        if (type == BuildingType.GUARD_POST
                && SettlementTier.current(data).ordinal() < SettlementTier.VILLAGE.ordinal()) {
            return "경비초소는 마을 단계에 도달하면 열립니다.";
        }
        if (type == BuildingType.MARKET
                && SettlementTier.current(data).ordinal() < SettlementTier.VILLAGE.ordinal()) {
            return "시장은 마을 단계에 도달하면 열립니다.";
        }
        if (type == BuildingType.CIVIC_HALL
                && (SettlementTier.current(data).ordinal() < SettlementTier.FRONTIER_TOWN.ordinal()
                || data.buildingCount(BuildingType.MARKET) < 1
                || data.buildingCount(BuildingType.WAREHOUSE) < 1)) {
            return "시민회관은 개척 도시 단계와 시장·창고 각 1곳이 필요합니다.";
        }
        if (type == BuildingType.TRADE_HALL
                && (SettlementTier.current(data).ordinal() < SettlementTier.DOMAIN.ordinal()
                || data.buildingCount(BuildingType.MARKET) < 1
                || data.buildingCount(BuildingType.CART_STATION) < 1)) {
            return "교역회관은 영지 단계와 시장·수레 정거장 각 1곳이 필요합니다.";
        }
        if (type == BuildingType.CITADEL
                && (SettlementTier.current(data).ordinal() < SettlementTier.DOMAIN.ordinal()
                || data.buildingCount(BuildingType.BARRACKS) < 1
                || data.buildingCount(BuildingType.WATCHTOWER) < 1
                || data.explorationScore() < 5)) {
            return "성채는 영지 단계, 병영·감시탑 각 1곳, 탐험 점수 5가 필요합니다.";
        }
        return null;
    }

    public static PlacementCheck checkPlacement(ServerPlayer player, BuildingType type, BlockPos selectedCenter) {
        return checkPlacement(player, type, selectedCenter, BuildingRotation.NONE.id());
    }

    public static PlacementCheck checkPlacement(ServerPlayer player, BuildingType type, BlockPos selectedCenter, int rotationId) {
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return invalidPlacement("공동 마을이 없습니다.");
        if (player.level() != server.overworld()) return invalidPlacement("오버월드에서만 배치할 수 있습니다.");
        String projectBlock = SettlementProjectAuthority.startBlockReason(server, data, SettlementProjectAuthority.ProjectLane.BUILDING);
        if (projectBlock != null) return invalidPlacement(projectBlock);
        if (!SettlementProjectAuthority.separatedFromOtherActive(data, SettlementProjectAuthority.ProjectLane.BUILDING, selectedCenter)) {
            return invalidPlacement("동시 공사 현장은 서로 " + SettlementProjectAuthority.MIN_PARALLEL_SEPARATION + "블록 이상 떨어뜨려 주세요.");
        }
        String locked = lockedReason(data, type);
        if (locked != null) return invalidPlacement(locked);

        if (!withinHorizontalDistance(player.blockPosition(), selectedCenter, MAX_PLAYER_PLACEMENT_DISTANCE)) {
            return invalidPlacement("건설 위치는 플레이어 24블록 안에서 지정해 주세요.");
        }

        if (!withinHorizontalDistance(data.centerPos(), selectedCenter, MAX_MAIN_SETTLEMENT_RADIUS)) {
            return invalidPlacement("본진 기능 건물은 마을 중심 72블록 안에 배치해 주세요. 먼 지역은 전초기지를 사용합니다.");
        }

        BuildingRotation rotation = BuildingRotation.fromId(rotationId);
        int width = rotation.rotatedWidth(type);
        int depth = rotation.rotatedDepth(type);
        int originX = selectedCenter.getX() - width / 2;
        int originZ = selectedCenter.getZ() - depth / 2;
        ServerLevel level = server.overworld();
        Site site = assessSite(level, originX, originZ, type, rotation);
        if (site == null) {
            return invalidPlacement("선택한 부지가 안전하지 않습니다. 자연 잔디·꽃·수목은 자동 정리되며, 높이 차 4블록 이하·최대 3블록 성토 범위의 물·보호 블록이 없는 곳을 선택해 주세요.");
        }
        if (overlapsInfrastructure(data, site.origin(), type, rotation)) {
            return invalidPlacement("선택한 부지가 기존 건물·도로·전초기지 또는 공동 창고와 겹칩니다.");
        }
        ConstructionState gradingPreview = new ConstructionState(
                type.id(), site.origin().getX(), site.origin().getY(), site.origin().getZ(),
                rotation.id(), ConstructionState.GRADE_STEP_OFFSET);
        for (GradeCell cell : createGradePlan(level, gradingPreview, type)) {
            if (!canGradeCell(level, gradingPreview, type, cell)) {
                return invalidPlacement("건물 주변 1블록까지 부지 정리가 가능한 공간이 필요합니다. 물·보호된 블록·깊은 절벽·미로드 경계를 피해 다시 지정해 주세요.");
            }
        }
        String message = "배치 가능";
        if (site.terrainWork()) {
            message += " · 지형 공사 포함";
            if (site.terrainStoneCost() > 0) message += " · 옹벽/기초 추가 석재 " + site.terrainStoneCost();
        }
        return new PlacementCheck(true, site.origin(), message, site.terrainWork(), site.terrainStoneCost());
    }

    private static boolean withinHorizontalDistance(BlockPos a, BlockPos b, int maxDistance) {
        long dx = Math.abs((long) a.getX() - b.getX());
        long dz = Math.abs((long) a.getZ() - b.getZ());
        if (dx > maxDistance || dz > maxDistance) return false;
        return dx * dx + dz * dz <= (long) maxDistance * maxDistance;
    }

    private static PlacementCheck invalidPlacement(String message) {
        return new PlacementCheck(false, BlockPos.ZERO, message, false, 0);
    }

    public static StartResult startAt(ServerPlayer player, BuildingType type, BlockPos selectedCenter) {
        return startAt(player, type, selectedCenter, BuildingRotation.NONE.id());
    }

    public static StartResult startAt(ServerPlayer player, BuildingType type, BlockPos selectedCenter, int rotationId) {
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return new StartResult(false, "먼저 공동 마을을 시작해야 합니다.");
        if (player.level() != server.overworld()) return new StartResult(false, "건설은 현재 오버월드 공동 마을에서만 시작할 수 있습니다.");
        String projectBlock = SettlementProjectAuthority.startBlockReason(server, data, SettlementProjectAuthority.ProjectLane.BUILDING);
        if (projectBlock != null) return new StartResult(false, projectBlock);
        if (!SettlementProjectAuthority.separatedFromOtherActive(data, SettlementProjectAuthority.ProjectLane.BUILDING, selectedCenter)) {
            return new StartResult(false, "동시 공사 현장은 서로 " + SettlementProjectAuthority.MIN_PARALLEL_SEPARATION + "블록 이상 떨어뜨려 주세요.");
        }

        PlacementCheck check = checkPlacement(player, type, selectedCenter, rotationId);
        if (!check.valid()) return new StartResult(false, check.message());

        ServerLevel level = server.overworld();
        if (!SettlementStorageService.storageAvailable(level, data)) {
            return new StartResult(false, "공동 창고가 모두 로드된 상태에서 착공해 주세요. 자원은 차감되지 않았습니다.");
        }
        SettlementService.refreshResources(server, data);
        SettlementResources resources = data.resources();
        long requiredStone = type.stoneCost() + check.terrainStoneCost();
        if (resources.wood() < type.woodCost() || resources.stone() < requiredStone) {
            return new StartResult(false, type.displayName() + " 필요 자원: 목재 " + type.woodCost()
                    + ", 석재 " + requiredStone
                    + (check.terrainStoneCost() > 0 ? " (건물 " + type.stoneCost() + " + 지형 공사 " + check.terrainStoneCost() + ")" : "")
                    + " | 현재 목재 " + resources.wood() + ", 석재 " + resources.stone());
        }

        BuildingRotation rotation = BuildingRotation.fromId(rotationId);
        data.beginConstruction(type, check.origin(), rotation);
        data.replaceConstructionStep(ConstructionState.GRADE_STEP_OFFSET);
        List<FrontierWorkerEntity> builders = buildingProjectBuilders(level, data);
        if (builders.isEmpty()) {
            data.clearConstruction();
            SettlementService.broadcast(server, data);
            return new StartResult(false, "건설 작업자를 안전하게 확보할 수 없어 착공하지 않았습니다. 주변 마을·공동 창고 청크를 로드한 뒤 다시 시도해 주세요. 자원은 차감되지 않았습니다.");
        }
        SettlementService.broadcast(server, data);
        String terrain = check.terrainWork()
                ? " 지형 공사 포함: 건설 주민이 절토·성토와 노출 기초 옹벽을 먼저 시공합니다."
                : "";
        return new StartResult(true, type.displayName() + " 착공." + terrain
                + " 건설 주민이 공동 창고에서 실제 자재를 운반해 시공합니다."
                + " (필요 목재 " + type.woodCost() + ", 석재 " + requiredStone + ")");
    }

    public static boolean tick(MinecraftServer server, SettlementData data) {
        ConstructionState construction = data.construction();
        if (!construction.active()) return false;
        ServerLevel level = server.overworld();
        BuildingType type = BuildingType.fromId(construction.type());
        if (type == null) {
            FrontierWorkerEntity strandedBuilder = findBuilder(level, data);
            if (strandedBuilder != null) strandedBuilder.setInvulnerable(false);
            data.clearConstruction();
            return true;
        }

        List<FrontierWorkerEntity> builders = buildingProjectBuilders(level, data);
        if (builders.isEmpty()) return false;
        for (int i = 0; i < builders.size(); i++) {
            if (!data.construction().active()) return true;
            tickConstructionBuilder(server, data, type, builders.get(i), i == 0);
        }
        return !data.construction().active();
    }

    private static boolean tickConstructionBuilder(MinecraftServer server, SettlementData data,
                                                   BuildingType type, FrontierWorkerEntity builder,
                                                   boolean coordinator) {
        ServerLevel level = server.overworld();
        ConstructionState construction = data.construction();
        if (!construction.active()) return true;
        if (builder.isNoAi()) builder.setNoAi(false);
        builder.setInvulnerable(false);

        if (construction.grading()) return tickGrading(server, data, type, builder, coordinator);

        List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(type, construction.origin(), construction.rotation());
        BlockPos supply = supplyPosition(construction.origin(), type, construction.buildingRotation());
        int buildStep = construction.buildStep();
        if (buildStep >= plan.size()) return coordinator && finishIfValid(server, data, type, plan, builder, supply);

        Container crate = ensureSupplyCrate(level, supply);
        if (crate == null) return false;
        if (coordinator) {
            retireLegacyConstructionScaffolds(level, data, type, builder, supply);
            if (!stageRemainingMaterials(server, data, type, plan.size(), builder, crate, supply)) return false;
        } else if (!builder.getMainHandItem().isEmpty()) {
            return false;
        }
        if (server.getTickCount() % BUILD_INTERVAL_TICKS != 0) return false;

        construction = data.construction();
        buildStep = construction.buildStep();
        if (buildStep >= plan.size()) return coordinator && finishIfValid(server, data, type, plan, builder, supply);
        BuildingBlueprints.Placement placement = plan.get(buildStep);
        if (!moveBuilderToWorkPosition(level, construction, type, placement, builder, supply)) return false;

        BlockPos target = placement.pos();
        if (!level.hasChunkAt(target)) return false;
        BlockState current = level.getBlockState(target);
        if (!current.is(placement.state().getBlock()) && !canReplaceConstructionTarget(level, target, current)) {
            builder.getNavigation().stop();
            return false;
        }

        long woodDelta = costAtStep(type.woodCost(), buildStep + 1, plan.size())
                - costAtStep(type.woodCost(), buildStep, plan.size());
        long stoneDelta = costAtStep(type.stoneCost(), buildStep + 1, plan.size())
                - costAtStep(type.stoneCost(), buildStep, plan.size());
        if (SettlementInventory.countWood(crate) < woodDelta || SettlementInventory.countStone(crate) < stoneDelta) return false;

        boolean placedNow = false;
        if (!current.is(placement.state().getBlock())) {
            if (!level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE)) return false;
            placedNow = true;
        }
        if (!SettlementInventory.consume(crate, woodDelta, stoneDelta, 0L)) {
            if (placedNow) level.setBlock(target, current, NORMAL_BLOCK_UPDATE);
            return false;
        }
        if (placedNow) builder.swing(InteractionHand.MAIN_HAND);
        data.advanceConstruction();
        if (data.construction().buildStep() >= plan.size()) {
            return coordinator && finishIfValid(server, data, type, plan, builder, supply);
        }
        return false;
    }

    private static boolean tickGrading(MinecraftServer server, SettlementData data,
                                       BuildingType type, FrontierWorkerEntity builder,
                                       boolean coordinator) {
        ServerLevel level = server.overworld();
        ConstructionState construction = data.construction();
        List<GradeCell> plan = createGradePlan(level, construction, type);
        int gradeStep = construction.gradeStep();
        if (gradeStep >= plan.size()) {
            data.replaceConstructionStep(ConstructionState.BUILD_STEP_OFFSET);
            return false;
        }
        GradeCell cell = plan.get(gradeStep);
        if (!canGradeCell(level, construction, type, cell)) {
            builder.getNavigation().stop();
            return false;
        }

        Container terrainCrate = null;
        if (cell.retainingStone() > 0) {
            BlockPos supply = supplyPosition(construction.origin(), type, construction.buildingRotation());
            terrainCrate = ensureSupplyCrate(level, supply);
            if (terrainCrate == null) return false;
            if (coordinator) {
                if (!stageTerrainStone(server, data, builder, terrainCrate, supply, cell.retainingStone())) return false;
            } else if (SettlementInventory.countStone(terrainCrate) < cell.retainingStone()) {
                return false;
            }
        }
        if (server.getTickCount() % GRADE_INTERVAL_TICKS != 0) return false;

        BlockPos work = gradeWorkPosition(level, cell.floor());
        if (builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D) > GRADE_WORK_RANGE_SQR) {
            moveBuilderTowardGradeCell(level, builder, work);
            return false;
        }
        if (cell.retainingStone() > 0 && (terrainCrate == null
                || SettlementInventory.countStone(terrainCrate) < cell.retainingStone())) return false;

        List<BlockSnapshot> gradeMutation = applyGradeCellTransactional(level, construction, type, cell);
        if (gradeMutation == null) return false;
        if (cell.retainingStone() > 0 && !SettlementInventory.consume(terrainCrate, 0L, cell.retainingStone(), 0L)) {
            rollbackGradeMutation(level, gradeMutation);
            return false;
        }
        if (cell.retainingStone() > 0) {
            SettlementService.refreshResources(server, data);
            SettlementService.broadcast(server, data);
        }
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceConstruction();
        if (data.construction().gradeStep() >= plan.size()) data.replaceConstructionStep(ConstructionState.BUILD_STEP_OFFSET);
        return false;
    }

    private static boolean moveBuilderTowardGradeCell(ServerLevel level, FrontierWorkerEntity builder, BlockPos target) {
        if (moveToReachable(builder, target, 1.05D)) return true;
        int[][] offsets = { {1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1} };
        for (int[] offset : offsets) {
            int x = target.getX() + offset[0];
            int z = target.getZ() + offset[1];
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos candidate = new BlockPos(x, y, z);
            if (!isWalkableApproachCell(level, candidate)) continue;
            if (moveToReachable(builder, candidate, 1.05D)) return true;
        }
        builder.getNavigation().stop();
        return false;
    }

    private static boolean hasReachableGradeWorkPosition(ServerLevel level, FrontierWorkerEntity builder, BlockPos target) {
        if (createReachablePath(builder, target) != null) return true;
        int[][] offsets = { {1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1} };
        for (int[] offset : offsets) {
            int x = target.getX() + offset[0];
            int z = target.getZ() + offset[1];
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos candidate = new BlockPos(x, y, z);
            if (isWalkableApproachCell(level, candidate) && createReachablePath(builder, candidate) != null) return true;
        }
        return false;
    }

    private static List<GradeCell> createGradePlan(ServerLevel level, ConstructionState construction, BuildingType type) {
        BuildingRotation rotation = construction.buildingRotation();
        int width = rotation.rotatedWidth(type);
        int depth = rotation.rotatedDepth(type);
        List<GradeCell> result = new ArrayList<>((width + 2) * (depth + 2));
        for (int x = -1; x <= width; x++) {
            for (int z = -1; z <= depth; z++) {
                boolean foundation = x >= 0 && x < width && z >= 0 && z < depth;
                boolean edge = foundation && (x == 0 || x == width - 1 || z == 0 || z == depth - 1);
                BlockPos floor = construction.origin().offset(x, -1, z);
                int fillDepth = foundation ? fillDepthToSupport(level, floor) : 0;
                int retainingStone = edge && fillDepth >= 2 ? fillDepth : 0;
                result.add(new GradeCell(floor, foundation, retainingStone));
            }
        }
        return List.copyOf(result);
    }

    private static boolean canGradeCell(ServerLevel level, ConstructionState construction,
                                        BuildingType type, GradeCell cell) {
        if (!level.hasChunkAt(cell.floor())) return false;
        BlockPos column = cell.floor().above();
        for (int y = 0; y <= type.clearHeight(); y++) {
            BlockPos pos = column.above(y);
            BlockState state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) != null || !state.getFluidState().isEmpty()) return false;
            if (isClearableSiteVegetation(level, pos, state)) continue;
            if (y <= MAX_TERRAIN_CUT_HEIGHT && isNaturalGround(state)) continue;
            return false;
        }
        if (!cell.foundation()) return true;

        BlockState floorState = level.getBlockState(cell.floor());
        if (level.getBlockEntity(cell.floor()) != null || !floorState.getFluidState().isEmpty()) return false;
        if (!floorState.isAir() && !floorState.canBeReplaced() && !isNaturalGround(floorState)) return false;
        return fillDepthToSupport(level, cell.floor()) >= 0;
    }

    private static int fillDepthToSupport(ServerLevel level, BlockPos floor) {
        BlockState floorState = level.getBlockState(floor);
        if (level.getBlockEntity(floor) != null || !floorState.getFluidState().isEmpty()) return -1;
        if (!floorState.isAir() && !floorState.canBeReplaced()) return isNaturalGround(floorState) ? 0 : -1;
        for (int depth = 1; depth <= MAX_GRADE_FILL_DEPTH; depth++) {
            BlockPos support = floor.below(depth);
            BlockState state = level.getBlockState(support);
            if (level.getBlockEntity(support) != null || !state.getFluidState().isEmpty()) return -1;
            if (!state.isAir() && !state.canBeReplaced()) return isNaturalGround(state) ? depth : -1;
        }
        return -1;
    }

    private static BlockPos gradeWorkPosition(ServerLevel level, BlockPos floor) {
        int y = terrainSurfaceHeight(level, floor.getX(), floor.getZ());
        return new BlockPos(floor.getX(), y, floor.getZ());
    }

    /**
     * Applies one grade cell as a reversible world transaction. The caller commits retaining stone
     * only after this returns a non-null snapshot list. A null result means every successful partial
     * mutation was rolled back and neither material nor construction step may advance.
     */
    private static List<BlockSnapshot> applyGradeCellTransactional(ServerLevel level, ConstructionState construction,
                                                                    BuildingType type, GradeCell cell) {
        List<BlockSnapshot> changed = new ArrayList<>();
        BlockPos column = cell.floor().above();
        for (int y = type.clearHeight(); y >= 0; y--) {
            BlockPos pos = column.above(y);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (!setGradeBlock(level, pos, Blocks.AIR.defaultBlockState(), changed)) {
                rollbackGradeMutation(level, changed);
                return null;
            }
        }
        if (!cell.foundation()) return List.copyOf(changed);

        BlockState fill = cell.retainingStone() > 0
                ? Blocks.COBBLESTONE.defaultBlockState()
                : Blocks.COARSE_DIRT.defaultBlockState();
        if (!setGradeBlock(level, cell.floor(), fill, changed)) {
            rollbackGradeMutation(level, changed);
            return null;
        }
        for (int depth = 1; depth <= MAX_GRADE_FILL_DEPTH; depth++) {
            BlockPos support = cell.floor().below(depth);
            BlockState state = level.getBlockState(support);
            if (!state.isAir() && !state.canBeReplaced()) break;
            if (!setGradeBlock(level, support, fill, changed)) {
                rollbackGradeMutation(level, changed);
                return null;
            }
        }
        return List.copyOf(changed);
    }

    private static boolean setGradeBlock(ServerLevel level, BlockPos pos, BlockState next,
                                         List<BlockSnapshot> changed) {
        BlockState current = level.getBlockState(pos);
        if (current.equals(next)) return true;
        if (!level.setBlock(pos, next, DIRECT_BLOCK_UPDATE)) return false;
        changed.add(new BlockSnapshot(pos, current));
        return true;
    }

    private static void rollbackGradeMutation(ServerLevel level, List<BlockSnapshot> changed) {
        for (int i = changed.size() - 1; i >= 0; i--) {
            BlockSnapshot snapshot = changed.get(i);
            level.setBlock(snapshot.pos(), snapshot.state(), DIRECT_BLOCK_UPDATE);
        }
    }

    private static boolean stageTerrainStone(MinecraftServer server, SettlementData data, FrontierWorkerEntity builder,
                                             Container crate, BlockPos supply, int requiredStone) {
        long missing = Math.max(0L, requiredStone - SettlementInventory.countStone(crate));
        if (missing <= 0L) return true;
        ServerLevel level = server.overworld();
        ItemStack carried = builder.getMainHandItem();
        if (!carried.isEmpty()) {
            if (builder.distanceToSqr(supply.getX() + 0.5D, supply.getY() + 0.5D, supply.getZ() + 0.5D)
                    > SUPPLY_INTERACTION_RANGE_SQR) {
                moveTowardInteraction(level, builder, supply, 1.10D);
                return false;
            }
            int before = carried.getCount();
            ItemStack remaining = SettlementInventory.insert(crate, carried);
            builder.setItemSlot(EquipmentSlot.MAINHAND, remaining);
            if (remaining.getCount() < before) {
                SettlementService.refreshResources(server, data);
                SettlementService.broadcast(server, data);
            }
            return false;
        }

        BlockPos source = findReachableExtractionTarget(level, data, builder, SettlementInventory::isStone);
        if (source == null) return false;
        if (builder.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > SUPPLY_INTERACTION_RANGE_SQR) {
            moveTowardInteraction(level, builder, source, 1.10D);
            return false;
        }
        int amount = (int) Math.min((long) HAUL_BATCH_SIZE, missing);
        ItemStack extracted = SettlementStorageService.extract(level, source, SettlementInventory::isStone, amount);
        if (extracted.isEmpty()) return false;
        builder.setItemSlot(EquipmentSlot.MAINHAND, extracted);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return false;
    }

    private static boolean stageRemainingMaterials(MinecraftServer server, SettlementData data, BuildingType type,
                                                   int totalSteps, FrontierWorkerEntity builder, Container crate, BlockPos supply) {
        int step = data.construction().buildStep();
        ServerLevel level = server.overworld();
        long spentWood = costAtStep(type.woodCost(), step, totalSteps);
        long spentStone = costAtStep(type.stoneCost(), step, totalSteps);
        long remainingWood = Math.max(0L, type.woodCost() - spentWood);
        long remainingStone = Math.max(0L, type.stoneCost() - spentStone);
        long currentWood = SettlementInventory.countWood(crate);
        long currentStone = SettlementInventory.countStone(crate);
        long nextWoodDelta = Math.max(0L, costAtStep(type.woodCost(), step + 1, totalSteps) - spentWood);
        long nextStoneDelta = Math.max(0L, costAtStep(type.stoneCost(), step + 1, totalSteps) - spentStone);

        // Alpha.85 accidentally treated every item consumed from a full reserve as an immediate
        // refill request. A 32 -> 31 transition therefore sent the same builder back to town for
        // exactly one item before another blueprint step could run. Keep physical hauling, but use
        // a low-water mark: initial staging is large, construction continues locally, and another
        // town trip is requested only when the crate is actually running low (or cannot fund the
        // very next transactional placement).
        boolean needsWood = currentWood < nextWoodDelta
                || (remainingWood > currentWood && currentWood <= SITE_RESERVE_LOW_WATER);
        boolean needsStone = currentStone < nextStoneDelta
                || (remainingStone > currentStone && currentStone <= SITE_RESERVE_LOW_WATER);
        long targetWood = Math.min(SITE_RESERVE_TARGET_PER_CATEGORY, remainingWood);
        long targetStone = Math.min(SITE_RESERVE_TARGET_PER_CATEGORY, remainingStone);
        long missingWood = needsWood ? Math.max(0L, targetWood - currentWood) : 0L;
        long missingStone = needsStone ? Math.max(0L, targetStone - currentStone) : 0L;

        ItemStack carried = builder.getMainHandItem();
        if (!carried.isEmpty()) {
            boolean usefulHere = (SettlementInventory.isWood(carried) && missingWood > 0L)
                    || (SettlementInventory.isStone(carried) && missingStone > 0L);
            if (!usefulHere) {
                returnCarriedToTownStorage(server, data, builder);
                return false;
            }
            if (builder.distanceToSqr(supply.getX() + 0.5D, supply.getY() + 0.5D, supply.getZ() + 0.5D)
                    > SUPPLY_INTERACTION_RANGE_SQR) {
                moveTowardInteraction(level, builder, supply, 1.10D);
                return false;
            }
            int before = carried.getCount();
            ItemStack remaining = SettlementInventory.insert(crate, carried);
            builder.setItemSlot(EquipmentSlot.MAINHAND, remaining);
            if (remaining.getCount() < before) {
                SettlementService.refreshResources(server, data);
                SettlementService.broadcast(server, data);
            }
            if (remaining.getCount() == before) returnCarriedToTownStorage(server, data, builder);
            return false;
        }

        if (!needsWood && !needsStone) return true;
        Predicate<ItemStack> wanted = needsWood ? SettlementInventory::isWood : SettlementInventory::isStone;
        long missing = needsWood ? missingWood : missingStone;
        if (missing <= 0L) return true;
        BlockPos source = findReachableExtractionTarget(level, data, builder, wanted);
        if (source == null) return false;
        if (builder.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > SUPPLY_INTERACTION_RANGE_SQR) {
            moveTowardInteraction(level, builder, source, 1.10D);
            return false;
        }

        int amount = (int) Math.min((long) HAUL_BATCH_SIZE, missing);
        ItemStack extracted = SettlementStorageService.extract(level, source, wanted, amount);
        if (extracted.isEmpty()) return false;
        builder.setItemSlot(EquipmentSlot.MAINHAND, extracted);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return false;
    }

    private static boolean returnCarriedToTownStorage(MinecraftServer server, SettlementData data, FrontierWorkerEntity builder) {
        ItemStack carried = builder.getMainHandItem();
        if (carried.isEmpty()) return true;
        ServerLevel level = server.overworld();
        BlockPos target = findReachableDepositTarget(level, data, builder, carried);
        if (target == null || !level.hasChunkAt(target) || !SettlementStorageService.hasRoomAt(level, target, carried)) {
            builder.getNavigation().stop();
            return false;
        }
        if (builder.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                > SUPPLY_INTERACTION_RANGE_SQR) {
            moveTowardInteraction(level, builder, target, 1.10D);
            return false;
        }
        ItemStack remaining = SettlementStorageService.insertAt(level, target, carried);
        builder.setItemSlot(EquipmentSlot.MAINHAND, remaining);
        if (remaining.isEmpty()) {
            SettlementService.refreshResources(server, data);
            SettlementService.broadcast(server, data);
            return true;
        }
        return false;
    }

    private static long costAtStep(long totalCost, int step, int totalSteps) {
        if (totalCost <= 0L || step <= 0 || totalSteps <= 0) return 0L;
        if (step >= totalSteps) return totalCost;
        return totalCost * step / totalSteps;
    }

    public static int gradingSteps(ServerLevel level, ConstructionState construction, BuildingType type) {
        if (construction == null || !construction.active() || type == null) return 0;
        return createGradePlan(level, construction, type).size();
    }

    public static int totalSteps(BuildingType type, BlockPos origin) {
        return totalSteps(type, origin, BuildingRotation.NONE.id());
    }

    public static int totalSteps(BuildingType type, BlockPos origin, int rotationId) {
        return RotatedBlueprints.create(type, origin, rotationId).size();
    }

    public static String phaseLabel(ConstructionState construction) {
        if (construction == null || !construction.active()) return "";
        if (construction.grading()) return "건물 부지 정리";
        BuildingType type = BuildingType.fromId(construction.type());
        if (type == null) return "건설";
        List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(type, construction.origin(), construction.rotation());
        int step = construction.buildStep();
        if (step <= 0) return "자재 운반";
        if (step >= plan.size()) return "마감 확인";
        return switch (plan.get(step).phase()) {
            case FLOOR -> "기초 시공";
            case FRAME_AND_WALLS -> "골조·벽체 시공";
            case ROOF -> "지붕 시공";
            case FINISH -> "내부·마감 시공";
        };
    }

    public static String constructionIssue(MinecraftServer server, SettlementData data) {
        ConstructionState construction = data.construction();
        if (!construction.active()) return "";
        BuildingType type = BuildingType.fromId(construction.type());
        if (type == null) return "알 수 없는 건물 공사 상태";
        ServerLevel level = server.overworld();
        FrontierWorkerEntity builder = findBuilder(level, data);
        if (builder == null) return "건설 주민 확인 대기 · 마을·창고·현장 주변 청크를 로드하세요";

        if (construction.grading()) {
            List<GradeCell> gradePlan = createGradePlan(level, construction, type);
            int gradeStep = construction.gradeStep();
            if (gradeStep >= gradePlan.size()) return "";
            GradeCell cell = gradePlan.get(gradeStep);
            if (!canGradeCell(level, construction, type, cell)) {
                BlockPos pos = cell.floor();
                return "부지 정리 막힘 · " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                        + " 주변의 물·보호 블록·깊은 지형을 확인하세요";
            }
            if (cell.retainingStone() > 0) {
                BlockPos supply = supplyPosition(construction.origin(), type, construction.buildingRotation());
                if (!level.hasChunkAt(supply)) return "부지 정리 자재통 청크 미로드";
                Container crate = level.getBlockState(supply).is(Blocks.BARREL)
                        && level.getBlockEntity(supply) instanceof Container existing ? existing : null;
                long staged = crate == null ? 0L : SettlementInventory.countStone(crate);
                if (staged < cell.retainingStone()) {
                    BlockPos stoneSource = SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isStone);
                    if (stoneSource == null) return "부지 정리 석재 대기 · 공동 저장소에 석재를 보충하세요";
                    if (findReachableExtractionTarget(level, data, builder, SettlementInventory::isStone) == null) {
                        return "부지 정리 석재 접근 불가 · 저장소까지 실제 통로를 확인하세요";
                    }
                }
            }
            BlockPos work = gradeWorkPosition(level, cell.floor());
            if (builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D) > GRADE_WORK_RANGE_SQR
                    && !hasReachableGradeWorkPosition(level, builder, work)) {
                return "부지 정리 현장 접근 불가 · 현재 정리 칸까지 실제 통로를 확인하세요";
            }
            return "";
        }

        List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(
                type, construction.origin(), construction.rotation());
        BlockPos supply = supplyPosition(construction.origin(), type, construction.buildingRotation());
        if (!level.hasChunkAt(supply)) {
            return "현장 자재통 청크 미로드 · " + supply.getX() + ", " + supply.getY() + ", " + supply.getZ();
        }
        BlockState supplyState = level.getBlockState(supply);
        if (!(supplyState.is(Blocks.BARREL) && level.getBlockEntity(supply) instanceof Container)
                && (level.getBlockEntity(supply) != null || !supplyState.getFluidState().isEmpty()
                || (!supplyState.isAir() && !supplyState.canBeReplaced()))) {
            return "현장 자재통 위치 막힘 · " + supply.getX() + ", " + supply.getY() + ", " + supply.getZ();
        }

        int step = construction.buildStep();
        if (step < plan.size()) {
            BuildingBlueprints.Placement placement = plan.get(step);
            BlockPos pos = placement.pos();
            if (!level.hasChunkAt(pos)) {
                return "다음 시공 위치 청크 미로드 · " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            }
            BlockState current = level.getBlockState(pos);
            if (!current.is(placement.state().getBlock()) && !canReplaceConstructionTarget(level, pos, current)) {
                return "다음 시공 위치 막힘 · " + current.getBlock().getName().getString() + " · "
                        + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            }
            Container crate = level.getBlockState(supply).is(Blocks.BARREL)
                    && level.getBlockEntity(supply) instanceof Container existing ? existing : null;
            if (!builder.getMainHandItem().isEmpty()
                    && builder.distanceToSqr(supply.getX() + 0.5D, supply.getY() + 0.5D, supply.getZ() + 0.5D)
                    > SUPPLY_INTERACTION_RANGE_SQR
                    && !canReachInteraction(level, builder, supply)) {
                return "현장 자재통 접근 불가 · 주변 통로 또는 발판을 확인하세요";
            }
            if (crate != null && builder.getMainHandItem().isEmpty()) {
                long woodDelta = costAtStep(type.woodCost(), step + 1, plan.size())
                        - costAtStep(type.woodCost(), step, plan.size());
                long stoneDelta = costAtStep(type.stoneCost(), step + 1, plan.size())
                        - costAtStep(type.stoneCost(), step, plan.size());
                if (SettlementInventory.countWood(crate) < woodDelta) {
                    BlockPos woodSource = SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isWood);
                    if (woodSource == null) return "건설 목재 대기 · 공동 저장소에 목재를 보충하세요";
                    if (findReachableExtractionTarget(level, data, builder, SettlementInventory::isWood) == null) {
                        return "건설 목재 접근 불가 · 자재가 든 저장소까지 통로를 확보하세요";
                    }
                }
                if (SettlementInventory.countStone(crate) < stoneDelta) {
                    BlockPos stoneSource = SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isStone);
                    if (stoneSource == null) return "건설 석재 대기 · 공동 저장소에 석재를 보충하세요";
                    if (findReachableExtractionTarget(level, data, builder, SettlementInventory::isStone) == null) {
                        return "건설 석재 접근 불가 · 자재가 든 저장소까지 통로를 확보하세요";
                    }
                }
            }
            if (!hasReachableGroundWorkPosition(level, construction, type, placement, builder, supply)) {
                return "건설 현장 접근 불가 · 건물 주변 지상 통로를 확인하세요";
            }
            return "";
        }

        for (BuildingBlueprints.Placement placement : plan) {
            BlockPos pos = placement.pos();
            if (!level.hasChunkAt(pos)) return "마감 위치 청크 미로드 · " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            BlockState current = level.getBlockState(pos);
            if (current.is(placement.state().getBlock()) || isRecoverableBlueprintDrift(current, placement.state())
                    || canReplaceConstructionTarget(level, pos, current)) continue;
            return "마감 위치 막힘 · " + current.getBlock().getName().getString() + " · "
                    + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        }
        return "";
    }

    /**
     * Explicit safe repair for /frontier normalize. Only a construction that has already consumed
     * every blueprint step is eligible. Air gaps and known self-changing farm/path blocks are repaired;
     * an unexpected solid/container is reported instead of being overwritten or deleting player items.
     */
    public static NormalizeConstructionResult normalizeCompletedConstruction(MinecraftServer server, SettlementData data) {
        ConstructionState construction = data.construction();
        if (!construction.active()) {
            return new NormalizeConstructionResult(false, false, 0, "활성 건물 공사 없음");
        }
        BuildingType type = BuildingType.fromId(construction.type());
        if (type == null) {
            return new NormalizeConstructionResult(true, false, 0, "알 수 없는 건물 공사 상태");
        }
        List<BuildingBlueprints.Placement> plan =
                RotatedBlueprints.create(type, construction.origin(), construction.rotation());
        if (construction.buildStep() < plan.size()) {
            return new NormalizeConstructionResult(true, false, 0,
                    type.displayName() + " 공사 " + construction.buildStep() + " / " + plan.size()
                            + " · 아직 100% 마감 단계가 아님");
        }

        ServerLevel level = server.overworld();
        int repaired = 0;
        for (BuildingBlueprints.Placement placement : plan) {
            BlockPos pos = placement.pos();
            if (!level.hasChunkAt(pos)) {
                return new NormalizeConstructionResult(true, false, repaired,
                        "마감 위치 청크 미로드: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            }
            BlockState current = level.getBlockState(pos);
            if (current.is(placement.state().getBlock())) continue;
            if (level.getBlockEntity(pos) != null) {
                return new NormalizeConstructionResult(true, false, repaired,
                        "예상치 못한 보관/블록 엔티티 보호: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            }
            if (!isRecoverableBlueprintDrift(current, placement.state())
                    && !canReplaceConstructionTarget(level, pos, current)) {
                return new NormalizeConstructionResult(true, false, repaired,
                        "예상치 못한 고체/유체 블록 보호: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                                + " · 직접 확인 후 다시 실행");
            }
            if (!level.setBlock(pos, placement.state(), NORMAL_BLOCK_UPDATE)) {
                return new NormalizeConstructionResult(true, false, repaired,
                        "마감 복구 실패: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            }
            repaired++;
        }

        FrontierWorkerEntity builder = ensureBuilder(level, data);
        if (builder == null) {
            return new NormalizeConstructionResult(true, false, repaired,
                    "건설 주민을 안전하게 확인할 수 없어 마감 기록은 유지됨");
        }
        BlockPos supply = supplyPosition(construction.origin(), type, construction.buildingRotation());
        boolean completed = finishIfValid(server, data, type, plan, builder, supply);
        return new NormalizeConstructionResult(true, completed, repaired,
                completed ? type.displayName() + " 마감 정상화 완료"
                        : type.displayName() + " 마감 상태 재검사 필요");
    }

    private static boolean isRecoverableBlueprintDrift(BlockState current, BlockState expected) {
        if (expected.is(Blocks.FARMLAND)) {
            return current.is(Blocks.DIRT) || current.is(Blocks.GRASS_BLOCK) || current.is(Blocks.COARSE_DIRT);
        }
        if (expected.is(Blocks.DIRT_PATH)) {
            return current.is(Blocks.DIRT) || current.is(Blocks.GRASS_BLOCK);
        }
        return false;
    }

    private static boolean finishIfValid(MinecraftServer server, SettlementData data, BuildingType type,
                                         List<BuildingBlueprints.Placement> plan, FrontierWorkerEntity builder,
                                         BlockPos supply) {
        ServerLevel level = server.overworld();
        for (BuildingBlueprints.Placement placement : plan) {
            if (!level.hasChunkAt(placement.pos())) return false;
            BlockState current = level.getBlockState(placement.pos());
            if (current.is(placement.state().getBlock())) continue;
            if (isRecoverableBlueprintDrift(current, placement.state())) {
                // Farmland may naturally fall back to dirt while a large farm is still being built.
                // This is Frontier-owned blueprint drift, not player obstruction; repair it in place
                // instead of leaving an otherwise completed farm permanently at 100% "마감 확인".
                if (server.getTickCount() % BUILD_INTERVAL_TICKS != 0) return false;
                if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;
                builder.swing(InteractionHand.MAIN_HAND);
                return false;
            }
            if (!canReplaceConstructionTarget(level, placement.pos(), current)) {
                builder.getNavigation().stop();
                return false;
            }
            if (server.getTickCount() % BUILD_INTERVAL_TICKS != 0) return false;
            // Already-paid blueprint drift uses the same safe ground-perimeter authority as ordinary construction.
            // High repairs never recreate the retired stair scaffold system.
            if (!moveBuilderToWorkPosition(level, data.construction(), type, placement, builder, supply)) return false;
            if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;
            builder.swing(InteractionHand.MAIN_HAND);
            return false;
        }

        // The valid physical blueprint is the commit boundary. Cargo consolidation, temporary scaffold
        // cleanup, an empty site barrel, and the builder's return walk are explicitly best-effort after
        // the settlement record commits, so none can leave a real finished building stuck at 100%.
        ConstructionState finished = data.construction();
        Container crate = level.getBlockEntity(supply) instanceof Container existing ? existing : null;
        if (crate != null) consolidateCompletionCargo(builder, crate, supply);
        boolean keepPhysicalLeftovers = (crate != null && !crateIsEmpty(crate)) || !builder.getMainHandItem().isEmpty();

        data.completeConstruction(type);
        builder.setInvulnerable(false);
        builder.setNoAi(false);
        builder.setCustomName(Component.literal(BUILDER_NAME));

        removeConstructionScaffoldsBestEffort(level, finished, type, supply);
        if (!keepPhysicalLeftovers && crate != null && level.getBlockState(supply).is(Blocks.BARREL)) {
            level.setBlock(supply, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
        }

        returnBuilderHome(level, data, builder);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return true;
    }

    private static void consolidateCompletionCargo(FrontierWorkerEntity builder, Container crate, BlockPos supply) {
        ItemStack carried = builder.getMainHandItem();
        if (carried.isEmpty()) return;
        if (builder.distanceToSqr(supply.getX() + 0.5D, supply.getY() + 0.5D, supply.getZ() + 0.5D)
                > SUPPLY_INTERACTION_RANGE_SQR) return;
        ItemStack remaining = SettlementInventory.insert(crate, carried);
        builder.setItemSlot(EquipmentSlot.MAINHAND, remaining);
    }

    private static boolean returnCrateExtrasPhysically(MinecraftServer server, SettlementData data,
                                                       FrontierWorkerEntity builder, Container crate, BlockPos supply) {
        if (!builder.getMainHandItem().isEmpty()) return returnCarriedToTownStorage(server, data, builder);
        ServerLevel level = server.overworld();
        int sourceSlot = -1;
        for (int slot = 0; slot < crate.getContainerSize(); slot++) {
            if (!crate.getItem(slot).isEmpty()) { sourceSlot = slot; break; }
        }
        if (sourceSlot < 0) return true;
        if (builder.distanceToSqr(supply.getX() + 0.5D, supply.getY() + 0.5D, supply.getZ() + 0.5D)
                > SUPPLY_INTERACTION_RANGE_SQR) {
            moveTowardInteraction(level, builder, supply, 1.10D);
            return false;
        }
        ItemStack moving = crate.getItem(sourceSlot).copy();
        crate.setItem(sourceSlot, ItemStack.EMPTY);
        crate.setChanged();
        builder.setItemSlot(EquipmentSlot.MAINHAND, moving);
        return false;
    }

    private static boolean crateIsEmpty(Container crate) {
        for (int slot = 0; slot < crate.getContainerSize(); slot++) {
            if (!crate.getItem(slot).isEmpty()) return false;
        }
        return true;
    }

    private static Container ensureSupplyCrate(ServerLevel level, BlockPos supply) {
        BlockPos head = supply.above();
        if (!level.hasChunkAt(supply) || !level.hasChunkAt(head)) return null;
        BlockState current = level.getBlockState(supply);
        BlockState above = level.getBlockState(head);
        if (current.is(Blocks.BARREL) && level.getBlockEntity(supply) instanceof Container crate) {
            if (level.getBlockEntity(head) != null || !above.getFluidState().isEmpty()) return null;
            if (!above.isAir() && !above.canBeReplaced()
                    && !isClearableSiteVegetation(level, head, above)) return null;
            if (!above.isAir() && !level.setBlock(head, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE)) return null;
            return crate;
        }
        if (level.getBlockEntity(supply) != null || level.getBlockEntity(head) != null
                || !current.getFluidState().isEmpty() || !above.getFluidState().isEmpty()) return null;
        if (!current.isAir() && !current.canBeReplaced()
                && !isClearableSiteVegetation(level, supply, current)) return null;
        if (!above.isAir() && !above.canBeReplaced()
                && !isClearableSiteVegetation(level, head, above)) return null;
        if (!above.isAir() && !level.setBlock(head, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE)) return null;
        if (!level.setBlock(supply, Blocks.BARREL.defaultBlockState(), DIRECT_BLOCK_UPDATE)) return null;
        return level.getBlockState(supply).is(Blocks.BARREL)
                && level.getBlockEntity(supply) instanceof Container crate ? crate : null;
    }

    private static boolean moveBuilderToWorkPosition(ServerLevel level, ConstructionState construction, BuildingType type,
                                                     BuildingBlueprints.Placement placement, FrontierWorkerEntity builder, BlockPos supply) {
        // The builder must physically reach the construction zone, but no individual blueprint block
        // requires a fragile exact perimeter cell or vertical scaffold. Once the worker is locally on site,
        // every height is authoritative from ground level. This keeps construction visible without letting
        // hedges, doorways or already-built walls turn one later blueprint step into a permanent stall.
        if (builderWithinSiteWorkEnvelope(level, construction, type, builder)) {
            builder.getNavigation().stop();
            return true;
        }
        for (BlockPos work : workPositionsFor(level, construction, type, placement, builder, supply)) {
            double workDistance = builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D);
            if (workDistance <= WORK_POSITION_REACHED_SQR) {
                builder.getNavigation().stop();
                return true;
            }
            if (moveToReachable(builder, work, 1.05D)) return false;
        }
        builder.getNavigation().stop();
        return false;
    }

    private static boolean builderWithinSiteWorkEnvelope(ServerLevel level, ConstructionState construction, BuildingType type,
                                                          FrontierWorkerEntity builder) {
        BuildingRotation rotation = construction.buildingRotation();
        int width = rotation.rotatedWidth(type);
        int depth = rotation.rotatedDepth(type);
        double minX = construction.originX() - SITE_WORK_MARGIN;
        double maxX = construction.originX() + width - 1 + SITE_WORK_MARGIN + 1.0D;
        double minZ = construction.originZ() - SITE_WORK_MARGIN;
        double maxZ = construction.originZ() + depth - 1 + SITE_WORK_MARGIN + 1.0D;
        if (builder.getX() < minX || builder.getX() > maxX || builder.getZ() < minZ || builder.getZ() > maxZ) return false;
        int x = (int) Math.floor(builder.getX());
        int z = (int) Math.floor(builder.getZ());
        BlockPos surface = safeSurfaceCell(level, x, z);
        return surface != null && Math.abs(builder.getY() - surface.getY()) <= 2.25D;
    }

    private static boolean hasReachableGroundWorkPosition(ServerLevel level, ConstructionState construction,
                                                          BuildingType type, BuildingBlueprints.Placement placement,
                                                          FrontierWorkerEntity builder, BlockPos supply) {
        if (builderWithinSiteWorkEnvelope(level, construction, type, builder)) return true;
        for (BlockPos work : workPositionsFor(level, construction, type, placement, builder, supply)) {
            if (createReachablePath(builder, work) != null) return true;
        }
        return false;
    }

    private static List<BlockPos> workPositionsFor(ServerLevel level, ConstructionState construction, BuildingType type,
                                                   BuildingBlueprints.Placement placement, FrontierWorkerEntity builder, BlockPos supply) {
        BlockPos target = placement.pos();
        BuildingRotation rotation = construction.buildingRotation();
        int width = rotation.rotatedWidth(type);
        int depth = rotation.rotatedDepth(type);
        int minX = construction.originX() - 1;
        int maxX = construction.originX() + width;
        int minZ = construction.originZ() - 1;
        int maxZ = construction.originZ() + depth;
        int alignedX = Math.max(construction.originX(), Math.min(construction.originX() + width - 1, target.getX()));
        int alignedZ = Math.max(construction.originZ(), Math.min(construction.originZ() + depth - 1, target.getZ()));

        Set<BlockPos> unique = new HashSet<>();
        addGroundWorkCandidate(level, unique, minX, alignedZ);
        addGroundWorkCandidate(level, unique, maxX, alignedZ);
        addGroundWorkCandidate(level, unique, alignedX, minZ);
        addGroundWorkCandidate(level, unique, alignedX, maxZ);

        // Fallback across the complete one-block grading ring. A tree, crate, doorway, or already-built wall
        // on one side must not make a tall blueprint step depend on climbing a temporary staircase.
        for (int x = minX; x <= maxX; x++) {
            addGroundWorkCandidate(level, unique, x, minZ);
            addGroundWorkCandidate(level, unique, x, maxZ);
        }
        for (int z = minZ + 1; z < maxZ; z++) {
            addGroundWorkCandidate(level, unique, minX, z);
            addGroundWorkCandidate(level, unique, maxX, z);
        }

        List<BlockPos> result = new ArrayList<>(unique);
        result.sort(Comparator.comparingDouble(pos -> {
            double tx = (double) pos.getX() - target.getX();
            double tz = (double) pos.getZ() - target.getZ();
            double targetHorizontal = tx * tx + tz * tz;
            return targetHorizontal * 4.0D + builder.distanceToSqr(
                    pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        }));
        return List.copyOf(result);
    }

    private static void addGroundWorkCandidate(ServerLevel level, Set<BlockPos> result, int x, int z) {
        BlockPos candidate = safeSurfaceCell(level, x, z);
        if (candidate != null) result.add(candidate);
    }

    private static void retireLegacyConstructionScaffolds(ServerLevel level, SettlementData data, BuildingType type,
                                                           FrontierWorkerEntity builder, BlockPos supply) {
        ConstructionState construction = data.construction();
        if (construction.scaffoldMask() == 0) return;
        BlockPos safe = findSafeBuilderHome(level, data);
        if (safe == null) return;
        builder.getNavigation().stop();
        builder.setPos(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
        removeConstructionScaffoldsBestEffort(level, construction, type, supply);
        data.setConstructionScaffoldMask(0);
    }

    private static Path createReachablePath(FrontierWorkerEntity builder, BlockPos target) {
        Path path = builder.getNavigation().createPath(target, 0);
        if (path == null || !path.canReach() || path.getEndNode() == null) return null;
        BlockPos end = path.getEndNode().asBlockPos();
        if (Math.abs(end.getX() - target.getX()) > 1
                || Math.abs(end.getY() - target.getY()) > 1
                || Math.abs(end.getZ() - target.getZ()) > 1) return null;
        if (!(builder.level() instanceof ServerLevel level) || !level.hasChunkAt(end)) return null;
        // Fresh vanilla navigation already accounts for legal stairs, slabs and other partial blocks.
        return path;
    }

    private static boolean moveToReachable(FrontierWorkerEntity builder, BlockPos target, double speed) {
        Path path = createReachablePath(builder, target);
        return path != null && builder.getNavigation().moveTo(path, speed);
    }

    private static boolean isWalkableApproachCell(ServerLevel level, BlockPos feet) {
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

    private static List<BlockPos> interactionApproachPositions(ServerLevel level, FrontierWorkerEntity builder,
                                                               BlockPos target) {
        int[][] offsets = { {0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1} };
        List<BlockPos> candidates = new ArrayList<>();
        for (int dy = -1; dy <= 1; dy++) {
            for (int[] offset : offsets) {
                BlockPos candidate = target.offset(offset[0], dy, offset[1]);
                if (isWalkableApproachCell(level, candidate)) candidates.add(candidate);
            }
        }
        candidates.sort(Comparator.comparingDouble(pos -> builder.distanceToSqr(
                pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D)));
        return List.copyOf(candidates);
    }

    private static boolean blocksCurrentPathCell(ServerLevel level, BlockPos pos, BlockState state) {
    if (state.isAir() || state.canBeReplaced()) return false;
    return !state.getCollisionShape(level, pos).isEmpty();
}

    private static boolean canReachInteraction(ServerLevel level, FrontierWorkerEntity builder, BlockPos target) {
        if (builder.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                <= SUPPLY_INTERACTION_RANGE_SQR) return true;
        for (BlockPos candidate : interactionApproachPositions(level, builder, target)) {
            if (createReachablePath(builder, candidate) != null) return true;
        }
        return false;
    }

    private static boolean moveTowardInteraction(ServerLevel level, FrontierWorkerEntity builder,
                                                 BlockPos target, double speed) {
        if (builder.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                <= SUPPLY_INTERACTION_RANGE_SQR) return true;
        for (BlockPos candidate : interactionApproachPositions(level, builder, target)) {
            if (moveToReachable(builder, candidate, speed)) return true;
        }
        builder.getNavigation().stop();
        return false;
    }

    private static BlockPos findReachableExtractionTarget(ServerLevel level, SettlementData data,
                                                          FrontierWorkerEntity builder, Predicate<ItemStack> predicate) {
        Set<BlockPos> excluded = new HashSet<>();
        while (true) {
            BlockPos source = SettlementStorageService.findExtractionTargetExcluding(level, data, predicate, excluded);
            if (source == null) return null;
            if (canReachInteraction(level, builder, source)) return source;
            excluded.add(source);
        }
    }

    private static BlockPos findReachableDepositTarget(ServerLevel level, SettlementData data,
                                                       FrontierWorkerEntity builder, ItemStack stack) {
        Set<BlockPos> excluded = new HashSet<>();
        while (true) {
            BlockPos target = SettlementStorageService.findDepositTargetExcluding(level, data, stack, excluded);
            if (target == null) return null;
            if (canReachInteraction(level, builder, target)) return target;
            excluded.add(target);
        }
    }

    private static double targetDistanceSqr(BlockPos work, BlockPos target) {
        double dx = (double) work.getX() + 0.5D - ((double) target.getX() + 0.5D);
        double dy = (double) work.getY() - ((double) target.getY() + 0.5D);
        double dz = (double) work.getZ() + 0.5D - ((double) target.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }


    private static boolean canReplaceConstructionTarget(ServerLevel level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) == null
                && state.getFluidState().isEmpty()
                && (state.isAir() || state.canBeReplaced());
    }


    private static List<ScaffoldTower> scaffoldTowers(BlockPos origin, BuildingType type,
                                                       BuildingRotation rotation, BlockPos supply) {
        int width = rotation.rotatedWidth(type);
        int depth = rotation.rotatedDepth(type);
        int midX = Math.max(1, width / 2);
        int midZ = Math.max(1, depth / 2);
        BlockPos westCenter = supply.offset(-1, 0, 0);
        BlockPos eastCenter = origin.offset(width + 2, 0, midZ);
        BlockPos northCenter = origin.offset(midX, 0, -3);
        BlockPos southCenter = origin.offset(midX, 0, depth + 2);
        return List.of(
                scaffoldTower(westCenter, supply),
                scaffoldTower(eastCenter, supply),
                scaffoldTower(northCenter, supply),
                scaffoldTower(southCenter, supply));
    }

    private static ScaffoldTower scaffoldTower(BlockPos center, BlockPos supply) {
        int[][] ring = new int[][] {
                {0, -1}, {1, -1}, {1, 0}, {1, 1},
                {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}
        };
        List<ScaffoldPiece> pieces = new ArrayList<>();
        BlockState support = Blocks.OAK_FENCE.defaultBlockState();
        BlockState tread = Blocks.OAK_PLANKS.defaultBlockState();
        for (int y = 0; y <= MAX_SCAFFOLD_STEP; y++) {
            pieces.add(new ScaffoldPiece(center.above(y), support));
        }
        int step = 0;
        for (int[] offset : ring) {
            if (step > MAX_SCAFFOLD_STEP) break;
            BlockPos column = center.offset(offset[0], 0, offset[1]);
            if (column.equals(supply)) continue;
            for (int y = 0; y < step; y++) pieces.add(new ScaffoldPiece(column.above(y), support));
            BlockPos treadPos = column.above(step);
            pieces.add(new ScaffoldPiece(treadPos, tread));
            step++;
        }
        return new ScaffoldTower(List.copyOf(pieces));
    }





    private static boolean towerUsable(ServerLevel level, ScaffoldTower tower) {
        for (ScaffoldPiece piece : tower.pieces()) {
            if (!level.hasChunkAt(piece.pos()) || !level.getBlockState(piece.pos()).is(piece.state().getBlock())) return false;
        }
        return true;
    }

    private static boolean removeConstructionScaffolds(ServerLevel level, ConstructionState construction,
                                                       BuildingType type, BlockPos supply) {
        List<ScaffoldTower> towers = scaffoldTowers(construction.origin(), type, construction.buildingRotation(), supply);
        for (int towerIndex = 0; towerIndex < towers.size(); towerIndex++) {
            if (!construction.ownsScaffold(towerIndex)) continue;
            List<ScaffoldPiece> pieces = towers.get(towerIndex).pieces();
            for (int i = pieces.size() - 1; i >= 0; i--) {
                ScaffoldPiece piece = pieces.get(i);
                if (!level.hasChunkAt(piece.pos())) return false;
                if (level.getBlockState(piece.pos()).is(piece.state().getBlock())
                        && !level.setBlock(piece.pos(), Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE)) return false;
            }
        }
        return true;
    }

    private static void removeConstructionScaffoldsBestEffort(ServerLevel level, ConstructionState construction,
                                                              BuildingType type, BlockPos supply) {
        List<ScaffoldTower> towers = scaffoldTowers(construction.origin(), type, construction.buildingRotation(), supply);
        for (int towerIndex = 0; towerIndex < towers.size(); towerIndex++) {
            if (!construction.ownsScaffold(towerIndex)) continue;
            List<ScaffoldPiece> pieces = towers.get(towerIndex).pieces();
            for (int i = pieces.size() - 1; i >= 0; i--) {
                ScaffoldPiece piece = pieces.get(i);
                if (!level.hasChunkAt(piece.pos())) continue;
                if (level.getBlockState(piece.pos()).is(piece.state().getBlock())) {
                    level.setBlock(piece.pos(), Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
                }
            }
        }
    }

    private static boolean isProtectedScaffoldBlock(ServerLevel level, ConstructionState construction,
                                                     BuildingType type, BlockPos supply, BlockPos pos) {
        List<ScaffoldTower> towers = scaffoldTowers(construction.origin(), type, construction.buildingRotation(), supply);
        for (int towerIndex = 0; towerIndex < towers.size(); towerIndex++) {
            if (!construction.ownsScaffold(towerIndex)) continue;
            for (ScaffoldPiece piece : towers.get(towerIndex).pieces()) {
                if (piece.pos().equals(pos) && level.getBlockState(pos).is(piece.state().getBlock())) return true;
            }
        }
        return false;
    }

    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (level != server.overworld()) return;
        SettlementData data = SettlementData.get(server);
        ConstructionState construction = data.construction();
        if (!construction.active()) return;
        BuildingType type = BuildingType.fromId(construction.type());
        if (type == null) return;
        BuildingRotation rotation = construction.buildingRotation();
        BlockPos pos = event.getPos();
        BlockPos supply = supplyPosition(construction.origin(), type, rotation);
        BlockState current = level.getBlockState(pos);

        if ((pos.equals(supply) && current.is(Blocks.BARREL))
                || isProtectedScaffoldBlock(level, construction, type, supply, pos)) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            return;
        }

        int width = rotation.rotatedWidth(type);
        int depth = rotation.rotatedDepth(type);
        int minFoundationY = construction.originY() - 1 - MAX_GRADE_FILL_DEPTH;
        if (pos.getY() >= minFoundationY && pos.getY() <= construction.originY() - 1
                && pos.getX() >= construction.originX() && pos.getX() < construction.originX() + width
                && pos.getZ() >= construction.originZ() && pos.getZ() < construction.originZ() + depth
                && (current.is(Blocks.COARSE_DIRT) || current.is(Blocks.COBBLESTONE))) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            return;
        }

        for (BuildingBlueprints.Placement placement : RotatedBlueprints.create(type, construction.origin(), construction.rotation())) {
            if (placement.pos().equals(pos) && current.is(placement.state().getBlock())) {
                event.setCanceled(true);
                event.setNotifyClient(true);
                return;
            }
        }
    }

    public static int desiredBuilderCount(SettlementData data) {
        int offices = Math.max(0, data.buildingCount(BuildingType.CONSTRUCTION_OFFICE));
        int outpostBonus = Math.min(OUTPOST_BUILDER_BONUS_CAP, data.outposts().size());
        return Math.min(MAX_BUILDER_CREW, BASE_BUILDER_CREW + offices * BUILDERS_PER_CONSTRUCTION_OFFICE + outpostBonus);
    }

    public static List<FrontierWorkerEntity> buildingProjectBuilders(ServerLevel level, SettlementData data) {
        List<FrontierWorkerEntity> all = ensureProjectBuilders(level, data);
        if (all.isEmpty()) return List.of();
        List<FrontierWorkerEntity> crew = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            if (data.roadConstruction().active() && i == 0) continue;
            if (data.outpostConstruction().active() && i == 1) continue;
            crew.add(all.get(i));
        }
        return List.copyOf(crew);
    }

    public static FrontierWorkerEntity infrastructureProjectBuilder(ServerLevel level, SettlementData data,
                                                                     SettlementProjectAuthority.ProjectLane lane) {
        List<FrontierWorkerEntity> all = ensureProjectBuilders(level, data);
        int index = switch (lane) {
            case ROAD -> 0;
            case OUTPOST -> 1;
            case BUILDING -> -1;
        };
        return index >= 0 && all.size() > index ? all.get(index) : null;
    }

    public static List<FrontierWorkerEntity> ensureProjectBuilders(ServerLevel level, SettlementData data) {
        // One authoritative query per active-project tick. The old path queried the same builder
        // envelope once in reconcileBuilderDuplicates() and immediately again in findBuilders().
        List<FrontierWorkerEntity> existing = new ArrayList<>(findBuilders(level, data));
        int desired = desiredBuilderCount(data);
        if (existing.size() > desired) {
            for (int i = desired; i < existing.size(); i++) removeDuplicateBuilderPreservingCargo(level, existing.get(i));
            existing = new ArrayList<>(existing.subList(0, desired));
        }
        for (FrontierWorkerEntity builder : existing) {
            if (!builder.entityTags().contains(BUILDER_TAG)) builder.addTag(BUILDER_TAG);
            builder.setNoAi(false);
            builder.setInvulnerable(false);
            recoverBuilderFromBlockedCell(level, data, builder);
        }
        if (existing.size() >= desired || !builderAssignmentEvidenceLoaded(level, data)) return List.copyOf(existing);

        Set<BlockPos> occupied = new HashSet<>();
        for (FrontierWorkerEntity builder : existing) occupied.add(builder.blockPosition());
        while (existing.size() < desired) {
            BlockPos spawn = findSafeBuilderHome(level, data, occupied);
            if (spawn == null) break;
            FrontierWorkerEntity builder = new FrontierWorkerEntity(FrontierContent.FRONTIER_WORKER.get(), level);
            builder.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
            builder.setYRot(0.0F);
            builder.setXRot(0.0F);
            builder.setCustomName(Component.literal(BUILDER_NAME));
            builder.setCustomNameVisible(true);
            builder.setPersistenceRequired();
            builder.setNoAi(false);
            builder.addTag(BUILDER_TAG);
            if (!level.addFreshEntity(builder)) break;
            existing.add(builder);
            occupied.add(spawn);
        }
        existing.sort(Comparator.comparing(builder -> builder.getUUID().toString()));
        return List.copyOf(existing);
    }

    public static FrontierWorkerEntity ensureProjectBuilder(ServerLevel level, SettlementData data) {
        List<FrontierWorkerEntity> builders = ensureProjectBuilders(level, data);
        return builders.isEmpty() ? null : builders.getFirst();
    }

    public static FrontierWorkerEntity ensureBuilder(ServerLevel level, SettlementData data) {
        return ensureProjectBuilder(level, data);
    }

    private static void recoverBuilderFromBlockedCell(ServerLevel level, SettlementData data, FrontierWorkerEntity builder) {
        BlockPos feet = builder.blockPosition();
        BlockPos head = feet.above();
        if (!level.hasChunkAt(feet) || !level.hasChunkAt(head)) return;
        if (!blocksCurrentPathCell(level, feet, level.getBlockState(feet))
                && !blocksCurrentPathCell(level, head, level.getBlockState(head))) return;
        BlockPos safe = findSafeBuilderHome(level, data);
        if (safe == null) return;
        builder.getNavigation().stop();
        builder.setPos(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
    }

    private static BlockPos findSafeBuilderHome(ServerLevel level, SettlementData data) {
        return findSafeBuilderHome(level, data, Set.of());
    }

    private static BlockPos findSafeBuilderHome(ServerLevel level, SettlementData data, Set<BlockPos> occupied) {
        BlockPos center = data.centerPos();
        BlockPos preferred = safeSurfaceCell(level, center.getX() + 1, center.getZ() + 1);
        if (preferred != null && !occupied.contains(preferred)) return preferred;
        for (int radius = 1; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BlockPos candidate = safeSurfaceCell(level, center.getX() + dx, center.getZ() + dz);
                    if (candidate != null && !occupied.contains(candidate)) return candidate;
                }
            }
        }
        BlockPos fallback = safeSurfaceCell(level, center.getX(), center.getZ());
        return fallback != null && !occupied.contains(fallback) ? fallback : null;
    }

    private static BlockPos safeSurfaceCell(ServerLevel level, int x, int z) {
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return null;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos candidate = new BlockPos(x, y, z);
        return isWalkableApproachCell(level, candidate) ? candidate : null;
    }

    /**
     * Reclaims historical duplicate shared builders once their complete legal lookup envelope is loaded.
     * The first UUID-ordered builder remains authoritative. Extras are never frozen or made invulnerable;
     * their exact MAINHAND cargo is first materialized as an ItemEntity, and only then are they discarded.
     */
    public static int reconcileBuilderDuplicates(ServerLevel level, SettlementData data) {
        List<FrontierWorkerEntity> builders = findBuilders(level, data);
        if (builders.isEmpty()) return 0;
        int allowed = desiredBuilderCount(data);
        int keep = Math.min(allowed, builders.size());
        for (int i = 0; i < keep; i++) {
            FrontierWorkerEntity builder = builders.get(i);
            if (!builder.entityTags().contains(BUILDER_TAG)) builder.addTag(BUILDER_TAG);
            builder.setNoAi(false);
            builder.setInvulnerable(false);
        }
        int removed = 0;
        for (int i = keep; i < builders.size(); i++) {
            if (removeDuplicateBuilderPreservingCargo(level, builders.get(i))) removed++;
        }
        return removed;
    }

    private static boolean removeDuplicateBuilderPreservingCargo(ServerLevel level, FrontierWorkerEntity duplicate) {
        duplicate.getNavigation().stop();
        duplicate.setNoAi(false);
        duplicate.setInvulnerable(false);
        ItemStack carried = duplicate.getMainHandItem();
        if (!carried.isEmpty()) {
            ItemEntity physical = new ItemEntity(level, duplicate.getX(), duplicate.getY(), duplicate.getZ(), carried.copy());
            if (!level.addFreshEntity(physical)) return false;
            duplicate.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        duplicate.discard();
        return true;
    }

    /**
     * Broader explicit maintenance scan for old saves. It is loaded-entity-only and never force-loads:
     * one UUID-ordered shared builder survives, excess loaded historical builders preserve cargo before
     * removal, and an idle survivor is sent back toward the settlement center.
     */
    public static int normalizeLoadedBuilders(ServerLevel level, SettlementData data) {
        List<FrontierWorkerEntity> builders = new ArrayList<>(findBuilders(level, data));
        BlockPos center = data.centerPos();
        AABB maintenance = new AABB(
                center.getX() - 256.0D, center.getY() - 96.0D, center.getZ() - 256.0D,
                center.getX() + 257.0D, center.getY() + 97.0D, center.getZ() + 257.0D);
        for (FrontierWorkerEntity candidate : level.getEntitiesOfClass(FrontierWorkerEntity.class, maintenance, worker ->
                worker.entityTags().contains(BUILDER_TAG)
                        || (worker.getCustomName() != null && BUILDER_NAME.equals(worker.getCustomName().getString())))) {
            if (!builders.contains(candidate)) builders.add(candidate);
        }
        builders.sort(Comparator.comparing(worker -> worker.getUUID().toString()));
        if (builders.isEmpty()) return 0;

        int allowed = desiredBuilderCount(data);
        int keep = Math.min(allowed, builders.size());
        for (int i = 0; i < keep; i++) {
            FrontierWorkerEntity builder = builders.get(i);
            if (!builder.entityTags().contains(BUILDER_TAG)) builder.addTag(BUILDER_TAG);
            builder.setNoAi(false);
            builder.setInvulnerable(false);
            builder.getNavigation().stop();
        }
        int removed = 0;
        for (int i = keep; i < builders.size(); i++) {
            if (removeDuplicateBuilderPreservingCargo(level, builders.get(i))) removed++;
        }
        if (!SettlementProjectAuthority.anyActive(level.getServer(), data)) {
            for (int i = 0; i < keep; i++) returnBuilderHome(level, data, builders.get(i));
        }
        return removed;
    }

    public static void settleIdleBuilders(MinecraftServer server, SettlementData data) {
        if (SettlementProjectAuthority.anyActive(server, data)) return;
        ServerLevel level = server.overworld();
        for (FrontierWorkerEntity builder : findBuilders(level, data)) {
            builder.setNoAi(false);
            builder.setInvulnerable(false);
            builder.setCustomName(Component.literal(BUILDER_NAME));
            if (!builder.getMainHandItem().isEmpty()) {
                returnCarriedToTownStorage(server, data, builder);
                continue;
            }
            returnBuilderHome(level, data, builder);
        }
    }

    static boolean returnBuilderHome(ServerLevel level, SettlementData data, FrontierWorkerEntity builder) {
        BlockPos home = findSafeBuilderHome(level, data);
        if (home == null) {
            builder.getNavigation().stop();
            return false;
        }
        double distance = builder.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
        if (distance <= 4.0D) {
            builder.getNavigation().stop();
            return true;
        }
        if (!moveToReachable(builder, home, 1.10D)) builder.getNavigation().stop();
        return false;
    }

    static FrontierWorkerEntity findBuilder(ServerLevel level, SettlementData data) {
        List<FrontierWorkerEntity> builders = findBuilders(level, data);
        return builders.isEmpty() ? null : builders.getFirst();
    }

    private static List<FrontierWorkerEntity> findBuilders(ServerLevel level, SettlementData data) {
        AABB search = builderRouteBounds(level, data);
        List<FrontierWorkerEntity> builders = level.getEntitiesOfClass(FrontierWorkerEntity.class, search, villager ->
                villager.entityTags().contains(BUILDER_TAG)
                        || (villager.getCustomName() != null && BUILDER_NAME.equals(villager.getCustomName().getString())));
        builders.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return builders;
    }

    private static AABB builderRouteBounds(ServerLevel level, SettlementData data) {
        BlockPos center = data.centerPos();
        int minX = center.getX(), minY = center.getY(), minZ = center.getZ();
        int maxX = center.getX() + 1, maxY = center.getY() + 1, maxZ = center.getZ() + 1;
        for (BlockPos pos : SettlementStorageService.storagePositions(data)) {
            minX = Math.min(minX, pos.getX()); minY = Math.min(minY, pos.getY()); minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX() + 1); maxY = Math.max(maxY, pos.getY() + 1); maxZ = Math.max(maxZ, pos.getZ() + 1);
        }
        // Completed historical infrastructure is not a legal builder location. Including every old
        // building/road/outpost here made an unrelated distant unloaded chunk block builder recovery.
        // The builder is authoritative only at the town/storage envelope or the currently active project.
        if (data.construction().active()) {
            BlockPos pos = data.construction().origin();
            minX = Math.min(minX, pos.getX()); minY = Math.min(minY, pos.getY() - MAX_GRADE_FILL_DEPTH); minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX() + 16); maxY = Math.max(maxY, pos.getY() + 24); maxZ = Math.max(maxZ, pos.getZ() + 16);
        }
        if (data.roadConstruction().active()) {
            for (BlockPos pos : data.roadConstruction().centers()) {
                minX = Math.min(minX, pos.getX()); minY = Math.min(minY, pos.getY() - 16); minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX() + 1); maxY = Math.max(maxY, pos.getY() + 17); maxZ = Math.max(maxZ, pos.getZ() + 1);
            }
            for (BlockPos pos : data.roadConstruction().bridgeSupportPositions()) {
                minX = Math.min(minX, pos.getX()); minY = Math.min(minY, pos.getY()); minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX() + 1); maxY = Math.max(maxY, pos.getY() + 1); maxZ = Math.max(maxZ, pos.getZ() + 1);
            }
        }
        if (data.outpostConstruction().active()) {
            BlockPos gate = data.outpostConstruction().gate();
            minX = Math.min(minX, gate.getX() - 8); minY = Math.min(minY, gate.getY() - 8); minZ = Math.min(minZ, gate.getZ() - 8);
            maxX = Math.max(maxX, gate.getX() + 17); maxY = Math.max(maxY, gate.getY() + 24); maxZ = Math.max(maxZ, gate.getZ() + 17);
        }
        CivilWorkState civil = SettlementCivilWorkData.get(level.getServer()).project();
        if (civil.active()) {
            minX = Math.min(minX, civil.minX() - 1); minY = Math.min(minY, civil.gradeY() - SettlementCivilRetainingService.MAX_RETAINING_HEIGHT); minZ = Math.min(minZ, civil.minZ() - 1);
            maxX = Math.max(maxX, civil.maxX() + 2); maxY = Math.max(maxY, civil.gradeY() + SettlementCivilWorkService.MAX_CUT_DEPTH + 1); maxZ = Math.max(maxZ, civil.maxZ() + 2);
        }
        return new AABB(minX - BUILDER_ROUTE_MARGIN, minY - 48.0D, minZ - BUILDER_ROUTE_MARGIN,
                maxX + BUILDER_ROUTE_MARGIN, maxY + 48.0D, maxZ + BUILDER_ROUTE_MARGIN);
    }

    private static boolean builderAssignmentEvidenceLoaded(ServerLevel level, SettlementData data) {
        AABB bounds = builderRouteBounds(level, data);
        int minChunkX = Math.floorDiv((int) Math.floor(bounds.minX), 16);
        int maxChunkX = Math.floorDiv((int) Math.floor(Math.nextDown(bounds.maxX)), 16);
        int minChunkZ = Math.floorDiv((int) Math.floor(bounds.minZ), 16);
        int maxChunkZ = Math.floorDiv((int) Math.floor(Math.nextDown(bounds.maxZ)), 16);
        int probeY = data.centerPos().getY();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunkAt(new BlockPos(chunkX * 16 + 8, probeY, chunkZ * 16 + 8))) return false;
            }
        }
        return true;
    }

    private static Site assessSite(ServerLevel level, int originX, int originZ, BuildingType type, BuildingRotation rotation) {
        int width = rotation.rotatedWidth(type);
        int depth = rotation.rotatedDepth(type);
        List<Integer> heights = new ArrayList<>(width * depth);
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                int worldX = originX + x;
                int worldZ = originZ + z;
                if (!level.hasChunkAt(new BlockPos(worldX, 0, worldZ))) return null;
                int height = terrainSurfaceHeight(level, worldX, worldZ);
                BlockPos surfaceBlock = new BlockPos(worldX, height - 1, worldZ);
                if (!level.getFluidState(surfaceBlock).isEmpty()) return null;
                heights.add(height);
                min = Math.min(min, height);
                max = Math.max(max, height);
            }
        }
        int terrainSpan = max - min;
        if (terrainSpan > MAX_TERRAIN_WORK_SPAN) return null;
        Collections.sort(heights);
        int baseY = heights.get(heights.size() / 2);
        BlockPos origin = new BlockPos(originX, baseY, originZ);

        for (int x = -1; x <= width; x++) {
            for (int z = -1; z <= depth; z++) {
                for (int y = -MAX_GRADE_FILL_DEPTH; y <= type.clearHeight(); y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null) return null;
                    BlockState state = level.getBlockState(pos);
                    if (!state.getFluidState().isEmpty()) return null;
                    if (y >= 0 && !isSafeAboveGround(level, pos, state, y)) return null;
                    if (y == -1 && !state.isAir() && !state.canBeReplaced() && !isNaturalGround(state)) return null;
                }
            }
        }

        int terrainStoneCost = 0;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                BlockPos floor = origin.offset(x, -1, z);
                int fillDepth = fillDepthToSupport(level, floor);
                if (fillDepth < 0) return null;
                boolean edge = x == 0 || x == width - 1 || z == 0 || z == depth - 1;
                if (edge && fillDepth >= 2) {
                    terrainStoneCost += fillDepth;
                    if (terrainStoneCost > MAX_TERRAIN_RETAINING_STONE) return null;
                }
            }
        }
        if (!isSafeSupplyPosition(level, supplyPosition(origin, type, rotation))) return null;
        return new Site(origin, terrainSpan, terrainStoneCost);
    }

    private static boolean isSafeSupplyPosition(ServerLevel level, BlockPos supply) {
        if (!level.hasChunkAt(supply) || !level.hasChunkAt(supply.above()) || !level.hasChunkAt(supply.below())) return false;
        BlockState current = level.getBlockState(supply);
        BlockState above = level.getBlockState(supply.above());
        BlockState below = level.getBlockState(supply.below());
        if (level.getBlockEntity(supply) != null || level.getBlockEntity(supply.above()) != null) return false;
        if (!current.getFluidState().isEmpty() || !above.getFluidState().isEmpty() || !below.getFluidState().isEmpty()) return false;
        if ((!current.isAir() && !current.canBeReplaced()
                && !isClearableSiteVegetation(level, supply, current))
                || (!above.isAir() && !above.canBeReplaced()
                && !isClearableSiteVegetation(level, supply.above(), above))) return false;
        return !below.isAir() && isNaturalGround(below);
    }

    private static boolean overlapsInfrastructure(SettlementData data, BlockPos origin, BuildingType type, BuildingRotation rotation) {
        int width = rotation.rotatedWidth(type);
        int depth = rotation.rotatedDepth(type);
        int minX = origin.getX() - 2;
        int maxX = origin.getX() + width;
        int minZ = origin.getZ() - 1;
        int maxZ = origin.getZ() + depth;

        BlockPos stock = data.stockpilePos();
        if (stock.getX() >= minX && stock.getX() <= maxX && stock.getZ() >= minZ && stock.getZ() <= maxZ) return true;

        for (BuildingRecord existing : data.buildings()) {
            int oldMinX = existing.originX() - 1;
            int oldMaxX = existing.originX() + existing.rotatedWidth();
            int oldMinZ = existing.originZ() - 1;
            int oldMaxZ = existing.originZ() + existing.rotatedDepth();
            if (minX <= oldMaxX && maxX >= oldMinX && minZ <= oldMaxZ && maxZ >= oldMinZ) return true;
        }
        for (RoadSegment road : data.roads()) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (road.containsXZ(new BlockPos(x, origin.getY(), z))) return true;
                }
            }
        }
        for (OutpostRecord outpost : data.outposts()) {
            if (outpost.centerX() + 6 >= minX && outpost.centerX() - 6 <= maxX
                    && outpost.centerZ() + 6 >= minZ && outpost.centerZ() - 6 <= maxZ) return true;
        }
        return false;
    }

    private static int terrainSurfaceHeight(ServerLevel level, int x, int z) {
        int rawHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int scanned = 0, y = rawHeight - 1; scanned < 32; scanned++, y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (isNaturalGround(state)) return y + 1;
            if (!isClearableSiteVegetation(level, pos, state)) return rawHeight;
        }
        return rawHeight;
    }

    private static boolean isSafeAboveGround(ServerLevel level, BlockPos pos, BlockState state, int relativeY) {
        if (isClearableSiteVegetation(level, pos, state)) return true;
        return relativeY <= MAX_TERRAIN_CUT_HEIGHT && isNaturalGround(state);
    }

    private static boolean isClearableSiteVegetation(ServerLevel level, BlockPos pos, BlockState state) {
        return isSoftVegetation(state) || isNaturalTreeLog(level, pos, state);
    }

    private static boolean isNaturalTreeLog(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.is(BlockTags.LOGS)) return false;
        for (int dy = 0; dy <= TREE_CANOPY_SEARCH_HEIGHT; dy++) {
            for (int dx = -TREE_CANOPY_SEARCH_RADIUS; dx <= TREE_CANOPY_SEARCH_RADIUS; dx++) {
                for (int dz = -TREE_CANOPY_SEARCH_RADIUS; dz <= TREE_CANOPY_SEARCH_RADIUS; dz++) {
                    BlockPos probe = pos.offset(dx, dy, dz);
                    if (level.hasChunkAt(probe) && level.getBlockState(probe).is(BlockTags.LEAVES)) return true;
                }
            }
        }
        return false;
    }

    private static boolean isSoftVegetation(BlockState state) {
        if (state.isAir()) return true;
        if (!state.getFluidState().isEmpty()) return false;
        return state.canBeReplaced()
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.getBlock() instanceof net.minecraft.world.level.block.BushBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.SnowLayerBlock;
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
                || state.is(Blocks.SNOW_BLOCK);
    }

    private static BlockPos supplyPosition(BlockPos origin, BuildingType type, BuildingRotation rotation) {
        return origin.offset(-2, 0, Math.max(1, rotation.rotatedDepth(type) / 2));
    }
}
