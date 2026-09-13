package dev.moonseungjun.fishinggame.network;

import dev.moonseungjun.fishinggame.fishing.FishingSessionManager;
import dev.moonseungjun.fishinggame.world.FishingTravelManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class FishingNetworking {
    private FishingNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(CastReleasePayload.TYPE, CastReleasePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ReelInputPayload.TYPE, ReelInputPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SellAllPayload.TYPE, SellAllPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BuyRodPayload.TYPE, BuyRodPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TravelRequestPayload.TYPE, TravelRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ProfileSnapshotPayload.TYPE, ProfileSnapshotPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FishingStatePayload.TYPE, FishingStatePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(
                CastReleasePayload.TYPE,
                (payload, context) -> FishingSessionManager.finishCastCharge(context.player(), payload.cancelled())
        );
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
        ServerPlayNetworking.registerGlobalReceiver(
                TravelRequestPayload.TYPE,
                (payload, context) -> FishingTravelManager.requestTravel(context.player(), payload.locationOrdinal())
        );
    }
}
