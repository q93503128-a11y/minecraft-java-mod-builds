package dev.moonseungjun.openworldrpg.network;

import dev.moonseungjun.openworldrpg.mount.TrailStagMountIntentRuntime;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Serverbound Trail Stag mount intent transport. No gameplay values are trusted from the client. */
public final class TrailStagMountNetworking {
    private TrailStagMountNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(
                TrailStagMountRequestPayload.TYPE,
                TrailStagMountRequestPayload.CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(
                TrailStagMountRequestPayload.TYPE,
                (payload, context) -> TrailStagMountIntentRuntime.request(
                        context.player(),
                        payload.sequence(),
                        payload.protocolVersion()
                )
        );
    }
}
