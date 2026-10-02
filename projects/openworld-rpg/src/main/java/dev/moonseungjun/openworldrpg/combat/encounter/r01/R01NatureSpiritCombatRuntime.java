package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.R01ExternalActorCatalog;
import dev.moonseungjun.openworldrpg.integration.bootstrap.RuntimeProfile;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

/**
 * Runtime seam for canon-closed Nature Spirit combat state.
 *
 * <p>Production attacks remain presentation-gated. This class binds only Living Shell authority and
 * its exact damage/poise inputs so later attack presentation cannot silently reimplement those rules.
 * Donor natural spawns and raw verification fixtures are excluded.</p>
 */
public final class R01NatureSpiritCombatRuntime {
    private static final R01SecondaryCreatureEncounterData DATA =
            R01SecondaryCreatureEncounterDataLoader.loadNatureSpirit();
    private static final Map<UUID, R01NatureSpiritCombatRuntimeState> STATES =
            new ConcurrentHashMap<>();
    private static volatile boolean initialized;

    private R01NatureSpiritCombatRuntime() {
    }

    public static synchronized void initialize(
            RuntimeProfile profile,
            Logger logger
    ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(logger, "logger");
        if (initialized) {
            return;
        }
        initialized = true;

        if (profile == RuntimeProfile.CORE) {
            logger.info(
                    "Openworld RPG Nature Spirit Living Shell runtime inactive for core profile."
            );
            return;
        }

        ServerEntityEvents.ENTITY_UNLOAD.register(
                (entity, level) -> STATES.remove(entity.getUUID())
        );
        ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> STATES.clear()
        );
        logger.info(
                "Openworld RPG Nature Spirit Living Shell authority armed: "
                        + "{} tick duration, {} tick reuse, directTaken={}x, poiseTaken={}x, "
                        + "damageTrigger={} over {} ticks, poiseTrigger={}.",
                DATA.livingShell().durationTicks(),
                DATA.livingShell().reuseTicks(),
                DATA.livingShell().directDamageTakenMultiplier(),
                DATA.livingShell().poiseDamageTakenMultiplier(),
                DATA.livingShell().recentDamageTriggerFraction(),
                DATA.livingShell().recentDamageWindowTicks(),
                DATA.livingShell().poiseTriggerFraction()
        );
    }

    public static Optional<R01NatureSpiritActionController.Decision> selectAtDecision(
            LivingEntity natureSpirit,
            boolean rootedSwipeLegal,
            boolean earthenRamLegal,
            boolean bloomQuakeLegal,
            double targetDistance,
            long gameTick
    ) {
        R01NatureSpiritCombatRuntimeState state = stateFor(natureSpirit);
        if (state == null) {
            return Optional.empty();
        }
        var poise = ExternalActorBindingRuntime
                .poiseSnapshot(natureSpirit, gameTick)
                .orElse(null);
        var health = ExternalActorBindingRuntime
                .canonicalHealthSnapshot(natureSpirit)
                .orElse(null);
        if (poise == null || health == null) {
            return Optional.empty();
        }
        return Optional.of(
                state.selectAtDecision(
                        rootedSwipeLegal,
                        earthenRamLegal,
                        bloomQuakeLegal,
                        targetDistance,
                        poise.currentPoise(),
                        poise.maxPoise(),
                        health.maxHealth(),
                        gameTick
                )
        );
    }

    public static void recordPostMitigationHostileDamage(
            LivingEntity natureSpirit,
            double appliedDamage,
            long gameTick
    ) {
        R01NatureSpiritCombatRuntimeState state = stateFor(natureSpirit);
        if (state != null) {
            state.recordPostMitigationHostileDamage(
                    appliedDamage,
                    gameTick
            );
        }
    }

    public static double directDamageTakenMultiplier(
            LivingEntity natureSpirit,
            long gameTick
    ) {
        R01NatureSpiritCombatRuntimeState state = stateFor(natureSpirit);
        return state == null
                ? 1.0
                : state.directDamageTakenMultiplier(gameTick);
    }

    public static double poiseDamageTakenMultiplier(
            LivingEntity natureSpirit,
            long gameTick
    ) {
        R01NatureSpiritCombatRuntimeState state = stateFor(natureSpirit);
        return state == null
                ? 1.0
                : state.poiseDamageTakenMultiplier(gameTick);
    }

    public static boolean onPoiseBroken(
            LivingEntity natureSpirit,
            long gameTick
    ) {
        R01NatureSpiritCombatRuntimeState state = stateFor(natureSpirit);
        return state != null && state.onPoiseBroken(gameTick);
    }

    private static R01NatureSpiritCombatRuntimeState stateFor(
            LivingEntity entity
    ) {
        if (!isAcceptedAuthoredNatureSpirit(entity)) {
            return null;
        }
        return STATES.computeIfAbsent(
                entity.getUUID(),
                ignored -> new R01NatureSpiritCombatRuntimeState(
                        "r01-nature-spirit:"
                                + entity.level().dimension()
                                + ":"
                                + entity.getUUID(),
                        entity.getUUID()
                )
        );
    }

    private static boolean isAcceptedAuthoredNatureSpirit(
            LivingEntity entity
    ) {
        if (entity == null
                || entity.level().isClientSide()
                || entity.isRemoved()
                || !ExternalActorBindingRuntime.isAuthoredSpawn(entity)) {
            return false;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(
                entity.getType()
        );
        return id != null
                && R01ExternalActorCatalog.NATURE_SPIRIT
                        .equals(id.toString());
    }
}
