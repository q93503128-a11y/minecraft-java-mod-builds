package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Canon-locked R01 Regalhart selection/phase contract.
 *
 * <p>Impact fields remain nullable where current canon has not stated them. This record is therefore
 * safe for action selection and combo-state authority without inventing damage-defense semantics.</p>
 */
public record R01RegalhartEncounterData(
        String id,
        String entityId,
        int contentLevel,
        int decisionDelayTicks,
        double weakPointMultiplier,
        List<AttackRule> actions,
        SovereignRule sovereign
) {
    public R01RegalhartEncounterData {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(entityId, "entityId");
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        Objects.requireNonNull(sovereign, "sovereign");
        if (contentLevel <= 0
                || decisionDelayTicks < 0
                || !Double.isFinite(weakPointMultiplier)
                || weakPointMultiplier <= 1.0) {
            throw new IllegalArgumentException("Invalid R01 Regalhart identity contract.");
        }
    }

    public Map<ActionId, AttackRule> rulesById() {
        EnumMap<ActionId, AttackRule> result = new EnumMap<>(ActionId.class);
        for (AttackRule rule : actions) {
            AttackRule previous = result.put(rule.id(), rule);
            if (previous != null) {
                throw new IllegalStateException("Duplicate Regalhart action: " + rule.id());
            }
        }
        return Map.copyOf(result);
    }

    public enum ActionId {
        ANTLER_SWEEP,
        CROWN_CHARGE,
        REAR_KICK,
        ROYAL_BOUND
    }

    public record AttackRule(
            ActionId id,
            int cooldownTicks,
            int tellTicks,
            int recoveryTicks,
            double maximumRange,
            double movementEnvelopeBlocks,
            double areaRadius,
            double benchmarkDamageShare,
            PlayerDefenseAuthority.GuardPressureBand guardPressure,
            Boolean guardable,
            Boolean perfectGuardable,
            Double playerPoisePressure,
            Double perfectGuardPoiseMultiplier
    ) {
        public AttackRule {
            Objects.requireNonNull(id, "id");
            if (cooldownTicks < 0
                    || tellTicks < 0
                    || recoveryTicks < 0
                    || !Double.isFinite(maximumRange)
                    || maximumRange < 0.0
                    || !Double.isFinite(movementEnvelopeBlocks)
                    || movementEnvelopeBlocks < 0.0
                    || !Double.isFinite(areaRadius)
                    || areaRadius < 0.0
                    || !Double.isFinite(benchmarkDamageShare)
                    || benchmarkDamageShare <= 0.0
                    || benchmarkDamageShare > 1.0
                    || (playerPoisePressure != null
                            && (!Double.isFinite(playerPoisePressure)
                                    || playerPoisePressure < 0.0))
                    || (perfectGuardPoiseMultiplier != null
                            && (!Double.isFinite(perfectGuardPoiseMultiplier)
                                    || perfectGuardPoiseMultiplier <= 0.0))) {
                throw new IllegalArgumentException("Invalid Regalhart action rule: " + id);
            }
        }

        public boolean impactContractClosed() {
            if (guardable == null || perfectGuardable == null) {
                return false;
            }
            boolean guardInteraction = guardable || perfectGuardable;
            return !guardInteraction || guardPressure != null;
        }

        public PlayerDefenseAuthority.IncomingHit toIncomingHit(
                int attackerLevel
        ) {
            if (!impactContractClosed()) {
                throw new IllegalStateException(
                        "Regalhart impact contract is not closed for " + id
                );
            }
            double rawDamage =
                    ProjectCombatRules.rawEnemyPhysicalDamageFromBenchmarkShare(
                            attackerLevel,
                            benchmarkDamageShare
                    );
            if (!guardable && !perfectGuardable) {
                return PlayerDefenseAuthority.IncomingHit.unguardable(
                        rawDamage,
                        ProjectImpactTransaction.DamageSchool.PHYSICAL,
                        attackerLevel,
                        true
                );
            }
            return PlayerDefenseAuthority.IncomingHit.baseline(
                    rawDamage,
                    ProjectImpactTransaction.DamageSchool.PHYSICAL,
                    attackerLevel,
                    guardPressure,
                    true,
                    guardable,
                    perfectGuardable
            );
        }
    }

    public record SovereignRule(
            double healthFractionInclusive,
            int transitionTicks,
            double transitionDamageTakenMultiplier,
            double movementSpeedMultiplier,
            int crownChargeComboEvery,
            double secondChargeMinimumLineBlocks,
            int secondChargeMinimumPivotTellTicks,
            int chainedChargeRecoveryTicks,
            int normalSweepComboEvery,
            int sovereignSweepComboEvery
    ) {
        public SovereignRule {
            if (!Double.isFinite(healthFractionInclusive)
                    || healthFractionInclusive <= 0.0
                    || healthFractionInclusive >= 1.0
                    || transitionTicks <= 0
                    || !Double.isFinite(transitionDamageTakenMultiplier)
                    || transitionDamageTakenMultiplier <= 0.0
                    || transitionDamageTakenMultiplier > 1.0
                    || !Double.isFinite(movementSpeedMultiplier)
                    || movementSpeedMultiplier <= 1.0
                    || crownChargeComboEvery <= 0
                    || !Double.isFinite(secondChargeMinimumLineBlocks)
                    || secondChargeMinimumLineBlocks <= 0.0
                    || secondChargeMinimumPivotTellTicks <= 0
                    || chainedChargeRecoveryTicks <= 0
                    || normalSweepComboEvery <= 0
                    || sovereignSweepComboEvery <= 0) {
                throw new IllegalArgumentException("Invalid Regalhart Sovereign rule.");
            }
        }
    }
}
