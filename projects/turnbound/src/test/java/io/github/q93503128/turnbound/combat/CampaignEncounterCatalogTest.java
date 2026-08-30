package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.world.CampaignProgressStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignEncounterCatalogTest {
    private final UUID playerId = UUID.randomUUID();

    @AfterEach
    void cleanup() { CampaignProgressStore.removeRuntime(playerId); }

    @Test
    void allV04FieldAndBossEncountersAreRuntimeAddressable() {
        assertEquals(30, CampaignEncounterCatalog.all().size());
        for (var encounter : CampaignEncounterCatalog.all()) {
            assertTrue(CampaignEncounterCatalog.contains(encounter.id()));
            BattleState state = CampaignEncounterCatalog.createBattle(playerId, encounter.id());
            assertEquals(4 + encounter.enemies().size(), state.combatants().size());
            assertTrue(state.combatants().stream().filter(c -> c.side() == CombatantSide.ENEMY).count() <= 5);
        }
    }

    @Test
    void legacySouthgateIdsResolveToCanonicalV04EncounterIds() {
        assertEquals("ENC_M01", CampaignEncounterCatalog.canonicalId("southgate_enc_m01"));
        assertEquals("BATTLE_B01", CampaignEncounterCatalog.canonicalId("southgate_b01_graul"));
    }

    @Test
    void campaignBattleUsesGrowthAndEquipmentAwareAllyDefinition() {
        var weapon = CampaignProgressStore.grantEquipment(playerId, "W01");
        CampaignProgressStore.equip(playerId, "P01", weapon.instanceId());
        BattleState state = CampaignEncounterCatalog.createBattle(playerId, "ENC_M01");
        CombatantState kyren = state.combatant("ally_p01");
        assertEquals(CampaignProgressStore.finalStats(playerId, "P01"), kyren.definition().stats());
    }
}
