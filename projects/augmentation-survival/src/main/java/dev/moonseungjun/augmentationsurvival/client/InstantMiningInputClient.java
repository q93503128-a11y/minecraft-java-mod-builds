package dev.moonseungjun.augmentationsurvival.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.augmentationsurvival.network.InstantMiningTogglePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Dedicated rebindable Instant Mining toggle. Ownership remains server-authoritative. */
public final class InstantMiningInputClient {
    private static final KeyMapping TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.augmentation_survival.instant_mining",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            ActiveAbilityInputClient.CATEGORY
    ));

    private InstantMiningInputClient() { }

    public static void initialize() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || client.player.isDeadOrDying() || client.gui.screen() != null) {
                drain();
                return;
            }
            while (TOGGLE_KEY.consumeClick()) {
                ClientPlayNetworking.send(new InstantMiningTogglePayload(true));
            }
        });
    }

    private static void drain() {
        while (TOGGLE_KEY.consumeClick()) {
            // Discard stale toggles while a screen is open or the local player cannot act.
        }
    }
}
