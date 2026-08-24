package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Loaded-only coast/river specialization overlay for otherwise-general outposts.
 *
 * The specialization is environmental rather than a menu toggle: a general outpost with a broad
 * nearby open-water shoreline gains one assigned fishing worker. The worker visibly walks to a dry
 * bank, uses a fishing rod, carries real fish ItemStacks back to the outpost stockpile, and leaves
 * long-distance movement to the existing road transporter. No emeralds, virtual trade points or
 * force-loaded water simulation are created here. Alpha.41 gives an actively dangerous general
 * outpost military precedence, so the same remote site cannot silently fish while under attack.
 */
public final class SettlementFishingOutpostService {
    public static final String FISHING_WORKER_TAG = "frontier_settlement_fishing_outpost_worker";
    public static final String FISHING_OUTPOST_TAG_PREFIX = "frontier_settlement_fishing_outpost_";

    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private static final int WATER_SEARCH_RADIUS = 12;
    private static final int MIN_OPEN_WATER_COLUMNS = 24;
    private static final int WORK_PERIOD_TICKS = 140;
    private static final int MAX_CATCH = 3;
    private static final double WORK_RANGE_SQR = 9.0D;
    private static final double SEARCH_RADIUS = 48.0D;

    private SettlementFishingOutpostService() {}

    private record FishingSpot(BlockPos bank, BlockPos water) {}

    public static void tick(MinecraftServer server, SettlementData data) {
        if (server.getTickCount() % 20 != 0) return;
        ServerLevel level = server.overworld();
        for (OutpostRecord outpost : data.outposts()) {
            if (!"general".equals(outpost.specialization())) continue;
            if (!level.hasChunkAt(outpost.center()) || !level.hasChunkAt(outpost.stockpile())) continue;

            Villager worker = findAssignedWorker(level, outpost);
            if (SettlementMilitaryOutpostService.isActiveMilitaryOutpost(level, outpost)) {
                if (worker != null && worker.getMainHandItem().isEmpty()) moveOrStop(worker, outpost.center().above(), 0.65D);
                continue;
            }

            FishingSpot spot = findFishingSpot(level, outpost);
            if (spot == null) {
                if (worker != null && worker.getMainHandItem().isEmpty()) moveOrStop(worker, outpost.center().above(), 0.65D);
                continue;
            }
            if (worker == null) worker = spawnAssignedWorker(level, outpost);
            if (worker != null) work(level, outpost, spot, worker);
        }
    }

    public static int activeFishingOutpostCount(ServerLevel level, SettlementData data) {
        int count = 0;
        for (OutpostRecord outpost : data.outposts()) {
            if ("general".equals(outpost.specialization())
                    && level.hasChunkAt(outpost.center())
                    && !SettlementMilitaryOutpostService.isActiveMilitaryOutpost(level, outpost)
                    && findFishingSpot(level, outpost) != null) count++;
        }
        return count;
    }

    public static String specializationDisplayName(ServerLevel level, OutpostRecord outpost) {
        if (!"general".equals(outpost.specialization())) return outpost.specializationDisplayName();
        if (!level.hasChunkAt(outpost.center())) return "일반(환경 판정 대기)";
        if (SettlementMilitaryOutpostService.isActiveMilitaryOutpost(level, outpost)) return "위험지역 군사거점";
        return findFishingSpot(level, outpost) != null ? "어업·수변교역" : "일반";
    }

