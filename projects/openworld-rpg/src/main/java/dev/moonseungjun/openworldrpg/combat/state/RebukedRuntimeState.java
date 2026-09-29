package dev.moonseungjun.openworldrpg.combat.state;

/**
 * One non-stacking Rebuked status on a project-owned hostile.
 *
 * <p>Reapplication never adds another multiplier. It keeps the stronger active reduction and the
 * later expiry so a normal Rebuke cannot shorten an already-empowered application.</p>
 */
public final class RebukedRuntimeState {
    private double outgoingDirectDamageMultiplier = 1.0;
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
                    "Rebuked multiplier must be finite inside (0, 1]."
            );
        }
        if (durationTicks <= 0L) {
            throw new IllegalArgumentException(
                    "Rebuked duration must be positive."
            );
        }

        refresh(nowTick);
        long requestedExpiry = Math.addExact(
                nowTick,
                durationTicks
        );
        if (active(nowTick)) {
            outgoingDirectDamageMultiplier = Math.min(
                    outgoingDirectDamageMultiplier,
                    multiplier
            );
            expiresAtTick = Math.max(
                    expiresAtTick,
                    requestedExpiry
            );
        } else {
            outgoingDirectDamageMultiplier = multiplier;
            expiresAtTick = requestedExpiry;
        }

        return new Application(
                outgoingDirectDamageMultiplier,
                expiresAtTick
        );
    }

    public double outgoingDirectDamageMultiplier(long nowTick) {
        refresh(nowTick);
        return active(nowTick)
                ? outgoingDirectDamageMultiplier
                : 1.0;
    }

    public Snapshot snapshot(long nowTick) {
        refresh(nowTick);
        return active(nowTick)
                ? new Snapshot(
                        true,
                        outgoingDirectDamageMultiplier,
                        expiresAtTick
                )
                : Snapshot.inactive();
    }

    public void reset() {
        outgoingDirectDamageMultiplier = 1.0;
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
                    "Rebuked time must be non-negative."
            );
        }
    }

    public record Application(
            double outgoingDirectDamageMultiplier,
            long expiresAtTick
    ) {
        public Application {
            if (!Double.isFinite(outgoingDirectDamageMultiplier)
                    || outgoingDirectDamageMultiplier <= 0.0
                    || outgoingDirectDamageMultiplier > 1.0
                    || expiresAtTick < 0L) {
                throw new IllegalArgumentException(
                        "Invalid Rebuked application."
                );
            }
        }
    }

    public record Snapshot(
            boolean active,
            double outgoingDirectDamageMultiplier,
            long expiresAtTick
    ) {
        public Snapshot {
            if (!Double.isFinite(outgoingDirectDamageMultiplier)
                    || outgoingDirectDamageMultiplier <= 0.0
                    || outgoingDirectDamageMultiplier > 1.0) {
                throw new IllegalArgumentException(
                        "Invalid Rebuked snapshot."
                );
            }
            if (!active
                    && (outgoingDirectDamageMultiplier != 1.0
                    || expiresAtTick != 0L)) {
                throw new IllegalArgumentException(
                        "Inactive Rebuked snapshot must be neutral."
                );
            }
        }

        public static Snapshot inactive() {
            return new Snapshot(false, 1.0, 0L);
        }
    }
}
