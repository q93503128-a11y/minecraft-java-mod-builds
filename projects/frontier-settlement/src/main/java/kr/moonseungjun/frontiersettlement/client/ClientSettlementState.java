package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.SettlementSnapshotPayload;

public final class ClientSettlementState {
    private static volatile SettlementSnapshotPayload snapshot =
            new SettlementSnapshotPayload(false, 0L, 0L, 0L, 0L, 0, "개척 캠프", 0);

    private ClientSettlementState() {}

    public static void accept(SettlementSnapshotPayload next) {
        snapshot = next;
    }

    public static SettlementSnapshotPayload snapshot() {
        return snapshot;
    }
}
