#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
LANG = ROOT / "src/main/resources/assets/villageguardians/lang/ko_kr.json"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def write(name: str, text: str) -> None:
    (JAVA / name).write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 marker, found {count}")
    return text.replace(old, new, 1)


def sub_once(text: str, pattern: str, replacement: str, label: str) -> str:
    result, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 regex match, found {count}")
    return result


# ---------------------------------------------------------------------------
# Version and old contract migration
# ---------------------------------------------------------------------------
props = ROOT / "gradle.properties"
props_text = props.read_text(encoding="utf-8")
props_text = replace_once(
    props_text,
    "mod_version=0.17.17-alpha.1",
    "mod_version=0.17.18-alpha.1",
    "version",
)
props.write_text(props_text, encoding="utf-8")

for test in sorted((ROOT / "tools").glob("test_*.py")):
    source = test.read_text(encoding="utf-8")
    source = source.replace("mod_version=0.17.17-alpha.1", "mod_version=0.17.18-alpha.1")
    source = source.replace("B/U", "B")
    source = source.replace(" · U 빠른 통신", "")
    source = source.replace(
        "assert 'GLFW.GLFW_KEY_J' in keys and 'GLFW.GLFW_KEY_K' in keys and 'GLFW.GLFW_KEY_U' in keys",
        "assert 'GLFW.GLFW_KEY_J' in keys and 'GLFW.GLFW_KEY_K' in keys\n"
        "    assert 'GLFW.GLFW_KEY_U' not in keys and 'CALLER' not in keys",
    )
    test.write_text(source, encoding="utf-8")


# ---------------------------------------------------------------------------
# Real bow draw acceleration: server finishes the prepared bow shot early.
# ---------------------------------------------------------------------------
ability = read("VillageRoleAbilitySystem.java")
ability = replace_once(
    ability,
    "import net.minecraft.world.item.CrossbowItem;\n",
    "",
    "remove crossbow duration import",
)
ability = replace_once(
    ability,
    "import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;\n",
    "",
    "remove ineffective use event import",
)
ability = replace_once(
    ability,
    "    private static final Map<UUID, Long> RAPID_UNTIL = new HashMap<>();\n",
    "    private static final Map<UUID, Long> RAPID_UNTIL = new HashMap<>();\n"
    "    private static final Map<UUID, Integer> RAPID_DRAW_TICKS = new HashMap<>();\n",
    "rapid draw map",
)
ability = replace_once(
    ability,
    "        RAPID_UNTIL.clear();\n",
    "        RAPID_UNTIL.clear();\n        RAPID_DRAW_TICKS.clear();\n",
    "rapid draw reset",
)
ability = replace_once(
    ability,
    "            UUID id = player.getUUID();\n            long spinUntil = SPIN_UNTIL.getOrDefault(id, 0L);",
    "            UUID id = player.getUUID();\n"
    "            tickRapidBow(player, id, now);\n"
    "            long spinUntil = SPIN_UNTIL.getOrDefault(id, 0L);",
    "rapid draw tick call",
)
rapid_method = '''
    private static void tickRapidBow(ServerPlayer player, UUID id, long now) {
        if (RAPID_UNTIL.getOrDefault(id, 0L) < now
                || !player.isUsingItem()
                || !(player.getUseItem().getItem() instanceof BowItem)) {
            RAPID_DRAW_TICKS.remove(id);
            return;
        }
        int usedTicks = RAPID_DRAW_TICKS.merge(id, 1, Integer::sum);
        int specialRank = VillageRoleSkillSystem.specialRank(player, VillageRole.RANGER);
        int completeAt = Math.max(5, 9 - Math.min(4, specialRank));
        if (usedTicks < completeAt) return;

        // ArrowLooseEvent turns this prepared shot into a full-charge shot.
        // Releasing the real use action also resets the client draw animation,
        // unlike only changing LivingEntityUseItemEvent.Tick duration.
        player.releaseUsingItem();
        RAPID_DRAW_TICKS.remove(id);
    }
'''
ability = replace_once(
    ability,
    "\n\n    private static void tickTrackingArrows(MinecraftServer server, long now) {",
    rapid_method + "\n    private static void tickTrackingArrows(MinecraftServer server, long now) {",
    "rapid draw method",
)
ability = replace_once(
    ability,
    "        RAPID_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);\n",
    "        RAPID_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);\n"
    "        RAPID_DRAW_TICKS.keySet().removeIf(id -> RAPID_UNTIL.getOrDefault(id, 0L) < now);\n",
    "rapid draw cleanup",
)
ability = sub_once(
    ability,
    r"\n    public static void handleUseItemTick\(LivingEntityUseItemEvent\.Tick event\) \{.*?\n    \}\n\n    public static void handleArrowLoose",
    "\n    public static void handleArrowLoose",
    "remove ineffective duration handler",
)

