package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.SnaredRuntimeState;
import org.junit.jupiter.api.Test;

class SnaredRuntimeStateTest {
    private static final double EPSILON = 0.0001;

    @Test
    void reapplicationKeepsStrongerSlowAndLaterExpiry() {
        var state = new SnaredRuntimeState();

        state.apply(0.88, 50L, 100L);
        state.apply(0.65, 30L, 110L);

        var snapshot = state.snapshot(120L);
        assertTrue(snapshot.active());
        assertEquals(
                0.65,
                snapshot.movementMultiplier(),
                EPSILON
        );
        assertEquals(150L, snapshot.expiresAtTick());
    }

    @Test
    void weakerFollowUpCannotShortenEmpoweredSnare() {
        var state = new SnaredRuntimeState();

        state.apply(0.65, 90L, 10L);
        state.apply(0.80, 50L, 20L);

        var snapshot = state.snapshot(30L);
        assertEquals(
                0.65,
                snapshot.movementMultiplier(),
                EPSILON
        );
        assertEquals(100L, snapshot.expiresAtTick());
    }

    @Test
    void expiryRestoresNeutralMovement() {
        var state = new SnaredRuntimeState();
        state.apply(0.80, 50L, 10L);

        assertTrue(state.snapshot(59L).active());
        assertFalse(state.snapshot(60L).active());
        assertEquals(
                1.0,
                state.snapshot(60L).movementMultiplier(),
                EPSILON
        );
    }
}
