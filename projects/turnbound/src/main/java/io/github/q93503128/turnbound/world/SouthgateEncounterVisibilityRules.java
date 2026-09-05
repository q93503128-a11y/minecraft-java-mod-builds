package io.github.q93503128.turnbound.world;

import java.util.Set;

/** Pure Chapter 1 field visibility rules shared by the Southgate world presentation and tests. */
final class SouthgateEncounterVisibilityRules {
    private SouthgateEncounterVisibilityRules() {}

    static boolean unlocked(String encounterId, Set<String> cleared) {
        if (encounterId == null) return false;
        Set<String> clears = cleared == null ? Set.of() : cleared;
        if ("ENC_M01".equals(encounterId) || "ENC_M02".equals(encounterId)) return true;
        if ("BATTLE_B01".equals(encounterId)) return StarterFieldProgress.bossUnlocked(clears);
        if (encounterId.startsWith("ENC_M")) return StarterFieldProgress.starterPatrolComplete(clears);
        return false;
    }
}
