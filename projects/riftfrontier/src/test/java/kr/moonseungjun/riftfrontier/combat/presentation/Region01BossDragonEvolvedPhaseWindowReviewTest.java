package kr.moonseungjun.riftfrontier.combat.presentation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Region01BossDragonEvolvedPhaseWindowReviewTest {
    @Test
    void exactReviewedHeadbuttAndPunchWindowsAreAvailable() {
        BossAnimationPhaseWindowReview review = Region01BossDragonEvolvedPhaseWindowReview.reviewedSourceWindows();

        assertWindows(review, "Headbutt", List.of(
            window(0.0D, 0.15555555555555556D),
            window(0.15555555555555556D, 0.2D),
            window(0.2D, 0.4888888888888889D)
        ));
        assertWindows(review, "Punch", List.of(
            window(0.0D, 0.2D),
            window(0.2D, 0.275D),
            window(0.275D, 1.0D)
        ));
    }

    @Test
    void unreviewedClipsAndApproximateWindowsFailClosed() {
        BossAnimationPhaseWindowReview review = Region01BossDragonEvolvedPhaseWindowReview.reviewedSourceWindows();

        assertThrows(IllegalArgumentException.class, () ->
            review.requireApproved("Flying_Idle", window(0.0D, 1.0D))
        );
        assertThrows(IllegalArgumentException.class, () ->
            review.requireApproved("Punch", window(0.2D, 0.28D))
        );
    }

    private static void assertWindows(
        BossAnimationPhaseWindowReview review,
        String clip,
        List<BossAnimationSourceBinding.ClipWindow> expected
    ) {
        List<BossAnimationPhaseWindowReview.ApprovedWindow> actual = review.approvedBySourceClip().get(clip);
        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual.stream().map(BossAnimationPhaseWindowReview.ApprovedWindow::window).toList());
        actual.forEach(window -> assertEquals(clip, window.sourceClipName()));
    }

    private static BossAnimationSourceBinding.ClipWindow window(double start, double end) {
        return new BossAnimationSourceBinding.ClipWindow(start, end);
    }
}
