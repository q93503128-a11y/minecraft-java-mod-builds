package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.moonseungjun.openworldrpg.combat.runtime.ClericRootPassiveEffects;
import org.junit.jupiter.api.Test;

class ClericRootPassiveEffectsTest {
    @Test
    void rootPassiveNumericContractsMatchCanonicalRanks() {
        assertEquals(
                15,
                ClericRootPassiveEffects.maxManaFlatBonusForRank(3)
        );
        assertEquals(
                1.09,
                ClericRootPassiveEffects.healingOutputMultiplierForRank(3),
                0.0001
        );
        assertEquals(
                0.09,
                ClericRootPassiveEffects.barrierOutputBonusForRank(3),
                0.0001
        );
        assertEquals(
                1.12,
                ClericRootPassiveEffects
                        .equipmentMagicResistanceMultiplierForRank(3),
                0.0001
        );
        assertEquals(
                60L,
                ClericRootPassiveEffects.graceExpiryBonusTicksForRank(2)
        );
        assertEquals(
                0.90,
                ClericRootPassiveEffects
                        .balancedServiceManaCostMultiplierForRank(2),
                0.0001
        );
        assertEquals(
                8.0,
                ClericRootPassiveEffects.LIVING_DOCTRINE_MANA_RESTORE,
                0.0001
        );
    }

    @Test
    void invalidRanksFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ClericRootPassiveEffects
                        .maxManaFlatBonusForRank(4)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ClericRootPassiveEffects
                        .balancedServiceManaCostMultiplierForRank(3)
        );
    }
}
