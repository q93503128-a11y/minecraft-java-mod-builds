package dev.moonseungjun.testmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SkillNetworking {
    private static final long CAST_LOCK_TICKS = 100;
    private static final Map<UUID, Long> LAST_CAST_TICK = new HashMap<>();

    private SkillNetworking() {
    }

    public static void registerCommon() {
        PayloadTypeRegistry.serverboundPlay().register(CastRequestPayload.TYPE, CastRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CastStartedPayload.TYPE, CastStartedPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CastRequestPayload.TYPE, (payload, context) -> {
            ServerPlayer caster = context.player();
            var level = caster.level();
            long now = level.getGameTime();
            long previous = LAST_CAST_TICK.getOrDefault(caster.getUUID(), Long.MIN_VALUE / 2);
            if (!caster.isAlive() || caster.isSpectator() || now - previous < CAST_LOCK_TICKS) {
                return;
            }

            LAST_CAST_TICK.put(caster.getUUID(), now);
            CastStartedPayload started = new CastStartedPayload(caster.getUUID());
            for (ServerPlayer viewer : level.players()) {
                ServerPlayNetworking.send(viewer, started);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_CAST_TICK.remove(handler.player.getUUID()));
    }
}
