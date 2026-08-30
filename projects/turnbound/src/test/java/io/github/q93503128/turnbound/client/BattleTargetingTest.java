package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleTargetingTest {
    private static final ClientBattleState.Unit ALLY_ALIVE =
            new ClientBattleState.Unit("ally_1", "p01", "ALLY", "Kyren", 900, 900, 0, 0, false);
    private static final ClientBattleState.Unit ALLY_OTHER =
            new ClientBattleState.Unit("ally_3", "p03", "ALLY", "Bram", 1000, 1000, 0, 0, false);
    private static final ClientBattleState.Unit ALLY_DOWN =
            new ClientBattleState.Unit("ally_2", "p04", "ALLY", "Elysia", 0, 800, 0, 0, true);
    private static final ClientBattleState.Unit ENEMY_ALIVE =
            new ClientBattleState.Unit("enemy_1", "e001", "ENEMY", "Enemy", 500, 500, 0, 0, false);
    private static final ClientBattleState.Unit ENEMY_DOWN =
            new ClientBattleState.Unit("enemy_2", "e002", "ENEMY", "Down", 0, 500, 0, 0, true);

    @Test
    void validatesSingleTargetRulesWithoutAllowingDownedUnitsByAccident() {
        assertTrue(BattleTargeting.validTarget("ALLY_SINGLE", ALLY_ALIVE, "ally_1"));
        assertFalse(BattleTargeting.validTarget("ALLY_SINGLE", ALLY_DOWN, "ally_1"));
        assertTrue(BattleTargeting.validTarget("DEAD_ALLY_SINGLE", ALLY_DOWN, "ally_1"));
        assertTrue(BattleTargeting.validTarget("ENEMY_SINGLE", ENEMY_ALIVE, "ally_1"));
        assertFalse(BattleTargeting.validTarget("ENEMY_SINGLE", ENEMY_DOWN, "ally_1"));
        assertFalse(BattleTargeting.validTarget("ENEMY_SINGLE", ENEMY_ALIVE, ""));
    }

    @Test
    void selfForbiddenAllyRuleSkipsTheCurrentActor() {
        List<ClientBattleState.Unit> units = List.of(ALLY_ALIVE, ALLY_OTHER, ALLY_DOWN, ENEMY_ALIVE);
        assertFalse(BattleTargeting.validTarget("ALLY_SINGLE_EXCEPT_SELF", ALLY_ALIVE, "ally_1"));
        assertTrue(BattleTargeting.validTarget("ALLY_SINGLE_EXCEPT_SELF", ALLY_OTHER, "ally_1"));
        assertEquals(1, BattleTargeting.firstValid(units, "ALLY_SINGLE_EXCEPT_SELF", "ally_1"));
    }

    @Test
    void tabCycleSkipsInvalidTargetsAndWrapsDeterministically() {
        List<ClientBattleState.Unit> units = List.of(ALLY_ALIVE, ALLY_DOWN, ENEMY_DOWN, ENEMY_ALIVE);

        assertEquals(3, BattleTargeting.firstValid(units, "ENEMY_SINGLE", "ally_1"));
        assertEquals(3, BattleTargeting.cycle(units, "ENEMY_SINGLE", "ally_1", -1, 1));
        assertEquals(3, BattleTargeting.cycle(units, "ENEMY_SINGLE", "ally_1", 3, 1));
        assertEquals(3, BattleTargeting.cycle(units, "ENEMY_SINGLE", "ally_1", -1, -1));
    }

    @Test
    void allyAndReviveCyclesUseDifferentTargetSets() {
        List<ClientBattleState.Unit> units = List.of(ALLY_ALIVE, ALLY_DOWN, ENEMY_ALIVE);

        assertEquals(0, BattleTargeting.firstValid(units, "ALLY_SINGLE", "ally_1"));
        assertEquals(1, BattleTargeting.firstValid(units, "DEAD_ALLY_SINGLE", "ally_1"));
        assertEquals(-1, BattleTargeting.firstValid(units, "SELF", "ally_1"));
    }
}
