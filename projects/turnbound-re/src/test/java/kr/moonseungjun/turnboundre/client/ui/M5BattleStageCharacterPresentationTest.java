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
                cue("p1", List.of("e1"), BattleActionTimelineState.ImpactStyle.PROJECTILE,
                        BattleActionTimelineState.Phase.WINDUP, 0.6D, 0));
        assertTrue(aiming.controlsAggressive());
        assertTrue(aiming.aggressive());
        assertTrue(aiming.yAngle() > BattleStageCharacterPresentation.DEFAULT_Y_ANGLE);

        BattleStageCharacterPresentation.Pose recovering = BattleStageCharacterPresentation.pose(
                "turnbound_re:skeleton", "p1",
                cue("p1", List.of("e1"), BattleActionTimelineState.ImpactStyle.PROJECTILE,
                        BattleActionTimelineState.Phase.RECOVERY, 0.2D, 0));
        assertTrue(recovering.controlsAggressive());
        assertFalse(recovering.aggressive());
        assertEquals(BattleStageCharacterPresentation.DEFAULT_Y_ANGLE, recovering.yAngle());
    }

    @Test
    void skeletonDoesNotAimWhenAnotherParticipantActs() {
        BattleStageCharacterPresentation.Pose pose = BattleStageCharacterPresentation.pose(
                "turnbound_re:skeleton", "p2",
                cue("p1", List.of("e1"), BattleActionTimelineState.ImpactStyle.PROJECTILE,
                        BattleActionTimelineState.Phase.WINDUP, 0.5D, 0));
        assertFalse(pose.aggressive());
    }

    @Test
    void endermanVoidBeatMovesTheActualModelButReturnsToNeutral() {
        BattleStageCharacterPresentation.Pose windup = BattleStageCharacterPresentation.pose(
                "turnbound_re:enderman", "p1",
                cue("p1", List.of("e1"), BattleActionTimelineState.ImpactStyle.VOID,
                        BattleActionTimelineState.Phase.WINDUP, 0.5D, 0));
        assertTrue(Math.abs(windup.offsetX()) > 0);
        assertTrue(Math.abs(windup.offsetX()) <= 3);
        assertTrue(Math.abs(windup.yAngle() - BattleStageCharacterPresentation.DEFAULT_Y_ANGLE) <= 0.18F + 0.0001F);

        BattleStageCharacterPresentation.Pose done = BattleStageCharacterPresentation.pose(
                "turnbound_re:enderman", "p1",
                cue("p1", List.of("e1"), BattleActionTimelineState.ImpactStyle.VOID,
                        BattleActionTimelineState.Phase.RECOVERY, 1.0D, 0));
        assertEquals(0, done.offsetX());
        assertEquals(BattleStageCharacterPresentation.DEFAULT_X_ANGLE, done.xAngle());
        assertEquals(BattleStageCharacterPresentation.DEFAULT_Y_ANGLE, done.yAngle());
    }

    @Test
    void zombieUsesDedicatedMeleeStanceOnlyForAnActualEnemyTarget() {
        BattleStageCharacterPresentation.Pose windup = BattleStageCharacterPresentation.pose(
                "turnbound_re:zombie", "p1",
                cue("p1", List.of("e1"), BattleActionTimelineState.ImpactStyle.MELEE,
                        BattleActionTimelineState.Phase.WINDUP, 0.5D, 0));
        assertTrue(windup.controlsAggressive());
        assertTrue(windup.aggressive());
        assertTrue(windup.offsetY() < 0);
        assertTrue(windup.yAngle() > BattleStageCharacterPresentation.DEFAULT_Y_ANGLE);

        BattleStageCharacterPresentation.Pose selfBuff = BattleStageCharacterPresentation.pose(
                "turnbound_re:zombie", "p1",
                cue("p1", List.of("p1"), BattleActionTimelineState.ImpactStyle.MELEE,
                        BattleActionTimelineState.Phase.WINDUP, 0.5D, 0));
        assertTrue(selfBuff.controlsAggressive());
        assertFalse(selfBuff.aggressive());
        assertEquals(0, selfBuff.offsetY());
        assertEquals(BattleStageCharacterPresentation.DEFAULT_X_ANGLE, selfBuff.xAngle());
        assertEquals(BattleStageCharacterPresentation.DEFAULT_Y_ANGLE, selfBuff.yAngle());

        BattleStageCharacterPresentation.Pose recovering = BattleStageCharacterPresentation.pose(
                "turnbound_re:zombie", "p1",
                cue("p1", List.of("e1"), BattleActionTimelineState.ImpactStyle.MELEE,
                        BattleActionTimelineState.Phase.RECOVERY, 0.4D, 0));
        assertTrue(recovering.controlsAggressive());
        assertFalse(recovering.aggressive());
        assertEquals(0, recovering.offsetY());
        assertEquals(BattleStageCharacterPresentation.DEFAULT_X_ANGLE, recovering.xAngle());
        assertEquals(BattleStageCharacterPresentation.DEFAULT_Y_ANGLE, recovering.yAngle());
    }

    @Test
    void blazeUsesFurnaceAttackOnlyForCanonicalEnemyFireActions() {
        BattleStageCharacterPresentation.Pose volley = BattleStageCharacterPresentation.pose(
                "turnbound_re:blaze", "p1",
                cue("p1", "turnbound_re:blaze_searing_volley", List.of("e1", "e2"),
                        BattleActionTimelineState.ImpactStyle.FIRE,
                        BattleActionTimelineState.Phase.WINDUP, 0.55D, 0));
        assertTrue(volley.controlsAggressive());
        assertTrue(volley.aggressive());
        assertTrue(volley.offsetY() < 0);
        assertTrue(volley.yAngle() > BattleStageCharacterPresentation.DEFAULT_Y_ANGLE);

        BattleStageCharacterPresentation.Pose heatUp = BattleStageCharacterPresentation.pose(
                "turnbound_re:blaze", "p1",
                cue("p1", "turnbound_re:blaze_heat_up", List.of("p1"),
                        BattleActionTimelineState.ImpactStyle.FIRE,
                        BattleActionTimelineState.Phase.WINDUP, 0.55D, 0));
        assertTrue(heatUp.controlsAggressive());
        assertFalse(heatUp.aggressive());
        assertTrue(heatUp.offsetY() < 0);
        assertTrue(heatUp.xAngle() < BattleStageCharacterPresentation.DEFAULT_X_ANGLE);
        assertEquals(BattleStageCharacterPresentation.DEFAULT_Y_ANGLE, heatUp.yAngle());

        BattleStageCharacterPresentation.Pose unrelatedFire = BattleStageCharacterPresentation.pose(
                "turnbound_re:blaze", "p1",
                cue("p1", "turnbound_re:unknown_fire_action", List.of("e1"),
                        BattleActionTimelineState.ImpactStyle.FIRE,
                        BattleActionTimelineState.Phase.WINDUP, 0.55D, 0));
        assertTrue(unrelatedFire.controlsAggressive());
        assertFalse(unrelatedFire.aggressive());
        assertEquals(0, unrelatedFire.offsetY());

        BattleStageCharacterPresentation.Pose recovering = BattleStageCharacterPresentation.pose(
                "turnbound_re:blaze", "p1",
                cue("p1", "turnbound_re:blaze_inferno_burst", List.of("e1", "e2", "e3"),
                        BattleActionTimelineState.ImpactStyle.FIRE,
                        BattleActionTimelineState.Phase.RECOVERY, 0.3D, 0));
        assertTrue(recovering.controlsAggressive());
        assertFalse(recovering.aggressive());
        assertEquals(0, recovering.offsetY());
        assertEquals(BattleStageCharacterPresentation.DEFAULT_X_ANGLE, recovering.xAngle());
        assertEquals(BattleStageCharacterPresentation.DEFAULT_Y_ANGLE, recovering.yAngle());
    }

    @Test
    void charactersWithoutDedicatedPresentationKeepExistingStageView() {
        assertEquals(BattleStageCharacterPresentation.Pose.DEFAULT,
                BattleStageCharacterPresentation.pose(
                        "turnbound_re:cow", "p1",
                        cue("p1", List.of("e1"), BattleActionTimelineState.ImpactStyle.MELEE,
                                BattleActionTimelineState.Phase.WINDUP, 0.5D, 0)));
    }

    private static BattleActionTimelineState.Cue cue(
            String actorId,
            List<String> targetIds,
            BattleActionTimelineState.ImpactStyle impactStyle,
            BattleActionTimelineState.Phase phase,
            double progress,
            int beatIndex
    ) {
        return cue(actorId, "action", targetIds, impactStyle, phase, progress, beatIndex);
    }

    private static BattleActionTimelineState.Cue cue(
            String actorId,
            String actionId,
            List<String> targetIds,
            BattleActionTimelineState.ImpactStyle impactStyle,
            BattleActionTimelineState.Phase phase,
            double progress,
            int beatIndex
    ) {
        BattleActionTimelineState.PresentationStyle style = impactStyle == BattleActionTimelineState.ImpactStyle.VOID
                ? BattleActionTimelineState.PresentationStyle.RIFT
                : BattleActionTimelineState.PresentationStyle.VOLLEY;
        return new BattleActionTimelineState.Cue(
                actorId,
                actionId,
                targetIds,
                BattleActionTimelineState.MotionStyle.RANGED,
                impactStyle,
                style,
                phase,
                progress,
                beatIndex,
                1);
    }
}
