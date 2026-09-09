package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BossPresentationClientAssetRuntimeTest {
    private static final ContentId MODEL_KEY = ContentId.parse("riftfrontier:boss/model");
    private static final ContentId MODEL_RESOURCE = ContentId.parse("riftfrontier:geo/boss.geo.json");

    @AfterEach
    void resetRuntime() {
        BossPresentationClientAssetRuntime.resetForTests();
    }

    @Test
    void publishesCompleteSelectionOnlyAfterWholeManifestPasses() {
        var manifest = manifest();

        var snapshot = BossPresentationClientAssetRuntime.reload(
            7,
            Optional.of(manifest),
            (kind, resource) -> kind == BossPresentationAssetManifest.Kind.MODEL && resource.equals(MODEL_RESOURCE)
        );

        assertTrue(snapshot.ready());
        assertEquals(7, snapshot.contentGeneration());
        assertEquals(snapshot, BossPresentationClientAssetRuntime.current());
    }

    @Test
    void failedCandidateFailsClosedForAttemptedGeneration() {
        var manifest = manifest();
        BossPresentationClientAssetRuntime.reload(3, Optional.of(manifest), (kind, resource) -> true);

        var failure = assertThrows(
            BossPresentationClientAssetRuntime.ResourceValidationException.class,
            () -> BossPresentationClientAssetRuntime.reload(4, Optional.of(manifest), (kind, resource) -> false)
        );

        assertEquals(1, failure.report().byCode(BossPresentationAssetManifest.Code.MISSING_RESOURCE).size());
        assertEquals(4, BossPresentationClientAssetRuntime.current().contentGeneration());
        assertFalse(BossPresentationClientAssetRuntime.current().ready());
    }

    @Test
    void sameContentGenerationRevalidatesAgainstCurrentResourcePack() {
        var manifest = manifest();
        BossPresentationClientAssetRuntime.reload(5, Optional.of(manifest), (kind, resource) -> true);

        assertThrows(
            BossPresentationClientAssetRuntime.ResourceValidationException.class,
            () -> BossPresentationClientAssetRuntime.reload(5, Optional.of(manifest), (kind, resource) -> false)
        );

        assertEquals(5, BossPresentationClientAssetRuntime.current().contentGeneration());
        assertFalse(BossPresentationClientAssetRuntime.current().ready());
    }

    @Test
    void staleContentGenerationCannotReplaceNewerPublication() {
        var manifest = manifest();
        var current = BossPresentationClientAssetRuntime.reload(8, Optional.of(manifest), (kind, resource) -> true);

        var stale = assertThrows(
            BossPresentationClientAssetRuntime.StaleContentGenerationException.class,
            () -> BossPresentationClientAssetRuntime.reload(7, Optional.empty(), (kind, resource) -> false)
        );

        assertEquals(7, stale.attemptedGeneration());
        assertEquals(8, stale.currentGeneration());
        assertEquals(current, BossPresentationClientAssetRuntime.current());
        assertTrue(BossPresentationClientAssetRuntime.current().ready());
    }

    @Test
    void noProductionManifestPublishesExplicitInactiveState() {
        BossPresentationClientAssetRuntime.reload(2, Optional.of(manifest()), (kind, resource) -> true);

        var inactive = BossPresentationClientAssetRuntime.reload(5, Optional.empty(), (kind, resource) -> false);

        assertFalse(inactive.ready());
        assertEquals(5, inactive.contentGeneration());
        assertEquals(inactive, BossPresentationClientAssetRuntime.current());
    }

    private static BossPresentationAssetManifest manifest() {
        return new BossPresentationAssetManifest(List.of(
            new BossPresentationAssetManifest.Asset(
                MODEL_KEY,
                BossPresentationAssetManifest.Kind.MODEL,
                MODEL_RESOURCE,
                "test fixture",
                "test-only"
            )
        ));
    }
}
