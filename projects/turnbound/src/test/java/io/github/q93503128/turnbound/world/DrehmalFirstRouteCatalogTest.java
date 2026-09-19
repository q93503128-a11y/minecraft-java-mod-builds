package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalFirstRouteCatalogTest {
    @Test
    void firstRouteCatalogIsInternallyValid() {
        assertTrue(DrehmalFirstRouteCatalog.validate().isEmpty(),
                () -> String.join("; ", DrehmalFirstRouteCatalog.validate()));
    }

    @Test
    void everyRouteSiteUsesASourceBackedSurveyAnchor() {
        for (DrehmalFirstRouteCatalog.Site site : DrehmalFirstRouteCatalog.route().sites()) {
            assertNotNull(DrehmalWorldProfile.enabled(site.surveySeedAnchor()), site.locator());
        }
    }

    @Test
    void productionEntriesCannotBypassThe26_2SurveyGate() {
        for (DrehmalFirstRouteCatalog.Site site : DrehmalFirstRouteCatalog.route().sites()) {
            if (site.productionEnabled()) {
                assertTrue(site.verifiedIn26_2(), site.locator());
                assertNotNull(site.runtimePosition(), site.locator());
            }
        }
        for (DrehmalFirstRouteCatalog.Footprint footprint : DrehmalFirstRouteCatalog.route().footprints()) {
            if (footprint.productionEnabled()) assertTrue(footprint.verifiedIn26_2(), footprint.locator());
        }
        for (DrehmalFirstRouteCatalog.Patrol patrol : DrehmalFirstRouteCatalog.route().patrols()) {
            if (patrol.productionEnabled()) {
                assertTrue(patrol.verifiedIn26_2(), patrol.locator());
                assertTrue(patrol.points().size() >= 2, patrol.locator());
            }
        }
        for (DrehmalFirstRouteCatalog.EncounterSlot encounter : DrehmalFirstRouteCatalog.route().encounters()) {
            if (encounter.productionEnabled()) assertTrue(encounter.verifiedIn26_2(), encounter.locator());
        }
    }

    @Test
    void authoredCombatBindingsResolveBeforeSpatialPromotion() {
        for (DrehmalFirstRouteCatalog.EncounterSlot encounter : DrehmalFirstRouteCatalog.route().encounters()) {
            if (!encounter.combatEncounterId().isBlank()) {
                assertTrue(CampaignEncounterCatalog.contains(encounter.combatEncounterId()),
                        encounter.locator() + " -> " + encounter.combatEncounterId());
            }
        }
        var first = DrehmalFirstRouteCatalog.route().encounters().stream()
                .filter(encounter -> encounter.locator().equals("turnbound:encounter/capital_valley/first_common"))
                .findFirst().orElseThrow();
        assertTrue(first.combatEncounterId().equals("CV_FIRST_COMMON"));
        assertFalse(first.productionEnabled(), "26.2 survey gate must still block spatial activation");
    }

    @Test
    void catalogContainsTheRequiredFirstRouteProductionRoles() {
        Set<String> kinds = DrehmalFirstRouteCatalog.route().sites().stream()
                .map(DrehmalFirstRouteCatalog.Site::kind)
                .collect(Collectors.toSet());
        assertTrue(kinds.contains("START_CANDIDATE"));
        assertTrue(kinds.contains("ENCOUNTER_ZONE"));
        assertTrue(kinds.contains("BREATHING_ZONE"));
        assertTrue(kinds.contains("ELITE_ZONE"));
        assertTrue(kinds.contains("REST_ZONE"));
        assertTrue(kinds.contains("PATROL_ZONE"));
        assertTrue(kinds.contains("HUB_SAFE"));

        assertFalse(DrehmalFirstRouteCatalog.route().footprints().isEmpty());
        assertFalse(DrehmalFirstRouteCatalog.route().patrols().isEmpty());
        assertFalse(DrehmalFirstRouteCatalog.route().encounters().isEmpty());
    }
}
