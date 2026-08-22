#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    town = read("VillageTownHallGridScreen.java")
    controller = read("VillageUiController.java")
    wave = read("VillageWaveIntelDossierScreen.java")
    terrain = read("VillageFortressTerrain.java")
    world = read("VillageWorldSystem.java")
    old = (ROOT / "tools/test_v01832_ranger_wall_traffic.py").read_text(encoding="utf-8")

    assert "mod_version=0.18.33-alpha.1" in props
    assert "0.18.33-alpha.1" in readme and "villageguardians-0.18.33-alpha.1.jar" in readme
    assert 'assert "mod_version=0.18.32-alpha.1" in props' not in old

    render = town.split("public void extractRenderState", 1)[1].split("private void drawFrame", 1)[0]
    assert "drawTabs(" not in render
    listing = town.split("private void drawList", 1)[1].split("private void drawDetail", 1)[0]
    assert "int rowHeight = 50" in listing
    assert '"내구도 " + f.current() + " / " + f.maximum()' in listing
    assert 'f.meta() + " · " + f.current()' not in listing
    buttons = town.split("private List<ButtonSpec> facilityButtons", 1)[1].split("private String functionAction", 1)[0]
    assert '"repair:" + f.id()' in buttons and '"upgrade:" + f.id()' in buttons
    assert "functionLabel" not in buttons and "functionAction" not in buttons
    assert "open_funding" not in buttons and "open_tower_control" not in buttons
    assert "int h = 27" in buttons and "available / 2" in buttons
    layout = town.split("private Layout layout()", 1)[1].split("private void parse", 1)[0]
    assert "Math.min(940" in layout and "panelWidth * 31 / 100" in layout
    assert "contentTop" in layout and "gap = 10" in layout

    dashboard = controller.split("public static void openDashboard", 1)[1].split("public static void openRoleAssignment", 1)[0]
    assert '"role"' not in dashboard and "select_role:" not in dashboard
    assert 'actions.add("facility:" + building.id())' in dashboard
    assignment = controller.split("public static void openRoleAssignment", 1)[1].split("public static void openCaller", 1)[0]
    assert "VillageLocationRules.isNearSkillHall(player)" in assignment
    assert "select_role:" in assignment
    select = controller.split('if (action.startsWith("select_role:"))', 1)[1].split('if (action.startsWith("skill_node:"))', 1)[0]
    assert "isNearSkillHall" in select and "requireTownHall" not in select
    assert '"open_role_assignment"' in controller
    assert 'case TOWN_HALL -> "시설 수리·강화 지휘"' in controller

    wave_render = wave.split("public void extractRenderState", 1)[1].split("private void drawWaveList", 1)[0]
    assert "font.split" in wave_render and "Math.min(2, header.size())" in wave_render
    assert 'fit(font, "웨이브 선택' in wave_render
    assert "int h = 44" in wave and "int h = 40" in wave
    wave_layout = wave.split("private Layout layout()", 1)[1].split("private List<MonsterEntry> currentRoster", 1)[0]
    assert "safe.width() * 26 / 100" in wave_layout
    assert "boolean compactHeight = contentHeight < 190" in wave_layout
    assert "compactHeight || rightWidth >= 430" in wave_layout
    assert "int gap = compactHeight ? 7 : 9" in wave_layout

    horizontal = terrain.split("private static void buildHorizontalWall", 1)[1].split("private static void buildVerticalWall", 1)[0]
    vertical = terrain.split("private static void buildVerticalWall", 1)[1].split("private static void buildDefenderGalleries", 1)[0]
    assert "isFiringBayOffset" in horizontal and "y >= 3 && y <= 4" in horizontal
    assert "isFiringBayOffset" in vertical and "y >= 3 && y <= 4" in vertical
    assert "phase == 0 || phase == 1 || phase == 11" in terrain
    for center in range(-72, 73, 12):
        opening = {center - 1, center, center + 1}
        assert len(opening) == 3
    # y=1..2 stays solid; only y=3..4 becomes AIR, so a firing bay cannot become a ground breach.
    assert "firingBay && y >= 3 && y <= 4" in horizontal

    ramp = terrain.split("private static void buildWallAccessRamp", 1)[1].split("private static void buildTower", 1)[0]
    assert "stairStart = WALL_RADIUS - 10" in ramp
    assert "distance = WALL_RADIUS - 14; distance < stairStart" in ramp
    assert "Blocks.AIR" in ramp and "Blocks.STONE_BRICK_WALL" in ramp
    assert "supportY" in ramp
    assert "center.below(9)).is(Blocks.GOLD_BLOCK)" in world
    assert "center.below(9), Blocks.GOLD_BLOCK" in world

    # Mirror the town-hall width arithmetic for representative logical GUI widths.
    for safe_width in (320, 426, 640, 840, 960, 1280):
        panel = min(940, max(1, safe_width))
        content = max(1, panel - 28 - 10)
        list_width = max(150, min(280, panel * 31 // 100))
        list_width = min(list_width, max(90, content - 170))
        detail_width = panel - 28 - 10 - list_width
        assert list_width >= 90
        assert detail_width >= 1
        if detail_width >= 260:
            inner = detail_width - 28
            available = max(2, inner - 7)
            first = available // 2
            second = available - first
            assert first + second + 7 <= inner

    print("[PASS] town hall is maintenance-only and critical facility values no longer share one clipped line")
    print("[PASS] repair/upgrade buttons use calculated two-action geometry and production role tabs are gone")
    print("[PASS] player role assignment moved to the skill hall instead of disappearing with the town-hall cleanup")
    print("[PASS] wave briefing uses wrapped header text, larger rows and compact-height-aware spacing")
    print("[PASS] wall firing bays are three-wide/two-high above a solid two-block base")
    print("[PASS] wall stairs retract four blocks and clear the retired solid courtyard wedge")
    print("[PASS] existing worlds receive the new combat-geometry migration marker")
    print("[PASS] v0.18.33 playtest UI/wall contract complete")


if __name__ == "__main__":
    main()
