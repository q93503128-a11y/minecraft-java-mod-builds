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
            assertTrue(Set.of("LOOP", "ROAM").contains(patrol.mode()), patrol.locator());
            assertTrue(patrol.dwellMinTicks() >= 0 && patrol.dwellMaxTicks() >= patrol.dwellMinTicks(), patrol.locator());
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
                int combatSize = CampaignEncounterCatalog.spec(encounter.combatEncounterId()).enemies().size();
                assertTrue(encounter.fieldVisibleCount() >= 1 && encounter.fieldVisibleCount() <= combatSize,
                        encounter.locator() + " visible=" + encounter.fieldVisibleCount() + " combat=" + combatSize);
            }
        }
        var first = DrehmalFirstRouteCatalog.route().encounters().stream()
                .filter(encounter -> encounter.locator().equals("turnbound:encounter/capital_valley/first_common"))
                .findFirst().orElseThrow();
        assertTrue(first.combatEncounterId().equals("CV_FIRST_COMMON"));
        assertFalse(first.productionEnabled(), "26.2 survey gate must still block spatial activation");

        var cave = DrehmalFirstRouteCatalog.route().encounters().stream()
                .filter(encounter -> encounter.locator().equals("turnbound:encounter/capital_valley/warning_cave_elite"))
                .findFirst().orElseThrow();
        assertTrue(cave.combatEncounterId().equals("CV_WARNING_CAVE_ELITE"));
        assertFalse(cave.productionEnabled(), "Warning Cave must remain survey-gated");

        var road = DrehmalFirstRouteCatalog.route().encounters().stream()
                .filter(encounter -> encounter.locator().equals("turnbound:encounter/capital_valley/drabyel_approach_patrol"))
                .findFirst().orElseThrow();
        assertTrue(road.combatEncounterId().equals("CV_DRABYEL_ROAD"));
        assertFalse(road.productionEnabled(), "Drabyel road must also remain survey-gated");
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
