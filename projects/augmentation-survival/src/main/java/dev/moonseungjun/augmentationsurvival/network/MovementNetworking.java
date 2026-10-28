package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Common networking registration for movement requests and held-input state. */
public final class MovementNetworking {
    private MovementNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(
                AirJumpRequestPayload.TYPE,
                AirJumpRequestPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                AirJumpHoldPayload.TYPE,
                AirJumpHoldPayload.CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(
                AirJumpRequestPayload.TYPE,
                (payload, context) -> {
                    if (payload.requested()) {
                        MovementAugmentService.tryAirJump(context.player());
                    }
                }
        );
        ServerPlayNetworking.registerGlobalReceiver(
                AirJumpHoldPayload.TYPE,
                (payload, context) -> MovementAugmentService.setJumpHeld(context.player(), payload.held())
        );
    }
}
