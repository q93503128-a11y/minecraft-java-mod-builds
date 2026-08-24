#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
ALPHA41 = ROOT / 'tools/test_alpha41_source.py'


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


# Preserve every Alpha.23-41 invariant. Adapt only the canonical version expectation and suppress
# the Alpha.41 PASS banner before adding the bounded unloaded-work contracts below.
alpha41_source = text(ALPHA41)
alpha41_source = alpha41_source.replace("print('Frontier Settlement alpha.41 source audit: PASS')", 'pass')
alpha41_source = alpha41_source.replace('0.1.0-alpha.41', '0.1.0-alpha.42')
namespace = {'__file__': str(ALPHA41), '__name__': '__main__'}
exec(compile(alpha41_source, str(ALPHA41), 'exec'), namespace, namespace)

required = [
    JAVA / 'settlement/OutpostDeferredState.java',
    JAVA / 'settlement/SettlementDeferredOutpostData.java',
    JAVA / 'settlement/SettlementDeferredOutpostService.java',
]
missing = [str(path.relative_to(ROOT)) for path in required if not path.is_file()]
if missing:
    raise SystemExit('alpha.42 missing required files: ' + ', '.join(missing))

state = text(JAVA / 'settlement/OutpostDeferredState.java')
must(state, (
    'long productionTicks', 'long logisticsTicks', 'String observedOverlay', 'boolean transportObserved',
    'Codec.LONG.optionalFieldOf("production_ticks", 0L)',
    'Codec.LONG.optionalFieldOf("logistics_ticks", 0L)',
    'Codec.STRING.optionalFieldOf("observed_overlay", "general")',
    'Codec.BOOL.optionalFieldOf("transport_observed", false)',
), 'alpha.42 persisted deferred state')

deferred_data = text(JAVA / 'settlement/SettlementDeferredOutpostData.java')
must(deferred_data, (
    'extends SavedData',
    'Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "outpost_deferred_work")',
    'OutpostDeferredState.CODEC.listOf().optionalFieldOf("states"',
    'server.getDataStorage().computeIfAbsent(TYPE)',
    'OutpostDeferredState stateFor(int outpostId)',
    'void replace(OutpostDeferredState replacement)',
), 'alpha.42 auxiliary SavedData')

deferred = text(JAVA / 'settlement/SettlementDeferredOutpostService.java')
must(deferred, (
    'SAMPLE_TICKS = 200',
    'MAX_PRODUCTION_TICKS = 24_000L',
    'MAX_LOGISTICS_TICKS = 24_000L',
    'LOGISTICS_CREDIT_TICKS = 1_200L',
    'OVERLAY_FISHING = "fishing"',
    'OVERLAY_MILITARY = "military"',
    '!localLoaded && productionEligible(outpost, state)',
    'state.transportObserved() && !routeLoaded',
    'Math.min(MAX_PRODUCTION_TICKS',
    'Math.min(MAX_LOGISTICS_TICKS',
    'hasProductionCredit(MinecraftServer server, OutpostRecord outpost, int workPeriodTicks)',
    'consumeProductionCredit(MinecraftServer server, OutpostRecord outpost, int workPeriodTicks)',
    'adjustedTransportBatch(MinecraftServer server, OutpostRecord outpost, int normalBatch)',
    'return Math.min(64, base * 2)',
    'consumeLogisticsCredit(MinecraftServer server, OutpostRecord outpost)',
    'case "lumber", "quarry", "mining", "agriculture" -> true',
    'case "general" -> OVERLAY_FISHING.equals(state.observedOverlay())',
    'never stores wood, stone, ore, fish, food or cargo as a virtual number',
), 'alpha.42 bounded unloaded work accounting')
forbid(deferred, (
    'new ItemStack', 'Container', 'level.setBlock(', 'forceChunk', 'setChunkForced', 'getChunk(',
    'teleportTo(', 'destroyBlock(', 'dropResources(', 'Items.', 'SettlementStorageService.insert',
    'data.addPopulation(', 'data.setPopulation(',
), 'alpha.42 deferred state must not become item/cargo authority')

