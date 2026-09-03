from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STORAGE = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementStorageService.java"
WORKERS = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java"

storage = STORAGE.read_text(encoding="utf-8")

if "findProductionDepositTarget" not in storage:
    anchor = '''    public static BlockPos findDepositTargetExcluding(ServerLevel level, SettlementData data,\n                                                      ItemStack stack, Set<BlockPos> excluded) {\n        for (BlockPos pos : depositPositions(level, data, stack)) {\n            if (excluded.contains(pos) || !level.hasChunkAt(pos)) continue;\n            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;\n            if (hasRoom(container, stack)) return pos;\n        }\n        BlockPos stockpile = data.stockpilePos();\n        if (!excluded.contains(stockpile) && hasRoomAt(level, stockpile, stack)) return stockpile;\n        return null;\n    }\n\n'''
    addition = anchor + '''    /**\n     * Ordinary profession output must enter the shared/public economy before construction staging.\n     * Construction-office bays are intentionally excluded here: otherwise lumber/quarry output can\n     * disappear into a builder-only staging buffer and never become visible to Survival Ascension's\n     * shared-depot bridge until that buffer fills. No chunk is force-loaded and a full network simply\n     * returns null so the worker keeps the physical cargo.\n     */\n    public static BlockPos findProductionDepositTarget(ServerLevel level, SettlementData data, ItemStack stack) {\n        Set<BlockPos> positions = new LinkedHashSet<>();\n        positions.addAll(SupplyDepotRegistryService.loadedPositions(level, data));\n        positions.addAll(generalStoragePositions(data));\n        for (BlockPos pos : positions) {\n            if (!level.hasChunkAt(pos)) continue;\n            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;\n            if (hasRoom(container, stack)) return pos;\n        }\n        return null;\n    }\n\n'''
    if anchor not in storage:
        raise SystemExit("SettlementStorageService deposit anchor missing")
    storage = storage.replace(anchor, addition, 1)
    STORAGE.write_text(storage, encoding="utf-8")

workers = WORKERS.read_text(encoding="utf-8")
old = '''        BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);\n        if (!level.hasChunkAt(target) || !SettlementStorageService.hasRoomAt(level, target, carried)) {\n            worker.getNavigation().stop();\n            return;\n        }\n'''
new = '''        BlockPos target = SettlementStorageService.findProductionDepositTarget(level, data, carried);\n        if (target == null || !level.hasChunkAt(target) || !SettlementStorageService.hasRoomAt(level, target, carried)) {\n            worker.getNavigation().stop();\n            return;\n        }\n'''
if new not in workers:
    if workers.count(old) != 1:
        raise SystemExit(f"SettlementWorkerService delivery anchor count={workers.count(old)}")
    workers = workers.replace(old, new, 1)
    WORKERS.write_text(workers, encoding="utf-8")

storage = STORAGE.read_text(encoding="utf-8")
workers = WORKERS.read_text(encoding="utf-8")
for token in [
    "public static BlockPos findProductionDepositTarget",
    "positions.addAll(SupplyDepotRegistryService.loadedPositions(level, data));",
    "positions.addAll(generalStoragePositions(data));",
    "return null;",
]:
    if token not in storage:
        raise SystemExit(f"storage invariant missing: {token}")
if "SettlementStorageService.findProductionDepositTarget(level, data, carried)" not in workers:
    raise SystemExit("worker production routing invariant missing")
if "target == null || !level.hasChunkAt(target)" not in workers:
    raise SystemExit("worker full-network backpressure invariant missing")

print("PRODUCTION SHARED DEPOT ROUTING PATCH PASS")
