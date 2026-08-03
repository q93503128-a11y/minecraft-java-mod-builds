#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
TOOLS = ROOT / "tools"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one marker, found {count}")
    write(path, text.replace(old, new, 1))


# ---------------------------------------------------------------------------
# X standalone versus vanilla X+number toolbar modifier.
# Standalone X fires on release; any hotbar-number press while X is held cancels
# the mod action and leaves the vanilla toolbar chord untouched.
# ---------------------------------------------------------------------------
keys = JAVA / "VillageClientKeys.java"
replace_once(
    keys,
    '''    private static boolean tickListenerRegistered;
    private static boolean bindingsChecked;
''',
    '''    private static boolean tickListenerRegistered;
    private static boolean bindingsChecked;
    private static boolean skillTwoPending;
    private static boolean skillTwoToolbarChord;
''',
    "skill two chord state",
)
replace_once(
    keys,
    '''        consume(ROLE_SKILL_ONE, "use_skill:0");
        consume(ROLE_SKILL_TWO, "use_skill:1");
        consume(QUICK_COMMUNICATION, "open_quick_chat");
''',
    '''        consume(ROLE_SKILL_ONE, "use_skill:0");
        consumeSkillTwo(minecraft);
        consume(QUICK_COMMUNICATION, "open_quick_chat");
''',
    "skill two deferred input",
)
replace_once(
    keys,
    '''            for (KeyMapping mapping : mappings()) drain(mapping);
            return;
''',
    '''            for (KeyMapping mapping : mappings()) drain(mapping);
            skillTwoPending = false;
            skillTwoToolbarChord = false;
            return;
''',
    "screen input state reset",
)
insert = '''    private static void drain(KeyMapping mapping) {
'''
helper = '''    private static void consumeSkillTwo(Minecraft minecraft) {
        while (ROLE_SKILL_TWO.consumeClick()) {
            skillTwoPending = true;
            skillTwoToolbarChord = false;
        }
        if (!skillTwoPending) return;
        for (KeyMapping hotbar : minecraft.options.keyHotbarSlots) {
            if (hotbar.isDown()) {
                skillTwoToolbarChord = true;
                break;
            }
        }
        if (ROLE_SKILL_TWO.isDown()) return;
        if (!skillTwoToolbarChord) {
            ClientPacketDistributor.sendToServer(
                    new VillageNetwork.VillageUiActionPayload("use_skill:1"));
        }
        skillTwoPending = false;
        skillTwoToolbarChord = false;
    }

''' + insert
replace_once(keys, insert, helper, "skill two chord helper")

# ---------------------------------------------------------------------------
# Prevent skill-power double multiplication on generated Ranger projectiles.
# ---------------------------------------------------------------------------
ability = JAVA / "VillageRoleAbilitySystem.java"
replace_once(
    ability,
    '''            spawnSideArrow(level, player, arrow, -8.0, scale.power());
            spawnSideArrow(level, player, arrow, 8.0, scale.power());
            if (scale.specialRank() >= 4) {
                spawnSideArrow(level, player, arrow, -16.0, scale.power() * 0.82f);
                spawnSideArrow(level, player, arrow, 16.0, scale.power() * 0.82f);
''',
    '''            spawnSideArrow(level, player, arrow, -8.0, 1.0f);
            spawnSideArrow(level, player, arrow, 8.0, 1.0f);
            if (scale.specialRank() >= 4) {
                spawnSideArrow(level, player, arrow, -16.0, 0.82f);
                spawnSideArrow(level, player, arrow, 16.0, 0.82f);
''',
    "rapid arrow single power application",
)
replace_once(
    ability,
    '''                float ricochetPower = ricochet == null ? 1.0f : ricochet.power();
                List<Mob> chain = targetsNear(level, attacker, primary.position(),
''',
    '''                List<Mob> chain = targetsNear(level, attacker, primary.position(),
''',
    "remove redundant ricochet power local",
)
replace_once(
    ability,
    '''                float damage = Math.max(2.0f, event.getAmount() * 0.72f * ricochetPower);
''',
    '''                float damage = Math.max(2.0f, event.getAmount() * 0.72f);
''',
    "ricochet single power application",
)
# The outer aegis variable is already in scope inside the sprint branch.
replace_once(
    ability,
    '''                    SkillScale aegis = AEGIS_SCALE.getOrDefault(id, SkillScale.DEFAULT);
                    player.setDeltaMovement(forward.scale(0.78 + Math.min(0.25, (aegis.power() - 1.0f) * 0.22))
''',
    '''                    player.setDeltaMovement(forward.scale(0.78 + Math.min(0.25, (aegis.power() - 1.0f) * 0.22))
''',
    "aegis local redeclaration",
)

