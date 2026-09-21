package dev.moonseungjun.openworldrpg.combat.authority;

import java.util.Objects;
import java.util.Optional;

/**
 * Pure server-authority transaction math for project-owned damage, healing, status merge and poise pressure.
 * Runtime integrations provide explicit snapshots; donor combat stats never become authority implicitly.
 */
public final class ProjectImpactTransaction {
    private ProjectImpactTransaction() {
    }

    public static DirectDamageResult resolveDirectDamage(DirectDamageRequest request) {
        Objects.requireNonNull(request, "request");
        DamageSourceSnapshot source = request.source();
        DamageTargetSnapshot target = request.target();

        double statMultiplier = ProjectCombatRules.attributeDamageMultiplier(source.weightedOffensiveStat());
        double rawActionDamage = source.weaponPower() * request.actionCoefficient() * statMultiplier;
        double additiveBucket = 1.0 + source.additivePowerBonus();
        double preMitigationDamage = rawActionDamage
                * additiveBucket
                * request.weakPointMultiplier()
                * request.criticalMultiplier();

        double effectiveDefense = request.school() == DamageSchool.PHYSICAL
                ? target.defense()
                : target.magicResistance();
        double armorTakenMultiplier = ProjectCombatRules.defenseTakenMultiplier(
                source.contentLevel(),
                effectiveDefense
        );
        double totalTakenMultiplier = ProjectCombatRules.routineTakenMultiplier(
                armorTakenMultiplier,
                target.authoredDamageTakenMultiplier(),
                target.authoredDamageReduction()
        );
        double finalDamage = ProjectCombatRules.roundFinal(preMitigationDamage * totalTakenMultiplier);

        return new DirectDamageResult(
                rawActionDamage,
                preMitigationDamage,
                armorTakenMultiplier,
                totalTakenMultiplier,
                finalDamage
        );
    }

    public static HealingResult resolveHealing(HealingRequest request) {
        Objects.requireNonNull(request, "request");

        double healingStatMultiplier = ProjectCombatRules.attributeDamageMultiplier(request.healingWeightedStat());
        double healingReference = ProjectCombatRules.baseHp(request.casterLevel()) * healingStatMultiplier;
        double rawHeal = ProjectCombatRules.roundFinal(
                healingReference
                        * request.healCoefficient()
                        * (1.0 + request.healingDoneBonus())
                        * (1.0 + request.targetHealingReceivedBonus())
        );
        double effectiveHeal = Math.min(rawHeal, request.targetMissingHealth());
        return new HealingResult(rawHeal, effectiveHeal, Math.max(0.0, rawHeal - effectiveHeal));
    }

    public static PoiseResult resolvePoise(PoiseRequest request) {
        Objects.requireNonNull(request, "request");

        double poiseDamage = 10.0
                * request.sourcePoiseOutputMultiplier()
                * request.actionPoiseCoefficient()
                * request.deliveryMultiplier()
                * request.targetPoiseTakenMultiplier();
        double remaining = Math.max(0.0, request.currentPoise() - poiseDamage);
        boolean broken = request.currentPoise() > 0.0 && remaining <= 0.0;
        return new PoiseResult(poiseDamage, remaining, broken);
    }

    public static StatusResolution resolveStatus(
            Optional<StatusState> current,
            StatusApplicationRequest request
    ) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(request, "request");

        if (current.isEmpty()) {
            return new StatusResolution(
                    new StatusState(request.statusId(), request.sourceKey(), request.magnitude(), request.durationTicks()),
                    true,
                    true
            );
        }

        StatusState existing = current.get();
        if (!existing.statusId().equals(request.statusId())) {
            throw new IllegalArgumentException(
                    "Status transaction received a state for a different status id: "
                            + existing.statusId() + " != " + request.statusId()
            );
        }

