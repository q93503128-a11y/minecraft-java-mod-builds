package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M5BattleStageSignatureFxTest {
    @Test
    void volleyCopiesTrailTheBaseProjectileInsteadOfArrivingEarly() {
        assertEquals(0.0D, BattleStageSignatureFx.extraProjectileProgress(0.10D, 1));
        assertEquals(0.0D, BattleStageSignatureFx.extraProjectileProgress(0.25D, 2));

        double first = BattleStageSignatureFx.extraProjectileProgress(0.70D, 1);
        double second = BattleStageSignatureFx.extraProjectileProgress(0.70D, 2);
        assertTrue(first > second);
        assertTrue(first > 0.0D && first < 1.0D);
        assertTrue(second > 0.0D && second < 1.0D);
    }

    @Test
    void semanticFamiliesReuseExistingFrameLanguage() {
        assertEquals(UiVisualLanguage.FrameState.WARNING,
                BattleStageSignatureFx.accentState(BattleActionTimelineState.PresentationStyle.HEAVY));
        assertEquals(UiVisualLanguage.FrameState.WARNING,
                BattleStageSignatureFx.accentState(BattleActionTimelineState.PresentationStyle.RIFT));
        assertEquals(UiVisualLanguage.FrameState.SUCCESS,
                BattleStageSignatureFx.accentState(BattleActionTimelineState.PresentationStyle.RITUAL));
        assertEquals(UiVisualLanguage.FrameState.FOCUS,
                BattleStageSignatureFx.accentState(BattleActionTimelineState.PresentationStyle.VOLLEY));
    }

    @Test
    void signatureAccentExistsOnlyDuringEarlyImpact() {
        BattleActionTimelineState.Cue impact = cue(
                BattleActionTimelineState.PresentationStyle.AREA,
                BattleActionTimelineState.Phase.IMPACT,
                0.3D);
        BattleActionTimelineState.Cue lateImpact = cue(
                BattleActionTimelineState.PresentationStyle.AREA,
                BattleActionTimelineState.Phase.IMPACT,
                0.8D);
        BattleActionTimelineState.Cue recovery = cue(
                BattleActionTimelineState.PresentationStyle.AREA,
                BattleActionTimelineState.Phase.RECOVERY,
                0.1D);

        assertTrue(BattleStageSignatureFx.accentVisible(impact));
        assertFalse(BattleStageSignatureFx.accentVisible(lateImpact));
        assertFalse(BattleStageSignatureFx.accentVisible(recovery));
    }

    @Test
    void riftOffsetPeaksMidFlightAndReturnsAtBothEnds() {
        assertEquals(0, BattleStageSignatureFx.riftSideOffset(0.0D));
        assertTrue(BattleStageSignatureFx.riftSideOffset(0.5D) > 0);
        assertEquals(0, BattleStageSignatureFx.riftSideOffset(1.0D));
    }

    private static BattleActionTimelineState.Cue cue(
            BattleActionTimelineState.PresentationStyle style,
            BattleActionTimelineState.Phase phase,
            double progress
    ) {
        return new BattleActionTimelineState.Cue(
                "p1", "action", List.of("e1"),
                BattleActionTimelineState.MotionStyle.CAST,
                BattleActionTimelineState.ImpactStyle.BLAST,
                style,
                phase, progress, 0, 1);
    }
}
