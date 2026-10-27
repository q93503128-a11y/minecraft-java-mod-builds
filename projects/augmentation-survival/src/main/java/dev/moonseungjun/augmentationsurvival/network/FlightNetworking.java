package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.augment.movement.FlightAugmentService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Server-authoritative dedicated flight toggle channel. */
public final class FlightNetworking {
    private FlightNetworking() { }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(FlightTogglePayload.TYPE, FlightTogglePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FlightTogglePayload.TYPE, (payload, context) -> {
            if (payload.toggle()) FlightAugmentService.toggle(context.player());
        });
    }
}
