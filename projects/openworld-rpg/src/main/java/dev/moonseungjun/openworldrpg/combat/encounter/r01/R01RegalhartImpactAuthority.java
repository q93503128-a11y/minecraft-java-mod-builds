package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerIncomingDamageRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.R01ExternalActorCatalog;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Project-owned impact authority for Regalhart attacks whose current impact contracts are complete.
 */
public final class R01RegalhartImpactAuthority {
    private static final R01RegalhartEncounterData DATA =
            R01RegalhartEncounterDataLoader.load();

    private R01RegalhartImpactAuthority() {
    }

    public static ImpactApplication applyConfirmedContact(
            LivingEntity regalhart,
            ServerPlayer target,
            R01RegalhartEncounterData.ActionId action
    ) {
        Objects.requireNonNull(regalhart, "regalhart");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(action, "action");

        var profile = ExternalActorBindingRuntime
                .combatProfile(regalhart)
                .orElse(null);
        if (profile == null
                || !R01ExternalActorCatalog.REGALHART
                        .equals(profile.entityId())
                || profile.contentLevel() != DATA.contentLevel()
                || !ExternalActorBindingRuntime
                        .isAuthoredSpawn(regalhart)) {
            return ImpactApplication.rejected();
        }

        if (action != R01RegalhartEncounterData.ActionId.ANTLER_SWEEP
                && action != R01RegalhartEncounterData.ActionId.ROYAL_BOUND) {
            return ImpactApplication.rejected();
        }

        var rule = DATA.rulesById().get(action);
        if (rule == null || !rule.impactContractClosed()) {
            return ImpactApplication.rejected();
        }

        var incoming =
                ProjectPlayerIncomingDamageRuntime.applyProjectOwnedActorHit(
                        regalhart,
                        target,
                        rule.toIncomingHit(DATA.contentLevel())
                );
        return new ImpactApplication(incoming);
    }

    static boolean impactReadyAction(
            R01RegalhartEncounterData.ActionId action
    ) {
        if (action != R01RegalhartEncounterData.ActionId.ANTLER_SWEEP
                && action != R01RegalhartEncounterData.ActionId.ROYAL_BOUND) {
            return false;
        }
        var rule = DATA.rulesById().get(action);
        return rule != null && rule.impactContractClosed();
    }

    public record ImpactApplication(
            ProjectPlayerIncomingDamageRuntime.IncomingApplication incoming
    ) {
        public ImpactApplication {
            Objects.requireNonNull(incoming, "incoming");
        }

        public static ImpactApplication rejected() {
            return new ImpactApplication(
                    ProjectPlayerIncomingDamageRuntime
                            .IncomingApplication.rejected()
            );
        }
    }
}
