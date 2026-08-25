package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SettlementWorkerService {
    public static final String RESOURCE_WORKER_TAG = "frontier_settlement_resource_worker";
    private static final String LUMBER_WORKER_NAME = "벌목 주민";
    private static final String FARM_WORKER_NAME = "농사 주민";
    private static final String QUARRY_WORKER_NAME = "채석 주민";
    private static final String MINE_WORKER_NAME = "광산 주민";
    private static final long ARRIVAL_FOOD_COST = 4L;
    private static final int TREE_SEARCH_RADIUS = 18;
    private static final int MAX_LOGS_PER_TRIP = 4;
    private static final int MAX_CROPS_PER_TRIP = 4;
    private static final int MAX_STONE_PER_TRIP = 3;
    private static final int LUMBER_WORK_PERIOD_TICKS = 100;
    private static final int FARM_WORK_PERIOD_TICKS = 120;
    private static final int QUARRY_WORK_PERIOD_TICKS = 80;
    private static final int MINING_WORK_PERIOD_TICKS = 160;

    private SettlementWorkerService() {}

    public static void tick(MinecraftServer server, SettlementData data) {
        ServerLevel level = server.overworld();
        if (server.getTickCount() % 10 == 0) {
            SettlementOutpostLogisticsService.migrateLegacyWorkers(level, data);
        }
        if (server.getTickCount() % 600 == 0) tryAttractWorker(server, level, data);
        if (server.getTickCount() % 10 != 0) return;

        runBuildingWorkers(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME, SettlementWorkerService::workLumber);
        runBuildingWorkers(level, data, BuildingType.FARM, FARM_WORKER_NAME, SettlementWorkerService::workFarm);
        runBuildingWorkers(level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME, SettlementWorkerService::workQuarry);
        runBuildingWorkers(level, data, BuildingType.MINE, MINE_WORKER_NAME, SettlementWorkerService::workMine);
        SettlementOutpostLogisticsService.tick(level, data);
    }

    @FunctionalInterface
    private interface BuildingWork {
        void run(ServerLevel level, SettlementData data, Villager worker, BuildingRecord building);
    }

    private static void runBuildingWorkers(ServerLevel level, SettlementData data, BuildingType type,
                                           String workerName, BuildingWork work) {
        List<BuildingRecord> buildings = buildings(data, type);
        List<Villager> workers = workersByName(level, data.centerPos(), workerName);
        int count = Math.min(buildings.size(), workers.size());
        for (int i = 0; i < count; i++) {
            BuildingRecord building = buildings.get(i);
            if (!level.hasChunkAt(building.workCenter())) continue;
            work.run(level, data, workers.get(i), building);
        }
    }

    private static void tryAttractWorker(MinecraftServer server, ServerLevel level, SettlementData data) {
        List<Villager> lumber = workersByName(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME);
        List<Villager> farm = workersByName(level, data, BuildingType.FARM, FARM_WORKER_NAME);
        List<Villager> quarry = workersByName(level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME);
        List<Villager> mine = workersByName(level, data, BuildingType.MINE, MINE_WORKER_NAME);
        boolean localEvidenceLoaded = localProductionEvidenceLoaded(level, data);

        // Population is repaired downward/upward only when every civilian evidence corridor is visible.
        // An unloaded resident is not a dead resident, and must never free a housing slot or trigger a duplicate.
        if (localEvidenceLoaded
                && SettlementOutpostLogisticsService.allRoutesLoaded(level, data)
                && SettlementWorkshopService.allAssignmentsLoaded(level, data)
                && SettlementAdvancedWorkshopService.allAssignmentsLoaded(level, data)) {
            int transport = SettlementOutpostLogisticsService.loadedAssignedWorkerCount(level, data);
            int workshop = SettlementWorkshopService.loadedAssignedWorkerCount(level, data);
            int advanced = SettlementAdvancedWorkshopService.loadedAssignedWorkerCount(level, data);
            int actualPopulation = 1 + lumber.size() + farm.size() + quarry.size() + mine.size()
                    + transport + workshop + advanced;
            if (data.population() != actualPopulation) data.setPopulation(actualPopulation);
        }
        if (data.population() >= data.housingCapacity()) return;

        // Ordinary production workers have no per-worker manual assignment UI. Do not infer a vacancy
        // from a partial entity view; only recruit when their work<->storage envelope is fully loaded.
        if (localEvidenceLoaded) {
            if (tryFillJob(server, level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME, lumber.size())) return;
            if (tryFillJob(server, level, data, BuildingType.FARM, FARM_WORKER_NAME, farm.size())) return;
            if (tryFillJob(server, level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME, quarry.size())) return;
            if (tryFillJob(server, level, data, BuildingType.MINE, MINE_WORKER_NAME, mine.size())) return;
        }

        BuildingRecord missingWorkshop = SettlementWorkshopService.firstMissingLoadedAssignment(level, data);
        if (missingWorkshop != null) {
            if (!arrivalFoodAvailable(level, data)) return;
            Villager arrival = SettlementWorkshopService.spawnAssignedWorker(level, data, missingWorkshop);
            commitArrival(server, level, data, arrival);
            return;
        }

        BuildingRecord missingAdvanced = SettlementAdvancedWorkshopService.firstMissingLoadedAssignment(level, data);
        if (missingAdvanced != null) {
            if (!arrivalFoodAvailable(level, data)) return;
            Villager arrival = SettlementAdvancedWorkshopService.spawnAssignedWorker(level, data, missingAdvanced);
            commitArrival(server, level, data, arrival);
            return;
        }

        OutpostRecord missing = SettlementOutpostLogisticsService.firstMissingLoadedAssignment(level, data);
        if (missing != null) {
            if (!arrivalFoodAvailable(level, data)) return;
            Villager arrival = SettlementOutpostLogisticsService.spawnAssignedWorker(level, data, missing);
            commitArrival(server, level, data, arrival);
        }
    }

    private static boolean localProductionEvidenceLoaded(ServerLevel level, SettlementData data) {
        if (!SettlementStorageService.storageAvailable(level, data)) return false;
        for (BuildingRecord building : data.buildings()) {
            BuildingType type = building.buildingType();
            if (type != BuildingType.LUMBER_CAMP && type != BuildingType.FARM
                    && type != BuildingType.QUARRY && type != BuildingType.MINE) continue;
            if (!workerRouteEvidenceLoaded(level, data, building.workCenter(), 24)) return false;
        }
        return true;
    }

    /**
     * Loaded-only visibility proof for one local civilian lifecycle envelope.
     *
     * Alpha.68 deliberately includes every real place the town routine can send the worker:
     * work target, concrete settlement storage, and every completed HOUSE rest footprint. The exact
     * same AABB is also used by assignment/entity lookup, so a worker sleeping in an unloaded house
     * cannot become an "unloaded == dead" false negative. Only hasChunkAt is used; no chunk is loaded.
     */
    static boolean workerRouteEvidenceLoaded(ServerLevel level, SettlementData data,
                                             BlockPos workCenter, int margin) {
        if (!SettlementStorageService.storageAvailable(level, data)) return false;
        return workerBoundsFullyLoaded(level, data, workerRouteBounds(data, workCenter, margin));
    }

    static AABB workerRouteBounds(SettlementData data, BlockPos workCenter, int margin) {
        int minX = workCenter.getX() - margin;
        int maxX = workCenter.getX() + margin;
        int minZ = workCenter.getZ() - margin;
        int maxZ = workCenter.getZ() + margin;
        for (BlockPos storage : SettlementStorageService.storagePositions(data)) {
            minX = Math.min(minX, storage.getX() - margin);
            maxX = Math.max(maxX, storage.getX() + margin);
            minZ = Math.min(minZ, storage.getZ() - margin);
            maxZ = Math.max(maxZ, storage.getZ() + margin);
        }
        for (BuildingRecord building : data.buildings()) {
            if (building.buildingType() != BuildingType.HOUSE) continue;
            minX = Math.min(minX, building.originX() - margin);
            maxX = Math.max(maxX, building.originX() + building.rotatedWidth() - 1 + margin);
            minZ = Math.min(minZ, building.originZ() - margin);
            maxZ = Math.max(maxZ, building.originZ() + building.rotatedDepth() - 1 + margin);
        }
        double minY = data.centerPos().getY() - 96.0D;
        double maxY = data.centerPos().getY() + 97.0D;
        return new AABB(minX, minY, minZ, maxX + 1.0D, maxY, maxZ + 1.0D);
    }

    private static boolean workerBoundsFullyLoaded(ServerLevel level, SettlementData data, AABB bounds) {
        int minX = (int) Math.floor(bounds.minX);
        int minChunkX = Math.floorDiv(minX, 16);
        int maxX = (int) Math.floor(Math.nextDown(bounds.maxX));
        int maxChunkX = Math.floorDiv(maxX, 16);
        int minZ = (int) Math.floor(bounds.minZ);
        int minChunkZ = Math.floorDiv(minZ, 16);
        int maxZ = (int) Math.floor(Math.nextDown(bounds.maxZ));
        int maxChunkZ = Math.floorDiv(maxZ, 16);
        int probeY = data.centerPos().getY();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                BlockPos probe = new BlockPos(chunkX * 16 + 8, probeY, chunkZ * 16 + 8);
                if (!level.hasChunkAt(probe)) return false;
            }
        }
        return true;
    }

    private static boolean tryFillJob(MinecraftServer server, ServerLevel level, SettlementData data,
                                      BuildingType type, String workerName, int existingWorkers) {
        List<BuildingRecord> available = buildings(data, type);
        if (existingWorkers >= available.size()) return false;
        BuildingRecord target = available.get(existingWorkers);
        if (!level.hasChunkAt(target.workCenter())) return true;
        if (!arrivalFoodAvailable(level, data)) return true;
        Villager arrival = spawnWorker(level, target.workCenter(), workerName);
        commitArrival(server, level, data, arrival);
        return true;
    }

    private static boolean arrivalFoodAvailable(ServerLevel level, SettlementData data) {
        if (!SettlementStorageService.storageAvailable(level, data)) return false;
        return SettlementStorageService.scan(level, data).food() >= ARRIVAL_FOOD_COST;
    }

    private static boolean consumeArrivalFood(ServerLevel level, SettlementData data) {
        return SettlementStorageService.consume(level, data, 0L, 0L, ARRIVAL_FOOD_COST);
    }

    private static boolean commitArrival(MinecraftServer server, ServerLevel level,
                                         SettlementData data, Villager arrival) {
        if (arrival == null) return false;
        if (!consumeArrivalFood(level, data)) {
            arrival.discard();
            return false;
        }
        finishArrival(server, data);
        return true;
    }

    private static void finishArrival(MinecraftServer server, SettlementData data) {
        data.addPopulation(1);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
    }

    private static Villager spawnWorker(ServerLevel level, BlockPos spawn, String name) {
        if (!level.hasChunkAt(spawn)) return null;
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(name));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.addTag(RESOURCE_WORKER_TAG);
        if (!level.addFreshEntity(worker)) return null;
        return worker;
    }

    /**
     * Frontier-managed local civilians can carry the only physical copy of harvested/staged cargo.
     * Remove vanilla equipment-drop randomness and expose that exact MAINHAND stack once.
     * The road transporter keeps its separate Alpha.63 handler and is explicitly excluded here.
     */
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Villager worker)) return;
        if (worker.entityTags().contains(SettlementOutpostLogisticsService.TRANSPORT_WORKER_TAG)) return;
        if (!isManagedCargoWorker(worker)) return;
        ItemStack carried = worker.getMainHandItem();
        event.getDrops().clear();
        if (carried.isEmpty()) return;
        event.getDrops().add(new ItemEntity(
                worker.level(), worker.getX(), worker.getY(), worker.getZ(), carried.copy()));
    }

    private static boolean isManagedCargoWorker(Villager worker) {
        if (worker.entityTags().contains(RESOURCE_WORKER_TAG)
                || worker.entityTags().contains(SettlementWorkshopService.WORKSHOP_WORKER_TAG)
                || worker.entityTags().contains(SettlementAdvancedWorkshopService.ADVANCED_WORKER_TAG)) {
            return true;
        }
        // Save-compatible fallback for pre-Alpha.65 ordinary workers that did not yet carry a role tag.
        Component name = worker.getCustomName();
        if (name == null) return false;
        String value = name.getString();
        return LUMBER_WORKER_NAME.equals(value) || FARM_WORKER_NAME.equals(value)
                || QUARRY_WORKER_NAME.equals(value) || MINE_WORKER_NAME.equals(value);
    }

    private static void workLumber(ServerLevel level, SettlementData data,
                                   Villager worker, BuildingRecord camp) {
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty()) { deliverToTownStorage(level, data, worker, carried); return; }
        BlockPos target = findTree(level, data, camp.workCenter());
        if (target == null) { move(worker, camp.workCenter(), 0.7D); return; }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 8.0D) {
            move(worker, target, 0.8D); return;
        }
        if (!workDue(level, camp, LUMBER_WORK_PERIOD_TICKS)) return;
        ItemStack harvested = harvestVerticalTrunk(level, data, target);
        if (!harvested.isEmpty()) {
            worker.swing(InteractionHand.MAIN_HAND);
            worker.setItemSlot(EquipmentSlot.MAINHAND, harvested);
        }
    }

    private static void workFarm(ServerLevel level, SettlementData data,
                                 Villager worker, BuildingRecord farm) {
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty()) { deliverToTownStorage(level, data, worker, carried); return; }
        if (worker.distanceToSqr(farm.workCenter().getX() + 0.5D, farm.workCenter().getY(), farm.workCenter().getZ() + 0.5D) > 64.0D) {
            move(worker, farm.workCenter(), 0.75D); return;
        }
        if (!workDue(level, farm, FARM_WORK_PERIOD_TICKS)) return;
        int harvested = 0;
        BuildingType type = farm.buildingType();
        if (type == null) return;
        for (int x = 0; x < type.width() && harvested < MAX_CROPS_PER_TRIP; x++) {
            for (int z = 0; z < type.depth() && harvested < MAX_CROPS_PER_TRIP; z++) {
                BlockPos crop = farm.origin().offset(x, 1, z);
                if (!level.hasChunkAt(crop)) continue;
                BlockState state = level.getBlockState(crop);
                if (!state.is(Blocks.WHEAT) || !state.hasProperty(BlockStateProperties.AGE_7)
                        || state.getValue(BlockStateProperties.AGE_7) < 7) continue;
                level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3);
                harvested++;
            }
        }
        if (harvested > 0) {
            worker.swing(InteractionHand.MAIN_HAND);
            worker.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WHEAT, harvested));
        }
    }

    private static void workQuarry(ServerLevel level, SettlementData data,
                                   Villager worker, BuildingRecord quarry) {
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty()) { deliverToTownStorage(level, data, worker, carried); return; }
        BlockPos target = findExposedStone(level, data, quarry.workCenter(), 14);
        if (target == null) { move(worker, quarry.workCenter(), 0.7D); return; }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 9.0D) {
            move(worker, target, 0.78D); return;
        }
        if (!workDue(level, quarry, QUARRY_WORK_PERIOD_TICKS)) return;
        ItemStack stone = harvestStoneCluster(level, data, target);
        if (!stone.isEmpty()) {
            worker.swing(InteractionHand.MAIN_HAND);
            worker.setItemSlot(EquipmentSlot.MAINHAND, stone);
        }
    }

    private static void workMine(ServerLevel level, SettlementData data,
                                 Villager worker, BuildingRecord mine) {
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty()) { deliverToTownStorage(level, data, worker, carried); return; }
        BlockPos work = mine.workCenter();
        if (worker.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D) > 16.0D) {
            move(worker, work, 0.75D); return;
        }
        if (!workDue(level, mine, MINING_WORK_PERIOD_TICKS)) return;
        BlockPos ore = findOreBelow(level, data, work);
        if (ore == null) return;
        ItemStack mined = mineOre(level, ore);
        if (!mined.isEmpty()) {
            worker.swing(InteractionHand.MAIN_HAND);
            worker.setItemSlot(EquipmentSlot.MAINHAND, mined);
        }
    }

    private static boolean workDue(ServerLevel level, BuildingRecord building, int periodTicks) {
        long periodSlots = Math.max(1L, periodTicks / 10L);
        long currentSlot = level.getGameTime() / 10L;
        long salt = (long) building.originX() * 31L + (long) building.originY() * 7L + (long) building.originZ() * 17L;
        return Math.floorMod(currentSlot + salt, periodSlots) == 0L;
    }

    private static void deliverToTownStorage(ServerLevel level, SettlementData data,
                                             Villager worker, ItemStack carried) {
        BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);
        if (!level.hasChunkAt(target)) {
            worker.getNavigation().stop();
            return;
        }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D) > 9.0D) {
            move(worker, target, 0.85D);
            return;
        }
        ItemStack remaining = SettlementStorageService.insert(level, data, carried);
        worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);
    }

    private static void move(Villager worker, BlockPos target, double speed) {
        worker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speed);
    }

    private static BlockPos findTree(ServerLevel level, SettlementData data, BlockPos center) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -TREE_SEARCH_RADIUS; dx <= TREE_SEARCH_RADIUS; dx++) {
            for (int dz = -TREE_SEARCH_RADIUS; dz <= TREE_SEARCH_RADIUS; dz++) {
                if (dx * dx + dz * dz > TREE_SEARCH_RADIUS * TREE_SEARCH_RADIUS) continue;
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                for (int y = center.getY() - 4; y <= center.getY() + 10; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.hasChunkAt(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (!state.is(BlockTags.LOGS) || isProtected(data, pos) || !hasLeavesAbove(level, pos)) continue;
                    double distance = pos.distSqr(center);
                    if (distance < bestDistance) { best = pos; bestDistance = distance; }
                    break;
                }
            }
        }
        return best;
    }

    private static boolean hasLeavesAbove(ServerLevel level, BlockPos trunk) {
        for (int y = 1; y <= 8; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = trunk.offset(dx, y, dz);
                    if (level.hasChunkAt(pos) && level.getBlockState(pos).is(BlockTags.LEAVES)) return true;
                }
            }
        }
        return false;
    }

    private static ItemStack harvestVerticalTrunk(ServerLevel level, SettlementData data, BlockPos base) {
        if (!level.hasChunkAt(base)) return ItemStack.EMPTY;
        BlockState first = level.getBlockState(base);
        if (!first.is(BlockTags.LOGS)) return ItemStack.EMPTY;
        Item item = first.getBlock().asItem();
        if (item == Items.AIR) return ItemStack.EMPTY;
        int count = 0;
        for (int y = 0; y < 10 && count < MAX_LOGS_PER_TRIP; y++) {
            BlockPos pos = base.above(y);
            if (!level.hasChunkAt(pos)) break;
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LOGS) || state.getBlock().asItem() != item || isProtected(data, pos)) {
                if (count > 0) break;
                continue;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            count++;
        }
        return count == 0 ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static BlockPos findExposedStone(ServerLevel level, SettlementData data, BlockPos center, int radius) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int y = center.getY() - 4; y <= center.getY() + 3; y++) {
                    BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                    if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.above())) continue;
                    BlockState state = level.getBlockState(pos);
                    if (!isQuarryStone(state) || isProtected(data, pos) || !level.getBlockState(pos.above()).isAir()) continue;
                    double distance = pos.distSqr(center);
                    if (distance < bestDistance) { best = pos; bestDistance = distance; }
                }
            }
        }
        return best;
    }

    private static ItemStack harvestStoneCluster(ServerLevel level, SettlementData data, BlockPos base) {
        if (!level.hasChunkAt(base)) return ItemStack.EMPTY;
        BlockState first = level.getBlockState(base);
        if (!isQuarryStone(first)) return ItemStack.EMPTY;
        Item item = first.getBlock().asItem();
        if (item == Items.AIR) return ItemStack.EMPTY;
        int count = 0;
        for (int dx = -1; dx <= 1 && count < MAX_STONE_PER_TRIP; dx++) {
            for (int dz = -1; dz <= 1 && count < MAX_STONE_PER_TRIP; dz++) {
                BlockPos pos = base.offset(dx, 0, dz);
                if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.above())) continue;
                BlockState state = level.getBlockState(pos);
                if (state.getBlock().asItem() != item || !isQuarryStone(state) || isProtected(data, pos)
                        || !level.getBlockState(pos.above()).isAir()) continue;
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                count++;
            }
        }
        return count == 0 ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static boolean isQuarryStone(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE) || state.is(Blocks.TUFF);
    }

    private static BlockPos findOreBelow(ServerLevel level, SettlementData data, BlockPos center) {
        for (int depth = 2; depth <= 24; depth++) {
            int y = center.getY() - depth;
            for (int radius = 0; radius <= 12; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                        if (!level.hasChunkAt(pos)) continue;
                        if (level.getBlockState(pos).is(Tags.Blocks.ORES) && !isProtected(data, pos)) return pos;
                    }
                }
            }
        }
        return null;
    }

    private static ItemStack mineOre(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return ItemStack.EMPTY;
        BlockState state = level.getBlockState(pos);
        if (!state.is(Tags.Blocks.ORES)) return ItemStack.EMPTY;
        ItemStack result;
        if (state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)) result = new ItemStack(Items.RAW_IRON);
        else if (state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)) result = new ItemStack(Items.RAW_COPPER, 2);
        else if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE)) result = new ItemStack(Items.RAW_GOLD);
        else if (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE)) result = new ItemStack(Items.COAL);
        else if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) result = new ItemStack(Items.DIAMOND);
        else if (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)) result = new ItemStack(Items.EMERALD);
        else if (state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)) result = new ItemStack(Items.REDSTONE, 4);
        else if (state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE)) result = new ItemStack(Items.LAPIS_LAZULI, 4);
        else {
            Item item = state.getBlock().asItem();
            result = item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        }
        if (!result.isEmpty()) level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        return result;
    }

    private static boolean isProtected(SettlementData data, BlockPos pos) {
        if (pos.closerThan(data.stockpilePos(), 3.0D)) return true;
        for (BuildingRecord building : data.buildings()) if (building.protectsXZ(pos, 1)) return true;
        for (RoadSegment road : data.roads()) if (road.containsXZ(pos)) return true;
        for (OutpostRecord outpost : data.outposts()) if (outpost.protectsXZ(pos, 1)) return true;
        return false;
    }

    private static List<BuildingRecord> buildings(SettlementData data, BuildingType type) {
        List<BuildingRecord> result = new ArrayList<>();
        for (BuildingRecord building : data.buildings()) if (type.id().equals(building.type())) result.add(building);
        return result;
    }

    private static List<Villager> workersByName(ServerLevel level, SettlementData data,
                                                BuildingType type, String name) {
        List<Villager> workers = new ArrayList<>();
        Set<java.util.UUID> ids = new HashSet<>();
        for (BuildingRecord building : buildings(data, type)) {
            AABB search = workerRouteBounds(data, building.workCenter(), 24);
            for (Villager villager : level.getEntitiesOfClass(Villager.class, search,
                    candidate -> candidate.getCustomName() != null
                            && name.equals(candidate.getCustomName().getString()))) {
                if (ids.add(villager.getUUID())) workers.add(villager);
            }
        }
        workers.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return workers;
    }
}