# Fire orb direct-contact detection is separate from its explosion radius.
ability = replace_once(
    ability,
    "            List<Mob> hits = targetsNear(level, owner, position, moving.radius(), 40);\n"
    "            boolean expired = entity == null || !entity.isAlive() || blocked || moving.age() >= moving.maxAge();",
    "            double contactRadius = moving.kind() == MovingKind.FIRE_ORB\n"
    "                    ? fireOrbContactRadius(moving.specialRank()) : moving.radius();\n"
    "            List<Mob> hits = targetsNear(level, owner, position, contactRadius, 40);\n"
    "            boolean expired = entity == null || !entity.isAlive() || blocked || moving.age() >= moving.maxAge();",
    "fire orb contact radius",
)
ability = replace_once(
    ability,
    "    private static double areaRadius(double base, int specialRank) {\n",
    "    private static double fireOrbContactRadius(int specialRank) {\n"
    "        return Math.min(1.80, 1.40 + Math.max(0, specialRank) * 0.08);\n"
    "    }\n\n"
    "    private static double areaRadius(double base, int specialRank) {\n",
    "fire orb contact helper",
)
write("VillageRoleAbilitySystem.java", ability)

# Remove the old event bridge now that the real server use action is released.
guard = read("VillageGuardians.java")
guard = replace_once(
    guard,
    "import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;\n",
    "",
    "guard use event import",
)
guard = sub_once(
    guard,
    r"\n    @SubscribeEvent\n    public void onUseItemTick\(LivingEntityUseItemEvent\.Tick event\) \{.*?\n    \}\n",
    "\n",
    "guard use event handler",
)
write("VillageGuardians.java", guard)

# Skill description now states the behavior the player actually sees.
skills = read("VillageRoleSkillSystem.java")
skills = replace_once(
    skills,
    "RANGER_VOLLEY(\"ranger_volley\", VillageRole.RANGER, 0, \"신속 삼연사\", 2, 70, 16, \"기술 사용 후 다음 실제 활·석궁 발사를 대기하며, 그 한 발이 즉시 세 갈래 화살로 강화됩니다.\"),",
    "RANGER_VOLLEY(\"ranger_volley\", VillageRole.RANGER, 0, \"신속 삼연사\", 2, 70, 16, \"기술 사용 후 다음 활은 빠르게 자동 완충·발사되며, 다음 실제 활·석궁 발사 한 번이 세 갈래 화살로 강화됩니다.\"),",
    "rapid skill description",
)
write("VillageRoleSkillSystem.java", skills)


# ---------------------------------------------------------------------------
# Shortcut single source of truth: remove obsolete U duplicate and render the
# player's current bindings instead of hard-coded default letters.
# ---------------------------------------------------------------------------
keys = read("VillageClientKeys.java")
keys = replace_once(
    keys,
    "    private static final KeyMapping CALLER = key(\"caller\", GLFW.GLFW_KEY_U);\n",
    "",
    "remove caller key",
)
keys = replace_once(keys, "        event.register(CALLER);\n", "", "remove caller registration")
keys = replace_once(keys, "            drain(CALLER);\n", "", "remove caller drain")
keys = replace_once(keys, "        consume(CALLER, \"open_quick_chat\");\n", "", "remove caller consume")
key_helpers = '''
    public static String skillOneKeyName() { return keyName(ROLE_SKILL_ONE); }
    public static String skillTwoKeyName() { return keyName(ROLE_SKILL_TWO); }
    public static String quickCommunicationKeyName() { return keyName(QUICK_COMMUNICATION); }
    public static String statusKeyName() { return keyName(STATUS); }
    public static String growthKeyName() { return keyName(GROWTH); }
    public static String roleProgressKeyName() { return keyName(ROLE_PROGRESS); }

    public static String compactSummary() {
        return quickCommunicationKeyName() + " 통신 · "
                + skillOneKeyName() + "/" + skillTwoKeyName() + " 기술";
    }

    private static String keyName(KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }
'''
keys = replace_once(
    keys,
    "\n    private static void migrateLegacySkillBindings(Minecraft minecraft) {",
    key_helpers + "\n    private static void migrateLegacySkillBindings(Minecraft minecraft) {",
    "dynamic key helpers",
)
write("VillageClientKeys.java", keys)

