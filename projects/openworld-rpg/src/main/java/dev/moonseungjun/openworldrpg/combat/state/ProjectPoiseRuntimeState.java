package dev.moonseungjun.openworldrpg.combat.state;

/**
 * Server-owned transient poise state for an elite/miniboss/boss actor.
 *
 * <p>The current M0 binding uses the canonical boss timing from COMBAT_BALANCE.md:
 * 6 s recovery delay, 12.5% max-poise recovery per second, 2.4 s break,
 * +15% direct damage taken while broken, then 1.5 s of 50% poise-damage reduction.
 * State is intentionally independent from Minecraft entities so timing math stays unit-testable.</p>
 */
public final class ProjectPoiseRuntimeState {
    public static final int BOSS_RECOVERY_DELAY_TICKS = 120;
    public static final double BOSS_RECOVERY_FRACTION_PER_SECOND = 0.125;
    public static final int BOSS_BREAK_WINDOW_TICKS = 48;
    public static final double BREAK_DAMAGE_TAKEN_MULTIPLIER = 1.15;
    public static final int POST_BREAK_REDUCTION_TICKS = 30;
    public static final double POST_BREAK_POISE_TAKEN_MULTIPLIER = 0.50;

    private static final long UNSET_TICK = Long.MIN_VALUE / 4;

    private final double maxPoise;
    private double currentPoise;
    private long lastPoiseDamageTick = UNSET_TICK;
    private long lastRefreshTick;
    private long breakUntilTick = UNSET_TICK;
    private long postBreakReductionUntilTick = UNSET_TICK;

    private ProjectPoiseRuntimeState(double maxPoise, long nowTick) {
        requireFiniteNonNegative("maxPoise", maxPoise);
        if (maxPoise <= 0.0) {
            throw new IllegalArgumentException("maxPoise must be positive.");
        }
        this.maxPoise = maxPoise;
        this.currentPoise = maxPoise;
        this.lastRefreshTick = nowTick;
    }

    public static ProjectPoiseRuntimeState boss(double maxPoise, long nowTick) {
        return new ProjectPoiseRuntimeState(maxPoise, nowTick);
    }

    public Snapshot snapshot(long nowTick) {
        refresh(nowTick);
        return new Snapshot(
                currentPoise,
                maxPoise,
                isBrokenWithoutRefresh(nowTick),
                damageTakenMultiplierWithoutRefresh(nowTick),
                poiseTakenMultiplierWithoutRefresh(nowTick)
        );
    }

    public Application apply(double rawPoiseDamage, long nowTick) {
        requireFiniteNonNegative("rawPoiseDamage", rawPoiseDamage);
        refresh(nowTick);

        if (rawPoiseDamage == 0.0 || isBrokenWithoutRefresh(nowTick)) {
            return new Application(
                    0.0,
                    currentPoise,
                    false,
                    isBrokenWithoutRefresh(nowTick)
            );
        }

        double effectivePoiseDamage = rawPoiseDamage * poiseTakenMultiplierWithoutRefresh(nowTick);
        currentPoise = Math.max(0.0, currentPoise - effectivePoiseDamage);
        lastPoiseDamageTick = nowTick;
        lastRefreshTick = nowTick;

        boolean breakTriggered = currentPoise <= 0.0;
        if (breakTriggered) {
            currentPoise = 0.0;
            breakUntilTick = nowTick + BOSS_BREAK_WINDOW_TICKS;
            postBreakReductionUntilTick = breakUntilTick + POST_BREAK_REDUCTION_TICKS;
        }

        return new Application(
                effectivePoiseDamage,
                currentPoise,
                breakTriggered,
                breakTriggered || isBrokenWithoutRefresh(nowTick)
        );
    }

    private void refresh(long nowTick) {
        if (nowTick < lastRefreshTick) {
            throw new IllegalArgumentException("Server poise time must be monotonic.");
        }

        if (breakUntilTick != UNSET_TICK) {
            if (nowTick < breakUntilTick) {
                lastRefreshTick = nowTick;
                return;
            }

            currentPoise = maxPoise;
            breakUntilTick = UNSET_TICK;
            lastRefreshTick = nowTick;
            return;
        }

        if (currentPoise < maxPoise && lastPoiseDamageTick != UNSET_TICK) {
            long recoveryStartTick = lastPoiseDamageTick + BOSS_RECOVERY_DELAY_TICKS;
            long effectiveStartTick = Math.max(lastRefreshTick, recoveryStartTick);
            if (nowTick > effectiveStartTick) {
                double recoveryPerTick =
                        maxPoise * BOSS_RECOVERY_FRACTION_PER_SECOND / 20.0;
                currentPoise = Math.min(
                        maxPoise,
                        currentPoise + (nowTick - effectiveStartTick) * recoveryPerTick
                );
            }
        }

        lastRefreshTick = nowTick;
    }

    private boolean isBrokenWithoutRefresh(long nowTick) {
        return breakUntilTick != UNSET_TICK && nowTick < breakUntilTick;
    }

    private double damageTakenMultiplierWithoutRefresh(long nowTick) {
        return isBrokenWithoutRefresh(nowTick) ? BREAK_DAMAGE_TAKEN_MULTIPLIER : 1.0;
    }

    private double poiseTakenMultiplierWithoutRefresh(long nowTick) {
        return postBreakReductionUntilTick != UNSET_TICK
                && nowTick >= postBreakReductionUntilTick - POST_BREAK_REDUCTION_TICKS
                && nowTick < postBreakReductionUntilTick
                && !isBrokenWithoutRefresh(nowTick)
                ? POST_BREAK_POISE_TAKEN_MULTIPLIER
                : 1.0;
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative.");
        }
    }

    public record Snapshot(
            double currentPoise,
            double maxPoise,
            boolean broken,
            double damageTakenMultiplier,
            double poiseTakenMultiplier
    ) {
    }

    public record Application(
            double effectivePoiseDamage,
            double remainingPoise,
            boolean breakTriggered,
            boolean broken
    ) {
    }
}
