package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.RewardTableDefinition;

import java.util.UUID;

/** Immutable reward and authored-world source metadata captured when an encounter opens. */
public record BattleRewardContext(
        UUID ownerPlayerId,
        RewardTableDefinition rewardTable,
        long rewardSeed,
        String worldAnchorLocator,
        boolean worldAnchorRepeatable
) {
    public BattleRewardContext(UUID ownerPlayerId, RewardTableDefinition rewardTable, long rewardSeed) {
        this(ownerPlayerId, rewardTable, rewardSeed, "", false);
    }

    public BattleRewardContext {
        if (ownerPlayerId == null) throw new IllegalArgumentException("ownerPlayerId must not be null");
        if (rewardTable == null) throw new IllegalArgumentException("rewardTable must not be null");
        worldAnchorLocator = worldAnchorLocator == null ? "" : worldAnchorLocator;
        if (worldAnchorLocator.isBlank()) worldAnchorLocator = "";
        if (worldAnchorRepeatable && worldAnchorLocator.isEmpty()) {
            throw new IllegalArgumentException("repeatable world anchor requires locator");
        }
    }

    public String rewardTableId() {
        return rewardTable.id();
    }

    public boolean fromWorldAnchor() {
        return !worldAnchorLocator.isEmpty();
    }
}
