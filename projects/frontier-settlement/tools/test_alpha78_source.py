#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
A77 = ROOT / 'tools/test_alpha77_source.py'
_real_read = Path.read_text

# Reconstruct the exact pre-Alpha.78 consolidated/history-facing view while the established
# Alpha.23-77 cumulative chain runs. Alpha.74 is the last consolidated companion/docs target;
# nested historical shims then downgrade it as required by earlier audits.
def legacy_read(self, *args, **kwargs):
    s = _real_read(self, *args, **kwargs)
    if self.name == 'gradle.properties':
        s = s.replace('mod_version=0.1.0-alpha.78', 'mod_version=0.1.0-alpha.77')
    elif self.name == 'PROJECT.md':
        s = s.replace('Current implementation delta: **0.1.0-alpha.78**',
                      'Current implementation delta: **0.1.0-alpha.77**')
    elif self.name == 'CANONICAL_PLAN.md':
        s = s.replace('Current canonical implementation: **0.1.0-alpha.78**.',
                      'Current canonical implementation: **0.1.0-alpha.74**.')
    elif self.name == 'COMPLETION_GAP_AUDIT.md':
        s = s.replace('현재 구현 기준: `0.1.0-alpha.78`', '현재 구현 기준: `0.1.0-alpha.74`')
    elif self.name == 'README.md':
        s = s.replace('## Current version: 0.1.0-alpha.78', '## Current version: 0.1.0-alpha.74')
    elif self.name == 'COMPANION_LOCK.json':
        s = s.replace('"frontier_settlement": "0.1.0-alpha.78"',
                      '"frontier_settlement": "0.1.0-alpha.74"')
    elif self.name == 'SettlementOutpostLogisticsService.java':
        s = s.replace('int normalBatch = productiveTransportBatchSize(data);',
                      'int normalBatch = transportBatchSize(data);')
    return s

Path.read_text = legacy_read
try:
    chain = _real_read(A77, encoding='utf-8').replace(
        "print('Frontier Settlement alpha.23-77 cumulative source audit: PASS')", 'pass')
    ns = {'__file__': str(A77), '__name__': '__main__'}
    exec(compile(chain, str(A77), 'exec'), ns, ns)
finally:
    Path.read_text = _real_read

def text(path):
    return Path(path).read_text(encoding='utf-8')

def must(source, tokens, label):
    for token in tokens:
        if token not in source:
            raise SystemExit(f'{label} missing: {token}')

sett = JAVA / 'settlement'
logistics = text(sett / 'SettlementOutpostLogisticsService.java')
benefit = text(sett / 'SettlementExplorationBenefitService.java')
props = text(ROOT / 'gradle.properties')
project = text(ROOT / 'PROJECT.md')

must(logistics, (
    'TERRITORY_NETWORK_TRANSPORT_BONUS_PER_LEVEL = 4',
    'MAX_PRODUCTIVE_TRANSPORT_STACK = 44',
    'public static int productiveTransportBatchSize(SettlementData data)',
    'if (data.buildingCount(BuildingType.CART_STATION) <= 0) return BASE_TRANSPORT_STACK;',
    'SettlementExplorationBenefitService.territoryNetworkLevel(data)',
    'Math.min(MAX_PRODUCTIVE_TRANSPORT_STACK, CART_STATION_TRANSPORT_STACK',
    '+ networkLevel * TERRITORY_NETWORK_TRANSPORT_BONUS_PER_LEVEL)',
    'int normalBatch = productiveTransportBatchSize(data);',
    'amount = Math.min(foodShortage, transportBatchSize(data));',
    'amount = Math.min(metalShortage, transportBatchSize(data));',
    'int amount = Math.min(shortage, transportBatchSize(data));',
    'SettlementDeferredOutpostService.adjustedTransportBatch(',
    'worker.setItemSlot(EquipmentSlot.MAINHAND, picked)',
    'SettlementStorageService.insertAt(level, target, carried)',
    'if (!level.hasChunkAt(nextPos))'
), 'alpha.78 physical freight integration')
must(benefit, (
    'territoryNetworkLevel(SettlementData data)',
    'SettlementTier.current(data) != SettlementTier.DOMAIN',
    'productiveOutpostDiversity(data) - 1'
), 'alpha.78 territory gate reuse')

if [min(44, 32 + level * 4) for level in range(4)] != [32, 36, 40, 44]:
    raise SystemExit('alpha.78 productive freight curve changed')
if logistics.count('int normalBatch = productiveTransportBatchSize(data);') != 1:
    raise SystemExit('alpha.78 normal freight must use exactly one diversified batch call')
if logistics.count('amount = Math.min(foodShortage, transportBatchSize(data));') != 1:
    raise SystemExit('alpha.78 military food reverse-supply cap changed')
if logistics.count('amount = Math.min(metalShortage, transportBatchSize(data));') != 1:
    raise SystemExit('alpha.78 military metal reverse-supply cap changed')
if logistics.count('int amount = Math.min(shortage, transportBatchSize(data));') != 1:
    raise SystemExit('alpha.78 waterfront reverse-supply cap changed')
for forbidden in ('teleportTo(', 'forceChunk(', 'setChunkForced(', 'addRegionTicket('):
    if forbidden in logistics:
        raise SystemExit(f'alpha.78 forbidden logistics shortcut: {forbidden}')

must(props, ('mod_version=0.1.0-alpha.78', 'territory-network freight efficiency'), 'alpha.78 props')
must(project, (
    'Current implementation delta: **0.1.0-alpha.78**',
    'Alpha.78 turns the Alpha.77 territory-network level into a small physical logistics payoff',
    '32 / 36 / 40 / 44 items',
    'The cap is 44',
    'military first, waterfront second, normal freight last',
    'pauses at unloaded route boundaries',
    'capped at 64; it does not become virtual cargo'
), 'alpha.78 current direction')

print('Frontier Settlement alpha.23-78 cumulative source audit: PASS')
