package io.github.q93503128.turnbound.presentation;

/**
 * Presentation-only lookup for the eight v0.4 core heroes.
 * Canonical skill IDs stay owned by combat data; this table only chooses a visual grammar.
 */
final class HeroSkillVfxStyle {
    enum Style {
        P01_BASIC, P01_BREAKER, P01_CHAIN_RUSH,
        P02_BASIC, P02_TIME_LEAP, P02_CLOCK_REVERSAL,
        P03_BASIC, P03_GUARD_TRANSFER, P03_COUNTER_FIELD,
        P04_BASIC, P04_RETURNED_BREATH, P04_LAST_PRAYER,
        P05_BASIC, P05_PURSUIT_MARK, P05_FINISHER,
        P06_BASIC, P06_MEMORY_CUT, P06_GRAVE_RETURN,
        P07_BASIC, P07_SUMMON_TOTO, P07_JOINT_ATTACK,
        P08_BASIC, P08_BLOOD_CHARGE, P08_BATTLE_MANIA,
        GENERIC
    }

    private HeroSkillVfxStyle() {}

    static Style resolve(String heroId, String skillId) {
        if (heroId == null || skillId == null) return Style.GENERIC;
        return switch (heroId + "|" + skillId) {
            case "P01|p01_basic_dual_slash" -> Style.P01_BASIC;
            case "P01|p01_breaker_strike" -> Style.P01_BREAKER;
            case "P01|p01_chain_rush" -> Style.P01_CHAIN_RUSH;
            case "P02|p02_basic_time_cut" -> Style.P02_BASIC;
            case "P02|p02_time_leap" -> Style.P02_TIME_LEAP;
            case "P02|p02_clock_reversal" -> Style.P02_CLOCK_REVERSAL;
            case "P03|p03_basic_shield_bash" -> Style.P03_BASIC;
            case "P03|p03_guard_transfer" -> Style.P03_GUARD_TRANSFER;
            case "P03|p03_counter_field" -> Style.P03_COUNTER_FIELD;
            case "P04|p04_basic_warm_light" -> Style.P04_BASIC;
            case "P04|p04_returned_breath" -> Style.P04_RETURNED_BREATH;
            case "P04|p04_last_prayer" -> Style.P04_LAST_PRAYER;
            case "P05|p05_basic_hunting_shot" -> Style.P05_BASIC;
            case "P05|p05_pursuit_mark" -> Style.P05_PURSUIT_MARK;
            case "P05|p05_finisher" -> Style.P05_FINISHER;
            case "P06|p06_basic_epitaph" -> Style.P06_BASIC;
            case "P06|p06_memory_cut" -> Style.P06_MEMORY_CUT;
            case "P06|p06_grave_return" -> Style.P06_GRAVE_RETURN;
            case "P07|p07_basic_command" -> Style.P07_BASIC;
            case "P07|p07_summon_toto" -> Style.P07_SUMMON_TOTO;
            case "P07|p07_joint_attack" -> Style.P07_JOINT_ATTACK;
            case "P08|p08_basic_swing" -> Style.P08_BASIC;
            case "P08|p08_blood_charge" -> Style.P08_BLOOD_CHARGE;
            case "P08|p08_battle_mania" -> Style.P08_BATTLE_MANIA;
            default -> Style.GENERIC;
        };
    }
}
