package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.R01EarthloongMythicEffectState;
import org.junit.jupiter.api.Test;

class R01EarthloongMythicEffectStateTest {
    private static final double EPSILON = 0.0001;

    @Test
    void rootquakeUsesFortyPercentWeaponPowerAndExactEightSecondIcd() {
        var state = new R01EarthloongMythicEffectState();

        var first = state.tryRootquake(22.0, 100L);
        assertTrue(first.triggered());
        assertEquals(9.0, first.damage(), EPSILON);
        assertEquals(260L, first.nextReadyTick());

        var blocked = state.tryRootquake(100.0, 259L);
        assertFalse(blocked.triggered());
        assertEquals(0.0, blocked.damage(), EPSILON);

        var ready = state.tryRootquake(100.0, 260L);
        assertTrue(ready.triggered());
        assertEquals(40.0, ready.damage(), EPSILON);
    }

    @Test
    void earthenReprieveCreatesEightPercentBarrierForFourSecondsWithTenSecondIcd() {
        var state = new R01EarthloongMythicEffectState();

        var first = state.tryEarthenReprieve(250.0, 20L);
        assertTrue(first.triggered());
        assertEquals(20.0, first.barrierAmount(), EPSILON);
        assertEquals(100L, first.barrierUntilTick());
        assertEquals(220L, first.nextReadyTick());

        var absorbed = state.absorb(7.0, 21L);
        assertEquals(0.0, absorbed.remainingDamage(), EPSILON);
        assertEquals(7.0, absorbed.absorbedDamage(), EPSILON);
        assertEquals(13.0, absorbed.remainingBarrier(), EPSILON);

        var partial = state.absorb(20.0, 22L);
        assertEquals(7.0, partial.remainingDamage(), EPSILON);
        assertEquals(13.0, partial.absorbedDamage(), EPSILON);
        assertEquals(0.0, partial.remainingBarrier(), EPSILON);

        var blocked = state.tryEarthenReprieve(500.0, 219L);
        assertFalse(blocked.triggered());

        var ready = state.tryEarthenReprieve(500.0, 220L);
        assertTrue(ready.triggered());
        assertEquals(40.0, ready.barrierAmount(), EPSILON);
    }

    @Test
    void reprieveBarrierExpiresAtExactFourSecondBoundary() {
        var state = new R01EarthloongMythicEffectState();
        state.tryEarthenReprieve(200.0, 0L);

        assertEquals(16.0, state.barrierAmount(79L), EPSILON);
        assertEquals(0.0, state.barrierAmount(80L), EPSILON);

        var afterExpiry = state.absorb(10.0, 80L);
        assertEquals(10.0, afterExpiry.remainingDamage(), EPSILON);
        assertEquals(0.0, afterExpiry.absorbedDamage(), EPSILON);
    }
}
