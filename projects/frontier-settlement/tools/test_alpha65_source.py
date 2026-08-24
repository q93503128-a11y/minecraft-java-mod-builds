#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A64=ROOT/'tools/test_alpha64_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A64).replace("print('Frontier Settlement alpha.23-64 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.64','0.1.0-alpha.65'); ns={'__file__':str(A64),'__name__':'__main__'}; exec(compile(a,str(A64),'exec'),ns,ns)
workers=text(JAVA/'settlement/SettlementWorkerService.java'); main=text(JAVA/'FrontierSettlement.java'); logistics=text(JAVA/'settlement/SettlementOutpostLogisticsService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
must(workers,('RESOURCE_WORKER_TAG = "frontier_settlement_resource_worker"','worker.addTag(RESOURCE_WORKER_TAG)','public static void onLivingDrops(LivingDropsEvent event)','instanceof Villager worker','SettlementOutpostLogisticsService.TRANSPORT_WORKER_TAG','if (!isManagedCargoWorker(worker)) return','ItemStack carried = worker.getMainHandItem()','event.getDrops().clear()','if (carried.isEmpty()) return','carried.copy()','SettlementWorkshopService.WORKSHOP_WORKER_TAG','Save-compatible fallback for pre-Alpha.65 ordinary workers','LUMBER_WORKER_NAME.equals(value)','FARM_WORKER_NAME.equals(value)','QUARRY_WORKER_NAME.equals(value)','MINE_WORKER_NAME.equals(value)'),'alpha.65 local civilian cargo recovery')
must(main,('SettlementWorkerService','NeoForge.EVENT_BUS.addListener(SettlementWorkerService::onLivingDrops)'),'alpha.65 event registration')
must(logistics,('public static void onLivingDrops(LivingDropsEvent event)','TRANSPORT_WORKER_TAG','event.getDrops().clear()','carried.copy()'),'alpha.63 transporter recovery retained')
# The local handler must reject transporters before clearing drops to avoid two recovery authorities.
start=workers.index('public static void onLivingDrops(LivingDropsEvent event)'); exclude=workers.index('SettlementOutpostLogisticsService.TRANSPORT_WORKER_TAG',start); clear=workers.index('event.getDrops().clear()',exclude)
if not start < exclude < clear: raise SystemExit('alpha.65 transporter exclusion must precede local drop mutation')
# New ordinary workers must be tagged before addFreshEntity; legacy names remain fallback only.
spawn=workers.index('private static Villager spawnWorker'); tag=workers.index('worker.addTag(RESOURCE_WORKER_TAG)',spawn); add=workers.index('level.addFreshEntity(worker)',tag)
if not spawn < tag < add: raise SystemExit('alpha.65 resource worker tag must be applied before entity add')
forbid(workers,('LOCAL_CARGO_LEDGER','RECOVERY_BALANCE','forceChunk','setChunkForced','teleportTo('),'alpha.65 no virtual recovery authority')
must(props,('mod_version=0.1.0-alpha.65','exact death recovery for real MAINHAND cargo carried by local production/workshop civilians'),'alpha.65 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.65"','Alpha.65 gives local production/workshop civilians exact physical MAINHAND cargo recovery on death'),'alpha.65 lock')
print('Frontier Settlement alpha.23-65 cumulative source audit: PASS')
