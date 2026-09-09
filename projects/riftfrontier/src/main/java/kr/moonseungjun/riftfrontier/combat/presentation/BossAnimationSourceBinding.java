package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClipInventory;
import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Explicit logical-animation-key -> verified imported source-clip-name binding.
 *
 * <p>This type intentionally does not infer mappings from similar names. A production binding is valid only when
 * every named source clip exists in the already-verified imported inventory.</p>
 */
public final class BossAnimationSourceBinding {
    private final Map<ContentId, String> sourceClipByLogicalKey;

    public BossAnimationSourceBinding(Map<ContentId, String> sourceClipByLogicalKey) {
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
            copy.put(logicalKey, sourceClipName);
        });
        this.sourceClipByLogicalKey = Map.copyOf(copy);
    }

    public Map<ContentId, AnimationClip> resolve(AnimationClipInventory verifiedInventory) {
        Objects.requireNonNull(verifiedInventory, "verifiedInventory");
        Map<ContentId, AnimationClip> resolved = new LinkedHashMap<>();
        sourceClipByLogicalKey.forEach((logicalKey, sourceClipName) ->
            resolved.put(logicalKey, verifiedInventory.requireClip(sourceClipName))
        );
        return Map.copyOf(resolved);
    }

    public Map<ContentId, String> sourceClipByLogicalKey() {
        return sourceClipByLogicalKey;
    }
}
