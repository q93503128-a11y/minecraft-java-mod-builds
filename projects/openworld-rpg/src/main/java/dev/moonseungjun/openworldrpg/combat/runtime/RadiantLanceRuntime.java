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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Server-owned impact resolution for the Cleric root active Radiant Lance. */
public final class RadiantLanceRuntime {
    public static final double CHAIN_RADIUS_BLOCKS = 4.0;
    public static final double FALLBACK_HEAL_RADIUS_BLOCKS = 8.0;
    private static final double CHAIN_RADIUS_SQR =
            CHAIN_RADIUS_BLOCKS * CHAIN_RADIUS_BLOCKS;
    private static final double FALLBACK_HEAL_RADIUS_SQR =
            FALLBACK_HEAL_RADIUS_BLOCKS * FALLBACK_HEAL_RADIUS_BLOCKS;

    private RadiantLanceRuntime() {
    }

    public static Application apply(
            ServerPlayer caster,
            LivingEntity primaryTarget
    ) {
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(primaryTarget, "primaryTarget");

        if (caster.level().isClientSide()
                || primaryTarget.level() != caster.level()
                || primaryTarget == caster
                || !primaryTarget.isAlive()
                || PlayerProgressionService.state(caster)
                        .activeClass()
                        .filter(RootClass.CLERIC::equals)
                        .isEmpty()) {
            return Application.rejected();
        }

        var cast = ClericSkillRuntime.consumeAcceptedCast(
                caster,
                ProjectSpellSpec.RADIANT_LANCE_ID
        );
        if (cast == null) {
            return Application.rejected();
        }

        var build = CombatStateServices.combatBuilds()
                .build(caster.getUUID())
                .orElse(null);
        if (build == null) {
            return Application.rejected();
        }

        long nowTick = caster.level().getGameTime();
        var source = build.damageSource(
                ProjectImpactTransaction.DamageSchool.MAGIC
        );
        var primarySnapshot = ExternalActorBindingRuntime
                .projectTargetSnapshot(primaryTarget, nowTick)
                .orElse(null);
        if (primarySnapshot == null) {
            return Application.rejected();
        }

        var primaryDamage = ProjectImpactTransaction.resolveDirectDamage(
                new ProjectImpactTransaction.DirectDamageRequest(
                        source,
                        primarySnapshot,
                        ProjectImpactTransaction.DamageSchool.MAGIC,
                        ProjectSpellSpec.RADIANT_LANCE_ACTION_COEFFICIENT
                                * cast.outputMultiplier(),
                        1.0,
                        1.0
                )
        );
        double primaryPoise = ProjectImpactTransaction.resolvePoise(
                new ProjectImpactTransaction.PoiseRequest(
                        primarySnapshot.poiseMax(),
                        primarySnapshot.poiseMax(),
                        source.poiseOutputMultiplier(),
                        ProjectSpellSpec.RADIANT_LANCE_POISE_COEFFICIENT,
                        1.0,
                        1.0
                )
        ).poiseDamage();

        boolean primaryApplied =
                ProjectMinecraftDamageApplicator.applyDirectMagic(
                        caster,
                        primaryTarget,
                        primaryDamage.finalDamage()
                );
        if (!primaryApplied) {
            return Application.rejected();
        }

        if (primaryPoise > 0.0) {
            ExternalActorBindingRuntime.applyProjectPoiseDamage(
                    primaryTarget,
                    primaryPoise,
                    nowTick
            );
        }

        var combat = CombatStateServices.states()
                .getOrCreate(caster.getUUID(), nowTick);
        var grace = CombatStateServices.clericGraceStates()
                .getOrCreate(caster.getUUID());
        grace.recordDamagingActiveHit(
                nowTick,
                combat.lastCombatActivityTick()
        );

        int chainCandidates = 0;
        int chainApplied = 0;
        double fallbackEffectiveHealing = 0.0;

        if (cast.graceEmpowered()) {
            List<LivingEntity> chainTargets = chainTargets(
                    caster,
                    primaryTarget,
                    nowTick
            );
            chainCandidates = chainTargets.size();

            if (!chainTargets.isEmpty()) {
                for (LivingEntity chainTarget : chainTargets) {
                    var snapshot = ExternalActorBindingRuntime
                            .projectTargetSnapshot(
                                    chainTarget,
                                    nowTick
                            )
                            .orElse(null);
                    if (snapshot == null) {
                        continue;
                    }
                    double damage = ProjectImpactTransaction
                            .resolveDirectDamage(
                                    new ProjectImpactTransaction
                                            .DirectDamageRequest(
                                                    source,
                                                    snapshot,
                                                    ProjectImpactTransaction
                                                            .DamageSchool.MAGIC,
                                                    ProjectSpellSpec
                                                            .RADIANT_LANCE_CHAIN_ACTION_COEFFICIENT
                                                            * cast.outputMultiplier(),
                                                    1.0,
                                                    1.0
                                            )
                            ).finalDamage();
                    if (ProjectMinecraftDamageApplicator.applyDirectMagic(
                            caster,
                            chainTarget,
                            damage
                    )) {
                        chainApplied++;
                        emitChainCue(
                                (ServerLevel) caster.level(),
                                primaryTarget,
                                chainTarget
                        );
                    }
                }
            } else {
                ServerPlayer recipient = fallbackRecipient(caster);
                if (recipient != null) {
                    var healing = ProjectHealingRuntime.applySkillHeal(
                            caster,
                            recipient,
                            ProjectSpellSpec
                                    .RADIANT_LANCE_FALLBACK_HEAL_COEFFICIENT
                    );
                    if (healing.accepted()) {
                        fallbackEffectiveHealing =
                                healing.effectiveHealing();
                        grace.recordEffectiveHeal(
                                recipient.getUUID(),
                                fallbackEffectiveHealing,
                                recipient.getMaxHealth(),
                                nowTick,
                                combat.lastCombatActivityTick()
                        );
                    }
                }
            }
        }

        return new Application(
                true,
                cast.graceEmpowered(),
                cast.outputMultiplier(),
                primaryDamage.finalDamage(),
                primaryPoise,
                chainCandidates,
                chainApplied,
                fallbackEffectiveHealing,
                grace.pips(
                        nowTick,
                        combat.lastCombatActivityTick()
                )
        );
    }

