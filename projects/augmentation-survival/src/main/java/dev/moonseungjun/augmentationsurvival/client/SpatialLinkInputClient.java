package dev.moonseungjun.augmentationsurvival.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.augmentationsurvival.network.SpatialLinkActionPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Rebindable dedicated key for the permanent spatial-link network. */
public final class SpatialLinkInputClient {
    private static final KeyMapping OPEN_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.augmentation_survival.spatial_link",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            ActiveAbilityInputClient.CATEGORY
    ));
    private static boolean wasDown;

    private SpatialLinkInputClient() { }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean down = client.player != null && client.gui.screen() == null && OPEN_KEY.isDown();
            if (down && !wasDown) {
                ClientPlayNetworking.send(new SpatialLinkActionPayload(
                        SpatialLinkActionPayload.Action.OPEN, "", ""
                ));
            }
            wasDown = down;
        });
    }
}
