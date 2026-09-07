package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ExpeditionReviewHistoryTest {
    private static final ContentId REGION = ContentId.rift("region/vertical_slice_01");
    private static final ContentId CONTRACT = ContentId.rift("contract/salvage_recovery");
    private static final UUID OWNER_A = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID OWNER_B = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

    @Test
    void historicalPressureAccountsForLaterSuccessesFromOtherPlayers() {
        ExpeditionRun firstSuccess = extracted(1L, OWNER_A, 10L, 20L);
        ExpeditionRun failedMiddle = ExpeditionRun.preparing(2L, REGION, CONTRACT, OWNER_A, "fp", 30L)
            .deploy().fail(35L, ExpeditionRun.EndReason.PLAYER_ABORT);
        ExpeditionRun laterSuccess = extracted(3L, OWNER_B, 40L, 50L);
        List<ExpeditionRun> history = List.of(firstSuccess, failedMiddle, laterSuccess);

        assertEquals(0, ExpeditionReviewHistory.pressureAtStart(firstSuccess, history, 2));
        assertEquals(1, ExpeditionReviewHistory.pressureAtStart(failedMiddle, history, 2));
        assertEquals(1, ExpeditionReviewHistory.pressureAtStart(laterSuccess, history, 2));
    }

    @Test
    void activeRunUsesCurrentPressureAndHistoryDriftFailsLoudly() {
        ExpeditionRun previous = extracted(1L, OWNER_A, 10L, 20L);
        ExpeditionRun active = ExpeditionRun.preparing(2L, REGION, CONTRACT, OWNER_B, "fp", 30L).deploy();
        List<ExpeditionRun> history = List.of(previous, active);

        assertEquals(1, ExpeditionReviewHistory.pressureAtStart(active, history, 1));
        assertThrows(IllegalArgumentException.class, () -> ExpeditionReviewHistory.pressureAtStart(active, List.of(previous), 1));
        assertThrows(IllegalStateException.class, () -> ExpeditionReviewHistory.pressureAtStart(previous, history, 0));
    }

    private static ExpeditionRun extracted(long sequence, UUID owner, long start, long end) {
        return ExpeditionRun.preparing(sequence, REGION, CONTRACT, owner, "fp", start)
            .deploy().requestExtraction().extract(end);
    }
}
