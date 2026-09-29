package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalWorldBossPlacementRulesTest {
    @Test
    void worldBossRequiresAFlatOffRoadMeadowAwayFromHubAndSafetyZones() {
        assertTrue(DrehmalWorldBossPlacementRules.candidate(190, 64, 500, 1, false));
        assertFalse(DrehmalWorldBossPlacementRules.candidate(190, 20, 500, 1, false));
        assertFalse(DrehmalWorldBossPlacementRules.candidate(190, 64, 250, 1, false));
        assertFalse(DrehmalWorldBossPlacementRules.candidate(190, 64, 500, 5, false));
        assertFalse(DrehmalWorldBossPlacementRules.candidate(190, 64, 500, 1, true));
    }

    @Test
    void scoringPrefersAVisibleOffRoadFlatMeadow() {
        double ideal = DrehmalWorldBossPlacementRules.score(190, 64, 0);
        double tooFarFromRoad = DrehmalWorldBossPlacementRules.score(270, 115, 0);
        double rough = DrehmalWorldBossPlacementRules.score(190, 64, 3);
        assertTrue(ideal < tooFarFromRoad);
        assertTrue(ideal < rough);
    }
}
