package dev.moonseungjun.augmentationsurvival.augment.active;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Small bridge state for applying the shared non-mythic active policy to Gigantification. */
public final class GigantificationPolicyState {
    private static final Map<UUID, Reservation> PENDING = new HashMap<>();
    private static final Map<UUID, Long> MAX_END_TICKS = new HashMap<>();

    private GigantificationPolicyState() { }

    public static void putReservation(UUID playerId, String key, long readyTick, boolean doubleCharge) {
        PENDING.put(playerId, new Reservation(key, readyTick, doubleCharge));
    }

    public static Reservation takeReservation(UUID playerId) {
        return PENDING.remove(playerId);
    }

    public static void recordMaximumEnd(UUID playerId, long maxEndTick) {
        MAX_END_TICKS.put(playerId, maxEndTick);
    }

    public static long maximumEnd(UUID playerId, long fallback) {
        return MAX_END_TICKS.getOrDefault(playerId, fallback);
    }

    public static void clear() {
        PENDING.clear();
        MAX_END_TICKS.clear();
    }

    public record Reservation(String key, long readyTick, boolean doubleCharge) { }
}
