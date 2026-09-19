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
    town = read("VillageTownHallGridScreen.java")
    controller = read("VillageUiController.java")
    merc = read("VillageMercenarySystem.java")
    deploy = read("VillageMercenaryDeploymentSystem.java")

    assert "mod_version=" in props
    assert "현재 소스 버전 `" in readme
    assert "목표 JAR `villageguardians-" in readme

    # Town-hall maintenance should be a compact work surface, not a near-fullscreen empty frame.
    layout = section(town, "private Layout layout()", "private void parse")
    assert "Math.min(620" in layout
    assert "Math.min(320" in layout
    assert "panelWidth * 28 / 100" in layout
    assert "190" in layout

    # Current/next information is grouped and the currency is visible in the action itself.
    detail = section(town, "private void drawFacilityDetail", "private int section")
    assert '"다음 강화"' in detail
    assert '" · 필요 공동 보급품 "' in detail
    cards = section(town, "private int section", "private List<ButtonSpec> facilityButtons")
    assert "rowHeight" in cards
    assert "graphics.fill(left, y + rowHeight" in cards
    buttons = section(town, "private List<ButtonSpec> facilityButtons", "private void drawButton")
    assert '"수리 · 보급 "' in buttons
    assert '"강화 · 보급 "' in buttons
    assert '"단계 / "' in controller

    # New hires appear outside the barracks door; existing trapped mercs are rescued during daytime.
    hire = section(merc, "public static synchronized String hire", "public static synchronized void captureNightSnapshot")
    assert "barracksYardSpawn(level, mercenary.getUUID())" in hire
    yard = section(merc, "static BlockPos barracksYardSpawn", "private static BlockPos safeSpawn")
    assert "VillageBuildingCatalog.entrance" in yard
    assert "relative(outward, 4)" in yard
    safe = section(merc, "private static BlockPos safeSpawn", "private static synchronized void unregister")
    assert "for (int radius = 0; radius <= 8; radius++)" in safe

    movement = section(deploy, "private static void moveMercenaries", "private static BlockPos rallyPoint")
    assert "!battlePhase && insideBarracks" in movement
    assert "battlePhase ? selectedRally : yard" in movement
    assert "VillageMercenarySystem.barracksYardSpawn" in movement
    assert "golem.snapTo" in movement
    assert "private static boolean insideBarracks" in deploy

    print("[PASS] v0.18.40 town-hall UI is compact and mercenaries cannot remain trapped inside barracks")


if __name__ == "__main__":
    main()
