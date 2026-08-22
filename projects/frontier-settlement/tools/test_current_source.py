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
    JAVA / 'settlement/ConstructionState.java',
    JAVA / 'settlement/BuildingType.java',
    JAVA / 'settlement/BuildingBlueprints.java',
    JAVA / 'settlement/SettlementConstructionService.java',
    JAVA / 'settlement/RoadSegment.java',
    JAVA / 'settlement/RoadConstructionState.java',
    JAVA / 'settlement/SettlementRoadService.java',
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
    'mod_version=0.1.0-alpha.3',
):
    if token not in props:
        raise SystemExit(f'missing canonical property: {token}')

service = (JAVA / 'settlement/SettlementService.java').read_text(encoding='utf-8')
for token in (
    'Blocks.BARREL',
    'getTickCount()',
    'ItemTags.LOGS',
    'SettlementConstructionService.tick',
    'SettlementRoadService.tick',
    'data.roadConstruction().active()',
):
    if token not in service:
        raise SystemExit(f'core settlement invariant missing: {token}')

construction = (JAVA / 'settlement/SettlementConstructionService.java').read_text(encoding='utf-8')
for token in (
    'frontier_settlement_builder',
    'Heightmap.Types.MOTION_BLOCKING_NO_LEAVES',
    'max - min > 2',
    'level.getBlockEntity(pos) != null',
    '!level.getFluidState(surfaceBlock).isEmpty()',
    'if (!current.isAir())',
    'Blocks.COBBLESTONE.defaultBlockState()',
    'DIRECT_BLOCK_UPDATE = 2',
    'data.beginConstruction(type, site.origin())',
    'consumeCost(level, data, type)',
    'count(container, true) < type.woodCost()',
    'count(container, false) < type.stoneCost()',
    'data.roadConstruction().active()',
):
    if token not in construction:
        raise SystemExit(f'construction safety invariant missing: {token}')
if 'destroyBlock(' in construction or 'dropResources(' in construction:
    raise SystemExit('construction must not use drop-producing block destruction paths')

blueprints = (JAVA / 'settlement/BuildingBlueprints.java').read_text(encoding='utf-8')
phase_order = [
    'Phase.FLOOR',
    'Phase.FRAME_AND_WALLS',
    'Phase.ROOF',
    'Phase.FINISH',
]
first_positions = [blueprints.find(token) for token in phase_order]
if any(pos < 0 for pos in first_positions) or first_positions != sorted(first_positions):
    raise SystemExit('blueprint phase order must begin floor -> frame/walls -> roof -> finish')
for token in (
    'isHouseDoorOpening',
    'return Blocks.GLASS.defaultBlockState()',
    'Blocks.LANTERN.defaultBlockState()',
    'Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()',
    'StairBlock.FACING',
):
    if token not in blueprints:
        raise SystemExit(f'building blueprint invariant missing: {token}')
if blueprints.count('Blocks.LANTERN.defaultBlockState()') < 8:
    raise SystemExit('starter buildings must retain at least eight planned lantern placements total')
if blueprints.count('Blocks.GLASS.defaultBlockState()') < 2:
    raise SystemExit('both starter blueprints must retain glazed window logic')

road = (JAVA / 'settlement/SettlementRoadService.java').read_text(encoding='utf-8')
for token in (
    'ROAD_LENGTH = 16',
    'ROAD_WIDTH = 3',
    'ROAD_STONE_COST = 24L',
    'MAX_ROUTE_HEIGHT_VARIANCE = 1',
    'data.houseCount() < 1 || data.lumberCampCount() < 1',
    'distanceSqr < 25L || distanceSqr > 1024L',
    'max - min > MAX_ROUTE_HEIGHT_VARIANCE',
    'overlapsExistingRoad',
    'prepareRoute(level, route)',
    'DIRECT_BLOCK_UPDATE = 2',
    'Blocks.GRAVEL.defaultBlockState()',
    'Blocks.COBBLESTONE.defaultBlockState()',
    'if (!current.isAir()',
    'countStone(container) < ROAD_STONE_COST',
    'data.beginRoadConstruction',
    'data.completeRoad(new RoadSegment',
):
    if token not in road:
        raise SystemExit(f'road safety invariant missing: {token}')
if 'destroyBlock(' in road or 'dropResources(' in road:
    raise SystemExit('road construction must not use drop-producing block destruction paths')

saved = (JAVA / 'settlement/SettlementData.java').read_text(encoding='utf-8')
for token in (
    'server.getDataStorage().computeIfAbsent(TYPE)',
    'ConstructionState.CODEC',
    'RoadSegment.CODEC.listOf()',
    'RoadConstructionState.CODEC',
    'road_construction',
    'beginRoadConstruction',
    'completeRoad',
    'housing_capacity',
    'house_count',
    'lumber_camp_count',
):
    if token not in saved:
        raise SystemExit(f'persistent settlement invariant missing: {token}')

commands = (JAVA / 'command/SettlementCommands.java').read_text(encoding='utf-8')
for token in ('Commands.literal("road")', 'SettlementRoadService.start', '도로 공사 중'):
    if token not in commands:
        raise SystemExit(f'road command/status invariant missing: {token}')

print('Frontier Settlement alpha.3 source audit: PASS')
