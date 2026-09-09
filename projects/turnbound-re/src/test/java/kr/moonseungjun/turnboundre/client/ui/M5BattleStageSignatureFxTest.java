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
                BattleActionTimelineState.ImpactStyle.BLAST,
                BattleActionTimelineState.PresentationStyle.AREA,
                BattleActionTimelineState.Phase.IMPACT,
                0.3D);
        BattleActionTimelineState.Cue lateImpact = cue(
                BattleActionTimelineState.ImpactStyle.BLAST,
                BattleActionTimelineState.PresentationStyle.AREA,
                BattleActionTimelineState.Phase.IMPACT,
                0.8D);
        BattleActionTimelineState.Cue recovery = cue(
                BattleActionTimelineState.ImpactStyle.BLAST,
                BattleActionTimelineState.PresentationStyle.AREA,
                BattleActionTimelineState.Phase.RECOVERY,
                0.1D);

        assertTrue(BattleStageSignatureFx.accentVisible(impact));
        assertFalse(BattleStageSignatureFx.accentVisible(lateImpact));
        assertFalse(BattleStageSignatureFx.accentVisible(recovery));
    }

    @Test
    void representativeCharacterSignaturesStayNarrowAndIntentional() {
        assertEquals(BattleStageSignatureFx.CharacterSignature.SKELETON_MARKSMAN,
                BattleStageSignatureFx.characterSignature("turnbound_re:skeleton"));
        assertEquals(BattleStageSignatureFx.CharacterSignature.ENDER_RIFT,
                BattleStageSignatureFx.characterSignature("turnbound_re:enderman"));
        assertEquals(BattleStageSignatureFx.CharacterSignature.STANDARD,
                BattleStageSignatureFx.characterSignature("turnbound_re:zombie"));
        assertEquals(BattleStageSignatureFx.CharacterSignature.STANDARD,
                BattleStageSignatureFx.characterSignature(""));

        BattleActionTimelineState.Cue volley = cue(
                BattleActionTimelineState.ImpactStyle.PROJECTILE,
                BattleActionTimelineState.PresentationStyle.VOLLEY,
                BattleActionTimelineState.Phase.WINDUP,
                0.5D);
        BattleActionTimelineState.Cue rift = cue(
                BattleActionTimelineState.ImpactStyle.VOID,
                BattleActionTimelineState.PresentationStyle.RIFT,
                BattleActionTimelineState.Phase.WINDUP,
                0.5D);

        assertTrue(BattleStageSignatureFx.actorSignatureVisible(
                BattleStageSignatureFx.CharacterSignature.SKELETON_MARKSMAN, volley));
        assertFalse(BattleStageSignatureFx.actorSignatureVisible(
                BattleStageSignatureFx.CharacterSignature.SKELETON_MARKSMAN, rift));
        assertTrue(BattleStageSignatureFx.actorSignatureVisible(
                BattleStageSignatureFx.CharacterSignature.ENDER_RIFT, rift));
        assertFalse(BattleStageSignatureFx.actorSignatureVisible(
                BattleStageSignatureFx.CharacterSignature.STANDARD, volley));
    }

    @Test
    void enderSignatureExpandsTowardImpactWithoutLeavingModelBounds() {
        int start = BattleStageSignatureFx.signatureInset(0.0D, 42, 64);
        int middle = BattleStageSignatureFx.signatureInset(0.5D, 42, 64);
        int end = BattleStageSignatureFx.signatureInset(1.0D, 42, 64);

        assertTrue(start > middle);
        assertTrue(middle >= end);
        assertEquals(0, end);
        assertTrue(start * 2 < 42);
        assertTrue(start * 2 < 64);
    }

    @Test
    void riftOffsetPeaksMidFlightAndReturnsAtBothEnds() {
        assertEquals(0, BattleStageSignatureFx.riftSideOffset(0.0D));
        assertTrue(BattleStageSignatureFx.riftSideOffset(0.5D) > 0);
        assertEquals(0, BattleStageSignatureFx.riftSideOffset(1.0D));
    }

    private static BattleActionTimelineState.Cue cue(
            BattleActionTimelineState.ImpactStyle impactStyle,
            BattleActionTimelineState.PresentationStyle style,
            BattleActionTimelineState.Phase phase,
            double progress
    ) {
        return new BattleActionTimelineState.Cue(
                "p1", "action", List.of("e1"),
                BattleActionTimelineState.MotionStyle.CAST,
                impactStyle,
                style,
                phase, progress, 0, 1);
    }
}
