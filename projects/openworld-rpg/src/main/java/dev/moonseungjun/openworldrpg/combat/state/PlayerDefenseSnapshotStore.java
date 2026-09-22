package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-owned current defensive equipment snapshot.
 *
 * <p>This snapshot is published from persistent project equipment independently of whether the
 * player currently has a main weapon. Incoming damage authority must never fall back to donor armor
 * values merely because an offensive build is absent.</p>
 */
public final class PlayerDefenseSnapshotStore {
    private final ConcurrentHashMap<UUID, PlayerDefenseAuthority.DefenseSnapshot> snapshots =
            new ConcurrentHashMap<>();

    public Optional<PlayerDefenseAuthority.DefenseSnapshot> snapshot(UUID playerId) {
        return Optional.ofNullable(snapshots.get(playerId));
    }

    public void bindAuthoritative(
            UUID playerId,
            PlayerDefenseAuthority.DefenseSnapshot snapshot
    ) {
        snapshots.put(playerId, snapshot);
    }

    public void remove(UUID playerId) {
        snapshots.remove(playerId);
    }

    public int size() {
        return snapshots.size();
    }
}
