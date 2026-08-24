#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
ALPHA44 = ROOT / 'tools/test_alpha44_source.py'


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


# Preserve Alpha.23-44 invariants while advancing only the canonical version.
alpha44_source = text(ALPHA44)
alpha44_source = alpha44_source.replace("print('Frontier Settlement alpha.44 source audit: PASS')", 'pass')
alpha44_source = alpha44_source.replace('0.1.0-alpha.44', '0.1.0-alpha.45')
namespace = {'__file__': str(ALPHA44), '__name__': '__main__'}
exec(compile(alpha44_source, str(ALPHA44), 'exec'), namespace, namespace)

exploration_path = JAVA / 'settlement/SettlementExplorationService.java'
if not exploration_path.is_file():
    raise SystemExit('alpha.45 missing SettlementExplorationService.java')
exploration = text(exploration_path)
must(exploration, (
    'STRUCTURE_SCAN_INTERVAL_TICKS = 100',
    'EXTERNAL_BOSS_MIN_HEALTH = 80.0F',
    'level.hasChunkAt(player.blockPosition())',
    'level.structureManager().getAllStructuresAt(player.blockPosition())',
    'level.structureManager().getStructureWithPieceAt(player.blockPosition(), structure).isValid()',
    'registry.getKey(structure)',
    'data.recordExternalStructure(id.toString())',
    'event.getSource().getEntity() instanceof ServerPlayer player',
    'BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType())',
    'victim.getMaxHealth() < EXTERNAL_BOSS_MIN_HEALTH',
    'data.recordExternalBoss(id.toString())',
    'VANILLA_CONQUEST_TARGETS',
    'minecraft:ender_dragon',
    'minecraft:wither',
), 'alpha.45 loaded exploration/conquest detection')
forbid(exploration, (
    'locate(', 'findNearestMapStructure', 'setChunkForced', 'forceChunk', 'getChunk(', 'teleportTo(',
    'SettlementStorageService', 'SettlementInventory', 'new ItemStack', 'setBlock(', 'destroyBlock(',
    'data.addPopulation(', 'data.setPopulation(', 'data.updateResources(',
), 'alpha.45 exploration must observe, not become world/resource authority')

settlement_data = text(JAVA / 'settlement/SettlementData.java')
must(settlement_data, (
    'MAX_DISCOVERED_STRUCTURE_TYPES = 64',
    'MAX_DEFEATED_BOSS_TYPES = 32',
    'MAX_EXPLORATION_SCORE = 8',
    'optionalFieldOf("discovered_external_structures", List.of())',
    'optionalFieldOf("defeated_external_bosses", List.of())',
    'recordExternalStructure(String id)',
    'recordExternalBoss(String id)',
    'discoveredExternalStructures.contains(id)',
    'defeatedExternalBosses.contains(id)',
    'Math.min(MAX_EXPLORATION_SCORE, discoveredExternalStructures.size() + defeatedExternalBosses.size() * 3)',
), 'alpha.45 shared non-farmable exploration persistence')

service = text(JAVA / 'settlement/SettlementService.java')
if service.count('SettlementExplorationService.tick(server, data)') != 1:
    raise SystemExit('alpha.45 exploration tick must have exactly one server authority call')
must(service, (
    'if (changed || activeProject) broadcast(server, data)',
    'else if (explorationChanged) broadcast(server, data)',
), 'alpha.45 exploration snapshot sync while preserving alpha.43 contract')

entry = text(JAVA / 'FrontierSettlement.java')
must(entry, (
    'SettlementExplorationService',
    'NeoForge.EVENT_BUS.addListener(SettlementExplorationService::onLivingDeath)',
), 'alpha.45 conquest event hook')

tier = text(JAVA / 'settlement/SettlementTier.java')
must(tier, (
    'boolean legacyDomain = data.population() >= 16',
    'data.outposts().size() >= 4',
    'boolean explorationDomain = data.population() >= 14',
    'data.outposts().size() >= 3',
    'data.explorationScore() >= 5',
    'if (legacyDomain || explorationDomain) return DOMAIN',
    'boolean legacyFrontierTown = data.population() >= 8',
    'boolean explorationFrontierTown = data.population() >= 7',
    'data.explorationScore() >= 2',
    'if (legacyFrontierTown || explorationFrontierTown) return FRONTIER_TOWN',
), 'alpha.45 exploration accelerates but never invalidates legacy tier routes')

commands = text(JAVA / 'command/SettlementCommands.java')
must(commands, (
    '"개척 진척 | 외부 구조물 "+data.discoveredExternalStructures().size()+"종 | 정복 강적 "+data.defeatedExternalBosses().size()+"종 | 진척 "+data.explorationScore()+" / 8 | 동일 종류 반복은 중복 없음"',
), 'alpha.45 compact exploration status')

props = text(ROOT / 'gradle.properties')
lock = text(ROOT / 'COMPANION_LOCK.json')
must(props, (
    'mod_version=0.1.0-alpha.45',
    'bounded medium-terrain work using real retaining stone',
    'exploration/conquest milestones',
), 'alpha.45 build properties')
must(lock, (
    '"frontier_settlement": "0.1.0-alpha.45"',
    'already-loaded external structure pieces',
    'historical public WaypointsManager API is absent',
    '"status": "candidate_runtime_lock"',
), 'alpha.45 companion lock truthfulness')

print('Frontier Settlement alpha.45 source audit: PASS')
