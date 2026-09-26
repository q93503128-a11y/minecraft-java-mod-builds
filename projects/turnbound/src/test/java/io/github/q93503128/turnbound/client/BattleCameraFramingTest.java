package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleCameraFramingTest {
    @Test
    void standardFormationFramesFromBattleAxisInsteadOfPlayerView() {
        ClientBattleState.Snapshot snapshot = snapshot(0.0F, List.of(
                unit("a1", "ALLY", -3.0, 64.0, -4.0),
                unit("a2", "ALLY", -1.0, 64.0, -4.0),
                unit("a3", "ALLY", 1.0, 64.0, -4.0),
                unit("a4", "ALLY", 3.0, 64.0, -4.0),
                unit("e1", "ENEMY", -4.0, 64.0, 4.0),
                unit("e2", "ENEMY", -2.0, 64.0, 4.0),
                unit("e3", "ENEMY", 0.0, 64.0, 4.0),
                unit("e4", "ENEMY", 2.0, 64.0, 4.0),
                unit("e5", "ENEMY", 4.0, 64.0, 4.0)));

        BattleCameraFraming.Plan plan = BattleCameraFraming.plan(snapshot);
        assertEquals(0.0F, plan.axisYaw(), 0.01F);
        assertEquals(0.0F, plan.yaw(), 0.01F);
        assertTrue(plan.distance() >= 11.5F && plan.distance() <= 12.5F);
        assertTrue(plan.pitch() > 22.0F);
    }

    @Test
    void rotatedFormationRecoversItsOwnAxis() {
        ClientBattleState.Snapshot snapshot = snapshot(-40.0F, List.of(
                unit("a1", "ALLY", 4.0, 64.0, -2.0),
                unit("a2", "ALLY", 4.0, 64.0, 2.0),
                unit("e1", "ENEMY", -4.0, 64.0, -2.0),
                unit("e2", "ENEMY", -4.0, 64.0, 2.0)));

        BattleCameraFraming.Plan plan = BattleCameraFraming.plan(snapshot);
        assertEquals(90.0F, plan.axisYaw(), 0.01F);
        assertEquals(90.0F, plan.yaw(), 0.01F);
    }

    @Test
    void verticalSpreadRaisesPitchWithoutBreakingDistanceBounds() {
        ClientBattleState.Snapshot flat = snapshot(0.0F, List.of(
                unit("a", "ALLY", 0.0, 64.0, -4.0),
                unit("e", "ENEMY", 0.0, 64.0, 4.0)));
        ClientBattleState.Snapshot uneven = snapshot(0.0F, List.of(
                unit("a", "ALLY", 0.0, 61.0, -4.0),
                unit("e", "ENEMY", 0.0, 68.0, 4.0)));

        BattleCameraFraming.Plan flatPlan = BattleCameraFraming.plan(flat);
        BattleCameraFraming.Plan unevenPlan = BattleCameraFraming.plan(uneven);
        assertTrue(unevenPlan.pitch() > flatPlan.pitch());
        assertTrue(unevenPlan.distance() >= 8.5F && unevenPlan.distance() <= 15.5F);
    }

    @Test
    void degenerateSnapshotFallsBackToAuthoredArenaYaw() {
        ClientBattleState.Snapshot snapshot = snapshot(-170.0F, List.of());
        BattleCameraFraming.Plan plan = BattleCameraFraming.plan(snapshot);
        assertEquals(-170.0F, plan.axisYaw(), 0.01F);
        assertEquals(-170.0F, plan.yaw(), 0.01F);
        assertEquals(10.5F, plan.distance(), 0.01F);
    }

    private static ClientBattleState.Unit unit(String id, String side, double x, double y, double z) {
        return new ClientBattleState.Unit(id, id, side, id, 100, 100, 0, 0, false, x, y, z);
    }

    private static ClientBattleState.Snapshot snapshot(float arenaYaw, List<ClientBattleState.Unit> units) {
        return new ClientBattleState.Snapshot(
                true, false, 1, "RUNNING", "a1", false,
                true, true, true,
                units, List.of(), List.of(), "",
                0.0, 64.0, 0.0, arenaYaw,
                ClientBattleState.Result.none());
    }
}
