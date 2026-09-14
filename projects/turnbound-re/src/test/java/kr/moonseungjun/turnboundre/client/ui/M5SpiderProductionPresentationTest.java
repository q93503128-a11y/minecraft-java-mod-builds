package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;
import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationPose;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class M5SpiderProductionPresentationTest {
    @Test
    void fangAndVenomUseDifferentBiteSilhouettes() {
        BattleStageCharacterPresentation.Pose fang = BattleStageCharacterPresentation.pose(
                "turnbound_re:spider", "p1",
                cue("p1", "turnbound_re:spider_fang", List.of("e1"),
                        BattleActionTimelineState.ImpactStyle.MELEE,
                        BattleActionTimelineState.Phase.WINDUP, 0.45D,
                        BattleActionTimelineState.PresentationStyle.STANDARD));
        BattleStageCharacterPresentation.Pose venom = BattleStageCharacterPresentation.pose(
                "turnbound_re:spider", "p1",
                cue("p1", "turnbound_re:spider_venom_bite", List.of("e1"),
                        BattleActionTimelineState.ImpactStyle.MELEE,
                        BattleActionTimelineState.Phase.WINDUP, 0.45D,
                        BattleActionTimelineState.PresentationStyle.STANDARD));

        assertEquals(TurnboundPresentationPose.OFFENSIVE, fang.modelPose());
        assertEquals(TurnboundPresentationPose.VENOM, venom.modelPose());
        assertTrue(fang.aggressive());
        assertTrue(venom.aggressive());
        assertTrue(venom.offsetY() < fang.offsetY());
    }

    @Test
    void bindingWebIsExactProjectileCue() {
        BattleActionTimelineState.Cue cue = cue(
                "p1", "turnbound_re:spider_binding_web", List.of("e1"),
                BattleActionTimelineState.ImpactStyle.PROJECTILE,
                BattleActionTimelineState.Phase.WINDUP, 0.35D,
                BattleActionTimelineState.PresentationStyle.STANDARD);
        BattleStageCharacterPresentation.Pose pose = BattleStageCharacterPresentation.pose(
                "turnbound_re:spider", "p1", cue);

        assertTrue(BattleStageCharacterPresentation.isSpiderBindingWebCue(cue));
        assertEquals(TurnboundPresentationPose.WEB, pose.modelPose());
        assertTrue(pose.aggressive());
    }

    @Test
    void broodPounceUsesReservedBurstCoil() {
        BattleStageCharacterPresentation.Pose pose = BattleStageCharacterPresentation.pose(
                "turnbound_re:spider", "p1",
                cue("p1", "turnbound_re:spider_brood_pounce", List.of("e1"),
                        BattleActionTimelineState.ImpactStyle.MELEE,
                        BattleActionTimelineState.Phase.WINDUP, 0.55D,
                        BattleActionTimelineState.PresentationStyle.HEAVY));

        assertEquals(TurnboundPresentationPose.POUNCE, pose.modelPose());
        assertTrue(pose.aggressive());
        assertTrue(pose.offsetY() < -1);
        assertTrue(pose.yAngle() > BattleStageCharacterPresentation.DEFAULT_Y_ANGLE);
    }

    @Test
    void malformedWebUnknownMeleeRecoveryAndOtherActorFailClosed() {
        BattleStageCharacterPresentation.Pose malformedWeb = BattleStageCharacterPresentation.pose(
                "turnbound_re:spider", "p1",
                cue("p1", "turnbound_re:spider_binding_web", List.of("e1"),
                        BattleActionTimelineState.ImpactStyle.MELEE,
                        BattleActionTimelineState.Phase.WINDUP, 0.4D,
                        BattleActionTimelineState.PresentationStyle.STANDARD));
        BattleStageCharacterPresentation.Pose unknown = BattleStageCharacterPresentation.pose(
                "turnbound_re:spider", "p1",
                cue("p1", "turnbound_re:unknown_melee", List.of("e1"),
                        BattleActionTimelineState.ImpactStyle.MELEE,
                        BattleActionTimelineState.Phase.WINDUP, 0.4D,
                        BattleActionTimelineState.PresentationStyle.STANDARD));
        BattleStageCharacterPresentation.Pose recovery = BattleStageCharacterPresentation.pose(
                "turnbound_re:spider", "p1",
                cue("p1", "turnbound_re:spider_brood_pounce", List.of("e1"),
                        BattleActionTimelineState.ImpactStyle.MELEE,
                        BattleActionTimelineState.Phase.RECOVERY, 0.2D,
                        BattleActionTimelineState.PresentationStyle.HEAVY));
        BattleStageCharacterPresentation.Pose otherActor = BattleStageCharacterPresentation.pose(
                "turnbound_re:spider", "p2",
                cue("p1", "turnbound_re:spider_venom_bite", List.of("e1"),
                        BattleActionTimelineState.ImpactStyle.MELEE,
                        BattleActionTimelineState.Phase.WINDUP, 0.4D,
                        BattleActionTimelineState.PresentationStyle.STANDARD));

        for (BattleStageCharacterPresentation.Pose pose : List.of(malformedWeb, unknown, recovery, otherActor)) {
            assertTrue(pose.controlsAggressive());
            assertFalse(pose.aggressive());
            assertEquals(TurnboundPresentationPose.NEUTRAL, pose.modelPose());
            assertEquals(0, pose.offsetY());
        }
    }

    private static BattleActionTimelineState.Cue cue(
            String actorId,
            String actionId,
            List<String> targets,
            BattleActionTimelineState.ImpactStyle impactStyle,
            BattleActionTimelineState.Phase phase,
            double progress,
            BattleActionTimelineState.PresentationStyle style
    ) {
        return new BattleActionTimelineState.Cue(
                actorId,
                actionId,
                targets,
                impactStyle == BattleActionTimelineState.ImpactStyle.PROJECTILE
                        ? BattleActionTimelineState.MotionStyle.RANGED
                        : BattleActionTimelineState.MotionStyle.CLOSE,
                impactStyle,
                style,
                phase,
                progress,
                0,
                1);
    }
}
