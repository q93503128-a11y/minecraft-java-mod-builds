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

/** Sparse gray geometric boundary for the actual server-authoritative time-stop sphere. */
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
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            double c = Math.cos(angle) * radius;
            double s = Math.sin(angle) * radius;
            point(level, cx + c, cy, cz + s, i);
            point(level, cx + c, cy + s, cz, i);
            point(level, cx, cy + c, cz + s, i);
        }
    }

    private static void renderGrayField(ServerPlayer player, double radius) {
        if (!(radius > 0.0D) || !Double.isFinite(radius)) return;
        ServerLevel level = player.level();
        double centerY = player.getY() + player.getBbHeight() * 0.5D;
        double spread = radius * 0.55D;
        level.sendParticles(
                ParticleTypes.ASH,
                player.getX(), centerY, player.getZ(),
                10, spread, Math.max(1.0D, radius * 0.35D), spread, 0.002D
        );
        level.sendParticles(
                ParticleTypes.WHITE_ASH,
                player.getX(), centerY, player.getZ(),
                6, spread, Math.max(1.0D, radius * 0.30D), spread, 0.001D
        );
    }

    private static void point(ServerLevel level, double x, double y, double z, int index) {
        level.sendParticles(index % 2 == 0 ? ParticleTypes.ASH : ParticleTypes.WHITE_ASH,
                x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}
