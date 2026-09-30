package dev.moonseungjun.openworldrpg.combat.authority;

import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import java.util.Objects;

public final class HunterPowerShotRules {
    public static final int WINDUP_TICKS = 14;
    public static final double PROJECTILE_VELOCITY = 3.20;

    private HunterPowerShotRules() {}

    public static boolean supportsCurrentProductionRangedWeapon(ProjectWeaponFamily family) {
        Objects.requireNonNull(family, "family");
        return family == ProjectWeaponFamily.BOW || family == ProjectWeaponFamily.CROSSBOW;
    }

    public static double actionCoefficient(boolean empowered) {
        return empowered
                ? ProjectSpellSpec.HUNTER_POWER_SHOT_EMPOWERED_ACTION_COEFFICIENT
                : ProjectSpellSpec.HUNTER_POWER_SHOT_ACTION_COEFFICIENT;
    }

    public static double weakPointMultiplier(boolean empowered, boolean authoredWeakPointHit) {
        if (!authoredWeakPointHit) return 1.0;
        return empowered
                ? ProjectSpellSpec.HUNTER_POWER_SHOT_EMPOWERED_WEAK_POINT_MULTIPLIER
                : ProjectSpellSpec.HUNTER_POWER_SHOT_WEAK_POINT_MULTIPLIER;
    }
}
