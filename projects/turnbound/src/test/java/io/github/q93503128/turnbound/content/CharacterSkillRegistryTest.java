package io.github.q93503128.turnbound.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CharacterSkillRegistryTest {
    private static final Set<String> EXPECTED = Set.of(
            "p01_chase_slash", "p01_breaker_strike", "p01_duel_lock",
            "p02_accelerate", "p02_time_leap", "p02_delay_field",
            "p03_guard_stance", "p03_guard_transfer", "p03_shield_pressure",
            "p04_heal", "p04_returned_breath", "p04_resting_light",
            "p05_suppressive_shot", "p05_piercing_shot", "p05_hunt_signal",
            "p06_echo", "p06_condolence", "p06_funeral_order",
            "p07_command", "p07_summon_toto", "p07_joint_attack",
            "p08_frenzy", "p08_blood_charge", "p08_battle_mania",
            "f01_wood_sword", "f02_first_aid", "f03_shot", "f03_focus_shot",
            "f04_shield_push", "f04_endure");

    @Test
    void canonicalRegistryExactlyMatchesCharacterWikiIds() {
        assertEquals(EXPECTED, CharacterSkillRegistry.canonicalActiveSkillIds());
    }

    @Test
    void canonicalJsonUsesRegistryIdsWithoutRuntimeAliases() throws Exception {
        JsonObject root = JsonParser.parseReader(new InputStreamReader(
                CharacterSkillRegistryTest.class.getResourceAsStream("/data/turnbound/characters/v04.json"),
                StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> jsonIds = new HashSet<>();
        for (JsonElement definitionElement : root.getAsJsonArray("definitions")) {
            JsonObject definition = definitionElement.getAsJsonObject();
            if (!definition.get("id").getAsString().matches("(?:P|F)\\d{2}")) continue;
            String basic = definition.get("basicSkillId").getAsString();
            assertTrue(EXPECTED.contains(basic), basic);
            for (JsonElement skillElement : definition.getAsJsonArray("skills")) {
                String skillId = skillElement.getAsJsonObject().get("id").getAsString();
                assertTrue(EXPECTED.contains(skillId), skillId);
                jsonIds.add(skillId);
            }
        }
        assertEquals(EXPECTED, jsonIds);
    }

    @Test
    void p08CanonicalBasicAndActivesRemainDistinct() {
        assertTrue(CharacterSkillRegistry.isCanonicalCharacterSkill("p08_frenzy"));
        assertTrue(CharacterSkillRegistry.isCanonicalCharacterSkill("p08_blood_charge"));
        assertTrue(CharacterSkillRegistry.isCanonicalCharacterSkill("p08_battle_mania"));
        assertEquals(3, Set.of("p08_frenzy", "p08_blood_charge", "p08_battle_mania").size());
    }
}
