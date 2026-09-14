package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;
import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationPose;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class M5IronGolemProductionPresentationTest {
    @Test
    void ironFistUsesOneArmHeavyOffensivePose() {
        BattleStageCharacterPresentation.Pose pose = BattleStageCharacterPresentation.pose(
                "turnbound_re:iron_golem", "p1",
                cue("p1", "turnbound_re:iron_golem_iron_fist", List.of("e1"),
                        BattleActionTimelineState.Phase.WINDUP, 0.45D,
                        BattleActionTimelineState.PresentationStyle.STANDARD));

        assertTrue(pose.controlsAggressive());
        assertTrue(pose.aggressive());
        assertEquals(TurnboundPresentationPose.OFFENSIVE, pose.modelPose());
        assertTrue(pose.yAngle() > BattleStageCharacterPresentation.DEFAULT_Y_ANGLE);
    }

    @Test
    void guardianPlateIsDefensiveAndNeverReusesAttackPose() {
        BattleActionTimelineState.Cue cue = cue(
                "p1", "turnbound_re:iron_golem_guardian_plate", List.of("p2"),
                BattleActionTimelineState.Phase.IMPACT, 0.25D,
                BattleActionTimelineState.PresentationStyle.STANDARD);
        BattleStageCharacterPresentation.Pose pose = BattleStageCharacterPresentation.pose(
                "turnbound_re:iron_golem", "p1", cue);

        assertTrue(pose.controlsAggressive());
        assertFalse(pose.aggressive());
        assertEquals(TurnboundPresentationPose.DEFENSIVE, pose.modelPose());
        assertTrue(pose.offsetY() < 0);
        assertTrue(BattleStageSignatureFx.isIronGolemGuardianCue(cue));
        assertTrue(BattleStageSignatureFx.ironGolemGuardianAccentVisible(cue));
    }

    @Test
    void groundSlamAndVillageJudgmentUseDifferentHeavySilhouettes() {
        BattleStageCharacterPresentation.Pose slam = BattleStageCharacterPresentation.pose(
                "turnbound_re:iron_golem", "p1",
                cue("p1", "turnbound_re:iron_golem_ground_slam", List.of("e1", "e2"),
                        BattleActionTimelineState.Phase.WINDUP, 0.60D,
                        BattleActionTimelineState.PresentationStyle.SLAM));
        BattleStageCharacterPresentation.Pose judgment = BattleStageCharacterPresentation.pose(
                "turnbound_re:iron_golem", "p1",
                cue("p1", "turnbound_re:iron_golem_village_judgment", List.of("e1"),
                        BattleActionTimelineState.Phase.WINDUP, 0.60D,
                        BattleActionTimelineState.PresentationStyle.HEAVY));

        assertEquals(TurnboundPresentationPose.SLAM, slam.modelPose());
        assertEquals(TurnboundPresentationPose.EXECUTE, judgment.modelPose());
        assertTrue(slam.aggressive());
        assertTrue(judgment.aggressive());
        assertTrue(judgment.yAngle() > slam.yAngle());
        assertTrue(judgment.xAngle() < slam.xAngle());
    }

    @Test
    void unknownMeleeRecoveryAndOtherActorFailClosedToNeutral() {
        BattleStageCharacterPresentation.Pose unknown = BattleStageCharacterPresentation.pose(
                "turnbound_re:iron_golem", "p1",
                cue("p1", "turnbound_re:unknown_melee", List.of("e1"),
                        BattleActionTimelineState.Phase.WINDUP, 0.4D,
                        BattleActionTimelineState.PresentationStyle.STANDARD));
        BattleStageCharacterPresentation.Pose recovery = BattleStageCharacterPresentation.pose(
                "turnbound_re:iron_golem", "p1",
                cue("p1", "turnbound_re:iron_golem_ground_slam", List.of("e1", "e2"),
                        BattleActionTimelineState.Phase.RECOVERY, 0.2D,
                        BattleActionTimelineState.PresentationStyle.SLAM));
        BattleStageCharacterPresentation.Pose otherActor = BattleStageCharacterPresentation.pose(
                "turnbound_re:iron_golem", "p2",
                cue("p1", "turnbound_re:iron_golem_village_judgment", List.of("e1"),
                        BattleActionTimelineState.Phase.WINDUP, 0.4D,
                        BattleActionTimelineState.PresentationStyle.HEAVY));

        for (BattleStageCharacterPresentation.Pose pose : List.of(unknown, recovery, otherActor)) {
            assertTrue(pose.controlsAggressive());
            assertFalse(pose.aggressive());
            assertEquals(TurnboundPresentationPose.NEUTRAL, pose.modelPose());
            assertEquals(0, pose.offsetY());
            assertEquals(BattleStageCharacterPresentation.DEFAULT_X_ANGLE, pose.xAngle());
            assertEquals(BattleStageCharacterPresentation.DEFAULT_Y_ANGLE, pose.yAngle());
        }
    }

    @Test
    void guardianCueRejectsWrongActionAndSelfOnlyTarget() {
        BattleActionTimelineState.Cue offensive = cue(
                "p1", "turnbound_re:iron_golem_iron_fist", List.of("e1"),
                BattleActionTimelineState.Phase.IMPACT, 0.2D,
                BattleActionTimelineState.PresentationStyle.STANDARD);
        BattleActionTimelineState.Cue selfOnly = cue(
                "p1", "turnbound_re:iron_golem_guardian_plate", List.of("p1"),
                BattleActionTimelineState.Phase.IMPACT, 0.2D,
                BattleActionTimelineState.PresentationStyle.STANDARD);

        assertFalse(BattleStageSignatureFx.isIronGolemGuardianCue(offensive));
        assertFalse(BattleStageSignatureFx.isIronGolemGuardianCue(selfOnly));
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
                BattleActionTimelineState.MotionStyle.CLOSE,
                BattleActionTimelineState.ImpactStyle.MELEE,
                style,
                phase,
                progress,
                0,
                1);
    }
}
