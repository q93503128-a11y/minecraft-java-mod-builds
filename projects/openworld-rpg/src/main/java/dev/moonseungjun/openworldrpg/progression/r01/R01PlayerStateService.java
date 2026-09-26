package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.time.PlayerActiveWorldTimeService;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-only persistence boundary for personal R01 progression.
 *
 * <p>World actors, shared encounter controllers and reward delivery remain separate authorities.
 * This service stores only committed personal state and does not infer completion from client UI.</p>
 */
public final class R01PlayerStateService {
    private R01PlayerStateService() {
    }

    public static R01PlayerState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                R01PlayerStateAttachments.R01_PLAYER_STATE,
                R01PlayerState.initial()
        );
    }

    public static R01PlayerState markOpeningLoadoutClaimed(ServerPlayer player) {
        return replace(player, state(player).markOpeningLoadoutClaimed(gameTick(player)));
    }

    public static R01PlayerState markFirstShrineActivated(ServerPlayer player) {
        R01PlayerState next = replace(
                player,
                state(player).markFirstShrineActivated(gameTick(player))
        );
        PlayerActiveWorldTimeService.ensureAlderfordShrineEpoch(player);
        return next;
    }

    public static void reconcileActiveTimeEpochs(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (state(player).opening().firstShrineActivated()) {
            PlayerActiveWorldTimeService.ensureAlderfordShrineEpoch(player);
        }
    }

    public static R01PlayerState markFirstRootClassSelected(ServerPlayer player) {
        return replace(player, state(player).markFirstRootClassSelected(gameTick(player)));
    }

    public static R01PlayerState markStarterPackageClaimed(ServerPlayer player) {
        return replace(player, state(player).markStarterPackageClaimed(gameTick(player)));
    }

    public static R01PlayerState markDodgeHintSeen(ServerPlayer player) {
        return replace(player, state(player).markDodgeHintSeen(gameTick(player)));
    }

    public static R01PlayerState markDodgeUsedOnce(ServerPlayer player) {
        return replace(player, state(player).markDodgeUsedOnce(gameTick(player)));
    }

    public static R01PlayerState recordQuarryRoadAction(
            ServerPlayer player,
            R01PlayerState.QuarryRoadAction action
    ) {
        return replace(player, state(player).recordQuarryRoadAction(action, gameTick(player)));
    }

    public static R01PlayerState markRoadsideEventParticipation(
            ServerPlayer player,
            long cycle
    ) {
        return replace(
                player,
                state(player).markRoadsideEventParticipation(cycle, gameTick(player))
        );
    }

    public static R01PlayerState markRoadsideEventEnded(
            ServerPlayer player,
            long cycle,
            long sharedActiveWorldTime
    ) {
        return replace(
                player,
                state(player).markRoadsideEventEnded(
                        cycle,
                        sharedActiveWorldTime,
                        gameTick(player)
                )
        );
    }

    public static R01PlayerState markRegalhartClueSeen(
            ServerPlayer player,
            R01PlayerState.RegalhartClue clue
    ) {
        return replace(player, state(player).markRegalhartClueSeen(clue, gameTick(player)));
    }

    public static R01PlayerState markRegalhartDiscovered(ServerPlayer player) {
        return replace(player, state(player).markRegalhartDiscovered(gameTick(player)));
    }

    public static R01PlayerState markQuarryDiscovered(ServerPlayer player) {
        return replace(player, state(player).markQuarryDiscovered(gameTick(player)));
    }

    public static R01PlayerState markQuarryWaystoneDiscovered(ServerPlayer player) {
        return replace(player, state(player).markQuarryWaystoneDiscovered(gameTick(player)));
    }

    public static R01PlayerState markQuarryWaystoneActivated(ServerPlayer player) {
        return replace(player, state(player).markQuarryWaystoneActivated(gameTick(player)));
    }

    public static R01PlayerState beginQuarryRun(ServerPlayer player, long runId) {
        return replace(player, state(player).beginQuarryRun(runId, gameTick(player)));
    }

    public static R01PlayerState markQuarryLiftOpen(ServerPlayer player) {
        return replace(player, state(player).markQuarryLiftOpen(gameTick(player)));
    }

    public static R01PlayerState markQuarryRelayEvidenceSeen(ServerPlayer player) {
        return replace(player, state(player).markQuarryRelayEvidenceSeen(gameTick(player)));
    }

    public static R01PlayerState markEarthloongFirstClear(ServerPlayer player) {
        return replace(player, state(player).markEarthloongFirstClear(gameTick(player)));
    }

    public static R01PlayerState reconcileEarthloongFirstClearClaims(ServerPlayer player) {
        return replace(
                player,
                state(player).reconcileEarthloongFirstClearClaims(gameTick(player))
        );
    }

    public static R01PlayerState commitEarthloongRewardChoice(
            ServerPlayer player,
            String choiceFlag
    ) {
        return replace(
                player,
                state(player).commitEarthloongRewardChoice(choiceFlag, gameTick(player))
        );
    }

    public static R01PlayerState markEarthloongRewardChoiceDelivered(
            ServerPlayer player,
            String choiceFlag
    ) {
        return replace(
                player,
                state(player).markEarthloongRewardChoiceDelivered(
                        choiceFlag,
                        gameTick(player)
                )
        );
    }

    public static R01PlayerState markEarthloongScalesDelivered(ServerPlayer player) {
        return replace(
                player,
                state(player).markEarthloongScalesDelivered(gameTick(player))
        );
    }

    private static R01PlayerState replace(
            ServerPlayer player,
            R01PlayerState next
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(next, "next");
        R01PlayerState current = state(player);
        if (current.equals(next)) {
            return current;
        }
        player.setAttached(R01PlayerStateAttachments.R01_PLAYER_STATE, next);
        return next;
    }

    private static long gameTick(ServerPlayer player) {
        return player.level().getGameTime();
    }
}
