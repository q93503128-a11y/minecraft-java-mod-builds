package dev.moonseungjun.openworldrpg.combat.state;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-owned registry for transient player barrier layers. */
public final class PlayerBarrierStateStore {
    private final ConcurrentHashMap<UUID, PlayerBarrierRuntimeState> states =
            new ConcurrentHashMap<>();

    public PlayerBarrierRuntimeState getOrCreate(UUID playerId) {
        return states.computeIfAbsent(
                playerId,
                ignored -> new PlayerBarrierRuntimeState()
        );
    }

    public Optional<PlayerBarrierRuntimeState> state(UUID playerId) {
        return Optional.ofNullable(states.get(playerId));
    }

    public void remove(UUID playerId) {
        states.remove(playerId);
    }

    public int size() {
        return states.size();
    }
}
