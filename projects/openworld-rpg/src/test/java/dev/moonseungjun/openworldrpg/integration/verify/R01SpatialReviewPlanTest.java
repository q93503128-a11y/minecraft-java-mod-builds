package dev.moonseungjun.openworldrpg.integration.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingLoader;
import org.junit.jupiter.api.Test;

class R01SpatialReviewPlanTest {
    @Test
    void reviewPlanStartsWithCriticalSurfaceSequenceAndExcludesUnsafeVolumes() {
        var data = R01SpatialBindingLoader.loadBundled();
        var points = R01SpatialReviewPlan.surfacePoints(data);

        assertEquals(101, points.size());
        assertEquals(
                "openworld_rpg:r01/alderford_center",
                points.get(0).id()
        );
        assertEquals(
                "openworld_rpg:r01/alderford_gate_probe",
                points.get(1).id()
        );
        assertEquals(
                "openworld_rpg:r01/broken_road_marker_probe",
                points.get(2).id()
        );
        assertEquals(
                "openworld_rpg:r01/roadside_trouble_probe",
                points.get(3).id()
        );
        assertEquals(
                "openworld_rpg:r01/lost_cargo_probe",
                points.get(4).id()
        );
        assertEquals(
                "openworld_rpg:r01/quarry_waystone_probe",
                points.get(5).id()
        );
        assertEquals(
                "openworld_rpg:r01/quarry_overlook_probe",
                points.get(6).id()
        );
        assertEquals(
                "openworld_rpg:r01/quarry_lower_entrance_probe",
                points.get(7).id()
        );

        assertFalse(points.stream().anyMatch(point ->
                point.id().contains("upper_gallery_review")
        ));
        assertFalse(points.stream().anyMatch(point ->
                point.id().contains("earthloong_chamber_review")
        ));
    }

    @Test
    void areaCentersAndRouteWaypointsAreReviewableWithoutBecomingAuthority() {
        var data = R01SpatialBindingLoader.loadBundled();
        var points = R01SpatialReviewPlan.surfacePoints(data);

        var roadsideArea = points.stream()
                .filter(point -> point.id().equals(
                        "openworld_rpg:r01/roadside_trouble_scene_candidate"
                ))
                .findFirst()
                .orElseThrow();
        assertEquals("area", roadsideArea.kind());
        assertEquals(-2472, roadsideArea.x());
        assertEquals(4388, roadsideArea.z());
        assertEquals(null, roadsideArea.y());

        var routeEnd = points.stream()
                .filter(point -> point.id().equals(
                        "openworld_rpg:r01/alderford_to_quarry_surface_candidate#7"
                ))
                .findFirst()
                .orElseThrow();
        assertEquals("route", routeEnd.kind());
        assertEquals(-2600, routeEnd.x());
        assertEquals(4700, routeEnd.z());

        assertTrue(points.stream().allMatch(point ->
                data.productionAnchor(point.id()).isEmpty()
                        || point.kind().equals("route")
                        || point.kind().equals("area")
        ));
        assertFalse(data.productionReady());
    }
}
