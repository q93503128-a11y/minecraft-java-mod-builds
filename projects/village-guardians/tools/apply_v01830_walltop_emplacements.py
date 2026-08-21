#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"missing patch anchor: {label}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> None:
    props = ROOT / "gradle.properties"
    replace_once(props, "mod_version=0.18.29-alpha.1", "mod_version=0.18.30-alpha.1", "version")

    readme = ROOT / "README.md"
    replace_once(readme,
                 "- 현재 소스 버전 `0.18.29-alpha.1`\n- 목표 JAR `villageguardians-0.18.29-alpha.1.jar`",
                 "- 현재 소스 버전 `0.18.30-alpha.1`\n- 목표 JAR `villageguardians-0.18.30-alpha.1.jar`",
                 "readme version")
    section = """## 0.18.30 성벽 상부 전투 거점·포탑 포좌 정합

- 성벽 보행로가 실제 전투 지형이 되도록 북·남·동·서 각 2곳, 총 8개의 5×5 성벽 상부 포좌를 추가했다. 중앙 문양 블록과 U자 안전 난간으로 배치 위치를 읽을 수 있고, 기존 성벽 보행로와 3블록 폭으로 직접 연결된다.
- 기존 포탑 배치 검증은 마을 중심 반경을 지상 기준으로만 잘라 성벽 상부가 방어구역 밖으로 판정되던 문제를 수정했다. 지정된 성벽 포좌만 예외적으로 허용하며 일반 외벽·임의 고지대에는 설치할 수 없다.
- 지붕이나 임의 구조물 위에 3블록 공간만 있으면 포탑을 올릴 수 있던 고도 우회도 막았다. 일반 포탑은 평탄화된 마을 지면 ±2블록, 성벽 상부는 지정 포좌에서만 배치된다.
- 성벽 수리/재건 시에도 상부 포좌와 난간 연결부가 다시 투영되며, 기존 0.18.29 월드에는 새 마이그레이션 마커를 통해 자동 재건된다.
- 포탑 총구는 설치 기준점보다 2블록 이상 높게 계산되고 실제 블록 충돌 LOS를 사용하므로, 포좌 포탑은 성벽 뒤 지상 포탑과 달리 흉벽 위로 사격할 수 있으면서 벽을 관통하지 않는다.

"""
    replace_once(readme, "## 0.18.29 복합 전장 가독성·공중 경고 정합\n",
                 section + "## 0.18.29 복합 전장 가독성·공중 경고 정합\n",
                 "readme v01830 section")

    enhancements = JAVA / "VillageBuildingEnhancements.java"
    replace_once(enhancements,
                 "    private static final int WALL_RADIUS = VillageWorldSystem.FORTRESS_RADIUS;\n    private static final int WALL_TOP_Y = 9;\n",
                 "    private static final int WALL_RADIUS = VillageWorldSystem.FORTRESS_RADIUS;\n"
                 "    private static final int WALL_TOP_Y = 9;\n"
                 "    private static final int WALL_EMPLACEMENT_LANE = 34;\n"
                 "    private static final int WALL_EMPLACEMENT_INSET = 7;\n"
                 "    private static final int WALL_EMPLACEMENT_HALF = 2;\n",
                 "wall emplacement constants")

    old_end = """            placeRailing(level, new BlockPos(eastOuter, railY, z));
            placeRailing(level, new BlockPos(eastInner, railY, z));
        }
    }

    private static void connectEntranceToRoad(
"""
    new_end = """            placeRailing(level, new BlockPos(eastOuter, railY, z));
            placeRailing(level, new BlockPos(eastInner, railY, z));
        }
        buildWallTopEmplacements(level, center);
    }

    /** True only for the authored 3x3 placement cores inside the eight wall-top pads. */
    static boolean isWallTopEmplacement(BlockPos center, BlockPos candidate) {
        if (center == null || candidate == null || candidate.getY() != center.getY() + WALL_TOP_Y) return false;
        int dx = candidate.getX() - center.getX();
        int dz = candidate.getZ() - center.getZ();
        int inset = WALL_RADIUS - WALL_EMPLACEMENT_INSET;
        boolean northSouth = Math.abs(Math.abs(dx) - WALL_EMPLACEMENT_LANE) <= 1
                && Math.abs(Math.abs(dz) - inset) <= 1;
        boolean eastWest = Math.abs(Math.abs(dz) - WALL_EMPLACEMENT_LANE) <= 1
                && Math.abs(Math.abs(dx) - inset) <= 1;
        return northSouth || eastWest;
    }

    private static void buildWallTopEmplacements(ServerLevel level, BlockPos center) {
        int floorY = center.getY() - 1 + WALL_TOP_Y;
        int inset = WALL_RADIUS - WALL_EMPLACEMENT_INSET;
        for (int lane : new int[]{-WALL_EMPLACEMENT_LANE, WALL_EMPLACEMENT_LANE}) {
            buildEmplacementPad(level,
                    new BlockPos(center.getX() + lane, floorY, center.getZ() - inset), Direction.NORTH);
            buildEmplacementPad(level,
                    new BlockPos(center.getX() + lane, floorY, center.getZ() + inset), Direction.SOUTH);
            buildEmplacementPad(level,
                    new BlockPos(center.getX() - inset, floorY, center.getZ() + lane), Direction.WEST);
            buildEmplacementPad(level,
                    new BlockPos(center.getX() + inset, floorY, center.getZ() + lane), Direction.EAST);
        }
    }

    /**
     * Five-by-five inward platform with a three-wide opening to the wall walk.
     * The centre chiseled brick is the obvious turret anchor; the 3x3 core is placement-valid.
     */
    private static void buildEmplacementPad(ServerLevel level, BlockPos padCenter, Direction outward) {
        Direction sideways = outward.getClockWise();
        for (int forward = -WALL_EMPLACEMENT_HALF; forward <= WALL_EMPLACEMENT_HALF; forward++) {
            for (int side = -WALL_EMPLACEMENT_HALF; side <= WALL_EMPLACEMENT_HALF; side++) {
                BlockPos floor = padCenter.relative(outward, forward).relative(sideways, side);
                set(level, floor, forward == 0 && side == 0 ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICKS);
                for (int clear = 1; clear <= 3; clear++) set(level, floor.above(clear), Blocks.AIR);
            }
        }

        // U-shaped guard rail on the village-facing and side edges; the wall-facing edge stays open.
        for (int side = -WALL_EMPLACEMENT_HALF; side <= WALL_EMPLACEMENT_HALF; side++) {
            placeRailing(level, padCenter.relative(outward, -WALL_EMPLACEMENT_HALF)
                    .relative(sideways, side).above());
        }
        for (int forward = -WALL_EMPLACEMENT_HALF + 1; forward <= WALL_EMPLACEMENT_HALF - 1; forward++) {
            placeRailing(level, padCenter.relative(outward, forward)
                    .relative(sideways, -WALL_EMPLACEMENT_HALF).above());
            placeRailing(level, padCenter.relative(outward, forward)
                    .relative(sideways, WALL_EMPLACEMENT_HALF).above());
        }

        // The reinforced inner parapet sits exactly three blocks outward from the pad centre.
        // Clear three cells so players can walk directly between the gallery and the emplacement.
        BlockPos galleryOpening = padCenter.relative(outward, WALL_EMPLACEMENT_HALF + 1);
        for (int side = -1; side <= 1; side++) {
            set(level, galleryOpening.relative(sideways, side).above(), Blocks.AIR);
        }
    }

    private static void connectEntranceToRoad(
"""
    replace_once(enhancements, old_end, new_end, "wall emplacement geometry")

    world = JAVA / "VillageWorldSystem.java"
    replace_once(world,
                 "        boolean visualRevisionMissing = !level.getBlockState(center.below(4)).is(Blocks.RESPAWN_ANCHOR)\n"
                 "                || !level.getBlockState(center.below(5)).is(Blocks.AMETHYST_BLOCK)\n"
                 "                || !level.getBlockState(center.below(6)).is(Blocks.LAPIS_BLOCK);",
                 "        boolean visualRevisionMissing = !level.getBlockState(center.below(4)).is(Blocks.RESPAWN_ANCHOR)\n"
                 "                || !level.getBlockState(center.below(5)).is(Blocks.AMETHYST_BLOCK)\n"
                 "                || !level.getBlockState(center.below(6)).is(Blocks.LAPIS_BLOCK)\n"
                 "                || !level.getBlockState(center.below(7)).is(Blocks.EMERALD_BLOCK);",
                 "v01830 migration marker check")
    replace_once(world,
                 "                player.sendSystemMessage(Component.literal(\n"
                 "                        \"§6[마을 정비] §f지붕 표식을 제거하고 출입구 정면 문양으로 교체합니다.\"));",
                 "                player.sendSystemMessage(Component.literal(\n"
                 "                        \"§6[마을 정비] §f성벽 상부 포좌와 보행로 연결부를 최신 방어 배치로 갱신합니다.\"));",
                 "migration message")
    replace_once(world,
                 "            player.sendSystemMessage(Component.literal(\n"
                 "                    \"§a[마을 준비 완료] §f건물 지붕이 복구되고 정면 문양과 방어탑이 적용됐습니다.\"));",
                 "            player.sendSystemMessage(Component.literal(\n"
                 "                    \"§a[마을 준비 완료] §f시설과 성벽 상부 포좌, 방어탑이 최신 상태로 적용됐습니다.\"));",
                 "completion message")
    replace_once(world,
                 "        if (building == VillageProgressionSystem.Building.WALLS) {\n"
                 "            VillageFortressTerrain.rebuildNorthGate(level, center);\n"
                 "            VillageDefenseTowerBuilder.build(level, center);\n",
                 "        if (building == VillageProgressionSystem.Building.WALLS) {\n"
                 "            VillageFortressTerrain.rebuildNorthGate(level, center);\n"
                 "            VillageBuildingEnhancements.reinforceWallRailings(level, center);\n"
                 "            VillageDefenseTowerBuilder.build(level, center);\n",
                 "wall rebuild projects emplacements")
    replace_once(world,
                 "        VillageFortressTerrain.set(level, center.below(6), Blocks.LAPIS_BLOCK);\n",
                 "        VillageFortressTerrain.set(level, center.below(7), Blocks.EMERALD_BLOCK);\n"
                 "        VillageFortressTerrain.set(level, center.below(6), Blocks.LAPIS_BLOCK);\n",
                 "v01830 migration marker write")

    turret = JAVA / "VillagePlacedTurretSystem.java"
    replace_once(turret,
                 "        return type.displayName() + \" 배치 모드 시작 · 설치할 바닥을 우클릭하면 위치를 미리 검증하고, 같은 위치를 다시 우클릭하면 확정합니다.\";",
                 "        return type.displayName() + \" 배치 모드 시작 · 지상 방어구역 또는 성벽 상부의 문양 포좌 바닥을 우클릭하면 위치를 미리 검증하고, 같은 위치를 다시 우클릭하면 확정합니다.\";",
                 "placement hint")
    replace_once(turret,
                 "            player.sendSystemMessage(Component.literal(\"§a[배치 미리보기] §f유효한 위치입니다. 같은 블록을 다시 우클릭해 확정하세요.\"));",
                 "            BlockPos villageCenter = VillageCouncilState.villageCenter().orElse(null);\n"
                 "            boolean wallTop = VillageBuildingEnhancements.isWallTopEmplacement(villageCenter, candidate);\n"
                 "            player.sendSystemMessage(Component.literal(\"§a[배치 미리보기] §f\"\n"
                 "                    + (wallTop ? \"성벽 상부 포좌\" : \"유효한 지상 위치\")\n"
                 "                    + \"입니다. 같은 블록을 다시 우클릭해 확정하세요.\"));",
                 "wall-top placement preview")

    old_validation = """        int dx = pos.getX() - center.getX();
        int dz = pos.getZ() - center.getZ();
        int r = VillageWorldSystem.FORTRESS_RADIUS - 2;
        if ((long) dx * dx + (long) dz * dz > (long) r * r) return \"마을 방어구역 안에 배치해야 합니다.\";
        if (Math.abs(dx) <= 7 && dz >= -72 && dz <= 40) return \"주 통행로를 막을 수 없습니다.\";
        if (dz <= -54 && Math.abs(dx) <= 28) return \"북문 진입로 도배 방지를 위해 성문 앞에는 설치할 수 없습니다.\";
"""
    new_validation = """        int dx = pos.getX() - center.getX();
        int dz = pos.getZ() - center.getZ();
        boolean wallEmplacement = VillageBuildingEnhancements.isWallTopEmplacement(center, pos);
        if (!wallEmplacement && Math.abs(pos.getY() - center.getY()) > 2) {
            return \"일반 포탑은 마을 지면에 설치해야 합니다. 높은 위치는 지정된 성벽 포좌만 사용할 수 있습니다.\";
        }
        int r = VillageWorldSystem.FORTRESS_RADIUS - 2;
        if (!wallEmplacement && (long) dx * dx + (long) dz * dz > (long) r * r) {
            return \"마을 방어구역 안에 배치해야 합니다.\";
        }
        if (!wallEmplacement && Math.abs(dx) <= 7 && dz >= -72 && dz <= 40) return \"주 통행로를 막을 수 없습니다.\";
        if (!wallEmplacement && dz <= -54 && Math.abs(dx) <= 28) return \"북문 진입로 도배 방지를 위해 성문 앞에는 설치할 수 없습니다.\";
"""
    replace_once(turret, old_validation, new_validation, "wall-top placement validator")

    historical = ROOT / "tools/test_v01829_battlefield_readability.py"
    replace_once(historical,
                 "    assert \"mod_version=0.18.29-alpha.1\" in props\n"
                 "    assert \"0.18.29-alpha.1\" in readme and \"villageguardians-0.18.29-alpha.1.jar\" in readme\n",
                 "    assert \"mod_version=\" in props\n"
                 "    assert \"현재 소스 버전\" in readme and \"목표 JAR\" in readme\n",
                 "historical v01829 version independence")

    # The patch is staging-only. Keep the durable regression contract, not the migration helper.
    Path(__file__).unlink()


if __name__ == "__main__":
    main()
