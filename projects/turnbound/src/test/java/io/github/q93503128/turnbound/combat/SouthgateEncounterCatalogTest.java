package io.github.q93503128.turnbound.combat;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class SouthgateEncounterCatalogTest {
    @Test
    void chapterOneCatalogBuildsFivePatrolsAndOneBossWithinPartyLimits() {
        assertEquals(5, SouthgateEncounterCatalog.normalEncounters().size());
        for (var spec : SouthgateEncounterCatalog.normalEncounters()) {
            assertFalse(spec.boss());
            assertTrue(spec.enemyDefinitionIds().size() >= 2 && spec.enemyDefinitionIds().size() <= 5);
            BattleState state = SouthgateEncounterCatalog.createBattle(spec.id());
            assertEquals(4, state.living(CombatantSide.ALLY).size());
            assertEquals(spec.enemyDefinitionIds().size(), state.living(CombatantSide.ENEMY).size());
            assertEquals(state.combatants().size(), new HashSet<>(state.combatants().stream().map(CombatantState::instanceId).toList()).size());
        }
        var boss = SouthgateEncounterCatalog.boss();
        assertTrue(boss.boss());
        assertEquals(1, SouthgateEncounterCatalog.createBattle(boss.id()).living(CombatantSide.ENEMY).size());
    }

    @Test
    void e003E004AndGraulHaveDistinctCombatRoles() {
        CombatantDefinition e003 = SouthgateEncounterCatalog.enemyDefinition("E003");
        CombatantDefinition e004 = SouthgateEncounterCatalog.enemyDefinition("E004");
        CombatantDefinition graul = SouthgateEncounterCatalog.enemyDefinition("B01");
        assertTrue(e003.stats().speed() > e004.stats().speed());
        assertTrue(e004.stats().defense() > e003.stats().defense());
        assertTrue(graul.stats().maxHp() > e004.stats().maxHp() * 3);
        assertEquals(TargetRule.ENEMY_ALL, graul.skill("b01_roar").targetRule());
    }
}
