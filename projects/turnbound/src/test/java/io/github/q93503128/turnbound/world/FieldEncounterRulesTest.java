package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldEncounterRulesTest {
    @Test
    void visiblePatrolAlertsChasesAndDisengagesDeterministically() {
        assertEquals(FieldEncounterRules.Phase.PATROL, FieldEncounterRules.nextPhase(FieldEncounterRules.Phase.PATROL, 11.0, 0));
        assertEquals(FieldEncounterRules.Phase.ALERT, FieldEncounterRules.nextPhase(FieldEncounterRules.Phase.PATROL, 9.9, 0));
        assertEquals(FieldEncounterRules.Phase.ALERT, FieldEncounterRules.nextPhase(FieldEncounterRules.Phase.ALERT, 12.0, 0));
        assertEquals(FieldEncounterRules.Phase.PATROL, FieldEncounterRules.nextPhase(FieldEncounterRules.Phase.ALERT, 15.0, 0));
    }

    @Test
    void encounterNeverStartsDuringReturnGrace() {
        assertFalse(FieldEncounterRules.shouldEngage(1.0, 20));
        assertTrue(FieldEncounterRules.shouldEngage(2.5, 0));
        assertFalse(FieldEncounterRules.shouldEngage(3.0, 0));
    }
}
