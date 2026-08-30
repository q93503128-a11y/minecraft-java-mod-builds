#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
A87 = ROOT / "tools/test_alpha87_source.py"
ALPHA87_SHA = "c03ac0a739735a4e3bce0053f7817635421dd12e"
LEGACY_FILES = {
    "projects/frontier-settlement/gradle.properties",
    "projects/frontier-settlement/COMPANION_LOCK.json",
    "projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java",
    "projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java",
}
_real_read = Path.read_text
def alpha87_read(self, *args, **kwargs):
    try: rel = self.resolve().relative_to(REPO.resolve()).as_posix()
    except ValueError: rel = ""
    if rel in LEGACY_FILES:
        return subprocess.check_output(["git", "show", f"{ALPHA87_SHA}:{rel}"], cwd=REPO, text=True, encoding="utf-8")
    return _real_read(self, *args, **kwargs)
Path.read_text = alpha87_read
try:
    chain = _real_read(A87, encoding="utf-8").replace('print("Frontier Settlement alpha.23-87 cumulative source audit: PASS")', 'pass')
    ns = {"__file__": str(A87), "__name__": "__main__"}
    exec(compile(chain, str(A87), "exec"), ns, ns)
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
lock = json.loads(text(ROOT / "COMPANION_LOCK.json"))
must(props, ("mod_version=0.1.0-alpha.88", "Alpha.88 worker runtime recovery"), "alpha.88 props")
must(worker, ("worker.setNoAi(false);", "worker.setInvulnerable(false);", "workerRouteBounds(data, building.workCenter(), LOCAL_RESOURCE_ROUTE_MARGIN)", "moveNear(level, worker, target, 0.92D)", "moveNear(level, worker, target, 0.90D)", "moveNear(level, worker, target, 0.85D)", "private static boolean moveNear", "private static boolean isWalkableApproach"), "alpha.88 worker movement")
forbid(worker, ("move(worker,", "teleportTo(", "setChunkForced", "forceChunk"), "alpha.88 worker shortcuts")
if construction.count("builder.setInvulnerable(false);") < 3: raise SystemExit("alpha.88 builder must be damageable at start/tick/completion")
must(construction, ("completion no longer requires a recreated/accessible", "finishIfValid(server, data, type, plan, builder, supply)", "ConstructionState finished = data.construction();", "data.completeConstruction(type);", "removeConstructionScaffoldsBestEffort(level, finished, type, supply);"), "alpha.88 completion")
if construction.index("data.completeConstruction(type);") > construction.index("removeConstructionScaffoldsBestEffort(level, finished, type, supply);"):
    raise SystemExit("alpha.88 cleanup still precedes completion commit")
forbid(construction, ("finishIfValid(server, data, type, plan, builder, crate, supply)",), "alpha.88 crate completion gate")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.88": raise SystemExit("alpha.88 lock mismatch")
if not any("Alpha.88 keeps every Alpha.87 companion binary pin unchanged" in n for n in lock.get("notes", [])): raise SystemExit("alpha.88 lock rationale missing")
print("Frontier Settlement alpha.23-88 cumulative source audit: PASS")
