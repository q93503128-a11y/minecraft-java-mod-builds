package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleTurnGaugeModelTest {
    @Test
    void rowsFollowServerPreviewAndExposeRepeatedUpcomingTurns() {
        var a = unit("a", "ALLY", 760, 110, true);
        var b = unit("b", "ENEMY", 820, 90, true);
        var c = unit("c", "ALLY", 300, 105, true);
        var snapshot = snapshot(List.of(a, b, c), List.of("a", "b", "a", "c", "b"));

        var rows = BattleTurnGaugeModel.rows(snapshot, 9);

        assertEquals(List.of("a", "b", "c"), rows.stream().map(row -> row.unit().id()).toList());
        assertEquals("1·3", rows.get(0).orderLabel());
        assertEquals("2·5", rows.get(1).orderLabel());
        assertEquals("4", rows.get(2).orderLabel());
    }

    @Test
    void downedAndNonScheduledCompanionsDoNotOccupyTheActionGauge() {
        var actor = unit("actor", "ALLY", 1000, 100, true);
        var summon = unit("summon", "ALLY", 900, 100, false);
        var down = new ClientBattleState.Unit(
                "down", "D", "ENEMY", "Down", 0, 100, 0, 800, true,
                0, 0, 0, List.of(), 100, true);
        var snapshot = snapshot(List.of(actor, summon, down), List.of("actor"));

        var rows = BattleTurnGaugeModel.rows(snapshot, 9);

        assertEquals(1, rows.size());
        assertEquals("actor", rows.getFirst().unit().id());
        assertEquals(1, BattleTurnGaugeModel.scheduledLivingCount(snapshot));
    }

    @Test
    void gaugeFillClampsAndReadyLabelIsExplicit() {
        var over = unit("over", "ALLY", 1250, 100, true);
        var negativeSafe = unit("zero", "ENEMY", 0, 100, true);

        assertEquals(1.0, BattleTurnGaugeModel.gaugeRatio(over), 0.0001);
        assertEquals("READY+250", BattleTurnGaugeModel.gaugeLabel(over));
        assertEquals(0.0, BattleTurnGaugeModel.gaugeRatio(negativeSafe), 0.0001);
        assertFalse(BattleTurnGaugeModel.gaugeLabel(negativeSafe).isBlank());
    }

    private static ClientBattleState.Unit unit(String id, String side, long gauge, int speed, boolean scheduled) {
        return new ClientBattleState.Unit(
                id, id.toUpperCase(), side, id, 100, 100, 0, gauge, false,
                0, 0, 0, List.of(), speed, scheduled);
    }

    private static ClientBattleState.Snapshot snapshot(List<ClientBattleState.Unit> units, List<String> timeline) {
        return new ClientBattleState.Snapshot(
                true, false, 1, "RUNNING", timeline.isEmpty() ? "" : timeline.getFirst(), false,
                true, true, true, units, timeline, List.of(), "",
                0, 0, 0, 0.0F);
    }
}
