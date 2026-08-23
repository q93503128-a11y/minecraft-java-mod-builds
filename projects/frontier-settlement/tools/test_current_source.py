#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
RES = ROOT / 'src/main/resources'


def text(path):
    return path.read_text(encoding='utf-8')


def must(source, tokens, label):
    for token in tokens:
        if token not in source:
            raise SystemExit(f'{label} missing: {token}')


def forbid(source, tokens, label):
    for token in tokens:
        if token in source:
            raise SystemExit(f'{label}: {token}')


required = [
    ROOT / 'build.gradle', ROOT / 'gradle.properties', ROOT / 'PROJECT.md', ROOT / 'CANONICAL_PLAN.md', ROOT / 'README.md',
    ROOT / 'ORIGINAL_DESIGN_v0.2.md', ROOT / 'COMPLETION_GAP_AUDIT.md', ROOT / 'COMPANION_MODS.md',
    ROOT / 'EXTERNAL_CONTENT_REGISTER.md', ROOT / 'COMPANION_LOCK.json',
    JAVA / 'FrontierSettlement.java', JAVA / 'content/FrontierContent.java', JAVA / 'content/PioneerMarkerItem.java',
    JAVA / 'compat/ExternalContentTags.java',
    JAVA / 'settlement/SettlementData.java', JAVA / 'settlement/ConstructionState.java', JAVA / 'settlement/RoadConstructionState.java',
    JAVA / 'settlement/OutpostConstructionState.java', JAVA / 'settlement/SettlementService.java', JAVA / 'settlement/SettlementGuidanceService.java',
    JAVA / 'settlement/SettlementCoreService.java', JAVA / 'settlement/SettlementResidentRoutineService.java',
    JAVA / 'settlement/SettlementTierInfrastructureService.java', JAVA / 'settlement/SettlementConstructionService.java',
    JAVA / 'settlement/SettlementRoadService.java', JAVA / 'settlement/SettlementOutpostService.java',
    JAVA / 'settlement/SettlementOutpostProductionService.java', JAVA / 'settlement/SettlementOutpostLogisticsService.java',
    JAVA / 'settlement/SettlementWorkerService.java', JAVA / 'settlement/SettlementBenefitService.java',
    JAVA / 'settlement/SettlementInventory.java', JAVA / 'settlement/SettlementStorageService.java', JAVA / 'settlement/SettlementExternalContentService.java',
    JAVA / 'settlement/SettlementMarketService.java', JAVA / 'settlement/MarketLayout.java', JAVA / 'settlement/MarketBuildingBlueprint.java',
    JAVA / 'settlement/SettlementWorkshopService.java', JAVA / 'settlement/WorkshopLayout.java', JAVA / 'settlement/WorkshopBuildingBlueprint.java',
    JAVA / 'settlement/SettlementCartStationService.java', JAVA / 'settlement/CartStationLayout.java', JAVA / 'settlement/CartStationBuildingBlueprint.java',
    JAVA / 'settlement/BuildingType.java', JAVA / 'settlement/BuildingBlueprints.java', JAVA / 'settlement/AdvancedBuildingBlueprints.java',
    JAVA / 'settlement/SettlementTier.java',
    JAVA / 'network/SettlementNetwork.java', JAVA / 'network/SettlementSnapshotPayload.java',
    JAVA / 'client/ClientSettlementState.java', JAVA / 'client/FrontierSettlementClient.java',
    JAVA / 'client/BuildingPlacementClient.java', JAVA / 'client/BuildingPaletteScreen.java',
    JAVA / 'client/RoadPlacementClient.java', JAVA / 'client/OutpostPlacementClient.java', JAVA / 'client/SettlementHudOverlay.java',
    RES / 'assets/frontier_settlement/items/pioneer_marker.json', RES / 'assets/frontier_settlement/lang/ko_kr.json',
    RES / 'data/frontier_settlement/recipe/pioneer_marker.json',
    RES / 'data/frontier_settlement/tags/item/settlement_wood.json',
    RES / 'data/frontier_settlement/tags/item/settlement_stone.json',
    RES / 'data/frontier_settlement/tags/item/settlement_metal.json',
    RES / 'data/frontier_settlement/tags/item/settlement_food.json',
    RES / 'data/frontier_settlement/tags/item/expedition_relics.json',
]
missing = [str(p.relative_to(ROOT)) for p in required if not p.is_file()]
if missing:
    raise SystemExit('missing required files: ' + ', '.join(missing))

