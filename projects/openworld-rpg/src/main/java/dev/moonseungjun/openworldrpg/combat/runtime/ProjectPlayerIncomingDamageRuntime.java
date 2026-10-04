package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongPhysicalEncounterRuntime;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerDefenseRuntimeState;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-side bridge from a normalized authored hostile hit to the canonical player defense state.
 *
 * <p>This class deliberately does not infer an attack identity from donor damage numbers. Encounter
 * adapters must first produce an explicit project {@link PlayerDefenseAuthority.IncomingHit}. The
 * donor entity remains presentation/movement/AI input, while HP/Stamina/guard/dodge results are
 * project-owned.</p>
 */
public final class ProjectPlayerIncomingDamageRuntime {
    private ProjectPlayerIncomingDamageRuntime() {
    }

    public static IncomingApplication applyProjectOwnedActorHit(
            LivingEntity attacker,
            ServerPlayer target,
            PlayerDefenseAuthority.IncomingHit hit
    ) {
        return applyProjectOwnedActorHit(
                attacker,
                target,
                hit,
                ProjectPlayerReactionRuntime.ReactionSpec.none()
        );
    }

    public static IncomingApplication applyProjectOwnedActorHit(
            LivingEntity attacker,
            ServerPlayer target,
            PlayerDefenseAuthority.IncomingHit hit,
            ProjectPlayerReactionRuntime.ReactionSpec reaction
    ) {
        return applyProjectOwnedActorHit(attacker, target, hit, reaction, 1.0);
    }

    public static IncomingApplication applyProjectOwnedActorHit(
            LivingEntity attacker,
            ServerPlayer target,
            PlayerDefenseAuthority.IncomingHit hit,
            double perfectGuardPoiseMultiplier
    ) {
        return applyProjectOwnedActorHit(
                attacker,
                target,
                hit,
                ProjectPlayerReactionRuntime.ReactionSpec.none(),
                perfectGuardPoiseMultiplier
        );
    }

    public static IncomingApplication applyProjectOwnedActorHit(
            LivingEntity attacker,
            ServerPlayer target,
            PlayerDefenseAuthority.IncomingHit hit,
            ProjectPlayerReactionRuntime.ReactionSpec reaction,
            double perfectGuardPoiseMultiplier
    ) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(hit, "hit");
        Objects.requireNonNull(reaction, "reaction");
        if (!Double.isFinite(perfectGuardPoiseMultiplier)
                || perfectGuardPoiseMultiplier <= 0.0) {
            throw new IllegalArgumentException(
                    "Perfect-guard poise multiplier must be finite and positive."
            );
        }

        if (target == attacker
                || target.level().isClientSide()
                || attacker.level() != target.level()) {
            return IncomingApplication.rejected();
        }

        var actorProfile = ExternalActorBindingRuntime.combatProfile(attacker);
        if (actorProfile.isEmpty()
                || actorProfile.orElseThrow().contentLevel() != hit.attackerLevel()) {
            return IncomingApplication.rejected();
        }

        long gameTick = target.level().getGameTime();
        if (MagePhaseStepRuntime.invulnerable(
                target,
                gameTick
        )) {
            CombatStateServices.states()
                    .getOrCreate(target.getUUID(), gameTick)
                    .markCombatActivity(gameTick);
            return IncomingApplication.accepted(
                    false,
                    new PlayerDefenseRuntimeState.IncomingDefenseResult(
                            hit.rawDamage(),
                            0.0,
                            0.0,
                            false,
                            false,
                            false,
                            false
                    )
            );
        }

        var defenseSnapshot = CombatStateServices.defenseSnapshots()
                .snapshot(target.getUUID());
        if (defenseSnapshot.isEmpty()) {
            // Fail closed. Never substitute vanilla/donor armor when project state is missing.
            return IncomingApplication.rejected();
        }

        double outgoingDirectDamageMultiplier =
                ProjectHostileStatusRuntime.outgoingDirectDamageMultiplier(
                        attacker,
                        gameTick
                );
        PlayerDefenseAuthority.IncomingHit effectiveHit =
                scaleDirectDamage(
                        hit,
                        outgoingDirectDamageMultiplier
                );
        effectiveHit = scaleDirectDamage(
                effectiveHit,
                GuardianSkillRuntime.incomingDamageTakenMultiplier(
                        target,
                        effectiveHit,
                        gameTick
                )
        );

