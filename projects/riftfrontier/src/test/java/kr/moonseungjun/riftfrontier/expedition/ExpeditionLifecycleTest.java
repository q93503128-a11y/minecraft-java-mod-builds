package kr.moonseungjun.riftfrontier.expedition;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
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
        run = lifecycle.requestExtraction(run);
        ExpeditionRun underfilled = run;
        assertThrows(IllegalStateException.class, () -> lifecycle.resolveExtraction(underfilled, 140L));

        run = lifecycle.recover(run, SALVAGE, 1);
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
        assertThrows(IllegalStateException.class, () -> lifecycle.fail(failed, 10L));
    }

    @Test
    void expeditionRunCodecRoundTripsAuthoritativeFields() {
        ExpeditionRun original = ExpeditionRun.preparing(7L, REGION, CONTRACT, "abc123", 42L)
            .deploy()
            .recover(SALVAGE, 4)
            .requestExtraction()
            .extract(90L);

        JsonElement encoded = ExpeditionRun.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        ExpeditionRun decoded = ExpeditionRun.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(original, decoded);
    }
}
