package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-authoritative frontal resolution for Cleric Rebuke.
 */
public final class RebukeRuntime {
    private RebukeRuntime() {
    }

    public static Application release(ServerPlayer caster) {
        Objects.requireNonNull(caster, "caster");
        if (caster.level().isClientSide()
                || PlayerProgressionService.state(caster)
                        .activeClass()
                        .filter(RootClass.CLERIC::equals)
                        .isEmpty()) {
            return Application.rejected();
        }

        var cast = ClericSkillRuntime.consumeAcceptedCast(
                caster,
                ProjectSpellSpec.REBUKE_ID
        );
        if (cast == null) {
            return Application.rejected();
        }

        var build = CombatStateServices.combatBuilds()
                .build(caster.getUUID())
                .orElse(null);
        if (build == null
                || !(caster.level() instanceof ServerLevel level)) {
            return Application.rejected();
        }

        long nowTick = level.getGameTime();
        var source = build.damageSource(
                ProjectImpactTransaction.DamageSchool.MAGIC
        );
        double poiseCoefficient = cast.graceEmpowered()
                ? ProjectSpellSpec.REBUKE_EMPOWERED_POISE_COEFFICIENT
                : ProjectSpellSpec.REBUKE_POISE_COEFFICIENT;

        List<LivingEntity> targets = level.getEntitiesOfClass(
                        LivingEntity.class,
                        caster.getBoundingBox().inflate(
                                RebukeBurstShape.RANGE_BLOCKS + 1.0
                        ),
                        target -> ExternalActorBindingRuntime
                                .projectTargetSnapshot(
                                        target,
                                        nowTick
                                ).isPresent()
                                && RebukeBurstShape.contains(
                                        caster,
                                        target
                                )
                ).stream()
                .sorted(
                        Comparator
                                .comparingDouble(
                                        (LivingEntity target) ->
                                                caster.distanceToSqr(target)
                                )
                                .thenComparingInt(
                                        LivingEntity::getId
                                )
                )
                .toList();

        int appliedTargets = 0;
        int rebukedTargets = 0;
        double totalDamage = 0.0;
        double totalPoiseDamage = 0.0;
        LivingEntity ultimateChargePrimary = null;

        for (LivingEntity target : targets) {
            var targetSnapshot = ExternalActorBindingRuntime
                    .projectTargetSnapshot(
                            target,
                            nowTick
                    )
                    .orElse(null);
            if (targetSnapshot == null) {
                continue;
            }

            double damage = ProjectImpactTransaction
                    .resolveDirectDamage(
                            new ProjectImpactTransaction
                                    .DirectDamageRequest(
                                            source,
                                            targetSnapshot,
                                            ProjectImpactTransaction
                                                    .DamageSchool.MAGIC,
                                            ProjectSpellSpec
                                                    .REBUKE_ACTION_COEFFICIENT
                                                    * cast.outputMultiplier(),
                                            1.0,
                                            1.0
                                    )
                    ).finalDamage();

            double poiseDamage = targetSnapshot.poiseMax() > 0.0
                    ? ProjectImpactTransaction
                            .resolvePoise(
                                    new ProjectImpactTransaction
                                            .PoiseRequest(
                                                    targetSnapshot
                                                            .poiseMax(),
                                                    targetSnapshot
                                                            .poiseMax(),
                                                    source.poiseOutputMultiplier(),
                                                    poiseCoefficient,
                                                    1.0,
                                                    1.0
                                            )
                            ).poiseDamage()
                    : 0.0;

            if (!ProjectMinecraftDamageApplicator.applyDirectMagic(
                    caster,
                    target,
                    damage
            )) {
                continue;
            }

            appliedTargets++;
            if (ultimateChargePrimary == null) {
                ultimateChargePrimary = target;
            }
            totalDamage += damage;
            if (poiseDamage > 0.0) {
                ExternalActorBindingRuntime.applyProjectPoiseDamage(
                        target,
                        poiseDamage,
                        nowTick
                );
                totalPoiseDamage += poiseDamage;
            }

            if (ProjectHostileStatusRuntime.applyRebuked(
                    target,
                    cast.graceEmpowered(),
                    nowTick
            ).isPresent()) {
                rebukedTargets++;
            }
        }

        var combat = CombatStateServices.states()
                .getOrCreate(caster.getUUID(), nowTick);
        var grace = CombatStateServices.clericGraceStates()
                .getOrCreate(caster.getUUID());
        if (appliedTargets > 0) {
            CombatStateServices.markCombatActivity(
                    caster.getUUID(),
                    nowTick
            );
            ClericRootPassiveRuntime.recordDamagingEligibleHit(
                    caster,
                    nowTick
            );
            var gain = grace.recordDamagingActiveHit(
                    nowTick,
                    combat.lastCombatActivityTick()
            );
            ClericRootPassiveRuntime.onGraceGain(
                    caster,
                    gain,
                    nowTick
            );
            if (ultimateChargePrimary != null) {
                ProjectUltimateChargeRuntime
                        .recordClericDamagingActive(
                                caster,
                                ultimateChargePrimary
                        );
            }
        }

        return new Application(
                true,
                cast.graceEmpowered(),
                cast.outputMultiplier(),
                poiseCoefficient,
                targets.size(),
                appliedTargets,
                rebukedTargets,
                totalDamage,
                totalPoiseDamage,
                grace.pips(
                        nowTick,
                        combat.lastCombatActivityTick()
                )
        );
    }

    public record Application(
            boolean accepted,
            boolean graceEmpowered,
            double doctrineDamageMultiplier,
            double poiseCoefficient,
            int eligibleTargets,
            int appliedTargets,
            int rebukedTargets,
            double totalDamage,
            double totalPoiseDamage,
            int gracePipsAfter
    ) {
        public Application {
            if (!Double.isFinite(doctrineDamageMultiplier)
                    || doctrineDamageMultiplier < 0.0
                    || !Double.isFinite(poiseCoefficient)
                    || poiseCoefficient < 0.0
                    || eligibleTargets < 0
                    || appliedTargets < 0
                    || appliedTargets > eligibleTargets
                    || rebukedTargets < 0
                    || rebukedTargets > appliedTargets
                    || !Double.isFinite(totalDamage)
                    || totalDamage < 0.0
                    || !Double.isFinite(totalPoiseDamage)
                    || totalPoiseDamage < 0.0
                    || gracePipsAfter < 0
                    || gracePipsAfter > 3) {
                throw new IllegalArgumentException(
                        "Invalid Rebuke runtime result."
                );
            }
            if (!accepted
                    && (graceEmpowered
                    || doctrineDamageMultiplier != 0.0
                    || poiseCoefficient != 0.0
                    || eligibleTargets != 0
                    || appliedTargets != 0
                    || rebukedTargets != 0
                    || totalDamage != 0.0
                    || totalPoiseDamage != 0.0
                    || gracePipsAfter != 0)) {
                throw new IllegalArgumentException(
                        "Rejected Rebuke cannot carry applied state."
                );
            }
        }

        public static Application rejected() {
            return new Application(
                    false,
                    false,
                    0.0,
                    0.0,
                    0,
                    0,
                    0,
                    0.0,
                    0.0,
                    0
            );
        }
    }
}
