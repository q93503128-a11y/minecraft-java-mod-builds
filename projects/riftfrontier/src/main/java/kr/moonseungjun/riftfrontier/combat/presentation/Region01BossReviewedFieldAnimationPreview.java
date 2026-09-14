package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClipInventory;
import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Presentation-only sampler for the two Region 01 Dragon Evolved attacks whose source motion and exact source windows
 * have already been visually reviewed.
 *
 * <p>This deliberately does not complete the production animation binding. Arena pressure remains unresolved, and a
 * semantic state that is not one of the two reviewed attack roles yields no sample. The caller may then use a neutral
 * review idle, but must not invent/recycle an attack clip merely to make the actor move.</p>
 */
public final class Region01BossReviewedFieldAnimationPreview {
    private static final String STRIKE_PATTERN = "riftfrontier:attack/boss/region_01_committed_strike";
    private static final String LINE_PATTERN = "riftfrontier:attack/boss/region_01_line_displacement";
    private static final String STRIKE_ROLE = "region_01_committed_strike";
    private static final String LINE_ROLE = "region_01_line_displacement";

    private Region01BossReviewedFieldAnimationPreview() {}

    public static Optional<ReviewedSample> sample(
        BossPresentationSemanticState state,
        AnimationClipInventory verifiedInventory
    ) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(verifiedInventory, "verifiedInventory");

        Optional<ContentId> logicalKey = reviewedLogicalAnimationKey(state);
        if (logicalKey.isEmpty()) return Optional.empty();

        Map<ContentId, BossAnimationSourceBinding.ResolvedSource> resolved =
            Region01BossDragonEvolvedStagedAttackBinding.reviewedResolvedAttacks().resolveWindows(verifiedInventory);
        BossAnimationSourceBinding.ResolvedSource source = resolved.get(logicalKey.orElseThrow());
        if (source == null) return Optional.empty();

        double normalizedSourceTime = source.window().sample(state.phaseProgress());
        AnimationClip clip = source.clip();
        float duration = clip.durationSeconds();
        float sampleTime = duration <= 0.0F
            ? 0.0F
            : (float) Math.min(duration, duration * normalizedSourceTime);
        return Optional.of(new ReviewedSample(logicalKey.orElseThrow(), clip, sampleTime));
    }

    static Optional<ContentId> reviewedLogicalAnimationKey(BossPresentationSemanticState state) {
        Objects.requireNonNull(state, "state");
        if (!state.active()) return Optional.empty();

        final AttackTimeline.Phase phase;
        try {
            phase = AttackTimeline.Phase.valueOf(state.attackPhase());
        } catch (IllegalArgumentException rejected) {
            return Optional.empty();
        }
        if (phase == AttackTimeline.Phase.COMPLETE) return Optional.empty();

        String role = switch (state.patternId()) {
            case STRIKE_PATTERN -> STRIKE_ROLE;
            case LINE_PATTERN -> LINE_ROLE;
            default -> null;
        };
        if (role == null) return Optional.empty();

        return Optional.of(ContentId.rift(
            "animation/boss/" + role + "/" + phase.name().toLowerCase(java.util.Locale.ROOT)
        ));
    }

    public record ReviewedSample(ContentId logicalKey, AnimationClip clip, float sampleTimeSeconds) {
        public ReviewedSample {
            Objects.requireNonNull(logicalKey, "logicalKey");
            Objects.requireNonNull(clip, "clip");
            if (!Float.isFinite(sampleTimeSeconds)
                || sampleTimeSeconds < 0.0F
                || sampleTimeSeconds > clip.durationSeconds()) {
                throw new IllegalArgumentException("reviewed field sample time must remain within clip duration");
            }
        }
    }
}
