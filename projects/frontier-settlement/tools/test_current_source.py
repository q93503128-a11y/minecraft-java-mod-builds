#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
required = [
    ROOT / 'build.gradle', ROOT / 'gradle.properties',
    JAVA / 'FrontierSettlement.java',
    JAVA / 'settlement/SettlementData.java', JAVA / 'settlement/SettlementService.java',
    JAVA / 'settlement/SettlementInfrastructureState.java', JAVA / 'settlement/BuildingRecord.java',
    JAVA / 'settlement/BuildingType.java', JAVA / 'settlement/BuildingRotation.java',
    JAVA / 'settlement/BuildingBlueprints.java', JAVA / 'settlement/RotatedBlueprints.java',
    JAVA / 'settlement/WarehouseLayout.java', JAVA / 'settlement/SettlementStorageService.java',
    JAVA / 'settlement/SettlementConstructionService.java', JAVA / 'settlement/SettlementRoadService.java',
    JAVA / 'settlement/RoadConstructionState.java', JAVA / 'settlement/RoadSegment.java',
    JAVA / 'settlement/SettlementOutpostService.java', JAVA / 'settlement/SettlementOutpostProductionService.java',
    JAVA / 'settlement/OutpostRecord.java', JAVA / 'settlement/SettlementInventory.java',
    JAVA / 'settlement/SettlementWorkerService.java', JAVA / 'settlement/SettlementTier.java',
    JAVA / 'command/SettlementCommands.java',
    JAVA / 'network/SettlementSnapshotPayload.java', JAVA / 'network/PlacementRequestPayload.java',
    JAVA / 'network/PlacementPreviewPayload.java', JAVA / 'network/RoadPlacementRequestPayload.java',
    JAVA / 'network/RoadPreviewPayload.java', JAVA / 'network/SettlementNetwork.java',
    JAVA / 'client/SettlementHudOverlay.java', JAVA / 'client/BuildingPlacementClient.java',
    JAVA / 'client/PlacementGhostRenderer.java', JAVA / 'client/RoadPlacementClient.java',
    JAVA / 'client/RoadGhostRenderer.java', JAVA / 'client/FrontierSettlementClient.java',
]
missing = [str(p.relative_to(ROOT)) for p in required if not p.is_file()]
if missing:
    raise SystemExit('missing required files: ' + ', '.join(missing))

props = (ROOT / 'gradle.properties').read_text(encoding='utf-8')
for token in ('minecraft_version=26.2', 'neo_version=26.2.0.38-beta',
              'mod_id=frontier_settlement', 'mod_version=0.1.0-alpha.15'):
    if token not in props:
        raise SystemExit(f'missing canonical property: {token}')

construction = (JAVA / 'settlement/SettlementConstructionService.java').read_text(encoding='utf-8')
for token in ('public static StartResult startAt', 'public static PlacementCheck checkPlacement',
              'MAX_MAIN_SETTLEMENT_RADIUS = 72', 'MAX_PLAYER_PLACEMENT_DISTANCE = 24',
              'BuildingRotation.fromId(rotationId)', 'RotatedBlueprints.create',
              'data.beginConstruction(type, check.origin(), rotation)',
              'SettlementStorageService.consume(level, data, type.woodCost(), type.stoneCost(), 0L)'):
    if token not in construction:
        raise SystemExit(f'authoritative building placement invariant missing: {token}')
if 'destroyBlock(' in construction or 'dropResources(' in construction:
    raise SystemExit('building construction must not use drop-producing destruction paths')

road_state = (JAVA / 'settlement/RoadConstructionState.java').read_text(encoding='utf-8')
for token in ('Codec.INT.listOf().optionalFieldOf("path", List.of())', 'fromPath(List<BlockPos> centers)',
              'public RoadConstructionState(int startX, int startY, int startZ,', 'centers()'):
    if token not in road_state:
        raise SystemExit(f'road construction persistence/legacy invariant missing: {token}')

road_record = (JAVA / 'settlement/RoadSegment.java').read_text(encoding='utf-8')
for token in ('Codec.INT.listOf().optionalFieldOf("path", List.of())', 'fromPath(List<BlockPos> centers)',
              'public RoadSegment(int startX, int startY, int startZ,', 'containsXZ'):
    if token not in road_record:
        raise SystemExit(f'completed routed road invariant missing: {token}')

