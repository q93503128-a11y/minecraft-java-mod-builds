package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerDefenseRuntimeState;
import org.junit.jupiter.api.Test;

class PlayerDefenseRuntimeStateTest {
    private static final double EPSILON = 0.0001;

    @Test
    void canonicalShieldGuardRatingsMatchEquipmentFormula() {
        assertEquals(
                22.0,
                PlayerDefenseAuthority.shieldGuardRating(
                        8,
                        PlayerDefenseAuthority.GuardType.BUCKLER
                ),
                EPSILON
        );
        assertEquals(
                28.0,
                PlayerDefenseAuthority.shieldGuardRating(
                        8,
                        PlayerDefenseAuthority.GuardType.STANDARD_SHIELD
                ),
                EPSILON
        );
        assertEquals(
                35.0,
                PlayerDefenseAuthority.shieldGuardRating(
                        8,
                        PlayerDefenseAuthority.GuardType.HEAVY_SHIELD
                ),
                EPSILON
        );
    }

    @Test
    void dodgeSpendsThirtyStaminaAndUsesExactIframeAndReentryWindows() {
        PlayerCombatState resources = new PlayerCombatState(5, 0);
        PlayerDefenseRuntimeState defense = new PlayerDefenseRuntimeState();

        assertTrue(defense.tryBeginDodge(resources, 0, false));
        assertEquals(70.0, resources.stamina(0), EPSILON);
        assertTrue(defense.isDodgeInvulnerable(5));
        assertFalse(defense.isDodgeInvulnerable(6));
        assertTrue(defense.isDodgeActionActive(8));
        assertFalse(defense.isDodgeActionActive(9));
        assertFalse(defense.tryBeginDodge(resources, 10, false));
        assertTrue(defense.tryBeginDodge(resources, 11, false));

        assertEquals(40.0, resources.stamina(11), EPSILON);
        assertEquals(22L, defense.nextDodgeAllowedTick());
    }

    @Test
    void perfectGuardUsesFourTickWindowAndQuarterGuardCostWithoutHpDamage() {
        PlayerCombatState resources = new PlayerCombatState(5, 0);
        PlayerDefenseRuntimeState active = new PlayerDefenseRuntimeState();
        var snapshot = PlayerDefenseAuthority.DefenseSnapshot.guarded(
                35.0,
                18.0,
                PlayerDefenseAuthority.shieldGuardRating(
                        8,
                        PlayerDefenseAuthority.GuardType.STANDARD_SHIELD
                ),
                PlayerDefenseAuthority.GuardType.STANDARD_SHIELD
        );

        assertTrue(active.pressGuard(0).perfectWindowStarted());
        assertTrue(active.isPerfectGuardWindow(3));
        assertFalse(active.isPerfectGuardWindow(4));

        var secondPressWhileHeld = active.pressGuard(1);
        assertTrue(secondPressWhileHeld.accepted());
        assertFalse(secondPressWhileHeld.perfectWindowStarted());

        active.releaseGuard();
        assertTrue(active.pressGuard(2).accepted());
        assertFalse(active.isPerfectGuardWindow(2));

        active.releaseGuard();
        assertTrue(active.pressGuard(10).perfectWindowStarted());
        var result = active.resolveIncoming(
                resources,
                snapshot,
                physicalHit(100.0, 8, PlayerDefenseAuthority.GuardPressureBand.MEDIUM),
                10
        );

        assertTrue(result.guarded());
        assertTrue(result.perfectGuarded());
        assertFalse(result.guardBroken());
        assertEquals(0.0, result.finalDamage(), EPSILON);
        assertEquals(3.3523083819, result.staminaSpent(), EPSILON);
        assertEquals(96.6476916181, resources.stamina(10), EPSILON);
    }

    @Test
    void ordinaryGuardAppliesDefenseFirstThenCanonicalAbsorption() {
        PlayerCombatState resources = new PlayerCombatState(5, 0);
        PlayerDefenseRuntimeState active = new PlayerDefenseRuntimeState();
        var snapshot = PlayerDefenseAuthority.DefenseSnapshot.guarded(
                35.0,
                18.0,
                28.0,
                PlayerDefenseAuthority.GuardType.STANDARD_SHIELD
        );

        assertTrue(active.pressGuard(0).accepted());
        var result = active.resolveIncoming(
                resources,
                snapshot,
                physicalHit(100.0, 8, PlayerDefenseAuthority.GuardPressureBand.MEDIUM),
                4
        );

        assertTrue(result.guarded());
        assertFalse(result.perfectGuarded());
        assertFalse(result.guardBroken());
        assertEquals(74.7974797480, result.mitigatedBeforeActiveDefense(), EPSILON);
        assertEquals(11.0, result.finalDamage(), EPSILON);
        assertEquals(13.4092335276, result.staminaSpent(), EPSILON);
    }

    @Test
    void guardBreakDrainsRemainingStaminaHalvesAbsorptionAndLocksDefenseRestart() {
        PlayerCombatState resources = new PlayerCombatState(5, 0);
        assertTrue(resources.spendStamina(95.0, 100, 0));

        PlayerDefenseRuntimeState active = new PlayerDefenseRuntimeState();
        var snapshot = PlayerDefenseAuthority.DefenseSnapshot.guarded(
                35.0,
                18.0,
                28.0,
                PlayerDefenseAuthority.GuardType.STANDARD_SHIELD
        );

        assertTrue(active.pressGuard(0).accepted());
        var result = active.resolveIncoming(
                resources,
                snapshot,
                physicalHit(100.0, 8, PlayerDefenseAuthority.GuardPressureBand.MEDIUM),
                4
        );

        assertTrue(result.guardBroken());
        assertEquals(5.0, result.staminaSpent(), EPSILON);
        assertEquals(0.0, resources.stamina(4), EPSILON);
        assertEquals(43.0, result.finalDamage(), EPSILON);
        assertTrue(active.isGuardBreakReaction(20));
        assertFalse(active.isGuardBreakReaction(21));
        assertFalse(active.pressGuard(12).accepted());
        assertTrue(active.pressGuard(13).accepted());
    }

