package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;

import java.util.Objects;
import java.util.Optional;

/**
 * Presentation-only authored motion candidate for Region 01 arena pressure field review.
 *
 * <p>This deliberately is not a production animation binding and does not relabel any Dragon Evolved source clip.
 * It applies a restrained whole-body compression/expansion silhouette cue to the non-production field preview so a
 * human can judge whether a planted radial charge reads better than the neutral hover. Combat timing remains entirely
 * server-authored through {@link BossPresentationSemanticState}; this class owns no clock, damage, movement or target
 * state. Production animation coverage remains incomplete until a reviewed motion is explicitly accepted.</p>
 */
public final class Region01BossArenaPressureFieldMotionCandidate {
    public static final String PATTERN = "riftfrontier:attack/boss/region_01_arena_pressure";

    private Region01BossArenaPressureFieldMotionCandidate() {}

    public static Optional<ScaleSample> sample(BossPresentationSemanticState state) {
        Objects.requireNonNull(state, "state");
        if (!state.active() || !PATTERN.equals(state.patternId())) return Optional.empty();

        final AttackTimeline.Phase phase;
        try {
            phase = AttackTimeline.Phase.valueOf(state.attackPhase());
        } catch (IllegalArgumentException rejected) {
            return Optional.empty();
        }
        if (phase == AttackTimeline.Phase.COMPLETE) return Optional.empty();

        float p = (float) state.phaseProgress();
        return Optional.of(switch (phase) {
            // Pull inward while rising slightly: a planted pressure load, not a forward lunge.
            case TELEGRAPH -> new ScaleSample(lerp(1.0F, 0.94F, p), lerp(1.0F, 1.04F, p));
            // Server ACTIVE entry produces the authoritative radial impulse; the silhouette snaps outward once.
            case ACTIVE -> new ScaleSample(lerp(1.12F, 1.08F, p), lerp(0.94F, 0.97F, p));
            // Settle back to the neutral silhouette without a second pulse.
            case RECOVERY -> new ScaleSample(lerp(1.08F, 1.0F, p), lerp(0.97F, 1.0F, p));
            case COMPLETE -> throw new IllegalStateException("COMPLETE is not an active presentation phase");
        });
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    /** Horizontal X/Z scale and vertical Y scale applied around the already-positioned actor origin. */
    public record ScaleSample(float horizontal, float vertical) {
        public ScaleSample {
            if (!Float.isFinite(horizontal) || !Float.isFinite(vertical) || horizontal <= 0.0F || vertical <= 0.0F) {
                throw new IllegalArgumentException("field-review scale must be finite and positive");
            }
        }
    }
}
