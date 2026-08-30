package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleHudLayoutTest {
    @Test
    void allHudRectanglesStayPositiveAndInsideViewport() {
        int[][] sizes = {
                {256, 160}, {320, 180}, {420, 225}, {480, 270},
                {640, 360}, {854, 480}, {960, 540}, {1920, 1080}
        };
        for (int[] size : sizes) {
            BattleHudLayout.Layout layout = BattleHudLayout.calculate(size[0], size[1]);
            assertEquals(BattleHudLayout.ALLY_COUNT, layout.allyBars().size());
            assertEquals(BattleHudLayout.ENEMY_COUNT, layout.enemyBars().size());
            assertEquals(BattleHudLayout.SKILL_COUNT, layout.skillButtons().size());

            List<BattleHudLayout.Rect> all = new ArrayList<>();
            all.addAll(layout.allyBars());
            all.addAll(layout.enemyBars());
            all.addAll(layout.skillButtons());
            all.add(layout.actionHeader());
            all.add(layout.confirmButton());
            all.add(layout.tooltipArea());
            all.add(layout.timeline());
            all.add(layout.autoButton());
            all.add(layout.speedButton());
            all.add(layout.fleeButton());
            all.add(layout.settingsPanel());

            for (BattleHudLayout.Rect rect : all) {
                assertTrue(rect.width() > 0 && rect.height() > 0, size[0] + "x" + size[1]);
                assertTrue(rect.x() >= 0 && rect.y() >= 0, size[0] + "x" + size[1]);
                assertTrue(rect.right() <= size[0] && rect.bottom() <= size[1], size[0] + "x" + size[1]);
            }
        }
    }

    @Test
    void standardViewportUsesCompactTwoColumnActionDockAndKeepsCenterOpen() {
        BattleHudLayout.Layout layout = BattleHudLayout.calculate(854, 480);
        int centerLeft = 854 / 4;
        int centerRight = 854 * 3 / 4;

        for (BattleHudLayout.Rect rect : layout.enemyBars()) {
            assertTrue(rect.x() >= 854 / 2, "enemy summaries stay in the upper-right half");
            assertTrue(rect.bottom() < 110, "enemy summaries remain shallow");
        }
        for (BattleHudLayout.Rect rect : layout.skillButtons()) {
            assertTrue(rect.x() >= centerRight, "contextual skills stay on the right edge");
            assertTrue(rect.height() >= 44, "standard skill buttons keep the planned click height");
            assertFalse(rect.overlaps(layout.autoButton()));
            assertFalse(rect.overlaps(layout.speedButton()));
            assertFalse(rect.overlaps(layout.fleeButton()));
        }
        assertEquals(layout.skillButtons().get(0).y(), layout.skillButtons().get(1).y());
        assertTrue(layout.skillButtons().get(2).y() > layout.skillButtons().get(0).y(), "skills form multiple rows");

        assertFalse(layout.confirmButton().overlaps(layout.autoButton()));
        assertFalse(layout.confirmButton().overlaps(layout.speedButton()));
        assertFalse(layout.confirmButton().overlaps(layout.fleeButton()));
        assertTrue(layout.tooltipArea().right() <= layout.actionHeader().x(), "hover tooltip stays left of the action dock");

        for (BattleHudLayout.Rect ally : layout.allyBars()) {
            assertTrue(ally.y() >= 450, "party bars stay on the bottom edge");
            assertFalse(ally.overlaps(layout.autoButton()));
            assertFalse(ally.overlaps(layout.speedButton()));
            assertFalse(ally.overlaps(layout.fleeButton()));
        }
        assertTrue(layout.timeline().x() > centerLeft && layout.timeline().right() < centerRight + 60);
    }
}
