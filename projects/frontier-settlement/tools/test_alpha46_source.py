#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
ALPHA45 = ROOT / 'tools/test_alpha45_source.py'


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


# Preserve Alpha.23-45 source/runtime invariants while Alpha.46 docs are synchronized after API compile.
alpha45_source = text(ALPHA45)
alpha45_source = alpha45_source.replace("print('Frontier Settlement alpha.45 source audit: PASS')", 'pass')
alpha45_source = alpha45_source.replace('0.1.0-alpha.45', '0.1.0-alpha.46')
alpha45_source = alpha45_source.split("readme = text(ROOT / 'README.md')")[0]
namespace = {'__file__': str(ALPHA45), '__name__': '__main__'}
exec(compile(alpha45_source, str(ALPHA45), 'exec'), namespace, namespace)

state = text(JAVA / 'settlement/WaterfrontState.java')
data = text(JAVA / 'settlement/SettlementWaterfrontData.java')
waterfront = text(JAVA / 'settlement/SettlementWaterfrontService.java')
fishing = text(JAVA / 'settlement/SettlementFishingOutpostService.java')
logistics = text(JAVA / 'settlement/SettlementOutpostLogisticsService.java')
service = text(JAVA / 'settlement/SettlementService.java')
entry = text(JAVA / 'FrontierSettlement.java')
commands = text(JAVA / 'command/SettlementCommands.java')
context = text(JAVA / 'settlement/SettlementContextService.java')

must(state, (
    'public record WaterfrontState',
    'Codec.INT.fieldOf("outpost_id")',
    'Codec.INT.optionalFieldOf("build_step", 0)',
    'withBuildStep(int next)',
), 'alpha.46 persisted waterfront anchor')
must(data, (
    '"waterfront_works"',
    'WaterfrontState.CODEC.listOf()',
    'stateFor(int outpostId)',
    'replace(WaterfrontState replacement)',
), 'alpha.46 separate waterfront SavedData')

must(waterfront, (
    'TRADE_FISH_COST = 16',
    'SettlementFishingOutpostService.hasFishingShoreline(level, outpost)',
    'SettlementFishingOutpostService.ensureAssignedWorker(level, outpost)',
    'SettlementMilitaryOutpostService.isActiveMilitaryOutpost(level, outpost)',
    'SettlementInventory.countWood(container)',
    'SettlementInventory.isWood(carried)',
    'extractWood(container, LOCAL_HAUL_BATCH)',
    'carried.shrink(1)',
    'Blocks.SPRUCE_SLAB.defaultBlockState()',
    'Blocks.BARREL.defaultBlockState()',
    'WATER_TRADER_TAG',
    'countFish(container) < TRADE_FISH_COST',
    'SettlementInventory.insert(container, new ItemStack(Items.EMERALD, 1))',
    'Ordinary outpost stock is never auto-sold',
    'onBreakBlock(BreakBlockEvent event)',
), 'alpha.46 real-wood pier and opt-in physical fish trade')
forbid(waterfront, (
    'destroyBlock(', 'dropResources(', 'setChunkForced', 'forceChunk', 'teleportTo(',
    'SettlementStorageService.extract(', 'SettlementStorageService.consume(',
    'data.updateResources(', 'settlement.updateResources(',
), 'alpha.46 waterfront cannot bypass physical/local authority')

must(fishing, (
    'hasFishingShoreline(ServerLevel level, OutpostRecord outpost)',
    'ensureAssignedWorker(ServerLevel level, OutpostRecord outpost)',
    'SettlementWaterfrontService.isConstructionActive(server, outpost)',
    '어업·수변교역·계류장',
), 'alpha.46 fishing/waterfront role integration')

must(logistics, (
    'WATERFRONT_SUPPLY_TRIP_TAG',
    'WATERFRONT_RETURN_TRIP_TAG',
    'boolean waterfrontSupply = !military && SettlementWaterfrontService.woodSupplyShortage(level, outpost) > 0',
    'loadWaterfrontSupply(level, data, outpost, worker)',
    'deliverWaterfrontSupply(level, outpost, worker, carried)',
    'SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isWood)',
    'SettlementStorageService.extract(level, source, SettlementInventory::isWood, amount)',
), 'alpha.46 same-transporter waterfront reverse supply')
if logistics.count('public static void tick(ServerLevel level, SettlementData data)') != 1:
    raise SystemExit('alpha.46 must not create a second outpost logistics tick authority')

waterfront_tick = service.find('SettlementWaterfrontService.tick(server, data)')
fishing_tick = service.find('SettlementFishingOutpostService.tick(server, data)')
if waterfront_tick < 0 or fishing_tick < 0 or waterfront_tick > fishing_tick:
    raise SystemExit('alpha.46 waterfront construction must run before ordinary fishing work')

must(entry, ('NeoForge.EVENT_BUS.addListener(SettlementWaterfrontService::onBreakBlock)',),
     'alpha.46 waterfront infrastructure protection hook')
must(commands, (
    '완공 계류장 ',
    'SettlementWaterfrontService.TRADE_FISH_COST',
    '일반 stockpile 자동판매 없음',
    '군사·수변 공사 역보급도 같은 도로 운송자',
), 'alpha.46 compact status')
must(context, (
    'SettlementWaterfrontService.tradeCrate(server, outpost)',
    '"수변 교역통 #" + outpost.id()',
    '"대구/연어 " + SettlementWaterfrontService.TRADE_FISH_COST + " → 에메랄드 1 · 전용 투입"',
), 'alpha.46 Jade waterfront context')

props = text(ROOT / 'gradle.properties')
lock = text(ROOT / 'COMPANION_LOCK.json')
must(props, (
    'mod_version=0.1.0-alpha.46',
    'real-wood fishing-outpost piers',
    'opt-in physical fish trade',
), 'alpha.46 build properties')
must(lock, (
    '"frontier_settlement": "0.1.0-alpha.46"',
    'do not add a companion dependency or a second outpost transport authority',
    'historical public WaypointsManager API is absent',
    '"status": "candidate_runtime_lock"',
), 'alpha.46 companion lock')

print('Frontier Settlement alpha.46 source audit: PASS')
