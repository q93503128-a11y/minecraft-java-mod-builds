package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.R01RegalhartMythicEffectState;
import org.junit.jupiter.api.Test;

class R01RegalhartMythicEffectStateTest {
    private static final double EPSILON = 0.0001;

    @Test
    void hartsMomentumPrimesAfterTwentyFiveSprintTicksForExactTwoSecondWindow() {
        var state = new R01RegalhartMythicEffectState();

        for (long tick = 0L; tick < 24L; tick++) {
            assertFalse(state.recordMovementTick(true, tick).primed(tick));
        }

        var primed = state.recordMovementTick(true, 24L);
        assertTrue(primed.primed(24L));
        assertEquals(64L, primed.primedUntilTick());

        var trigger = state.tryConsume(64L);
        assertTrue(trigger.triggered());
        assertEquals(1.35, trigger.poiseMultiplier(), EPSILON);
        assertEquals(164L, trigger.nextReadyTick());
    }

    @Test
    void expiredWindowDoesNotRearmInsideSameUninterruptedSprintSegment() {
        var state = new R01RegalhartMythicEffectState();
        for (long tick = 0L; tick <= 24L; tick++) {
            state.recordMovementTick(true, tick);
        }

        assertFalse(state.tryConsume(65L).triggered());
        for (long tick = 65L; tick < 100L; tick++) {
            assertFalse(state.recordMovementTick(true, tick).primed(tick));
        }

        state.recordMovementTick(false, 100L);
        for (long tick = 101L; tick < 125L; tick++) {
            assertFalse(state.recordMovementTick(true, tick).primed(tick));
        }
        assertTrue(state.recordMovementTick(true, 125L).primed(125L));
    }

    @Test
    void fiveSecondIcdBlocksNewChargeUntilReadyTick() {
        var state = new R01RegalhartMythicEffectState();
        for (long tick = 0L; tick <= 24L; tick++) {
            state.recordMovementTick(true, tick);
        }
        assertTrue(state.tryConsume(24L).triggered());

        state.recordMovementTick(false, 25L);
        for (long tick = 26L; tick < 124L; tick++) {
            assertFalse(state.recordMovementTick(true, tick).primed(tick));
        }
        assertTrue(state.recordMovementTick(true, 124L).primed(124L));
    }

    @Test
    void stoppingMovementResetsTheOnePointTwoFiveSecondQualification() {
        var state = new R01RegalhartMythicEffectState();
        for (long tick = 0L; tick < 20L; tick++) {
            state.recordMovementTick(true, tick);
        }

        state.recordMovementTick(false, 20L);
        for (long tick = 21L; tick < 45L; tick++) {
            assertFalse(state.recordMovementTick(true, tick).primed(tick));
        }
        assertTrue(state.recordMovementTick(true, 45L).primed(45L));
    }
}
