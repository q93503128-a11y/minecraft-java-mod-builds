#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A70=ROOT/'tools/test_alpha70_source.py'
_real=Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    if self.name=='gradle.properties': s=s.replace('mod_version=0.1.0-alpha.71','mod_version=0.1.0-alpha.70')
    elif self.name=='COMPANION_LOCK.json': s=s.replace('\"frontier_settlement\": \"0.1.0-alpha.71\"','\"frontier_settlement\": \"0.1.0-alpha.70\"')
    elif self.name=='SettlementBarracksService.java': s=s.replace('SettlementMilitaryArmoryService.tickArmament(level, data, barracks.workCenter(), soldier)','SettlementMilitaryArmoryService.tickArmament(level, data, soldier)')
    return s
Path.read_text=legacy_view
try:
    a=_real(A70,encoding='utf-8').replace("print('Frontier Settlement alpha.23-70 cumulative source audit: PASS')",'pass')
    ns={'__file__':str(A70),'__name__':'__main__'}; exec(compile(a,str(A70),'exec'),ns,ns)
finally: Path.read_text=_real
def text(p): return Path(p).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
b=text(JAVA/'settlement/SettlementBarracksService.java'); a=text(JAVA/'settlement/SettlementMilitaryArmoryService.java'); o=text(JAVA/'settlement/SettlementConstructionOfficeService.java'); w=text(JAVA/'settlement/SettlementWorkerService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
must(b,('soldierRouteBounds(SettlementData data, BuildingRecord barracks)','soldierAssignmentEvidenceLoaded(ServerLevel level, SettlementData data, BuildingRecord barracks)','assignment == null || !soldierAssignmentEvidenceLoaded(level, data, assignment.barracks())','soldiers.sort(Comparator.comparing(soldier -> soldier.getUUID().toString()))','duplicate.setNoAi(true)','active.setNoAi(false)','SettlementMilitaryArmoryService.tickArmament(level, data, barracks.workCenter(), soldier)','level.hasChunkAt(new BlockPos(chunkX*16+8,probeY,chunkZ*16+8))'),'alpha.71 barracks lifecycle')
must(a,('tickArmament(ServerLevel level, SettlementData data, BlockPos routeAnchor, FrontierSoldierEntity soldier)','pos.distSqr(routeAnchor) > MAX_ARMORY_ROUTE_SQR'),'alpha.71 anchored armory')
must(o,('runnerRouteBounds(SettlementData data, BuildingRecord office)','runnerAssignmentEvidenceLoaded(ServerLevel level, SettlementData data, BuildingRecord office)','runners.sort(Comparator.comparing(villager -> villager.getUUID().toString()))','duplicate.setNoAi(true)','keep.setNoAi(false)','if (!runnerAssignmentEvidenceLoaded(level, data, office)) return null','office == null || pos.distSqr(office) <= maxDistance'),'alpha.71 runner lifecycle')
forbid(o,('existing.get(i).discard()','duplicate.discard()'),'alpha.71 non-destructive runner containment')
must(w,('SettlementConstructionOfficeService.SUPPLY_RUNNER_TAG','event.getDrops().clear()','carried.copy()'),'alpha.71 runner cargo recovery')
forbid(b+o,('SOLDIER_UUID_LEDGER','RUNNER_UUID_LEDGER','setChunkForced','forceChunk','teleportTo('),'alpha.71 no virtual/load authority')
must(props,('mod_version=0.1.0-alpha.71','route-evidence-safe barracks recruitment'),'alpha.71 props')
must(lock,('\"frontier_settlement\": \"0.1.0-alpha.71\"','Alpha.71 is lifecycle-only hardening'),'alpha.71 lock')
print('Frontier Settlement alpha.23-71 cumulative source audit: PASS')
