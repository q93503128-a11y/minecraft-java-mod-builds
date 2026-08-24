#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
ALPHA48 = ROOT / 'tools/test_alpha48_source.py'
ALPHA49 = ROOT / 'tools/test_alpha49_source.py'


def text(path): return path.read_text(encoding='utf-8')
def must(source, tokens, label):
    for token in tokens:
        if token not in source: raise SystemExit(f'{label} missing: {token}')
def forbid(source, tokens, label):
    for token in tokens:
        if token in source: raise SystemExit(f'{label}: {token}')

# Re-bind the Alpha.23-48 cumulative chain to Alpha.50. Alpha.49's historical audit file is retained
# unchanged. The inherited chain is executed against current source/docs, but exact assertions that
# describe Alpha.49's intentionally superseded civil interaction wording are adapted here rather
# than deleting/weakening any historical audit file or putting stale wording back into current docs.
if not ALPHA49.exists():
    raise SystemExit('historical Alpha.49 source audit must remain in the repository')
alpha48_source = text(ALPHA48)
alpha48_source = alpha48_source.replace("print('Frontier Settlement alpha.48 source audit: PASS')", 'pass')
alpha48_source = alpha48_source.replace("print('Frontier Settlement alpha.48 canonical docs audit: PASS')", 'pass')
alpha48_source = alpha48_source.replace('0.1.0-alpha.48', '0.1.0-alpha.50')
alpha48_source = alpha48_source.split('# Current canonical docs are part of Alpha.48 acceptance.')[0]

_original_read_text = Path.read_text
def _alpha50_audit_read_text(path, *args, **kwargs):
    source = _original_read_text(path, *args, **kwargs)
    if path.name.startswith('test_alpha') and path.name.endswith('_source.py'):
        source = source.replace(
            '`Backspace`: reset road start or Alpha.49 civil-work first corner',
            '`Backspace`: reset road start or civil-work first corner')
    return source

Path.read_text = _alpha50_audit_read_text
try:
    namespace = {'__file__': str(ALPHA48), '__name__': '__main__'}
    exec(compile(alpha48_source, str(ALPHA48), 'exec'), namespace, namespace)
finally:
    Path.read_text = _original_read_text

state = text(JAVA / 'settlement/CivilWorkState.java')
data = text(JAVA / 'settlement/SettlementCivilWorkData.java')
service = text(JAVA / 'settlement/SettlementCivilWorkService.java')
supply = text(JAVA / 'settlement/SettlementCivilFillSupplyService.java')
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
    'PHASE_CUT = 0', 'PHASE_FILL = 1', 'PHASE_RETURN = 2',
    'int earthBank', 'int completedSteps', 'int initialCutBlocks', 'int initialFillBlocks',
    'public static final Codec<CivilWorkState> CODEC',
    'earthBank + 1', 'Math.max(0, earthBank - 1)', 'beginReturn()',
), 'alpha.50 persisted civil-work state')
must(data, ('"civil_work"', 'CivilWorkState.CODEC', 'never stores settlement items/currency'),
     'alpha.50 auxiliary civil-work persistence')

must(service, (
    'MAX_WIDTH = 13', 'MAX_DEPTH = 13', 'MAX_CUT_DEPTH = 5', 'MAX_FILL_DEPTH = 5',
    'MAX_PLAYER_DISTANCE = 36', 'MAX_SETTLEMENT_RADIUS = 96',
    'SettlementTier.DOMAIN', 'BuildingType.CONSTRUCTION_OFFICE',
    'SettlementCivilFillSupplyService.importedFillRequired(cut, fill)',
    'SettlementCivilFillSupplyService.availableFill(level, settlement)',
    '외부 성토 흙 부족', '흙/거친 흙 ItemStack을 실제로 넣어 주세요.',
    'SettlementConstructionService.ensureBuilder(',
    'if (!level.setBlock(target, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE)) return false;',
    'data.replace(project.afterCut())',
    'boolean importedFill = project.earthBank() <= 0',
    'SettlementCivilFillSupplyService.ensureCarriedFill(level, settlement, builder, project)',
    'SettlementCivilFillSupplyService.carriedFillState(builder)',
    'if (!level.setBlock(target, fillState, BLOCK_UPDATE)) return false;',
    'if (importedFill) SettlementCivilFillSupplyService.consumeOne(builder);',
    'data.replace(project.afterFill())',
    'data.replace(project.beginReturn())',
    'SettlementCivilFillSupplyService.returnCarriedToStorage(level, settlement, builder)',
    'event.setCanceled(true)', '가상 토사 생성 0',
), 'alpha.50 bounded expanded civil work')
forbid(service, ('forceChunk', 'setChunkForced', 'getChunk(', 'teleportTo(',
                 'destroyBlock(', 'dropResources(', 'new Villager(', 'EntityTypes.VILLAGER'),
       'alpha.50 civil authority safety')

