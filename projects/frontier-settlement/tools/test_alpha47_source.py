#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
ALPHA46 = ROOT / 'tools/test_alpha46_source.py'


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


# Preserve every Alpha.23-46 source/runtime invariant while Alpha.47 docs are synchronized only after
# the new 26.2 enchantment path has compiled successfully. Adapt just version expectations, then stop
# before Alpha.46's canonical-document section.
alpha46_source = text(ALPHA46)
alpha46_source = alpha46_source.replace("print('Frontier Settlement alpha.46 source audit: PASS')", 'pass')
alpha46_source = alpha46_source.replace("print('Frontier Settlement alpha.46 canonical docs audit: PASS')", 'pass')
alpha46_source = alpha46_source.replace("'mod_version=0.1.0-alpha.46'", "'mod_version=0.1.0-alpha.47'")
alpha46_source = alpha46_source.replace("'\"frontier_settlement\": \"0.1.0-alpha.46\"'", "'\"frontier_settlement\": \"0.1.0-alpha.47\"'")
alpha46_source = alpha46_source.split('# Canonical documentation is part of the accepted Alpha.46 state.')[0]
namespace = {'__file__': str(ALPHA46), '__name__': '__main__'}
exec(compile(alpha46_source, str(ALPHA46), 'exec'), namespace, namespace)

advanced = text(JAVA / 'settlement/SettlementAdvancedWorkshopService.java')
commands = text(JAVA / 'command/SettlementCommands.java')
props = text(ROOT / 'gradle.properties')
lock = text(ROOT / 'COMPANION_LOCK.json')

must(advanced, (
    'REFORGE_RELIC_COST = 2',
    'REFORGE_METAL_COST = 8',
    'REFORGE_POWER = 40',
    'SettlementTier.DOMAIN',
    'readyReforgeCommissionCount(ServerLevel level, SettlementData data)',
    'findReforgeWeapon(crate)',
    'isReforgeableWeapon(ItemStack stack)',
    '&& !EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty()',
    'EnchantmentHelper.selectEnchantment(level.getRandom(), reforged, REFORGE_POWER',
    '.filter(holder -> existing.getLevel(holder) == 0)',
    '.filter(holder -> EnchantmentHelper.isEnchantmentCompatible(existing.keySet(), holder))',
    'reforged.enchant(addition.enchantment(), addition.level())',
    'if (result.equals(existing)) return false;',
    'if (result.getLevel(holder) < existing.getLevel(holder)) return false;',
    'consumeMatching(crate, SettlementStorageService::isMetalStack, REFORGE_METAL_COST)',
    'consumeMatching(crate, stack -> stack.is(ExternalContentTags.EXPEDITION_RELICS), REFORGE_RELIC_COST)',
    'Math.min(metalRequired - metal, METAL_HAUL_BATCH)',
), 'alpha.47 domain relic reforge')
forbid(advanced, (
    'EnchantmentHelper.setEnchantments(',
    'forceChunk', 'setChunkForced', 'getChunk(', 'teleportTo(', 'destroyBlock(', 'dropResources(',
    'ModList', 'weaponsexpanded.',
), 'alpha.47 reforge safety/soft compatibility')

# Existing enchantments must be preserved before any Alpha.47 commission resource mutation.
select_index = advanced.find('List<EnchantmentInstance> additions = EnchantmentHelper.selectEnchantment')
add_index = advanced.find('reforged.enchant(addition.enchantment(), addition.level())')
result_index = advanced.find('var result = EnchantmentHelper.getEnchantmentsForCrafting(reforged)')
preserve_index = advanced.find('if (result.getLevel(holder) < existing.getLevel(holder)) return false;')
metal_index = advanced.find('consumeMatching(crate, SettlementStorageService::isMetalStack, REFORGE_METAL_COST)')
relic_index = advanced.find('consumeMatching(crate, stack -> stack.is(ExternalContentTags.EXPEDITION_RELICS), REFORGE_RELIC_COST)')
replace_index = advanced.find('crate.setItem(weaponSlot, reforged)')
if min(select_index, add_index, result_index, preserve_index, metal_index, relic_index, replace_index) < 0:
    raise SystemExit('alpha.47 reforge transaction-order evidence missing')
if not (select_index < add_index < result_index < preserve_index < metal_index < relic_index < replace_index):
    raise SystemExit('alpha.47 reforge must validate improvement/preservation before consuming metal/relic')

# Alpha.39's first forge remains a separate unchanged path rather than being replaced by the reforge.
must(advanced, (
    'private static boolean forgeOne(ServerLevel level, Container crate, int weaponSlot, int relicSlot)',
    'ItemStack enchanted = EnchantmentHelper.enchantItem',
    'consumeMatching(crate, SettlementStorageService::isMetalStack, METAL_COST)',
    'relic.shrink(RELIC_COST)',
    '? reforgeOne(level, crate, weaponSlot)',
    ': forgeOne(level, crate, weaponSlot, relicSlot)',
), 'alpha.47 preserves alpha.39 forge path')

must(commands, (
    '고급 제작 | 준비 의뢰',
    'SettlementAdvancedWorkshopService.reforgeUnlocked(data)',
    '영지 재련 준비 ',
    'SettlementAdvancedWorkshopService.readyReforgeCommissionCount(server.overworld(),data)',
    'SettlementAdvancedWorkshopService.REFORGE_RELIC_COST',
    'SettlementAdvancedWorkshopService.REFORGE_METAL_COST',
    'SettlementAdvancedWorkshopService.REFORGE_POWER',
    '영지 재련 잠김',
), 'alpha.47 compact reforge status')

must(props, (
    'mod_version=0.1.0-alpha.47',
    'domain relic reforging for compatible external weapons',
), 'alpha.47 build properties')
must(lock, (
    '"frontier_settlement": "0.1.0-alpha.47"',
    'Alpha.47 domain relic reforging operates only on Frontier-recognized external weapons',
    'does not add a hard Weapons Expanded class/item dependency',
    'historical public WaypointsManager API is absent',
    '"status": "candidate_runtime_lock"',
), 'alpha.47 companion lock')

print('Frontier Settlement alpha.47 source audit: PASS')
