package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalAdaptiveRoutePlacementRecoveryTest {
    @Test
    void incompleteOpeningSnapshotRetriesOnlyAfterDelay() {
        assertFalse(OpeningRouteRecoveryRules.due(
                false, 0, 79L, OpeningRouteRecoveryRules.INTERVAL_TICKS));
        assertTrue(OpeningRouteRecoveryRules.due(
                false, 0, OpeningRouteRecoveryRules.INTERVAL_TICKS,
                OpeningRouteRecoveryRules.INTERVAL_TICKS));
        assertFalse(OpeningRouteRecoveryRules.due(
                false, OpeningRouteRecoveryRules.MAX_ATTEMPTS, 10_000L, 0L));
    }

    @Test
    void readyOpeningSnapshotNeverSchedulesRecovery() {
        assertFalse(OpeningRouteRecoveryRules.due(true, 0, 10_000L, 0L));
    }
}
