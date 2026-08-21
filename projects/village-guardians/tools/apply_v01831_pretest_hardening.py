#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"missing patch anchor: {label}")
    if text.count(old) != 1:
        raise RuntimeError(f"ambiguous patch anchor ({text.count(old)}): {label}")
    return text.replace(old, new, 1)


def main() -> None:
    props_path = ROOT / "gradle.properties"
    props = read(props_path)
    props = replace_once(props, "mod_version=0.18.30-alpha.1", "mod_version=0.18.31-alpha.1", "version")
    write(props_path, props)

    readme_path = ROOT / "README.md"
    readme = read(readme_path)
    readme = replace_once(readme,
        "현재 소스 버전 `0.18.30-alpha.1`\n- 목표 JAR `villageguardians-0.18.30-alpha.1.jar`",
        "현재 소스 버전 `0.18.31-alpha.1`\n- 목표 JAR `villageguardians-0.18.31-alpha.1.jar`",
        "readme version")
    marker = "## 0.18.30 성벽 상부 전투 거점·포탑 포좌 정합"
    section = """## 0.18.31 실플레이 직전 성벽 동선·성루 명사수 배치 안정화

- 북문 쪽 두 계단에만 의존하던 성벽 접근을 보완했다. 기존 북측 접근로는 보존하고 남·동·서 성벽에 각 2개씩 5블록 폭 계단과 상부 착지 공간을 추가해 측면/후방 포좌까지 성 내부에서 직접 올라갈 수 있다.
- 새 계단 위치의 안쪽 흉벽 난간도 실제 통로 폭만큼 열어 두므로 계단 꼭대기에서 난간에 막히지 않는다. 외곽 난간은 유지해 성벽 밖 추락 방지는 보존한다.
- `성루 명사수`의 기존 WALL 거점 좌표가 실제 성벽 보행면보다 한 블록 높고 성벽 안쪽 램프 쪽에 치우쳐 있던 문제를 수정했다. 이제 북측 실제 성벽 상부 중앙선에 서며 UUID 기반 10개 슬롯으로 여러 명이 한 좌표에 겹치지 않는다.
- 성루 명사수의 성벽 경로 탐색이 즉시 실패하면 곧바로 성 내부 배치로 후퇴시키지 않고, 해당 북측 계단 아래의 지상 집결점으로 먼저 이동한 뒤 다음 배치 틱에서 다시 성벽 상부 경로를 시도한다.
- 기존 월드는 새 마이그레이션 마커를 통해 성벽 계단·난간 개구부를 자동 투영한다. 이번 버전은 대규모 신규 콘텐츠보다 실제 플레이 테스트 직전 이동/배치 실패 가능성을 줄이는 안정화 패스다.

"""
    readme = replace_once(readme, marker, section + marker, "readme section")
    write(readme_path, readme)

    terrain_path = JAVA / "VillageFortressTerrain.java"
    terrain = read(terrain_path)
    old_access = '''    private static void buildWallAccess(ServerLevel level, BlockPos center, int groundY) {
        for (int side : new int[]{-25, 25}) {
            for (int step = 0; step < WALL_TOP_Y; step++) {
                int z = center.getZ() - WALL_RADIUS + 14 - step;
                int y = groundY + 1 + step;
                for (int width = -2; width <= 2; width++) {
                    BlockPos stairPos = new BlockPos(center.getX() + side + width, y, z);
                    for (int supportY = groundY + 1; supportY < y; supportY++) {
                        set(level, new BlockPos(stairPos.getX(), supportY, stairPos.getZ()), Blocks.STONE_BRICKS);
                    }
                    level.setBlockAndUpdate(
                            stairPos,
                            Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
                    for (int clearY = 1; clearY <= 3; clearY++) {
                        set(level, stairPos.above(clearY), Blocks.AIR);
                    }
                }
            }

            for (int z = -WALL_RADIUS + 1; z <= -WALL_RADIUS + 6; z++) {
                for (int width = -3; width <= 3; width++) {
                    BlockPos landing = new BlockPos(
                            center.getX() + side + width,
                            groundY + WALL_TOP_Y,
                            center.getZ() + z);
                    set(level, landing, Blocks.STONE_BRICKS);
                    for (int y = 1; y <= 3; y++) {
                        set(level, landing.above(y), Blocks.AIR);
                    }
                }
            }
        }
    }
'''
    new_access = '''    private static void buildWallAccess(ServerLevel level, BlockPos center, int groundY) {
        // Preserve the original north-gate access lanes while giving the other three walls
        // their own direct routes. Side/rear lanes align with the authored wall-top defense zones.
        for (int lane : new int[]{-25, 25}) {
            buildWallAccessRamp(level, center, groundY, Direction.NORTH, lane);
        }
        for (int lane : new int[]{-34, 34}) {
            buildWallAccessRamp(level, center, groundY, Direction.SOUTH, lane);
            buildWallAccessRamp(level, center, groundY, Direction.WEST, lane);
            buildWallAccessRamp(level, center, groundY, Direction.EAST, lane);
        }
    }

    private static void buildWallAccessRamp(
            ServerLevel level, BlockPos center, int groundY, Direction outward, int lane) {
        Direction sideways = outward.getClockWise();
        int stairStart = WALL_RADIUS - 14;
        for (int step = 0; step < WALL_TOP_Y; step++) {
            BlockPos row = center.relative(outward, stairStart + step).relative(sideways, lane);
            int y = groundY + 1 + step;
            for (int width = -2; width <= 2; width++) {
                BlockPos column = row.relative(sideways, width);
                BlockPos stairPos = new BlockPos(column.getX(), y, column.getZ());
                for (int supportY = groundY + 1; supportY < y; supportY++) {
                    set(level, new BlockPos(stairPos.getX(), supportY, stairPos.getZ()), Blocks.STONE_BRICKS);
                }
                level.setBlockAndUpdate(
                        stairPos,
                        Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, outward));
                for (int clearY = 1; clearY <= 3; clearY++) {
                    set(level, stairPos.above(clearY), Blocks.AIR);
                }
            }
        }

        int landingStart = WALL_RADIUS - 6;
        int landingEnd = WALL_RADIUS - 1;
        for (int distance = landingStart; distance <= landingEnd; distance++) {
            BlockPos row = center.relative(outward, distance).relative(sideways, lane);
            for (int width = -3; width <= 3; width++) {
                BlockPos column = row.relative(sideways, width);
                BlockPos landing = new BlockPos(column.getX(), groundY + WALL_TOP_Y, column.getZ());
                set(level, landing, Blocks.STONE_BRICKS);
                for (int clearY = 1; clearY <= 3; clearY++) {
                    set(level, landing.above(clearY), Blocks.AIR);
                }
            }
        }
    }
'''
    terrain = replace_once(terrain, old_access, new_access, "four-side wall access")
    write(terrain_path, terrain)

    enhancements_path = JAVA / "VillageBuildingEnhancements.java"
    enhancements = read(enhancements_path)
    old_rails = '''            placeRailing(level, new BlockPos(x, railY, southOuter));
            placeRailing(level, new BlockPos(x, railY, southInner));

            int z = center.getZ() + offset;
            placeRailing(level, new BlockPos(westOuter, railY, z));
            placeRailing(level, new BlockPos(westInner, railY, z));
            placeRailing(level, new BlockPos(eastOuter, railY, z));
            placeRailing(level, new BlockPos(eastInner, railY, z));
'''
    new_rails = '''            placeRailing(level, new BlockPos(x, railY, southOuter));
            if (!isSideRearStairOpening(offset)) {
                placeRailing(level, new BlockPos(x, railY, southInner));
            }

            int z = center.getZ() + offset;
            placeRailing(level, new BlockPos(westOuter, railY, z));
            if (!isSideRearStairOpening(offset)) {
                placeRailing(level, new BlockPos(westInner, railY, z));
            }
            placeRailing(level, new BlockPos(eastOuter, railY, z));
            if (!isSideRearStairOpening(offset)) {
                placeRailing(level, new BlockPos(eastInner, railY, z));
            }
'''
    enhancements = replace_once(enhancements, old_rails, new_rails, "side/rear railing openings")
    old_helper = '''    private static boolean isNorthStairOpening(int offset) {
        return Math.abs(Math.abs(offset) - 25) <= 3;
    }
'''
    new_helper = '''    private static boolean isNorthStairOpening(int offset) {
        return Math.abs(Math.abs(offset) - 25) <= 3;
    }

    private static boolean isSideRearStairOpening(int offset) {
        return Math.abs(Math.abs(offset) - WALL_EMPLACEMENT_LANE) <= 3;
    }
'''
    enhancements = replace_once(enhancements, old_helper, new_helper, "side/rear opening helper")
    write(enhancements_path, enhancements)

    deployment_path = JAVA / "VillageMercenaryDeploymentSystem.java"
    deployment = read(deployment_path)
    deployment = replace_once(deployment, "import java.util.Locale;\n", "import java.util.Locale;\nimport java.util.UUID;\n", "uuid import")
    old_move = '''        BlockPos rally = rallyPoint(center, zone, kind);
        AABB area = new AABB(center).inflate(VillageWorldSystem.BATTLEFIELD_RADIUS, 96,
                VillageWorldSystem.BATTLEFIELD_RADIUS);
        for (IronGolem golem : level.getEntitiesOfClass(IronGolem.class, area,
                mob -> VillageMercenarySystem.classOf(mob) == kind && mob.isAlive())) {
            double leash = switch (kind) {
'''
    new_move = '''        AABB area = new AABB(center).inflate(VillageWorldSystem.BATTLEFIELD_RADIUS, 96,
                VillageWorldSystem.BATTLEFIELD_RADIUS);
        for (IronGolem golem : level.getEntitiesOfClass(IronGolem.class, area,
                mob -> VillageMercenarySystem.classOf(mob) == kind && mob.isAlive())) {
            BlockPos rally = rallyPoint(center, zone, kind, golem.getUUID());
            double leash = switch (kind) {
'''
    deployment = replace_once(deployment, old_move, new_move, "per-mercenary rally")
    old_fallback = '''                if (!accepted && zone == Deployment.WALL) {
                    BlockPos fallback = rallyPoint(center, Deployment.INNER, kind);
                    golem.getNavigation().moveTo(fallback.getX() + 0.5, fallback.getY(), fallback.getZ() + 0.5, 1.0);
                }
'''
    new_fallback = '''                if (!accepted && zone == Deployment.WALL) {
                    BlockPos staging = rangerWallStagingPoint(center, golem.getUUID());
                    golem.getNavigation().moveTo(staging.getX() + 0.5, staging.getY(), staging.getZ() + 0.5, 1.0);
                }
'''
    deployment = replace_once(deployment, old_fallback, new_fallback, "wall staging fallback")
    old_rally = '''    private static BlockPos rallyPoint(BlockPos center, Deployment zone, VillageMercenarySystem.MercenaryClass kind) {
        return switch (zone) {
            case GATE_FRONT -> center.offset(kind == VillageMercenarySystem.MercenaryClass.STRIKER ? 9 : -9, 0, -58);
            case INNER -> center.offset(kind.ordinal() * 4 - 6, 0, -18);
            case WALL -> center.offset(kind == VillageMercenarySystem.MercenaryClass.RANGER ? 26 : -26, 10, -69);
        };
    }
'''
    new_rally = '''    private static BlockPos rallyPoint(
            BlockPos center, Deployment zone, VillageMercenarySystem.MercenaryClass kind, UUID mercenaryId) {
        return switch (zone) {
            case GATE_FRONT -> center.offset(kind == VillageMercenarySystem.MercenaryClass.STRIKER ? 9 : -9, 0, -58);
            case INNER -> center.offset(kind.ordinal() * 4 - 6, 0, -18);
            case WALL -> rangerWallPost(center, mercenaryId);
        };
    }

    /** Actual north-wall walk surface: ten stable slots keep multiple rangers from occupying one block. */
    private static BlockPos rangerWallPost(BlockPos center, UUID mercenaryId) {
        int slot = Math.floorMod(mercenaryId == null ? 0 : mercenaryId.hashCode(), 10);
        int lane = slot < 5 ? -25 : 25;
        int spread = slot % 5 - 2;
        return center.offset(lane + spread, 9, -74);
    }

    /** If a long path is temporarily rejected, approach the foot of the matching north stair and retry. */
    private static BlockPos rangerWallStagingPoint(BlockPos center, UUID mercenaryId) {
        int slot = Math.floorMod(mercenaryId == null ? 0 : mercenaryId.hashCode(), 10);
        int lane = slot < 5 ? -25 : 25;
        return center.offset(lane, 0, -62);
    }
'''
    deployment = replace_once(deployment, old_rally, new_rally, "physical wall ranger posts")
    write(deployment_path, deployment)

    world_path = JAVA / "VillageWorldSystem.java"
    world = read(world_path)
    world = replace_once(world,
        "|| !level.getBlockState(center.below(7)).is(Blocks.EMERALD_BLOCK);",
        "|| !level.getBlockState(center.below(7)).is(Blocks.EMERALD_BLOCK)\n                || !level.getBlockState(center.below(8)).is(Blocks.DIAMOND_BLOCK);",
        "migration detection")
    world = replace_once(world,
        "§6[마을 정비] §f성벽 상부 포좌와 보행로 연결부를 최신 방어 배치로 갱신합니다.",
        "§6[마을 정비] §f성벽 4면 접근 계단·포좌와 용병 고지 동선을 최신 방어 배치로 갱신합니다.",
        "migration message")
    world = replace_once(world,
        "§a[마을 준비 완료] §f시설과 성벽 상부 포좌, 방어탑이 최신 상태로 적용됐습니다.",
        "§a[마을 준비 완료] §f시설과 성벽 4면 접근로·상부 포좌·방어탑이 최신 상태로 적용됐습니다.",
        "ready message")
    world = replace_once(world,
        "VillageFortressTerrain.set(level, center.below(7), Blocks.EMERALD_BLOCK);",
        "VillageFortressTerrain.set(level, center.below(8), Blocks.DIAMOND_BLOCK);\n        VillageFortressTerrain.set(level, center.below(7), Blocks.EMERALD_BLOCK);",
        "migration marker")
    write(world_path, world)

    old_test_path = ROOT / "tools/test_v01830_walltop_emplacements.py"
    old_test = read(old_test_path)
    old_test = replace_once(old_test,
        '    assert "mod_version=0.18.30-alpha.1" in props\n    assert "0.18.30-alpha.1" in readme and "villageguardians-0.18.30-alpha.1.jar" in readme\n',
        '    assert "mod_version=" in props\n    assert "현재 소스 버전" in readme and "목표 JAR" in readme\n',
        "historical v01830 version independence")
    write(old_test_path, old_test)

    test_path = ROOT / "tools/test_v01831_pretest_hardening.py"
    test_path.write_text(r'''#!/usr/bin/env python3
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
''', encoding="utf-8")

    print("[PATCH] Village Guardians 0.18.31 pre-playtest hardening applied")


if __name__ == "__main__":
    main()
