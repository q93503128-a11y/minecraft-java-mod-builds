package dev.moonseungjun.openworldrpg.combat.state;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-owned registry for transient dodge/guard/perfect-guard timing state. */
public final class PlayerDefenseStateStore {
    private final ConcurrentHashMap<UUID, PlayerDefenseRuntimeState> states =
            new ConcurrentHashMap<>();

    public PlayerDefenseRuntimeState getOrCreate(UUID playerId) {
        return states.computeIfAbsent(playerId, ignored -> new PlayerDefenseRuntimeState());
    }

    public void remove(UUID playerId) {
        states.remove(playerId);
    }

    public int size() {
        return states.size();
    }
}
