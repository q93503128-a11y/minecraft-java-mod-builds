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
    JAVA / 'settlement/SettlementData.java', JAVA / 'settlement/ConstructionState.java',
    JAVA / 'settlement/RoadConstructionState.java', JAVA / 'settlement/OutpostConstructionState.java',
    JAVA / 'settlement/SettlementService.java', JAVA / 'settlement/SettlementGuidanceService.java',
    JAVA / 'settlement/SettlementCoreService.java', JAVA / 'settlement/SettlementResidentRoutineService.java',
    JAVA / 'settlement/SettlementTierInfrastructureService.java', JAVA / 'settlement/SettlementConstructionService.java',
    JAVA / 'settlement/SettlementRoadService.java', JAVA / 'settlement/SettlementOutpostService.java',
    JAVA / 'settlement/SettlementOutpostProductionService.java', JAVA / 'settlement/SettlementOutpostLogisticsService.java',
    JAVA / 'settlement/SettlementWorkerService.java', JAVA / 'settlement/SettlementBenefitService.java',
    JAVA / 'settlement/SettlementInventory.java', JAVA / 'settlement/SettlementStorageService.java',
    JAVA / 'settlement/SettlementExternalContentService.java', JAVA / 'settlement/SettlementMarketService.java',
    JAVA / 'settlement/MarketLayout.java', JAVA / 'settlement/MarketBuildingBlueprint.java',
    JAVA / 'settlement/SettlementWorkshopService.java', JAVA / 'settlement/WorkshopLayout.java',
    JAVA / 'settlement/WorkshopBuildingBlueprint.java', JAVA / 'settlement/SettlementCartStationService.java',
    JAVA / 'settlement/CartStationLayout.java', JAVA / 'settlement/CartStationBuildingBlueprint.java',
    JAVA / 'settlement/BuildingType.java', JAVA / 'settlement/BuildingBlueprints.java', JAVA / 'settlement/AdvancedBuildingBlueprints.java',
    JAVA / 'settlement/SettlementTier.java', JAVA / 'network/SettlementNetwork.java', JAVA / 'network/SettlementSnapshotPayload.java',
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
             'mod_version=0.1.0-alpha.35'), 'canonical property')

