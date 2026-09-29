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

    @Test
    void activePathWithoutPhysicalProgressStillRecovers() {
        assertFalse(FieldNavigationRules.shouldRecoverStalledNavigation(
                FieldEncounterRules.Phase.ALERT, 129, 100, 9.0D));
        assertTrue(FieldNavigationRules.shouldRecoverStalledNavigation(
                FieldEncounterRules.Phase.ALERT, 130, 100, 9.0D));
        assertTrue(FieldNavigationRules.shouldRecoverStalledNavigation(
                FieldEncounterRules.Phase.RETURN, 140, 100, 9.0D));
        assertTrue(FieldNavigationRules.shouldRecoverStalledNavigation(
                FieldEncounterRules.Phase.PATROL, 160, 100, 9.0D));
    }

    @Test
    void walkingPresentationRequiresRecentCoordinateProgress() {
        assertFalse(FieldNavigationRules.madePhysicalProgress(0.000001D));
        assertTrue(FieldNavigationRules.madePhysicalProgress(0.0001D));
        assertFalse(FieldNavigationRules.walkingFromRecentProgress(
                100, FieldNavigationRules.NEVER, false));
        assertTrue(FieldNavigationRules.walkingFromRecentProgress(102, 100, false));
        assertFalse(FieldNavigationRules.walkingFromRecentProgress(103, 100, false));
        assertFalse(FieldNavigationRules.walkingFromRecentProgress(101, 100, true));
    }
}
