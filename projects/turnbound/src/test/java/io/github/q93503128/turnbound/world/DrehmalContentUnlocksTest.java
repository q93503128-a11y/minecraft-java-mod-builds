package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalContentUnlocksTest {
    @Test
    void enteringDrabyelByItselfDoesNotAutoUnlockSummoning() {
        assertFalse(DrehmalContentUnlocks.summonUnlocked(Set.of()));
        assertFalse(DrehmalContentUnlocks.summonUnlocked(Set.of("CV_FIRST_COMMON")));
    }

    @Test
    void optionalEliteUnlocksSummoningForPlayersWhoTakeTheDangerRoute() {
        assertTrue(DrehmalContentUnlocks.summonUnlocked(Set.of(
                DrehmalContentUnlocks.WARNING_CAVE_ELITE)));
    }

    @Test
    void drabyelRoadMilestoneAlsoUnlocksSummoningForPlayersWhoSkipTheElite() {
        assertTrue(DrehmalContentUnlocks.summonUnlocked(Set.of(
                DrehmalContentUnlocks.DRABYEL_ROAD)));
    }

    @Test
    void retiredAsterBossClearDoesNotDefineDrehmalSummonAccess() {
        assertFalse(DrehmalContentUnlocks.summonUnlocked(Set.of("BATTLE_B01")));
    }
}
