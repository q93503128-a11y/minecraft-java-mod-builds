package io.github.q93503128.turnbound.world;

import java.util.Set;

/** Pure player-facing filtering rules for the current Drehmal production route. */
final class DrehmalMetaPresentationRules {
    private static final Set<String> LEGACY_WORLD_CHALLENGES = Set.of(
            "CH12_KILL_E003_BEFORE_EXPLOSION",
            "CH13_SURVIVE_E003_EXPLOSION",
            "CH15_HARD_B01",
            "CH16_HARD_B02",
            "CH17_HARD_B03",
            "CH18_HARD_B04",
            "CH19_HARD_B05",
            "CH20_RIFT_F30");

    private DrehmalMetaPresentationRules() {}

    static boolean challengeVisible(String id) {
        return id != null && !LEGACY_WORLD_CHALLENGES.contains(id);
    }

    static boolean firstHubShopVisible(String tier) {
        return "T1".equals(tier);
    }

    static boolean combatantCodexVisible(boolean discovered) {
        return discovered;
    }
}
