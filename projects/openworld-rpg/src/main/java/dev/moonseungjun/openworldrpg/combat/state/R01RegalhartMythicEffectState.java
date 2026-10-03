package dev.moonseungjun.openworldrpg.combat.state;

/**
 * Server-owned transient trigger state for the R01 Hartcrown Spear unique power.
 *
 * <p>A sprint segment qualifies exactly once after 1.25 seconds of real sprint movement. The
 * resulting charge lasts 2.0 seconds; if it expires, that same uninterrupted sprint segment does
 * not silently create another charge. A successful melee-basic consumption starts the authored
 * 5.0-second internal cooldown.</p>
 */
public final class R01RegalhartMythicEffectState {
    public static final long HARTS_MOMENTUM_SPRINT_TICKS = 25L;
    public static final long HARTS_MOMENTUM_WINDOW_TICKS = 40L;
    public static final long HARTS_MOMENTUM_COOLDOWN_TICKS = 100L;
    public static final double HARTS_MOMENTUM_POISE_MULTIPLIER = 1.35;

    private long sprintTicks;
    private boolean sprintSegmentQualified;
    private long primedUntilTick = Long.MIN_VALUE / 4;
    private long readyTick = Long.MIN_VALUE / 4;

    public SprintSnapshot recordMovementTick(
            boolean movingAtSprintSpeed,
            long nowTick
    ) {
        requireTick(nowTick);
        if (!movingAtSprintSpeed) {
            resetMovementSegment();
            return snapshot();
        }

        if (sprintTicks < HARTS_MOMENTUM_SPRINT_TICKS) {
            sprintTicks++;
        }

        if (!sprintSegmentQualified
                && sprintTicks >= HARTS_MOMENTUM_SPRINT_TICKS
                && nowTick >= readyTick) {
            sprintSegmentQualified = true;
            primedUntilTick = Math.addExact(
                    nowTick,
                    HARTS_MOMENTUM_WINDOW_TICKS
            );
        }

        if (sprintSegmentQualified && nowTick > primedUntilTick) {
            primedUntilTick = Long.MIN_VALUE / 4;
        }

        return snapshot();
    }

    public Trigger tryConsume(long nowTick) {
        requireTick(nowTick);
        if (!sprintSegmentQualified
                || nowTick > primedUntilTick
                || nowTick < readyTick) {
            return Trigger.rejected(readyTick);
        }

        primedUntilTick = Long.MIN_VALUE / 4;
        readyTick = Math.addExact(
                nowTick,
                HARTS_MOMENTUM_COOLDOWN_TICKS
        );
        return new Trigger(
                true,
                HARTS_MOMENTUM_POISE_MULTIPLIER,
                readyTick
        );
    }

    public void resetMovementSegment() {
        sprintTicks = 0L;
        sprintSegmentQualified = false;
        primedUntilTick = Long.MIN_VALUE / 4;
    }

    public SprintSnapshot snapshot() {
        return new SprintSnapshot(
                sprintTicks,
                sprintSegmentQualified,
                primedUntilTick,
                readyTick
        );
    }

    private static void requireTick(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Hart's Momentum tick must be non-negative."
            );
        }
    }

    public record SprintSnapshot(
            long sprintTicks,
            boolean sprintSegmentQualified,
            long primedUntilTick,
            long readyTick
    ) {
        public boolean primed(long nowTick) {
            requireTick(nowTick);
            return sprintSegmentQualified
                    && nowTick <= primedUntilTick
                    && nowTick >= readyTick;
        }
    }

    public record Trigger(
            boolean triggered,
            double poiseMultiplier,
            long nextReadyTick
    ) {
        private static Trigger rejected(long nextReadyTick) {
            return new Trigger(false, 1.0, nextReadyTick);
        }
    }
}
