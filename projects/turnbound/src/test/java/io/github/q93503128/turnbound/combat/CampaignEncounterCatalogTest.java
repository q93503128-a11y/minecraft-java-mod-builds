package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.world.CampaignProgressStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignEncounterCatalogTest {
    private final UUID playerId = UUID.randomUUID();

    @AfterEach
    void cleanup() { CampaignProgressStore.removeRuntime(playerId); }

    @Test
    void allV04FieldAndBossEncountersAreRuntimeAddressable() {
        assertEquals(30, CampaignEncounterCatalog.all().size());
        int activeAllies = CampaignProgressStore.activeParty(playerId).size();
        assertTrue(activeAllies >= 1 && activeAllies <= 4);
        for (var encounter : CampaignEncounterCatalog.all()) {
            assertTrue(CampaignEncounterCatalog.contains(encounter.id()));
            BattleState state = CampaignEncounterCatalog.createBattle(playerId, encounter.id());
            assertEquals(activeAllies + encounter.enemies().size(), state.combatants().size());
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

    @Test
    void transientBattleStateNeverLeaksIntoTheNextBattle() {
        CombatantState first = CampaignEncounterCatalog.createBattle(playerId, "ENC_M01").combatant("ally_p01");
        first.takeDamage(Math.max(1, first.maxHp() / 3));
        first.addBarrier(120);
        first.setGauge(777);
        first.setCooldown(first.definition().basicSkillId(), 2);
        first.setCounter("temporary_test_counter", 4);
        first.setFlag("temporary_test_flag");

        CombatantState fresh = CampaignEncounterCatalog.createBattle(playerId, "ENC_M01").combatant("ally_p01");
        assertEquals(fresh.maxHp(), fresh.hp());
        assertEquals(0, fresh.barrier());
        assertEquals(0, fresh.gauge());
        assertFalse(fresh.downed());
        assertTrue(fresh.cooldownsView().isEmpty());
        assertTrue(fresh.statusesView().isEmpty());
        assertEquals(0, fresh.counter("temporary_test_counter"));
        assertFalse(fresh.flag("temporary_test_flag"));
    }
}
