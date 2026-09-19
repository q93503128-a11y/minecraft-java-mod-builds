package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.CanonicalData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapitalValleyEnemyAiTest {
    private static CombatantState ally(String id, int formation) {
        return new CombatantState(id, PrototypeRoster.kyren(), CombatantSide.ALLY, formation);
    }

    private static CombatantState enemy(String instanceId, String id, int formation) {
        return new CombatantState(instanceId, CanonicalData.definition(id, 2, 0, false), CombatantSide.ENEMY, formation);
    }

    @Test
    void mossbackBoarActuallyUsesItsAuthoredChargeAndGaugeDelay() {
        CombatantState target = ally("target", 0);
        CombatantState boar = enemy("boar", "CV_A", 1);
        target.setGauge(500);
        boar.setGauge(1000);
        BattleState state = new BattleState(List.of(target, boar));
        BattleEngine engine = new BattleEngine(state);
        assertEquals(boar, engine.nextReady());
        BattleAutoController.chooseAutoAction(engine, state, boar);
        assertEquals(440, target.gauge());
        assertEquals(2, boar.cooldown("cv_a_charge"));
    }

    @Test
    void cutthroatUsesOpportunistAgainstLowHpTarget() {
        CombatantState low = ally("low", 0);
        CombatantState healthy = ally("healthy", 1);
        CombatantState cutthroat = enemy("cutthroat", "CV_B", 2);
        low.takeDamage(low.maxHp() / 2);
        cutthroat.setGauge(1000);
        int lowBefore = low.hp();
        int healthyBefore = healthy.hp();
        BattleState state = new BattleState(List.of(low, healthy, cutthroat));
        BattleEngine engine = new BattleEngine(state);
        assertEquals(cutthroat, engine.nextReady());
        BattleAutoController.chooseAutoAction(engine, state, cutthroat);
        assertTrue(low.hp() < lowBefore);
        assertEquals(healthyBefore, healthy.hp());
        assertEquals(2, cutthroat.cooldown("cv_b_opportunist"));
    }

    @Test
    void marksmanAimedShotTargetsTheWeakestAlly() {
        CombatantState weak = ally("weak", 0);
        CombatantState healthy = ally("healthy", 1);
        CombatantState marksman = enemy("marksman", "CV_C", 2);
        weak.takeDamage(100);
        marksman.setGauge(1000);
        int weakBefore = weak.hp();
        int healthyBefore = healthy.hp();
        BattleState state = new BattleState(List.of(weak, healthy, marksman));
        BattleEngine engine = new BattleEngine(state);
        assertEquals(marksman, engine.nextReady());
        BattleAutoController.chooseAutoAction(engine, state, marksman);
        assertTrue(weak.hp() < weakBefore);
        assertEquals(healthyBefore, healthy.hp());
        assertEquals(2, marksman.cooldown("cv_c_aimed"));
    }
}
