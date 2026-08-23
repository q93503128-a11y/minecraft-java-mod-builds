#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
RES = ROOT / 'src/main/resources'

required = [
    ROOT / 'build.gradle', ROOT / 'gradle.properties', ROOT / 'PROJECT.md', ROOT / 'CANONICAL_PLAN.md', ROOT / 'README.md',
    JAVA / 'FrontierSettlement.java', JAVA / 'content/FrontierContent.java', JAVA / 'content/PioneerMarkerItem.java',
    JAVA / 'settlement/SettlementData.java', JAVA / 'settlement/ConstructionState.java', JAVA / 'settlement/RoadConstructionState.java',
    JAVA / 'settlement/OutpostConstructionState.java', JAVA / 'settlement/SettlementService.java', JAVA / 'settlement/SettlementGuidanceService.java',
    JAVA / 'settlement/SettlementCoreService.java', JAVA / 'settlement/SettlementResidentRoutineService.java',
    JAVA / 'settlement/SettlementTierInfrastructureService.java', JAVA / 'settlement/SettlementConstructionService.java',
    JAVA / 'settlement/SettlementRoadService.java', JAVA / 'settlement/SettlementOutpostService.java',
    JAVA / 'settlement/SettlementOutpostProductionService.java', JAVA / 'settlement/SettlementOutpostLogisticsService.java',
    JAVA / 'settlement/SettlementWorkerService.java', JAVA / 'settlement/SettlementBenefitService.java',
    JAVA / 'settlement/SettlementStorageService.java', JAVA / 'settlement/BuildingType.java',
    JAVA / 'settlement/BuildingBlueprints.java', JAVA / 'settlement/AdvancedBuildingBlueprints.java', JAVA / 'settlement/SettlementTier.java',
    JAVA / 'network/SettlementNetwork.java', JAVA / 'network/SettlementSnapshotPayload.java',
    JAVA / 'client/ClientSettlementState.java', JAVA / 'client/FrontierSettlementClient.java',
    JAVA / 'client/BuildingPlacementClient.java', JAVA / 'client/BuildingPaletteScreen.java',
    JAVA / 'client/RoadPlacementClient.java', JAVA / 'client/OutpostPlacementClient.java', JAVA / 'client/SettlementHudOverlay.java',
    RES / 'assets/frontier_settlement/items/pioneer_marker.json', RES / 'assets/frontier_settlement/lang/ko_kr.json',
    RES / 'data/frontier_settlement/recipe/pioneer_marker.json',
]
missing = [str(p.relative_to(ROOT)) for p in required if not p.is_file()]
if missing: raise SystemExit('missing required files: ' + ', '.join(missing))

props = (ROOT / 'gradle.properties').read_text(encoding='utf-8')
for token in ('minecraft_version=26.2', 'neo_version=26.2.0.38-beta', 'mod_id=frontier_settlement', 'mod_version=0.1.0-alpha.28'):
    if token not in props: raise SystemExit(f'missing canonical property: {token}')

plan = (ROOT / 'CANONICAL_PLAN.md').read_text(encoding='utf-8')
for token in ('survival -> settlement growth', 'One world/server has one shared settlement',
              'Resources remain physical Minecraft items', '`B`: settlement palette',
              '`R`: rotate current building placement', '`Enter`: confirm', '`Backspace`: reset/cancel',
              'builder walks from actual settlement storage carrying real wood/stone stacks',
              'Transport workers belong to a specific outpost', 'pause at unloaded route boundaries'):
    if token not in plan: raise SystemExit(f'canonical plan invariant missing: {token}')

entry = (JAVA / 'FrontierSettlement.java').read_text(encoding='utf-8')
for token in ('SettlementConstructionService::onBreakBlock', 'SettlementRoadService::onBreakBlock',
              'SettlementOutpostService::onBreakBlock'):
    if token not in entry: raise SystemExit(f'active construction protection missing: {token}')

service = (JAVA / 'settlement/SettlementService.java').read_text(encoding='utf-8')
if service.count('SettlementConstructionService.tick(server, data)') != 1:
    raise SystemExit('construction service must have exactly one canonical server tick call')
if service.count('SettlementRoadService.tick(server, data)') != 1:
    raise SystemExit('road service must have exactly one canonical server tick call')
if service.count('SettlementOutpostService.tick(server, data)') != 1:
    raise SystemExit('outpost service must have exactly one canonical server tick call')

