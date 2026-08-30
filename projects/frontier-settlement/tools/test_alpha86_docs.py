#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
A85 = ROOT / "tools/test_alpha85_docs.py"
ALPHA85_SHA = "8ec714a0a9c17bee51e67cd2c2840df65db31141"
_real_read = Path.read_text

def alpha85_read(self, *args, **kwargs):
    try:
        rel = self.resolve().relative_to(REPO.resolve()).as_posix()
    except ValueError:
        rel = ""
    if rel in {"projects/frontier-settlement/gradle.properties", "projects/frontier-settlement/COMPANION_LOCK.json"}:
        return subprocess.check_output(["git", "show", f"{ALPHA85_SHA}:{rel}"], cwd=REPO, text=True, encoding="utf-8")
    return _real_read(self, *args, **kwargs)

Path.read_text = alpha85_read
try:
    chain = _real_read(A85, encoding="utf-8").replace(
        'print("Frontier Settlement alpha.85 canonical docs audit: PASS")', 'pass')
    ns = {"__file__": str(A85), "__name__": "__main__"}
    exec(compile(chain, str(A85), "exec"), ns, ns)
finally:
    Path.read_text = _real_read

note = (ROOT / "CONSTRUCTION_PACING_ALPHA86.md").read_text(encoding="utf-8")
props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
lock = json.loads((ROOT / "COMPANION_LOCK.json").read_text(encoding="utf-8"))
for token in ("0.1.0-alpha.86", "367 blueprint placements", "121 grading cells", "64 items", "8 items", "2 ticks", "45–90 seconds", "No builder teleports"):
    if token not in note: raise SystemExit(f"alpha.86 note missing: {token}")
if "mod_version=0.1.0-alpha.86" not in props: raise SystemExit("alpha.86 version missing")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.86": raise SystemExit("alpha.86 lock mismatch")
print("Frontier Settlement alpha.86 canonical docs audit: PASS")
