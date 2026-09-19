package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class DrehmalLocationBannerRulesTest {
    @Test
    void onlyPromotedLandmarkLikeSitesCanDriveLocationTitles() {
        var tower = site("tower", "BREATHING_ZONE", "Capital Valley Tower", 36, 0, true, true, 0, 0);
        var draftGuide = site("guide", "GUIDE_CANDIDATE", "첫 길잡이 후보 지점", 36, 0, true, true, 50, 0);
        var unverifiedTown = site("town", "HUB_SAFE", "New Drabyel", 64, 0, false, false, 100, 0);

        assertEquals(tower, DrehmalLocationBannerRules.current(List.of(tower, draftGuide, unverifiedTown), 10, 0));
        assertNull(DrehmalLocationBannerRules.current(List.of(draftGuide, unverifiedTown), 50, 0));
    }

    @Test
    void bannerActivationUsesExistingSafetyOrEncounterRadius() {
        var cave = site("cave", "ELITE_ZONE", "경고 동굴", 0, 28, true, true, 0, 0);

        assertEquals(cave, DrehmalLocationBannerRules.current(List.of(cave), 27.9, 0.5));
        assertNull(DrehmalLocationBannerRules.current(List.of(cave), 29.1, 0.5));
    }

    @Test
    void currentCatalogNeverPromotesCandidateCopyAsAPlayerLocationTitle() {
        for (var site : DrehmalFirstRouteCatalog.route().sites()) {
            if (!DrehmalLocationBannerRules.eligible(site)) continue;
            assertFalse(site.playerLabel().contains("후보"), site.locator());
        }
    }

    private static DrehmalFirstRouteCatalog.Site site(
            String id, String kind, String label, int safety, int encounter,
            boolean verified, boolean enabled, int x, int z
    ) {
        return new DrehmalFirstRouteCatalog.Site(
                "turnbound:test/" + id,
                kind,
                DrehmalWorldProfile.PRIMAL_CAVERNS,
                label,
                new DrehmalFirstRouteCatalog.Position(x, 70, z),
                safety,
                encounter,
                verified,
                enabled);
    }
}
