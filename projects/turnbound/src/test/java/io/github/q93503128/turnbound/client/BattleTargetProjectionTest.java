package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleTargetProjectionTest {
    @Test
    void projectsAndPicksAVisibleEnemyFromTheBattlefield() {
        BattleCameraController.View view = new BattleCameraController.View(0.0F, 28.0F, 7.8F, 70.0F);
        ClientBattleState.Unit ally = new ClientBattleState.Unit(
                "ally_1", "P01", "ALLY", "Kairen", 900, 900, 0, 0, false,
                -1.0, 64.0, -2.2);
        ClientBattleState.Unit enemy = new ClientBattleState.Unit(
                "enemy_1", "E01", "ENEMY", "Enemy", 600, 600, 0, 0, false,
                0.0, 64.0, 2.8);

        BattleTargetProjection.ScreenPoint point = BattleTargetProjection.project(
                0.0, 64.0, 0.0, view, 854, 480, enemy.x(), enemy.y() + 1.15, enemy.z());
        assertNotNull(point);
        assertTrue(point.depth() > 0.0);

        int picked = BattleTargetProjection.pick(List.of(ally, enemy), "ENEMY_SINGLE", "ally_1",
                0.0, 64.0, 0.0, view, 854, 480, point.x(), point.y());
        assertEquals(1, picked);
    }

    @Test
    void doesNotPickInvalidSideEvenWhenCoordinatesOverlap() {
        BattleCameraController.View view = new BattleCameraController.View(0.0F, 28.0F, 7.8F, 70.0F);
        ClientBattleState.Unit ally = new ClientBattleState.Unit(
                "ally_1", "P01", "ALLY", "Kairen", 900, 900, 0, 0, false,
                0.0, 64.0, 2.8);
        BattleTargetProjection.ScreenPoint point = BattleTargetProjection.project(
                0.0, 64.0, 0.0, view, 854, 480, ally.x(), ally.y() + 1.15, ally.z());
        assertNotNull(point);
        assertEquals(-1, BattleTargetProjection.pick(List.of(ally), "ENEMY_SINGLE", "ally_1",
                0.0, 64.0, 0.0, view, 854, 480, point.x(), point.y()));
    }
}