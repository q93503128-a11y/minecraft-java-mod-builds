package dev.moonseungjun.openworldrpg.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingLoader;
import org.junit.jupiter.api.Test;

class R01SpatialBindingDataTest {
    @Test
    void bundledPass2DataLoadsButCannotActAsProductionAuthority() {
        var data = R01SpatialBindingLoader.loadBundled();

        assertEquals(2, data.schemaVersion());
        assertEquals(
                "openworld_rpg:r01/azari_spatial_candidates",
                data.id()
        );
        assertEquals("AzariNEW4252026", data.mapBuild());
        assertFalse(data.productionReady());
        assertTrue(data.productionAnchor(
                "openworld_rpg:r01/alderford_center"
        ).isEmpty());

        var alderford = data.anchor(
                "openworld_rpg:r01/alderford_center"
        ).orElseThrow();
        assertEquals(-2208, alderford.x());
        assertEquals(67, alderford.y());
        assertEquals(4000, alderford.z());
        assertEquals("candidate", alderford.status());

        var quarryOverlook = data.anchor(
                "openworld_rpg:r01/quarry_overlook_probe"
        ).orElseThrow();
        assertEquals(-2628, quarryOverlook.x());
        assertEquals(96, quarryOverlook.y());
        assertEquals(4764, quarryOverlook.z());

        assertTrue(data.anchor(
                "openworld_rpg:r01/broken_road_marker_probe"
        ).orElseThrow().y() == null);
        assertTrue(data.anchor(
                "openworld_rpg:r01/roadside_trouble_probe"
        ).orElseThrow().y() == null);
        assertTrue(data.anchor(
                "openworld_rpg:r01/lost_cargo_probe"
        ).orElseThrow().y() == null);

        var route = data.route(
                "openworld_rpg:r01/alderford_to_quarry_surface_candidate"
        ).orElseThrow();
        assertEquals(9, route.points().size());
        assertEquals(812.38, route.horizontalLengthBlocks(), 0.02);
    }

    @Test
    void quarryInteriorReviewVolumesStayCandidateAndNeverLeakAsProduction() {
        var data = R01SpatialBindingLoader.loadBundled();

        assertEquals(5, data.volumes().size());
        assertTrue(data.productionVolume(
                "openworld_rpg:r01/quarry/earthloong_chamber_review"
        ).isEmpty());

        var upper = data.volume(
                "openworld_rpg:r01/quarry/upper_gallery_review"
        ).orElseThrow();
        assertEquals("natural_seam", upper.reviewMode());
        assertEquals(-2576, upper.minX());
        assertEquals(32, upper.minY());
        assertEquals(4753, upper.maxZ());

        var earthloong = data.volume(
                "openworld_rpg:r01/quarry/earthloong_chamber_review"
        ).orElseThrow();
        assertEquals("solid_carve_probe", earthloong.reviewMode());
        assertEquals(32, earthloong.maxX() - earthloong.minX() + 1);
        assertEquals(12, earthloong.maxY() - earthloong.minY() + 1);
        assertEquals(32, earthloong.maxZ() - earthloong.minZ() + 1);
        assertEquals("candidate", earthloong.status());
    }
    @Test
    void alderfordCoreAndQuarrySurfaceReviewBoxesRemainExplicitCandidates() {
        var data = R01SpatialBindingLoader.loadBundled();

        var alderford = data.area(
                "openworld_rpg:r01/alderford_core_candidate"
        ).orElseThrow();
        assertEquals(-2288, alderford.minX());
        assertEquals(-2112, alderford.maxX());
        assertEquals(3968, alderford.minZ());
        assertEquals(4080, alderford.maxZ());
        assertEquals("candidate", alderford.status());

        var quarry = data.area(
                "openworld_rpg:r01/quarry_surface_candidate"
        ).orElseThrow();
        assertEquals(-2688, quarry.minX());
        assertEquals(-2528, quarry.maxX());
        assertEquals(4624, quarry.minZ());
        assertEquals(4832, quarry.maxZ());
        assertEquals("candidate", quarry.status());
    }
}