road = (JAVA / 'settlement/SettlementRoadService.java').read_text(encoding='utf-8')
for token in ('MAX_ROUTE_LENGTH = 96', 'public static RouteCheck checkRoute', 'public static StartResult startAt',
              'assessCandidate', 'manhattanPath', 'choose(xThenZ, zThenX)', 'MAX_STEP_HEIGHT = 1',
              'MAX_CROSS_SLOPE = 1', 'hasOrCanMakeSupport', 'prepareRoute(level, check.centers())',
              'data.beginRoadConstruction(check.centers())', 'RoadSegment.fromPath(road.centers())'):
    if token not in road:
        raise SystemExit(f'routed road invariant missing: {token}')
if 'destroyBlock(' in road or 'dropResources(' in road:
    raise SystemExit('road preparation must not create loose drops')

network = (JAVA / 'network/SettlementNetwork.java').read_text(encoding='utf-8')
for token in ('PROTOCOL = "3"', 'playToServer(PlacementRequestPayload.TYPE',
              'playToClient(PlacementPreviewPayload.TYPE', 'playToServer(RoadPlacementRequestPayload.TYPE',
              'playToClient(RoadPreviewPayload.TYPE', 'SettlementRoadService.checkRoute',
              'SettlementRoadService.startAt'):
    if token not in network:
        raise SystemExit(f'placement networking invariant missing: {token}')

building_client = (JAVA / 'client/BuildingPlacementClient.java').read_text(encoding='utf-8')
for token in ('GLFW.GLFW_KEY_B', 'GLFW.GLFW_KEY_N', 'GLFW.GLFW_KEY_R', 'GLFW.GLFW_KEY_ENTER',
              'RoadPlacementClient.cancel()', 'public static void cancel()'):
    if token not in building_client:
        raise SystemExit(f'building client invariant missing: {token}')

road_client = (JAVA / 'client/RoadPlacementClient.java').read_text(encoding='utf-8')
for token in ('GLFW.GLFW_KEY_J', 'GLFW.GLFW_KEY_ENTER', 'GLFW.GLFW_KEY_BACKSPACE',
              'BuildingPlacementClient.cancel()', 'ClientPacketDistributor.sendToServer',
              'RoadPlacementRequestPayload', 'ghostBlocks()', 'next.confirmed()'):
    if token not in road_client:
        raise SystemExit(f'road planning client invariant missing: {token}')

for renderer_name in ('PlacementGhostRenderer.java', 'RoadGhostRenderer.java'):
    renderer = (JAVA / 'client' / renderer_name).read_text(encoding='utf-8')
    for token in ('ExtractLevelRenderStateEvent', 'SubmitCustomGeometryEvent', 'submitShapeOutline',
                  'VALID_COLOR', 'INVALID_COLOR'):
        if token not in renderer:
            raise SystemExit(f'3D ghost invariant missing in {renderer_name}: {token}')

storage = (JAVA / 'settlement/SettlementStorageService.java').read_text(encoding='utf-8')
for token in ('BuildingType.WAREHOUSE', 'WarehouseLayout.storagePositions(building)',
              'public static SettlementResources scan', 'public static boolean consume',
              'public static ItemStack insert', 'allStorageChunksLoaded'):
    if token not in storage:
        raise SystemExit(f'aggregate storage invariant missing: {token}')

service = (JAVA / 'settlement/SettlementService.java').read_text(encoding='utf-8')
if 'SettlementOutpostProductionService.tick(server, data)' not in service:
    raise SystemExit('specialized outpost production must be part of the canonical server tick')

outpost_production = (JAVA / 'settlement/SettlementOutpostProductionService.java').read_text(encoding='utf-8')
for token in ('case "lumber" -> workLumber', 'case "quarry" -> workQuarry',
              'case "mining" -> workMine', 'case "agriculture" -> workAgriculture',
              'ensureAgriculturePlot', 'SettlementInventory.insert(container, carried)', 'Tags.Blocks.ORES'):
    if token not in outpost_production:
        raise SystemExit(f'productive outpost invariant missing: {token}')
if 'destroyBlock(' in outpost_production or 'dropResources(' in outpost_production:
    raise SystemExit('outpost workers must not produce loose item drops')

worker = (JAVA / 'settlement/SettlementWorkerService.java').read_text(encoding='utf-8')
if 'destroyBlock(' in worker or 'dropResources(' in worker:
    raise SystemExit('workers must not create loose drops through block destruction')

print('Frontier Settlement alpha.15 source audit: PASS')
