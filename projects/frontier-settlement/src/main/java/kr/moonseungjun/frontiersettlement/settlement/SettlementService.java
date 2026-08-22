package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.network.SettlementNetwork;
import kr.moonseungjun.frontiersettlement.network.SettlementSnapshotPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class SettlementService {
    private static final String BUILDER_TAG = "frontier_settlement_builder";

    private SettlementService() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return;
        if (refreshResources(server, data)) broadcast(server, data);
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SettlementData data = SettlementData.get(player.getServer());
        if (data.founded()) refreshResources(player.getServer(), data);
        sync(player, data);
    }

    public static boolean found(ServerPlayer founder) {
        MinecraftServer server = founder.getServer();
        SettlementData data = SettlementData.get(server);
        if (data.founded()) return false;
        if (founder.level() != server.overworld()) return false;

        ServerLevel level = server.overworld();
        BlockPos center = founder.blockPosition();
        BlockPos stockpile = findStockpilePosition(level, center);
        if (stockpile == null) return false;

        level.setBlock(stockpile, Blocks.BARREL.defaultBlockState(), 3);
        spawnBuilder(level, center.offset(1, 0, 1));
        data.found(center, stockpile);
        refreshResources(server, data);
        broadcast(server, data);
        return true;
    }

    public static boolean refreshResources(MinecraftServer server, SettlementData data) {
        if (!data.founded()) return false;
        ServerLevel level = server.overworld();
        BlockPos pos = data.stockpilePos();
        if (!level.hasChunkAt(pos)) return false;
        if (!(level.getBlockEntity(pos) instanceof Container container)) {
            return data.updateResources(SettlementResources.ZERO);
        }

        long wood = 0L;
        long stone = 0L;
        long metal = 0L;
        long food = 0L;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            long count = stack.getCount();
            if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS)) {
                wood += count;
            } else if (isStone(stack)) {
                stone += count;
            } else if (isMetal(stack)) {
                metal += count;
            } else if (stack.get(DataComponents.FOOD) != null) {
                food += count;
            }
        }
        return data.updateResources(new SettlementResources(wood, stone, metal, food));
    }

    private static boolean isStone(ItemStack stack) {
        return stack.is(Items.STONE)
                || stack.is(Items.COBBLESTONE)
                || stack.is(Items.DEEPSLATE)
                || stack.is(Items.COBBLED_DEEPSLATE)
                || stack.is(Items.ANDESITE)
                || stack.is(Items.DIORITE)
                || stack.is(Items.GRANITE);
    }

    private static boolean isMetal(ItemStack stack) {
        return stack.is(Items.IRON_INGOT)
                || stack.is(Items.RAW_IRON)
                || stack.is(Items.COPPER_INGOT)
                || stack.is(Items.RAW_COPPER)
                || stack.is(Items.GOLD_INGOT)
                || stack.is(Items.RAW_GOLD);
    }

    private static BlockPos findStockpilePosition(ServerLevel level, BlockPos center) {
        BlockPos[] candidates = new BlockPos[] {
                center.offset(2, 0, 0), center.offset(-2, 0, 0),
                center.offset(0, 0, 2), center.offset(0, 0, -2),
                center.offset(2, 0, 2), center.offset(-2, 0, 2),
                center.offset(2, 0, -2), center.offset(-2, 0, -2)
        };
        for (BlockPos candidate : candidates) {
            if (level.getBlockState(candidate).isAir()) return candidate;
        }
        return null;
    }

    private static void spawnBuilder(ServerLevel level, BlockPos pos) {
        Villager builder = new Villager(EntityType.VILLAGER, level);
        builder.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        builder.setCustomName(net.minecraft.network.chat.Component.literal("건설 주민"));
        builder.setCustomNameVisible(true);
        builder.setPersistenceRequired();
        builder.setNoAi(true);
        builder.addTag(BUILDER_TAG);
        level.addFreshEntity(builder);
    }

    public static void sync(ServerPlayer player, SettlementData data) {
        SettlementResources r = data.resources();
        SettlementNetwork.sendSnapshot(player, new SettlementSnapshotPayload(
                data.founded(), r.wood(), r.stone(), r.metal(), r.food(), data.population()));
    }

    public static void broadcast(MinecraftServer server, SettlementData data) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sync(player, data);
    }
}