        var resources = CombatStateServices.states()
                .getOrCreate(target.getUUID(), gameTick);
        long combatActivityBeforeHit =
                resources.lastCombatActivityTick();
        var activeDefense = CombatStateServices.defenseStates()
                .getOrCreate(target.getUUID());

        var counterwall = GuardianSkillRuntime.tryResolveCounterwall(
                attacker,
                target,
                effectiveHit,
                defenseSnapshot.orElseThrow(),
                gameTick
        );
        if (counterwall.isPresent()) {
            var counterwallResolution = counterwall.orElseThrow();
            resources.markCombatActivity(gameTick);
            recordEarthloongGuardThreat(
                    attacker,
                    target,
                    counterwallResolution,
                    gameTick
            );
            ProjectPerfectGuardRuntime.onSuccessfulPerfectGuard(
                    target,
                    attacker,
                    gameTick,
                    perfectGuardPoiseMultiplier
            );
            ProjectUltimateChargeRuntime.recordGuardianPerfectGuard(
                    target,
                    attacker
            );
            ProjectUltimateChargeRuntime.recordGuardianProvokedHit(
                    target,
                    attacker,
                    gameTick
            );
            R01EarthloongMythicRuntime.onPerfectGuard(
                    target,
                    gameTick
            );
            return IncomingApplication.accepted(
                    false,
                    counterwallResolution
            );
        }

        var counter = WarriorSkillRuntime.tryResolveIronCounter(
                attacker,
                target,
                effectiveHit,
                defenseSnapshot.orElseThrow(),
                gameTick
        );
        if (counter.isPresent()) {
            var counterResolution = counter.orElseThrow();
            resources.markCombatActivity(gameTick);
            recordEarthloongGuardThreat(
                    attacker,
                    target,
                    counterResolution,
                    gameTick
            );
            ProjectPerfectGuardRuntime.onSuccessfulPerfectGuard(
                    target,
                    attacker,
                    gameTick,
                    perfectGuardPoiseMultiplier
            );
            GuardianResolveRuntime.onPerfectGuard(
                    target,
                    gameTick
            );
            R01EarthloongMythicRuntime.onPerfectGuard(
                    target,
                    gameTick
            );
            return IncomingApplication.accepted(
                    false,
                    counterResolution
            );
        }

        var bulwarkGuard = GuardianSkillRuntime
                .tryResolveBulwarkRushGuard(
                        attacker,
                        target,
                        effectiveHit,
                        defenseSnapshot.orElseThrow(),
                        gameTick
                );
        PlayerDefenseRuntimeState.IncomingDefenseResult resolution;
        if (bulwarkGuard.isPresent()) {
            resolution = bulwarkGuard.orElseThrow();
        } else {
            resolution = activeDefense.resolveIncoming(
                    resources,
                    defenseSnapshot.orElseThrow(),
                    effectiveHit,
                    gameTick,
                    GuardianRootPassiveEffects
                            .guardImpactStaminaCostMultiplier(target)
                            * GuardianSkillRuntime
                                    .guardStaminaCostMultiplier(
                                            target,
                                            gameTick
                                    )
            );
        }
        resources.markCombatActivity(gameTick);
        recordEarthloongGuardThreat(
                attacker,
                target,
                resolution,
                gameTick
        );

        if (!resolution.dodged()
                && (resolution.guarded()
                        || resolution.finalDamage() > 0.0)) {
            ProjectUltimateChargeRuntime.recordGuardianProvokedHit(
                    target,
                    attacker,
                    gameTick
            );
        }

        var barrier = ProjectBarrierRuntime.absorbHostileDamage(
                attacker,
                target,
                resolution.finalDamage(),
                gameTick
        );
        if (barrier.absorbedDamage() > 0.0) {
            resolution = new PlayerDefenseRuntimeState.IncomingDefenseResult(
                    resolution.mitigatedBeforeActiveDefense(),
                    barrier.remainingDamage(),
                    resolution.staminaSpent(),
                    resolution.dodged(),
                    resolution.guarded(),
                    resolution.perfectGuarded(),
                    resolution.guardBroken()
            );
        }

