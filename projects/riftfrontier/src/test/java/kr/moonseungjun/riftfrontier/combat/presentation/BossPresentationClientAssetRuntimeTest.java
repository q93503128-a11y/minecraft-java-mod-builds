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
    void failedCandidatePreservesPreviouslyPublishedSelectionAtomically() {
        var manifest = manifest();
        var previous = BossPresentationClientAssetRuntime.reload(3, Optional.of(manifest), (kind, resource) -> true);

        var failure = assertThrows(
            BossPresentationClientAssetRuntime.ResourceValidationException.class,
            () -> BossPresentationClientAssetRuntime.reload(4, Optional.of(manifest), (kind, resource) -> false)
        );

        assertEquals(1, failure.report().byCode(BossPresentationAssetManifest.Code.MISSING_RESOURCE).size());
        assertEquals(previous, BossPresentationClientAssetRuntime.current());
        assertEquals(3, BossPresentationClientAssetRuntime.current().contentGeneration());
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
