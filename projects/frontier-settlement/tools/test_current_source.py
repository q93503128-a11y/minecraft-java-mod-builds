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
    JAVA / 'settlement/SettlementOutpostService.java', JAVA / 'settlement/OutpostRecord.java',
    JAVA / 'settlement/SettlementInventory.java', JAVA / 'settlement/SettlementWorkerService.java',
    JAVA / 'settlement/SettlementTier.java', JAVA / 'command/SettlementCommands.java',
    JAVA / 'network/SettlementSnapshotPayload.java', JAVA / 'network/PlacementRequestPayload.java',
    JAVA / 'network/PlacementPreviewPayload.java', JAVA / 'network/SettlementNetwork.java',
    JAVA / 'client/SettlementHudOverlay.java', JAVA / 'client/BuildingPlacementClient.java',
    JAVA / 'client/PlacementGhostRenderer.java', JAVA / 'client/FrontierSettlementClient.java',
]
missing = [str(p.relative_to(ROOT)) for p in required if not p.is_file()]
if missing:
    raise SystemExit('missing required files: ' + ', '.join(missing))

props = (ROOT / 'gradle.properties').read_text(encoding='utf-8')
for token in ('minecraft_version=26.2', 'neo_version=26.2.0.38-beta',
              'mod_id=frontier_settlement', 'mod_version=0.1.0-alpha.13'):
    if token not in props:
        raise SystemExit(f'missing canonical property: {token}')

construction = (JAVA / 'settlement/SettlementConstructionService.java').read_text(encoding='utf-8')
for token in ('public static StartResult startAt', 'public static PlacementCheck checkPlacement',
              'MAX_MAIN_SETTLEMENT_RADIUS = 72', 'MAX_PLAYER_PLACEMENT_DISTANCE = 24',
              'BuildingRotation.fromId(rotationId)', 'rotation.rotatedWidth(type)',
              'RotatedBlueprints.create(type, construction.origin(), construction.rotation())',
              'data.beginConstruction(type, check.origin(), rotation)',
              'SettlementStorageService.consume(level, data, type.woodCost(), type.stoneCost(), 0L)',
              'lockedReason(SettlementData data, BuildingType type)'):
    if token not in construction:
        raise SystemExit(f'authoritative rotated placement invariant missing: {token}')
if 'findBuildSite(' in construction:
    raise SystemExit('building placement must not silently auto-pick a random nearby lot')
if 'destroyBlock(' in construction or 'dropResources(' in construction:
    raise SystemExit('building construction must not use drop-producing destruction paths')

rotation = (JAVA / 'settlement/BuildingRotation.java').read_text(encoding='utf-8')
for token in ('CLOCKWISE_90', 'CLOCKWISE_180', 'COUNTERCLOCKWISE_90',
              'rotateLocal', 'rotateState', 'rotatedWidth', 'rotatedDepth'):
    if token not in rotation:
        raise SystemExit(f'building rotation invariant missing: {token}')

record = (JAVA / 'settlement/BuildingRecord.java').read_text(encoding='utf-8')
for token in ('optionalFieldOf("rotation", 0)', 'localToWorld', 'rotatedWidth()', 'rotatedDepth()'):
    if token not in record:
        raise SystemExit(f'completed building rotation persistence missing: {token}')

state = (JAVA / 'settlement/ConstructionState.java').read_text(encoding='utf-8')
for token in ('int rotation, int step', 'optionalFieldOf("rotation", 0)', 'buildingRotation()'):
    if token not in state:
        raise SystemExit(f'active construction rotation persistence missing: {token}')

network = (JAVA / 'network/SettlementNetwork.java').read_text(encoding='utf-8')
for token in ('PROTOCOL = "2"', 'playToServer(PlacementRequestPayload.TYPE',
              'playToClient(PlacementPreviewPayload.TYPE', 'SettlementConstructionService.checkPlacement',
              'SettlementConstructionService.startAt', 'context.reply(new PlacementPreviewPayload'):
    if token not in network:
        raise SystemExit(f'placement networking invariant missing: {token}')

client = (JAVA / 'client/BuildingPlacementClient.java').read_text(encoding='utf-8')
for token in ('GLFW.GLFW_KEY_B', 'GLFW.GLFW_KEY_N', 'GLFW.GLFW_KEY_R', 'GLFW.GLFW_KEY_ENTER',
              'ClientPacketDistributor.sendToServer', 'refreshTicks = 5', 'ghostOrigin()',
              'preview.valid()', 'next.confirmed()'):
    if token not in client:
        raise SystemExit(f'placement client invariant missing: {token}')

ghost = (JAVA / 'client/PlacementGhostRenderer.java').read_text(encoding='utf-8')
for token in ('ExtractLevelRenderStateEvent', 'SubmitCustomGeometryEvent', 'submitShapeOutline',
              'RotatedBlueprints.create', 'VALID_COLOR', 'INVALID_COLOR'):
    if token not in ghost:
        raise SystemExit(f'3D placement ghost invariant missing: {token}')

warehouse_layout = (JAVA / 'settlement/WarehouseLayout.java').read_text(encoding='utf-8')
if 'warehouse.localToWorld' not in warehouse_layout:
    raise SystemExit('rotated warehouse storage positions must follow the building transform')

storage = (JAVA / 'settlement/SettlementStorageService.java').read_text(encoding='utf-8')
for token in ('BuildingType.WAREHOUSE', 'WarehouseLayout.storagePositions(building)',
              'public static SettlementResources scan', 'public static boolean consume',
              'public static ItemStack insert', 'allStorageLoaded'):
    if token not in storage:
        raise SystemExit(f'aggregate storage invariant missing: {token}')

worker = (JAVA / 'settlement/SettlementWorkerService.java').read_text(encoding='utf-8')
for token in ('FARM_WORKER_NAME', 'QUARRY_WORKER_NAME', 'MINE_WORKER_NAME', 'TRANSPORT_WORKER_NAME',
              'workFarm', 'workQuarry', 'workMine', 'Tags.Blocks.ORES',
              'SettlementStorageService.findDepositTarget', 'SettlementStorageService.insert'):
    if token not in worker:
        raise SystemExit(f'worker production/storage invariant missing: {token}')
if 'destroyBlock(' in worker or 'dropResources(' in worker:
    raise SystemExit('workers must not create loose drops through block destruction')

print('Frontier Settlement alpha.13 source audit: PASS')
