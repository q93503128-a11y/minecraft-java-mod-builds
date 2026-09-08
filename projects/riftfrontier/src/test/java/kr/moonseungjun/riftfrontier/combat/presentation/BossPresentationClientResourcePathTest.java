package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BossPresentationClientResourcePathTest {
    @Test
    void resolvesExactClientPackPathsPerAssetKind() {
        assertEquals(
            ContentId.rift("models/boss/region_01.json"),
            BossPresentationClientResourcePath.resolve(
                BossPresentationAssetManifest.Kind.MODEL,
                ContentId.rift("models/boss/region_01")
            ).orElseThrow()
        );
        assertEquals(
            ContentId.rift("geo/boss/region_01.geo.json"),
            BossPresentationClientResourcePath.resolve(
                BossPresentationAssetManifest.Kind.MODEL,
                ContentId.rift("geo/boss/region_01")
            ).orElseThrow()
        );
        assertEquals(
            ContentId.rift("animations/boss/slam_active.animation.json"),
            BossPresentationClientResourcePath.resolve(
                BossPresentationAssetManifest.Kind.ANIMATION,
                ContentId.rift("animations/boss/slam_active")
            ).orElseThrow()
        );
        assertEquals(
            ContentId.rift("vfx/boss/slam_active.json"),
            BossPresentationClientResourcePath.resolve(
                BossPresentationAssetManifest.Kind.VFX,
                ContentId.rift("vfx/boss/slam_active")
            ).orElseThrow()
        );
        assertEquals(
            ContentId.rift("sounds/boss/slam_active.ogg"),
            BossPresentationClientResourcePath.resolve(
                BossPresentationAssetManifest.Kind.SOUND,
                ContentId.rift("sounds/boss/slam_active")
            ).orElseThrow()
        );
    }

    @Test
    void rejectsCrossKindOrUnscopedPathsInsteadOfGuessingFallbacks() {
        assertTrue(BossPresentationClientResourcePath.resolve(
            BossPresentationAssetManifest.Kind.SOUND,
            ContentId.rift("animations/boss/slam_active")
        ).isEmpty());
        assertTrue(BossPresentationClientResourcePath.resolve(
            BossPresentationAssetManifest.Kind.ANIMATION,
            ContentId.rift("boss/slam_active")
        ).isEmpty());
        assertTrue(BossPresentationClientResourcePath.resolve(
            BossPresentationAssetManifest.Kind.MODEL,
            ContentId.rift("textures/entity/boss")
        ).isEmpty());
    }

    @Test
    void doesNotDuplicateAlreadyExplicitPhysicalSuffixes() {
        assertEquals(
            ContentId.rift("models/boss/region_01.json"),
            BossPresentationClientResourcePath.resolve(
                BossPresentationAssetManifest.Kind.MODEL,
                ContentId.rift("models/boss/region_01.json")
            ).orElseThrow()
        );
        assertEquals(
            ContentId.rift("sounds/boss/slam_active.ogg"),
            BossPresentationClientResourcePath.resolve(
                BossPresentationAssetManifest.Kind.SOUND,
                ContentId.rift("sounds/boss/slam_active.ogg")
            ).orElseThrow()
        );
    }
}
