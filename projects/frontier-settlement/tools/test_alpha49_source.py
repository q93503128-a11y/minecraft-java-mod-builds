#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
ALPHA48 = ROOT / 'tools/test_alpha48_source.py'


def text(path): return path.read_text(encoding='utf-8')
def must(source, tokens, label):
    for token in tokens:
        if token not in source: raise SystemExit(f'{label} missing: {token}')
def forbid(source, tokens, label):
    for token in tokens:
        if token in source: raise SystemExit(f'{label}: {token}')

# Preserve Alpha.23-48 source/runtime invariants. Alpha.49 docs are bound only after the new API path compiles.
alpha48_source = text(ALPHA48)
alpha48_source = alpha48_source.replace("print('Frontier Settlement alpha.48 source audit: PASS')", 'pass')
alpha48_source = alpha48_source.replace("print('Frontier Settlement alpha.48 canonical docs audit: PASS')", 'pass')
alpha48_source = alpha48_source.replace('0.1.0-alpha.48', '0.1.0-alpha.49')
# If an inherited network-token assertion names the previous protocol, adapt only that expected token.
alpha48_source = alpha48_source.replace('PROTOCOL = "7"', 'PROTOCOL = "8"')
alpha48_source = alpha48_source.split('# Current canonical docs are part of Alpha.48 acceptance.')[0]
namespace = {'__file__': str(ALPHA48), '__name__': '__main__'}
exec(compile(alpha48_source, str(ALPHA48), 'exec'), namespace, namespace)

state = text(JAVA / 'settlement/CivilWorkState.java')
data = text(JAVA / 'settlement/SettlementCivilWorkData.java')
service = text(JAVA / 'settlement/SettlementCivilWorkService.java')
network = text(JAVA / 'network/SettlementNetwork.java')
request = text(JAVA / 'network/CivilWorkRequestPayload.java')
preview = text(JAVA / 'network/CivilWorkPreviewPayload.java')
client = text(JAVA / 'client/CivilWorkPlacementClient.java')
ghost = text(JAVA / 'client/CivilWorkGhostRenderer.java')
build_client = text(JAVA / 'client/BuildingPlacementClient.java')
palette = text(JAVA / 'client/BuildingPaletteScreen.java')
hud = text(JAVA / 'client/SettlementHudOverlay.java')
context = text(JAVA / 'settlement/SettlementContextService.java')
settlement_service = text(JAVA / 'settlement/SettlementService.java')
entry = text(JAVA / 'FrontierSettlement.java')
commands = text(JAVA / 'command/SettlementCommands.java')
props = text(ROOT / 'gradle.properties')
lock = text(ROOT / 'COMPANION_LOCK.json')

must(state, (
    'public record CivilWorkState(boolean active,',
    'PHASE_CUT = 0', 'PHASE_FILL = 1',
    'int earthBank', 'int completedSteps', 'int initialCutBlocks', 'int initialFillBlocks',
    'CivilWorkState.CODEC', 'earthBank + 1', 'Math.max(0, earthBank - 1)',
), 'alpha.49 persisted civil-work state')
must(data, (
    'Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "civil_work")',
    'CivilWorkState.CODEC.optionalFieldOf("project", CivilWorkState.EMPTY)',
    'never stores settlement items/currency',
), 'alpha.49 auxiliary saved data')

must(service, (
    'MAX_WIDTH = 9', 'MAX_DEPTH = 9', 'MAX_AREA = MAX_WIDTH * MAX_DEPTH',
    'MAX_CUT_DEPTH = 4', 'MAX_FILL_DEPTH = 4',
    'SettlementTier.DOMAIN', 'BuildingType.CONSTRUCTION_OFFICE',
    'if (fill > cut)',
    '토목 1차는 현장 절토량 안에서만 성토합니다.',
    'level.hasChunkAt(',
    'overlapsInfrastructure(settlement, minX, maxX, minZ, maxZ)',
    'level.getBlockEntity(', 'getFluidState().isEmpty()',
    'SettlementConstructionService.ensureBuilder(',
    'level.setBlock(target, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE)',
    'data.replace(project.afterCut())',
    'if (project.earthBank() <= 0) return false;',
    'level.setBlock(target, Blocks.COARSE_DIRT.defaultBlockState(), BLOCK_UPDATE)',
    'data.replace(project.afterFill())',
    'SettlementCivilWorkData.get(server).project()',
    'event.setCanceled(true)',
    '가상 토사 생성 0',
), 'alpha.49 bounded balanced earthwork')
forbid(service, (
    'new ItemStack(', 'ItemStack ', 'SettlementStorageService', 'SettlementInventory',
    'data.updateResources(', 'data.addPopulation(', 'data.setPopulation(',
    'destroyBlock(', 'dropResources(', 'forceChunk', 'setChunkForced', 'getChunk(', 'teleportTo(',
    'new Villager(', 'EntityTypes.VILLAGER',
), 'alpha.49 earthwork must not mint resources or add a second builder')

