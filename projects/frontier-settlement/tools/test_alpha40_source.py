#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
ALPHA39 = ROOT / 'tools/test_alpha39_source.py'


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


# Preserve every Alpha.23-39 invariant. Adapt only the version expectation and suppress the prior
# PASS banner before adding the Alpha.40 coast/river specialization contracts below.
alpha39_source = text(ALPHA39)
alpha39_source = alpha39_source.replace('0.1.0-alpha.39', '0.1.0-alpha.40')
alpha39_source = alpha39_source.replace("print('Frontier Settlement alpha.39 source audit: PASS')", 'pass')
namespace = {'__file__': str(ALPHA39), '__name__': '__main__'}
exec(compile(alpha39_source, str(ALPHA39), 'exec'), namespace, namespace)

fishing_path = JAVA / 'settlement/SettlementFishingOutpostService.java'
if not fishing_path.is_file():
    raise SystemExit('alpha.40 missing required file: settlement/SettlementFishingOutpostService.java')

fishing = text(fishing_path)
must(fishing, (
    'FISHING_WORKER_TAG = "frontier_settlement_fishing_outpost_worker"',
    'FISHING_OUTPOST_TAG_PREFIX',
    'WATER_SEARCH_RADIUS = 12',
    'MIN_OPEN_WATER_COLUMNS = 24',
    'WORK_PERIOD_TICKS = 140',
    'MAX_CATCH = 3',
    '"general".equals(outpost.specialization())',
    'findFishingSpot(level, outpost)',
    'Items.FISHING_ROD',
    'EquipmentSlot.OFFHAND',
    'Items.COD', 'Items.SALMON',
    'InteractionHand.OFF_HAND',
    'SettlementInventory.insert(container, carried)',
    'level.hasChunkAt(',
    'isOpenSurfaceWater(',
    'isSafeBank(',
    '어업·수변교역',
), 'alpha.40 physical fishing specialization')
forbid(fishing, (
    'forceChunk', 'setChunkForced', 'getChunk(', 'teleportTo(',
    'Items.EMERALD', 'data.addPopulation(', 'data.setPopulation(',
    'SettlementOutpostLogisticsService.tick(',
), 'alpha.40 fishing specialization safety')

service = text(JAVA / 'settlement/SettlementService.java')
if service.count('SettlementFishingOutpostService.tick(server, data)') != 1:
    raise SystemExit('alpha.40 fishing outpost service must have exactly one server tick call')
must(service, (
    'SettlementOutpostProductionService.tick(server, data);\n            SettlementFishingOutpostService.tick(server, data);\n            SettlementMarketService.tick(server, data);',
), 'alpha.40 daytime production integration')

production = text(JAVA / 'settlement/SettlementOutpostProductionService.java')
must(production, ('"general".equals(outpost.specialization()) || !outpostLoaded(level, outpost)',),
     'alpha.40 general outposts must stay out of legacy specialization worker path')

logistics = text(JAVA / 'settlement/SettlementOutpostLogisticsService.java')
must(logistics, (
    'default -> SettlementInventory.isWood(stack) || SettlementInventory.isStone(stack)',
    '|| SettlementInventory.isFood(stack) || isMiningCargo(stack);',
), 'alpha.40 fishing cargo must reuse existing outpost transport authority')

commands = text(JAVA / 'command/SettlementCommands.java')
must(commands, (
    '수변 전초 | 로드된 어업·수변교역',
    'SettlementFishingOutpostService.activeFishingOutpostCount(server.overworld(),data)',
    'SettlementFishingOutpostService.specializationDisplayName(server.overworld(),last)',
), 'alpha.40 outpost status integration')

print('Frontier Settlement alpha.40 source audit: PASS')
