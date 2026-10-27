package dev.moonseungjun.augmentationsurvival.augment.active;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Sparse geometric boundary for the actual server-authoritative time-stop sphere. */
public final class TimeStopBoundaryService {
    private static final Map<UUID, Long> ACTIVE_UNTIL = new HashMap<>();
    private static boolean initialized;

    private TimeStopBoundaryService() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(TimeStopBoundaryService::tick);
    }

    public static void begin(ServerPlayer player) {
        long now = player.level().getGameTime();
        ACTIVE_UNTIL.put(
                player.getUUID(),
                now + SpecialActiveTuning.current().timeStopDurationTicks()
        );
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
            if (now % 20L == 0L) {
                renderSphere(player, SpecialActiveTuning.current().timeStopRadius());
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
            point(level, cx + c, cy, cz + s);      // horizontal great circle
            point(level, cx + c, cy + s, cz);      // vertical X/Y great circle
            point(level, cx, cy + c, cz + s);      // vertical Y/Z great circle
        }
    }

    private static void point(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}
