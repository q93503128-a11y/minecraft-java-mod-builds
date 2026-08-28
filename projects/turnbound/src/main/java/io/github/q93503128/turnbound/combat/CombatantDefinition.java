package io.github.q93503128.turnbound.combat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record CombatantDefinition(String id, String name, BattleStats stats, String basicSkillId, List<SkillDefinition> skills) {
    public CombatantDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(name); Objects.requireNonNull(stats); Objects.requireNonNull(basicSkillId); skills = List.copyOf(skills);
        Map<String, SkillDefinition> seen = new LinkedHashMap<>();
        for (SkillDefinition skill : skills) if (seen.put(skill.id(), skill) != null) throw new IllegalArgumentException("Duplicate skill " + skill.id());
        if (!seen.containsKey(basicSkillId) || !seen.get(basicSkillId).isBasic()) throw new IllegalArgumentException("Invalid basic skill " + basicSkillId);
    }
    public SkillDefinition skill(String skillId) { return skills.stream().filter(s -> s.id().equals(skillId)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown skill " + skillId)); }
}
