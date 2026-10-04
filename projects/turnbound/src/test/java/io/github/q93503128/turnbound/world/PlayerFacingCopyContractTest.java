package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlayerFacingCopyContractTest {
    @Test
    void fieldCopyRemovesInternalQuestEncounterAndDevelopmentTokens() {
        String raw = "MQ_C04_02 BATTLE_B04 E014 · Director Iven · Rift Gate / Hard Boss / Signature Trial / Awakening · CANON_GAP";
        String shown = FieldUiSnapshot.playerFacingText(raw);

        for (String forbidden : List.of("MQ_", "BATTLE_B", "E014", "Director Iven", "Rift Gate",
                "Hard Boss", "Signature Trial", "Awakening", "CANON_GAP")) {
            assertFalse(shown.contains(forbidden), () -> "player-facing copy leaked " + forbidden + ": " + shown);
        }
        assertTrue(shown.contains("콜바크"));
        assertTrue(shown.contains("용암굴착수"));
        assertTrue(shown.contains("총괄관 아이븐"));
        assertTrue(shown.contains("균열문"));
    }

    @Test
    void developmentStageAndLogCopyIsDroppedAtPresentationBoundary() {
        for (String raw : List.of(
                "DEBUG battle camera",
                "developer note",
                "prototype UI",
                "temporary route",
                "legacy map",
                "alpha build",
                "TODO actor",
                "IMPLEMENTATION_BRIDGE",
                "FALLBACK",
                "TURN_READY pulse=120",
                "P0 diagnostic",
                "P4 content gate")) {
            assertEquals("", FieldUiSnapshot.playerFacingText(raw), () -> "development copy leaked: " + raw);
        }
    }

    @Test
    void legacyFullMapKeyPhrasesNormalizeToJWithoutChangingMinimapCopy() {
        for (String raw : List.of(
                "M 지도에서 목표를 확인하십시오.",
                "M키 지도에서 상인을 찾으십시오.",
                "M 키 지도에서 길을 찾으십시오.",
                "지도 M키를 눌러 위치를 확인하십시오.",
                "지도 M 키를 눌러 위치를 확인하십시오.")) {
            String normalized = PlayerFacingCopyRules.normalizeMapKeys(raw);
            assertFalse(normalized.contains("M 지도"));
            assertFalse(normalized.contains("M키 지도"));
            assertFalse(normalized.contains("M 키 지도"));
            assertTrue(normalized.contains("J 전체 지도"));
        }
        assertEquals("M 미니맵을 켜거나 끕니다.",
                PlayerFacingCopyRules.normalizeMapKeys("M 미니맵을 켜거나 끕니다."));
    }

    @Test
    void playerFacingSourceAndResourcesContainNoStaleFullMapMKeyCopy() throws IOException {
        List<String> stale = List.of("M 지도", "M키 지도", "M 키 지도", "지도 M키", "지도 M 키");
        for (Path root : List.of(Path.of("src/main/java"), Path.of("src/main/resources"))) {
            assertTrue(Files.isDirectory(root), () -> "missing source root: " + root);
            try (Stream<Path> files = Files.walk(root)) {
                for (Path path : files.filter(Files::isRegularFile).toList()) {
                    String name = path.getFileName().toString();
                    if (name.equals("PlayerFacingCopyRules.java")) continue;
                    if (!(name.endsWith(".java") || name.endsWith(".json") || name.endsWith(".txt")
                            || name.endsWith(".toml") || name.endsWith(".mcmeta"))) continue;
                    String content = Files.readString(path);
                    for (String token : stale) {
                        assertFalse(content.contains(token),
                                () -> "stale fullscreen-map copy '" + token + "' in " + path);
                    }
                }
            }
        }
    }

    @Test
    void regionQuestWireTitleNeverFallsBackToCanonicalId() {
        String unknown = MetaUiCodec.regionQuestTitle("RQ_INTERNAL_DEV_99", "GLOAMWOOD");
        assertFalse(unknown.contains("RQ_"));
        assertTrue(unknown.contains("그늘숲"));
    }
}
