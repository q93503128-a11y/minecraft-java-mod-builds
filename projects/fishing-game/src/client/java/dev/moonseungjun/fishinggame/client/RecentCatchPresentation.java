package dev.moonseungjun.fishinggame.client;

import dev.moonseungjun.fishinggame.fishing.FishSizeGrade;
import dev.moonseungjun.fishinggame.profile.CatchEntry;

public record RecentCatchPresentation(
        CatchEntry catchEntry,
        FishSizeGrade sizeGrade,
        boolean firstDiscovery,
        boolean newWeightRecord,
        boolean newLengthRecord
) {
    public boolean personalBest() {
        return newWeightRecord || newLengthRecord;
    }

    public String highlightText() {
        if (firstDiscovery) return "신규 도감 등록";
        if (newWeightRecord && newLengthRecord) return "개인 최고 · 무게 + 길이";
        if (newWeightRecord) return "개인 최고 · 무게";
        if (newLengthRecord) return "개인 최고 · 길이";
        return "";
    }
}
