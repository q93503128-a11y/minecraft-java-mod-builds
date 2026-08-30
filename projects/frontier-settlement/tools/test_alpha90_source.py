#!/usr/bin/env python3
import json
import shutil
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
ALPHA89_SHA = "0536cf2e2d554005ca0683f3ec3ff6f9b0a91d26"

tmp = Path(tempfile.mkdtemp(prefix="frontier-alpha89-audit-"))
try:
    subprocess.run(["git", "worktree", "add", "--detach", str(tmp), ALPHA89_SHA], cwd=REPO, check=True,
                   stdout=subprocess.DEVNULL)
    subprocess.run(["python3", str(tmp / "projects/frontier-settlement/tools/test_alpha89_source.py")],
                   cwd=tmp, check=True)
finally:
    subprocess.run(["git", "worktree", "remove", "--force", str(tmp)], cwd=REPO,
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    shutil.rmtree(tmp, ignore_errors=True)

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
commands = text(JAVA / "command/SettlementCommands.java")
lock = json.loads(text(ROOT / "COMPANION_LOCK.json"))

must(props, ("mod_version=0.1.0-alpha.90", "Alpha.90 maintenance recovery"), "alpha.90 props")
must(commands, (
    'Commands.literal("normalize").executes(SettlementCommands::normalize)',
    "normalizeCompletedConstruction(server, data)",
    "normalizeLoadedBuilders(server.overworld(), data)",
    "normalizeLoadedWorkers(server, data)",
    "M 팔레트",
    "건설 주민 1명은 공동 건설 담당",
), "alpha.90 command")
forbid(commands, ("토목 | B 팔레트",), "stale key text")

must(worker, (
    "public record NormalizeResult",
    "public static NormalizeResult normalizeLoadedWorkers",
    "trimExcessLoadedTownWorkers",
    "loadedTownWorkersByName",
    "No unloaded resident can make N+1 loaded bodies legal",
    "private record TreeCandidate",
    "descendToTrunkBase",
    "isNaturalTreeBase",
    "hasWalkableApproach",
    "availableByItem.merge",
    "candidate.availableLogs()",
    "approaches.sort(Comparator.comparingDouble",
    "MAX_LOGS_PER_WORK = 16",
    "building.buildingType() != BuildingType.HOUSE",
), "alpha.90 worker")
forbid(worker, (
    "private static void reconcileProductionDuplicates",
    "if (!localProductionEvidenceLoaded(level, data)) return;\n        int removed = 0;\n        removed += trimExcessProductionWorkers",
), "alpha.90 duplicate gate")

must(construction, (
    "public record NormalizeConstructionResult",
    "public static NormalizeConstructionResult normalizeCompletedConstruction",
    "isRecoverableBlueprintDrift",
    "expected.is(Blocks.FARMLAND)",
    "current.is(Blocks.DIRT)",
    '100% "마감 확인"',
    "public static int normalizeLoadedBuilders",
    "N+1 loaded builders are definitive duplicate evidence",
    "SettlementProjectAuthority.anyActive(level.getServer(), data)",
), "alpha.90 construction")
forbid(construction, (
    "public static int reconcileBuilderDuplicates(ServerLevel level, SettlementData data) {\n        if (!builderAssignmentEvidenceLoaded(level, data)) return 0;",
), "alpha.90 builder duplicate gate")

if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.90":
    raise SystemExit("alpha.90 companion lock target mismatch")
notes = "\n".join(lock.get("notes", []))
must(notes, ("Alpha.90 keeps every Alpha.89 companion binary pin unchanged", "/frontier normalize",
             "no force-load"), "alpha.90 lock")

print("Frontier Settlement alpha.23-90 cumulative source audit: PASS")
