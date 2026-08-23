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
    JAVA / 'settlement/SettlementCoreService.java', JAVA / 'settlement/SettlementResidentRoutineService.java',
    JAVA / 'settlement/SettlementTierInfrastructureService.java', JAVA / 'settlement/SettlementConstructionService.java',
    JAVA / 'settlement/SettlementRoadService.java', JAVA / 'settlement/SettlementOutpostService.java',
    JAVA / 'settlement/SettlementOutpostProductionService.java', JAVA / 'settlement/SettlementWorkerService.java',
    JAVA / 'settlement/SettlementBenefitService.java', JAVA / 'settlement/SettlementStorageService.java',
    JAVA / 'settlement/BuildingType.java', JAVA / 'settlement/BuildingBlueprints.java',
    JAVA / 'settlement/AdvancedBuildingBlueprints.java', JAVA / 'settlement/SettlementTier.java',
    JAVA / 'network/SettlementNetwork.java', JAVA / 'network/SettlementSnapshotPayload.java',
    JAVA / 'client/SettlementHudOverlay.java',
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
    raise SystemExit('pioneer marker registry is not attached to the mod bus')

content = (JAVA / 'content/FrontierContent.java').read_text(encoding='utf-8')
for token in ('DeferredRegister.createItems(FrontierSettlement.MOD_ID)', '"pioneer_marker"',
              'PioneerMarkerItem::new', 'properties -> properties.stacksTo(1)', 'ITEMS.register(modBus)'):
    if token not in content:
        raise SystemExit(f'pioneer marker registration invariant missing: {token}')

marker = (JAVA / 'content/PioneerMarkerItem.java').read_text(encoding='utf-8')
for token in ('public InteractionResult useOn(UseOnContext context)',
              'context.getClickedPos().relative(context.getClickedFace())',
              'SettlementService.foundAt(player, markerPos)',
              'if (!result.founded()) return InteractionResult.FAIL',
              'context.getItemInHand().shrink(1)', 'InteractionResult.SUCCESS_SERVER'):
    if token not in marker:
        raise SystemExit(f'pioneer marker interaction invariant missing: {token}')
if marker.index('if (!result.founded())') > marker.index('shrink(1)'):
    raise SystemExit('pioneer marker is consumed before founding succeeds')

service = (JAVA / 'settlement/SettlementService.java').read_text(encoding='utf-8')
for token in ('public record FoundResult(boolean founded, String message)',
              'public static FoundResult foundAt(ServerPlayer founder, BlockPos markerPos)',
              'MAX_MARKER_DISTANCE_SQR', 'isSafeMarkerPosition(level, center)',
              'isSafeStockpilePosition(level, candidate)', 'Blocks.OAK_FENCE.defaultBlockState()',
              'Blocks.TORCH.defaultBlockState()', 'Blocks.BARREL.defaultBlockState()',
              'data.found(center, stockpile)', 'SettlementConstructionService.ensureBuilder(level, center)',
              'SettlementCoreService.tick(server, data)', 'SettlementTierInfrastructureService.tick(server, data)',
              'SettlementBenefitService.tick(server, data)', 'SettlementTier.current(data).displayName()'):
    if token not in service:
        raise SystemExit(f'founding/runtime invariant missing: {token}')

core = (JAVA / 'settlement/SettlementCoreService.java').read_text(encoding='utf-8')
for token in ('BlockPos center = data.centerPos()', 'MAX_PLACEMENTS_PER_TICK = 6',
              'SettlementTier.DOMAIN.ordinal()', 'addLampRing', 'canSafelyReplace'):
    if token not in core:
        raise SystemExit(f'civic core invariant missing: {token}')

routine = (JAVA / 'settlement/SettlementResidentRoutineService.java').read_text(encoding='utf-8')
for token in ('ClockManager', 'getTotalTicks(defaultClock)', 'time >= 13000L && time < 23000L',
              'BuildingType.HOUSE', 'house.localToWorld'):
    if token not in routine:
        raise SystemExit(f'resident routine invariant missing: {token}')

infra = (JAVA / 'settlement/SettlementTierInfrastructureService.java').read_text(encoding='utf-8')
for token in ('SettlementTier.FRONTIER_TOWN.ordinal()', '!SettlementResidentRoutineService.isRestTime(level)',
              'data.buildingCount(BuildingType.WAREHOUSE) > 0', 'data.buildingCount(BuildingType.BLACKSMITH) > 0',
              'tier == SettlementTier.DOMAIN ? 48 : 32', 'tier == SettlementTier.DOMAIN ? 2 : 1',
              'BuildingType.GUARD_POST', 'new IronGolem(EntityTypes.IRON_GOLEM, level)'):
    if token not in infra:
        raise SystemExit(f'tier infrastructure invariant missing: {token}')

recipe = json.loads((RES / 'data/frontier_settlement/recipe/pioneer_marker.json').read_text(encoding='utf-8'))
if recipe.get('type') != 'minecraft:crafting_shaped':
    raise SystemExit('pioneer marker must have a normal survival crafting recipe')
if recipe.get('result', {}).get('id') != 'frontier_settlement:pioneer_marker':
    raise SystemExit('pioneer marker recipe result is wrong')
if set(recipe.get('key', {}).values()) != {'minecraft:white_wool', 'minecraft:stick', 'minecraft:chest'}:
    raise SystemExit('pioneer marker recipe ingredients changed unexpectedly')

item_model = json.loads((RES / 'assets/frontier_settlement/items/pioneer_marker.json').read_text(encoding='utf-8'))
if item_model.get('model', {}).get('type') != 'minecraft:model':
    raise SystemExit('pioneer marker client item model is missing')
lang = json.loads((RES / 'assets/frontier_settlement/lang/ko_kr.json').read_text(encoding='utf-8'))
if lang.get('item.frontier_settlement.pioneer_marker') != '개척 표식':
    raise SystemExit('pioneer marker Korean name is missing')

snapshot = (JAVA / 'network/SettlementSnapshotPayload.java').read_text(encoding='utf-8')
if 'int population, String tier' not in snapshot:
    raise SystemExit('settlement tier snapshot regressed')
hud = (JAVA / 'client/SettlementHudOverlay.java').read_text(encoding='utf-8')
if 'String line = data.tier()' not in hud:
    raise SystemExit('settlement tier HUD regressed')
network = (JAVA / 'network/SettlementNetwork.java').read_text(encoding='utf-8')
if 'PROTOCOL = "5"' not in network:
    raise SystemExit('network protocol regressed')

building_type = (JAVA / 'settlement/BuildingType.java').read_text(encoding='utf-8')
if 'CART_DEPOT(' in building_type or 'BARRACKS(' in building_type:
    raise SystemExit('management surface expanded instead of upgrading existing building families')

for path in (
    JAVA / 'settlement/SettlementService.java', JAVA / 'settlement/SettlementCoreService.java',
    JAVA / 'settlement/SettlementConstructionService.java', JAVA / 'settlement/SettlementRoadService.java',
    JAVA / 'settlement/SettlementOutpostService.java', JAVA / 'settlement/SettlementWorkerService.java',
    JAVA / 'settlement/SettlementOutpostProductionService.java',
    JAVA / 'settlement/SettlementTierInfrastructureService.java'):
    text = path.read_text(encoding='utf-8')
    if 'destroyBlock(' in text or 'dropResources(' in text:
        raise SystemExit(f'loose-drop destruction path forbidden: {path.name}')

print('Frontier Settlement alpha.20 source audit: PASS')
