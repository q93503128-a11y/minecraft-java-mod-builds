package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignAwakeningAuthorityTest {
    private final UUID playerId = UUID.randomUUID();

    @AfterEach
    void cleanup() {
        CampaignProgressStore.resetForTests(playerId);
    }

    @Test
    void legacyMaterialAwakeningCannotBypassCanonGap() {
        CampaignProgressStore.restore(playerId, CampaignSaveCodec.decode("""
                {
                  "schemaVersion": 1,
                  "profile": {
                    "gold": 5000,
                    "summonCrystal": 0,
                    "starEssence": 0,
                    "awakeningCore": 0,
                    "ownedCharacters": ["P01", "F03"],
                    "fiveStarPity": 0,
                    "starterArchiveUnlocked": false,
                    "starterArchiveUsed": false
                  },
                  "characters": {"P01":{"level":1,"xp":0},"F03":{"level":1,"xp":0}},
                  "clearedEncounters": []
                }
                """));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> CampaignProgressStore.awaken(playerId, "F03"));

        assertTrue(error.getMessage().startsWith("CANON GAP"), error.getMessage());
        assertFalse(CampaignProgressStore.growth(playerId, "F03").awakened());
    }

    @Test
    void authoredHeroStillReachesNormalGrowthValidation() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> CampaignProgressStore.awaken(playerId, "P01"));

        assertTrue(error.getMessage().contains("Lv60 / ★6 / Signature Trial clear"), error.getMessage());
    }
}
