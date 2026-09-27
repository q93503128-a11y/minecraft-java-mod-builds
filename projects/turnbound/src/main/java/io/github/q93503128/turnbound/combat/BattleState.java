package io.github.q93503128.turnbound.combat;

import java.util.ArrayList;
import java.util.List;

public final class BattleState {
    public record Capacity(int regularAllies, int allySummons, int enemies, int totalCombatants) {
        public static final Capacity SOLO = new Capacity(4, 1, 5, 16);

        public Capacity {
            if (regularAllies < 1 || allySummons < 0 || enemies < 1) throw new IllegalArgumentException("Invalid battle capacity");
            if (totalCombatants < regularAllies + allySummons + enemies) {
                throw new IllegalArgumentException("Battle total capacity is too small");
            }
        }
    }

    private final List<CombatantState> combatants;
    private final Capacity capacity;
    private final List<BattleEvent> events = new ArrayList<>();
    private String currentActorId;
    private long logicalTimeMicro;

    public BattleState(List<CombatantState> combatants) {
        this(combatants, Capacity.SOLO);
    }

    public BattleState(List<CombatantState> combatants, Capacity capacity) {
        this.capacity = capacity == null ? Capacity.SOLO : capacity;
        this.combatants = new ArrayList<>(combatants);
        validateSides();
    }

    public List<CombatantState> combatants() { return List.copyOf(combatants); }
    public Capacity capacity() { return capacity; }
    public List<BattleEvent> events() { return List.copyOf(events); }
    public String currentActorId() { return currentActorId; }
    public long logicalTimeMicro() { return logicalTimeMicro; }
    /** Compatibility accessor for older internal callers. Value is now fixed-point logical micro-time. */
    @Deprecated
    public long logicalPulse() { return logicalTimeMicro; }
    void setCurrentActorId(String id) { currentActorId = id; }
    void addLogicalTimeMicro(long delta) { logicalTimeMicro = Math.addExact(logicalTimeMicro, Math.max(0L, delta)); }
    @Deprecated
    void addLogicalPulse(long delta) { addLogicalTimeMicro(delta); }
    void addEvent(BattleEvent e) { events.add(e); }

    public void addCombatant(CombatantState combatant) {
        if (combatants.stream().anyMatch(unit -> unit.instanceId().equals(combatant.instanceId()))) {
            throw new IllegalArgumentException("Duplicate combatant " + combatant.instanceId());
        }
        if (combatant.side() == CombatantSide.ALLY) {
            long regularAllies = combatants.stream().filter(unit -> unit.side() == CombatantSide.ALLY && !unit.definition().summon()).count();
            long livingSummons = combatants.stream().filter(unit -> unit.side() == CombatantSide.ALLY && unit.definition().summon() && !unit.downed()).count();
            if ((!combatant.definition().summon() && regularAllies >= capacity.regularAllies()) || (combatant.definition().summon() && livingSummons >= capacity.allySummons())) {
                throw new IllegalStateException("TURNBOUND ally/summon cap reached");
            }
        } else {
            long livingEnemies = combatants.stream().filter(unit -> unit.side() == CombatantSide.ENEMY && !unit.downed()).count();
            if (livingEnemies >= capacity.enemies()) throw new IllegalStateException("TURNBOUND enemy cap reached");
        }
        if (combatants.size() >= capacity.totalCombatants()) throw new IllegalStateException("Battle combatant cap reached");
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
        if (regularAllies < 1 || regularAllies > capacity.regularAllies()
                || allySummons > capacity.allySummons()
                || enemies < 1 || enemies > capacity.enemies()
                || combatants.size() > capacity.totalCombatants()) {
            throw new IllegalArgumentException("Battle combatants exceed configured capacity");
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
        return TurnScheduler.preview(this, count);
    }
}
