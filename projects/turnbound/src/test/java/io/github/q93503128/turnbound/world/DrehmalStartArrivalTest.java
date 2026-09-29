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
    void openingEntryKeepsFirstTownWithinAReasonableTutorialWalk() {
        var placement = DrehmalMapPlacementCatalog.placement(DrehmalStartArrival.ENTRY_SITE);
        var hub = DrehmalWorldProfile.enabled(DrehmalWorldProfile.HUB_LOCATOR);
        assertNotNull(placement);
        assertNotNull(hub);
        var seed = placement.siteSeeds().getFirst();
        double distance = Math.hypot(seed.x() - hub.x(), seed.z() - hub.z());
        assertTrue(distance <= 450.0, "opening-to-town walk must stay under 450m, got " + distance);
    }

    @Test
    void missingCurrentArrivalFlagRepairsBuggyHubBootstrapWithoutUsingHubReachedFlag() {
        assertTrue(DrehmalStartMigrationRules.shouldRepairMissingRouteArrival(false, true));
        assertFalse(DrehmalStartMigrationRules.shouldRepairMissingRouteArrival(true, true));
        assertFalse(DrehmalStartMigrationRules.shouldRepairMissingRouteArrival(false, false));
    }
}
