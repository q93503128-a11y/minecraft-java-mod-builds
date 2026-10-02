package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrehmalFieldNpcCatalogTest {
    @Test
    void fieldNpcCatalogSeparatesHiddenOffersFromAuthoredRoadSideOffers() {
        assertTrue(DrehmalFieldNpcCatalog.validate().isEmpty(),
                () -> String.join("; ", DrehmalFieldNpcCatalog.validate()));

        long allOffers = DrehmalFieldNpcCatalog.all().stream()
                .filter(npc -> !npc.questOfferFlag().isBlank()).count();
        long hiddenOffers = DrehmalFieldNpcCatalog.all().stream()
                .filter(npc -> npc.questOfferFlag().startsWith("HIDDEN_")).count();
        assertEquals(8, allOffers);
        assertEquals(2, hiddenOffers);

        assertEquals(3, DrehmalFieldNpcCatalog.all().stream()
                .filter(npc -> !npc.progressFlag().isBlank()).count());
        assertEquals(2, DrehmalFieldNpcCatalog.all().stream()
                .filter(npc -> npc.progressRequiresFlag().equals(AvsalExpansionProgress.OUTSKIRTS_REACHED)).count());
        assertEquals(1, DrehmalFieldNpcCatalog.all().stream()
                .filter(npc -> npc.progressRequiresFlag().equals(DrabyelLocalArcProgress.ACCEPTED)).count());

        for (var npc : DrehmalFieldNpcCatalog.all()) {
            if (!npc.questOfferFlag().startsWith("HIDDEN_")) continue;
            assertTrue(npc.questOfferId().startsWith("turnbound:quest/drehmal/hidden_"));
            assertFalse(npc.questOfferDialogue().isBlank());
        }

        assertEquals(5, DrehmalFieldNpcCatalog.all().stream()
                .filter(npc -> !npc.questOfferFlag().isBlank())
                .filter(npc -> !npc.questOfferFlag().startsWith("HIDDEN_"))
                .count());

        var deren = DrehmalFieldNpcCatalog.npc("turnbound:npc/avsal/road_courier");
        assertNotNull(deren);
        assertEquals("AVSAL_ROAD_COURIER_OFFERED", deren.questOfferFlag());
        assertEquals("SQ_AV02_RELAY_SENTRIES", deren.questOfferId());
        assertFalse(deren.questOfferDialogue().isBlank());
    }
}
