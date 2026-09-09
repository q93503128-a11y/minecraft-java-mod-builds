package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BossAnimationPhaseWindowReviewTest {
    private static final ContentId TELEGRAPH = ContentId.rift("boss/region_01/headbutt/telegraph");
    private static final ContentId ACTIVE = ContentId.rift("boss/region_01/headbutt/active");
    private static final ContentId RECOVERY = ContentId.rift("boss/region_01/headbutt/recovery");

    @Test
    void productionReviewedBindingRequiresExactFineReviewedWindows() {
        var anticipation = new BossAnimationSourceBinding.ClipWindow(0.0D, 7.0D / 45.0D);
        var action = new BossAnimationSourceBinding.ClipWindow(7.0D / 45.0D, 9.0D / 45.0D);
        var recovery = new BossAnimationSourceBinding.ClipWindow(9.0D / 45.0D, 22.0D / 45.0D);
        BossAnimationPhaseWindowReview phaseReview = new BossAnimationPhaseWindowReview(List.of(
            approved("Headbutt", BossAnimationPhaseWindowReview.Segment.ANTICIPATION, anticipation),
            approved("Headbutt", BossAnimationPhaseWindowReview.Segment.ACTION, action),
            approved("Headbutt", BossAnimationPhaseWindowReview.Segment.RECOVERY, recovery)
        ));

        BossAnimationSourceBinding binding = BossAnimationSourceBinding.reviewed(
            motionReview("Headbutt"),
            phaseReview,
            Map.of(TELEGRAPH, "Headbutt", ACTIVE, "Headbutt", RECOVERY, "Headbutt"),
            Map.of(TELEGRAPH, anticipation, ACTIVE, action, RECOVERY, recovery)
        );

        assertTrue(binding.hasReviewedPhaseWindows());
        assertEquals(binding, binding.requireReviewedPhaseWindows());
        assertEquals(action, binding.sourceWindowByLogicalKey().get(ACTIVE));
    }

    @Test
    void alteredOrOverlappingCutCannotMasqueradeAsReviewedWindow() {
        var reviewedAction = new BossAnimationSourceBinding.ClipWindow(7.0D / 45.0D, 9.0D / 45.0D);
        BossAnimationPhaseWindowReview phaseReview = new BossAnimationPhaseWindowReview(List.of(
            approved("Headbutt", BossAnimationPhaseWindowReview.Segment.ACTION, reviewedAction)
        ));

        var altered = new BossAnimationSourceBinding.ClipWindow(7.0D / 45.0D, 10.0D / 45.0D);
        assertThrows(IllegalArgumentException.class, () -> BossAnimationSourceBinding.reviewed(
            motionReview("Headbutt"),
            phaseReview,
            Map.of(ACTIVE, "Headbutt"),
            Map.of(ACTIVE, altered)
        ));
    }

    @Test
    void reviewEvidenceIsClipSpecificAndRejectsDuplicateSegments() {
        var action = new BossAnimationSourceBinding.ClipWindow(0.2D, 0.275D);
        BossAnimationPhaseWindowReview punchReview = new BossAnimationPhaseWindowReview(List.of(
            approved("Punch", BossAnimationPhaseWindowReview.Segment.ACTION, action)
        ));

        assertThrows(IllegalArgumentException.class, () -> punchReview.requireApproved("Headbutt", action));
        assertThrows(IllegalArgumentException.class, () -> new BossAnimationPhaseWindowReview(List.of(
            approved("Punch", BossAnimationPhaseWindowReview.Segment.ACTION, action),
            approved("Punch", BossAnimationPhaseWindowReview.Segment.ACTION,
                new BossAnimationSourceBinding.ClipWindow(0.21D, 0.28D))
        )));
    }

    @Test
    void legacyExplicitWindowsRemainNonProductionUntilFineReviewIsAttached() {
        BossAnimationSourceBinding legacy = new BossAnimationSourceBinding(
            motionReview("Punch"),
            Map.of(ACTIVE, "Punch"),
            Map.of(ACTIVE, new BossAnimationSourceBinding.ClipWindow(0.2D, 0.275D))
        );

        assertFalse(legacy.hasReviewedPhaseWindows());
        assertThrows(IllegalStateException.class, legacy::requireReviewedPhaseWindows);
    }

    private static BossAnimationPhaseWindowReview.ApprovedWindow approved(
        String sourceClip,
        BossAnimationPhaseWindowReview.Segment segment,
        BossAnimationSourceBinding.ClipWindow window
    ) {
        return new BossAnimationPhaseWindowReview.ApprovedWindow(
            sourceClip,
            segment,
            window,
            "fixture:phase-window:" + sourceClip + ":" + segment,
            "fixture reviewed " + segment + " source motion"
        );
    }

    private static BossAnimationMotionReview motionReview(String sourceClip) {
        return new BossAnimationMotionReview(Map.of(
            sourceClip,
            new BossAnimationMotionReview.ApprovedClip(
                sourceClip,
                "fixture:motion:" + sourceClip,
                "fixture whole-clip motion review"
            )
        ));
    }
}
