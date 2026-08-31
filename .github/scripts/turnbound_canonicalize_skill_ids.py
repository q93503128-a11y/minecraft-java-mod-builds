from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROJECT = ROOT / "projects" / "turnbound"
MAIN = PROJECT / "src" / "main" / "java"
TEST = PROJECT / "src" / "test" / "java"


def replace_exact(path: Path, old: str, new: str, expected: int = 1) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != expected:
        raise SystemExit(f"{path}: expected {expected} exact block(s), found {count}")
    path.write_text(text.replace(old, new), encoding="utf-8")


def p(*parts: str) -> Path:
    return PROJECT.joinpath(*parts)


# P08 is the historical collision: old runtime A2 was p08_frenzy, while canonical
# p08_frenzy is Basic. Never mechanically rewrite this token. Refuse unknown sites.
p08_allowed = {
    p("src", "main", "java", "io", "github", "q93503128", "turnbound", "combat", "P0Scenario.java"),
    p("src", "main", "java", "io", "github", "q93503128", "turnbound", "content", "CharacterSkillRegistry.java"),
    p("src", "test", "java", "io", "github", "q93503128", "turnbound", "content", "CharacterSkillRegistryTest.java"),
}
p08_sites = []
for source_root in (MAIN, TEST):
    for path in source_root.rglob("*.java"):
        for lineno, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if '"p08_frenzy"' in line:
                p08_sites.append((path, lineno, line.strip()))
                if path not in p08_allowed:
                    raise SystemExit(f"Ambiguous p08_frenzy at {path}:{lineno}: {line.strip()}")
print("Pre-migration p08_frenzy sites:")
for path, lineno, line in p08_sites:
    print(f"  {path.relative_to(ROOT)}:{lineno}: {line}")

# Canonical JSON IDs become the internal runtime IDs. No canonical->legacy turnback.
canonical_data = p("src", "main", "java", "io", "github", "q93503128", "turnbound", "content", "CanonicalData.java")
replace_exact(
    canonical_data,
    '        String canonicalBasicId = string(raw, "basicSkillId", "");\n'
    '        String runtimeBasicId = CharacterSkillRegistry.runtimeSkillId(canonicalBasicId);\n'
    '        return new CombatantDefinition(\n'
    '                string(raw, "id", id), string(raw, "name", id),\n'
    '                new BattleStats(hp, attack, defense, speed), runtimeBasicId, skills,\n',
    '        String basicSkillId = string(raw, "basicSkillId", "");\n'
    '        return new CombatantDefinition(\n'
    '                string(raw, "id", id), string(raw, "name", id),\n'
    '                new BattleStats(hp, attack, defense, speed), basicSkillId, skills,\n'
)
replace_exact(
    canonical_data,
    '        String canonicalId = string(raw, "id", "");\n'
    '        String runtimeId = CharacterSkillRegistry.runtimeSkillId(canonicalId);\n'
    '        return new SkillDefinition(\n'
    '                runtimeId, string(raw, "name", ""),\n',
    '        String skillId = string(raw, "id", "");\n'
    '        return new SkillDefinition(\n'
    '                skillId, string(raw, "name", ""),\n'
)

# Definitions and cooldown state now use exact canonical IDs for playable characters;
# generic/enemy skills remain their exact data IDs, with no alias layer.
definition = p("src", "main", "java", "io", "github", "q93503128", "turnbound", "combat", "CombatantDefinition.java")
replace_exact(definition, 'import io.github.q93503128.turnbound.content.CharacterSkillRegistry;\n\n', '', 1)
replace_exact(
    definition,
    '    public SkillDefinition skill(String skillId) {\n'
    '        String runtimeId = CharacterSkillRegistry.runtimeSkillId(skillId);\n'
    '        return skills.stream().filter(s -> s.id().equals(runtimeId)).findFirst()\n'
    '                .orElseThrow(() -> new IllegalArgumentException("Unknown skill " + skillId));\n'
    '    }\n\n'
    '    /** Canonical v0.4 ID for UI/network/persistence boundaries. */\n'
    '    public String canonicalBasicSkillId() {\n'
    '        return CharacterSkillRegistry.canonicalSkillId(basicSkillId);\n'
    '    }\n\n'
    '    public String canonicalSkillId(String runtimeOrCanonicalId) {\n'
    '        String runtimeId = CharacterSkillRegistry.runtimeSkillId(runtimeOrCanonicalId);\n'
    '        return CharacterSkillRegistry.canonicalSkillId(runtimeId);\n'
    '    }\n',
    '    public SkillDefinition skill(String skillId) {\n'
    '        return skills.stream().filter(s -> s.id().equals(skillId)).findFirst()\n'
    '                .orElseThrow(() -> new IllegalArgumentException("Unknown skill " + skillId));\n'
    '    }\n\n'
    '    /** Exact data ID; playable definitions use the canonical v0.4 character-wiki ID. */\n'
    '    public String canonicalBasicSkillId() { return basicSkillId; }\n\n'
    '    public String canonicalSkillId(String skillId) { return skill(skillId).id(); }\n'
)

