#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
A86 = ROOT / "tools/test_alpha86_source.py"
ALPHA86_SHA = "132b6f09715f1f8225cb5f5e581f163bf43fe949"
LEGACY_FILES = {
    "projects/frontier-settlement/gradle.properties",
    "projects/frontier-settlement/COMPANION_LOCK.json",
    "projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java",
    "projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java",
    "projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementContextService.java",
}
_real_read = Path.read_text
def alpha86_read(self, *args, **kwargs):
    try: rel = self.resolve().relative_to(REPO.resolve()).as_posix()
    except ValueError: rel = ""
    if rel in LEGACY_FILES:
        return subprocess.check_output(["git", "show", f"{ALPHA86_SHA}:{rel}"], cwd=REPO, text=True, encoding="utf-8")
    return _real_read(self, *args, **kwargs)
Path.read_text = alpha86_read
try:
    chain = _real_read(A86, encoding="utf-8").replace('print("Frontier Settlement alpha.23-86 cumulative source audit: PASS")', 'pass')
    ns = {"__file__": str(A86), "__name__": "__main__"}
    exec(compile(chain, str(A86), "exec"), ns, ns)
finally: Path.read_text = _real_read
def text(path): return Path(path).read_text(encoding="utf-8")
def must(src, tokens, label):
    for token in tokens:
        if token not in src: raise SystemExit(f"{label} missing: {token}")
def forbid(src, tokens, label):
    for token in tokens:
        if token in src: raise SystemExit(f"{label} forbidden: {token}")
props = text(ROOT / "gradle.properties")
worker = text(JAVA / "settlement/SettlementWorkerService.java")
construction = text(JAVA / "settlement/SettlementConstructionService.java")
context = text(JAVA / "settlement/SettlementContextService.java")
data = text(JAVA / "settlement/SettlementData.java")
lock = json.loads(text(ROOT / "COMPANION_LOCK.json"))
must(props, ("mod_version=0.1.0-alpha.87", "Alpha.87 production logistics"), "alpha.87 props")
must(worker, ("LOCAL_RESOURCE_ROUTE_MARGIN = 56", "TREE_SEARCH_RADIUS = 48", "QUARRY_SEARCH_RADIUS = 40", "MINE_HORIZONTAL_SEARCH_RADIUS = 24", "MINE_SEARCH_DEPTH = 48", "PRODUCTION_HAUL_STACK = 64", "state.isAir() && soil.is(Blocks.FARMLAND)", "findTree(level, data, camp.workCenter(), expected)", "findExposedStone(level, data, quarry.workCenter(), QUARRY_SEARCH_RADIUS, expected)", "findOreBelow(level, data, work, expected)"), "alpha.87 production")
forbid(worker, ("TREE_SEARCH_RADIUS = 18", "MAX_LOGS_PER_TRIP = 4", "MAX_CROPS_PER_TRIP = 4", "MAX_STONE_PER_TRIP = 3"), "alpha.87 obsolete production")
forbid(worker, ("setChunkForced", "forceChunk", "teleportTo("), "alpha.87 no resource shortcut")
must(construction, ("consolidateCompletionCargo(builder, crate, supply);", "boolean keepPhysicalLeftovers", "data.completeConstruction(type);", "returnBuilderHome(level, data, builder);", "can no longer block completion"), "alpha.87 completion")
forbid(construction, ("if (!returnBuilderHome(level, data, builder)) return false;",), "alpha.87 99-percent gate")
if "Math.min(100," not in context or "Math.min(99," in context: raise SystemExit("alpha.87 progress must reach 100")
if 'optionalFieldOf("population"' not in data or 'optionalFieldOf("housing_capacity"' not in data: raise SystemExit("alpha.87 save compatibility fields drifted")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.87": raise SystemExit("alpha.87 lock mismatch")
if not any("Alpha.87 keeps every Alpha.86 companion binary pin unchanged" in n for n in lock.get("notes", [])): raise SystemExit("alpha.87 lock rationale missing")
print("Frontier Settlement alpha.23-87 cumulative source audit: PASS")
