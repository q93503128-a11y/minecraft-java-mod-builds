package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import dev.moonseungjun.augmentationsurvival.network.ActiveLoadoutStatePayload;
import dev.moonseungjun.augmentationsurvival.network.ActivePresentationPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ActiveLoadoutClientNetworking {
    private ActiveLoadoutClientNetworking() { }

    public static void initialize() {
        AugmentPresentationCatalog.initialize();
        ClientPlayNetworking.registerGlobalReceiver(
                ActiveLoadoutStatePayload.TYPE,
                (payload, context) -> ClientActiveLoadoutState.apply(
                        payload.slotIds(),
                        payload.cooldownTicks(),
                        payload.maxCharges(),
                        payload.charge0Ticks(),
                        payload.charge1Ticks(),
                        payload.availableIds()
                )
        );
        ClientPlayNetworking.registerGlobalReceiver(
                ActivePresentationPayload.TYPE,
                (payload, context) -> ClientActivePresentationState.apply(payload)
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientActiveLoadoutState.clear();
            ClientActivePresentationState.clear();
        });
        AugmentProgressClientNetworking.initialize();
        ProtectionHudClientNetworking.initialize();
    }
}
