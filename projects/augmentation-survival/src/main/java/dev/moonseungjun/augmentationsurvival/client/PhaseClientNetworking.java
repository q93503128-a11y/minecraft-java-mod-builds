package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ClientPhaseState;
import dev.moonseungjun.augmentationsurvival.augment.active.PhaseStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Applies the server-owned Phase Shift state to local movement prediction and presentation. */
public final class PhaseClientNetworking {
    private static final String PHASE_SHIFT_ID = AugmentationSurvival.MOD_ID + ":phase_shift";

    private PhaseClientNetworking() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                PhaseStatePayload.TYPE,
                (payload, context) -> {
                    ClientPhaseState.apply(payload.phaseShiftActive());
                    ClientActivePresentationState.setExternalActive(
                            PHASE_SHIFT_ID,
                            payload.phaseShiftActive()
                    );
                }
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientPhaseState.clear();
            ClientActivePresentationState.setExternalActive(PHASE_SHIFT_ID, false);
        });
    }
}
