package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrehmalQuestCatalogTest {
    @Test
    void firstRouteQuestCatalogIsInternallyValid() {
        assertTrue(DrehmalQuestCatalog.validate().isEmpty(),
                () -> String.join("; ", DrehmalQuestCatalog.validate()));
    }

    @Test
    void routeUsesSequentialMainQuestsAndSupportsCombatAndNonCombatSideQuests() {
        assertEquals(4, DrehmalQuestCatalog.all().stream()
                .filter(quest -> quest.kind() == DrehmalQuestCatalog.Kind.MAIN)
                .count());

        var warning = DrehmalQuestCatalog.all().stream()
                .filter(q -> q.id().equals("turnbound:quest/drehmal/warning_cave_elite"))
                .findFirst().orElseThrow();
        assertEquals(DrehmalQuestCatalog.Kind.SIDE, warning.kind());
        assertTrue(warning.requiresProductionEncounter());
        assertEquals("CV_WARNING_CAVE_ELITE", warning.encounterId());

        var camp = DrehmalQuestCatalog.all().stream()
                .filter(q -> q.id().equals("turnbound:quest/drehmal/explorer_camp_records"))
                .findFirst().orElseThrow();
        assertEquals(DrehmalQuestCatalog.Kind.SIDE, camp.kind());
        assertFalse(camp.requiresProductionEncounter());
        assertEquals(DrehmalFirstRouteProgress.CAMP_REACHED, camp.completionFlag());

        var patrol = DrehmalQuestCatalog.all().stream()
                .filter(quest -> quest.id().equals("turnbound:quest/drehmal/drabyel_approach_patrol"))
                .findFirst().orElseThrow();
        assertEquals(DrehmalQuestCatalog.Kind.MAIN, patrol.kind());
        assertEquals(DrabyelOpeningTutorial.GREETER_FLAG, patrol.activationFlag());

        var local = DrehmalQuestCatalog.all().stream()
                .filter(quest -> quest.id().equals("turnbound:quest/drehmal/drabyel_local_signs"))
                .findFirst().orElseThrow();
        assertEquals(DrabyelLocalArcProgress.ACCEPTED, local.activationFlag());
        assertEquals(DrabyelLocalArcProgress.COMPLETE, local.completionFlag());
        assertTrue(local.objective().contains("아렌이"));

        var regional = DrehmalQuestCatalog.all().stream()
                .filter(quest -> quest.id().equals("turnbound:quest/drehmal/capital_valley_scouting"))
                .findFirst().orElseThrow();
        assertEquals(DrehmalQuestCatalog.Kind.MAIN, regional.kind());
        assertEquals(DrabyelLocalArcProgress.REGIONAL_ACCEPTED, regional.activationFlag());
        assertEquals(DrabyelLocalArcProgress.REGIONAL_COMPLETE, regional.completionFlag());

        assertTrue(DrehmalQuestCatalog.all().stream()
                .allMatch(quest -> quest.rewardCrystal() > 0 && quest.rewardGold() > 0));
        assertEquals(2, DrehmalQuestCatalog.all().stream()
                .filter(quest -> quest.kind() == DrehmalQuestCatalog.Kind.HIDDEN)
                .count());
    }
}
