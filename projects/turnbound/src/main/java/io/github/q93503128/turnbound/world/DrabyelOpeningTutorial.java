package io.github.q93503128.turnbound.world;

import java.util.Set;

/** Pure state contract for the short New Drabyel gate -> north-road -> hub opening loop. */
final class DrabyelOpeningTutorial {
    static final String ENCOUNTER_ID = DrehmalContentUnlocks.DRABYEL_ROAD;
    static final String ENCOUNTER_SITE = "turnbound:site/capital_valley/drabyel_approach";
    static final String FOOTPRINT_ID = "turnbound:footprint/capital_valley/drabyel_approach";
    static final String ENCOUNTER_SLOT = "turnbound:encounter/capital_valley/drabyel_approach_patrol";
    static final String GREETER_FLAG = "HUB_SERVICE_GREETER";

    private DrabyelOpeningTutorial() {}

    static boolean introReady(Set<String> flags) {
        return flags != null
                && flags.contains(GREETER_FLAG)
                && flags.contains(DrehmalContextualOnboarding.HUB_MENU_VIEWED);
    }

    static boolean patrolCleared(Set<String> clears) {
        return clears != null && clears.contains(ENCOUNTER_ID);
    }

    static boolean shouldSendOut(Set<String> flags, Set<String> clears) {
        return introReady(flags) && !patrolCleared(clears);
    }

    static boolean shouldReturnToHub(boolean insideHub, Set<String> clears) {
        return !insideHub && patrolCleared(clears);
    }
}
