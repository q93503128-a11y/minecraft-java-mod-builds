package dev.moonseungjun.openworldrpg.combat.state;

public final class ChilledRuntimeState {
    private double movementMultiplier = 1.0;
    private long expiresAtTick = Long.MIN_VALUE;

    public Snapshot apply(
            double incomingMovementMultiplier,
            long durationTicks,
            long nowTick
    ) {
        if (!Double.isFinite(incomingMovementMultiplier)
                || incomingMovementMultiplier <= 0.0
                || incomingMovementMultiplier > 1.0
                || durationTicks <= 0L
                || nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Invalid Chilled application."
            );
        }

        refresh(nowTick);
        movementMultiplier = Math.min(
                movementMultiplier,
                incomingMovementMultiplier
        );
        expiresAtTick = Math.max(
                expiresAtTick,
                Math.addExact(nowTick, durationTicks)
        );
        return snapshot(nowTick);
    }

    public Snapshot snapshot(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Chilled time must be non-negative."
            );
        }
        refresh(nowTick);
        return expiresAtTick > nowTick
                ? new Snapshot(
                        true,
                        movementMultiplier,
                        expiresAtTick
                )
                : Snapshot.inactive();
    }

    private void refresh(long nowTick) {
        if (expiresAtTick <= nowTick) {
            movementMultiplier = 1.0;
            expiresAtTick = Long.MIN_VALUE;
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
                        "Invalid Chilled movement multiplier."
                );
            }
            if (active && expiresAtTick < 0L) {
                throw new IllegalArgumentException(
                        "Active Chilled must have a non-negative expiry."
                );
            }
        }

        public static Snapshot inactive() {
            return new Snapshot(
                    false,
                    1.0,
                    Long.MIN_VALUE
            );
        }
    }
}
