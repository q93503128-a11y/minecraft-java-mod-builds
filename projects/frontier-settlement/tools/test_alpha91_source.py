from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"

def text(path): return path.read_text(encoding="utf-8")
def must(src, tokens, label):
    for token in tokens:
        if token not in src: raise SystemExit(f"{label} missing: {token}")
def forbid(src, tokens, label):
    for token in tokens:
        if token in src: raise SystemExit(f"{label} forbidden: {token}")

props = text(ROOT / "gradle.properties")
lock = json.loads(text(ROOT / "COMPANION_LOCK.json"))
worker = text(JAVA / "settlement/SettlementWorkerService.java")
storage = text(JAVA / "settlement/SettlementStorageService.java")
entity = text(JAVA / "content/FrontierWorkerEntity.java")
blueprints = text(JAVA / "settlement/BuildingBlueprints.java")
service = text(JAVA / "settlement/SettlementService.java")
core = text(JAVA / "settlement/SettlementCoreService.java")

must(props, ("mod_version=0.1.0-alpha.91", "Alpha.91 navigation/storage hardening"), "alpha91 props")
if lock["target"]["frontier_settlement"] != "0.1.0-alpha.91": raise SystemExit("alpha91 lock target")
if not any(str(x).startswith("Alpha.91 ") for x in lock.get("notes", [])): raise SystemExit("alpha91 lock note")
must(worker, (
    "TREE_SEARCH_RADIUS = 128", "QUARRY_SEARCH_RADIUS = 96", "MINE_HORIZONTAL_SEARCH_RADIUS = 48",
    "MINE_SEARCH_DEPTH = 80", "RESOURCE_TARGET_CACHE_TICKS", "RESOURCE_SEARCH_RETRY_TICKS",
    "STUCK_PROGRESS_TIMEOUT_TICKS", "path.canReach()", "createPath(approach, 0)",
    "deliverToWorksiteStorage", "worksiteStoragePosition", "farm.localToWorld(x, 1, z)",
    "resourceRouteMargin(type)", "findTreeForWorker", "findQuarryTargetForWorker"
), "worker hardening")
must(storage, (
    "PUBLIC_STOCKPILE_TARGET_BARRELS = 4", "publicStockpilePositions", "worksiteStoragePosition",
    "ensureManagedStorage", "generalStoragePositions", "positions.addAll(worksiteStoragePositions(data))"
), "storage hardening")
must(entity, ("PathType.WATER", "setPathfindingMalus(PathType.WATER, -1.0F)"), "water avoidance")
must(blueprints, ("b.put(5, 1, 6, Blocks.BARREL.defaultBlockState(), Phase.FINISH);",), "lumber barrel")
must(service, ("SettlementStorageService.ensureManagedStorage(server.overworld(), data)", "SettlementStorageService.ensureManagedStorage(level, data)"), "storage maintenance")
must(core, ("SettlementStorageService.isManagedStoragePosition(data, pos)",), "managed storage protection")
forbid(worker, ("farm.origin().offset(x, 1, z)",), "rotated farm bug")
forbid(worker, ("getNavigation().moveTo(\n                    approach.getX() + 0.5D",), "coordinate-only partial path")
print("Alpha.91 source audit: PASS")
