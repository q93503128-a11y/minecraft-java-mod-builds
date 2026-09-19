package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalFirstRouteRewardRulesTest {
    @Test
    void warningCaveUsesItsExistingOnePullPlusTopUpToExactlyTenPullBudget() {
        assertEquals(3_000, DrehmalFirstRouteRewardRules.supplementalCrystal(
                DrehmalContentUnlocks.WARNING_CAVE_ELITE, true, Set.of()));
        assertTrue(DrehmalFirstRouteRewardRules.unlocksSummonNow(
                DrehmalContentUnlocks.WARNING_CAVE_ELITE, true, Set.of()));
    }

    @Test
    void roadMilestonePaysTheFullLaunchBudgetWhenEliteWasSkipped() {
        assertEquals(3_000, DrehmalFirstRouteRewardRules.supplementalCrystal(
                DrehmalContentUnlocks.DRABYEL_ROAD, true, Set.of()));
        assertTrue(DrehmalFirstRouteRewardRules.unlocksSummonNow(
                DrehmalContentUnlocks.DRABYEL_ROAD, true, Set.of()));
    }

    @Test
    void secondEligibleMilestoneDoesNotRepeatTheLaunchBudget() {
        assertEquals(300, DrehmalFirstRouteRewardRules.supplementalCrystal(
                DrehmalContentUnlocks.WARNING_CAVE_ELITE, true,
                Set.of(DrehmalContentUnlocks.DRABYEL_ROAD)));
        assertFalse(DrehmalFirstRouteRewardRules.unlocksSummonNow(
                DrehmalContentUnlocks.WARNING_CAVE_ELITE, true,
                Set.of(DrehmalContentUnlocks.DRABYEL_ROAD)));

        assertEquals(0, DrehmalFirstRouteRewardRules.supplementalCrystal(
                DrehmalContentUnlocks.DRABYEL_ROAD, true,
                Set.of(DrehmalContentUnlocks.WARNING_CAVE_ELITE)));
    }

    @Test
    void repeatsAndUnrelatedEncountersPayNothing() {
        assertEquals(0, DrehmalFirstRouteRewardRules.supplementalCrystal(
                DrehmalContentUnlocks.WARNING_CAVE_ELITE, false, Set.of()));
        assertEquals(0, DrehmalFirstRouteRewardRules.supplementalCrystal(
                "CV_FIRST_COMMON", true, Set.of()));
    }
}
