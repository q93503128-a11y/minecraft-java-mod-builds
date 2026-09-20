package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.CharacterGrowthRules;
import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import io.github.q93503128.turnbound.progression.QuestProgress;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignSaveCodecTest {
    @Test
    void campaignSaveRoundTripsEveryAuthoritativeField() {
        EquipmentInventory inventory = EquipmentInventory.empty();
        var weapon = inventory.grant("W03");
        inventory.equip("P01", weapon.instanceId(), 4);
        inventory.grantChoiceToken("T2", 1);

        Set<String> owned = Set.of("P01", "P03", "P04", "F03", "P08", "P05");
        Map<String, CharacterProgression.State> characters = new LinkedHashMap<>();
        Map<String, CharacterGrowthRules.State> growth = new LinkedHashMap<>();
        for (String id : owned) {
            characters.put(id, new CharacterProgression.State(1, 0));
            growth.put(id, CharacterGrowthRules.initial(id));
        }
        characters.put("P01", new CharacterProgression.State(8, 33));
        characters.put("P08", new CharacterProgression.State(3, 77));
        growth.put("P05", new CharacterGrowthRules.State(5, false, true, false));
        QuestProgress.Snapshot quests = new QuestProgress.Snapshot(
                Set.of("MQ_P00_01_arrival"),
                List.of("MQ_P00_02_first_party"),
                Set.of("MENU_E", "MENU_P"),
                Map.of("REGION_TIER_CHEST", 1),
                Map.of("MQ_C02_02_root_wall", 1),
                Map.of("MQ_C01_01_patrol", Set.of("ENC_M01")));

        List<PlayerProfile.SummonHistory> history = List.of(
                new PlayerProfile.SummonHistory("P05", 4, true, 0, 0),
                new PlayerProfile.SummonHistory("P01", 4, false, 60, 1));
        List<List<String>> presets = List.of(
                List.of("P01", "P03", "P04", "F03"),
                List.of("P05", "P08", "P03"),
                List.of());
        CampaignProgressStore.Snapshot snapshot = new CampaignProgressStore.Snapshot(
                new PlayerProfile.Snapshot(17_000, 2_100, 410, 2, owned, 37, true, true, history, presets),
                characters, growth, inventory.snapshot(), quests,
                Set.of("ENC_M01", "BATTLE_B01"),
                Set.of("P99_ORPHAN"), Set.of("OLD_EQUIPMENT"));

        CampaignProgressStore.Snapshot decoded = CampaignSaveCodec.decode(CampaignSaveCodec.encode(snapshot));
        assertEquals(snapshot, decoded);
        assertEquals(history, decoded.profile().summonHistory());
        assertEquals(presets, decoded.profile().partyPresets());
    }

    @Test
    void schemaOneMigratesMissingGrowthEquipmentQuestArchiveAndPresetsToCanonicalDefaults() {
        String old = """
                {
                  "schemaVersion": 1,
                  "profile": {
                    "gold": 5000,
                    "summonCrystal": 0,
                    "starEssence": 0,
                    "awakeningCore": 0,
                    "ownedCharacters": ["P01", "P08", "REMOVED_CHARACTER"],
                    "fiveStarPity": 0,
                    "starterArchiveUnlocked": false,
                    "starterArchiveUsed": false
                  },
                  "characters": {"P01":{"level":7,"xp":15}},
                  "clearedEncounters": []
                }
                """;

        CampaignProgressStore.Snapshot migrated = CampaignSaveCodec.decode(old);
        assertEquals(4, migrated.growth().get("P01").currentStar());
        assertEquals(3, migrated.growth().get("P08").currentStar());
        assertEquals(new CharacterProgression.State(1, 0), migrated.characters().get("P08"));
        assertTrue(migrated.equipment().items().isEmpty());
        assertTrue(migrated.quests().completed().isEmpty());
        assertTrue(migrated.profile().summonHistory().isEmpty());
        assertEquals(List.of(List.of(), List.of(), List.of()), migrated.profile().partyPresets());
        assertTrue(migrated.orphanedCharacterIds().contains("REMOVED_CHARACTER"));
    }

    @Test
    void schemaFourPlusTwentyEquipmentMigratesToPlusTenAndRefundsRetiredSpend() {
        String legacy = """
                {
                  "schemaVersion": 4,
                  "profile": {
                    "gold": 1000,
                    "summonCrystal": 0,
                    "starEssence": 0,
                    "awakeningCore": 4,
                    "ownedCharacters": ["P01"],
                    "fiveStarPity": 0,
                    "starterArchiveUnlocked": false,
                    "starterArchiveUsed": false
                  },
                  "characters": {"P01":{"level":60,"xp":0}},
                  "growth": {"P01":{"currentStar":6,"awakened":false,"characterQuestComplete":true,"signatureTrialCleared":false}},
                  "equipment": {
                    "nextSerial": 2,
                    "items": [{"instanceId":"eq_00000001","itemId":"W01","enhancementLevel":20}],
                    "pendingRewards": [],
                    "loadouts": {},
                    "choiceTokens": {}
                  },
                  "quests": {},
                  "clearedEncounters": []
                }
                """;

        CampaignProgressStore.Snapshot migrated = CampaignSaveCodec.decode(legacy);
        assertEquals(10, migrated.equipment().items().get("eq_00000001").enhancementLevel());
        assertEquals(1_000 + EquipmentInventory.legacyOverflowRefund("W01", 20), migrated.profile().gold());
        assertEquals(4, migrated.profile().awakeningCore(), "legacy field is preserved but no longer active");
        assertEquals(6, migrated.growth().get("P01").currentStar(), "legacy star data remains readable");
    }

    @Test
    void unknownSchemaIsRejectedInsteadOfSilentlyResettingProgress() {
        String json = CampaignSaveCodec.encode(new CampaignProgressStore.Snapshot(
                new PlayerProfile.Snapshot(5_000, 0, 0, 0, Set.of("P01"), 0, false, false),
                Map.of("P01", new CharacterProgression.State(1, 0)),
                Map.of("P01", CharacterGrowthRules.initial("P01")), EquipmentInventory.Snapshot.empty(), QuestProgress.Snapshot.empty(),
                Set.of(), Set.of(), Set.of()));
        assertThrows(IllegalStateException.class,
                () -> CampaignSaveCodec.decode(json.replace("\"schemaVersion\": 5", "\"schemaVersion\": 999")));
    }
}
