package dev.moonseungjun.openworldrpg.combat.state;

/**
 * Server-owned transient enemy poise state.
 *
 * <p>Recovery and break timing come from the canonical role bands in COMBAT_BALANCE.md. Keeping the
 * policy explicit prevents ordinary external creatures from accidentally inheriting boss stagger
 * timing merely because they share the same dependency-binding runtime.</p>
 */
public final class ProjectPoiseRuntimeState {
    public static final int COMMON_RECOVERY_DELAY_TICKS = 40;
    public static final int ELITE_RECOVERY_DELAY_TICKS = 80;
    public static final int BOSS_RECOVERY_DELAY_TICKS = 120;
    public static final double ELITE_RECOVERY_FRACTION_PER_SECOND = 0.25;
    public static final double BOSS_RECOVERY_FRACTION_PER_SECOND = 0.125;

    public static final int COMMON_BREAK_WINDOW_TICKS = 14;
    public static final int ELITE_BREAK_WINDOW_TICKS = 30;
    public static final int MINIBOSS_BREAK_WINDOW_TICKS = 40;
    public static final int BOSS_BREAK_WINDOW_TICKS = 48;

    public static final double BREAK_DAMAGE_TAKEN_MULTIPLIER = 1.15;
    public static final int POST_BREAK_REDUCTION_TICKS = 30;
    public static final double POST_BREAK_POISE_TAKEN_MULTIPLIER = 0.50;

    private static final long UNSET_TICK = Long.MIN_VALUE / 4;

    private final double maxPoise;
    private final Policy policy;
    private double currentPoise;
    private long lastPoiseDamageTick = UNSET_TICK;
    private long lastRefreshTick;
    private long breakUntilTick = UNSET_TICK;
    private long postBreakReductionUntilTick = UNSET_TICK;

    private ProjectPoiseRuntimeState(
            double maxPoise,
            long nowTick,
            Policy policy
    ) {
        requireFiniteNonNegative("maxPoise", maxPoise);
        if (maxPoise <= 0.0) {
            throw new IllegalArgumentException("maxPoise must be positive.");
        }
        this.maxPoise = maxPoise;
        this.policy = java.util.Objects.requireNonNull(policy, "policy");
        this.currentPoise = maxPoise;
        this.lastRefreshTick = nowTick;
    }

    public static ProjectPoiseRuntimeState common(
            double maxPoise,
            long nowTick
    ) {
        return new ProjectPoiseRuntimeState(
                maxPoise,
                nowTick,
                Policy.COMMON
        );
    }

    public static ProjectPoiseRuntimeState elite(
            double maxPoise,
            long nowTick
    ) {
        return new ProjectPoiseRuntimeState(
                maxPoise,
                nowTick,
                Policy.ELITE
        );
    }

    public static ProjectPoiseRuntimeState miniboss(
            double maxPoise,
            long nowTick
    ) {
        return new ProjectPoiseRuntimeState(
                maxPoise,
                nowTick,
                Policy.MINIBOSS
        );
    }

