#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A65=ROOT/'tools/test_alpha65_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A65).replace("print('Frontier Settlement alpha.23-65 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.65','0.1.0-alpha.66'); ns={'__file__':str(A65),'__name__':'__main__'}; exec(compile(a,str(A65),'exec'),ns,ns)
workers=text(JAVA/'settlement/SettlementWorkerService.java'); workshop=text(JAVA/'settlement/SettlementWorkshopService.java'); advanced=text(JAVA/'settlement/SettlementAdvancedWorkshopService.java'); service=text(JAVA/'settlement/SettlementService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
must(workers,('boolean localEvidenceLoaded = localProductionEvidenceLoaded(level, data)','workerRouteEvidenceLoaded(ServerLevel level, SettlementData data','SettlementStorageService.storagePositions(data)','Math.floorDiv(minX, 16)','level.hasChunkAt(probe)','SettlementAdvancedWorkshopService.allAssignmentsLoaded(level, data)','SettlementAdvancedWorkshopService.loadedAssignedWorkerCount(level, data)','+ transport + workshop + advanced','if (localEvidenceLoaded) {','SettlementAdvancedWorkshopService.firstMissingLoadedAssignment(level, data)','SettlementAdvancedWorkshopService.spawnAssignedWorker(level, data, missingAdvanced)','SettlementAdvancedWorkshopService.ADVANCED_WORKER_TAG'),'alpha.66 civilian evidence/lifecycle')
must(workshop,('SettlementWorkerService.workerRouteEvidenceLoaded(level, data, workshop.workCenter(), 12)','WorkshopLayout.serviceCrate(workshop)'),'alpha.66 workshop loaded assignment evidence')
must(advanced,('public static Villager spawnAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop)','SettlementWorkerService.workerRouteEvidenceLoaded(level, data, workshop.workCenter(), 12)','AdvancedWorkshopLayout.artisanHome(workshop)','findAssignedWorker(level, data, workshop) != null','if (!level.addFreshEntity(worker)) return null','return worker;'),'alpha.66 advanced artisan atomic spawn')
if 'SettlementAdvancedWorkshopService.spawnAssignedWorker(server.overworld(), missingAdvanced)' in service or 'BuildingRecord missingAdvanced =' in service:
    raise SystemExit('alpha.66 old free advanced-artisan spawn remains in SettlementService')
# Complete evidence must precede destructive population reconciliation.
evidence=workers.index('if (localEvidenceLoaded'); reconcile=workers.index('if (data.population() != actualPopulation) data.setPopulation(actualPopulation)', evidence)
if not evidence < reconcile: raise SystemExit('alpha.66 population reconciliation is not evidence-gated')
# Advanced replacement must use the same commitArrival path; addFreshEntity -> food -> population ordering stays owned by Alpha.64.
missing=workers.index('BuildingRecord missingAdvanced'); spawn=workers.index('SettlementAdvancedWorkshopService.spawnAssignedWorker',missing); commit=workers.index('commitArrival(server, level, data, arrival)',spawn)
if not missing < spawn < commit: raise SystemExit('alpha.66 advanced replacement does not use civilian arrival commit')
forbid(workers,('CIVILIAN_RESERVATION_LEDGER','UNLOADED_RESIDENT_COUNT','forceChunk','setChunkForced','teleportTo('),'alpha.66 no virtual resident/load authority')
must(props,('mod_version=0.1.0-alpha.66','loaded-evidence-safe civilian population/replacement authority'),'alpha.66 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.66"','Alpha.66 unifies the civilian lifecycle boundary'),'alpha.66 lock')
print('Frontier Settlement alpha.23-66 cumulative source audit: PASS')