inventory = read("VillageInventoryPanel.java")
inventory = replace_once(
    inventory,
    "        graphics.text(minecraft.font, fit(minecraft, \"B 통신 · Z/X 기술\", layout.width() - 18),\n",
    "        graphics.text(minecraft.font, fit(minecraft, VillageClientKeys.compactSummary(), layout.width() - 18),\n",
    "inventory summary",
)
inventory = replace_once(
    inventory,
    "                layout.left() + 9, firstY, buttonWidth, \"상태 H\", ACCENT);",
    "                layout.left() + 9, firstY, buttonWidth, VillageClientKeys.statusKeyName() + \" 상태\", ACCENT);",
    "inventory status key",
)
inventory = replace_once(
    inventory,
    "                layout.left() + 9 + buttonWidth + gap, firstY, buttonWidth, \"성장 J\", GOLD);",
    "                layout.left() + 9 + buttonWidth + gap, firstY, buttonWidth, VillageClientKeys.growthKeyName() + \" 성장\", GOLD);",
    "inventory growth key",
)
inventory = replace_once(
    inventory,
    "                layout.left() + 9, firstY + 21, buttonWidth, \"직업 K\", ACCENT);",
    "                layout.left() + 9, firstY + 21, buttonWidth, VillageClientKeys.roleProgressKeyName() + \" 직업 성장\", ACCENT);",
    "inventory role key",
)
inventory = replace_once(
    inventory,
    "                layout.left() + 9 + buttonWidth + gap, firstY + 21, buttonWidth, \"통신 U\", GOLD);",
    "                layout.left() + 9 + buttonWidth + gap, firstY + 21, buttonWidth, VillageClientKeys.quickCommunicationKeyName() + \" 통신\", GOLD);",
    "inventory communication key",
)
write("VillageInventoryPanel.java", inventory)

quick = read("VillageQuickChatScreen.java")
quick = replace_once(
    quick,
    "        graphics.text(font, fit(\"B/U 빠른 통신 · 선택 즉시 전송 · ESC 닫기\", Math.max(1, width - 20)),\n",
    "        graphics.text(font, fit(VillageClientKeys.quickCommunicationKeyName()\n"
    "                        + \" 빠른 통신 · 선택 즉시 전송 · ESC 닫기\", Math.max(1, width - 20)),\n",
    "quick chat key label",
)
write("VillageQuickChatScreen.java", quick)

starter = read("VillageStarterKit.java")
starter = replace_once(
    starter,
    "                    \"§6[수호단 조작] §f빠른 통신은 인벤토리 버튼이나 B/U 키로 엽니다. \"\n"
    "                            + \"기본키 Z 기술1 · X 기술2 · B 빠른 통신 · H 상태 · J 성장 · K 직업 성장 · U 빠른 통신\"));",
    "                    \"§6[수호단 조작] §f인벤토리의 상태·성장·직업 성장·통신 버튼을 사용할 수 있습니다. \"\n"
    "                            + \"현재 단축키는 설정 > 조작 > 마을 지키기에서 확인하거나 변경하세요.\"));",
    "starter key notice",
)
write("VillageStarterKit.java", starter)

controller = read("VillageUiController.java")
controller = replace_once(
    controller,
    "                + \"기본키 Z 기술1 · X 기술2 · B 빠른 통신 · H 상태 · J 성장 · K 직업 성장 · U 빠른 통신\";",
    "                + \"단축키  설정 > 조작 > 마을 지키기에서 현재 지정 키 확인·변경\";",
    "status shortcut help",
)
write("VillageUiController.java", controller)

lang = json.loads(LANG.read_text(encoding="utf-8"))
lang.pop("key.villageguardians.caller", None)
lang["key.villageguardians.quick_communication"] = "빠른 통신 열기"
lang["key.villageguardians.status"] = "수호자 상태 열기"
lang["key.villageguardians.personal_progress"] = "공통 성장 열기"
lang["key.villageguardians.role_progress"] = "현재 직업 성장 열기"
LANG.write_text(json.dumps(lang, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

print("Applied Village Guardians v0.17.18 bow draw, fire-orb contact and shortcut consistency fixes")
