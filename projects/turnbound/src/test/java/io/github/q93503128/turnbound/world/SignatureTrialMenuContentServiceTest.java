package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.CharacterGrowthRules;
import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.progression.GrowthRulesV1;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import io.github.q93503128.turnbound.progression.QuestProgress;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureTrialMenuContentServiceTest {
    @Test
    void serverProjectsCompatibilityRowsWithoutExposingGlobalAwakeningCore() {
        UUID playerId = UUID.randomUUID();
        try {
            List<String> rows = SignatureTrialMenuContentService.encode(playerId).lines()
                    .filter(line -> line.startsWith("T|"))
                    .toList();

            assertEquals(12, rows.size());
            Set<String> ids = rows.stream().map(line -> line.split("\\|", -1)[1]).collect(Collectors.toSet());
            for (int i = 1; i <= 8; i++) assertTrue(ids.contains("P0" + i));
            for (int i = 1; i <= 4; i++) assertTrue(ids.contains("F0" + i));
            for (String line : rows) assertEquals("0", line.split("\\|", -1)[12], "legacy core wire slot stays zero");
        } finally {
            CampaignProgressStore.resetForTests(playerId);
        }
    }

    @Test
    void legacyMaterialCharactersRemainUnavailableWithoutDeveloperFacingCopy() {
        UUID playerId = UUID.randomUUID();
        try {
            List<String> rows = SignatureTrialMenuContentService.encode(playerId).lines()
                    .filter(line -> line.startsWith("T|F0"))
                    .toList();

            assertEquals(4, rows.size());
            for (String line : rows) {
                String[] p = line.split("\\|", -1);
                assertEquals(17, p.length);
                assertEquals("0", p[14]);
                assertFalse(p[16].contains("CANON GAP"));
                assertTrue(p[16].contains("열려 있지 않습니다"));
            }
        } finally {
            CampaignProgressStore.resetForTests(playerId);
        }
    }

    @Test
    void awakeningReadinessUsesLevelQuestAndGoldNotSignatureTrialOrCore() {
        UUID playerId = UUID.randomUUID();
        try {
            CampaignProgressStore.restore(playerId, new CampaignProgressStore.Snapshot(
                    new PlayerProfile.Snapshot(GrowthRulesV1.awakeningGoldCost(), 0, 0, 999,
                            Set.of("P01"), 0, false, false),
                    Map.of("P01", new CharacterProgression.State(60, 0)),
                    Map.of("P01", new CharacterGrowthRules.State(4, false, true, false)),
                    EquipmentInventory.Snapshot.empty(), QuestProgress.Snapshot.empty(),
                    Set.of(), Set.of(), Set.of()));

            String[] row = SignatureTrialMenuContentService.encode(playerId).lines()
                    .filter(line -> line.startsWith("T|P01|"))
                    .findFirst().orElseThrow().split("\\|", -1);
            assertEquals("0", row[8], "signature trial can remain uncleared");
            assertEquals("0", row[12], "global Awakening Core is not exposed");
            assertEquals("1", row[14], "Awakening is ready from level + personal quest + Gold");
        } finally {
            CampaignProgressStore.resetForTests(playerId);
        }
    }
}
