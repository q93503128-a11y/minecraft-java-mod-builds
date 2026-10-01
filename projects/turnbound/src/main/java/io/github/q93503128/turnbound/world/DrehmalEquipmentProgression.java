package io.github.q93503128.turnbound.world;

import java.util.Set;
import java.util.UUID;

/** Shared server-authoritative shop progression for the Drehmal production world. */
final class DrehmalEquipmentProgression {
    private DrehmalEquipmentProgression() {}

    static int shopChapter(UUID playerId) {
        if (playerId == null) return 1;
        int averageLevel = CampaignProgressStore.averageActivePartyLevel(playerId);
        Set<String> clears = CampaignProgressStore.snapshot(playerId).clearedEncounters();
        boolean tier2 = averageLevel >= 8
                || clears.contains("CV_WARNING_CAVE_ELITE")
                || clears.contains("CV_BRIAR_STAG")
                || clears.contains("AV_ROAD_PATROL")
                || clears.contains("AV_ROAD_HOUNDS")
                || clears.contains("AV_RELAY_SENTRIES");
        return tier2 ? 2 : 1;
    }

    static boolean tier2Unlocked(UUID playerId) {
        return shopChapter(playerId) >= 2;
    }
}
