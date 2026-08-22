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
    JAVA / 'settlement/BuildingBlueprints.java', JAVA / 'settlement/AdvancedBuildingBlueprints.java',
    JAVA / 'settlement/RotatedBlueprints.java', JAVA / 'settlement/WarehouseLayout.java',
    JAVA / 'settlement/SettlementStorageService.java', JAVA / 'settlement/SettlementConstructionService.java',
    JAVA / 'settlement/SettlementRoadService.java', JAVA / 'settlement/RoadConstructionState.java',
    JAVA / 'settlement/RoadSegment.java', JAVA / 'settlement/SettlementOutpostService.java',
    JAVA / 'settlement/SettlementOutpostProductionService.java', JAVA / 'settlement/SettlementBenefitService.java',
    JAVA / 'settlement/SettlementCoreService.java', JAVA / 'settlement/SettlementResidentRoutineService.java',
    JAVA / 'settlement/OutpostBlueprints.java', JAVA / 'settlement/OutpostConstructionState.java',
    JAVA / 'settlement/OutpostRecord.java', JAVA / 'settlement/SettlementInventory.java',
    JAVA / 'settlement/SettlementWorkerService.java', JAVA / 'settlement/SettlementTier.java',
    JAVA / 'command/SettlementCommands.java', JAVA / 'network/SettlementNetwork.java',
    JAVA / 'network/SettlementSnapshotPayload.java',
    JAVA / 'network/OutpostPlacementRequestPayload.java', JAVA / 'network/OutpostPreviewPayload.java',
    JAVA / 'client/ClientSettlementState.java', JAVA / 'client/SettlementHudOverlay.java',
    JAVA / 'client/BuildingPlacementClient.java', JAVA / 'client/RoadPlacementClient.java',
    JAVA / 'client/OutpostPlacementClient.java', JAVA / 'client/PlacementGhostRenderer.java',
    JAVA / 'client/RoadGhostRenderer.java', JAVA / 'client/OutpostGhostRenderer.java',
]
missing = [str(p.relative_to(ROOT)) for p in required if not p.is_file()]
if missing:
    raise SystemExit('missing required files: ' + ', '.join(missing))

props = (ROOT / 'gradle.properties').read_text(encoding='utf-8')
for token in ('minecraft_version=26.2', 'neo_version=26.2.0.38-beta',
              'mod_id=frontier_settlement', 'mod_version=0.1.0-alpha.18'):
    if token not in props:
        raise SystemExit(f'missing canonical property: {token}')

core = (JAVA / 'settlement/SettlementCoreService.java').read_text(encoding='utf-8')
for token in ('MAX_PLACEMENTS_PER_TICK = 6', 'SettlementTier.current(data)',
              'addFloor(placements, center, 1, Blocks.COARSE_DIRT.defaultBlockState())',
              'SettlementTier.HAMLET.ordinal()', 'SettlementTier.VILLAGE.ordinal()',
              'SettlementTier.FRONTIER_TOWN.ordinal()', 'SettlementTier.DOMAIN.ordinal()',
              'addLampRing', 'canSafelyReplace', 'isNaturalGround', 'level.setBlock'):
    if token not in core:
        raise SystemExit(f'tiered civic core invariant missing: {token}')
if 'destroyBlock(' in core or 'dropResources(' in core:
    raise SystemExit('civic core upgrades must never create loose drops')

routine = (JAVA / 'settlement/SettlementResidentRoutineService.java').read_text(encoding='utf-8')
for token in ('time >= 13000L && time < 23000L', 'TOWN_WORKER_NAMES',
              'BuildingType.HOUSE', 'house.localToWorld', 'villager.getNavigation().moveTo',
              'villager.getNavigation().stop()'):
    if token not in routine:
        raise SystemExit(f'resident routine invariant missing: {token}')

snapshot = (JAVA / 'network/SettlementSnapshotPayload.java').read_text(encoding='utf-8')
for token in ('int population, String tier', 'buf.writeUtf(payload.tier())', 'buf.readUtf()'):
    if token not in snapshot:
        raise SystemExit(f'tier snapshot invariant missing: {token}')

hud = (JAVA / 'client/SettlementHudOverlay.java').read_text(encoding='utf-8')
if 'String line = data.tier()' not in hud:
    raise SystemExit('persistent HUD must display the synced settlement tier')

building_type = (JAVA / 'settlement/BuildingType.java').read_text(encoding='utf-8')
for token in ('BLACKSMITH("blacksmith", "대장간"', 'GUARD_POST("guard_post", "경비초소"'):
    if token not in building_type:
        raise SystemExit(f'advanced building type missing: {token}')

blueprints = (JAVA / 'settlement/BuildingBlueprints.java').read_text(encoding='utf-8')
if 'case BLACKSMITH, GUARD_POST -> AdvancedBuildingBlueprints.create(type, origin)' not in blueprints:
    raise SystemExit('advanced building blueprint dispatch missing')

advanced = (JAVA / 'settlement/AdvancedBuildingBlueprints.java').read_text(encoding='utf-8')
for token in ('case BLACKSMITH -> blacksmith(origin)', 'case GUARD_POST -> guardPost(origin)',
              'Blocks.ANVIL.defaultBlockState()', 'Blocks.SMITHING_TABLE.defaultBlockState()',
              'Blocks.BLAST_FURNACE.defaultBlockState()', 'b.put(7, y, 2,',
              'int[][] lamps = {{2,2},{6,2},{2,6},{6,6},{4,2},{4,6}}'):
    if token not in advanced:
        raise SystemExit(f'advanced blueprint safety/detail invariant missing: {token}')

