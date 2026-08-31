package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.SouthgateEncounterCatalog;

import java.util.HashSet;
import java.util.Set;

/** Pure projection from persistent campaign clears into the currently implemented starter field slice. */
final class StarterFieldProgress {
    private StarterFieldProgress() {}

    static Set<String> project(Set<String> persistedClears) {
        Set<String> result = new HashSet<>();
        if (persistedClears == null) return result;
        if (persistedClears.contains(SouthgateEncounterCatalog.ENC_M01)) result.add(SouthgateEncounterCatalog.ENC_M01);
        if (persistedClears.contains(SouthgateEncounterCatalog.ENC_M02)) result.add(SouthgateEncounterCatalog.ENC_M02);
        return Set.copyOf(result);
    }
}
