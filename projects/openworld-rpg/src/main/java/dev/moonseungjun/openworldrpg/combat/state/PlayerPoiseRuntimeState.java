package dev.moonseungjun.openworldrpg.combat.state;

/** Server-owned transient player poise, recovery and post-break protection. */
public final class PlayerPoiseRuntimeState {
    public static final long RECOVERY_DELAY_TICKS = 20L;
    public static final double RECOVERY_PER_SECOND = 45.0;
    public static final long POST_BREAK_IMMUNITY_TICKS = 7L;

    private static final long UNSET_TICK = Long.MIN_VALUE / 4;

    private double maxPoise;
    private double currentPoise;
    private long lastPressureTick = UNSET_TICK;
    private long lastRefreshTick;
    private long immunityUntilTick = UNSET_TICK;

    public PlayerPoiseRuntimeState(double maxPoise, long nowTick) {
        requirePositive(maxPoise);
        this.maxPoise = maxPoise;
        this.currentPoise = maxPoise;
        this.lastRefreshTick = nowTick;
    }

    public void synchronizeMaxPoise(double newMaxPoise, long nowTick) {
        requirePositive(newMaxPoise);
        refresh(nowTick);
        double fraction = maxPoise <= 0.0 ? 1.0 : currentPoise / maxPoise;
        maxPoise = newMaxPoise;
        currentPoise = Math.max(0.0, Math.min(maxPoise, maxPoise * fraction));
    }

    public Snapshot snapshot(long nowTick) {
        refresh(nowTick);
        return new Snapshot(
                currentPoise,
                maxPoise,
                nowTick < immunityUntilTick
        );
    }

    public Application apply(double pressure, long nowTick) {
        if (!Double.isFinite(pressure) || pressure < 0.0) {
            throw new IllegalArgumentException("Player poise pressure must be finite and non-negative.");
        }
        refresh(nowTick);
        if (pressure == 0.0 || nowTick < immunityUntilTick) {
            return new Application(
                    0.0,
                    currentPoise,
                    false,
                    nowTick < immunityUntilTick
            );
        }

        double applied = Math.min(pressure, currentPoise);
        currentPoise -= applied;
        lastPressureTick = nowTick;
        lastRefreshTick = nowTick;

        boolean broken = currentPoise <= 0.0;
        if (broken) {
            currentPoise = maxPoise;
            immunityUntilTick = nowTick + POST_BREAK_IMMUNITY_TICKS;
        }

        return new Application(applied, currentPoise, broken, nowTick < immunityUntilTick);
    }

    private void refresh(long nowTick) {
        if (nowTick < lastRefreshTick) {
            throw new IllegalArgumentException("Player poise time must be monotonic.");
        }
        if (currentPoise < maxPoise && lastPressureTick != UNSET_TICK) {
            long recoveryStart = lastPressureTick + RECOVERY_DELAY_TICKS;
            long effectiveStart = Math.max(lastRefreshTick, recoveryStart);
            if (nowTick > effectiveStart) {
                currentPoise = Math.min(
                        maxPoise,
                        currentPoise
                                + (nowTick - effectiveStart) * RECOVERY_PER_SECOND / 20.0
                );
            }
        }
        lastRefreshTick = nowTick;
    }

    private static void requirePositive(double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException("Player max poise must be finite and positive.");
        }
    }

    public record Snapshot(
            double currentPoise,
            double maxPoise,
            boolean postBreakImmune
    ) {}

    public record Application(
            double effectivePressure,
            double remainingPoise,
            boolean breakTriggered,
            boolean postBreakImmune
    ) {}
}
