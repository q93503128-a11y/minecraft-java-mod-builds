package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.List;

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
    void regionQuestWireTitleNeverFallsBackToCanonicalId() {
        String unknown = MetaUiCodec.regionQuestTitle("RQ_INTERNAL_DEV_99", "GLOAMWOOD");
        assertFalse(unknown.contains("RQ_"));
        assertTrue(unknown.contains("그늘숲"));
    }
}
