package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void standardViewportKeepsCenterFreeOfLegacySideWalls() {
        BattleHudLayout.Layout layout = BattleHudLayout.calculate(854, 480);
        int centerLeft = 854 / 4;
        int centerRight = 854 * 3 / 4;

        for (BattleHudLayout.Rect rect : layout.enemyBars()) {
            assertTrue(rect.x() >= centerRight - 80, "enemy summary should remain near the upper-right edge");
        }
        for (BattleHudLayout.Rect rect : layout.skillButtons()) {
            assertTrue(rect.x() >= centerRight, "contextual skills should remain on the right edge");
        }
        for (BattleHudLayout.Rect rect : layout.allyBars()) {
            assertTrue(rect.y() >= 430, "party strip should stay at the bottom edge");
            assertTrue(rect.right() < centerRight + 20, "party strip must not become a full-screen wall");
        }
        assertTrue(layout.timeline().x() > centerLeft && layout.timeline().right() < centerRight + 40);
    }
}
