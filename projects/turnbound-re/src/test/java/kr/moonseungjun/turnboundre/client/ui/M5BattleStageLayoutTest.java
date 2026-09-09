package kr.moonseungjun.turnboundre.client.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M5BattleStageLayoutTest {
    @Test
    void fullVirtualStageFitsInsideCanonicalReservedViewport() {
        UiLayoutMetrics.Rect viewport = UiLayoutMetrics.battleHud(480, 270).reservedWorldViewport();
        BattleStageLayout.Layout layout = BattleStageLayout.arrange(viewport, 4, 5);

        assertEquals(4, layout.players().size());
        assertEquals(5, layout.enemies().size());
        assertFalse(layout.enemyBand().intersects(layout.playerBand()));

        layout.enemies().forEach(slot -> assertInside(viewport, slot.bounds()));
        layout.players().forEach(slot -> assertInside(viewport, slot.bounds()));
    }

    @Test
    void largerCanvasKeepsOpposingRowsCenteredAndSeparated() {
        UiLayoutMetrics.Rect viewport = UiLayoutMetrics.battleHud(1280, 720).reservedWorldViewport();
        BattleStageLayout.Layout layout = BattleStageLayout.arrange(viewport, 4, 3);

        assertEquals(BattleStageLayout.Side.ENEMY, layout.enemies().getFirst().side());
        assertEquals(BattleStageLayout.Side.PLAYER, layout.players().getFirst().side());
        assertTrue(layout.enemies().getFirst().bounds().y() < layout.players().getFirst().bounds().y());
        assertFalse(layout.enemyBand().intersects(layout.playerBand()));
    }

    @Test
    void stageHitTestResolvesRenderedSlotsButNotGapsOrOutsidePoints() {
        UiLayoutMetrics.Rect viewport = UiLayoutMetrics.battleHud(640, 360).reservedWorldViewport();
        BattleStageLayout.Layout layout = BattleStageLayout.arrange(viewport, 4, 3);
        BattleStageLayout.Slot enemy = layout.enemies().get(1);
        BattleStageLayout.Slot player = layout.players().get(2);

        assertEquals(enemy, BattleStageLayout.slotAt(
                layout,
                enemy.bounds().x() + enemy.bounds().width() / 2.0D,
                enemy.bounds().y() + enemy.bounds().height() / 2.0D).orElseThrow());
        assertEquals(player, BattleStageLayout.slotAt(
                layout,
                player.bounds().x() + player.bounds().width() / 2.0D,
                player.bounds().y() + player.bounds().height() / 2.0D).orElseThrow());

        BattleStageLayout.Slot firstEnemy = layout.enemies().getFirst();
        BattleStageLayout.Slot secondEnemy = layout.enemies().get(1);
        double gapX = (firstEnemy.bounds().right() + secondEnemy.bounds().x()) / 2.0D;
        assertTrue(BattleStageLayout.slotAt(layout, gapX, firstEnemy.bounds().y() + 1.0D).isEmpty());
        assertTrue(BattleStageLayout.slotAt(layout, viewport.x() - 1.0D, viewport.y()).isEmpty());
        assertTrue(BattleStageLayout.slotAt(layout, Double.NaN, viewport.y()).isEmpty());
    }

    @Test
    void stageRejectsCountsOutsideCanonicalBattleLimits() {
        UiLayoutMetrics.Rect viewport = UiLayoutMetrics.battleHud(640, 360).reservedWorldViewport();
        assertThrows(IllegalArgumentException.class, () -> BattleStageLayout.arrange(viewport, 5, 1));
        assertThrows(IllegalArgumentException.class, () -> BattleStageLayout.arrange(viewport, 1, 6));
    }

    private static void assertInside(UiLayoutMetrics.Rect parent, UiLayoutMetrics.Rect child) {
        assertTrue(child.x() >= parent.x());
        assertTrue(child.y() >= parent.y());
        assertTrue(child.right() <= parent.right());
        assertTrue(child.bottom() <= parent.bottom());
    }
}
