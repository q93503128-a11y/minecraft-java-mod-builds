package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClipInventory;

import java.util.Objects;
import java.util.Optional;

/**
 * Evidence-backed, presentation-only sampling for reviewed Dragon Evolved reaction clips.
 *
 * <p>{@code HitReact} and {@code Death} were directly observed in the accepted sanitized source-motion review. This
 * helper does not assign either clip to an attack role and owns no gameplay clock. The field-review renderer gives an
 * authoritative reviewed attack sample priority over {@code HitReact}, so receiving damage cannot visually cancel an
 * attack that the server says is still executing. {@code Death} is terminal presentation and therefore has priority.
 * Production publication remains gated independently.</p>
 */
public final class Region01BossReviewedReactionPreview {
    private static final String HIT_REACT_CLIP = "HitReact";
    private static final String DEATH_CLIP = "Death";
    private static final float TICKS_PER_SECOND = 20.0F;

    private Region01BossReviewedReactionPreview() {}

    public static Optional<ReviewedReactionSample> sample(
        int hurtTime,
        int hurtDuration,
        int deathTime,
        AnimationClipInventory verifiedInventory
    ) {
        if (hurtTime < 0 || hurtDuration < 0 || deathTime < 0) {
            throw new IllegalArgumentException("reaction timers must be >= 0");
        }
        Objects.requireNonNull(verifiedInventory, "verifiedInventory");

        if (deathTime > 0) {
            Region01BossDragonEvolvedMotionReview.requireReviewed(DEATH_CLIP);
            AnimationClip clip = verifiedInventory.requireClip(DEATH_CLIP);
            return Optional.of(new ReviewedReactionSample(
                ReactionKind.DEATH,
                clip,
                clampSampleTime(clip, deathTime / TICKS_PER_SECOND)
            ));
        }

        if (hurtTime > 0) {
            Region01BossDragonEvolvedMotionReview.requireReviewed(HIT_REACT_CLIP);
            AnimationClip clip = verifiedInventory.requireClip(HIT_REACT_CLIP);
            int elapsedTicks = Math.max(0, hurtDuration - hurtTime);
            return Optional.of(new ReviewedReactionSample(
                ReactionKind.HIT_REACT,
                clip,
                clampSampleTime(clip, elapsedTicks / TICKS_PER_SECOND)
            ));
        }

        return Optional.empty();
    }

    private static float clampSampleTime(AnimationClip clip, float requestedSeconds) {
        return Math.min(clip.durationSeconds(), Math.max(0.0F, requestedSeconds));
    }

    public enum ReactionKind {
        HIT_REACT,
        DEATH
    }

    public record ReviewedReactionSample(ReactionKind kind, AnimationClip clip, float sampleTimeSeconds) {
        public ReviewedReactionSample {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(clip, "clip");
            if (!Float.isFinite(sampleTimeSeconds)
                || sampleTimeSeconds < 0.0F
                || sampleTimeSeconds > clip.durationSeconds()) {
                throw new IllegalArgumentException("reviewed reaction sample time must remain within clip duration");
            }
        }
    }
}
