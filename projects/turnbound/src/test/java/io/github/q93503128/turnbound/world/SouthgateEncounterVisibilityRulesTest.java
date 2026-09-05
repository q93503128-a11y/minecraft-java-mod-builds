package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SouthgateEncounterVisibilityRulesTest {
    @Test
    void chapterOneVisibilityTracksProjectedClears() {
        assertTrue(SouthgateEncounterVisibilityRules.unlocked("ENC_M01", Set.of()));
        assertTrue(SouthgateEncounterVisibilityRules.unlocked("ENC_M02", Set.of()));
        assertFalse(SouthgateEncounterVisibilityRules.unlocked("ENC_M03", Set.of()));
        assertFalse(SouthgateEncounterVisibilityRules.unlocked("BATTLE_B01", Set.of()));

        Set<String> starter = Set.of("ENC_M01", "ENC_M02");
        assertTrue(SouthgateEncounterVisibilityRules.unlocked("ENC_M03", starter));
        assertTrue(SouthgateEncounterVisibilityRules.unlocked("ENC_M04", starter));
        assertTrue(SouthgateEncounterVisibilityRules.unlocked("ENC_M05", starter));
        assertFalse(SouthgateEncounterVisibilityRules.unlocked("BATTLE_B01", starter));

        Set<String> bossReady = Set.of("ENC_M01", "ENC_M02", "ENC_M04");
        assertTrue(SouthgateEncounterVisibilityRules.unlocked("BATTLE_B01", bossReady));
    }
}
