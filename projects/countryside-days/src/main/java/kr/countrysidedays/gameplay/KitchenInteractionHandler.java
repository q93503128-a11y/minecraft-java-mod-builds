package kr.countrysidedays.gameplay;

import kr.countrysidedays.registry.ModBlocks;
import kr.countrysidedays.registry.ModItems;
import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.PlayerEstateLayout;
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
        CountrysideWorldData.PlayerEstate restaurantEstate = SharedRestaurantAccess
                .restaurantEstate(data)
                .orElse(null);
        boolean sharedCounter = restaurantEstate != null
                && pos.equals(PlayerEstateLayout.kitchenCounter(restaurantEstate.originPos()));
        if (!sharedCounter || !SharedRestaurantAccess.isStaff(data, player.getUUID())) {
            player.sendOverlayMessage(Component.translatable("message.countrysidedays.restaurant_staff_only"));
            return;
        }

        if (heldItem.isEmpty()) {
            if (!RuralNpcManager.isRestaurantBusinessTime(level)) {
                player.sendOverlayMessage(Component.translatable("message.countrysidedays.restaurant_closed"));
                return;
            }
            boolean open = SharedRestaurantAccess.toggleOpen(data, player.getUUID()).orElse(false);
            player.sendSystemMessage(Component.translatable(
                    open
                            ? "message.countrysidedays.restaurant_opened"
                            : "message.countrysidedays.restaurant_closed_by_owner"
            ));
            return;
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
            if (!player.addItem(result)) player.drop(result, false);
            SharedRestaurantAccess.recordPreparedMeal(data);
            int prepared = SharedRestaurantAccess.restaurantEstate(data)
                    .map(CountrysideWorldData.PlayerEstate::mealsPrepared)
                    .orElse(data.mealsPrepared());
            player.sendOverlayMessage(Component.translatable(
                    "message.countrysidedays.stew_completed",
                    prepared
            ));
        }
    }

    private static void consumeOneUnlessCreative(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) stack.shrink(1);
    }
}
