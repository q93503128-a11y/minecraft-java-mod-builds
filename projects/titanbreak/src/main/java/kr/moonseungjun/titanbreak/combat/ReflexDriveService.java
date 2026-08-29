package kr.moonseungjun.titanbreak.combat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflexDriveService {
    public static final float NORMAL_TICK_RATE = 20.0F;
    public static final int P0_RATING = 80;
    public static final double P0_WORLD_RELATIVE_RATE = 0.40D;

    private static final Map<UUID, DriveState> STATES = new ConcurrentHashMap<>();

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

    /**
     * Reflex Drive no longer mutates the server's global tick rate. The status value remains
     * available for the existing network snapshot and should stay at the vanilla 20 TPS axis.
     */
    public static float currentWorldTickRate() {
        return NORMAL_TICK_RATE;
    }

    public static void restore(MinecraftServer server) {
        STATES.clear();
    }

    private record DriveState(boolean requested, boolean active, int rating) {}
}
