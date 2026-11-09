package io.github.q93503128.turnbound.combat;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CombatFoundationTest {
    private static CombatantState fighter(String id, CombatantSide side, int speed, int seed) {
        var basic = new SkillDefinition("basic", "Basic", TargetRule.ENEMY_SINGLE, 0,
                List.of(SkillEffect.damage(1.0)));
        var def = new CombatantDefinition(id, id, new BattleStats(100, 20, 10, speed),
                "basic", List.of(basic));
        return new CombatantState(id, def, side, seed);
    }

    @Test void gaugeSchedulerPreservesForecastAndUsesSpeed() {
        var fast = fighter("ally", CombatantSide.ALLY, 200, 1);
        var slow = fighter("enemy", CombatantSide.ENEMY, 100, 2);
        var state = new BattleState(List.of(fast, slow));
        assertEquals("ally", TurnScheduler.preview(state, 1).get(0).instanceId());
        assertNull(state.currentActorId());
        assertEquals(0L, fast.gauge());
        assertEquals("ally", TurnScheduler.nextReady(state).instanceId());
        assertEquals("ally", state.currentActorId());
    }

    @Test void zeroLivingAlliesCannotWinSimultaneousKo() {
        var ally = fighter("ally", CombatantSide.ALLY, 100, 1);
        var enemy = fighter("enemy", CombatantSide.ENEMY, 100, 2);
        var state = new BattleState(List.of(ally, enemy));
        ally.forceDown();
        enemy.forceDown();
        assertEquals(BattleOutcome.ENEMY_VICTORY, state.outcome());
    }

    @Test void mathRemainsIndependentOfWorldAndLoader() {
        assertTrue(CombatMath.calculateDamage(30, 10, 1.0) > 0);
    }
}
