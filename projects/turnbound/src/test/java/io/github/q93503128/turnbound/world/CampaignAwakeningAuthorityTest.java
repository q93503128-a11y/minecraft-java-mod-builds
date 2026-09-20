package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.CharacterGrowthRules;
import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.progression.GrowthRulesV1;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import io.github.q93503128.turnbound.progression.QuestProgress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void legacyMaterialAwakeningStaysUnavailableWithoutDeveloperFacingCanonGapCopy() {
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
        assertEquals("현재 이 동료의 각성 경로는 열려 있지 않습니다.", error.getMessage());
        assertFalse(CampaignProgressStore.growth(playerId, "F03").awakened());
    }

    @Test
    void authoredHeroRequiresLevelSixtyAndPersonalQuestInsteadOfStarSixOrSignatureTrial() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> CampaignProgressStore.awaken(playerId, "P01"));
        assertTrue(error.getMessage().contains("Lv60 and character quest completion"), error.getMessage());
    }

    @Test
    void awakeningSpendsGoldAndDoesNotConsumeLegacyCoreOrRequireSignatureTrial() {
        restoreReady(GrowthRulesV1.awakeningGoldCost(), 7);
        var awakened = CampaignProgressStore.awaken(playerId, "P01");

        assertTrue(awakened.awakened());
        assertEquals(4, awakened.currentStar());
        assertFalse(awakened.signatureTrialCleared());
        assertEquals(0, CampaignProgressStore.gold(playerId));
        assertEquals(7, CampaignProgressStore.snapshot(playerId).profile().awakeningCore());
    }

    @Test
    void awakeningFailsClosedWhenGoldIsShort() {
        restoreReady(GrowthRulesV1.awakeningGoldCost() - 1L, 99);
        assertThrows(IllegalStateException.class, () -> CampaignProgressStore.awaken(playerId, "P01"));
        assertFalse(CampaignProgressStore.growth(playerId, "P01").awakened());
        assertEquals(99, CampaignProgressStore.snapshot(playerId).profile().awakeningCore());
    }

    private void restoreReady(long gold, long legacyCore) {
        CampaignProgressStore.restore(playerId, new CampaignProgressStore.Snapshot(
                new PlayerProfile.Snapshot(gold, 0, 0, legacyCore, Set.of("P01"), 0, false, false),
                Map.of("P01", new CharacterProgression.State(60, 0)),
                Map.of("P01", new CharacterGrowthRules.State(4, false, true, false)),
                EquipmentInventory.Snapshot.empty(), QuestProgress.Snapshot.empty(),
                Set.of(), Set.of(), Set.of()));
    }
}
