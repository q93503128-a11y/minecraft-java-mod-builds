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
    public static final String MINOR_DISPELLABLE_TAG = "minor_dispellable";
    public static final double CLEANSING_BUILDUP_RECEIVED_MULTIPLIER = 0.80;

    private final Map<String, ActiveStatus> activeStatuses = new HashMap<>();
    private long negativeBuildupResistanceUntilTick = Long.MIN_VALUE / 4;
    private double negativeStatusDurationMultiplier = 1.0;
    private long negativeStatusDurationMultiplierUntilTick = Long.MIN_VALUE / 4;
    private double equipmentNegativeStatusDurationReduction = 0.0;

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

    /**
     * Applies an authored negative status from a duration so transient duration modifiers such as
     * Sanctuary can be resolved at the server-owned application boundary.
     */
    public void applyStatusForDuration(
            String statusId,
            Set<String> tags,
            long durationTicks,
            long nowTick
    ) {
        if (durationTicks <= 0L || nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Status duration must be positive and time non-negative."
            );
        }
        double multiplier = negativeStatusDurationMultiplier(nowTick);
        long adjustedDuration = Math.max(
                1L,
                (long) Math.ceil(durationTicks * multiplier - 1.0e-9)
        );
        applyStatus(
                statusId,
                tags,
                Math.addExact(nowTick, adjustedDuration)
        );
    }

    /**
     * Applies a temporary multiplier only to negative statuses created while this window is active.
     * Existing statuses are intentionally not shortened retroactively.
     */
    public void applyNegativeStatusDurationMultiplier(
            double multiplier,
            long durationTicks,
            long nowTick
    ) {
        if (!Double.isFinite(multiplier)
                || multiplier <= 0.0
                || multiplier > 1.0
                || durationTicks <= 0L
                || nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Negative-status duration modifier is invalid."
            );
        }
        refreshNegativeStatusDurationMultiplier(nowTick);
        negativeStatusDurationMultiplier = Math.min(
                negativeStatusDurationMultiplier,
                multiplier
        );
        negativeStatusDurationMultiplierUntilTick = Math.max(
                negativeStatusDurationMultiplierUntilTick,
                Math.addExact(nowTick, durationTicks)
        );
    }

    public double negativeStatusDurationMultiplier(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException("nowTick must be non-negative.");
        }
        refreshNegativeStatusDurationMultiplier(nowTick);
        return Math.max(
                0.0,
                (1.0 - equipmentNegativeStatusDurationReduction)
                        * negativeStatusDurationMultiplier
        );
    }

    public void synchronizeEquipmentNegativeStatusDurationReduction(
            double reduction
    ) {
        if (!Double.isFinite(reduction) || reduction < 0.0) {
            throw new IllegalArgumentException(
                    "Equipment negative-status duration reduction must be finite and non-negative."
            );
        }
        equipmentNegativeStatusDurationReduction = Math.min(1.0, reduction);
    }

    public double equipmentNegativeStatusDurationReduction() {
        return equipmentNegativeStatusDurationReduction;
    }

    public PlayerCombatSessionState.NegativeStatusesSnapshot
    persistentSnapshot(long nowTick) {
        expire(nowTick);
        Map<String, PlayerCombatSessionState.ActiveNegativeStatusSnapshot>
                statuses = new HashMap<>();
        activeStatuses.forEach((id, status) -> statuses.put(
                id,
                new PlayerCombatSessionState.ActiveNegativeStatusSnapshot(
                        status.tags(),
                        status.expiresAtTick()
                )
        ));
        return new PlayerCombatSessionState.NegativeStatusesSnapshot(
                nowTick,
                Map.copyOf(statuses),
                negativeBuildupResistanceUntilTick
        );
    }

    public void restorePersistent(
            PlayerCombatSessionState.NegativeStatusesSnapshot snapshot,
            long nowTick
    ) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (nowTick < 0L) {
            throw new IllegalArgumentException("nowTick must be non-negative.");
        }

        long rebase = nowTick < snapshot.savedAtTick()
                ? nowTick - snapshot.savedAtTick()
                : 0L;
        activeStatuses.clear();
        snapshot.activeStatuses().forEach((id, status) -> {
            long expiresAtTick = rebaseTick(status.expiresAtTick(), rebase);
            if (expiresAtTick > nowTick) {
                activeStatuses.put(
                        id,
                        new ActiveStatus(status.tags(), expiresAtTick)
                );
            }
        });
        negativeBuildupResistanceUntilTick = rebaseTick(
                snapshot.negativeBuildupResistanceUntilTick(),
                rebase
        );
        // Sanctuary and equipment projection are transient runtime state, not reconnect state.
        negativeStatusDurationMultiplier = 1.0;
        negativeStatusDurationMultiplierUntilTick = Long.MIN_VALUE / 4;
        equipmentNegativeStatusDurationReduction = 0.0;
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

    /**
     * Removes exactly one matching status using stable status-id ordering.
     *
     * <p>The canon requires Mend to cleanse one minor-dispellable status but does not define a
     * gameplay priority between several eligible statuses. Stable ID ordering is therefore only a
     * deterministic tie-breaker; it does not invent a hidden severity ranking.</p>
     */
    public int cleanseOneTagged(String tag, long nowTick) {
        requireStableTag(tag);
        expire(nowTick);
        String selected = activeStatuses.entrySet().stream()
                .filter(entry -> entry.getValue().tags().contains(tag))
                .map(Map.Entry::getKey)
                .sorted()
                .findFirst()
                .orElse(null);
        if (selected == null) {
            return 0;
        }
        activeStatuses.remove(selected);
        return 1;
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

    private void refreshNegativeStatusDurationMultiplier(long nowTick) {
        if (nowTick >= negativeStatusDurationMultiplierUntilTick) {
            negativeStatusDurationMultiplier = 1.0;
            negativeStatusDurationMultiplierUntilTick = Long.MIN_VALUE / 4;
        }
    }

    private void expire(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException("nowTick must be non-negative.");
        }
        activeStatuses.entrySet().removeIf(
                entry -> nowTick >= entry.getValue().expiresAtTick()
        );
    }

    private static long rebaseTick(long tick, long delta) {
        if (delta == 0L || tick <= Long.MIN_VALUE / 8) {
            return tick;
        }
        return Math.addExact(tick, delta);
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
