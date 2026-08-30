package io.github.q93503128.turnbound.combat;

/** Engine-level effects only. Character-specific conditions are rule IDs from data definitions. */
public enum EffectType {
    DAMAGE,
    HEAL,
    BARRIER_MAX_HP,
    GAUGE_ADD,
    SELF_GAUGE_ADD,
    GAUGE_AT_LEAST,
    GUARD_REDIRECT,
    DEFENSE_UP,
    REVIVE,
    ATTACK_MOD,
    DEFENSE_MOD,
    SPEED_MOD,
    DAMAGE_REDUCTION,
    DAMAGE_TAKEN_MOD,
    DOT_MAX_HP,
    SELF_HP_COST,
    STATUS_MARK,
    STATUS_CLEAR,
    NOOP
}
