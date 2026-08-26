#!/usr/bin/env python3
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
A74 = ROOT / 'tools/test_alpha74_source.py'
_real_read = Path.read_text

def legacy_read(self, *args, **kwargs):
    s = _real_read(self, *args, **kwargs)
    if self.name == 'gradle.properties':
        s = s.replace('mod_version=0.1.0-alpha.75', 'mod_version=0.1.0-alpha.74')
    return s

Path.read_text = legacy_read
try:
    a = _real_read(A74, encoding='utf-8').replace("print('Frontier Settlement alpha.23-74 cumulative source audit: PASS')", 'pass')
    ns = {'__file__': str(A74), '__name__': '__main__'}
    exec(compile(a, str(A74), 'exec'), ns, ns)
finally:
    Path.read_text = _real_read

def text(p):
    return Path(p).read_text(encoding='utf-8')

def must(s, tokens, label):
    for token in tokens:
        if token not in s:
            raise SystemExit(f'{label} missing: {token}')

sett = JAVA / 'settlement'
benefit = text(sett / 'SettlementExplorationBenefitService.java')
props = text(ROOT / 'gradle.properties')
project = text(ROOT / 'PROJECT.md')

must(benefit, (
    'MAX_STRUCTURE_ARCHETYPE_LEVEL = 2',
    'fortifiedKnowledge(SettlementData data)',
    'tradeKnowledge(SettlementData data)',
    'industrialKnowledge(SettlementData data)',
    'relicKnowledge(SettlementData data)',
    'StructureArchetype classifyStructure(String rawId)',
    'BARRACKS_FORTIFIED_RADIUS_BONUS_PER_LEVEL = 2.0D',
    'OUTPOST_STONE_DISCOUNT_PER_FORTIFIED = 1L',
    'MARKET_EMERALD_BONUS_PER_TRADE = 1',
    'REPAIR_BONUS_PER_INDUSTRIAL = 12',
    'FORGE_POWER_BONUS_PER_RELIC = 2'
), 'alpha.75 structure archetype benefits')
must(props, ('mod_version=0.1.0-alpha.75', 'soft external-structure archetype knowledge'), 'alpha.75 props')
must(project, (
    'NPC event/dialogue/social breadth is intentionally bounded',
    'External structures should not all collapse into one generic survey reward'
), 'alpha.75 current direction')
for forbidden in ('repurposed_structures.', 'dungeonsandtaverns.', 'com.telepathicgrunt'):
    if forbidden in benefit:
        raise SystemExit(f'alpha.75 hard companion dependency: {forbidden}')
print('Frontier Settlement alpha.23-75 cumulative source audit: PASS')
