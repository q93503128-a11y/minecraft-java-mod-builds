package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
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
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(hit, "hit");

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

        var defenseSnapshot = CombatStateServices.defenseSnapshots()
                .snapshot(target.getUUID());
        if (defenseSnapshot.isEmpty()) {
            // Fail closed. Never substitute vanilla/donor armor when project state is missing.
            return IncomingApplication.rejected();
        }

        long gameTick = target.level().getGameTime();
        var resources = CombatStateServices.states()
                .getOrCreate(target.getUUID(), gameTick);
        var activeDefense = CombatStateServices.defenseStates()
                .getOrCreate(target.getUUID());

        var resolution = activeDefense.resolveIncoming(
                resources,
                defenseSnapshot.orElseThrow(),
                hit,
                gameTick
        );
        resources.markCombatActivity(gameTick);

        var barrier = R01EarthloongMythicRuntime.absorbBarrier(
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
            R01EarthloongMythicRuntime.onPerfectGuard(target, gameTick);
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
            resources.markHostileHpActivity(gameTick);
        }

        return IncomingApplication.accepted(applied, resolution);
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
