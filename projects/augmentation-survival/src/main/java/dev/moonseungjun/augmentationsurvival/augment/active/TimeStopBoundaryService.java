package dev.moonseungjun.augmentationsurvival.augment.active;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Fixed world-space boundary for the actual server-authoritative time-stop sphere. */
public final class TimeStopBoundaryService {
    private static final DustParticleOptions SHELL_GRAY = new DustParticleOptions(0xAEB7BD, 1.32F);
    private static final DustParticleOptions SHELL_DARK = new DustParticleOptions(0x68727A, 1.16F);
    private static final DustParticleOptions SHELL_MIST = new DustParticleOptions(0xC4CACC, 0.92F);
    private static final DustParticleOptions INNER_GRAY = new DustParticleOptions(0x879198, 0.90F);
    private static final Map<UUID, ActiveField> ACTIVE_FIELDS = new HashMap<>();
    private static boolean initialized;

    private TimeStopBoundaryService() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(TimeStopBoundaryService::tick);
        ServerPlayerEvents.LEAVE.register(player -> ACTIVE_FIELDS.remove(player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE_FIELDS.clear());
    }

    /** Captures the cast origin. Repeated ticks must never move this center with the caster. */
    public static void begin(ServerPlayer player) {
        long now = player.level().getGameTime();
        Vec3 center = player.getBoundingBox().getCenter();
        ACTIVE_FIELDS.put(
                player.getUUID(),
                new ActiveField(
                        player.getUUID(),
                        player.level(),
                        center,
                        now + SpecialActiveTuning.current().timeStopDurationTicks()
                )
        );
    }

    /** Used by the Time Stop runtime on the first field application, before the router presentation hook runs. */
    public static ActiveField ensureActive(ServerPlayer player) {
        ActiveField existing = activeField(player);
        if (existing != null) return existing;
        begin(player);
        return ACTIVE_FIELDS.get(player.getUUID());
    }

    /** Test/readability boundary: zero means this player's time-stop sphere is not active. */
    public static double activeRadius(ServerPlayer player) {
        return activeField(player) == null ? 0.0D : SpecialActiveTuning.current().timeStopRadius();
    }

    /** Returns the immutable cast-time field, or null when it is no longer active. */
    public static ActiveField activeField(ServerPlayer player) {
        if (player == null) return null;
        ActiveField field = ACTIVE_FIELDS.get(player.getUUID());
        if (field == null) return null;
        if (field.level() != player.level() || field.endTick() <= field.level().getGameTime()) return null;
        return field;
    }

    /** Snapshot used by integrity checks without exposing the mutable backing map. */
    public static List<ActiveField> activeFields(ServerLevel level) {
        if (level == null) return List.of();
        long now = level.getGameTime();
        ArrayList<ActiveField> result = new ArrayList<>();
        for (ActiveField field : ACTIVE_FIELDS.values()) {
            if (field.level() == level && field.endTick() > now) result.add(field);
        }
        return List.copyOf(result);
    }

    /** Shared generic-entity boundary: the caster is the only entity exempt from their own field. */
    public static boolean freezesWorldEntity(Entity entity) {
        if (entity == null || entity.level().isClientSide() || !(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        double radius = SpecialActiveTuning.current().timeStopRadius();
        double radiusSquared = radius * radius;
        Vec3 entityCenter = entity.getBoundingBox().getCenter();
        for (ActiveField field : activeFields(level)) {
            if (field.casterId().equals(entity.getUUID())) continue;
            if (field.center().distanceToSqr(entityCenter) <= radiusSquared) return true;
        }
        return false;
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, ActiveField>> iterator = ACTIVE_FIELDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveField> entry = iterator.next();
            ActiveField field = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isDeadOrDying() || player.level() != field.level()) {
                iterator.remove();
                continue;
            }
            long now = field.level().getGameTime();
            if (now >= field.endTick()) {
                iterator.remove();
                continue;
            }
            if (now % 5L == 0L) {
                double radius = SpecialActiveTuning.current().timeStopRadius();
                renderSphere(field.level(), field.center(), radius, now);
                renderShellHaze(field.level(), field.center(), radius, now);
                renderGrayField(field.level(), field.center(), radius, now);
            }
        }
    }

