#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A58=ROOT/'tools/test_alpha58_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A58).replace("print('Frontier Settlement alpha.23-58 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.58','0.1.0-alpha.59'); ns={'__file__':str(A58),'__name__':'__main__'}; exec(compile(a,str(A58),'exec'),ns,ns)
auth=text(JAVA/'settlement/SettlementProjectAuthority.java'); construction=text(JAVA/'settlement/SettlementConstructionService.java'); road=text(JAVA/'settlement/SettlementRoadService.java'); outpost=text(JAVA/'settlement/SettlementOutpostService.java'); civil=text(JAVA/'settlement/SettlementCivilWorkService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json'); building=text(JAVA/'settlement/BuildingType.java')
must(auth,('public final class SettlementProjectAuthority','public static boolean anyActive(MinecraftServer server, SettlementData data)','data.construction().active()','data.roadConstruction().active()','data.outpostConstruction().active()','SettlementCivilWorkData.get(server).project().active()','UI/network/commands may pre-check'),'alpha.59 central project authority')
forbid(auth,('SavedData','Codec','ItemStack','setBlock(','new Thread(','CompletableFuture','parallelStream('),'alpha.59 authority must be a read-only shared gate')
if construction.count('SettlementProjectAuthority.anyActive(server, data)') < 2: raise SystemExit('alpha.59 construction preview/start must both use central authority')
if road.count('SettlementProjectAuthority.anyActive(server, data)') < 1: raise SystemExit('alpha.59 road preview/start path must use central authority')
if outpost.count('SettlementProjectAuthority.anyActive(server, data)') < 2: raise SystemExit('alpha.59 outpost preview/start must both use central authority')
if civil.count('SettlementProjectAuthority.anyActive(server, settlement)') < 2: raise SystemExit('alpha.59 civil preview/start must both use central authority')
must(construction,('현재 공동 공사가 끝난 뒤 새 건물을 배치해 주세요.','현재 공동 공사가 끝난 뒤 건물을 시작해 주세요.'),'alpha.59 building feedback')
must(road,('현재 공동 공사가 끝난 뒤 새 도로를 계획해 주세요.'),'alpha.59 road feedback')
must(outpost,('현재 공동 공사가 끝난 뒤 전초기지를 배치해 주세요.','현재 공동 공사가 끝난 뒤 전초기지를 시작해 주세요.'),'alpha.59 outpost feedback')
must(civil,('현재 공동 공사가 끝난 뒤 선택영역 토목을 계획해 주세요.','현재 공동 공사가 끝난 뒤 토목을 시작해 주세요.'),'alpha.59 civil feedback')
enum_block=building.split('public enum BuildingType {',1)[1].split(';',1)[0]; actual=[line.strip().split('(',1)[0] for line in enum_block.splitlines() if '(' in line]; expected=['HOUSE','LUMBER_CAMP','FARM','QUARRY','MINE','WAREHOUSE','CONSTRUCTION_OFFICE','BLACKSMITH','WORKSHOP','ADVANCED_WORKSHOP','GUARD_POST','WATCHTOWER','BARRACKS','MARKET','CART_STATION']
if actual!=expected: raise SystemExit(f'alpha.59 expected exact 15 functional building families, got: {actual}')
must(props,('mod_version=0.1.0-alpha.59','centralized service-level shared-project authority gate'),'alpha.59 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.59"','Alpha.59 centralizes building, road, outpost and civil-work exclusivity','no new save field, worker, currency, key or companion dependency','"status": "candidate_runtime_lock"'),'alpha.59 lock')
print('Frontier Settlement alpha.23-59 cumulative source audit: PASS')
