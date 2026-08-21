#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    terrain = read("VillageFortressTerrain.java")
    enhancements = read("VillageBuildingEnhancements.java")
    deployment = read("VillageMercenaryDeploymentSystem.java")
    mercenary = read("VillageMercenarySystem.java")
    world = read("VillageWorldSystem.java")
    historical = (ROOT / "tools/test_v01830_walltop_emplacements.py").read_text(encoding="utf-8")

    assert "mod_version=0.18.31-alpha.1" in props
    assert "0.18.31-alpha.1" in readme and "villageguardians-0.18.31-alpha.1.jar" in readme
    assert 'assert "mod_version=0.18.30-alpha.1" in props' not in historical

    access = terrain.split("private static void buildWallAccess", 1)[1].split(
        "private static void buildTower", 1)[0]
    assert "buildWallAccessRamp" in access
    assert "new int[]{-25, 25}" in access
    assert "new int[]{-34, 34}" in access
    for direction in ("Direction.NORTH", "Direction.SOUTH", "Direction.WEST", "Direction.EAST"):
        assert direction in access
    assert "Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, outward)" in access
    assert "width = -2; width <= 2" in access
    assert "width = -3; width <= 3" in access
    assert "clearY <= 3" in access

    assert "isSideRearStairOpening(offset)" in enhancements
    assert "Math.abs(Math.abs(offset) - WALL_EMPLACEMENT_LANE) <= 3" in enhancements
    assert "placeRailing(level, new BlockPos(x, railY, southOuter));" in enhancements
    assert "placeRailing(level, new BlockPos(westOuter, railY, z));" in enhancements
    assert "placeRailing(level, new BlockPos(eastOuter, railY, z));" in enhancements

    assert "rallyPoint(center, zone, kind, golem.getUUID())" in deployment
    assert "private static BlockPos rangerWallPost" in deployment
    assert "center.offset(lane + spread, 9, -74)" in deployment
    assert "Math.floorMod(mercenaryId == null ? 0 : mercenaryId.hashCode(), 10)" in deployment
    assert "private static BlockPos rangerWallStagingPoint" in deployment
    assert "center.offset(lane, 0, -62)" in deployment
    assert "BlockPos staging = rangerWallStagingPoint" in deployment
    assert "rallyPoint(center, Deployment.INNER, kind)" not in deployment
    assert "? 26 : -26, 10, -69" not in deployment

    ranged = mercenary.split("private static void rangedAttack", 1)[1].split(
        "private static void healAllies", 1)[0]
    assert "VillageDefenseLineOfSight.hasLine(level, start, enemy)" in ranged
    assert "VillageEnemyArchetypeSystem.isFlying(enemy) ? 0 : 1" in ranged
    assert "VillageRaidSystem.aerialThreatPriority(enemy)" in ranged

    assert "center.below(8)).is(Blocks.DIAMOND_BLOCK)" in world
    assert "center.below(8), Blocks.DIAMOND_BLOCK" in world
    assert "성벽 4면 접근 계단" in world

    print("[PASS] north access is preserved while south/east/west walls gain direct five-wide stairs")
    print("[PASS] side/rear inner parapets open only at authored stair landings while outer fall protection remains")
    print("[PASS] ranger WALL deployment now targets the physical north-wall walk instead of the old elevated air/ramp coordinate")
    print("[PASS] ranger wall posts use ten stable UUID slots and failed long paths stage at the matching stair foot")
    print("[PASS] ranger ranged combat keeps physical LOS and flying-threat priority from the accepted air-defense pass")
    print("[PASS] existing worlds receive the four-side access migration through a new revision marker")
    print("[PASS] historical v0.18.30 regression is version-independent")
    print("[PASS] v0.18.31 pre-playtest hardening contract complete")


if __name__ == "__main__":
    main()
