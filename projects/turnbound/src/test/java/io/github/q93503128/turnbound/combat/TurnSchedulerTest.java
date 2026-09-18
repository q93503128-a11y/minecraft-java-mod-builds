package io.github.q93503128.turnbound.combat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TurnSchedulerTest {
    private static CombatantDefinition unit(String id, int speed) {
        return PrototypeRoster.trainingEnemy(id, id, 999_999, 1, 999, speed);
    }

    @Test
    void fixedPointClockDoesNotPromoteNearSpeedOpponentToReadyEarly() {
        var fast = new CombatantState("fast", unit("FAST_101", 101), CombatantSide.ALLY, 0);
        var slow = new CombatantState("slow", unit("SLOW_100", 100), CombatantSide.ENEMY, 1);
        var state = new BattleState(List.of(fast, slow));

        CombatantState selected = TurnScheduler.nextReady(state);

        assertEquals("fast", selected.instanceId());
        assertEquals(9_900_991L, state.logicalTimeMicro());
        assertEquals(990_099_100L, slow.gaugeMicro());
        assertEquals(990L, slow.gauge());
        assertTrue(fast.gaugeMicro() >= TurnScheduler.TURN_THRESHOLD_MICRO);
    }

    @Test
    void previewAndRuntimeUseTheSameSchedulerOrder() {
        var a = new CombatantState("a", unit("A", 83), CombatantSide.ALLY, 0);
        var b = new CombatantState("b", unit("B", 101), CombatantSide.ALLY, 1);
        var c = new CombatantState("c", unit("C", 117), CombatantSide.ENEMY, 2);
        a.setGauge(100);
        b.setGauge(350);
        c.setGauge(20);
        var state = new BattleState(List.of(a, b, c));

        List<String> projected = state.timelinePreview(8).stream().map(CombatantState::instanceId).toList();
        List<String> actual = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            CombatantState actor = TurnScheduler.nextReady(state);
            actual.add(actor.instanceId());
            actor.spendTurnGauge();
            state.setCurrentActorId(null);
        }

        assertEquals(projected, actual);
    }

    @Test
    void finalSpeedModifiersFeedTheSharedScheduler() {
        var first = new CombatantState("first", unit("FIRST", 100), CombatantSide.ALLY, 0);
        var boosted = new CombatantState("boosted", unit("BOOSTED", 100), CombatantSide.ENEMY, 1);
        boosted.putStatus(new StatusInstance("speed_multiplier", "source", 2, 0.10));
        var state = new BattleState(List.of(first, boosted));

        assertEquals("boosted", state.timelinePreview(1).getFirst().instanceId());
        assertEquals("boosted", TurnScheduler.nextReady(state).instanceId());
    }
}
