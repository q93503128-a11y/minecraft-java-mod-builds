package io.github.q93503128.turnbound.content;

import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterSkillRegistryTest {
    private static final Map<String, List<String>> ACTIVE = Map.ofEntries(
            Map.entry("P01", List.of("p01_chase_slash", "p01_breaker_strike", "p01_duel_lock")),
            Map.entry("P02", List.of("p02_accelerate", "p02_time_leap", "p02_delay_field")),
            Map.entry("P03", List.of("p03_guard_stance", "p03_guard_transfer", "p03_shield_pressure")),
            Map.entry("P04", List.of("p04_heal", "p04_returned_breath", "p04_resting_light")),
            Map.entry("P05", List.of("p05_suppressive_shot", "p05_piercing_shot", "p05_hunt_signal")),
            Map.entry("P06", List.of("p06_echo", "p06_condolence", "p06_funeral_order")),
            Map.entry("P07", List.of("p07_command", "p07_summon_toto", "p07_joint_attack")),
            Map.entry("P08", List.of("p08_frenzy", "p08_blood_charge", "p08_battle_mania")),
            Map.entry("F01", List.of("f01_wood_sword")),
            Map.entry("F02", List.of("f02_first_aid")),
            Map.entry("F03", List.of("f03_shot", "f03_focus_shot")),
            Map.entry("F04", List.of("f04_shield_push", "f04_endure")));

    private static final Map<String, List<String>> PASSIVES = Map.ofEntries(
            Map.entry("P01", List.of("p01_relentless_pursuit")),
            Map.entry("P02", List.of("p02_wait_for_slow")),
            Map.entry("P03", List.of("p03_retribution")),
            Map.entry("P04", List.of("p04_last_touch")),
            Map.entry("P05", List.of("p05_exploit_gap")),
            Map.entry("P06", List.of("p06_record_death", "p06_last_page")),
            Map.entry("P07", List.of("p07_broken_contract")),
            Map.entry("P08", List.of("p08_edge_of_cliff")),
            Map.entry("F01", List.of()), Map.entry("F02", List.of()), Map.entry("F03", List.of()), Map.entry("F04", List.of()));

    private static final Map<String, String> AWAKENING = Map.ofEntries(
            Map.entry("P01", "p01_unending_edge"), Map.entry("P02", "p02_fixed_point"),
            Map.entry("P03", "p03_iron_echo"), Map.entry("P04", "p04_place_to_return"),
            Map.entry("P05", "p05_hunt_rhythm"), Map.entry("P06", "p06_rewritten_last_page"),
            Map.entry("P07", "p07_second_contract_awaken"), Map.entry("P08", "p08_undying_frenzy"),
            Map.entry("F01", "f01_awaken"), Map.entry("F02", "f02_awaken"),
            Map.entry("F03", "f03_awaken"), Map.entry("F04", "f04_awaken"));

    @Test
    void bundledCharacterJsonUsesWikiRegistryIdsExactly() {
        Set<String> allActive = new LinkedHashSet<>();
        for (var entry : ACTIVE.entrySet()) {
            var raw = CanonicalData.rawCopy(entry.getKey());
            List<String> active = raw.getAsJsonArray("skills").asList().stream()
                    .map(JsonElement::getAsJsonObject).map(skill -> skill.get("id").getAsString()).toList();
            List<String> passive = raw.getAsJsonArray("passiveIds").asList().stream().map(JsonElement::getAsString).toList();
            assertEquals(entry.getValue(), active, entry.getKey() + " active IDs");
            assertEquals(entry.getValue().getFirst(), raw.get("basicSkillId").getAsString(), entry.getKey() + " basic ID");
            assertEquals(PASSIVES.get(entry.getKey()), passive, entry.getKey() + " passive IDs");
            assertEquals(AWAKENING.get(entry.getKey()), raw.get("awakeningId").getAsString(), entry.getKey() + " awakening ID");
            allActive.addAll(active);
        }
        assertEquals(allActive, CharacterSkillRegistry.canonicalActiveSkillIds());
    }

    @Test
    void p08HistoricalCollisionIsExplicitAndNotMechanicalReplacement() {
        assertEquals("p08_basic", CharacterSkillRegistry.runtimeSkillId("p08_frenzy"));
        assertEquals("p08_frenzy", CharacterSkillRegistry.runtimeSkillId("p08_battle_mania"));
        assertEquals("p08_battle_mania", CharacterSkillRegistry.canonicalSkillId("p08_frenzy"));
        assertTrue(CharacterSkillRegistry.isCanonicalCharacterSkill("p08_frenzy"));
        assertTrue(CharacterSkillRegistry.isCanonicalCharacterSkill("p08_battle_mania"));
    }
}
