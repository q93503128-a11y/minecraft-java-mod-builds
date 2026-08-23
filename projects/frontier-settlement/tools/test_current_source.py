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
    JAVA / 'client/ClientSettlementState.java', JAVA / 'client/FrontierSettlementClient.java',
    JAVA / 'client/BuildingPlacementClient.java', JAVA / 'client/BuildingPaletteScreen.java',
    JAVA / 'client/RoadPlacementClient.java', JAVA / 'client/OutpostPlacementClient.java',
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
              'mod_id=frontier_settlement', 'mod_version=0.1.0-alpha.21'):
    if token not in props:
        raise SystemExit(f'missing canonical property: {token}')

bootstrap = (JAVA / 'FrontierSettlement.java').read_text(encoding='utf-8')
if 'FrontierContent.register(modBus)' not in bootstrap:
    raise SystemExit('pioneer marker registry is not attached to the mod bus')

marker = (JAVA / 'content/PioneerMarkerItem.java').read_text(encoding='utf-8')
for token in ('SettlementService.foundAt(player, markerPos)', 'context.getItemInHand().shrink(1)',
              'InteractionResult.SUCCESS_SERVER'):
    if token not in marker:
        raise SystemExit(f'pioneer marker invariant missing: {token}')

service = (JAVA / 'settlement/SettlementService.java').read_text(encoding='utf-8')
for token in ('public static FoundResult foundAt(ServerPlayer founder, BlockPos markerPos)',
              'SettlementCoreService.tick(server, data)', 'SettlementTierInfrastructureService.tick(server, data)',
              'buildingUnlockMask(SettlementData data)', 'SettlementConstructionService.lockedReason(data, type)',
              'SettlementTier.current(data).displayName(), buildingUnlockMask(data)'):
    if token not in service:
        raise SystemExit(f'runtime/snapshot invariant missing: {token}')

routine = (JAVA / 'settlement/SettlementResidentRoutineService.java').read_text(encoding='utf-8')
for token in ('level.dimensionType().defaultClock()', 'level.clockManager().getTotalTicks(defaultClock.get())',
              'time >= 13000L && time < 23000L', 'BuildingType.HOUSE'):
    if token not in routine:
        raise SystemExit(f'resident routine invariant missing: {token}')

snapshot = (JAVA / 'network/SettlementSnapshotPayload.java').read_text(encoding='utf-8')
for token in ('int population, String tier, int buildingUnlockMask',
              'buf.writeVarInt(payload.buildingUnlockMask())', 'buf.readVarInt()'):
    if token not in snapshot:
        raise SystemExit(f'palette unlock snapshot invariant missing: {token}')

network = (JAVA / 'network/SettlementNetwork.java').read_text(encoding='utf-8')
if 'PROTOCOL = "6"' not in network:
    raise SystemExit('alpha.21 snapshot shape requires network protocol 6')

building_type = (JAVA / 'settlement/BuildingType.java').read_text(encoding='utf-8')
for token in ('"주택 1채 필요"', '"벌목소 1곳 필요"', '"채석장 + 전초기지 필요"',
              '"농장 1곳 필요"', '"광산 1곳 필요"', '"마을 단계 필요"', 'unlockHint()'):
    if token not in building_type:
        raise SystemExit(f'building progression hint missing: {token}')
if 'CART_DEPOT(' in building_type or 'BARRACKS(' in building_type:
    raise SystemExit('management surface expanded instead of upgrading existing building families')

placement = (JAVA / 'client/BuildingPlacementClient.java').read_text(encoding='utf-8')
for token in ('GLFW.GLFW_KEY_B', 'GLFW.GLFW_KEY_R', 'GLFW.GLFW_KEY_ENTER', 'GLFW.GLFW_KEY_BACKSPACE',
              'minecraft.gui.setScreen(new BuildingPaletteScreen())', 'RoadPlacementClient.confirm()',
              'OutpostPlacementClient.confirm()', 'RoadPlacementClient.resetStart()'):
    if token not in placement:
        raise SystemExit(f'unified control invariant missing: {token}')
