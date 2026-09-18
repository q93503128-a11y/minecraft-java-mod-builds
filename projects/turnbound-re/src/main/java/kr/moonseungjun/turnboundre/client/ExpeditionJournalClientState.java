package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.ExpeditionNetworkPayloads;

import java.util.Optional;

/** Client cache for the server-authored Expedition Journal plus the player's local world-route selection. */
public final class ExpeditionJournalClientState {
    private static ExpeditionNetworkPayloads.JournalView latest;
    private static ExpeditionNetworkPayloads.EncounterView tracked;
    private static long generation;

    private ExpeditionJournalClientState() {}

    public static synchronized void accept(ExpeditionNetworkPayloads.JournalSnapshotS2C payload) {
        if (payload == null) return;
        latest = payload.decode();
        if (tracked != null) {
            String locator = tracked.locator();
            tracked = latest.encounters().stream()
                    .filter(ExpeditionNetworkPayloads.EncounterView::hasWorldRoute)
                    .filter(encounter -> locator.equals(encounter.locator()))
                    .findFirst()
                    .orElse(null);
        }
        generation++;
    }

    public static synchronized Optional<ExpeditionNetworkPayloads.JournalView> view() {
        return Optional.ofNullable(latest);
    }

    public static synchronized Optional<ExpeditionNetworkPayloads.EncounterView> trackedRoute() {
        return Optional.ofNullable(tracked);
    }

    public static synchronized boolean isTracking(String locator) {
        return tracked != null && locator != null && locator.equals(tracked.locator());
    }

    public static synchronized boolean toggleTracking(ExpeditionNetworkPayloads.EncounterView encounter) {
        if (encounter == null || !encounter.hasWorldRoute()) return false;
        if (isTracking(encounter.locator())) {
            tracked = null;
            generation++;
            return false;
        }
        tracked = encounter;
        generation++;
        return true;
    }

    public static synchronized void clearTracking() {
        if (tracked == null) return;
        tracked = null;
        generation++;
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
