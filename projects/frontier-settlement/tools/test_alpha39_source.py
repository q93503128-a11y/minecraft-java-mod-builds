#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
ALPHA38 = ROOT / 'tools/test_alpha38_source.py'


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


# Preserve every Alpha.23-38 invariant. Adapt only the canonical version expectation and suppress
# the Alpha.38 PASS banner before adding the Alpha.39 high-tier crafting contracts below.
alpha38_source = text(ALPHA38)
alpha38_source = alpha38_source.replace('0.1.0-alpha.38', '0.1.0-alpha.39')
alpha38_source = alpha38_source.replace("print('Frontier Settlement alpha.38 source audit: PASS')", 'pass')
namespace = {'__file__': str(ALPHA38), '__name__': '__main__'}
exec(compile(alpha38_source, str(ALPHA38), 'exec'), namespace, namespace)

required = [
    JAVA / 'settlement/AdvancedWorkshopLayout.java',
    JAVA / 'settlement/AdvancedWorkshopBuildingBlueprint.java',
    JAVA / 'settlement/SettlementAdvancedWorkshopService.java',
]
missing = [str(path.relative_to(ROOT)) for path in required if not path.is_file()]
if missing:
    raise SystemExit('alpha.39 missing required files: ' + ', '.join(missing))

building_type = text(JAVA / 'settlement/BuildingType.java')
must(building_type, (
    'ADVANCED_WORKSHOP("advanced_workshop", "고급 제작소", 168, 120, 15, 11, 14, 0',
), 'alpha.39 advanced workshop definition')

blueprints = text(JAVA / 'settlement/BuildingBlueprints.java')
must(blueprints, (
    'case ADVANCED_WORKSHOP -> AdvancedWorkshopBuildingBlueprint.create(origin);',
), 'alpha.39 advanced workshop blueprint routing')

data = text(JAVA / 'settlement/SettlementData.java')
must(data, ('WORKSHOP, ADVANCED_WORKSHOP,',), 'alpha.39 advanced workshop completion persistence')

layout = text(JAVA / 'settlement/AdvancedWorkshopLayout.java')
must(layout, ('commissionCrate(BuildingRecord workshop)', 'artisanHome(BuildingRecord workshop)',
              'workshop.localToWorld'), 'alpha.39 rotation-aware commission layout')

blueprint = text(JAVA / 'settlement/AdvancedWorkshopBuildingBlueprint.java')
must(blueprint, ('Blocks.BARREL.defaultBlockState()', 'Blocks.SMITHING_TABLE.defaultBlockState()',
                 'Blocks.ANVIL.defaultBlockState()', 'Blocks.ENCHANTING_TABLE.defaultBlockState()',
                 'Blocks.GRINDSTONE.defaultBlockState()', 'Blocks.BLAST_FURNACE.defaultBlockState()',
                 'BuildingBlueprints.Phase.ROOF', 'BuildingBlueprints.Phase.FINISH'),
     'alpha.39 physical advanced workshop blueprint')

advanced = text(JAVA / 'settlement/SettlementAdvancedWorkshopService.java')
must(advanced, (
    'ADVANCED_WORKER_TAG = "frontier_settlement_advanced_workshop_worker"',
    'ADVANCED_ASSIGNMENT_PREFIX',
    'RELIC_COST = 1', 'METAL_COST = 4', 'ENCHANTMENT_POWER = 30',
    'SettlementTier.FRONTIER_TOWN', 'BuildingType.WORKSHOP', 'BuildingType.MARKET',
    'AdvancedWorkshopLayout.commissionCrate(workshop)',
    'SettlementExternalContentService.isExternalWeapon(stack)',
    'ExternalContentTags.EXPEDITION_RELICS',
    'EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty()',
    'EnchantmentHelper.enchantItem(', 'EnchantmentTags.IN_ENCHANTING_TABLE',
    '.<Holder<Enchantment>>map(holder -> holder)', '.filter(forged::supportsEnchantment)',
    'enchanted.setDamageValue(0)',
    'SettlementStorageService.findExtractionTarget(level, data, SettlementStorageService::isMetalStack)',
    'SettlementStorageService.extract(level, source, SettlementStorageService::isMetalStack',
    'EquipmentSlot.MAINHAND', 'level.hasChunkAt(',
    'SettlementResidentRoutineService.isRestTime(level)',
    'BreakBlockEvent',
), 'alpha.39 physical rare-material forging service')
forbid(advanced, (
    'forceChunk', 'setChunkForced', 'getChunk(', 'destroyBlock(', 'dropResources(',
    'data.addPopulation(', 'data.setPopulation(', 'ModList', 'weaponsexpanded.',
), 'alpha.39 advanced workshop safety/soft-compatibility')

