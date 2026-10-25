package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.mixin.ProtectionPoolAccessor;
import dev.moonseungjun.augmentationsurvival.mixin.ProtectionRuntimeAccessor;
import dev.moonseungjun.augmentationsurvival.mixin.ProtectionRuntimeStateAccessor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Read-only server snapshots for the canonical health HUD's shield/augment-absorption layers. */
public final class ProtectionHudNetworking {
    private static final Map<UUID, Snapshot> LAST = new HashMap<>();

    private ProtectionHudNetworking() { }

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(ProtectionHudStatePayload.TYPE, ProtectionHudStatePayload.CODEC);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if ((server.getTickCount() & 1) != 0) return;
            Set<UUID> online = new HashSet<>();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                online.add(player.getUUID());
                syncIfChanged(player);
            }
            LAST.keySet().removeIf(id -> !online.contains(id));
        });
    }

    private static void syncIfChanged(ServerPlayer player) {
        Snapshot snapshot = snapshot(player);
        Snapshot previous = LAST.get(player.getUUID());
        if (snapshot.nearlyEquals(previous)) return;
        LAST.put(player.getUUID(), snapshot);
        if (!ServerPlayNetworking.canSend(player, ProtectionHudStatePayload.TYPE)) return;
        ServerPlayNetworking.send(player, new ProtectionHudStatePayload(
                snapshot.shield(), snapshot.shieldCapacity(),
                snapshot.absorption(), snapshot.absorptionCapacity()
        ));
    }

    private static Snapshot snapshot(ServerPlayer player) {
        Object runtime = ProtectionRuntimeAccessor.augmentationSurvival$getRuntime().get(player.getUUID());
        if (!(runtime instanceof ProtectionRuntimeStateAccessor state)) return Snapshot.ZERO;
        float shield = 0.0F, shieldCap = 0.0F, absorption = 0.0F, absorptionCap = 0.0F;
        for (Map.Entry<String, Object> entry : state.augmentationSurvival$getPools().entrySet()) {
            if (!(entry.getValue() instanceof ProtectionPoolAccessor pool)) continue;
            float amount = Math.max(0.0F, pool.augmentationSurvival$getAmount());
            float capacity = Math.max(amount, pool.augmentationSurvival$getEffectiveAmount());
            if (entry.getKey().startsWith("SHIELD:")) {
                shield += amount;
                shieldCap += capacity;
            } else if (entry.getKey().startsWith("ABSORPTION:")) {
                absorption += amount;
                absorptionCap += capacity;
            }
        }
        return new Snapshot(shield, shieldCap, absorption, absorptionCap);
    }

    private record Snapshot(float shield, float shieldCapacity, float absorption, float absorptionCapacity) {
        private static final Snapshot ZERO = new Snapshot(0.0F, 0.0F, 0.0F, 0.0F);
        private boolean nearlyEquals(Snapshot other) {
            return other != null
                    && near(shield, other.shield)
                    && near(shieldCapacity, other.shieldCapacity)
                    && near(absorption, other.absorption)
                    && near(absorptionCapacity, other.absorptionCapacity);
        }
        private static boolean near(float a, float b) { return Math.abs(a - b) < 0.01F; }
    }
}
