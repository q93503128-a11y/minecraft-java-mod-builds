package io.github.q93503128.turnbound.combat;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Resolves authoritative player ownership for regular allies and their dynamically-created summons. */
public final class SharedBattleOwnership {
    private SharedBattleOwnership() {}

    public static UUID ownerOf(BattleState state, Map<String, UUID> directOwners, String actorInstanceId) {
        return ownerOf(state, directOwners, actorInstanceId, new HashSet<>(), 0);
    }

    private static UUID ownerOf(BattleState state, Map<String, UUID> directOwners, String actorInstanceId,
                                Set<String> visited, int depth) {
        if (actorInstanceId == null || actorInstanceId.isBlank() || state == null
                || depth > 8 || !visited.add(actorInstanceId)) return null;
        if (directOwners != null) {
            UUID direct = directOwners.get(actorInstanceId);
            if (direct != null) return direct;
        }

        CombatantState actor = state.find(actorInstanceId);
        if (actor == null) return null;

        String explicitPlayer = actor.ref("playerOwnerId");
        if (explicitPlayer != null && !explicitPlayer.isBlank()) {
            try { return UUID.fromString(explicitPlayer); }
            catch (IllegalArgumentException ignored) { }
        }

        String ownerActor = actor.ref("ownerId");
        if (ownerActor == null || ownerActor.isBlank() || ownerActor.equals(actorInstanceId)) return null;
        return ownerOf(state, directOwners, ownerActor, visited, depth + 1);
    }
}
