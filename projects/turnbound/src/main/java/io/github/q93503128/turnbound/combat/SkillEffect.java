package io.github.q93503128.turnbound.combat;

public record SkillEffect(EffectType type, double magnitude, int flatValue, int duration) {
    public static SkillEffect damage(double potency) { return new SkillEffect(EffectType.DAMAGE, potency, 0, 0); }
    public static SkillEffect heal(double potency) { return new SkillEffect(EffectType.HEAL, potency, 0, 0); }
    public static SkillEffect barrier(double ratio) { return new SkillEffect(EffectType.BARRIER_MAX_HP, ratio, 0, 0); }
    public static SkillEffect gaugeAdd(int amount) { return new SkillEffect(EffectType.GAUGE_ADD, 0, amount, 0); }
    public static SkillEffect selfGaugeAdd(int amount) { return new SkillEffect(EffectType.SELF_GAUGE_ADD, 0, amount, 0); }
    public static SkillEffect gaugeAtLeast(int amount) { return new SkillEffect(EffectType.GAUGE_AT_LEAST, 0, amount, 0); }
    public static SkillEffect guardRedirect(double ratio, int turns) { return new SkillEffect(EffectType.GUARD_REDIRECT, ratio, 0, turns); }
    public static SkillEffect revive(double ratio) { return new SkillEffect(EffectType.REVIVE, ratio, 0, 0); }
}
