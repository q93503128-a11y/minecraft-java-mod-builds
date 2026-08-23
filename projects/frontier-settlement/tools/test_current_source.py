#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
RES = ROOT / 'src/main/resources'
required = [
    ROOT / 'build.gradle', ROOT / 'gradle.properties',
    JAVA / 'FrontierSettlement.java',
    JAVA / 'content/FrontierContent.java', JAVA / 'content/PioneerMarkerItem.java',
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
    JAVA / 'settlement/SettlementTierInfrastructureService.java',
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
    RES / 'assets/frontier_settlement/items/pioneer_marker.json',
    RES / 'assets/frontier_settlement/lang/ko_kr.json',
    RES / 'data/frontier_settlement/recipe/pioneer_marker.json',
]
missing = [str(p.relative_to(ROOT)) for p in required if not p.is_file()]
if missing:
    raise SystemExit('missing required files: ' + ', '.join(missing))

props = (ROOT / 'gradle.properties').read_text(encoding='utf-8')
for token in ('minecraft_version=26.2', 'neo_version=26.2.0.38-beta',
              'mod_id=frontier_settlement', 'mod_version=0.1.0-alpha.20'):
    if token not in props:
        raise SystemExit(f'missing canonical property: {token}')

bootstrap = (JAVA / 'FrontierSettlement.java').read_text(encoding='utf-8')
if 'FrontierContent.register(modBus)' not in bootstrap:
    raise SystemExit('pioneer marker content registry is not attached to the mod bus')

content = (JAVA / 'content/FrontierContent.java').read_text(encoding='utf-8')
for token in ('DeferredRegister.createItems(FrontierSettlement.MOD_ID)', '"pioneer_marker"',
              'PioneerMarkerItem::new', 'new net.minecraft.world.item.Item.Properties().stacksTo(1)',
              'ITEMS.register(modBus)'):
    if token not in content:
        raise SystemExit(f'pioneer marker registration invariant missing: {token}')

marker_item = (JAVA / 'content/PioneerMarkerItem.java').read_text(encoding='utf-8')
for token in ('public InteractionResult useOn(UseOnContext context)',
              'context.getClickedPos().relative(context.getClickedFace())',
              'SettlementService.foundAt(player, markerPos)',
              'if (!result.founded()) return InteractionResult.FAIL',
              'context.getItemInHand().shrink(1)', 'InteractionResult.SUCCESS_SERVER'):
    if token not in marker_item:
        raise SystemExit(f'pioneer marker interaction invariant missing: {token}')
if marker_item.index('if (!result.founded())') > marker_item.index('shrink(1)'):
    raise SystemExit('pioneer marker must never be consumed before founding succeeds')

service = (JAVA / 'settlement/SettlementService.java').read_text(encoding='utf-8')
for token in ('public record FoundResult(boolean founded, String message)',
              'public static FoundResult foundAt(ServerPlayer founder, BlockPos markerPos)',
              'foundInternal(founder, markerPos, true)', 'MAX_MARKER_DISTANCE_SQR',
              'isSafeMarkerPosition(level, center)', 'isSafeStockpilePosition(level, candidate)',
              'Blocks.OAK_FENCE.defaultBlockState()', 'Blocks.TORCH.defaultBlockState()',
              'Blocks.BARREL.defaultBlockState()', 'data.found(center, stockpile)',
              'SettlementConstructionService.ensureBuilder(level, center)',
              'SettlementCoreService.tick(server, data)',
              'SettlementResidentRoutineService.isRestTime(server.overworld())',
              'SettlementTierInfrastructureService.tick(server, data)',
              'SettlementBenefitService.tick(server, data)',
              'SettlementTier.current(data).displayName()'):
    if token not in service:
        raise SystemExit(f'server-authoritative founding/runtime invariant missing: {token}')
if 'destroyBlock(' in service or 'dropResources(' in service:
    raise SystemExit('settlement founding must not use drop-producing destruction')

core = (JAVA / 'settlement/SettlementCoreService.java').read_text(encoding='utf-8')
for token in ('MAX_PLACEMENTS_PER_TICK = 6', 'BlockPos center = data.centerPos()',
              'SettlementTier.current(data)', 'SettlementTier.HAMLET.ordinal()',
              'SettlementTier.VILLAGE.ordinal()', 'SettlementTier.FRONTIER_TOWN.ordinal()',
              'SettlementTier.DOMAIN.ordinal()', 'addLampRing', 'canSafelyReplace',
              'isNaturalGround', 'level.setBlock'):
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

