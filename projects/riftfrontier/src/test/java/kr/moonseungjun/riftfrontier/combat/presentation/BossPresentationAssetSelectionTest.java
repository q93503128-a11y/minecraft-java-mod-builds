package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BossPresentationAssetSelectionTest {
    private static final ContentId PROFILE = ContentId.rift("presentation/boss_region_01");
    private static final ContentId MODEL_KEY = ContentId.rift("presentation/model/boss_region_01");
    private static final ContentId ANIMATION_KEY = ContentId.rift("presentation/animation/slam_active");
    private static final ContentId VFX_KEY = ContentId.rift("presentation/vfx/slam_active");
    private static final ContentId SOUND_KEY = ContentId.rift("presentation/sound/slam_active");

    @Test
    void validationFailsClosedWhenAnySelectedPhysicalResourceIsMissing() {
        BossPresentationAssetManifest manifest = manifest();
        BossPresentationAssetSelection.Result result = BossPresentationAssetSelection.validate(
            manifest,
            (kind, resourceId) -> !resourceId.equals(ContentId.rift("sounds/boss/slam_active"))
        );

        assertFalse(result.ready());
        assertTrue(result.selection().isEmpty());
        assertEquals(1, result.report().byCode(BossPresentationAssetManifest.Code.MISSING_RESOURCE).size());
        assertEquals(SOUND_KEY, result.report().byCode(BossPresentationAssetManifest.Code.MISSING_RESOURCE).getFirst().logicalKey());
    }

    @Test
    void validatedSelectionPromotesLogicalKeysToSelectedPhysicalResourcesWithoutChangingTiming() {
        BossPresentationAssetSelection selection = BossPresentationAssetSelection.validate(manifest(), (kind, resourceId) -> true)
            .selection()
            .orElseThrow();

        BossPresentationResolver.ResolvedPresentation logical = new BossPresentationResolver.ResolvedPresentation(
            PROFILE,
            MODEL_KEY,
            ANIMATION_KEY,
            VFX_KEY,
            SOUND_KEY,
            AttackTimeline.Phase.ACTIVE,
            0.5D,
            true
        );

        BossPresentationAssetSelection.PhysicalPresentation physical = selection.resolve(logical).orElseThrow();
        assertEquals(ContentId.rift("models/boss/region_01"), physical.modelResource());
        assertEquals(ContentId.rift("animations/boss/slam_active"), physical.animationResource());
        assertEquals(ContentId.rift("vfx/boss/slam_active"), physical.vfxResource());
        assertEquals(ContentId.rift("sounds/boss/slam_active"), physical.soundResource());
        assertEquals(AttackTimeline.Phase.ACTIVE, physical.phase());
        assertEquals(0.5D, physical.phaseProgress());
        assertTrue(physical.hitWindowOpen());
    }

    @Test
    void validatedSelectionStillFailsClosedForUnexpectedLogicalKeyOrKind() {
        BossPresentationAssetSelection selection = BossPresentationAssetSelection.validate(manifest(), (kind, resourceId) -> true)
            .selection()
            .orElseThrow();

        BossPresentationResolver.ResolvedPresentation unknownAnimation = new BossPresentationResolver.ResolvedPresentation(
            PROFILE,
            MODEL_KEY,
            ContentId.rift("presentation/animation/not_selected"),
            VFX_KEY,
            SOUND_KEY,
            AttackTimeline.Phase.TELEGRAPH,
            0.25D,
            false
        );

        assertEquals(Optional.empty(), selection.resolve(unknownAnimation));
    }

    private static BossPresentationAssetManifest manifest() {
        return new BossPresentationAssetManifest(List.of(
            asset(MODEL_KEY, BossPresentationAssetManifest.Kind.MODEL, "models/boss/region_01"),
            asset(ANIMATION_KEY, BossPresentationAssetManifest.Kind.ANIMATION, "animations/boss/slam_active"),
            asset(VFX_KEY, BossPresentationAssetManifest.Kind.VFX, "vfx/boss/slam_active"),
            asset(SOUND_KEY, BossPresentationAssetManifest.Kind.SOUND, "sounds/boss/slam_active")
        ));
    }

    private static BossPresentationAssetManifest.Asset asset(
        ContentId logicalKey,
        BossPresentationAssetManifest.Kind kind,
        String resourcePath
    ) {
        return new BossPresentationAssetManifest.Asset(
            logicalKey,
            kind,
            ContentId.rift(resourcePath),
            "test fixture",
            "test-only fixture"
        );
    }
}