        if (resolution.perfectGuarded()) {
            ProjectPerfectGuardRuntime.onSuccessfulPerfectGuard(
                    target,
                    attacker,
                    gameTick,
                    perfectGuardPoiseMultiplier
            );
            GuardianResolveRuntime.onPerfectGuard(
                    target,
                    gameTick
            );
            ProjectUltimateChargeRuntime.recordGuardianPerfectGuard(
                    target,
                    attacker
            );
            R01EarthloongMythicRuntime.onPerfectGuard(target, gameTick);
            WarriorSkillRuntime.onSuccessfulPerfectGuard(
                    target,
                    attacker,
                    gameTick
            );
        } else if (resolution.guarded()
                && !resolution.guardBroken()) {
            GuardianResolveRuntime.onOrdinaryGuardedHit(
                    target,
                    resolution.staminaSpent(),
                    gameTick
            );
            ProjectUltimateChargeRuntime
                    .recordGuardianOrdinaryGuardedHit(
                            target,
                            attacker,
                            resolution.staminaSpent()
                    );
        }
        if (resolution.guardBroken()) {
            ProjectPlayerReactionRuntime.applyGuardBreak(
                    target,
                    Math.toIntExact(
                            PlayerDefenseRuntimeState
                                    .GUARD_BREAK_REACTION_TICKS
                    )
            );
        }

        if (resolution.finalDamage() <= 0.0) {
            return IncomingApplication.accepted(false, resolution);
        }

        boolean applied = switch (hit.school()) {
            case PHYSICAL -> ProjectMinecraftDamageApplicator.applyDirectPhysical(
                    attacker,
                    target,
                    resolution.finalDamage()
            );
            case MAGIC -> ProjectMinecraftDamageApplicator.applyDirectMagic(
                    attacker,
                    target,
                    resolution.finalDamage()
            );
        };
        if (applied) {
            MageArcaneWeaveRuntime.onDirectHpDamage(
                    target,
                    gameTick
            );
            HunterSkillRuntime.onDirectHpDamage(
                    target,
                    gameTick,
                    combatActivityBeforeHit
            );
            resources.markHostileHpActivity(gameTick);
            if (!resolution.guarded()) {
                ProjectPlayerReactionRuntime.apply(
                        target,
                        reaction
                );
            }
        }

        return IncomingApplication.accepted(applied, resolution);
    }

    private static void recordEarthloongGuardThreat(
            LivingEntity attacker,
            ServerPlayer target,
            PlayerDefenseRuntimeState.IncomingDefenseResult resolution,
            long gameTick
    ) {
        if (!resolution.guarded()) {
            return;
        }
        double preventedHpDamage = Math.max(
                0.0,
                resolution.mitigatedBeforeActiveDefense()
                        - resolution.finalDamage()
        );
        R01EarthloongPhysicalEncounterRuntime.recordGuardThreat(
                attacker,
                target,
                preventedHpDamage,
                resolution.perfectGuarded(),
                gameTick
        );
    }

    static PlayerDefenseAuthority.IncomingHit scaleDirectDamage(
            PlayerDefenseAuthority.IncomingHit hit,
            double multiplier
    ) {
        Objects.requireNonNull(hit, "hit");
        if (!Double.isFinite(multiplier)
                || multiplier <= 0.0
                || multiplier > 1.0) {
            throw new IllegalArgumentException(
                    "Outgoing direct-damage multiplier must be inside (0, 1]."
            );
        }
        if (multiplier == 1.0) {
            return hit;
        }
        return new PlayerDefenseAuthority.IncomingHit(
                hit.rawDamage() * multiplier,
                hit.school(),
                hit.attackerLevel(),
                hit.guardPressure(),
                hit.dodgeable(),
                hit.guardable(),
                hit.perfectGuardable(),
                hit.authoredDamageTakenMultiplier(),
                hit.authoredDamageReduction()
        );
    }

    public record IncomingApplication(
            boolean accepted,
            boolean minecraftDamageApplied,
            Optional<PlayerDefenseRuntimeState.IncomingDefenseResult> resolution
    ) {
        public IncomingApplication {
            Objects.requireNonNull(resolution, "resolution");
            if (!accepted && resolution.isPresent()) {
                throw new IllegalArgumentException(
                        "Rejected incoming applications cannot carry a resolution."
                );
            }
        }

        public static IncomingApplication accepted(
                boolean minecraftDamageApplied,
                PlayerDefenseRuntimeState.IncomingDefenseResult resolution
        ) {
            return new IncomingApplication(
                    true,
                    minecraftDamageApplied,
                    Optional.of(Objects.requireNonNull(resolution, "resolution"))
            );
        }

        public static IncomingApplication rejected() {
            return new IncomingApplication(false, false, Optional.empty());
        }
    }
}
