package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.PlayerShockRuntimeState;
import org.junit.jupiter.api.Test;

class PlayerShockRuntimeStateTest {
    @Test
    void shockBuildupWaitsThreeSecondsThenDecaysTwentyPerSecond() {
        var state = new PlayerShockRuntimeState(100.0, 0);
        state.apply(35.0, 0);
        assertEquals(35.0, state.snapshot(60).buildup(), 0.0001);
        assertEquals(25.0, state.snapshot(70).buildup(), 0.0001);
        assertEquals(0.0, state.snapshot(100).buildup(), 0.0001);
    }

    @Test
    void thresholdProcResetsMeterAndStartsFourSecondConductive() {
        var state = new PlayerShockRuntimeState(100.0, 0);
        state.apply(70.0, 0);
        var proc = state.apply(35.0, 1);
        assertTrue(proc.procced());
        assertEquals(0.0, proc.remainingBuildup(), 0.0001);
        assertTrue(state.snapshot(80).conductive());
        assertFalse(state.snapshot(81).conductive());
    }
}
