package dev.moonseungjun.openworldrpg.combat.state;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-owned registry for transient player negative-status state. */
public final class PlayerNegativeStatusStateStore {
    private final ConcurrentHashMap<UUID, PlayerNegativeStatusRuntimeState> states =
            new ConcurrentHashMap<>();

    public PlayerNegativeStatusRuntimeState getOrCreate(UUID playerId) {
        return states.computeIfAbsent(
                playerId,
                ignored -> new PlayerNegativeStatusRuntimeState()
        );
    }

    public void remove(UUID playerId) {
        states.remove(playerId);
    }

    public int size() {
        return states.size();
    }
}
