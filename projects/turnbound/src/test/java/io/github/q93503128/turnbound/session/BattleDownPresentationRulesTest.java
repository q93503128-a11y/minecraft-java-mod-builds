package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.CombatantSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BattleDownPresentationRulesTest {
    @Test
    void defeatedEnemiesKeepRecoveryTargetsForFutureReviveSkills() {
        assertFalse(BattleDownPresentationRules.retiresVisual(CombatantSide.ENEMY, false));
        assertTrue(BattleDownPresentationRules.usesRecoveryMarker(CombatantSide.ENEMY, false));
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