    @Test
    void physicalAndMagicHitsUseTheirOwnDefenseStatsAndRoutineCap() {
        var snapshot = PlayerDefenseAuthority.DefenseSnapshot.unguarded(
                10_000.0,
                18.0
        );
        PlayerCombatState resources = new PlayerCombatState(5, 0);
        PlayerDefenseRuntimeState active = new PlayerDefenseRuntimeState();

        var physical = active.resolveIncoming(
                resources,
                snapshot,
                physicalHit(100.0, 8, PlayerDefenseAuthority.GuardPressureBand.LIGHT),
                0
        );
        var magic = active.resolveIncoming(
                resources,
                snapshot,
                PlayerDefenseAuthority.IncomingHit.unguardable(
                        100.0,
                        ProjectImpactTransaction.DamageSchool.MAGIC,
                        8,
                        true
                ),
                0
        );

        assertEquals(30.0, physical.finalDamage(), EPSILON);
        assertEquals(85.0, magic.finalDamage(), EPSILON);
    }

    @Test
    void acceptedDodgeNullifiesOnlyDodgeableHits() {
        PlayerCombatState resources = new PlayerCombatState(5, 0);
        PlayerDefenseRuntimeState active = new PlayerDefenseRuntimeState();
        var snapshot = PlayerDefenseAuthority.DefenseSnapshot.unguarded(0.0, 0.0);

        assertTrue(active.tryBeginDodge(resources, 0, false));

        var dodgeable = active.resolveIncoming(
                resources,
                snapshot,
                physicalHit(50.0, 8, PlayerDefenseAuthority.GuardPressureBand.LIGHT),
                2
        );
        var unavoidable = active.resolveIncoming(
                resources,
                snapshot,
                PlayerDefenseAuthority.IncomingHit.unguardable(
                        50.0,
                        ProjectImpactTransaction.DamageSchool.PHYSICAL,
                        8,
                        false
                ),
                2
        );

        assertTrue(dodgeable.dodged());
        assertEquals(0.0, dodgeable.finalDamage(), EPSILON);
        assertFalse(unavoidable.dodged());
        assertEquals(50.0, unavoidable.finalDamage(), EPSILON);
    }

    @Test
    void unguardableHitCarriesNoInventedGuardPressure() {
        var hit = PlayerDefenseAuthority.IncomingHit.unguardable(
                50.0,
                ProjectImpactTransaction.DamageSchool.MAGIC,
                8,
                true
        );

        assertTrue(hit.guardPressure().isEmpty());
        assertFalse(hit.guardable());
        assertFalse(hit.perfectGuardable());
    }

    private static PlayerDefenseAuthority.IncomingHit physicalHit(
            double rawDamage,
            int attackerLevel,
            PlayerDefenseAuthority.GuardPressureBand band
    ) {
        return PlayerDefenseAuthority.IncomingHit.baseline(
                rawDamage,
                ProjectImpactTransaction.DamageSchool.PHYSICAL,
                attackerLevel,
                band,
                true,
                true,
                true
        );
    }
    @Test
    void perfectOnlyCommittedChargeCanBeJustGuardedButNotHeldBlocked() {
        var snapshot = PlayerDefenseAuthority.DefenseSnapshot.guarded(
                35.0,
                18.0,
                28.0,
                PlayerDefenseAuthority.GuardType.STANDARD_SHIELD
        );
        var perfectOnlyCharge = PlayerDefenseAuthority.IncomingHit.baseline(
                100.0,
                ProjectImpactTransaction.DamageSchool.PHYSICAL,
                8,
                PlayerDefenseAuthority.GuardPressureBand.HEAVY,
                true,
                false,
                true
        );

        PlayerCombatState perfectResources = new PlayerCombatState(5, 0);
        PlayerDefenseRuntimeState perfectDefense = new PlayerDefenseRuntimeState();
        assertTrue(perfectDefense.pressGuard(0).perfectWindowStarted());

        var perfect = perfectDefense.resolveIncoming(
                perfectResources,
                snapshot,
                perfectOnlyCharge,
                0
        );

        assertTrue(perfect.guarded());
        assertTrue(perfect.perfectGuarded());
        assertEquals(0.0, perfect.finalDamage(), EPSILON);
        assertTrue(perfect.staminaSpent() > 0.0);

        PlayerCombatState lateResources = new PlayerCombatState(5, 0);
        PlayerDefenseRuntimeState lateDefense = new PlayerDefenseRuntimeState();
        assertTrue(lateDefense.pressGuard(0).perfectWindowStarted());

        var late = lateDefense.resolveIncoming(
                lateResources,
                snapshot,
                perfectOnlyCharge,
                4
        );

        assertFalse(late.guarded());
        assertFalse(late.perfectGuarded());
        assertFalse(late.guardBroken());
        assertEquals(75.0, late.finalDamage(), EPSILON);
        assertEquals(0.0, late.staminaSpent(), EPSILON);
        assertEquals(100.0, lateResources.stamina(4), EPSILON);
    }


}
