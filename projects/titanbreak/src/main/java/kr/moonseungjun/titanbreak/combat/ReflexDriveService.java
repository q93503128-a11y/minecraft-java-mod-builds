package kr.moonseungjun.titanbreak.combat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflexDriveService {
    public static final float NORMAL_TICK_RATE = 20.0F;
    public static final float P0_TICK_RATE = 8.0F;
    public static final int P0_RATING = 80;

    private static final Map<UUID, DriveState> STATES = new ConcurrentHashMap<>();
    private static float appliedTickRate = NORMAL_TICK_RATE;

    private ReflexDriveService() {}

    public static void setRequested(ServerPlayer player, boolean requested) {
        DriveState previous = STATES.get(player.getUUID());
        if (!requested) {
            if (previous != null) STATES.put(player.getUUID(), new DriveState(false, previous.active(), previous.rating()));
            return;
        }
        STATES.put(player.getUUID(), new DriveState(true, previous != null && previous.active(), P0_RATING));
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
        DriveState previous = STATES.get(player.getUUID());
        if (previous == null) {
            STATES.put(player.getUUID(), new DriveState(active, active, P0_RATING));
        } else {
            STATES.put(player.getUUID(), new DriveState(previous.requested(), active, previous.rating()));
        }
    }

    public static void clear(UUID playerId) {
        STATES.remove(playerId);
    }

    public static void tickServer(MinecraftServer server) {
        boolean anyActive = STATES.values().stream().anyMatch(DriveState::active);
        applyTickRate(server, anyActive ? P0_TICK_RATE : NORMAL_TICK_RATE);
    }

    public static float currentWorldTickRate() {
        return appliedTickRate;
    }

    public static double userCompensation(UUID playerId) {
        if (!active(playerId)) return 1.0;
        return NORMAL_TICK_RATE / Math.max(1.0F, appliedTickRate);
    }

    public static void restore(MinecraftServer server) {
        STATES.clear();
        applyTickRate(server, NORMAL_TICK_RATE);
    }

    private static void applyTickRate(MinecraftServer server, float rate) {
        if (Math.abs(appliedTickRate - rate) < 0.001F) return;
        server.tickRateManager().setTickRate(rate);
        appliedTickRate = rate;
    }

    private record DriveState(boolean requested, boolean active, int rating) {}
}
