package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalQuestCatalogTest {
    @Test
    void firstRouteQuestCatalogIsInternallyValid() {
        assertTrue(DrehmalQuestCatalog.validate().isEmpty(),
                () -> String.join("; ", DrehmalQuestCatalog.validate()));
    }

    @Test
    void routeUsesSequentialMainQuestsAndSurveyGatedPlaceSideQuests() {
        assertEquals(3, DrehmalQuestCatalog.all().stream()
                .filter(quest -> quest.kind() == DrehmalQuestCatalog.Kind.MAIN)
                .count());
        assertTrue(DrehmalQuestCatalog.all().stream()
                .filter(quest -> quest.kind() == DrehmalQuestCatalog.Kind.SIDE)
                .allMatch(DrehmalQuestCatalog.Quest::requiresProductionEncounter));
        var patrol=DrehmalQuestCatalog.all().stream()
                .filter(quest->quest.id().equals("turnbound:quest/drehmal/drabyel_approach_patrol")).findFirst().orElseThrow();
        assertEquals(DrehmalQuestCatalog.Kind.MAIN,patrol.kind());
        assertEquals(DrabyelOpeningTutorial.GREETER_FLAG,patrol.activationFlag());
        var local=DrehmalQuestCatalog.all().stream()
                .filter(quest->quest.id().equals("turnbound:quest/drehmal/drabyel_local_signs")).findFirst().orElseThrow();
        assertEquals(DrabyelLocalArcProgress.ACCEPTED,local.activationFlag());
        assertEquals(DrabyelLocalArcProgress.COMPLETE,local.completionFlag());
        assertTrue(DrehmalQuestCatalog.all().stream()
                .allMatch(quest -> quest.rewardCrystal() > 0 && quest.rewardGold() > 0));
        assertEquals(2, DrehmalQuestCatalog.all().stream()
                .filter(quest -> quest.kind() == DrehmalQuestCatalog.Kind.HIDDEN)
                .count());
    }
}
