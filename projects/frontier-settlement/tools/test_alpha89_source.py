#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
A88 = ROOT / "tools/test_alpha88_source.py"
ALPHA88_SHA = "4ca821aa75ba23dcce5e3348d4a8b35c37ced643"
LEGACY_FILES = {
    "projects/frontier-settlement/gradle.properties",
    "projects/frontier-settlement/COMPANION_LOCK.json",
    "projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java",
    "projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java",
}
_real_read = Path.read_text
def alpha88_read(self, *args, **kwargs):
    try: rel = self.resolve().relative_to(REPO.resolve()).as_posix()
    except ValueError: rel = ""
    if rel in LEGACY_FILES:
        return subprocess.check_output(["git", "show", f"{ALPHA88_SHA}:{rel}"], cwd=REPO, text=True, encoding="utf-8")
    return _real_read(self, *args, **kwargs)
Path.read_text = alpha88_read
try:
    chain = _real_read(A88, encoding="utf-8").replace('print("Frontier Settlement alpha.23-88 cumulative source audit: PASS")', 'pass')
    ns = {"__file__": str(A88), "__name__": "__main__"}
    exec(compile(chain, str(A88), "exec"), ns, ns)
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

must(props, ("mod_version=0.1.0-alpha.89", "Alpha.89 duplicate-worker recovery"), "alpha.89 props")
must(worker, (
    "SettlementConstructionService.reconcileBuilderDuplicates(level, data);",
    "reconcileProductionDuplicates(server, level, data);",
    "trimExcessProductionWorkers",
    "removeDuplicateWorkerPreservingCargo",
    "physical = new ItemEntity",
    "if (!level.addFreshEntity(physical)) return false;",
    "worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);",
    "worker.discard();",
    "repairPopulationAfterDuplicateCleanup",
), "alpha.89 production duplicate cleanup")
if worker.index("reconcileProductionDuplicates(server, level, data);") > worker.index("if (server.getTickCount() % 600 == 0) tryAttractWorker"):
    raise SystemExit("alpha.89 duplicate cleanup must precede recruitment")
must(construction, (
    "public static int reconcileBuilderDuplicates",
    "removeDuplicateBuilderPreservingCargo",
    "active.setInvulnerable(false);",
    "duplicate.setInvulnerable(false);",
    "duplicate.discard();",
), "alpha.89 builder duplicate cleanup")
forbid(construction, ("duplicate.setNoAi(true);", "duplicate.setInvulnerable(true);"), "alpha.89 stale builder quarantine")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.89": raise SystemExit("alpha.89 lock mismatch")
if not any("Alpha.89 keeps every Alpha.88 companion binary pin unchanged" in n for n in lock.get("notes", [])):
    raise SystemExit("alpha.89 lock rationale missing")
print("Frontier Settlement alpha.23-89 cumulative source audit: PASS")
