package dev.moonseungjun.openworldrpg.combat.state;

/**
 * One non-stacking movement snare on a project-owned hostile.
 *
 * <p>Reapplication keeps the stronger active slow and later expiry; it never multiplies slows
 * together or lets a weaker follow-up shorten an empowered application.</p>
 */
public final class SnaredRuntimeState {
    private double movementMultiplier = 1.0;
    private long expiresAtTick = Long.MIN_VALUE / 4;

    public Application apply(
            double multiplier,
            long durationTicks,
            long nowTick
    ) {
        validateTime(nowTick);
        if (!Double.isFinite(multiplier)
                || multiplier <= 0.0
                || multiplier > 1.0) {
            throw new IllegalArgumentException(
                    "Snared movement multiplier must be finite inside (0, 1]."
            );
        }
        if (durationTicks <= 0L) {
            throw new IllegalArgumentException(
                    "Snared duration must be positive."
            );
        }

        refresh(nowTick);
        long requestedExpiry = Math.addExact(
                nowTick,
                durationTicks
        );
        if (active(nowTick)) {
            movementMultiplier = Math.min(
                    movementMultiplier,
                    multiplier
            );
            expiresAtTick = Math.max(
                    expiresAtTick,
                    requestedExpiry
            );
        } else {
            movementMultiplier = multiplier;
            expiresAtTick = requestedExpiry;
        }

        return new Application(
                movementMultiplier,
                expiresAtTick
        );
    }

    public Snapshot snapshot(long nowTick) {
        refresh(nowTick);
        return active(nowTick)
                ? new Snapshot(
                        true,
                        movementMultiplier,
                        expiresAtTick
                )
                : Snapshot.inactive();
    }

    public void reset() {
        movementMultiplier = 1.0;
        expiresAtTick = Long.MIN_VALUE / 4;
    }

    private boolean active(long nowTick) {
        return nowTick < expiresAtTick;
    }

    private void refresh(long nowTick) {
        validateTime(nowTick);
        if (nowTick >= expiresAtTick) {
            reset();
        }
    }

    private static void validateTime(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Snared time must be non-negative."
            );
        }
    }

    public record Application(
            double movementMultiplier,
            long expiresAtTick
    ) {
        public Application {
            if (!Double.isFinite(movementMultiplier)
                    || movementMultiplier <= 0.0
                    || movementMultiplier > 1.0
                    || expiresAtTick < 0L) {
                throw new IllegalArgumentException(
                        "Invalid Snared application."
                );
            }
        }
    }

    public record Snapshot(
            boolean active,
            double movementMultiplier,
            long expiresAtTick
    ) {
        public Snapshot {
            if (!Double.isFinite(movementMultiplier)
                    || movementMultiplier <= 0.0
                    || movementMultiplier > 1.0) {
                throw new IllegalArgumentException(
                        "Invalid Snared snapshot."
                );
            }
            if (!active
                    && (movementMultiplier != 1.0
                    || expiresAtTick != 0L)) {
                throw new IllegalArgumentException(
                        "Inactive Snared snapshot must be neutral."
                );
            }
        }

        public static Snapshot inactive() {
            return new Snapshot(false, 1.0, 0L);
        }
    }
}
