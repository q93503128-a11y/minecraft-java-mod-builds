package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnboundWorldEncounterClearTest {
    @Test
    void optionalGraulClearDoesNotMasqueradeAsLegacyB01WorldProgress() {
        TurnboundWorldSavedData data = new TurnboundWorldSavedData();

        data.recordEncounterClear(DrehmalWorldBossPlacementRules.ENCOUNTER_ID);

        assertTrue(data.encounterCleared(DrehmalWorldBossPlacementRules.ENCOUNTER_ID));
        assertFalse(data.bossCleared("B01"));
        assertFalse(data.regionUnlocked(TurnboundWorldSavedData.REGION_GLOAMWOOD));

        data.recordEncounterClear("BATTLE_B01");
        assertTrue(data.encounterCleared("BATTLE_B01"));
        assertTrue(data.bossCleared("B01"));
        assertTrue(data.regionUnlocked(TurnboundWorldSavedData.REGION_GLOAMWOOD));
    }
}
