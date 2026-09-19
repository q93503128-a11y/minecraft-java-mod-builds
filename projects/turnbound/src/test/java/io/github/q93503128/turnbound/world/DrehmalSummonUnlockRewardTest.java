package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalSummonUnlockRewardTest {
    @Test
    void unlockMilestonesAreProgressionSignalsNotASecondRewardTable() {
        assertTrue(DrehmalContentUnlocks.summonMilestone(DrehmalContentUnlocks.WARNING_CAVE_ELITE));
        assertTrue(DrehmalContentUnlocks.summonMilestone(DrehmalContentUnlocks.DRABYEL_ROAD));
        assertFalse(DrehmalContentUnlocks.summonMilestone("CV_FIRST_COMMON"));

        // Existing cleared-state projection remains the authority for whether the facility can be used.
        assertTrue(DrehmalContentUnlocks.summonUnlocked(Set.of(DrehmalContentUnlocks.WARNING_CAVE_ELITE)));
        assertTrue(DrehmalContentUnlocks.summonUnlocked(Set.of(DrehmalContentUnlocks.DRABYEL_ROAD)));
    }
}
