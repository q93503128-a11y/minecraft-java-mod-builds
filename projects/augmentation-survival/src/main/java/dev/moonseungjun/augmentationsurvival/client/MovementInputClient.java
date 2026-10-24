package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.network.AirJumpRequestPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Detects a rising edge on the vanilla jump key and asks the server to consume an air jump. */
public final class MovementInputClient {
    private static boolean jumpWasDown;

    private MovementInputClient() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                jumpWasDown = false;
                return;
            }

            boolean jumpDown = client.options.keyJump.isDown();
            if (client.screen == null && jumpDown && !jumpWasDown) {
                ClientPlayNetworking.send(new AirJumpRequestPayload(true));
            }
            jumpWasDown = jumpDown;
        });
    }
}