guidance = (JAVA / 'settlement/SettlementGuidanceService.java').read_text(encoding='utf-8')
for token in ('SettlementConstructionService.phaseLabel(data.construction())',
              'SettlementRoadService.phaseLabel(data.roadConstruction())',
              'SettlementOutpostService.phaseLabel(data.outpostConstruction())',
              'data.houseCount() < 1', 'data.lumberCampCount() < 1', 'BuildingType.FARM', 'BuildingType.QUARRY',
              'data.roads().isEmpty()', 'data.outposts().isEmpty()', 'data.population() < 4', 'BuildingType.MINE',
              'data.outposts().size() < 2', 'data.population() < 8', 'BuildingType.BLACKSMITH',
              'data.outposts().size() < 4', 'data.population() < 16', 'BuildingType.GUARD_POST'):
    if token not in guidance: raise SystemExit(f'next-goal invariant missing: {token}')
if 'reward' in guidance.lower() or 'quest' in guidance.lower():
    raise SystemExit('next-goal helper must remain guidance-only')

construction_state = (JAVA / 'settlement/ConstructionState.java').read_text(encoding='utf-8')
for token in ('int scaffoldMask', 'optionalFieldOf("scaffold_mask", 0)', 'ownsScaffold(int index)', 'withScaffoldMask(int nextMask)'):
    if token not in construction_state: raise SystemExit(f'construction persistence invariant missing: {token}')

road_state = (JAVA / 'settlement/RoadConstructionState.java').read_text(encoding='utf-8')
for token in ('GRADE_STEP_OFFSET = 1_000_000', 'PAVE_STEP_OFFSET = 2_000_000', 'step >= GRADE_STEP_OFFSET',
              'physicalPaving()', 'legacyPrepaidPaving()', 'centers.size(), GRADE_STEP_OFFSET'):
    if token not in road_state: raise SystemExit(f'alpha.25 road phase persistence invariant missing: {token}')

outpost_state = (JAVA / 'settlement/OutpostConstructionState.java').read_text(encoding='utf-8')
for token in ('GRADE_STEP_OFFSET = 1_000_000', 'BUILD_STEP_OFFSET = 2_000_000',
              'physicalBuilding()', 'legacyPrepaidBuilding()', 'buildStep()', 'legacyStep()'):
    if token not in outpost_state: raise SystemExit(f'alpha.26 outpost phase persistence invariant missing: {token}')

construction = (JAVA / 'settlement/SettlementConstructionService.java').read_text(encoding='utf-8')
for token in ('HAUL_BATCH_SIZE = 16', 'SettlementStorageService.findExtractionTarget', 'EquipmentSlot.MAINHAND',
              'SettlementInventory.consume(crate, woodDelta, stoneDelta, 0L)', 'builder.setInvulnerable(true)',
              'builder.setInvulnerable(false)', 'builder.swing(InteractionHand.MAIN_HAND)',
              'villager.entityTags().contains(BUILDER_TAG)', 'construction.ownsScaffold(towerIndex)',
              'removeConstructionScaffolds'):
    if token not in construction: raise SystemExit(f'physical building construction invariant missing: {token}')
if 'SettlementStorageService.consume(level, data, type.woodCost(), type.stoneCost(), 0L)' in construction:
    raise SystemExit('building approval still deletes full material cost')
if 'towerOwned(' in construction:
    raise SystemExit('scaffold ownership must come from persisted state')

road = (JAVA / 'settlement/SettlementRoadService.java').read_text(encoding='utf-8')
for token in ('SettlementStorageService.storageAvailable(level, data)', 'RoadConstructionState road = data.roadConstruction()',
              'road.grading()', 'tickGrading(', 'applyGradePlacement(', 'Blocks.COARSE_DIRT.defaultBlockState()',
              'tickPaving(', 'SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isStone)',
              'HAUL_BATCH_SIZE = 16', 'EquipmentSlot.MAINHAND', 'consumeCarriedStone(', 'returnCarriedToStorage(',
              'builder.setInvulnerable(true)', 'builder.setInvulnerable(false)', 'builder.swing(InteractionHand.MAIN_HAND)',
              'BreakBlockEvent', 'event.setNotifyClient(true)', '도로 지반 정리', '도로 석재 운반·포설'):
    if token not in road: raise SystemExit(f'alpha.25 physical road invariant missing: {token}')
