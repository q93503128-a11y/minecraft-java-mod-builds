package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldEncounterRulesTest {
    @Test
    void visiblePatrolAlertsThenReturnsHomeBeforeRearming() {
        assertEquals(FieldEncounterRules.Phase.PATROL,
                FieldEncounterRules.nextPhase(FieldEncounterRules.Phase.PATROL, 11.0, 0.0, 0));
        assertEquals(FieldEncounterRules.Phase.ALERT,
                FieldEncounterRules.nextPhase(FieldEncounterRules.Phase.PATROL, 9.9, 0.0, 0));
        assertEquals(FieldEncounterRules.Phase.ALERT,
                FieldEncounterRules.nextPhase(FieldEncounterRules.Phase.ALERT, 12.0, 4.0, 0));
        assertEquals(FieldEncounterRules.Phase.RETURN,
                FieldEncounterRules.nextPhase(FieldEncounterRules.Phase.ALERT, 15.0, 4.0, 0));
        assertEquals(FieldEncounterRules.Phase.RETURN,
                FieldEncounterRules.nextPhase(FieldEncounterRules.Phase.RETURN, 20.0, 2.0, 0));
        assertEquals(FieldEncounterRules.Phase.PATROL,
                FieldEncounterRules.nextPhase(FieldEncounterRules.Phase.RETURN, 20.0, 0.5, 0));
    }

    @Test
    void homeLeashForcesReturnEvenWhenPlayerStaysClose() {
        assertEquals(FieldEncounterRules.Phase.RETURN,
                FieldEncounterRules.nextPhase(FieldEncounterRules.Phase.ALERT, 3.0, 18.0, 0));
    }

    @Test
    void encounterOnlyStartsWhileAlertAndOutsideGrace() {
        assertFalse(FieldEncounterRules.shouldEngage(FieldEncounterRules.Phase.PATROL, 1.0, 0));
        assertFalse(FieldEncounterRules.shouldEngage(FieldEncounterRules.Phase.RETURN, 1.0, 0));
        assertFalse(FieldEncounterRules.shouldEngage(FieldEncounterRules.Phase.ALERT, 1.0, 20));
        assertTrue(FieldEncounterRules.shouldEngage(FieldEncounterRules.Phase.ALERT, 2.5, 0));
        assertFalse(FieldEncounterRules.shouldEngage(FieldEncounterRules.Phase.ALERT, 3.0, 0));
    }
}
