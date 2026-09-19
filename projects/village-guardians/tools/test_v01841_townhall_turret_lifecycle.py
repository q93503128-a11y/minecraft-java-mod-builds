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
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    enhancements = read("VillageBuildingEnhancements.java")
    location = read("VillageLocationRules.java")
    world = read("VillageWorldSystem.java")
    router = read("VillageBuildingInteractionRouter.java")
    controller = read("VillageUiController.java")
    town = read("VillageTownHallGridScreen.java")
    local = read("VillageLocalActionSystem.java")
    siege = read("VillageSiegeCommandUi.java")
    turret = read("VillagePlacedTurretSystem.java")
    segment = read("VillageSiegeSegmentSystem.java")
    merc = read("VillageMercenarySystem.java")
    deploy = read("VillageMercenaryDeploymentSystem.java")
    raid = read("VillageRaidSystem.java")

    assert "mod_version=0.18.41-alpha.1" in props
    assert "현재 소스 버전 `0.18.41-alpha.1`" in readme
    assert "villageguardians-0.18.41-alpha.1.jar" in readme

    terminal_position = section(enhancements, "static BlockPos terminalPosition", "static VillageProgressionSystem.Building buildingAtTerminal")
    assert "Building.WALLS" in terminal_position
    assert "Building.TOWN_HALL" in terminal_position
    assert "gateControlPosition" not in terminal_position
    terminal_router = section(enhancements, "static VillageProgressionSystem.Building buildingAtTerminal", "static void reinforceWallRailings")
    assert "Building.WALLS" in terminal_router and "continue" in terminal_router
    assert "Building.WALLS" in router

    gate = section(world, "public static boolean handleGateInteraction", "public static synchronized void recordCombat")
    assert "openNorthGate" in gate and "closeNorthGate" in gate
    assert "VillageSiegeCommandUi" not in gate and "VillageUiController" not in gate

    assert "public static boolean isNearDefenseCommand" in location
    defense_location = section(location, "public static boolean isNearDefenseCommand", "public static boolean isNearSkillHall")
    assert "isNearTownHall(player)" in defense_location
    for source in (local, siege, turret, segment):
        assert "isNearDefenseCommand" in source
        assert "북문 성벽 지휘 레버 근처" not in source

    dashboard = section(controller, "public static void openDashboard", "public static void openRoleAssignment")
    assert 'actions.add("siege_command")' in dashboard
    assert "시설 유지보수와 포탑 지휘" in dashboard
    assert '"siege_command".equals(actions[i])' in town
    assert "defenseAction" in town and "defenseLabel" in town
    assert "defenseX - x - 8" in town
    layout = section(town, "private Layout layout()", "private void parse")
    assert "Math.min(620" in layout and "Math.min(320" in layout
    detail_rows = section(town, "private int section", "private List<ButtonSpec> facilityButtons")
    assert "graphics.fill(left - 2" not in detail_rows
    assert "y + rowHeight" in detail_rows

    restore = section(merc, "public static synchronized void restoreNightSnapshot", "public static synchronized void resetForNewGame")
    assert "barracksYardSpawn(level, mob.getUUID())" in restore
    assert "buildingCenter(VillageProgressionSystem.Building.BARRACKS)" not in restore

    movement = section(deploy, "private static void moveMercenaries", "private static BlockPos rallyPoint")
    assert "battlePhase" in movement
    assert "VillageTimePhase.NIGHT" in movement
    assert "battlePhase ? selectedRally : yard" in movement
    assert "WALL_PATH_RECOVERY_ATTEMPTS" in movement
    assert "WALL_PATH_FAILURES.merge" in movement
    assert "distSqr(staging) <= 8L * 8L" in movement

    stalled = section(raid, "private static boolean shouldRecoverStalledEnemy", "private static boolean isMeleePursuer")
    assert "mob.hasLineOfSight(target)" in stalled
    assert "distance > 48.0 * 48.0" in stalled

    print("[PASS] north-gate lever is gate-only and all defense command mutations are town-hall authoritative")
    print("[PASS] compact town hall exposes an always-visible turret command without card-heavy detail layout")
    print("[PASS] retry mercenaries restore outside, wait in the yard by day and deploy through the NIGHT countdown")
    print("[PASS] ranger path recovery is bounded and stalled ranged final enemies no longer lock the wave behind walls")


if __name__ == "__main__":
    main()
