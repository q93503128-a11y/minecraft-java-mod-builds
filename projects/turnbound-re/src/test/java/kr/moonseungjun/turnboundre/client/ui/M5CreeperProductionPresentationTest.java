package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;
import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationPose;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class M5CreeperProductionPresentationTest {
    @Test
    void fuseBashReadsAsBodyAttackWithoutFusePose() {
        BattleStageCharacterPresentation.Pose pose = BattleStageCharacterPresentation.pose(
                "turnbound_re:creeper", "p1",
                cue("p1", "turnbound_re:creeper_fuse_bash", List.of("e1"),
                        BattleActionTimelineState.ImpactStyle.MELEE,
                        BattleActionTimelineState.Phase.WINDUP, 0.45D,
                        BattleActionTimelineState.PresentationStyle.STANDARD));

        assertTrue(pose.controlsAggressive());
        assertTrue(pose.aggressive());
        assertEquals(TurnboundPresentationPose.OFFENSIVE, pose.modelPose());
    }

    @Test
    void volatileChargeIsSelfPrimingNotExplosionImpact() {
        BattleActionTimelineState.Cue cue = cue(
                "p1", "turnbound_re:creeper_volatile_charge", List.of("p1"),
                BattleActionTimelineState.ImpactStyle.BLAST,
                BattleActionTimelineState.Phase.WINDUP, 0.35D,
                BattleActionTimelineState.PresentationStyle.STANDARD);
        BattleStageCharacterPresentation.Pose pose = BattleStageCharacterPresentation.pose(
                "turnbound_re:creeper", "p1", cue);

        assertTrue(BattleStageCharacterPresentation.isCreeperVolatileChargeCue(cue));
        assertTrue(pose.controlsAggressive());
        assertFalse(pose.aggressive());
        assertEquals(TurnboundPresentationPose.CHARGE, pose.modelPose());
    }

    @Test
    void blastWaveAndCatastropheUseDistinctExplosionSilhouettes() {
        BattleStageCharacterPresentation.Pose wave = BattleStageCharacterPresentation.pose(
                "turnbound_re:creeper", "p1",
                cue("p1", "turnbound_re:creeper_blast_wave", List.of("e1", "e2", "e3"),
                        BattleActionTimelineState.ImpactStyle.BLAST,
                        BattleActionTimelineState.Phase.WINDUP, 0.55D,
                        BattleActionTimelineState.PresentationStyle.AREA));
        BattleStageCharacterPresentation.Pose burst = BattleStageCharacterPresentation.pose(
                "turnbound_re:creeper", "p1",
                cue("p1", "turnbound_re:creeper_catastrophe", List.of("e1", "e2", "e3"),
                        BattleActionTimelineState.ImpactStyle.BLAST,
                        BattleActionTimelineState.Phase.WINDUP, 0.55D,
                        BattleActionTimelineState.PresentationStyle.AREA));

        assertEquals(TurnboundPresentationPose.BLAST, wave.modelPose());
        assertEquals(TurnboundPresentationPose.CATASTROPHE, burst.modelPose());
        assertTrue(wave.aggressive());
        assertTrue(burst.aggressive());
        assertTrue(burst.offsetY() < wave.offsetY());
        assertTrue(burst.yAngle() > wave.yAngle());
    }

    @Test
    void malformedChargeUnknownBlastRecoveryAndOtherActorFailClosed() {
        BattleStageCharacterPresentation.Pose wrongTarget = BattleStageCharacterPresentation.pose(
                "turnbound_re:creeper", "p1",
                cue("p1", "turnbound_re:creeper_volatile_charge", List.of("e1"),
                        BattleActionTimelineState.ImpactStyle.BLAST,
                        BattleActionTimelineState.Phase.WINDUP, 0.4D,
                        BattleActionTimelineState.PresentationStyle.STANDARD));
        BattleStageCharacterPresentation.Pose unknown = BattleStageCharacterPresentation.pose(
                "turnbound_re:creeper", "p1",
                cue("p1", "turnbound_re:unknown_blast", List.of("e1"),
                        BattleActionTimelineState.ImpactStyle.BLAST,
                        BattleActionTimelineState.Phase.WINDUP, 0.4D,
                        BattleActionTimelineState.PresentationStyle.AREA));
        BattleStageCharacterPresentation.Pose recovery = BattleStageCharacterPresentation.pose(
                "turnbound_re:creeper", "p1",
                cue("p1", "turnbound_re:creeper_catastrophe", List.of("e1", "e2", "e3"),
                        BattleActionTimelineState.ImpactStyle.BLAST,
                        BattleActionTimelineState.Phase.RECOVERY, 0.2D,
                        BattleActionTimelineState.PresentationStyle.AREA));
        BattleStageCharacterPresentation.Pose otherActor = BattleStageCharacterPresentation.pose(
                "turnbound_re:creeper", "p2",
                cue("p1", "turnbound_re:creeper_blast_wave", List.of("e1", "e2", "e3"),
                        BattleActionTimelineState.ImpactStyle.BLAST,
                        BattleActionTimelineState.Phase.WINDUP, 0.4D,
                        BattleActionTimelineState.PresentationStyle.AREA));

        for (BattleStageCharacterPresentation.Pose pose : List.of(wrongTarget, unknown, recovery, otherActor)) {
            assertTrue(pose.controlsAggressive());
            assertFalse(pose.aggressive());
            assertEquals(TurnboundPresentationPose.NEUTRAL, pose.modelPose());
            assertEquals(0, pose.offsetY());
            assertEquals(BattleStageCharacterPresentation.DEFAULT_X_ANGLE, pose.xAngle());
            assertEquals(BattleStageCharacterPresentation.DEFAULT_Y_ANGLE, pose.yAngle());
        }
        assertFalse(BattleStageCharacterPresentation.isCreeperVolatileChargeCue(
                cue("p1", "turnbound_re:creeper_volatile_charge", List.of("e1"),
                        BattleActionTimelineState.ImpactStyle.BLAST,
                        BattleActionTimelineState.Phase.WINDUP, 0.4D,
                        BattleActionTimelineState.PresentationStyle.STANDARD)));
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
                impactStyle == BattleActionTimelineState.ImpactStyle.MELEE
                        ? BattleActionTimelineState.MotionStyle.CLOSE
                        : BattleActionTimelineState.MotionStyle.CAST,
                impactStyle,
                style,
                phase,
                progress,
                0,
                1);
    }
}
