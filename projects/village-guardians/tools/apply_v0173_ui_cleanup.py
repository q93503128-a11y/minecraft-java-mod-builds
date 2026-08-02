#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
RES = ROOT / "src/main/resources/assets/villageguardians/lang"


def replace(path: Path, old: str, new: str, *, required: bool = True) -> None:
    text = path.read_text(encoding="utf-8")
    if old in text:
        path.write_text(text.replace(old, new), encoding="utf-8")
        return
    if new in text:
        return
    if required:
        raise RuntimeError(f"{path}: replacement target not found: {old[:80]!r}")


def regex_replace(path: Path, pattern: str, replacement: str) -> None:
    text = path.read_text(encoding="utf-8")
    updated, count = re.subn(pattern, replacement, text, flags=re.S)
    if count == 0:
        if replacement.strip() in text:
            return
        raise RuntimeError(f"{path}: regex target not found")
    path.write_text(updated, encoding="utf-8")


def main() -> None:
    controller = JAVA / "VillageUiController.java"
    service = JAVA / "VillageUiService.java"
    starter = JAVA / "VillageStarterKit.java"
    descriptions = JAVA / "VillageActionDescriptions.java"
    role_system = JAVA / "VillageRoleSkillSystem.java"
    town = JAVA / "VillageTownHallScreen.java"
    facility = JAVA / "VillageFacilityScreen.java"
    runtime_test = ROOT / "tools/test_runtime_safety.py"
    depth_test = ROOT / "tools/test_progression_depth.py"
    lang = RES / "ko_kr.json"
    gradle = ROOT / "gradle.properties"

    # Legacy personal-growth entry now routes directly to the tactical tree.
    regex_replace(
        controller,
        r"    public static void openPersonalProgress\(ServerPlayer player\) \{.*?\n    \}\n\n    public static void openSkillTree",
        "    public static void openPersonalProgress(ServerPlayer player) {\n"
        "        openSkillTree(player);\n"
        "    }\n\n"
        "    public static void openSkillTree",
    )
    replace(controller, 'send(player, "skill_tree", "개인 전술 발전", body, actions, labels);',
            'send(player, "skill_tree", "성장", body, actions, labels);')
    replace(controller,
            '"단축키: I 상태 · P 개인 성장 · O 직업 성장 · V 호출기 · C 빠른 통신 · R/G 기술"',
            '"단축키: H 상태 · J 성장 · K 직업 성장 · U 호출기 · B 빠른 통신 · Z/X 기술"')
    replace(controller,
            'List.of("open_status", "open_personal_progress", "open_role_progress_current",',
            'List.of("open_status", "open_skill_tree", "open_role_progress_current",')
    replace(controller, '"상태 (I)|현재 전투 상태와 재화 확인",',
            '"상태 (H)|현재 전투 상태와 재화 확인",')
    replace(controller, '"개인 성장 (P)|개인 전술과 장비 숙련 강화",',
            '"성장 (J)|공용 전술 성장 트리 바로 열기",')
    replace(controller, '"직업 성장 (O)|현재 직업의 세 갈래 성장 확인",',
            '"직업 성장 (K)|현재 직업의 세 갈래 성장 확인",')
    replace(controller, '"빠른 통신 (C)|접속 중인 수호단에게 즉시 신호",',
            '"빠른 통신 (B)|접속 중인 수호단에게 즉시 신호",')
    replace(controller,
            '"I 상태 · P 개인 성장 · O 직업 성장 · V 호출기 · C 통신 · R/G 기술"',
            '"H 상태 · J 성장 · K 직업 성장 · U 호출기 · B 통신 · Z/X 기술"')
    replace(controller,
            '+ " · 성장 노드는 어디서나, 기술 습득·장착은 연구소에서";',
            '+ " · 성장 노드는 어디서나 · 기술 습득은 연구소 · 장착은 어디서나";')
    replace(controller,
            '            case "forge_upgrade" -> {\n'
            '                player.sendSystemMessage(Component.literal("§e" + VillageProgressionSystem.improveForgeRank(player)));\n'
            '                openPersonalProgress(player);\n'
            '            }',
            '            case "forge_upgrade" -> {\n'
            '                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.SMITHY)) {\n'
            '                    player.sendSystemMessage(Component.literal("§c장비 강화는 대장간 단말기 근처에서만 가능합니다."));\n'
            '                } else {\n'
            '                    player.sendSystemMessage(Component.literal("§e" + VillageProgressionSystem.improveForgeRank(player)));\n'
            '                    openBuilding(player, VillageProgressionSystem.Building.SMITHY);\n'
            '                }\n'
            '            }')

    # Skill learning remains laboratory-only, but loadout changes are allowed anywhere.
    replace(service,
            '        if (action.startsWith("role_skill_equip:")) {\n'
            '            if (!requireSkillHall(player, "기술 장착은 기술 연구소에서만 가능합니다.")) return;\n',
            '        if (action.startsWith("role_skill_equip:")) {\n')

    replace(descriptions,
            'return label + "\\n요구 레벨과 수호 주화를 사용해 직업 기술을 습득합니다.";',
            'return label + "\\n기술 연구소에서 요구 레벨과 수호 주화를 사용해 습득합니다.";')
    replace(descriptions,
            'return label + "\\n습득한 기술을 R 또는 G 슬롯에 장착합니다.";',
            'return label + "\\n습득한 기술을 어디서나 Z 또는 X 슬롯에 장착합니다.";')

    replace(role_system, 'return "R: " + first + " | G: " + second;',
            'return "Z: " + first + " | X: " + second;')

    replace(starter,
            '"I 상태 · P 개인 성장 · O 직업 성장 · V 호출기 · C 빠른 통신 · R/G 기술"',
            '"H 상태 · J 성장 · K 직업 성장 · U 호출기 · B 빠른 통신 · Z/X 기술"')

    replace(lang, '"key.villageguardians.personal_progress": "개인 성장 열기"',
            '"key.villageguardians.personal_progress": "성장 열기"')

    # Town hall: less luminous palette and compact controls so information gets the space.
    for old, new in (
        ('PANEL = 0xFFF1E6CF', 'PANEL = 0xFFE4D8BF'),
        ('SURFACE = 0xFFFFFAEE', 'SURFACE = 0xFFF1E9D7'),
        ('SURFACE_ALT = 0xFFE9DCC1', 'SURFACE_ALT = 0xFFD8CBB1'),
        ('SELECTED = 0xFFFFE1A2', 'SELECTED = 0xFFE1C98F'),
        ('private static final int CARD_HEIGHT = 36;', 'private static final int CARD_HEIGHT = 30;'),
        ('private static final int CARD_GAP = 4;', 'private static final int CARD_GAP = 3;'),
        ('private static final int ACTION_HEIGHT = 24;', 'private static final int ACTION_HEIGHT = 20;'),
        ('int tabY = layout.top() + 43;', 'int tabY = layout.top() + 40;'),
        ('inside(mouseX, mouseY, x, y, width, 23)', 'inside(mouseX, mouseY, x, y, width, 18)'),
        ('x + width + 1, y + 24', 'x + width + 1, y + 19'),
        ('x + width, y + 23', 'x + width, y + 18'),
        ('x + width / 2, y + 7', 'x + width / 2, y + 5'),
        ('y + 22, role.current()', 'y + 18, role.current()'),
        ('y + 20, MUTED', 'y + 17, MUTED'),
        ('y + 30, barRight, y + 33', 'y + 25, barRight, y + 28'),
        ('y + 30, barLeft + Math.max(0, fill), y + 33', 'y + 25, barLeft + Math.max(0, fill), y + 28'),
        ('section.major() ? 16 : 13', 'section.major() ? 13 : 11'),
        ('new DetailLine(line, TEXT, 13, first ? 2 : 0)', 'new DetailLine(line, TEXT, 11, first ? 2 : 0)'),
        ('int gap = lines.isEmpty() ? 0 : 7;', 'int gap = lines.isEmpty() ? 0 : 5;'),
        ('Math.min(142, Math.max(82,', 'Math.min(112, Math.max(72,'),
        ('bound.y() + 7, TEXT', 'bound.y() + 5, TEXT'),
        ('tabWidth, 23)', 'tabWidth, 18)'),
        ('int top = layout.top() + 76;', 'int top = layout.top() + 64;'),
    ):
        replace(town, old, new, required=False)

    # Facility screens follow the same density and muted palette.
    for old, new in (
        ('PANEL = 0xFFF1E6CF', 'PANEL = 0xFFE4D8BF'),
        ('SURFACE = 0xFFFFFAEE', 'SURFACE = 0xFFF1E9D7'),
        ('SURFACE_ALT = 0xFFE9DCC1', 'SURFACE_ALT = 0xFFD8CBB1'),
        ('SELECTED = 0xFFFFE1A2', 'SELECTED = 0xFFE1C98F'),
        ('private static final int CARD_HEIGHT = 36;', 'private static final int CARD_HEIGHT = 30;'),
        ('private static final int CARD_GAP = 4;', 'private static final int CARD_GAP = 3;'),
        ('private static final int ACTION_HEIGHT = 24;', 'private static final int ACTION_HEIGHT = 20;'),
        ('Math.min(142, Math.max(86, areas.detailWidth() / 3))',
         'Math.min(108, Math.max(70, areas.detailWidth() / 4))'),
        ('x + 10, y + 21, MUTED', 'x + 10, y + 18, MUTED'),
        ('buttonLeft + buttonWidth / 2, buttonTop + 7, TEXT',
         'buttonLeft + buttonWidth / 2, buttonTop + 5, TEXT'),
        ('width, accent(), 16, 0', 'width, accent(), 13, 0'),
        ('width, TEXT, 13, 3', 'width, TEXT, 11, 2'),
        ('width, MUTED, 13, 8', 'width, MUTED, 11, 5'),
        ('width, accent(), 13, 10', 'width, accent(), 11, 7'),
        ('width, TEXT, 13, 0', 'width, TEXT, 11, 0'),
        ('width, MUTED, 13, 0', 'width, MUTED, 11, 0'),
        ('int top = layout.top() + 48;', 'int top = layout.top() + 43;'),
    ):
        replace(facility, old, new, required=False)

    # Update deterministic contracts for the new interaction model.
    replace(runtime_test, 'assert "CARD_HEIGHT = 36" in facility_ui',
            'assert "CARD_HEIGHT = 30" in facility_ui')
    replace(runtime_test, 'assert "ACTION_HEIGHT = 24" in facility_ui',
            'assert "ACTION_HEIGHT = 20" in facility_ui')
    replace(runtime_test, 'assert "Math.min(142" in facility_ui',
            'assert "Math.min(108" in facility_ui')
    replace(runtime_test, 'assert "CARD_HEIGHT = 36" in town_ui',
            'assert "CARD_HEIGHT = 30" in town_ui')
    replace(runtime_test, 'assert "Math.min(142" in town_ui',
            'assert "Math.min(112" in town_ui')
    replace(runtime_test, 'assert \'"open_personal_progress"\' in inventory',
            'assert \'"open_skill_tree"\' in inventory')
    replace(runtime_test, 'assert "C 통신 · R/G 기술" in inventory',
            'assert "B 통신 · Z/X 기술" in inventory')
    replace(runtime_test,
            'for key in ("GLFW_KEY_I", "GLFW_KEY_P", "GLFW_KEY_O", "GLFW_KEY_V", "GLFW_KEY_C"):',
            'for key in ("GLFW_KEY_Z", "GLFW_KEY_X", "GLFW_KEY_B", "GLFW_KEY_H", "GLFW_KEY_J", "GLFW_KEY_K", "GLFW_KEY_U"):')
    insert_after = '    assert "개인 장비 피해 보정 강화" in controller\n'
    extra = (
        '    assert "openPersonalProgress(ServerPlayer player)" in controller\n'
        '    assert "openSkillTree(player);" in controller\n'
        '    assert "장비 강화는 대장간 단말기 근처에서만 가능합니다." in controller\n'
        '    assert "기술 장착은 기술 연구소에서만 가능합니다." not in read("VillageUiService.java")\n'
        '    assert "직업 기술 습득은 기술 연구소에서만 가능합니다." in read("VillageUiService.java")\n'
        '    assert "TreeBubble" in read("VillageRoleProgressScreen.java")\n'
        '    assert "SkillBubble" in read("VillageRoleProgressScreen.java")\n'
        '    assert "renderDetail" not in read("VillageSkillTreeScreen.java")\n'
    )
    text = runtime_test.read_text(encoding="utf-8")
    if extra.strip() not in text:
        if insert_after not in text:
            raise RuntimeError("runtime test insertion anchor missing")
        runtime_test.write_text(text.replace(insert_after, insert_after + extra), encoding="utf-8")

    replace(depth_test, 'assert "double distance = 92.0" in common_ui',
            'assert "double distance = 86.0" in common_ui')
    replace(depth_test, 'assert "savedZoom = 0.82" in common_ui',
            'assert "savedZoom = 0.74" in common_ui')
    replace(depth_test, 'assert "double y = -tier * 88.0" in role_ui',
            'assert "double y = -tier * 76.0" in role_ui')
    replace(depth_test, 'assert "savedZoom = 0.86" in role_ui',
            'assert "savedZoom = 0.74" in role_ui')
    replace(depth_test, 'assert "ACTION_HEIGHT = 24" in shop_ui',
            'assert "ACTION_HEIGHT = 24" in shop_ui', required=False)
    anchor = '    assert "savedZoom = 0.74" in role_ui\n'
    add = (
        '    assert "renderDetail" not in common_ui\n'
        '    assert "Bubble" in common_ui\n'
        '    assert "TreeBubble" in role_ui and "SkillBubble" in role_ui\n'
        '    assert "renderTreeFooter" not in role_ui and "renderSkillFooter" not in role_ui\n'
    )
    text = depth_test.read_text(encoding="utf-8")
    if add.strip() not in text:
        depth_test.write_text(text.replace(anchor, anchor + add), encoding="utf-8")

    replace(gradle, 'mod_version=0.17.2-alpha.1', 'mod_version=0.17.3-alpha.1')

    print("[PATCHED] v0.17.3 compact growth, smithy and skill-management cleanup")


if __name__ == "__main__":
    main()
