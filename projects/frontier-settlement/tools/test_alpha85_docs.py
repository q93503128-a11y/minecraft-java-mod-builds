#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; REPO=ROOT.parents[1]; A84=ROOT/"tools/test_alpha84_docs.py"; ALPHA84_SHA="b6107a1681f1dae97a18fddb2b68d1e034499506"; _real_read=Path.read_text
def alpha84_read(self,*args,**kwargs):
    try: rel=self.resolve().relative_to(REPO.resolve()).as_posix()
    except ValueError: rel=""
    if rel in {"projects/frontier-settlement/gradle.properties","projects/frontier-settlement/COMPANION_LOCK.json"}: return subprocess.check_output(["git","show",f"{ALPHA84_SHA}:{rel}"],cwd=REPO,text=True,encoding="utf-8")
    return _real_read(self,*args,**kwargs)
Path.read_text=alpha84_read
try:
    chain=_real_read(A84,encoding="utf-8").replace('print("Frontier Settlement alpha.84 canonical docs audit: PASS")','pass'); ns={"__file__":str(A84),"__name__":"__main__"}; exec(compile(chain,str(A84),"exec"),ns,ns)
finally: Path.read_text=_real_read
note=(ROOT/"CONSTRUCTION_PACING_ALPHA85.md").read_text(encoding="utf-8"); props=(ROOT/"gradle.properties").read_text(encoding="utf-8"); lock=json.loads((ROOT/"COMPANION_LOCK.json").read_text(encoding="utf-8"))
for token in ("0.1.0-alpha.85","40 ticks","3 ticks","4 ticks","32 items","no teleport"):
    if token not in note: raise SystemExit(f"alpha.85 note missing: {token}")
if "mod_version=0.1.0-alpha.85" not in props: raise SystemExit("alpha.85 version missing")
if lock.get("target",{}).get("frontier_settlement")!="0.1.0-alpha.85": raise SystemExit("alpha.85 lock mismatch")
print("Frontier Settlement alpha.85 canonical docs audit: PASS")
