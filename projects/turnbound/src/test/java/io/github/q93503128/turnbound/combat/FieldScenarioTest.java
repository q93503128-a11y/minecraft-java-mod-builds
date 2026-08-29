package io.github.q93503128.turnbound.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FieldScenarioTest {
    @Test
    void firstVisiblePatrolMatchesTheCanonicalSouthgateEnemyParty() {
        BattleState state = P0Scenario.createFieldPatrol();
        assertEquals(4, state.living(CombatantSide.ALLY).size());
        assertEquals(3, state.living(CombatantSide.ENEMY).size());
        assertEquals("E001", state.combatant("enemy_e001").definition().id());
        assertEquals("부패 보행자", state.combatant("enemy_e001").definition().name());
        assertEquals(720, state.combatant("enemy_e001").maxHp());
        assertEquals(82, state.combatant("enemy_e001").speed());
        assertEquals("E002", state.combatant("enemy_e002").definition().id());
        assertEquals(560, state.combatant("enemy_e002").maxHp());
        assertEquals(105, state.combatant("enemy_e002").speed());
        assertEquals("E005", state.combatant("enemy_e005").definition().id());
        assertEquals(590, state.combatant("enemy_e005").maxHp());
        assertEquals(94, state.combatant("enemy_e005").speed());
    }

    @Test
    void firstFieldEncounterResolvesUnderServerAuthoritativeAutoRules() {
        BattleState state = P0Scenario.createFieldPatrol();
        BattleEngine engine = new BattleEngine(state);
        int actions = 0;
        while (state.outcome() == BattleOutcome.RUNNING && actions++ < 240) P0Scenario.chooseAutoAction(engine, state, engine.nextReady());
        assertNotEquals(BattleOutcome.RUNNING, state.outcome());
    }
}
