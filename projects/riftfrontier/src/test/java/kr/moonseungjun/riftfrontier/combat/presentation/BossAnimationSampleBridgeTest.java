package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClipInventory;
import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossAnimationSampleBridgeTest {
    private static final ContentId ANIMATION_KEY = ContentId.parse("riftfrontier:boss/region_01/test_attack");

    @Test
    void derivesSampleTimeOnlyFromAuthoritativePhaseProgress() {
        AnimationClip clip = clip("Punch", 2.0f);
        BossAnimationSampleBridge bridge = new BossAnimationSampleBridge(Map.of(ANIMATION_KEY, clip));
        BossPresentationSemanticState state = state(AttackTimeline.Phase.ACTIVE, 0.25D, true, 42L);
        BossPresentationResolver.ResolvedPresentation resolved = resolved(AttackTimeline.Phase.ACTIVE, 0.25D, true);

        BossAnimationSampleBridge.Sample sample = bridge.sample(state, resolved).orElseThrow();

        assertEquals("Punch", sample.clip().name());
        assertEquals(0.5f, sample.sampleTimeSeconds(), 1.0e-6f);
        assertEquals(42L, sample.serverGameTick());
        assertTrue(sample.hitWindowOpen());
    }

    @Test
    void reviewedSourceWindowMapsAuthoritativePhaseProgressIntoOnlyThatClipSegment() {
        AnimationClip clip = clip("Headbutt", 2.0f);
        AnimationClipInventory inventory = AnimationClipInventory.fromImported(List.of(clip));
        BossAnimationMotionReview review = new BossAnimationMotionReview(Map.of(
            "Headbutt",
            new BossAnimationMotionReview.ApprovedClip(
                "Headbutt",
                "fixture-motion-review:Headbutt",
                "forward head-and-torso drive followed by recovery"
            )
        ));
        BossAnimationSourceBinding binding = new BossAnimationSourceBinding(
            review,
            Map.of(ANIMATION_KEY, "Headbutt"),
            Map.of(ANIMATION_KEY, new BossAnimationSourceBinding.ClipWindow(0.20D, 0.40D))
        );
        BossAnimationSampleBridge bridge = new BossAnimationSampleBridge(binding, inventory);

        assertEquals(0.4f, bridge.sample(
            state(AttackTimeline.Phase.TELEGRAPH, 0.0D, false, 40L),
            resolved(AttackTimeline.Phase.TELEGRAPH, 0.0D, false)
        ).orElseThrow().sampleTimeSeconds(), 1.0e-6f);
        assertEquals(0.6f, bridge.sample(
            state(AttackTimeline.Phase.TELEGRAPH, 0.5D, false, 41L),
            resolved(AttackTimeline.Phase.TELEGRAPH, 0.5D, false)
        ).orElseThrow().sampleTimeSeconds(), 1.0e-6f);
        assertEquals(0.8f, bridge.sample(
            state(AttackTimeline.Phase.TELEGRAPH, 1.0D, false, 42L),
            resolved(AttackTimeline.Phase.TELEGRAPH, 1.0D, false)
        ).orElseThrow().sampleTimeSeconds(), 1.0e-6f);
    }

    @Test
    void heldPoseWindowDoesNotCreateAnIndependentClock() {
        AnimationClip clip = clip("Punch", 2.0f);
        AnimationClipInventory inventory = AnimationClipInventory.fromImported(List.of(clip));
        BossAnimationMotionReview review = new BossAnimationMotionReview(Map.of(
            "Punch",
            new BossAnimationMotionReview.ApprovedClip(
                "Punch",
                "fixture-motion-review:Punch",
                "forelimb extension with recovery"
            )
        ));
        BossAnimationSourceBinding binding = new BossAnimationSourceBinding(
            review,
            Map.of(ANIMATION_KEY, "Punch"),
            Map.of(ANIMATION_KEY, new BossAnimationSourceBinding.ClipWindow(0.35D, 0.35D))
        );
        BossAnimationSampleBridge bridge = new BossAnimationSampleBridge(binding, inventory);

        float early = bridge.sample(
            state(AttackTimeline.Phase.ACTIVE, 0.1D, true, 50L),
            resolved(AttackTimeline.Phase.ACTIVE, 0.1D, true)
        ).orElseThrow().sampleTimeSeconds();
        float late = bridge.sample(
            state(AttackTimeline.Phase.ACTIVE, 0.9D, true, 59L),
            resolved(AttackTimeline.Phase.ACTIVE, 0.9D, true)
        ).orElseThrow().sampleTimeSeconds();

        assertEquals(0.7f, early, 1.0e-6f);
        assertEquals(early, late, 0.0f);
    }

    @Test
    void inactiveSemanticStateProducesNoSample() {
        BossAnimationSampleBridge bridge = new BossAnimationSampleBridge(Map.of(ANIMATION_KEY, clip("Idle", 1.0f)));
        BossPresentationSemanticState clear = BossPresentationSemanticState.clear(7, 9L);
        assertTrue(bridge.sample(clear, resolved(AttackTimeline.Phase.TELEGRAPH, 0.0D, false)).isEmpty());
    }

    @Test
    void missingLogicalClipFailsClosedWithoutGuessingFallback() {
        BossAnimationSampleBridge bridge = new BossAnimationSampleBridge(Map.of());
        assertTrue(bridge.sample(
            state(AttackTimeline.Phase.TELEGRAPH, 0.5D, false, 1L),
            resolved(AttackTimeline.Phase.TELEGRAPH, 0.5D, false)
        ).isEmpty());
    }

    @Test
    void staleResolvedPresentationCannotAdvanceASecondClock() {
        BossAnimationSampleBridge bridge = new BossAnimationSampleBridge(Map.of(ANIMATION_KEY, clip("Punch", 2.0f)));
        BossPresentationSemanticState state = state(AttackTimeline.Phase.ACTIVE, 0.75D, true, 100L);
        BossPresentationResolver.ResolvedPresentation stale = resolved(AttackTimeline.Phase.ACTIVE, 0.50D, true);
        assertThrows(IllegalArgumentException.class, () -> bridge.sample(state, stale));
    }

    @Test
    void zeroDurationClipSamplesAtZero() {
        BossAnimationSampleBridge bridge = new BossAnimationSampleBridge(Map.of(ANIMATION_KEY, clip("Pose", 0.0f)));
        BossAnimationSampleBridge.Sample sample = bridge.sample(
            state(AttackTimeline.Phase.RECOVERY, 1.0D, false, 5L),
            resolved(AttackTimeline.Phase.RECOVERY, 1.0D, false)
        ).orElseThrow();
        assertEquals(0.0f, sample.sampleTimeSeconds(), 0.0f);
        assertFalse(sample.hitWindowOpen());
    }

    private static AnimationClip clip(String name, float duration) {
        return new AnimationClip(name, duration, List.of(new AnimationClip.Channel(
            0,
            AnimationClip.Path.TRANSLATION,
            AnimationClip.Interpolation.STEP,
            new float[]{0.0f},
            new float[]{0.0f, 0.0f, 0.0f}
        )));
    }

    private static BossPresentationSemanticState state(
        AttackTimeline.Phase phase,
        double progress,
        boolean hitWindow,
        long tick
    ) {
        return new BossPresentationSemanticState(
            7,
            tick,
            true,
            1,
            "riftfrontier:region_01_test_attack",
            phase.name(),
            progress,
            "test_cue",
            "test_delivery",
            List.of(),
            hitWindow
        );
    }

    private static BossPresentationResolver.ResolvedPresentation resolved(
        AttackTimeline.Phase phase,
        double progress,
        boolean hitWindow
    ) {
        return new BossPresentationResolver.ResolvedPresentation(
            ContentId.parse("riftfrontier:boss/region_01/test_profile"),
            ContentId.parse("riftfrontier:boss/region_01/test_model"),
            ANIMATION_KEY,
            ContentId.parse("riftfrontier:boss/region_01/test_vfx"),
            ContentId.parse("riftfrontier:boss/region_01/test_sound"),
            phase,
            progress,
            hitWindow
        );
    }
}
