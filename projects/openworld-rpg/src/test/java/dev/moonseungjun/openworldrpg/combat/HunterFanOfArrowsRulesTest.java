package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.HunterFanOfArrowsRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import org.junit.jupiter.api.Test;

class HunterFanOfArrowsRulesTest {
    private static final double EPSILON = 0.0001;

    @Test
    void projectileCountsAndCapsMatchCanon() {
        assertEquals(5, HunterFanOfArrowsRules.projectileCount(false));
        assertEquals(7, HunterFanOfArrowsRules.projectileCount(true));
        assertEquals(2, HunterFanOfArrowsRules.PER_TARGET_HIT_CAP);
        assertEquals(
                ProjectSpellSpec.HUNTER_FAN_ACTION_COEFFICIENT_CAP,
                HunterFanOfArrowsRules.perProjectileActionCoefficient(false) * 2,
                EPSILON
        );
        assertEquals(
                ProjectSpellSpec.HUNTER_FAN_EMPOWERED_ACTION_COEFFICIENT_CAP,
                HunterFanOfArrowsRules.perProjectileActionCoefficient(true) * 2,
                EPSILON
        );
        assertEquals(
                ProjectSpellSpec.HUNTER_FAN_WHOLE_ACTION_POISE_COEFFICIENT,
                HunterFanOfArrowsRules.perProjectilePoiseCoefficient(false) * 5,
                EPSILON
        );
        assertEquals(
                ProjectSpellSpec.HUNTER_FAN_WHOLE_ACTION_POISE_COEFFICIENT,
                HunterFanOfArrowsRules.perProjectilePoiseCoefficient(true) * 7,
                EPSILON
        );
    }

    @Test
    void currentProductionGateAllowsOnlyBowAndCrossbow() {
        assertTrue(HunterFanOfArrowsRules.supportsCurrentProductionRangedWeapon(ProjectWeaponFamily.BOW));
        assertTrue(HunterFanOfArrowsRules.supportsCurrentProductionRangedWeapon(ProjectWeaponFamily.CROSSBOW));
        assertFalse(HunterFanOfArrowsRules.supportsCurrentProductionRangedWeapon(ProjectWeaponFamily.SWORD));
    }
}
