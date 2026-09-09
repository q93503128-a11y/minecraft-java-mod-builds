package kr.moonseungjun.riftfrontier.combat.presentation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Fine-grained visual evidence for source-animation motion windows.
 *
 * <p>A whole-clip {@link BossAnimationMotionReview} proves what a source clip visibly does. This companion gate
 * proves only that a concrete normalized sub-window was inspected as a source-motion segment. It deliberately does
 * not claim gameplay hit timing: server-authoritative {@code attack_pattern} timing remains a separate authored
 * contract.</p>
 */
public final class BossAnimationPhaseWindowReview {
    private final Map<String, List<ApprovedWindow>> approvedBySourceClip;

    public BossAnimationPhaseWindowReview(Collection<ApprovedWindow> approvals) {
        Objects.requireNonNull(approvals, "approvals");
        if (approvals.isEmpty()) {
            throw new IllegalArgumentException("phase-window review must contain at least one approval");
        }
        Map<String, List<ApprovedWindow>> grouped = new LinkedHashMap<>();
        for (ApprovedWindow approval : approvals) {
            Objects.requireNonNull(approval, "phase-window approval");
            List<ApprovedWindow> sameClip = grouped.computeIfAbsent(approval.sourceClipName(), ignored -> new ArrayList<>());
            boolean duplicateSegment = sameClip.stream().anyMatch(existing -> existing.segment() == approval.segment());
            if (duplicateSegment) {
                throw new IllegalArgumentException(
                    "duplicate reviewed source-motion segment " + approval.segment() + " for " + approval.sourceClipName()
                );
            }
            sameClip.add(approval);
        }
        Map<String, List<ApprovedWindow>> immutable = new LinkedHashMap<>();
        grouped.forEach((clip, windows) -> immutable.put(
            clip,
            windows.stream().sorted(Comparator.comparing(ApprovedWindow::segment)).toList()
        ));
        this.approvedBySourceClip = Map.copyOf(immutable);
    }

    /**
     * Requires an exact reviewed source window. Numerically close or overlapping windows are intentionally rejected.
     */
    public ApprovedWindow requireApproved(
        String sourceClipName,
        BossAnimationSourceBinding.ClipWindow window
    ) {
        if (sourceClipName == null || sourceClipName.isBlank()) {
            throw new IllegalArgumentException("source clip name must be non-blank");
        }
        Objects.requireNonNull(window, "window");
        List<ApprovedWindow> approvals = approvedBySourceClip.get(sourceClipName);
        if (approvals == null) {
            throw new IllegalArgumentException("source clip has no fine phase-window review: " + sourceClipName);
        }
        return approvals.stream()
            .filter(approval -> approval.window().equals(window))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "source clip window has no exact fine review: " + sourceClipName + " " + window
            ));
    }

    public Map<String, List<ApprovedWindow>> approvedBySourceClip() {
        return approvedBySourceClip;
    }

    public enum Segment {
        ANTICIPATION,
        ACTION,
        RECOVERY
    }

    /**
     * Source-motion observation only. {@code ACTION} does not itself authorize a Minecraft damage window.
     */
    public record ApprovedWindow(
        String sourceClipName,
        Segment segment,
        BossAnimationSourceBinding.ClipWindow window,
        String evidenceId,
        String observedMotion
    ) {
        public ApprovedWindow {
            if (sourceClipName == null || sourceClipName.isBlank()) {
                throw new IllegalArgumentException("reviewed source clip name must be non-blank");
            }
            Objects.requireNonNull(segment, "segment");
            Objects.requireNonNull(window, "window");
            if (evidenceId == null || evidenceId.isBlank()) {
                throw new IllegalArgumentException("phase-window evidence id must be non-blank for " + sourceClipName);
            }
            if (observedMotion == null || observedMotion.isBlank()) {
                throw new IllegalArgumentException("observed source-window motion must be non-blank for " + sourceClipName);
            }
        }
    }
}
