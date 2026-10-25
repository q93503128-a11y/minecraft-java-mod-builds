package dev.moonseungjun.augmentationsurvival.augment.active;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** S2C registration and send helper for local Phase Shift collision prediction. */
public final class PhaseStateNetworking {
    private PhaseStateNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(
                PhaseStatePayload.TYPE,
                PhaseStatePayload.CODEC
        );
    }

    public static void send(ServerPlayer player, boolean active) {
        ServerPlayNetworking.send(player, new PhaseStatePayload(active));
    }
}
