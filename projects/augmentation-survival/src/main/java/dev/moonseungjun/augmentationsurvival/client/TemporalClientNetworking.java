package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.augment.active.ClientTemporalState;
import dev.moonseungjun.augmentationsurvival.augment.active.TemporalStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TemporalClientNetworking {
    private TemporalClientNetworking() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                TemporalStatePayload.TYPE,
                (payload, context) -> ClientTemporalState.apply(payload.timeAccelerationActive())
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientTemporalState.clear());
    }
}