props = text(ROOT / 'gradle.properties')
must(props, ('minecraft_version=26.2', 'neo_version=26.2.0.38-beta', 'mod_id=frontier_settlement',
             'mod_version=0.1.0-alpha.34'), 'canonical property')

plan = text(ROOT / 'CANONICAL_PLAN.md')
must(plan, ('survival -> settlement growth', 'One world/server has one shared settlement',
            'Resources remain physical Minecraft items', '`B`: settlement palette',
            '`R`: rotate current building placement', '`Enter`: confirm', '`Backspace`: reset/cancel',
            'builder walks from actual settlement storage carrying real wood/stone stacks',
            'Transport workers belong to a specific outpost', 'pause at unloaded route boundaries',
            'tier-visible public works', 'single authority for outpost transport'), 'canonical plan invariant')

original = text(ROOT / 'ORIGINAL_DESIGN_v0.2.md')
must(original, ('외부 모드팩 연동 방향', '15~20개 계열', '작은 다리', '수레 정거장', '감시탑', '병영',
                '시장', '작업장', '고급 제작소'), 'original v0.2 scope invariant')

companion = text(ROOT / 'COMPANION_MODS.md')
must(companion, ('외부 콘텐츠는 **Frontier의 폭을 빠르게 늘리는 핵심 개발 수단**',
                 'Repurposed Structures', 'Lootr', 'Weapons Expanded', 'Better Combat', 'Terralith'),
     'external-content strategy invariant')
register = text(ROOT / 'EXTERNAL_CONTENT_REGISTER.md')
must(register, ('DEPENDENCY', 'REFERENCE', 'CODE_REUSE', 'ASSET_REUSE', 'Weapons Expanded', 'Lootr'),
     'external-content register invariant')

lock = json.loads(text(ROOT / 'COMPANION_LOCK.json'))
if lock.get('target', {}).get('minecraft') != '26.2' or lock.get('target', {}).get('loader') != 'neoforge':
    raise SystemExit('companion lock target drifted from 26.2 NeoForge')
if lock.get('target', {}).get('frontier_settlement') != '0.1.0-alpha.34':
    raise SystemExit('companion lock Frontier version drifted from alpha.34')
lock_ids = {entry.get('id') for entry in lock.get('entries', [])}
for required_mod in ('terralith', 'dungeons_and_taverns', 'repurposed_structures', 'better_combat',
                     'weapons_expanded', 'lootr', 'sophisticated_backpacks', 'jade', 'xaeros_minimap', 'lithostitched'):
    if required_mod not in lock_ids:
        raise SystemExit(f'companion lock missing: {required_mod}')

entry = text(JAVA / 'FrontierSettlement.java')
must(entry, ('SettlementConstructionService::onBreakBlock', 'SettlementRoadService::onBreakBlock',
             'SettlementOutpostService::onBreakBlock', 'SettlementCoreService::onBreakBlock',
             'SettlementTierInfrastructureService::onBreakBlock', 'SettlementMarketService::onBreakBlock',
             'SettlementWorkshopService::onBreakBlock', 'SettlementCartStationService::onBreakBlock'),
     'active infrastructure protection')

service = text(JAVA / 'settlement/SettlementService.java')
for token in ('SettlementConstructionService.tick(server, data)', 'SettlementRoadService.tick(server, data)',
              'SettlementOutpostService.tick(server, data)', 'SettlementMarketService.tick(server, data)',
              'SettlementWorkshopService.tick(server, data)'):
    if service.count(token) != 1:
        raise SystemExit(f'server tick authority must have exactly one call: {token}')
for daytime in ('SettlementMarketService.tick(server, data)', 'SettlementWorkshopService.tick(server, data)'):
    if service.index(daytime) < service.index('else {'):
        raise SystemExit(f'daytime service escaped non-rest branch: {daytime}')
must(service, ('type == BuildingType.WORKSHOP', 'SettlementWorkshopService.lockedReason(data)',
               'type == BuildingType.CART_STATION', 'SettlementCartStationService.lockedReason(data)'),
     'special building unlock mask invariant')

# Cart station must not add its own server tick/navigation authority.
forbid(service, ('SettlementCartStationService.tick(',), 'cart station introduced a second logistics tick authority')

