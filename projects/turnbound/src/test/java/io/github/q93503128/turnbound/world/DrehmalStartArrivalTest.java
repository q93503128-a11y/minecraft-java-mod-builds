package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrehmalStartArrivalTest {
    @Test
    void onlyExplicitLegacyDirectHubStateMigratesToTheFirstRoute() {
        assertTrue(DrehmalStartMigrationRules.shouldMigrateLegacyHubArrival(false,true,true));
        assertFalse(DrehmalStartMigrationRules.shouldMigrateLegacyHubArrival(true,true,true));
        assertFalse(DrehmalStartMigrationRules.shouldMigrateLegacyHubArrival(false,false,true),
                "normal HUB_REACHED progress is not legacy direct-arrival provenance");
        assertFalse(DrehmalStartMigrationRules.shouldMigrateLegacyHubArrival(false,true,false));
    }

    @Test
    void missingCurrentArrivalFlagRepairsBuggyHubBootstrapWithoutUsingHubReachedFlag() {
        assertTrue(DrehmalStartMigrationRules.shouldRepairMissingRouteArrival(false, true));
        assertFalse(DrehmalStartMigrationRules.shouldRepairMissingRouteArrival(true, true));
        assertFalse(DrehmalStartMigrationRules.shouldRepairMissingRouteArrival(false, false));
    }
}
