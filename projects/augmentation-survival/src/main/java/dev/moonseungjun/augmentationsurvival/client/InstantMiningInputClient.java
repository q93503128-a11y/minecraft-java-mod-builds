package dev.moonseungjun.augmentationsurvival.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.augmentationsurvival.network.InstantMiningStatePayload;
import dev.moonseungjun.augmentationsurvival.network.InstantMiningTogglePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Dedicated rebindable Instant Mining toggle. The server remains authoritative. */
public final class InstantMiningInputClient {
    private static final KeyMapping TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.augmentation_survival.instant_mining",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            ActiveAbilityInputClient.CATEGORY
    ));
    private static volatile boolean enabled;

    private InstantMiningInputClient() { }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                InstantMiningStatePayload.TYPE,
                (payload, context) -> enabled = payload.enabled()
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> enabled = false);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.player.isDeadOrDying() || client.gui.screen() != null) {
                while (TOGGLE_KEY.consumeClick()) {
                    // Discard stale clicks while gameplay input is unavailable.
                }
                return;
            }
            while (TOGGLE_KEY.consumeClick()) {
                ClientPlayNetworking.send(new InstantMiningTogglePayload());
            }
        });
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
