package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerIncomingDamageRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Converts a confirmed authored Earthloong contact into the common player incoming-damage runtime.
 *
 * <p>The donor entity/animation may eventually report a visual hit frame, but it never supplies
 * damage. Only attack ids with canon-closed impact data are accepted here.</p>
 */
public final class R01EarthloongImpactAuthority {
    public static final String EARTHLOONG_ENTITY_ID = "threateningly_mobs:the_earthloong";
    private static final R01EarthloongEncounterData DATA =
            R01EarthloongEncounterDataLoader.loadBundled();

    private R01EarthloongImpactAuthority() {
    }

    public static ProjectPlayerIncomingDamageRuntime.IncomingApplication applyConfirmedContact(
            LivingEntity earthloong,
            ServerPlayer target,
            R01EarthloongEncounterData.ActionId action
    ) {
        Objects.requireNonNull(earthloong, "earthloong");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(action, "action");

        var profile = ExternalActorBindingRuntime.combatProfile(earthloong);
        if (profile.isEmpty()
                || !EARTHLOONG_ENTITY_ID.equals(profile.orElseThrow().entityId())
                || profile.orElseThrow().contentLevel() != DATA.contentLevel()) {
            return ProjectPlayerIncomingDamageRuntime.IncomingApplication.rejected();
        }

        var impact = DATA.impactRulesById().get(action);
        if (impact == null) {
            return ProjectPlayerIncomingDamageRuntime.IncomingApplication.rejected();
        }

        return ProjectPlayerIncomingDamageRuntime.applyProjectOwnedActorHit(
                earthloong,
                target,
                impact.toIncomingHit(DATA.contentLevel())
        );
    }

    static R01EarthloongEncounterData data() {
        return DATA;
    }
}
