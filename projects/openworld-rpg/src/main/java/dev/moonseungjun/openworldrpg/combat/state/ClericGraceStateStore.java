package dev.moonseungjun.openworldrpg.combat.state;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-owned registry for transient Cleric Grace state. */
public final class ClericGraceStateStore {
    private final ConcurrentHashMap<UUID, ClericGraceRuntimeState> states =
            new ConcurrentHashMap<>();

    public ClericGraceRuntimeState getOrCreate(UUID playerId) {
        return states.computeIfAbsent(
                playerId,
                ignored -> new ClericGraceRuntimeState()
        );
    }

    public Optional<ClericGraceRuntimeState> state(UUID playerId) {
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

    public int size() {
        return states.size();
    }
}