guidance = text(JAVA / 'settlement/SettlementGuidanceService.java')
must(guidance, ('SettlementConstructionService.phaseLabel(data.construction())',
                'SettlementRoadService.phaseLabel(data.roadConstruction())',
                'SettlementOutpostService.phaseLabel(data.outpostConstruction())',
                'data.houseCount() < 1', 'data.lumberCampCount() < 1', 'BuildingType.FARM', 'BuildingType.QUARRY',
                'data.roads().isEmpty()', 'data.outposts().isEmpty()', 'data.population() < 4', 'BuildingType.MARKET',
                'BuildingType.CART_STATION', 'BuildingType.MINE', 'data.outposts().size() < 2', 'data.population() < 8',
                'BuildingType.BLACKSMITH', 'BuildingType.WORKSHOP', 'data.outposts().size() < 4',
                'data.population() < 16', 'BuildingType.GUARD_POST'), 'next-goal invariant')
if 'reward' in guidance.lower() or 'quest' in guidance.lower():
    raise SystemExit('next-goal helper must remain guidance-only')

construction_state = text(JAVA / 'settlement/ConstructionState.java')
must(construction_state, ('int scaffoldMask', 'optionalFieldOf("scaffold_mask", 0)', 'ownsScaffold(int index)',
                          'withScaffoldMask(int nextMask)', 'GRADE_STEP_OFFSET = 1_000_000', 'BUILD_STEP_OFFSET = 2_000_000',
                          'grading()', 'physicalBuilding()', 'legacyPreparedBuilding()', 'gradeStep()', 'buildStep()'),
     'alpha.30 construction persistence invariant')

road_state = text(JAVA / 'settlement/RoadConstructionState.java')
must(road_state, ('GRADE_STEP_OFFSET = 1_000_000', 'PAVE_STEP_OFFSET = 2_000_000', 'step >= GRADE_STEP_OFFSET',
                  'physicalPaving()', 'legacyPrepaidPaving()', 'centers.size(), GRADE_STEP_OFFSET'),
     'alpha.25 road phase persistence invariant')
outpost_state = text(JAVA / 'settlement/OutpostConstructionState.java')
must(outpost_state, ('GRADE_STEP_OFFSET = 1_000_000', 'BUILD_STEP_OFFSET = 2_000_000',
                     'physicalBuilding()', 'legacyPrepaidBuilding()', 'buildStep()', 'legacyStep()'),
     'alpha.26 outpost phase persistence invariant')

building_type = text(JAVA / 'settlement/BuildingType.java')
must(building_type, ('MARKET("market", "시장", 96, 48, 11, 11, 8, 0',
                     'WORKSHOP("workshop", "작업장", 88, 44, 11, 9, 10, 0',
                     'CART_STATION("cart_station", "수레 정거장", 104, 56, 13, 9, 8, 0'),
     'functional building definition')

blueprints = text(JAVA / 'settlement/BuildingBlueprints.java')
must(blueprints, ('case MARKET -> MarketBuildingBlueprint.create(origin);',
                  'case WORKSHOP -> WorkshopBuildingBlueprint.create(origin);',
                  'case CART_STATION -> CartStationBuildingBlueprint.create(origin);'),
     'functional blueprint routing')

market_layout = text(JAVA / 'settlement/MarketLayout.java')
must(market_layout, ('TRADE_X = 5', 'TRADE_Y = 1', 'TRADE_Z = 5',
                     'tradeCrate(BuildingRecord market)', 'market.localToWorld'), 'alpha.32 market layout invariant')
market_blueprint = text(JAVA / 'settlement/MarketBuildingBlueprint.java')
must(market_blueprint, ('MarketLayout.tradeCrate(origin)', 'Blocks.BARREL.defaultBlockState()',
                        'Blocks.BELL.defaultBlockState()', 'BuildingBlueprints.Phase.FINISH',
                        'Blocks.STONE_BRICKS.defaultBlockState()', 'Blocks.SPRUCE_PLANKS.defaultBlockState()'),
     'alpha.32 market blueprint invariant')

workshop_layout = text(JAVA / 'settlement/WorkshopLayout.java')
must(workshop_layout, ('SERVICE_X = 5', 'SERVICE_Y = 1', 'SERVICE_Z = 4', 'serviceCrate(BlockPos origin)',
                       'serviceCrate(BuildingRecord workshop)', 'workshop.localToWorld'),
     'alpha.33 workshop layout invariant')
