#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]

def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    controller = read("VillageUiController.java")
    town = read("VillageTownHallGridScreen.java")
    local = read("VillageLocalActionSystem.java")
    deploy = read("VillageMercenaryDeploymentSystem.java")
    merc = read("VillageMercenarySystem.java")
    role = read("VillageRoleSkillSystem.java")
    enhance = read("VillageBuildingEnhancements.java")
    router = read("VillageBuildingInteractionRouter.java")
    siege = read("VillageSiegeCommandUi.java")
    segments = read("VillageSiegeSegmentSystem.java")
    turrets = read("VillagePlacedTurretSystem.java")

    assert "mod_version=0.18.34-alpha.1" in props

    dashboard = section(controller, "public static void openDashboard", "public static void openRoleAssignment")
    assert 'actions.add("facility_card:" + building.id())' in dashboard
    assert 'actions.add("facility:" + building.id())' not in dashboard
    buttons = section(town, "private List<ButtonSpec> facilityButtons", "private void drawButton")
    assert '"repair:" + f.id()' in buttons and '"upgrade:" + f.id()' in buttons
    assert "siege_command" not in buttons and "open_mercenary_command" not in buttons

    assert "VillageLocationRules.isNearTownHall(player)" not in deploy
    assert "Building.BARRACKS" in section(deploy, "public static boolean canOpenAt", "public static void openCommand")
    open_class = section(deploy, "public static void openClass", "public static String setDeployment")
    assert "if (!canOpenAt(player))" in open_class
    assert "병영 또는 마을 회관" not in deploy
    retire = section(merc, "public static synchronized String retire", "private static void bastionControl")
    assert "isNearTownHall" not in retire and "Building.BARRACKS" in retire
    assert "병영 또는 마을 회관" not in retire

    assert "마을 회관에서 직업을 먼저 배치" not in role
    assert "기술 연구소에서 직업을 먼저 배치" in role

    legacy = section(local, 'if (action.startsWith("facility:")', "// Compatibility guard")
    assert "구식 시설 바로가기" in legacy and "openBuilding" not in legacy and "VillageSiegeCommandUi.open" not in legacy
    gate = section(local, "private static boolean isSiegeCommandAction", "private static int parseInt")
    for token in ("siege_command", "siege_turret_catalog", "siege_segment_open:", "siege_turret_open:", "tower_open:"):
        assert token in gate
    assert "Building.WALLS" in local
    assert "requiresSiegeCommandAccess" not in local

    terminal = section(enhance, "static VillageProgressionSystem.Building buildingAtTerminal", "static void reinforceWallRailings")
    assert "Building.TOWN_HALL" in terminal
    assert "Building.WALLS" not in terminal
    assert "buildingAtTerminal" in router and "openBuilding(player, building)" in router
    wall_actions = section(controller, "private static void fillLocalActions", "private static String localDescription")
    assert 'case WALLS -> add(actions, labels,' in wall_actions and '"siege_command"' in wall_actions
    wall_desc = section(controller, "private static String localDescription", "private static String managementEffect")
    assert "포탑 건설은 회관" not in wall_desc and "성벽 지휘 레버" in wall_desc

    assert "Town-hall command surface" not in siege
    assert "nearTownHall" not in siege
    assert "nearWallCommand" in siege and "Building.WALLS" in siege
    assert 'actions.add("open_dashboard")' not in siege

    segment_repair = section(segments, "public static String repair", "public static String upgrade")
    segment_upgrade = section(segments, "public static String upgrade", "public static BlockPos attackPoint")
    assert "Building.WALLS" in segment_repair and "Building.WALLS" in segment_upgrade

    for start, end in (
        ("public static String selectPlacement", "public static boolean handlePlacementClick"),
        ("public static synchronized String repair(ServerPlayer player, int id)", "public static synchronized String upgrade"),
        ("public static synchronized String upgrade(ServerPlayer player, int id)", "public static synchronized String dismantle"),
        ("public static synchronized String dismantle(ServerPlayer player, int id)", "public static synchronized String repairAll"),
        ("public static synchronized String repairAll(ServerPlayer player)", "public static void tick"),
    ):
        assert "Building.WALLS" in section(turrets, start, end)
    placement = section(turrets, "public static boolean handlePlacementClick", "public static String cancelPlacement")
    assert 'VillageMaintenanceRules.blockReason("포탑 배치")' in placement
    assert "PENDING.remove(player.getUUID())" in placement

    print("[PASS] town hall emits display-only facility cards and exposes only repair/upgrade actions")
    print("[PASS] mercenary ownership is barracks-only from UI entry through retirement/deployment leaves")
    print("[PASS] wall command lever is the sole normal siege/turret command entry and legacy facility actions are rejected")
    print("[PASS] segment/turret mutation leaves revalidate wall-command location")
    print("[PASS] turret placement confirmation revalidates daytime maintenance phase")
    print("[PASS] stale role guidance now points to the skill hall")

if __name__ == "__main__":
    main()
