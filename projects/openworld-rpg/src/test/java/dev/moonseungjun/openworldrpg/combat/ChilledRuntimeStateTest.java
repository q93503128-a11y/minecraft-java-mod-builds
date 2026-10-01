package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.ChilledRuntimeState;
import org.junit.jupiter.api.Test;

class ChilledRuntimeStateTest {
    @Test
    void strongerMagnitudeWinsAndReapplicationNeverShortensDuration() {
        var state = new ChilledRuntimeState();

        state.apply(0.80, 50L, 10L);
        state.apply(0.65, 30L, 20L);

        var snapshot = state.snapshot(30L);
        assertTrue(snapshot.active());
        assertEquals(0.65, snapshot.movementMultiplier(), 0.0001);
        assertEquals(60L, snapshot.expiresAtTick());

        state.apply(0.85, 40L, 40L);
        snapshot = state.snapshot(50L);
        assertEquals(0.65, snapshot.movementMultiplier(), 0.0001);
        assertEquals(80L, snapshot.expiresAtTick());
    }

    @Test
    void expiryRestoresNeutralMovement() {
        var state = new ChilledRuntimeState();
        state.apply(0.65, 70L, 10L);

        assertTrue(state.snapshot(79L).active());
        assertFalse(state.snapshot(80L).active());
        assertEquals(1.0, state.snapshot(80L).movementMultiplier(), 0.0001);
    }
}
