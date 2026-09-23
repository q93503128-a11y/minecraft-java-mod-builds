package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.reward.PlayerRewardTransactionService;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * R01-specific entry points into the generic reconnect-safe reward transaction coordinator.
 *
 * <p>These methods bind only reward components whose canon and runtime authority are already
 * closed. Earthloong's item rolls / deterministic weapon choice remain separate and are not
 * falsely marked complete here.</p>
 */
public final class R01RewardService {
    public static final String DUST_ON_QUARRY_ROAD_TRANSACTION =
            "openworld_rpg:r01/dust_on_quarry_road";
    public static final String EARTHLOONG_FIRST_CLEAR_PROGRESSION_TRANSACTION =
            "openworld_rpg:r01/earthloong_first_clear/progression";

    private R01RewardService() {
    }

    public static PlayerRewardTransactionService.RewardResult grantDustOnQuarryRoad(
            ServerPlayer player,
            RootClass rewardClass
    ) {
        return grant(
                player,
                DUST_ON_QUARRY_ROAD_TRANSACTION,
                rewardClass,
                R01RewardRules.DUST_ON_QUARRY_ROAD
        );
    }

    public static PlayerRewardTransactionService.RewardResult grantEarthloongFirstClearProgression(
            ServerPlayer player,
            RootClass rewardClass
    ) {
        return grant(
                player,
                EARTHLOONG_FIRST_CLEAR_PROGRESSION_TRANSACTION,
                rewardClass,
                R01RewardRules.EARTHLOONG_FIRST_CLEAR_PROGRESSION
        );
    }

    private static PlayerRewardTransactionService.RewardResult grant(
            ServerPlayer player,
            String transactionId,
            RootClass rewardClass,
            R01RewardRules.RewardRule rule
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(rewardClass, "rewardClass");
        Objects.requireNonNull(rule, "rule");
        return PlayerRewardTransactionService.grantPercentageRewardOnce(
                player,
                transactionId,
                rewardClass,
                rule.combatRequirementFraction(),
                rule.classRequirementFraction(),
                rule.gold()
        );
    }
}
