package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M5BattleActionTimelineHudTest {
    @Test
    void impactUsesTheStrongestExistingFocusFrameWithoutInventingNewVisualTokens() {
        assertEquals(UiVisualLanguage.FrameState.FOCUS,
                BattleActionTimelineHud.frameState(BattleActionTimelineState.Phase.WINDUP));
        assertEquals(UiVisualLanguage.FrameState.FOCUS,
                BattleActionTimelineHud.frameState(BattleActionTimelineState.Phase.IMPACT));
        assertEquals(UiVisualLanguage.FrameState.IDLE,
                BattleActionTimelineHud.frameState(BattleActionTimelineState.Phase.RECOVERY));
    }

    @Test
    void impactFrameExpandsInsideTheSameRenderedModelRegion() {
        UiLayoutMetrics.Rect viewport = UiLayoutMetrics.battleHud(640, 360).reservedWorldViewport();
        UiLayoutMetrics.Rect slot = BattleStageLayout.arrange(viewport, 4, 3).enemies().getFirst().bounds();

        UiLayoutMetrics.Rect windup = BattleActionTimelineHud.actionBounds(
                slot, true, 9, BattleActionTimelineState.Phase.WINDUP).orElseThrow();
        UiLayoutMetrics.Rect impact = BattleActionTimelineHud.actionBounds(
                slot, true, 9, BattleActionTimelineState.Phase.IMPACT).orElseThrow();
        UiLayoutMetrics.Rect recovery = BattleActionTimelineHud.actionBounds(
                slot, true, 9, BattleActionTimelineState.Phase.RECOVERY).orElseThrow();

        assertTrue(impact.width() > recovery.width());
        assertTrue(recovery.width() > windup.width());
        assertTrue(impact.height() > recovery.height());
        assertTrue(recovery.height() > windup.height());
        assertInside(slot, windup);
        assertInside(slot, impact);
        assertInside(slot, recovery);
    }

    @Test
    void hudFocusStaysOnTheActionActorUntilTheTimelineBeatFinishes() {
        BattleActionTimelineState.Cue action = new BattleActionTimelineState.Cue(
                "e1", "bite", List.of("p1"), BattleActionTimelineState.Phase.IMPACT, 0.4D, 1, 2);

        assertEquals("e1", BattleHud.presentationActorId("p2", action));
        assertEquals("p2", BattleHud.presentationActorId("p2", null));
        assertEquals("", BattleHud.presentationActorId(null, null));
    }

    private static void assertInside(UiLayoutMetrics.Rect parent, UiLayoutMetrics.Rect child) {
        assertTrue(child.x() >= parent.x());
        assertTrue(child.y() >= parent.y());
        assertTrue(child.right() <= parent.right());
        assertTrue(child.bottom() <= parent.bottom());
    }
}
