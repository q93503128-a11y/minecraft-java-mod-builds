package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
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
    private static final double BUILDER_WORK_RANGE_SQR = 22.0D;
    private static final double SUPPLY_INTERACTION_RANGE_SQR = 9.0D;
    private static final int HAUL_BATCH_SIZE = 16;
    private static final long MAX_SITE_RESERVE_PER_CATEGORY = 12L;
    private static final int BUILD_INTERVAL_TICKS = 10;
    private static final int COMMAND_PLACEMENT_DISTANCE = 10;
    private static final int MAX_MAIN_SETTLEMENT_RADIUS = 72;
    private static final int MAX_PLAYER_PLACEMENT_DISTANCE = 24;

    private SettlementConstructionService() {}

    public record StartResult(boolean started, String message) {}
    public record PlacementCheck(boolean valid, BlockPos origin, String message) {}
    private record Site(BlockPos origin) {}

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
        return null;
    }

    public static PlacementCheck checkPlacement(ServerPlayer player, BuildingType type, BlockPos selectedCenter) {
        return checkPlacement(player, type, selectedCenter, BuildingRotation.NONE.id());
    }

    public static PlacementCheck checkPlacement(ServerPlayer player, BuildingType type, BlockPos selectedCenter, int rotationId) {
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return new PlacementCheck(false, BlockPos.ZERO, "공동 마을이 없습니다.");
        if (player.level() != server.overworld()) return new PlacementCheck(false, BlockPos.ZERO, "오버월드에서만 배치할 수 있습니다.");
        String locked = lockedReason(data, type);
        if (locked != null) return new PlacementCheck(false, BlockPos.ZERO, locked);

        long pdx = (long) selectedCenter.getX() - player.blockPosition().getX();
        long pdz = (long) selectedCenter.getZ() - player.blockPosition().getZ();
        if (pdx * pdx + pdz * pdz > (long) MAX_PLAYER_PLACEMENT_DISTANCE * MAX_PLAYER_PLACEMENT_DISTANCE) {
            return new PlacementCheck(false, BlockPos.ZERO, "건설 위치는 플레이어 24블록 안에서 지정해 주세요.");
        }

        long dx = (long) selectedCenter.getX() - data.centerPos().getX();
        long dz = (long) selectedCenter.getZ() - data.centerPos().getZ();
        if (dx * dx + dz * dz > (long) MAX_MAIN_SETTLEMENT_RADIUS * MAX_MAIN_SETTLEMENT_RADIUS) {
            return new PlacementCheck(false, BlockPos.ZERO, "본진 기능 건물은 마을 중심 72블록 안에 배치해 주세요. 먼 지역은 전초기지를 사용합니다.");
        }

        BuildingRotation rotation = BuildingRotation.fromId(rotationId);
        int width = rotation.rotatedWidth(type);
        int depth = rotation.rotatedDepth(type);
        int originX = selectedCenter.getX() - width / 2;
        int originZ = selectedCenter.getZ() - depth / 2;
        ServerLevel level = server.overworld();
        Site site = assessSite(level, originX, originZ, type, rotation);
        if (site == null) {
            return new PlacementCheck(false, BlockPos.ZERO,
                    "선택한 부지가 안전하지 않습니다. 높이 차 2블록 이하의 물·나무·기존 건축물이 없는 곳을 선택해 주세요.");
        }
        if (overlapsInfrastructure(data, site.origin(), type, rotation)) {
            return new PlacementCheck(false, BlockPos.ZERO, "선택한 부지가 기존 건물·도로·전초기지 또는 공동 창고와 겹칩니다.");
        }
        return new PlacementCheck(true, site.origin(), "배치 가능");
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
        if (resources.wood() < type.woodCost() || resources.stone() < type.stoneCost()) {
            return new StartResult(false, type.displayName() + " 필요 자원: 목재 " + type.woodCost()
                    + ", 석재 " + type.stoneCost() + " | 현재 목재 " + resources.wood()
                    + ", 석재 " + resources.stone());
        }

        BuildingRotation rotation = BuildingRotation.fromId(rotationId);
        prepareSite(level, check.origin(), type, rotation);
        BlockPos supply = supplyPosition(check.origin(), type, rotation);
        if (!(level.getBlockEntity(supply) instanceof Container)) {
            return new StartResult(false, "건설 자재함을 만들 수 없어 착공을 취소했습니다. 자원은 차감되지 않았습니다.");
        }
        data.beginConstruction(type, check.origin(), rotation);
        ensureBuilder(level, data.centerPos());
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return new StartResult(true, type.displayName() + " 착공. 건설 주민이 공동 창고에서 실제 자재를 운반해 현장 자재함에 쌓은 뒤 공사를 진행합니다."
                + " (필요 목재 " + type.woodCost() + ", 석재 " + type.stoneCost() + ")");
    }

    public static boolean tick(MinecraftServer server, SettlementData data) {
        ConstructionState construction = data.construction();
        if (!construction.active()) return false;
        BuildingType type = BuildingType.fromId(construction.type());
        if (type == null) {
            data.clearConstruction();
            return true;
        }

        ServerLevel level = server.overworld();
        List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(type, construction.origin(), construction.rotation());
        Villager builder = ensureBuilder(level, data.centerPos());
        if (builder == null) return false;
        if (builder.isNoAi()) builder.setNoAi(false);

        BlockPos supply = supplyPosition(construction.origin(), type, construction.buildingRotation());
        Container crate = ensureSupplyCrate(level, supply);
        if (crate == null) return false;

        if (construction.step() >= plan.size()) return finishIfValid(server, data, type, plan, builder, crate, supply);
        if (!stageRemainingMaterials(server, data, type, plan.size(), builder, crate, supply)) return false;
        if (server.getTickCount() % BUILD_INTERVAL_TICKS != 0) return false;

        BuildingBlueprints.Placement placement = plan.get(data.construction().step());
        BlockPos target = placement.pos();
        BlockPos work = new BlockPos(target.getX(), construction.originY(), target.getZ());
        double distance = builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D);
        if (distance > BUILDER_WORK_RANGE_SQR) {
            builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 0.85D);
            return false;
        }

        BlockState current = level.getBlockState(target);
        if (!current.isAir() && !current.is(placement.state().getBlock())) {
            builder.getNavigation().stop();
            return false;
        }

        long woodDelta = costAtStep(type.woodCost(), data.construction().step() + 1, plan.size())
                - costAtStep(type.woodCost(), data.construction().step(), plan.size());
        long stoneDelta = costAtStep(type.stoneCost(), data.construction().step() + 1, plan.size())
                - costAtStep(type.stoneCost(), data.construction().step(), plan.size());
        if (!SettlementInventory.consume(crate, woodDelta, stoneDelta, 0L)) return false;

        if (current.isAir()) level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE);
        data.advanceConstruction();
        if (data.construction().step() >= plan.size()) return finishIfValid(server, data, type, plan, builder, crate, supply);
        return false;
    }

    private static boolean stageRemainingMaterials(MinecraftServer server, SettlementData data, BuildingType type,
                                                   int totalSteps, Villager builder, Container crate, BlockPos supply) {
        int step = data.construction().step();
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
            BlockPos work = new BlockPos(placement.pos().getX(), data.construction().originY(), placement.pos().getZ());
            if (builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D) > BUILDER_WORK_RANGE_SQR) {
                builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 0.85D);
                return false;
            }
            level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE);
            return false;
        }

        returnCrateExtras(level, data, crate);
        if (crateIsEmpty(crate)) level.setBlock(supply, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
        data.completeConstruction(type);
        builder.getNavigation().stop();
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
        if (level.getBlockEntity(supply) instanceof Container crate) return crate;
        BlockState current = level.getBlockState(supply);
        if (!current.isAir() && !current.canBeReplaced()) return null;
        level.setBlock(supply, Blocks.BARREL.defaultBlockState(), DIRECT_BLOCK_UPDATE);
        return level.getBlockEntity(supply) instanceof Container crate ? crate : null;
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

        if (pos.equals(supplyPosition(construction.origin(), type, rotation))) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            return;
        }

        int width = rotation.rotatedWidth(type);
        int depth = rotation.rotatedDepth(type);
        if (pos.getY() == construction.originY() - 1
                && pos.getX() >= construction.originX() && pos.getX() < construction.originX() + width
                && pos.getZ() >= construction.originZ() && pos.getZ() < construction.originZ() + depth) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            return;
        }

        BlockState current = level.getBlockState(pos);
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
        List<Villager> builders = level.getEntitiesOfClass(Villager.class, search,
                villager -> villager.getCustomName() != null && BUILDER_NAME.equals(villager.getCustomName().getString()));
        return builders.isEmpty() ? null : builders.getFirst();
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
        if (max - min > 2) return null;
        Collections.sort(heights);
        int baseY = heights.get(heights.size() / 2);
        BlockPos origin = new BlockPos(originX, baseY, originZ);

        for (int x = -1; x <= width; x++) {
            for (int z = -1; z <= depth; z++) {
                for (int y = -3; y <= type.clearHeight(); y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (level.getBlockEntity(pos) != null) return null;
                    BlockState state = level.getBlockState(pos);
                    if (!state.getFluidState().isEmpty()) return null;
                    if (y >= 0 && !isSafeAboveGround(state, y)) return null;
                    if (y == -1 && !state.isAir() && !isNaturalGround(state)) return null;
                }
            }
        }
        if (!isSafeSupplyPosition(level, supplyPosition(origin, type, rotation))) return null;
        return new Site(origin);
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
        return relativeY <= 1 && isNaturalGround(state);
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

    private static void prepareSite(ServerLevel level, BlockPos origin, BuildingType type, BuildingRotation rotation) {
        int width = rotation.rotatedWidth(type);
        int depth = rotation.rotatedDepth(type);
        for (int y = type.clearHeight(); y >= 0; y--) {
            for (int x = -1; x <= width; x++) {
                for (int z = -1; z <= depth; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
                }
            }
        }
        BlockPos supply = supplyPosition(origin, type, rotation);
        if (!level.getBlockState(supply).isAir()) level.setBlock(supply, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
        if (!level.getBlockState(supply.above()).isAir()) level.setBlock(supply.above(), Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                BlockPos top = origin.offset(x, -1, z);
                level.setBlock(top, Blocks.COBBLESTONE.defaultBlockState(), DIRECT_BLOCK_UPDATE);
                for (int down = 2; down <= 4; down++) {
                    BlockPos support = origin.offset(x, -down, z);
                    if (!level.getBlockState(support).isAir()) break;
                    level.setBlock(support, Blocks.COBBLESTONE.defaultBlockState(), DIRECT_BLOCK_UPDATE);
                }
            }
        }
        level.setBlock(supply, Blocks.BARREL.defaultBlockState(), DIRECT_BLOCK_UPDATE);
    }
}
