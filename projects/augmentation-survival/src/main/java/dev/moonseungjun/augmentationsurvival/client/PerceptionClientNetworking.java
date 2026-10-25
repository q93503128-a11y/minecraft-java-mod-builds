package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.network.PerceptionStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class PerceptionClientNetworking {
    private PerceptionClientNetworking() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                PerceptionStatePayload.TYPE,
                (payload, context) -> ClientPerceptionState.apply(
                        payload.oreSenseLevel(),
                        payload.enchantRefreshOwned(),
                        payload.chestPositions(),
                        payload.dangerEntityIds(),
                        payload.reviveMarkerPositions()
                )
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientPerceptionState.clear());
        ClientPerceptionState.initialize();
    }
}
