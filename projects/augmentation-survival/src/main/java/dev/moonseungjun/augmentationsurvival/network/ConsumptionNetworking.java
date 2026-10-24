package dev.moonseungjun.augmentationsurvival.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Common registration for the consumption/fishing client prediction snapshot. */
public final class ConsumptionNetworking {
    private ConsumptionNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(
                ConsumptionStatePayload.TYPE,
                ConsumptionStatePayload.CODEC
        );
    }
}
