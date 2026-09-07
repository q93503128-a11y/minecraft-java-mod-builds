package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ExpeditionEvidenceCheckpointTest {
    private static final ContentId REGION = ContentId.rift("region/region_01");
    private static final ContentId CONTRACT = ContentId.rift("contract/region_01_salvage_recovery");
    private static final ContentId RESOURCE = ContentId.rift("resource/region_01_salvage");
    private static final UUID OWNER = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Test
    void lifecycleEvidenceRemainsBoundedOrderedAndTerminal() {
        ExpeditionRun run = ExpeditionRun.preparing(31L, REGION, CONTRACT, OWNER, "fp", 100L).deploy();
        run = run.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.DEPLOYED, 100L, 0, 3));
        run = run.recover(RESOURCE, 1);
        run = run.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.SALVAGE_RECOVERED, 110L, 1, 3));
        run = run.recover(RESOURCE, 2);
        run = run.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.PRE_EXTRACTION, 120L, 3, 0));
        run = run.requestExtraction().extract(121L);
        run = run.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.EXTRACTED, 121L, 3, 0));

        assertEquals(4, run.evidenceTrail().size());
        assertEquals(ExpeditionEvidenceCheckpoint.Stage.DEPLOYED, run.evidenceTrail().getFirst().stage());
        assertEquals(ExpeditionEvidenceCheckpoint.Stage.EXTRACTED, run.evidenceTrail().getLast().stage());
        assertTrue(run.evidenceTrail().getLast().reportLine(run.sequence(), run.startedGameTime()).contains("elapsedTicks=21"));
        ExpeditionRun terminal = run;
        assertThrows(IllegalStateException.class, () -> terminal.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.EXTRACTED, 122L, 3, 0)));
    }

    @Test
    void restartFailureMayRecordUnavailableThreatCountButActiveEvidenceMayNot() {
        assertThrows(IllegalArgumentException.class, () -> checkpoint(ExpeditionEvidenceCheckpoint.Stage.DEPLOYED, 100L, 0, -1));

        ExpeditionRun failed = ExpeditionRun.preparing(32L, REGION, CONTRACT, OWNER, "fp", 100L)
            .deploy().fail(105L, ExpeditionRun.EndReason.SERVER_RESTART)
            .appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.FAILED, 105L, 0, -1));

        assertEquals(-1, failed.evidenceTrail().getLast().liveThreats());
        assertTrue(failed.evidenceTrail().getLast().reportLine(failed.sequence(), failed.startedGameTime()).contains("liveThreats=unavailable"));
    }

    @Test
    void fullTrailCannotBlockGameplayAndTerminalOutcomeIsStillPreserved() {
        ExpeditionRun run = ExpeditionRun.preparing(33L, REGION, CONTRACT, OWNER, "fp", 100L).deploy();
        for (int i = 0; i < 16; i++) {
            run = run.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.SALVAGE_RECOVERED, 100L + i, 0, 3));
        }
        ExpeditionRun full = run;
        ExpeditionRun ignored = full.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.SALVAGE_RECOVERED, 116L, 0, 3));
        assertSame(full, ignored, "A diagnostic checkpoint past the bound must not mutate or fail gameplay");

        ExpeditionRun failed = full.fail(117L, ExpeditionRun.EndReason.PLAYER_ABORT)
            .appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.FAILED, 117L, 0, 2));
        assertEquals(16, failed.evidenceTrail().size());
        assertEquals(ExpeditionEvidenceCheckpoint.Stage.FAILED, failed.evidenceTrail().getLast().stage());
        assertEquals(101L, failed.evidenceTrail().getFirst().gameTime(), "Terminal evidence may evict the oldest non-terminal observation at the hard bound");
    }

    private static ExpeditionEvidenceCheckpoint checkpoint(ExpeditionEvidenceCheckpoint.Stage stage, long time, int salvage, int threats) {
        return new ExpeditionEvidenceCheckpoint(stage, time, salvage, threats, 2, 1, 0);
    }
}
