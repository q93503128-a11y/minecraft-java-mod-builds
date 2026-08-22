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
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SettlementWorkerService {
    private static final String LUMBER_WORKER_NAME = "벌목 주민";
    private static final String TRANSPORT_WORKER_NAME = "운송 주민";
    private static final String LUMBER_WORKER_TAG = "frontier_settlement_lumber_worker";
    private static final String TRANSPORT_WORKER_TAG = "frontier_settlement_transport_worker";
    private static final long ARRIVAL_FOOD_COST = 4L;
    private static final int TREE_SEARCH_RADIUS = 18;
    private static final int MAX_LOGS_PER_TRIP = 6;
    private static final int MAX_TRANSPORT_STACK = 16;

    private SettlementWorkerService() {}

    public static void tick(MinecraftServer server, SettlementData data) {
        ServerLevel level = server.overworld();
        if (server.getTickCount() % 1200 == 0) tryAttractWorker(server, level, data);
        if (server.getTickCount() % 10 != 0) return;

        List<BuildingRecord> camps = lumberCamps(data);
        List<Villager> lumber = workersByName(level, data.centerPos(), LUMBER_WORKER_NAME);
        int lumberCount = Math.min(camps.size(), lumber.size());
        for (int i = 0; i < lumberCount; i++) workLumber(level, data, lumber.get(i), camps.get(i));

        List<Villager> transport = workersByName(level, data.centerPos(), TRANSPORT_WORKER_NAME);
        int transportCount = Math.min(data.outposts().size(), transport.size());
        for (int i = 0; i < transportCount; i++) workTransport(level, data, transport.get(i), data.outposts().get(i));
    }

    private static void tryAttractWorker(MinecraftServer server, ServerLevel level, SettlementData data) {
        List<BuildingRecord> camps = lumberCamps(data);
        List<Villager> lumber = workersByName(level, data.centerPos(), LUMBER_WORKER_NAME);
        List<Villager> transport = workersByName(level, data.centerPos(), TRANSPORT_WORKER_NAME);
        int actualPopulation = 1 + lumber.size() + transport.size();
        if (data.population() != actualPopulation) data.setPopulation(actualPopulation);
        if (data.population() >= data.housingCapacity()) return;

        int desiredLumber = Math.min(camps.size(), Math.max(0, data.housingCapacity() - 1));
        if (lumber.size() < desiredLumber) {
            if (!consumeArrivalFood(level, data)) return;
            spawnWorker(level, camps.get(lumber.size()).workCenter(), LUMBER_WORKER_NAME, LUMBER_WORKER_TAG);
            finishArrival(server, data);
            return;
        }

        int remainingBeds = Math.max(0, data.housingCapacity() - 1 - lumber.size());
        int desiredTransport = Math.min(data.outposts().size(), remainingBeds);
        if (transport.size() < desiredTransport) {
            if (!consumeArrivalFood(level, data)) return;
            OutpostRecord outpost = data.outposts().get(transport.size());
            spawnWorker(level, outpost.center().above(), TRANSPORT_WORKER_NAME, TRANSPORT_WORKER_TAG);
            finishArrival(server, data);
        }
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

    private static void spawnWorker(ServerLevel level, BlockPos spawn, String name, String tag) {
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(name));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.addTag(tag);
        level.addFreshEntity(worker);
    }

    private static void workLumber(ServerLevel level, SettlementData data,
                                   Villager worker, BuildingRecord camp) {
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty()) {
            deliverToMainStockpile(level, data, worker, carried);
            return;
        }

        BlockPos target = findTree(level, data, camp.workCenter());
        if (target == null) {
            move(worker, camp.workCenter(), 0.7D);
            return;
        }

        double distance = worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        if (distance > 8.0D) {
            move(worker, target, 0.8D);
            return;
        }

        ItemStack harvested = harvestVerticalTrunk(level, data, target);
        if (!harvested.isEmpty()) worker.setItemSlot(EquipmentSlot.MAINHAND, harvested);
    }

    private static void workTransport(ServerLevel level, SettlementData data,
                                      Villager worker, OutpostRecord outpost) {
        ItemStack carried = worker.getMainHandItem();
        if (carried.isEmpty()) {
            BlockPos stock = outpost.stockpile();
            if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D) > 9.0D) {
                move(worker, stock, 0.85D);
                return;
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
                move(worker, roadStart, 0.9D);
                return;
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
        double distance = worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D);
        if (distance > 9.0D) {
            move(worker, stock, 0.85D);
            return;
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

    private static boolean isProtected(SettlementData data, BlockPos pos) {
        if (pos.closerThan(data.stockpilePos(), 3.0D)) return true;
        for (BuildingRecord building : data.buildings()) if (building.protectsXZ(pos, 1)) return true;
        for (RoadSegment road : data.roads()) if (road.containsXZ(pos)) return true;
        for (OutpostRecord outpost : data.outposts()) if (outpost.protectsXZ(pos, 1)) return true;
        return false;
    }

    private static List<BuildingRecord> lumberCamps(SettlementData data) {
        List<BuildingRecord> camps = new ArrayList<>();
        for (BuildingRecord building : data.buildings()) {
            if (BuildingType.LUMBER_CAMP.id().equals(building.type())) camps.add(building);
        }
        return camps;
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