    private static List<LivingEntity> chainTargets(
            ServerPlayer caster,
            LivingEntity primaryTarget,
            long nowTick
    ) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return List.of();
        }

        return level.getEntitiesOfClass(
                        LivingEntity.class,
                        primaryTarget.getBoundingBox()
                                .inflate(CHAIN_RADIUS_BLOCKS),
                        target -> target.isAlive()
                                && target != caster
                                && target != primaryTarget
                                && target.distanceToSqr(primaryTarget)
                                        <= CHAIN_RADIUS_SQR
                                && ExternalActorBindingRuntime
                                        .projectTargetSnapshot(
                                                target,
                                                nowTick
                                        ).isPresent()
                ).stream()
                .sorted(
                        Comparator
                                .comparingDouble(
                                        (LivingEntity target) ->
                                                target.distanceToSqr(
                                                        primaryTarget
                                                )
                                )
                                .thenComparingInt(
                                        LivingEntity::getId
                                )
                )
                .limit(2)
                .toList();
    }

    private static ServerPlayer fallbackRecipient(
            ServerPlayer caster
    ) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return null;
        }

        return level.players().stream()
                .filter(player -> player.isAlive()
                        && !player.isSpectator()
                        && player.distanceToSqr(caster)
                                <= FALLBACK_HEAL_RADIUS_SQR)
                .min(
                        Comparator
                                .comparingDouble(
                                        RadiantLanceRuntime
                                                ::healthFraction
                                )
                                .thenComparing(
                                        player -> player.getUUID()
                                                .toString()
                                )
                )
                .orElse(null);
    }

    private static double healthFraction(ServerPlayer player) {
        double max = Math.max(1.0, player.getMaxHealth());
        return player.getHealth() / max;
    }

    /**
     * Restrained secondary link cue. The actual attack identity remains the 3D Lance body; this
     * particle line is only the empowered chain readability layer.
     */
    private static void emitChainCue(
            ServerLevel level,
            LivingEntity from,
            LivingEntity to
    ) {
        Vec3 start = from.getBoundingBox().getCenter();
        Vec3 end = to.getBoundingBox().getCenter();
        Vec3 delta = end.subtract(start);
        for (int step = 1; step <= 7; step++) {
            double t = step / 8.0;
            Vec3 point = start.add(delta.scale(t));
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    point.x,
                    point.y,
                    point.z,
                    1,
                    0.01,
                    0.01,
                    0.01,
                    0.0
            );
        }
    }

    public record Application(
            boolean accepted,
            boolean graceEmpowered,
            double doctrineDamageMultiplier,
            double primaryDamage,
            double primaryPoiseDamage,
            int chainCandidates,
            int chainApplied,
            double fallbackEffectiveHealing,
            int gracePipsAfter
    ) {
        public Application {
            if (!Double.isFinite(doctrineDamageMultiplier)
                    || doctrineDamageMultiplier < 0.0
                    || !Double.isFinite(primaryDamage)
                    || primaryDamage < 0.0
                    || !Double.isFinite(primaryPoiseDamage)
                    || primaryPoiseDamage < 0.0
                    || chainCandidates < 0
                    || chainCandidates > 2
                    || chainApplied < 0
                    || chainApplied > chainCandidates
                    || !Double.isFinite(fallbackEffectiveHealing)
                    || fallbackEffectiveHealing < 0.0
                    || gracePipsAfter < 0
                    || gracePipsAfter > 3) {
                throw new IllegalArgumentException(
                        "Invalid Radiant Lance runtime result."
                );
            }
            if (!accepted
                    && (graceEmpowered
                    || doctrineDamageMultiplier != 0.0
                    || primaryDamage != 0.0
                    || primaryPoiseDamage != 0.0
                    || chainCandidates != 0
                    || chainApplied != 0
                    || fallbackEffectiveHealing != 0.0
                    || gracePipsAfter != 0)) {
                throw new IllegalArgumentException(
                        "Rejected Radiant Lance cannot carry applied state."
                );
            }
        }

        public static Application rejected() {
            return new Application(
                    false,
                    false,
                    0.0,
                    0.0,
                    0.0,
                    0,
                    0,
                    0.0,
                    0
            );
        }
    }
}
