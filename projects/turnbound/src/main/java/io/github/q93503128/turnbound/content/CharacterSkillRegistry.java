package io.github.q93503128.turnbound.content;

import java.util.Set;

/** Canonical v0.4 playable Character/Skill ID registry from character wiki 17.6. */
public final class CharacterSkillRegistry {
    private static final Set<String> CANONICAL_ACTIVE_SKILLS = Set.of(
            "p01_chase_slash", "p01_breaker_strike", "p01_duel_lock",
            "p02_accelerate", "p02_time_leap", "p02_delay_field",
            "p03_guard_stance", "p03_guard_transfer", "p03_shield_pressure",
            "p04_heal", "p04_returned_breath", "p04_resting_light",
            "p05_suppressive_shot", "p05_piercing_shot", "p05_hunt_signal",
            "p06_echo", "p06_condolence", "p06_funeral_order",
            "p07_command", "p07_summon_toto", "p07_joint_attack",
            "p08_frenzy", "p08_blood_charge", "p08_battle_mania",
            "f01_wood_sword", "f02_first_aid",
            "f03_shot", "f03_focus_shot",
            "f04_shield_push", "f04_endure");

    private CharacterSkillRegistry() {}

    public static boolean isCanonicalCharacterSkill(String id) { return CANONICAL_ACTIVE_SKILLS.contains(id); }
    public static Set<String> canonicalActiveSkillIds() { return CANONICAL_ACTIVE_SKILLS; }
}
