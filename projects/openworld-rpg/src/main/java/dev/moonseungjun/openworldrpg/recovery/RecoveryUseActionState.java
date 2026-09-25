package dev.moonseungjun.openworldrpg.recovery;

import java.util.Objects;

/** Pure timing state for one server-authoritative Recovery Belt use action. */
public record RecoveryUseActionState(
        int slot,
        RecoveryConsumable consumable,
        long startedAtTick,
        boolean resolved
) {
    public RecoveryUseActionState {
        if (slot < 0 || slot >= RecoveryActionRules.BELT_CAPACITY) {
            throw new IllegalArgumentException("slot must be inside [0, 3].");
        }
        Objects.requireNonNull(consumable, "consumable");
        if (startedAtTick < 0L) {
            throw new IllegalArgumentException("startedAtTick must be non-negative.");
        }
    }

    public long resolutionTick() {
        return Math.addExact(startedAtTick, RecoveryActionRules.RESOLUTION_TICKS);
    }

    public long endTick() {
        return Math.addExact(startedAtTick, RecoveryActionRules.USE_DURATION_TICKS);
    }

    public boolean isPreResolution(long nowTick) {
        return !resolved && nowTick < resolutionTick();
    }

    public boolean shouldResolve(long nowTick) {
        return !resolved && nowTick >= resolutionTick();
    }

    public boolean isComplete(long nowTick) {
        return nowTick >= endTick();
    }

    public RecoveryUseActionState markResolved(long nowTick) {
        if (resolved) {
            return this;
        }
        if (nowTick < resolutionTick()) {
            throw new IllegalStateException(
                    "Recovery action cannot resolve before its canonical resolution point."
            );
        }
        return new RecoveryUseActionState(slot, consumable, startedAtTick, true);
    }
}
