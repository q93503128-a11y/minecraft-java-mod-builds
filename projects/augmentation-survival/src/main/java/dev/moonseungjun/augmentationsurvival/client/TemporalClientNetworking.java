package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ClientTemporalState;
import dev.moonseungjun.augmentationsurvival.augment.active.TemporalStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TemporalClientNetworking {
    private static final String TIME_ACCELERATION_ID = AugmentationSurvival.MOD_ID + ":time_acceleration";

    private TemporalClientNetworking() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                TemporalStatePayload.TYPE,
                (payload, context) -> {
                    ClientTemporalState.apply(payload.timeAccelerationActive());
                    ClientActivePresentationState.setExternalActive(
                            TIME_ACCELERATION_ID,
                            payload.timeAccelerationActive()
                    );
                }
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientTemporalState.clear();
            ClientActivePresentationState.setExternalActive(TIME_ACCELERATION_ID, false);
        });
    }
}
