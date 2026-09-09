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
 * inventory.</p>
 */
public final class BossAnimationSourceBinding {
    private final BossAnimationMotionReview motionReview;
    private final Map<ContentId, String> sourceClipByLogicalKey;

    public BossAnimationSourceBinding(
        BossAnimationMotionReview motionReview,
        Map<ContentId, String> sourceClipByLogicalKey
    ) {
        this.motionReview = Objects.requireNonNull(motionReview, "motionReview");
        Objects.requireNonNull(sourceClipByLogicalKey, "sourceClipByLogicalKey");
        if (sourceClipByLogicalKey.isEmpty()) {
            throw new IllegalArgumentException("boss animation source binding must not be empty");
        }
        Map<ContentId, String> copy = new LinkedHashMap<>();
        sourceClipByLogicalKey.forEach((logicalKey, sourceClipName) -> {
            Objects.requireNonNull(logicalKey, "logical animation key");
            if (sourceClipName == null || sourceClipName.isBlank()) {
                throw new IllegalArgumentException("source clip name must be non-blank for " + logicalKey);
            }
            motionReview.requireApproved(sourceClipName);
            copy.put(logicalKey, sourceClipName);
        });
        this.sourceClipByLogicalKey = Map.copyOf(copy);
    }

    public Map<ContentId, AnimationClip> resolve(AnimationClipInventory verifiedInventory) {
        Objects.requireNonNull(verifiedInventory, "verifiedInventory");
        Map<ContentId, AnimationClip> resolved = new LinkedHashMap<>();
        sourceClipByLogicalKey.forEach((logicalKey, sourceClipName) -> {
            motionReview.requireApproved(sourceClipName);
            resolved.put(logicalKey, verifiedInventory.requireClip(sourceClipName));
        });
        return Map.copyOf(resolved);
    }

    public BossAnimationMotionReview motionReview() {
        return motionReview;
    }

    public Map<ContentId, String> sourceClipByLogicalKey() {
        return sourceClipByLogicalKey;
    }
}