for forbidden in ('GLFW_KEY_N', 'GLFW_KEY_J', 'GLFW_KEY_K'):
    if forbidden in placement:
        raise SystemExit(f'legacy fragmented control still registered: {forbidden}')

road_client = (JAVA / 'client/RoadPlacementClient.java').read_text(encoding='utf-8')
outpost_client = (JAVA / 'client/OutpostPlacementClient.java').read_text(encoding='utf-8')
for text, name in ((road_client, 'road'), (outpost_client, 'outpost')):
    for forbidden in ('KeyMapping', 'RegisterKeyMappingsEvent', 'GLFW_KEY_'):
        if forbidden in text:
            raise SystemExit(f'{name} mode still owns a separate gameplay key: {forbidden}')
if 'public static void beginPlacement()' not in road_client or 'public static void confirm()' not in road_client:
    raise SystemExit('road mode is not palette-driven')
if 'public static void beginPlacement()' not in outpost_client or 'public static void confirm()' not in outpost_client:
    raise SystemExit('outpost mode is not palette-driven')

client_bootstrap = (JAVA / 'client/FrontierSettlementClient.java').read_text(encoding='utf-8')
if 'RoadPlacementClient::registerKeys' in client_bootstrap or 'OutpostPlacementClient::registerKeys' in client_bootstrap:
    raise SystemExit('legacy road/outpost key registrations remain')

palette = (JAVA / 'client/BuildingPaletteScreen.java').read_text(encoding='utf-8')
for token in ('BuildingType.HOUSE', 'BuildingType.BLACKSMITH', 'data.buildingUnlockMask()',
              'button.active = unlocked', 'RoadPlacementClient.beginPlacement()',
              'OutpostPlacementClient.beginPlacement()', '자원부족', '잠금:',
              'public boolean isPauseScreen()', 'return false;'):
    if token not in palette:
        raise SystemExit(f'compact palette invariant missing: {token}')
if 'extractBackground' not in palette or 'No blur/full-screen dim' not in palette:
    raise SystemExit('palette must preserve visible world context')

hud = (JAVA / 'client/SettlementHudOverlay.java').read_text(encoding='utf-8')
for token in ('B 팔레트   R 회전   Enter 건설', 'B 팔레트   Enter 시작점',
              'Backspace 시작점 재선택', 'B 팔레트   도로 끝 조준   Enter 건설'):
    if token not in hud:
        raise SystemExit(f'player-facing control hint missing: {token}')
for legacy in ('J 종료', 'K 종료', 'N 건물'):
    if legacy in hud:
        raise SystemExit(f'legacy control hint remains: {legacy}')

lang = json.loads((RES / 'assets/frontier_settlement/lang/ko_kr.json').read_text(encoding='utf-8'))
expected_keys = {
    'key.frontier_settlement.build_mode',
    'key.frontier_settlement.rotate_building',
    'key.frontier_settlement.confirm_building',
    'key.frontier_settlement.road_reset',
}
if not expected_keys.issubset(lang):
    raise SystemExit('unified key localization incomplete')
for legacy in ('key.frontier_settlement.next_building', 'key.frontier_settlement.road_mode',
               'key.frontier_settlement.outpost_mode'):
    if legacy in lang:
        raise SystemExit(f'legacy key localization remains: {legacy}')

for path in (
    JAVA / 'settlement/SettlementService.java', JAVA / 'settlement/SettlementCoreService.java',
    JAVA / 'settlement/SettlementConstructionService.java', JAVA / 'settlement/SettlementRoadService.java',
    JAVA / 'settlement/SettlementOutpostService.java', JAVA / 'settlement/SettlementWorkerService.java',
    JAVA / 'settlement/SettlementOutpostProductionService.java',
    JAVA / 'settlement/SettlementTierInfrastructureService.java'):
    text = path.read_text(encoding='utf-8')
    if 'destroyBlock(' in text or 'dropResources(' in text:
        raise SystemExit(f'loose-drop destruction path forbidden: {path.name}')

print('Frontier Settlement alpha.21 source audit: PASS')
