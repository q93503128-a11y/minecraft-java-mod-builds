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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

/**
 * Runtime seam for Steelboar melee timing and impact authority.
 *
 * <p>The runtime is deliberately not a production spawn admission. It activates only for an exact
 * authored Steelboar and only after a caller supplies a controller decision for Iron Tusk or
 * Shoulder Hook.</p>
 */
public final class R01SteelboarCombatRuntime {
    private static final R01SteelboarEncounterData DATA =
            R01SteelboarEncounterDataLoader.load();
    private static final Map<UUID, R01SteelboarMeleeExecutionState> STATES =
            new ConcurrentHashMap<>();
    private static volatile boolean initialized;

    private R01SteelboarCombatRuntime() {
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
                    "Openworld RPG Steelboar melee runtime inactive for core profile."
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
                "Openworld RPG Steelboar melee authority armed for Iron Tusk and Shoulder Hook; "
                        + "Iron Rush remains impact-gated."
        );
    }

    public static boolean beginClosedMelee(
            LivingEntity steelboar,
            R01SteelboarActionController.Decision decision,
            long gameTick
    ) {
        R01SteelboarMeleeExecutionState state = stateFor(steelboar);
        return state != null && state.begin(decision, gameTick);
    }

    public static Optional<R01SteelboarMeleeExecutionState.Snapshot> executionSnapshot(
            LivingEntity steelboar,
            long gameTick
    ) {
        R01SteelboarMeleeExecutionState state = stateFor(steelboar);
        return state == null
                ? Optional.empty()
                : state.snapshot(gameTick);
    }

    public static R01SteelboarImpactAuthority.ImpactApplication confirmScheduledMeleeImpact(
            LivingEntity steelboar,
            ServerPlayer target,
            long actionCounter,
            long gameTick
    ) {
        R01SteelboarMeleeExecutionState state = stateFor(steelboar);
        if (state == null
                || target == null
                || target.level() != steelboar.level()) {
            return R01SteelboarImpactAuthority
                    .ImpactApplication.rejected();
        }

        var action = state.confirmContact(
                actionCounter,
                target.getUUID(),
                gameTick
        );
        if (action.isEmpty()) {
            return R01SteelboarImpactAuthority
                    .ImpactApplication.rejected();
        }
        return R01SteelboarImpactAuthority
                .applyConfirmedMeleeContact(
                        steelboar,
                        target,
                        action.orElseThrow()
                );
    }

    private static R01SteelboarMeleeExecutionState stateFor(
            LivingEntity entity
    ) {
        if (!isAcceptedAuthoredSteelboar(entity)) {
            return null;
        }
        return STATES.computeIfAbsent(
                entity.getUUID(),
                ignored -> new R01SteelboarMeleeExecutionState(DATA)
        );
    }

    private static boolean isAcceptedAuthoredSteelboar(
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
                && R01ExternalActorCatalog.STEELBOAR
                        .equals(id.toString());
    }
}
