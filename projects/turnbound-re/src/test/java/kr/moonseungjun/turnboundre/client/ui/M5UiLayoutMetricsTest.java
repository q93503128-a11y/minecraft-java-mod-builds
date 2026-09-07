package kr.moonseungjun.turnboundre.client.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class M5UiLayoutMetricsTest {
    @Test
    void battleHudRegionsStayInsideAndProtectTheWorldViewportAtTargetSizes() {
        for (int[] size : List.of(new int[]{1280, 720}, new int[]{1920, 1080}, new int[]{640, 360})) {
            assertTrue(UiLayoutMetrics.supportsBattleHud(size[0], size[1]));
            UiLayoutMetrics.BattleHudLayout layout = UiLayoutMetrics.battleHud(size[0], size[1]);
            List<UiLayoutMetrics.Rect> regions = List.of(
                    layout.turnRail(), layout.enemySummary(), layout.partyStatus(),
                    layout.commandStrip(), layout.reservedWorldViewport());
            assertTrue(regions.stream().allMatch(rect -> rect.inside(size[0], size[1])),
                    () -> "out of bounds at " + size[0] + "x" + size[1]);

            assertFalse(layout.partyStatus().intersects(layout.commandStrip()));
            assertFalse(layout.reservedWorldViewport().intersects(layout.turnRail()));
            assertFalse(layout.reservedWorldViewport().intersects(layout.enemySummary()));
            assertFalse(layout.reservedWorldViewport().intersects(layout.partyStatus()));
            assertFalse(layout.reservedWorldViewport().intersects(layout.commandStrip()));
            assertTrue(layout.reservedWorldViewport().width() >= 300);
            assertTrue(layout.reservedWorldViewport().height() >= 120);
        }
    }

    @Test
    void unsupportedTinyLogicalCanvasIsDetectedBeforeRenderAndStillFailsExplicitLayout() {
        assertFalse(UiLayoutMetrics.supportsBattleHud(320, 180));
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> UiLayoutMetrics.battleHud(320, 180));
        assertTrue(error.getMessage().contains("requires at least"));
    }
}
