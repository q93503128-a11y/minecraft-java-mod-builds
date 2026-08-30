package io.github.q93503128.turnbound.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BattleState {
    private final List<CombatantState> combatants;
    private final List<BattleEvent> events = new ArrayList<>();
    private String currentActorId;
    private long logicalPulse;

    public BattleState(List<CombatantState> combatants) {
        this.combatants = new ArrayList<>(combatants);
        validateSides();
    }

    public List<CombatantState> combatants() { return List.copyOf(combatants); }
    public List<BattleEvent> events() { return List.copyOf(events); }
    public String currentActorId() { return currentActorId; }
    public long logicalPulse() { return logicalPulse; }
    void setCurrentActorId(String id) { currentActorId = id; }
    void addLogicalPulse(long p) { logicalPulse += p; }
    void addEvent(BattleEvent e) { events.add(e); }

    public void addCombatant(CombatantState combatant) {
        if (combatants.stream().anyMatch(unit -> unit.instanceId().equals(combatant.instanceId()))) {
            throw new IllegalArgumentException("Duplicate combatant " + combatant.instanceId());
        }
        if (combatant.side() == CombatantSide.ALLY) {
            long regularAllies = combatants.stream().filter(unit -> unit.side() == CombatantSide.ALLY && !unit.definition().summon()).count();
            long livingSummons = combatants.stream().filter(unit -> unit.side() == CombatantSide.ALLY && unit.definition().summon() && !unit.downed()).count();
            if ((!combatant.definition().summon() && regularAllies >= 4) || (combatant.definition().summon() && livingSummons >= 1)) {
                throw new IllegalStateException("TURNBOUND ally/summon cap reached");
            }
        } else {
            long livingEnemies = combatants.stream().filter(unit -> unit.side() == CombatantSide.ENEMY && !unit.downed()).count();
            if (livingEnemies >= 5) throw new IllegalStateException("TURNBOUND enemy cap reached");
        }
        if (combatants.size() >= 16) throw new IllegalStateException("Battle combatant cap reached");
        combatants.add(combatant);
    }

    public void removeCombatant(String instanceId) {
        if (instanceId != null && instanceId.equals(currentActorId)) currentActorId = null;
        combatants.removeIf(unit -> unit.instanceId().equals(instanceId));
    }

    private void validateSides() {
        long regularAllies = combatants.stream().filter(c -> c.side() == CombatantSide.ALLY && !c.definition().summon()).count();
        long allySummons = combatants.stream().filter(c -> c.side() == CombatantSide.ALLY && c.definition().summon()).count();
        long enemies = combatants.stream().filter(c -> c.side() == CombatantSide.ENEMY).count();
        if (regularAllies < 1 || regularAllies > 4 || allySummons > 1 || enemies < 1 || enemies > 5) {
            throw new IllegalArgumentException("Battle requires 1-4 allies, at most one allied summon, and 1-5 enemies");
        }
    }

    public CombatantState combatant(String id) {
        return combatants.stream().filter(c -> c.instanceId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown combatant " + id));
    }

    public CombatantState find(String id) {
        return combatants.stream().filter(c -> c.instanceId().equals(id)).findFirst().orElse(null);
    }

    public List<CombatantState> living(CombatantSide side) {
        return combatants.stream().filter(c -> c.side() == side && !c.downed()).toList();
    }

    public List<CombatantState> downed(CombatantSide side) {
        return combatants.stream().filter(c -> c.side() == side && c.downed()).toList();
    }

    public BattleOutcome outcome() {
        if (living(CombatantSide.ENEMY).isEmpty()) return BattleOutcome.ALLY_VICTORY;
        if (living(CombatantSide.ALLY).isEmpty()) {
            boolean pendingReturn = combatants.stream().anyMatch(c -> c.side() == CombatantSide.ALLY && c.downed()
                    && c.definition().id().equals("P06") && c.counter("p06_return_wait") > 0);
            if (!pendingReturn) return BattleOutcome.ENEMY_VICTORY;
        }
        return BattleOutcome.RUNNING;
    }

    public List<CombatantState> timelinePreview(int count) {
        record N(CombatantState c, long[] g) {}
        List<N> nodes = combatants.stream().filter(c -> !c.downed()).map(c -> new N(c, new long[]{c.gauge()})).toList();
        List<CombatantState> out = new ArrayList<>();
        while (out.size() < count && !nodes.isEmpty()) {
            long p = nodes.stream().mapToLong(n -> pulses(n.c(), n.g()[0])).min().orElse(0);
            for (N n : nodes) n.g()[0] += p * n.c().speed();
            N selected = nodes.stream().filter(n -> n.g()[0] >= BattleEngine.TURN_THRESHOLD)
                    .max(Comparator.comparingLong((N n) -> n.g()[0])
                            .thenComparingInt(n -> n.c().speed())
                            .thenComparingInt(n -> -n.c().initiativeSeed())).orElseThrow();
            out.add(selected.c());
            selected.g()[0] -= BattleEngine.TURN_THRESHOLD;
        }
        return List.copyOf(out);
    }

    private static long pulses(CombatantState c, long gauge) {
        if (gauge >= BattleEngine.TURN_THRESHOLD) return 0;
        long missing = BattleEngine.TURN_THRESHOLD - gauge;
        return (missing + c.speed() - 1L) / c.speed();
    }
}
