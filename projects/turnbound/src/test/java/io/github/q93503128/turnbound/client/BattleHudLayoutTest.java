package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BattleHudLayoutTest {
    @Test
    void allHudRectanglesStayPositiveAndInsideViewport() {
        int[][] sizes = {{256,160},{320,180},{420,225},{480,270},{640,360},{854,480},{960,540},{1280,720},{1920,1080}};
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
        for (var enemy : layout.enemyBars()) assertEquals(1, enemy.width());
        assertEquals(1, layout.confirmButton().width());
    }

    @Test
    void utilityControlsReserveEnoughWidthForKoreanStateLabels() {
        for (int[] size : new int[][]{{320,180},{640,360},{854,480},{1280,720}}) {
            BattleHudLayout.Layout layout = BattleHudLayout.calculate(size[0], size[1]);
            assertTrue(layout.autoButton().width() >= 48, size[0] + "x" + size[1]);
            assertTrue(layout.speedButton().width() >= 50, size[0] + "x" + size[1]);
            assertTrue(layout.fleeButton().width() >= 50, size[0] + "x" + size[1]);
        }
    }

    @Test
    void referenceActionDockKeepsOneVerticalScanPathAndLowScreenCoverage() {
        for (int[] size : new int[][]{{854,480},{1280,720},{1920,1080}}) {
            BattleHudLayout.Layout layout = BattleHudLayout.calculate(size[0], size[1]);
            var skills = layout.skillButtons();
            assertFalse(layout.compact(), size[0] + "x" + size[1]);
            assertTrue(skills.getFirst().width() <= size[0] * 0.18, size[0] + "x" + size[1]);
            assertTrue(skills.getFirst().height() <= 22, size[0] + "x" + size[1]);
            for (int i = 1; i < skills.size(); i++) {
                assertEquals(skills.getFirst().x(), skills.get(i).x());
                assertEquals(skills.getFirst().width(), skills.get(i).width());
                assertTrue(skills.get(i).y() > skills.get(i - 1).bottom());
            }
            assertEquals(skills.getFirst().x(), layout.actionHeader().x());
            assertEquals(skills.getFirst().width(), layout.actionHeader().width());
            assertTrue(layout.allyBars().getFirst().height() <= size[1] * 0.05);
        }
    }

    @Test
    void smallerGuiViewportUsesCompactDockBeforeItBecomesAUiWall() {
        BattleHudLayout.Layout layout = BattleHudLayout.calculate(640, 360);
        assertTrue(layout.compact());
        assertTrue(layout.skillButtons().getFirst().width() <= 640 * 0.20);
        for (var skill : layout.skillButtons()) {
            assertFalse(skill.overlaps(layout.autoButton()));
            assertFalse(skill.overlaps(layout.speedButton()));
            assertFalse(skill.overlaps(layout.fleeButton()));
        }
    }

    @Test
    void tinyViewportShrinksRowsBeforeAllowingActionDockToHitUtilityControls() {
        for (int[] size : new int[][]{{256,160},{320,180}}) {
            BattleHudLayout.Layout layout = BattleHudLayout.calculate(size[0], size[1]);
            for (var skill : layout.skillButtons()) {
                assertFalse(skill.overlaps(layout.autoButton()), size[0] + "x" + size[1]);
                assertFalse(skill.overlaps(layout.speedButton()), size[0] + "x" + size[1]);
                assertFalse(skill.overlaps(layout.fleeButton()), size[0] + "x" + size[1]);
            }
        }
    }
}
