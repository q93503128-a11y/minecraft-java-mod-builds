package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.content.ContentCatalog;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.bootstrap.CoreContentBootstrap;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ExpeditionLifecycleTest {
    private static final ContentId REGION = ContentId.rift("region/vertical_slice_01");
    private static final ContentId CONTRACT = ContentId.rift("contract/salvage_recovery");
    private static final ContentId SALVAGE = ContentId.rift("resource/rift_salvage");
    private static final UUID OWNER = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID OTHER = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

    @Test
    void prepareDeployRecoverExtractCompletesOnlyAfterContractRequirement() {
        var pack = CoreContentBootstrap.bootstrapAndValidate();
        var lifecycle = new ExpeditionLifecycle(pack.registry());
        String fingerprint = ContentCatalog.from(pack.registry()).fingerprint();

        ExpeditionRun run = lifecycle.begin(1L, REGION, CONTRACT, OWNER, fingerprint, 100L);
        assertEquals(ExpeditionRun.Status.PREPARING, run.status());
        assertEquals(ExpeditionRun.EndReason.NONE, run.endReason());
        assertTrue(run.ownedBy(OWNER));
        assertFalse(run.ownedBy(OTHER));

        run = lifecycle.deploy(run);
        run = lifecycle.recover(run, SALVAGE, 2);
        ExpeditionRun underfilled = run;
        assertThrows(IllegalStateException.class, () -> lifecycle.requestExtraction(underfilled));
        assertEquals(
            ExpeditionRun.Status.DEPLOYED,
            underfilled.status(),
            "Rejected extraction must not strand the authoritative run in EXTRACTION_REQUESTED"
        );
        assertEquals(ExpeditionRun.EndReason.NONE, underfilled.endReason());

        run = lifecycle.recover(run, SALVAGE, 1);
        run = lifecycle.requestExtraction(run);
        var resolution = lifecycle.resolveExtraction(run, 150L);
        assertEquals(ExpeditionRun.Status.EXTRACTED, resolution.run().status());
        assertEquals(ExpeditionRun.EndReason.EXTRACTION, resolution.run().endReason());
        assertEquals(OWNER, resolution.run().ownerId().orElseThrow());
        assertEquals(150L, resolution.run().endedGameTime());
        assertEquals(3, resolution.retainedResources().get(SALVAGE));
        assertEquals(-1, resolution.resultProfile().threatDelta());
        assertFalse(resolution.worldConsequence().isBlank());
    }

    @Test
    void invalidTransitionsAndUnknownResourcesFailLoudly() {
        var pack = CoreContentBootstrap.bootstrapAndValidate();
        var lifecycle = new ExpeditionLifecycle(pack.registry());
        ExpeditionRun preparing = lifecycle.begin(2L, REGION, CONTRACT, OWNER, "fixture", 5L);

        assertThrows(IllegalStateException.class, preparing::requestExtraction);
        assertThrows(IllegalStateException.class, () -> preparing.recover(SALVAGE, 1));
        ExpeditionRun deployed = lifecycle.deploy(preparing);
        assertThrows(
            IllegalArgumentException.class,
            () -> lifecycle.recover(deployed, ContentId.rift("resource/not_registered"), 1)
        );

        ExpeditionRun failed = lifecycle.fail(deployed, 9L, ExpeditionRun.EndReason.PLAYER_DEATH);
        assertEquals(ExpeditionRun.Status.FAILED, failed.status());
        assertEquals(ExpeditionRun.EndReason.PLAYER_DEATH, failed.endReason());
        assertEquals(OWNER, failed.ownerId().orElseThrow());
        assertEquals(9L, failed.endedGameTime());
        assertThrows(IllegalStateException.class, () -> lifecycle.fail(failed, 10L, ExpeditionRun.EndReason.PLAYER_LOGOUT));
        assertThrows(IllegalArgumentException.class, () -> deployed.fail(10L, ExpeditionRun.EndReason.NONE));
        assertThrows(IllegalArgumentException.class, () -> deployed.fail(10L, ExpeditionRun.EndReason.EXTRACTION));
    }

    @Test
    void immutableTransitionsPreserveExpeditionIdentityOwnerAndSnapshotFingerprint() {
        ExpeditionRun preparing = ExpeditionRun.preparing(7L, REGION, CONTRACT, OWNER, "abc123", 42L);
        ExpeditionRun deployed = preparing.deploy();
        ExpeditionRun recovered = deployed.recover(SALVAGE, 4);
        ExpeditionRun requested = recovered.requestExtraction();
        ExpeditionRun extracted = requested.extract(90L);

        assertEquals(7L, extracted.sequence());
        assertEquals(REGION, extracted.regionId());
        assertEquals(CONTRACT, extracted.contractId());
        assertEquals(OWNER, extracted.ownerId().orElseThrow());
        assertEquals("abc123", extracted.contentFingerprint());
        assertEquals(4, extracted.recoveredResources().get(SALVAGE));
        assertEquals(ExpeditionRun.Status.EXTRACTED, extracted.status());
        assertEquals(ExpeditionRun.EndReason.EXTRACTION, extracted.endReason());
        assertEquals(42L, extracted.startedGameTime());
        assertEquals(90L, extracted.endedGameTime());
        assertEquals(ExpeditionRun.Status.PREPARING, preparing.status(), "transitions must not mutate older snapshots");
        assertEquals(ExpeditionRun.EndReason.NONE, preparing.endReason());
    }

    @Test
    void legacyFixtureRunsRemainDecodableAsExplicitlyUnowned() {
        ExpeditionRun legacy = ExpeditionRun.preparing(8L, REGION, CONTRACT, "legacy", 2L);
        assertTrue(legacy.ownerId().isEmpty());
        assertFalse(legacy.ownedBy(OWNER));
    }
}
