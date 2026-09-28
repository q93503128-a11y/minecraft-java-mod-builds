package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrehmalStartArrivalTest {
    @Test
    void onlyLegacyHubStateMigratesToTheFirstRoute() {
        assertTrue(DrehmalStartArrival.shouldMigrateLegacyHubArrival(false,true,true));
        assertFalse(DrehmalStartArrival.shouldMigrateLegacyHubArrival(true,true,true));
        assertFalse(DrehmalStartArrival.shouldMigrateLegacyHubArrival(false,false,true));
        assertFalse(DrehmalStartArrival.shouldMigrateLegacyHubArrival(false,true,false));
    }
}
