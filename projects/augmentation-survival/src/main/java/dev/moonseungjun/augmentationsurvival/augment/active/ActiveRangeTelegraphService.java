package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.network.ActivePresentationPayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Sparse world-space range boundaries for active fields/channels that do not already own a more
 * exact specialized boundary renderer. Geometry is fed by the same radius used by the gameplay
 * presentation state; Time Stop stays on its dedicated three-great-circle sphere renderer.
 */
public final class ActiveRangeTelegraphService {
    private static final Map<Key, Window> ACTIVE = new HashMap<>();
    private static boolean initialized;

    private ActiveRangeTelegraphService() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(ActiveRangeTelegraphService::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
    }

    public static void onState(
            ServerPlayer player,
            String abilityId,
            int phase,
            int durationTicks,
            float radius
    ) {
        if (player == null || abilityId == null || abilityId.isBlank()) return;
        if ("time_stop".equals(path(abilityId))) return;
        Key key = new Key(player.getUUID(), abilityId);
        if (phase == ActivePresentationPayload.END
                || phase == ActivePresentationPayload.CANCEL
                || phase == ActivePresentationPayload.TOGGLE_OFF) {
            ACTIVE.remove(key);
            return;
        }
        if ((phase != ActivePresentationPayload.SUSTAINED && phase != ActivePresentationPayload.CHANNEL)
                || durationTicks <= 0 || !(radius > 0.0F) || !Float.isFinite(radius)) {
            return;
        }
        long now = player.level().getGameTime();
        ParticleOptions particle = particleFor(abilityId);
        ACTIVE.put(key, new Window(player.level(), now + durationTicks, radius, particle));
        emitBoundary(player.level(), player.position(), radius, particle);
    }

    /** Test/readability boundary: returns zero when no exact timed range is being presented. */
    public static float trackedRadius(ServerPlayer player, String abilityId) {
        Window window = activeWindow(player, abilityId);
        return window == null ? 0.0F : window.radius;
    }

    public static int remainingTicks(ServerPlayer player, String abilityId) {
        Window window = activeWindow(player, abilityId);
        if (window == null) return 0;
        long remaining = Math.max(0L, window.endTick - player.level().getGameTime());
        return (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    private static Window activeWindow(ServerPlayer player, String abilityId) {
        Window window = ACTIVE.get(new Key(player.getUUID(), abilityId));
        if (window == null || window.level != player.level()
                || window.endTick <= player.level().getGameTime()) return null;
        return window;
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<Key, Window>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, Window> entry = iterator.next();
            Window window = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey().playerId);
            if (player == null || player.isDeadOrDying() || player.level() != window.level) {
                iterator.remove();
                continue;
            }
            long now = player.level().getGameTime();
            if (now >= window.endTick) {
                iterator.remove();
                continue;
            }
            // Four refreshes per second keep the edge readable without turning the field into particle fog.
            if (now % 5L == 0L) {
                emitBoundary(player.level(), player.position(), window.radius, window.particle);
            }
        }
    }

    private static void emitBoundary(ServerLevel level, Vec3 center, float radius, ParticleOptions particle) {
        int points = Math.max(24, Math.min(72, Math.round(radius * 2.0F)));
        double y = center.y + 0.12D;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static ParticleOptions particleFor(String abilityId) {
        String path = path(abilityId);
        return switch (path) {
            case "area_acceleration" -> ParticleTypes.ENCHANT;
            case "revive" -> ParticleTypes.HAPPY_VILLAGER;
            default -> ParticleTypes.END_ROD;
        };
    }

    private static String path(String abilityId) {
        int split = abilityId.indexOf(':');
        return split >= 0 ? abilityId.substring(split + 1) : abilityId;
    }

    private record Key(UUID playerId, String abilityId) { }
    private record Window(ServerLevel level, long endTick, float radius, ParticleOptions particle) { }
}
