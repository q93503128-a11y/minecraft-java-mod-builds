package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.CanonicalData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CavehornRavagerAiTest {
    private static CombatantState ally(String id, int formation) {
        return new CombatantState(id, PrototypeRoster.kyren(), CombatantSide.ALLY, formation);
    }

    private static CombatantState cavehorn(int formation) {
        return new CombatantState("cavehorn", CanonicalData.definition("EL_CV01", 3, 0, false),
                CombatantSide.ENEMY, formation);
    }

    @Test
    void chargeIsVisiblyWarnedOneActionBeforeImpact() {
        CombatantState target = ally("target", 0);
        CombatantState elite = cavehorn(1);
        BattleState state = new BattleState(List.of(target, elite));
        BattleEngine engine = new BattleEngine(state);

        elite.setGauge(1000);
        assertEquals(elite, engine.nextReady());
        int hpBefore = target.hp();
        BattleAutoController.chooseAutoAction(engine, state, elite);
        assertTrue(elite.flag("el_cv01_charge_ready"));
        assertEquals(hpBefore, target.hp(), "telegraph action must not deal hidden damage");

        target.setGauge(500);
        elite.setGauge(1000);
        assertEquals(elite, engine.nextReady());
        BattleAutoController.chooseAutoAction(engine, state, elite);
        assertTrue(target.hp() < hpBefore);
        assertEquals(380, target.gauge());
        assertEquals(2, elite.cooldown("el_cv01_charge"));
        assertTrue(!elite.flag("el_cv01_charge_ready"));
    }

    @Test
    void repeatedTargetPressureIncreasesGoreDamage() {
        CombatantState target = ally("target", 0);
        CombatantState elite = cavehorn(1);
        BattleState state = new BattleState(List.of(target, elite));
        BattleEngine engine = new BattleEngine(state);

        elite.setGauge(1000);
        assertEquals(elite, engine.nextReady());
        int beforeFirst = target.hp();
        engine.useSkill(elite.instanceId(), "el_cv01_gore", target.instanceId());
        int first = beforeFirst - target.hp();

        elite.setGauge(1000);
        assertEquals(elite, engine.nextReady());
        int beforeSecond = target.hp();
        engine.useSkill(elite.instanceId(), "el_cv01_gore", target.instanceId());
        int second = beforeSecond - target.hp();

        assertTrue(second > first, "same-target follow-up must communicate rising pressure");
    }

    @Test
    void secondPatternActivatesBelowHalfHp() {
        CombatantState a = ally("a", 0);
        CombatantState b = ally("b", 1);
        CombatantState elite = cavehorn(2);
        elite.takeDamage(elite.maxHp() / 2 + 1);
        elite.setCooldown("el_cv01_charge", 2);
        a.setGauge(300);
        b.setGauge(450);
        elite.setGauge(1000);
        BattleState state = new BattleState(List.of(a, b, elite));
        BattleEngine engine = new BattleEngine(state);

        assertEquals(elite, engine.nextReady());
        int ahp = a.hp();
        int bhp = b.hp();
        BattleAutoController.chooseAutoAction(engine, state, elite);

        assertTrue(a.hp() < ahp);
        assertTrue(b.hp() < bhp);
        assertEquals(230, a.gauge());
        assertEquals(380, b.gauge());
        assertEquals(2, elite.cooldown("el_cv01_stomp"));
    }
}
