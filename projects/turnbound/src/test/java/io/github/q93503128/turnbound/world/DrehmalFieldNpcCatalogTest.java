package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DrehmalFieldNpcCatalogTest {
    @Test
    void fieldNpcCatalogCarriesDiscoverableHiddenQuestOffers() {
        assertTrue(DrehmalFieldNpcCatalog.validate().isEmpty(),
                () -> String.join("; ", DrehmalFieldNpcCatalog.validate()));
        long offers = DrehmalFieldNpcCatalog.all().stream().filter(npc -> !npc.questOfferFlag().isBlank()).count();
        assertEquals(2, offers);
        assertEquals(3, DrehmalFieldNpcCatalog.all().stream().filter(npc -> !npc.progressFlag().isBlank()).count());
        assertEquals(2,DrehmalFieldNpcCatalog.all().stream()
                .filter(npc->npc.progressRequiresFlag().equals(AvsalExpansionProgress.OUTSKIRTS_REACHED)).count());
        assertEquals(1,DrehmalFieldNpcCatalog.all().stream()
                .filter(npc->npc.progressRequiresFlag().equals(DrabyelLocalArcProgress.ACCEPTED)).count());
        for (var npc : DrehmalFieldNpcCatalog.all()) {
            if (npc.questOfferFlag().isBlank()) continue;
            assertTrue(npc.questOfferFlag().startsWith("HIDDEN_"));
            assertTrue(npc.questOfferId().startsWith("turnbound:quest/drehmal/hidden_"));
            assertFalse(npc.questOfferDialogue().isBlank());
        }
    }
}
