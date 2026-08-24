#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'


def text(path): return path.read_text(encoding='utf-8')
def must(source, tokens, label):
    for token in tokens:
        if token not in source: raise SystemExit(f'{label} missing: {token}')
def forbid(source, tokens, label):
    for token in tokens:
        if token in source: raise SystemExit(f'{label}: {token}')

# Alpha.50 source/API gate. The next canonicalization pass must re-bind the historical Alpha.23-49
# chain after these intentionally changed civil-work limits and imported-fill rules are proven to compile.
state = text(JAVA / 'settlement/CivilWorkState.java')
data = text(JAVA / 'settlement/SettlementCivilWorkData.java')
service = text(JAVA / 'settlement/SettlementCivilWorkService.java')
supply = text(JAVA / 'settlement/SettlementCivilFillSupplyService.java')
client = text(JAVA / 'client/CivilWorkPlacementClient.java')
props = text(ROOT / 'gradle.properties')
lock = text(ROOT / 'COMPANION_LOCK.json')

must(state, ('int earthBank', 'int completedSteps', 'int initialCutBlocks', 'int initialFillBlocks',
             'earthBank + 1', 'Math.max(0, earthBank - 1)'), 'alpha.50 preserves alpha.49 project state')
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
    'level.setBlock(target, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE)',
    'data.replace(project.afterCut())',
    'boolean importedFill = project.earthBank() <= 0',
    'SettlementCivilFillSupplyService.ensureCarriedFill(level, settlement, builder, project)',
    'level.setBlock(target, SettlementCivilFillSupplyService.carriedFillState(builder), BLOCK_UPDATE)',
    'SettlementCivilFillSupplyService.consumeOne(builder)',
    'level.setBlock(target, Blocks.COARSE_DIRT.defaultBlockState(), BLOCK_UPDATE)',
    'data.replace(project.afterFill())',
    'event.setCanceled(true)',
), 'alpha.50 bounded expanded civil work')
forbid(service, ('new ItemStack(', 'forceChunk', 'setChunkForced', 'getChunk(', 'teleportTo(',
                 'destroyBlock(', 'dropResources(', 'new Villager(', 'EntityTypes.VILLAGER'),
       'alpha.50 civil authority safety')

must(supply, (
    'HAUL_BATCH = 16', 'SettlementStorageService.storageAvailable(level, data)',
    'SettlementStorageService.storagePositions(data)', 'isFillStack(stack)',
    'stack.is(Items.DIRT) || stack.is(Items.COARSE_DIRT)',
    'SettlementStorageService.findExtractionTarget(level, data',
    'builder.getNavigation().moveTo(',
    'SettlementStorageService.extract(level, source',
    'Math.min(HAUL_BATCH, remaining)',
    'builder.setItemSlot(EquipmentSlot.MAINHAND, picked)',
    'Block.byItem(carried.getItem())',
    'carried.shrink(1)',
), 'alpha.50 physical imported fill hauling')
forbid(supply, ('level.setBlock(', 'data.updateResources(', 'data.addPopulation(', 'data.setPopulation(',
                'forceChunk', 'setChunkForced', 'getChunk(', 'teleportTo(', 'new ItemStack('),
       'alpha.50 fill supplier cannot become world/resource authority')

place = service.find('level.setBlock(target, SettlementCivilFillSupplyService.carriedFillState(builder), BLOCK_UPDATE)')
consume = service.find('SettlementCivilFillSupplyService.consumeOne(builder)')
advance = service.find('data.replace(project.afterFill())')
if min(place, consume, advance) < 0 or not (place < consume < advance):
    raise SystemExit('alpha.50 imported fill must place real block before consuming carried ItemStack and advancing state')

must(client, ('if (maxX - minX + 1 > 13 || maxZ - minZ + 1 > 13) return List.of()',
              'CivilWorkRequestPayload', 'resetStart()'), 'alpha.50 13x13 client preview')

must(props, ('mod_version=0.1.0-alpha.50',
             'bounded selected-area civil works with physical imported fill hauling'),
     'alpha.50 properties')
must(lock, ('"frontier_settlement": "0.1.0-alpha.50"',
            '13x13 / plus-or-minus-5 envelope',
            'real dirt/coarse-dirt ItemStacks physically hauled by the existing construction worker',
            'no companion class, worldgen lookup, virtual soil balance, or second construction authority',
            '"status": "candidate_runtime_lock"'), 'alpha.50 companion lock')

print('Frontier Settlement alpha.50 source audit: PASS')
