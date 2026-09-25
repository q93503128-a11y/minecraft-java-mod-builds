package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.time.PlayerActiveWorldTimeService;
import dev.moonseungjun.openworldrpg.time.PlayerActiveWorldTimeState;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared non-spatial Roadside Trouble lifecycle.
 *
 * <p>Spatial binding owns the authored event volume and physical actor spawning. This controller
 * owns persistent cycle/timing/participation/completion semantics only.</p>
 */
public final class R01RoadsideTroubleController {
    private static final int PERSIST_INTERVAL_TICKS = 20;

    private R01RoadsideTroubleController() {
    }

    public static R01SharedWorldState state(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        ServerLevel overworld = server.overworld();
        return overworld.getAttachedOrSet(
                R01SharedWorldAttachments.SHARED_R01,
                R01SharedWorldState.initial()
        );
    }

    public static void tickActiveWorld(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        if (Math.floorMod(server.getTickCount(), PERSIST_INTERVAL_TICKS) != 0) {
            return;
        }

        R01SharedWorldState next = state(server).advanceActiveWorld(PERSIST_INTERVAL_TICKS);
        if (next.roadsideShouldAbandon()) {
            next = next.abandonRoadside();
        }
        replace(server, next);
    }

    /**
     * Tries to start the shared event after a server-authored volume entry/recheck.
     */
    public static StartResult tryStart(
            ServerPlayer trigger,
            boolean insideAuthoredVolume,
            int activeParticipantCount
    ) {
        Objects.requireNonNull(trigger, "trigger");
        MinecraftServer server = trigger.getServer();
        if (server == null) {
            throw new IllegalStateException("Roadside Trouble requires a live server.");
        }

        R01SharedWorldState shared = state(server);
        if (shared.roadsideActive()) {
            return new StartResult(false, shared.roadsideCycle(), 0);
        }

        boolean eligible;
        if (shared.roadsideCycle() == 0L) {
            R01PlayerState personal = R01PlayerStateService.state(trigger);
            PlayerActiveWorldTimeState activeTime =
                    PlayerActiveWorldTimeService.state(trigger);
            var epoch = activeTime.epoch(
                    PlayerActiveWorldTimeService.ALDERFORD_SHRINE_EPOCH
            );
            long elapsed = epoch.isPresent()
                    ? activeTime.activeTicks() - epoch.getAsLong()
                    : 0L;

            eligible = epoch.isPresent()
                    && R01RoadsideTroubleRules.firstCycleEligible(
                            personal.opening().firstShrineActivated(),
                            elapsed,
                            false,
                            insideAuthoredVolume
                    );
        } else {
            long elapsed = shared.activeWorldTicks()
                    - shared.roadsideLastEndActiveTicks();
            eligible = R01RoadsideTroubleRules.repeatCycleEligible(
                    elapsed,
                    false,
                    insideAuthoredVolume
            );
        }

        if (!eligible) {
            return new StartResult(false, shared.roadsideCycle(), 0);
        }

        R01SharedWorldState started = shared.startRoadside();
        replace(server, started);
        return new StartResult(
                true,
                started.roadsideCycle(),
                R01RoadsideTroubleRules.threatCount(activeParticipantCount)
        );
    }

    /**
     * One valid combat/support/objective action establishes reward participation for this cycle.
     */
    public static boolean recordParticipation(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        MinecraftServer server = player.getServer();
        if (server == null) {
            throw new IllegalStateException("Roadside Trouble requires a live server.");
        }

        R01SharedWorldState shared = state(server);
        if (!shared.roadsideActive()) {
            throw new IllegalStateException("Roadside Trouble is not active.");
        }

        String uuid = player.getUUID().toString();
        boolean already = shared.roadsideParticipants().containsKey(uuid);
        R01PlayerState personal = R01PlayerStateService.state(player);
        boolean dustEligible = personal.opening().mainStage()
                .isAtLeast(R01MainStage.QUARRY_ROAD_ACTIVE)
                && !personal.opening().mainStage()
                .isAtLeast(R01MainStage.QUARRY_ROAD_COMPLETE);

        R01SharedWorldState next = shared.recordRoadsideParticipation(
                uuid,
                dustEligible
        );
        replace(server, next);
        R01PlayerStateService.markRoadsideEventParticipation(
                player,
                next.roadsideCycle()
        );
        return !already;
    }

