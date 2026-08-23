package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.compat.ExternalContentTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.List;

/**
 * Physical player-intent market trading.
 *
 * Only items placed in the market's dedicated barrel are eligible. The ordinary settlement storage
 * is never auto-sold. One visiting merchant performs a readable sale at a time and pays physical
 * emerald ItemStacks back into the same barrel. Missing companion tags simply mean no matching goods.
 */
public final class SettlementMarketService {
    public static final String MARKET_TRADER_TAG = "frontier_settlement_market_visitor";
    public static final String MARKET_ASSIGNMENT_PREFIX = "frontier_settlement_market_";

    private static final int TRADE_PERIOD_TICKS = 100;
    private static final double CRATE_REACHED_SQR = 9.0D;

    private SettlementMarketService() {}

    public static void tick(MinecraftServer server, SettlementData data) {
        if (server.getTickCount() % 10 != 0) return;
        ServerLevel level = server.overworld();
        for (BuildingRecord market : data.buildings()) {
            if (market.buildingType() != BuildingType.MARKET) continue;
            BlockPos crate = MarketLayout.tradeCrate(market);
            if (!level.hasChunkAt(market.workCenter()) || !level.hasChunkAt(crate)) continue;
            if (!(level.getBlockEntity(crate) instanceof Container container)) continue;

            Villager trader = ensureTrader(level, market);
            if (trader == null) continue;
            if (trader.distanceToSqr(crate.getX() + 0.5D, crate.getY() + 0.5D, crate.getZ() + 0.5D) > CRATE_REACHED_SQR) {
                trader.getNavigation().moveTo(crate.getX() + 0.5D, crate.getY(), crate.getZ() + 0.5D, 0.7D);
                continue;
            }
            if (!workDue(server, market)) continue;
            tradeOne(container, trader);
        }
    }

    private static boolean workDue(MinecraftServer server, BuildingRecord market) {
        int salt = Math.floorMod(market.originX() * 31 + market.originZ() * 17, TRADE_PERIOD_TICKS);
        return Math.floorMod(server.getTickCount() + salt, TRADE_PERIOD_TICKS) < 10;
    }

    private static void tradeOne(Container container, Villager trader) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack goods = container.getItem(slot);
            if (goods.isEmpty() || !goods.is(ExternalContentTags.EXPEDITION_RELICS)) continue;
            int payout = tradeValue(goods);
            if (payout <= 0 || !hasEmeraldRoom(container, slot, goods.getCount() == 1, payout)) return;

            ItemStack sold = goods.copyWithCount(1);
            goods.shrink(1);
            if (goods.isEmpty()) container.setItem(slot, ItemStack.EMPTY);
            ItemStack remainder = SettlementInventory.insert(container, new ItemStack(Items.EMERALD, payout));
            if (!remainder.isEmpty()) {
                int inserted = payout - remainder.getCount();
                if (inserted > 0) removeEmeralds(container, inserted);
                SettlementInventory.insert(container, sold);
                container.setChanged();
                return;
            }
            container.setChanged();
            trader.swing(InteractionHand.MAIN_HAND);
            return;
        }
    }

    private static int tradeValue(ItemStack stack) {
        if (stack.is(Items.HEAVY_CORE)) return 24;
        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return 16;
        if (stack.is(Items.HEART_OF_THE_SEA)) return 12;
        if (stack.is(Items.OMINOUS_TRIAL_KEY)) return 10;
        if (stack.is(Items.TRIAL_KEY)) return 6;
        if (stack.is(Items.OMINOUS_BOTTLE)) return 5;
        if (stack.is(Items.ECHO_SHARD)) return 4;
        return 6;
    }

    private static boolean hasEmeraldRoom(Container container, int sourceSlot, boolean sourceWillEmpty, int payout) {
        int emeraldStackSize = new ItemStack(Items.EMERALD).getMaxStackSize();
        int room = sourceWillEmpty ? emeraldStackSize : 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (slot == sourceSlot && sourceWillEmpty) continue;
            ItemStack current = container.getItem(slot);
            if (current.isEmpty()) {
                room += emeraldStackSize;
            } else if (current.is(Items.EMERALD)) {
                room += current.getMaxStackSize() - current.getCount();
            }
            if (room >= payout) return true;
        }
        return room >= payout;
    }

    private static void removeEmeralds(Container container, int amount) {
        int left = amount;
        for (int slot = container.getContainerSize() - 1; slot >= 0 && left > 0; slot--) {
            ItemStack current = container.getItem(slot);
            if (!current.is(Items.EMERALD)) continue;
            int take = Math.min(left, current.getCount());
            current.shrink(take);
            if (current.isEmpty()) container.setItem(slot, ItemStack.EMPTY);
            left -= take;
        }
    }

    private static Villager ensureTrader(ServerLevel level, BuildingRecord market) {
        String assignment = assignmentTag(market);
        AABB area = new AABB(market.workCenter()).inflate(48.0D, 24.0D, 48.0D);
        List<Villager> assigned = level.getEntitiesOfClass(Villager.class, area,
                villager -> villager.entityTags().contains(MARKET_TRADER_TAG)
                        && villager.entityTags().contains(assignment));
        if (!assigned.isEmpty()) return assigned.getFirst();

        Villager trader = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = market.workCenter().above();
        trader.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        trader.setCustomName(Component.literal("방문 상인"));
        trader.setCustomNameVisible(true);
        trader.setPersistenceRequired();
        trader.setNoAi(false);
        trader.addTag(MARKET_TRADER_TAG);
        trader.addTag(assignment);
        level.addFreshEntity(trader);
        return trader;
    }

    private static String assignmentTag(BuildingRecord market) {
        return MARKET_ASSIGNMENT_PREFIX + encode(market.originX()) + "_" + encode(market.originZ());
    }

    private static String encode(int value) {
        return value < 0 ? "n" + Math.abs((long) value) : "p" + value;
    }

    /** The functional trade barrel is settlement infrastructure, not a free recoverable blueprint block. */
    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (level != server.overworld()) return;
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return;
        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(Blocks.BARREL)) return;
        for (BuildingRecord market : data.buildings()) {
            if (market.buildingType() == BuildingType.MARKET && pos.equals(MarketLayout.tradeCrate(market))) {
                event.setCanceled(true);
                event.setNotifyClient(true);
                return;
            }
        }
    }
}
