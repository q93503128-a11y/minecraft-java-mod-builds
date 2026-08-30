#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match in {path}, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> None:
    terrain = JAVA / "VillageFortressTerrain.java"
    replace_once(
        terrain,
        '''        int landingStart = WALL_RADIUS - 6;\n        int landingEnd = WALL_RADIUS - 1;\n''',
        '''        // The landing must begin AFTER the ninth stair. The old R-6 start overlapped\n        // stairs 5-9 and replaced them with a roof-height slab, creating an unclimbable wall.\n        // R-1 bridges the top stair at R-2 into the wall top; R..R+2 matches the defender gallery.\n        int landingStart = stairStart + WALL_TOP_Y;\n        int landingEnd = WALL_RADIUS + 2;\n''',
        "wall landing overlap fix",
    )

    world = JAVA / "VillageWorldSystem.java"
    replace_once(
        world,
        '''                || !level.getBlockState(center.below(8)).is(Blocks.DIAMOND_BLOCK)\n                || !level.getBlockState(center.below(9)).is(Blocks.GOLD_BLOCK);\n''',
        '''                || !level.getBlockState(center.below(8)).is(Blocks.DIAMOND_BLOCK)\n                || !level.getBlockState(center.below(9)).is(Blocks.GOLD_BLOCK)\n                || !level.getBlockState(center.below(10)).is(Blocks.REDSTONE_BLOCK);\n''',
        "wall access migration marker check",
    )
    replace_once(
        world,
        '''        // 26.2 exposes Blocks.COPPER_BLOCK as a weathering collection, so the migration marker uses a stable block.\n        VillageFortressTerrain.set(level, center.below(9), Blocks.GOLD_BLOCK);\n''',
        '''        // 26.2 exposes Blocks.COPPER_BLOCK as a weathering collection, so migration markers use stable blocks.\n        // v0.18.35: force one rebuild so old overlapping stair landings are physically removed from existing saves.\n        VillageFortressTerrain.set(level, center.below(10), Blocks.REDSTONE_BLOCK);\n        VillageFortressTerrain.set(level, center.below(9), Blocks.GOLD_BLOCK);\n''',
        "wall access migration marker write",
    )
    replace_once(
        world,
        '''                        "§6[마을 정비] §f성벽 4면 접근 계단·사격구·포좌 동선을 최신 실전 배치로 갱신합니다."));\n''',
        '''                        "§6[마을 정비] §f성벽 4면 접근 계단·상단 착지부·사격구·포좌 동선을 최신 실전 배치로 갱신합니다."));\n''',
        "migration player message",
    )

    props = ROOT / "gradle.properties"
    replace_once(props, "mod_version=0.18.34-alpha.1\n", "mod_version=0.18.35-alpha.1\n", "version bump")

    readme = ROOT / "README.md"
    replace_once(readme, "- 현재 소스 버전 `0.18.34-alpha.1`\n- 목표 JAR `villageguardians-0.18.34-alpha.1.jar`\n",
                 "- 현재 소스 버전 `0.18.35-alpha.1`\n- 목표 JAR `villageguardians-0.18.35-alpha.1.jar`\n", "README version")
    marker = "## 0.18.34 Codex 후처리 · 시설 소유권/서버 leaf 정합\n"
    text = readme.read_text(encoding="utf-8")
    if text.count(marker) != 1:
        raise RuntimeError("README release insertion marker mismatch")
    release = '''## 0.18.35 성벽 계단 상단 접속부 실플레이 수정\n\n- 실플레이에서 성벽 계단이 중간부터 최고 높이의 착지 상판에 덮여 플레이어가 성벽 위로 올라갈 수 없는 물리 지오메트리 회귀를 수정했다.\n- 원인은 9칸 계단이 `R-10~R-2`를 사용하면서 착지부가 `R-6~R-1`에서 시작해 마지막 5개 계단의 수평 좌표를 겹쳐 덮던 것이었다.\n- 착지부는 이제 마지막 계단 바로 다음 칸인 `R-1`에서 시작하고 `R+2`까지 이어져 성벽 상부와 외곽 defender gallery에 연속적으로 연결된다.\n- 계단 각 칸의 3블록 머리 공간과 5블록 폭 본체는 유지하며, 기존 북측 2개·남/동/서 각 2개 접근로 모두 같은 수정된 생성 함수를 사용한다.\n- 새 마이그레이션 마커를 추가해 0.18.34 기존 월드도 한 번 자동 재투영하며, 이전 착지 상판 잔재를 제거한 뒤 올바른 계단/착지부를 다시 생성한다.\n- 회귀검사는 계단과 착지부 수평 구간 비중첩, 최고 계단과 착지부의 높이 연속성, 3블록 머리 공간, 기존 월드 재생성 마커를 검증한다.\n\n'''
    readme.write_text(text.replace(marker, release + marker, 1), encoding="utf-8")

    historical = ROOT / "tools/test_v01834_post_codex_followup.py"
    replace_once(historical,
                 '    assert "mod_version=0.18.34-alpha.1" in props\n',
                 '    assert "mod_version=" in props\n',
                 "make v0.18.34 historical regression version-independent")

    test = ROOT / "tools/test_v01835_wall_access_climb.py"
    test.write_text('''#!/usr/bin/env python3\nfrom pathlib import Path\n\nROOT = Path(__file__).resolve().parents[1]\nJAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"\n\n\ndef read(name: str) -> str:\n    return (JAVA / name).read_text(encoding="utf-8")\n\n\ndef main() -> None:\n    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")\n    terrain = read("VillageFortressTerrain.java")\n    world = read("VillageWorldSystem.java")\n\n    assert "mod_version=0.18.35-alpha.1" in props\n\n    access = terrain.split("private static void buildWallAccessRamp", 1)[1].split("private static void buildTower", 1)[0]\n    assert "int stairStart = WALL_RADIUS - 10" in access\n    assert "step < WALL_TOP_Y" in access\n    assert "int landingStart = stairStart + WALL_TOP_Y" in access\n    assert "int landingEnd = WALL_RADIUS + 2" in access\n    assert "clearY <= 3" in access\n\n    radius = 76\n    wall_top_y = 9\n    stair_start = radius - 10\n    stair_distances = list(range(stair_start, stair_start + wall_top_y))\n    stair_heights = list(range(1, wall_top_y + 1))\n    landing_start = stair_start + wall_top_y\n    landing_end = radius + 2\n    landing_distances = list(range(landing_start, landing_end + 1))\n\n    assert stair_distances == list(range(radius - 10, radius - 1))\n    assert stair_distances[-1] == radius - 2\n    assert landing_start == stair_distances[-1] + 1\n    assert set(stair_distances).isdisjoint(landing_distances)\n    assert stair_heights[-1] == wall_top_y\n    assert landing_start <= radius <= landing_end\n    assert landing_end == radius + 2\n\n    build_base = terrain.split("static void buildBase", 1)[1].split("static void rebuildNorthGate", 1)[0]\n    assert build_base.index("terraform(level, center, groundY)") < build_base.index("buildWallAccess(level, center, groundY)")\n\n    assert "center.below(10)).is(Blocks.REDSTONE_BLOCK)" in world\n    assert "center.below(10), Blocks.REDSTONE_BLOCK" in world\n    visual = world.split("boolean visualRevisionMissing", 1)[1].split("if (!firstBuild", 1)[0]\n    assert "below(10)" in visual\n\n    print("[PASS] nine stair rows remain intact instead of being overwritten by the roof-height landing")\n    print("[PASS] landing begins exactly one block after the top stair and connects through the wall to the outer gallery")\n    print("[PASS] every stair keeps three blocks of authored head clearance")\n    print("[PASS] existing v0.18.34 worlds force one full fortress rebuild to remove the stale blocking slab")\n    print("[PASS] v0.18.35 wall-access climb contract complete")\n\n\nif __name__ == "__main__":\n    main()\n''', encoding="utf-8")

    print("[PATCH] v0.18.35 wall-access climb fix applied")


if __name__ == "__main__":
    main()
