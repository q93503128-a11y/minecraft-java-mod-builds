package dev.moonseungjun.openworldrpg.progression.r01;

/** Canon-locked non-spatial timing rules for the R01 Regalhart territory controller. */
public final class R01RegalhartTerritoryRules {
    public static final long REPEAT_DELAY_ACTIVE_TICKS = 20L * 60L * 20L;
    public static final long POST_ELIGIBILITY_ARENA_EMPTY_ACTIVE_TICKS = 60L * 20L;
    public static final long DISENGAGE_EMPTY_ACTIVE_TICKS = 25L * 20L;

    private R01RegalhartTerritoryRules() {
    }

    public static long repeatEligibilityActiveTick(long lastDefeatActiveTick) {
        requireTick(lastDefeatActiveTick);
        return Math.addExact(
                lastDefeatActiveTick,
                REPEAT_DELAY_ACTIVE_TICKS
        );
    }

    public static boolean repeatDelayElapsed(
            long lastDefeatActiveTick,
            long activeWorldTick
    ) {
        requireTick(lastDefeatActiveTick);
        requireTick(activeWorldTick);
        return activeWorldTick
                >= repeatEligibilityActiveTick(lastDefeatActiveTick);
    }

    public static boolean emptyWindowElapsed(
            long emptySinceActiveTick,
            long requiredTicks,
            long activeWorldTick
    ) {
        requireTick(emptySinceActiveTick);
        requireTick(activeWorldTick);
        if (requiredTicks < 0L) {
            throw new IllegalArgumentException(
                    "Required Regalhart empty-window ticks must be non-negative."
            );
        }
        return activeWorldTick - emptySinceActiveTick >= requiredTicks;
    }

    private static void requireTick(long tick) {
        if (tick < 0L) {
            throw new IllegalArgumentException(
                    "Regalhart active-world tick must be non-negative."
            );
        }
    }
}
