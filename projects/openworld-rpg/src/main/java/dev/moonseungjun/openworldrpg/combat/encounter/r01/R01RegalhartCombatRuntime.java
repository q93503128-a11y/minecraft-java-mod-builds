package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.R01ExternalActorCatalog;
import dev.moonseungjun.openworldrpg.integration.bootstrap.RuntimeProfile;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;

/**
 * Runtime seam for currently closed Regalhart impact pieces.
 *
 * <p>This does not admit Regalhart production spawning. It provides exact single/mirrored Antler
 * Sweep timing and closed impact results for a later accepted movement/presentation binder.</p>
 */
public final class R01RegalhartCombatRuntime {
    private static final R01RegalhartEncounterData DATA =
            R01RegalhartEncounterDataLoader.load();
    private static final Map<UUID, R01RegalhartSweepExecutionState> SWEEPS =
            new ConcurrentHashMap<>();
    private static final Map<UUID, SovereignBinding> SOVEREIGN =
            new ConcurrentHashMap<>();
    private static final Identifier SOVEREIGN_MOVEMENT_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(
                    OpenworldRpgMod.MOD_ID,
                    "regalhart_sovereign_movement"
            );
    private static volatile boolean initialized;

    private R01RegalhartCombatRuntime() {
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
                    "Openworld RPG Regalhart closed-impact runtime inactive for core profile."
            );
            return;
        }
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            SWEEPS.remove(entity.getUUID());
            if (entity instanceof LivingEntity living) {
                clearSovereignState(living);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            for (SovereignBinding binding : SOVEREIGN.values()) {
                removeSovereignMovementModifier(binding.regalhart);
            }
            SOVEREIGN.clear();
            SWEEPS.clear();
        });
        logger.info(
                "Openworld RPG Regalhart authority armed for scheduled Antler Sweep, "
                        + "presentation-confirmed impact results, and Sovereign transition modifiers."
        );
    }

    public static boolean beginScheduledAntlerSweep(
            LivingEntity regalhart,
            R01RegalhartActionController.Decision decision,
            long gameTick
    ) {
        var state = stateFor(regalhart);
        return state != null && state.begin(decision, gameTick);
    }

    public static boolean beginNonComboAntlerSweep(
            LivingEntity regalhart,
            R01RegalhartActionController.Decision decision,
            long gameTick
    ) {
        if (decision == null || decision.mirroredSweepFollowUp()) {
            return false;
        }
        return beginScheduledAntlerSweep(regalhart, decision, gameTick);
    }

    public static Optional<R01RegalhartSweepExecutionState.Snapshot> sweepSnapshot(
            LivingEntity regalhart,
            long gameTick
    ) {
        var state = stateFor(regalhart);
        return state == null
                ? Optional.empty()
                : state.snapshot(gameTick);
    }

    public static R01RegalhartImpactAuthority.ImpactApplication confirmScheduledAntlerSweep(
            LivingEntity regalhart,
            ServerPlayer target,
            long actionCounter,
            long gameTick
    ) {
        var state = stateFor(regalhart);
        if (state == null
                || target == null
                || target.level() != regalhart.level()) {
            return R01RegalhartImpactAuthority
                    .ImpactApplication.rejected();
        }
        var action = state.confirmContact(
                actionCounter,
                target.getUUID(),
                gameTick
        );
        if (action.isEmpty()) {
            return R01RegalhartImpactAuthority
                    .ImpactApplication.rejected();
        }
        return R01RegalhartImpactAuthority.applyConfirmedContact(
                regalhart,
                target,
                action.orElseThrow()
        );
    }

    /**
     * Resolves only the already-canonical 4.5-block Royal Bound landing area after a future physical
     * leap binder confirms the actual landing frame. No minimum tell is converted into a fake exact
     * landing time here.
     */
    public static AreaResolution resolveRoyalBoundLanding(
            LivingEntity regalhart,
            Collection<? extends ServerPlayer> candidatePlayers
    ) {
        Objects.requireNonNull(candidatePlayers, "candidatePlayers");
        if (!isAcceptedAuthoredRegalhart(regalhart)) {
            return AreaResolution.rejected();
        }
        var rule = DATA.rulesById().get(
                R01RegalhartEncounterData.ActionId.ROYAL_BOUND
        );
        if (rule == null || !rule.impactContractClosed()) {
            return AreaResolution.rejected();
        }

        int insideArea = 0;
        int accepted = 0;
        for (ServerPlayer player : candidatePlayers) {
            if (player == null
                    || !player.isAlive()
                    || player.isSpectator()
                    || player.level() != regalhart.level()
                    || !insideHorizontalRadius(
                            regalhart.getX(),
                            regalhart.getZ(),
                            player.getX(),
                            player.getZ(),
                            rule.areaRadius()
                    )) {
                continue;
            }
            insideArea++;
            var application =
                    R01RegalhartImpactAuthority.applyConfirmedContact(
                            regalhart,
                            player,
                            R01RegalhartEncounterData.ActionId.ROYAL_BOUND
                    );
            if (application.incoming().accepted()) {
                accepted++;
            }
        }
        return new AreaResolution(
                true,
                rule.areaRadius(),
                insideArea,
                accepted
        );
    }


    /**
     * Starts the one-time Sovereign transition only from the exact server-owned selection decision.
     * The transition state is shared with the central incoming-damage path; movement is applied only
     * after the authored 30-tick transition completes.
     */
    public static boolean beginSovereignTransition(
            LivingEntity regalhart,
            R01RegalhartActionController.Decision decision,
            long gameTick
    ) {
        if (!isAcceptedAuthoredRegalhart(regalhart)
                || decision == null
                || regalhart.getAttribute(Attributes.MOVEMENT_SPEED) == null) {
            return false;
        }

        R01RegalhartSovereignExecutionState state =
                new R01RegalhartSovereignExecutionState(DATA.sovereign());
        if (!state.begin(decision, gameTick)) {
            return false;
        }

        SovereignBinding binding = new SovereignBinding(regalhart, state);
        if (SOVEREIGN.putIfAbsent(regalhart.getUUID(), binding) != null) {
            return false;
        }

        removeSovereignMovementModifier(regalhart);
        return true;
    }

    /**
     * Central project-damage multiplier for the authored Sovereign transition. Outside the exact
     * transition window, including after the transition completes, damage remains unmodified.
     */
    public static double incomingDamageMultiplier(
            LivingEntity regalhart,
            long gameTick
    ) {
        if (!isAcceptedAuthoredRegalhart(regalhart)) {
            return 1.0;
        }

        SovereignBinding binding = SOVEREIGN.get(regalhart.getUUID());
        return binding == null
                ? 1.0
                : binding.state.incomingDamageMultiplier(gameTick);
    }

    /**
     * Applies the authored +10% Sovereign movement modifier exactly once after transition completion.
     */
    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        for (Map.Entry<UUID, SovereignBinding> entry : SOVEREIGN.entrySet()) {
            SovereignBinding binding = entry.getValue();
            LivingEntity regalhart = binding.regalhart;
            if (regalhart.isRemoved() || regalhart.level().isClientSide()) {
                SOVEREIGN.remove(entry.getKey(), binding);
                continue;
            }

            long gameTick = regalhart.level().getGameTime();
            if (!binding.movementApplied
                    && binding.state.movementSpeedMultiplier(gameTick) > 1.0) {
                if (!applySovereignMovementModifier(regalhart)) {
                    clearSovereignState(regalhart);
                    continue;
                }
                binding.movementApplied = true;
            }
        }
    }

    /**
     * Encounter-reset seam for the later Regalhart territory controller. Reset removes both the
     * transient modifier and the one-time transition state rather than leaving a stale phase behind.
     */
    public static void clearSovereignState(LivingEntity regalhart) {
        if (regalhart == null) {
            return;
        }

        SovereignBinding removed = SOVEREIGN.remove(regalhart.getUUID());
        if (removed != null) {
            removeSovereignMovementModifier(removed.regalhart);
        } else {
            removeSovereignMovementModifier(regalhart);
        }
    }

    /**
     * Clears all transient Regalhart encounter execution state owned by this runtime.
     */
    public static void resetEncounterState(LivingEntity regalhart) {
        if (regalhart == null) {
            return;
        }
        SWEEPS.remove(regalhart.getUUID());
        clearSovereignState(regalhart);
    }

    private static boolean applySovereignMovementModifier(
            LivingEntity regalhart
    ) {
        var movement = regalhart.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            return false;
        }

        movement.addOrUpdateTransientModifier(
                new AttributeModifier(
                        SOVEREIGN_MOVEMENT_MODIFIER_ID,
                        DATA.sovereign().movementSpeedMultiplier() - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
        );
        return true;
    }

    private static void removeSovereignMovementModifier(
            LivingEntity regalhart
    ) {
        var movement = regalhart.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(SOVEREIGN_MOVEMENT_MODIFIER_ID);
        }
    }

    static boolean insideHorizontalRadius(
            double centerX,
            double centerZ,
            double targetX,
            double targetZ,
            double radius
    ) {
        if (!Double.isFinite(centerX)
                || !Double.isFinite(centerZ)
                || !Double.isFinite(targetX)
                || !Double.isFinite(targetZ)
                || !Double.isFinite(radius)
                || radius < 0.0) {
            throw new IllegalArgumentException(
                    "Regalhart area inputs must be finite and radius non-negative."
            );
        }
        double dx = targetX - centerX;
        double dz = targetZ - centerZ;
        return dx * dx + dz * dz <= radius * radius;
    }

    private static R01RegalhartSweepExecutionState stateFor(
            LivingEntity entity
    ) {
        if (!isAcceptedAuthoredRegalhart(entity)) {
            return null;
        }
        return SWEEPS.computeIfAbsent(
                entity.getUUID(),
                ignored -> new R01RegalhartSweepExecutionState(DATA)
        );
    }

    private static boolean isAcceptedAuthoredRegalhart(
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
                && R01ExternalActorCatalog.REGALHART
                        .equals(id.toString());
    }

    private static final class SovereignBinding {
        private final LivingEntity regalhart;
        private final R01RegalhartSovereignExecutionState state;
        private boolean movementApplied;

        private SovereignBinding(
                LivingEntity regalhart,
                R01RegalhartSovereignExecutionState state
        ) {
            this.regalhart = Objects.requireNonNull(regalhart, "regalhart");
            this.state = Objects.requireNonNull(state, "state");
        }
    }

    public record AreaResolution(
            boolean acceptedLanding,
            double radius,
            int playersInsideArea,
            int acceptedImpacts
    ) {
        public AreaResolution {
            if (!Double.isFinite(radius)
                    || radius < 0.0
                    || playersInsideArea < 0
                    || acceptedImpacts < 0
                    || acceptedImpacts > playersInsideArea) {
                throw new IllegalArgumentException(
                        "Invalid Regalhart Royal Bound area resolution."
                );
            }
            if (!acceptedLanding
                    && (radius != 0.0
                            || playersInsideArea != 0
                            || acceptedImpacts != 0)) {
                throw new IllegalArgumentException(
                        "Rejected Royal Bound landing cannot carry resolved state."
                );
            }
        }

        public static AreaResolution rejected() {
            return new AreaResolution(false, 0.0, 0, 0);
        }
    }
}
