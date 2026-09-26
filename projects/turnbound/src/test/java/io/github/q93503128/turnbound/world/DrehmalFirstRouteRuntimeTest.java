package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalFirstRouteRuntimeTest {
    @Test
    void sourceBackedNewDrabyelAnchorActsAsCurrentHubSafetyFallback() {
        assertTrue(DrehmalFirstRouteRuntime.insideHubCoordinates(502.5D, 1801.5D));
        assertTrue(DrehmalFirstRouteRuntime.insideHubCoordinates(505.0D, 1810.0D));
        assertFalse(DrehmalFirstRouteRuntime.insideHubCoordinates(700.0D, 1801.5D));
    }
}
