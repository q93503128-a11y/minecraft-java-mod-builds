package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BossPresentationRenderResolverTest {
    private static final ContentId BOSS = ContentId.parse("riftfrontier:boss/region_01");
    private static final ContentId PRESENTATION = ContentId.parse("riftfrontier:presentation/region_01");
    private static final ContentId MODEL_KEY = ContentId.parse("riftfrontier:boss/model");
    private static final ContentId ANIMATION_KEY = ContentId.parse("riftfrontier:boss/animation/slam");
    private static final ContentId VFX_KEY = ContentId.parse("riftfrontier:boss/vfx/slam");
    private static final ContentId SOUND_KEY = ContentId.parse("riftfrontier:boss/sound/slam");
    private static final ContentId MODEL_RESOURCE = ContentId.parse("riftfrontier:geo/region_01_boss.geo.json");
    private static final ContentId ANIMATION_RESOURCE = ContentId.parse("riftfrontier:animations/region_01_boss.animation.json");
    private static final ContentId VFX_RESOURCE = ContentId.parse("riftfrontier:vfx/region_01_boss_slam.json");
    private static final ContentId SOUND_RESOURCE = ContentId.parse("riftfrontier:sounds/region_01_boss_slam.ogg");

    @Test
    void resolvesSemanticStateOnlyThroughMatchingValidatedGeneration() {
        var physical = BossPresentationRenderResolver.resolve(
            9,
            resolver(),
            assetSnapshot(9),
            BOSS,
            "base",
            activeState()
        );

        assertTrue(physical.isPresent());
        assertEquals(MODEL_RESOURCE, physical.orElseThrow().modelResource());
        assertEquals(ANIMATION_RESOURCE, physical.orElseThrow().animationResource());
        assertEquals(VFX_RESOURCE, physical.orElseThrow().vfxResource());
        assertEquals(SOUND_RESOURCE, physical.orElseThrow().soundResource());
        assertEquals(AttackTimeline.Phase.ACTIVE, physical.orElseThrow().phase());
        assertEquals(0.5D, physical.orElseThrow().phaseProgress());
        assertTrue(physical.orElseThrow().hitWindowOpen());
    }

    @Test
    void rejectsStalePhysicalSelectionAfterLogicalContentGenerationChanges() {
        var stale = BossPresentationRenderResolver.resolve(
            10,
            resolver(),
            assetSnapshot(9),
            BOSS,
            "base",
            activeState()
        );

        assertTrue(stale.isEmpty());
    }

    @Test
    void rejectsInactiveClientAssetsWithoutInventingFallbacks() {
        var inactive = BossPresentationRenderResolver.resolve(
            9,
            resolver(),
            BossPresentationClientAssetRuntime.Snapshot.inactive(9),
            BOSS,
            "base",
            activeState()
        );

        assertTrue(inactive.isEmpty());
    }

    @Test
    void preservesLogicalFailClosedBehaviourForUnknownContextAndInactiveSemantics() {
        assertTrue(BossPresentationRenderResolver.resolve(
            9,
            resolver(),
            assetSnapshot(9),
            ContentId.parse("riftfrontier:boss/unknown"),
            "base",
            activeState()
        ).isEmpty());

        assertFalse(BossPresentationRenderResolver.resolve(
            9,
            resolver(),
            assetSnapshot(9),
            BOSS,
            "base",
            BossPresentationSemanticState.clear(42, 101)
        ).isPresent());
    }

    private static BossPresentationResolver resolver() {
        var key = new BossPresentationProfile.BindingKey("slam", "melee", AttackTimeline.Phase.ACTIVE);
        var profile = new BossPresentationProfile(
            PRESENTATION,
            BOSS,
            "base",
            MODEL_KEY,
            Map.of(key, new BossPresentationProfile.AssetBinding(ANIMATION_KEY, VFX_KEY, SOUND_KEY))
        );
        return new BossPresentationResolver(List.of(profile));
    }

    private static BossPresentationClientAssetRuntime.Snapshot assetSnapshot(long generation) {
        var manifest = new BossPresentationAssetManifest(List.of(
            asset(MODEL_KEY, BossPresentationAssetManifest.Kind.MODEL, MODEL_RESOURCE),
            asset(ANIMATION_KEY, BossPresentationAssetManifest.Kind.ANIMATION, ANIMATION_RESOURCE),
            asset(VFX_KEY, BossPresentationAssetManifest.Kind.VFX, VFX_RESOURCE),
            asset(SOUND_KEY, BossPresentationAssetManifest.Kind.SOUND, SOUND_RESOURCE)
        ));
        var selection = BossPresentationAssetSelection.validate(manifest, (kind, resource) -> true)
            .selection()
            .orElseThrow();
        return new BossPresentationClientAssetRuntime.Snapshot(generation, Optional.of(selection));
    }

    private static BossPresentationAssetManifest.Asset asset(
        ContentId logicalKey,
        BossPresentationAssetManifest.Kind kind,
        ContentId resource
    ) {
        return new BossPresentationAssetManifest.Asset(logicalKey, kind, resource, "test fixture", "test-only");
    }

    private static BossPresentationSemanticState activeState() {
        return new BossPresentationSemanticState(
            42,
            100,
            true,
            1,
            "riftfrontier:attack/slam",
            AttackTimeline.Phase.ACTIVE.name(),
            0.5D,
            "slam",
            "melee",
            List.of("dodge"),
            true
        );
    }
}
