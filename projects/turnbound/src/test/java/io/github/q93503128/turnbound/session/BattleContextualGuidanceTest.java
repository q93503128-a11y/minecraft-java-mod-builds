package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.BattleEngine;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.content.CanonicalData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleContextualGuidanceTest {
    @Test
    void firstCapitalValleyBattleTeachesOnlyThreeContextualBeats() {
        CombatantState ally = new CombatantState(
                "ally_p01", CanonicalData.definition("P01"), CombatantSide.ALLY, 0);
        CombatantState enemy = new CombatantState(
                "enemy_b05", CanonicalData.definition("B05"), CombatantSide.ENEMY, 4);
        BattleState state = new BattleState(List.of(ally, enemy));
        BattleEngine engine = new BattleEngine(state);

        var first = BattleContextualGuidance.resolve("CV_FIRST_COMMON", state);
        assertTrue(first.text().contains("기본 공격"));

        act(engine, ally, enemy, "p01_chase_slash");
        var second = BattleContextualGuidance.resolve("CV_FIRST_COMMON", state);
        assertTrue(second.text().contains("행동 순서"));

        act(engine, ally, enemy, "p01_breaker_strike");
        var third = BattleContextualGuidance.resolve("CV_FIRST_COMMON", state);
        assertTrue(third.text().contains("액티브"));
        assertTrue(third.text().contains("CD"));

        act(engine, ally, enemy, "p01_duel_lock");
        assertFalse(BattleContextualGuidance.resolve("CV_FIRST_COMMON", state).visible(),
                "contextual teaching must disappear instead of becoming permanent HUD noise");
    }

    @Test
    void guidanceNeverLeaksIntoOtherEncounters() {
        CombatantState ally = new CombatantState(
                "ally_p01", CanonicalData.definition("P01"), CombatantSide.ALLY, 0);
        CombatantState enemy = new CombatantState(
                "enemy_b05", CanonicalData.definition("B05"), CombatantSide.ENEMY, 4);
        BattleState state = new BattleState(List.of(ally, enemy));

        assertFalse(BattleContextualGuidance.resolve("CV_DRABYEL_ROAD", state).visible());
        assertFalse(BattleContextualGuidance.resolve("", state).visible());
    }

    private static void act(BattleEngine engine, CombatantState ally, CombatantState enemy, String skillId) {
        ally.setGauge(BattleEngine.TURN_THRESHOLD);
        enemy.setGauge(0);
        engine.nextReady();
        engine.useSkill(ally.instanceId(), skillId, enemy.instanceId());
    }
}
