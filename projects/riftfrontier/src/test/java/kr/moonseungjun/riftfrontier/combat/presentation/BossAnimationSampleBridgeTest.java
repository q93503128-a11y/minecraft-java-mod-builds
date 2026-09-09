package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
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
