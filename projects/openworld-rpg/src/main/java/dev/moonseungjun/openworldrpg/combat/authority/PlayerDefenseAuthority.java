package dev.moonseungjun.openworldrpg.combat.authority;

import java.util.Objects;
import java.util.Optional;

/**
 * Canonical player-side defense math.
 *
 * <p>This class owns the numeric contract only. External combat/animation mods may provide
 * presentation or normalized hit candidates, but they do not decide dodge invulnerability,
 * guard absorption, Stamina pressure, Defense/MR mitigation, or guard break.</p>
 */
public final class PlayerDefenseAuthority {
    private PlayerDefenseAuthority() {
    }

    public enum GuardType {
        WEAPON(0.55, 0.35),
        BUCKLER(0.70, 0.45),
        STANDARD_SHIELD(0.85, 0.60),
        HEAVY_SHIELD(0.95, 0.70);

        private final double physicalAbsorption;
        private final double magicAbsorption;

        GuardType(double physicalAbsorption, double magicAbsorption) {
            this.physicalAbsorption = physicalAbsorption;
            this.magicAbsorption = magicAbsorption;
        }

        public double absorption(ProjectImpactTransaction.DamageSchool school) {
            Objects.requireNonNull(school, "school");
            return switch (school) {
                case PHYSICAL -> physicalAbsorption;
                case MAGIC -> magicAbsorption;
            };
        }
    }

    public enum GuardPressureBand {
        LIGHT(24.0),
        MEDIUM(36.0),
        HEAVY(54.0),
        CRUSH(78.0);

        private final double basePressure;

        GuardPressureBand(double basePressure) {
            this.basePressure = basePressure;
        }

        public double basePressure() {
            return basePressure;
        }
    }

    public record DefenseSnapshot(
            double defense,
            double magicResistance,
            double guardRating,
            Optional<GuardType> guardType
    ) {
        public DefenseSnapshot {
            requireFiniteNonNegative("defense", defense);
            requireFiniteNonNegative("magicResistance", magicResistance);
            requireFiniteNonNegative("guardRating", guardRating);
            Objects.requireNonNull(guardType, "guardType");
            if (guardType.isEmpty() && guardRating > 0.0) {
                throw new IllegalArgumentException(
                        "guardRating requires an equipped/validated guard type."
                );
            }
        }

        public static DefenseSnapshot unguarded(
                double defense,
                double magicResistance
        ) {
            return new DefenseSnapshot(defense, magicResistance, 0.0, Optional.empty());
        }

        public static DefenseSnapshot guarded(
                double defense,
                double magicResistance,
                double guardRating,
                GuardType guardType
        ) {
            return new DefenseSnapshot(
                    defense,
                    magicResistance,
                    guardRating,
                    Optional.of(Objects.requireNonNull(guardType, "guardType"))
            );
        }
    }

    public record IncomingHit(
            double rawDamage,
            ProjectImpactTransaction.DamageSchool school,
            int attackerLevel,
            GuardPressureBand guardPressure,
            boolean dodgeable,
            boolean guardable,
            boolean perfectGuardable,
            double authoredDamageTakenMultiplier,
            double authoredDamageReduction
    ) {
        public IncomingHit {
            requireFiniteNonNegative("rawDamage", rawDamage);
            Objects.requireNonNull(school, "school");
            Objects.requireNonNull(guardPressure, "guardPressure");
            ProjectCombatRules.gearScale(attackerLevel);
            if (perfectGuardable && !guardable) {
                throw new IllegalArgumentException(
                        "A perfect-guardable hit must also be guardable."
                );
            }
            requireFinitePositive(
                    "authoredDamageTakenMultiplier",
                    authoredDamageTakenMultiplier
            );
            if (!Double.isFinite(authoredDamageReduction)
                    || authoredDamageReduction < 0.0
                    || authoredDamageReduction >= 1.0) {
                throw new IllegalArgumentException(
                        "authoredDamageReduction must be inside [0, 1)."
                );
            }
        }

        public static IncomingHit baseline(
                double rawDamage,
                ProjectImpactTransaction.DamageSchool school,
                int attackerLevel,
                GuardPressureBand guardPressure,
                boolean dodgeable,
                boolean guardable,
                boolean perfectGuardable
        ) {
            return new IncomingHit(
                    rawDamage,
                    school,
                    attackerLevel,
                    guardPressure,
                    dodgeable,
                    guardable,
                    perfectGuardable,
                    1.0,
                    0.0
            );
        }
    }

    public static double mitigatedDamageBeforeActiveDefense(
            IncomingHit hit,
            DefenseSnapshot defense
    ) {
        Objects.requireNonNull(hit, "hit");
        Objects.requireNonNull(defense, "defense");

        double defensiveStat = switch (hit.school()) {
            case PHYSICAL -> defense.defense();
            case MAGIC -> defense.magicResistance();
        };
        double armorTaken = ProjectCombatRules.defenseTakenMultiplier(
                hit.attackerLevel(),
                defensiveStat
        );
        double routineTaken = ProjectCombatRules.routineTakenMultiplier(
                armorTaken,
                hit.authoredDamageTakenMultiplier(),
                hit.authoredDamageReduction()
        );
        return hit.rawDamage() * routineTaken;
    }

    public static double guardStaminaCost(
            GuardPressureBand band,
            double guardRating,
            int endurance,
            int attackerLevel
    ) {
        Objects.requireNonNull(band, "band");
        requireFiniteNonNegative("guardRating", guardRating);
        if (endurance < 0) {
            throw new IllegalArgumentException("END must be non-negative.");
        }

        double scale = guardRating
                / (guardRating + 12.0 * ProjectCombatRules.gearScale(attackerLevel));
        double endBonus = Math.min(0.15, Math.max(0, endurance - 5) * 0.002);
        return Math.max(
                3.0,
                band.basePressure() * (1.0 - scale) * (1.0 - endBonus)
        );
    }

    public static double shieldGuardRating(int itemLevel, GuardType guardType) {
        Objects.requireNonNull(guardType, "guardType");
        double familyFactor = switch (guardType) {
            case BUCKLER -> 0.80;
            case STANDARD_SHIELD -> 1.00;
            case HEAVY_SHIELD -> 1.25;
            case WEAPON -> throw new IllegalArgumentException(
                    "Weapon guard rating is weapon-data-owned, not a shield-family formula."
            );
        };
        return Math.round(
                20.0 * ProjectCombatRules.gearScale(itemLevel) * familyFactor
        );
    }

    public static double guardAbsorption(
            GuardType guardType,
            ProjectImpactTransaction.DamageSchool school
    ) {
        return Objects.requireNonNull(guardType, "guardType").absorption(school);
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative.");
        }
    }

    private static void requireFinitePositive(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and positive.");
        }
    }
}
