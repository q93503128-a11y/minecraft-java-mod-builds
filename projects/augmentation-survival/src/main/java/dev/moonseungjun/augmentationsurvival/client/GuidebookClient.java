package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.client.screen.AugmentGuidebookScreen;
import dev.moonseungjun.augmentationsurvival.guide.GuidebookItems;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

/** Opens the local recipe guide when the guidebook is used. */
public final class GuidebookClient {
    private GuidebookClient() {
    }

    public static void initialize() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!level.isClientSide()) return InteractionResult.PASS;
            if (!player.getItemInHand(hand).is(GuidebookItems.GUIDEBOOK)) return InteractionResult.PASS;
            Minecraft.getInstance().gui.setScreen(new AugmentGuidebookScreen());
            return InteractionResult.SUCCESS;
        });
    }
}
