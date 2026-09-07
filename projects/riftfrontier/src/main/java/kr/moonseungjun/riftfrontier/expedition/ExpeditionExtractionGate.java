package kr.moonseungjun.riftfrontier.expedition;

import java.util.Objects;

/**
 * Pure accepted-extraction boundary shared by production gameplay and regression tests.
 * Validation happens before the evidence checkpoint is appended, and both changes remain immutable
 * until the caller chooses to persist the returned EXTRACTION_REQUESTED run.
 */
public final class ExpeditionExtractionGate {
    private ExpeditionExtractionGate() {}

    public static ExpeditionRun accept(
        ExpeditionLifecycle lifecycle,
        ExpeditionRun run,
        ExpeditionEvidenceCheckpoint checkpoint
    ) {
        Objects.requireNonNull(lifecycle, "lifecycle");
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(checkpoint, "checkpoint");
        if (checkpoint.stage() != ExpeditionEvidenceCheckpoint.Stage.PRE_EXTRACTION) {
            throw new IllegalArgumentException("Extraction gate requires a PRE_EXTRACTION evidence checkpoint");
        }

        // This must remain first: a rejected attempt cannot gain evidence or mutate authoritative state.
        lifecycle.validateExtractionRequest(run);
        ExpeditionRun evidenced = run.appendEvidence(checkpoint);
        return lifecycle.requestExtraction(evidenced);
    }
}
