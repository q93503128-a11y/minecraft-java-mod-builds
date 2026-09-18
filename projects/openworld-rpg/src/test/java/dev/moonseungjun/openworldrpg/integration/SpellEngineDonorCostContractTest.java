package dev.moonseungjun.openworldrpg.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.integration.spellengine.SpellEngineDonorCostContract;
import org.junit.jupiter.api.Test;

class SpellEngineDonorCostContractTest {
    @Test
    void fullyNeutralDonorCostIsAccepted() {
        var contract = new SpellEngineDonorCostContract(
                false,
                0.0,
                0,
                false,
                false,
                false,
                0.0,
                0.0
        );

        assertTrue(contract.isNeutralForProjectAuthority());
    }

    @Test
    void spellEngineDefaultExhaustAndDurabilityAreRejected() {
        var contract = new SpellEngineDonorCostContract(
                false,
                0.1,
                1,
                false,
                false,
                false,
                0.0,
                0.0
        );

        assertFalse(contract.isNeutralForProjectAuthority());
    }

    @Test
    void delayedOrHiddenDonorCostsAreRejected() {
        assertFalse(new SpellEngineDonorCostContract(
                true, 0.0, 0, false, false, false, 0.0, 0.0
        ).isNeutralForProjectAuthority());

        assertFalse(new SpellEngineDonorCostContract(
                false, 0.0, 0, false, true, false, 0.0, 0.0
        ).isNeutralForProjectAuthority());

        assertFalse(new SpellEngineDonorCostContract(
                false, 0.0, 0, false, false, true, 0.0, 0.0
        ).isNeutralForProjectAuthority());

        assertFalse(new SpellEngineDonorCostContract(
                false, 0.0, 0, false, false, false, 0.25, 0.0
        ).isNeutralForProjectAuthority());
    }
}