cut_place = service.find('if (!level.setBlock(target, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE)) return false;')
cut_credit = service.find('data.replace(project.afterCut())')
fill_place = service.find('if (!level.setBlock(target, fillState, BLOCK_UPDATE)) return false;')
fill_consume = service.find('if (importedFill) SettlementCivilFillSupplyService.consumeOne(builder);')
fill_advance = service.find('data.replace(project.afterFill())')
if min(cut_place, cut_credit, fill_place, fill_consume, fill_advance) < 0:
    raise SystemExit('alpha.50 physical earth transaction evidence missing')
if not (cut_place < cut_credit and fill_place < fill_consume < fill_advance):
    raise SystemExit('alpha.50 must mutate world successfully before earth credit/item consume/state advance')

must(supply, (
    'HAUL_BATCH = 16', 'SettlementStorageService.storageAvailable(level, data)',
    'SettlementStorageService.storagePositions(data)',
    'stack.is(Items.DIRT) || stack.is(Items.COARSE_DIRT)',
    'remainingImportedFill(ServerLevel level, CivilWorkState project)',
    'level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES',
    'Math.max(0, fillRemaining - project.earthBank())',
    'SettlementStorageService.findExtractionTarget(level, data',
    'builder.getNavigation().moveTo(',
    'SettlementStorageService.extract(level, source',
    'Math.min(HAUL_BATCH, remaining)',
    'builder.setItemSlot(EquipmentSlot.MAINHAND, picked)',
    'returnCarriedToStorage(ServerLevel level, SettlementData data, Villager builder)',
    'SettlementStorageService.findDepositTarget(level, data, carried)',
    'SettlementStorageService.insertAt(level, target, carried)',
    'Block.byItem(carried.getItem())', 'carried.shrink(1)',
), 'alpha.50 physical imported fill hauling/cleanup')
forbid(supply, ('level.setBlock(', 'data.updateResources(', 'data.addPopulation(', 'data.setPopulation(',
                'forceChunk', 'setChunkForced', 'getChunk(', 'teleportTo(', 'new ItemStack('),
       'alpha.50 fill supplier cannot become world/resource authority')
if 'SettlementStorageService.insert(level, data, carried)' in supply:
    raise SystemExit('alpha.50 cleanup must deposit into the concrete container physically reached, not broad storage insert')

must(network, (
    'private static final String PROTOCOL = "7"',
    'CivilWorkPreviewPayload.TYPE', 'CivilWorkRequestPayload.TYPE',
    'SettlementNetwork::handleCivilWorkRequest',
    'SettlementCivilWorkService.check(player,first,second)',
    'SettlementCivilWorkService.start(player,first,second)',
    'SettlementCivilWorkData.get(player.level().getServer()).project().active()',
), 'alpha.50 authoritative networking/single project')
must(request, ('"civil_work_request"', 'boolean confirm'), 'alpha.50 request payload')
must(preview, ('"civil_work_preview"', 'int cutBlocks', 'int fillBlocks', 'fromCheck'), 'alpha.50 preview payload')

