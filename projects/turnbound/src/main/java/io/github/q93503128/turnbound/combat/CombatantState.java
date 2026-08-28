package io.github.q93503128.turnbound.combat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CombatantState {
    private final String instanceId;
    private final CombatantDefinition definition;
    private final CombatantSide side;
    private final int initiativeSeed;
    private int hp;
    private int barrier;
    private long gauge;
    private boolean downed;
    private final Map<String, Integer> cooldowns = new HashMap<>();
    private final Map<String, Integer> counters = new HashMap<>();
    private final Map<String, String> refs = new HashMap<>();
    private final Map<String, StatusInstance> statuses = new HashMap<>();
    private final Set<String> flags = new HashSet<>();

    public CombatantState(String instanceId, CombatantDefinition definition, CombatantSide side, int initiativeSeed) {
        this.instanceId = instanceId;
        this.definition = definition;
        this.side = side;
        this.initiativeSeed = initiativeSeed;
        this.hp = definition.stats().maxHp();
    }

    public String instanceId() { return instanceId; }
    public CombatantDefinition definition() { return definition; }
    public CombatantSide side() { return side; }
    public int initiativeSeed() { return initiativeSeed; }
    public int hp() { return hp; }
    public int maxHp() { return definition.stats().maxHp(); }
    public int attack() {
        StatusInstance boost = status("attack_multiplier");
        double multiplier = boost == null ? 1.0 : 1.0 + boost.magnitude();
        return Math.max(1, (int) Math.floor(definition.stats().attack() * multiplier));
    }
    public int defense() { return definition.stats().defense(); }
    public int speed() { return definition.stats().speed(); }
    public int barrier() { return barrier; }
    public long gauge() { return gauge; }
    public boolean downed() { return downed; }

    public void setGauge(long value) { gauge = Math.max(0L, value); }
    public void addGauge(long value) { setGauge(gauge + value); }
    public void spendTurnGauge() { gauge = Math.max(0L, gauge - BattleEngine.TURN_THRESHOLD); }

    public void setCooldown(String skillId, int value) {
        if (value <= 0) cooldowns.remove(skillId); else cooldowns.put(skillId, value);
    }
    public int cooldown(String skillId) { return cooldowns.getOrDefault(skillId, 0); }
    public Map<String, Integer> cooldownsView() { return Map.copyOf(cooldowns); }

    public int counter(String id) { return counters.getOrDefault(id, 0); }
    public void setCounter(String id, int value) { if (value == 0) counters.remove(id); else counters.put(id, value); }
    public void incrementCounter(String id, int delta, int max) { setCounter(id, Math.min(max, counter(id) + delta)); }
    public String ref(String id) { return refs.get(id); }
    public void setRef(String id, String value) { if (value == null) refs.remove(id); else refs.put(id, value); }
    public boolean flag(String id) { return flags.contains(id); }
    public void setFlag(String id) { flags.add(id); }

    public StatusInstance status(String id) { return statuses.get(id); }
    public void putStatus(StatusInstance status) { statuses.put(status.id(), status); }
    public void removeStatus(String id) { statuses.remove(id); }
    public Map<String, StatusInstance> statusesView() { return Map.copyOf(statuses); }

    public int addBarrier(int amount) {
        int cap = (int) Math.floor(maxHp() * 0.60);
        int before = barrier;
        barrier = Math.min(cap, Math.max(0, barrier + amount));
        return barrier - before;
    }

    public int heal(int amount) {
        if (downed || amount <= 0) return 0;
        int before = hp;
        hp = Math.min(maxHp(), hp + amount);
        return hp - before;
    }

    public int takeDamage(int amount) {
        if (downed || amount <= 0) return 0;
        int remaining = amount;
        int absorbed = Math.min(barrier, remaining);
        barrier -= absorbed;
        remaining -= absorbed;
        int hpBefore = hp;
        hp = Math.max(0, hp - remaining);
        if (hp == 0) downed = true;
        return hpBefore - hp;
    }

    public int revive(double ratio) {
        if (!downed) return 0;
        hp = Math.max(1, (int) Math.floor(maxHp() * ratio));
        barrier = 0;
        gauge = 0;
        downed = false;
        statuses.clear();
        return hp;
    }

    public void tickStatusesOnOwnTurn() {
        var copy = Map.copyOf(statuses);
        for (var entry : copy.entrySet()) {
            StatusInstance next = entry.getValue().tickOwnerTurn();
            if (next.remainingOwnerTurns() <= 0) statuses.remove(entry.getKey());
            else statuses.put(entry.getKey(), next);
        }
    }
}
