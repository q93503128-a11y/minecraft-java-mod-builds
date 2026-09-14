package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClipInventory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Region01BossReviewedReactionPreviewTest {
    @Test
    void deathUsesReviewedTerminalClipAndClampsAtItsEnd() {
        AnimationClipInventory inventory = inventory();

        var sample = Region01BossReviewedReactionPreview.sample(5, 10, 80, inventory).orElseThrow();

        assertEquals(Region01BossReviewedReactionPreview.ReactionKind.DEATH, sample.kind());
        assertEquals("Death", sample.clip().name());
        assertEquals(sample.clip().durationSeconds(), sample.sampleTimeSeconds());
    }

    @Test
    void hitReactUsesElapsedHurtTicksWithoutLooping() {
        AnimationClipInventory inventory = inventory();

        var sample = Region01BossReviewedReactionPreview.sample(6, 10, 0, inventory).orElseThrow();

        assertEquals(Region01BossReviewedReactionPreview.ReactionKind.HIT_REACT, sample.kind());
        assertEquals("HitReact", sample.clip().name());
        assertEquals(0.2F, sample.sampleTimeSeconds(), 0.0001F);
    }

    @Test
    void noReactionTimerProducesNoPresentationOverride() {
        assertTrue(Region01BossReviewedReactionPreview.sample(0, 0, 0, inventory()).isEmpty());
    }

    private static AnimationClipInventory inventory() {
        return AnimationClipInventory.fromImported(List.of(
            clip("HitReact", 0.4F),
            clip("Death", 1.25F)
        ));
    }

    private static AnimationClip clip(String name, float duration) {
        return new AnimationClip(name, duration, List.of(new AnimationClip.Channel(
            0,
            AnimationClip.Path.TRANSLATION,
            AnimationClip.Interpolation.LINEAR,
            new float[] {0.0F, duration},
            new float[] {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F}
        )));
    }
}
