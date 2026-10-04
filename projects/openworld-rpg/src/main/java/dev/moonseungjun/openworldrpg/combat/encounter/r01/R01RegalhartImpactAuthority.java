package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerIncomingDamageRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerPoisePressureRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.R01ExternalActorCatalog;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Project-owned result authority for presentation-confirmed Regalhart contacts. */
public final class R01RegalhartImpactAuthority {
    private static final R01RegalhartEncounterData DATA = R01RegalhartEncounterDataLoader.load();

    private R01RegalhartImpactAuthority() {}

    public static ImpactApplication applyConfirmedContact(
            LivingEntity regalhart,
            ServerPlayer target,
            R01RegalhartEncounterData.ActionId action
    ) {
        Objects.requireNonNull(regalhart, "regalhart");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(action, "action");
        var profile = ExternalActorBindingRuntime.combatProfile(regalhart).orElse(null);
        if (profile == null
                || !R01ExternalActorCatalog.REGALHART.equals(profile.entityId())
                || profile.contentLevel() != DATA.contentLevel()
                || !ExternalActorBindingRuntime.isAuthoredSpawn(regalhart)) {
            return ImpactApplication.rejected();
        }
        var rule = DATA.rulesById().get(action);
        if (rule == null || !rule.impactContractClosed()) return ImpactApplication.rejected();

        double perfectGuardPoiseMultiplier = rule.perfectGuardPoiseMultiplier() == null
                ? 1.0
                : rule.perfectGuardPoiseMultiplier();
        var incoming = ProjectPlayerIncomingDamageRuntime.applyProjectOwnedActorHit(
                regalhart,
                target,
                rule.toIncomingHit(DATA.contentLevel()),
                perfectGuardPoiseMultiplier
        );
        if (!incoming.accepted()) {
            return new ImpactApplication(incoming, Optional.empty());
        }
        Optional<ProjectPlayerPoisePressureRuntime.Application> poise =
                rule.playerPoisePressure() != null && rule.playerPoisePressure() > 0.0
                        ? Optional.of(ProjectPlayerPoisePressureRuntime.applyAuthoredPressure(
                                target,
                                rule.playerPoisePressure()
                        ))
                        : Optional.empty();
        return new ImpactApplication(incoming, poise);
    }

    static boolean impactReadyAction(R01RegalhartEncounterData.ActionId action) {
        var rule = DATA.rulesById().get(Objects.requireNonNull(action, "action"));
        return rule != null && rule.impactContractClosed();
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
                        "Rejected Regalhart impact cannot apply player-poise pressure."
                );
            }
        }
        public static ImpactApplication rejected() {
            return new ImpactApplication(
                    ProjectPlayerIncomingDamageRuntime.IncomingApplication.rejected(),
                    Optional.empty()
            );
        }
    }
}
