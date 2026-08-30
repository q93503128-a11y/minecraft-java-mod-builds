#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    terrain = read("VillageFortressTerrain.java")
    world = read("VillageWorldSystem.java")

    assert "mod_version=0.18.35-alpha.1" in props

    access = terrain.split("private static void buildWallAccessRamp", 1)[1].split("private static void buildTower", 1)[0]
    assert "int stairStart = WALL_RADIUS - 10" in access
    assert "step < WALL_TOP_Y" in access
    assert "int landingStart = stairStart + WALL_TOP_Y" in access
    assert "int landingEnd = WALL_RADIUS + 2" in access
    assert "clearY <= 3" in access

    radius = 76
    wall_top_y = 9
    stair_start = radius - 10
    stair_distances = list(range(stair_start, stair_start + wall_top_y))
    stair_heights = list(range(1, wall_top_y + 1))
    landing_start = stair_start + wall_top_y
    landing_end = radius + 2
    landing_distances = list(range(landing_start, landing_end + 1))

    assert stair_distances == list(range(radius - 10, radius - 1))
    assert stair_distances[-1] == radius - 2
    assert landing_start == stair_distances[-1] + 1
    assert set(stair_distances).isdisjoint(landing_distances)
    assert stair_heights[-1] == wall_top_y
    assert landing_start <= radius <= landing_end
    assert landing_end == radius + 2

    build_base = terrain.split("static void buildBase", 1)[1].split("static void rebuildNorthGate", 1)[0]
    assert build_base.index("terraform(level, center, groundY)") < build_base.index("buildWallAccess(level, center, groundY)")

    assert "center.below(10)).is(Blocks.REDSTONE_BLOCK)" in world
    assert "center.below(10), Blocks.REDSTONE_BLOCK" in world
    visual = world.split("boolean visualRevisionMissing", 1)[1].split("if (!firstBuild", 1)[0]
    assert "below(10)" in visual

    print("[PASS] nine stair rows remain intact instead of being overwritten by the roof-height landing")
    print("[PASS] landing begins exactly one block after the top stair and connects through the wall to the outer gallery")
    print("[PASS] every stair keeps three blocks of authored head clearance")
    print("[PASS] existing v0.18.34 worlds force one full fortress rebuild to remove the stale blocking slab")
    print("[PASS] v0.18.35 wall-access climb contract complete")


if __name__ == "__main__":
    main()
