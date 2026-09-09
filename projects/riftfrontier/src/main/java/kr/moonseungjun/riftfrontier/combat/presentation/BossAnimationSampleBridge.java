package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClipInventory;
import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Presentation-only bridge from authoritative boss semantic state to an imported animation clip sample.
 *
 * <p>This type intentionally owns no clock. Sample time is derived only from the server-authored phase progress
 * carried by {@link BossPresentationSemanticState} / {@link BossPresentationResolver.ResolvedPresentation}.
 * Logical animation keys are bound to visually reviewed imported clip windows so a source clip can be partitioned
 * across authoritative phases without introducing a second timing source.</p>
 */
public final class BossAnimationSampleBridge {
    private final Map<ContentId, BossAnimationSourceBinding.ResolvedSource> sourcesByLogicalKey;

    /**
     * Full-clip fixture/general constructor. Production reviewed bindings should use
     * {@link #BossAnimationSampleBridge(BossAnimationSourceBinding, AnimationClipInventory)}.
     */
    public BossAnimationSampleBridge(Map<ContentId, AnimationClip> clipsByLogicalKey) {
        Map<ContentId, BossAnimationSourceBinding.ResolvedSource> copy = new LinkedHashMap<>();
        Objects.requireNonNull(clipsByLogicalKey, "clipsByLogicalKey").forEach((key, clip) -> {
            Objects.requireNonNull(key, "animation logical key");
            Objects.requireNonNull(clip, "animation clip");
            if (copy.putIfAbsent(
                key,
                new BossAnimationSourceBinding.ResolvedSource(
                    clip,
                    new BossAnimationSourceBinding.ClipWindow(0.0D, 1.0D)
                )
            ) != null) {
                throw new IllegalArgumentException("duplicate animation logical key: " + key);
            }
        });
        this.sourcesByLogicalKey = Map.copyOf(copy);
    }

    /**
     * Production-safe constructor that resolves explicit source-clip names and reviewed clip windows only against a
     * verified imported inventory. It performs no name guessing and creates no fallback clips.
     */
    public BossAnimationSampleBridge(BossAnimationSourceBinding binding, AnimationClipInventory verifiedInventory) {
        this.sourcesByLogicalKey = Objects.requireNonNull(binding, "binding")
            .resolveWindows(Objects.requireNonNull(verifiedInventory, "verifiedInventory"));
    }

    public Optional<Sample> sample(
        BossPresentationSemanticState state,
        BossPresentationResolver.ResolvedPresentation resolved
    ) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(resolved, "resolved");
        if (!state.active()) return Optional.empty();

        AttackTimeline.Phase semanticPhase;
        try {
            semanticPhase = AttackTimeline.Phase.valueOf(state.attackPhase());
        } catch (IllegalArgumentException error) {
            return Optional.empty();
        }
        if (semanticPhase == AttackTimeline.Phase.COMPLETE) return Optional.empty();
        if (semanticPhase != resolved.phase()
            || Double.compare(state.phaseProgress(), resolved.phaseProgress()) != 0
            || state.hitWindowOpen() != resolved.hitWindowOpen()) {
            throw new IllegalArgumentException("resolved presentation no longer matches authoritative semantic sample");
        }

        BossAnimationSourceBinding.ResolvedSource source = sourcesByLogicalKey.get(resolved.animationKey());
        if (source == null) return Optional.empty();

        AnimationClip clip = source.clip();
        float duration = clip.durationSeconds();
        double normalizedSourceTime = source.window().sample(state.phaseProgress());
        float sampleTime = duration == 0.0f
            ? 0.0f
            : (float) Math.min(duration, duration * normalizedSourceTime);
        return Optional.of(new Sample(
            resolved.animationKey(),
            clip,
            sampleTime,
            semanticPhase,
            state.phaseProgress(),
            state.hitWindowOpen(),
            state.serverGameTick()
        ));
    }

    public record Sample(
        ContentId animationKey,
        AnimationClip clip,
        float sampleTimeSeconds,
        AttackTimeline.Phase phase,
        double authoritativePhaseProgress,
        boolean hitWindowOpen,
        long serverGameTick
    ) {
        public Sample {
            Objects.requireNonNull(animationKey, "animationKey");
            Objects.requireNonNull(clip, "clip");
            Objects.requireNonNull(phase, "phase");
            if (!Float.isFinite(sampleTimeSeconds) || sampleTimeSeconds < 0.0f || sampleTimeSeconds > clip.durationSeconds()) {
                throw new IllegalArgumentException("sample time must be within clip duration");
            }
            if (!Double.isFinite(authoritativePhaseProgress)
                || authoritativePhaseProgress < 0.0D
                || authoritativePhaseProgress > 1.0D) {
                throw new IllegalArgumentException("authoritative phase progress must be finite and between 0 and 1");
            }
            if (phase == AttackTimeline.Phase.COMPLETE) {
                throw new IllegalArgumentException("COMPLETE cannot produce an animation sample");
            }
            if (hitWindowOpen != (phase == AttackTimeline.Phase.ACTIVE)) {
                throw new IllegalArgumentException("animation sample hit window must exactly match ACTIVE phase");
            }
            if (serverGameTick < 0) throw new IllegalArgumentException("serverGameTick must be >= 0");
        }
    }
}
