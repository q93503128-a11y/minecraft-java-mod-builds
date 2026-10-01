package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.BurningRuntimeState;
import org.junit.jupiter.api.Test;

class BurningRuntimeStateTest {
    @Test
    void fullDurationProducesExactlyEightTimedDamageTicks() {
        var state = new BurningRuntimeState();
        state.apply(5.0, 0L);

        int count = 0;
        double total = 0.0;
        for (long tick = 0L; tick <= 80L; tick++) {
            var due = state.pollTick(tick);
            if (!due.due()) {
                continue;
            }
            count++;
            total += due.damage();
            if (tick < 80L) {
                assertFalse(due.finalTick());
            }
        }

        assertEquals(8, count);
        assertEquals(40.0, total, 0.0001);
        assertFalse(state.snapshot(80L).active());
    }

    @Test
    void strongerMagnitudeWinsWhileWeakerReapplicationStillRefreshesDuration() {
        var state = new BurningRuntimeState();
        var first = state.apply(5.0, 0L);
        assertTrue(first.magnitudeReplaced());
        assertEquals(80L, first.expiresAt());

        var weaker = state.apply(4.0, 20L);
        assertFalse(weaker.magnitudeReplaced());
        assertEquals(5.0, weaker.tickDamage(), 0.0001);
        assertEquals(100L, weaker.expiresAt());

        var stronger = state.apply(6.0, 30L);
        assertTrue(stronger.magnitudeReplaced());
        assertEquals(6.0, stronger.tickDamage(), 0.0001);
        assertEquals(110L, stronger.expiresAt());
    }
}
