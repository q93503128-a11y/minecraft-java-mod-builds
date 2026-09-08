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

    public static synchronized void clear() {
        latest = null;
        generation++;
    }
}
