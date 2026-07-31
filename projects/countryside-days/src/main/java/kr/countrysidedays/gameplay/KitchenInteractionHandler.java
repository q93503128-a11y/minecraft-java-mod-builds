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
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK) {
            return;
        }
        if (!event.getLevel().getBlockState(event.getPos()).is(ModBlocks.COUNTRY_KITCHEN_COUNTER.get())) {
            return;
        }

        ItemStack heldItem = event.getItemStack();
        boolean supportedInteraction = heldItem.isEmpty()
                || heldItem.is(ModItems.WILD_HERB.get())
                || heldItem.is(ModItems.RIVER_FISH.get())
                || heldItem.is(ModItems.RECIPE_NOTEBOOK.get());
        if (!supportedInteraction) {
            return;
        }

        if (event.getLevel() instanceof ServerLevel serverLevel) {
            Player player = event.getPlayer();
            if (player != null) {
                handleServerInteraction(serverLevel, event.getPos(), player, heldItem);
            }
        }

        event.cancelWithResult(InteractionResult.SUCCESS_SERVER);
    }

    public static void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!event.getState().is(ModBlocks.COUNTRY_KITCHEN_COUNTER.get())) {
            return;
        }
        CountrysideWorldData.get(serverLevel.getServer()).removeKitchenState(event.getPos());
    }

    private static void handleServerInteraction(ServerLevel level, BlockPos pos, Player player, ItemStack heldItem) {
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        boolean firstAnchor = data.claimRestaurantAnchor(pos);
        if (firstAnchor) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.restaurant_anchor_set"));
        }

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
            if (!player.addItem(result)) {
                player.drop(result, false);
            }
            data.recordPreparedMeal();
            player.sendOverlayMessage(
                    Component.translatable("message.countrysidedays.stew_completed", data.mealsPrepared())
            );
            return;
        }

        if (heldItem.is(ModItems.RECIPE_NOTEBOOK.get())) {
            BlockPos anchor = data.restaurantAnchor().orElse(pos);
            player.sendSystemMessage(
                    Component.translatable(
                            "message.countrysidedays.notebook_status",
                            anchor.getX(),
                            anchor.getY(),
                            anchor.getZ(),
                            data.mealsPrepared()
                    )
            );
            return;
        }

        String statusKey = data.hasHerbPreparation(pos)
                ? "message.countrysidedays.counter_waiting_for_fish"
                : "message.countrysidedays.counter_waiting_for_herb";
        player.sendOverlayMessage(Component.translatable(statusKey));
    }

    private static void consumeOneUnlessCreative(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
