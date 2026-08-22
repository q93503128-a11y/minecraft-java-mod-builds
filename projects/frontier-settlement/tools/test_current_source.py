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
for token in (
    'minecraft_version=26.2',
    'neo_version=26.2.0.38-beta',
    'mod_id=frontier_settlement',
    'mod_version=0.1.0-alpha.5',
):
    if token not in props:
        raise SystemExit(f'missing canonical property: {token}')

service = (JAVA / 'settlement/SettlementService.java').read_text(encoding='utf-8')
for token in ('Blocks.BARREL', 'SettlementConstructionService.tick', 'SettlementRoadService.tick',
              'SettlementOutpostService.tick', 'SettlementWorkerService.tick'):
    if token not in service:
        raise SystemExit(f'core settlement invariant missing: {token}')

saved = (JAVA / 'settlement/SettlementData.java').read_text(encoding='utf-8')
for token in ('server.getDataStorage().computeIfAbsent(TYPE)', 'SettlementInfrastructureState.CODEC',
              'BuildingRecord', 'beginOutpostConstruction', 'completeOutpost', 'housing_capacity'):
    if token not in saved:
        raise SystemExit(f'persistent settlement invariant missing: {token}')

construction = (JAVA / 'settlement/SettlementConstructionService.java').read_text(encoding='utf-8')
for token in ('frontier_settlement_builder', 'Heightmap.Types.MOTION_BLOCKING_NO_LEAVES',
              'max - min > 2', 'Blocks.COBBLESTONE.defaultBlockState()'):
    if token not in construction:
        raise SystemExit(f'construction safety invariant missing: {token}')
if 'destroyBlock(' in construction or 'dropResources(' in construction:
    raise SystemExit('building construction must not use drop-producing block destruction paths')

road = (JAVA / 'settlement/SettlementRoadService.java').read_text(encoding='utf-8')
for token in ('ROAD_LENGTH = 16', 'ROAD_WIDTH = 3', 'MAX_ROUTE_HEIGHT_VARIANCE = 1',
              'Blocks.GRAVEL.defaultBlockState()', 'Blocks.COBBLESTONE.defaultBlockState()'):
    if token not in road:
        raise SystemExit(f'road invariant missing: {token}')
if 'destroyBlock(' in road or 'dropResources(' in road:
    raise SystemExit('road construction must not use drop-producing block destruction paths')

outpost = (JAVA / 'settlement/SettlementOutpostService.java').read_text(encoding='utf-8')
for token in ('WOOD_COST = 72L', 'STONE_COST = 48L', 'latestUnclaimedRoad',
              'Math.abs(surfaceY - roadY) > 1', 'data.beginOutpostConstruction', 'data.completeOutpost'):
    if token not in outpost:
        raise SystemExit(f'outpost invariant missing: {token}')
if 'destroyBlock(' in outpost or 'dropResources(' in outpost:
    raise SystemExit('outpost construction must not use drop-producing block destruction paths')

outpost_bp = (JAVA / 'settlement/OutpostBlueprints.java').read_text(encoding='utf-8')
for token in ('LENGTH = 9', 'WIDTH = 9', 'Blocks.OAK_FENCE.defaultBlockState()',
              'Blocks.GLASS.defaultBlockState()', 'Blocks.SPRUCE_SLAB.defaultBlockState()',
              'Blocks.BARREL.defaultBlockState()', 'Blocks.LANTERN.defaultBlockState()'):
    if token not in outpost_bp:
        raise SystemExit(f'outpost blueprint invariant missing: {token}')

worker = (JAVA / 'settlement/SettlementWorkerService.java').read_text(encoding='utf-8')
for token in ('ARRIVAL_FOOD_COST = 4L', 'MAX_LOGS_PER_TRIP = 6', 'BlockTags.LOGS',
              'hasLeavesAbove', 'SettlementInventory.insert', 'EquipmentSlot.MAINHAND'):
    if token not in worker:
        raise SystemExit(f'worker invariant missing: {token}')
if 'destroyBlock(' in worker or 'dropResources(' in worker:
    raise SystemExit('worker harvesting must not create loose drop entities through block destruction')

print('Frontier Settlement alpha.5 source audit: PASS')
