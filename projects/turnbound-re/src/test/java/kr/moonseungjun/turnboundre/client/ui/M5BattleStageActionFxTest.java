package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M5BattleStageActionFxTest {
    @Test
    void projectileTravelStartsAtActorAndArrivesAtTargetAtImpactBoundary() {
        BattleStageActionFx.Point from = new BattleStageActionFx.Point(40, 210);
        BattleStageActionFx.Point to = new BattleStageActionFx.Point(120, 70);

        assertEquals(from, BattleStageActionFx.travel(from, to, 0.0D));
        assertEquals(to, BattleStageActionFx.travel(from, to, 1.0D));

        BattleStageActionFx.Point middle = BattleStageActionFx.travel(from, to, 0.5D);
        assertTrue(middle.x() > from.x() && middle.x() < to.x());
        assertTrue(middle.y() < from.y() && middle.y() > to.y());
    }

    @Test
    void onlyTravellingFamiliesCrossTheVirtualStageDuringWindup() {
        assertTrue(BattleStageActionFx.hasTravel(BattleActionTimelineState.ImpactStyle.PROJECTILE));
        assertTrue(BattleStageActionFx.hasTravel(BattleActionTimelineState.ImpactStyle.FIRE));
        assertTrue(BattleStageActionFx.hasTravel(BattleActionTimelineState.ImpactStyle.ARCANE));
        assertTrue(BattleStageActionFx.hasTravel(BattleActionTimelineState.ImpactStyle.VOID));
        assertFalse(BattleStageActionFx.hasTravel(BattleActionTimelineState.ImpactStyle.MELEE));
        assertFalse(BattleStageActionFx.hasTravel(BattleActionTimelineState.ImpactStyle.BLAST));
        assertFalse(BattleStageActionFx.hasTravel(BattleActionTimelineState.ImpactStyle.NONE));
    }

    @Test
    void launchSoundsLeadRangedAndCastWhileMeleeAndBlastLandOnImpact() {
        assertEquals(BattleActionTimelineState.Phase.WINDUP,
                BattleStageActionFx.soundTriggerPhase(BattleActionTimelineState.ImpactStyle.PROJECTILE));
        assertEquals(BattleActionTimelineState.Phase.WINDUP,
                BattleStageActionFx.soundTriggerPhase(BattleActionTimelineState.ImpactStyle.ARCANE));
        assertEquals(BattleActionTimelineState.Phase.IMPACT,
                BattleStageActionFx.soundTriggerPhase(BattleActionTimelineState.ImpactStyle.MELEE));
        assertEquals(BattleActionTimelineState.Phase.IMPACT,
                BattleStageActionFx.soundTriggerPhase(BattleActionTimelineState.ImpactStyle.BLAST));
    }

    @Test
    void impactFlashLivesInsideTheRenderedModelRegionAndExpiresBeforeRecovery() {
        UiLayoutMetrics.Rect viewport = UiLayoutMetrics.battleHud(640, 360).reservedWorldViewport();
        UiLayoutMetrics.Rect enemySlot = BattleStageLayout.arrange(viewport, 4, 3).enemies().getFirst().bounds();
        UiLayoutMetrics.Rect model = BattleStageActionFx.modelBounds(enemySlot, true, 9).orElseThrow();

        assertTrue(model.x() >= enemySlot.x());
        assertTrue(model.y() > enemySlot.y());
        assertTrue(model.right() <= enemySlot.right());
        assertTrue(model.bottom() <= enemySlot.bottom());

        BattleActionTimelineState.Cue earlyImpact = new BattleActionTimelineState.Cue(
                "p1", "shot", List.of("e1"),
                BattleActionTimelineState.MotionStyle.RANGED,
                BattleActionTimelineState.ImpactStyle.PROJECTILE,
                BattleActionTimelineState.Phase.IMPACT, 0.2D, 0, 1);
        BattleActionTimelineState.Cue lateImpact = new BattleActionTimelineState.Cue(
                "p1", "shot", List.of("e1"),
                BattleActionTimelineState.MotionStyle.RANGED,
                BattleActionTimelineState.ImpactStyle.PROJECTILE,
                BattleActionTimelineState.Phase.IMPACT, 0.9D, 0, 1);
        BattleActionTimelineState.Cue recovery = new BattleActionTimelineState.Cue(
                "p1", "shot", List.of("e1"),
                BattleActionTimelineState.MotionStyle.RANGED,
                BattleActionTimelineState.ImpactStyle.PROJECTILE,
                BattleActionTimelineState.Phase.RECOVERY, 0.1D, 0, 1);

        assertTrue(BattleStageActionFx.impactFlashVisible(earlyImpact));
        assertFalse(BattleStageActionFx.impactFlashVisible(lateImpact));
        assertFalse(BattleStageActionFx.impactFlashVisible(recovery));
        assertTrue(BattleStageActionFx.impactInset(0.7D) >= BattleStageActionFx.impactInset(0.1D));
    }
}