    public static RequirementResult markRequirement(
            MinecraftServer server,
            R01SharedWorldState.RoadsideRequirement requirement
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(requirement, "requirement");

        R01SharedWorldState before = state(server);
        R01SharedWorldState next = before.markRoadsideRequirement(requirement);
        boolean completedNow = false;
        if (next.roadsideRequirementsComplete()) {
            next = next.completeRoadside();
            completedNow = true;
        }
        replace(server, next);

        if (completedNow) {
            reconcileOnlineFinalizations(server);
        }

        return new RequirementResult(
                next.roadsideCycle(),
                completedNow,
                next.roadsideActive()
        );
    }

    /**
     * Called by the later spatial-volume adapter; no coordinate search is performed here.
     */
    public static boolean updatePresence(
            MinecraftServer server,
            boolean anyIncompleteOrParticipatingPlayerInside
    ) {
        Objects.requireNonNull(server, "server");
        R01SharedWorldState current = state(server);
        R01SharedWorldState next = current.updateRoadsidePresence(
                anyIncompleteOrParticipatingPlayerInside
        );
        if (next.roadsideShouldAbandon()) {
            next = next.abandonRoadside();
        }
        replace(server, next);
        return current.roadsideActive() && !next.roadsideActive();
    }

    /**
     * Finalizes personal event metadata and Dust category after completion, including reconnect.
     *
     * <p>The repeatable Roadside Trouble EXP/Class XP/Gold reward is intentionally not paid here
     * yet; its Class-XP owner is the class active during the qualifying contribution and that
     * attribution is bound in the next reward layer rather than guessed from current class.</p>
     */
    public static boolean reconcilePendingFinalization(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        String uuid = player.getUUID().toString();
        R01SharedWorldState shared = state(server);
        R01SharedWorldState.ParticipantSnapshot pending =
                shared.pendingRoadsideFinalization(uuid);
        if (pending == null) {
            return false;
        }

        R01PlayerStateService.markRoadsideEventEnded(
                player,
                pending.cycle(),
                shared.roadsideLastEndActiveTicks()
        );

        if (pending.dustQuestEligible()) {
            R01PlayerState personal = R01PlayerStateService.state(player);
            if (personal.opening().mainStage()
                    .isAtLeast(R01MainStage.QUARRY_ROAD_ACTIVE)
                    && !personal.opening().mainStage()
                    .isAtLeast(R01MainStage.QUARRY_ROAD_COMPLETE)) {
                R01WorldActionService.recordRoadsideTroubleQuestCredit(player);
            }
        }

        replace(
                server,
                state(server).clearPendingRoadsideFinalization(
                        uuid,
                        pending.cycle()
                )
        );
        return true;
    }

    private static void reconcileOnlineFinalizations(MinecraftServer server) {
        for (String uuidText
                : new ArrayList<>(state(server).pendingRoadsideFinalizations().keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(uuidText));
            if (player != null) {
                reconcilePendingFinalization(player);
            }
        }
    }

    private static R01SharedWorldState replace(
            MinecraftServer server,
            R01SharedWorldState next
    ) {
        ServerLevel overworld = server.overworld();
        R01SharedWorldState current = state(server);
        if (current.equals(next)) {
            return current;
        }
        overworld.setAttached(R01SharedWorldAttachments.SHARED_R01, next);
        return next;
    }

    public record StartResult(
            boolean started,
            long cycle,
            int threatCount
    ) {
        public StartResult {
            if (cycle < 0L || threatCount < 0) {
                throw new IllegalArgumentException("Invalid Roadside Trouble start result.");
            }
            if (!started && threatCount != 0) {
                throw new IllegalArgumentException(
                        "Non-started Roadside Trouble cannot publish threat count."
                );
            }
        }
    }

    public record RequirementResult(
            long cycle,
            boolean completedNow,
            boolean stillActive
    ) {
        public RequirementResult {
            if (cycle <= 0L) {
                throw new IllegalArgumentException("Active event cycle must be positive.");
            }
            if (completedNow && stillActive) {
                throw new IllegalArgumentException(
                        "Completed Roadside Trouble cannot remain active."
                );
            }
        }
    }
}
