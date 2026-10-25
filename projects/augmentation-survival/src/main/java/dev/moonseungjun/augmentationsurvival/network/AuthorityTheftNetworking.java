package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.augment.active.AuthorityTheftService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Server-authoritative dedicated Authority Theft activation channel. */
public final class AuthorityTheftNetworking {
    private AuthorityTheftNetworking() { }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(AuthorityReleasePayload.TYPE, AuthorityReleasePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AuthorityReleasePayload.TYPE, (payload, context) -> {
            if (payload.release()) AuthorityTheftService.tryRelease(context.player());
        });
    }
}
