package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.network.AugmentProgressStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class AugmentProgressClientNetworking {
    private AugmentProgressClientNetworking() { }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                AugmentProgressStatePayload.TYPE,
                (payload, context) -> ClientAugmentProgressState.apply(payload.ids(), payload.levels())
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientAugmentProgressState.clear());
    }
}
