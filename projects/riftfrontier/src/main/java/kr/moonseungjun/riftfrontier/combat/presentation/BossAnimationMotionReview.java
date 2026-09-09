package kr.moonseungjun.riftfrontier.combat.presentation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Human/visual-review evidence that a concrete imported source clip has been inspected for motion semantics.
 *
 * <p>Numerical glTF metadata, source clip names and inventory membership are deliberately insufficient to create a
 * production logical animation binding. Each source clip used by gameplay presentation must have an explicit approved
 * review entry describing what was actually observed and where the evidence lives.</p>
 */
public final class BossAnimationMotionReview {
    private final Map<String, ApprovedClip> approvedBySourceClip;

    public BossAnimationMotionReview(Map<String, ApprovedClip> approvedBySourceClip) {
        Objects.requireNonNull(approvedBySourceClip, "approvedBySourceClip");
        Map<String, ApprovedClip> copy = new LinkedHashMap<>();
        approvedBySourceClip.forEach((sourceClipName, approved) -> {
            if (sourceClipName == null || sourceClipName.isBlank()) {
                throw new IllegalArgumentException("reviewed source clip name must be non-blank");
            }
            Objects.requireNonNull(approved, "approved clip review for " + sourceClipName);
            if (!sourceClipName.equals(approved.sourceClipName())) {
                throw new IllegalArgumentException(
                    "motion review key does not match approved source clip: key=" + sourceClipName
                        + ", entry=" + approved.sourceClipName()
                );
            }
            copy.put(sourceClipName, approved);
        });
        this.approvedBySourceClip = Map.copyOf(copy);
    }

    public ApprovedClip requireApproved(String sourceClipName) {
        if (sourceClipName == null || sourceClipName.isBlank()) {
            throw new IllegalArgumentException("source clip name must be non-blank");
        }
        ApprovedClip approved = approvedBySourceClip.get(sourceClipName);
        if (approved == null) {
            throw new IllegalArgumentException(
                "source clip has no approved visual motion review: " + sourceClipName
            );
        }
        return approved;
    }

    public Map<String, ApprovedClip> approvedBySourceClip() {
        return approvedBySourceClip;
    }

    public record ApprovedClip(
        String sourceClipName,
        String evidenceId,
        String observedMotion
    ) {
        public ApprovedClip {
            if (sourceClipName == null || sourceClipName.isBlank()) {
                throw new IllegalArgumentException("approved source clip name must be non-blank");
            }
            if (evidenceId == null || evidenceId.isBlank()) {
                throw new IllegalArgumentException("motion review evidence id must be non-blank for " + sourceClipName);
            }
            if (observedMotion == null || observedMotion.isBlank()) {
                throw new IllegalArgumentException("observed motion must be non-blank for " + sourceClipName);
            }
        }
    }
}
