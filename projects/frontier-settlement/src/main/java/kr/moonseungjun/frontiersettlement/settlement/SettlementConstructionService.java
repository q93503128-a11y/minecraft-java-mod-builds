package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public final class SettlementConstructionService {
    static final String BUILDER_TAG = "frontier_settlement_builder";
    private static final String BUILDER_NAME = "건설 주민";
    private static final int DIRECT_BLOCK_UPDATE = 2;
    private static final int NORMAL_BLOCK_UPDATE = 3;
    private static final double WORK_POSITION_REACHED_SQR = 2.25D;
    private static final double HIGH_WORK_RANGE_SQR = 49.0D;
    private static final double SUPPLY_INTERACTION_RANGE_SQR = 9.0D;
    private static final int HAUL_BATCH_SIZE = 16;
    private static final long MAX_SITE_RESERVE_PER_CATEGORY = 12L;
    private static final int GRADE_INTERVAL_TICKS = 8;
    private static final int BUILD_INTERVAL_TICKS = 10;
    private static final int MAX_GRADE_FILL_DEPTH = 3;
    private static final int SMALL_TERRAIN_SPAN = 2;
    private static final int MAX_TERRAIN_WORK_SPAN = 4;
    private static final int MAX_TERRAIN_CUT_HEIGHT = 3;
    private static final int MAX_TERRAIN_RETAINING_STONE = 96;
    private static final int COMMAND_PLACEMENT_DISTANCE = 10;
    private static final int MAX_MAIN_SETTLEMENT_RADIUS = 72;
    private static final int MAX_PLAYER_PLACEMENT_DISTANCE = 24;
    private static final int MAX_SCAFFOLD_STEP = 7;

    private SettlementConstructionService() {}

    public record StartResult(boolean started, String message) {}
    public record PlacementCheck(boolean valid, BlockPos origin, String message,
                                 boolean terrainWork, int terrainStoneCost) {}
    private record Site(BlockPos origin, int terrainSpan, int terrainStoneCost) {
        boolean terrainWork() {
            return terrainSpan > SMALL_TERRAIN_SPAN || terrainStoneCost > 0;
        }
    }
    private record GradeCell(BlockPos floor, boolean foundation, int retainingStone) {}
    private record ScaffoldPiece(BlockPos pos, BlockState state) {}
    private record ScaffoldTower(BlockPos anchor, List<ScaffoldPiece> pieces, List<BlockPos> steps) {}

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
        String locked = lockedReason(data, type);
        if (locked != null) return invalidPlacement(locked);

        long pdx = (long) selectedCenter.getX() - player.blockPosition().getX();
        long pdz = (long) selectedCenter.getZ() - player.blockPosition().getZ();
        if (pdx * pdx + pdz * pdz > (long) MAX_PLAYER_PLACEMENT_DISTANCE * MAX_PLAYER_PLACEMENT_DISTANCE) {
            return invalidPlacement("건설 위치는 플레이어 24블록 안에서 지정해 주세요.");
        }

        long dx = (long) selectedCenter.getX() - data.centerPos().getX();
        long dz = (long) selectedCenter.getZ() - data.centerPos().getZ();
        if (dx * dx + dz * dz > (long) MAX_MAIN_SETTLEMENT_RADIUS * MAX_MAIN_SETTLEMENT_RADIUS) {
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
            return invalidPlacement("선택한 부지가 안전하지 않습니다. 높이 차 4블록 이하·최대 3블록 성토 범위의 물·기존 건축물이 없는 곳을 선택해 주세요.");
        }
        if (overlapsInfrastructure(data, site.origin(), type, rotation)) {
            return invalidPlacement("선택한 부지가 기존 건물·도로·전초기지 또는 공동 창고와 겹칩니다.");
        }
        String message = "배치 가능";
        if (site.terrainWork()) {
            message += " · 지형 공사 포함";
            if (site.terrainStoneCost() > 0) message += " · 옹벽/기초 추가 석재 " + site.terrainStoneCost();
        }
        return new PlacementCheck(true, site.origin(), message, site.terrainWork(), site.terrainStoneCost());
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
        if (data.construction().active()) {
            BuildingType active = BuildingType.fromId(data.construction().type());
            String name = active == null ? data.construction().type() : active.displayName();
            return new StartResult(false, "이미 " + name + " 건설이 진행 중입니다.");
        }
        if (data.roadConstruction().active() || data.outpostConstruction().active()) {
            return new StartResult(false, "현재 인프라 공사가 끝난 뒤 건물을 시작해 주세요.");
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
        Villager builder = ensureBuilder(level, data.centerPos());
        if (builder != null) builder.setInvulnerable(true);
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
            Villager strandedBuilder = findBuilder(level, data.centerPos());
            if (strandedBuilder != null) strandedBuilder.setInvulnerable(false);
            data.clearConstruction();
            return true;
        }

        Villager builder = ensureBuilder(level, data.centerPos());
        if (builder == null) return false;
        if (builder.isNoAi()) builder.setNoAi(false);
        builder.setInvulnerable(true);

        if (construction.grading()) return tickGrading(server, data, type, builder);

        List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(type, construction.origin(), construction.rotation());
        BlockPos supply = supplyPosition(construction.origin(), type, construction.buildingRotation());
        Container crate = ensureSupplyCrate(level, supply);
        if (crate == null) return false;
        ensureConstructionScaffolds(level, data, type, supply);
        construction = data.construction();
        int buildStep = construction.buildStep();

        if (buildStep >= plan.size()) return finishIfValid(server, data, type, plan, builder, crate, supply);
        if (!stageRemainingMaterials(server, data, type, plan.size(), builder, crate, supply)) return false;
        if (server.getTickCount() % BUILD_INTERVAL_TICKS != 0) return false;

        construction = data.construction();
        buildStep = construction.buildStep();
        if (buildStep >= plan.size()) return finishIfValid(server, data, type, plan, builder, crate, supply);
        BuildingBlueprints.Placement placement = plan.get(buildStep);
        if (!moveBuilderToWorkPosition(level, construction, type, placement, builder, supply)) return false;

        BlockPos target = placement.pos();
        BlockState current = level.getBlockState(target);
        if (!current.isAir() && !current.is(placement.state().getBlock())) {
            builder.getNavigation().stop();
            return false;
        }

        long woodDelta = costAtStep(type.woodCost(), buildStep + 1, plan.size())
                - costAtStep(type.woodCost(), buildStep, plan.size());
        long stoneDelta = costAtStep(type.stoneCost(), buildStep + 1, plan.size())
                - costAtStep(type.stoneCost(), buildStep, plan.size());
        if (!SettlementInventory.consume(crate, woodDelta, stoneDelta, 0L)) return false;

        if (current.isAir()) {
            level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE);
            builder.swing(InteractionHand.MAIN_HAND);
        }
        data.advanceConstruction();
        if (data.construction().buildStep() >= plan.size()) return finishIfValid(server, data, type, plan, builder, crate, supply);
        return false;
    }

    private static boolean tickGrading(MinecraftServer server, SettlementData data,
                                       BuildingType type, Villager builder) {
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

        if (cell.retainingStone() > 0) {
            BlockPos supply = supplyPosition(construction.origin(), type, construction.buildingRotation());
            Container crate = ensureSupplyCrate(level, supply);
            if (crate == null) return false;
            if (!stageTerrainStone(server, data, builder, crate, supply, cell.retainingStone())) return false;
        }
        if (server.getTickCount() % GRADE_INTERVAL_TICKS != 0) return false;

        BlockPos work = gradeWorkPosition(level, cell.floor());
        if (builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D) > 4.0D) {
            builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 0.82D);
            return false;
        }

        if (cell.retainingStone() > 0) {
            BlockPos supply = supplyPosition(construction.origin(), type, construction.buildingRotation());
            Container crate = ensureSupplyCrate(level, supply);
            if (crate == null || !SettlementInventory.consume(crate, 0L, cell.retainingStone(), 0L)) return false;
            SettlementService.refreshResources(server, data);
            SettlementService.broadcast(server, data);
        }
        applyGradeCell(level, construction, type, cell);
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceConstruction();
        if (data.construction().gradeStep() >= plan.size()) {
            data.replaceConstructionStep(ConstructionState.BUILD_STEP_OFFSET);
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
            if (state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES)) continue;
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
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, floor.getX(), floor.getZ());
        return new BlockPos(floor.getX(), y, floor.getZ());
    }

    private static void applyGradeCell(ServerLevel level, ConstructionState construction,
                                       BuildingType type, GradeCell cell) {
        BlockPos column = cell.floor().above();
        for (int y = type.clearHeight(); y >= 0; y--) {
            BlockPos pos = column.above(y);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
        }
        if (!cell.foundation()) return;

        BlockState fill = cell.retainingStone() > 0
                ? Blocks.COBBLESTONE.defaultBlockState()
                : Blocks.COARSE_DIRT.defaultBlockState();
        level.setBlock(cell.floor(), fill, DIRECT_BLOCK_UPDATE);
        for (int depth = 1; depth <= MAX_GRADE_FILL_DEPTH; depth++) {
            BlockPos support = cell.floor().below(depth);
            BlockState state = level.getBlockState(support);
            if (!state.isAir() && !state.canBeReplaced()) break;
            level.setBlock(support, fill, DIRECT_BLOCK_UPDATE);
        }
    }

    private static boolean stageTerrainStone(MinecraftServer server, SettlementData data, Villager builder,
                                             Container crate, BlockPos supply, int requiredStone) {
        long missing = Math.max(0L, requiredStone - SettlementInventory.countStone(crate));
        if (missing <= 0L) return true;
        ItemStack carried = builder.getMainHandItem();
        if (!carried.isEmpty()) {
            if (builder.distanceToSqr(supply.getX() + 0.5D, supply.getY() + 0.5D, supply.getZ() + 0.5D)
                    > SUPPLY_INTERACTION_RANGE_SQR) {
                builder.getNavigation().moveTo(supply.getX() + 0.5D, supply.getY(), supply.getZ() + 0.5D, 0.9D);
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

        ServerLevel level = server.overworld();
        BlockPos source = SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isStone);
        if (source == null) return false;
        if (builder.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > SUPPLY_INTERACTION_RANGE_SQR) {
            builder.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, 0.9D);
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
                                                   int totalSteps, Villager builder, Container crate, BlockPos supply) {
        int step = data.construction().buildStep();
        long spentWood = costAtStep(type.woodCost(), step, totalSteps);
        long spentStone = costAtStep(type.stoneCost(), step, totalSteps);
        long remainingWood = Math.max(0L, type.woodCost() - spentWood);
        long remainingStone = Math.max(0L, type.stoneCost() - spentStone);
        long targetWood = Math.min(MAX_SITE_RESERVE_PER_CATEGORY, remainingWood);
        long targetStone = Math.min(MAX_SITE_RESERVE_PER_CATEGORY, remainingStone);
        long missingWood = Math.max(0L, targetWood - SettlementInventory.countWood(crate));
        long missingStone = Math.max(0L, targetStone - SettlementInventory.countStone(crate));

        ItemStack carried = builder.getMainHandItem();
        if (!carried.isEmpty()) {
            if (builder.distanceToSqr(supply.getX() + 0.5D, supply.getY() + 0.5D, supply.getZ() + 0.5D)
                    > SUPPLY_INTERACTION_RANGE_SQR) {
                builder.getNavigation().moveTo(supply.getX() + 0.5D, supply.getY(), supply.getZ() + 0.5D, 0.9D);
                return false;
            }
            int before = carried.getCount();
            ItemStack remaining = SettlementInventory.insert(crate, carried);
            if (remaining.getCount() == before
                    && relieveCratePressure(server.overworld(), data, crate, targetWood, targetStone)) {
                remaining = SettlementInventory.insert(crate, carried);
            }
            builder.setItemSlot(EquipmentSlot.MAINHAND, remaining);
            if (remaining.getCount() < before) {
                SettlementService.refreshResources(server, data);
                SettlementService.broadcast(server, data);
            }
            return false;
        }

        if (missingWood <= 0L && missingStone <= 0L) return true;
        Predicate<ItemStack> wanted = missingWood > 0L ? SettlementInventory::isWood : SettlementInventory::isStone;
        long missing = missingWood > 0L ? missingWood : missingStone;
        ServerLevel level = server.overworld();
        BlockPos source = SettlementStorageService.findExtractionTarget(level, data, wanted);
        if (source == null) return false;
        if (builder.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > SUPPLY_INTERACTION_RANGE_SQR) {
            builder.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, 0.9D);
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

    private static boolean relieveCratePressure(ServerLevel level, SettlementData data, Container crate,
                                               long targetWood, long targetStone) {
        long excessWood = Math.max(0L, SettlementInventory.countWood(crate) - targetWood);
        long excessStone = Math.max(0L, SettlementInventory.countStone(crate) - targetStone);
        boolean changed = false;
        for (int slot = crate.getContainerSize() - 1; slot >= 0; slot--) {
            ItemStack current = crate.getItem(slot);
            if (current.isEmpty()) continue;
            long returnLimit;
            if (SettlementInventory.isWood(current)) returnLimit = excessWood;
            else if (SettlementInventory.isStone(current)) returnLimit = excessStone;
            else returnLimit = current.getCount();
            if (returnLimit <= 0L) continue;

            int offered = (int) Math.min(returnLimit, current.getCount());
            ItemStack moving = current.copyWithCount(offered);
            ItemStack remaining = SettlementStorageService.insert(level, data, moving);
            int moved = offered - remaining.getCount();
            if (moved <= 0) continue;
            current.shrink(moved);
            crate.setItem(slot, current);
            if (SettlementInventory.isWood(moving)) excessWood -= moved;
            else if (SettlementInventory.isStone(moving)) excessStone -= moved;
            changed = true;
        }
        if (changed) crate.setChanged();
        return changed;
    }

    private static long costAtStep(long totalCost, int step, int totalSteps) {
        if (totalCost <= 0L || step <= 0 || totalSteps <= 0) return 0L;
        if (step >= totalSteps) return totalCost;
        return totalCost * step / totalSteps;
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

    private static boolean finishIfValid(MinecraftServer server, SettlementData data, BuildingType type,
                                         List<BuildingBlueprints.Placement> plan, Villager builder,
                                         Container crate, BlockPos supply) {
        ServerLevel level = server.overworld();
        for (BuildingBlueprints.Placement placement : plan) {
            BlockState current = level.getBlockState(placement.pos());
            if (current.is(placement.state().getBlock())) continue;
            if (!current.isAir()) {
                builder.getNavigation().stop();
                return false;
            }
            if (server.getTickCount() % BUILD_INTERVAL_TICKS != 0) return false;
            if (!moveBuilderToWorkPosition(level, data.construction(), type, placement, builder, supply)) return false;
            level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE);
            builder.swing(InteractionHand.MAIN_HAND);
            return false;
        }

        returnCrateExtras(level, data, crate);
        if (crateIsEmpty(crate)) level.setBlock(supply, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
        removeConstructionScaffolds(level, data.construction(), type, supply);
        data.completeConstruction(type);
        builder.getNavigation().stop();
        builder.setInvulnerable(false);
        builder.setCustomName(Component.literal(BUILDER_NAME));
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return true;
    }

    private static void returnCrateExtras(ServerLevel level, SettlementData data, Container crate) {
        for (int slot = 0; slot < crate.getContainerSize(); slot++) {
            ItemStack current = crate.getItem(slot);
            if (current.isEmpty()) continue;
            ItemStack remaining = SettlementStorageService.insert(level, data, current);
            crate.setItem(slot, remaining);
        }
        crate.setChanged();
    }

    private static boolean crateIsEmpty(Container crate) {
        for (int slot = 0; slot < crate.getContainerSize(); slot++) {
            if (!crate.getItem(slot).isEmpty()) return false;
        }
        return true;
    }

    private static Container ensureSupplyCrate(ServerLevel level, BlockPos supply) {
        if (!level.hasChunkAt(supply)) return null;
        if (level.getBlockEntity(supply) instanceof Container crate) return crate;
        BlockState current = level.getBlockState(supply);
        if (!current.isAir() && !current.canBeReplaced()) return null;
        level.setBlock(supply, Blocks.BARREL.defaultBlockState(), DIRECT_BLOCK_UPDATE);
        return level.getBlockEntity(supply) instanceof Container crate ? crate : null;
    }

    private static boolean moveBuilderToWorkPosition(ServerLevel level, ConstructionState construction, BuildingType type,
                                                     BuildingBlueprints.Placement placement, Villager builder, BlockPos supply) {
        BlockPos work = workPositionFor(level, construction, type, placement, supply);
        double workDistance = builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D);
        if (workDistance > WORK_POSITION_REACHED_SQR) {
            builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 0.85D);
            return false;
        }
        if (work.getY() <= construction.originY()) return true;
        return builder.distanceToSqr(placement.pos().getX() + 0.5D, placement.pos().getY() + 0.5D,
                placement.pos().getZ() + 0.5D) <= HIGH_WORK_RANGE_SQR;
    }

    private static BlockPos workPositionFor(ServerLevel level, ConstructionState construction, BuildingType type,
                                            BuildingBlueprints.Placement placement, BlockPos supply) {
        BlockPos target = placement.pos();
        int relativeY = target.getY() - construction.originY();
        BlockPos ground = new BlockPos(target.getX(), construction.originY(), target.getZ());
        if (relativeY <= 3) return ground;

        List<ScaffoldTower> towers = scaffoldTowers(construction.origin(), type, construction.buildingRotation(), supply);
        BlockPos bestWork = null;
        double bestTargetDistance = Double.MAX_VALUE;
        for (int towerIndex = 0; towerIndex < towers.size(); towerIndex++) {
            if (!construction.ownsScaffold(towerIndex)) continue;
            ScaffoldTower tower = towers.get(towerIndex);
            if (!towerUsable(level, tower) || tower.steps().isEmpty()) continue;
            int index = Math.min(tower.steps().size() - 1, Math.max(0, relativeY - 3));
            BlockPos candidate = tower.steps().get(index).above();
            double dx = (double) candidate.getX() + 0.5D - ((double) target.getX() + 0.5D);
            double dy = (double) candidate.getY() - ((double) target.getY() + 0.5D);
            double dz = (double) candidate.getZ() + 0.5D - ((double) target.getZ() + 0.5D);
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance <= HIGH_WORK_RANGE_SQR && distance < bestTargetDistance) {
                bestTargetDistance = distance;
                bestWork = candidate;
            }
        }
        return bestWork == null ? ground : bestWork;
    }

    private static void ensureConstructionScaffolds(ServerLevel level, SettlementData data,
                                                    BuildingType type, BlockPos supply) {
        ConstructionState construction = data.construction();
        List<ScaffoldTower> towers = scaffoldTowers(construction.origin(), type, construction.buildingRotation(), supply);
        int mask = construction.scaffoldMask();
        for (int towerIndex = 0; towerIndex < towers.size(); towerIndex++) {
            ScaffoldTower tower = towers.get(towerIndex);
            int bit = 1 << towerIndex;
            if ((mask & bit) != 0) {
                repairClaimedTower(level, tower);
                continue;
            }
            if (!canClaimFreshTower(level, tower)) continue;
            mask |= bit;
            data.setConstructionScaffoldMask(mask);
            placeClaimedTower(level, tower);
        }
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
        List<BlockPos> steps = new ArrayList<>();
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
            steps.add(treadPos);
            step++;
        }
        return new ScaffoldTower(center, List.copyOf(pieces), List.copyOf(steps));
    }

    private static boolean canClaimFreshTower(ServerLevel level, ScaffoldTower tower) {
        for (ScaffoldPiece piece : tower.pieces()) {
            if (!level.hasChunkAt(piece.pos())) return false;
            BlockState current = level.getBlockState(piece.pos());
            if (level.getBlockEntity(piece.pos()) != null || !current.getFluidState().isEmpty()) return false;
            if (!current.isAir() && !current.canBeReplaced()) return false;
        }
        return true;
    }

    private static void placeClaimedTower(ServerLevel level, ScaffoldTower tower) {
        for (ScaffoldPiece piece : tower.pieces()) {
            if (!level.hasChunkAt(piece.pos())) continue;
            BlockState current = level.getBlockState(piece.pos());
            if (current.isAir() || current.canBeReplaced()) {
                level.setBlock(piece.pos(), piece.state(), DIRECT_BLOCK_UPDATE);
            }
        }
    }

    private static void repairClaimedTower(ServerLevel level, ScaffoldTower tower) {
        for (ScaffoldPiece piece : tower.pieces()) {
            if (!level.hasChunkAt(piece.pos())) continue;
            BlockState current = level.getBlockState(piece.pos());
            if (current.is(piece.state().getBlock())) continue;
            if (level.getBlockEntity(piece.pos()) != null || !current.getFluidState().isEmpty()) continue;
            if (current.isAir() || current.canBeReplaced()) {
                level.setBlock(piece.pos(), piece.state(), DIRECT_BLOCK_UPDATE);
            }
        }
    }

    private static boolean towerUsable(ServerLevel level, ScaffoldTower tower) {
        for (ScaffoldPiece piece : tower.pieces()) {
            if (!level.hasChunkAt(piece.pos()) || !level.getBlockState(piece.pos()).is(piece.state().getBlock())) return false;
        }
        return true;
    }

    private static void removeConstructionScaffolds(ServerLevel level, ConstructionState construction,
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

    public static Villager ensureBuilder(ServerLevel level, BlockPos center) {
        Villager existing = findBuilder(level, center);
        if (existing != null) {
            if (existing.isNoAi()) existing.setNoAi(false);
            if (!existing.entityTags().contains(BUILDER_TAG)) existing.addTag(BUILDER_TAG);
            return existing;
        }
        Villager builder = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = center.offset(1, 0, 1);
        builder.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        builder.setYRot(0.0F);
        builder.setXRot(0.0F);
        builder.setCustomName(Component.literal(BUILDER_NAME));
        builder.setCustomNameVisible(true);
        builder.setPersistenceRequired();
        builder.setNoAi(false);
        builder.addTag(BUILDER_TAG);
        level.addFreshEntity(builder);
        return builder;
    }

    private static Villager findBuilder(ServerLevel level, BlockPos center) {
        AABB search = new AABB(
                center.getX() - 96.0D, center.getY() - 48.0D, center.getZ() - 96.0D,
                center.getX() + 97.0D, center.getY() + 49.0D, center.getZ() + 97.0D);
        List<Villager> tagged = level.getEntitiesOfClass(Villager.class, search,
                villager -> villager.entityTags().contains(BUILDER_TAG));
        if (!tagged.isEmpty()) return tagged.getFirst();
        List<Villager> legacy = level.getEntitiesOfClass(Villager.class, search,
                villager -> villager.getCustomName() != null && BUILDER_NAME.equals(villager.getCustomName().getString()));
        return legacy.isEmpty() ? null : legacy.getFirst();
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
                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);
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
                    if (level.getBlockEntity(pos) != null) return null;
                    BlockState state = level.getBlockState(pos);
                    if (!state.getFluidState().isEmpty()) return null;
                    if (y >= 0 && !isSafeAboveGround(state, y)) return null;
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
        BlockState current = level.getBlockState(supply);
        BlockState above = level.getBlockState(supply.above());
        BlockState below = level.getBlockState(supply.below());
        if (level.getBlockEntity(supply) != null || level.getBlockEntity(supply.above()) != null) return false;
        if (!current.getFluidState().isEmpty() || !above.getFluidState().isEmpty() || !below.getFluidState().isEmpty()) return false;
        if ((!current.isAir() && !current.canBeReplaced()) || (!above.isAir() && !above.canBeReplaced())) return false;
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

    private static boolean isSafeAboveGround(BlockState state, int relativeY) {
        if (state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES)) return true;
        return relativeY <= MAX_TERRAIN_CUT_HEIGHT && isNaturalGround(state);
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
