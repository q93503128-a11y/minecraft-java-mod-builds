package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClipInventory;
import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals(new BossAnimationSourceBinding.ClipWindow(0.0D, 1.0D),
            binding.resolveWindows(inventory).get(logical).window());
        new BossAnimationSampleBridge(binding, inventory);
    }

    @Test
    void preservesExplicitReviewedSourceWindowForAuthoritativePhaseSampling() {
        AnimationClip punch = clip("Punch");
        AnimationClipInventory inventory = AnimationClipInventory.fromImported(List.of(punch));
        ContentId telegraph = ContentId.rift("boss/region_01/attack/telegraph");
        BossAnimationSourceBinding.ClipWindow window = new BossAnimationSourceBinding.ClipWindow(0.10D, 0.35D);
        BossAnimationSourceBinding binding = new BossAnimationSourceBinding(
            approvedReview("Punch"),
            Map.of(telegraph, "Punch"),
            Map.of(telegraph, window)
        );

        var resolved = binding.resolveWindows(inventory).get(telegraph);
        assertSame(punch, resolved.clip());
        assertEquals(window, resolved.window());
        assertEquals(0.10D, window.sample(0.0D), 1.0e-9D);
        assertEquals(0.225D, window.sample(0.5D), 1.0e-9D);
        assertEquals(0.35D, window.sample(1.0D), 1.0e-9D);
    }

    @Test
    void explicitWindowMapMustExactlyCoverLogicalBindings() {
        ContentId telegraph = ContentId.rift("boss/region_01/attack/telegraph");
        ContentId active = ContentId.rift("boss/region_01/attack/active");
        BossAnimationMotionReview review = approvedReview("Punch");

        assertThrows(IllegalArgumentException.class, () -> new BossAnimationSourceBinding(
            review,
            Map.of(telegraph, "Punch"),
            Map.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new BossAnimationSourceBinding(
            review,
            Map.of(telegraph, "Punch"),
            Map.of(
                telegraph, new BossAnimationSourceBinding.ClipWindow(0.0D, 0.5D),
                active, new BossAnimationSourceBinding.ClipWindow(0.5D, 1.0D)
            )
        ));
    }

    @Test
    void rejectsInvalidSourceWindowsAndAllowsIntentionalHeldPoseWindow() {
        assertThrows(IllegalArgumentException.class,
            () -> new BossAnimationSourceBinding.ClipWindow(-0.01D, 0.5D));
        assertThrows(IllegalArgumentException.class,
            () -> new BossAnimationSourceBinding.ClipWindow(0.5D, 1.01D));
        assertThrows(IllegalArgumentException.class,
            () -> new BossAnimationSourceBinding.ClipWindow(0.8D, 0.2D));
        assertThrows(IllegalArgumentException.class,
            () -> new BossAnimationSourceBinding.ClipWindow(Double.NaN, 0.2D));

        BossAnimationSourceBinding.ClipWindow held = new BossAnimationSourceBinding.ClipWindow(0.4D, 0.4D);
        assertEquals(0.4D, held.sample(0.0D), 0.0D);
        assertEquals(0.4D, held.sample(1.0D), 0.0D);
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