        return switch (request.mergeRule()) {
            case STRONGEST_MAGNITUDE_REFRESH -> {
                double magnitude = Math.max(existing.magnitude(), request.magnitude());
                yield new StatusResolution(
                        new StatusState(request.statusId(), existing.sourceKey(), magnitude, request.durationTicks()),
                        magnitude != existing.magnitude(),
                        true
                );
            }
            case REPLACE_REFRESH -> new StatusResolution(
                    new StatusState(
                            request.statusId(),
                            request.sourceKey(),
                            request.magnitude(),
                            request.durationTicks()
                    ),
                    request.magnitude() != existing.magnitude()
                            || !request.sourceKey().equals(existing.sourceKey()),
                    true
            );
        };
    }

    public enum DamageSchool {
        PHYSICAL,
        MAGIC
    }

    public enum StatusMergeRule {
        STRONGEST_MAGNITUDE_REFRESH,
        REPLACE_REFRESH
    }

    public record DamageSourceSnapshot(
            int contentLevel,
            double weaponPower,
            double weightedOffensiveStat,
            double additivePowerBonus,
            double poiseOutputMultiplier
    ) {
        public DamageSourceSnapshot {
            if (contentLevel < ProjectCombatRules.MIN_CONTENT_LEVEL
                    || contentLevel > ProjectCombatRules.MAX_CONTENT_LEVEL) {
                throw new IllegalArgumentException("contentLevel must be inside [1, 80].");
            }
            ProjectCombatRules.requireFiniteNonNegative("weaponPower", weaponPower);
            ProjectCombatRules.requireFinite("weightedOffensiveStat", weightedOffensiveStat);
            ProjectCombatRules.requireFinite("additivePowerBonus", additivePowerBonus);
            ProjectCombatRules.requireFiniteNonNegative("poiseOutputMultiplier", poiseOutputMultiplier);
            if (1.0 + additivePowerBonus <= 0.0) {
                throw new IllegalArgumentException("Additive power bucket must remain positive.");
            }
        }
    }

    public record DamageTargetSnapshot(
            double defense,
            double magicResistance,
            double authoredDamageTakenMultiplier,
            double authoredDamageReduction,
            double poiseMax
    ) {
        public DamageTargetSnapshot {
            ProjectCombatRules.requireFiniteNonNegative("defense", defense);
            ProjectCombatRules.requireFiniteNonNegative("magicResistance", magicResistance);
            ProjectCombatRules.requireFinitePositive("authoredDamageTakenMultiplier", authoredDamageTakenMultiplier);
            ProjectCombatRules.requireFinite("authoredDamageReduction", authoredDamageReduction);
            ProjectCombatRules.requireFiniteNonNegative("poiseMax", poiseMax);
            if (authoredDamageReduction < 0.0 || authoredDamageReduction >= 1.0) {
                throw new IllegalArgumentException("authoredDamageReduction must be inside [0, 1).");
            }
        }
    }

    public record DirectDamageRequest(
            DamageSourceSnapshot source,
            DamageTargetSnapshot target,
            DamageSchool school,
            double actionCoefficient,
            double weakPointMultiplier,
            double criticalMultiplier
    ) {
        public DirectDamageRequest {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(school, "school");
            ProjectCombatRules.requireFiniteNonNegative("actionCoefficient", actionCoefficient);
            ProjectCombatRules.requireFinitePositive("weakPointMultiplier", weakPointMultiplier);
            ProjectCombatRules.requireFinitePositive("criticalMultiplier", criticalMultiplier);
        }
    }

    public record DirectDamageResult(
            double rawActionDamage,
            double preMitigationDamage,
            double armorTakenMultiplier,
            double totalTakenMultiplier,
            double finalDamage
    ) {
    }

    public record HealingRequest(
            int casterLevel,
            double healingWeightedStat,
            double healCoefficient,
            double healingDoneBonus,
            double targetHealingReceivedBonus,
            double targetMissingHealth
    ) {
        public HealingRequest {
            if (casterLevel < ProjectCombatRules.MIN_CONTENT_LEVEL
                    || casterLevel > ProjectCombatRules.MAX_CONTENT_LEVEL) {
                throw new IllegalArgumentException("casterLevel must be inside [1, 80].");
            }
            ProjectCombatRules.requireFinite("healingWeightedStat", healingWeightedStat);
            ProjectCombatRules.requireFiniteNonNegative("healCoefficient", healCoefficient);
            ProjectCombatRules.requireFinite("healingDoneBonus", healingDoneBonus);
            ProjectCombatRules.requireFinite("targetHealingReceivedBonus", targetHealingReceivedBonus);
            ProjectCombatRules.requireFiniteNonNegative("targetMissingHealth", targetMissingHealth);
            if (1.0 + healingDoneBonus < 0.0 || 1.0 + targetHealingReceivedBonus < 0.0) {
                throw new IllegalArgumentException("Healing bonus multipliers cannot be negative.");
            }
        }
    }

    public record HealingResult(double rawHeal, double effectiveHeal, double overheal) {
    }

    public record PoiseRequest(
            double currentPoise,
            double maxPoise,
            double sourcePoiseOutputMultiplier,
            double actionPoiseCoefficient,
            double deliveryMultiplier,
            double targetPoiseTakenMultiplier
    ) {
        public PoiseRequest {
            ProjectCombatRules.requireFiniteNonNegative("currentPoise", currentPoise);
            ProjectCombatRules.requireFiniteNonNegative("maxPoise", maxPoise);
            ProjectCombatRules.requireFiniteNonNegative("sourcePoiseOutputMultiplier", sourcePoiseOutputMultiplier);
            ProjectCombatRules.requireFiniteNonNegative("actionPoiseCoefficient", actionPoiseCoefficient);
            ProjectCombatRules.requireFiniteNonNegative("deliveryMultiplier", deliveryMultiplier);
            ProjectCombatRules.requireFiniteNonNegative("targetPoiseTakenMultiplier", targetPoiseTakenMultiplier);
            if (currentPoise > maxPoise) {
                throw new IllegalArgumentException("currentPoise cannot exceed maxPoise.");
            }
        }
    }

    public record PoiseResult(double poiseDamage, double remainingPoise, boolean broken) {
    }

    public record StatusState(
            String statusId,
            String sourceKey,
            double magnitude,
            int remainingTicks
    ) {
        public StatusState {
            requireStatusIdentity(statusId, sourceKey);
            ProjectCombatRules.requireFiniteNonNegative("magnitude", magnitude);
            if (remainingTicks < 0) {
                throw new IllegalArgumentException("remainingTicks must be non-negative.");
            }
        }
    }

    public record StatusApplicationRequest(
            String statusId,
            String sourceKey,
            double magnitude,
            int durationTicks,
            StatusMergeRule mergeRule
    ) {
        public StatusApplicationRequest {
            requireStatusIdentity(statusId, sourceKey);
            ProjectCombatRules.requireFiniteNonNegative("magnitude", magnitude);
            if (durationTicks <= 0) {
                throw new IllegalArgumentException("durationTicks must be positive.");
            }
            Objects.requireNonNull(mergeRule, "mergeRule");
        }
    }

    public record StatusResolution(StatusState state, boolean magnitudeChanged, boolean durationRefreshed) {
        public StatusResolution {
            Objects.requireNonNull(state, "state");
        }
    }

    private static void requireStatusIdentity(String statusId, String sourceKey) {
        Objects.requireNonNull(statusId, "statusId");
        Objects.requireNonNull(sourceKey, "sourceKey");
        if (statusId.isBlank() || sourceKey.isBlank()) {
            throw new IllegalArgumentException("statusId/sourceKey must be non-blank.");
        }
    }
}
