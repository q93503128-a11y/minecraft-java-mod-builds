package dev.moonseungjun.openworldrpg.combat.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClericRootPassiveRuntimeStateTest {
    @Test
    void balancedServiceDiscountsOnlyTheOppositeNextSkillInsideFourSeconds() {
        var state = new ClericRootPassiveRuntimeState();
        state.synchronizeBalancedServiceRank(2);

        state.recordDamagingEligibleHit(100L);
        assertEquals(
                0.90,
                state.previewManaCostMultiplier(
                        false,
                        true,
                        179L
                ),
                0.0001
        );
        assertEquals(
                1.0,
                state.previewManaCostMultiplier(
                        true,
                        false,
                        179L
                ),
                0.0001
        );
        state.consumeManaDiscount(false, true, 179L);
        assertEquals(
                1.0,
                state.previewManaCostMultiplier(
                        false,
                        true,
                        179L
                ),
                0.0001
        );

        state.recordEffectiveHealing(200L);
        assertEquals(
                0.90,
                state.previewManaCostMultiplier(
                        true,
                        false,
                        279L
                ),
                0.0001
        );
        assertEquals(
                1.0,
                state.previewManaCostMultiplier(
                        true,
                        false,
                        280L
                ),
                0.0001
        );
    }

    @Test
    void livingDoctrineUsesSixSecondIcd() {
        var state = new ClericRootPassiveRuntimeState();

        assertTrue(state.tryClaimLivingDoctrine(100L));
        assertFalse(state.tryClaimLivingDoctrine(219L));
        assertTrue(state.tryClaimLivingDoctrine(220L));
    }
}
