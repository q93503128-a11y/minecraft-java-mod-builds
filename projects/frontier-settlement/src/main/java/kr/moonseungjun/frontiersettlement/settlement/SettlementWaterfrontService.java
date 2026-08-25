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
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Alpha.46 fishing-outpost waterfront works.
 *
 * A qualifying loaded fishing outpost may establish one small persisted pier. The existing fishing
 * worker builds it one block at a time using real wood from the outpost stockpile. If that local
 * stockpile lacks construction wood, the existing road transporter may reverse-supply it; this
 * class never creates a second long-distance logistics authority. Once complete, the pier exposes a
 * dedicated opt-in fish trade barrel. A visible local merchant converts 16 real cod/salmon into one
 * real emerald in that barrel only. Ordinary outpost stock is never auto-sold.
 */
public final class SettlementWaterfrontService {
    public static final String WATER_TRADER_TAG = "frontier_settlement_waterfront_trader";
    public static final String WATER_TRADER_ASSIGNMENT_PREFIX = "frontier_settlement_waterfront_";
    public static final int TRADE_FISH_COST = 16;

    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private static final int WATER_SEARCH_RADIUS = 12;
    private static final int LOCAL_HAUL_BATCH = 8;
    private static final int LOCAL_SUPPLY_TARGET = 12;
    private static final int TRADE_PERIOD_TICKS = 200;
    private static final double INTERACTION_RANGE_SQR = 9.0D;
    private static final double ENTITY_SEARCH_RADIUS = 48.0D;

    private SettlementWaterfrontService() {}

    private record Placement(BlockPos pos, BlockState state) {}

    public static void tick(MinecraftServer server, SettlementData settlement) {
        if (server.getTickCount() % 20 != 0) return;
        ServerLevel level = server.overworld();
        SettlementWaterfrontData data = SettlementWaterfrontData.get(server);

        for (OutpostRecord outpost : settlement.outposts()) {
            if (!"general".equals(outpost.specialization())) continue;
            if (!level.hasChunkAt(outpost.center()) || !level.hasChunkAt(outpost.stockpile())) continue;
            if (SettlementMilitaryOutpostService.isActiveMilitaryOutpost(level, outpost)) continue;
            if (!SettlementFishingOutpostService.hasFishingShoreline(level, outpost)) continue;

            WaterfrontState state = data.stateFor(outpost.id());
            if (state == null) {
                state = findWaterfrontSite(level, outpost);
                if (state == null) continue;
                data.replace(state);
            }

            List<Placement> plan = createPlan(state);
            if (state.buildStep() < plan.size()) {
                Villager worker = SettlementFishingOutpostService.ensureAssignedWorker(level, outpost);
                if (worker != null) workConstruction(level, settlement, data, outpost, state, plan, worker);
                continue;
            }

            BlockPos crate = tradeCrate(state);
            if (!level.hasChunkAt(crate) || !(level.getBlockEntity(crate) instanceof Container container)) continue;
            Villager trader = ensureTrader(level, outpost, state);
            if (trader == null) continue;
            BlockPos station = traderStation(state);
            if (trader.distanceToSqr(station.getX() + 0.5D, station.getY(), station.getZ() + 0.5D)
                    > INTERACTION_RANGE_SQR) {
                trader.getNavigation().moveTo(station.getX() + 0.5D, station.getY(), station.getZ() + 0.5D, 0.72D);
                continue;
            }
            if (tradeDue(server, outpost)) tradeOne(container, trader);
        }
    }

    public static boolean isConstructionActive(MinecraftServer server, OutpostRecord outpost) {
        WaterfrontState state = SettlementWaterfrontData.get(server).stateFor(outpost.id());
        return state != null && state.buildStep() < createPlan(state).size();
    }

    public static boolean isComplete(MinecraftServer server, OutpostRecord outpost) {
        WaterfrontState state = SettlementWaterfrontData.get(server).stateFor(outpost.id());
        return state != null && state.buildStep() >= createPlan(state).size();
    }

    public static int completedWaterfrontCount(MinecraftServer server, SettlementData settlement) {
        int count = 0;
        for (OutpostRecord outpost : settlement.outposts()) if (isComplete(server, outpost)) count++;
        return count;
    }

