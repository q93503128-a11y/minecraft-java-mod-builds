package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TurnboundWorldEncounterClearTest {
    @Test
    void optionalGraulUsesEncounterClearKeyInsteadOfLegacyB01BossKey() {
        assertNull(WorldEncounterClearPolicy.legacyBossId(DrehmalWorldBossPlacementRules.ENCOUNTER_ID));
        assertEquals("ENCOUNTER_CLEAR:" + DrehmalWorldBossPlacementRules.ENCOUNTER_ID,
                WorldEncounterClearPolicy.claimKey(DrehmalWorldBossPlacementRules.ENCOUNTER_ID));

        assertEquals("B01", WorldEncounterClearPolicy.legacyBossId("BATTLE_B01"));
        assertNotEquals(WorldEncounterClearPolicy.claimKey(DrehmalWorldBossPlacementRules.ENCOUNTER_ID),
                WorldEncounterClearPolicy.claimKey("BATTLE_B01"));
    }
}
