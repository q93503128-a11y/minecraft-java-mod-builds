package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpeditionRestartReconcilerTest {
    private static final ContentId REGION = ContentId.rift("region/region_01");
    private static final ContentId CONTRACT = ContentId.rift("contract/region_01_salvage_recovery");
    private static final UUID OWNER = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Test
    void selectsEveryNonTerminalRunInStableSequenceOrder() {
        ExpeditionRun preparing = run(4);
        ExpeditionRun deployed = run(1).deploy();
        ExpeditionRun extractionRequested = run(2).deploy().requestExtraction();
        ExpeditionRun extracted = run(3).deploy().requestExtraction().extract(20L);
        ExpeditionRun failed = run(5).deploy().fail(21L, ExpeditionRun.EndReason.PLAYER_ABORT);

        List<ExpeditionRun> candidates = ExpeditionRestartReconciler.nonTerminalRuns(
            List.of(failed, preparing, extracted, extractionRequested, deployed)
        );

        assertEquals(List.of(1L, 2L, 4L), candidates.stream().map(ExpeditionRun::sequence).toList());
        assertEquals(
            List.of(
                ExpeditionRun.Status.DEPLOYED,
                ExpeditionRun.Status.EXTRACTION_REQUESTED,
                ExpeditionRun.Status.PREPARING
            ),
            candidates.stream().map(ExpeditionRun::status).toList()
        );
    }

    @Test
    void ignoresWorldsThatContainOnlyTerminalHistory() {
        ExpeditionRun extracted = run(1).deploy().requestExtraction().extract(20L);
        ExpeditionRun failed = run(2).deploy().fail(21L, ExpeditionRun.EndReason.SERVER_RESTART);

        assertEquals(List.of(), ExpeditionRestartReconciler.nonTerminalRuns(List.of(extracted, failed)));
    }

    private static ExpeditionRun run(long sequence) {
        return ExpeditionRun.preparing(sequence, REGION, CONTRACT, OWNER, "fingerprint", 10L);
    }
}
