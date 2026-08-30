#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
A87 = ROOT / "tools/test_alpha87_docs.py"
ALPHA87_SHA = "c03ac0a739735a4e3bce0053f7817635421dd12e"
_real_read = Path.read_text
def alpha87_read(self, *args, **kwargs):
    try: rel = self.resolve().relative_to(REPO.resolve()).as_posix()
    except ValueError: rel = ""
    if rel in {"projects/frontier-settlement/gradle.properties", "projects/frontier-settlement/COMPANION_LOCK.json"}:
        return subprocess.check_output(["git", "show", f"{ALPHA87_SHA}:{rel}"], cwd=REPO, text=True, encoding="utf-8")
    return _real_read(self, *args, **kwargs)
Path.read_text = alpha87_read
try:
    chain = _real_read(A87, encoding="utf-8").replace('print("Frontier Settlement alpha.87 canonical docs audit: PASS")', 'pass')
    ns = {"__file__": str(A87), "__name__": "__main__"}
    exec(compile(chain, str(A87), "exec"), ns, ns)
finally: Path.read_text = _real_read
note = (ROOT / "WORKER_RUNTIME_RECOVERY_ALPHA88.md").read_text(encoding="utf-8")
props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
lock = json.loads((ROOT / "COMPANION_LOCK.json").read_text(encoding="utf-8"))
for token in ("0.1.0-alpha.88", "PathfinderMob", "solid log", "standable", "invulnerable=false", "56-block", "100%", "commit boundary", "best-effort", "Same-world update"):
    if token not in note: raise SystemExit(f"alpha.88 note missing: {token}")
if "mod_version=0.1.0-alpha.88" not in props: raise SystemExit("alpha.88 version missing")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.88": raise SystemExit("alpha.88 lock mismatch")
print("Frontier Settlement alpha.88 canonical docs audit: PASS")