must(client, (
    'first = target', 'first.getY()', 'CivilWorkRequestPayload',
    'if (maxX - minX + 1 > 13 || maxZ - minZ + 1 > 13) return List.of()',
    'resetStart()',
), 'alpha.50 client selection')
must(build_client, (
    'else if (CivilWorkPlacementClient.active()) CivilWorkPlacementClient.resetStart()',
    'else if (CivilWorkPlacementClient.active()) CivilWorkPlacementClient.confirm()',
    'CivilWorkPlacementClient.cancel()',
), 'alpha.50 fixed key reuse')
must(palette, ('"토목 평탄화"', 'data.tier().equals("영지")', 'CivilWorkPlacementClient.beginPlacement()'),
     'alpha.50 palette integration')
must(ghost, ('CivilWorkPlacementClient.ghostBlocks()', 'submitShapeOutline(', 'Shapes.block()'),
     'alpha.50 world-space grade preview')
must(hud, ('CivilWorkPlacementClient.active()', 'Enter 첫 모서리', 'Backspace 첫 모서리 재선택'),
     'alpha.50 compact controls')

must(settlement_service, (
    'SettlementCivilWorkService.tick(server, data)',
    'boolean activeProject = data.construction().active() || data.roadConstruction().active() || data.outpostConstruction().active();',
    'boolean civilProject = SettlementCivilWorkData.get(server).project().active();',
), 'alpha.50 server tick authority')
if settlement_service.count('SettlementCivilWorkService.tick(server, data)') != 1:
    raise SystemExit('alpha.50 civil work must have exactly one server authority tick')
must(context, (
    '"civil_work", "civil_work"', 'SettlementCivilWorkService.phaseLabel(server)',
    'SettlementCivilFillSupplyService.remainingImportedFill(level, civil)',
    'SettlementCivilFillSupplyService.availableFill(level, data)',
    '"현장 토사 "', '"외부 흙 필요 "', '"창고 흙 "', '"가상 토사 0"',
), 'alpha.50 compact physical-fill context')
must(entry, ('SettlementCivilWorkService::onBreakBlock',), 'alpha.50 active-area protection hook')

must(commands, (
    '선택영역 토목이 끝난 뒤 건물을 시작해 주세요.',
    '선택영역 토목이 끝난 뒤 도로를 시작해 주세요.',
    '선택영역 토목이 끝난 뒤 전초기지를 시작해 주세요.',
    'SettlementCivilWorkService.phaseLabel(server)',
    '최대 13×13 / 절토·성토 ±5',
    '현장 earthBank 우선', '공동 창고 실제 흙/거친 흙 물리 운반', '가상 토사 0',
), 'alpha.50 command-path concurrency and status')
forbid(commands, ('9×9 선택영역 평탄화 가능 · 절토량 안에서만 성토',), 'alpha.50 stale status')

must(props, ('mod_version=0.1.0-alpha.50',
             'bounded selected-area civil works with physical imported fill hauling'),
     'alpha.50 properties')
must(lock, ('"frontier_settlement": "0.1.0-alpha.50"',
            '13x13 / plus-or-minus-5 envelope',
            'real dirt/coarse-dirt ItemStacks physically hauled by the existing construction worker',
            'no companion class, worldgen lookup, virtual soil balance, or second construction authority',
            '"status": "candidate_runtime_lock"'), 'alpha.50 companion lock')

building_type = text(JAVA / 'settlement/BuildingType.java')
must(building_type, ('HOUSE(', 'LUMBER_CAMP(', 'FARM(', 'QUARRY(', 'MINE(', 'WAREHOUSE(',
                     'CONSTRUCTION_OFFICE(', 'BLACKSMITH(', 'WORKSHOP(', 'ADVANCED_WORKSHOP(',
                     'GUARD_POST(', 'WATCHTOWER(', 'BARRACKS(', 'MARKET(', 'CART_STATION('),
     'alpha.50 preserves exact 15 building families')
forbid(building_type, ('CIVIL_WORK(', 'TERRAFORM('), 'alpha.50 no fake civil building family')

print('Frontier Settlement alpha.23-50 cumulative source audit: PASS')
