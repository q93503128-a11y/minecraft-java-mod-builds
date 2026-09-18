package io.github.q93503128.turnbound.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Single deterministic fixed-point clock for runtime turn selection and HUD timeline projection.
 *
 * <p>Displayed Gauge remains in 0..1000-style units, while scheduling uses micro-Gauge so SPD differences are
 * not quantized into whole pulses.</p>
 */
public final class TurnScheduler {
    public static final long GAUGE_SCALE = 1_000_000L;
    public static final long TURN_THRESHOLD = 1000L;
    public static final long TURN_THRESHOLD_MICRO = TURN_THRESHOLD * GAUGE_SCALE;

    private TurnScheduler() {}

    public static CombatantState nextReady(BattleState state) {
        if (state.currentActorId() != null) return state.combatant(state.currentActorId());

        List<Node> nodes = state.combatants().stream()
                .filter(unit -> !unit.downed())
                .map(Node::new)
                .toList();
        if (nodes.isEmpty()) throw new IllegalStateException("No living combatants");

        Step step = advance(nodes);
        for (Node node : nodes) node.combatant.setGaugeMicro(node.gaugeMicro);
        state.addLogicalTimeMicro(step.deltaTimeMicro);
        state.setCurrentActorId(step.selected.combatant.instanceId());
        return step.selected.combatant;
    }

    public static List<CombatantState> preview(BattleState state, int count) {
        if (count <= 0) return List.of();

        List<Node> nodes = state.combatants().stream()
                .filter(unit -> !unit.downed())
                .map(Node::new)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (nodes.isEmpty()) return List.of();

        List<CombatantState> out = new ArrayList<>(count);

        if (state.currentActorId() != null) {
            Node current = nodes.stream()
                    .filter(node -> node.combatant.instanceId().equals(state.currentActorId()))
                    .findFirst().orElse(null);
            if (current != null) {
                out.add(current.combatant);
                current.gaugeMicro = Math.max(0L, current.gaugeMicro - TURN_THRESHOLD_MICRO);
            }
        }

        while (out.size() < count && !nodes.isEmpty()) {
            Step step = advance(nodes);
            out.add(step.selected.combatant);
            step.selected.gaugeMicro = Math.max(0L, step.selected.gaugeMicro - TURN_THRESHOLD_MICRO);
        }
        return List.copyOf(out);
    }

    public static long toGaugeMicro(long gaugeUnits) {
        return Math.multiplyExact(gaugeUnits, GAUGE_SCALE);
    }

    public static long displayGauge(long gaugeMicro) {
        return Math.max(0L, gaugeMicro) / GAUGE_SCALE;
    }

    private static Step advance(List<Node> nodes) {
        long delta = nodes.stream().mapToLong(TurnScheduler::timeToReadyMicro).min().orElseThrow();
        if (delta > 0L) {
            for (Node node : nodes) {
                node.gaugeMicro = Math.addExact(node.gaugeMicro,
                        Math.multiplyExact(delta, (long)node.speed()));
            }
        }

        Node selected = nodes.stream()
                .filter(node -> node.gaugeMicro >= TURN_THRESHOLD_MICRO)
                .max(priorityComparator())
                .orElseThrow(() -> new IllegalStateException("Scheduler advanced without a ready combatant"));
        return new Step(delta, selected);
    }

    private static long timeToReadyMicro(Node node) {
        if (node.gaugeMicro >= TURN_THRESHOLD_MICRO) return 0L;
        long missing = TURN_THRESHOLD_MICRO - node.gaugeMicro;
        long speed = node.speed();
        long whole = missing / speed;
        return whole + (missing % speed == 0L ? 0L : 1L);
    }

    private static Comparator<Node> priorityComparator() {
        return Comparator.comparingLong((Node node) -> node.gaugeMicro)
                .thenComparingInt(Node::speed)
                .thenComparingInt(node -> -node.combatant.initiativeSeed());
    }

    private static final class Node {
        private final CombatantState combatant;
        private long gaugeMicro;

        private Node(CombatantState combatant) {
            this.combatant = combatant;
            this.gaugeMicro = combatant.gaugeMicro();
        }

        private int speed() {
            return combatant.speed();
        }
    }

    private record Step(long deltaTimeMicro, Node selected) {}
}
