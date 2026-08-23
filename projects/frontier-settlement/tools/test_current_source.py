#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
RES = ROOT / 'src/main/resources'

required = [
    ROOT / 'build.gradle', ROOT / 'gradle.properties',
    JAVA / 'FrontierSettlement.java', JAVA / 'content/FrontierContent.java', JAVA / 'content/PioneerMarkerItem.java',
    JAVA / 'settlement/SettlementData.java', JAVA / 'settlement/SettlementService.java',
    JAVA / 'settlement/SettlementGuidanceService.java', JAVA / 'settlement/SettlementCoreService.java',
    JAVA / 'settlement/SettlementResidentRoutineService.java', JAVA / 'settlement/SettlementTierInfrastructureService.java',
    JAVA / 'settlement/SettlementConstructionService.java', JAVA / 'settlement/SettlementRoadService.java',
    JAVA / 'settlement/SettlementOutpostService.java', JAVA / 'settlement/SettlementOutpostProductionService.java',
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
for token in ('minecraft_version=26.2', 'neo_version=26.2.0.38-beta', 'mod_id=frontier_settlement', 'mod_version=0.1.0-alpha.22'):
    if token not in props: raise SystemExit(f'missing canonical property: {token}')

marker = (JAVA / 'content/PioneerMarkerItem.java').read_text(encoding='utf-8')
for token in ('SettlementService.foundAt(player, markerPos)', 'context.getItemInHand().shrink(1)', 'InteractionResult.SUCCESS_SERVER'):
    if token not in marker: raise SystemExit(f'pioneer marker invariant missing: {token}')

service = (JAVA / 'settlement/SettlementService.java').read_text(encoding='utf-8')
for token in ('SettlementCoreService.tick(server, data)', 'SettlementTierInfrastructureService.tick(server, data)',
              'buildingUnlockMask(SettlementData data)', 'SettlementConstructionService.lockedReason(data, type)',
              'SettlementGuidanceService.nextGoal(data)'):
    if token not in service: raise SystemExit(f'runtime/snapshot invariant missing: {token}')

guidance = (JAVA / 'settlement/SettlementGuidanceService.java').read_text(encoding='utf-8')
for token in ('data.construction().active()', 'data.roadConstruction().active()', 'data.outpostConstruction().active()',
              'data.houseCount() < 1', 'data.lumberCampCount() < 1', 'BuildingType.FARM', 'BuildingType.QUARRY',
              'data.roads().isEmpty()', 'data.outposts().isEmpty()', 'data.population() < 4', 'BuildingType.MINE',
              'data.outposts().size() < 2', 'data.population() < 8', 'BuildingType.BLACKSMITH',
              'data.outposts().size() < 4', 'data.population() < 16', 'BuildingType.GUARD_POST',
              'data.housingCapacity() <= data.population()', 'data.resources().food() < 8L'):
    if token not in guidance: raise SystemExit(f'next-goal invariant missing: {token}')
if 'reward' in guidance.lower() or 'quest' in guidance.lower():
    raise SystemExit('next-goal helper must remain guidance-only, not become a quest/reward system')

routine = (JAVA / 'settlement/SettlementResidentRoutineService.java').read_text(encoding='utf-8')
for token in ('level.dimensionType().defaultClock()', 'level.clockManager().getTotalTicks(defaultClock.get())',
              'time >= 13000L && time < 23000L', 'BuildingType.HOUSE'):
    if token not in routine: raise SystemExit(f'resident routine invariant missing: {token}')

snapshot = (JAVA / 'network/SettlementSnapshotPayload.java').read_text(encoding='utf-8')
for token in ('int buildingUnlockMask, String nextGoal', 'buf.writeUtf(payload.nextGoal())', 'buf.readUtf()'):
    if token not in snapshot: raise SystemExit(f'guidance snapshot invariant missing: {token}')
network = (JAVA / 'network/SettlementNetwork.java').read_text(encoding='utf-8')
if 'PROTOCOL = "7"' not in network: raise SystemExit('alpha.22 snapshot shape requires network protocol 7')

placement = (JAVA / 'client/BuildingPlacementClient.java').read_text(encoding='utf-8')
for token in ('GLFW.GLFW_KEY_B', 'GLFW.GLFW_KEY_R', 'GLFW.GLFW_KEY_ENTER', 'GLFW.GLFW_KEY_BACKSPACE'):
    if token not in placement: raise SystemExit(f'unified control invariant missing: {token}')
for forbidden in ('GLFW_KEY_N', 'GLFW_KEY_J', 'GLFW_KEY_K'):
    if forbidden in placement: raise SystemExit(f'legacy fragmented control still registered: {forbidden}')
for client in ('RoadPlacementClient.java', 'OutpostPlacementClient.java'):
    text = (JAVA / 'client' / client).read_text(encoding='utf-8')
    for forbidden in ('KeyMapping', 'RegisterKeyMappingsEvent', 'GLFW_KEY_'):
        if forbidden in text: raise SystemExit(f'{client} owns a separate gameplay key: {forbidden}')

palette = (JAVA / 'client/BuildingPaletteScreen.java').read_text(encoding='utf-8')
for token in ('data.buildingUnlockMask()', 'button.active = unlocked', 'RoadPlacementClient.beginPlacement()',
              'OutpostPlacementClient.beginPlacement()', 'public boolean isPauseScreen()', 'return false;'):
    if token not in palette: raise SystemExit(f'compact palette invariant missing: {token}')

hud = (JAVA / 'client/SettlementHudOverlay.java').read_text(encoding='utf-8')
for token in ('data.nextGoal()', 'y + 18', 'int modeY = y + 37', 'B 팔레트   R 회전   Enter 건설'):
    if token not in hud: raise SystemExit(f'compact guidance HUD invariant missing: {token}')

lang = json.loads((RES / 'assets/frontier_settlement/lang/ko_kr.json').read_text(encoding='utf-8'))
for legacy in ('key.frontier_settlement.next_building', 'key.frontier_settlement.road_mode', 'key.frontier_settlement.outpost_mode'):
    if legacy in lang: raise SystemExit(f'legacy key localization remains: {legacy}')

for path in (JAVA / 'settlement/SettlementService.java', JAVA / 'settlement/SettlementCoreService.java',
             JAVA / 'settlement/SettlementConstructionService.java', JAVA / 'settlement/SettlementRoadService.java',
             JAVA / 'settlement/SettlementOutpostService.java', JAVA / 'settlement/SettlementWorkerService.java',
             JAVA / 'settlement/SettlementOutpostProductionService.java', JAVA / 'settlement/SettlementTierInfrastructureService.java'):
    text = path.read_text(encoding='utf-8')
    if 'destroyBlock(' in text or 'dropResources(' in text: raise SystemExit(f'loose-drop destruction path forbidden: {path.name}')

print('Frontier Settlement alpha.22 source audit: PASS')
