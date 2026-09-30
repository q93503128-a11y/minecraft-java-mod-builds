package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.runtime.HunterQuickstepVolleyRules;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import org.junit.jupiter.api.Test;

class HunterQuickstepVolleyRulesTest {
    @Test
    void canonicalSpellEconomyAndRootClassStayLocked() {
        var spec = ProjectSpellSpec.hunterQuickstepVolley();

        assertEquals(
                ProjectSpellSpec.HUNTER_QUICKSTEP_VOLLEY_ID,
                spec.id()
        );
        assertEquals(18.0, spec.manaCost(), 0.0001);
        assertEquals(0.0, spec.staminaCost(), 0.0001);
        assertEquals(140, spec.cooldownTicks());
        assertEquals(1.65, spec.actionCoefficient(), 0.0001);
        assertEquals(0.70, spec.poiseCoefficient(), 0.0001);
        assertEquals(
                RootClass.HUNTER,
                ProjectSpellSpec.requiredRootClass(spec.id())
                        .orElseThrow()
        );
    }

    @Test
    void ordinaryPlanHasThreeBlockDashAndNoHiddenIframes() {
        var plan = HunterQuickstepVolleyRules.plan(false);

        assertFalse(plan.empowered());
        assertEquals(3.0, plan.dashBlocks(), 0.0001);
        assertEquals(0, plan.iframeTicks());
        assertEquals(3, plan.projectileCount());
        assertEquals(
                0.55,
                plan.perProjectileActionCoefficient(),
                0.0001
        );
        assertEquals(
                1.65,
                plan.sameTargetActionCoefficientCap(),
                0.0001
        );
        assertEquals(
                0.70,
                plan.wholeActionPoiseCoefficient(),
                0.0001
        );
        assertEquals(0, plan.normalEnemyPiercesPerProjectile());
    }

    @Test
    void empoweredPlanChangesOnlyAuthoredMobilityAndPierceRules() {
        var plan = HunterQuickstepVolleyRules.plan(true);

        assertTrue(plan.empowered());
        assertEquals(4.0, plan.dashBlocks(), 0.0001);
        assertEquals(0, plan.iframeTicks());
        assertEquals(3, plan.projectileCount());
        assertEquals(
                1,
                plan.normalEnemyPiercesPerProjectile()
        );
        assertEquals(
                1.65,
                plan.sameTargetActionCoefficientCap(),
                0.0001
        );
    }

    @Test
    void sameTargetBudgetCannotExceedThreeProjectileCap() {
        assertEquals(
                0.0,
                HunterQuickstepVolleyRules
                        .sameTargetActionCoefficient(0),
                0.0001
        );
        assertEquals(
                0.55,
                HunterQuickstepVolleyRules
                        .sameTargetActionCoefficient(1),
                0.0001
        );
        assertEquals(
                1.10,
                HunterQuickstepVolleyRules
                        .sameTargetActionCoefficient(2),
                0.0001
        );
        assertEquals(
                1.65,
                HunterQuickstepVolleyRules
                        .sameTargetActionCoefficient(3),
                0.0001
        );
        assertEquals(
                1.65,
                HunterQuickstepVolleyRules
                        .sameTargetActionCoefficient(99),
                0.0001
        );
    }

    @Test
    void currentProductionRangedGateIsBowAndCrossbowOnly() {
        assertTrue(
                HunterQuickstepVolleyRules
                        .supportsCurrentProductionRangedWeapon(
                                ProjectWeaponFamily.BOW
                        )
        );
        assertTrue(
                HunterQuickstepVolleyRules
                        .supportsCurrentProductionRangedWeapon(
                                ProjectWeaponFamily.CROSSBOW
                        )
        );
        assertFalse(
                HunterQuickstepVolleyRules
                        .supportsCurrentProductionRangedWeapon(
                                ProjectWeaponFamily.BLACK_POWDER_PISTOL
                        )
        );
        assertFalse(
                HunterQuickstepVolleyRules
                        .supportsCurrentProductionRangedWeapon(
                                ProjectWeaponFamily.SWORD
                        )
        );
    }
}
