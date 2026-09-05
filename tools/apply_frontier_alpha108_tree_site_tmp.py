from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT / "projects/frontier-settlement"
CONSTRUCTION = PROJECT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java"
VERIFY = PROJECT / "tools/test_current_source.py"
GRADLE = PROJECT / "gradle.properties"
LOCK = PROJECT / "COMPANION_LOCK.json"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, got {count}")
    return text.replace(old, new, 1)


construction = CONSTRUCTION.read_text(encoding="utf-8")
construction = replace_once(
    construction,
    "    private static final int MAX_TERRAIN_RETAINING_STONE = 96;\n",
    "    private static final int MAX_TERRAIN_RETAINING_STONE = 96;\n"
    "    private static final int TREE_CANOPY_SEARCH_HEIGHT = 10;\n"
    "    private static final int TREE_CANOPY_SEARCH_RADIUS = 2;\n",
    "tree constants",
)
construction = replace_once(
    construction,
    "                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);\n",
    "                int height = terrainSurfaceHeight(level, worldX, worldZ);\n",
    "site terrain height",
)
construction = replace_once(
    construction,
    "                    if (y >= 0 && !isSafeAboveGround(state, y)) return null;\n",
    "                    if (y >= 0 && !isSafeAboveGround(level, pos, state, y)) return null;\n",
    "site above-ground check",
)
construction = replace_once(
    construction,
    "            if (isSoftVegetation(state)) continue;\n"
    "            if (y <= MAX_TERRAIN_CUT_HEIGHT && isNaturalGround(state)) continue;\n",
    "            if (isClearableSiteVegetation(level, pos, state)) continue;\n"
    "            if (y <= MAX_TERRAIN_CUT_HEIGHT && isNaturalGround(state)) continue;\n",
    "grading vegetation check",
)
construction = replace_once(
    construction,
    "        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, floor.getX(), floor.getZ());\n"
    "        return new BlockPos(floor.getX(), y, floor.getZ());\n",
    "        int y = terrainSurfaceHeight(level, floor.getX(), floor.getZ());\n"
    "        return new BlockPos(floor.getX(), y, floor.getZ());\n",
    "grade work surface",
)
construction = replace_once(
    construction,
    "        if ((!current.isAir() && !current.canBeReplaced()) || (!above.isAir() && !above.canBeReplaced())) return false;\n"
    "        return !below.isAir() && isNaturalGround(below);\n",
    "        if ((!current.isAir() && !current.canBeReplaced()\n"
    "                && !isClearableSiteVegetation(level, supply, current))\n"
    "                || (!above.isAir() && !above.canBeReplaced()\n"
    "                && !isClearableSiteVegetation(level, supply.above(), above))) return false;\n"
    "        return !below.isAir() && isNaturalGround(below);\n",
    "safe supply vegetation",
)
construction = replace_once(
    construction,
    "    private static Container ensureSupplyCrate(ServerLevel level, BlockPos supply) {\n"
    "        if (!level.hasChunkAt(supply)) return null;\n"
    "        BlockState current = level.getBlockState(supply);\n"
    "        if (current.is(Blocks.BARREL) && level.getBlockEntity(supply) instanceof Container crate) return crate;\n"
    "        if (level.getBlockEntity(supply) != null || !current.getFluidState().isEmpty()) return null;\n"
    "        if (!current.isAir() && !current.canBeReplaced()) return null;\n"
    "        if (!level.setBlock(supply, Blocks.BARREL.defaultBlockState(), DIRECT_BLOCK_UPDATE)) return null;\n"
    "        return level.getBlockState(supply).is(Blocks.BARREL)\n"
    "                && level.getBlockEntity(supply) instanceof Container crate ? crate : null;\n"
    "    }\n",
    "    private static Container ensureSupplyCrate(ServerLevel level, BlockPos supply) {\n"
    "        BlockPos head = supply.above();\n"
    "        if (!level.hasChunkAt(supply) || !level.hasChunkAt(head)) return null;\n"
    "        BlockState current = level.getBlockState(supply);\n"
    "        BlockState above = level.getBlockState(head);\n"
    "        if (current.is(Blocks.BARREL) && level.getBlockEntity(supply) instanceof Container crate) {\n"
    "            if (level.getBlockEntity(head) != null || !above.getFluidState().isEmpty()) return null;\n"
    "            if (!above.isAir() && !above.canBeReplaced()\n"
    "                    && !isClearableSiteVegetation(level, head, above)) return null;\n"
    "            if (!above.isAir() && !level.setBlock(head, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE)) return null;\n"
    "            return crate;\n"
    "        }\n"
    "        if (level.getBlockEntity(supply) != null || level.getBlockEntity(head) != null\n"
    "                || !current.getFluidState().isEmpty() || !above.getFluidState().isEmpty()) return null;\n"
    "        if (!current.isAir() && !current.canBeReplaced()\n"
    "                && !isClearableSiteVegetation(level, supply, current)) return null;\n"
    "        if (!above.isAir() && !above.canBeReplaced()\n"
    "                && !isClearableSiteVegetation(level, head, above)) return null;\n"
    "        if (!above.isAir() && !level.setBlock(head, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE)) return null;\n"
    "        if (!level.setBlock(supply, Blocks.BARREL.defaultBlockState(), DIRECT_BLOCK_UPDATE)) return null;\n"
    "        return level.getBlockState(supply).is(Blocks.BARREL)\n"
    "                && level.getBlockEntity(supply) instanceof Container crate ? crate : null;\n"
    "    }\n",
    "supply crate natural vegetation clearing",
)
construction = replace_once(
    construction,
    "    private static boolean isSafeAboveGround(BlockState state, int relativeY) {\n"
    "        if (isSoftVegetation(state)) return true;\n"
    "        return relativeY <= MAX_TERRAIN_CUT_HEIGHT && isNaturalGround(state);\n"
    "    }\n\n"
    "    private static boolean isSoftVegetation(BlockState state) {\n",
    "    private static int terrainSurfaceHeight(ServerLevel level, int x, int z) {\n"
    "        int rawHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);\n"
    "        for (int scanned = 0, y = rawHeight - 1; scanned < 32; scanned++, y--) {\n"
    "            BlockPos pos = new BlockPos(x, y, z);\n"
    "            BlockState state = level.getBlockState(pos);\n"
    "            if (isNaturalGround(state)) return y + 1;\n"
    "            if (!isClearableSiteVegetation(level, pos, state)) return rawHeight;\n"
    "        }\n"
    "        return rawHeight;\n"
    "    }\n\n"
    "    private static boolean isSafeAboveGround(ServerLevel level, BlockPos pos, BlockState state, int relativeY) {\n"
    "        if (isClearableSiteVegetation(level, pos, state)) return true;\n"
    "        return relativeY <= MAX_TERRAIN_CUT_HEIGHT && isNaturalGround(state);\n"
    "    }\n\n"
    "    private static boolean isClearableSiteVegetation(ServerLevel level, BlockPos pos, BlockState state) {\n"
    "        return isSoftVegetation(state) || isNaturalTreeLog(level, pos, state);\n"
    "    }\n\n"
    "    private static boolean isNaturalTreeLog(ServerLevel level, BlockPos pos, BlockState state) {\n"
    "        if (!state.is(BlockTags.LOGS)) return false;\n"
    "        for (int dy = 0; dy <= TREE_CANOPY_SEARCH_HEIGHT; dy++) {\n"
    "            for (int dx = -TREE_CANOPY_SEARCH_RADIUS; dx <= TREE_CANOPY_SEARCH_RADIUS; dx++) {\n"
    "                for (int dz = -TREE_CANOPY_SEARCH_RADIUS; dz <= TREE_CANOPY_SEARCH_RADIUS; dz++) {\n"
    "                    BlockPos probe = pos.offset(dx, dy, dz);\n"
    "                    if (level.hasChunkAt(probe) && level.getBlockState(probe).is(BlockTags.LEAVES)) return true;\n"
    "                }\n"
    "            }\n"
    "        }\n"
    "        return false;\n"
    "    }\n\n"
    "    private static boolean isSoftVegetation(BlockState state) {\n",
    "tree-aware helpers",
)
construction = construction.replace(
    "선택한 부지가 안전하지 않습니다. 높이 차 4블록 이하·최대 3블록 성토 범위의 물·기존 건축물이 없는 곳을 선택해 주세요.",
    "선택한 부지가 안전하지 않습니다. 자연 잔디·꽃·수목은 자동 정리되며, 높이 차 4블록 이하·최대 3블록 성토 범위의 물·보호 블록이 없는 곳을 선택해 주세요.",
    1,
)
CONSTRUCTION.write_text(construction, encoding="utf-8")

