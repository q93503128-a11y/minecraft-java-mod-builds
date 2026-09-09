package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M5BattleStageCharacterPresentationTest {
    @Test
    void skeletonAlwaysCarriesBowButOnlyAimsDuringItsProjectileBeat() {
        assertTrue(BattleStageCharacterPresentation.usesBow("turnbound_re:skeleton"));
        assertFalse(BattleStageCharacterPresentation.usesBow("turnbound_re:zombie"));

        BattleStageCharacterPresentation.Pose aiming = BattleStageCharacterPresentation.pose(
                "turnbound_re:skeleton", "p1",
                cue("p1", BattleActionTimelineState.ImpactStyle.PROJECTILE,
                        BattleActionTimelineState.Phase.WINDUP, 0.6D, 0));
        assertTrue(aiming.controlsAggressive());
        assertTrue(aiming.aggressive());
        assertTrue(aiming.yAngle() > BattleStageCharacterPresentation.DEFAULT_Y_ANGLE);

        BattleStageCharacterPresentation.Pose recovering = BattleStageCharacterPresentation.pose(
                "turnbound_re:skeleton", "p1",
                cue("p1", BattleActionTimelineState.ImpactStyle.PROJECTILE,
                        BattleActionTimelineState.Phase.RECOVERY, 0.2D, 0));
        assertTrue(recovering.controlsAggressive());
        assertFalse(recovering.aggressive());
        assertEquals(BattleStageCharacterPresentation.DEFAULT_Y_ANGLE, recovering.yAngle());
    }

    @Test
    void skeletonDoesNotAimWhenAnotherParticipantActs() {
        BattleStageCharacterPresentation.Pose pose = BattleStageCharacterPresentation.pose(
                "turnbound_re:skeleton", "p2",
                cue("p1", BattleActionTimelineState.ImpactStyle.PROJECTILE,
                        BattleActionTimelineState.Phase.WINDUP, 0.5D, 0));
        assertFalse(pose.aggressive());
    }

    @Test
    void endermanVoidBeatMovesTheActualModelButReturnsToNeutral() {
        BattleStageCharacterPresentation.Pose windup = BattleStageCharacterPresentation.pose(
                "turnbound_re:enderman", "p1",
                cue("p1", BattleActionTimelineState.ImpactStyle.VOID,
                        BattleActionTimelineState.Phase.WINDUP, 0.5D, 0));
        assertTrue(Math.abs(windup.offsetX()) > 0);
        assertTrue(Math.abs(windup.offsetX()) <= 3);
        assertTrue(Math.abs(windup.yAngle() - BattleStageCharacterPresentation.DEFAULT_Y_ANGLE) <= 0.18F + 0.0001F);

        BattleStageCharacterPresentation.Pose done = BattleStageCharacterPresentation.pose(
                "turnbound_re:enderman", "p1",
                cue("p1", BattleActionTimelineState.ImpactStyle.VOID,
                        BattleActionTimelineState.Phase.RECOVERY, 1.0D, 0));
        assertEquals(0, done.offsetX());
        assertEquals(BattleStageCharacterPresentation.DEFAULT_X_ANGLE, done.xAngle());
        assertEquals(BattleStageCharacterPresentation.DEFAULT_Y_ANGLE, done.yAngle());
    }

    @Test
    void nonRepresentativeCharactersKeepExistingStageView() {
        assertEquals(BattleStageCharacterPresentation.Pose.DEFAULT,
                BattleStageCharacterPresentation.pose(
                        "turnbound_re:zombie", "p1",
                        cue("p1", BattleActionTimelineState.ImpactStyle.MELEE,
                                BattleActionTimelineState.Phase.WINDUP, 0.5D, 0)));
    }

    private static BattleActionTimelineState.Cue cue(
            String actorId,
            BattleActionTimelineState.ImpactStyle impactStyle,
            BattleActionTimelineState.Phase phase,
            double progress,
            int beatIndex
    ) {
        return new BattleActionTimelineState.Cue(
                actorId,
                "action",
                List.of("e1"),
                BattleActionTimelineState.MotionStyle.RANGED,
                impactStyle,
                impactStyle == BattleActionTimelineState.ImpactStyle.VOID
                        ? BattleActionTimelineState.PresentationStyle.RIFT
                        : BattleActionTimelineState.PresentationStyle.VOLLEY,
                phase,
                progress,
                beatIndex,
                1);
    }
}