# Failure safety: a valid enchantment result must exist before any commission resource is mutated.
enchant_index = advanced.find('ItemStack enchanted = EnchantmentHelper.enchantItem')
validated_index = advanced.find('EnchantmentHelper.getEnchantmentsForCrafting(enchanted).isEmpty()')
metal_consume_index = advanced.find('consumeMatching(crate, SettlementStorageService::isMetalStack, METAL_COST)')
relic_consume_index = advanced.find('relic.shrink(RELIC_COST)')
if min(enchant_index, validated_index, metal_consume_index, relic_consume_index) < 0:
    raise SystemExit('alpha.39 commission mutation-order evidence missing')
if not (enchant_index < validated_index < metal_consume_index < relic_consume_index):
    raise SystemExit('alpha.39 commission must validate enchant output before consuming metal/relic')

# The commission barrel is explicit opt-in infrastructure, not generic shared storage. Shared storage
# may supply only metal; it must never cause automatic relic/weapon consumption.
storage = text(JAVA / 'settlement/SettlementStorageService.java')
forbid(storage, ('BuildingType.ADVANCED_WORKSHOP', 'AdvancedWorkshopLayout.commissionCrate'),
       'alpha.39 commission barrel must stay outside generic settlement storage')
forbid(advanced, ('SettlementStorageService.storagePositions(data)',),
       'alpha.39 advanced workshop must not scan shared storage for commissions')

service = text(JAVA / 'settlement/SettlementService.java')
if service.count('SettlementAdvancedWorkshopService.tick(server, data)') != 1:
    raise SystemExit('alpha.39 advanced workshop service must have exactly one server tick call')
must(service, ('SettlementAdvancedWorkshopService.firstMissingLoadedAssignment(server.overworld(), data)',
               'SettlementAdvancedWorkshopService.spawnAssignedWorker(server.overworld(), missingAdvanced)',
               'type == BuildingType.ADVANCED_WORKSHOP',
               'SettlementAdvancedWorkshopService.lockedReason(data)'),
     'alpha.39 advanced workshop runtime/unlock integration')

entry = text(JAVA / 'FrontierSettlement.java')
must(entry, ('SettlementAdvancedWorkshopService',
             'SettlementAdvancedWorkshopService::onBreakBlock'),
     'alpha.39 advanced workshop event integration')

commands = text(JAVA / 'command/SettlementCommands.java')
must(commands, ('Commands.literal("advanced_workshop")', 'BuildingType.ADVANCED_WORKSHOP',
                 'SettlementAdvancedWorkshopService.lockedReason(data)',
                 '고급 제작 | 준비 의뢰'), 'alpha.39 command/status integration')

network = text(JAVA / 'network/SettlementNetwork.java')
must(network, ('BuildingType.ADVANCED_WORKSHOP',
               'SettlementAdvancedWorkshopService.lockedReason(data)'),
     'alpha.39 network server guard')

palette = text(JAVA / 'client/BuildingPaletteScreen.java')
must(palette, ('BuildingType.ADVANCED_WORKSHOP', '생산·건설'),
     'alpha.39 compact palette integration')

guidance = text(JAVA / 'settlement/SettlementGuidanceService.java')
must(guidance, ('BuildingType.ADVANCED_WORKSHOP', '고급 제작 의뢰'),
     'alpha.39 progression guidance')

print('Frontier Settlement alpha.39 source audit: PASS')
