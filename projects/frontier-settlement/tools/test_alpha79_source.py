#!/usr/bin/env python3
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
A78 = ROOT / 'tools/test_alpha78_source.py'
_real_read = Path.read_text

def legacy_read(self, *args, **kwargs):
    s = _real_read(self, *args, **kwargs)
    if self.name == 'gradle.properties':
        s = s.replace('mod_version=0.1.0-alpha.79', 'mod_version=0.1.0-alpha.78')
    elif self.name == 'PROJECT.md':
        s = s.replace('Current implementation delta: **0.1.0-alpha.79**', 'Current implementation delta: **0.1.0-alpha.78**')
    elif self.name == 'CANONICAL_PLAN.md':
        s = s.replace('Current canonical implementation: **0.1.0-alpha.79**.', 'Current canonical implementation: **0.1.0-alpha.78**.')
    elif self.name == 'COMPLETION_GAP_AUDIT.md':
        s = s.replace('현재 구현 기준: `0.1.0-alpha.79`', '현재 구현 기준: `0.1.0-alpha.78`')
    elif self.name == 'README.md':
        s = s.replace('## Current version: 0.1.0-alpha.79', '## Current version: 0.1.0-alpha.78')
    elif self.name == 'COMPANION_LOCK.json':
        s = s.replace('"frontier_settlement": "0.1.0-alpha.79"', '"frontier_settlement": "0.1.0-alpha.78"')
    elif self.name == 'SettlementOutpostService.java':
        new = """        BlockState current = level.getBlockState(placement.pos());
        boolean alreadyPlaced = current.is(placement.state().getBlock());
        if (!alreadyPlaced && !canReplaceForBlueprint(level, placement.pos(), current)) {
"""
        old = """        BlockState current = level.getBlockState(placement.pos());
        if (current.is(placement.state().getBlock())) {
            data.advanceOutpostConstruction();
            return false;
        }
        if (!canReplaceForBlueprint(level, placement.pos(), current)) {
"""
        s = s.replace(new, old)
        extra = """        // A matching player-preplaced block is useful physical work, but it is not free settlement
        // material. New physical outpost projects still consume this blueprint step's exact share.
        // Pre-Alpha.26 legacy prepaid projects never enter this branch and remain migration-safe.
        if (alreadyPlaced) {
            if (requiredNow > 0L && !consumeCarried(builder, predicate, requiredNow)) return false;
            data.advanceOutpostConstruction();
            return false;
        }

"""
        s = s.replace(extra, '')
    return s

Path.read_text = legacy_read
try:
    chain = _real_read(A78, encoding='utf-8').replace("print('Frontier Settlement alpha.23-78 cumulative source audit: PASS')", 'pass')
    ns = {'__file__': str(A78), '__name__': '__main__'}
    exec(compile(chain, str(A78), 'exec'), ns, ns)
finally:
    Path.read_text = _real_read

def text(path): return Path(path).read_text(encoding='utf-8')
def must(src, tokens, label):
    for token in tokens:
        if token not in src: raise SystemExit(f'{label} missing: {token}')

sett = JAVA / 'settlement'
inv = text(sett / 'SettlementInventory.java')
storage = text(sett / 'SettlementStorageService.java')
outpost = text(sett / 'SettlementOutpostService.java')
props = text(ROOT / 'gradle.properties')
project = text(ROOT / 'PROJECT.md')
must(inv, ('RESOURCE_WOOD = 1', 'RESOURCE_STONE = 2', 'RESOURCE_METAL = 4', 'RESOURCE_FOOD = 8',
           'exclusiveResource(stack, RESOURCE_WOOD)', 'exclusiveResource(stack, RESOURCE_STONE)',
           'exclusiveResource(stack, RESOURCE_FOOD)', 'isMetalResource(ItemStack stack)',
           'stack.is(ExternalContentTags.EXPEDITION_RELICS)', 'SettlementExternalContentService.isExternalWeapon(stack)',
           'return mask == expected;'), 'alpha.79 exclusive physical resources')
must(storage, ('if (!SettlementInventory.isMetalResource(stack)) return false;',), 'alpha.79 metal authority gate')
must(outpost, ('boolean alreadyPlaced = current.is(placement.state().getBlock());',
               'if (!alreadyPlaced && !canReplaceForBlueprint', 'if (alreadyPlaced) {',
               'if (requiredNow > 0L && !consumeCarried(builder, predicate, requiredNow)) return false;',
               'Pre-Alpha.26 legacy prepaid projects never enter this branch', 'legacyPrepaidBuilding()'),
     'alpha.79 outpost preplacement charge')
if outpost.index('if (alreadyPlaced) {') <= outpost.index('long requiredNow = 0L;'):
    raise SystemExit('alpha.79 preplaced outpost branch occurs before exact cost calculation')
must(props, ('mod_version=0.1.0-alpha.79', 'pre-playtest manual-audit hardening'), 'alpha.79 props')
must(project, ('Current implementation delta: **0.1.0-alpha.79**', 'Alpha.79 pre-playtest manual-audit hardening'), 'alpha.79 project')
for forbidden in ('forceChunk(', 'setChunkForced(', 'teleportTo('):
    if forbidden in outpost: raise SystemExit(f'alpha.79 forbidden outpost shortcut: {forbidden}')
print('Frontier Settlement alpha.23-79 cumulative source audit: PASS')
