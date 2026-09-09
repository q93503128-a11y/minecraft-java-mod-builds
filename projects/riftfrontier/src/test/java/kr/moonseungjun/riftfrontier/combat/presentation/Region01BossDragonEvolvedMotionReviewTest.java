package kr.moonseungjun.riftfrontier.combat.presentation;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Region01BossDragonEvolvedMotionReviewTest {
    @Test
    void reviewExactlyCoversEveryAcceptedSourceClip() {
        var review = Region01BossDragonEvolvedMotionReview.reviewedSourceMotion();

        assertEquals(
            Set.copyOf(Region01BossDragonEvolvedAnimationContract.EXPECTED_SOURCE_CLIPS),
            review.approvedBySourceClip().keySet()
        );
        assertEquals(8, review.approvedBySourceClip().size());
    }

    @Test
    void everyReviewPointsAtTheCheckedInVisualEvidenceReceipt() {
        var review = Region01BossDragonEvolvedMotionReview.reviewedSourceMotion();

        review.approvedBySourceClip().forEach((clipName, approved) -> {
            assertEquals(clipName, approved.sourceClipName());
            assertTrue(approved.evidenceId().startsWith(Region01BossDragonEvolvedMotionReview.EVIDENCE_RECEIPT + "/"));
            assertFalse(approved.observedMotion().isBlank());
        });
    }

    @Test
    void visuallyDistinctHeadGestureClipsKeepDistinctObservedEvidence() {
        var no = Region01BossDragonEvolvedMotionReview.requireReviewed("No");
        var yes = Region01BossDragonEvolvedMotionReview.requireReviewed("Yes");

        assertNotEquals(no.observedMotion(), yes.observedMotion());
        assertThrows(IllegalArgumentException.class,
            () -> Region01BossDragonEvolvedMotionReview.requireReviewed("Unreviewed"));
    }
}
