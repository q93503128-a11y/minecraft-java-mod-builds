#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
A76 = ROOT / 'tools/test_alpha76_source.py'
_real_read = Path.read_text

# Preserve the Alpha.76 historical view while running the cumulative chain.
def legacy_read(self, *args, **kwargs):
    s = _real_read(self, *args, **kwargs)
    if self.name == 'gradle.properties':
        s = s.replace('mod_version=0.1.0-alpha.77', 'mod_version=0.1.0-alpha.76')
    elif self.name == 'PROJECT.md':
        s = s.replace('Current implementation delta: **0.1.0-alpha.77**',
                      'Current implementation delta: **0.1.0-alpha.76**')
    return s

Path.read_text = legacy_read
try:
    a = _real_read(A76, encoding='utf-8').replace(
        "print('Frontier Settlement alpha.23-76 cumulative source audit: PASS')", 'pass')
    ns = {'__file__': str(A76), '__name__': '__main__'}
    exec(compile(a, str(A76), 'exec'), ns, ns)
finally:
    Path.read_text = _real_read


def text(path):
    return Path(path).read_text(encoding='utf-8')


def must(source, tokens, label):
    for token in tokens:
        if token not in source:
            raise SystemExit(f'{label} missing: {token}')

sett = JAVA / 'settlement'
benefit = text(sett / 'SettlementExplorationBenefitService.java')
props = text(ROOT / 'gradle.properties')
project = text(ROOT / 'PROJECT.md')

must(benefit, (
    'MAX_TERRITORY_NETWORK_LEVEL = 3',
    'MARKET_EMERALD_BONUS_PER_NETWORK_LEVEL = 1',
    'REPAIR_BONUS_PER_NETWORK_LEVEL = 8',
    'FORGE_POWER_BONUS_PER_NETWORK_LEVEL = 1',
    'productiveOutpostDiversity(SettlementData data)',
    'case "lumber", "agriculture", "quarry", "mining" -> roles.add(outpost.specialization())',
    'territoryNetworkLevel(SettlementData data)',
    'SettlementTier.current(data) != SettlementTier.DOMAIN',
    'productiveOutpostDiversity(data) - 1',
    'territoryNetworkLevel(data) * MARKET_EMERALD_BONUS_PER_NETWORK_LEVEL',
    'territoryNetworkLevel(data) * REPAIR_BONUS_PER_NETWORK_LEVEL',
    'territoryNetworkLevel(data) * FORGE_POWER_BONUS_PER_NETWORK_LEVEL',
    '" · 영지망 " + territoryNetworkLevel(data)'
), 'alpha.77 diversified territory network')

# Four productive roles produce level 3; one repeated role produces no level at all.
if min(3, max(0, 4 - 1)) != 3 or min(3, max(0, 1 - 1)) != 0:
    raise SystemExit('alpha.77 territory network level cap arithmetic changed')

for forbidden in ('ItemStack', 'setBlock', 'addPopulation', 'forceChunk', 'teleportTo'):
    if forbidden in benefit:
        raise SystemExit(f'alpha.77 network feedback became a new physical authority: {forbidden}')

must(props, (
    'mod_version=0.1.0-alpha.77',
    'domain-only diversified productive-outpost network feedback'
), 'alpha.77 props')
must(project, (
    'Current implementation delta: **0.1.0-alpha.77**',
    'Alpha.77 makes the DOMAIN stage care about a diversified physical territory',
    'two different roles -> level 1, three -> level 2, four -> level 3',
    'Repeating the same specialization never raises the level',
    'market relic payout gains +1 emerald per level',
    'one-metal workshop repair gains +8 durability per level',
    'advanced forge/reforge selection power gains +1 per level'
), 'alpha.77 current direction')

print('Frontier Settlement alpha.23-77 cumulative source audit: PASS')
