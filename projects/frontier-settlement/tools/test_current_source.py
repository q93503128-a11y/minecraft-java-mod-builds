#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
required = [
    ROOT / 'build.gradle',
    ROOT / 'gradle.properties',
    JAVA / 'FrontierSettlement.java',
    JAVA / 'settlement/SettlementData.java',
    JAVA / 'settlement/SettlementService.java',
    JAVA / 'settlement/SettlementInfrastructureState.java',
    JAVA / 'settlement/BuildingRecord.java',
    JAVA / 'settlement/ConstructionState.java',
    JAVA / 'settlement/BuildingType.java',
    JAVA / 'settlement/BuildingBlueprints.java',
    JAVA / 'settlement/SettlementConstructionService.java',
    JAVA / 'settlement/RoadSegment.java',
    JAVA / 'settlement/RoadConstructionState.java',
    JAVA / 'settlement/SettlementRoadService.java',
    JAVA / 'settlement/OutpostRecord.java',
    JAVA / 'settlement/OutpostConstructionState.java',
    JAVA / 'settlement/OutpostBlueprints.java',
    JAVA / 'settlement/SettlementOutpostService.java',
    JAVA / 'settlement/SettlementInventory.java',
    JAVA / 'settlement/SettlementWorkerService.java',
    JAVA / 'network/SettlementSnapshotPayload.java',
    JAVA / 'client/SettlementHudOverlay.java',
]
missing = [str(p.relative_to(ROOT)) for p in required if not p.is_file()]
if missing:
    raise SystemExit('missing required files: ' + ', '.join(missing))

props = (ROOT / 'gradle.properties').read_text(encoding='utf-8')
for token in ('minecraft_version=26.2', 'neo_version=26.2.0.38-beta', 'mod_id=frontier_settlement', 'mod_version=0.1.0-alpha.6'):
    if token not in props:
        raise SystemExit(f'missing canonical property: {token}')

service = (JAVA / 'settlement/SettlementService.java').read_text(encoding='utf-8')
for token in ('Blocks.BARREL', 'SettlementConstructionService.tick', 'SettlementRoadService.tick', 'SettlementOutpostService.tick', 'SettlementWorkerService.tick'):
    if token not in service:
        raise SystemExit(f'core settlement invariant missing: {token}')

saved = (JAVA / 'settlement/SettlementData.java').read_text(encoding='utf-8')
for token in ('server.getDataStorage().computeIfAbsent(TYPE)', 'SettlementInfrastructureState.CODEC', 'BuildingRecord', 'beginOutpostConstruction', 'completeOutpost', 'housing_capacity'):
    if token not in saved:
        raise SystemExit(f'persistent settlement invariant missing: {token}')

for filename in ('SettlementConstructionService.java', 'SettlementRoadService.java', 'SettlementOutpostService.java'):
    source = (JAVA / 'settlement' / filename).read_text(encoding='utf-8')
    if 'destroyBlock(' in source or 'dropResources(' in source:
        raise SystemExit(f'{filename} must not use drop-producing destruction paths')

worker = (JAVA / 'settlement/SettlementWorkerService.java').read_text(encoding='utf-8')
for token in ('ARRIVAL_FOOD_COST = 4L', 'MAX_LOGS_PER_TRIP = 6', 'MAX_TRANSPORT_STACK = 16',
              'TRANSPORT_WORKER_NAME', 'workTransport', 'takeFirstStack', 'road.start().above()',
              'SettlementInventory.insert', 'EquipmentSlot.MAINHAND', 'BlockTags.LOGS', 'hasLeavesAbove'):
    if token not in worker:
        raise SystemExit(f'worker/logistics invariant missing: {token}')
if 'destroyBlock(' in worker or 'dropResources(' in worker:
    raise SystemExit('workers must not create loose drops through block destruction')

outpost = (JAVA / 'settlement/SettlementOutpostService.java').read_text(encoding='utf-8')
for token in ('WOOD_COST = 72L', 'STONE_COST = 48L', 'latestUnclaimedRoad', 'data.completeOutpost'):
    if token not in outpost:
        raise SystemExit(f'outpost invariant missing: {token}')

print('Frontier Settlement alpha.6 source audit: PASS')
