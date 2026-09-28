package dev.moonseungjun.openworldrpg.fishing;

import dev.moonseungjun.openworldrpg.gathering.R01GatheringRules;
import dev.moonseungjun.openworldrpg.gathering.R01GatheringService;
import dev.moonseungjun.openworldrpg.time.PlayerActiveWorldTimeService;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-only R01 fishing transaction boundary.
 *
 * <p>Live entry requires a production fishing spot. Final fish item/name/model materialization is
 * intentionally not performed here until the asset-binding gate accepts those player-facing files.</p>
 */
public final class R01FishingService {
    private R01FishingService() {
    }

    public static R01FishingState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                R01FishingAttachments.FISHING,
                R01FishingState.initial()
        );
    }

    public static PrepareResult prepareBiteCandidate(
            ServerPlayer player,
            long worldSeed,
            String spotId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(spotId, "spotId");

        var binding = R01FishingSpatialRegistry.productionSpot(spotId);
        if (binding.isEmpty()) {
            return new PrepareResult(
                    PrepareStatus.SPOT_NOT_PRODUCTION,
                    Optional.empty()
            );
        }

        long activeTicks =
                PlayerActiveWorldTimeService.state(player).activeTicks();
        R01FishingRules.SpotTier tier = binding.orElseThrow().tier();
        R01FishingState current = state(player);
        if (!current.isSpotAvailable(spotId, tier, activeTicks)) {
            return new PrepareResult(
                    PrepareStatus.SPOT_DEPLETED_OR_HOOKED,
                    Optional.empty()
            );
        }

        int masteryRank = R01GatheringService.state(player).masteryRank(
                R01GatheringRules.GatheringDiscipline.FISHING
        );
        R01FishingState.PrepareResult prepared = current.prepareCandidate(
                worldSeed,
                player.getUUID().toString(),
                spotId,
                tier,
                activeTicks,
                masteryRank
        );
        replace(player, prepared.state());
        return new PrepareResult(
                PrepareStatus.READY,
                Optional.of(prepared.pending())
        );
    }

    public static boolean missHook(ServerPlayer player, String spotId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(spotId, "spotId");
        R01FishingState current = state(player);
        if (current.pendingCandidate(spotId).isEmpty()) {
            return false;
        }
        replace(player, current.missHook(spotId));
        return true;
    }

    public static boolean commitHook(ServerPlayer player, String spotId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(spotId, "spotId");
        var authored = R01FishingSpatialRegistry.spot(spotId);
        if (authored.isEmpty()) {
            return false;
        }
        R01FishingState current = state(player);
        if (current.pendingCandidate(spotId).isEmpty()) {
            return false;
        }
        long activeTicks =
                PlayerActiveWorldTimeService.state(player).activeTicks();
        replace(
                player,
                current.commitHook(
                        spotId,
                        authored.orElseThrow().tier(),
                        activeTicks
                )
        );
        return true;
    }

    public static boolean failHookedAttempt(
            ServerPlayer player,
            String spotId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(spotId, "spotId");
        var pending = state(player).pendingCandidate(spotId);
        if (pending.isEmpty() || !pending.orElseThrow().hooked()) {
            return false;
        }
        replace(player, state(player).resolveHookFailure(spotId));
        return true;
    }

    public static Optional<R01FishingState.PendingReward>
    resolveSuccessfulCatch(
            ServerPlayer player,
            String spotId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(spotId, "spotId");
        var pending = state(player).pendingCandidate(spotId);
        if (pending.isEmpty() || !pending.orElseThrow().hooked()) {
            return Optional.empty();
        }

        R01FishingState.CatchResolution resolved =
                state(player).resolveCatchSuccess(spotId);
        replace(player, resolved.state());

        /*
         * Do not grant an item yet. The pending reward is the reconnect-safe semantic catch plan
         * that the accepted fish model/name/icon materializer will later consume atomically.
         */
        return resolved.reward();
    }

    public static void reconcileInterruptedHooks(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        replace(player, state(player).reconcileInterruptedHooks());
    }

    public static R01FishingState clearDeliveredReward(
            ServerPlayer player,
            String transactionId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(transactionId, "transactionId");
        return replace(
                player,
                state(player).clearPendingReward(transactionId)
        );
    }

    private static R01FishingState replace(
            ServerPlayer player,
            R01FishingState next
    ) {
        R01FishingState current = state(player);
        if (current.equals(next)) {
            return current;
        }
        player.setAttached(R01FishingAttachments.FISHING, next);
        return next;
    }

    public enum PrepareStatus {
        READY,
        SPOT_NOT_PRODUCTION,
        SPOT_DEPLETED_OR_HOOKED
    }

    public record PrepareResult(
            PrepareStatus status,
            Optional<R01FishingState.PendingCandidate> candidate
    ) {
        public PrepareResult {
            Objects.requireNonNull(status, "status");
            candidate = Objects.requireNonNull(candidate, "candidate");
            if ((status == PrepareStatus.READY) != candidate.isPresent()) {
                throw new IllegalArgumentException(
                        "Fishing prepare status/candidate mismatch."
                );
            }
        }
    }
}
