package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvsalExpansionProgressTest {
    @Test
    void mqAv01OnlyBriefsAfterOpeningPatrolAndMapReview() {
        Set<String> clear = Set.of(DrabyelOpeningTutorial.ENCOUNTER_ID);
        assertFalse(AvsalExpansionProgress.briefingReady(clear, Set.of()));
        assertTrue(AvsalExpansionProgress.briefingReady(
                clear, Set.of(DrehmalContextualOnboarding.HUB_ROUTE_REVIEWED)));
    }

    @Test
    void routeStateNeverRewindsAfterMilestones() {
        Set<String> clear = Set.of(DrabyelOpeningTutorial.ENCOUNTER_ID);
        assertEquals(AvsalExpansionProgress.Stage.ROAD_EVENT,
                AvsalExpansionProgress.stage(Set.of(AvsalExpansionProgress.BRIEFED), clear));
        assertEquals(AvsalExpansionProgress.Stage.ROAD_PATROL,
                AvsalExpansionProgress.stage(Set.of(
                        AvsalExpansionProgress.BRIEFED,
                        AvsalExpansionProgress.ROADSIDE_ECHO_SEEN), clear));
        Set<String> investigating=Set.of(
                AvsalExpansionProgress.BRIEFED,
                AvsalExpansionProgress.ROADSIDE_ECHO_SEEN,
                AvsalExpansionProgress.ROAD_PATROL_SEEN,
                AvsalExpansionProgress.OUTSKIRTS_REACHED,
                AvsalExpansionProgress.CLUE_SCAVENGER);
        assertEquals(AvsalExpansionProgress.Stage.INVESTIGATE,
                AvsalExpansionProgress.stage(investigating, clear));
        assertEquals(1,AvsalExpansionProgress.investigationCount(investigating));

        Set<String> complete=Set.of(
                AvsalExpansionProgress.BRIEFED,
                AvsalExpansionProgress.OUTSKIRTS_REACHED,
                AvsalExpansionProgress.CLUE_SCAVENGER,
                AvsalExpansionProgress.CLUE_RECORDS,
                AvsalExpansionProgress.INVESTIGATION_COMPLETE);
        assertTrue(AvsalExpansionProgress.investigationComplete(complete));
        assertEquals(AvsalExpansionProgress.Stage.NORTHBOUND,AvsalExpansionProgress.stage(complete,clear));
    }
}
