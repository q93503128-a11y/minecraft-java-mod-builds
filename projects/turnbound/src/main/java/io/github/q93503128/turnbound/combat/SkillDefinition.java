package io.github.q93503128.turnbound.combat;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public record SkillDefinition(
        String id,
        String name,
        TargetRule targetRule,
        int cooldown,
        List<SkillEffect> effects,
        String description,
        List<String> rules,
        Map<String, Double> params
) {
    public SkillDefinition(String id, String name, TargetRule targetRule, int cooldown, List<SkillEffect> effects) {
        this(id, name, targetRule, cooldown, effects, defaultDescription(effects), List.of(), Map.of());
    }

    public SkillDefinition(String id, String name, TargetRule targetRule, int cooldown, List<SkillEffect> effects, String description) {
        this(id, name, targetRule, cooldown, effects, description, List.of(), Map.of());
    }

    public SkillDefinition {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(targetRule);
        Objects.requireNonNull(description);
        effects = List.copyOf(effects);
        rules = List.copyOf(rules == null ? List.of() : rules);
        params = Map.copyOf(params == null ? Map.of() : params);
        if (id.isBlank() || name.isBlank() || cooldown < 0 || effects.isEmpty() || description.isBlank()) {
            throw new IllegalArgumentException("Invalid skill " + id);
        }
    }

    public boolean isBasic() { return cooldown == 0 && !rules.contains("NON_BASIC_ZERO_CD"); }
    public boolean hasRule(String rule) { return rules.contains(rule); }
    public double param(String key, double fallback) { return params.getOrDefault(key, fallback); }
    public int intParam(String key, int fallback) { return (int)Math.round(params.getOrDefault(key, (double)fallback)); }

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
                case GUARD_REDIRECT -> joiner.add("단일 직접 피해 " + percent(effect.magnitude()) + " 대신 받기, " + effect.duration() + "행동");
                case DEFENSE_UP -> joiner.add("DEF +" + percent(effect.magnitude()) + ", " + effect.duration() + "행동");
                case REVIVE -> joiner.add("MaxHP " + percent(effect.magnitude()) + "로 부활");
                case ATTACK_MOD -> joiner.add("ATK " + signedPercent(effect.magnitude()) + ", " + effect.duration() + "행동");
                case DEFENSE_MOD -> joiner.add("DEF " + signedPercent(effect.magnitude()) + ", " + effect.duration() + "행동");
                case SPEED_MOD -> joiner.add("SPD " + signedPercent(effect.magnitude()) + ", " + effect.duration() + "행동");
                case DAMAGE_REDUCTION -> joiner.add("피해 감소 " + percent(effect.magnitude()) + ", " + effect.duration() + "행동");
                case DAMAGE_TAKEN_MOD -> joiner.add("받는 피해 " + signedPercent(effect.magnitude()) + ", " + effect.duration() + "행동");
                case DOT_MAX_HP -> joiner.add("MaxHP " + percent(effect.magnitude()) + " 지속 피해 ×" + effect.duration());
                case SELF_HP_COST -> joiner.add("현재 HP " + percent(effect.magnitude()) + " 소모");
                case STATUS_MARK -> joiner.add(effect.key().isBlank() ? "표식 부여" : effect.key() + " 부여");
                case STATUS_CLEAR -> joiner.add(effect.key().isBlank() ? "상태 제거" : effect.key() + " 제거");
                case NOOP -> { }
            }
        }
        String text = joiner.toString();
        return text.isBlank() ? "고유 규칙을 적용합니다." : text + ".";
    }

    private static String percent(double value) { return Math.round(value * 100.0) + "%"; }
    private static String signed(int value) { return value >= 0 ? "+" + value : Integer.toString(value); }
    private static String signedPercent(double value) { return value >= 0 ? "+" + percent(value) : "-" + percent(-value); }
}
