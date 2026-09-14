package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Region01BossReviewedFieldAnimationPreviewTest {
    @Test
    void reviewedStrikeAndLineStatesResolveTheirExactLogicalPhaseKeys() {
        assertKey("riftfrontier:attack/boss/region_01_committed_strike", "TELEGRAPH",
            "riftfrontier:animation/boss/region_01_committed_strike/telegraph");
        assertKey("riftfrontier:attack/boss/region_01_committed_strike", "ACTIVE",
            "riftfrontier:animation/boss/region_01_committed_strike/active");
        assertKey("riftfrontier:attack/boss/region_01_committed_strike", "RECOVERY",
            "riftfrontier:animation/boss/region_01_committed_strike/recovery");

        assertKey("riftfrontier:attack/boss/region_01_line_displacement", "TELEGRAPH",
            "riftfrontier:animation/boss/region_01_line_displacement/telegraph");
        assertKey("riftfrontier:attack/boss/region_01_line_displacement", "ACTIVE",
            "riftfrontier:animation/boss/region_01_line_displacement/active");
        assertKey("riftfrontier:attack/boss/region_01_line_displacement", "RECOVERY",
            "riftfrontier:animation/boss/region_01_line_displacement/recovery");
    }

    @Test
    void unresolvedArenaPressureNeverReceivesARecycledReviewedAttackClip() {
        for (String phase : List.of("TELEGRAPH", "ACTIVE", "RECOVERY")) {
            assertTrue(Region01BossReviewedFieldAnimationPreview.reviewedLogicalAnimationKey(
                active("riftfrontier:attack/boss/region_01_arena_pressure", phase)
            ).isEmpty());
        }
    }

    @Test
    void inactiveOrMalformedSemanticStateFailsClosed() {
        BossPresentationSemanticState inactive = BossPresentationSemanticState.clear(7, 10L);
        assertTrue(Region01BossReviewedFieldAnimationPreview.reviewedLogicalAnimationKey(inactive).isEmpty());

        BossPresentationSemanticState malformed = new BossPresentationSemanticState(
            7, 11L, true, 1,
            "riftfrontier:attack/boss/region_01_committed_strike",
            "NOT_A_PHASE", 0.5D, "strike", "committed", List.of(), false
        );
        assertTrue(Region01BossReviewedFieldAnimationPreview.reviewedLogicalAnimationKey(malformed).isEmpty());
    }

    private static void assertKey(String patternId, String phase, String expected) {
        ContentId actual = Region01BossReviewedFieldAnimationPreview.reviewedLogicalAnimationKey(active(patternId, phase))
            .orElseThrow();
        assertEquals(ContentId.parse(expected), actual);
    }

    private static BossPresentationSemanticState active(String patternId, String phase) {
        boolean hitWindow = phase.equals("ACTIVE");
        return new BossPresentationSemanticState(
            7, 10L, true, 1, patternId, phase, 0.5D, "reviewed", "reviewed", List.of(), hitWindow
        );
    }
}
