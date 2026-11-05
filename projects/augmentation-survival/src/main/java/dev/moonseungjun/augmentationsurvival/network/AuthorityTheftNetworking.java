package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.augment.active.AuthorityTheftService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative authority collection, toggle validation and J cooldown sync. */
public final class AuthorityTheftNetworking {
    private static final String AUTHORITY_ID = "augmentation_survival:authority_theft";
    private static final String RELEASE_COOLDOWN_KEY = AUTHORITY_ID + "#release";
    private AuthorityTheftNetworking() { }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(AuthorityReleasePayload.TYPE, AuthorityReleasePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AuthorityTogglePayload.TYPE, AuthorityTogglePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AuthorityCollectionPayload.TYPE, AuthorityCollectionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AuthorityReleasePayload.TYPE, (payload, context) -> {
            if (payload.release()) {
                AuthorityTheftService.tryRelease(context.player());
            } else {
                sendCollection(context.player(), true);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(AuthorityTogglePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
            String id = payload.packageId();
            if (state.level(AUTHORITY_ID) > 0 && state.hasStolenAuthority(id)
                    && AuthorityTheftService.supports(id)) {
                AugmentationPlayerStateService.replace(player, state.withPersistentInt(
                        AuthorityTheftService.DISABLED_PREFIX + id, payload.enabled() ? 0 : 1));
            }
            sendCollection(player, false);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendCollection(handler.player, false));
    }

    public static void sendCollection(ServerPlayer player, boolean openScreen) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (!ServerPlayNetworking.canSend(player, AuthorityCollectionPayload.TYPE)) return;
        long now = player.level().getGameTime();
        long cooldown = Math.max(0L, state.cooldownReadyTick(RELEASE_COOLDOWN_KEY) - now);
        var owned = new ArrayList<String>();
        var disabled = new ArrayList<String>();
        if (state.level(AUTHORITY_ID) > 0) {
            for (String id : state.stolenAuthorityBossIds()) {
                if (!AuthorityTheftService.supports(id)) continue;
                owned.add(id);
                if (!AuthorityTheftService.isEnabled(state, id)) disabled.add(id);
            }
        }
        ServerPlayNetworking.send(player, new AuthorityCollectionPayload(owned, disabled,
                (int) Math.min(Integer.MAX_VALUE, cooldown), openScreen && state.level(AUTHORITY_ID) > 0));
    }
}
