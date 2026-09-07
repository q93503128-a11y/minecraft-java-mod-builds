package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldPlayEvidenceMetricsTest {
    private static final ContentId REGION = ContentId.rift("region_01");
    private static final ContentId CONTRACT = ContentId.rift("contract_region_01_salvage");
    private static final ContentId SALVAGE = ContentId.rift("region_01_salvage");

    @Test
    void derivesComparablePacingAndThreatMetricsFromObservedTrailOnly() {
        ExpeditionRun run = ExpeditionRun.preparing(7L, REGION, CONTRACT, UUID.randomUUID(), "fingerprint", 100L).deploy();
        run = run.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.DEPLOYED, 110L, 0, 3));
        run = run.recover(SALVAGE, 1).appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.SALVAGE_RECOVERED, 160L, 1, 3));
        run = run.recover(SALVAGE, 1).appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.SALVAGE_RECOVERED, 220L, 2, 2));
        run = run.recover(SALVAGE, 1).appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.SALVAGE_RECOVERED, 310L, 3, 1));
        run = run.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.PRE_EXTRACTION, 340L, 3, 1));
        run = run.requestExtraction().extract(360L);
        run = run.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.EXTRACTED, 360L, 3, 0));

        FieldPlayEvidenceMetrics metrics = FieldPlayEvidenceMetrics.from(run);

        assertEquals(6, metrics.checkpoints());
        assertEquals(3, metrics.salvageCheckpoints());
        assertEquals(1, metrics.extractionAttemptCheckpoints());
        assertEquals(OptionalLong.of(60L), metrics.firstSalvageElapsedTicks());
        assertEquals(OptionalLong.of(240L), metrics.preExtractionElapsedTicks());
        assertEquals(OptionalLong.of(260L), metrics.terminalElapsedTicks());
        assertEquals(OptionalLong.of(60L), metrics.minSalvageIntervalTicks());
        assertEquals(OptionalLong.of(90L), metrics.maxSalvageIntervalTicks());
        assertEquals(OptionalInt.of(0), metrics.minObservedLiveThreats());
        assertEquals(OptionalInt.of(3), metrics.maxObservedLiveThreats());
        assertEquals(OptionalInt.of(0), metrics.terminalObservedLiveThreats());
        assertEquals(OptionalInt.of(3), metrics.finalObservedSalvage());
        assertEquals("extracted", metrics.terminalStage());
        assertEquals("extraction", metrics.endReason());
        assertTrue(metrics.reportLine().contains("extractionAttempts=1"));
        assertTrue(metrics.reportLine().contains("firstSalvageTicks=60"));
        assertTrue(metrics.reportLine().contains("liveThreatsMax=3"));
    }

    @Test
    void rejectedExtractionAttemptDoesNotMasqueradeAsAcceptedPreExtraction() {
        ExpeditionRun run = ExpeditionRun.preparing(10L, REGION, CONTRACT, UUID.randomUUID(), "fingerprint", 100L).deploy();
        run = run.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.DEPLOYED, 110L, 0, 3));
        run = run.recover(SALVAGE, 1).appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.SALVAGE_RECOVERED, 150L, 1, 3));
        run = run.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.PRE_EXTRACTION, 170L, 1, 3));

        FieldPlayEvidenceMetrics activeMetrics = FieldPlayEvidenceMetrics.from(run);
        assertEquals(1, activeMetrics.extractionAttemptCheckpoints());
        assertFalse(activeMetrics.preExtractionElapsedTicks().isPresent());
        assertTrue(activeMetrics.reportLine().contains("preExtractionTicks=unavailable"));

        run = run.recover(SALVAGE, 2).appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.SALVAGE_RECOVERED, 230L, 3, 1));
        run = run.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.PRE_EXTRACTION, 260L, 3, 1));
        run = run.requestExtraction().extract(280L);
        run = run.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.EXTRACTED, 280L, 3, 1));

        FieldPlayEvidenceMetrics extractedMetrics = FieldPlayEvidenceMetrics.from(run);
        assertEquals(2, extractedMetrics.extractionAttemptCheckpoints());
        assertEquals(OptionalLong.of(160L), extractedMetrics.preExtractionElapsedTicks());
        assertTrue(extractedMetrics.reportLine().contains("extractionAttempts=2"));
    }

    @Test
    void failedRunNeverClaimsARejectedAttemptWasAcceptedExtraction() {
        ExpeditionRun run = ExpeditionRun.preparing(11L, REGION, CONTRACT, UUID.randomUUID(), "fingerprint", 100L).deploy();
        run = run.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.DEPLOYED, 110L, 0, 3));
        run = run.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.PRE_EXTRACTION, 140L, 0, 3));
        run = run.fail(180L, ExpeditionRun.EndReason.PLAYER_ABORT);
        run = run.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.FAILED, 180L, 0, 3));

        FieldPlayEvidenceMetrics metrics = FieldPlayEvidenceMetrics.from(run);
        assertEquals(1, metrics.extractionAttemptCheckpoints());
        assertFalse(metrics.preExtractionElapsedTicks().isPresent());
        assertEquals("failed", metrics.terminalStage());
        assertEquals("player_abort", metrics.endReason());
    }

    @Test
    void restartUnavailableThreatIsNotInventedAsZero() {
        ExpeditionRun run = ExpeditionRun.preparing(8L, REGION, CONTRACT, UUID.randomUUID(), "fingerprint", 200L).deploy();
        run = run.appendEvidence(checkpoint(ExpeditionEvidenceCheckpoint.Stage.DEPLOYED, 205L, 0, 4));
        run = run.fail(250L, ExpeditionRun.EndReason.SERVER_RESTART);
        run = run.appendEvidence(new ExpeditionEvidenceCheckpoint(
            ExpeditionEvidenceCheckpoint.Stage.FAILED, 250L, 0, -1, 0, 0, 2
        ));

        FieldPlayEvidenceMetrics metrics = FieldPlayEvidenceMetrics.from(run);

        assertEquals(OptionalInt.of(4), metrics.minObservedLiveThreats());
        assertEquals(OptionalInt.of(4), metrics.maxObservedLiveThreats());
        assertFalse(metrics.terminalObservedLiveThreats().isPresent());
        assertEquals("failed", metrics.terminalStage());
        assertEquals("server_restart", metrics.endReason());
        assertTrue(metrics.reportLine().contains("terminalLiveThreats=unavailable"));
    }

    @Test
    void legacyTrailStaysExplicitlyUnavailableInsteadOfReconstructed() {
        ExpeditionRun run = ExpeditionRun.preparing(9L, REGION, CONTRACT, "fingerprint", 300L)
            .deploy()
            .fail(400L, ExpeditionRun.EndReason.OTHER_FAILURE);

        FieldPlayEvidenceMetrics metrics = FieldPlayEvidenceMetrics.from(run);

        assertEquals(0, metrics.checkpoints());
        assertEquals(0, metrics.extractionAttemptCheckpoints());
        assertEquals(OptionalLong.of(100L), metrics.terminalElapsedTicks());
        assertFalse(metrics.firstSalvageElapsedTicks().isPresent());
        assertFalse(metrics.minObservedLiveThreats().isPresent());
        assertFalse(metrics.finalObservedSalvage().isPresent());
        assertTrue(metrics.reportLine().contains("finalSalvage=unavailable"));
        assertEquals("legacy-unavailable", metrics.terminalStage());
        assertEquals("other_failure", metrics.endReason());
    }

    private static ExpeditionEvidenceCheckpoint checkpoint(
        ExpeditionEvidenceCheckpoint.Stage stage,
        long gameTime,
        int salvage,
        int liveThreats
    ) {
        return new ExpeditionEvidenceCheckpoint(stage, gameTime, salvage, liveThreats, 0, 0, 1);
    }
}