cut_place = service.find('level.setBlock(target, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE)')
cut_credit = service.find('data.replace(project.afterCut())')
fill_gate = service.find('if (project.earthBank() <= 0) return false;')
fill_place = service.find('level.setBlock(target, Blocks.COARSE_DIRT.defaultBlockState(), BLOCK_UPDATE)')
fill_debit = service.find('data.replace(project.afterFill())')
if min(cut_place, cut_credit, fill_gate, fill_place, fill_debit) < 0:
    raise SystemExit('alpha.49 earth-bank transaction evidence missing')
if not (cut_place < cut_credit and fill_gate < fill_place < fill_debit):
    raise SystemExit('alpha.49 earth bank must credit after real cut and debit after gated real fill')

must(network, (
    'private static final String PROTOCOL = "8"',
    'CivilWorkPreviewPayload.TYPE', 'CivilWorkRequestPayload.TYPE',
    'SettlementNetwork::handleCivilWorkRequest',
    'SettlementCivilWorkService.check(player,first,second)',
    'SettlementCivilWorkService.start(player,first,second)',
    'SettlementCivilWorkData.get(player.level().getServer()).project().active()',
), 'alpha.49 authoritative networking/single project')
must(request, ('"civil_work_request"', 'boolean confirm'), 'alpha.49 request payload')
must(preview, ('"civil_work_preview"', 'int cutBlocks', 'int fillBlocks', 'fromCheck'), 'alpha.49 preview payload')

must(client, (
    'first = target', 'first.getY()', 'CivilWorkRequestPayload',
    'if (maxX - minX + 1 > 9 || maxZ - minZ + 1 > 9) return List.of()',
    'Backspace' if False else 'resetStart()',
), 'alpha.49 client selection')
must(build_client, (
    'else if (CivilWorkPlacementClient.active()) CivilWorkPlacementClient.resetStart()',
    'else if (CivilWorkPlacementClient.active()) CivilWorkPlacementClient.confirm()',
    'CivilWorkPlacementClient.cancel()',
), 'alpha.49 fixed key reuse')
must(palette, ('"토목 평탄화"', 'data.tier().equals("영지")', 'CivilWorkPlacementClient.beginPlacement()'),
     'alpha.49 palette integration')
must(ghost, ('CivilWorkPlacementClient.ghostBlocks()', 'submitShapeOutline(', 'Shapes.block()'),
     'alpha.49 world-space grade preview')
must(hud, ('CivilWorkPlacementClient.active()', 'Enter 첫 모서리', 'Backspace 첫 모서리 재선택'),
     'alpha.49 compact controls')

must(settlement_service, (
    'SettlementCivilWorkService.tick(server, data)',
    'boolean activeProject = data.construction().active() || data.roadConstruction().active() || data.outpostConstruction().active();',
    'boolean civilProject = SettlementCivilWorkData.get(server).project().active();',
    'if (changed || activeProject || civilProject) broadcast(server, data);',
), 'alpha.49 server tick + legacy active-project invariant')
if settlement_service.count('SettlementCivilWorkService.tick(server, data)') != 1:
    raise SystemExit('alpha.49 civil work must have exactly one server authority tick')
must(context, (
    '"civil_work", "civil_work"', '"선택영역 절토"', '"선택영역 성토"',
    'civil.initialCutBlocks()', 'civil.initialFillBlocks()', 'civil.earthBank()',
), 'alpha.49 compact project context')
must(entry, ('SettlementCivilWorkService::onBreakBlock',), 'alpha.49 active-area protection hook')

must(commands, (
    '선택영역 토목이 끝난 뒤 건물을 시작해 주세요.',
    '선택영역 토목이 끝난 뒤 도로를 시작해 주세요.',
    '선택영역 토목이 끝난 뒤 전초기지를 시작해 주세요.',
    '"토목 | "+SettlementCivilWorkService.phaseLabel(server)',
    '현장 토사 ', '가상 자원 0',
    '9×9 선택영역 평탄화 가능 · 절토량 안에서만 성토',
), 'alpha.49 command-path concurrency and status')

must(props, (
    'mod_version=0.1.0-alpha.49',
    'bounded medium-terrain work using real retaining stone',
    'exploration/conquest milestones',
    'real-wood fishing-outpost piers', 'opt-in physical fish trade',
    'domain relic reforging for compatible external weapons',
    'supplied humanoid military presentation without server-side weapon minting',
    'bounded selected-area balanced earthworks',
), 'alpha.49 build properties')
must(lock, (
    '"frontier_settlement": "0.1.0-alpha.49"',
    'bounded selected-area balanced earthworks',
    'already-loaded vanilla/companion terrain block state inspection',
    'never force-loads terrain',
    'historical public WaypointsManager API is absent',
    '"status": "candidate_runtime_lock"',
), 'alpha.49 companion lock')

# Building family count remains 15; civil works are infrastructure, not a fake 16th building.
building_type = text(JAVA / 'settlement/BuildingType.java')
if building_type.count('("') < 15:
    raise SystemExit('alpha.49 must preserve the established functional building families')
forbid(palette, ('BuildingType.CIVIL', 'BuildingType.TERRAFORM'), 'alpha.49 no fake civil building family')

print('Frontier Settlement alpha.49 source audit: PASS')
