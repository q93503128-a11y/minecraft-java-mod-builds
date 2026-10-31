package dev.moonseungjun.augmentationsurvival.augment.active;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Visible world-space boundary for the actual server-authoritative time-stop sphere. */
public final class TimeStopBoundaryService {
    private static final Map<UUID, Long> ACTIVE_UNTIL = new HashMap<>();
    private static boolean initialized;

    private TimeStopBoundaryService() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(TimeStopBoundaryService::tick);
        ServerPlayerEvents.LEAVE.register(player -> ACTIVE_UNTIL.remove(player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE_UNTIL.clear());
    }

    public static void begin(ServerPlayer player) {
        long now = player.level().getGameTime();
        ACTIVE_UNTIL.put(
                player.getUUID(),
                now + SpecialActiveTuning.current().timeStopDurationTicks()
        );
    }

    /** Test/readability boundary: zero means this player's time-stop sphere is not active. */
    public static double activeRadius(ServerPlayer player) {
        Long until = ACTIVE_UNTIL.get(player.getUUID());
        if (until == null || until <= player.level().getGameTime()) return 0.0D;
        return SpecialActiveTuning.current().timeStopRadius();
    }

    /** Shared generic-entity boundary: the caster is the only entity exempt from their own field. */
    public static boolean freezesWorldEntity(Entity entity) {
        if (entity == null || entity.level().isClientSide() || !(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        MinecraftServer server = level.getServer();
        long now = level.getGameTime();
        double radius = SpecialActiveTuning.current().timeStopRadius();
        double radiusSquared = radius * radius;
        for (Map.Entry<UUID, Long> entry : ACTIVE_UNTIL.entrySet()) {
            if (entry.getValue() <= now || entry.getKey().equals(entity.getUUID())) continue;
            ServerPlayer caster = server.getPlayerList().getPlayer(entry.getKey());
            if (caster == null || caster.isDeadOrDying() || caster.level() != level) continue;
            if (caster.distanceToSqr(entity) <= radiusSquared) return true;
        }
        return false;
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Long>> iterator = ACTIVE_UNTIL.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isDeadOrDying()) {
                iterator.remove();
                continue;
            }
            long now = player.level().getGameTime();
            if (now >= entry.getValue()) {
                iterator.remove();
                continue;
            }
            if (now % 5L == 0L) {
                double radius = SpecialActiveTuning.current().timeStopRadius();
                renderSphere(player, radius);
                renderGrayField(player, radius);
            }
        }
    }

    private static void renderSphere(ServerPlayer player, double radius) {
        if (!(radius > 0.0D) || !Double.isFinite(radius)) return;
        ServerLevel level = player.level();
        double cx = player.getX();
        double cy = player.getY() + player.getBbHeight() * 0.5D;
        double cz = player.getZ();
        int points = 36;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            double c = Math.cos(angle) * radius;
            double s = Math.sin(angle) * radius;
            point(level, cx + c, cy, cz + s, i);
            point(level, cx + c, cy + s, cz, i + 1);
            point(level, cx, cy + c, cz + s, i + 2);
        }
        latitude(level, cx, cy, cz, radius, radius * 0.42D, points, 3);
        latitude(level, cx, cy, cz, radius, -radius * 0.42D, points, 5);
    }

    private static void latitude(
            ServerLevel level,
            double cx,
            double cy,
            double cz,
            double radius,
            double yOffset,
            int points,
            int phase
    ) {
        double horizontal = Math.sqrt(Math.max(0.0D, radius * radius - yOffset * yOffset));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            point(level,
                    cx + Math.cos(angle) * horizontal,
                    cy + yOffset,
                    cz + Math.sin(angle) * horizontal,
                    i + phase);
        }
    }

    private static void renderGrayField(ServerPlayer player, double radius) {
        if (!(radius > 0.0D) || !Double.isFinite(radius)) return;
        ServerLevel level = player.level();
        double centerY = player.getY() + player.getBbHeight() * 0.5D;
        double spread = radius * 0.55D;
        double vertical = Math.max(1.0D, radius * 0.42D);
        level.sendParticles(
                ParticleTypes.ASH,
                player.getX(), centerY, player.getZ(),
                34, spread, vertical, spread, 0.002D
        );
        level.sendParticles(
                ParticleTypes.WHITE_ASH,
                player.getX(), centerY, player.getZ(),
                24, spread, vertical, spread, 0.001D
        );
        level.sendParticles(
                ParticleTypes.SMOKE,
                player.getX(), centerY, player.getZ(),
                14, spread * 0.75D, vertical * 0.75D, spread * 0.75D, 0.001D
        );
    }

    private static void point(ServerLevel level, double x, double y, double z, int index) {
        var particle = switch (Math.floorMod(index, 3)) {
            case 0 -> ParticleTypes.ASH;
            case 1 -> ParticleTypes.WHITE_ASH;
            default -> ParticleTypes.SMOKE;
        };
        level.sendParticles(particle, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}
