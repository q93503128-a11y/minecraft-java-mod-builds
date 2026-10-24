package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.augment.resource.ClientConsumptionState;
import dev.moonseungjun.augmentationsurvival.network.ConsumptionStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Applies server-owned food-use prediction state to the local client. */
public final class ConsumptionClientNetworking {
    private ConsumptionClientNetworking() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                ConsumptionStatePayload.TYPE,
                (payload, context) -> ClientConsumptionState.apply(
                        payload.fastEatingLevel(),
                        payload.eatWhileWalkingLevel(),
                        payload.reserveMealLevel(),
                        payload.reservePoints()
                )
        );
    }
}
