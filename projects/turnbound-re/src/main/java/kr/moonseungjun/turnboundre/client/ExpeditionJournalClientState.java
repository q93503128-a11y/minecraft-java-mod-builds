package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.ExpeditionNetworkPayloads;

import java.util.Optional;

/** Client cache for the server-authored Expedition Journal snapshot. */
public final class ExpeditionJournalClientState {
    private static ExpeditionNetworkPayloads.JournalView latest;
    private static long generation;

    private ExpeditionJournalClientState() {}

    public static synchronized void accept(ExpeditionNetworkPayloads.JournalSnapshotS2C payload) {
        if (payload == null) return;
        latest = payload.decode();
        generation++;
    }

    public static synchronized Optional<ExpeditionNetworkPayloads.JournalView> view() {
        return Optional.ofNullable(latest);
    }

    public static synchronized long generation() { return generation; }

    /** Clears only the cached journal view. Route tracking intentionally survives menu refreshes. */
    public static synchronized void clearView() {
        latest = null;
        generation++;
    }

    /** Compatibility alias retained for older callers/tests. */
    public static synchronized void clear() {
        clearView();
    }
}