    private static void renderSphere(ServerLevel level, Vec3 center, double radius, long now) {
        if (!(radius > 0.0D) || !Double.isFinite(radius)) return;
        int points = 36;
        int phase = (int) Math.floorMod(now / 5L, 6L);
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            double c = Math.cos(angle) * radius;
            double s = Math.sin(angle) * radius;
            point(level, center.x + c, center.y, center.z + s, i + phase);
            point(level, center.x + c, center.y + s, center.z, i + phase + 1);
            point(level, center.x, center.y + c, center.z + s, i + phase + 2);
        }
        latitude(level, center, radius, radius * 0.34D, points, phase + 3);
        latitude(level, center, radius, -radius * 0.34D, points, phase + 5);
        latitude(level, center, radius, radius * 0.68D, points, phase + 7);
        latitude(level, center, radius, -radius * 0.68D, points, phase + 9);
    }

    private static void latitude(
            ServerLevel level,
            Vec3 center,
            double radius,
            double yOffset,
            int points,
            int phase
    ) {
        double horizontal = Math.sqrt(Math.max(0.0D, radius * radius - yOffset * yOffset));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            point(level,
                    center.x + Math.cos(angle) * horizontal,
                    center.y + yOffset,
                    center.z + Math.sin(angle) * horizontal,
                    i + phase);
        }
    }

    /** Adds a sparse distributed skin so the boundary reads as a sphere instead of three crossing rings. */
    private static void renderShellHaze(ServerLevel level, Vec3 center, double radius, long now) {
        if (!(radius > 0.0D) || !Double.isFinite(radius)) return;
        int samples = 40;
        double phase = (now / 5L) * 0.09763107293781749D;
        for (int i = 0; i < samples; i++) {
            double u = fract((i + 1) * 0.6180339887498949D + phase * 0.19D);
            double v = fract((i + 1) * 0.7548776662466927D + phase);
            double jitter = fract((i + 1) * 0.438579021D + phase * 0.41D);
            double yUnit = 1.0D - 2.0D * u;
            double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - yUnit * yUnit));
            double angle = Math.PI * 2.0D * v;
            double r = radius * (0.94D + 0.045D * jitter);
            double x = center.x + Math.cos(angle) * horizontal * r;
            double y = center.y + yUnit * r;
            double z = center.z + Math.sin(angle) * horizontal * r;
            ParticleOptions particle = Math.floorMod(i + (int) (now / 5L), 5) == 0
                    ? ParticleTypes.WHITE_ASH
                    : SHELL_MIST;
            level.sendParticles(particle, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /**
     * Fills the actual sphere rather than tinting the player's screen. The deterministic rotating
     * sample keeps the volume readable from both inside and outside without spawning an entity mesh.
     */
    private static void renderGrayField(ServerLevel level, Vec3 center, double radius, long now) {
        if (!(radius > 0.0D) || !Double.isFinite(radius)) return;
        int samples = 64;
        double phase = (now / 5L) * 0.17320508075688773D;
        for (int i = 0; i < samples; i++) {
            double u = fract((i + 1) * 0.6180339887498949D + phase);
            double v = fract((i + 1) * 0.7548776662466927D + phase * 0.71D);
            double w = fract((i + 1) * 0.5698402909980532D + phase * 1.31D);
            double yUnit = 1.0D - 2.0D * u;
            double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - yUnit * yUnit));
            double angle = Math.PI * 2.0D * v;
            double r = radius * 0.92D * Math.cbrt(0.06D + 0.94D * w);
            double x = center.x + Math.cos(angle) * horizontal * r;
            double y = center.y + yUnit * r;
            double z = center.z + Math.sin(angle) * horizontal * r;
            ParticleOptions particle = switch (Math.floorMod(i + (int) (now / 5L), 5)) {
                case 0 -> ParticleTypes.ASH;
                case 1 -> ParticleTypes.WHITE_ASH;
                case 2 -> ParticleTypes.SMOKE;
                default -> INNER_GRAY;
            };
            level.sendParticles(particle, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private static void point(ServerLevel level, double x, double y, double z, int index) {
        ParticleOptions particle = Math.floorMod(index, 2) == 0 ? SHELL_GRAY : SHELL_DARK;
        level.sendParticles(particle, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    public record ActiveField(UUID casterId, ServerLevel level, Vec3 center, long endTick) {
    }
}
