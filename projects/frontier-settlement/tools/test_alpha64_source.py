#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A63=ROOT/'tools/test_alpha63_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A63).replace("print('Frontier Settlement alpha.23-63 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.63','0.1.0-alpha.64'); ns={'__file__':str(A63),'__name__':'__main__'}; exec(compile(a,str(A63),'exec'),ns,ns)
workers=text(JAVA/'settlement/SettlementWorkerService.java'); logistics=text(JAVA/'settlement/SettlementOutpostLogisticsService.java'); workshop=text(JAVA/'settlement/SettlementWorkshopService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json'); building=text(JAVA/'settlement/BuildingType.java')
must(workers,('arrivalFoodAvailable(ServerLevel level, SettlementData data)','SettlementStorageService.scan(level, data).food() >= ARRIVAL_FOOD_COST','commitArrival(MinecraftServer server, ServerLevel level','if (arrival == null) return false','if (!consumeArrivalFood(level, data))','arrival.discard()','finishArrival(server, data)','Villager arrival = SettlementWorkshopService.spawnAssignedWorker(level, data, missingWorkshop)','Villager arrival = SettlementOutpostLogisticsService.spawnAssignedWorker(level, data, missing)','private static Villager spawnWorker','if (!level.addFreshEntity(worker)) return null'),'alpha.64 atomic worker arrivals')
spawn=workers.find('private static boolean commitArrival('); consume=workers.find('if (!consumeArrivalFood(level, data))',spawn); discard=workers.find('arrival.discard()',consume); finish=workers.find('finishArrival(server, data)',discard)
if min(spawn,consume,discard,finish)<0 or not (spawn < consume < discard < finish): raise SystemExit('alpha.64 arrival commit order invalid')
must(logistics,('public static Villager spawnAssignedWorker(ServerLevel level, SettlementData data, OutpostRecord outpost)','routeFullyLoaded(level, data, outpost)','findAssignedWorker(level, data, outpost) != null','if (!level.addFreshEntity(worker)) return null','return worker;'),'alpha.64 transporter assignment spawn')
must(workshop,('public static Villager spawnAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop)','findAssignedWorker(level, data, workshop) != null','if (!level.addFreshEntity(worker)) return null','return worker;'),'alpha.64 workshop assignment spawn')
forbid(workers,('consumeArrivalFood(level, data)) return;\n            SettlementWorkshopService.spawnAssignedWorker','consumeArrivalFood(level, data)) return;\n            SettlementOutpostLogisticsService.spawnAssignedWorker'),'alpha.64 no precharge specialist arrivals')
forbid(logistics,('TRANSPORT_REPLACEMENT_LEDGER','forceChunk','setChunkForced','teleportTo('),'alpha.64 transporter replacement authority')
enum_block=building.split('public enum BuildingType {',1)[1].split(';',1)[0]; actual=[line.strip().split('(',1)[0] for line in enum_block.splitlines() if '(' in line]; expected=['HOUSE','LUMBER_CAMP','FARM','QUARRY','MINE','WAREHOUSE','CONSTRUCTION_OFFICE','BLACKSMITH','WORKSHOP','ADVANCED_WORKSHOP','GUARD_POST','WATCHTOWER','BARRACKS','MARKET','CART_STATION']
if actual!=expected: raise SystemExit(f'alpha.64 expected exact 15 functional building families, got: {actual}')
must(props,('mod_version=0.1.0-alpha.64','atomic food-funded worker arrival commits'),'alpha.64 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.64"','Alpha.64 makes existing food-funded resident arrivals atomic'),'alpha.64 lock')
print('Frontier Settlement alpha.23-64 cumulative source audit: PASS')
