package kr.moonseungjun.turnboundre.battle;

import java.util.UUID;

/** Immutable reward metadata captured when an encounter opens. */
public record BattleRewardContext(
        UUID ownerPlayerId,
        String rewardTableId,
        long rewardSeed
) {
    public BattleRewardContext {
        if (ownerPlayerId == null) throw new IllegalArgumentException("ownerPlayerId must not be null");
        if (rewardTableId == null || rewardTableId.isBlank() || rewardTableId.indexOf(':') <= 0) {
            throw new IllegalArgumentException("rewardTableId must be a namespaced id");
        }
    }
}
