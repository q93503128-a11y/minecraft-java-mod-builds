package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.CombatantSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BattleDownPresentationRulesTest {
    @Test
    void defeatedEnemiesRetireInsteadOfOccupyingArenaForever() {
        assertTrue(BattleDownPresentationRules.retiresVisual(CombatantSide.ENEMY, false));
        assertFalse(BattleDownPresentationRules.usesRecoveryMarker(CombatantSide.ENEMY, false));
        assertEquals(20, BattleDownPresentationRules.removalTicks(false, false));
        assertEquals(28, BattleDownPresentationRules.removalTicks(false, true));
    }

    @Test
    void downedAlliesKeepARecoveryTargetInsteadOfABrokenBody() {
        assertFalse(BattleDownPresentationRules.retiresVisual(CombatantSide.ALLY, false));
        assertTrue(BattleDownPresentationRules.usesRecoveryMarker(CombatantSide.ALLY, false));
        assertTrue(BattleDownPresentationRules.recoveryMarkerDelayTicks("P04") >= 20);
    }

    @Test
    void summonsMayDisappearWhileDownAndReturnOnRecovery() {
        assertTrue(BattleDownPresentationRules.retiresVisual(CombatantSide.ALLY, true));
        assertFalse(BattleDownPresentationRules.usesRecoveryMarker(CombatantSide.ALLY, true));
        assertEquals(12, BattleDownPresentationRules.removalTicks(true, false));
    }
}