    public static ProjectPoiseRuntimeState boss(
            double maxPoise,
            long nowTick
    ) {
        return new ProjectPoiseRuntimeState(
                maxPoise,
                nowTick,
                Policy.BOSS
        );
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

        double effectivePoiseDamage =
                rawPoiseDamage * poiseTakenMultiplierWithoutRefresh(nowTick);
        currentPoise = Math.max(0.0, currentPoise - effectivePoiseDamage);
        lastPoiseDamageTick = nowTick;
        lastRefreshTick = nowTick;

        boolean breakTriggered = currentPoise <= 0.0;
        if (breakTriggered) {
            currentPoise = 0.0;
            breakUntilTick = nowTick + policy.breakWindowTicks();
            postBreakReductionUntilTick =
                    policy.postBreakReductionTicks() > 0
                            ? breakUntilTick + policy.postBreakReductionTicks()
                            : UNSET_TICK;
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
            throw new IllegalArgumentException(
                    "Server poise time must be monotonic."
            );
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

        if (currentPoise < maxPoise
                && lastPoiseDamageTick != UNSET_TICK) {
            long recoveryStartTick =
                    lastPoiseDamageTick + policy.recoveryDelayTicks();
            if (policy.instantResetAfterDelay()
                    && nowTick >= recoveryStartTick) {
                currentPoise = maxPoise;
            } else if (!policy.instantResetAfterDelay()) {
                long effectiveStartTick = Math.max(
                        lastRefreshTick,
                        recoveryStartTick
                );
                if (nowTick > effectiveStartTick) {
                    double recoveryPerTick =
                            maxPoise
                                    * policy.recoveryFractionPerSecond()
                                    / 20.0;
                    currentPoise = Math.min(
                            maxPoise,
                            currentPoise
                                    + (nowTick - effectiveStartTick)
                                    * recoveryPerTick
                    );
                }
            }
        }

        lastRefreshTick = nowTick;
    }

    private boolean isBrokenWithoutRefresh(long nowTick) {
        return breakUntilTick != UNSET_TICK
                && nowTick < breakUntilTick;
    }

    private double damageTakenMultiplierWithoutRefresh(long nowTick) {
        return isBrokenWithoutRefresh(nowTick)
                ? policy.breakDamageTakenMultiplier()
                : 1.0;
    }

    private double poiseTakenMultiplierWithoutRefresh(long nowTick) {
        return postBreakReductionUntilTick != UNSET_TICK
                && nowTick >= postBreakReductionUntilTick
                        - policy.postBreakReductionTicks()
                && nowTick < postBreakReductionUntilTick
                && !isBrokenWithoutRefresh(nowTick)
                ? policy.postBreakPoiseTakenMultiplier()
                : 1.0;
    }

    private static void requireFiniteNonNegative(
            String name,
            double value
    ) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(
                    name + " must be finite and non-negative."
            );
        }
    }

    public enum Policy {
        COMMON(
                COMMON_RECOVERY_DELAY_TICKS,
                0.0,
                true,
                COMMON_BREAK_WINDOW_TICKS,
                1.0,
                0,
                1.0
        ),
        ELITE(
                ELITE_RECOVERY_DELAY_TICKS,
                ELITE_RECOVERY_FRACTION_PER_SECOND,
                false,
                ELITE_BREAK_WINDOW_TICKS,
                BREAK_DAMAGE_TAKEN_MULTIPLIER,
                POST_BREAK_REDUCTION_TICKS,
                POST_BREAK_POISE_TAKEN_MULTIPLIER
        ),
        MINIBOSS(
                BOSS_RECOVERY_DELAY_TICKS,
                BOSS_RECOVERY_FRACTION_PER_SECOND,
                false,
                MINIBOSS_BREAK_WINDOW_TICKS,
                BREAK_DAMAGE_TAKEN_MULTIPLIER,
                POST_BREAK_REDUCTION_TICKS,
                POST_BREAK_POISE_TAKEN_MULTIPLIER
        ),
        BOSS(
                BOSS_RECOVERY_DELAY_TICKS,
                BOSS_RECOVERY_FRACTION_PER_SECOND,
                false,
                BOSS_BREAK_WINDOW_TICKS,
                BREAK_DAMAGE_TAKEN_MULTIPLIER,
                POST_BREAK_REDUCTION_TICKS,
                POST_BREAK_POISE_TAKEN_MULTIPLIER
        );

        private final int recoveryDelayTicks;
        private final double recoveryFractionPerSecond;
        private final boolean instantResetAfterDelay;
        private final int breakWindowTicks;
        private final double breakDamageTakenMultiplier;
        private final int postBreakReductionTicks;
        private final double postBreakPoiseTakenMultiplier;

        Policy(
                int recoveryDelayTicks,
                double recoveryFractionPerSecond,
                boolean instantResetAfterDelay,
                int breakWindowTicks,
                double breakDamageTakenMultiplier,
                int postBreakReductionTicks,
                double postBreakPoiseTakenMultiplier
        ) {
            this.recoveryDelayTicks = recoveryDelayTicks;
            this.recoveryFractionPerSecond = recoveryFractionPerSecond;
            this.instantResetAfterDelay = instantResetAfterDelay;
            this.breakWindowTicks = breakWindowTicks;
            this.breakDamageTakenMultiplier = breakDamageTakenMultiplier;
            this.postBreakReductionTicks = postBreakReductionTicks;
            this.postBreakPoiseTakenMultiplier =
                    postBreakPoiseTakenMultiplier;
        }

        public int recoveryDelayTicks() {
            return recoveryDelayTicks;
        }

        public double recoveryFractionPerSecond() {
            return recoveryFractionPerSecond;
        }

        public boolean instantResetAfterDelay() {
            return instantResetAfterDelay;
        }

        public int breakWindowTicks() {
            return breakWindowTicks;
        }

        public double breakDamageTakenMultiplier() {
            return breakDamageTakenMultiplier;
        }

        public int postBreakReductionTicks() {
            return postBreakReductionTicks;
        }

        public double postBreakPoiseTakenMultiplier() {
            return postBreakPoiseTakenMultiplier;
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
