package kr.moonseungjun.riftfrontier.client;

import kr.moonseungjun.riftfrontier.network.BossPresentationPayload;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side semantic presentation cache. Final render/animation systems resolve assets from this state. */
public final class BossPresentationClientState {
    private static final Map<Integer, BossPresentationPayload> CURRENT = new ConcurrentHashMap<>();

    private BossPresentationClientState() {}

    /**
     * Accepts only monotonic server snapshots per entity so delayed packets cannot rewind presentation state.
     * An inactive payload clears an entity only when it is at least as new as the cached state.
     */
    public static boolean accept(BossPresentationPayload payload) {
        final boolean[] changed = {false};
        CURRENT.compute(payload.entityId(), (entityId, current) -> {
            if (current != null && payload.serverGameTick() < current.serverGameTick()) return current;
            changed[0] = true;
            return payload.active() ? payload : null;
        });
        return changed[0];
    }

    public static Optional<BossPresentationPayload> current(int entityId) {
        return Optional.ofNullable(CURRENT.get(entityId));
    }

    public static void clearAll() {
        CURRENT.clear();
    }
}
