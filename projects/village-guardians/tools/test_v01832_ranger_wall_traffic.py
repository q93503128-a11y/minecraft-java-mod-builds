#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    deployment = read("VillageMercenaryDeploymentSystem.java")
    mercenary = read("VillageMercenarySystem.java")
    entry = read("VillageGuardians.java")
    old = (ROOT / "tools/test_v01831_pretest_hardening.py").read_text(encoding="utf-8")

    assert 'assert "mod_version=0.18.31-alpha.1" in props' not in old

    post = deployment.split("private static BlockPos rangerWallPost", 1)[1].split(
        "private static BlockPos rangerWallStagingPoint", 1)[0]
    assert "slot < 5 ? -48 : 48" in post
    assert "(slot % 5 - 2) * 4" in post
    assert "return center.offset(lane + spread, 9, -74)" in post

    # Mirror the authored formula and prove the final firing line cannot occupy either
    # five-wide north-stair landing. The landing itself is seven blocks wide at the top.
    posts = []
    for slot in range(10):
        lane = -48 if slot < 5 else 48
        spread = (slot % 5 - 2) * 4
        posts.append(lane + spread)
    assert posts == [-56, -52, -48, -44, -40, 40, 44, 48, 52, 56]
    assert len(set(posts)) == 10
    for x in posts:
        assert not (-28 <= x <= -22), f"west stair landing blocked by ranger post x={x}"
        assert not (22 <= x <= 28), f"east stair landing blocked by ranger post x={x}"
        assert abs(abs(x) - 34) >= 6, f"ranger post crowds north emplacement connector x={x}"
    for group in (posts[:5], posts[5:]):
        assert min(b - a for a, b in zip(group, group[1:])) >= 4

    staging = deployment.split("private static BlockPos rangerWallStagingPoint", 1)[1].split(
        "private static boolean allowed", 1)[0]
    assert "slot < 5 ? -25 : 25" in staging
    assert "center.offset(lane, 0, -62)" in staging
    assert "BlockPos staging = rangerWallStagingPoint" in deployment

    ranged = mercenary.split("private static void rangedAttack", 1)[1].split(
        "private static void healAllies", 1)[0]
    assert "VillageDefenseLineOfSight.hasLine(level, start, enemy)" in ranged
    assert "VillageEnemyArchetypeSystem.isFlying(enemy) ? 0 : 1" in ranged
    assert "VillageRaidSystem.aerialThreatPriority(enemy)" in ranged
    assert "getNavigation()" not in ranged

    tick = entry.split("public void onServerTick", 1)[1]
    assert tick.index("VillageMercenarySystem.tick") < tick.index("VillageMercenaryDeploymentSystem.tick")
    assert "VillageMercenaryDeploymentSystem.tick" in tick

    print("[PASS] ten stable ranger wall posts are physically separated from both north stair landings")
    print("[PASS] ranger wall posts keep at least four-block spacing and clear north emplacement connectors")
    print("[PASS] stair-foot staging remains aligned with the authored ±25 north access routes")
    print("[PASS] ranger combat retains LOS/aerial priority without stealing navigation ownership")
    print("[PASS] historical v0.18.31 contract is version-independent")
    print("[PASS] v0.18.32 ranger wall traffic integrity contract complete")


if __name__ == "__main__":
    main()
