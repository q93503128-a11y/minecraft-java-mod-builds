package dev.moonseungjun.fishinggame.client;

import java.util.List;

import dev.moonseungjun.fishinggame.fishing.FishCatalog;
import dev.moonseungjun.fishinggame.fishing.FishSizeGrade;
import dev.moonseungjun.fishinggame.fishing.FishSpecies;
import dev.moonseungjun.fishinggame.network.FishingStatePayload;
import dev.moonseungjun.fishinggame.network.ProfileSnapshotPayload;
import dev.moonseungjun.fishinggame.profile.CatchEntry;
import dev.moonseungjun.fishinggame.profile.FishRecord;

public final class ClientFishingState {
    private static int coins;
    private static int rodTier;
    private static List<CatchEntry> catches = List.of();
    private static List<FishRecord> records = List.of();
    private static int stage = 3;
    private static float tension;
    private static float progress;
    private static String speciesName = "";
    private static String locationName = "청람 호수";
    private static String notice = "";
    private static long noticeUntilMs;
    private static boolean profileInitialized;
    private static RecentCatchPresentation recentCatch;
    private static long recentCatchUntilMs;

    private ClientFishingState() {
    }

    public static void apply(ProfileSnapshotPayload payload) {
        List<CatchEntry> incoming = List.copyOf(payload.catches());
        List<FishRecord> previousRecords = records;

        if (profileInitialized && stage == 2 && incoming.size() > catches.size() && !incoming.isEmpty()) {
            CatchEntry entry = incoming.getLast();
            FishSpecies species = FishCatalog.byId(entry.speciesId());
            FishRecord previous = recordFor(previousRecords, entry.speciesId());
            boolean firstDiscovery = previous == null;
            boolean newWeightRecord = previous != null && entry.weightGrams() > previous.bestWeightGrams();
            boolean newLengthRecord = previous != null && entry.lengthMm() > previous.bestLengthMm();
            recentCatch = new RecentCatchPresentation(
                    entry,
                    FishSizeGrade.classify(species, entry.weightGrams(), entry.lengthMm()),
                    firstDiscovery,
                    newWeightRecord,
                    newLengthRecord
            );
            recentCatchUntilMs = System.currentTimeMillis() + 5200L;
        }

        coins = payload.coins();
        rodTier = payload.rodTier();
        catches = incoming;
        records = List.copyOf(payload.records());
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

    private static FishRecord recordFor(List<FishRecord> source, String speciesId) {
        for (FishRecord record : source) {
            if (record.speciesId().equals(speciesId)) return record;
        }
        return null;
    }

    public static int coins() { return coins; }
    public static int rodTier() { return rodTier; }
    public static List<CatchEntry> catches() { return catches; }
    public static List<FishRecord> records() { return records; }
    public static int stage() { return stage; }
    public static float tension() { return tension; }
    public static float progress() { return progress; }
    public static String speciesName() { return speciesName; }
    public static String locationName() { return locationName; }
    public static String notice() { return System.currentTimeMillis() <= noticeUntilMs ? notice : ""; }
    public static RecentCatchPresentation recentCatch() {
        return System.currentTimeMillis() <= recentCatchUntilMs ? recentCatch : null;
    }
}