plan = text(ROOT / 'CANONICAL_PLAN.md')
must(plan, ('survival -> settlement growth', 'One world/server has one shared settlement',
            'Resources remain physical Minecraft items', '`B`: settlement palette', '`R`: rotate current building placement',
            '`Enter`: confirm', '`Backspace`: reset/cancel',
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
if lock.get('target', {}).get('frontier_settlement') != '0.1.0-alpha.35':
    raise SystemExit('companion lock Frontier version drifted from alpha.35')
lock_ids = {entry.get('id') for entry in lock.get('entries', [])}
for required_mod in ('terralith', 'dungeons_and_taverns', 'repurposed_structures', 'better_combat', 'weapons_expanded',
                     'lootr', 'sophisticated_backpacks', 'jade', 'xaeros_minimap', 'lithostitched'):
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
forbid(service, ('SettlementCartStationService.tick(',), 'cart station introduced second logistics tick authority')
must(service, ('SettlementWorkshopService.lockedReason(data)', 'SettlementCartStationService.lockedReason(data)'),
     'special building unlock mask invariant')

construction_state = text(JAVA / 'settlement/ConstructionState.java')
must(construction_state, ('GRADE_STEP_OFFSET = 1_000_000', 'BUILD_STEP_OFFSET = 2_000_000', 'grading()',
                          'physicalBuilding()', 'legacyPreparedBuilding()', 'gradeStep()', 'buildStep()',
                          'scaffoldMask', 'withScaffoldMask'), 'building phase persistence')

road_state = text(JAVA / 'settlement/RoadConstructionState.java')
must(road_state, ('GRADE_STEP_OFFSET = 1_000_000', 'PAVE_STEP_OFFSET = 2_000_000', 'legacyPrepaidPaving()',
                  'physicalPaving()', 'PROFILE_NORMAL = 0', 'PROFILE_BRIDGE = 1',
                  'optionalFieldOf("profile", List.of())', 'fromPath(List<BlockPos> centers, List<Integer> profile)',
                  'bridgeAt(int centerIndex)', 'bridgeCenterCount()', 'path.size() % 3 == 0'),
     'alpha.35 road profile persistence')
settlement_data = text(JAVA / 'settlement/SettlementData.java')
must(settlement_data, ('beginRoadConstruction(List<BlockPos> centers, List<Integer> profile)',
                       'RoadConstructionState.fromPath(centers, profile)'), 'alpha.35 profiled road save authority')

outpost_state = text(JAVA / 'settlement/OutpostConstructionState.java')
must(outpost_state, ('GRADE_STEP_OFFSET = 1_000_000', 'BUILD_STEP_OFFSET = 2_000_000',
                     'physicalBuilding()', 'legacyPrepaidBuilding()', 'buildStep()', 'legacyStep()'),
     'outpost phase persistence')

building_type = text(JAVA / 'settlement/BuildingType.java')
must(building_type, ('MARKET("market", "시장", 96, 48, 11, 11, 8, 0',
                     'WORKSHOP("workshop", "작업장", 88, 44, 11, 9, 10, 0',
                     'CART_STATION("cart_station", "수레 정거장", 104, 56, 13, 9, 8, 0'),
     'functional building definition')
blueprints = text(JAVA / 'settlement/BuildingBlueprints.java')
must(blueprints, ('case MARKET -> MarketBuildingBlueprint.create(origin);',
                  'case WORKSHOP -> WorkshopBuildingBlueprint.create(origin);',
                  'case CART_STATION -> CartStationBuildingBlueprint.create(origin);'), 'functional blueprint routing')

construction = text(JAVA / 'settlement/SettlementConstructionService.java')
must(construction, ('HAUL_BATCH_SIZE = 16', 'SettlementStorageService.findExtractionTarget', 'EquipmentSlot.MAINHAND',
                    'SettlementInventory.consume(crate, woodDelta, stoneDelta, 0L)', 'builder.setInvulnerable(true)',
                    'builder.swing(InteractionHand.MAIN_HAND)', 'construction.ownsScaffold(towerIndex)',
                    'data.replaceConstructionStep(ConstructionState.GRADE_STEP_OFFSET)', 'construction.grading()',
                    'tickGrading(', 'createGradePlan(', 'canGradeCell(', 'applyGradeCell(', 'ConstructionState.BUILD_STEP_OFFSET',
                    'Blocks.COARSE_DIRT.defaultBlockState()', 'GRADE_INTERVAL_TICKS = 8', 'MAX_GRADE_FILL_DEPTH = 3',
                    'construction.buildStep()', '건물 부지 정리', 'level.hasChunkAt(supply)'),
     'physical building construction')
forbid(construction, ('SettlementStorageService.consume(level, data, type.woodCost(), type.stoneCost(), 0L)',
                      'prepareSite(', 'destroyBlock(', 'dropResources(', 'towerOwned('),
       'physical building construction violated')

road = text(JAVA / 'settlement/SettlementRoadService.java')
must(road, ('MAX_BRIDGE_SPAN = 6', 'BRIDGE_SURCHARGE_PER_CENTER = 2', 'STAIR_SURCHARGE_PER_CENTER = 1',
            'FluidTags.WATER', 'sampleSurface(', 'bridgeColumnSafe(', 'chooseCandidate(',
            'RoadConstructionState.PROFILE_BRIDGE', 'data.beginRoadConstruction(chosen.centers(), chosen.profile())',
            'Blocks.COBBLESTONE_STAIRS.defaultBlockState()', 'StairBlock.FACING',
            'Blocks.STONE_BRICKS.defaultBlockState()', 'placement.bridge() || hasOrCanMakeSupport',
            'if (!placement.bridge())', 'stoneCost(RouteCandidate candidate)', 'stoneCost(RoadConstructionState road)',
            'SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isStone)',
            'HAUL_BATCH_SIZE = 16', 'EquipmentSlot.MAINHAND', 'consumeCarriedStone(', 'returnCarriedToStorage(',
            'builder.setInvulnerable(true)', 'builder.swing(InteractionHand.MAIN_HAND)',
            'applyGradePlacement(', 'Blocks.COARSE_DIRT.defaultBlockState()', 'BreakBlockEvent',
            '도로 지반', '교량', '계단'), 'alpha.35 physical road stairs/bridge invariant')
forbid(road, ('SettlementStorageService.consume(level, data, 0L, check.stoneCost(), 0L)',
              'prepareRoute(level, check.centers())', 'destroyBlock(', 'dropResources(',
              'Blocks.OAK_LOG.defaultBlockState()', 'Blocks.COBBLESTONE.defaultBlockState(), DIRECT_BLOCK_UPDATE'),
       'road must not prepay, drop resources, or spawn free economic bridge supports')

outpost = text(JAVA / 'settlement/SettlementOutpostService.java')
must(outpost, ('SettlementStorageService.storageAvailable(level, data)',
               'OutpostConstructionState.GRADE_STEP_OFFSET', 'state.grading()', 'tickGrading(', 'applyGradeCell(',
               'Blocks.COARSE_DIRT.defaultBlockState()', 'state.legacyPrepaidBuilding()', 'tickLegacyPrepaid(',
               'tickPhysicalBuilding(', 'SettlementStorageService.findExtractionTarget(level, data, predicate)',
               'HAUL_BATCH_SIZE = 16', 'EquipmentSlot.MAINHAND', 'consumeCarried(', 'returnCarriedToStorage(',
               'builder.setInvulnerable(true)', 'builder.swing(InteractionHand.MAIN_HAND)'), 'physical outpost construction')
forbid(outpost, ('SettlementStorageService.consume(level, data, WOOD_COST, STONE_COST, 0L)',
                 'prepareSite(level, gate, road.directionX(), road.directionZ())', 'destroyBlock(', 'dropResources('),
       'outpost construction violated')

production = text(JAVA / 'settlement/SettlementOutpostProductionService.java')
must(production, ('PRODUCTION_WORKER_TAG', 'PRODUCTION_OUTPOST_TAG_PREFIX', 'outpostLoaded(', 'level.hasChunkAt(',
                  'LUMBER_WORK_PERIOD_TICKS = 100', 'QUARRY_WORK_PERIOD_TICKS = 80', 'MINING_WORK_PERIOD_TICKS = 160',
                  'AGRICULTURE_WORK_PERIOD_TICKS = 120', 'MAX_LOGS = 4', 'MAX_STONE = 3', 'MAX_CROPS = 4',
                  'workDue(', 'worker.swing(InteractionHand.MAIN_HAND)', 'pristineLegacyAgriculturePlot('),
     'bounded outpost production')
forbid(production, ('forceChunk', 'setChunkForced'), 'outpost production force-load')

logistics = text(JAVA / 'settlement/SettlementOutpostLogisticsService.java')
must(logistics, ('TRANSPORT_WORKER_TAG', 'TRANSPORT_OUTPOST_TAG_PREFIX', 'migrateLegacyWorkers(', 'routeFromTown(',
                 'appendRoadPrefixFromTown(', 'road.centers()', 'ROAD_WAYPOINT_STRIDE = 3', 'routeFullyLoaded(',
                 'level.hasChunkAt(', 'firstMissingLoadedAssignment(', 'takeFirstTransportStack(',
                 'SettlementInventory.isWood', 'SettlementInventory.isStone', 'SettlementInventory.isFood',
                 'EquipmentSlot.MAINHAND', 'BASE_TRANSPORT_STACK = 16', 'CART_STATION_TRANSPORT_STACK = 32',
                 'transportBatchSize(SettlementData data)', 'BuildingType.CART_STATION',
                 'SettlementStorageService.findLogisticsDepositTarget(level, data, carried)',
                 'SettlementStorageService.insertAt(level, target, carried)'), 'single road-bound logistics authority')
forbid(logistics, ('forceChunk', 'setChunkForced', 'SettlementStorageService.findDepositTarget(level, data, carried)'),
       'outpost logistics invariant violated')

worker = text(JAVA / 'settlement/SettlementWorkerService.java')
must(worker, ('SettlementOutpostLogisticsService.tick(level, data)',
              'SettlementOutpostLogisticsService.allRoutesLoaded(level, data)',
              'SettlementWorkshopService.allAssignmentsLoaded(level, data)',
              'LUMBER_WORK_PERIOD_TICKS = 100', 'FARM_WORK_PERIOD_TICKS = 120',
              'QUARRY_WORK_PERIOD_TICKS = 80', 'MINING_WORK_PERIOD_TICKS = 160',
              'ARRIVAL_FOOD_COST = 4L', 'consumeArrivalFood(level, data)', 'workDue(',
              'worker.swing(InteractionHand.MAIN_HAND)', 'level.hasChunkAt('), 'bounded town worker invariant')
forbid(worker, ('TRANSPORT_WORKER_NAME', 'workTransport(', 'takeFirstStack('), 'legacy transport backend remains')

core = text(JAVA / 'settlement/SettlementCoreService.java')
must(core, ('BreakBlockEvent', 'event.setCanceled(true)', 'event.setNotifyClient(true)',
            'pos.equals(data.stockpilePos()) && current.is(Blocks.BARREL)'), 'civic/stockpile protection')

tier_infra = text(JAVA / 'settlement/SettlementTierInfrastructureService.java')
must(tier_infra, ('FRONTIER_TOWN_LAMP_SPACING = 16', 'DOMAIN_LAMP_SPACING = 8', 'maintainRoadPublicWorks(',
                  'level.hasChunkAt(', 'maintainTierGarrison(', 'BreakBlockEvent'), 'tier public works')
forbid(tier_infra, ('TRANSPORT_WORKER_NAME', 'transportWorkers(', 'topUpMatching('), 'legacy tier transport authority remains')

external_tags = text(JAVA / 'compat/ExternalContentTags.java')
must(external_tags, ('SETTLEMENT_WOOD', 'SETTLEMENT_STONE', 'SETTLEMENT_METAL', 'SETTLEMENT_FOOD',
                     'EXPEDITION_RELICS', 'C_INGOTS', 'C_RAW_MATERIALS', 'C_STONES', 'C_COBBLESTONES', 'C_FOODS'),
     'external content tags')
inventory = text(JAVA / 'settlement/SettlementInventory.java')
must(inventory, ('ExternalContentTags.SETTLEMENT_WOOD', 'ExternalContentTags.C_STONES',
                 'ExternalContentTags.C_COBBLESTONES', 'ExternalContentTags.SETTLEMENT_STONE',
                 'ExternalContentTags.C_FOODS', 'ExternalContentTags.SETTLEMENT_FOOD'), 'external material classification')
storage = text(JAVA / 'settlement/SettlementStorageService.java')
must(storage, ('storageAvailable(ServerLevel level, SettlementData data)', 'findExtractionTarget(', 'findDepositTarget(',
               'extract(', 'isMetalStack(ItemStack stack)', 'BuildingType.CART_STATION',
               'CartStationLayout.freightPositions(building)', 'findLogisticsDepositTarget(', 'insertAt('),
     'physical storage/cart hub invariant')

external = text(JAVA / 'settlement/SettlementExternalContentService.java')
must(external, ('SettlementStorageService.storageAvailable(level, data)', 'ExternalContentTags.EXPEDITION_RELICS',
                'BuiltInRegistries.ITEM.getKey', 'EXTERNAL_WEAPON_NAMESPACES', '"weaponsexpanded"',
                'stack.isDamageableItem()', 'FrontierSettlement.MOD_ID'), 'external content bridge')
forbid(external, ('ModList',), 'external bridge hard dependency')

market = text(JAVA / 'settlement/SettlementMarketService.java')
must(market, ('MARKET_TRADER_TAG', 'TRADE_PERIOD_TICKS = 100', 'ExternalContentTags.EXPEDITION_RELICS',
              'MarketLayout.tradeCrate(market)', 'new ItemStack(Items.EMERALD, payout)', 'hasEmeraldRoom(',
              'trader.swing(InteractionHand.MAIN_HAND)', 'BreakBlockEvent'), 'physical market')
forbid(market, ('SettlementStorageService.extract(', 'SettlementStorageService.storagePositions(',
                'SettlementStorageService.consume(', 'destroyBlock(', 'dropResources('), 'market shared-storage violation')

workshop = text(JAVA / 'settlement/SettlementWorkshopService.java')
must(workshop, ('WORKSHOP_WORKER_TAG', 'SERVICE_PERIOD_TICKS = 100', 'REPAIR_PER_METAL = 64',
                'WorkshopLayout.serviceCrate(workshop)', 'SettlementExternalContentService.isExternalWeapon',
                'SettlementStorageService.findExtractionTarget(level, data, SettlementStorageService::isMetalStack)',
                'SettlementStorageService.extract(level, source, SettlementStorageService::isMetalStack, 1)',
                'EquipmentSlot.MAINHAND', 'worker.swing(InteractionHand.MAIN_HAND)', 'returnCarriedItem(',
                'allAssignmentsLoaded(', 'loadedAssignedWorkerCount(', 'firstMissingLoadedAssignment(', 'spawnAssignedWorker('),
     'physical workshop')
forbid(workshop, ('SettlementStorageService.insert(level, data, carried)', 'destroyBlock(', 'dropResources('),
       'workshop physical hauling violation')

cart_service = text(JAVA / 'settlement/SettlementCartStationService.java')
must(cart_service, ('lockedReason(SettlementData data)', 'data.roads().isEmpty()', 'data.outposts().isEmpty()',
                    'BuildingType.CART_STATION', 'CartStationLayout.freightPositions(station)', 'BreakBlockEvent'),
     'cart station service')
forbid(cart_service, ('void tick(', 'getNavigation().moveTo(', 'spawnAssignedWorker('),
       'cart station second transport authority')

commands = text(JAVA / 'command/SettlementCommands.java')
must(commands, ('Commands.literal("market")', 'Commands.literal("workshop")', 'Commands.literal("cart_station")',
                 'SettlementExternalContentService.snapshot', 'SettlementOutpostLogisticsService.transportBatchSize(data)',
                 '운송 1회 적재'), 'command/status integration')
network = text(JAVA / 'network/SettlementNetwork.java')
must(network, ('PROTOCOL = "7"', 'SettlementWorkshopService.lockedReason(data)',
               'SettlementCartStationService.lockedReason(data)'), 'network server guards')
palette = text(JAVA / 'client/BuildingPaletteScreen.java')
must(palette, ('BuildingType.MARKET', 'BuildingType.WORKSHOP', 'BuildingType.CART_STATION', '물류·교역'),
     'compact palette')

placement = text(JAVA / 'client/BuildingPlacementClient.java')
must(placement, ('GLFW.GLFW_KEY_B', 'GLFW.GLFW_KEY_R', 'GLFW.GLFW_KEY_ENTER', 'GLFW.GLFW_KEY_BACKSPACE'),
     'unified controls')
forbid(placement, ('GLFW_KEY_N', 'GLFW_KEY_J', 'GLFW_KEY_K'), 'legacy fragmented control')
for client in ('RoadPlacementClient.java', 'OutpostPlacementClient.java'):
    client_text = text(JAVA / 'client' / client)
    forbid(client_text, ('KeyMapping', 'RegisterKeyMappingsEvent', 'GLFW_KEY_'), f'{client} owns separate gameplay key')

for tag_name in ('settlement_wood', 'settlement_stone', 'settlement_metal', 'settlement_food', 'expedition_relics'):
    tag = json.loads(text(RES / f'data/frontier_settlement/tags/item/{tag_name}.json'))
    if tag.get('replace') is not False or not tag.get('values'):
        raise SystemExit(f'external data tag must be additive and non-empty: {tag_name}')

for path in (JAVA / 'settlement/SettlementService.java', JAVA / 'settlement/SettlementCoreService.java',
             JAVA / 'settlement/SettlementConstructionService.java', JAVA / 'settlement/SettlementRoadService.java',
             JAVA / 'settlement/SettlementOutpostService.java', JAVA / 'settlement/SettlementWorkerService.java',
             JAVA / 'settlement/SettlementOutpostProductionService.java', JAVA / 'settlement/SettlementOutpostLogisticsService.java',
             JAVA / 'settlement/SettlementTierInfrastructureService.java', JAVA / 'settlement/SettlementMarketService.java',
             JAVA / 'settlement/SettlementWorkshopService.java', JAVA / 'settlement/SettlementCartStationService.java'):
    source = text(path)
    if 'destroyBlock(' in source or 'dropResources(' in source:
        raise SystemExit(f'loose-drop destruction path forbidden: {path.name}')

print('Frontier Settlement alpha.35 source audit: PASS')