state = p("src", "main", "java", "io", "github", "q93503128", "turnbound", "combat", "CombatantState.java")
replace_exact(state, 'import io.github.q93503128.turnbound.content.CharacterSkillRegistry;\n\n', '', 1)
replace_exact(
    state,
    '    public void setCooldown(String skillId, int value) {\n'
    '        String runtimeId = CharacterSkillRegistry.runtimeSkillId(skillId);\n'
    '        if (value <= 0) cooldowns.remove(runtimeId); else cooldowns.put(runtimeId, Math.min(9, value));\n'
    '    }\n'
    '    public int cooldown(String skillId) { return cooldowns.getOrDefault(CharacterSkillRegistry.runtimeSkillId(skillId), 0); }\n'
    '    /** Internal runtime view; network boundaries canonicalize these keys explicitly. */\n'
    '    public Map<String, Integer> cooldownsView() { return Map.copyOf(cooldowns); }\n',
    '    public void setCooldown(String skillId, int value) {\n'
    '        String exactId = definition.skill(skillId).id();\n'
    '        if (value <= 0) cooldowns.remove(exactId); else cooldowns.put(exactId, Math.min(9, value));\n'
    '    }\n'
    '    public int cooldown(String skillId) { return cooldowns.getOrDefault(definition.skill(skillId).id(), 0); }\n'
    '    /** Exact skill IDs; playable cooldown keys are canonical v0.4 IDs. */\n'
    '    public Map<String, Integer> cooldownsView() { return Map.copyOf(cooldowns); }\n'
)

# Replace the registry with a canonical set only. The historical dual-ID runtime bridge
# is intentionally deleted so the P08 Basic/A2 collision cannot recur internally.
registry = p("src", "main", "java", "io", "github", "q93503128", "turnbound", "content", "CharacterSkillRegistry.java")
registry.write_text('''package io.github.q93503128.turnbound.content;\n\nimport java.util.Set;\n\n/** Canonical v0.4 playable Character/Skill ID registry from character wiki 17.6. */\npublic final class CharacterSkillRegistry {\n    private static final Set<String> CANONICAL_ACTIVE_SKILLS = Set.of(\n            "p01_chase_slash", "p01_breaker_strike", "p01_duel_lock",\n            "p02_accelerate", "p02_time_leap", "p02_delay_field",\n            "p03_guard_stance", "p03_guard_transfer", "p03_shield_pressure",\n            "p04_heal", "p04_returned_breath", "p04_resting_light",\n            "p05_suppressive_shot", "p05_piercing_shot", "p05_hunt_signal",\n            "p06_echo", "p06_condolence", "p06_funeral_order",\n            "p07_command", "p07_summon_toto", "p07_joint_attack",\n            "p08_frenzy", "p08_blood_charge", "p08_battle_mania",\n            "f01_wood_sword", "f02_first_aid",\n            "f03_shot", "f03_focus_shot",\n            "f04_shield_push", "f04_endure");\n\n    private CharacterSkillRegistry() {}\n\n    public static boolean isCanonicalCharacterSkill(String id) { return CANONICAL_ACTIVE_SKILLS.contains(id); }\n    public static Set<String> canonicalActiveSkillIds() { return CANONICAL_ACTIVE_SKILLS; }\n}\n''', encoding="utf-8")

