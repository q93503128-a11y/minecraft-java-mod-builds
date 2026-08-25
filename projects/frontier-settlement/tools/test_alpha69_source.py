#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A68=ROOT/'tools/test_alpha68_source.py'
def real_text(p): return Path(p).read_text(encoding='utf-8')
_real = Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    name=self.name
    if name=='gradle.properties': s=s.replace('mod_version=0.1.0-alpha.69','mod_version=0.1.0-alpha.68')
    elif name=='COMPANION_LOCK.json': s=s.replace('"frontier_settlement": "0.1.0-alpha.69"','"frontier_settlement": "0.1.0-alpha.68"')
    elif name in ('SettlementWorkshopService.java','SettlementAdvancedWorkshopService.java'):
        s=s.replace('|| !findAssignedWorkers(level, data, workshop).isEmpty()) return null;','|| findAssignedWorker(level, data, workshop) != null) return null;')
    elif name=='SettlementOutpostLogisticsService.java':
        s=s.replace('|| !findAssignedWorkers(level, data, outpost).isEmpty()) return null;','|| findAssignedWorker(level, data, outpost) != null) return null;')
    return s
Path.read_text=legacy_view
try:
    a=_real(A68,encoding='utf-8').replace("print('Frontier Settlement alpha.23-68 cumulative source audit: PASS')",'pass')
    ns={'__file__':str(A68),'__name__':'__main__'}; exec(compile(a,str(A68),'exec'),ns,ns)
finally:
    Path.read_text=_real

def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
workshop=real_text(JAVA/'settlement/SettlementWorkshopService.java'); advanced=real_text(JAVA/'settlement/SettlementAdvancedWorkshopService.java'); outpost=real_text(JAVA/'settlement/SettlementOutpostLogisticsService.java'); props=real_text(ROOT/'gradle.properties')
for label,s,role in (('workshop',workshop,'WORKSHOP_WORKER_TAG'),('advanced',advanced,'ADVANCED_WORKER_TAG')):
    must(s,('private static List<Villager> findAssignedWorkers(ServerLevel level, SettlementData data, BuildingRecord workshop)','assigned.sort(Comparator.comparing(villager -> villager.getUUID().toString()))','for (Villager worker : findAssignedWorkers(level, data, workshop)) ids.add(worker.getUUID());','if (findAssignedWorkers(level, data, workshop).isEmpty()) return workshop;','|| !findAssignedWorkers(level, data, workshop).isEmpty()) return null;',role),f'alpha.69 {label}')
must(outpost,('private static List<Villager> findAssignedWorkers(ServerLevel level, SettlementData data, OutpostRecord outpost)','for (Villager worker : findAssignedWorkers(level, data, outpost)) ids.add(worker.getUUID());','if (findAssignedWorkers(level, data, outpost).isEmpty()) return outpost;','|| !findAssignedWorkers(level, data, outpost).isEmpty()) return null;','found.sort(Comparator.comparing(villager -> villager.getUUID().toString()))'),'alpha.69 outpost')
for label,s in (('workshop',workshop),('advanced',advanced),('outpost',outpost)):
    forbid(s,('DUPLICATE_WORKER_LEDGER','WORKER_UUID_LEDGER','setChunkForced','forceChunk','teleportTo('),f'alpha.69 {label} no virtual/destructive authority')
must(props,('mod_version=0.1.0-alpha.69','multiplicity-safe assigned-worker accounting'),'alpha.69 props')
print('Frontier Settlement alpha.23-69 cumulative source audit: PASS')
