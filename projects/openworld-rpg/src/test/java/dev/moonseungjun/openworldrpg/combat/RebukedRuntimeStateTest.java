package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.RebukedRuntimeState;
import org.junit.jupiter.api.Test;

class RebukedRuntimeStateTest {
    private static final double EPSILON = 0.0001;

    @Test
    void reapplicationDoesNotStackOrShortenStrongerLongerState() {
        var state = new RebukedRuntimeState();

        state.apply(0.92, 80L, 100L);
        state.apply(0.92, 60L, 110L);

        var snapshot = state.snapshot(120L);
        assertTrue(snapshot.active());
        assertEquals(
                0.92,
                snapshot.outgoingDirectDamageMultiplier(),
                EPSILON
        );
        assertEquals(180L, snapshot.expiresAtTick());
    }

    @Test
    void strongerReapplicationKeepsStrongerMultiplierAndLaterExpiry() {
        var state = new RebukedRuntimeState();

        state.apply(0.92, 60L, 20L);
        state.apply(0.85, 40L, 30L);

        var snapshot = state.snapshot(40L);
        assertEquals(
                0.85,
                snapshot.outgoingDirectDamageMultiplier(),
                EPSILON
        );
        assertEquals(80L, snapshot.expiresAtTick());
    }

    @Test
    void expiryRestoresNeutralOutgoingDamage() {
        var state = new RebukedRuntimeState();
        state.apply(0.85, 80L, 10L);

        assertEquals(
                0.85,
                state.outgoingDirectDamageMultiplier(89L),
                EPSILON
        );
        assertEquals(
                1.0,
                state.outgoingDirectDamageMultiplier(90L),
                EPSILON
        );
        assertFalse(state.snapshot(90L).active());
    }
}
