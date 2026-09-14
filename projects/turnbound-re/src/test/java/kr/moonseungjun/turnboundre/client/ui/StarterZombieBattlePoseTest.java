package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StarterZombieBattlePoseTest {
    @Test
    void enemyTargetingMeleeWindupUsesAggressivePresentationPose() {
        BattleActionTimelineState.Cue cue = cue(
                "turnbound_re:zombie_rotten_swing",
                List.of("enemy:1"),
                BattleActionTimelineState.Phase.WINDUP);

        BattleStageCharacterPresentation.Pose pose = BattleStageCharacterPresentation.pose(
                "turnbound_re:zombie", "player:zombie", cue);

        assertTrue(pose.controlsAggressive());
        assertTrue(pose.aggressive());
    }

    @Test
    void selfTargetingUndeadGritDoesNotUseAttackPose() {
        BattleActionTimelineState.Cue cue = cue(
                "turnbound_re:zombie_undead_grit",
                List.of("player:zombie"),
                BattleActionTimelineState.Phase.WINDUP);

        BattleStageCharacterPresentation.Pose pose = BattleStageCharacterPresentation.pose(
                "turnbound_re:zombie", "player:zombie", cue);

        assertTrue(pose.controlsAggressive());
        assertFalse(pose.aggressive());
    }

    @Test
    void meleeRecoveryExplicitlyReturnsToIdleState() {
        BattleActionTimelineState.Cue cue = cue(
                "turnbound_re:zombie_gravebreaker",
                List.of("enemy:1"),
                BattleActionTimelineState.Phase.RECOVERY);

        BattleStageCharacterPresentation.Pose pose = BattleStageCharacterPresentation.pose(
                "turnbound_re:zombie", "player:zombie", cue);

        assertTrue(pose.controlsAggressive());
        assertFalse(pose.aggressive());
    }

    private static BattleActionTimelineState.Cue cue(
            String actionId,
            List<String> targetIds,
            BattleActionTimelineState.Phase phase
    ) {
        return new BattleActionTimelineState.Cue(
                "player:zombie",
                actionId,
                targetIds,
                BattleActionTimelineState.MotionStyle.CLOSE,
                BattleActionTimelineState.ImpactStyle.MELEE,
                BattleActionTimelineState.PresentationStyle.STANDARD,
                phase,
                0.4D,
                0,
                1);
    }
}
