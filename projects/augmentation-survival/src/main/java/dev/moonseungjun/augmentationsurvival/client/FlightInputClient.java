package dev.moonseungjun.augmentationsurvival.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.augmentationsurvival.network.FlightTogglePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Dedicated rebindable flight toggle. Ownership is always validated on the server. */
public final class FlightInputClient {
    private static final KeyMapping FLIGHT_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.augmentation_survival.flight", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V,
            ActiveAbilityInputClient.CATEGORY
    ));

    private FlightInputClient() { }

    public static void initialize() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || client.player.isDeadOrDying() || client.gui.screen() != null) {
                drain();
                return;
            }
            while (FLIGHT_KEY.consumeClick()) {
                ClientPlayNetworking.send(new FlightTogglePayload(true));
            }
        });
    }

    private static void drain() {
        while (FLIGHT_KEY.consumeClick()) {
            // Discard stale toggles while a screen is open or the local player cannot act.
        }
    }
}