registry_test = p("src", "test", "java", "io", "github", "q93503128", "turnbound", "content", "CharacterSkillRegistryTest.java")
registry_test.write_text('''package io.github.q93503128.turnbound.content;\n\nimport com.google.gson.JsonElement;\nimport com.google.gson.JsonObject;\nimport com.google.gson.JsonParser;\nimport org.junit.jupiter.api.Test;\n\nimport java.io.InputStreamReader;\nimport java.nio.charset.StandardCharsets;\nimport java.util.HashSet;\nimport java.util.Set;\n\nimport static org.junit.jupiter.api.Assertions.*;\n\nclass CharacterSkillRegistryTest {\n    private static final Set<String> EXPECTED = Set.of(\n            "p01_chase_slash", "p01_breaker_strike", "p01_duel_lock",\n            "p02_accelerate", "p02_time_leap", "p02_delay_field",\n            "p03_guard_stance", "p03_guard_transfer", "p03_shield_pressure",\n            "p04_heal", "p04_returned_breath", "p04_resting_light",\n            "p05_suppressive_shot", "p05_piercing_shot", "p05_hunt_signal",\n            "p06_echo", "p06_condolence", "p06_funeral_order",\n            "p07_command", "p07_summon_toto", "p07_joint_attack",\n            "p08_frenzy", "p08_blood_charge", "p08_battle_mania",\n            "f01_wood_sword", "f02_first_aid", "f03_shot", "f03_focus_shot",\n            "f04_shield_push", "f04_endure");\n\n    @Test\n    void canonicalRegistryExactlyMatchesCharacterWikiIds() {\n        assertEquals(EXPECTED, CharacterSkillRegistry.canonicalActiveSkillIds());\n    }\n\n    @Test\n    void canonicalJsonUsesRegistryIdsWithoutRuntimeAliases() throws Exception {\n        JsonObject root = JsonParser.parseReader(new InputStreamReader(\n                CharacterSkillRegistryTest.class.getResourceAsStream("/data/turnbound/characters/v04.json"),\n                StandardCharsets.UTF_8)).getAsJsonObject();\n        Set<String> jsonIds = new HashSet<>();\n        for (JsonElement definitionElement : root.getAsJsonArray("definitions")) {\n            JsonObject definition = definitionElement.getAsJsonObject();\n            if (!definition.get("id").getAsString().matches("(?:P|F)\\\\d{2}")) continue;\n            String basic = definition.get("basicSkillId").getAsString();\n            assertTrue(EXPECTED.contains(basic), basic);\n            for (JsonElement skillElement : definition.getAsJsonArray("skills")) {\n                String skillId = skillElement.getAsJsonObject().get("id").getAsString();\n                assertTrue(EXPECTED.contains(skillId), skillId);\n                jsonIds.add(skillId);\n            }\n        }\n        assertEquals(EXPECTED, jsonIds);\n    }\n\n    @Test\n    void p08CanonicalBasicAndActivesRemainDistinct() {\n        assertTrue(CharacterSkillRegistry.isCanonicalCharacterSkill("p08_frenzy"));\n        assertTrue(CharacterSkillRegistry.isCanonicalCharacterSkill("p08_blood_charge"));\n        assertTrue(CharacterSkillRegistry.isCanonicalCharacterSkill("p08_battle_mania"));\n        assertEquals(3, Set.of("p08_frenzy", "p08_blood_charge", "p08_battle_mania").size());\n    }\n}\n''', encoding="utf-8")

# Unambiguous playable/fixed-companion legacy literals. Replacement is quote-exact,
# so status/ref IDs that merely contain these strings are not touched.
LITERAL_MAP = {
    '"p01_basic"': '"p01_chase_slash"',
    '"p01_shatter"': '"p01_breaker_strike"',
    '"p02_basic"': '"p02_accelerate"',
    '"p03_basic"': '"p03_guard_stance"',
    '"p03_guard"': '"p03_guard_transfer"',
    '"p03_press"': '"p03_shield_pressure"',
    '"p04_basic"': '"p04_heal"',
    '"p04_revive"': '"p04_returned_breath"',
    '"p04_rest_light"': '"p04_resting_light"',
    '"p05_basic"': '"p05_suppressive_shot"',
    '"p05_pierce"': '"p05_piercing_shot"',
    '"p06_basic"': '"p06_echo"',
    '"p07_basic"': '"p07_command"',
    '"p07_summon"': '"p07_summon_toto"',
    '"p07_joint"': '"p07_joint_attack"',
    '"p08_basic"': '"p08_frenzy"',
    '"p08_blood_rush"': '"p08_blood_charge"',
    '"f01_basic"': '"f01_wood_sword"',
    '"f02_basic"': '"f02_first_aid"',
    '"f03_basic"': '"f03_shot"',
    '"f04_basic"': '"f04_shield_push"',
}
for source_root in (MAIN, TEST):
    for path in source_root.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        changed = text
        for old, new in LITERAL_MAP.items():
            changed = changed.replace(old, new)
        if changed != text:
            path.write_text(changed, encoding="utf-8")

# The only allowed p08_frenzy sites after migration are explicit canonical Basic uses.
p08_after_allowed = {
    p("src", "main", "java", "io", "github", "q93503128", "turnbound", "combat", "P0Scenario.java"),
    p("src", "main", "java", "io", "github", "q93503128", "turnbound", "combat", "BattleEngine.java"),
    p("src", "main", "java", "io", "github", "q93503128", "turnbound", "content", "CharacterSkillRegistry.java"),
    p("src", "test", "java", "io", "github", "q93503128", "turnbound", "content", "CharacterSkillRegistryTest.java"),
}
for source_root in (MAIN, TEST):
    for path in source_root.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        if '"p08_frenzy"' in text and path not in p08_after_allowed:
            raise SystemExit(f"Unexpected canonical/legacy-collision token remains in {path}")

# No Java code may depend on the deleted runtime alias bridge.
for source_root in (MAIN, TEST):
    for path in source_root.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        if "CharacterSkillRegistry.runtimeSkillId" in text or "CharacterSkillRegistry.canonicalSkillId" in text:
            raise SystemExit(f"Legacy registry bridge call remains in {path}")

# No unambiguous legacy playable literal may remain.
legacy_left = []
for source_root in (MAIN, TEST):
    for path in source_root.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        for legacy in LITERAL_MAP:
            if legacy in text:
                legacy_left.append(f"{path.relative_to(ROOT)} -> {legacy}")
if legacy_left:
    raise SystemExit("Legacy playable skill IDs remain:\n" + "\n".join(legacy_left))

print("Canonical skill migration completed with guarded P08 collision handling.")
