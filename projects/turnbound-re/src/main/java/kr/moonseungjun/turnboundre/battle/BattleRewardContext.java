package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.RewardTableDefinition;

import java.util.UUID;

/** Immutable reward metadata captured when an authored encounter opens. */
public record BattleRewardContext(
        UUID ownerPlayerId,
        RewardTableDefinition rewardTable,
        long rewardSeed
) {
    public BattleRewardContext {
        if (ownerPlayerId == null) throw new IllegalArgumentException("ownerPlayerId must not be null");
        if (rewardTable == null) throw new IllegalArgumentException("rewardTable must not be null");
    }

    public String rewardTableId() {
        return rewardTable.id();
    }
}
