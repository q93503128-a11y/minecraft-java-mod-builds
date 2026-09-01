package kr.moonseungjun.titanbreak.combat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflexDriveService {
    public static final float NORMAL_TICK_RATE = 20.0F;
    public static final double BASE_WORLD_RELATIVE_RATE = 0.08D;

    private static final int[] MK_RATING = {0, 35, 50, 68, 84, 100};
    private static final double[] MK_RADIUS = {0.0D, 64.0D, 80.0D, 96.0D, 128.0D, 160.0D};
    private static final double[] MK_HEAT_PER_TICK = {0.0D, 1.90D, 1.36D, 1.06D, 0.79D, 0.68D};

    private static final Map<UUID, DriveState> STATES = new ConcurrentHashMap<>();

    private ReflexDriveService() {}

    public static void setRequested(ServerPlayer player, boolean requested) {
        setRequested(player, requested, ratingForMk(1));
    }

    public static void setRequested(ServerPlayer player, boolean requested, int rating) {
        DriveState previous = STATES.get(player.getUUID());
        int safeRating = Math.max(1, rating);
        if (requested && NullSuppressionService.isSuppressed(player, "reflex_drive_i")) {
            STATES.put(player.getUUID(), new DriveState(false, false, safeRating));
            return;
        }
        if (!requested) {
            if (previous != null) STATES.put(player.getUUID(), new DriveState(false, previous.active(), safeRating));
            return;
        }
        STATES.put(player.getUUID(), new DriveState(true, previous != null && previous.active(), safeRating));
    }

    public static void updateRating(ServerPlayer player, int rating) {
        DriveState previous = STATES.get(player.getUUID());
        if (previous == null) return;
        STATES.put(player.getUUID(), new DriveState(previous.requested(), previous.active(), Math.max(1, rating)));
    }

    public static boolean requested(UUID playerId) {
        DriveState state = STATES.get(playerId);
        return state != null && state.requested();
    }

    public static boolean active(UUID playerId) {
        DriveState state = STATES.get(playerId);
        return state != null && state.active();
    }

    public static int rating(UUID playerId) {
        DriveState state = STATES.get(playerId);
        return state == null ? 0 : state.rating();
    }

    public static void setActive(ServerPlayer player, boolean active) {
        if (active && NullSuppressionService.isSuppressed(player, "reflex_drive_i")) active = false;
        DriveState previous = STATES.get(player.getUUID());
        if (previous == null) {
            STATES.put(player.getUUID(), new DriveState(active, active, ratingForMk(1)));
        } else {
            STATES.put(player.getUUID(), new DriveState(previous.requested(), active, previous.rating()));
        }
    }

    public static int ratingForMk(int mk) {
        return MK_RATING[clampMk(mk)];
    }

    public static double radiusForMk(int mk) {
        return MK_RADIUS[clampMk(mk)];
    }

    public static double heatPerTickForMk(int mk) {
        return MK_HEAT_PER_TICK[clampMk(mk)];
    }

    private static int clampMk(int mk) {
        return Math.max(1, Math.min(5, mk));
    }

    public static void clear(UUID playerId) {
        STATES.remove(playerId);
    }

    public static float currentWorldTickRate() {
        return NORMAL_TICK_RATE;
    }

    public static void restore(MinecraftServer server) {
        STATES.clear();
    }

    private record DriveState(boolean requested, boolean active, int rating) {}
}
