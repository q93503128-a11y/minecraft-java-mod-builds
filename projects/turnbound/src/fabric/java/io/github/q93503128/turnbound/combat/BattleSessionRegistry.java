package io.github.q93503128.turnbound.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-lifetime registry: multiple battles, one battle per participant, shared ownership.
 * Persistence and per-dungeon disconnect policy are deliberately not assumed here.
 */
public final class BattleSessionRegistry {
    private static final int MAX_CONCURRENT_SESSIONS = 128;
    private final Map<Long, BattleSessionAuthority> byId = new HashMap<>();
    private final Map<UUID, Long> playerSession = new HashMap<>();
    private long nextId = 1L;

    public BattleSessionAuthority open(String worldId, BattleState state,
                                       Map<UUID, Set<String>> actorOwnership) {
        if (byId.size() >= MAX_CONCURRENT_SESSIONS || nextId == Long.MAX_VALUE) {
            throw new IllegalStateException("Server session capacity reached");
        }
        if (actorOwnership == null || actorOwnership.isEmpty()) {
            throw new IllegalArgumentException("No battle participants");
        }
        for (UUID player : actorOwnership.keySet()) {
            if (playerSession.containsKey(player)) {
                throw new IllegalStateException("Player already in a battle");
            }
        }
        long id = nextId;
        BattleSessionAuthority battle = new BattleSessionAuthority(id, worldId, state, actorOwnership);
        nextId = Math.incrementExact(nextId);
        byId.put(id, battle);
        for (UUID player : battle.participants()) playerSession.put(player, id);
        return battle;
    }

    public BattleSessionAuthority sessionFor(UUID participant) {
        Long id = playerSession.get(participant);
        return id == null ? null : byId.get(id);
    }

    public BattleSessionAuthority.Status submit(UUID sender, String worldId,
                                                 BattleSessionAuthority.Action action) {
        BattleSessionAuthority battle = sessionFor(sender);
        return battle == null
                ? BattleSessionAuthority.Status.NO_SESSION
                : battle.submit(sender, worldId, action);
    }

    public void close(long sessionId) {
        BattleSessionAuthority battle = byId.remove(sessionId);
        if (battle == null) return;
        battle.close();
        for (UUID player : battle.participants()) playerSession.remove(player, sessionId);
    }

    public void clear() {
        for (BattleSessionAuthority battle : byId.values()) battle.close();
        byId.clear();
        playerSession.clear();
    }

    public int activeCount() { return byId.size(); }
}
