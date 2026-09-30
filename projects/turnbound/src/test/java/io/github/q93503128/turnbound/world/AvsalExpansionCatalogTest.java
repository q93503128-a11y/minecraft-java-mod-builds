package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvsalExpansionCatalogTest {
    @Test
    void firstProductionSliceIsSourceBackedAndValid() {
        assertTrue(AvsalExpansionCatalog.validate().isEmpty(),
                () -> String.join("; ", AvsalExpansionCatalog.validate()));
        assertEquals("zachaa/DrehmalMap", AvsalExpansionCatalog.plan().source().repository());
        assertEquals("72d82180cbe3f950f068cf2d8e8668c6b09d5c58", AvsalExpansionCatalog.plan().source().commit());
    }

    @Test
    void routeProvidesCommonAndOptionalEliteWithExistingProductionVisuals() {
        var common = AvsalExpansionCatalog.site(AvsalExpansionRuntime.ROAD_PATROL_SITE);
        var elite = AvsalExpansionCatalog.site(AvsalExpansionRuntime.ROAD_ELITE_SITE);
        assertEquals("AV_ROAD_PATROL", common.combatEncounterId());
        assertEquals("COMMON", common.tier());
        assertEquals("AV_ROAD_ELITE", elite.combatEncounterId());
        assertEquals("ELITE", elite.tier());
        assertTrue(CampaignEncounterCatalog.contains(common.combatEncounterId()));
        assertTrue(CampaignEncounterCatalog.contains(elite.combatEncounterId()));
        var spec = CampaignEncounterCatalog.spec("AV_ROAD_PATROL");
        assertTrue(spec.enemies().contains("CV_B"));
        assertTrue(spec.enemies().contains("CV_C"));
        assertTrue(spec.enemies().contains("E005"));
    }
}
