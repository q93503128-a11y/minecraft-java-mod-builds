package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClipInventory;
import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BossAnimationSourceBindingTest {
    @Test
    void resolvesOnlyExplicitVisuallyReviewedVerifiedSourceClipNames() {
        AnimationClip punch = clip("Punch");
        AnimationClipInventory inventory = AnimationClipInventory.fromImported(List.of(punch));
        ContentId logical = ContentId.rift("boss/region_01/attack/active");
        BossAnimationMotionReview review = approvedReview("Punch");
        BossAnimationSourceBinding binding = new BossAnimationSourceBinding(review, Map.of(logical, "Punch"));

        assertSame(punch, binding.resolve(inventory).get(logical));
        new BossAnimationSampleBridge(binding, inventory);
    }

    @Test
    void rejectsBindingWithoutVisualMotionReviewEvenWhenClipExists() {
        AnimationClipInventory inventory = AnimationClipInventory.fromImported(List.of(clip("Headbutt")));
        BossAnimationMotionReview review = approvedReview("Punch");

        assertThrows(IllegalArgumentException.class, () -> new BossAnimationSourceBinding(review, Map.of(
            ContentId.rift("boss/region_01/attack/active"), "Headbutt"
        )));
    }

    @Test
    void rejectsBindingToReviewedSourceClipThatWasNotImported() {
        AnimationClipInventory inventory = AnimationClipInventory.fromImported(List.of(clip("Punch")));
        BossAnimationMotionReview review = approvedReview("Headbutt");
        BossAnimationSourceBinding binding = new BossAnimationSourceBinding(review, Map.of(
            ContentId.rift("boss/region_01/attack/active"), "Headbutt"
        ));

        assertThrows(IllegalArgumentException.class, () -> binding.resolve(inventory));
        assertThrows(IllegalArgumentException.class, () -> new BossAnimationSampleBridge(binding, inventory));
    }

    @Test
    void rejectsBlankSourceClipName() {
        BossAnimationMotionReview review = approvedReview("Punch");
        assertThrows(IllegalArgumentException.class, () -> new BossAnimationSourceBinding(review, Map.of(
            ContentId.rift("boss/region_01/attack/active"), " "
        )));
    }

    @Test
    void rejectsMismatchedOrIncompleteReviewEvidence() {
        assertThrows(IllegalArgumentException.class, () -> new BossAnimationMotionReview(Map.of(
            "Punch",
            new BossAnimationMotionReview.ApprovedClip("Headbutt", "capture-01", "forward head strike")
        )));
        assertThrows(IllegalArgumentException.class, () -> new BossAnimationMotionReview.ApprovedClip(
            "Punch", " ", "forelimb strike with recovery"
        ));
        assertThrows(IllegalArgumentException.class, () -> new BossAnimationMotionReview.ApprovedClip(
            "Punch", "capture-01", " "
        ));
    }

    private static BossAnimationMotionReview approvedReview(String sourceClipName) {
        return new BossAnimationMotionReview(Map.of(
            sourceClipName,
            new BossAnimationMotionReview.ApprovedClip(
                sourceClipName,
                "fixture-motion-capture:" + sourceClipName,
                "fixture-observed motion for " + sourceClipName
            )
        ));
    }

    private static AnimationClip clip(String name) {
        return new AnimationClip(name, 1.0f, List.of(new AnimationClip.Channel(
            0,
            AnimationClip.Path.ROTATION,
            AnimationClip.Interpolation.LINEAR,
            new float[]{0.0f, 1.0f},
            new float[]{0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f}
        )));
    }
}
