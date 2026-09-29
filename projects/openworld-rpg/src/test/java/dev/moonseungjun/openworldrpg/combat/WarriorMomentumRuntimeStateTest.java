package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.runtime.WarriorMomentumRuntimeState;
import org.junit.jupiter.api.Test;

class WarriorMomentumRuntimeStateTest {
    @Test
    void basicFinisherUsesPersonalIcdAndOneCyclePublication() {
        var state = new WarriorMomentumRuntimeState();

        assertTrue(state.recordMeleeBasicHit(true, 100L));
        assertEquals(1, state.pips(100L));
        assertTrue(state.claimBasicCyclePublication(100L));

        assertFalse(state.recordMeleeBasicHit(true, 100L));
        assertFalse(state.claimBasicCyclePublication(100L));
        assertEquals(1, state.pips(100L));

        assertFalse(state.recordMeleeBasicHit(true, 115L));
        assertEquals(1, state.pips(115L));

        assertTrue(state.recordMeleeBasicHit(true, 116L));
        assertEquals(2, state.pips(116L));
        assertTrue(state.claimBasicCyclePublication(116L));
    }

    @Test
    void momentumScalesPoiseAndExpiresAfterSevenSeconds() {
        var state = new WarriorMomentumRuntimeState();

        state.recordSuccessfulCounter(20L);
        assertEquals(1.05, state.poiseOutputMultiplier(20L), 0.0001);
        assertEquals(
                0.90,
                state.ordinaryHitStaggerTakenMultiplier(20L),
                0.0001
        );

        state.recordEliteBossPoiseBreak(30L);
        assertEquals(3, state.pips(30L));
        assertEquals(1.15, state.poiseOutputMultiplier(30L), 0.0001);

        assertEquals(3, state.pips(169L));
        assertEquals(0, state.pips(170L));
        assertEquals(1.0, state.poiseOutputMultiplier(170L), 0.0001);
        assertEquals(
                1.0,
                state.ordinaryHitStaggerTakenMultiplier(170L),
                0.0001
        );
    }

    @Test
    void fullMomentumSpenderConsumesAllPips() {
        var state = new WarriorMomentumRuntimeState();
        state.recordSuccessfulCounter(0L);
        state.recordEliteBossPoiseBreak(1L);

        assertEquals(3, state.pips(1L));
        assertTrue(state.consumeSpenderIfFull(1L));
        assertEquals(0, state.pips(1L));
        assertFalse(state.consumeSpenderIfFull(1L));
    }

    @Test
    void counterAndHyperarmorWindowsAreOneShotAndFinite() {
        var state = new WarriorMomentumRuntimeState();
        state.beginCounter(50L, 10L);
        assertTrue(state.counterActive(59L));
        assertTrue(state.consumeCounterWindow(59L));
        assertFalse(state.consumeCounterWindow(59L));

        state.beginHyperarmor(1.60, 100L, 18L);
        assertEquals(1.60, state.hyperarmorMultiplier(117L), 0.0001);
        assertEquals(1.0, state.hyperarmorMultiplier(118L), 0.0001);
    }
}
