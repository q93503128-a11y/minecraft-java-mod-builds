#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
TOOLS = ROOT / "tools"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def patch_action_layout() -> None:
    path = TOOLS / "test_action_layout.py"
    text = read(path)
    old_geometry = '''def town_geometry(width: int, height: int) -> tuple[int, int, int]:
    safe_left, _, safe_right, _ = safe_rect(width, height)
    safe_width = safe_right - safe_left
    panel_width = min(820, max(300, safe_width - 12))
    panel_width = min(panel_width, safe_width)
    gap = 8
    content_width = max(1, panel_width - 28 - gap)
    list_width = clamp(panel_width * 27 // 100, 110, 205)
    list_width = min(list_width, max(90, content_width - 120))
    detail_width = panel_width - 28 - gap - list_width
    return panel_width, list_width, detail_width


def facility_button_widths(detail_width: int) -> list[int]:
    inner = max(1, detail_width - 28)
    if detail_width < 230:
        return [inner, inner, inner]
    gap = 5
    available = max(3, inner - gap * 2)
    base = max(1, available // 3)
    remainder = max(0, available - base * 3)
    return [base + (1 if remainder > 0 else 0),
            base + (1 if remainder > 1 else 0), base]
'''
    new_geometry = '''def town_geometry(width: int, height: int) -> tuple[int, int, int]:
    safe_left, _, safe_right, _ = safe_rect(width, height)
    safe_width = safe_right - safe_left
    panel_width = min(940, max(1, safe_width))
    gap = 10
    content_width = max(1, panel_width - 28 - gap)
    list_width = clamp(panel_width * 31 // 100, 150, 280)
    list_width = min(list_width, max(90, content_width - 170))
    detail_width = panel_width - 28 - gap - list_width
    return panel_width, list_width, detail_width


def facility_button_widths(detail_width: int) -> list[int]:
    inner = max(1, detail_width - 28)
    if detail_width < 260:
        return [inner, inner]
    gap = 7
    available = max(2, inner - gap)
    first = available // 2
    return [first, available - first]
'''
    text = replace_once(text, old_geometry, new_geometry, "action layout town geometry")
    text = replace_once(text,
'''    assert 'FACILITIES("시설 관리")' in TOWN and 'ROLES("직업 배치")' in TOWN
    assert '"repair:" + f.id()' in TOWN and '"upgrade:" + f.id()' in TOWN
    assert "pane.width() < 230" in TOWN
    assert "actionTop" in TOWN and "enableScissor" in TOWN
    assert "Math.max(48, available / count)" not in TOWN
''',
'''    render = TOWN.split("public void extractRenderState", 1)[1].split("private void drawFrame", 1)[0]
    buttons = TOWN.split("private List<ButtonSpec> facilityButtons", 1)[1].split("private String functionAction", 1)[0]
    assert "drawTabs(" not in render
    assert '"repair:" + f.id()' in buttons and '"upgrade:" + f.id()' in buttons
    assert "functionLabel" not in buttons and "functionAction" not in buttons
    assert "pane.width() < 260" in buttons
    assert "actionTop" in TOWN and "enableScissor" in TOWN
    assert "Math.max(48, available / count)" not in TOWN
''', "action layout town assertions")
    text = replace_once(text,
'''        if detail >= 230:
            assert sum(button_widths) + 10 <= detail - 28, (width, detail, button_widths)
        else:
            assert max(button_widths) <= detail - 28, (width, detail, button_widths)
''',
'''        if detail >= 260:
            assert sum(button_widths) + 7 <= detail - 28, (width, detail, button_widths)
        else:
            assert max(button_widths) <= detail - 28, (width, detail, button_widths)
''', "action layout button arithmetic")
    text = replace_once(text,
'''    print("[PASS] Town-hall function/repair/upgrade buttons stay inside narrow detail panes")''',
'''    print("[PASS] Town-hall repair/upgrade-only controls stay inside calculated narrow detail panes")''',
"action layout pass text")
    write(path, text)


def patch_interaction_contract() -> None:
    path = TOOLS / "test_interaction_contract.py"
    text = read(path)
    text = replace_once(text,
'''    # Town hall list clicks only select. Explicit bottom controls own function/repair/upgrade.
    assert "VillageConfirmScreen" in town
    assert "FacilityCard" in town and "functionAction(f)" in town
    assert '"repair:" + f.id()' in town and '"upgrade:" + f.id()' in town
    assert 'FACILITIES("시설 관리")' in town and 'ROLES("직업 배치")' in town
''',
'''    # Town hall list only selects a building. Repair/upgrade are the only executable hall actions.
    assert "VillageConfirmScreen" in town
    assert "FacilityCard" in town
    buttons = town.split("private List<ButtonSpec> facilityButtons", 1)[1].split("private String functionAction", 1)[0]
    assert '"repair:" + f.id()' in buttons and '"upgrade:" + f.id()' in buttons
    assert "functionAction" not in buttons and "open_funding" not in buttons and "open_tower_control" not in buttons
    dashboard = controller.split("public static void openDashboard", 1)[1].split("public static void openRoleAssignment", 1)[0]
    assert "select_role:" not in dashboard and '"role"' not in dashboard
    assert "openRoleAssignment" in controller and "VillageLocationRules.isNearSkillHall(player)" in controller
''', "interaction town contract")
    text = replace_once(text,
'''    print("[PASS] Facility cards route to explicit function/repair/upgrade controls")''',
'''    print("[PASS] Town-hall facility cards expose repair/upgrade only; local functions remain local")''',
"interaction pass text")
    write(path, text)


