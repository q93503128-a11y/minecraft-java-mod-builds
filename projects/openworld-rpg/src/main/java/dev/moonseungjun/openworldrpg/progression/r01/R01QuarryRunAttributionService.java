package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.reward.PlayerRewardTransactionService;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/** Server-only persistence and reward boundary for R01 Quarry run class attribution. */
public final class R01QuarryRunAttributionService {
    private R01QuarryRunAttributionService() {
    }

    public static R01QuarryRunAttributionState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                R01QuarryRunAttributionAttachments.QUARRY_RUN_ATTRIBUTION,
                R01QuarryRunAttributionState.initial()
        );
    }

    public static R01QuarryRunAttributionState beginRun(
            ServerPlayer player,
            long runId
    ) {
        Objects.requireNonNull(player, "player");
        R01QuarryRunAttributionState current = state(player);
        R01QuarryRunAttributionState next = current.beginRun(runId);
        return replace(player, current, next);
    }

    public static boolean recordCurrentClassContribution(
            ServerPlayer player,
            R01QuarryRunContribution contribution
    ) {
        Objects.requireNonNull(player, "player");
        RootClass activeClass = PlayerProgressionService.state(player)
                .activeClass()
                .orElseThrow(() -> new IllegalStateException(
                        "Quarry contribution requires an active root class."
                ));
        return recordContribution(player, contribution, activeClass);
    }

    public static boolean recordContribution(
            ServerPlayer player,
            R01QuarryRunContribution contribution,
            RootClass owner
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(contribution, "contribution");
        Objects.requireNonNull(owner, "owner");

        R01PlayerState personal = R01PlayerStateService.state(player);
        if (personal.opening().mainStage() != R01MainStage.QUARRY_DUNGEON_ACTIVE
                || personal.quarry().runId() <= 0L
                || personal.quarry().runState().filter("active"::equals).isEmpty()) {
            return false;
        }

        R01QuarryRunAttributionState current = state(player);
        if (current.runId() != personal.quarry().runId()) {
            current = beginRun(player, personal.quarry().runId());
        }
        boolean already = current.contributionOwners().containsKey(contribution);
        R01QuarryRunAttributionState next = current.record(
                personal.quarry().runId(),
                contribution,
                owner
        );
        replace(player, current, next);
        return !already;
    }

    /**
     * Replays a class captured by a shared room controller after the room cleared. This is allowed
     * for the same personal run while it is still active or already marked cleared, so a brief
     * disconnect at room completion cannot erase a canonical Quarry attribution unit.
     */
    public static boolean ensureCapturedContribution(
            ServerPlayer player,
            long runId,
            R01QuarryRunContribution contribution,
            RootClass owner
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(contribution, "contribution");
        Objects.requireNonNull(owner, "owner");
        if (runId <= 0L) {
            throw new IllegalArgumentException("runId must be positive.");
        }

        R01PlayerState personal = R01PlayerStateService.state(player);
        boolean sameRun = personal.quarry().runId() == runId;
        boolean runEligible = personal.quarry().runState()
                .map(value -> "active".equals(value) || "cleared".equals(value))
                .orElse(false);
        if (!sameRun || !runEligible) {
            return false;
        }

        R01QuarryRunAttributionState current = state(player);
        if (current.runId() != runId) {
            current = beginRun(player, runId);
        }
        R01QuarryRunAttributionState next = current.record(
                runId,
                contribution,
                owner
        );
        replace(player, current, next);
        return true;
    }

    /**
     * Finalizes the canonical first-clear dungeon-completion numeric reward after personal
     * first-clear state has committed. The generic transaction persists exact percentages before
     * any domain mutation, so reconnect cannot reroll the current-requirement amounts.
     */
    public static boolean reconcileFirstClearCompletion(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        R01PlayerState personal = R01PlayerStateService.state(player);
        if (!personal.quarry().firstClear()
                || personal.quarry().runId() <= 0L
                || personal.quarry().runState().filter("cleared"::equals).isEmpty()) {
            return false;
        }

        R01QuarryRunAttributionState attribution = state(player);
        if (attribution.runId() != personal.quarry().runId()) {
            return false;
        }

        Optional<RootClass> completionClass = attribution.completionClass();
        if (completionClass.isEmpty()) {
            return false;
        }

        PlayerRewardTransactionService.RewardResult result =
                R01RewardService.grantEarthloongFirstDungeonCompletion(
                        player,
                        completionClass.orElseThrow()
                );
        return result.appliedNow();
    }

    private static R01QuarryRunAttributionState replace(
            ServerPlayer player,
            R01QuarryRunAttributionState current,
            R01QuarryRunAttributionState next
    ) {
        if (!current.equals(next)) {
            player.setAttached(
                    R01QuarryRunAttributionAttachments.QUARRY_RUN_ATTRIBUTION,
                    next
            );
        }
        return next;
    }
}
