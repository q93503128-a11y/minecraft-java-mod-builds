package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.HunterPowerShotRules;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import org.junit.jupiter.api.Test;

class HunterPowerShotRulesTest {
    private static final double EPSILON = 0.0001;

    @Test
    void canonicalCoefficientsAndWeakPointMultipliersStayLocked() {
        assertEquals(3.00, HunterPowerShotRules.actionCoefficient(false), EPSILON);
        assertEquals(3.35, HunterPowerShotRules.actionCoefficient(true), EPSILON);
        assertEquals(1.0, HunterPowerShotRules.weakPointMultiplier(false, false), EPSILON);
        assertEquals(1.40, HunterPowerShotRules.weakPointMultiplier(false, true), EPSILON);
        assertEquals(1.55, HunterPowerShotRules.weakPointMultiplier(true, true), EPSILON);
        assertEquals(14, HunterPowerShotRules.WINDUP_TICKS);
    }

    @Test
    void currentProductionGateAllowsOnlyBowAndCrossbow() {
        assertTrue(
                HunterPowerShotRules.supportsCurrentProductionRangedWeapon(
                        ProjectWeaponFamily.BOW
                )
        );
        assertTrue(
                HunterPowerShotRules.supportsCurrentProductionRangedWeapon(
                        ProjectWeaponFamily.CROSSBOW
                )
        );
        assertFalse(
                HunterPowerShotRules.supportsCurrentProductionRangedWeapon(
                        ProjectWeaponFamily.SWORD
                )
        );
    }
}
