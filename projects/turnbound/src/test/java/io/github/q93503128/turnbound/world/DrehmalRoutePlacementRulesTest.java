package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrehmalRoutePlacementRulesTest {
    @Test
    void encounterAndNpcSitesPreferReadableRoadShoulders() {
        assertEquals(3.5D, DrehmalRoutePlacementRules.preferredRoadDistance("NPC_ZONE"));
        assertEquals(5.0D, DrehmalRoutePlacementRules.preferredRoadDistance("ENCOUNTER_ZONE"));
        assertEquals(7.0D, DrehmalRoutePlacementRules.preferredRoadDistance("ELITE_ZONE"));

        assertFalse(DrehmalRoutePlacementRules.acceptableRoadDistance("ENCOUNTER_ZONE", 0.5D));
        assertTrue(DrehmalRoutePlacementRules.acceptableRoadDistance("ENCOUNTER_ZONE", 5.0D));
        assertFalse(DrehmalRoutePlacementRules.acceptableRoadDistance("NPC_ZONE", 24.0D));
    }

    @Test
    void shoulderCandidateBeatsRoadCenterEvenWhenCenterIsCloserToSeed() {
        double center = DrehmalRoutePlacementRules.score("NPC_ZONE", 0.0D, 0.0D);
        double shoulder = DrehmalRoutePlacementRules.score("NPC_ZONE", 4.0D, 3.5D);
        assertTrue(shoulder < center);
    }

    @Test
    void corridorDistanceUsesSegmentsNotOnlySparseWaypoints() {
        var zone = new DrehmalMapPlacementCatalog.Zone(
                "test", "test", "ROAD",
                java.util.List.of(
                        new DrehmalMapPlacementCatalog.Seed(0, 0),
                        new DrehmalMapPlacementCatalog.Seed(0, 20)));

        assertEquals(4.5D, DrehmalRoutePlacementRules.corridorDistance(zone, 5.0D, 10.5D), 0.0001D);
        assertEquals(0.0D, DrehmalRoutePlacementRules.corridorDistance(zone, 0.5D, 10.5D), 0.0001D);
    }

    @Test
    void landmarkSitesKeepSeedDistanceOnly() {
        assertFalse(DrehmalRoutePlacementRules.roadAware("BREATHING_ZONE"));
        assertEquals(9.0D, DrehmalRoutePlacementRules.score("BREATHING_ZONE", 9.0D, 99.0D));
    }
}
