package dev.moonseungjun.openworldrpg.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RecoveryEffectAuthorityTest {
    @Test
    void healingPotionRestoresExactlyThirtyFivePercentMaxHp() {
        var effect = RecoveryEffectAuthority.resolve(
                RecoveryConsumable.HEALING_POTION,
                200.0,
                120.0
        );

        assertEquals(70.0, effect.hpRestore(), 0.0001);
        assertEquals(0.0, effect.immediateManaRestore(), 0.0001);
        assertFalse(effect.cleanseMinorDispellable());
    }

    @Test
    void focusDraughtSplitsFortyPercentManaIntoImmediateAndThreeSecondTail() {
        var effect = RecoveryEffectAuthority.resolve(
                RecoveryConsumable.FOCUS_DRAUGHT,
                200.0,
                120.0
        );

        assertEquals(30.0, effect.immediateManaRestore(), 0.0001);
        assertEquals(0.3, effect.manaRestorePerTick(), 0.0001);
        assertEquals(60, effect.manaTailTicks());
    }

    @Test
    void cleansingTonicPublishesTenSecondBuildupResistanceRequest() {
        var effect = RecoveryEffectAuthority.resolve(
                RecoveryConsumable.CLEANSING_TONIC,
                200.0,
                120.0
        );

        assertTrue(effect.cleanseMinorDispellable());
        assertEquals(200, effect.negativeBuildupResistanceTicks());
    }

    @Test
    void potionFoodStrengthScalesNumericPotionMagnitudeButNotQualitativeCleanse() {
        var healing = RecoveryEffectAuthority.strengthen(
                RecoveryEffectAuthority.resolve(
                        RecoveryConsumable.HEALING_POTION,
                        200.0,
                        120.0
                ),
                0.10
        );
        assertEquals(77.0, healing.hpRestore(), 0.0001);

        var focus = RecoveryEffectAuthority.strengthen(
                RecoveryEffectAuthority.resolve(
                        RecoveryConsumable.FOCUS_DRAUGHT,
                        200.0,
                        120.0
                ),
                0.10
        );
        assertEquals(33.0, focus.immediateManaRestore(), 0.0001);
        assertEquals(0.33, focus.manaRestorePerTick(), 0.0001);
        assertEquals(60, focus.manaTailTicks());

        var tonic = RecoveryEffectAuthority.strengthen(
                RecoveryEffectAuthority.resolve(
                        RecoveryConsumable.CLEANSING_TONIC,
                        200.0,
                        120.0
                ),
                0.10
        );
        assertTrue(tonic.cleanseMinorDispellable());
        assertEquals(200, tonic.negativeBuildupResistanceTicks());
    }

    @Test
    void canonicalRecoveryTimingsRemainLocked() {
        assertEquals(19, RecoveryActionRules.USE_DURATION_TICKS);
        assertEquals(14, RecoveryActionRules.RESOLUTION_TICKS);
        assertEquals(120, RecoveryActionRules.SHARED_LOCKOUT_TICKS);
        assertEquals(200, RecoveryActionRules.CLEANSING_BUILDUP_RESISTANCE_TICKS);
        assertEquals(0.65, RecoveryActionRules.ACTION_MOVEMENT_MULTIPLIER, 0.0001);
    }
}
