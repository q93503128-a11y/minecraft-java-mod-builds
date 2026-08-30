package io.github.q93503128.turnbound.combat;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public record SkillDefinition(
        String id,
        String name,
        TargetRule targetRule,
        int cooldown,
        List<SkillEffect> effects,
        String description
) {
    public SkillDefinition(String id, String name, TargetRule targetRule, int cooldown, List<SkillEffect> effects) {
        this(id, name, targetRule, cooldown, effects, defaultDescription(effects));
    }

    public SkillDefinition {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(targetRule);
        Objects.requireNonNull(description);
        effects = List.copyOf(effects);
        if (id.isBlank() || name.isBlank() || cooldown < 0 || effects.isEmpty() || description.isBlank()) {
            throw new IllegalArgumentException("Invalid skill " + id);
        }
    }

    public boolean isBasic() { return cooldown == 0; }

    private static String defaultDescription(List<SkillEffect> effects) {
        StringJoiner joiner = new StringJoiner(" · ");
        for (SkillEffect effect : effects) {
            switch (effect.type()) {
                case DAMAGE -> joiner.add("ATK " + percent(effect.magnitude()) + " 피해");
                case HEAL -> joiner.add("ATK " + percent(effect.magnitude()) + " 회복");
                case BARRIER_MAX_HP -> joiner.add("대상 MaxHP " + percent(effect.magnitude()) + " 보호막");
                case GAUGE_ADD -> joiner.add("Turn Gauge " + signed(effect.flatValue()));
                case SELF_GAUGE_ADD -> joiner.add("자신의 Turn Gauge " + signed(effect.flatValue()));
                case GAUGE_AT_LEAST -> joiner.add("Turn Gauge를 최소 " + effect.flatValue() + "까지 증가");
                case GUARD_REDIRECT -> joiner.add("피해 " + percent(effect.magnitude()) + "를 대신 받음, " + effect.duration() + "행동 지속");
                case DEFENSE_UP -> joiner.add("DEF +" + percent(effect.magnitude()) + ", " + effect.duration() + "행동 지속");
                case REVIVE -> joiner.add("MaxHP " + percent(effect.magnitude()) + "로 부활");
            }
        }
        String text = joiner.toString();
        return text.isBlank() ? "스킬 효과를 적용합니다." : text + ".";
    }

    private static String percent(double value) {
        return Math.round(value * 100.0) + "%";
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }
}
