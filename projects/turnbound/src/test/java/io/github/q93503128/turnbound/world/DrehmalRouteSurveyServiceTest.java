package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalRouteSurveyServiceTest {
    @Test
    void inspectionProducesCatalogReadyJsonWithoutPromotingAnything() {
        var inspection = new DrehmalRouteSurveyService.Inspection(
                500, 72, 1700, -45.5F,
                true, true, true, 1, false,
                true, true,
                "turnbound:hub/new_drabyel", 101);

        assertTrue(inspection.siteGeometryPass());
        assertEquals("{\"x\":500,\"y\":72,\"z\":1700}", inspection.positionJson());
        assertEquals("{\"center\":{\"x\":500,\"y\":72,\"z\":1700},\"yaw\":-45.5}", inspection.arenaJson());
        assertTrue(inspection.summary().contains("arenaCoop4=PASS"));
    }

    @Test
    void cliffRiskBlocksSiteGeometryEvenWhenGroundItselfLooksFlat() {
        var inspection = new DrehmalRouteSurveyService.Inspection(
                0, 64, 0, 0.0F,
                true, true, true, 0, true,
                true, false,
                "turnbound:landmark/primal_caverns", 50);
        assertFalse(inspection.siteGeometryPass());
    }

    @Test
    void routeSeedsRemainSurveyOnlyUntilCatalogGateChanges() {
        assertFalse(DrehmalRouteSurveyPlan.routeSeedLines().isEmpty());
        assertTrue(DrehmalFirstRouteCatalog.productionEncounters().isEmpty());
    }
}
