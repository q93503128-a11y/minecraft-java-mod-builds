package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvsalExpansionProgressTest {
    @Test
    void mqAv01OnlyBriefsAfterAcceptedRegionalQuestCompletes() {
        Set<String> openingOnly = Set.of(DrabyelOpeningTutorial.ENCOUNTER_ID);
        assertFalse(AvsalExpansionProgress.briefingReady(openingOnly, Set.of(
                DrabyelLocalArcProgress.COMPLETE,
                DrabyelLocalArcProgress.REGIONAL_ACCEPTED)));
        Set<String> regional = Set.of(DrabyelOpeningTutorial.ENCOUNTER_ID,"CV_DRABYEL_NORTH");
        assertTrue(AvsalExpansionProgress.briefingReady(
                regional, Set.of(
                        DrabyelLocalArcProgress.COMPLETE,
                        DrabyelLocalArcProgress.REGIONAL_ACCEPTED,
                        DrabyelLocalArcProgress.REGIONAL_COMPLETE)));
    }

    @Test
    void routeStateNeverRewindsAfterMilestones() {
        Set<String> clear = Set.of(DrabyelOpeningTutorial.ENCOUNTER_ID);
        assertEquals(AvsalExpansionProgress.Stage.ROAD_EVENT,
                AvsalExpansionProgress.stage(Set.of(AvsalExpansionProgress.BRIEFED), clear));
        assertEquals(AvsalExpansionProgress.Stage.COURIER,
                AvsalExpansionProgress.stage(Set.of(
                        AvsalExpansionProgress.BRIEFED,
                        AvsalExpansionProgress.ROADSIDE_ECHO_SEEN), clear));
        assertEquals(AvsalExpansionProgress.Stage.ROAD_PATROL,
                AvsalExpansionProgress.stage(Set.of(
                        AvsalExpansionProgress.BRIEFED,
                        AvsalExpansionProgress.ROADSIDE_ECHO_SEEN,
                        "AVSAL_ROAD_COURIER_OFFERED"), clear));
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
        assertEquals(AvsalExpansionProgress.Stage.RELAYS,AvsalExpansionProgress.stage(complete,clear));

        Set<String> relays=Set.of(
                AvsalExpansionProgress.BRIEFED,
                AvsalExpansionProgress.OUTSKIRTS_REACHED,
                AvsalExpansionProgress.INVESTIGATION_COMPLETE,
                AvsalExpansionProgress.RELAY_WEST,
                AvsalExpansionProgress.RELAY_EAST,
                AvsalExpansionProgress.RELAY_COMPLETE);
        assertTrue(AvsalExpansionProgress.relayComplete(relays));
        assertEquals(2,AvsalExpansionProgress.relayCount(relays));
        assertEquals(AvsalExpansionProgress.Stage.BOSS,AvsalExpansionProgress.stage(relays,clear));
        assertEquals(AvsalExpansionProgress.Stage.REPORT,
                AvsalExpansionProgress.stage(relays,Set.of(DrabyelOpeningTutorial.ENCOUNTER_ID,"AV_FIRST_BOSS")));
        Set<String> reported=new java.util.HashSet<>(relays);
        reported.add(AvsalExpansionProgress.FIRST_BOSS_REPORTED);
        assertEquals(AvsalExpansionProgress.Stage.CLEARED,
                AvsalExpansionProgress.stage(reported,Set.of(DrabyelOpeningTutorial.ENCOUNTER_ID,"AV_FIRST_BOSS")));
    }
}
