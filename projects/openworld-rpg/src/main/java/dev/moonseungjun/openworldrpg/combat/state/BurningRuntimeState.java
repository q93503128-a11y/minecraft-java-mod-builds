package dev.moonseungjun.openworldrpg.combat.state;

public final class BurningRuntimeState {
    public static final long TICK_INTERVAL_TICKS = 10L;
    public static final long DURATION_TICKS = 80L;

    private double tickDamage;
    private long nextTickAt = Long.MIN_VALUE;
    private long expiresAt = Long.MIN_VALUE;

    public Application apply(
            double incomingTickDamage,
            long nowTick
    ) {
        if (!Double.isFinite(incomingTickDamage)
                || incomingTickDamage <= 0.0
                || nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Invalid Burning application."
            );
        }

        if (!activeInternal(nowTick)) {
            reset();
            nextTickAt = Math.addExact(
                    nowTick,
                    TICK_INTERVAL_TICKS
            );
        }

        boolean magnitudeReplaced =
                tickDamage <= 0.0
                        || incomingTickDamage
                        > tickDamage + 1.0e-9;
        if (magnitudeReplaced) {
            tickDamage = incomingTickDamage;
        }

        expiresAt = Math.addExact(
                nowTick,
                DURATION_TICKS
        );
        return new Application(
                true,
                magnitudeReplaced,
                tickDamage,
                nextTickAt,
                expiresAt
        );
    }

    public Tick pollTick(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Burning time must be non-negative."
            );
        }
        if (tickDamage <= 0.0) {
            return Tick.none();
        }
        if (nowTick > expiresAt
                || nextTickAt > expiresAt) {
            reset();
            return Tick.none();
        }
        if (nowTick < nextTickAt) {
            return Tick.none();
        }

        double damage = tickDamage;
        long scheduledTick = nextTickAt;
        nextTickAt = Math.addExact(
                nextTickAt,
                TICK_INTERVAL_TICKS
        );
        boolean finalTick = nextTickAt > expiresAt;
        if (finalTick) {
            reset();
        }
        return new Tick(
                true,
                damage,
                scheduledTick,
                finalTick
        );
    }

    public Snapshot snapshot(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Burning time must be non-negative."
            );
        }
        if (!activeInternal(nowTick)) {
            return Snapshot.inactive();
        }
        return new Snapshot(
                true,
                tickDamage,
                nextTickAt,
                expiresAt
        );
    }

    private boolean activeInternal(long nowTick) {
        return tickDamage > 0.0
                && expiresAt >= nowTick
                && nextTickAt <= expiresAt;
    }

    private void reset() {
        tickDamage = 0.0;
        nextTickAt = Long.MIN_VALUE;
        expiresAt = Long.MIN_VALUE;
    }

    public record Application(
            boolean active,
            boolean magnitudeReplaced,
            double tickDamage,
            long nextTickAt,
            long expiresAt
    ) {
        public Application {
            if (!active
                    || !Double.isFinite(tickDamage)
                    || tickDamage <= 0.0
                    || nextTickAt < 0L
                    || expiresAt < nextTickAt) {
                throw new IllegalArgumentException(
                        "Invalid Burning application result."
                );
            }
        }
    }

    public record Tick(
            boolean due,
            double damage,
            long scheduledTick,
            boolean finalTick
    ) {
        public Tick {
            if (!due
                    && (damage != 0.0
                    || scheduledTick != 0L
                    || finalTick)) {
                throw new IllegalArgumentException(
                        "Non-due Burning tick cannot carry output."
                );
            }
            if (due
                    && (!Double.isFinite(damage)
                    || damage <= 0.0
                    || scheduledTick < 0L)) {
                throw new IllegalArgumentException(
                        "Invalid due Burning tick."
                );
            }
        }

        public static Tick none() {
            return new Tick(
                    false,
                    0.0,
                    0L,
                    false
            );
        }
    }

    public record Snapshot(
            boolean active,
            double tickDamage,
            long nextTickAt,
            long expiresAt
    ) {
        public Snapshot {
            if (!active
                    && (tickDamage != 0.0
                    || nextTickAt != 0L
                    || expiresAt != 0L)) {
                throw new IllegalArgumentException(
                        "Inactive Burning snapshot cannot carry state."
                );
            }
            if (active
                    && (!Double.isFinite(tickDamage)
                    || tickDamage <= 0.0
                    || nextTickAt < 0L
                    || expiresAt < nextTickAt)) {
                throw new IllegalArgumentException(
                        "Invalid active Burning snapshot."
                );
            }
        }

        public static Snapshot inactive() {
            return new Snapshot(
                    false,
                    0.0,
                    0L,
                    0L
            );
        }
    }
}
