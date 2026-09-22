package dev.moonseungjun.openworldrpg.combat.state;

/** One server-owned player Shock buildup meter with canonical decay and Conductive duration. */
public final class PlayerShockRuntimeState {
    public static final long DECAY_DELAY_TICKS = 60L;
    public static final double DECAY_PER_SECOND = 20.0;
    public static final long CONDUCTIVE_TICKS = 80L;

    private static final long UNSET_TICK = Long.MIN_VALUE / 4;

    private double threshold;
    private double buildup;
    private long lastBuildupTick = UNSET_TICK;
    private long lastRefreshTick;
    private long conductiveUntilTick = UNSET_TICK;

    public PlayerShockRuntimeState(double threshold, long nowTick) {
        requirePositive(threshold);
        this.threshold = threshold;
        this.lastRefreshTick = nowTick;
    }

    public void synchronizeThreshold(double newThreshold, long nowTick) {
        requirePositive(newThreshold);
        refresh(nowTick);
        double fraction = threshold <= 0.0 ? 0.0 : buildup / threshold;
        threshold = newThreshold;
        buildup = Math.max(0.0, Math.min(threshold, threshold * fraction));
    }

    public Application apply(double amount, long nowTick) {
        if (!Double.isFinite(amount) || amount < 0.0) {
            throw new IllegalArgumentException("Shock buildup must be finite and non-negative.");
        }
        refresh(nowTick);
        buildup += amount;
        lastBuildupTick = nowTick;
        lastRefreshTick = nowTick;

        boolean procced = buildup >= threshold;
        if (procced) {
            buildup = 0.0;
            conductiveUntilTick = nowTick + CONDUCTIVE_TICKS;
        }
        return new Application(amount, buildup, threshold, procced, nowTick < conductiveUntilTick);
    }

    public Snapshot snapshot(long nowTick) {
        refresh(nowTick);
        return new Snapshot(buildup, threshold, nowTick < conductiveUntilTick);
    }

    private void refresh(long nowTick) {
        if (nowTick < lastRefreshTick) {
            throw new IllegalArgumentException("Shock state time must be monotonic.");
        }
        if (buildup > 0.0 && lastBuildupTick != UNSET_TICK) {
            long decayStart = lastBuildupTick + DECAY_DELAY_TICKS;
            long effectiveStart = Math.max(lastRefreshTick, decayStart);
            if (nowTick > effectiveStart) {
                buildup = Math.max(
                        0.0,
                        buildup - (nowTick - effectiveStart) * DECAY_PER_SECOND / 20.0
                );
            }
        }
        lastRefreshTick = nowTick;
    }

    private static void requirePositive(double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException("Shock threshold must be finite and positive.");
        }
    }

    public record Snapshot(double buildup, double threshold, boolean conductive) {}

    public record Application(
            double acceptedBuildup,
            double remainingBuildup,
            double threshold,
            boolean procced,
            boolean conductive
    ) {}
}
