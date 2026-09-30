package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.CombatDamageAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.runtime.HunterQuarryFocusRuntimeState;
import dev.moonseungjun.openworldrpg.combat.runtime.HunterRootPassiveEffects;
import dev.moonseungjun.openworldrpg.combat.state.AttributeAllocation;
import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatState;
import dev.moonseungjun.openworldrpg.combat.state.EquipmentResourceModifiers;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatBuildState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatState;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HunterRootPassiveEffectsTest {
    private static final long NO_COMBAT = Long.MIN_VALUE / 4;

    @Test
    void canonicalRootPassiveRanksResolveExactMagnitudes() {
        assertEquals(0.06, HunterRootPassiveEffects.criticalChanceBonusForRank(3), 0.0001);
        assertEquals(0.06, HunterRootPassiveEffects.movementSpeedBonusForRank(3), 0.0001);
        assertEquals(0.91, HunterRootPassiveEffects.skillManaCostMultiplierForRank(3), 0.0001);
        assertEquals(1.06, HunterRootPassiveEffects.quarryDirectDamageMultiplierForRank(3), 0.0001);
        assertEquals(60L, HunterRootPassiveEffects.focusExpiryBonusTicksForRank(2));
        assertEquals(0.08, HunterRootPassiveEffects.weakPointBonusForRank(2), 0.0001);
        assertTrue(HunterRootPassiveEffects.isRangedCapable(ProjectWeaponFamily.BOW));
        assertTrue(HunterRootPassiveEffects.isRangedCapable(ProjectWeaponFamily.MUSKET_HAND_CANNON));
        assertFalse(HunterRootPassiveEffects.isRangedCapable(ProjectWeaponFamily.SWORD));
    }

    @Test
    void focusRetentionExtendsQuarryFocusLifetimeWithoutBreakingClearInvariant() {
        var state = new HunterQuarryFocusRuntimeState();
        UUID target = UUID.randomUUID();
        state.recordRangedHit(target, 8.0, false, 0L, NO_COMBAT, 60L, false);

        assertTrue(state.snapshot(219L, 59L).quarryId().isPresent());
        assertEquals(1, state.snapshot(219L, 59L).focus());
        assertTrue(state.snapshot(220L, 59L).quarryId().isEmpty());
        assertEquals(0, state.snapshot(220L, 59L).focus());
    }

    @Test
    void trailSenseUsesPerTargetTwentySecondIcd() {
        var state = new HunterQuarryFocusRuntimeState();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        var firstHit = state.recordRangedHit(first, 3.0, false, 0L, NO_COMBAT, 0L, true);
        assertTrue(firstHit.trailSenseFocusGranted());
        assertEquals(1, firstHit.focusAfter());

        var secondHit = state.recordRangedHit(second, 3.0, false, 1L, 0L, 0L, true);
        assertTrue(secondHit.trailSenseFocusGranted());
        assertEquals(2, secondHit.focusAfter());

        var inside = state.recordRangedHit(first, 3.0, false, 2L, 1L, 0L, true);
        assertFalse(inside.trailSenseFocusGranted());

        state.recordRangedHit(second, 3.0, false, 401L, 400L, 0L, false);
        var after = state.recordRangedHit(first, 3.0, false, 402L, 401L, 0L, true);
        assertTrue(after.trailSenseFocusGranted());
    }

    @Test
    void efficientDrawIsIndependentFromTheGearReductionCap() {
        PlayerCombatState state = new PlayerCombatState(5, 0L);
        state.synchronizeResourceModifiers(
                new EquipmentResourceModifiers(
                        0.0, 0.0, 0.0, 0.0, 0.0,
                        0.20, 0.0
                ),
                0L
        );
        state.synchronizeClassSkillManaCostMultiplier(0.91, 0L);
        assertEquals(36.4, state.effectiveManaCost(50.0), 0.0001);
    }

    @Test
    void keenEyeAndWeakPointStudyFeedNormalBasicAuthorityWithoutChangingPoise() {
        var build = new PlayerCombatBuildState(
                8,
                RootClass.HUNTER,
                new AttributeAllocation(0, 0, 0, 7, 0, 0),
                EquipmentCombatState.weaponOnly(ProjectWeaponFamily.BOW, 8)
        );
        var target = new ProjectImpactTransaction.DamageTargetSnapshot(
                45.0, 35.0, 1.0, 0.0, 190.0
        );

        var baseline = CombatDamageAuthority.authorizeBowProjectileBasic(
                5.0F, 1.0, build, target, 0.10
        );
        var passive = CombatDamageAuthority.authorizeBowProjectileBasic(
                5.0F, 1.0, build, target, 0.10,
                0.06, 1.33, 1.0
        );

        assertFalse(baseline.critical());
        assertTrue(passive.critical());
        assertTrue(passive.finalDamage() > baseline.finalDamage());
        assertEquals(baseline.poiseDamage(), passive.poiseDamage(), 0.0001);
    }
}