infrastructure = (JAVA / 'settlement/SettlementTierInfrastructureService.java').read_text(encoding='utf-8')
for token in ('SettlementTier.FRONTIER_TOWN.ordinal()', 'SettlementTier.DOMAIN',
              'data.buildingCount(BuildingType.WAREHOUSE) > 0',
              'data.buildingCount(BuildingType.BLACKSMITH) > 0',
              '!SettlementResidentRoutineService.isRestTime(level)',
              'int carryLimit = tier == SettlementTier.DOMAIN ? 48 : 32',
              'double moveSpeed = tier == SettlementTier.DOMAIN ? 1.15D : 1.05D',
              'topUpMatching(container, carried, carryLimit)',
              'SettlementStorageService.findDepositTarget',
              'int reinforcementsPerPost = tier == SettlementTier.DOMAIN ? 2 : 1',
              'BuildingType.GUARD_POST', 'new IronGolem(EntityTypes.IRON_GOLEM, level)',
              'guard.setPersistenceRequired()', 'guard.setPlayerCreated(true)'):
    if token not in infrastructure:
        raise SystemExit(f'tier infrastructure invariant missing: {token}')
if 'destroyBlock(' in infrastructure or 'dropResources(' in infrastructure:
    raise SystemExit('tier infrastructure must not create loose block drops')

recipe = json.loads((RES / 'data/frontier_settlement/recipe/pioneer_marker.json').read_text(encoding='utf-8'))
if recipe.get('type') != 'minecraft:crafting_shaped' or recipe.get('result', {}).get('id') != 'frontier_settlement:pioneer_marker':
    raise SystemExit('pioneer marker survival recipe is invalid')
if set(recipe.get('key', {}).values()) != {'minecraft:white_wool', 'minecraft:stick', 'minecraft:chest'}:
    raise SystemExit('pioneer marker recipe ingredients changed unexpectedly')

client_item = json.loads((RES / 'assets/frontier_settlement/items/pioneer_marker.json').read_text(encoding='utf-8'))
model = client_item.get('model', {})
if model.get('type') != 'minecraft:model' or model.get('model') != 'minecraft:item/white_banner':
    raise SystemExit('pioneer marker client item model is missing')

lang = json.loads((RES / 'assets/frontier_settlement/lang/ko_kr.json').read_text(encoding='utf-8'))
if lang.get('item.frontier_settlement.pioneer_marker') != '개척 표식':
    raise SystemExit('pioneer marker Korean translation is missing')

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
if 'CART_DEPOT(' in building_type or 'BARRACKS(' in building_type:
    raise SystemExit('management surface must stay small; use existing building-family upgrades')

construction = (JAVA / 'settlement/SettlementConstructionService.java').read_text(encoding='utf-8')
for token in ('public static StartResult startAt', 'public static PlacementCheck checkPlacement',
              'MAX_MAIN_SETTLEMENT_RADIUS = 72', 'MAX_PLAYER_PLACEMENT_DISTANCE = 24',
              'SettlementStorageService.consume(level, data, type.woodCost(), type.stoneCost(), 0L)'):
    if token not in construction:
        raise SystemExit(f'construction invariant missing: {token}')
if 'destroyBlock(' in construction or 'dropResources(' in construction:
    raise SystemExit('building construction must not use drop-producing destruction paths')

network = (JAVA / 'network/SettlementNetwork.java').read_text(encoding='utf-8')
if 'PROTOCOL = "5"' not in network:
    raise SystemExit('tier-aware networking protocol must remain at version 5')

for renderer_name in ('PlacementGhostRenderer.java', 'RoadGhostRenderer.java', 'OutpostGhostRenderer.java'):
    renderer = (JAVA / 'client' / renderer_name).read_text(encoding='utf-8')
    for token in ('ExtractLevelRenderStateEvent', 'SubmitCustomGeometryEvent', 'submitShapeOutline'):
        if token not in renderer:
            raise SystemExit(f'3D ghost renderer invariant missing in {renderer_name}: {token}')

for path in JAVA.rglob('*.java'):
    text = path.read_text(encoding='utf-8')
    if path.name in {'SettlementService.java', 'SettlementConstructionService.java', 'SettlementRoadService.java',
                     'SettlementOutpostService.java', 'SettlementWorkerService.java',
                     'SettlementOutpostProductionService.java', 'SettlementCoreService.java',
                     'SettlementTierInfrastructureService.java'}:
        if 'destroyBlock(' in text or 'dropResources(' in text:
            raise SystemExit(f'loose-drop destruction path forbidden: {path.name}')

print('Frontier Settlement alpha.20 source audit: PASS')
