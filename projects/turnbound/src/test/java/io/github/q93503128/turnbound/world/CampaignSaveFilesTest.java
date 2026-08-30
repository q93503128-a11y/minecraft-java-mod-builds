package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.CharacterGrowthRules;
import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignSaveFilesTest {
    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadRoundTripsCampaignSnapshot() throws Exception {
        Path primary = tempDir.resolve("player.json");
        CampaignProgressStore.Snapshot snapshot = sample(17_000, 12);
        CampaignSaveFiles.save(primary, snapshot);
        var loaded = CampaignSaveFiles.load(primary).orElseThrow();
        assertEquals(snapshot, loaded.snapshot());
        assertFalse(loaded.recoveredBackup());
    }

    @Test
    void corruptPrimaryRecoversLastGoodBackupWithoutDestroyingIt() throws Exception {
        Path primary = tempDir.resolve("player.json");
        CampaignProgressStore.Snapshot first = sample(17_000, 12);
        CampaignProgressStore.Snapshot second = sample(19_000, 21);

        CampaignSaveFiles.save(primary, first);
        CampaignSaveFiles.save(primary, second);
        Path backup = CampaignSaveFiles.backup(primary);
        assertTrue(Files.exists(backup));
        assertEquals(first, CampaignSaveCodec.decode(Files.readString(backup, StandardCharsets.UTF_8)));

        Files.writeString(primary, "{broken", StandardCharsets.UTF_8);
        var recovered = CampaignSaveFiles.load(primary).orElseThrow();
        assertTrue(recovered.recoveredBackup());
        assertEquals(first, recovered.snapshot());

        Path quarantined = CampaignSaveFiles.quarantinePrimary(primary);
        assertTrue(Files.exists(quarantined));
        assertTrue(Files.exists(backup));
        assertEquals(first, CampaignSaveCodec.decode(Files.readString(backup, StandardCharsets.UTF_8)));

        CampaignSaveFiles.save(primary, recovered.snapshot());
        assertEquals(first, CampaignSaveFiles.load(primary).orElseThrow().snapshot());
        assertEquals(first, CampaignSaveCodec.decode(Files.readString(backup, StandardCharsets.UTF_8)));
    }

    private static CampaignProgressStore.Snapshot sample(long gold, int pity) {
        Set<String> owned = Set.of("P01", "P03", "P04", "F03", "P08");
        Map<String, CharacterProgression.State> characters = new LinkedHashMap<>();
        Map<String, CharacterGrowthRules.State> growth = new LinkedHashMap<>();
        for (String id : owned) {
            characters.put(id, new CharacterProgression.State(1, 0));
            growth.put(id, CharacterGrowthRules.initial(id));
        }
        characters.put("P01", new CharacterProgression.State(5, 10));
        return new CampaignProgressStore.Snapshot(
                new PlayerProfile.Snapshot(gold, 3_000, 60, 0, owned, pity, true, false),
                characters, growth, EquipmentInventory.Snapshot.empty(),
                Set.of("southgate_enc_m01", "southgate_b01_graul"), Set.of(), Set.of());
    }
}
