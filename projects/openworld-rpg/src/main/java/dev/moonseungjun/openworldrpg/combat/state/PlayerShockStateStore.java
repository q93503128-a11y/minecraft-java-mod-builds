package dev.moonseungjun.openworldrpg.combat.state;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-owned player Shock meter registry. */
public final class PlayerShockStateStore {
    private final ConcurrentHashMap<UUID, PlayerShockRuntimeState> states =
            new ConcurrentHashMap<>();

    public PlayerShockRuntimeState synchronize(
            UUID playerId,
            double threshold,
            long gameTick
    ) {
        return states.compute(playerId, (ignored, existing) -> {
            if (existing == null) {
                return new PlayerShockRuntimeState(threshold, gameTick);
            }
            existing.synchronizeThreshold(threshold, gameTick);
            return existing;
        });
    }

    public void remove(UUID playerId) {
        states.remove(playerId);
    }

    public int size() {
        return states.size();
    }
}
