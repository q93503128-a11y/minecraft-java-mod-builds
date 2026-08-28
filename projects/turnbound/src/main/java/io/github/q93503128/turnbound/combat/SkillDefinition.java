package io.github.q93503128.turnbound.combat;

import java.util.List;
import java.util.Objects;

public record SkillDefinition(String id, String name, TargetRule targetRule, int cooldown, List<SkillEffect> effects) {
    public SkillDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(name); Objects.requireNonNull(targetRule); effects = List.copyOf(effects);
        if (id.isBlank() || name.isBlank() || cooldown < 0 || effects.isEmpty()) throw new IllegalArgumentException("Invalid skill " + id);
    }
    public boolean isBasic() { return cooldown == 0; }
}
