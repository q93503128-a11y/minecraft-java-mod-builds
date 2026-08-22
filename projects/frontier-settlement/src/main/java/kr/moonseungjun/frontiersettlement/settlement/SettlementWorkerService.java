package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SettlementWorkerService {
    private static final String LUMBER_WORKER_NAME = "벌목 주민";
    private static final String FARM_WORKER_NAME = "농사 주민";
    private static final String QUARRY_WORKER_NAME = "채석 주민";
    private static final String MINE_WORKER_NAME = "광산 주민";
    private static final String TRANSPORT_WORKER_NAME = "운송 주민";
    private static final long ARRIVAL_FOOD_COST = 4L;
    private static final int TREE_SEARCH_RADIUS = 18;
    private static final int MAX_LOGS_PER_TRIP = 6;
    private static final int MAX_CROPS_PER_TRIP = 8;
    private static final int MAX_STONE_PER_TRIP = 6;
    private static final int MAX_TRANSPORT_STACK = 16;

    private SettlementWorkerService() {}

    public static void tick(MinecraftServer server, SettlementData data) {
        ServerLevel level = server.overworld();
        if (server.getTickCount() % 600 == 0) tryAttractWorker(server, level, data);
        if (server.getTickCount() % 10 != 0) return;

        runBuildingWorkers(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME, SettlementWorkerService::workLumber);
        runBuildingWorkers(level, data, BuildingType.FARM, FARM_WORKER_NAME, SettlementWorkerService::workFarm);
        runBuildingWorkers(level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME, SettlementWorkerService::workQuarry);
        runBuildingWorkers(level, data, BuildingType.MINE, MINE_WORKER_NAME, SettlementWorkerService::workMine);

        List<Villager> transport = workersByName(level, data.centerPos(), TRANSPORT_WORKER_NAME);
        int transportCount = Math.min(data.outposts().size(), transport.size());
        for (int i = 0; i < transportCount; i++) workTransport(level, data, transport.get(i), data.outposts().get(i));
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
        for (int i = 0; i < count; i++) work.run(level, data, workers.get(i), buildings.get(i));
    }

    private static void tryAttractWorker(MinecraftServer server, ServerLevel level, SettlementData data) {
        List<Villager> lumber = workersByName(level, data.centerPos(), LUMBER_WORKER_NAME);
        List<Villager> farm = workersByName(level, data.centerPos(), FARM_WORKER_NAME);
        List<Villager> quarry = workersByName(level, data.centerPos(), QUARRY_WORKER_NAME);
        List<Villager> mine = workersByName(level, data.centerPos(), MINE_WORKER_NAME);
        List<Villager> transport = workersByName(level, data.centerPos(), TRANSPORT_WORKER_NAME);
        int actualPopulation = 1 + lumber.size() + farm.size() + quarry.size() + mine.size() + transport.size();
        if (data.population() != actualPopulation) data.setPopulation(actualPopulation);
        if (data.population() >= data.housingCapacity()) return;

        if (tryFillJob(server, level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME, lumber.size())) return;
        if (tryFillJob(server, level, data, BuildingType.FARM, FARM_WORKER_NAME, farm.size())) return;
        if (tryFillJob(server, level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME, quarry.size())) return;
        if (tryFillJob(server, level, data, BuildingType.MINE, MINE_WORKER_NAME, mine.size())) return;

        int desiredTransport = data.outposts().size();
        if (transport.size() < desiredTransport) {
            if (!consumeArrivalFood(level, data)) return;
            OutpostRecord outpost = data.outposts().get(transport.size());
            spawnWorker(level, outpost.center().above(), TRANSPORT_WORKER_NAME);
            finishArrival(server, data);
        }
    }

    private static boolean tryFillJob(MinecraftServer server, ServerLevel level, SettlementData data,
                                      BuildingType type, String workerName, int existingWorkers) {
        List<BuildingRecord> available = buildings(data, type);
        if (existingWorkers >= available.size()) return false;
        if (!consumeArrivalFood(level, data)) return true;
        spawnWorker(level, available.get(existingWorkers).workCenter(), workerName);
        finishArrival(server, data);
        return true;
    }

    private static boolean consumeArrivalFood(ServerLevel level, SettlementData data) {
        if (!(level.getBlockEntity(data.stockpilePos()) instanceof Container container)) return false;
        return SettlementInventory.consume(container, 0L, 0L, ARRIVAL_FOOD_COST);
    }

    private static void finishArrival(MinecraftServer server, SettlementData data) {
        data.addPopulation(1);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
    }

    private static void spawnWorker(ServerLevel level, BlockPos spawn, String name) {
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(name));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        level.addFreshEntity(worker);
    }

    private static void workLumber(ServerLevel level, SettlementData data,
                                   Villager worker, BuildingRecord camp) {
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty()) { deliverToMainStockpile(level, data, worker, carried); return; }
        BlockPos target = findTree(level, data, camp.workCenter());
        if (target == null) { move(worker, camp.workCenter(), 0.7D); return; }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 8.0D) {
            move(worker, target, 0.8D); return;
        }
        ItemStack harvested = harvestVerticalTrunk(level, data, target);
        if (!harvested.isEmpty()) worker.setItemSlot(EquipmentSlot.MAINHAND, harvested);
    }

    private static void workFarm(ServerLevel level, SettlementData data,
                                 Villager worker, BuildingRecord farm) {
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty()) { deliverToMainStockpile(level, data, worker, carried); return; }
        if (worker.distanceToSqr(farm.workCenter().getX() + 0.5D, farm.workCenter().getY(), farm.workCenter().getZ() + 0.5D) > 64.0D) {
            move(worker, farm.workCenter(), 0.75D); return;
        }
        int harvested = 0;
        BuildingType type = farm.buildingType();
        if (type == null) return;
        for (int x = 0; x < type.width() && harvested < MAX_CROPS_PER_TRIP; x++) {
            for (int z = 0; z < type.depth() && harvested < MAX_CROPS_PER_TRIP; z++) {
                BlockPos crop = farm.origin().offset(x, 1, z);
                BlockState state = level.getBlockState(crop);
                if (!state.is(Blocks.WHEAT) || !state.hasProperty(BlockStateProperties.AGE_7)
                        || state.getValue(BlockStateProperties.AGE_7) < 7) continue;
                level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3);
                harvested++;
            }
        }
        if (harvested > 0) worker.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WHEAT, harvested));
    }

    private static void workQuarry(ServerLevel level, SettlementData data,
                                   Villager worker, BuildingRecord quarry) {
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty()) { deliverToMainStockpile(level, data, worker, carried); return; }
        BlockPos target = findExposedStone(level, data, quarry.workCenter(), 14);
        if (target == null) { move(worker, quarry.workCenter(), 0.7D); return; }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 9.0D) {
            move(worker, target, 0.78D); return;
        }
        ItemStack stone = harvestStoneCluster(level, data, target);
        if (!stone.isEmpty()) worker.setItemSlot(EquipmentSlot.MAINHAND, stone);
    }

    private static void workMine(ServerLevel level, SettlementData data,
                                 Villager worker, BuildingRecord mine) {
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty()) { deliverToMainStockpile(level, data, worker, carried); return; }
        BlockPos work = mine.workCenter();
        if (worker.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D) > 16.0D) {
            move(worker, work, 0.75D); return;
        }
        BlockPos ore = findOreBelow(level, data, work);
        if (ore == null) return;
        ItemStack mined = mineOre(level, ore);
        if (!mined.isEmpty()) worker.setItemSlot(EquipmentSlot.MAINHAND, mined);
    }

    private static void workTransport(ServerLevel level, SettlementData data,
                                      Villager worker, OutpostRecord outpost) {
        ItemStack carried = worker.getMainHandItem();
        if (carried.isEmpty()) {
            BlockPos stock = outpost.stockpile();
            if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D) > 9.0D) {
                move(worker, stock, 0.85D); return;
            }
            if (!(level.getBlockEntity(stock) instanceof Container container)) return;
            ItemStack picked = takeFirstStack(container, MAX_TRANSPORT_STACK);
            if (!picked.isEmpty()) worker.setItemSlot(EquipmentSlot.MAINHAND, picked);
            else move(worker, outpost.center().above(), 0.6D);
            return;
        }
        if (outpost.roadIndex() >= 0 && outpost.roadIndex() < data.roads().size()) {
            RoadSegment road = data.roads().get(outpost.roadIndex());
            BlockPos roadStart = road.start().above();
            if (worker.distanceToSqr(roadStart.getX() + 0.5D, roadStart.getY(), roadStart.getZ() + 0.5D) > 16.0D) {
                move(worker, roadStart, 0.9D); return;
            }
        }
        deliverToMainStockpile(level, data, worker, carried);
    }

    private static ItemStack takeFirstStack(Container container, int maxCount) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack current = container.getItem(slot);
            if (current.isEmpty()) continue;
            int take = Math.min(maxCount, current.getCount());
            ItemStack result = current.copyWithCount(take);
            current.shrink(take);
            container.setChanged();
            return result;
        }
        return ItemStack.EMPTY;
    }

    private static void deliverToMainStockpile(ServerLevel level, SettlementData data,
                                               Villager worker, ItemStack carried) {
        BlockPos stock = data.stockpilePos();
        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D) > 9.0D) {
            move(worker, stock, 0.85D); return;
        }
        if (!(level.getBlockEntity(stock) instanceof Container container)) return;
        ItemStack remaining = SettlementInventory.insert(container, carried);
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
                    if (level.getBlockState(trunk.offset(dx, y, dz)).is(BlockTags.LEAVES)) return true;
                }
            }
        }
        return false;
    }

    private static ItemStack harvestVerticalTrunk(ServerLevel level, SettlementData data, BlockPos base) {
        BlockState first = level.getBlockState(base);
        if (!first.is(BlockTags.LOGS)) return ItemStack.EMPTY;
        Item item = first.getBlock().asItem();
        if (item == Items.AIR) return ItemStack.EMPTY;
        int count = 0;
        for (int y = 0; y < 10 && count < MAX_LOGS_PER_TRIP; y++) {
            BlockPos pos = base.above(y);
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
        BlockState first = level.getBlockState(base);
        if (!isQuarryStone(first)) return ItemStack.EMPTY;
        Item item = first.getBlock().asItem();
        if (item == Items.AIR) return ItemStack.EMPTY;
        int count = 0;
        for (int dx = -1; dx <= 1 && count < MAX_STONE_PER_TRIP; dx++) {
            for (int dz = -1; dz <= 1 && count < MAX_STONE_PER_TRIP; dz++) {
                BlockPos pos = base.offset(dx, 0, dz);
                BlockState state = level.getBlockState(pos);
                if (state.getBlock().asItem() != item || !isQuarryStone(state) || isProtected(data, pos)) continue;
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
                        if (level.getBlockState(pos).is(Tags.Blocks.ORES) && !isProtected(data, pos)) return pos;
                    }
                }
            }
        }
        return null;
    }

    private static ItemStack mineOre(ServerLevel level, BlockPos pos) {
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

    private static List<Villager> workersByName(ServerLevel level, BlockPos center, String name) {
        AABB search = new AABB(
                center.getX() - 256.0D, center.getY() - 96.0D, center.getZ() - 256.0D,
                center.getX() + 257.0D, center.getY() + 97.0D, center.getZ() + 257.0D);
        List<Villager> workers = level.getEntitiesOfClass(Villager.class, search,
                villager -> villager.getCustomName() != null && name.equals(villager.getCustomName().getString()));
        workers.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return workers;
    }
}
