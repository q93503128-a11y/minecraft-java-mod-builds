package io.github.q93503128.turnbound.world;

import java.util.Set;
import java.util.UUID;

/**
 * Drehmal-specific progression gates that replace retired Aster March chapter assumptions.
 *
 * <p>The first hub teaches shop/equipment immediately. Summoning is the deliberate exception: it becomes available
 * only after a meaningful first-route combat milestone, matching the current first-route/tutorial canon.</p>
 */
public final class DrehmalContentUnlocks {
    public static final String WARNING_CAVE_ELITE = "CV_WARNING_CAVE_ELITE";
    public static final String DRABYEL_ROAD = "CV_DRABYEL_ROAD";

    private DrehmalContentUnlocks() {}

    public static boolean summonUnlocked(UUID playerId) {
        if (playerId == null) return false;
        return summonUnlocked(CampaignProgressStore.snapshot(playerId).clearedEncounters());
    }

    public static boolean summonMilestone(String encounterId) {
        return WARNING_CAVE_ELITE.equals(encounterId) || DRABYEL_ROAD.equals(encounterId);
    }

    static boolean summonUnlocked(Set<String> clearedEncounters) {
        if (clearedEncounters == null || clearedEncounters.isEmpty()) return false;
        return clearedEncounters.contains(WARNING_CAVE_ELITE)
                || clearedEncounters.contains(DRABYEL_ROAD);
    }
}
