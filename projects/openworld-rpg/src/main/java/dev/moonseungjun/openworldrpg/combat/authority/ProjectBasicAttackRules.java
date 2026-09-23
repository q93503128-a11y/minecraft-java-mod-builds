package dev.moonseungjun.openworldrpg.combat.authority;

import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import java.util.Objects;

/**
 * Canonical basic-attack cycle budgeting from EQUIPMENT_BALANCE.md.
 *
 * <p>Better Combat owns animation/presentation and hit candidate timing. It does not own the RPG
 * damage budget. Single-hit melee families spend one whole basic-attack cycle per visible hit.
 * Dual blades spend one two-hit cycle split 55/45 across alternating Better Combat attack events.</p>
 */
public final class ProjectBasicAttackRules {
    private ProjectBasicAttackRules() {
    }

    public static boolean supportsBetterCombatMelee(ProjectWeaponFamily family) {
        Objects.requireNonNull(family, "family");
        return switch (family) {
            case DAGGER, DUAL_BLADES, SWORD, GREATSWORD, SPEAR, AXE, HAMMER_MACE -> true;
            default -> false;
        };
    }

    /**
     * Full-shot projectile basics that already have an unambiguous server-side release state.
     *
     * <p>Crossbow is admitted first because a fired bolt is already a committed full shot. Bow
     * damage remains fail-closed here until draw fraction is bound without trusting donor damage.</p>
     */
    public static boolean supportsAuthoritativeProjectileBasic(ProjectWeaponFamily family) {
        Objects.requireNonNull(family, "family");
        return family == ProjectWeaponFamily.CROSSBOW;
    }

    public static BasicHitProfile projectileFullShotProfile(ProjectWeaponFamily family) {
        Objects.requireNonNull(family, "family");
        if (!supportsAuthoritativeProjectileBasic(family)) {
            throw new IllegalArgumentException(
                    "Weapon family does not have an authoritative full-shot projectile profile: "
                            + family
            );
        }
        return new BasicHitProfile(
                1.0 / family.basicCadence(),
                1.0,
                true
        );
    }

    public static BasicHitProfile hitProfile(ProjectWeaponFamily family, int comboCount) {
        Objects.requireNonNull(family, "family");
        if (comboCount < 0) {
            throw new IllegalArgumentException("comboCount must be non-negative.");
        }
        if (!supportsBetterCombatMelee(family)) {
            throw new IllegalArgumentException(
                    "Weapon family is not a Better Combat melee-basic family: " + family
            );
        }

        if (family == ProjectWeaponFamily.DUAL_BLADES) {
            boolean secondHit = (comboCount & 1) == 1;
            double hitShare = secondHit ? 0.45 : 0.55;
            return new BasicHitProfile(
                    hitShare / family.basicCadence(),
                    hitShare,
                    secondHit
            );
        }

        return new BasicHitProfile(
                1.0 / family.basicCadence(),
                1.0,
                true
        );
    }

    public record BasicHitProfile(
            double damageActionCoefficient,
            double poiseActionCoefficient,
            boolean cycleFinisher
    ) {
        public BasicHitProfile {
            ProjectCombatRules.requireFinitePositive(
                    "damageActionCoefficient",
                    damageActionCoefficient
            );
            ProjectCombatRules.requireFinitePositive(
                    "poiseActionCoefficient",
                    poiseActionCoefficient
            );
        }
    }
}
