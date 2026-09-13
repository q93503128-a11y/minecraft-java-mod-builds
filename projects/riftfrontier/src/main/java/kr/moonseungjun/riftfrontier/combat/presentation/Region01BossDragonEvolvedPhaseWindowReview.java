package kr.moonseungjun.riftfrontier.combat.presentation;

import java.util.List;

/**
 * Production-facing source-motion window evidence for the exact accepted Region 01 Dragon Evolved derivation.
 *
 * <p>The numerical bounds in this catalog are copied from the reviewed source receipt at
 * {@code assets/sources/region_01_boss_dragon_evolved.phase_window_review.json}. They describe visually inspected
 * source-motion segments only. In particular, an {@link BossAnimationPhaseWindowReview.Segment#ACTION ACTION}
 * segment does not authorize damage, hit geometry, attack timing, encounter attachment, VFX or sound. Those remain
 * owned by server-authoritative combat semantics and later presentation gates.</p>
 */
public final class Region01BossDragonEvolvedPhaseWindowReview {
    public static final String EVIDENCE_RECEIPT =
        "assets/sources/region_01_boss_dragon_evolved.phase_window_review.json#reviewed_clips";

    private static final BossAnimationPhaseWindowReview REVIEW = new BossAnimationPhaseWindowReview(List.of(
        approved(
            "Headbutt",
            BossAnimationPhaseWindowReview.Segment.ANTICIPATION,
            0.0D,
            0.15555555555555556D,
            "pre-commit head/torso preparation ending immediately before the abrupt forward/downward drive"
        ),
        approved(
            "Headbutt",
            BossAnimationPhaseWindowReview.Segment.ACTION,
            0.15555555555555556D,
            0.2D,
            "abrupt committed forward/downward head-and-torso drive through the maximum-displacement pose and into the first recovery key"
        ),
        approved(
            "Headbutt",
            BossAnimationPhaseWindowReview.Segment.RECOVERY,
            0.2D,
            0.4888888888888889D,
            "continuous return from the committed headbutt pose toward the starting airborne silhouette"
        ),
        approved(
            "Punch",
            BossAnimationPhaseWindowReview.Segment.ANTICIPATION,
            0.0D,
            0.2D,
            "forelimb and torso preparation before the committed outward extension"
        ),
        approved(
            "Punch",
            BossAnimationPhaseWindowReview.Segment.ACTION,
            0.2D,
            0.275D,
            "committed forelimb extension through the maximum reach and the first transition into retraction"
        ),
        approved(
            "Punch",
            BossAnimationPhaseWindowReview.Segment.RECOVERY,
            0.275D,
            1.0D,
            "sustained forelimb retraction and whole-body return toward the starting airborne pose"
        )
    ));

    private Region01BossDragonEvolvedPhaseWindowReview() {
    }

    public static BossAnimationPhaseWindowReview reviewedSourceWindows() {
        return REVIEW;
    }

    public static BossAnimationPhaseWindowReview.ApprovedWindow requireReviewed(
        String sourceClipName,
        BossAnimationSourceBinding.ClipWindow window
    ) {
        return REVIEW.requireApproved(sourceClipName, window);
    }

    private static BossAnimationPhaseWindowReview.ApprovedWindow approved(
        String sourceClipName,
        BossAnimationPhaseWindowReview.Segment segment,
        double normalizedStart,
        double normalizedEnd,
        String observedMotion
    ) {
        return new BossAnimationPhaseWindowReview.ApprovedWindow(
            sourceClipName,
            segment,
            new BossAnimationSourceBinding.ClipWindow(normalizedStart, normalizedEnd),
            EVIDENCE_RECEIPT + "/" + sourceClipName + "/" + segment.name(),
            observedMotion
        );
    }
}
