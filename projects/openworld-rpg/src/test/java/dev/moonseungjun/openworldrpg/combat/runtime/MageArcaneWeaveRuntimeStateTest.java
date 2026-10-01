package dev.moonseungjun.openworldrpg.combat.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MageArcaneWeaveRuntimeStateTest {
    @Test
    void threeDistinctActivesCompleteWeaveAndNextConsumerCarriesTriuneSnapshot() {
        var state = new MageArcaneWeaveRuntimeState();

        var first = state.recordAcceptedActive("openworld_rpg:a", true, 0L, 60L, 1.10);
        assertTrue(first.distinctSigilAdded());
        assertEquals(1, first.sigilCountAfter());
        assertFalse(first.weaveReadyAfter());

        state.recordAcceptedActive("openworld_rpg:b", true, 10L, 60L, 1.10);
        var third = state.recordAcceptedActive("openworld_rpg:c", true, 20L, 60L, 1.10);
        assertTrue(third.weaveCompleted());
        assertEquals(0, third.sigilCountAfter());
        assertTrue(third.weaveReadyAfter());

        var consumer = state.recordAcceptedActive("openworld_rpg:d", true, 21L, 60L, 1.10);
        assertTrue(consumer.weaveConsumed());
        assertEquals(1.10, consumer.weaveEffectMagnitudeMultiplier(), 0.0001);
        assertEquals(1, consumer.sigilCountAfter());

        var snapshot = state.consumeEmpoweredCast("openworld_rpg:d", 22L);
        assertTrue(snapshot.isPresent());
        assertEquals(1.10, snapshot.getAsDouble(), 0.0001);
        assertTrue(state.consumeEmpoweredCast("openworld_rpg:d", 22L).isEmpty());
    }

    @Test
    void duplicateSigilsRefreshTimerWithoutAddingAndExpiryIsExact() {
        var state = new MageArcaneWeaveRuntimeState();

        state.recordAcceptedActive("openworld_rpg:a", true, 0L, 30L, 1.0);
        var duplicate = state.recordAcceptedActive(
                "openworld_rpg:a", true, 180L, 30L, 1.0
        );
        assertFalse(duplicate.distinctSigilAdded());
        assertEquals(1, duplicate.sigilCountAfter());
        assertEquals(1, state.sigilCount(369L));
        assertEquals(0, state.sigilCount(370L));
    }

    @Test
    void weaveReadyAndResonantMindUseExactCanonicalWindows() {
        var state = new MageArcaneWeaveRuntimeState();
        state.recordAcceptedActive("openworld_rpg:a", true, 0L, 0L, 1.0);
        state.recordAcceptedActive("openworld_rpg:b", true, 1L, 0L, 1.0);
        state.recordAcceptedActive("openworld_rpg:c", true, 2L, 0L, 1.0);

        assertTrue(state.weaveReady(161L));
        assertFalse(state.weaveReady(162L));

        assertTrue(state.tryClaimResonantMind(100L));
        assertFalse(state.tryClaimResonantMind(199L));
        assertTrue(state.tryClaimResonantMind(200L));
    }
}
