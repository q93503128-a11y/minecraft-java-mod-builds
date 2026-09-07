package kr.moonseungjun.turnboundre.progression;

import kr.moonseungjun.turnboundre.battle.BattleManager;
import kr.moonseungjun.turnboundre.battle.BattleRewardContext;
import net.minecraft.server.MinecraftServer;

import java.util.Optional;
import java.util.UUID;

/** Bridges one-shot battle reward claims into persisted player progression. */
public final class BattleRewardSettlementService {
    public record Settlement(
            UUID ownerPlayerId,
            String rewardTableId,
            RewardService.RewardGrant grant,
            PlayerProgress state
    ) {}

    private final BattleManager battles;
    private final PlayerProgressStore progress;

    public BattleRewardSettlementService(BattleManager battles, PlayerProgressStore progress) {
        if (battles == null || progress == null) throw new IllegalArgumentException("battles/progress required");
        this.battles = battles;
        this.progress = progress;
    }

    /**
     * Returns empty when the battle is not a reward-bearing VICTORY or was already claimed.
     * Persistence occurs inside the BattleManager claim callback, so a failed save/application does not burn the claim.
     */
    public Optional<Settlement> settleIfReady(MinecraftServer server, UUID battleId) {
        if (server == null || battleId == null) throw new IllegalArgumentException("server/battleId required");
        return battles.claimVictoryReward(battleId, context -> settle(server, context));
    }

    private Settlement settle(MinecraftServer server, BattleRewardContext context) {
        RewardService.Applied applied = progress.applyReward(
                server,
                context.ownerPlayerId(),
                context.rewardTableId(),
                context.rewardSeed());
        return new Settlement(
                context.ownerPlayerId(),
                context.rewardTableId(),
                applied.grant(),
                applied.state());
    }
}
