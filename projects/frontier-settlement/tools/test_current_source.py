#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
required = [
    ROOT / 'build.gradle', ROOT / 'gradle.properties',
    JAVA / 'FrontierSettlement.java',
    JAVA / 'settlement/SettlementData.java',
    JAVA / 'settlement/SettlementService.java',
    JAVA / 'settlement/SettlementInfrastructureState.java',
    JAVA / 'settlement/BuildingRecord.java',
    JAVA / 'settlement/BuildingType.java',
    JAVA / 'settlement/BuildingBlueprints.java',
    JAVA / 'settlement/SettlementConstructionService.java',
    JAVA / 'settlement/SettlementRoadService.java',
    JAVA / 'settlement/SettlementOutpostService.java',
    JAVA / 'settlement/OutpostRecord.java',
    JAVA / 'settlement/SettlementInventory.java',
    JAVA / 'settlement/SettlementWorkerService.java',
    JAVA / 'settlement/SettlementTier.java',
    JAVA / 'command/SettlementCommands.java',
    JAVA / 'network/SettlementSnapshotPayload.java',
    JAVA / 'client/SettlementHudOverlay.java',
]
missing = [str(p.relative_to(ROOT)) for p in required if not p.is_file()]
if missing:
    raise SystemExit('missing required files: ' + ', '.join(missing))

props = (ROOT / 'gradle.properties').read_text(encoding='utf-8')
for token in ('minecraft_version=26.2', 'neo_version=26.2.0.38-beta', 'mod_id=frontier_settlement', 'mod_version=0.1.0-alpha.11'):
    if token not in props:
        raise SystemExit(f'missing canonical property: {token}')

service = (JAVA / 'settlement/SettlementService.java').read_text(encoding='utf-8')
for token in ('Blocks.BARREL', 'SettlementConstructionService.tick', 'SettlementRoadService.tick', 'SettlementOutpostService.tick', 'SettlementWorkerService.tick'):
    if token not in service:
        raise SystemExit(f'core settlement invariant missing: {token}')

construction = (JAVA / 'settlement/SettlementConstructionService.java').read_text(encoding='utf-8')
for token in ('public static StartResult startAt', 'public static PlacementCheck checkPlacement',
              'COMMAND_PLACEMENT_DISTANCE = 10', 'MAX_MAIN_SETTLEMENT_RADIUS = 72',
              'overlapsInfrastructure', 'selectedCenter.getX() - type.width() / 2',
              '선택한 부지가', '선택한 위치에서 건설 주민이 작업을 시작합니다'):
    if token not in construction:
        raise SystemExit(f'player placement invariant missing: {token}')
if 'findBuildSite(' in construction:
    raise SystemExit('building placement must not silently auto-pick a random nearby lot')
if 'destroyBlock(' in construction or 'dropResources(' in construction:
    raise SystemExit('building construction must not use drop-producing destruction paths')

building_types = (JAVA / 'settlement/BuildingType.java').read_text(encoding='utf-8')
for token in ('FARM(', 'QUARRY(', 'MINE('):
    if token not in building_types:
        raise SystemExit(f'production building type missing: {token}')

blueprints = (JAVA / 'settlement/BuildingBlueprints.java').read_text(encoding='utf-8')
for token in ('case FARM -> farm(origin)', 'case QUARRY -> quarry(origin)', 'case MINE -> mine(origin)',
              'Blocks.FARMLAND.defaultBlockState()', 'Blocks.WHEAT.defaultBlockState()',
              'Blocks.STONECUTTER.defaultBlockState()', 'Blocks.FURNACE.defaultBlockState()',
              'Blocks.LANTERN.defaultBlockState()'):
    if token not in blueprints:
        raise SystemExit(f'production blueprint invariant missing: {token}')

saved = (JAVA / 'settlement/SettlementData.java').read_text(encoding='utf-8')
for token in ('server.getDataStorage().computeIfAbsent(TYPE)', 'SettlementInfrastructureState.CODEC',
              'buildingCount(BuildingType type)', 'case FARM, QUARRY, MINE -> { }', 'housing_capacity'):
    if token not in saved:
        raise SystemExit(f'persistent settlement invariant missing: {token}')

worker = (JAVA / 'settlement/SettlementWorkerService.java').read_text(encoding='utf-8')
for token in ('FARM_WORKER_NAME', 'QUARRY_WORKER_NAME', 'MINE_WORKER_NAME', 'TRANSPORT_WORKER_NAME',
              'workFarm', 'workQuarry', 'workMine', 'BlockStateProperties.AGE_7', 'Tags.Blocks.ORES',
              'SettlementInventory.insert'):
    if token not in worker:
        raise SystemExit(f'worker production invariant missing: {token}')
if 'destroyBlock(' in worker or 'dropResources(' in worker:
    raise SystemExit('workers must not create loose drops through block destruction')

inventory = (JAVA / 'settlement/SettlementInventory.java').read_text(encoding='utf-8')
for token in ('stack.is(Items.WHEAT)', 'stack.is(Items.CARROT)', 'stack.is(Items.POTATO)', 'stack.is(Items.BEETROOT)'):
    if token not in inventory:
        raise SystemExit(f'settlement staple food invariant missing: {token}')

outpost_record = (JAVA / 'settlement/OutpostRecord.java').read_text(encoding='utf-8')
for token in ('specialization', 'optionalFieldOf("specialization", "general")', 'specializationDisplayName'):
    if token not in outpost_record:
        raise SystemExit(f'outpost specialization persistence missing: {token}')

outpost = (JAVA / 'settlement/SettlementOutpostService.java').read_text(encoding='utf-8')
for token in ('detectSpecialization', 'Tags.Blocks.ORES', 'return "mining"', 'return "lumber"',
              'return "agriculture"', 'return "quarry"'):
    if token not in outpost:
        raise SystemExit(f'outpost specialization invariant missing: {token}')
if 'destroyBlock(' in outpost or 'dropResources(' in outpost:
    raise SystemExit('outpost construction must not create loose drops')

tier = (JAVA / 'settlement/SettlementTier.java').read_text(encoding='utf-8')
for token in ('CAMP("개척 캠프")', 'HAMLET("촌락")', 'VILLAGE("마을")',
              'FRONTIER_TOWN("개척 도시")', 'DOMAIN("영지")', 'public static SettlementTier current'):
    if token not in tier:
        raise SystemExit(f'settlement tier invariant missing: {token}')

print('Frontier Settlement alpha.11 source audit: PASS')
