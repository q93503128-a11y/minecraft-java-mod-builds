package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureTrialMenuContentServiceTest {
    @Test
    void serverProjectsAwakeningStateForAllTwelveV04Characters() {
        UUID playerId = UUID.randomUUID();
        try {
            List<String> rows = SignatureTrialMenuContentService.encode(playerId).lines()
                    .filter(line -> line.startsWith("T|"))
                    .toList();

            assertEquals(12, rows.size());
            Set<String> ids = rows.stream().map(line -> line.split("\\|", -1)[1]).collect(Collectors.toSet());
            for (int i = 1; i <= 8; i++) assertTrue(ids.contains("P0" + i));
            for (int i = 1; i <= 4; i++) assertTrue(ids.contains("F0" + i));
        } finally {
            CampaignProgressStore.resetForTests(playerId);
        }
    }

    @Test
    void materialCharactersAreServerAuthoredCanonGapRowsWithoutInventedSignatureRewards() {
        UUID playerId = UUID.randomUUID();
        try {
            List<String> rows = SignatureTrialMenuContentService.encode(playerId).lines()
                    .filter(line -> line.startsWith("T|F0"))
                    .toList();

            assertEquals(4, rows.size());
            for (String line : rows) {
                String[] p = line.split("\\|", -1);
                assertEquals(17, p.length);
                assertTrue(p[2].startsWith("없음 · 소재형 각성 경로"));
                assertEquals("1", p[7], "material characters have no personal quest prerequisite");
                assertEquals("0", p[8], "there is no material Signature Trial first clear");
                assertEquals("0", p[9], "there is no authored material Signature Trial encounter");
                assertEquals("0", p[10], "there is no material Signature Equipment reward");
                assertEquals("0", p[11], "there is no pending material Signature Equipment reward");
                assertEquals("0", p[14], "canon-gap material Awakening must stay disabled");
                assertTrue(p[15].contains("전용 장비와 개인 퀘스트 없음"));
                assertTrue(p[16].startsWith("CANON GAP"));
            }
        } finally {
            CampaignProgressStore.resetForTests(playerId);
        }
    }
}
