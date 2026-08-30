package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.CharacterGrowthRules;
import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
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

        CampaignProgressStore.Snapshot snapshot = new CampaignProgressStore.Snapshot(
                new PlayerProfile.Snapshot(17_000, 2_100, 410, 2, owned, 37, true, true),
                characters, growth, inventory.snapshot(),
                Set.of("southgate_enc_m01", "southgate_b01_graul"),
                Set.of("P99_ORPHAN"), Set.of("OLD_EQUIPMENT"));

        assertEquals(snapshot, CampaignSaveCodec.decode(CampaignSaveCodec.encode(snapshot)));
    }

    @Test
    void schemaOneMigratesMissingGrowthAndEquipmentToCanonicalDefaults() {
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
        assertTrue(migrated.orphanedCharacterIds().contains("REMOVED_CHARACTER"));
    }

    @Test
    void unknownSchemaIsRejectedInsteadOfSilentlyResettingProgress() {
        String json = CampaignSaveCodec.encode(new CampaignProgressStore.Snapshot(
                new PlayerProfile.Snapshot(5_000, 0, 0, 0, Set.of("P01"), 0, false, false),
                Map.of("P01", new CharacterProgression.State(1, 0)),
                Map.of("P01", CharacterGrowthRules.initial("P01")), EquipmentInventory.Snapshot.empty(),
                Set.of(), Set.of(), Set.of()));
        assertThrows(IllegalStateException.class,
                () -> CampaignSaveCodec.decode(json.replace("\"schemaVersion\": 4", "\"schemaVersion\": 999")));
    }
}
