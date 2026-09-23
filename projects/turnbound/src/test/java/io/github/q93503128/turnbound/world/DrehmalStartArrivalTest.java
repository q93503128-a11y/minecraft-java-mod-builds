package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalStartArrivalTest {
    @Test
    void recognizesTheObservedDrehmalSetupTerminalButNotCampaignAnchors() {
        assertTrue(DrehmalStartArrival.legacySetupZone(26521.0, 178.0, -106.0));
        assertTrue(DrehmalStartArrival.legacySetupZone(26520.0, 177.0, -136.0));
        assertFalse(DrehmalStartArrival.legacySetupZone(855.0, 65.0, 553.0));
        assertFalse(DrehmalStartArrival.legacySetupZone(502.0, 67.0, 1801.0));
    }
}
