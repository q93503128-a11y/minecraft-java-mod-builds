package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.EquipmentNetworkPayloads;

import java.util.Optional;

/** Client presentation cache only. Equipment inventory/progression truth remains server-owned. */
public final class EquipmentClientState {
    private static EquipmentNetworkPayloads.Snapshot snapshot;
    private static long generation;

    private EquipmentClientState() {}

    public static void accept(EquipmentNetworkPayloads.SnapshotS2C payload) {
        if (payload == null) return;
        try {
            snapshot = payload.decode();
            generation++;
        } catch (RuntimeException invalidPayload) {
            snapshot = null;
            generation++;
        }
    }

    public static Optional<EquipmentNetworkPayloads.Snapshot> snapshot() {
        return Optional.ofNullable(snapshot);
    }

    public static long generation() { return generation; }

    public static void clear() {
        snapshot = null;
        generation++;
    }
}