workshop_blueprint = text(JAVA / 'settlement/WorkshopBuildingBlueprint.java')
must(workshop_blueprint, ('WorkshopLayout.serviceCrate(origin)', 'Blocks.BARREL.defaultBlockState()',
                          'Blocks.GRINDSTONE.defaultBlockState()', 'Blocks.SMITHING_TABLE.defaultBlockState()',
                          'Blocks.ANVIL.defaultBlockState()', 'Blocks.BLAST_FURNACE.defaultBlockState()',
                          'BuildingBlueprints.Phase.FINISH'), 'alpha.33 workshop blueprint invariant')

cart_layout = text(JAVA / 'settlement/CartStationLayout.java')
must(cart_layout, ('FREIGHT', 'freightPositions(BlockPos origin)', 'freightPositions(BuildingRecord station)',
                   'station.localToWorld', 'loadingLane(BuildingRecord station)'), 'alpha.34 cart station layout invariant')
cart_blueprint = text(JAVA / 'settlement/CartStationBuildingBlueprint.java')
must(cart_blueprint, ('CartStationLayout.freightPositions(origin)', 'Blocks.BARREL.defaultBlockState()',
                      'Blocks.RAIL.defaultBlockState()', 'Blocks.BELL.defaultBlockState()',
                      'BuildingBlueprints.Phase.FINISH', 'Blocks.STONE_BRICKS.defaultBlockState()'),
     'alpha.34 cart station blueprint invariant')
cart_service = text(JAVA / 'settlement/SettlementCartStationService.java')
must(cart_service, ('lockedReason(SettlementData data)', 'SettlementTier.VILLAGE', 'data.roads().isEmpty()',
                    'data.outposts().isEmpty()', 'BuildingType.CART_STATION',
                    'CartStationLayout.freightPositions(station)', 'BreakBlockEvent',
                    'event.setCanceled(true)', 'event.setNotifyClient(true)'), 'alpha.34 cart station service invariant')
forbid(cart_service, ('void tick(', 'getNavigation().moveTo(', 'spawnAssignedWorker('),
       'cart station must not become a second transport authority')

construction = text(JAVA / 'settlement/SettlementConstructionService.java')
must(construction, ('HAUL_BATCH_SIZE = 16', 'SettlementStorageService.findExtractionTarget', 'EquipmentSlot.MAINHAND',
                    'SettlementInventory.consume(crate, woodDelta, stoneDelta, 0L)', 'builder.setInvulnerable(true)',
                    'builder.setInvulnerable(false)', 'builder.swing(InteractionHand.MAIN_HAND)',
                    'villager.entityTags().contains(BUILDER_TAG)', 'construction.ownsScaffold(towerIndex)',
                    'removeConstructionScaffolds', 'data.replaceConstructionStep(ConstructionState.GRADE_STEP_OFFSET)',
                    'construction.grading()', 'tickGrading(', 'createGradePlan(', 'canGradeCell(', 'applyGradeCell(',
                    'ConstructionState.BUILD_STEP_OFFSET', 'Blocks.COARSE_DIRT.defaultBlockState()',
                    'GRADE_INTERVAL_TICKS = 8', 'MAX_GRADE_FILL_DEPTH = 3', 'construction.buildStep()',
                    '건물 부지 정리', 'level.hasChunkAt(supply)', 'type == BuildingType.MARKET',
                    '시장은 마을 단계에 도달하면 열립니다.'), 'physical building/market invariant')
forbid(construction, ('SettlementStorageService.consume(level, data, type.woodCost(), type.stoneCost(), 0L)',
                      'prepareSite(', 'level.setBlock(top, Blocks.COBBLESTONE.defaultBlockState()',
                      'destroyBlock(', 'dropResources(', 'towerOwned('), 'physical building invariant violated')

road = text(JAVA / 'settlement/SettlementRoadService.java')
must(road, ('SettlementStorageService.storageAvailable(level, data)', 'RoadConstructionState road = data.roadConstruction()',
            'road.grading()', 'tickGrading(', 'applyGradePlacement(', 'Blocks.COARSE_DIRT.defaultBlockState()',
            'tickPaving(', 'SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isStone)',
            'HAUL_BATCH_SIZE = 16', 'EquipmentSlot.MAINHAND', 'consumeCarriedStone(', 'returnCarriedToStorage(',
            'builder.setInvulnerable(true)', 'builder.setInvulnerable(false)', 'builder.swing(InteractionHand.MAIN_HAND)',
            'BreakBlockEvent', 'event.setNotifyClient(true)', '도로 지반 정리', '도로 석재 운반·포설'),
     'alpha.25 physical road invariant')
