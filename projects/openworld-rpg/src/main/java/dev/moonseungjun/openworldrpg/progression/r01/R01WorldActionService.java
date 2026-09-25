package dev.moonseungjun.openworldrpg.progression.r01;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-only semantic bridge from authored world/runtime interactions into R01 personal progress.
 *
 * <p>No coordinate/entity-id guesses live here. Spatial binding, external actor adapters and the
 * Roadside Trouble shared controller validate physical facts before calling this service.</p>
 */
public final class R01WorldActionService {
    private R01WorldActionService() {
    }

    public static R01MainQuestService.QuarryRoadActionResult recordPersonalInteraction(
            ServerPlayer player,
            R01WorldActionAuthority.PersonalInteraction interaction
    ) {
        Objects.requireNonNull(player, "player");
        return R01MainQuestService.recordQuarryRoadAction(
                player,
                R01WorldActionAuthority.personalInteraction(interaction)
        );
    }

    public static R01MainQuestService.QuarryRoadActionResult recordValidGather(
            ServerPlayer player,
            String resourceId
    ) {
        Objects.requireNonNull(player, "player");
        return R01MainQuestService.recordQuarryRoadAction(
                player,
                R01WorldActionAuthority.validGather(resourceId)
        );
    }

    public static R01MainQuestService.QuarryRoadActionResult recordMeadowViperContribution(
            ServerPlayer player,
            R01WorldActionAuthority.CombatContribution contribution
    ) {
        Objects.requireNonNull(player, "player");
        return R01MainQuestService.recordQuarryRoadAction(
                player,
                R01WorldActionAuthority.meadowViperContribution(contribution)
        );
    }

    /**
     * Quest-category credit only.
     *
     * <p>The shared Roadside Trouble controller owns event-cycle participation and its repeatable
     * 5%/4%/20 reward. It may call this only after the shared event completed and this player was
     * already proven eligible for that event instance.</p>
     */
    public static R01MainQuestService.QuarryRoadActionResult recordRoadsideTroubleQuestCredit(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");
        return R01MainQuestService.recordQuarryRoadAction(
                player,
                R01PlayerState.QuarryRoadAction.ROADSIDE_TROUBLE
        );
    }
}
