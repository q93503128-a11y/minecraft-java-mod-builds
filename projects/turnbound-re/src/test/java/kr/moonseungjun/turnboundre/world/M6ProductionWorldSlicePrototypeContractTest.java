package kr.moonseungjun.turnboundre.world;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M6ProductionWorldSlicePrototypeContractTest {
    @Test
    void productionPlanKeepsReadableScaleAndDistinctLandmarks() {
        assertEquals(java.util.List.of(), ProductionWorldSlicePlan.validate());
        assertEquals(2, ProductionWorldSlicePlan.ROUTE_HALF_WIDTH);
        assertEquals(14, ProductionWorldSlicePlan.LANTERN_SPACING);
        assertTrue(ProductionWorldSlicePlan.HUB_HALF_WIDTH >= 10);
        assertTrue(ProductionWorldSlicePlan.HUB_HALF_DEPTH >= 8);

        Set<String> ids = new HashSet<>();
        for (ProductionWorldSlicePlan.Footprint footprint : ProductionWorldSlicePlan.LANDMARK_FOOTPRINTS) {
            assertTrue(ids.add(footprint.id()));
        }
        for (int i = 0; i < ProductionWorldSlicePlan.LANDMARK_FOOTPRINTS.size(); i++) {
            for (int j = i + 1; j < ProductionWorldSlicePlan.LANDMARK_FOOTPRINTS.size(); j++) {
                assertFalse(ProductionWorldSlicePlan.LANDMARK_FOOTPRINTS.get(i)
                        .overlaps(ProductionWorldSlicePlan.LANDMARK_FOOTPRINTS.get(j)));
            }
        }
    }

    @Test
    void functionalLocatorsRemainTheOnlyGameplayIdentity() {
        assertEquals("turnbound_re:region_01/ore_outcrop", FunctionalWorldSliceLayout.ORE_OUTCROP.locator());
        assertEquals("turnbound_re:region_01/riverside_plot", FunctionalWorldSliceLayout.RIVERSIDE_PLOT.locator());
        assertEquals("turnbound_re:region_01/river_pool", FunctionalWorldSliceLayout.RIVER_POOL.locator());
        assertEquals("turnbound_re:region_01/overworld_patrol", FunctionalWorldSliceLayout.OVERWORLD_PATROL.locator());
        assertEquals("turnbound_re:region_01/rift_elite", FunctionalWorldSliceLayout.RIFT_ELITE.locator());
    }

    @Test
    void firstRegionMaintainsResourceChoiceBeforeCombatEscalation() {
        int branchX = FunctionalWorldSliceLayout.ORE_OUTCROP.offsetX();
        int patrolX = FunctionalWorldSliceLayout.OVERWORLD_PATROL.offsetX();
        int eliteX = FunctionalWorldSliceLayout.RIFT_ELITE.offsetX();

        assertTrue(branchX < patrolX);
        assertTrue(patrolX < eliteX);
        assertTrue(eliteX - patrolX >= 16);
        assertTrue(ProductionWorldSlicePlan.ROUTE_END_X >= eliteX + ProductionWorldSlicePlan.ELITE_HALF_SIZE);
    }
}
