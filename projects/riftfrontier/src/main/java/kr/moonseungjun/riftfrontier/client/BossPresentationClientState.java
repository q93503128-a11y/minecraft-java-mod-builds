package kr.moonseungjun.riftfrontier.client;

import kr.moonseungjun.riftfrontier.network.BossPresentationPayload;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side semantic presentation cache. Final render/animation systems resolve assets from this state. */
public final class BossPresentationClientState {
    private static final Map<Integer, Entry> ENTRIES = new ConcurrentHashMap<>();

    private BossPresentationClientState() {}

    /**
     * Accepts only monotonic server snapshots per entity so delayed packets cannot rewind presentation state.
     * A clear retains its server-tick watermark, preventing an older active packet from resurrecting stale visuals.
     */
    public static boolean accept(BossPresentationPayload payload) {
        final boolean[] changed = {false};
        ENTRIES.compute(payload.entityId(), (entityId, current) -> {
            if (current != null && payload.serverGameTick() < current.latestServerGameTick()) return current;
            changed[0] = true;
            return new Entry(payload.serverGameTick(), payload.active() ? payload : null);
        });
        return changed[0];
    }

    public static Optional<BossPresentationPayload> current(int entityId) {
        Entry entry = ENTRIES.get(entityId);
        return entry == null ? Optional.empty() : Optional.ofNullable(entry.activePayload());
    }

    /** Clears both active semantics and ordering watermarks when the client leaves the current connection/world. */
    public static void clearAll() {
        ENTRIES.clear();
    }

    private record Entry(long latestServerGameTick, BossPresentationPayload activePayload) {
        private Entry {
            if (latestServerGameTick < 0) throw new IllegalArgumentException("latestServerGameTick must be >= 0");
        }
    }
}
