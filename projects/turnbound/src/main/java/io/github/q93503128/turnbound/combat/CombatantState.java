package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.CharacterSkillRegistry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private final Map<String, StatusInstance> statuses = new LinkedHashMap<>();
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
        double statusMod = cappedStatMod("attack_multiplier", 0.60);
        double passive = 0.0;
        if (definition.id().equals("P06")) passive += definition.param("memoryAttackPer", 0.06) * counter("memory");
        if (definition.id().equals("P08")) {
            double ratio = hp / (double)maxHp();
            if (ratio <= definition.param("midThreshold", 0.50)) passive += definition.param("midAtk", 0.15);
            if (ratio <= definition.param("lowThreshold", 0.30)) passive += definition.param("lowExtraAtk", 0.15);
        }
        return Math.max(1, (int)Math.floor(definition.stats().attack() * Math.max(0.0, 1.0 + statusMod + passive)));
    }

    public int defense() {
        double statusMod = cappedStatMod("defense_multiplier", 0.60);
        double passive = 0.0;
        if (definition.id().equals("P08") && hp / (double)maxHp() <= definition.param("lowThreshold", 0.30)) {
            passive += definition.param("lowDef", -0.20);
        }
        return Math.max(0, (int)Math.floor(definition.stats().defense() * Math.max(0.0, 1.0 + statusMod + passive)));
    }

    public int speed() {
        double statusMod = cappedStatMod("speed_multiplier", 0.50);
        return Math.max(30, (int)Math.floor(definition.stats().speed() * Math.max(0.50, 1.0 + statusMod)));
    }

    public double damageReduction() { return Math.max(0.0, Math.min(0.60, statusMagnitude("damage_reduction"))); }
    public double damageTakenModifier() { return clamp(statusMagnitude("damage_taken_multiplier"), -0.60, 0.60); }
    public double healingReceivedModifier() { return clamp(statusMagnitude("healing_received_multiplier"), -0.60, 0.60); }
    public int barrier() { return barrier; }
    public long gauge() { return gauge; }
    public boolean downed() { return downed; }

    public void setGauge(long value) { gauge = Math.max(0L, value); }
    public void addGauge(long value) { setGauge(gauge + value); }
    public void spendTurnGauge() { gauge = Math.max(0L, gauge - BattleEngine.TURN_THRESHOLD); }

    public void setCooldown(String skillId, int value) {
        String runtimeId = CharacterSkillRegistry.runtimeSkillId(skillId);
        if (value <= 0) cooldowns.remove(runtimeId); else cooldowns.put(runtimeId, Math.min(9, value));
    }
    public int cooldown(String skillId) { return cooldowns.getOrDefault(CharacterSkillRegistry.runtimeSkillId(skillId), 0); }
    /** Internal runtime view; network boundaries canonicalize these keys explicitly. */
    public Map<String, Integer> cooldownsView() { return Map.copyOf(cooldowns); }

    public int counter(String id) { return counters.getOrDefault(id, 0); }
    public void setCounter(String id, int value) { if (value == 0) counters.remove(id); else counters.put(id, value); }
    public void incrementCounter(String id, int delta, int max) { setCounter(id, Math.max(0, Math.min(max, counter(id) + delta))); }
    public String ref(String id) { return refs.get(id); }
    public void setRef(String id, String value) { if (value == null || value.isBlank()) refs.remove(id); else refs.put(id, value); }
    public boolean flag(String id) { return flags.contains(id); }
    public void setFlag(String id) { flags.add(id); }
    public void clearFlag(String id) { flags.remove(id); }

    public StatusInstance status(String id) {
        return statuses.values().stream().filter(status -> status.id().equals(id)).findFirst().orElse(null);
    }
    public StatusInstance status(String id, String sourceId) { return statuses.get(statusKey(id, sourceId)); }
    public boolean hasStatus(String id) { return status(id) != null; }
    public double statusMagnitude(String id) {
        return statuses.values().stream().filter(s -> s.id().equals(id))
                .mapToDouble(s -> s.magnitude() * Math.max(1, s.stacks())).sum();
    }

    public void putStatus(StatusInstance status) {
        String key = statusKey(status.id(), status.sourceId());
        StatusInstance current = statuses.get(key);
        statuses.put(key, current == null ? status : current.refresh(status.remainingOwnerTurns(), status.magnitude()));
    }

    public void addStatusStack(String id, String sourceId, int turns, double magnitude, int amount, int maxStacks) {
        String key = statusKey(id, sourceId);
        StatusInstance current = statuses.get(key);
        if (current == null) {
            statuses.put(key, new StatusInstance(id, sourceId, turns, magnitude, Math.min(maxStacks, Math.max(1, amount))));
        } else {
            statuses.put(key, new StatusInstance(id, sourceId, Math.max(turns, current.remainingOwnerTurns()), magnitude,
                    Math.min(maxStacks, Math.max(1, current.stacks() + amount))));
        }
    }

    public void removeStatus(String id) { statuses.entrySet().removeIf(entry -> entry.getValue().id().equals(id)); }
    public void removeStatus(String id, String sourceId) { statuses.remove(statusKey(id, sourceId)); }

    public Map<String, StatusInstance> statusesView() {
        Map<String, StatusInstance> out = new LinkedHashMap<>();
        for (StatusInstance status : statuses.values()) {
            String key = status.id();
            if (out.containsKey(key)) key = status.id() + "@" + safeSource(status.sourceId());
            int suffix = 2;
            String unique = key;
            while (out.containsKey(unique)) unique = key + "#" + suffix++;
            out.put(unique, status);
        }
        return Map.copyOf(out);
    }

    public int addBarrier(int amount) {
        int cap = (int)Math.floor(maxHp() * 0.60);
        int before = barrier;
        barrier = Math.min(cap, Math.max(0, barrier + amount));
        return barrier - before;
    }

    public int heal(int amount) {
        if (downed || amount <= 0) return 0;
        int adjusted = Math.max(0, (int)Math.floor(amount * Math.max(0.0, 1.0 + healingReceivedModifier())));
        int before = hp;
        hp = Math.min(maxHp(), hp + adjusted);
        return hp - before;
    }

    public int takeDamage(int amount) {
        if (downed || amount <= 0) return 0;
        int remaining = amount;
        int absorbed = Math.min(barrier, remaining);
        barrier -= absorbed;
        remaining -= absorbed;
        int hpBefore = hp;
        if (remaining >= hp && definition.id().equals("P08") && definition.hasRule("AWAKENED") && !flag("p08_lethal_survival_used")) {
            hp = 1;
            setFlag("p08_lethal_survival_used");
            setFlag("p08_lethal_just_triggered");
            setFlag("p08_next_blood_free");
            addGauge(definition.intParam("awakenLethalGauge", 500));
            return hpBefore - hp;
        }
        hp = Math.max(0, hp - remaining);
        if (hp == 0) downed = true;
        return hpBefore - hp;
    }

    public int spendCurrentHp(double ratio) {
        if (downed || ratio <= 0) return 0;
        int cost = Math.max(0, (int)Math.floor(hp * ratio));
        int before = hp;
        hp = Math.max(1, hp - cost);
        return before - hp;
    }

    public int forceDown() {
        if (downed) return 0;
        int before = hp;
        hp = 0;
        barrier = 0;
        downed = true;
        return before;
    }

    public int revive(double ratio) {
        if (!downed) return 0;
        hp = Math.max(1, (int)Math.floor(maxHp() * ratio));
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

    private double cappedStatMod(String id, double cap) { return clamp(statusMagnitude(id), -cap, cap); }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private static String statusKey(String id, String sourceId) { return id + "\u0000" + (sourceId == null ? "" : sourceId); }
    private static String safeSource(String sourceId) { return sourceId == null || sourceId.isBlank() ? "unknown" : sourceId.replace('|', '/'); }
}
