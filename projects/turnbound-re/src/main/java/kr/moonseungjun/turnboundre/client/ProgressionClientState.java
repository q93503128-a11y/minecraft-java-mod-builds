package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.ProgressionNetworkPayloads;

import java.util.Optional;

/** Client presentation cache only. Persistent progression truth remains server-owned. */
public final class ProgressionClientState {
    private static ProgressionNetworkPayloads.Snapshot snapshot;
    private static long generation;

    private ProgressionClientState() {}

    public static void accept(ProgressionNetworkPayloads.ProgressSnapshotS2C payload) {
        if (payload == null) return;
        try {
            snapshot = payload.decode();
            generation++;
        } catch (RuntimeException invalidPayload) {
            snapshot = null;
            generation++;
        }
    }

    public static Optional<ProgressionNetworkPayloads.Snapshot> snapshot() {
        return Optional.ofNullable(snapshot);
    }

    public static long generation() {
        return generation;
    }

    public static void clear() {
        snapshot = null;
        generation++;
    }
}
