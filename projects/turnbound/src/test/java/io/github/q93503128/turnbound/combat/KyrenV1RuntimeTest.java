package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.CanonicalData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class KyrenV1RuntimeTest {
    private static CombatantState kyren() {
        return new CombatantState("kyren", CanonicalData.definition("P01", 1, 0, false), CombatantSide.ALLY, 0);
    }

    private static CombatantState enemy(String instanceId, int seed) {
        return new CombatantState(instanceId,
                PrototypeRoster.trainingEnemy(instanceId.toUpperCase(), instanceId, 99_999, 1, 80, 90),
                CombatantSide.ENEMY, seed);
    }

    @Test
    void insightSlashIsARealAttackAndStartsTheDuelAtFocusOne() {
        CombatantState kyren = kyren();
        CombatantState target = enemy("target", 1);
        BattleEngine engine = new BattleEngine(new BattleState(List.of(kyren, target)));
        kyren.setGauge(1000);

        int before = target.hp();
        assertEquals("kyren", engine.nextReady().instanceId());
        engine.useSkill("kyren", "p01_duel_lock", "target");

        assertTrue(target.hp() < before);
        assertEquals("target", kyren.ref("focusTarget"));
        assertEquals(1, kyren.counter("focus"));
        assertEquals(100L, kyren.gauge());
    }

    @Test
    void directTargetSwitchStartsAtFocusOneInsteadOfDroppingToZero() {
        CombatantState kyren = kyren();
        CombatantState first = enemy("first", 1);
        CombatantState second = enemy("second", 2);
        BattleEngine engine = new BattleEngine(new BattleState(List.of(kyren, first, second)));

        kyren.setGauge(1000);
        engine.nextReady();
        engine.useSkill("kyren", "p01_chase_slash", "first");
        assertEquals(1, kyren.counter("focus"));

        kyren.setGauge(1000);
        engine.nextReady();
        engine.useSkill("kyren", "p01_chase_slash", "second");
        assertEquals("second", kyren.ref("focusTarget"));
        assertEquals(1, kyren.counter("focus"));
    }

    @Test
    void maxFocusFollowupKillClearsDeadTargetAndAwakeningCarriesMomentum() {
        CombatantState kyren = new CombatantState("kyren",
                CanonicalData.definition("P01", 1, 4, true), CombatantSide.ALLY, 0);
        CombatantState target = new CombatantState("target",
                PrototypeRoster.trainingEnemy("TARGET", "target", 170, 1, 0, 90),
                CombatantSide.ENEMY, 1);
        BattleState state = new BattleState(List.of(kyren, target));
        BattleEngine engine = new BattleEngine(state);
        kyren.setRef("focusTarget", "target");
        kyren.setCounter("focus", 3);
        kyren.setGauge(1000);

        engine.nextReady();
        engine.useSkill("kyren", "p01_chase_slash", "target");

        assertTrue(target.downed());
        assertEquals(null, kyren.ref("focusTarget"));
        assertEquals(0, kyren.counter("focus"));
        assertTrue(kyren.flag("p01_carry_focus"));
    }

    @Test
    void maxFocusChangesBreakerBehaviorWithAVisibleFollowupHit() {
        CombatantState kyren = kyren();
        CombatantState target = enemy("target", 1);
        BattleState state = new BattleState(List.of(kyren, target));
        BattleEngine engine = new BattleEngine(state);
        kyren.setRef("focusTarget", "target");
        kyren.setCounter("focus", 3);
        kyren.setGauge(1000);

        engine.nextReady();
        engine.useSkill("kyren", "p01_breaker_strike", "target");

        assertTrue(state.events().stream().anyMatch(event ->
                "REACTION_DAMAGE".equals(event.type()) && "P01_BREAKER_FOLLOWUP".equals(event.detail())));
        assertEquals(3, kyren.counter("focus"));
    }
}
