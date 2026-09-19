package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalRouteZoneRulesTest {
    @Test
    void safetyRingUsesHorizontalDistanceAndOnlyPromotedSites() {
        var safeTown = new DrehmalFirstRouteCatalog.Site(
                "town", "HUB_SAFE", "seed", "마을",
                new DrehmalFirstRouteCatalog.Position(10, 70, 10),
                8, 0, true, true);
        var dormantHugeRing = new DrehmalFirstRouteCatalog.Site(
                "draft", "BREATHING_ZONE", "seed", "미검증",
                new DrehmalFirstRouteCatalog.Position(100, 10, 100),
                80, 0, false, false);

        assertTrue(DrehmalRouteZoneRules.insideSafetyZone(List.of(safeTown), 18.0D, 10.5D));
        assertFalse(DrehmalRouteZoneRules.insideSafetyZone(List.of(safeTown), 19.0D, 10.5D));
        assertFalse(DrehmalRouteZoneRules.insideSafetyZone(List.of(dormantHugeRing), 100.5D, 100.5D));
    }

    @Test
    void multipleBreathingAndHubZonesShareTheSamePolicy() {
        var tower = new DrehmalFirstRouteCatalog.Site(
                "tower", "BREATHING_ZONE", "seed", "탑",
                new DrehmalFirstRouteCatalog.Position(0, 80, 0),
                36, 0, true, true);
        var camp = new DrehmalFirstRouteCatalog.Site(
                "camp", "REST_ZONE", "seed", "야영지",
                new DrehmalFirstRouteCatalog.Position(80, 65, 0),
                24, 0, true, true);

        assertTrue(DrehmalRouteZoneRules.insideSafetyZone(List.of(tower, camp), 20.0D, 0.5D));
        assertTrue(DrehmalRouteZoneRules.insideSafetyZone(List.of(tower, camp), 90.0D, 0.5D));
        assertFalse(DrehmalRouteZoneRules.insideSafetyZone(List.of(tower, camp), 50.0D, 40.0D));
    }
}
