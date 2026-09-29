package dev.moonseungjun.openworldrpg.combat.state;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-owned registry for transient Balanced Doctrine state. */
public final class ClericDoctrineStateStore {
    private final ConcurrentHashMap<UUID, ClericDoctrineRuntimeState> states =
            new ConcurrentHashMap<>();

    public ClericDoctrineRuntimeState getOrCreate(UUID playerId) {
        return states.computeIfAbsent(
                playerId,
                ignored -> new ClericDoctrineRuntimeState()
        );
    }

    public Optional<ClericDoctrineRuntimeState> state(UUID playerId) {
        return Optional.ofNullable(states.get(playerId));
    }

    public void reset(UUID playerId) {
        states.computeIfPresent(
                playerId,
                (ignored, state) -> {
                    state.reset();
                    return state;
                }
        );
    }

    public void remove(UUID playerId) {
        states.remove(playerId);
    }
}
