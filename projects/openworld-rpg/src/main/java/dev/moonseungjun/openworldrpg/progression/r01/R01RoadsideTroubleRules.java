package dev.moonseungjun.openworldrpg.progression.r01;

/** Locked non-spatial rules for the R01 Roadside Trouble shared world event. */
public final class R01RoadsideTroubleRules {
    public static final long FIRST_CYCLE_DELAY_TICKS = 4L * 60L * 20L;
    public static final long REPEAT_DELAY_TICKS = 12L * 60L * 20L;
    public static final long ABANDON_EMPTY_TICKS = 120L * 20L;
    public static final int SOLO_THREAT_COUNT = 2;
    public static final int MAX_THREAT_COUNT = 4;

    private R01RoadsideTroubleRules() {
    }

    public static boolean firstCycleEligible(
            boolean shrineActivated,
            long personalActiveTicksSinceShrine,
            boolean sharedEventActive,
            boolean insideAuthoredVolume
    ) {
        if (personalActiveTicksSinceShrine < 0L) {
            throw new IllegalArgumentException(
                    "personalActiveTicksSinceShrine must be non-negative."
            );
        }
        return shrineActivated
                && insideAuthoredVolume
                && !sharedEventActive
                && personalActiveTicksSinceShrine >= FIRST_CYCLE_DELAY_TICKS;
    }

    public static boolean repeatCycleEligible(
            long activeWorldTicksSinceLastEnd,
            boolean sharedEventActive,
            boolean insideAuthoredVolume
    ) {
        if (activeWorldTicksSinceLastEnd < 0L) {
            throw new IllegalArgumentException(
                    "activeWorldTicksSinceLastEnd must be non-negative."
            );
        }
        return insideAuthoredVolume
                && !sharedEventActive
                && activeWorldTicksSinceLastEnd >= REPEAT_DELAY_TICKS;
    }

    public static int threatCount(int activeParticipants) {
        if (activeParticipants <= 0) {
            throw new IllegalArgumentException("activeParticipants must be positive.");
        }
        return Math.min(
                MAX_THREAT_COUNT,
                SOLO_THREAT_COUNT + activeParticipants - 1
        );
    }

    public static boolean complete(
            boolean threatPackDefeated,
            boolean wheelBraced,
            boolean cargoReturned
    ) {
        return threatPackDefeated && wheelBraced && cargoReturned;
    }
}
