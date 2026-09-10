package dev.moonseungjun.fishinggame.client;

import java.util.List;

import dev.moonseungjun.fishinggame.network.FishingStatePayload;
import dev.moonseungjun.fishinggame.network.ProfileSnapshotPayload;
import dev.moonseungjun.fishinggame.profile.CatchEntry;

public final class ClientFishingState {
    private static int coins;
    private static int rodTier;
    private static List<CatchEntry> catches = List.of();
    private static int stage = 3;
    private static float tension;
    private static float progress;
    private static String speciesName = "";
    private static String locationName = "청람 호수";
    private static String notice = "";
    private static long noticeUntilMs;
    private static boolean profileInitialized;
    private static CatchEntry recentCatch;
    private static long recentCatchUntilMs;

    private ClientFishingState() {
    }

    public static void apply(ProfileSnapshotPayload payload) {
        List<CatchEntry> incoming = List.copyOf(payload.catches());
        if (profileInitialized && stage == 2 && incoming.size() > catches.size() && !incoming.isEmpty()) {
            recentCatch = incoming.getLast();
            recentCatchUntilMs = System.currentTimeMillis() + 4200L;
        }

        coins = payload.coins();
        rodTier = payload.rodTier();
        catches = incoming;
        profileInitialized = true;
    }

    public static void apply(FishingStatePayload payload) {
        stage = payload.stage();
        tension = payload.tension();
        progress = payload.progress();
        speciesName = payload.speciesName();
        locationName = payload.locationName();
        if (!payload.notice().isBlank()) {
            notice = payload.notice();
            noticeUntilMs = System.currentTimeMillis() + 3200L;
        }
    }

    public static int coins() { return coins; }
    public static int rodTier() { return rodTier; }
    public static List<CatchEntry> catches() { return catches; }
    public static int stage() { return stage; }
    public static float tension() { return tension; }
    public static float progress() { return progress; }
    public static String speciesName() { return speciesName; }
    public static String locationName() { return locationName; }
    public static String notice() { return System.currentTimeMillis() <= noticeUntilMs ? notice : ""; }
    public static CatchEntry recentCatch() { return System.currentTimeMillis() <= recentCatchUntilMs ? recentCatch : null; }
}
