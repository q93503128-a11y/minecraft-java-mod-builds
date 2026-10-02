package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Canon-locked server combat contract for the non-boss R01 external creatures that are currently
 * registry/stat bound but still fail-closed for production spawning.
 *
 * <p>This data intentionally owns combat rules only. Donor entities remain presentation/movement
 * sources until an accepted runtime binder proves that their actual animation/contact surfaces
 * match these authored actions.</p>
 */
public record R01SecondaryCreatureEncounterData(
        String id,
        String entityId,
        CreatureKind kind,
        int contentLevel,
        List<ActionRule> actions,
        LivingShellRule livingShell
) {
    public R01SecondaryCreatureEncounterData {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(kind, "kind");
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
    }

    public Map<ActionId, ActionRule> rulesById() {
        EnumMap<ActionId, ActionRule> result = new EnumMap<>(ActionId.class);
        for (ActionRule rule : actions) {
            ActionRule previous = result.put(rule.id(), rule);
            if (previous != null) {
                throw new IllegalStateException("Duplicate R01 secondary-creature action: " + rule.id());
            }
        }
        return Map.copyOf(result);
    }

    public enum CreatureKind {
        CAVE_CENTIPEDE,
        NATURE_SPIRIT
    }

    public enum ActionId {
        SCUTTLE_BITE,
        BODY_RAKE,
        CEILING_DROP,
        ROOTED_SWIPE,
        EARTHEN_RAM,
        BLOOM_QUAKE
    }

    public record ActionRule(
            ActionId id,
            int weight,
            int cooldownTicks,
            int tellTicks,
            int activeTicks,
            int recoveryTicks,
            double minimumRange,
            double maximumRange,
            boolean requiresMeaningfullyAboveTarget,
            double committedMovementBlocks,
            double areaRadius,
            double benchmarkDamageShare,
            ProjectImpactTransaction.DamageSchool school,
            PlayerDefenseAuthority.GuardPressureBand guardPressure,
            boolean guardable,
            boolean perfectGuardable,
            double poisonBuildup,
            double playerPoisePressure
    ) {
        public ActionRule {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(school, "school");
            if (weight <= 0
                    || cooldownTicks < 0
                    || tellTicks < 0
                    || activeTicks < 0
                    || recoveryTicks < 0
                    || !Double.isFinite(minimumRange)
                    || !Double.isFinite(maximumRange)
                    || minimumRange < 0.0
                    || maximumRange < minimumRange
                    || !Double.isFinite(committedMovementBlocks)
                    || committedMovementBlocks < 0.0
                    || !Double.isFinite(areaRadius)
                    || areaRadius < 0.0
                    || !Double.isFinite(benchmarkDamageShare)
                    || benchmarkDamageShare <= 0.0
                    || benchmarkDamageShare > 1.0
                    || !Double.isFinite(poisonBuildup)
                    || poisonBuildup < 0.0
                    || !Double.isFinite(playerPoisePressure)
                    || playerPoisePressure < 0.0) {
                throw new IllegalArgumentException("Invalid R01 secondary-creature action rule: " + id);
            }
            boolean guardInteraction = guardable || perfectGuardable;
            if (guardInteraction != (guardPressure != null)) {
                throw new IllegalArgumentException(
                        "Guardable secondary-creature action requires guard pressure: " + id
                );
            }
        }

        public PlayerDefenseAuthority.IncomingHit toIncomingHit(int contentLevel) {
            double rawDamage = switch (school) {
                case PHYSICAL -> ProjectCombatRules.rawEnemyPhysicalDamageFromBenchmarkShare(
                        contentLevel,
                        benchmarkDamageShare
                );
                case MAGIC -> ProjectCombatRules.rawEnemyMagicDamageFromBenchmarkShare(
                        contentLevel,
                        benchmarkDamageShare
                );
            };
            if (!guardable && !perfectGuardable) {
                return PlayerDefenseAuthority.IncomingHit.unguardable(
                        rawDamage,
                        school,
                        contentLevel,
                        true
                );
            }
            return PlayerDefenseAuthority.IncomingHit.baseline(
                    rawDamage,
                    school,
                    contentLevel,
                    guardPressure,
                    true,
                    guardable,
                    perfectGuardable
            );
        }
    }

    public record LivingShellRule(
            int durationTicks,
            int reuseTicks,
            double directDamageTakenMultiplier,
            double poiseDamageTakenMultiplier,
            double recentDamageTriggerFraction,
            int recentDamageWindowTicks,
            double poiseTriggerFraction,
            double forcedBloomQuakeRange
    ) {
        public LivingShellRule {
            if (durationTicks <= 0
                    || reuseTicks <= 0
                    || recentDamageWindowTicks <= 0
                    || !Double.isFinite(directDamageTakenMultiplier)
                    || directDamageTakenMultiplier <= 0.0
                    || directDamageTakenMultiplier > 1.0
                    || !Double.isFinite(poiseDamageTakenMultiplier)
                    || poiseDamageTakenMultiplier < 1.0
                    || !Double.isFinite(recentDamageTriggerFraction)
                    || recentDamageTriggerFraction <= 0.0
                    || recentDamageTriggerFraction > 1.0
                    || !Double.isFinite(poiseTriggerFraction)
                    || poiseTriggerFraction < 0.0
                    || poiseTriggerFraction > 1.0
                    || !Double.isFinite(forcedBloomQuakeRange)
                    || forcedBloomQuakeRange <= 0.0) {
                throw new IllegalArgumentException("Invalid R01 Nature Spirit Living Shell rule.");
            }
        }
    }
}
