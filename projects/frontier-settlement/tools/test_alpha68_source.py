#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A67=ROOT/'tools/test_alpha67_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
# Preserve every Alpha.23-67 invariant while evaluating the current Alpha.68 version/lock.
a=text(A67).replace("print('Frontier Settlement alpha.23-67 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.67','0.1.0-alpha.68'); ns={'__file__':str(A67),'__name__':'__main__'}; exec(compile(a,str(A67),'exec'),ns,ns)
workers=text(JAVA/'settlement/SettlementWorkerService.java'); workshop=text(JAVA/'settlement/SettlementWorkshopService.java'); advanced=text(JAVA/'settlement/SettlementAdvancedWorkshopService.java'); outpost=text(JAVA/'settlement/SettlementOutpostLogisticsService.java'); routine=text(JAVA/'settlement/SettlementResidentRoutineService.java'); props=text(ROOT/'gradle.properties')
must(routine,('TOWN_WORKER_NAMES = Set.of(','"벌목 주민", "농사 주민", "채석 주민", "광산 주민", "작업장 주민"','moveToHouseSlot(residents.get(i), houses, i)','moveToHouseSlot(villager, houses, slot)'),'alpha.68 real house-rest call paths')
must(workers,(
    'static AABB workerRouteBounds(SettlementData data, BlockPos workCenter, int margin)',
    'if (building.buildingType() != BuildingType.HOUSE) continue',
    'building.originX() + building.rotatedWidth() - 1 + margin',
    'building.originZ() + building.rotatedDepth() - 1 + margin',
    'return workerBoundsFullyLoaded(level, data, workerRouteBounds(data, workCenter, margin))',
    'Math.floor(Math.nextDown(bounds.maxX))',
    'workersByName(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME)',
    'Set<java.util.UUID> ids = new HashSet<>()',
    'AABB search = workerRouteBounds(data, building.workCenter(), 24)',
),'alpha.68 local civilian lookup/evidence envelope')
for fixed in ('center.getX() - 256.0D','center.getX() + 257.0D'):
    if fixed in workers: raise SystemExit(f'alpha.68 old ordinary fixed-radius authority remains: {fixed}')
must(workshop,(
    'SettlementWorkerService.workerRouteEvidenceLoaded(level, data, workshop.workCenter(), 12)',
    'SettlementWorkerService.workerRouteBounds(data, workshop.workCenter(), 12)',
    '!level.hasChunkAt(WorkshopLayout.serviceCrate(workshop))',
),'alpha.68 workshop evidence/search/spawn')
forbid(workshop,('ASSIGNMENT_SEARCH_RADIUS = 192.0D',),'alpha.68 workshop fixed-radius mismatch')
must(advanced,('SettlementWorkerService.workerRouteBounds(data, workshop.workCenter(), 12)',),'alpha.68 advanced search envelope')
forbid(advanced,('ASSIGNMENT_SEARCH_RADIUS = 192.0D',),'alpha.68 advanced fixed-radius mismatch')
must(outpost,(
    'Town-side transporters may intentionally sleep in completed houses',
    'if (building.buildingType() != BuildingType.HOUSE) continue',
    'building.originX() + building.rotatedWidth() - 1',
    'building.originZ() + building.rotatedDepth() - 1',
    'return new AABB(minX - ROUTE_SEARCH_MARGIN',
),'alpha.68 transporter house-rest routeBounds')
# Do not solve lifecycle visibility by loading/teleporting or adding a reservation authority.
for label,s in (('workers',workers),('workshop',workshop),('advanced',advanced),('outpost',outpost)):
    forbid(s,('setChunkForced','forceChunk','teleportTo(','CIVILIAN_RESERVATION_LEDGER','WORKER_UUID_LEDGER'),f'alpha.68 {label} no force/virtual lifecycle authority')
must(props,('mod_version=0.1.0-alpha.68','rest-anchor-aware civilian lifecycle evidence'),'alpha.68 props')
print('Frontier Settlement alpha.23-68 cumulative source audit: PASS')
