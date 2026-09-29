package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrabyelHubSurveyServiceTest {
    @Test
    void candidateJsonNeverAutoPromotesAService() {
        var market = DrabyelHubServiceCatalog.hub().services().stream()
                .filter(service -> service.role().equals("MARKET"))
                .findFirst().orElseThrow();
        var inspection = new DrehmalRouteSurveyService.Inspection(
                516, 67, 1854, -90.0F,
                true, true, true, 0, false,
                true, true,
                "turnbound:hub/new_drabyel", 54);
        var candidate = new DrabyelHubSurveyService.Candidate(market, inspection, 54);

        assertTrue(candidate.candidateGeometryPass());
        assertTrue(candidate.summary().contains("MARKET"));
        assertTrue(candidate.summary().contains("geometry=PASS"));
        assertTrue(candidate.catalogPatchJson().contains("\"position\":{\"x\":516,\"y\":67,\"z\":1854}"));
        assertTrue(candidate.catalogPatchJson().contains("\"verifiedIn26_2\":false"));
        assertTrue(candidate.catalogPatchJson().contains("\"productionEnabled\":false"));
    }

    @Test
    void serviceOutsideHubSurveyRadiusCannotPassGeometryGate() {
        var smith = DrabyelHubServiceCatalog.hub().services().stream()
                .filter(service -> service.role().equals("BLACKSMITH"))
                .findFirst().orElseThrow();
        var inspection = new DrehmalRouteSurveyService.Inspection(
                900, 67, 2200, 0.0F,
                true, true, true, 0, false,
                true, true,
                "turnbound:hub/new_drabyel", 600);
        var candidate = new DrabyelHubSurveyService.Candidate(smith, inspection, 600);
        assertFalse(candidate.candidateGeometryPass());
    }
}
