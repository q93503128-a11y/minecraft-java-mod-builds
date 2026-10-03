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
 * Server authority for a presentation-confirmed R01 Nature Spirit impact.
 *
 * <p>This class deliberately does not decide animation timing, movement, frontal-arc geometry or
 * Bloom Quake's visible radius. A later presentation binder must confirm contact first. Once that
 * happens, damage/guard rules and authored player-poise pressure come only from the bundled R01
 * encounter contract rather than donor damage values.</p>
 */
public final class R01NatureSpiritImpactAuthority {
    private static final R01SecondaryCreatureEncounterData DATA =
            R01SecondaryCreatureEncounterDataLoader.loadNatureSpirit();

    private R01NatureSpiritImpactAuthority() {
    }

    public static ImpactApplication applyConfirmedContact(
            LivingEntity natureSpirit,
            ServerPlayer target,
            R01SecondaryCreatureEncounterData.ActionId action
    ) {
        Objects.requireNonNull(natureSpirit, "natureSpirit");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(action, "action");

        var profile = ExternalActorBindingRuntime
                .combatProfile(natureSpirit)
                .orElse(null);
        if (profile == null
                || !R01ExternalActorCatalog.NATURE_SPIRIT
                        .equals(profile.entityId())
                || profile.contentLevel() != DATA.contentLevel()
                || !ExternalActorBindingRuntime
                        .isAuthoredSpawn(natureSpirit)) {
            return ImpactApplication.rejected();
        }

        R01SecondaryCreatureEncounterData.ActionRule rule =
                DATA.rulesById().get(action);
        if (rule == null) {
            return ImpactApplication.rejected();
        }

        ProjectPlayerIncomingDamageRuntime.IncomingApplication incoming =
                ProjectPlayerIncomingDamageRuntime
                        .applyProjectOwnedActorHit(
                                natureSpirit,
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

    static Optional<R01SecondaryCreatureEncounterData.ActionRule> rule(
            R01SecondaryCreatureEncounterData.ActionId action
    ) {
        Objects.requireNonNull(action, "action");
        return Optional.ofNullable(DATA.rulesById().get(action));
    }

    static boolean isNatureSpiritAction(
            R01SecondaryCreatureEncounterData.ActionId action
    ) {
        return rule(action).isPresent();
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
                        "Rejected Nature Spirit impact cannot apply player-poise pressure."
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
