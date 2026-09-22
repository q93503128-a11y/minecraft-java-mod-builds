package dev.moonseungjun.openworldrpg.combat.state;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-owned registry for transient player-poise state. */
public final class PlayerPoiseStateStore {
    private final ConcurrentHashMap<UUID, PlayerPoiseRuntimeState> states =
            new ConcurrentHashMap<>();

    public PlayerPoiseRuntimeState synchronize(
            UUID playerId,
            double maxPoise,
            long gameTick
    ) {
        return states.compute(playerId, (ignored, existing) -> {
            if (existing == null) {
                return new PlayerPoiseRuntimeState(maxPoise, gameTick);
            }
            existing.synchronizeMaxPoise(maxPoise, gameTick);
            return existing;
        });
    }

    public Optional<PlayerPoiseRuntimeState> state(UUID playerId) {
        return Optional.ofNullable(states.get(playerId));
    }

    public void remove(UUID playerId) {
        states.remove(playerId);
    }

    public int size() {
        return states.size();
    }
}