# Test-system chat responses are not client payloads, so use slot numbers there
# instead of unresolved client key tokens.
test_system = JAVA / "VillageSkillTestSystem.java"
text = read(test_system)
text = text.replace('safeSlot == 0 ? "{SKILL1}" : "{SKILL2}"', 'safeSlot == 0 ? "1" : "2"')
write(test_system, text)

# ---------------------------------------------------------------------------
# Historical contracts: preserve the intent but point them at the current key
# set and expanded content. Do not weaken gameplay assertions.
# ---------------------------------------------------------------------------
for path in sorted(TOOLS.glob("test_*.py")):
    text = read(path)
    text = text.replace("mod_version=0.17.19-alpha.1", "mod_version=0.18.0-alpha.1")
    text = text.replace("Z/V/B/H/J/K", "Z/X/V/H/J/K")
    text = text.replace("Z/V mappings", "Z/X mappings")
    text = text.replace("Z/V packets", "Z/X packets")
    text = text.replace("Z/V input", "Z/X input")
    text = text.replace("Z/V and", "Z/X and")
    text = text.replace("Z/V", "Z/X")
    text = text.replace(
        'ROLE_SKILL_TWO = key("role_skill_two", GLFW.GLFW_KEY_V)',
        'ROLE_SKILL_TWO = key("role_skill_two", GLFW.GLFW_KEY_X)')
    text = text.replace(
        'QUICK_COMMUNICATION = key("quick_communication", GLFW.GLFW_KEY_B)',
        'QUICK_COMMUNICATION = key("quick_communication", GLFW.GLFW_KEY_V)')
    write(path, text)

# v0.17.13 now verifies current migration rather than the retired v0.17.19 set.
path = TOOLS / "test_v01713_effects_and_keys.py"
text = read(path)
text = text.replace(
    'assert "GLFW.GLFW_KEY_Z" in keys and "GLFW.GLFW_KEY_V" in keys',
    'assert "GLFW.GLFW_KEY_Z" in keys and "GLFW.GLFW_KEY_X" in keys and "GLFW.GLFW_KEY_V" in keys')
text = text.replace(
    "Unsafe vanilla/conflicting bindings migrate to safe Z/X/B/H/J/K and persist",
    "Unsafe vanilla/conflicting bindings migrate to safe Z/X/V/H/J/K and persist")
write(path, text)

# v0.17.18 shortcut meaning changed only for quick communication.
path = TOOLS / "test_v01718_bow_shortcuts.py"
text = read(path)
text = text.replace(
    '"단축키 기능 의미가 B 통신·H 상태·J 공통 성장·K 현재 직업 성장으로 일치합니다"',
    '"단축키 기능 의미가 V 통신·H 상태·J 공통 성장·K 현재 직업 성장으로 일치합니다"')
write(path, text)

# v0.17.19 exact-count audit becomes the v0.18.0 breadth audit.
path = TOOLS / "test_v01719_access_content.py"
text = read(path)
text = text.replace('assert len(offers) == 10, offers', 'assert len(offers) == 24, offers')
text = text.replace('assert len(wave_traits) == 8, wave_traits', 'assert len(wave_traits) == 12, wave_traits')
text = text.replace(
    'print("[PASS] 콘텐츠 감사: 장비 10, 적 14, 웨이브 특성 8, 액티브 기술 20, 무한 일수")',
    'print("[PASS] 콘텐츠 감사: 장비 24, 적 14, 보스 변이 6, 웨이브 특성 12, 액티브 기술 20")')
text = text.replace('CONTENT-AUDIT-v0.17.19.md', 'CONTENT-AUDIT-v0.18.0.md')
for old, new in [
    ('"일반 적 병과 10종"', '"일반 적 병과 10종"'),
    ('"보스 병과 4종"', '"기본 보스 4종"'),
    ('"웨이브 특성 8종"', '"웨이브 특성 12종"'),
    ('"유물 6종"', '"유물 11종"'),
]:
    text = text.replace(old, new)
write(path, text)

# Add the four new deterministic wave traits to the long-running content test.
path = TOOLS / "test_enemy_content.py"
text = read(path)
text = text.replace(
    '"STANDARD", "SWARM", "IRONCLAD", "SIEGE", "HUNTERS", "HEXED", "FRENZY", "REGENERATING"',
    '"STANDARD", "SWARM", "IRONCLAD", "SIEGE", "HUNTERS", "HEXED", "FRENZY", "REGENERATING",\n'
    '        "PHALANX", "BLOOD_MOON", "STORMFRONT", "RIFTED"')
text = text.replace("Eight readable wave traits", "Twelve readable wave traits")
write(path, text)

print("Applied v0.18.0 key-chord, damage and historical-contract corrections")
