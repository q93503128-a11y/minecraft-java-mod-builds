package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BattleHudLayoutTest {
    @Test
    void allHudRectanglesStayPositiveAndInsideViewport() {
        int[][] sizes = {{256,160},{320,180},{420,225},{480,270},{640,360},{854,480},{960,540},{1920,1080}};
        for (int[] size : sizes) {
            BattleHudLayout.Layout layout = BattleHudLayout.calculate(size[0], size[1]);
            assertEquals(4, layout.allyBars().size());
            assertEquals(5, layout.enemyBars().size());
            assertEquals(5, layout.skillButtons().size());
            List<BattleHudLayout.Rect> all = new ArrayList<>();
            all.addAll(layout.allyBars()); all.addAll(layout.enemyBars()); all.addAll(layout.skillButtons());
            all.add(layout.actionHeader()); all.add(layout.confirmButton()); all.add(layout.tooltipArea()); all.add(layout.timeline());
            all.add(layout.autoButton()); all.add(layout.speedButton()); all.add(layout.fleeButton()); all.add(layout.settingsPanel());
            for (var r : all) {
                assertTrue(r.width() > 0 && r.height() > 0, size[0] + "x" + size[1]);
                assertTrue(r.x() >= 0 && r.y() >= 0 && r.right() <= size[0] && r.bottom() <= size[1], size[0] + "x" + size[1]);
            }
        }
    }

    @Test
    void standardViewportKeepsBattlefieldOpenAndControlsSeparated() {
        BattleHudLayout.Layout layout = BattleHudLayout.calculate(854, 480);
        for (var skill : layout.skillButtons()) {
            assertTrue(skill.x() >= 854 / 2);
            assertFalse(skill.overlaps(layout.autoButton()));
            assertFalse(skill.overlaps(layout.speedButton()));
            assertFalse(skill.overlaps(layout.fleeButton()));
            for (var ally : layout.allyBars()) assertFalse(skill.overlaps(ally));
        }
        for (var ally : layout.allyBars()) {
            assertTrue(ally.y() >= 440);
            assertFalse(ally.overlaps(layout.autoButton()));
            assertFalse(ally.overlaps(layout.speedButton()));
            assertFalse(ally.overlaps(layout.fleeButton()));
        }
        assertTrue(layout.timeline().right() < 854 * 3 / 4);
        assertTrue(layout.timeline().x() > 854 / 4);
        assertTrue(layout.tooltipArea().right() <= layout.actionHeader().x());
        // Enemy state is world-space in alpha.15; compatibility rectangles must not form a top-right wall.
        for (var enemy : layout.enemyBars()) assertEquals(1, enemy.width());
        assertEquals(1, layout.confirmButton().width());
    }
}