forbid(road, ('SettlementStorageService.consume(level, data, 0L, check.stoneCost(), 0L)',
              'prepareRoute(level, check.centers())', 'destroyBlock(', 'dropResources('), 'physical road invariant violated')

outpost = text(JAVA / 'settlement/SettlementOutpostService.java')
must(outpost, ('SettlementStorageService.storageAvailable(level, data)',
               'data.replaceOutpostConstructionStep(OutpostConstructionState.GRADE_STEP_OFFSET)',
               'state.grading()', 'tickGrading(', 'applyGradeCell(', 'Blocks.COARSE_DIRT.defaultBlockState()',
               'state.legacyPrepaidBuilding()', 'tickLegacyPrepaid(', 'tickPhysicalBuilding(',
               'SettlementStorageService.findExtractionTarget(level, data, predicate)', 'HAUL_BATCH_SIZE = 16',
               'EquipmentSlot.MAINHAND', 'consumeCarried(', 'returnCarriedToStorage(', 'materialCostDelta(',
               'isWoodPlacement(', 'isStonePlacement(', 'builder.setInvulnerable(true)', 'builder.setInvulnerable(false)',
               'builder.swing(InteractionHand.MAIN_HAND)', 'BreakBlockEvent', 'event.setNotifyClient(true)',
               '전초기지 부지 정리', '전초기지 자재 운반·시공'), 'alpha.26 physical outpost invariant')
forbid(outpost, ('SettlementStorageService.consume(level, data, WOOD_COST, STONE_COST, 0L)',
                 'prepareSite(level, gate, road.directionX(), road.directionZ())', 'while (placed < 2',
                 'destroyBlock(', 'dropResources('), 'physical outpost invariant violated')

production = text(JAVA / 'settlement/SettlementOutpostProductionService.java')
must(production, ('PRODUCTION_WORKER_TAG', 'PRODUCTION_OUTPOST_TAG_PREFIX', 'outpostLoaded(', 'level.hasChunkAt(',
                  'LUMBER_WORK_PERIOD_TICKS = 100', 'QUARRY_WORK_PERIOD_TICKS = 80', 'MINING_WORK_PERIOD_TICKS = 160',
                  'AGRICULTURE_WORK_PERIOD_TICKS = 120', 'MAX_LOGS = 4', 'MAX_STONE = 3', 'MAX_CROPS = 4',
                  'workDue(', 'worker.swing(InteractionHand.MAIN_HAND)', 'pristineLegacyAgriculturePlot(',
                  'initializeSpecializationSite(', 'findMatureCrop(', 'isMatureWheat(',
                  '!level.getBlockState(pos.above()).isAir()', 'level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3)'),
     'alpha.28 outpost production invariant')
forbid(production, ('ensureAgriculturePlot(level, data, outpost)', 'forceChunk', 'setChunkForced'),
       'outpost production invariant violated')

logistics = text(JAVA / 'settlement/SettlementOutpostLogisticsService.java')
must(logistics, ('TRANSPORT_WORKER_TAG', 'TRANSPORT_OUTPOST_TAG_PREFIX', 'migrateLegacyWorkers(', 'routeFromTown(',
                 'appendRoadPrefixFromTown(', 'road.centers()', 'ROAD_WAYPOINT_STRIDE = 3', 'routeFullyLoaded(',
                 'level.hasChunkAt(', 'firstMissingLoadedAssignment(', 'takeFirstTransportStack(',
                 'SettlementInventory.isWood', 'SettlementInventory.isStone', 'SettlementInventory.isFood',
                 'isMiningCargo(', 'EquipmentSlot.MAINHAND', 'BASE_TRANSPORT_STACK = 16',
                 'CART_STATION_TRANSPORT_STACK = 32', 'transportBatchSize(SettlementData data)',
                 'BuildingType.CART_STATION', 'SettlementStorageService.findLogisticsDepositTarget(level, data, carried)',
                 'SettlementStorageService.insertAt(level, target, carried)'), 'outpost/cart logistics invariant')
forbid(logistics, ('forceChunk', 'setChunkForced', 'SettlementStorageService.findDepositTarget(level, data, carried)'),
       'outpost/cart logistics invariant violated')

