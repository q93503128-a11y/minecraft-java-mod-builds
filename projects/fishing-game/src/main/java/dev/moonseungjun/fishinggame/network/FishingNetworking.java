package dev.moonseungjun.fishinggame.network;

import dev.moonseungjun.fishinggame.fishing.FishingSessionManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class FishingNetworking {
    private FishingNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(ReelInputPayload.TYPE, ReelInputPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SellAllPayload.TYPE, SellAllPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BuyRodPayload.TYPE, BuyRodPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ProfileSnapshotPayload.TYPE, ProfileSnapshotPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FishingStatePayload.TYPE, FishingStatePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(
                ReelInputPayload.TYPE,
                (payload, context) -> FishingSessionManager.setReelHeld(context.player(), payload.held())
        );
        ServerPlayNetworking.registerGlobalReceiver(
                SellAllPayload.TYPE,
                (payload, context) -> {
                    if (payload.requested()) FishingSessionManager.sellAll(context.player());
                }
        );
        ServerPlayNetworking.registerGlobalReceiver(
                BuyRodPayload.TYPE,
                (payload, context) -> FishingSessionManager.buyRod(context.player(), payload.tier())
        );
    }
}
