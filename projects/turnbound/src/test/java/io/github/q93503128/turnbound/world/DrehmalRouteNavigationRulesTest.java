package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalRouteNavigationRulesTest {
    @Test
    void mainRouteAdvancesThroughSurveyedWaypointsInCatalogOrder() {
        var roadhead = site("roadhead", "START_CANDIDATE", "Capital Valley 길머리", 0, 0, 40);
        var tower = site("tower", "BREATHING_ZONE", "Capital Valley Tower", 100, 0, 36);
        var camp = site("camp", "REST_ZONE", "Explorer's Guide 야영지", 200, 0, 24);
        var approach = site("approach", "PATROL_ZONE", "New Drabyel 진입로", 300, 0, 26);
        var hub = site("hub", "HUB_SAFE", "New Drabyel", 400, 0, 64);

        var fromStart = DrehmalRouteNavigationRules.target(
                List.of(roadhead, tower, camp, approach, hub), 0.5D, 0.5D);
        var fromTower = DrehmalRouteNavigationRules.target(
                List.of(roadhead, tower, camp, approach, hub), 100.5D, 0.5D);
        var atHub = DrehmalRouteNavigationRules.target(
                List.of(roadhead, tower, camp, approach, hub), 400.5D, 0.5D);

        assertEquals(tower.locator(), fromStart.id());
        assertEquals(camp.locator(), fromTower.id());
        assertFalse(atHub.active());
    }

    @Test
    void optionalDangerAndCandidateCopyNeverBecomeMainRouteArrows() {
        var roadhead = site("roadhead", "START_CANDIDATE", "Capital Valley 길머리", 0, 0, 40);
        var cave = site("cave", "ELITE_ZONE", "경고 동굴", 40, 0, 28);
        var guide = site("guide", "GUIDE_CANDIDATE", "첫 길잡이 후보 지점", 50, 0, 24);
        var hub = site("hub", "HUB_SAFE", "New Drabyel", 100, 0, 64);

        var target = DrehmalRouteNavigationRules.target(
                List.of(roadhead, cave, guide, hub), 0.5D, 0.5D);

        assertEquals(hub.locator(), target.id());
        assertFalse(DrehmalRouteNavigationRules.eligible(cave));
        assertFalse(DrehmalRouteNavigationRules.eligible(guide));
    }

    @Test
    void durableProgressNeverPointsBackToTowerOrCamp() {
        var roadhead = site("roadhead", "START_CANDIDATE", "Capital Valley 길머리", 0, 0, 40);
        var tower = site("tower", "BREATHING_ZONE", "Capital Valley Tower", 100, 0, 36);
        var camp = site("camp", "REST_ZONE", "Explorer's Guide 야영지", 200, 0, 24);
        var approach = site("approach", "PATROL_ZONE", "New Drabyel 진입로", 300, 0, 26);
        var hub = site("hub", "HUB_SAFE", "New Drabyel", 400, 0, 64);
        var route = List.of(roadhead, tower, camp, approach, hub);

        var afterCampWalkingBackward = DrehmalRouteNavigationRules.target(
                route, 100.5D, 0.5D,
                java.util.Set.of(
                        DrehmalFirstRouteProgress.TOWER_REACHED,
                        DrehmalFirstRouteProgress.CAMP_REACHED),
                java.util.Set.of("CV_FIRST_COMMON"));
        assertEquals(approach.locator(), afterCampWalkingBackward.id());

        var afterRoadWinWalkingBackward = DrehmalRouteNavigationRules.target(
                route, 100.5D, 0.5D,
                java.util.Set.of(DrehmalFirstRouteProgress.TOWER_REACHED),
                java.util.Set.of(DrehmalContentUnlocks.DRABYEL_ROAD));
        assertEquals(hub.locator(), afterRoadWinWalkingBackward.id());

        var afterHub = DrehmalRouteNavigationRules.target(
                route, 200.5D, 0.5D,
                java.util.Set.of(DrehmalFirstRouteProgress.HUB_REACHED),
                java.util.Set.of());
        assertFalse(afterHub.active());
    }

    @Test
    void unverifiedCoordinatesRemainCompletelyInvisibleToHudNavigation() {
        var hub = new DrehmalFirstRouteCatalog.Site(
                "turnbound:test/hub",
                "HUB_SAFE",
                "turnbound:hub/new_drabyel",
                "New Drabyel",
                new DrehmalFirstRouteCatalog.Position(400, 67, 0),
                64,
                0,
                false,
                false);

        var target = DrehmalRouteNavigationRules.target(List.of(hub), 0.0D, 0.0D);
        assertFalse(target.active());
    }

    @Test
    void outsideTheFirstWaypointPointsToThatWaypointInsteadOfInventingCoordinates() {
        var roadhead = site("roadhead", "START_CANDIDATE", "Capital Valley 길머리", 0, 0, 40);
        var hub = site("hub", "HUB_SAFE", "New Drabyel", 400, 0, 64);

        var target = DrehmalRouteNavigationRules.target(
                List.of(roadhead, hub), -80.0D, 0.5D);

        assertTrue(target.active());
        assertEquals(roadhead.locator(), target.id());
        assertEquals(0.5D, target.x());
        assertEquals(0.5D, target.z());
    }

    private static DrehmalFirstRouteCatalog.Site site(
            String id, String kind, String label, int x, int z, int radius
    ) {
        int safety = switch (kind) {
            case "START_CANDIDATE", "BREATHING_ZONE", "REST_ZONE", "HUB_SAFE" -> radius;
            default -> 0;
        };
        int encounter = "PATROL_ZONE".equals(kind) ? radius : 0;
        return new DrehmalFirstRouteCatalog.Site(
                "turnbound:test/" + id,
                kind,
                "turnbound:landmark/primal_caverns",
                label,
                new DrehmalFirstRouteCatalog.Position(x, 70, z),
                safety,
                encounter,
                true,
                true);
    }
}
