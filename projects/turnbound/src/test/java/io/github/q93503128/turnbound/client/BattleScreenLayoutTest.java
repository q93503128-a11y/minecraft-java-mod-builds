package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleScreenLayoutTest {
    @Test
    void everyInteractiveRectangleStaysInsideCommonGuiViewports() {
        int[][] viewports = {
                {256, 160},
                {320, 180},
                {420, 225},
                {480, 270},
                {640, 360},
                {854, 480},
                {960, 540},
                {1920, 1080}
        };

        for (int[] viewport : viewports) {
            BattleScreenLayout.Layout layout = BattleScreenLayout.calculate(viewport[0], viewport[1]);
            assertEquals(BattleScreenLayout.SKILL_COUNT, layout.skillButtons().size());
            assertEquals(
                    BattleScreenLayout.ALLY_TARGET_COUNT + BattleScreenLayout.ENEMY_TARGET_COUNT,
                    layout.targetButtons().size()
            );

            List<BattleScreenLayout.Rect> rectangles = new ArrayList<>();
            rectangles.addAll(layout.skillButtons());
            rectangles.addAll(layout.targetButtons());
            rectangles.add(layout.autoButton());
            rectangles.add(layout.speedButton());
            rectangles.add(layout.fleeButton());
            rectangles.add(layout.leftPanel());
            rectangles.add(layout.rightPanel());
            rectangles.add(layout.topInset());
            rectangles.add(layout.bottomInset());

            for (BattleScreenLayout.Rect rect : rectangles) {
                assertTrue(rect.width() > 0, () -> describe(viewport, rect, "width"));
                assertTrue(rect.height() > 0, () -> describe(viewport, rect, "height"));
                assertTrue(rect.x() >= 0, () -> describe(viewport, rect, "x"));
                assertTrue(rect.y() >= 0, () -> describe(viewport, rect, "y"));
                assertTrue(rect.right() <= viewport[0], () -> describe(viewport, rect, "right"));
                assertTrue(rect.bottom() <= viewport[1], () -> describe(viewport, rect, "bottom"));
            }

            int skillTop = layout.skillButtons().getFirst().y();
            for (BattleScreenLayout.Rect target : layout.targetButtons()) {
                assertTrue(target.bottom() <= skillTop, () -> describe(viewport, target, "target/skill overlap"));
            }
        }
    }

    @Test
    void highGuiScaleUsesCompactTwoColumnTargetsInsteadOfDroppingEnemyButtonsBelowScreen() {
        BattleScreenLayout.Layout layout = BattleScreenLayout.calculate(420, 225);
        int skillTop = layout.skillButtons().getFirst().y();

        BattleScreenLayout.Rect fifthEnemy = layout.targetButtons().get(
                BattleScreenLayout.ALLY_TARGET_COUNT + BattleScreenLayout.ENEMY_TARGET_COUNT - 1
        );
        assertTrue(fifthEnemy.bottom() <= skillTop);
        assertTrue(fifthEnemy.height() > 0);
    }

    private static String describe(int[] viewport, BattleScreenLayout.Rect rect, String problem) {
        return viewport[0] + "x" + viewport[1] + " " + problem + " " + rect;
    }
}
