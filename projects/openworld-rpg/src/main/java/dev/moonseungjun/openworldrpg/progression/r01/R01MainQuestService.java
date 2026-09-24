package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative entry points for the first R01 main-quest progression transactions. */
public final class R01MainQuestService {
    private R01MainQuestService() {
    }

    public static QuarryRoadActionResult recordQuarryRoadAction(
            ServerPlayer player,
            R01PlayerState.QuarryRoadAction action
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(action, "action");

        R01PlayerState before = R01PlayerStateService.state(player);
        boolean wasComplete = before.opening().mainStage()
                .isAtLeast(R01MainStage.QUARRY_ROAD_COMPLETE);

        R01PlayerState after = R01PlayerStateService.recordQuarryRoadAction(player, action);
        boolean nowComplete = after.opening().mainStage()
                .isAtLeast(R01MainStage.QUARRY_ROAD_COMPLETE);
        boolean newlyCompleted = !wasComplete && nowComplete;

        if (newlyCompleted) {
            RootClass rewardClass = PlayerProgressionService.state(player)
                    .activeClass()
                    .orElseThrow(() -> new IllegalStateException(
                            "Dust on the Quarry Road cannot complete before first root class."
                    ));
            R01RewardService.grantDustOnQuarryRoad(player, rewardClass);
        }

        return new QuarryRoadActionResult(after, newlyCompleted);
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
