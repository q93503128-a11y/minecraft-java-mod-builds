package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class M5WitchProductionPresentationTest {
    @Test
    void offensiveArcaneActionsUseThrowingSilhouetteOnlyOnActiveEnemyBeat() {
        BattleStageCharacterPresentation.Pose attack = BattleStageCharacterPresentation.pose(
                "turnbound_re:witch", "p1",
                cue("p1", "turnbound_re:witch_weakening_brew", List.of("e1", "e2"),
                        BattleActionTimelineState.Phase.WINDUP, 0.55D,
                        BattleActionTimelineState.PresentationStyle.STANDARD));

        assertTrue(attack.controlsAggressive());
        assertTrue(attack.aggressive());
        assertTrue(attack.offsetY() < 0);
        assertTrue(attack.yAngle() > BattleStageCharacterPresentation.DEFAULT_Y_ANGLE);

        BattleStageCharacterPresentation.Pose recovery = BattleStageCharacterPresentation.pose(
                "turnbound_re:witch", "p1",
                cue("p1", "turnbound_re:witch_splash_hex", List.of("e1"),
                        BattleActionTimelineState.Phase.RECOVERY, 0.25D,
                        BattleActionTimelineState.PresentationStyle.STANDARD));
        assertTrue(recovery.controlsAggressive());
        assertFalse(recovery.aggressive());
        assertEquals(BattleStageCharacterPresentation.DEFAULT_Y_ANGLE, recovery.yAngle());
    }

    @Test
    void allySupportActionsStayNonAggressiveAndUseDistinctLift() {
        BattleStageCharacterPresentation.Pose draught = BattleStageCharacterPresentation.pose(
                "turnbound_re:witch", "p1",
                cue("p1", "turnbound_re:witch_restorative_draught", List.of("p2"),
                        BattleActionTimelineState.Phase.WINDUP, 0.45D,
                        BattleActionTimelineState.PresentationStyle.STANDARD));

        assertTrue(draught.controlsAggressive());
        assertFalse(draught.aggressive());
        assertTrue(draught.offsetY() < 0);
        assertTrue(draught.xAngle() < BattleStageCharacterPresentation.DEFAULT_X_ANGLE);
        assertTrue(draught.yAngle() < BattleStageCharacterPresentation.DEFAULT_Y_ANGLE);

        BattleStageCharacterPresentation.Pose overflow = BattleStageCharacterPresentation.pose(
                "turnbound_re:witch", "p1",
                cue("p1", "turnbound_re:witch_cauldron_overflow", List.of("p2", "p3", "p4"),
                        BattleActionTimelineState.Phase.IMPACT, 0.30D,
                        BattleActionTimelineState.PresentationStyle.RITUAL));
        assertTrue(overflow.controlsAggressive());
        assertFalse(overflow.aggressive());
        assertTrue(overflow.offsetY() < 0);
    }

    @Test
    void arcaneTagAloneCannotSelectWitchAttackOrSupportIdentity() {
        BattleStageCharacterPresentation.Pose unknown = BattleStageCharacterPresentation.pose(
                "turnbound_re:witch", "p1",
                cue("p1", "turnbound_re:unknown_arcane", List.of("e1"),
                        BattleActionTimelineState.Phase.WINDUP, 0.5D,
                        BattleActionTimelineState.PresentationStyle.STANDARD));
        assertTrue(unknown.controlsAggressive());
        assertFalse(unknown.aggressive());
        assertEquals(0, unknown.offsetY());
        assertEquals(BattleStageCharacterPresentation.DEFAULT_X_ANGLE, unknown.xAngle());
        assertEquals(BattleStageCharacterPresentation.DEFAULT_Y_ANGLE, unknown.yAngle());

        BattleStageCharacterPresentation.Pose otherActor = BattleStageCharacterPresentation.pose(
                "turnbound_re:witch", "p2",
                cue("p1", "turnbound_re:witch_weakening_brew", List.of("e1", "e2"),
                        BattleActionTimelineState.Phase.WINDUP, 0.5D,
                        BattleActionTimelineState.PresentationStyle.STANDARD));
        assertTrue(otherActor.controlsAggressive());
        assertFalse(otherActor.aggressive());
    }

    @Test
    void supportTransferAcceptsSingleAndMultiAllyActionsButRejectsOffense() {
        BattleActionTimelineState.Cue single = cue(
                "p1", "turnbound_re:witch_restorative_draught", List.of("p2"),
                BattleActionTimelineState.Phase.WINDUP, 0.5D,
                BattleActionTimelineState.PresentationStyle.STANDARD);
        BattleActionTimelineState.Cue multi = cue(
                "p1", "turnbound_re:witch_cauldron_overflow", List.of("p2", "p3", "p4"),
                BattleActionTimelineState.Phase.IMPACT, 0.2D,
                BattleActionTimelineState.PresentationStyle.RITUAL);
        BattleActionTimelineState.Cue offense = cue(
                "p1", "turnbound_re:witch_weakening_brew", List.of("e1", "e2"),
                BattleActionTimelineState.Phase.WINDUP, 0.5D,
                BattleActionTimelineState.PresentationStyle.STANDARD);

        assertTrue(BattleStageSignatureFx.isWitchSupportCue(single));
        assertTrue(BattleStageSignatureFx.isWitchSupportCue(multi));
        assertFalse(BattleStageSignatureFx.isWitchSupportCue(offense));
        assertTrue(BattleStageSignatureFx.witchSupportAccentVisible(multi));
    }

    @Test
    void supportArcIsBoundedAndMultiTargetTransferIsStaggered() {
        assertEquals(0, BattleStageSignatureFx.supportArcLift(0.0D));
        assertEquals(8, BattleStageSignatureFx.supportArcLift(0.5D));
        assertEquals(0, BattleStageSignatureFx.supportArcLift(1.0D));

        double first = BattleStageSignatureFx.supportTransferProgress(0.50D, 0);
        double second = BattleStageSignatureFx.supportTransferProgress(0.50D, 1);
        double third = BattleStageSignatureFx.supportTransferProgress(0.50D, 2);
        assertTrue(first > second);
        assertTrue(second > third);
        assertTrue(third > 0.0D);
    }

    private static BattleActionTimelineState.Cue cue(
            String actorId,
            String actionId,
            List<String> targets,
            BattleActionTimelineState.Phase phase,
            double progress,
            BattleActionTimelineState.PresentationStyle style
    ) {
        return new BattleActionTimelineState.Cue(
                actorId,
                actionId,
                targets,
                BattleActionTimelineState.MotionStyle.CAST,
                BattleActionTimelineState.ImpactStyle.ARCANE,
                style,
                phase,
                progress,
                0,
                1);
    }
}
