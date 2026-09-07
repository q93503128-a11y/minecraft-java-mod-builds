package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.content.ContentCatalog;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.bootstrap.CoreContentBootstrap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExpeditionLifecycleTest {
    private static final ContentId REGION = ContentId.rift("region/vertical_slice_01");
    private static final ContentId CONTRACT = ContentId.rift("contract/salvage_recovery");
    private static final ContentId SALVAGE = ContentId.rift("resource/rift_salvage");

    @Test
    void prepareDeployRecoverExtractCompletesOnlyAfterContractRequirement() {
        var pack = CoreContentBootstrap.bootstrapAndValidate();
        var lifecycle = new ExpeditionLifecycle(pack.registry());
        String fingerprint = ContentCatalog.from(pack.registry()).fingerprint();

        ExpeditionRun run = lifecycle.begin(1L, REGION, CONTRACT, fingerprint, 100L);
        assertEquals(ExpeditionRun.Status.PREPARING, run.status());

        run = lifecycle.deploy(run);
        run = lifecycle.recover(run, SALVAGE, 2);
        ExpeditionRun underfilled = run;
        assertThrows(IllegalStateException.class, () -> lifecycle.requestExtraction(underfilled));
        assertEquals(
            ExpeditionRun.Status.DEPLOYED,
            underfilled.status(),
            "Rejected extraction must not strand the authoritative run in EXTRACTION_REQUESTED"
        );

        run = lifecycle.recover(run, SALVAGE, 1);
        run = lifecycle.requestExtraction(run);
        var resolution = lifecycle.resolveExtraction(run, 150L);
        assertEquals(ExpeditionRun.Status.EXTRACTED, resolution.run().status());
        assertEquals(150L, resolution.run().endedGameTime());
        assertEquals(3, resolution.retainedResources().get(SALVAGE));
        assertEquals(-1, resolution.resultProfile().threatDelta());
        assertFalse(resolution.worldConsequence().isBlank());
    }

    @Test
    void invalidTransitionsAndUnknownResourcesFailLoudly() {
        var pack = CoreContentBootstrap.bootstrapAndValidate();
        var lifecycle = new ExpeditionLifecycle(pack.registry());
        ExpeditionRun preparing = lifecycle.begin(2L, REGION, CONTRACT, "fixture", 5L);

        assertThrows(IllegalStateException.class, preparing::requestExtraction);
        assertThrows(IllegalStateException.class, () -> preparing.recover(SALVAGE, 1));
        ExpeditionRun deployed = lifecycle.deploy(preparing);
        assertThrows(
            IllegalArgumentException.class,
            () -> lifecycle.recover(deployed, ContentId.rift("resource/not_registered"), 1)
        );

        ExpeditionRun failed = lifecycle.fail(deployed, 9L);
        assertEquals(ExpeditionRun.Status.FAILED, failed.status());
        assertEquals(9L, failed.endedGameTime());
        assertThrows(IllegalStateException.class, () -> lifecycle.fail(failed, 10L));
    }

    @Test
    void immutableTransitionsPreserveExpeditionIdentityAndSnapshotFingerprint() {
        ExpeditionRun preparing = ExpeditionRun.preparing(7L, REGION, CONTRACT, "abc123", 42L);
        ExpeditionRun deployed = preparing.deploy();
        ExpeditionRun recovered = deployed.recover(SALVAGE, 4);
        ExpeditionRun requested = recovered.requestExtraction();
        ExpeditionRun extracted = requested.extract(90L);

        assertEquals(7L, extracted.sequence());
        assertEquals(REGION, extracted.regionId());
        assertEquals(CONTRACT, extracted.contractId());
        assertEquals("abc123", extracted.contentFingerprint());
        assertEquals(4, extracted.recoveredResources().get(SALVAGE));
        assertEquals(ExpeditionRun.Status.EXTRACTED, extracted.status());
        assertEquals(42L, extracted.startedGameTime());
        assertEquals(90L, extracted.endedGameTime());
        assertEquals(ExpeditionRun.Status.PREPARING, preparing.status(), "transitions must not mutate older snapshots");
    }
}
