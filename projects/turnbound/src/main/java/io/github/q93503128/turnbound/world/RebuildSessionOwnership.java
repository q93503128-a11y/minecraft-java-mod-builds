package io.github.q93503128.turnbound.world;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Session ownership uses object identity as well as UUID. A rejected duplicate
 * connection must never save or release an already-live player's state.
 */
public final class RebuildSessionOwnership<T> {
    private final Map<UUID, T> owners = new HashMap<>();

    public boolean claim(UUID playerId, T player) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(player, "player");
        return owners.putIfAbsent(playerId, player) == null;
    }

    public boolean hasOwner(UUID playerId) {
        return playerId != null && owners.containsKey(playerId);
    }

    public boolean owns(UUID playerId, T player) {
        return playerId != null && player != null && owners.get(playerId) == player;
    }

    public boolean transfer(UUID playerId, T previousPlayer, T clonedPlayer) {
        Objects.requireNonNull(clonedPlayer, "clonedPlayer");
        if (!owns(playerId, previousPlayer)) return false;
        owners.put(playerId, clonedPlayer);
        return true;
    }

    public boolean release(UUID playerId, T player) {
        return owns(playerId, player) && owners.remove(playerId, player);
    }

    public void clear() {
        owners.clear();
    }
}