    /** Existing road transporter asks this; no chunk is loaded merely to answer it. */
    public static int woodSupplyShortage(ServerLevel level, OutpostRecord outpost) {
        MinecraftServer server = level.getServer();
        WaterfrontState state = SettlementWaterfrontData.get(server).stateFor(outpost.id());
        if (state == null) return 0;
        int remaining = Math.max(0, createPlan(state).size() - state.buildStep());
        if (remaining <= 0 || !level.hasChunkAt(outpost.stockpile())) return 0;
        if (!(level.getBlockEntity(outpost.stockpile()) instanceof Container container)) return 0;
        int target = Math.min(LOCAL_SUPPLY_TARGET, remaining);
        return Math.max(0, target - (int) Math.min(Integer.MAX_VALUE, SettlementInventory.countWood(container)));
    }

    public static BlockPos tradeCrate(MinecraftServer server, OutpostRecord outpost) {
        WaterfrontState state = SettlementWaterfrontData.get(server).stateFor(outpost.id());
        return state == null || !isComplete(server, outpost) ? null : tradeCrate(state);
    }

    private static WaterfrontState findWaterfrontSite(ServerLevel level, OutpostRecord outpost) {
        BlockPos center = outpost.center();
        WaterfrontState best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -WATER_SEARCH_RADIUS; dx <= WATER_SEARCH_RADIUS; dx++) {
            for (int dz = -WATER_SEARCH_RADIUS; dz <= WATER_SEARCH_RADIUS; dz++) {
                if (dx * dx + dz * dz > WATER_SEARCH_RADIUS * WATER_SEARCH_RADIUS) continue;
                BlockPos water = findSurfaceWaterInColumn(level, center.getX() + dx, center.getZ() + dz,
                        center.getY() - 4, center.getY() + 2);
                if (water == null) continue;
                for (Direction groundDirection : HORIZONTAL) {
                    BlockPos ground = water.relative(groundDirection);
                    BlockPos bank = ground.above();
                    if (!isSafeBank(level, ground, bank)) continue;
                    int directionX = water.getX() - bank.getX();
                    int directionZ = water.getZ() - bank.getZ();
                    WaterfrontState candidate = new WaterfrontState(outpost.id(), bank.getX(), bank.getY(), bank.getZ(),
                            directionX, directionZ, 0);
                    if (!canBuildPlanAt(level, candidate)) continue;
                    double distance = bank.distSqr(center);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    private static boolean canBuildPlanAt(ServerLevel level, WaterfrontState state) {
        if (Math.abs(state.directionX()) + Math.abs(state.directionZ()) != 1) return false;
        List<Placement> plan = createPlan(state);
        for (int i = 0; i < plan.size(); i++) {
            Placement placement = plan.get(i);
            BlockPos pos = placement.pos();
            if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null) return false;
            BlockState current = level.getBlockState(pos);
            if ((!current.isAir() && !current.canBeReplaced()) || !current.getFluidState().isEmpty()) return false;
            if (placement.state().is(Blocks.SPRUCE_SLAB) && !isOpenSurfaceWater(level, pos.below())) return false;
        }
        return true;
    }

    private static void workConstruction(ServerLevel level, SettlementData settlement, SettlementWaterfrontData data,
                                         OutpostRecord outpost, WaterfrontState state, List<Placement> plan,
                                         Villager worker) {
        int step = state.buildStep();
        if (step >= plan.size()) return;
        Placement placement = plan.get(step);
        if (!level.hasChunkAt(placement.pos())) { worker.getNavigation().stop(); return; }
        BlockState current = level.getBlockState(placement.pos());
        if (current.is(placement.state().getBlock())) {
            data.replace(state.withBuildStep(step + 1));
            return;
        }
        if (level.getBlockEntity(placement.pos()) != null
                || !current.getFluidState().isEmpty()
                || (!current.isAir() && !current.canBeReplaced())) {
            worker.getNavigation().stop();
            return;
        }

        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty() && !SettlementInventory.isWood(carried)) return;
        if (carried.isEmpty()) {
            loadLocalWood(level, outpost, worker);
            return;
        }

        if (worker.distanceToSqr(placement.pos().getX() + 0.5D, placement.pos().getY(), placement.pos().getZ() + 0.5D)
                > INTERACTION_RANGE_SQR) {
            worker.getNavigation().moveTo(placement.pos().getX() + 0.5D, placement.pos().getY(), placement.pos().getZ() + 0.5D, 0.78D);
            return;
        }

        if (!level.setBlock(placement.pos(), placement.state(), 3)) return;
        carried.shrink(1);
        if (carried.isEmpty()) worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        worker.swing(InteractionHand.MAIN_HAND);
        WaterfrontState next = state.withBuildStep(step + 1);
        data.replace(next);
        if (next.buildStep() >= plan.size()) SettlementService.broadcast(level.getServer(), settlement);
    }

    private static void loadLocalWood(ServerLevel level, OutpostRecord outpost, Villager worker) {
        BlockPos stock = outpost.stockpile();
        if (!level.hasChunkAt(stock)) {
            worker.getNavigation().stop();
            return;
        }
        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D)
                > INTERACTION_RANGE_SQR) {
            worker.getNavigation().moveTo(stock.getX() + 0.5D, stock.getY(), stock.getZ() + 0.5D, 0.8D);
            return;
        }
        if (!(level.getBlockEntity(stock) instanceof Container container)) return;
        ItemStack extracted = extractWood(container, LOCAL_HAUL_BATCH);
        if (!extracted.isEmpty()) worker.setItemSlot(EquipmentSlot.MAINHAND, extracted);
    }

    private static ItemStack extractWood(Container container, int maxCount) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack current = container.getItem(slot);
            if (current.isEmpty() || !SettlementInventory.isWood(current)) continue;
            int take = Math.min(maxCount, current.getCount());
            ItemStack result = current.copyWithCount(take);
            current.shrink(take);
            if (current.isEmpty()) container.setItem(slot, ItemStack.EMPTY);
            container.setChanged();
            return result;
        }
        return ItemStack.EMPTY;
    }

    private static List<Placement> createPlan(WaterfrontState state) {
        List<Placement> plan = new ArrayList<>();
        add(plan, state, 1, 0, 0, Blocks.SPRUCE_SLAB.defaultBlockState());
        add(plan, state, 2, 0, 0, Blocks.SPRUCE_SLAB.defaultBlockState());
        for (int forward = 3; forward <= 4; forward++) {
            for (int side = -1; side <= 1; side++) {
                add(plan, state, forward, side, 0, Blocks.SPRUCE_SLAB.defaultBlockState());
            }
        }
        add(plan, state, 3, 1, 1, Blocks.BARREL.defaultBlockState());
        add(plan, state, 3, -1, 1, Blocks.OAK_FENCE.defaultBlockState());
        add(plan, state, 4, -1, 1, Blocks.OAK_FENCE.defaultBlockState());
        add(plan, state, 4, 1, 1, Blocks.OAK_FENCE.defaultBlockState());
        return List.copyOf(plan);
    }

    private static void add(List<Placement> plan, WaterfrontState state,
                            int forward, int side, int y, BlockState blockState) {
        plan.add(new Placement(local(state, forward, side, y), blockState));
    }

    private static BlockPos local(WaterfrontState state, int forward, int side, int y) {
        int x = state.bankX() + state.directionX() * forward - state.directionZ() * side;
        int z = state.bankZ() + state.directionZ() * forward + state.directionX() * side;
        return new BlockPos(x, state.bankY() + y, z);
    }

    private static BlockPos tradeCrate(WaterfrontState state) {
        return local(state, 3, 1, 1);
    }

    private static BlockPos traderStation(WaterfrontState state) {
        return local(state, 4, 0, 1);
    }

    private static Villager ensureTrader(ServerLevel level, OutpostRecord outpost, WaterfrontState state) {
        String assignment = WATER_TRADER_ASSIGNMENT_PREFIX + outpost.id();
        BlockPos station = traderStation(state);
        AABB area = new AABB(station).inflate(ENTITY_SEARCH_RADIUS, 24.0D, ENTITY_SEARCH_RADIUS);
        List<Villager> traders = level.getEntitiesOfClass(Villager.class, area,
                villager -> villager.entityTags().contains(WATER_TRADER_TAG)
                        && villager.entityTags().contains(assignment));
        traders.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        if (!traders.isEmpty()) {
            Villager active = traders.getFirst();
            active.setNoAi(false);
            for (int i = 1; i < traders.size(); i++) {
                Villager duplicate = traders.get(i);
                duplicate.getNavigation().stop();
                duplicate.setNoAi(true);
                duplicate.setInvulnerable(true);
            }
            return active;
        }
        if (!entityAreaLoaded(level, area) || !level.hasChunkAt(station)) return null;

        Villager trader = new Villager(EntityTypes.VILLAGER, level);
        trader.setPos(station.getX() + 0.5D, station.getY(), station.getZ() + 0.5D);
        trader.setCustomName(Component.literal("수변 상인 #" + outpost.id()));
        trader.setCustomNameVisible(true);
        trader.setPersistenceRequired();
        trader.setNoAi(false);
        trader.setInvulnerable(true);
        trader.addTag(WATER_TRADER_TAG);
        trader.addTag(assignment);
        return level.addFreshEntity(trader) ? trader : null;
    }

    private static boolean entityAreaLoaded(ServerLevel level, AABB area) {
        int minChunkX = Math.floorDiv((int) Math.floor(area.minX), 16);
        int maxChunkX = Math.floorDiv((int) Math.floor(Math.nextDown(area.maxX)), 16);
        int minChunkZ = Math.floorDiv((int) Math.floor(area.minZ), 16);
        int maxChunkZ = Math.floorDiv((int) Math.floor(Math.nextDown(area.maxZ)), 16);
        int probeY = (int) Math.floor((area.minY + area.maxY) * 0.5D);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunkAt(new BlockPos(chunkX * 16 + 8, probeY, chunkZ * 16 + 8))) return false;
            }
        }
        return true;
    }

    private static boolean tradeDue(MinecraftServer server, OutpostRecord outpost) {
        int salt = Math.floorMod(outpost.id() * 37, TRADE_PERIOD_TICKS);
        return Math.floorMod(server.getTickCount() + salt, TRADE_PERIOD_TICKS) < 20;
    }

    private static void tradeOne(Container container, Villager trader) {
        if (countFish(container) < TRADE_FISH_COST || !hasEmeraldRoom(container)) return;
        removeFish(container, TRADE_FISH_COST);
        ItemStack remainder = SettlementInventory.insert(container, new ItemStack(Items.EMERALD, 1));
        if (!remainder.isEmpty()) return;
        container.setChanged();
        trader.swing(InteractionHand.MAIN_HAND);
    }

    private static int countFish(Container container) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(Items.COD) || stack.is(Items.SALMON)) total += stack.getCount();
        }
        return total;
    }

    private static void removeFish(Container container, int amount) {
        int left = amount;
        for (int slot = 0; slot < container.getContainerSize() && left > 0; slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.is(Items.COD) && !stack.is(Items.SALMON)) continue;
            int take = Math.min(left, stack.getCount());
            stack.shrink(take);
            if (stack.isEmpty()) container.setItem(slot, ItemStack.EMPTY);
            left -= take;
        }
        container.setChanged();
    }

    private static boolean hasEmeraldRoom(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) return true;
            if (stack.is(Items.EMERALD) && stack.getCount() < stack.getMaxStackSize()) return true;
        }
        return false;
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

    /** Persisted pier/trade blocks are infrastructure and cannot become free recoverable materials. */
    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (level != server.overworld()) return;
        SettlementData settlement = SettlementData.get(server);
        if (!settlement.founded()) return;
        BlockPos pos = event.getPos();
        for (WaterfrontState state : SettlementWaterfrontData.get(server).states()) {
            List<Placement> plan = createPlan(state);
            int built = Math.min(state.buildStep(), plan.size());
            for (int i = 0; i < built; i++) {
                Placement placement = plan.get(i);
                if (!pos.equals(placement.pos())) continue;
                if (!level.getBlockState(pos).is(placement.state().getBlock())) continue;
                event.setCanceled(true);
                event.setNotifyClient(true);
                return;
            }
        }
    }
}
