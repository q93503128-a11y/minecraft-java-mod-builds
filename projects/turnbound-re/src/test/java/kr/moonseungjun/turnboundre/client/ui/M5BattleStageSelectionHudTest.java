package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleTargetMarkerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M5BattleStageSelectionHudTest {
    @Test
    void targetStatesReuseExistingSemanticFrameHierarchy() {
        assertEquals(UiVisualLanguage.FrameState.WARNING,
                BattleStageSelectionHud.frameState(BattleTargetMarkerState.MarkerKind.ELIGIBLE));
        assertEquals(UiVisualLanguage.FrameState.FOCUS,
                BattleStageSelectionHud.frameState(BattleTargetMarkerState.MarkerKind.HOVERED));
        assertEquals(UiVisualLanguage.FrameState.SUCCESS,
                BattleStageSelectionHud.frameState(BattleTargetMarkerState.MarkerKind.SELECTED));
    }

    @Test
    void strongerSelectionStatesExpandWithinTheRenderedModelRegion() {
        UiLayoutMetrics.Rect viewport = UiLayoutMetrics.battleHud(640, 360).reservedWorldViewport();
        BattleStageLayout.Layout layout = BattleStageLayout.arrange(viewport, 4, 3);
        UiLayoutMetrics.Rect enemySlot = layout.enemies().getFirst().bounds();

        UiLayoutMetrics.Rect eligible = BattleStageSelectionHud.selectionBounds(
                enemySlot, true, 9, BattleTargetMarkerState.MarkerKind.ELIGIBLE).orElseThrow();
        UiLayoutMetrics.Rect hovered = BattleStageSelectionHud.selectionBounds(
                enemySlot, true, 9, BattleTargetMarkerState.MarkerKind.HOVERED).orElseThrow();
        UiLayoutMetrics.Rect selected = BattleStageSelectionHud.selectionBounds(
                enemySlot, true, 9, BattleTargetMarkerState.MarkerKind.SELECTED).orElseThrow();

        assertTrue(selected.width() > hovered.width());
        assertTrue(hovered.width() > eligible.width());
        assertTrue(selected.height() > hovered.height());
        assertTrue(hovered.height() > eligible.height());
        assertInside(enemySlot, eligible);
        assertInside(enemySlot, hovered);
        assertInside(enemySlot, selected);
    }

    @Test
    void playerSelectionFrameLeavesNameAndStateRowsUntouched() {
        UiLayoutMetrics.Rect viewport = UiLayoutMetrics.battleHud(480, 270).reservedWorldViewport();
        BattleStageLayout.Layout layout = BattleStageLayout.arrange(viewport, 4, 2);
        UiLayoutMetrics.Rect playerSlot = layout.players().getFirst().bounds();
        UiLayoutMetrics.Rect selected = BattleStageSelectionHud.selectionBounds(
                playerSlot, false, 9, BattleTargetMarkerState.MarkerKind.SELECTED).orElseThrow();

        assertTrue(selected.y() >= playerSlot.y() + 11);
        assertTrue(selected.bottom() <= playerSlot.bottom() - 9);
    }

    private static void assertInside(UiLayoutMetrics.Rect parent, UiLayoutMetrics.Rect child) {
        assertTrue(child.x() >= parent.x());
        assertTrue(child.y() >= parent.y());
        assertTrue(child.right() <= parent.right());
        assertTrue(child.bottom() <= parent.bottom());
    }
}
