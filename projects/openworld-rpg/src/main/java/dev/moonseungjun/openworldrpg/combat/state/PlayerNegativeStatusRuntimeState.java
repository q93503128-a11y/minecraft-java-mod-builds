package dev.moonseungjun.openworldrpg.combat.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Minimal server-owned negative-status runtime boundary.
 *
 * <p>This does not invent R01 status effects. Authored status systems register their real status
 * IDs/tags here so Cleansing Tonic can remove only entries explicitly tagged minor_dispellable.
 * The same boundary owns the tonic's temporary negative-buildup multiplier.</p>
 */
public final class PlayerNegativeStatusRuntimeState {
    public static final double CLEANSING_BUILDUP_RECEIVED_MULTIPLIER = 0.80;

    private final Map<String, ActiveStatus> activeStatuses = new HashMap<>();
    private long negativeBuildupResistanceUntilTick = Long.MIN_VALUE / 4;

    public void applyStatus(String statusId, Set<String> tags, long expiresAtTick) {
        requireStableId(statusId);
        Objects.requireNonNull(tags, "tags");
        if (expiresAtTick < 0L) {
            throw new IllegalArgumentException("expiresAtTick must be non-negative.");
        }
        Set<String> normalizedTags = new HashSet<>();
        for (String tag : tags) {
            requireStableTag(tag);
            normalizedTags.add(tag);
        }
        activeStatuses.put(
                statusId,
                new ActiveStatus(Set.copyOf(normalizedTags), expiresAtTick)
        );
    }

    public int cleanseTagged(String tag, long nowTick) {
        requireStableTag(tag);
        expire(nowTick);
        int before = activeStatuses.size();
        activeStatuses.entrySet().removeIf(
                entry -> entry.getValue().tags().contains(tag)
        );
        return before - activeStatuses.size();
    }

    public boolean hasStatus(String statusId, long nowTick) {
        requireStableId(statusId);
        expire(nowTick);
        return activeStatuses.containsKey(statusId);
    }

    public void applyNegativeBuildupResistance(int durationTicks, long nowTick) {
        if (durationTicks <= 0 || nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Resistance duration must be positive and time non-negative."
            );
        }
        negativeBuildupResistanceUntilTick = Math.max(
                negativeBuildupResistanceUntilTick,
                Math.addExact(nowTick, durationTicks)
        );
    }

    public double negativeBuildupReceivedMultiplier(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException("nowTick must be non-negative.");
        }
        return nowTick < negativeBuildupResistanceUntilTick
                ? CLEANSING_BUILDUP_RECEIVED_MULTIPLIER
                : 1.0;
    }

    public long negativeBuildupResistanceUntilTick() {
        return negativeBuildupResistanceUntilTick;
    }

    public int activeStatusCount(long nowTick) {
        expire(nowTick);
        return activeStatuses.size();
    }

    private void expire(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException("nowTick must be non-negative.");
        }
        activeStatuses.entrySet().removeIf(
                entry -> nowTick >= entry.getValue().expiresAtTick()
        );
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Expected stable namespaced status id.");
        }
    }

    private static void requireStableTag(String value) {
        if (value == null || value.isBlank() || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Expected stable status tag.");
        }
    }

    public record ActiveStatus(Set<String> tags, long expiresAtTick) {
        public ActiveStatus {
            tags = Set.copyOf(Objects.requireNonNull(tags, "tags"));
            if (expiresAtTick < 0L) {
                throw new IllegalArgumentException("expiresAtTick must be non-negative.");
            }
        }
    }
}