gradle = GRADLE.read_text(encoding="utf-8")
gradle = replace_once(gradle, "mod_version=0.1.0-alpha.107", "mod_version=0.1.0-alpha.108", "gradle version")
if "# Alpha.108 natural-site placement:" not in gradle:
    gradle += "\n# Alpha.108 natural-site placement: building grading ignores clearable natural tree trunks when measuring terrain, clears verified tree vegetation inside the grading envelope, and permits the derived site-supply barrel to replace natural vegetation while preserving fluids, block entities, and non-tree solid structures.\n"
GRADLE.write_text(gradle, encoding="utf-8")

lock = json.loads(LOCK.read_text(encoding="utf-8"))
if lock.get("versions", {}).get("frontier_settlement") != "0.1.0-alpha.107":
    raise RuntimeError("COMPANION_LOCK frontier version drift")
lock["versions"]["frontier_settlement"] = "0.1.0-alpha.108"
note = "Alpha.108 makes ordinary building placement tree-aware without treating arbitrary wooden structures as terrain: verified natural log columns with nearby leaf canopy are ignored for grade-height sampling, then cleared by the existing physical grading pass; grass, flowers, leaves and the derived site-supply position remain safely clearable while fluids, block entities, non-tree solids, settlement overlap and bounded cut/fill limits still fail closed."
if note not in lock.setdefault("notes", []):
    lock["notes"].append(note)
