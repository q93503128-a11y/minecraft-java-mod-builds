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
        int width = 854;
        int height = 480;
        BattleHudLayout.Layout layout = BattleHudLayout.calculate(width, height);
        int centerLeft = width / 4;
        int centerRight = width * 3 / 4;

        int minEnemyX = layout.enemyBars().stream().mapToInt(BattleHudLayout.Rect::x).min().orElseThrow();
        int maxEnemyRight = layout.enemyBars().stream().mapToInt(BattleHudLayout.Rect::right).max().orElseThrow();
        int maxEnemyBottom = layout.enemyBars().stream().mapToInt(BattleHudLayout.Rect::bottom).max().orElseThrow();
        assertTrue(minEnemyX > width / 2 + 60,
                "enemy summary must live in the right half instead of rebuilding a side wall across the battlefield");
        assertTrue(maxEnemyRight >= width - 12,
                "enemy summary must stay anchored to the upper-right edge");
        assertTrue(maxEnemyBottom < height / 5,
                "enemy summary must remain a shallow top-edge HUD");

        for (BattleHudLayout.Rect rect : layout.skillButtons()) {
            assertTrue(rect.x() >= centerRight,
                    "contextual skills should remain on the right edge");
        }
        for (BattleHudLayout.Rect rect : layout.allyBars()) {
            assertTrue(rect.y() >= height - 50,
                    "party strip should stay at the bottom edge");
            assertTrue(rect.right() < centerRight + 20,
                    "party strip must not become a full-screen wall");
        }
        assertTrue(layout.timeline().x() > centerLeft
                        && layout.timeline().right() < centerRight + 40,
                "timeline should stay a narrow top-center strip");
    }
}
