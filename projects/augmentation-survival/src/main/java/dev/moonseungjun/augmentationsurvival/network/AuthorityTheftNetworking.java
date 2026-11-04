package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.augment.active.AuthorityTheftService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** The J release and Shift+J inspection paths share the existing serverbound request. */
public final class AuthorityTheftNetworking {
    private static final String AUTHORITY_ID = "augmentation_survival:authority_theft";
    private static final String RELEASE_COOLDOWN_KEY = AUTHORITY_ID + "#release";

    private AuthorityTheftNetworking() { }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(AuthorityReleasePayload.TYPE, AuthorityReleasePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AuthorityCollectionPayload.TYPE, AuthorityCollectionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AuthorityReleasePayload.TYPE, (payload, context) -> {
            if (payload.release()) {
                AuthorityTheftService.tryRelease(context.player());
            } else {
                sendCollection(context.player());
            }
        });
    }

    private static void sendCollection(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (state.level(AUTHORITY_ID) <= 0 || !ServerPlayNetworking.canSend(player, AuthorityCollectionPayload.TYPE)) {
            return;
        }
        long now = player.level().getGameTime();
        long cooldown = Math.max(0L, state.cooldownReadyTick(RELEASE_COOLDOWN_KEY) - now);
        var owned = new ArrayList<String>();
        for (String id : state.stolenAuthorityBossIds()) {
            if (AuthorityTheftService.supports(id)) owned.add(id);
        }
        ServerPlayNetworking.send(player, new AuthorityCollectionPayload(
                owned, (int) Math.min(Integer.MAX_VALUE, cooldown)
        ));
    }
}
