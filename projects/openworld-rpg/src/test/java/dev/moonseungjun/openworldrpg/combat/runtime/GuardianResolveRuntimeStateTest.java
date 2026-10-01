package dev.moonseungjun.openworldrpg.combat.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class GuardianResolveRuntimeStateTest {
    @Test
    void perfectGuardsReachMaxAndResolveKeeperExtendsExactExpiry() {
        var state = new GuardianResolveRuntimeState();
        state.synchronizeExpiryBonusTicks(60L);

        assertTrue(state.recordPerfectGuard(0L, 0L).pipAdded());
        assertTrue(state.recordPerfectGuard(1L, 1L).pipAdded());
        var max = state.recordPerfectGuard(2L, 2L);
        assertTrue(max.pipAdded());
        assertTrue(max.reachedMaxNow());
        assertEquals(3, state.pips(261L, 2L));
        assertEquals(0, state.pips(262L, 2L));
    }

    @Test
    void guardedHitUsesFinalCostThresholdAndExactTwoSecondIcd() {
        var state = new GuardianResolveRuntimeState();

        var low = state.recordGuardedHit(17.99, 0L, 0L);
        assertFalse(low.thresholdQualified());
        assertEquals(0, low.currentPips());

        var first = state.recordGuardedHit(18.0, 0L, 0L);
        assertTrue(first.pipAdded());
        assertTrue(state.recordGuardedHit(54.0, 39L, 39L).icdBlocked());
        assertTrue(state.recordGuardedHit(18.0, 40L, 40L).pipAdded());
    }

    @Test
    void barrierGainUsesEightPercentThresholdPerRecipientAndFourSecondIcd() {
        var state = new GuardianResolveRuntimeState();
        UUID ally = UUID.randomUUID();

        assertFalse(
                state.recordBarrierAbsorption(
                        ally,
                        7.99,
                        100.0,
                        0L,
                        0L
                ).thresholdQualified()
        );
        assertTrue(
                state.recordBarrierAbsorption(
                        ally,
                        8.0,
                        100.0,
                        1L,
                        1L
                ).pipAdded()
        );
        assertTrue(
                state.recordBarrierAbsorption(
                        ally,
                        20.0,
                        100.0,
                        80L,
                        80L
                ).icdBlocked()
        );
        assertTrue(
                state.recordBarrierAbsorption(
                        ally,
                        8.0,
                        100.0,
                        81L,
                        81L
                ).pipAdded()
        );
    }

    @Test
    void standTogetherClaimUsesExactTenSecondIcd() {
        var state = new GuardianResolveRuntimeState();

        assertTrue(state.tryClaimStandTogether(100L));
        assertFalse(state.tryClaimStandTogether(299L));
        assertTrue(state.tryClaimStandTogether(300L));
    }
}
