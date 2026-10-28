package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.augment.utility.InstantMiningToggleService;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative dedicated Instant Mining key channel and client state sync. */
public final class InstantMiningNetworking {
    private InstantMiningNetworking() { }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(InstantMiningTogglePayload.TYPE, InstantMiningTogglePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InstantMiningStatePayload.TYPE, InstantMiningStatePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(InstantMiningTogglePayload.TYPE, (payload, context) -> {
            handleToggleRequest(context.player(), payload);
            sync(context.player());
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
    }

    /** Same server handler used by the real client payload receiver and runtime tests. */
    public static boolean handleToggleRequest(ServerPlayer player, InstantMiningTogglePayload payload) {
        return payload != null && payload.toggle() && InstantMiningToggleService.toggle(player);
    }

    public static void sync(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, InstantMiningStatePayload.TYPE)) return;
        ServerPlayNetworking.send(player, new InstantMiningStatePayload(
                UtilityAugmentService.isInstantMiningEnabled(player)
        ));
    }
}
