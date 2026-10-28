package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.augment.utility.InstantMiningToggleService;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Server-authoritative dedicated Instant Mining key channel and client state sync. */
public final class InstantMiningNetworking {
    private InstantMiningNetworking() { }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(InstantMiningTogglePayload.TYPE, InstantMiningTogglePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InstantMiningStatePayload.TYPE, InstantMiningStatePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(InstantMiningTogglePayload.TYPE, (payload, context) -> {
            if (payload.toggle()) InstantMiningToggleService.toggle(context.player());
            sync(context.player());
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
    }

    public static void sync(net.minecraft.server.level.ServerPlayer player) {
        ServerPlayNetworking.send(player, new InstantMiningStatePayload(
                UtilityAugmentService.isInstantMiningEnabled(player)
        ));
    }
}