production = text(JAVA / 'settlement/SettlementOutpostProductionService.java')
must(production, (
    'SettlementDeferredOutpostService.hasProductionCredit(level.getServer(), outpost, periodTicks)',
    'SettlementDeferredOutpostService.consumeProductionCredit(level.getServer(), outpost, LUMBER_WORK_PERIOD_TICKS)',
    'SettlementDeferredOutpostService.consumeProductionCredit(level.getServer(), outpost, QUARRY_WORK_PERIOD_TICKS)',
    'SettlementDeferredOutpostService.consumeProductionCredit(level.getServer(), outpost, MINING_WORK_PERIOD_TICKS)',
    'SettlementDeferredOutpostService.consumeProductionCredit(level.getServer(), outpost, AGRICULTURE_WORK_PERIOD_TICKS)',
    'harvestVerticalTrunk(level, data, target)',
    'harvestStoneCluster(level, data, target)',
    'mineOre(level, ore)',
    'isMatureWheat(state)',
), 'alpha.42 physical production catch-up')

fishing = text(JAVA / 'settlement/SettlementFishingOutpostService.java')
must(fishing, (
    'SettlementDeferredOutpostService.observeGeneralOverlay(server, outpost,',
    'SettlementDeferredOutpostService.OVERLAY_MILITARY',
    'SettlementDeferredOutpostService.OVERLAY_GENERAL',
    'SettlementDeferredOutpostService.OVERLAY_FISHING',
    'SettlementDeferredOutpostService.hasProductionCredit(level.getServer(), outpost, WORK_PERIOD_TICKS)',
    'SettlementDeferredOutpostService.consumeProductionCredit(level.getServer(), outpost, WORK_PERIOD_TICKS)',
    'isOpenSurfaceWater(level, spot.water())',
), 'alpha.42 fishing catch-up/overlay observation')

logistics = text(JAVA / 'settlement/SettlementOutpostLogisticsService.java')
must(logistics, (
    'SettlementDeferredOutpostService.observeTransportReady(level.getServer(), outpost)',
    'int normalBatch = transportBatchSize(data)',
    'SettlementDeferredOutpostService.adjustedTransportBatch(',
    'ItemStack picked = takeFirstTransportStack(container, outpost, adjustedBatch)',
    'if (picked.getCount() > normalBatch)',
    'SettlementDeferredOutpostService.consumeLogisticsCredit(level.getServer(), outpost)',
    'public static boolean routeFullyLoaded(ServerLevel level, SettlementData data, OutpostRecord outpost)',
    'there is still only one authority for long-distance outpost transport',
), 'alpha.42 physical logistics catch-up')
forbid(logistics, (
    'teleportTo(', 'forceChunk', 'setChunkForced', 'getChunk(',
    'new SettlementOutpostLogisticsService',
), 'alpha.42 logistics remains physical single authority')

service = text(JAVA / 'settlement/SettlementService.java')
if service.count('SettlementDeferredOutpostService.tick(server, data)') != 1:
    raise SystemExit('alpha.42 deferred outpost service must have exactly one server tick call')
must(service, (
    'SettlementOutpostProductionService.tick(server, data);\n            SettlementFishingOutpostService.tick(server, data);\n            SettlementMarketService.tick(server, data);',
    'SettlementWorkshopService.tick(server, data);\n            SettlementDeferredOutpostService.tick(server, data);',
), 'alpha.42 work-hour deferred accounting order')

commands = text(JAVA / 'command/SettlementCommands.java')
must(commands, (
    'SettlementDeferredOutpostService.Snapshot deferred=SettlementDeferredOutpostService.snapshot(server,data)',
    '언로드 보정 | 생산 작업시간 ',
    '가상 자원·가상 화물 0',
), 'alpha.42 compact deferred status')

print('Frontier Settlement alpha.42 source audit: PASS')
