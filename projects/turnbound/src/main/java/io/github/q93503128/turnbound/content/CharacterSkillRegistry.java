package io.github.q93503128.turnbound.content;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Canonical v0.4 Character/Skill ID Registry.
 *
 * JSON/content-facing IDs are the canonical IDs from character wiki 17.6.
 * A small legacy-runtime bridge remains only so older BattleEngine/P0Scenario
 * string literals can be removed incrementally without corrupting canonical data.
 */
public final class CharacterSkillRegistry {
    private static final Map<String, String> CANONICAL_TO_RUNTIME = new LinkedHashMap<>();
    private static final Map<String, String> RUNTIME_TO_CANONICAL = new LinkedHashMap<>();

    static {
        alias("p01_chase_slash", "p01_basic");
        alias("p01_breaker_strike", "p01_shatter");
        alias("p01_duel_lock", "p01_duel_lock");

        alias("p02_accelerate", "p02_basic");
        alias("p02_time_leap", "p02_time_leap");
        alias("p02_delay_field", "p02_delay_field");

        alias("p03_guard_stance", "p03_basic");
        alias("p03_guard_transfer", "p03_guard");
        alias("p03_shield_pressure", "p03_press");

        alias("p04_heal", "p04_basic");
        alias("p04_returned_breath", "p04_revive");
        alias("p04_resting_light", "p04_rest_light");

        alias("p05_suppressive_shot", "p05_basic");
        alias("p05_piercing_shot", "p05_pierce");
        alias("p05_hunt_signal", "p05_hunt_signal");

        alias("p06_echo", "p06_basic");
        alias("p06_condolence", "p06_condolence");
        alias("p06_funeral_order", "p06_funeral_order");

        alias("p07_command", "p07_basic");
        alias("p07_summon_toto", "p07_summon");
        alias("p07_joint_attack", "p07_joint");

        // P08 is intentionally non-mechanical: old p08_frenzy was Active 2,
        // while canonical p08_frenzy is the Basic. Keep both directions explicit.
        alias("p08_frenzy", "p08_basic");
        alias("p08_blood_charge", "p08_blood_rush");
        alias("p08_battle_mania", "p08_frenzy");

        alias("f01_wood_sword", "f01_basic");
        alias("f02_first_aid", "f02_basic");
        alias("f03_shot", "f03_basic");
        alias("f03_focus_shot", "f03_focus_shot");
        alias("f04_shield_push", "f04_basic");
        alias("f04_endure", "f04_endure");
    }

    private CharacterSkillRegistry() {}

    public static String runtimeSkillId(String canonicalId) {
        return CANONICAL_TO_RUNTIME.getOrDefault(canonicalId, canonicalId);
    }

    public static String canonicalSkillId(String runtimeId) {
        return RUNTIME_TO_CANONICAL.getOrDefault(runtimeId, runtimeId);
    }

    public static boolean isCanonicalCharacterSkill(String id) {
        return CANONICAL_TO_RUNTIME.containsKey(id);
    }

    public static Set<String> canonicalActiveSkillIds() {
        return Set.copyOf(CANONICAL_TO_RUNTIME.keySet());
    }

    private static void alias(String canonical, String runtime) {
        if (CANONICAL_TO_RUNTIME.put(canonical, runtime) != null) {
            throw new IllegalStateException("Duplicate canonical skill id " + canonical);
        }
        // Several canonical IDs can only collide if the legacy runtime id was reused.
        // P08 is the known historical collision; map the old Active-2 literal to the
        // canonical Active-2 and let canonical Basic map through the forward table.
        if ("p08_frenzy".equals(runtime)) {
            RUNTIME_TO_CANONICAL.put(runtime, "p08_battle_mania");
        } else {
            RUNTIME_TO_CANONICAL.putIfAbsent(runtime, canonical);
        }
    }
}
