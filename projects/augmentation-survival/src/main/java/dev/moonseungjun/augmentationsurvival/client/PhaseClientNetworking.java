package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.augment.active.ClientPhaseState;
import dev.moonseungjun.augmentationsurvival.augment.active.PhaseStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Applies the server-owned Phase Shift state to local movement prediction. */
public final class PhaseClientNetworking {
    private PhaseClientNetworking() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                PhaseStatePayload.TYPE,
                (payload, context) -> ClientPhaseState.apply(payload.phaseShiftActive())
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientPhaseState.clear());
    }
}
