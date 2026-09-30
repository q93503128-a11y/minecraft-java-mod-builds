package dev.moonseungjun.openworldrpg.combat.authority;

import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import java.util.Objects;

public final class HunterFanOfArrowsRules {
    public static final int BASE_PROJECTILE_COUNT = 5;
    public static final int EMPOWERED_PROJECTILE_COUNT = 7;
    public static final int PER_TARGET_HIT_CAP = 2;

    private HunterFanOfArrowsRules() {}

    public static boolean supportsCurrentProductionRangedWeapon(ProjectWeaponFamily family) {
        Objects.requireNonNull(family, "family");
        return family == ProjectWeaponFamily.BOW || family == ProjectWeaponFamily.CROSSBOW;
    }

    public static int projectileCount(boolean empowered) {
        return empowered ? EMPOWERED_PROJECTILE_COUNT : BASE_PROJECTILE_COUNT;
    }

    public static double perProjectileActionCoefficient(boolean empowered) {
        double cap = empowered
                ? ProjectSpellSpec.HUNTER_FAN_EMPOWERED_ACTION_COEFFICIENT_CAP
                : ProjectSpellSpec.HUNTER_FAN_ACTION_COEFFICIENT_CAP;
        return cap / PER_TARGET_HIT_CAP;
    }

    public static double perProjectilePoiseCoefficient(boolean empowered) {
        return ProjectSpellSpec.HUNTER_FAN_WHOLE_ACTION_POISE_COEFFICIENT
                / projectileCount(empowered);
    }
}
