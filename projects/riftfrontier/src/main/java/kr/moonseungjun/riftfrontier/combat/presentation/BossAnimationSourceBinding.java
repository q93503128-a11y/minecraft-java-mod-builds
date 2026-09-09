package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClipInventory;
import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Explicit logical-animation-key -> visually reviewed, verified imported source-clip-name binding.
 *
 * <p>This type intentionally does not infer mappings from similar names. A production binding is valid only when
 * every named source clip has explicit visual motion-review evidence and exists in the already-verified imported
 * inventory. Optional normalized source windows allow one reviewed source clip to supply distinct authoritative
 * presentation phases without restarting the whole clip for each phase.</p>
 */
public final class BossAnimationSourceBinding {
    private static final ClipWindow FULL_CLIP = new ClipWindow(0.0D, 1.0D);

    private final BossAnimationMotionReview motionReview;
    private final BossAnimationPhaseWindowReview phaseWindowReview;
    private final Map<ContentId, String> sourceClipByLogicalKey;
    private final Map<ContentId, ClipWindow> sourceWindowByLogicalKey;

    /**
     * Backwards-compatible full-clip binding for general/non-partitioned clips.
     */
    public BossAnimationSourceBinding(
        BossAnimationMotionReview motionReview,
        Map<ContentId, String> sourceClipByLogicalKey
    ) {
        this(motionReview, null, sourceClipByLogicalKey, fullWindows(sourceClipByLogicalKey));
    }

    /**
     * Explicit source windows without fine window evidence. Kept for API-free fixtures/general experimentation.
     *
     * <p>Production phase-partitioned bindings must use {@link #reviewed} so arbitrary cut points cannot be published
     * as if they had been visually reviewed.</p>
     */
    public BossAnimationSourceBinding(
        BossAnimationMotionReview motionReview,
        Map<ContentId, String> sourceClipByLogicalKey,
        Map<ContentId, ClipWindow> sourceWindowByLogicalKey
    ) {
        this(motionReview, null, sourceClipByLogicalKey, sourceWindowByLogicalKey);
    }

    /**
     * Creates a production-eligible phase-partitioned binding only when every exact source window has fine review
     * evidence in addition to whole-clip motion review.
     */
    public static BossAnimationSourceBinding reviewed(
        BossAnimationMotionReview motionReview,
        BossAnimationPhaseWindowReview phaseWindowReview,
        Map<ContentId, String> sourceClipByLogicalKey,
        Map<ContentId, ClipWindow> sourceWindowByLogicalKey
    ) {
        return new BossAnimationSourceBinding(
            motionReview,
            Objects.requireNonNull(phaseWindowReview, "phaseWindowReview"),
            sourceClipByLogicalKey,
            sourceWindowByLogicalKey
        );
    }

    private BossAnimationSourceBinding(
        BossAnimationMotionReview motionReview,
        BossAnimationPhaseWindowReview phaseWindowReview,
        Map<ContentId, String> sourceClipByLogicalKey,
        Map<ContentId, ClipWindow> sourceWindowByLogicalKey
    ) {
        this.motionReview = Objects.requireNonNull(motionReview, "motionReview");
        this.phaseWindowReview = phaseWindowReview;
        Objects.requireNonNull(sourceClipByLogicalKey, "sourceClipByLogicalKey");
        Objects.requireNonNull(sourceWindowByLogicalKey, "sourceWindowByLogicalKey");
        if (sourceClipByLogicalKey.isEmpty()) {
            throw new IllegalArgumentException("boss animation source binding must not be empty");
        }
        if (!sourceClipByLogicalKey.keySet().equals(sourceWindowByLogicalKey.keySet())) {
            throw new IllegalArgumentException(
                "source clip windows must exactly cover logical animation keys: bindings="
                    + sourceClipByLogicalKey.keySet() + ", windows=" + sourceWindowByLogicalKey.keySet()
            );
        }

        Map<ContentId, String> clipCopy = new LinkedHashMap<>();
        Map<ContentId, ClipWindow> windowCopy = new LinkedHashMap<>();
        sourceClipByLogicalKey.forEach((logicalKey, sourceClipName) -> {
            Objects.requireNonNull(logicalKey, "logical animation key");
            if (sourceClipName == null || sourceClipName.isBlank()) {
                throw new IllegalArgumentException("source clip name must be non-blank for " + logicalKey);
            }
            motionReview.requireApproved(sourceClipName);
            ClipWindow window = Objects.requireNonNull(
                sourceWindowByLogicalKey.get(logicalKey),
                "source clip window for " + logicalKey
            );
            if (phaseWindowReview != null) {
                phaseWindowReview.requireApproved(sourceClipName, window);
            }
            clipCopy.put(logicalKey, sourceClipName);
            windowCopy.put(logicalKey, window);
        });
        this.sourceClipByLogicalKey = Map.copyOf(clipCopy);
        this.sourceWindowByLogicalKey = Map.copyOf(windowCopy);
    }

