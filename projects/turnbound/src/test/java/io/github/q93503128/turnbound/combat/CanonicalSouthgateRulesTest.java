package io.github.q93503128.turnbound.combat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CanonicalSouthgateRulesTest {
    @Test
    void encounterTemplatesMatchV04() {
        assertEquals(List.of("E001", "E001"), SouthgateEncounterCatalog.spec(SouthgateEncounterCatalog.ENC_M01).enemyDefinitionIds());
        assertEquals(List.of("E001", "E002"), SouthgateEncounterCatalog.spec(SouthgateEncounterCatalog.ENC_M02).enemyDefinitionIds());
        assertEquals(List.of("E004", "E004"), SouthgateEncounterCatalog.spec(SouthgateEncounterCatalog.ENC_M03).enemyDefinitionIds());
        assertEquals(List.of("E003", "E002"), SouthgateEncounterCatalog.spec(SouthgateEncounterCatalog.ENC_M04).enemyDefinitionIds());
        assertEquals(List.of("E005", "E001", "E001"), SouthgateEncounterCatalog.spec(SouthgateEncounterCatalog.ENC_M05).enemyDefinitionIds());
    }

    @Test
    void canonicalEnemyStatsAndCampaignPartyAreUsed() {
        assertEquals(new BattleStats(650, 125, 50, 78), SouthgateEncounterCatalog.enemyDefinition("E003").stats());
        assertEquals("불안정 폭발체", SouthgateEncounterCatalog.enemyDefinition("E003").name());
        assertEquals(new BattleStats(680, 98, 64, 100), SouthgateEncounterCatalog.enemyDefinition("E004").stats());
        assertEquals("길목 약탈자", SouthgateEncounterCatalog.enemyDefinition("E004").name());
        assertEquals(new BattleStats(2800, 150, 115, 92), SouthgateEncounterCatalog.enemyDefinition("B01").stats());

        BattleState campaign = SouthgateEncounterCatalog.createBattle(SouthgateEncounterCatalog.ENC_M01);
        assertEquals(List.of("P01", "P03", "P04", "F03"), campaign.combatants().stream()
                .filter(unit -> unit.side() == CombatantSide.ALLY)
                .map(unit -> unit.definition().id()).toList());
    }

    @Test
    void e003ArmedExplosionDownsItself() {
        BattleState state = new BattleState(List.of(
                new CombatantState("ally", PrototypeRoster.kyren(), CombatantSide.ALLY, 0),
                new CombatantState("e003", PrototypeRoster.unstableExploder(), CombatantSide.ENEMY, 1)
        ));
        BattleEngine engine = new BattleEngine(state);
        state.combatant("e003").setGauge(1000);
        assertEquals("e003", engine.nextReady().instanceId());
        engine.useSkill("e003", "e003_arm");
        assertNotNull(state.combatant("e003").status("e003_armed"));
        state.combatant("e003").setGauge(1000);
        assertEquals("e003", engine.nextReady().instanceId());
        engine.useSkill("e003", "e003_explode");
        assertTrue(state.combatant("e003").downed());
        assertTrue(state.combatant("ally").hp() < state.combatant("ally").maxHp());
    }
}
