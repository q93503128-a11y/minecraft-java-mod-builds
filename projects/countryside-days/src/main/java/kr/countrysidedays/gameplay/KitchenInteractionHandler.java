package kr.countrysidedays.gameplay;

import kr.countrysidedays.registry.ModBlocks;
import kr.countrysidedays.registry.ModItems;
import kr.countrysidedays.world.CountrysideWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

public final class KitchenInteractionHandler {
    private KitchenInteractionHandler() {
    }

    public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        if (event.isCanceled() || event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK) return;
        if (!event.getLevel().getBlockState(event.getPos()).is(ModBlocks.COUNTRY_KITCHEN_COUNTER.get())) return;

        ItemStack heldItem = event.getItemStack();
        boolean supportedInteraction = heldItem.isEmpty()
                || heldItem.is(ModItems.WILD_HERB.get())
                || heldItem.is(ModItems.RIVER_FISH.get());
        if (!supportedInteraction) return;

        if (event.getLevel() instanceof ServerLevel serverLevel) {
            Player player = event.getPlayer();
            if (player != null) handleServerInteraction(serverLevel, event.getPos(), player, heldItem);
        }

        event.cancelWithResult(InteractionResult.SUCCESS_SERVER);
    }

    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!event.getState().is(ModBlocks.COUNTRY_KITCHEN_COUNTER.get())) return;
        CountrysideWorldData.get(serverLevel.getServer()).removeKitchenState(event.getPos());
    }

    private static void handleServerInteraction(ServerLevel level, BlockPos pos, Player player, ItemStack heldItem) {
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());

        if (heldItem.is(ModItems.WILD_HERB.get())) {
            if (data.addHerbPreparation(pos)) {
                consumeOneUnlessCreative(player, heldItem);
                player.sendOverlayMessage(Component.translatable("message.countrysidedays.herb_prepared"));
            } else {
                player.sendOverlayMessage(Component.translatable("message.countrysidedays.herb_already_prepared"));
            }
            return;
        }

        if (heldItem.is(ModItems.RIVER_FISH.get())) {
            if (!data.consumeHerbPreparation(pos)) {
                player.sendOverlayMessage(Component.translatable("message.countrysidedays.need_herb_first"));
                return;
            }
            consumeOneUnlessCreative(player, heldItem);
            ItemStack result = ModItems.COUNTRY_STEW.get().getDefaultInstance();
            if (!player.addItem(result)) player.drop(result, false);
            data.recordPreparedMeal(player.getUUID());
            int prepared = data.estate(player.getUUID())
                    .map(CountrysideWorldData.PlayerEstate::mealsPrepared)
                    .orElse(data.mealsPrepared());
            player.sendOverlayMessage(Component.translatable(
                    "message.countrysidedays.stew_completed",
                    prepared
            ));
            return;
        }

        player.sendOverlayMessage(Component.translatable(
                data.hasHerbPreparation(pos)
                        ? "message.countrysidedays.counter_waiting_for_fish"
                        : "message.countrysidedays.counter_waiting_for_herb"
        ));
    }

    private static void consumeOneUnlessCreative(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) stack.shrink(1);
    }
}