LOCK.write_text(json.dumps(lock, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

verify = VERIFY.read_text(encoding="utf-8")
verify = replace_once(verify, 'require("mod_version=0.1.0-alpha.107" in gradle, "current verifier/version drift")', 'require("mod_version=0.1.0-alpha.108" in gradle, "current verifier/version drift")', "verifier version")
anchor = 'require("i == 0" in construction and "tickConstructionBuilder" in construction, "builder crew is not serialized through one scheduler")\n'
addition = (
    anchor
    + 'require("terrainSurfaceHeight(level, worldX, worldZ)" in construction, "placement height still treats natural trunks as terrain peaks")\n'
    + 'require("isNaturalTreeLog" in construction and "BlockTags.LOGS" in construction and "BlockTags.LEAVES" in construction, "tree-aware natural vegetation evidence missing")\n'
    + 'require("isClearableSiteVegetation(level, pos, state)" in construction, "grading does not clear verified natural tree vegetation")\n'
    + 'require("isClearableSiteVegetation(level, supply, current)" in construction, "site supply position still rejects natural vegetation")\n'
    + 'require("TREE_CANOPY_SEARCH_HEIGHT = 10" in construction and "TREE_CANOPY_SEARCH_RADIUS = 2" in construction, "bounded tree evidence envelope drifted")\n'
)
verify = replace_once(verify, anchor, addition, "verifier tree placement assertions")
verify = replace_once(
    verify,
    'print("CURRENT SOURCE CHECK PASS: alpha107 worker handoff + physical house repair + prior authority invariants")',
    'print("CURRENT SOURCE CHECK PASS: alpha108 tree-aware placement + alpha107 worker/repair + prior authority invariants")',
    "verifier pass text",
)
VERIFY.write_text(verify, encoding="utf-8")

print("FRONTIER ALPHA108 PATCH APPLIED")
