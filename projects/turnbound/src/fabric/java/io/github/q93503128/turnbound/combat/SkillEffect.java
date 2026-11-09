package io.github.q93503128.turnbound.combat;

public record SkillEffect(EffectType type, double magnitude, int flatValue, int duration, String key) {
    public SkillEffect(EffectType type, double magnitude, int flatValue, int duration) {
        this(type, magnitude, flatValue, duration, "");
    }

    public SkillEffect { if (key == null) key = ""; }

    public static SkillEffect damage(double potency) { return new SkillEffect(EffectType.DAMAGE, potency, 0, 0); }
    public static SkillEffect heal(double potency) { return new SkillEffect(EffectType.HEAL, potency, 0, 0); }
    public static SkillEffect barrier(double maxHpRatio) { return new SkillEffect(EffectType.BARRIER_MAX_HP, maxHpRatio, 0, 0); }
    public static SkillEffect gaugeAdd(int amount) { return new SkillEffect(EffectType.GAUGE_ADD, 0.0, amount, 0); }
    public static SkillEffect selfGaugeAdd(int amount) { return new SkillEffect(EffectType.SELF_GAUGE_ADD, 0.0, amount, 0); }
    public static SkillEffect gaugeAtLeast(int amount) { return new SkillEffect(EffectType.GAUGE_AT_LEAST, 0.0, amount, 0); }
    public static SkillEffect guardRedirect(double redirectRatio, int targetTurns) { return new SkillEffect(EffectType.GUARD_REDIRECT, redirectRatio, 0, targetTurns); }
    public static SkillEffect defenseUp(double ratio, int ownerTurns) { return new SkillEffect(EffectType.DEFENSE_UP, ratio, 0, ownerTurns); }
    public static SkillEffect revive(double maxHpRatio) { return new SkillEffect(EffectType.REVIVE, maxHpRatio, 0, 0); }
    public static SkillEffect attackMod(double ratio, int turns) { return new SkillEffect(EffectType.ATTACK_MOD, ratio, 0, turns); }
    public static SkillEffect defenseMod(double ratio, int turns) { return new SkillEffect(EffectType.DEFENSE_MOD, ratio, 0, turns); }
    public static SkillEffect speedMod(double ratio, int turns) { return new SkillEffect(EffectType.SPEED_MOD, ratio, 0, turns); }
    public static SkillEffect damageReduction(double ratio, int turns) { return new SkillEffect(EffectType.DAMAGE_REDUCTION, ratio, 0, turns); }
    public static SkillEffect damageTakenMod(double ratio, int turns) { return new SkillEffect(EffectType.DAMAGE_TAKEN_MOD, ratio, 0, turns); }
    public static SkillEffect dotMaxHp(double ratio, int turns) { return new SkillEffect(EffectType.DOT_MAX_HP, ratio, 0, turns); }
    public static SkillEffect selfHpCost(double currentHpRatio) { return new SkillEffect(EffectType.SELF_HP_COST, currentHpRatio, 0, 0); }
    public static SkillEffect mark(String key, int turns) { return new SkillEffect(EffectType.STATUS_MARK, 0.0, 0, turns, key); }
    public static SkillEffect clear(String key) { return new SkillEffect(EffectType.STATUS_CLEAR, 0.0, 0, 0, key); }
    public static SkillEffect noop(String ruleKey) { return new SkillEffect(EffectType.NOOP, 0.0, 0, 0, ruleKey); }
}
