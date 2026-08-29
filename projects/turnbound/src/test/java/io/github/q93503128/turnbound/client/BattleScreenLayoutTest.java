package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleScreenLayoutTest {
    @Test
    void everyHudRectangleStaysInsideCommonGuiViewports() {
        int[][] viewports = {
                {256, 160},
                {320, 180},
                {420, 225},
                {480, 270},
                {640, 360},
                {840, 450},
                {960, 540},
                {1920, 1080}
        };

        for (int[] viewport : viewports) {
            BattleScreenLayout.Layout layout = BattleScreenLayout.calculate(viewport[0], viewport[1]);
            assertEquals(BattleScreenLayout.SKILL_COUNT, layout.skillButtons().size());
            assertEquals(BattleScreenLayout.ALLY_COUNT, layout.allyHud().size());
            assertEquals(BattleScreenLayout.ENEMY_COUNT, layout.enemyHud().size());

            List<BattleScreenLayout.Rect> rectangles = new ArrayList<>();
            rectangles.addAll(layout.skillButtons());
            rectangles.addAll(layout.allyHud());
            rectangles.addAll(layout.enemyHud());
            rectangles.add(layout.autoButton());
            rectangles.add(layout.speedButton());
            rectangles.add(layout.fleeButton());
            rectangles.add(layout.actionPanel());
            rectangles.add(layout.timelinePanel());
            rectangles.add(layout.settingsPanel());

            for (BattleScreenLayout.Rect rect : rectangles) {
                assertTrue(rect.width() > 0, () -> describe(viewport, rect, "width"));
                assertTrue(rect.height() > 0, () -> describe(viewport, rect, "height"));
                assertTrue(rect.x() >= 0, () -> describe(viewport, rect, "x"));
                assertTrue(rect.y() >= 0, () -> describe(viewport, rect, "y"));
                assertTrue(rect.right() <= viewport[0], () -> describe(viewport, rect, "right"));
                assertTrue(rect.bottom() <= viewport[1], () -> describe(viewport, rect, "bottom"));
            }
        }
    }

    @Test
    void standardGameplayViewportLeavesCenterMostlyUncovered() {
        BattleScreenLayout.Layout layout = BattleScreenLayout.calculate(840, 450);

        assertTrue(layout.actionPanel().width() <= 190);
        assertTrue(layout.timelinePanel().height() <= 28);
        for (BattleScreenLayout.Rect ally : layout.allyHud()) {
            assertTrue(ally.y() >= 400, () -> "ally HUD should hug bottom edge: " + ally);
        }
        for (BattleScreenLayout.Rect enemy : layout.enemyHud()) {
            assertTrue(enemy.y() < 70, () -> "enemy HUD should hug top edge: " + enemy);
        }
    }

    @Test
    void highGuiScaleUsesCompactPartyAndNeverCreatesZeroSizedScissors() {
        BattleScreenLayout.Layout layout = BattleScreenLayout.calculate(420, 225);
        assertTrue(layout.compact());
        for (BattleScreenLayout.Rect rect : layout.allyHud()) assertTrue(rect.height() > 0);
        for (BattleScreenLayout.Rect rect : layout.enemyHud()) assertTrue(rect.height() > 0);
        for (BattleScreenLayout.Rect rect : layout.skillButtons()) assertTrue(rect.height() > 0);
    }

    private static String describe(int[] viewport, BattleScreenLayout.Rect rect, String problem) {
        return viewport[0] + "x" + viewport[1] + " " + problem + " " + rect;
    }
}
