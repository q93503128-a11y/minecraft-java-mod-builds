#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
A88 = ROOT / "tools/test_alpha88_docs.py"
ALPHA88_SHA = "4ca821aa75ba23dcce5e3348d4a8b35c37ced643"
_real_read = Path.read_text
def alpha88_read(self, *args, **kwargs):
    try: rel = self.resolve().relative_to(REPO.resolve()).as_posix()
    except ValueError: rel = ""
    if rel in {"projects/frontier-settlement/gradle.properties", "projects/frontier-settlement/COMPANION_LOCK.json"}:
        return subprocess.check_output(["git", "show", f"{ALPHA88_SHA}:{rel}"], cwd=REPO, text=True, encoding="utf-8")
    return _real_read(self, *args, **kwargs)
Path.read_text = alpha88_read
try:
    chain = _real_read(A88, encoding="utf-8").replace('print("Frontier Settlement alpha.88 canonical docs audit: PASS")', 'pass')
    ns = {"__file__": str(A88), "__name__": "__main__"}
    exec(compile(chain, str(A88), "exec"), ns, ns)
finally: Path.read_text = _real_read
note = (ROOT / "DUPLICATE_WORKER_RECOVERY_ALPHA89.md").read_text(encoding="utf-8")
props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
lock = json.loads((ROOT / "COMPANION_LOCK.json").read_text(encoding="utf-8"))
for token in ("0.1.0-alpha.89", "exactly one", "UUID order", "MAINHAND", "ItemEntity", "600-tick", "NoAI=true", "invulnerable=true", "Natural Minecraft villagers are untouched", "Same-world update"):
    if token not in note: raise SystemExit(f"alpha.89 note missing: {token}")
if "mod_version=0.1.0-alpha.89" not in props: raise SystemExit("alpha.89 version missing")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.89": raise SystemExit("alpha.89 lock mismatch")
print("Frontier Settlement alpha.89 canonical docs audit: PASS")
