package kr.moonseungjun.riftfrontier.client;

import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side semantic presentation cache. Final render/animation systems resolve assets from this state. */
public final class BossPresentationClientState {
    private static final Map<Integer, Entry> ENTRIES = new ConcurrentHashMap<>();

    private BossPresentationClientState() {}

    /**
     * Accepts monotonic server snapshots per logical Minecraft actor.
     *
     * <p>For the same UUID, older ticks are rejected and the exact same tick is immutable: an identical replay is
     * idempotent while any same-tick attempt to rewrite active/clear state, phase progress or other semantics is
     * ignored. This preserves the server runtime's one-authoritative-meaning-per-tick contract after transport.</p>
     *
     * <p>If a numeric entity id is reused by a new UUID, the new actor replaces the old watermark instead of
     * inheriting another actor's level-time epoch. Production render lookup still requires both numeric id and UUID,
     * so a reused id can never expose the previous actor's cached presentation. Clears retain the current actor
     * watermark until that exact actor leaves the client level.</p>
     */
    public static boolean accept(BossPresentationSemanticState state) {
        BossPresentationSemanticState incoming = Objects.requireNonNull(state, "state");
        final boolean[] changed = {false};
        ENTRIES.compute(incoming.entityId(), (entityId, current) -> {
            if (current != null && current.entityUuid().equals(incoming.entityUuid())) {
                if (incoming.serverGameTick() < current.latestServerGameTick()) {
                    return current;
                }
                if (incoming.serverGameTick() == current.latestServerGameTick()) {
                    // Equal-tick transport is replay-only. Never let packet ordering rewrite one authoritative tick.
                    return current;
                }
            }
            changed[0] = true;
            return new Entry(
                incoming.entityUuid(),
                incoming.serverGameTick(),
                incoming.active() ? incoming : null
            );
        });
        return changed[0];
    }

    /** Fail-closed render lookup: a reused numeric id cannot expose another actor's cached presentation. */
    public static Optional<BossPresentationSemanticState> current(int entityId, UUID entityUuid) {
        if (entityId < 0) throw new IllegalArgumentException("entityId must be >= 0");
        UUID requiredUuid = Objects.requireNonNull(entityUuid, "entityUuid");
        Entry entry = ENTRIES.get(entityId);
        if (entry == null || !entry.entityUuid().equals(requiredUuid)) return Optional.empty();
        return Optional.ofNullable(entry.activeState());
    }

    /**
     * Drops active semantics and the ordering watermark for one exact logical actor when it leaves the client level.
     *
     * <p>The UUID comparison is deliberate: a delayed leave callback for an old actor must never erase a new actor
     * that has already reused the same numeric entity id. Removing the whole entry also retires a clear-only
     * watermark, so a later lifecycle of the same UUID starts with a fresh level-time epoch.</p>
     */
    public static boolean forgetActor(int entityId, UUID entityUuid) {
        if (entityId < 0) throw new IllegalArgumentException("entityId must be >= 0");
        UUID requiredUuid = Objects.requireNonNull(entityUuid, "entityUuid");
        final boolean[] removed = {false};
        ENTRIES.computeIfPresent(entityId, (ignored, current) -> {
            if (!current.entityUuid().equals(requiredUuid)) return current;
            removed[0] = true;
            return null;
        });
        return removed[0];
    }

    /** Clears both active semantics and ordering watermarks when the client leaves the current connection/world. */
    public static void clearAll() {
        ENTRIES.clear();
    }

    private record Entry(UUID entityUuid, long latestServerGameTick, BossPresentationSemanticState activeState) {
        private Entry {
            entityUuid = Objects.requireNonNull(entityUuid, "entityUuid");
            if (latestServerGameTick < 0) throw new IllegalArgumentException("latestServerGameTick must be >= 0");
            if (activeState != null && (!activeState.entityUuid().equals(entityUuid)
                || activeState.serverGameTick() != latestServerGameTick)) {
                throw new IllegalArgumentException("active state must match cache actor identity and watermark");
            }
        }
    }
}
