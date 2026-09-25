package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative entry points for the first R01 main-quest progression transactions. */
public final class R01MainQuestService {
    private static final String DUST_COMPLETION_STEP_ID =
            "openworld_rpg:r01/dust_on_quarry_road/complete";

    private R01MainQuestService() {
    }

    public static QuarryRoadActionResult recordQuarryRoadAction(
            ServerPlayer player,
            R01PlayerState.QuarryRoadAction action
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(action, "action");

        R01PlayerState before = R01PlayerStateService.state(player);
        boolean wasComplete = dustCompleted(before);
        boolean actionAlreadyCredited =
                (before.opening().quarryRoadActionBits() & action.mask()) != 0;

        if (!actionAlreadyCredited) {
            RootClass contributionClass = PlayerProgressionService.state(player)
                    .activeClass()
                    .orElseThrow(() -> new IllegalStateException(
                            "Dust objective credit requires an active root class."
                    ));
            R01QuestAttributionService.recordDustAction(
                    player,
                    action,
                    contributionClass
            );
        }

        R01PlayerState after = R01PlayerStateService.recordQuarryRoadAction(player, action);
        boolean nowComplete = dustCompleted(after);
        boolean newlyCompleted = !wasComplete && nowComplete;

        if (newlyCompleted) {
            grantDustRewardFromCommittedState(player);
        }

        return new QuarryRoadActionResult(after, newlyCompleted);
    }

    /**
     * Closes the disconnect/crash window between personal quest-state commit and reward-plan commit.
     *
     * <p>Call on join before normal play. The reward transaction itself is idempotent, so this also
     * safely completes a previously interrupted multi-domain reward without duplicating Gold/XP.</p>
     */
    public static boolean reconcileCommittedRewards(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!dustCompleted(R01PlayerStateService.state(player))) {
            return false;
        }
        return grantDustRewardFromCommittedState(player);
    }

    private static boolean grantDustRewardFromCommittedState(ServerPlayer player) {
        RootClass completionClass = PlayerProgressionService.state(player)
                .activeClass()
                .orElseThrow(() -> new IllegalStateException(
                        "Committed Dust completion requires an active root class."
                ));
        RootClass rewardClass = R01QuestAttributionService
                .dustMajorityClass(player)
                .orElse(completionClass);
        return R01RewardService.grantDustOnQuarryRoad(player, rewardClass).appliedNow();
    }

    private static boolean dustCompleted(R01PlayerState state) {
        return state.opening().mainStage().isAtLeast(R01MainStage.QUARRY_ROAD_COMPLETE)
                && state.ledger().completedStepIds().contains(DUST_COMPLETION_STEP_ID);
    }

    public record QuarryRoadActionResult(
            R01PlayerState state,
            boolean newlyCompleted
    ) {
        public QuarryRoadActionResult {
            Objects.requireNonNull(state, "state");
        }
    }
}
