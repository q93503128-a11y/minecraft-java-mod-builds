package dev.moonseungjun.fishinggame.network;

import dev.moonseungjun.fishinggame.fishing.FishingSessionManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class FishingNetworking {
    private FishingNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(ReelInputPayload.TYPE, ReelInputPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(
                ReelInputPayload.TYPE,
                (payload, context) -> FishingSessionManager.setReelHeld(context.player(), payload.held())
        );
    }
}
