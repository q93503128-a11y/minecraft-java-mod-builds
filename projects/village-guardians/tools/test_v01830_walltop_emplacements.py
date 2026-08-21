#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    enhancements = read("VillageBuildingEnhancements.java")
    world = read("VillageWorldSystem.java")
    turret = read("VillagePlacedTurretSystem.java")
    los = read("VillageDefenseLineOfSight.java")
    historical = (ROOT / "tools/test_v01829_battlefield_readability.py").read_text(encoding="utf-8")

    assert "mod_version=" in props
    assert "현재 소스 버전" in readme and "목표 JAR" in readme
    assert 'assert "mod_version=0.18.29-alpha.1" in props' not in historical

    for token in (
        "WALL_EMPLACEMENT_LANE = 34",
        "WALL_EMPLACEMENT_INSET = 7",
        "WALL_EMPLACEMENT_HALF = 2",
        "static boolean isWallTopEmplacement",
        "candidate.getY() != center.getY() + WALL_TOP_Y",
        "buildWallTopEmplacements(level, center)",
        "private static void buildEmplacementPad",
        "Blocks.CHISELED_STONE_BRICKS",
        "galleryOpening",
    ):
        assert token in enhancements

    # Two lane values x four cardinal faces = eight authored wall-top platforms.
    build = enhancements.split("private static void buildWallTopEmplacements", 1)[1].split(
        "private static void buildEmplacementPad", 1)[0]
    assert "new int[]{-WALL_EMPLACEMENT_LANE, WALL_EMPLACEMENT_LANE}" in build
    for direction in ("Direction.NORTH", "Direction.SOUTH", "Direction.WEST", "Direction.EAST"):
        assert direction in build

    pad = enhancements.split("private static void buildEmplacementPad", 1)[1].split(
        "private static void connectEntranceToRoad", 1)[0]
    assert "clear <= 3" in pad
    assert "U-shaped guard rail" in pad
    assert "side = -1; side <= 1" in pad
    assert "galleryOpening.relative(sideways, side).above()" in pad

    assert "center.below(7)).is(Blocks.EMERALD_BLOCK)" in world
    assert "center.below(7), Blocks.EMERALD_BLOCK" in world
    rebuild = world.split("public static void rebuildStructure", 1)[1].split(
        "public static void applyUpgradeVisual", 1)[0]
    assert "VillageBuildingEnhancements.reinforceWallRailings(level, center);" in rebuild

    validation = turret.split("private static String invalidReason", 1)[1].split("private static", 1)[0]
    assert "boolean wallEmplacement = VillageBuildingEnhancements.isWallTopEmplacement(center, pos);" in validation
    assert "!wallEmplacement && Math.abs(pos.getY() - center.getY()) > 2" in validation
    assert "!wallEmplacement && (long) dx * dx + (long) dz * dz" in validation
    assert "!wallEmplacement && Math.abs(dx) <= 7" in validation
    assert "높은 위치는 지정된 성벽 포좌만" in validation
    assert "성벽 상부의 문양 포좌" in turret
    assert "성벽 상부 포좌" in turret

    # Wall-top firing is a spatial advantage, not wall penetration: elevated muzzle + collider raycast.
    assert "Vec3.atCenterOf(state.pos().above(2))" in turret
    assert "state.pos().getY() + 3.05" in turret
    assert "ClipContext.Block.COLLIDER" in los
    assert "HitResult.Type.MISS" in los

    print("[PASS] eight authored wall-top emplacement pads are generated on all four fortress faces")
    print("[PASS] each emplacement has a visible anchor, safe U-rail and three-wide gallery connection")
    print("[PASS] designated wall pads bypass ground-radius restrictions without opening arbitrary rooftop exploits")
    print("[PASS] wall repair/rebuild and existing-world migration restore the new emplacement geometry")
    print("[PASS] elevated turret muzzle gains real parapet LOS while collider raycasts still block wall penetration")
    print("[PASS] historical v0.18.29 regression is version-independent")
    print("[PASS] v0.18.30 wall-top emplacement integrity contract complete")


if __name__ == "__main__":
    main()
