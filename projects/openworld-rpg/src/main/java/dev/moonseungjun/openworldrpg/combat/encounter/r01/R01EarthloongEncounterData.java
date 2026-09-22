package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record R01EarthloongEncounterData(
        String id,
        int contentLevel,
        int decisionDelayTicks,
        double phaseTwoHealthThreshold,
        LightningFurrowPattern lightningFurrow,
        List<ActionRule> actions,
        List<ImpactRule> impacts,
        List<PhysicalBindingRule> physicalBindings,
        List<SpaceControlBindingRule> spaceControlBindings
) {
    public R01EarthloongEncounterData {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(lightningFurrow, "lightningFurrow");
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        impacts = List.copyOf(Objects.requireNonNull(impacts, "impacts"));
        physicalBindings = List.copyOf(
                Objects.requireNonNull(physicalBindings, "physicalBindings")
        );
        spaceControlBindings = List.copyOf(
                Objects.requireNonNull(spaceControlBindings, "spaceControlBindings")
        );
    }

    public Map<ActionId, ActionRule> rulesById() {
        EnumMap<ActionId, ActionRule> result = new EnumMap<>(ActionId.class);
        for (ActionRule action : actions) {
            ActionRule previous = result.put(action.id(), action);
            if (previous != null) {
                throw new IllegalStateException("Duplicate Earthloong action rule: " + action.id());
            }
        }
        return Map.copyOf(result);
    }

    public Map<ActionId, ImpactRule> impactRulesById() {
        EnumMap<ActionId, ImpactRule> result = new EnumMap<>(ActionId.class);
        for (ImpactRule impact : impacts) {
            ImpactRule previous = result.put(impact.action(), impact);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate Earthloong impact rule: " + impact.action()
                );
            }
        }
        return Map.copyOf(result);
    }

    public Map<ActionId, SpaceControlBindingRule> spaceControlBindingsById() {
        EnumMap<ActionId, SpaceControlBindingRule> result = new EnumMap<>(ActionId.class);
        for (SpaceControlBindingRule binding : spaceControlBindings) {
            SpaceControlBindingRule previous = result.put(binding.action(), binding);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate Earthloong space-control binding: " + binding.action()
                );
            }
        }
        return Map.copyOf(result);
    }

    public Map<ActionId, PhysicalBindingRule> physicalBindingsById() {
        EnumMap<ActionId, PhysicalBindingRule> result = new EnumMap<>(ActionId.class);
        for (PhysicalBindingRule binding : physicalBindings) {
            PhysicalBindingRule previous = result.put(binding.action(), binding);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate Earthloong physical binding: " + binding.action()
                );
            }
        }
        return Map.copyOf(result);
    }

    public enum ActionId {
        CLAW_SWEEP,
        TAIL_SCYTHE,
        QUARRY_RUSH,
        LIGHTNING_FURROW,
        ROOT_BREAKER,
        FORKED_HEAVEN,
        EARTHLINE_SURGE
    }

    public enum Phase {
        ONE(1),
        TWO(2);

        private final int number;

        Phase(int number) {
            this.number = number;
        }

        public int number() {
            return number;
        }
    }

    public record LightningFurrowPattern(
            int phaseOneLaneCount,
            int phaseTwoFirstLaneCount,
            List<Integer> phaseTwoAlternatingLaneCounts
    ) {
        public LightningFurrowPattern {
            phaseTwoAlternatingLaneCounts = List.copyOf(
                    Objects.requireNonNull(
                            phaseTwoAlternatingLaneCounts,
                            "phaseTwoAlternatingLaneCounts"
                    )
            );
        }
    }

    public record ActionRule(
            ActionId id,
            int minimumPhase,
            int weight,
            int cooldownTicks,
            boolean spaceControl
    ) {
        public ActionRule {
            Objects.requireNonNull(id, "id");
        }
    }

    public record ImpactRule(
            ActionId action,
            double benchmarkDamageShare,
            dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction.DamageSchool school,
            dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority.GuardPressureBand guardPressure,
            boolean guardable,
            boolean perfectGuardable
    ) {
        public ImpactRule {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(school, "school");
            if ((guardable || perfectGuardable) && guardPressure == null) {
                throw new IllegalArgumentException(
                        "Guardable Earthloong impact requires guard pressure: " + action
                );
            }
        }

        public dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority.IncomingHit
        toIncomingHit(int contentLevel) {
            if (school
                    != dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction
                    .DamageSchool.PHYSICAL) {
                throw new IllegalStateException(
                        "Current Earthloong impact rule has no closed raw-damage authoring bridge: "
                                + action
                );
            }
            double rawDamage = dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules
                    .rawEnemyPhysicalDamageFromBenchmarkShare(
                            contentLevel,
                            benchmarkDamageShare
                    );
            if (!guardable && !perfectGuardable) {
                return dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority
                        .IncomingHit.unguardable(
                                rawDamage,
                                school,
                                contentLevel,
                                true
                        );
            }
            return dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority
                    .IncomingHit.baseline(
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

    public record SpaceControlBindingRule(
            ActionId action,
            int tellTicks,
            int recoveryTicks,
            double radius,
            double laneWidth,
            double laneLength,
            double shockBuildup,
            double playerPoisePressure,
            Integer donorSkillNumber,
            Integer donorAnimationTicks,
            boolean technicalPresentationCandidate
    ) {
        public SpaceControlBindingRule {
            Objects.requireNonNull(action, "action");
        }

        public boolean hasDonorPresentationCandidate() {
            return technicalPresentationCandidate
                    && donorSkillNumber != null
                    && donorAnimationTicks != null;
        }
    }

    public record PhysicalBindingRule(
            ActionId action,
            int tellTicks,
            int recoveryTicks,
            double minimumRange,
            double maximumRange,
            double minimumAbsoluteAngleDegrees,
            double maximumAbsoluteAngleDegrees,
            boolean requiresClearLine,
            double forwardPathBlocks,
            Integer donorSkillNumber,
            Integer donorAnimationTicks,
            boolean technicalPresentationCandidate
    ) {
        public PhysicalBindingRule {
            Objects.requireNonNull(action, "action");
        }

        public boolean hasDonorPresentationCandidate() {
            return technicalPresentationCandidate
                    && donorSkillNumber != null
                    && donorAnimationTicks != null;
        }
    }
}
