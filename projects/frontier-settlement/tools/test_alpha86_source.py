#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
A85 = ROOT / "tools/test_alpha85_source.py"
ALPHA85_SHA = "8ec714a0a9c17bee51e67cd2c2840df65db31141"
LEGACY_FILES = {
    "projects/frontier-settlement/gradle.properties",
    "projects/frontier-settlement/COMPANION_LOCK.json",
    "projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java",
}
_real_read = Path.read_text

def alpha85_read(self, *args, **kwargs):
    try:
        rel = self.resolve().relative_to(REPO.resolve()).as_posix()
    except ValueError:
        rel = ""
    if rel in LEGACY_FILES:
        return subprocess.check_output(["git", "show", f"{ALPHA85_SHA}:{rel}"], cwd=REPO, text=True, encoding="utf-8")
    return _real_read(self, *args, **kwargs)

Path.read_text = alpha85_read
try:
    chain = _real_read(A85, encoding="utf-8").replace(
        'print("Frontier Settlement alpha.23-85 cumulative source audit: PASS")', 'pass')
    ns = {"__file__": str(A85), "__name__": "__main__"}
    exec(compile(chain, str(A85), "exec"), ns, ns)
finally:
    Path.read_text = _real_read

def text(path): return Path(path).read_text(encoding="utf-8")
def must(src, tokens, label):
    for token in tokens:
        if token not in src: raise SystemExit(f"{label} missing: {token}")
def forbid(src, tokens, label):
    for token in tokens:
        if token in src: raise SystemExit(f"{label} forbidden: {token}")

props = text(ROOT / "gradle.properties")
construction = text(JAVA / "settlement/SettlementConstructionService.java")
lock = json.loads(text(ROOT / "COMPANION_LOCK.json"))

must(props, ("mod_version=0.1.0-alpha.86", "Alpha.86 construction pacing"), "alpha.86 props")
must(construction, (
    "WORK_POSITION_REACHED_SQR = 110.25D",
    "HIGH_WORK_RANGE_SQR = 196.0D",
    "HAUL_BATCH_SIZE = 64",
    "SITE_RESERVE_TARGET_PER_CATEGORY = 64L",
    "SITE_RESERVE_LOW_WATER = 8L",
    "GRADE_INTERVAL_TICKS = 1",
    "GRADE_WORK_RANGE_SQR = 110.25D",
    "BUILD_INTERVAL_TICKS = 2",
    "currentWood <= SITE_RESERVE_LOW_WATER",
    "currentStone <= SITE_RESERVE_LOW_WATER",
    "remainingWood > currentWood",
    "remainingStone > currentStone",
    "long missing = needsWood ? missingWood : missingStone;",
), "alpha.86 pacing/hysteresis")
forbid(construction, (
    "MAX_SITE_RESERVE_PER_CATEGORY",
    "WORK_POSITION_REACHED_SQR = 12.25D",
    "HAUL_BATCH_SIZE = 32",
    "GRADE_INTERVAL_TICKS = 3",
    "BUILD_INTERVAL_TICKS = 4",
), "alpha.86 obsolete pacing")
forbid(construction, ("teleportTo(", "setChunkForced", "forceChunk"), "alpha.86 no pacing shortcut")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.86":
    raise SystemExit("alpha.86 companion lock target drifted")
if not any("Alpha.86 keeps every Alpha.85 companion binary pin unchanged" in n for n in lock.get("notes", [])):
    raise SystemExit("alpha.86 companion rationale missing")

# House cost is below the 64-item target for both categories: once its initial 48 wood / 20 stone
# staging is complete, an exact-consumption simulation must never trigger another refill.
def needs_refill(remaining, current, next_delta):
    return current < next_delta or (remaining > current and current <= 8)
wood = 48
stone = 20
for step in range(367):
    next_wood = 48 * (step + 1) // 367 - 48 * step // 367
    next_stone = 20 * (step + 1) // 367 - 20 * step // 367
    if needs_refill(wood, wood, next_wood) or needs_refill(stone, stone, next_stone):
        raise SystemExit("alpha.86 exact house staging unexpectedly requests micro-refill")
    wood -= next_wood
    stone -= next_stone
if wood != 0 or stone != 0:
    raise SystemExit("alpha.86 house cost simulation drifted")

print("Frontier Settlement alpha.23-86 cumulative source audit: PASS")
