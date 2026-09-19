package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldNavigationRulesTest {
    @Test
    void staticPatrolDoesNotReissueEveryTick() {
        assertFalse(FieldNavigationRules.shouldIssue(
                FieldEncounterRules.Phase.PATROL, 101, 100, 0.01D, false));
        assertTrue(FieldNavigationRules.shouldIssue(
                FieldEncounterRules.Phase.PATROL, 112, 100, 0.01D, false));
    }

    @Test
    void alertRepathsSoonerWhenPlayerMoves() {
        assertTrue(FieldNavigationRules.shouldIssue(
                FieldEncounterRules.Phase.ALERT, 101, 100, 1.01D, false));
        assertFalse(FieldNavigationRules.shouldIssue(
                FieldEncounterRules.Phase.ALERT, 101, 100, 0.25D, false));
        assertTrue(FieldNavigationRules.shouldIssue(
                FieldEncounterRules.Phase.ALERT, 104, 100, 0.25D, false));
    }

    @Test
    void completedPathCanImmediatelyRequestAnotherTarget() {
        assertTrue(FieldNavigationRules.shouldIssue(
                FieldEncounterRules.Phase.RETURN, 2, 1, 0.0D, true));
    }
}
