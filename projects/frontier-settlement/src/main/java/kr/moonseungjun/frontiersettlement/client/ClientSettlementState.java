package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.SettlementContextPayload;
import kr.moonseungjun.frontiersettlement.network.SettlementContextTarget;
import kr.moonseungjun.frontiersettlement.network.SettlementSnapshotPayload;

import java.util.HashSet;
import java.util.Set;

public final class ClientSettlementState {
    private static volatile SettlementSnapshotPayload snapshot =
            new SettlementSnapshotPayload(false, 0L, 0L, 0L, 0L, 0, "개척 캠프", 0, "", SettlementContextPayload.EMPTY);
    private static volatile SettlementContextPayload context = SettlementContextPayload.EMPTY;
    private static boolean snapshotInitialized;
    private static boolean contextInitialized;

    private ClientSettlementState() {}

    public static synchronized void accept(SettlementSnapshotPayload next) {
        SettlementSnapshotPayload previous = snapshot;
        snapshot = next;
        if (snapshotInitialized && next.founded() && previous.founded()
                && !next.tier().equals(previous.tier())) {
            SettlementNoticeQueue.push("마을 성장 · " + next.tier());
        }
        acceptContext(next.context());
        snapshotInitialized = true;
    }

    private static void acceptContext(SettlementContextPayload next) {
        SettlementContextPayload previous = context;
        context = next;
        if (contextInitialized) {
            Set<String> oldKeys = new HashSet<>();
            for (SettlementContextTarget target : previous.targets()) oldKeys.add(target.key());
            for (SettlementContextTarget target : next.targets()) {
                if (oldKeys.contains(target.key())) continue;
                if ("building".equals(target.kind())) SettlementNoticeQueue.push("완공 · " + target.title());
                else if ("outpost".equals(target.kind())) SettlementNoticeQueue.push("영토 확장 · " + target.title());
            }
            if (previous.projectLabel().isBlank() && !next.projectLabel().isBlank()) {
                SettlementNoticeQueue.push("공사 시작 · " + next.projectLabel());
            }
        }
        contextInitialized = true;
    }

    public static SettlementSnapshotPayload snapshot() { return snapshot; }
    public static SettlementContextPayload context() { return context; }
}
