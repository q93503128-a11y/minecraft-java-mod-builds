package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Common networking registration for movement requests. */
public final class MovementNetworking {
    private MovementNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(
                AirJumpRequestPayload.TYPE,
                AirJumpRequestPayload.CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(
                AirJumpRequestPayload.TYPE,
                (payload, context) -> {
                    if (payload.requested()) {
                        MovementAugmentService.tryAirJump(context.player());
                    }
                }
        );
    }
}
