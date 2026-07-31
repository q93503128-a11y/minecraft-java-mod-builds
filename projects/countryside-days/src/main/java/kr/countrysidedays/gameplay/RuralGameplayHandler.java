package kr.countrysidedays.gameplay;

import kr.countrysidedays.registry.ModItems;
import kr.countrysidedays.world.CountrysideWorldData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

public final class RuralGameplayHandler {
    private static final float WILD_HERB_CHANCE = 0.32F;
    private static final float RIVER_FISH_CHANCE = 0.45F;

    private RuralGameplayHandler() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CountrysideWorldData data = CountrysideWorldData.get(player.serverLevel().getServer());
        if (!data.claimStarterKit()) {
            return;
        }

        giveOrDrop(player, ModItems.COUNTRY_KITCHEN_COUNTER.get().getDefaultInstance());
        giveOrDrop(player, ModItems.RECIPE_NOTEBOOK.get().getDefaultInstance());
        giveOrDrop(player, Items.FISHING_ROD.getDefaultInstance());
        player.sendSystemMessage(Component.translatable("message.countrysidedays.starter_kit"));
    }

    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.isCanceled() || !(event.getBreaker() instanceof Player player)) {
            return;
        }
        if (!isForagePlant(event.getState())) {
            return;
        }
        if (event.getLevel().getRandom().nextFloat() >= WILD_HERB_CHANCE) {
            return;
        }

        giveOrDrop(player, ModItems.WILD_HERB.get().getDefaultInstance());
        player.sendOverlayMessage(Component.translatable("message.countrysidedays.wild_herb_found"));
    }

    public static void onItemFished(ItemFishedEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.serverLevel().getRandom().nextFloat() >= RIVER_FISH_CHANCE) {
            return;
        }

        giveOrDrop(player, ModItems.RIVER_FISH.get().getDefaultInstance());
        player.sendOverlayMessage(Component.translatable("message.countrysidedays.river_fish_caught"));
    }

    public static boolean isForagePlant(BlockState state) {
        return state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.LARGE_FERN);
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
