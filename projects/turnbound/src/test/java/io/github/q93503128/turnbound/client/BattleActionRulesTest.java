package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BattleActionRulesTest {
    private static final ClientBattleState.Unit ALLY =
            new ClientBattleState.Unit("ally_1", "P01", "ALLY", "Kairen", 900, 900, 0, 0, false);
    private static final ClientBattleState.Unit ENEMY =
            new ClientBattleState.Unit("enemy_1", "E01", "ENEMY", "Enemy", 600, 600, 0, 0, false);

    @Test
    void selfAndAllActionsStillRequireTheSeparateConfirmStep() {
        List<ClientBattleState.Unit> units = List.of(ALLY, ENEMY);
        assertEquals(0, BattleActionRules.defaultTarget(units, "SELF", "ally_1"));
        assertEquals("", BattleActionRules.confirmedTarget(units, "SELF", "ally_1", 0));
        assertEquals("", BattleActionRules.confirmedTarget(units, "ALLY_ALL", "ally_1", -1));
        assertEquals("", BattleActionRules.confirmedTarget(units, "ENEMY_ALL", "ally_1", -1));
    }

    @Test
    void singleTargetCannotConfirmWithoutAValidSelection() {
        List<ClientBattleState.Unit> units = List.of(ALLY, ENEMY);
        assertNull(BattleActionRules.confirmedTarget(units, "ENEMY_SINGLE", "ally_1", -1));
        assertNull(BattleActionRules.confirmedTarget(units, "ENEMY_SINGLE", "ally_1", 0));
        assertEquals("enemy_1", BattleActionRules.confirmedTarget(units, "ENEMY_SINGLE", "ally_1", 1));
    }
}