worker = text(JAVA / 'settlement/SettlementWorkerService.java')
must(worker, ('SettlementOutpostLogisticsService.migrateLegacyWorkers(level, data)',
              'SettlementOutpostLogisticsService.tick(level, data)', 'SettlementOutpostLogisticsService.allRoutesLoaded(level, data)',
              'SettlementOutpostLogisticsService.loadedAssignedWorkerCount(level, data)',
              'SettlementOutpostLogisticsService.firstMissingLoadedAssignment(level, data)',
              'SettlementOutpostLogisticsService.spawnAssignedWorker(level, missing)',
              'SettlementWorkshopService.allAssignmentsLoaded(level, data)',
              'SettlementWorkshopService.loadedAssignedWorkerCount(level, data)',
              'SettlementWorkshopService.firstMissingLoadedAssignment(level, data)',
              'SettlementWorkshopService.spawnAssignedWorker(level, missingWorkshop)',
              'LUMBER_WORK_PERIOD_TICKS = 100', 'FARM_WORK_PERIOD_TICKS = 120',
              'QUARRY_WORK_PERIOD_TICKS = 80', 'MINING_WORK_PERIOD_TICKS = 160',
              'MAX_LOGS_PER_TRIP = 4', 'MAX_CROPS_PER_TRIP = 4', 'MAX_STONE_PER_TRIP = 3',
              'ARRIVAL_FOOD_COST = 4L', 'consumeArrivalFood(level, data)', 'finishArrival(server, data)',
              'workDue(', 'worker.swing(InteractionHand.MAIN_HAND)', 'level.hasChunkAt(',
              '!level.getBlockState(pos.above()).isAir()'), 'bounded worker/population invariant')
forbid(worker, ('TRANSPORT_WORKER_NAME', 'workTransport(', 'takeFirstStack('),
       'legacy UUID-order transport backend remains in worker service')

core = text(JAVA / 'settlement/SettlementCoreService.java')
must(core, ('level.getBlockEntity(placement.pos())', 'BreakBlockEvent', 'event.setCanceled(true)',
            'event.setNotifyClient(true)', 'desired(data)', 'desired(SettlementData data, SettlementTier tier)',
            'for (SettlementTier tier : SettlementTier.values())',
            'pos.equals(data.stockpilePos()) && current.is(Blocks.BARREL)'), 'alpha.30 civic/stockpile protection invariant')

tier_infra = text(JAVA / 'settlement/SettlementTierInfrastructureService.java')
must(tier_infra, ('FRONTIER_TOWN_LAMP_SPACING = 16', 'DOMAIN_LAMP_SPACING = 8', 'LAMP_START_OFFSET = 8',
                  'maintainRoadPublicWorks(', 'lampSite(', 'level.hasChunkAt(', 'protectedXZ(',
                  'maintainTierGarrison(', 'BreakBlockEvent', 'event.setCanceled(true)', 'matchesLampPlan(',
                  'matchesLampPlan(level, data, pos, block, DOMAIN_LAMP_SPACING)',
                  'matchesLampPlan(level, data, pos, block, FRONTIER_TOWN_LAMP_SPACING)'),
     'alpha.29 tier public-works invariant')
forbid(tier_infra, ('TRANSPORT_WORKER_NAME', 'transportWorkers(', 'topUpMatching(', 'Comparator.comparing'),
       'legacy tier transport authority remains')

routine = text(JAVA / 'settlement/SettlementResidentRoutineService.java')
must(routine, ('SettlementOutpostLogisticsService.TRANSPORT_WORKER_TAG',
               'SettlementOutpostProductionService.PRODUCTION_WORKER_TAG', 'assignedTransportOutpost(',
               'settlementBounds(', 'moveToHouseSlot(', 'villager.getNavigation().stop()', '"작업장 주민"'),
     'resident routine invariant')
if '"운송 주민"' in routine:
    raise SystemExit('night routine must use transport assignment tags, not legacy generic name matching')

benefit = text(JAVA / 'settlement/SettlementBenefitService.java')
must(benefit, ('if (!level.hasChunkAt(work)) continue;', 'if (!level.hasChunkAt(center)) continue;'),
     'alpha.30 loaded-guard invariant')

external_tags = text(JAVA / 'compat/ExternalContentTags.java')
must(external_tags, ('SETTLEMENT_WOOD', 'SETTLEMENT_STONE', 'SETTLEMENT_METAL', 'SETTLEMENT_FOOD',
                     'EXPEDITION_RELICS', 'C_INGOTS', 'C_RAW_MATERIALS', 'C_STONES', 'C_COBBLESTONES', 'C_FOODS'),
     'alpha.31 external tag invariant')
