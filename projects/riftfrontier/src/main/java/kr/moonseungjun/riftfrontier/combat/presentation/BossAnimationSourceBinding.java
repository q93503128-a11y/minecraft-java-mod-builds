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
    private final Map<ContentId, String> sourceClipByLogicalKey;
    private final Map<ContentId, ClipWindow> sourceWindowByLogicalKey;

    /**
     * Backwards-compatible full-clip binding. Production attack phases should prefer the explicit-window constructor
     * once visual review has authored phase boundaries.
     */
    public BossAnimationSourceBinding(
        BossAnimationMotionReview motionReview,
        Map<ContentId, String> sourceClipByLogicalKey
    ) {
        this(motionReview, sourceClipByLogicalKey, fullWindows(sourceClipByLogicalKey));
    }

    /**
     * Explicit source binding with a normalized [start, end] window for every logical key.
     *
     * <p>The window map must exactly cover the logical keys in {@code sourceClipByLogicalKey}. Missing or extra
     * windows are rejected rather than silently falling back to whole-clip playback.</p>
     */
    public BossAnimationSourceBinding(
        BossAnimationMotionReview motionReview,
        Map<ContentId, String> sourceClipByLogicalKey,
        Map<ContentId, ClipWindow> sourceWindowByLogicalKey
    ) {
        this.motionReview = Objects.requireNonNull(motionReview, "motionReview");
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
            resolved.put(logicalKey, new ResolvedSource(
                verifiedInventory.requireClip(sourceClipName),
                sourceWindowByLogicalKey.get(logicalKey)
            ));
        });
        return Map.copyOf(resolved);
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
