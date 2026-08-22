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
    private static final String LUMBER_WORKER_TAG = "frontier_settlement_lumber_worker";
    private static final long ARRIVAL_FOOD_COST = 4L;
    private static final int TREE_SEARCH_RADIUS = 18;
    private static final int MAX_LOGS_PER_TRIP = 6;

    private SettlementWorkerService() {}

    public static void tick(MinecraftServer server, SettlementData data) {
        ServerLevel level = server.overworld();
        if (server.getTickCount() % 1200 == 0) {
            tryAttractLumberWorker(server, level, data);
        }
        if (server.getTickCount() % 10 != 0) return;

        List<BuildingRecord> camps = lumberCamps(data);
        if (camps.isEmpty()) return;
        List<Villager> workers = lumberWorkers(level, data.centerPos());
        int count = Math.min(camps.size(), workers.size());
        for (int i = 0; i < count; i++) {
            workLumber(level, data, workers.get(i), camps.get(i));
        }
    }

    private static void tryAttractLumberWorker(MinecraftServer server, ServerLevel level, SettlementData data) {
        List<BuildingRecord> camps = lumberCamps(data);
        if (camps.isEmpty() || data.housingCapacity() <= 1) return;

        List<Villager> workers = lumberWorkers(level, data.centerPos());
        int actualPopulation = 1 + workers.size();
        if (data.population() != actualPopulation) data.setPopulation(actualPopulation);

        int desiredWorkers = Math.min(camps.size(), Math.max(0, data.housingCapacity() - 1));
        if (workers.size() >= desiredWorkers || data.population() >= data.housingCapacity()) return;

        if (!(level.getBlockEntity(data.stockpilePos()) instanceof Container container)) return;
        if (!SettlementInventory.consume(container, 0L, 0L, ARRIVAL_FOOD_COST)) return;

        BuildingRecord camp = camps.get(workers.size());
        BlockPos spawn = camp.workCenter();
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(LUMBER_WORKER_NAME));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.addTag(LUMBER_WORKER_TAG);
        level.addFreshEntity(worker);

        data.addPopulation(1);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
    }

    private static void workLumber(ServerLevel level, SettlementData data,
                                   Villager worker, BuildingRecord camp) {
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty()) {
            deliverToStockpile(level, data, worker, carried);
            return;
        }

        BlockPos target = findTree(level, data, camp.workCenter());
        if (target == null) {
            worker.getNavigation().moveTo(
                    camp.workCenter().getX() + 0.5D,
                    camp.workCenter().getY(),
                    camp.workCenter().getZ() + 0.5D,
                    0.7D);
            return;
        }

        double distance = worker.distanceToSqr(
                target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        if (distance > 8.0D) {
            worker.getNavigation().moveTo(
                    target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.8D);
            return;
        }

        ItemStack harvested = harvestVerticalTrunk(level, data, target);
        if (!harvested.isEmpty()) {
            worker.setItemSlot(EquipmentSlot.MAINHAND, harvested);
        }
    }

    private static void deliverToStockpile(ServerLevel level, SettlementData data,
                                           Villager worker, ItemStack carried) {
        BlockPos stock = data.stockpilePos();
        double distance = worker.distanceToSqr(
                stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D);
        if (distance > 9.0D) {
            worker.getNavigation().moveTo(
                    stock.getX() + 0.5D, stock.getY(), stock.getZ() + 0.5D, 0.85D);
            return;
        }

        if (!(level.getBlockEntity(stock) instanceof Container container)) return;
        ItemStack remaining = SettlementInventory.insert(container, carried);
        worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);
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
                    if (distance < bestDistance) {
                        best = pos;
                        bestDistance = distance;
                    }
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
        for (BuildingRecord building : data.buildings()) {
            if (building.protectsXZ(pos, 1)) return true;
        }
        for (RoadSegment road : data.roads()) {
            if (road.containsXZ(pos)) return true;
        }
        for (OutpostRecord outpost : data.outposts()) {
            if (outpost.protectsXZ(pos, 1)) return true;
        }
        return false;
    }

    private static List<BuildingRecord> lumberCamps(SettlementData data) {
        List<BuildingRecord> camps = new ArrayList<>();
        for (BuildingRecord building : data.buildings()) {
            if (BuildingType.LUMBER_CAMP.id().equals(building.type())) camps.add(building);
        }
        return camps;
    }

    private static List<Villager> lumberWorkers(ServerLevel level, BlockPos center) {
        AABB search = new AABB(
                center.getX() - 128.0D, center.getY() - 64.0D, center.getZ() - 128.0D,
                center.getX() + 129.0D, center.getY() + 65.0D, center.getZ() + 129.0D);
        List<Villager> workers = level.getEntitiesOfClass(Villager.class, search,
                villager -> villager.getCustomName() != null
                        && LUMBER_WORKER_NAME.equals(villager.getCustomName().getString()));
        workers.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return workers;
    }
}
