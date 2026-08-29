#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
A83 = ROOT / "tools/test_alpha83_docs.py"
ALPHA83_SHA = "47d8da5bb29d5019bead9b28771a2fb1416c8584"
_real_read = Path.read_text

def legacy_read(self, *args, **kwargs):
    if self.name in {"gradle.properties", "COMPANION_LOCK.json"}:
        rel = self.resolve().relative_to(REPO.resolve()).as_posix()
        return subprocess.check_output(["git", "show", f"{ALPHA83_SHA}:{rel}"], cwd=REPO, text=True, encoding="utf-8")
    return _real_read(self, *args, **kwargs)

Path.read_text = legacy_read
try:
    chain = _real_read(A83, encoding="utf-8").replace(
        'print("Frontier Settlement alpha.83 canonical docs audit: PASS")', 'pass')
    ns = {"__file__": str(A83), "__name__": "__main__"}
    exec(compile(chain, str(A83), "exec"), ns, ns)
finally:
    Path.read_text = _real_read

note = (ROOT / "WORKER_RUNTIME_ALPHA84.md").read_text(encoding="utf-8")
props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
lock = json.loads((ROOT / "COMPANION_LOCK.json").read_text(encoding="utf-8"))
for token in ("0.1.0-alpha.84", "Villager", "PathfinderMob", "부지 정리 0%", "no teleport or force-load"):
    if token not in note: raise SystemExit(f"alpha.84 note missing: {token}")
if "mod_version=0.1.0-alpha.84" not in props: raise SystemExit("alpha.84 version missing")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.84": raise SystemExit("alpha.84 lock mismatch")
print("Frontier Settlement alpha.84 canonical docs audit: PASS")
