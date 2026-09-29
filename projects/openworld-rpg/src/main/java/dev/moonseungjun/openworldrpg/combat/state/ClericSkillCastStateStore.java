package dev.moonseungjun.openworldrpg.combat.state;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-owned registry for accepted Cleric skill snapshots. */
public final class ClericSkillCastStateStore {
    private final ConcurrentHashMap<UUID, ClericSkillCastRuntimeState> states =
            new ConcurrentHashMap<>();

    public ClericSkillCastRuntimeState getOrCreate(UUID playerId) {
        return states.computeIfAbsent(
                playerId,
                ignored -> new ClericSkillCastRuntimeState()
        );
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