def patch_runtime_safety() -> None:
    path = TOOLS / "test_runtime_safety.py"
    text = read(path)
    text = replace_once(text,
'''    assert 'FACILITIES("시설 관리")' in town_ui and 'ROLES("직업 배치")' in town_ui
    assert '"repair:" + f.id()' in town_ui and '"upgrade:" + f.id()' in town_ui
''',
'''    town_render = town_ui.split("public void extractRenderState", 1)[1].split("private void drawFrame", 1)[0]
    town_buttons = town_ui.split("private List<ButtonSpec> facilityButtons", 1)[1].split("private String functionAction", 1)[0]
    assert "drawTabs(" not in town_render
    assert '"repair:" + f.id()' in town_buttons and '"upgrade:" + f.id()' in town_buttons
    assert "functionAction" not in town_buttons and "open_funding" not in town_buttons
''', "runtime town assertions")
    text = replace_once(text,
'''    assert "수리·강화·포탑 건설은 회관" in controller
''',
'''    assert "수리·강화·포탑 건설은 회관" in controller
    dashboard = controller.split("public static void openDashboard", 1)[1].split("public static void openRoleAssignment", 1)[0]
    assert "select_role:" not in dashboard and '"role"' not in dashboard
    assert "openRoleAssignment" in controller and "직업 배치는 기술 연구소" in controller
''', "runtime controller ownership")
    text = replace_once(text,
'''    print("[PASS] Town hall exposes explicit facility function, repair and upgrade controls")''',
'''    print("[PASS] Town hall exposes repair/upgrade only while facility functions remain location-authoritative")''',
"runtime pass text")
    write(path, text)


def patch_ui_layout_contract() -> None:
    path = TOOLS / "test_ui_layout_contract.py"
    text = read(path)
    text = replace_once(text,
'''        for max_width, max_height in ((820, 430), (760, 360)):
            x0, y0, x1, y1 = centered_panel(width, height, max_width, max_height)
            assert x0 >= left and y0 >= top
            assert x1 <= right and y1 <= bottom
''',
'''        # Generic action screen remains centered; town hall intentionally consumes more of the safe viewport.
        x0, y0, x1, y1 = centered_panel(width, height, 760, 360)
        assert x0 >= left and y0 >= top
        assert x1 <= right and y1 <= bottom
        town_width = min(940, max(1, right - left))
        town_height = min(500, max(1, bottom - top))
        town_left = left + (right - left) // 2 - town_width // 2
        town_top = top + max(0, ((bottom - top) - town_height) // 2)
        assert town_left >= left and town_top >= top
        assert town_left + town_width <= right and town_top + town_height <= bottom
''', "ui safe panel arithmetic")
    text = replace_once(text,
'''    assert "panelWidth = Math.min(820" in town and "panelHeight = Math.min(430" in town
''',
'''    assert "panelWidth = Math.min(940" in town and "panelHeight = Math.min(500" in town
    assert "panelWidth * 31 / 100" in town and "gap = 10" in town
''', "ui town size source")
    write(path, text)


def patch_fortress_layout() -> None:
    path = TOOLS / "test_fortress_layout.py"
    text = read(path)
    text = replace_once(text,
'''    stair_z = [-FORTRESS_RADIUS + 14 - step for step in range(9)]
    landing_z = list(range(-FORTRESS_RADIUS + 1, -FORTRESS_RADIUS + 7))
    assert stair_z[-1] == -FORTRESS_RADIUS + 6
    assert stair_z[-1] in landing_z
''',
'''    stair_z = [-FORTRESS_RADIUS + 10 - step for step in range(9)]
    landing_z = list(range(-FORTRESS_RADIUS + 1, -FORTRESS_RADIUS + 7))
    assert stair_z[0] == -FORTRESS_RADIUS + 10
    assert stair_z[-1] == -FORTRESS_RADIUS + 2
    assert any(z in landing_z for z in stair_z)
    assert max(stair_z) <= -FORTRESS_RADIUS + 10
''', "fortress stair retraction")
    write(path, text)


def preserve_historical_phrase() -> None:
    path = JAVA / "VillageWorldSystem.java"
    text = read(path)
    text = replace_once(text,
'''§6[마을 정비] §f성벽 사격구·접근 계단·포좌 동선을 최신 실전 배치로 갱신합니다.''',
'''§6[마을 정비] §f성벽 4면 접근 계단·사격구·포좌 동선을 최신 실전 배치로 갱신합니다.''',
"migration wording")
    write(path, text)


def main() -> None:
    props = read(ROOT / "gradle.properties")
    if "mod_version=0.18.33-alpha.1" not in props:
        raise RuntimeError("contract migration must run after v0.18.33 source patch")
    patch_action_layout()
    patch_interaction_contract()
    patch_runtime_safety()
    patch_ui_layout_contract()
    patch_fortress_layout()
    preserve_historical_phrase()
    print("[PATCH] v0.18.33 regression contracts migrated to maintenance-only town hall and open wall geometry")


if __name__ == "__main__":
    main()
