package kr.moonseungjun.riftfrontier.client;

import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side semantic presentation cache. Final render/animation systems resolve assets from this state. */
public final class BossPresentationClientState {
    private static final Map<Integer, Entry> ENTRIES = new ConcurrentHashMap<>();

    private BossPresentationClientState() {}

    /**
     * Accepts only monotonic server snapshots per entity so delayed packets cannot rewind presentation state.
     * A clear retains its server-tick watermark, preventing an older active snapshot from resurrecting stale visuals.
     */
    public static boolean accept(BossPresentationSemanticState state) {
        final boolean[] changed = {false};
        ENTRIES.compute(state.entityId(), (entityId, current) -> {
            if (current != null && state.serverGameTick() < current.latestServerGameTick()) return current;
            changed[0] = true;
            return new Entry(state.serverGameTick(), state.active() ? state : null);
        });
        return changed[0];
    }

    public static Optional<BossPresentationSemanticState> current(int entityId) {
        Entry entry = ENTRIES.get(entityId);
        return entry == null ? Optional.empty() : Optional.ofNullable(entry.activeState());
    }

    /** Clears both active semantics and ordering watermarks when the client leaves the current connection/world. */
    public static void clearAll() {
        ENTRIES.clear();
    }

    private record Entry(long latestServerGameTick, BossPresentationSemanticState activeState) {
        private Entry {
            if (latestServerGameTick < 0) throw new IllegalArgumentException("latestServerGameTick must be >= 0");
        }
    }
}
