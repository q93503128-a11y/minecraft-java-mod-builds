package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.moonseungjun.openworldrpg.combat.runtime.GuardianRootPassiveEffects;
import org.junit.jupiter.api.Test;

class GuardianRootPassiveEffectsTest {
    @Test
    void rootPassiveNumericContractsMatchCanonicalRanks() {
        assertEquals(
                0.09,
                GuardianRootPassiveEffects.maxHealthPercentBonusForRank(3),
                0.0001
        );
        assertEquals(
                12,
                GuardianRootPassiveEffects.maxStaminaFlatBonusForRank(3)
        );
        assertEquals(
                0.03,
                GuardianRootPassiveEffects.staminaRecoveryBonusForRank(3),
                0.0001
        );
        assertEquals(
                0.88,
                GuardianRootPassiveEffects
                        .guardImpactStaminaCostMultiplierForRank(3),
                0.0001
        );
        assertEquals(
                0.09,
                GuardianRootPassiveEffects.barrierOutputBonusForRank(3),
                0.0001
        );
        assertEquals(
                60L,
                GuardianRootPassiveEffects.resolveExpiryBonusTicksForRank(2)
        );
        assertEquals(
                1.16,
                GuardianRootPassiveEffects
                        .perfectGuardPoiseOutputMultiplierForRank(2),
                0.0001
        );
        assertEquals(
                200L,
                GuardianRootPassiveEffects.STAND_TOGETHER_ICD_TICKS
        );
    }

    @Test
    void invalidRanksFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> GuardianRootPassiveEffects
                        .maxHealthPercentBonusForRank(4)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> GuardianRootPassiveEffects
                        .resolveExpiryBonusTicksForRank(3)
        );
    }
}
