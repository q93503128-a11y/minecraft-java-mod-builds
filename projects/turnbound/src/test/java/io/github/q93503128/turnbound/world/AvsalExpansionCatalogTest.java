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
        assertTrue(AvsalExpansionCatalog.site(AvsalExpansionRuntime.SCAVENGER_CLUE_SITE).kind().equals("NPC_ZONE"));
        assertTrue(AvsalExpansionCatalog.site(AvsalExpansionRuntime.SURVIVOR_CLUE_SITE).kind().equals("NPC_ZONE"));
        assertTrue(AvsalExpansionCatalog.site(AvsalExpansionRuntime.RECORDS_CLUE_SITE).kind().equals("CLUE_ZONE"));
        var spec = CampaignEncounterCatalog.spec("AV_ROAD_PATROL");
        assertTrue(spec.enemies().contains("CV_B"));
        assertTrue(spec.enemies().contains("CV_C"));
        assertTrue(spec.enemies().contains("E005"));

        var hounds = AvsalExpansionCatalog.site(AvsalExpansionRuntime.ROAD_HOUNDS_SITE);
        var sentries = AvsalExpansionCatalog.site(AvsalExpansionRuntime.RELAY_SENTRIES_SITE);
        assertEquals("AV_ROAD_HOUNDS", hounds.combatEncounterId());
        assertEquals("AV_RELAY_SENTRIES", sentries.combatEncounterId());
        assertTrue(hounds.patrolSeeds().size() >= 2);
        assertTrue(sentries.patrolSeeds().size() >= 2);
        assertTrue(AvsalExpansionCatalog.site(AvsalExpansionRuntime.ROAD_COURIER_SITE).kind().equals("NPC_ZONE"));
        assertTrue(AvsalExpansionCatalog.site(AvsalExpansionRuntime.WAYSIDE_CACHE_SITE).detectionRadius() >= 12);
        assertTrue(AvsalExpansionCatalog.site("turnbound:site/avsal/contract_broker").kind().equals("NPC_ZONE"));

        var relayGuard = AvsalExpansionCatalog.site(AvsalExpansionRuntime.RELAY_GUARD_SITE);
        var firstBoss = AvsalExpansionCatalog.site(AvsalExpansionRuntime.FIRST_BOSS_SITE);
        assertEquals("AV_RELAY_GUARD", relayGuard.combatEncounterId());
        assertEquals("COMMON", relayGuard.tier());
        assertEquals("AV_FIRST_BOSS", firstBoss.combatEncounterId());
        assertEquals("BOSS", firstBoss.tier());
        assertTrue(CampaignEncounterCatalog.contains("AV_FIRST_BOSS"));
        assertTrue(firstBoss.patrolSeeds().size() >= 2);
    }
}
