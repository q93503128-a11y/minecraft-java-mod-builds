package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerIncomingDamageRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerPoisePressureRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.R01ExternalActorCatalog;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Project-owned impact authority for the two fully closed Steelboar melee attacks.
 */
public final class R01SteelboarImpactAuthority {
    private static final R01SteelboarEncounterData DATA =
            R01SteelboarEncounterDataLoader.load();

    private R01SteelboarImpactAuthority() {
    }

    public static ImpactApplication applyConfirmedMeleeContact(
            LivingEntity steelboar,
            ServerPlayer target,
            R01SteelboarEncounterData.ActionId action
    ) {
        Objects.requireNonNull(steelboar, "steelboar");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(action, "action");

        var profile = ExternalActorBindingRuntime
                .combatProfile(steelboar)
                .orElse(null);
        if (profile == null
                || !R01ExternalActorCatalog.STEELBOAR
                        .equals(profile.entityId())
                || profile.contentLevel() != DATA.contentLevel()
                || !ExternalActorBindingRuntime
                        .isAuthoredSpawn(steelboar)) {
            return ImpactApplication.rejected();
        }

        R01SteelboarEncounterData.AttackRule rule =
                DATA.rulesById().get(action);
        if (rule == null
                || action == R01SteelboarEncounterData.ActionId.IRON_RUSH
                || !rule.impactContractClosed()) {
            return ImpactApplication.rejected();
        }

        ProjectPlayerIncomingDamageRuntime.IncomingApplication incoming =
                ProjectPlayerIncomingDamageRuntime.applyProjectOwnedActorHit(
                        steelboar,
                        target,
                        rule.toIncomingHit(DATA.contentLevel())
                );
        if (!incoming.accepted()) {
            return new ImpactApplication(
                    incoming,
                    Optional.empty()
            );
        }

        Optional<ProjectPlayerPoisePressureRuntime.Application> poise =
                rule.playerPoisePressure() > 0.0
                        ? Optional.of(
                                ProjectPlayerPoisePressureRuntime
                                        .applyAuthoredPressure(
                                                target,
                                                rule.playerPoisePressure()
                                        )
                        )
                        : Optional.empty();
        return new ImpactApplication(incoming, poise);
    }

    static Optional<R01SteelboarEncounterData.AttackRule> rule(
            R01SteelboarEncounterData.ActionId action
    ) {
        Objects.requireNonNull(action, "action");
        return Optional.ofNullable(DATA.rulesById().get(action));
    }

    static boolean impactReadyAction(
            R01SteelboarEncounterData.ActionId action
    ) {
        return rule(action)
                .map(rule -> action
                        != R01SteelboarEncounterData.ActionId.IRON_RUSH
                        && rule.impactContractClosed())
                .orElse(false);
    }

    public record ImpactApplication(
            ProjectPlayerIncomingDamageRuntime.IncomingApplication incoming,
            Optional<ProjectPlayerPoisePressureRuntime.Application> poisePressure
    ) {
        public ImpactApplication {
            Objects.requireNonNull(incoming, "incoming");
            Objects.requireNonNull(poisePressure, "poisePressure");
            if (!incoming.accepted() && poisePressure.isPresent()) {
                throw new IllegalArgumentException(
                        "Rejected Steelboar impact cannot apply player-poise pressure."
                );
            }
        }

        public static ImpactApplication rejected() {
            return new ImpactApplication(
                    ProjectPlayerIncomingDamageRuntime
                            .IncomingApplication.rejected(),
                    Optional.empty()
            );
        }
    }
}