if 'SettlementStorageService.consume(level, data, 0L, check.stoneCost(), 0L)' in road:
    raise SystemExit('road approval still deletes full stone cost')
if 'prepareRoute(level, check.centers())' in road:
    raise SystemExit('road terrain is still magically prepared at approval')
if 'destroyBlock(' in road or 'dropResources(' in road:
    raise SystemExit('road construction must not create loose drops')

outpost = (JAVA / 'settlement/SettlementOutpostService.java').read_text(encoding='utf-8')
for token in ('SettlementStorageService.storageAvailable(level, data)',
              'data.replaceOutpostConstructionStep(OutpostConstructionState.GRADE_STEP_OFFSET)',
              'state.grading()', 'tickGrading(', 'applyGradeCell(', 'Blocks.COARSE_DIRT.defaultBlockState()',
              'state.legacyPrepaidBuilding()', 'tickLegacyPrepaid(', 'tickPhysicalBuilding(',
              'SettlementStorageService.findExtractionTarget(level, data, predicate)', 'HAUL_BATCH_SIZE = 16',
              'EquipmentSlot.MAINHAND', 'consumeCarried(', 'returnCarriedToStorage(',
              'materialCostDelta(', 'isWoodPlacement(', 'isStonePlacement(',
              'builder.setInvulnerable(true)', 'builder.setInvulnerable(false)', 'builder.swing(InteractionHand.MAIN_HAND)',
              'BreakBlockEvent', 'event.setNotifyClient(true)', '전초기지 부지 정리', '전초기지 자재 운반·시공'):
    if token not in outpost: raise SystemExit(f'alpha.26 physical outpost invariant missing: {token}')
if 'SettlementStorageService.consume(level, data, WOOD_COST, STONE_COST, 0L)' in outpost:
    raise SystemExit('outpost approval still deletes full material cost')
if 'prepareSite(level, gate, road.directionX(), road.directionZ())' in outpost:
    raise SystemExit('outpost terrain is still magically prepared at approval')
if 'while (placed < 2' in outpost:
    raise SystemExit('legacy two-block outpost burst still present')
if 'destroyBlock(' in outpost or 'dropResources(' in outpost:
    raise SystemExit('outpost construction must not create loose drops')

production = (JAVA / 'settlement/SettlementOutpostProductionService.java').read_text(encoding='utf-8')
for token in ('PRODUCTION_WORKER_TAG', 'PRODUCTION_OUTPOST_TAG_PREFIX', 'outpostLoaded(',
              'level.hasChunkAt(', 'LUMBER_WORK_PERIOD_TICKS = 100', 'QUARRY_WORK_PERIOD_TICKS = 80',
              'MINING_WORK_PERIOD_TICKS = 160', 'AGRICULTURE_WORK_PERIOD_TICKS = 120',
              'MAX_LOGS = 4', 'MAX_STONE = 3', 'MAX_CROPS = 4', 'workDue(',
              'worker.swing(InteractionHand.MAIN_HAND)', 'pristineLegacyAgriculturePlot(',
              'initializeSpecializationSite(', 'findMatureCrop(', 'isMatureWheat(',
              '!level.getBlockState(pos.above()).isAir()', 'level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3)'):
    if token not in production: raise SystemExit(f'alpha.28 outpost production invariant missing: {token}')
if 'ensureAgriculturePlot(level, data, outpost)' in production:
    raise SystemExit('agriculture plot must not be rebuilt every production tick')
if 'forceChunk' in production or 'setChunkForced' in production:
    raise SystemExit('outpost production must not force-load remote chunks')

logistics = (JAVA / 'settlement/SettlementOutpostLogisticsService.java').read_text(encoding='utf-8')
for token in ('TRANSPORT_WORKER_TAG', 'TRANSPORT_OUTPOST_TAG_PREFIX', 'migrateLegacyWorkers(',
              'routeFromTown(', 'appendRoadPrefixFromTown(', 'road.centers()', 'ROAD_WAYPOINT_STRIDE = 3',
              'routeFullyLoaded(', 'level.hasChunkAt(', 'firstMissingLoadedAssignment(',
              'takeFirstTransportStack(', 'SettlementInventory.isWood', 'SettlementInventory.isStone',
              'SettlementInventory.isFood', 'isMiningCargo(', 'EquipmentSlot.MAINHAND'):
    if token not in logistics: raise SystemExit(f'alpha.27 outpost logistics invariant missing: {token}')