inventory = text(JAVA / 'settlement/SettlementInventory.java')
must(inventory, ('ExternalContentTags.SETTLEMENT_WOOD', 'ExternalContentTags.C_STONES',
                 'ExternalContentTags.C_COBBLESTONES', 'ExternalContentTags.SETTLEMENT_STONE',
                 'ExternalContentTags.C_FOODS', 'ExternalContentTags.SETTLEMENT_FOOD'),
     'alpha.31 external material classification')

storage = text(JAVA / 'settlement/SettlementStorageService.java')
must(storage, ('storageAvailable(ServerLevel level, SettlementData data)', 'findExtractionTarget(', 'findDepositTarget(',
               'extract(', 'isMetalStack(ItemStack stack)', 'SettlementStorageService::isMetalStack',
               'ExternalContentTags.C_INGOTS', 'ExternalContentTags.C_RAW_MATERIALS', 'ExternalContentTags.SETTLEMENT_METAL',
               'BuildingType.CART_STATION', 'CartStationLayout.freightPositions(building)',
               'cartStationFreightPositions(', 'findLogisticsDepositTarget(', 'insertAt('),
     'physical/external/cart storage invariant')

external = text(JAVA / 'settlement/SettlementExternalContentService.java')
must(external, ('SettlementStorageService.storageAvailable(level, data)', 'ExternalContentTags.EXPEDITION_RELICS',
                'BuiltInRegistries.ITEM.getKey', 'EXTERNAL_WEAPON_NAMESPACES', '"weaponsexpanded"',
                'stack.isDamageableItem()', 'FrontierSettlement.MOD_ID'), 'alpha.31 companion-content bridge')
if 'ModList' in external:
    raise SystemExit('external-content bridge must not require companion loader classes just to read physical content')

market = text(JAVA / 'settlement/SettlementMarketService.java')
must(market, ('MARKET_TRADER_TAG', 'MARKET_ASSIGNMENT_PREFIX', 'TRADE_PERIOD_TICKS = 100',
              'ExternalContentTags.EXPEDITION_RELICS', 'MarketLayout.tradeCrate(market)',
              'SettlementInventory.insert(container, new ItemStack(Items.EMERALD, payout))',
              'hasEmeraldRoom(', 'trader.swing(InteractionHand.MAIN_HAND)', 'level.hasChunkAt(',
              'BreakBlockEvent', 'event.setCanceled(true)', 'event.setNotifyClient(true)', '방문 상인'),
     'alpha.32 physical market invariant')
forbid(market, ('SettlementStorageService.extract(', 'SettlementStorageService.storagePositions(',
                'SettlementStorageService.consume(', 'destroyBlock(', 'dropResources('),
       'market must not auto-sell shared storage or create loose drops')

workshop = text(JAVA / 'settlement/SettlementWorkshopService.java')
must(workshop, ('WORKSHOP_WORKER_TAG', 'WORKSHOP_ASSIGNMENT_PREFIX', 'SERVICE_PERIOD_TICKS = 100', 'REPAIR_PER_METAL = 64',
                'lockedReason(SettlementData data)', 'BuildingType.BLACKSMITH', 'WorkshopLayout.serviceCrate(workshop)',
                'SettlementExternalContentService.isExternalWeapon', 'SettlementStorageService.storageAvailable(level, data)',
                'SettlementStorageService.findExtractionTarget(level, data, SettlementStorageService::isMetalStack)',
                'SettlementStorageService.extract(level, source, SettlementStorageService::isMetalStack, 1)',
                'EquipmentSlot.MAINHAND', 'worker.swing(InteractionHand.MAIN_HAND)', 'returnCarriedItem(',
                'SettlementInventory.insert(container, carried)', 'allAssignmentsLoaded(', 'loadedAssignedWorkerCount(',
                'firstMissingLoadedAssignment(', 'spawnAssignedWorker(', 'level.hasChunkAt(',
                'BreakBlockEvent', 'event.setCanceled(true)', 'event.setNotifyClient(true)', '"작업장 주민"'),
     'alpha.33 physical workshop invariant')
forbid(workshop, ('SettlementStorageService.insert(level, data, carried)', 'SettlementStorageService.storagePositions(',
                  'destroyBlock(', 'dropResources('), 'workshop violated physical opt-in/hauling invariant')

