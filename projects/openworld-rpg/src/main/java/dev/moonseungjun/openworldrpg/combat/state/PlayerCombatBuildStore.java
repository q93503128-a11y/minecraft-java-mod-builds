package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-owned current combat build store.
 *
 * <p>No player receives a fabricated weapon/class build. Until progression and equipment systems
 * publish a validated build, project damage integrations remain fail closed.</p>
 */
public final class PlayerCombatBuildStore {
    private final ConcurrentHashMap<UUID, PlayerCombatBuildState> builds = new ConcurrentHashMap<>();

    public Optional<PlayerCombatBuildState> build(UUID playerId) {
        return Optional.ofNullable(builds.get(playerId));
    }

    public Optional<ProjectImpactTransaction.DamageSourceSnapshot> sourceSnapshot(
            UUID playerId,
            ProjectImpactTransaction.DamageSchool school
    ) {
        return build(playerId).map(build -> build.damageSource(school));
    }

    public void bindAuthoritative(UUID playerId, PlayerCombatBuildState build) {
        builds.put(playerId, build);
    }

    public void remove(UUID playerId) {
        builds.remove(playerId);
    }

    public int size() {
        return builds.size();
    }
}
