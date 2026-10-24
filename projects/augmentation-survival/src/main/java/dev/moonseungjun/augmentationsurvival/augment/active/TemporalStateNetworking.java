package dev.moonseungjun.augmentationsurvival.augment.active;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Common S2C registration and send helper for temporal client prediction. */
public final class TemporalStateNetworking {
    private TemporalStateNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(
                TemporalStatePayload.TYPE,
                TemporalStatePayload.CODEC
        );
    }

    public static void send(ServerPlayer player, boolean active) {
        ServerPlayNetworking.send(player, new TemporalStatePayload(active));
    }
}
