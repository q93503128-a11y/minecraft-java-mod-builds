package kr.moonseungjun.riftfrontier.client;

import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side semantic presentation cache. Final render/animation systems resolve assets from this state. */
public final class BossPresentationClientState {
    private static final Map<Integer, Entry> ENTRIES = new ConcurrentHashMap<>();

    private BossPresentationClientState() {}

    /**
     * Accepts monotonic server snapshots per logical Minecraft actor. If a numeric entity id is reused by a new UUID,
     * the new actor replaces the old watermark instead of inheriting it. Clears retain the current actor watermark.
     */
    public static boolean accept(BossPresentationSemanticState state) {
        final boolean[] changed = {false};
        ENTRIES.compute(state.entityId(), (entityId, current) -> {
            if (current != null && current.entityUuid().equals(state.entityUuid())
                && state.serverGameTick() < current.latestServerGameTick()) {
                return current;
            }
            changed[0] = true;
            return new Entry(state.entityUuid(), state.serverGameTick(), state.active() ? state : null);
        });
        return changed[0];
    }

    /** Compatibility lookup by numeric id. Production render paths must use the UUID-checked overload. */
    public static Optional<BossPresentationSemanticState> current(int entityId) {
        Entry entry = ENTRIES.get(entityId);
        return entry == null ? Optional.empty() : Optional.ofNullable(entry.activeState());
    }

    /** Fail-closed render lookup: a reused numeric id cannot expose another actor's cached presentation. */
    public static Optional<BossPresentationSemanticState> current(int entityId, UUID entityUuid) {
        if (entityId < 0) throw new IllegalArgumentException("entityId must be >= 0");
        UUID requiredUuid = java.util.Objects.requireNonNull(entityUuid, "entityUuid");
        Entry entry = ENTRIES.get(entityId);
        if (entry == null || !entry.entityUuid().equals(requiredUuid)) return Optional.empty();
        return Optional.ofNullable(entry.activeState());
    }

    /** Clears both active semantics and ordering watermarks when the client leaves the current connection/world. */
    public static void clearAll() {
        ENTRIES.clear();
    }

    private record Entry(UUID entityUuid, long latestServerGameTick, BossPresentationSemanticState activeState) {
        private Entry {
            entityUuid = java.util.Objects.requireNonNull(entityUuid, "entityUuid");
            if (latestServerGameTick < 0) throw new IllegalArgumentException("latestServerGameTick must be >= 0");
            if (activeState != null && (!activeState.entityUuid().equals(entityUuid)
                || activeState.serverGameTick() != latestServerGameTick)) {
                throw new IllegalArgumentException("active state must match cache actor identity and watermark");
            }
        }
    }
}
