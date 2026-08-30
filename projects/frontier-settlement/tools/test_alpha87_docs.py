#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
A86 = ROOT / "tools/test_alpha86_docs.py"
ALPHA86_SHA = "132b6f09715f1f8225cb5f5e581f163bf43fe949"
_real_read = Path.read_text
def alpha86_read(self, *args, **kwargs):
    try: rel = self.resolve().relative_to(REPO.resolve()).as_posix()
    except ValueError: rel = ""
    if rel in {"projects/frontier-settlement/gradle.properties", "projects/frontier-settlement/COMPANION_LOCK.json"}:
        return subprocess.check_output(["git", "show", f"{ALPHA86_SHA}:{rel}"], cwd=REPO, text=True, encoding="utf-8")
    return _real_read(self, *args, **kwargs)
Path.read_text = alpha86_read
try:
    chain = _real_read(A86, encoding="utf-8").replace('print("Frontier Settlement alpha.86 canonical docs audit: PASS")', 'pass')
    ns = {"__file__": str(A86), "__name__": "__main__"}
    exec(compile(chain, str(A86), "exec"), ns, ns)
finally: Path.read_text = _real_read
note = (ROOT / "PRODUCTION_AND_FINISH_ALPHA87.md").read_text(encoding="utf-8")
props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
lock = json.loads((ROOT / "COMPANION_LOCK.json").read_text(encoding="utf-8"))
for token in ("0.1.0-alpha.87", "64-item stack", "48 blocks", "40 blocks", "AIR crop cell", "population 1", "4 housing capacity", "600 server ticks", "4 real food", "no periodic ordinary-civilian", "99% forever", "best-effort", "same-world update"):
    if token not in note: raise SystemExit(f"alpha.87 note missing: {token}")
if "mod_version=0.1.0-alpha.87" not in props: raise SystemExit("alpha.87 version missing")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.87": raise SystemExit("alpha.87 lock mismatch")
print("Frontier Settlement alpha.87 canonical docs audit: PASS")
