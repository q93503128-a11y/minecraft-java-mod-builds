package kr.moonseungjun.survivalascension.client;

import kr.moonseungjun.survivalascension.network.ExpeditionSnapshotPayload;

import java.util.Map;

public final class ClientExpeditionState {
    private static int discoveredMask;
    private static int completedMask;
    private static Map<String, String> directives = Map.of();
    private static boolean loaded;

    private ClientExpeditionState() {}

    public static void onSnapshot(ExpeditionSnapshotPayload payload) {
        discoveredMask = payload.discoveredMask();
        completedMask = payload.completedMask();
        directives = Map.copyOf(payload.directives());
        loaded = true;
    }

    public static void reset() {
        discoveredMask = 0;
        completedMask = 0;
        directives = Map.of();
        loaded = false;
    }

    public static boolean loaded() { return loaded; }
    public static int discoveredMask() { return discoveredMask; }
    public static int completedMask() { return completedMask; }
    public static String directive(String regionId) { return directives.getOrDefault(regionId, ""); }
}
