package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class MageArcBoltRuntime {
    public static final double WEAVE_FORK_RADIUS_BLOCKS = 4.0;
    private static final double WEAVE_FORK_RADIUS_SQR =
            WEAVE_FORK_RADIUS_BLOCKS * WEAVE_FORK_RADIUS_BLOCKS;

    private MageArcBoltRuntime() {
    }

    public static ForkApplication applyWeaveForks(
            ServerPlayer caster,
            LivingEntity primaryTarget,
            ProjectImpactTransaction.DamageSourceSnapshot source,
            long nowTick
    ) {
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(primaryTarget, "primaryTarget");
        Objects.requireNonNull(source, "source");

        ProjectUltimateChargeRuntime.recordMagePrimaryActiveHit(
                caster,
                primaryTarget
        );

        var empowerment = MageArcaneWeaveRuntime.consumeEmpoweredCast(
                caster.getUUID(),
                ProjectSpellSpec.ARC_BOLT_ID,
                nowTick
        );
        if (empowerment.isEmpty()) {
            return ForkApplication.none();
        }

        double magnitudeMultiplier = empowerment.getAsDouble();
        List<LivingEntity> targets = forkTargets(caster, primaryTarget, nowTick);
        int applied = 0;
        double totalDamage = 0.0;
        for (LivingEntity target : targets) {
            var snapshot = ExternalActorBindingRuntime
                    .projectTargetSnapshot(target, nowTick).orElse(null);
            if (snapshot == null) {
                continue;
            }
            double damage = ProjectImpactTransaction.resolveDirectDamage(
                    new ProjectImpactTransaction.DirectDamageRequest(
                            source,
                            snapshot,
                            ProjectImpactTransaction.DamageSchool.MAGIC,
                            ProjectSpellSpec.ARC_BOLT_WEAVE_FORK_ACTION_COEFFICIENT
                                    * magnitudeMultiplier,
                            1.0,
                            1.0
                    )
            ).finalDamage();
            if (ProjectMinecraftDamageApplicator.applyDirectMagic(caster, target, damage)) {
                applied++;
                totalDamage += damage;
                ProjectUltimateChargeRuntime.recordMageAdditionalActiveHit(
                        caster,
                        target
                );
            }
        }

        return new ForkApplication(
                true,
                magnitudeMultiplier,
                targets.size(),
                applied,
                totalDamage
        );
    }

    private static List<LivingEntity> forkTargets(
            ServerPlayer caster,
            LivingEntity primaryTarget,
            long nowTick
    ) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return List.of();
        }
        return level.getEntitiesOfClass(
                        LivingEntity.class,
                        primaryTarget.getBoundingBox().inflate(WEAVE_FORK_RADIUS_BLOCKS),
                        target -> target.isAlive()
                                && target != caster
                                && target != primaryTarget
                                && target.distanceToSqr(primaryTarget) <= WEAVE_FORK_RADIUS_SQR
                                && ExternalActorBindingRuntime
                                        .projectTargetSnapshot(target, nowTick).isPresent()
                ).stream()
                .sorted(
                        Comparator
                                .comparingDouble(
                                        (LivingEntity target) ->
                                                target.distanceToSqr(primaryTarget)
                                )
                                .thenComparingInt(LivingEntity::getId)
                )
                .limit(2)
                .toList();
    }

    public record ForkApplication(
            boolean empowered,
            double magnitudeMultiplier,
            int candidateCount,
            int appliedCount,
            double totalDamage
    ) {
        public ForkApplication {
            if (!Double.isFinite(magnitudeMultiplier)
                    || magnitudeMultiplier < 0.0
                    || candidateCount < 0 || candidateCount > 2
                    || appliedCount < 0 || appliedCount > candidateCount
                    || !Double.isFinite(totalDamage)
                    || totalDamage < 0.0) {
                throw new IllegalArgumentException("Invalid Arc Bolt Weave fork result.");
            }
            if (!empowered
                    && (magnitudeMultiplier != 0.0
                    || candidateCount != 0
                    || appliedCount != 0
                    || totalDamage != 0.0)) {
                throw new IllegalArgumentException(
                        "Non-empowered Arc Bolt cannot carry fork output."
                );
            }
        }

        public static ForkApplication none() {
            return new ForkApplication(false, 0.0, 0, 0, 0.0);
        }
    }
}
