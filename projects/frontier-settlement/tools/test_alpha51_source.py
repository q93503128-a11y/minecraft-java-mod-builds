#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
ALPHA50 = ROOT / 'tools/test_alpha50_source.py'


def text(path): return path.read_text(encoding='utf-8')
def must(source, tokens, label):
    for token in tokens:
        if token not in source: raise SystemExit(f'{label} missing: {token}')
def forbid(source, tokens, label):
    for token in tokens:
        if token in source: raise SystemExit(f'{label}: {token}')

if not ALPHA50.exists(): raise SystemExit('historical Alpha.50 source audit must remain present')
alpha50 = text(ALPHA50)
alpha50 = alpha50.replace("print('Frontier Settlement alpha.23-50 cumulative source audit: PASS')", 'pass')
alpha50 = alpha50.replace('0.1.0-alpha.50', '0.1.0-alpha.51')
alpha50 = alpha50.replace('MAX_WIDTH = 13', 'MAX_WIDTH = 17')
alpha50 = alpha50.replace('MAX_DEPTH = 13', 'MAX_DEPTH = 17')
alpha50 = alpha50.replace('MAX_CUT_DEPTH = 5', 'MAX_CUT_DEPTH = 7')
alpha50 = alpha50.replace('MAX_FILL_DEPTH = 5', 'MAX_FILL_DEPTH = 7')
alpha50 = alpha50.replace('MAX_PLAYER_DISTANCE = 36', 'MAX_PLAYER_DISTANCE = 44')
alpha50 = alpha50.replace('MAX_SETTLEMENT_RADIUS = 96', 'MAX_SETTLEMENT_RADIUS = 112')
alpha50 = alpha50.replace('> 13 || maxZ - minZ + 1 > 13', '> 17 || maxZ - minZ + 1 > 17')
alpha50 = alpha50.replace('최대 13×13 / 절토·성토 ±5', '최대 17×17 / 절토·성토 ±7')
namespace = {'__file__': str(ALPHA50), '__name__': '__main__'}
exec(compile(alpha50, str(ALPHA50), 'exec'), namespace, namespace)

state = text(JAVA / 'settlement/CivilWorkState.java')
service = text(JAVA / 'settlement/SettlementCivilWorkService.java')
retaining = text(JAVA / 'settlement/SettlementCivilRetainingService.java')
client = text(JAVA / 'client/CivilWorkPlacementClient.java')
context = text(JAVA / 'settlement/SettlementContextService.java')
commands = text(JAVA / 'command/SettlementCommands.java')
props = text(ROOT / 'gradle.properties')
lock = text(ROOT / 'COMPANION_LOCK.json')

must(state, ('PHASE_CUT = 0', 'PHASE_FILL = 1', 'PHASE_RETURN = 2', 'PHASE_RETAIN = 3',
             'int initialRetainingBlocks', 'optionalFieldOf("initial_retaining_blocks", 0)',
             'beginRetaining()', 'afterRetaining()', 'initialCutBlocks + initialRetainingBlocks + initialFillBlocks'),
     'alpha.51 persisted retaining phase')

must(retaining, ('MIN_RETAINING_HEIGHT = 3', 'MAX_RETAINING_HEIGHT = 7', 'HAUL_BATCH = 16',
                 'checkPlan(ServerLevel level', 'SettlementCivilWorkService.isNaturalGround(state)',
                 'SettlementStorageService.storageAvailable(level, data)', 'stack.is(Items.COBBLESTONE)',
                 'SettlementStorageService.findExtractionTarget(level, data',
                 'SettlementStorageService.extract(level, source', 'Math.min(HAUL_BATCH, remaining)',
                 'builder.setItemSlot(EquipmentSlot.MAINHAND, picked)', 'carried.shrink(1)'),
     'alpha.51 exact physical retaining material')
forbid(retaining, ('level.setBlock(', 'SettlementInventory::isStone', 'data.updateResources(',
                   'forceChunk', 'setChunkForced', 'getChunk(', 'teleportTo(', 'destroyBlock(',
                   'dropResources(', 'new ItemStack('),
       'alpha.51 retaining supplier cannot become world/resource authority')