commands = text(JAVA / 'command/SettlementCommands.java')
must(commands, ('SettlementExternalContentService.snapshot', '탐험 연동 | 유물', '외부 무기',
                 'Commands.literal("market")', 'BuildingType.MARKET', ' | 시장 ',
                 'Commands.literal("workshop")', 'BuildingType.WORKSHOP', ' | 작업장 ',
                 'SettlementWorkshopService.lockedReason(data)', 'Commands.literal("cart_station")',
                 'BuildingType.CART_STATION', 'SettlementCartStationService.lockedReason(data)',
                 'SettlementOutpostLogisticsService.transportBatchSize(data)', '수레 정거장', '운송 1회 적재'),
     'command/status invariant')

network = text(JAVA / 'network/SettlementNetwork.java')
must(network, ('PROTOCOL = "7"', 'type == BuildingType.WORKSHOP', 'SettlementWorkshopService.lockedReason(data)',
                'type == BuildingType.CART_STATION', 'SettlementCartStationService.lockedReason(data)'),
     'network/server special-building guard')

palette = text(JAVA / 'client/BuildingPaletteScreen.java')
must(palette, ('BuildingType.MARKET', 'BuildingType.WORKSHOP', 'BuildingType.CART_STATION',
               '물류·교역', '작업장←대장간', '정거장←도로+전초'), 'compact palette invariant')

for tag_name in ('settlement_wood', 'settlement_stone', 'settlement_metal', 'settlement_food', 'expedition_relics'):
    tag = json.loads(text(RES / f'data/frontier_settlement/tags/item/{tag_name}.json'))
    if tag.get('replace') is not False or not tag.get('values'):
        raise SystemExit(f'alpha.31 data tag must be additive and non-empty: {tag_name}')
relic_tag = json.loads(text(RES / 'data/frontier_settlement/tags/item/expedition_relics.json'))
for token in ('minecraft:echo_shard', 'minecraft:heart_of_the_sea', 'minecraft:heavy_core'):
    if token not in relic_tag['values']:
        raise SystemExit(f'expedition relic baseline missing: {token}')

snapshot = text(JAVA / 'network/SettlementSnapshotPayload.java')
must(snapshot, ('int buildingUnlockMask, String nextGoal', 'buf.writeUtf(payload.nextGoal())', 'buf.readUtf()'),
     'guidance snapshot invariant')
placement = text(JAVA / 'client/BuildingPlacementClient.java')
must(placement, ('GLFW.GLFW_KEY_B', 'GLFW.GLFW_KEY_R', 'GLFW.GLFW_KEY_ENTER', 'GLFW.GLFW_KEY_BACKSPACE'),
     'unified control invariant')
forbid(placement, ('GLFW_KEY_N', 'GLFW_KEY_J', 'GLFW_KEY_K'), 'legacy fragmented control still registered')
for client in ('RoadPlacementClient.java', 'OutpostPlacementClient.java'):
    client_text = text(JAVA / 'client' / client)
    forbid(client_text, ('KeyMapping', 'RegisterKeyMappingsEvent', 'GLFW_KEY_'), f'{client} owns a separate gameplay key')

lang = json.loads(text(RES / 'assets/frontier_settlement/lang/ko_kr.json'))
for legacy in ('key.frontier_settlement.next_building', 'key.frontier_settlement.road_mode', 'key.frontier_settlement.outpost_mode'):
    if legacy in lang:
        raise SystemExit(f'legacy key localization remains: {legacy}')

for path in (JAVA / 'settlement/SettlementService.java', JAVA / 'settlement/SettlementCoreService.java',
             JAVA / 'settlement/SettlementConstructionService.java', JAVA / 'settlement/SettlementRoadService.java',
             JAVA / 'settlement/SettlementOutpostService.java', JAVA / 'settlement/SettlementWorkerService.java',
             JAVA / 'settlement/SettlementOutpostProductionService.java', JAVA / 'settlement/SettlementOutpostLogisticsService.java',
             JAVA / 'settlement/SettlementTierInfrastructureService.java', JAVA / 'settlement/SettlementMarketService.java',
             JAVA / 'settlement/SettlementWorkshopService.java', JAVA / 'settlement/SettlementCartStationService.java'):
    source = text(path)
    if 'destroyBlock(' in source or 'dropResources(' in source:
        raise SystemExit(f'loose-drop destruction path forbidden: {path.name}')

print('Frontier Settlement alpha.34 source audit: PASS')
