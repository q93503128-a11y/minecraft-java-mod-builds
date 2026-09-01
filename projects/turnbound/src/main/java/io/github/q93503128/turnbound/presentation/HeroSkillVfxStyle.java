package io.github.q93503128.turnbound.presentation;

/**
 * Presentation-only lookup for the eight v0.4 core heroes.
 * Canonical skill IDs stay owned by combat data; this table only chooses a visual grammar.
 */
final class HeroSkillVfxStyle {
    enum Style {
        P01_CHASE_SLASH, P01_BREAKER_STRIKE, P01_DUEL_LOCK,
        P02_ACCELERATE, P02_TIME_LEAP, P02_DELAY_FIELD,
        P03_GUARD_STANCE, P03_GUARD_TRANSFER, P03_SHIELD_PRESSURE,
        P04_HEAL, P04_RETURNED_BREATH, P04_RESTING_LIGHT,
        P05_SUPPRESSIVE_SHOT, P05_PIERCING_SHOT, P05_HUNT_SIGNAL,
        P06_ECHO, P06_CONDOLENCE, P06_FUNERAL_ORDER,
        P07_COMMAND, P07_SUMMON_TOTO, P07_JOINT_ATTACK,
        P08_FRENZY, P08_BLOOD_CHARGE, P08_BATTLE_MANIA,
        GENERIC
    }

    private HeroSkillVfxStyle() {}

    static Style resolve(String heroId, String skillId) {
        if (heroId == null || skillId == null) return Style.GENERIC;
        return switch (heroId + "|" + skillId) {
            case "P01|p01_chase_slash" -> Style.P01_CHASE_SLASH;
            case "P01|p01_breaker_strike" -> Style.P01_BREAKER_STRIKE;
            case "P01|p01_duel_lock" -> Style.P01_DUEL_LOCK;
            case "P02|p02_accelerate" -> Style.P02_ACCELERATE;
            case "P02|p02_time_leap" -> Style.P02_TIME_LEAP;
            case "P02|p02_delay_field" -> Style.P02_DELAY_FIELD;
            case "P03|p03_guard_stance" -> Style.P03_GUARD_STANCE;
            case "P03|p03_guard_transfer" -> Style.P03_GUARD_TRANSFER;
            case "P03|p03_shield_pressure" -> Style.P03_SHIELD_PRESSURE;
            case "P04|p04_heal" -> Style.P04_HEAL;
            case "P04|p04_returned_breath" -> Style.P04_RETURNED_BREATH;
            case "P04|p04_resting_light" -> Style.P04_RESTING_LIGHT;
            case "P05|p05_suppressive_shot" -> Style.P05_SUPPRESSIVE_SHOT;
            case "P05|p05_piercing_shot" -> Style.P05_PIERCING_SHOT;
            case "P05|p05_hunt_signal" -> Style.P05_HUNT_SIGNAL;
            case "P06|p06_echo" -> Style.P06_ECHO;
            case "P06|p06_condolence" -> Style.P06_CONDOLENCE;
            case "P06|p06_funeral_order" -> Style.P06_FUNERAL_ORDER;
            case "P07|p07_command" -> Style.P07_COMMAND;
            case "P07|p07_summon_toto" -> Style.P07_SUMMON_TOTO;
            case "P07|p07_joint_attack" -> Style.P07_JOINT_ATTACK;
            case "P08|p08_frenzy" -> Style.P08_FRENZY;
            case "P08|p08_blood_charge" -> Style.P08_BLOOD_CHARGE;
            case "P08|p08_battle_mania" -> Style.P08_BATTLE_MANIA;
            default -> Style.GENERIC;
        };
    }
}
