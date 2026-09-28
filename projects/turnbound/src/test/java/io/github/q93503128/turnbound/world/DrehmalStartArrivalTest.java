package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrehmalStartArrivalTest {
    @Test
    void onlyLegacyHubStateMigratesToTheFirstRoute() {
        assertTrue(DrehmalStartMigrationRules.shouldMigrateLegacyHubArrival(false,true,true));
        assertFalse(DrehmalStartMigrationRules.shouldMigrateLegacyHubArrival(true,true,true));
        assertFalse(DrehmalStartMigrationRules.shouldMigrateLegacyHubArrival(false,false,true));
        assertFalse(DrehmalStartMigrationRules.shouldMigrateLegacyHubArrival(false,true,false));
    }
}
