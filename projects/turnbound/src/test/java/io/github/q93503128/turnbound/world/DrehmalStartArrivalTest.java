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
    void openingStartsAtTheTownGateAndFirstCombatIsClose() {
        var entry = DrehmalMapPlacementCatalog.placement(DrehmalStartArrival.ENTRY_SITE);
        var patrol = DrehmalMapPlacementCatalog.placement(DrabyelOpeningTutorial.ENCOUNTER_SITE);
        var hub = DrehmalWorldProfile.enabled(DrehmalWorldProfile.HUB_LOCATOR);
        assertNotNull(entry);
        assertNotNull(patrol);
        assertNotNull(hub);
        var seed = entry.siteSeeds().get(DrehmalStartArrival.ENTRY_SEED_INDEX);
        double hubDistance = Math.hypot(seed.x() - hub.x(), seed.z() - hub.z());
        double patrolDistance = Math.hypot(
                seed.x() - patrol.siteSeeds().getFirst().x(),
                seed.z() - patrol.siteSeeds().getFirst().z());
        assertTrue(hubDistance <= 80.0, "opening must start at New Drabyel's entrance, got " + hubDistance);
        assertTrue(patrolDistance <= 100.0, "first outdoor combat must be within 100m, got " + patrolDistance);
    }

    @Test
    void missingCurrentArrivalFlagRepairsBuggyHubBootstrapWithoutUsingHubReachedFlag() {
        assertTrue(DrehmalStartMigrationRules.shouldRepairMissingRouteArrival(false, true));
        assertFalse(DrehmalStartMigrationRules.shouldRepairMissingRouteArrival(true, true));
        assertFalse(DrehmalStartMigrationRules.shouldRepairMissingRouteArrival(false, false));
    }
}
