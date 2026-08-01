package kr.moonseungjun.arcanecircle.gameplay;

import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class StarterKitHandler {
    private static final String STARTER_KIT_TAG = "arcanecircle_test_kit";
    private StarterKitHandler() {}

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.addTag(STARTER_KIT_TAG)) return;
        give(player, new ItemStack(ModItems.MAGIC_CIRCLE.get(), 3));
        give(player, Items.BLAZE_POWDER.getDefaultInstance());
        give(player, Items.REDSTONE.getDefaultInstance());
        give(player, Items.GUNPOWDER.getDefaultInstance());
        give(player, Items.SNOWBALL.getDefaultInstance());
        give(player, Items.QUARTZ.getDefaultInstance());
        give(player, Items.SUGAR.getDefaultInstance());
        give(player, Items.ENDER_PEARL.getDefaultInstance());
        give(player, Items.AMETHYST_SHARD.getDefaultInstance());
        give(player, Items.GLOWSTONE_DUST.getDefaultInstance());
        player.sendSystemMessage(Component.translatable("message.arcanecircle.starter_kit"));
        player.sendSystemMessage(Component.translatable("message.arcanecircle.instructions"));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) player.drop(stack, false);
    }
}
