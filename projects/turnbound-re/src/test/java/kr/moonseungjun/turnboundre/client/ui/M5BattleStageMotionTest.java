package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M5BattleStageMotionTest {
    @Test
    void closeActionsWindUpAwayThenLungeTowardTheOpposingBand() {
        BattleActionTimelineState.Cue playerWindup = cue(
                BattleActionTimelineState.MotionStyle.CLOSE,
                BattleActionTimelineState.Phase.WINDUP,
                1.0D);
        BattleActionTimelineState.Cue playerImpact = cue(
                BattleActionTimelineState.MotionStyle.CLOSE,
                BattleActionTimelineState.Phase.IMPACT,
                0.0D);

        assertTrue(BattleStageMotion.actorOffset(playerWindup, false).y() > 0);
        assertTrue(BattleStageMotion.actorOffset(playerImpact, false).y() < 0);
        assertTrue(BattleStageMotion.actorOffset(playerImpact, true).y() > 0);
    }

    @Test
    void rangedActionsRecoilInsteadOfCrossingIntoTheTargetBand() {
        BattleActionTimelineState.Cue rangedImpact = cue(
                BattleActionTimelineState.MotionStyle.RANGED,
                BattleActionTimelineState.Phase.IMPACT,
                0.0D);
        BattleActionTimelineState.Cue closeImpact = cue(
                BattleActionTimelineState.MotionStyle.CLOSE,
                BattleActionTimelineState.Phase.IMPACT,
                0.0D);

        int rangedPlayerY = BattleStageMotion.actorOffset(rangedImpact, false).y();
        int closePlayerY = BattleStageMotion.actorOffset(closeImpact, false).y();
        assertTrue(rangedPlayerY > 0);
        assertTrue(closePlayerY < 0);
        assertTrue(Math.abs(rangedPlayerY) < Math.abs(closePlayerY));
    }

    @Test
    void utilityActionsStayStationaryAndRecoveryReturnsTowardZero() {
        BattleActionTimelineState.Cue utility = cue(
                BattleActionTimelineState.MotionStyle.UTILITY,
                BattleActionTimelineState.Phase.IMPACT,
                0.5D);
        assertEquals(BattleStageMotion.Offset.ZERO, BattleStageMotion.actorOffset(utility, false));

        BattleActionTimelineState.Cue earlyRecovery = cue(
                BattleActionTimelineState.MotionStyle.CLOSE,
                BattleActionTimelineState.Phase.RECOVERY,
                0.0D);
        BattleActionTimelineState.Cue lateRecovery = cue(
                BattleActionTimelineState.MotionStyle.CLOSE,
                BattleActionTimelineState.Phase.RECOVERY,
                0.99D);
        assertTrue(Math.abs(BattleStageMotion.actorOffset(earlyRecovery, false).y())
                > Math.abs(BattleStageMotion.actorOffset(lateRecovery, false).y()));
    }

    private static BattleActionTimelineState.Cue cue(
            BattleActionTimelineState.MotionStyle style,
            BattleActionTimelineState.Phase phase,
            double progress
    ) {
        return new BattleActionTimelineState.Cue(
                "p1", "action", List.of("e1"), style, phase, progress, 0, 1);
    }
}
