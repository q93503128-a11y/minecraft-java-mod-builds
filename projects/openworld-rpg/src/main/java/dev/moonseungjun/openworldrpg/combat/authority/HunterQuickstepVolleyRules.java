package dev.moonseungjun.openworldrpg.combat.authority;

import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import java.util.Objects;

/**
 * Canonical backend contract for Hunter Quickstep Volley.
 *
 * <p>This class intentionally owns only gameplay facts that are already closed in
 * {@code CLASS_COMBAT_KITS.md}. The player-facing spell stays unpublished until an accepted
 * movement animation/projectile presentation is bound. Server movement and hit resolution must
 * consume this contract instead of deriving gameplay from animation/root-motion.</p>
 */
public final class HunterQuickstepVolleyRules {
    public static final double BASE_DASH_BLOCKS = 3.0;
    public static final double EMPOWERED_DASH_BLOCKS = 4.0;
    public static final int IFRAME_TICKS = 0;
    public static final int PROJECTILE_COUNT = 3;
    public static final double PER_PROJECTILE_ACTION_COEFFICIENT = 0.55;
    public static final double SAME_TARGET_ACTION_COEFFICIENT_CAP = 1.65;
    public static final double WHOLE_ACTION_POISE_COEFFICIENT = 0.70;
    public static final int EMPOWERED_NORMAL_ENEMY_PIERCE_PER_PROJECTILE = 1;

    private HunterQuickstepVolleyRules() {
    }

    public static boolean supportsCurrentProductionRangedWeapon(
            ProjectWeaponFamily family
    ) {
        Objects.requireNonNull(family, "family");
        return family == ProjectWeaponFamily.BOW
                || family == ProjectWeaponFamily.CROSSBOW;
    }

    public static CastPlan plan(boolean empowered) {
        return new CastPlan(
                empowered,
                empowered
                        ? EMPOWERED_DASH_BLOCKS
                        : BASE_DASH_BLOCKS,
                IFRAME_TICKS,
                PROJECTILE_COUNT,
                PER_PROJECTILE_ACTION_COEFFICIENT,
                SAME_TARGET_ACTION_COEFFICIENT_CAP,
                WHOLE_ACTION_POISE_COEFFICIENT,
                empowered
                        ? EMPOWERED_NORMAL_ENEMY_PIERCE_PER_PROJECTILE
                        : 0
        );
    }

    public static double sameTargetActionCoefficient(int landedProjectiles) {
        if (landedProjectiles < 0) {
            throw new IllegalArgumentException(
                    "Quickstep Volley landed-projectile count cannot be negative."
            );
        }
        return Math.min(
                SAME_TARGET_ACTION_COEFFICIENT_CAP,
                Math.min(PROJECTILE_COUNT, landedProjectiles)
                        * PER_PROJECTILE_ACTION_COEFFICIENT
        );
    }

    public record CastPlan(
            boolean empowered,
            double dashBlocks,
            int iframeTicks,
            int projectileCount,
            double perProjectileActionCoefficient,
            double sameTargetActionCoefficientCap,
            double wholeActionPoiseCoefficient,
            int normalEnemyPiercesPerProjectile
    ) {
        public CastPlan {
            if (!Double.isFinite(dashBlocks)
                    || dashBlocks <= 0.0
                    || iframeTicks != 0
                    || projectileCount <= 0
                    || !Double.isFinite(perProjectileActionCoefficient)
                    || perProjectileActionCoefficient <= 0.0
                    || !Double.isFinite(sameTargetActionCoefficientCap)
                    || sameTargetActionCoefficientCap <= 0.0
                    || !Double.isFinite(wholeActionPoiseCoefficient)
                    || wholeActionPoiseCoefficient < 0.0
                    || normalEnemyPiercesPerProjectile < 0) {
                throw new IllegalArgumentException(
                        "Invalid Quickstep Volley cast plan."
                );
            }
        }
    }
}
