package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.network.InstantMiningStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Server-confirmed local mirror used only for mining prediction and UI state. */
public final class ClientInstantMiningState {
    private static volatile boolean enabled;

    private ClientInstantMiningState() { }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                InstantMiningStatePayload.TYPE,
                (payload, context) -> enabled = payload.enabled()
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> enabled = false);
    }

    public static boolean enabled() {
        return enabled;
    }
}
