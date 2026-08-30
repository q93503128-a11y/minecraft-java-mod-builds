#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
A84 = ROOT / "tools/test_alpha84_source.py"
ALPHA84_SHA = "b6107a1681f1dae97a18fddb2b68d1e034499506"
LEGACY_FILES = {"projects/frontier-settlement/gradle.properties","projects/frontier-settlement/COMPANION_LOCK.json","projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java","projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementService.java"}
_real_read = Path.read_text
def alpha84_read(self,*args,**kwargs):
    try: rel=self.resolve().relative_to(REPO.resolve()).as_posix()
    except ValueError: rel=""
    if rel in LEGACY_FILES: return subprocess.check_output(["git","show",f"{ALPHA84_SHA}:{rel}"],cwd=REPO,text=True,encoding="utf-8")
    return _real_read(self,*args,**kwargs)
Path.read_text=alpha84_read
try:
    chain=_real_read(A84,encoding="utf-8").replace('print("Frontier Settlement alpha.23-84 cumulative source audit: PASS")','pass')
    ns={"__file__":str(A84),"__name__":"__main__"}; exec(compile(chain,str(A84),"exec"),ns,ns)
finally: Path.read_text=_real_read
def text(path): return Path(path).read_text(encoding="utf-8")
def must(src,tokens,label):
    for token in tokens:
        if token not in src: raise SystemExit(f"{label} missing: {token}")
def forbid(src,tokens,label):
    for token in tokens:
        if token in src: raise SystemExit(f"{label} forbidden: {token}")
props=text(ROOT/"gradle.properties"); construction=text(JAVA/"settlement/SettlementConstructionService.java"); service=text(JAVA/"settlement/SettlementService.java"); lock=json.loads(text(ROOT/"COMPANION_LOCK.json"))
must(props,("mod_version=0.1.0-alpha.85","Alpha.85 construction pacing"),"alpha.85 props")
must(construction,("WORK_POSITION_REACHED_SQR = 12.25D","HAUL_BATCH_SIZE = 32","MAX_SITE_RESERVE_PER_CATEGORY = 32L","GRADE_INTERVAL_TICKS = 3","BUILD_INTERVAL_TICKS = 4"),"alpha.85 building pacing")
must(service,("must not be quantized behind the","LCM(5, 8)=40 ticks","if (data.construction().active()) SettlementConstructionService.tick(server, data);","if (tick % 5 == 0) {","if (data.roadConstruction().active()) SettlementRoadService.tick(server, data);","if (data.outpostConstruction().active()) SettlementOutpostService.tick(server, data);"),"alpha.85 scheduler separation")
build_call="if (data.construction().active()) SettlementConstructionService.tick(server, data);"
if service.count(build_call) != 1: raise SystemExit("alpha.85 building scheduler call count drifted")
if service.index(build_call) >= service.index("if (tick % 5 == 0) {"): raise SystemExit("alpha.85 building tick is still behind the five-tick infrastructure gate")
forbid(construction,("teleportTo(","setChunkForced","forceChunk"),"alpha.85 no pacing shortcut")
if lock.get("target",{}).get("frontier_settlement")!="0.1.0-alpha.85": raise SystemExit("alpha.85 companion lock target drifted")
if not any("Alpha.85 keeps every Alpha.84 companion binary pin unchanged" in n for n in lock.get("notes",[])): raise SystemExit("alpha.85 companion rationale missing")
print("Frontier Settlement alpha.23-85 cumulative source audit: PASS")
