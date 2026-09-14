package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Region01BossArenaPressureFieldMotionCandidateTest {
    @Test
    void onlyArenaPressureReceivesTheAuthoredFieldReviewCandidate() {
        assertTrue(Region01BossArenaPressureFieldMotionCandidate.sample(
            active("riftfrontier:attack/boss/region_01_committed_strike", "TELEGRAPH", 0.5D)).isEmpty());
        assertTrue(Region01BossArenaPressureFieldMotionCandidate.sample(
            active("riftfrontier:attack/boss/region_01_line_displacement", "ACTIVE", 0.5D)).isEmpty());
        assertTrue(Region01BossArenaPressureFieldMotionCandidate.sample(
            BossPresentationSemanticState.clear(7, 10L)).isEmpty());
    }

    @Test
    void silhouetteLoadsInwardSnapsOutwardAndSettlesWithoutASecondPulse() {
        var telegraphEnd = Region01BossArenaPressureFieldMotionCandidate.sample(
            active(Region01BossArenaPressureFieldMotionCandidate.PATTERN, "TELEGRAPH", 1.0D)).orElseThrow();
        var activeStart = Region01BossArenaPressureFieldMotionCandidate.sample(
            active(Region01BossArenaPressureFieldMotionCandidate.PATTERN, "ACTIVE", 0.0D)).orElseThrow();
        var recoveryEnd = Region01BossArenaPressureFieldMotionCandidate.sample(
            active(Region01BossArenaPressureFieldMotionCandidate.PATTERN, "RECOVERY", 1.0D)).orElseThrow();

        assertEquals(0.94F, telegraphEnd.horizontal(), 0.0001F);
        assertEquals(1.04F, telegraphEnd.vertical(), 0.0001F);
        assertEquals(1.12F, activeStart.horizontal(), 0.0001F);
        assertEquals(0.94F, activeStart.vertical(), 0.0001F);
        assertEquals(1.0F, recoveryEnd.horizontal(), 0.0001F);
        assertEquals(1.0F, recoveryEnd.vertical(), 0.0001F);
    }

    private static BossPresentationSemanticState active(String patternId, String phase, double progress) {
        boolean hitWindow = phase.equals("ACTIVE");
        return new BossPresentationSemanticState(
            7, 10L, true, 2, patternId, phase, progress, "field-review", "field-review", List.of(), hitWindow
        );
    }
}