    public Map<ContentId, AnimationClip> resolve(AnimationClipInventory verifiedInventory) {
        Objects.requireNonNull(verifiedInventory, "verifiedInventory");
        Map<ContentId, AnimationClip> resolved = new LinkedHashMap<>();
        resolveWindows(verifiedInventory).forEach((logicalKey, source) -> resolved.put(logicalKey, source.clip()));
        return Map.copyOf(resolved);
    }

    public Map<ContentId, ResolvedSource> resolveWindows(AnimationClipInventory verifiedInventory) {
        Objects.requireNonNull(verifiedInventory, "verifiedInventory");
        Map<ContentId, ResolvedSource> resolved = new LinkedHashMap<>();
        sourceClipByLogicalKey.forEach((logicalKey, sourceClipName) -> {
            motionReview.requireApproved(sourceClipName);
            ClipWindow window = sourceWindowByLogicalKey.get(logicalKey);
            if (phaseWindowReview != null) {
                phaseWindowReview.requireApproved(sourceClipName, window);
            }
            resolved.put(logicalKey, new ResolvedSource(
                verifiedInventory.requireClip(sourceClipName),
                window
            ));
        });
        return Map.copyOf(resolved);
    }

    /**
     * Fails closed when a caller is about to use an explicit phase-partitioned binding in production without
     * fine window evidence.
     */
    public BossAnimationSourceBinding requireReviewedPhaseWindows() {
        if (phaseWindowReview == null) {
            throw new IllegalStateException("boss animation source binding has no reviewed phase-window evidence");
        }
        return this;
    }

    public boolean hasReviewedPhaseWindows() {
        return phaseWindowReview != null;
    }

    public BossAnimationMotionReview motionReview() {
        return motionReview;
    }

    public Map<ContentId, String> sourceClipByLogicalKey() {
        return sourceClipByLogicalKey;
    }

    public Map<ContentId, ClipWindow> sourceWindowByLogicalKey() {
        return sourceWindowByLogicalKey;
    }

    public record ClipWindow(double normalizedStart, double normalizedEnd) {
        public ClipWindow {
            if (!Double.isFinite(normalizedStart) || !Double.isFinite(normalizedEnd)) {
                throw new IllegalArgumentException("source clip window bounds must be finite");
            }
            if (normalizedStart < 0.0D || normalizedStart > 1.0D
                || normalizedEnd < 0.0D || normalizedEnd > 1.0D) {
                throw new IllegalArgumentException("source clip window bounds must be between 0 and 1");
            }
            if (normalizedEnd < normalizedStart) {
                throw new IllegalArgumentException("source clip window end must be >= start");
            }
        }

        public double sample(double phaseProgress) {
            if (!Double.isFinite(phaseProgress) || phaseProgress < 0.0D || phaseProgress > 1.0D) {
                throw new IllegalArgumentException("phaseProgress must be finite and between 0 and 1");
            }
            return normalizedStart + (normalizedEnd - normalizedStart) * phaseProgress;
        }
    }

    public record ResolvedSource(AnimationClip clip, ClipWindow window) {
        public ResolvedSource {
            Objects.requireNonNull(clip, "clip");
            Objects.requireNonNull(window, "window");
        }
    }

    private static Map<ContentId, ClipWindow> fullWindows(Map<ContentId, String> sourceClipByLogicalKey) {
        Objects.requireNonNull(sourceClipByLogicalKey, "sourceClipByLogicalKey");
        Map<ContentId, ClipWindow> windows = new LinkedHashMap<>();
        sourceClipByLogicalKey.keySet().forEach(key -> windows.put(key, FULL_CLIP));
        return Map.copyOf(windows);
    }
}