if 'forceChunk' in logistics or 'setChunkForced' in logistics:
    raise SystemExit('outpost logistics must not force-load route chunks')

worker = (JAVA / 'settlement/SettlementWorkerService.java').read_text(encoding='utf-8')
for token in ('SettlementOutpostLogisticsService.migrateLegacyWorkers(level, data)',
              'SettlementOutpostLogisticsService.tick(level, data)',
              'SettlementOutpostLogisticsService.allRoutesLoaded(level, data)',
              'SettlementOutpostLogisticsService.loadedAssignedWorkerCount(level, data)',
              'SettlementOutpostLogisticsService.firstMissingLoadedAssignment(level, data)',
              'SettlementOutpostLogisticsService.spawnAssignedWorker(level, missing)'):
    if token not in worker: raise SystemExit(f'alpha.27 worker/logistics integration missing: {token}')
for forbidden in ('TRANSPORT_WORKER_NAME', 'workTransport(', 'takeFirstStack('):
    if forbidden in worker: raise SystemExit(f'legacy UUID-order transport backend remains in worker service: {forbidden}')

storage = (JAVA / 'settlement/SettlementStorageService.java').read_text(encoding='utf-8')
for token in ('storageAvailable(ServerLevel level, SettlementData data)', 'findExtractionTarget(', 'findDepositTarget(', 'extract('):
    if token not in storage: raise SystemExit(f'physical storage invariant missing: {token}')

snapshot = (JAVA / 'network/SettlementSnapshotPayload.java').read_text(encoding='utf-8')
for token in ('int buildingUnlockMask, String nextGoal', 'buf.writeUtf(payload.nextGoal())', 'buf.readUtf()'):
    if token not in snapshot: raise SystemExit(f'guidance snapshot invariant missing: {token}')
network = (JAVA / 'network/SettlementNetwork.java').read_text(encoding='utf-8')
if 'PROTOCOL = "7"' not in network: raise SystemExit('snapshot shape requires network protocol 7')

placement = (JAVA / 'client/BuildingPlacementClient.java').read_text(encoding='utf-8')
for token in ('GLFW.GLFW_KEY_B', 'GLFW.GLFW_KEY_R', 'GLFW.GLFW_KEY_ENTER', 'GLFW.GLFW_KEY_BACKSPACE'):
    if token not in placement: raise SystemExit(f'unified control invariant missing: {token}')
for forbidden in ('GLFW_KEY_N', 'GLFW_KEY_J', 'GLFW_KEY_K'):
    if forbidden in placement: raise SystemExit(f'legacy fragmented control still registered: {forbidden}')
for client in ('RoadPlacementClient.java', 'OutpostPlacementClient.java'):
    text = (JAVA / 'client' / client).read_text(encoding='utf-8')
    for forbidden in ('KeyMapping', 'RegisterKeyMappingsEvent', 'GLFW_KEY_'):
        if forbidden in text: raise SystemExit(f'{client} owns a separate gameplay key: {forbidden}')

lang = json.loads((RES / 'assets/frontier_settlement/lang/ko_kr.json').read_text(encoding='utf-8'))
for legacy in ('key.frontier_settlement.next_building', 'key.frontier_settlement.road_mode', 'key.frontier_settlement.outpost_mode'):
    if legacy in lang: raise SystemExit(f'legacy key localization remains: {legacy}')

for path in (JAVA / 'settlement/SettlementService.java', JAVA / 'settlement/SettlementCoreService.java',
             JAVA / 'settlement/SettlementConstructionService.java', JAVA / 'settlement/SettlementRoadService.java',
             JAVA / 'settlement/SettlementOutpostService.java', JAVA / 'settlement/SettlementWorkerService.java',
             JAVA / 'settlement/SettlementOutpostProductionService.java', JAVA / 'settlement/SettlementOutpostLogisticsService.java',
             JAVA / 'settlement/SettlementTierInfrastructureService.java'):
    text = path.read_text(encoding='utf-8')
    if 'destroyBlock(' in text or 'dropResources(' in text: raise SystemExit(f'loose-drop destruction path forbidden: {path.name}')

print('Frontier Settlement alpha.28 source audit: PASS')
