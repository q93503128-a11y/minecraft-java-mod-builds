package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-owned seam for the currently equipped/project-derived offensive combat snapshot.
 *
 * <p>This store intentionally has no fallback snapshot. Until project progression/equipment code binds
 * a real authoritative snapshot, project damage integrations must fail closed instead of borrowing
 * donor spell power, vanilla attack damage, or invented bootstrap stats.</p>
 */
public final class PlayerCombatSnapshotStore {
    private final ConcurrentHashMap<UUID, ProjectImpactTransaction.DamageSourceSnapshot> snapshots =
            new ConcurrentHashMap<>();

    public Optional<ProjectImpactTransaction.DamageSourceSnapshot> snapshot(UUID playerId) {
        return Optional.ofNullable(snapshots.get(playerId));
    }

    public void bindAuthoritative(
            UUID playerId,
            ProjectImpactTransaction.DamageSourceSnapshot snapshot
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
