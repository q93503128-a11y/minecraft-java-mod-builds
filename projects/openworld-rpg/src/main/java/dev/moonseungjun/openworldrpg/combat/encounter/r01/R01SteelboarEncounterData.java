package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Canon-locked R01 Steelboar selection/timing contract.
 *
 * <p>Iron Rush intentionally keeps ordinary guardability nullable because current canon explicitly
 * closes perfect-guardability but does not yet state the ordinary guardable tag. This data is
 * selection-ready without fabricating that missing impact rule.</p>
 */
public record R01SteelboarEncounterData(
        String id,
        String entityId,
        int contentLevel,
        int decisionDelayTicks,
        List<AttackRule> actions,
        FuriousRouteRule furiousRoute
) {
    public R01SteelboarEncounterData {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(entityId, "entityId");
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        Objects.requireNonNull(furiousRoute, "furiousRoute");
        if (contentLevel <= 0 || decisionDelayTicks < 0) {
            throw new IllegalArgumentException("Invalid R01 Steelboar identity/timing contract.");
        }
    }

    public Map<ActionId, AttackRule> rulesById() {
        EnumMap<ActionId, AttackRule> result = new EnumMap<>(ActionId.class);
        for (AttackRule rule : actions) {
            AttackRule previous = result.put(rule.id(), rule);
            if (previous != null) {
                throw new IllegalStateException("Duplicate R01 Steelboar action: " + rule.id());
            }
        }
        return Map.copyOf(result);
    }

    public enum ActionId {
        IRON_TUSK,
        SHOULDER_HOOK,
        IRON_RUSH
    }

    public record AttackRule(
            ActionId id,
            int weight,
            int cooldownTicks,
            int tellTicks,
            int recoveryTicks,
            double minimumRange,
            double maximumRange,
            double committedMovementBlocks,
            double benchmarkDamageShare,
            PlayerDefenseAuthority.GuardPressureBand guardPressure,
            Boolean guardable,
            boolean perfectGuardable,
            double playerPoisePressure,
            Double perfectGuardPoiseMultiplier,
            Double obstacleSelfPoiseDamage
    ) {
        public AttackRule {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(guardPressure, "guardPressure");
            if (weight <= 0
                    || cooldownTicks < 0
                    || tellTicks < 0
                    || recoveryTicks < 0
                    || !Double.isFinite(minimumRange)
                    || !Double.isFinite(maximumRange)
                    || minimumRange < 0.0
                    || maximumRange < minimumRange
                    || !Double.isFinite(committedMovementBlocks)
                    || committedMovementBlocks < 0.0
                    || !Double.isFinite(benchmarkDamageShare)
                    || benchmarkDamageShare <= 0.0
                    || benchmarkDamageShare > 1.0
                    || !Double.isFinite(playerPoisePressure)
                    || playerPoisePressure < 0.0) {
                throw new IllegalArgumentException("Invalid R01 Steelboar attack rule: " + id);
            }
            if (perfectGuardPoiseMultiplier != null
                    && (!Double.isFinite(perfectGuardPoiseMultiplier)
                            || perfectGuardPoiseMultiplier <= 0.0)) {
                throw new IllegalArgumentException(
                        "Invalid Steelboar perfect-guard poise multiplier: " + id
                );
            }
            if (obstacleSelfPoiseDamage != null
                    && (!Double.isFinite(obstacleSelfPoiseDamage)
                            || obstacleSelfPoiseDamage < 0.0)) {
                throw new IllegalArgumentException(
                        "Invalid Steelboar obstacle self-poise damage: " + id
                );
            }
        }

        public boolean impactContractClosed() {
            return guardable != null;
        }

        public PlayerDefenseAuthority.IncomingHit toIncomingHit(
                int attackerLevel
        ) {
            if (!impactContractClosed()) {
                throw new IllegalStateException(
                        "Steelboar impact contract is not closed for " + id
                );
            }
            double rawDamage =
                    ProjectCombatRules.rawEnemyPhysicalDamageFromBenchmarkShare(
                            attackerLevel,
                            benchmarkDamageShare
                    );
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

    public record FuriousRouteRule(
            double healthFractionExclusive,
            int cooldownTicks,
            double minimumRange,
            double maximumRange,
            int minimumSecondChargePivotTellTicks,
            int recoveryAfterSecondChargeTicks
    ) {
        public FuriousRouteRule {
            if (!Double.isFinite(healthFractionExclusive)
                    || healthFractionExclusive <= 0.0
                    || healthFractionExclusive >= 1.0
                    || cooldownTicks <= 0
                    || !Double.isFinite(minimumRange)
                    || !Double.isFinite(maximumRange)
                    || minimumRange < 0.0
                    || maximumRange < minimumRange
                    || minimumSecondChargePivotTellTicks <= 0
                    || recoveryAfterSecondChargeTicks <= 0) {
                throw new IllegalArgumentException("Invalid R01 Steelboar Furious Route rule.");
            }
        }
    }
}
