#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
ALPHA40 = ROOT / 'tools/test_alpha40_source.py'


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


# Preserve every Alpha.23-40 invariant. Adapt only the canonical version expectation and suppress
# the Alpha.40 PASS banner before adding the dangerous-region military outpost contracts below.
alpha40_source = text(ALPHA40)
alpha40_source = alpha40_source.replace('0.1.0-alpha.40', '0.1.0-alpha.41')
alpha40_source = alpha40_source.replace("print('Frontier Settlement alpha.40 source audit: PASS')", 'pass')
namespace = {'__file__': str(ALPHA40), '__name__': '__main__'}
exec(compile(alpha40_source, str(ALPHA40), 'exec'), namespace, namespace)

military_path = JAVA / 'settlement/SettlementMilitaryOutpostService.java'
if not military_path.is_file():
    raise SystemExit('alpha.41 missing required file: settlement/SettlementMilitaryOutpostService.java')

military = text(military_path)
must(military, (
    'MILITARY_SENTRY_TAG = "frontier_settlement_military_outpost_sentry"',
    'MILITARY_OUTPOST_TAG_PREFIX',
    'RECRUIT_FOOD_COST = 6L',
    'RECRUIT_METAL_COST = 2L',
    'TARGET_FOOD_RESERVE = 12',
    'TARGET_METAL_RESERVE = 4',
    'record DangerEvidence(boolean loaded, int hostiles, int closeHostiles,',
    'boolean concentratedSwarm = hostiles >= 5 && closeHostiles >= 3',
    'boolean mixedPressure = hostiles >= 4 && closeHostiles >= 2 && threatKinds >= 2',
    'boolean entrenchedDarkness = hostiles >= 3 && closeHostiles >= 1 && enclosedDarkSamples >= 3',
    '"general".equals(outpost.specialization())',
    'List<Monster> threats = level.getEntitiesOfClass(Monster.class',
    'Set<Class<?>> threatClasses = new HashSet<>()',
    'level.canSeeSky(sample)',
    'level.getBrightness(LightLayer.BLOCK, sample)',
    'level.hasChunkAt(',
    'SettlementInventory.countFood(container)',
    'SettlementStorageService::isMetalStack',
    'monster.isAlive() && !(monster instanceof Creeper)',
    'event.getDrops().clear()',
    'standDown(outpost, sentry)',
), 'alpha.41 loaded dangerous-region military outpost')
if ('new IronGolem(EntityTypes.IRON_GOLEM, level)' not in military
        and 'new FrontierSoldierEntity(FrontierContent.FRONTIER_SOLDIER.get(), level)' not in military):
    raise SystemExit('alpha.41 sentry body must remain supplied Iron Golem combat authority or its Frontier subclass')
forbid(military, (
    'forceChunk', 'setChunkForced', 'getChunk(', 'teleportTo(', 'destroyBlock(', 'dropResources(',
    'data.addPopulation(', 'data.setPopulation(', 'SettlementOutpostLogisticsService.tick(',
    'new ItemStack(Items.IRON_INGOT', 'Items.EMERALD',
), 'alpha.41 military outpost safety')

# Recruitment must be funded by the physical outpost container, not the town ledger or a virtual counter.
if military.find('SettlementInventory.countFood(container) < RECRUIT_FOOD_COST') < 0:
    raise SystemExit('alpha.41 sentry recruitment must check physical local food first')
if military.find('consumeLocalSupply(container, RECRUIT_FOOD_COST, RECRUIT_METAL_COST)') < 0:
    raise SystemExit('alpha.41 sentry recruitment must consume physical local food/metal')

logistics = text(JAVA / 'settlement/SettlementOutpostLogisticsService.java')
must(logistics, (
    'MILITARY_SUPPLY_TRIP_TAG = "frontier_settlement_military_supply_trip"',
    'MILITARY_RETURN_TRIP_TAG = "frontier_settlement_military_return_trip"',
    'SettlementMilitaryOutpostService.isActiveMilitaryOutpost(level, outpost)',
    'SettlementMilitaryOutpostService.foodSupplyShortage(level, outpost)',
    'SettlementMilitaryOutpostService.metalSupplyShortage(level, outpost)',
    'SettlementStorageService.storageAvailable(level, data)',
    'SettlementStorageService.findExtractionTarget(level, data, predicate)',
    'SettlementStorageService.extract(level, source, predicate, amount)',
    'SettlementInventory::isFood',
    'SettlementStorageService::isMetalStack',
    'moveAlongRoute(level, worker, route, false)',
    'moveAlongRoute(level, worker, route, true)',
    'SettlementInventory.insert(container, carried)',
    'there is still only one authority for long-distance outpost transport',
), 'alpha.41 reverse military supply on existing road authority')
forbid(logistics, (
    'forceChunk', 'setChunkForced', 'getChunk(', 'teleportTo(',
    'SettlementMilitaryOutpostService.tick(',
), 'alpha.41 logistics authority safety')

fishing = text(JAVA / 'settlement/SettlementFishingOutpostService.java')
must(fishing, (
    'SettlementMilitaryOutpostService.isActiveMilitaryOutpost(level, outpost)',
    'return "위험지역 군사거점"',
), 'alpha.41 military-over-fishing precedence')

service = text(JAVA / 'settlement/SettlementService.java')
if service.count('SettlementMilitaryOutpostService.tick(server, data)') != 1:
    raise SystemExit('alpha.41 military outpost service must have exactly one server tick call')
must(service, (
    'SettlementBarracksService.tick(server, data);\n        SettlementMilitaryOutpostService.tick(server, data);',
), 'alpha.41 independent barracks/outpost military roles')

entry = text(JAVA / 'FrontierSettlement.java')
must(entry, (
    'SettlementBarracksService::onLivingDrops',
    'SettlementMilitaryOutpostService::onLivingDrops',
), 'alpha.41 combat proxy drop protection')

commands = text(JAVA / 'command/SettlementCommands.java')
must(commands, (
    '위험지역 전초 | 활성 ',
    'SettlementMilitaryOutpostService.activeMilitaryOutpostCount(server.overworld(),data)',
    'SettlementMilitaryOutpostService.loadedSentryCount(server.overworld(),data)',
    '군사 전초도 같은 도로 운송자가 역방향 보급',
    '위험지역 군사 역할이 우선',
), 'alpha.41 compact status integration')

print('Frontier Settlement alpha.41 source audit: PASS')
