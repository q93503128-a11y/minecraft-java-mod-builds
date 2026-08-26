#!/usr/bin/env python3
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
A75 = ROOT / 'tools/test_alpha75_source.py'
_real_read = Path.read_text

def legacy_read(self, *args, **kwargs):
    s = _real_read(self, *args, **kwargs)
    if self.name == 'gradle.properties':
        s = s.replace('mod_version=0.1.0-alpha.76', 'mod_version=0.1.0-alpha.75')
    return s

Path.read_text = legacy_read
try:
    a = _real_read(A75, encoding='utf-8').replace("print('Frontier Settlement alpha.23-75 cumulative source audit: PASS')", 'pass')
    ns = {'__file__': str(A75), '__name__': '__main__'}
    exec(compile(a, str(A75), 'exec'), ns, ns)
finally:
    Path.read_text = _real_read

def text(p):
    return Path(p).read_text(encoding='utf-8')

def must(s, tokens, label):
    for token in tokens:
        if token not in s:
            raise SystemExit(f'{label} missing: {token}')

sett = JAVA / 'settlement'
biome = text(sett / 'SettlementOutpostBiomeService.java')
outpost = text(sett / 'SettlementOutpostService.java')
props = text(ROOT / 'gradle.properties')
project = text(ROOT / 'PROJECT.md')

must(biome, (
    'TRADE_FIELD_BONUS_PER_LEVEL = 8',
    'INDUSTRIAL_STONE_BONUS_PER_LEVEL = 3',
    'SettlementData data = SettlementData.get(level.getServer())',
    'SettlementExplorationBenefitService.tradeKnowledge(data)',
    'SettlementExplorationBenefitService.industrialKnowledge(data)',
    'field += tradeKnowledge * TRADE_FIELD_BONUS_PER_LEVEL',
    'stone += industrialKnowledge * INDUSTRIAL_STONE_BONUS_PER_LEVEL'
), 'alpha.76 bounded outpost knowledge bias')
must(outpost, (
    'if (fieldGround >= 120) return "agriculture";',
    'if (exposedStone >= 24) return "quarry";',
    'SettlementOutpostBiomeService.bias(level, center)'
), 'alpha.76 existing specialization thresholds')
# Maximum metadata/biome-only thematic additions intentionally remain below thresholds by themselves.
if 24 + 3 * 8 + 2 * 8 >= 120:
    raise SystemExit('alpha.76 field metadata/biome bias can independently force agriculture')
if 8 + 3 * 2 + 2 * 3 >= 24:
    raise SystemExit('alpha.76 stone metadata/biome bias can independently force quarry')
must(props, (
    'mod_version=0.1.0-alpha.76',
    'bounded trade/industrial structure knowledge that biases agriculture/quarry outpost specialization'
), 'alpha.76 props')
must(project, (
    'Current implementation delta: **0.1.0-alpha.76**',
    'Alpha.76 deepens the exploration -> territory loop',
    'Mining remains untouched so metadata cannot fabricate an ore specialization'
), 'alpha.76 current direction')
for forbidden in ('repurposed_structures.', 'dungeonsandtaverns.', 'terralith.'):
    if forbidden in biome:
        raise SystemExit(f'alpha.76 hard companion dependency: {forbidden}')
print('Frontier Settlement alpha.23-76 cumulative source audit: PASS')
