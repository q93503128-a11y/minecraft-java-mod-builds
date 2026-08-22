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

import java.util.List;

/** Physical local production for specialized outposts. One resident outpost worker is bundled with
 * each specialized outpost, so the player never has to assign jobs or routes manually. */
public final class SettlementOutpostProductionService {
    private static final int TREE_RADIUS = 18;
    private static final int QUARRY_RADIUS = 16;
    private static final int MAX_LOGS = 6;
    private static final int MAX_STONE = 6;
    private static final int MAX_CROPS = 8;

    private SettlementOutpostProductionService() {}

    public static void tick(MinecraftServer server, SettlementData data) {
        if (server.getTickCount() % 10 != 0) return;
        ServerLevel level = server.overworld();
        for (OutpostRecord outpost : data.outposts()) {
            if ("general".equals(outpost.specialization())) continue;
            if ("agriculture".equals(outpost.specialization())) ensureAgriculturePlot(level, data, outpost);
            Villager worker = ensureWorker(level, outpost);
            if (worker != null) work(level, data, outpost, worker);
        }
    }

    private static Villager ensureWorker(ServerLevel level, OutpostRecord outpost) {
        String name = workerName(outpost);
        AABB search = new AABB(
                outpost.centerX() - 48.0D, outpost.centerY() - 32.0D, outpost.centerZ() - 48.0D,
                outpost.centerX() + 49.0D, outpost.centerY() + 33.0D, outpost.centerZ() + 49.0D);
        List<Villager> found = level.getEntitiesOfClass(Villager.class, search,
                villager -> villager.getCustomName() != null && name.equals(villager.getCustomName().getString()));
        if (!found.isEmpty()) return found.getFirst();

        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = outpost.center().above();
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(name));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        level.addFreshEntity(worker);
        return worker;
    }

    private static String workerName(OutpostRecord outpost) {
        String role = switch (outpost.specialization()) {
            case "lumber" -> "벌목";
            case "quarry" -> "채석";
            case "mining" -> "광산";
            case "agriculture" -> "농업";
            default -> "작업";
        };
        return "전초 " + role + " 주민 #" + outpost.id();
    }

    private static void work(ServerLevel level, SettlementData data, OutpostRecord outpost, Villager worker) {
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty()) {
            deliver(level, outpost, worker, carried);
            return;
        }

        switch (outpost.specialization()) {
            case "lumber" -> workLumber(level, data, outpost, worker);
            case "quarry" -> workQuarry(level, data, outpost, worker);
            case "mining" -> workMine(level, data, outpost, worker);
            case "agriculture" -> workAgriculture(level, data, outpost, worker);
            default -> move(worker, outpost.center().above(), 0.65D);
        }
    }

    private static void deliver(ServerLevel level, OutpostRecord outpost, Villager worker, ItemStack carried) {
        BlockPos stock = outpost.stockpile();
        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D) > 9.0D) {
            move(worker, stock, 0.82D);
            return;
        }
        if (!(level.getBlockEntity(stock) instanceof Container container)) return;
        worker.setItemSlot(EquipmentSlot.MAINHAND, SettlementInventory.insert(container, carried));
    }

    private static void workLumber(ServerLevel level, SettlementData data, OutpostRecord outpost, Villager worker) {
        BlockPos target = findTree(level, data, outpost.center(), TREE_RADIUS);
        if (target == null) {
            move(worker, outpost.center().above(), 0.65D);
            return;
        }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 8.0D) {
            move(worker, target, 0.8D);
            return;
        }
        ItemStack harvested = harvestVerticalTrunk(level, data, target);
        if (!harvested.isEmpty()) worker.setItemSlot(EquipmentSlot.MAINHAND, harvested);
    }

    private static void workQuarry(ServerLevel level, SettlementData data, OutpostRecord outpost, Villager worker) {
        BlockPos target = findExposedStone(level, data, outpost.center(), QUARRY_RADIUS);
        if (target == null) {
            move(worker, outpost.center().above(), 0.65D);
            return;
        }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 9.0D) {
            move(worker, target, 0.78D);
            return;
        }
        ItemStack harvested = harvestStoneCluster(level, data, target);
        if (!harvested.isEmpty()) worker.setItemSlot(EquipmentSlot.MAINHAND, harvested);
    }

    private static void workMine(ServerLevel level, SettlementData data, OutpostRecord outpost, Villager worker) {
        BlockPos home = outpost.center().above();
        if (worker.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D) > 16.0D) {
            move(worker, home, 0.72D);
            return;
        }
        BlockPos ore = findOreBelow(level, data, outpost.center());
        if (ore == null) return;
        ItemStack mined = mineOre(level, ore);
        if (!mined.isEmpty()) worker.setItemSlot(EquipmentSlot.MAINHAND, mined);
    }

    private static void workAgriculture(ServerLevel level, SettlementData data, OutpostRecord outpost, Villager worker) {
        int harvested = 0;
        for (int forward = 1; forward <= 3 && harvested < MAX_CROPS; forward++) {
            for (int side : new int[] {-2, -1, 1, 2}) {
                BlockPos crop = local(data, outpost, forward, side, 1);
                BlockState state = level.getBlockState(crop);
                if (!state.is(Blocks.WHEAT) || !state.hasProperty(BlockStateProperties.AGE_7)
                        || state.getValue(BlockStateProperties.AGE_7) < 7) continue;
                if (worker.distanceToSqr(crop.getX() + 0.5D, crop.getY(), crop.getZ() + 0.5D) > 9.0D) {
                    move(worker, crop, 0.72D);
                    return;
                }
                level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3);
                harvested++;
                if (harvested >= MAX_CROPS) break;
            }
        }
        if (harvested > 0) worker.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WHEAT, harvested));
        else move(worker, outpost.center().above(), 0.6D);
    }

    public static void ensureAgriculturePlot(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        if (!"agriculture".equals(outpost.specialization())) return;
        BlockPos water = local(data, outpost, 3, 0, 0);
        BlockState waterBase = level.getBlockState(water);
        if (waterBase.is(Blocks.COARSE_DIRT) || waterBase.is(Blocks.DIRT) || waterBase.is(Blocks.GRASS_BLOCK)
                || waterBase.is(Blocks.FARMLAND)) {
            level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);
        }
        for (int forward = 1; forward <= 3; forward++) {
            for (int side : new int[] {-2, -1, 1, 2}) {
                BlockPos soil = local(data, outpost, forward, side, 0);
                BlockState existing = level.getBlockState(soil);
                if (existing.is(Blocks.COARSE_DIRT) || existing.is(Blocks.DIRT) || existing.is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), 3);
                }
                BlockPos crop = soil.above();
                if (level.getBlockState(soil).is(Blocks.FARMLAND) && level.getBlockState(crop).isAir()) {
                    level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3);
                }
            }
        }
    }

    private static BlockPos local(SettlementData data, OutpostRecord outpost, int forward, int side, int y) {
        if (outpost.roadIndex() < 0 || outpost.roadIndex() >= data.roads().size()) return outpost.center().offset(side, y, forward - 4);
        RoadSegment road = data.roads().get(outpost.roadIndex());
        BlockPos gate = road.end().offset(road.directionX(), 0, road.directionZ());
        int x = gate.getX() + road.directionX() * forward - road.directionZ() * side;
        int z = gate.getZ() + road.directionZ() * forward + road.directionX() * side;
        return new BlockPos(x, gate.getY() + y, z);
    }

    private static BlockPos findTree(ServerLevel level, SettlementData data, BlockPos center, int radius) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int y = center.getY() - 4; y <= center.getY() + 10; y++) {
                    BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
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
        for (int y = 0; y < 10 && count < MAX_LOGS; y++) {
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
                for (int y = center.getY() - 5; y <= center.getY() + 4; y++) {
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
        for (int dx = -1; dx <= 1 && count < MAX_STONE; dx++) {
            for (int dz = -1; dz <= 1 && count < MAX_STONE; dz++) {
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
        for (int depth = 2; depth <= 28; depth++) {
            int y = center.getY() - depth;
            for (int radius = 0; radius <= 14; radius++) {
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

    private static void move(Villager worker, BlockPos target, double speed) {
        worker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speed);
    }
}
