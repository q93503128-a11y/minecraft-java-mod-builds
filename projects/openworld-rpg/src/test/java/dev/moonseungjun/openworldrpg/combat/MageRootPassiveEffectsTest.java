package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.runtime.MageRootPassiveEffects;
import org.junit.jupiter.api.Test;

class MageRootPassiveEffectsTest {
    @Test
    void rootPassiveNumericContractsMatchCanonicalRanks() {
        assertEquals(18, MageRootPassiveEffects.maxManaFlatBonusForRank(3));
        assertEquals(0.91, MageRootPassiveEffects.skillManaCostMultiplierForRank(3), 0.0001);
        assertEquals(0.06, MageRootPassiveEffects.magicPowerBonusForRank(3), 0.0001);
        assertEquals(0.06, MageRootPassiveEffects.castSpeedBonusForRank(3), 0.0001);
        assertEquals(60L, MageRootPassiveEffects.weaveSequenceExpiryBonusTicksForRank(2));
        assertEquals(1.10, MageRootPassiveEffects.triuneStudyMagnitudeMultiplierForRank(2), 0.0001);
        assertEquals(8.0, MageRootPassiveEffects.RESONANT_MIND_MANA_RESTORE, 0.0001);
        assertEquals(100L, MageRootPassiveEffects.RESONANT_MIND_ICD_TICKS);
    }

    @Test
    void spellEdgeAddsOnlyToTheMagicPowerBucket() {
        var source = new ProjectImpactTransaction.DamageSourceSnapshot(
                20, 44.0, 18.0, 0.20, 1.15
        );
        var boosted = MageRootPassiveEffects.applyMagicPowerBonus(
                source,
                MageRootPassiveEffects.magicPowerBonusForRank(3)
        );

        assertEquals(source.contentLevel(), boosted.contentLevel());
        assertEquals(source.weaponPower(), boosted.weaponPower());
        assertEquals(source.weightedOffensiveStat(), boosted.weightedOffensiveStat());
        assertEquals(0.26, boosted.additivePowerBonus(), 0.0001);
        assertEquals(source.poiseOutputMultiplier(), boosted.poiseOutputMultiplier());
    }

    @Test
    void invalidRanksFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MageRootPassiveEffects.maxManaFlatBonusForRank(4)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> MageRootPassiveEffects.triuneStudyMagnitudeMultiplierForRank(3)
        );
    }
}
