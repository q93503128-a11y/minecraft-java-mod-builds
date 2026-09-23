package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.CombatDamageAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.state.AttributeAllocation;
import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatBuildState;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import org.junit.jupiter.api.Test;

class CombatDamageAuthorityTest {
    private static final ProjectImpactTransaction.DamageTargetSnapshot EARTHLOONG =
            new ProjectImpactTransaction.DamageTargetSnapshot(
                    45.0,
                    35.0,
                    1.0,
                    0.0,
                    190.0
            );

    @Test
    void swordBasicHitUsesProjectCadenceAndIgnoresDonorDamageMagnitude() {
        var build = new PlayerCombatBuildState(
                8,
                RootClass.WARRIOR,
                new AttributeAllocation(0, 0, 7, 0, 0, 0),
                EquipmentCombatState.weaponOnly(ProjectWeaponFamily.SWORD, 8)
        );

        var weakDonor = CombatDamageAuthority.authorizeBetterCombatMelee(
                2.0F,
                0,
                build,
                EARTHLOONG
        );
        var hugeDonor = CombatDamageAuthority.authorizeBetterCombatMelee(
                9999.0F,
                0,
                build,
                EARTHLOONG
        );

        assertTrue(weakDonor.accepted());
        assertEquals(17.0, weakDonor.finalDamage(), 0.0001);
        assertEquals(10.0, weakDonor.poiseDamage(), 0.0001);
        assertEquals(0.8, weakDonor.damageActionCoefficient(), 0.0001);
        assertEquals(1.0, weakDonor.poiseActionCoefficient(), 0.0001);
        assertTrue(weakDonor.cycleFinisher());

        assertEquals(weakDonor.finalDamage(), hugeDonor.finalDamage(), 0.0001);
        assertEquals(weakDonor.poiseDamage(), hugeDonor.poiseDamage(), 0.0001);
    }

    @Test
    void dualBladesSplitOneCanonicalCycleFiftyFiveFortyFive() {
        var build = new PlayerCombatBuildState(
                8,
                RootClass.WARRIOR,
                new AttributeAllocation(0, 0, 2, 5, 0, 0),
                EquipmentCombatState.weaponOnly(ProjectWeaponFamily.DUAL_BLADES, 8)
        );

        var first = CombatDamageAuthority.authorizeBetterCombatMelee(
                7.0F,
                0,
                build,
                EARTHLOONG
        );
        var second = CombatDamageAuthority.authorizeBetterCombatMelee(
                7.0F,
                1,
                build,
                EARTHLOONG
        );

        assertTrue(first.accepted());
        assertTrue(second.accepted());
        assertEquals(0.55 / 1.35, first.damageActionCoefficient(), 0.0001);
        assertEquals(0.45 / 1.35, second.damageActionCoefficient(), 0.0001);
        assertEquals(0.55, first.poiseActionCoefficient(), 0.0001);
        assertEquals(0.45, second.poiseActionCoefficient(), 0.0001);
        assertFalse(first.cycleFinisher());
        assertTrue(second.cycleFinisher());
        assertEquals(6.5, first.poiseDamage() + second.poiseDamage(), 0.0001);
    }

    @Test
    void crossbowFullShotUsesProjectCadenceAndIgnoresDonorDamageMagnitude() {
        var build = new PlayerCombatBuildState(
                8,
                RootClass.HUNTER,
                new AttributeAllocation(0, 0, 0, 7, 0, 0),
                EquipmentCombatState.weaponOnly(ProjectWeaponFamily.CROSSBOW, 8)
        );

        var weakDonor = CombatDamageAuthority.authorizeProjectileBasic(
                2.0F,
                build,
                EARTHLOONG
        );
        var hugeDonor = CombatDamageAuthority.authorizeProjectileBasic(
                9999.0F,
                build,
                EARTHLOONG
        );

        assertTrue(weakDonor.accepted());
        assertTrue(weakDonor.finalDamage() > 0.0);
        assertTrue(weakDonor.poiseDamage() > 0.0);
        assertEquals(1.0 / 0.72, weakDonor.damageActionCoefficient(), 0.0001);
        assertEquals(1.0, weakDonor.poiseActionCoefficient(), 0.0001);
        assertEquals(weakDonor.finalDamage(), hugeDonor.finalDamage(), 0.0001);
        assertEquals(weakDonor.poiseDamage(), hugeDonor.poiseDamage(), 0.0001);
    }

    @Test
    void rejectsInvalidDonorProposalOrNonMeleeProjectFamily() {
        var sword = new PlayerCombatBuildState(
                8,
                RootClass.WARRIOR,
                new AttributeAllocation(0, 0, 7, 0, 0, 0),
                EquipmentCombatState.weaponOnly(ProjectWeaponFamily.SWORD, 8)
        );
        var staff = new PlayerCombatBuildState(
                8,
                RootClass.MAGE,
                new AttributeAllocation(0, 0, 0, 0, 7, 0),
                EquipmentCombatState.weaponOnly(ProjectWeaponFamily.STAFF, 8)
        );

        assertFalse(CombatDamageAuthority.authorizeBetterCombatMelee(
                0.0F, 0, sword, EARTHLOONG
        ).accepted());
        assertFalse(CombatDamageAuthority.authorizeBetterCombatMelee(
                Float.NaN, 0, sword, EARTHLOONG
        ).accepted());
        assertFalse(CombatDamageAuthority.authorizeBetterCombatMelee(
                5.0F, -1, sword, EARTHLOONG
        ).accepted());
        assertFalse(CombatDamageAuthority.authorizeBetterCombatMelee(
                5.0F, 0, staff, EARTHLOONG
        ).accepted());
        assertFalse(CombatDamageAuthority.authorizeProjectileBasic(
                5.0F, staff, EARTHLOONG
        ).accepted());
        assertFalse(CombatDamageAuthority.authorizeProjectileBasic(
                Float.NaN, sword, EARTHLOONG
        ).accepted());
    }
}
