package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.network.ProtectionHudStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ProtectionHudClientNetworking {
    private ProtectionHudClientNetworking() { }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                ProtectionHudStatePayload.TYPE,
                (payload, context) -> ClientProtectionHudState.apply(
                        payload.shield(), payload.shieldCapacity(),
                        payload.augmentAbsorption(), payload.augmentAbsorptionCapacity()
                )
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientProtectionHudState.clear());
    }
}