    private static Villager spawnAssignedWorker(ServerLevel level, OutpostRecord outpost) {
        if (!level.hasChunkAt(outpost.center())) return null;
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = outpost.center().above();
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal("전초 어업 주민 #" + outpost.id()));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.addTag(FISHING_WORKER_TAG);
        worker.addTag(assignmentTag(outpost));
        worker.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.FISHING_ROD));
        level.addFreshEntity(worker);
        return worker;
    }

    private static Villager findAssignedWorker(ServerLevel level, OutpostRecord outpost) {
        String assignment = assignmentTag(outpost);
        AABB area = new AABB(outpost.center()).inflate(SEARCH_RADIUS, 24.0D, SEARCH_RADIUS);
        List<Villager> workers = level.getEntitiesOfClass(Villager.class, area,
                villager -> villager.entityTags().contains(FISHING_WORKER_TAG)
                        && villager.entityTags().contains(assignment));
        return workers.isEmpty() ? null : workers.getFirst();
    }

    private static String assignmentTag(OutpostRecord outpost) {
        return FISHING_OUTPOST_TAG_PREFIX + outpost.id();
    }

    private static void work(ServerLevel level, OutpostRecord outpost, FishingSpot spot, Villager worker) {
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty()) {
            deliver(level, outpost, worker, carried);
            return;
        }
        if (worker.distanceToSqr(spot.bank().getX() + 0.5D, spot.bank().getY(), spot.bank().getZ() + 0.5D)
                > WORK_RANGE_SQR) {
            worker.getNavigation().moveTo(spot.bank().getX() + 0.5D, spot.bank().getY(), spot.bank().getZ() + 0.5D, 0.76D);
            return;
        }
        if (!workDue(level, outpost)) return;
        if (!isOpenSurfaceWater(level, spot.water())) return;

        int amount = 1 + level.getRandom().nextInt(MAX_CATCH);
        ItemStack caught = level.getRandom().nextInt(4) == 0
                ? new ItemStack(Items.SALMON, amount)
                : new ItemStack(Items.COD, amount);
        worker.swing(InteractionHand.OFF_HAND);
        worker.setItemSlot(EquipmentSlot.MAINHAND, caught);
    }

    private static void deliver(ServerLevel level, OutpostRecord outpost, Villager worker, ItemStack carried) {
        BlockPos stock = outpost.stockpile();
        if (!level.hasChunkAt(stock)) {
            worker.getNavigation().stop();
            return;
        }
        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D) > WORK_RANGE_SQR) {
            worker.getNavigation().moveTo(stock.getX() + 0.5D, stock.getY(), stock.getZ() + 0.5D, 0.82D);
            return;
        }
        if (!(level.getBlockEntity(stock) instanceof Container container)) return;
        worker.setItemSlot(EquipmentSlot.MAINHAND, SettlementInventory.insert(container, carried));
    }

    private static boolean workDue(ServerLevel level, OutpostRecord outpost) {
        long slots = Math.max(1L, WORK_PERIOD_TICKS / 20L);
        long current = level.getGameTime() / 20L;
        return Math.floorMod(current + outpost.id() * 5L, slots) == 0L;
    }

    private static FishingSpot findFishingSpot(ServerLevel level, OutpostRecord outpost) {
        BlockPos center = outpost.center();
        int waterColumns = 0;
        BlockPos bestBank = null;
        BlockPos bestWater = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dx = -WATER_SEARCH_RADIUS; dx <= WATER_SEARCH_RADIUS; dx++) {
            for (int dz = -WATER_SEARCH_RADIUS; dz <= WATER_SEARCH_RADIUS; dz++) {
                if (dx * dx + dz * dz > WATER_SEARCH_RADIUS * WATER_SEARCH_RADIUS) continue;
                BlockPos water = findSurfaceWaterInColumn(level, center.getX() + dx, center.getZ() + dz,
                        center.getY() - 4, center.getY() + 2);
                if (water == null) continue;
                waterColumns++;
                for (Direction direction : HORIZONTAL) {
                    BlockPos ground = water.relative(direction);
                    BlockPos bank = ground.above();
                    if (!isSafeBank(level, ground, bank)) continue;
                    double distance = bank.distSqr(center);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestBank = bank;
                        bestWater = water;
                    }
                }
            }
        }
        if (waterColumns < MIN_OPEN_WATER_COLUMNS || bestBank == null || bestWater == null) return null;
        return new FishingSpot(bestBank, bestWater);
    }

    private static BlockPos findSurfaceWaterInColumn(ServerLevel level, int x, int z, int minY, int maxY) {
        for (int y = maxY; y >= minY; y--) {
            BlockPos water = new BlockPos(x, y, z);
            if (!level.hasChunkAt(water) || !level.hasChunkAt(water.above())) continue;
            if (isOpenSurfaceWater(level, water)) return water;
        }
        return null;
    }

    private static boolean isOpenSurfaceWater(ServerLevel level, BlockPos water) {
        if (!level.hasChunkAt(water) || !level.hasChunkAt(water.above())) return false;
        return level.getBlockState(water).is(Blocks.WATER)
                && level.getBlockState(water.above()).getFluidState().isEmpty();
    }

    private static boolean isSafeBank(ServerLevel level, BlockPos ground, BlockPos bank) {
        if (!level.hasChunkAt(ground) || !level.hasChunkAt(bank)) return false;
        if (level.getBlockEntity(ground) != null || level.getBlockEntity(bank) != null) return false;
        BlockState floor = level.getBlockState(ground);
        BlockState space = level.getBlockState(bank);
        return !floor.isAir() && floor.getFluidState().isEmpty()
                && (space.isAir() || space.canBeReplaced()) && space.getFluidState().isEmpty();
    }

    private static void moveOrStop(Villager worker, BlockPos target, double speed) {
        double distance = worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        if (distance > 4.0D) worker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speed);
        else worker.getNavigation().stop();
    }
}