construction = (JAVA / 'settlement/SettlementConstructionService.java').read_text(encoding='utf-8')
for token in ('public static StartResult startAt', 'public static PlacementCheck checkPlacement',
              'MAX_MAIN_SETTLEMENT_RADIUS = 72', 'MAX_PLAYER_PLACEMENT_DISTANCE = 24',
              'type == BuildingType.BLACKSMITH', 'data.buildingCount(BuildingType.MINE) < 1',
              'type == BuildingType.GUARD_POST', 'SettlementTier.VILLAGE.ordinal()',
              'SettlementStorageService.consume(level, data, type.woodCost(), type.stoneCost(), 0L)'):
    if token not in construction:
        raise SystemExit(f'construction/progression invariant missing: {token}')
if 'destroyBlock(' in construction or 'dropResources(' in construction:
    raise SystemExit('building construction must not use drop-producing destruction paths')

storage = (JAVA / 'settlement/SettlementStorageService.java').read_text(encoding='utf-8')
for token in ('allStorageChunksLoaded', 'public static boolean consumeMetal',
              'SettlementStorageService::isMetal', 'public static ItemStack insert'):
    if token not in storage:
        raise SystemExit(f'physical storage/metal invariant missing: {token}')

benefit = (JAVA / 'settlement/SettlementBenefitService.java').read_text(encoding='utf-8')
for token in ('BuildingType.BLACKSMITH', 'BuildingType.GUARD_POST',
              'SettlementStorageService.consumeMetal(level, data, 1L)', 'REPAIR_PER_METAL = 16',
              'mostDamagedEquippedItem', 'new IronGolem(EntityTypes.IRON_GOLEM, level)',
              'guard.setPersistenceRequired()', 'guard.setPlayerCreated(true)', 'guardIdentity(post)'):
    if token not in benefit:
        raise SystemExit(f'settlement benefit invariant missing: {token}')

service = (JAVA / 'settlement/SettlementService.java').read_text(encoding='utf-8')
for token in ('SettlementCoreService.tick(server, data)',
              'SettlementResidentRoutineService.isRestTime(server.overworld())',
              'SettlementResidentRoutineService.tick(server, data)',
              'SettlementWorkerService.tick(server, data)',
              'SettlementOutpostProductionService.tick(server, data)',
              'SettlementBenefitService.tick(server, data)',
              'SettlementTier.current(data).displayName()'):
    if token not in service:
        raise SystemExit(f'canonical server tick/tier sync invariant missing: {token}')

road = (JAVA / 'settlement/SettlementRoadService.java').read_text(encoding='utf-8')
for token in ('MAX_ROUTE_LENGTH = 96', 'public static RouteCheck checkRoute',
              'MAX_STEP_HEIGHT = 1', 'MAX_CROSS_SLOPE = 1', 'RoadSegment.fromPath(road.centers())'):
    if token not in road:
        raise SystemExit(f'routed road invariant missing: {token}')
if 'destroyBlock(' in road or 'dropResources(' in road:
    raise SystemExit('roads must not produce loose drops while preparing terrain')

outpost = (JAVA / 'settlement/SettlementOutpostService.java').read_text(encoding='utf-8')
for token in ('public static PlacementCheck checkPlacement', 'MAX_TARGET_DISTANCE_FROM_ROAD_END = 8',
              'MAX_PLAYER_DISTANCE_FROM_ROAD_END = 48', 'nearestUnclaimedRoad', 'isRoadClaimed'):
    if token not in outpost:
        raise SystemExit(f'outpost placement invariant missing: {token}')

network = (JAVA / 'network/SettlementNetwork.java').read_text(encoding='utf-8')
for token in ('PROTOCOL = "5"', 'playToServer(OutpostPlacementRequestPayload.TYPE',
              'playToClient(OutpostPreviewPayload.TYPE', 'SettlementOutpostService.checkPlacement',
              'SettlementOutpostService.startAt'):
    if token not in network:
        raise SystemExit(f'outpost/tier networking invariant missing: {token}')

for renderer_name in ('PlacementGhostRenderer.java', 'RoadGhostRenderer.java', 'OutpostGhostRenderer.java'):
    renderer = (JAVA / 'client' / renderer_name).read_text(encoding='utf-8')
    for token in ('ExtractLevelRenderStateEvent', 'SubmitCustomGeometryEvent', 'submitShapeOutline'):
        if token not in renderer:
            raise SystemExit(f'3D ghost renderer invariant missing in {renderer_name}: {token}')

for path in JAVA.rglob('*.java'):
    text = path.read_text(encoding='utf-8')
    if path.name in {'SettlementConstructionService.java', 'SettlementRoadService.java',
                     'SettlementOutpostService.java', 'SettlementWorkerService.java',
                     'SettlementOutpostProductionService.java', 'SettlementCoreService.java'}:
        if 'destroyBlock(' in text or 'dropResources(' in text:
            raise SystemExit(f'loose-drop destruction path forbidden: {path.name}')

print('Frontier Settlement alpha.18 source audit: PASS')
