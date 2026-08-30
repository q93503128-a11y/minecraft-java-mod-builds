package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.CharacterSkillRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable combat definition. Character-specific values live in the bundled v0.4 data definitions. */
public record CombatantDefinition(
        String id,
        String name,
        BattleStats stats,
        String basicSkillId,
        List<SkillDefinition> skills,
        int nativeStars,
        List<String> rules,
        Map<String, Double> params
) {
    public CombatantDefinition(String id, String name, BattleStats stats, String basicSkillId, List<SkillDefinition> skills) {
        this(id, name, stats, basicSkillId, skills, 0, List.of(), Map.of());
    }

    public CombatantDefinition {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(stats);
        Objects.requireNonNull(basicSkillId);
        skills = List.copyOf(skills);
        rules = List.copyOf(rules == null ? List.of() : rules);
        params = Map.copyOf(params == null ? Map.of() : params);
        Map<String, SkillDefinition> seen = new LinkedHashMap<>();
        for (SkillDefinition skill : skills) {
            if (seen.put(skill.id(), skill) != null) throw new IllegalArgumentException("Duplicate skill " + skill.id());
        }
        if (!seen.containsKey(basicSkillId) || !seen.get(basicSkillId).isBasic()) {
            throw new IllegalArgumentException("Invalid basic skill " + basicSkillId);
        }
    }

    public SkillDefinition skill(String skillId) {
        String runtimeId = CharacterSkillRegistry.runtimeSkillId(skillId);
        return skills.stream().filter(s -> s.id().equals(runtimeId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown skill " + skillId));
    }

    /** Canonical v0.4 ID for UI/network/persistence boundaries. */
    public String canonicalBasicSkillId() {
        return CharacterSkillRegistry.canonicalSkillId(basicSkillId);
    }

    public String canonicalSkillId(String runtimeOrCanonicalId) {
        String runtimeId = CharacterSkillRegistry.runtimeSkillId(runtimeOrCanonicalId);
        return CharacterSkillRegistry.canonicalSkillId(runtimeId);
    }

    public boolean hasRule(String rule) { return rules.contains(rule); }
    public double param(String key, double fallback) { return params.getOrDefault(key, fallback); }
    public int intParam(String key, int fallback) { return (int)Math.round(params.getOrDefault(key, (double)fallback)); }
    public boolean summon() { return hasRule("SUMMON"); }
    public boolean boss() { return hasRule("BOSS"); }
    public boolean elite() { return hasRule("ELITE"); }
}
