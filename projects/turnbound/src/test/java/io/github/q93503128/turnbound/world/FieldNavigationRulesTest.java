package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldNavigationRulesTest {
    @Test
    void staticPatrolKeepsItsAcceptedPathWithoutPeriodicReissue() {
        assertFalse(FieldNavigationRules.shouldIssue(
                FieldEncounterRules.Phase.PATROL, 101, 100, 0.01D, false));
        assertFalse(FieldNavigationRules.shouldIssue(
                FieldEncounterRules.Phase.PATROL, 140, 100, 0.01D, false));
    }

    @Test
    void completedStaticPathUsesBackoffInsteadOfRetryingEveryTick() {
        assertFalse(FieldNavigationRules.shouldIssue(
                FieldEncounterRules.Phase.RETURN, 101, 100, 0.0D, true));
        assertFalse(FieldNavigationRules.shouldIssue(
                FieldEncounterRules.Phase.RETURN, 119, 100, 0.0D, true));
        assertTrue(FieldNavigationRules.shouldIssue(
                FieldEncounterRules.Phase.RETURN, 120, 100, 0.0D, true));
    }

    @Test
    void alertRepathsSoonerWhenPlayerMovesOrIntervalExpires() {
        assertTrue(FieldNavigationRules.shouldIssue(
                FieldEncounterRules.Phase.ALERT, 101, 100, 1.01D, false));
        assertFalse(FieldNavigationRules.shouldIssue(
                FieldEncounterRules.Phase.ALERT, 101, 100, 0.25D, false));
        assertTrue(FieldNavigationRules.shouldIssue(
                FieldEncounterRules.Phase.ALERT, 104, 100, 0.25D, false));
    }

    @Test
    void patrolRetargetsOnlyAfterARealBlockedWindow() {
        assertFalse(FieldNavigationRules.shouldSkipBlockedPatrolTarget(
                FieldEncounterRules.Phase.PATROL, 159, 100, true, 9.0D));
        assertTrue(FieldNavigationRules.shouldSkipBlockedPatrolTarget(
                FieldEncounterRules.Phase.PATROL, 160, 100, true, 9.0D));
        assertFalse(FieldNavigationRules.shouldSkipBlockedPatrolTarget(
                FieldEncounterRules.Phase.RETURN, 200, 100, true, 9.0D));
        assertFalse(FieldNavigationRules.shouldSkipBlockedPatrolTarget(
                FieldEncounterRules.Phase.PATROL, 200, 100, false, 9.0D));
        assertFalse(FieldNavigationRules.shouldSkipBlockedPatrolTarget(
                FieldEncounterRules.Phase.PATROL, 200, FieldNavigationRules.NEVER, true, 9.0D));
    }
}