must(service, ('MAX_WIDTH = 17', 'MAX_DEPTH = 17', 'MAX_CUT_DEPTH = 7', 'MAX_FILL_DEPTH = 7',
               'MAX_PLAYER_DISTANCE = 44', 'MAX_SETTLEMENT_RADIUS = 112',
               'overlapsInfrastructure(settlement, minX - 1, maxX + 1, minZ - 1, maxZ + 1)',
               'SettlementCivilRetainingService.checkPlan(', 'int retaining = retainingPlan.requiredBlocks()',
               'SettlementCivilRetainingService.availableRetaining(level, settlement)',
               'check.retainingBlocks()', 'project.beginRetaining()',
               'project.phase() == CivilWorkState.PHASE_RETAIN',
               'SettlementCivilRetainingService.ensureCarriedRetaining(level, settlement, builder, project)',
               'level.setBlock(retainingTarget, Blocks.COBBLESTONE.defaultBlockState(), BLOCK_UPDATE)',
               'SettlementCivilRetainingService.consumeOne(builder)', 'data.replace(project.afterRetaining())',
               '"선택영역 테라스 옹벽 시공"',
               'state.minX() - 1', 'SettlementCivilRetainingService.MAX_RETAINING_HEIGHT'),
     'alpha.51 retaining civil authority')
place = service.find('level.setBlock(retainingTarget, Blocks.COBBLESTONE.defaultBlockState(), BLOCK_UPDATE)')
consume = service.find('SettlementCivilRetainingService.consumeOne(builder)')
advance = service.find('data.replace(project.afterRetaining())')
if min(place, consume, advance) < 0 or not (place < consume < advance):
    raise SystemExit('alpha.51 retaining must place successfully before exact ItemStack consume/state advance')
forbid(service, ('forceChunk', 'setChunkForced', 'getChunk(', 'teleportTo(', 'destroyBlock(', 'dropResources('),
       'alpha.51 civil safety')

must(client, ('> 17 || maxZ - minZ + 1 > 17',), 'alpha.51 17x17 ghost limit')
must(context, ('civil.initialRetainingBlocks()', 'civil.remainingRetainingBlocks()',
               'SettlementCivilRetainingService.availableRetaining(level, data)', '창고 조약돌 '),
     'alpha.51 compact retaining context')
must(commands, ('최대 17×17 / 절토·성토 ±7', '3블록+ 노출 가장자리 실제 조약돌 옹벽',
                'SettlementCivilRetainingService.availableRetaining(server.overworld(),data)', '창고 조약돌'),
     'alpha.51 compact retaining status')
must(props, ('mod_version=0.1.0-alpha.51', 'retaining-heavy 17x17 terraces with exact cobblestone hauling'),
     'alpha.51 build properties')
must(lock, ('"frontier_settlement": "0.1.0-alpha.51"',
            'Alpha.51 expands the same civil tool to 17x17 / plus-or-minus-7',
            'exact COBBLESTONE ItemStacks are physically hauled and consumed only after successful wall placement',
            'seven-block retaining-height ceiling', '"status": "candidate_runtime_lock"'),
     'alpha.51 companion lock')

building_type = text(JAVA / 'settlement/BuildingType.java')
enum_block = building_type.split('public enum BuildingType {', 1)[1].split(';', 1)[0]
actual_families = [line.strip().split('(', 1)[0] for line in enum_block.splitlines() if '(' in line]
expected_families = ['HOUSE', 'LUMBER_CAMP', 'FARM', 'QUARRY', 'MINE', 'WAREHOUSE',
                     'CONSTRUCTION_OFFICE', 'BLACKSMITH', 'WORKSHOP', 'ADVANCED_WORKSHOP',
                     'GUARD_POST', 'WATCHTOWER', 'BARRACKS', 'MARKET', 'CART_STATION']
if actual_families != expected_families:
    raise SystemExit(f'alpha.51 expected exact 15 functional building families, got: {actual_families}')
if False and sum(building_type.count(token) for token in ('HOUSE(', 'LUMBER_CAMP(', 'FARM(', 'QUARRY(', 'MINE(', 'WAREHOUSE(',
    'CONSTRUCTION_OFFICE(', 'BLACKSMITH(', 'WORKSHOP(', 'ADVANCED_WORKSHOP(', 'GUARD_POST(', 'WATCHTOWER(',
    'BARRACKS(', 'MARKET(', 'CART_STATION(')) != 15:
    raise SystemExit('alpha.51 must preserve exactly 15 functional building families')
forbid(building_type, ('CIVIL_WORK(', 'TERRAFORM(', 'RETAINING('), 'alpha.51 no fake building family')

print('Frontier Settlement alpha.23-51 cumulative source audit: PASS')
