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
    void firstRouteUsesOneMainQuestAndSurveyGatedPlaceSideQuests() {
        assertEquals(1, DrehmalQuestCatalog.all().stream()
                .filter(quest -> quest.kind() == DrehmalQuestCatalog.Kind.MAIN)
                .count());
        assertTrue(DrehmalQuestCatalog.all().stream()
                .filter(quest -> quest.kind() == DrehmalQuestCatalog.Kind.SIDE)
                .allMatch(DrehmalQuestCatalog.Quest::requiresProductionEncounter));
        assertTrue(DrehmalQuestCatalog.all().stream()
                .allMatch(quest -> quest.rewardCrystal() > 0 && quest.rewardGold() > 0));
        assertEquals(2, DrehmalQuestCatalog.all().stream()
                .filter(quest -> quest.kind() == DrehmalQuestCatalog.Kind.HIDDEN)
                .count());
    }
}
