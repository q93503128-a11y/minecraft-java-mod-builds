package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.BattleEvent;
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
                "enemy_cv_a", CanonicalData.definition("CV_A"), CombatantSide.ENEMY, 4);
        BattleState state = new BattleState(List.of(ally, enemy));

        var first = BattleContextualGuidance.resolve("CV_FIRST_COMMON", state);
        assertTrue(first.text().contains("기본 공격"));

        state.addEvent(new BattleEvent("ACTION", enemy.instanceId(), ally.instanceId(), 0, "cv_a_basic"));
        assertTrue(BattleContextualGuidance.resolve("CV_FIRST_COMMON", state).text().contains("기본 공격"),
                "enemy actions must not advance player teaching");

        state.addEvent(new BattleEvent("ACTION", ally.instanceId(), enemy.instanceId(), 0, "p01_chase_slash"));
        var second = BattleContextualGuidance.resolve("CV_FIRST_COMMON", state);
        assertTrue(second.text().contains("행동 순서"));

        state.addEvent(new BattleEvent("ACTION", ally.instanceId(), enemy.instanceId(), 0, "p01_breaker_strike"));
        var third = BattleContextualGuidance.resolve("CV_FIRST_COMMON", state);
        assertTrue(third.text().contains("액티브"));
        assertTrue(third.text().contains("CD"));

        state.addEvent(new BattleEvent("ACTION", ally.instanceId(), enemy.instanceId(), 0, "p01_duel_lock"));
        assertFalse(BattleContextualGuidance.resolve("CV_FIRST_COMMON", state).visible(),
                "contextual teaching must disappear instead of becoming permanent HUD noise");
    }

    @Test
    void guidanceNeverLeaksIntoOtherEncounters() {
        CombatantState ally = new CombatantState(
                "ally_p01", CanonicalData.definition("P01"), CombatantSide.ALLY, 0);
        CombatantState enemy = new CombatantState(
                "enemy_cv_a", CanonicalData.definition("CV_A"), CombatantSide.ENEMY, 4);
        BattleState state = new BattleState(List.of(ally, enemy));

        assertFalse(BattleContextualGuidance.resolve("CV_DRABYEL_ROAD", state).visible());
        assertFalse(BattleContextualGuidance.resolve("", state).visible());
    }
}
