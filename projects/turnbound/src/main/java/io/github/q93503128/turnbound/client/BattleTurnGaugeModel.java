package io.github.q93503128.turnbound.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure projection for the battle action-gauge HUD. Server timeline remains the ordering authority. */
final class BattleTurnGaugeModel {
    record Row(ClientBattleState.Unit unit, List<Integer> previewSlots) {
        Row {
            previewSlots = List.copyOf(previewSlots == null ? List.of() : previewSlots);
        }

        int firstPreviewSlot() {
            return previewSlots.isEmpty() ? Integer.MAX_VALUE : previewSlots.getFirst();
        }

        String orderLabel() {
            if (previewSlots.isEmpty()) return "·";
            if (previewSlots.size() == 1) return Integer.toString(previewSlots.getFirst());
            return previewSlots.getFirst() + "·" + previewSlots.get(1) + (previewSlots.size() > 2 ? "+" : "");
        }
    }

    private BattleTurnGaugeModel() {}

    static List<Row> rows(ClientBattleState.Snapshot snapshot, int maxRows) {
        if (snapshot == null || maxRows <= 0) return List.of();

        Map<String, List<Integer>> preview = new LinkedHashMap<>();
        for (int i = 0; i < snapshot.timeline().size(); i++) {
            String id = snapshot.timeline().get(i);
            preview.computeIfAbsent(id, ignored -> new ArrayList<>()).add(i + 1);
        }

        List<Row> rows = snapshot.units().stream()
                .filter(unit -> !unit.downed() && unit.scheduled())
                .map(unit -> new Row(unit, preview.getOrDefault(unit.id(), List.of())))
                .sorted(Comparator
                        .comparingInt(Row::firstPreviewSlot)
                        .thenComparingDouble(row -> estimatedReadyDistance(row.unit()))
                        .thenComparing(row -> row.unit().id()))
                .toList();

        if (rows.size() <= maxRows) return rows;
        return List.copyOf(rows.subList(0, maxRows));
    }

    static int scheduledLivingCount(ClientBattleState.Snapshot snapshot) {
        if (snapshot == null) return 0;
        return (int)snapshot.units().stream().filter(unit -> !unit.downed() && unit.scheduled()).count();
    }

    static double gaugeRatio(ClientBattleState.Unit unit) {
        if (unit == null) return 0.0;
        return Math.max(0.0, Math.min(1.0, unit.gauge() / 1000.0));
    }

    static String gaugeLabel(ClientBattleState.Unit unit) {
        if (unit == null) return "0";
        if (unit.gauge() >= 1000) return "READY";
        return Long.toString(Math.max(0L, unit.gauge()));
    }

    private static double estimatedReadyDistance(ClientBattleState.Unit unit) {
        if (unit == null || unit.speed() <= 0) return Double.POSITIVE_INFINITY;
        return Math.max(0.0, 1000.0 - unit.gauge()) / unit.speed();
    }
}
