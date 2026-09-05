package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.MetaUiCodec;
import io.github.q93503128.turnbound.world.MetaUiSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetaPresentationBoundaryTest {
    @Test
    void translatesRegionCodesAndHidesRegionQuestIdentifiers() {
        var region = new MetaUiSnapshot.RegionQuestRow("RQ_M01", "MEADOW", false, false, "UNRESOLVED");
        ClientMetaState.update(snapshot(List.of(), List.of(), List.of(region)));

        var row = ClientMetaState.snapshot().regionQuests().getFirst();
        assertEquals("남문 초원", row.region());
        assertFalse(row.objectiveSpecified());
        assertEquals("남문 초원 지역 임무", row.id());
        assertFalse(row.id().contains("RQ_"));
    }

    @Test
    void hidesEnglishChallengeLabelsAtTheClientBoundary() {
        var challenge = new MetaUiSnapshot.ChallengeRow("CH08", 8, "Gauge delay total 800", false, false, "CANON_GAP");
        ClientMetaState.update(snapshot(List.of(), List.of(challenge), List.of()));

        var row = ClientMetaState.snapshot().challenges().getFirst();
        assertEquals("행동 게이지 지연 합계 800", row.label());
        // The unresolved marker remains internal client state and is not rendered by the challenge list.
        assertEquals("CANON_GAP", row.unresolvedReason());
    }

    @Test
    void hidesHardAndRiftInternalIdentifiersFromDisplayLabels() {
        var hard = new MetaUiSnapshot.EndgameRow("HARD_B01", "HARD", "B01 Hard", true, false, 20, false);
        var rift = new MetaUiSnapshot.EndgameRow("RIFT_F10", "RIFT", "Rift Gate Floor 10", true, false, 24, true);
        ClientMetaState.update(snapshot(List.of(hard, rift), List.of(), List.of()));

        var hardRow = ClientMetaState.snapshot().endgame().get(0);
        var riftRow = ClientMetaState.snapshot().endgame().get(1);
        assertTrue(hardRow.label().contains("하드"));
        assertFalse(hardRow.label().contains("B01"));
        assertEquals("균열 관문 10층", riftRow.label());
        assertFalse(riftRow.label().contains("RIFT"));
        assertFalse(riftRow.label().contains("Floor"));
    }

    private static String snapshot(
            List<MetaUiSnapshot.EndgameRow> endgame,
            List<MetaUiSnapshot.ChallengeRow> challenges,
            List<MetaUiSnapshot.RegionQuestRow> regions) {
        var snapshot = new MetaUiSnapshot(
                0, 0, 0, 0, 0, false, 0, false,
                List.of(), List.of(), List.of(), List.of(),
                endgame, challenges, regions,
                List.of(), List.of(), List.of(), List.of());
        return MetaUiCodec.encode(snapshot);
    }
}
