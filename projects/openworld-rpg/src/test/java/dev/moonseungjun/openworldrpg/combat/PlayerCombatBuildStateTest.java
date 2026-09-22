package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.state.AttributeAllocation;
import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatBuildState;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import org.junit.jupiter.api.Test;

class PlayerCombatBuildStateTest {
    @Test
    void validLv8MageStaffBuildProducesCanonicalSourceSnapshot() {
        var build = new PlayerCombatBuildState(
                8,
                RootClass.MAGE,
                new AttributeAllocation(0, 0, 0, 0, 7, 0),
                EquipmentCombatState.weaponOnly(ProjectWeaponFamily.STAFF, 8)
        );

        var source = build.damageSource(ProjectImpactTransaction.DamageSchool.MAGIC);

        assertEquals(8, source.contentLevel());
        assertEquals(30.0, source.weaponPower(), 0.0001);
        assertEquals(10.95, source.weightedOffensiveStat(), 0.0001);
        assertEquals(0.0, source.additivePowerBonus(), 0.0001);
        assertEquals(0.85, source.poiseOutputMultiplier(), 0.0001);
    }

    @Test
    void equipmentFlatStatsApplyAfterCanonicalClassAllocation() {
        var equipment = new EquipmentCombatState(
                ProjectWeaponFamily.STAFF,
                8,
                new dev.moonseungjun.openworldrpg.combat.state.EffectiveAttributes(
                        0, 0, 0, 0, 3, 2
                ),
                0.0,
                0.075,
                0.10,
                0.20,
                0.0
        );
        var build = new PlayerCombatBuildState(
                8,
                RootClass.MAGE,
                new AttributeAllocation(0, 0, 0, 0, 7, 0),
                equipment
        );

        var effective = build.effectiveAttributes();
        var source = build.damageSource(ProjectImpactTransaction.DamageSchool.MAGIC);

        assertEquals(15.0, effective.intel(), 0.0001);
        assertEquals(7.0, effective.wil(), 0.0001);
        assertEquals(13.8, source.weightedOffensiveStat(), 0.0001);
        assertEquals(0.175, source.additivePowerBonus(), 0.0001);
        assertEquals(1.02, source.poiseOutputMultiplier(), 0.0001);
    }

    @Test
    void canonicalMaxHealthFormulaMatchesLockedDesignAnchors() {
        assertEquals(100, ProjectCombatRules.maxPlayerHealth(1, 5, 0.0));
        assertEquals(143, ProjectCombatRules.maxPlayerHealth(8, 7, 0.0));
        assertEquals(223, ProjectCombatRules.maxPlayerHealth(20, 10, 0.0));
        assertEquals(403, ProjectCombatRules.maxPlayerHealth(44, 16, 0.0));
        assertEquals(575, ProjectCombatRules.maxPlayerHealth(64, 21, 0.0));
        assertEquals(727, ProjectCombatRules.maxPlayerHealth(80, 25, 0.0));
        assertEquals(935, ProjectCombatRules.maxPlayerHealth(80, 60, 0.0));
    }

    @Test
    void buildMaxHealthUsesEffectiveVitIncludingEquipment() {
        var equipment = new EquipmentCombatState(
                ProjectWeaponFamily.SWORD,
                8,
                new dev.moonseungjun.openworldrpg.combat.state.EffectiveAttributes(
                        3, 0, 0, 0, 0, 0
                ),
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );
        var build = new PlayerCombatBuildState(
                8,
                RootClass.WARRIOR,
                new AttributeAllocation(2, 0, 5, 0, 0, 0),
                equipment
        );

        assertEquals(150, build.maxHealth());
    }

    @Test
    void attributeOverspendFailsClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerCombatBuildState(
                        8,
                        RootClass.MAGE,
                        new AttributeAllocation(0, 0, 0, 0, 8, 0),
                        EquipmentCombatState.weaponOnly(ProjectWeaponFamily.STAFF, 8)
                )
        );
    }

    @Test
    void preEquipmentAttributeCapIsEnforced() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AttributeAllocation(0, 0, 0, 0, 56, 0)
        );
    }
}
