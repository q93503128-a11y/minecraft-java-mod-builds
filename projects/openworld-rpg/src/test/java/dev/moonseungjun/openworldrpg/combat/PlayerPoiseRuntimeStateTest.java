package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.PlayerPoiseRuntimeState;
import org.junit.jupiter.api.Test;

class PlayerPoiseRuntimeStateTest {
    @Test
    void playerPoiseRecoversAfterOneSecondAtFortyFivePerSecond() {
        var state = new PlayerPoiseRuntimeState(80.0, 0);
        state.apply(40.0, 0);
        assertEquals(40.0, state.snapshot(20).currentPoise(), 0.0001);
        assertEquals(62.5, state.snapshot(30).currentPoise(), 0.0001);
        assertEquals(80.0, state.snapshot(40).currentPoise(), 0.0001);
    }

    @Test
    void breakRefillsAndGrantsSevenTickPressureImmunity() {
        var state = new PlayerPoiseRuntimeState(60.0, 0);
        var broken = state.apply(75.0, 0);
        assertTrue(broken.breakTriggered());
        assertEquals(60.0, broken.remainingPoise(), 0.0001);

        var ignored = state.apply(20.0, 6);
        assertEquals(0.0, ignored.effectivePressure(), 0.0001);
        assertTrue(ignored.postBreakImmune());

        var accepted = state.apply(20.0, 7);
        assertEquals(20.0, accepted.effectivePressure(), 0.0001);
        assertFalse(accepted.postBreakImmune());
    }
}
