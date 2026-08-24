#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
ALPHA51 = ROOT / 'tools/test_alpha51_source.py'


def text(path): return path.read_text(encoding='utf-8')
def must(source, tokens, label):
    for token in tokens:
        if token not in source: raise SystemExit(f'{label} missing: {token}')
def forbid(source, tokens, label):
    for token in tokens:
        if token in source: raise SystemExit(f'{label}: {token}')

if not ALPHA51.exists(): raise SystemExit('historical Alpha.51 source audit must remain present')
alpha51 = text(ALPHA51).replace("print('Frontier Settlement alpha.23-51 cumulative source audit: PASS')", 'pass')
alpha51 = alpha51.replace('0.1.0-alpha.51', '0.1.0-alpha.52')
_original_read_text = Path.read_text
def _alpha52_audit_read_text(path, *args, **kwargs):
    source = _original_read_text(path, *args, **kwargs)
    if path.name == 'test_current_source.py':
        source = source.replace("'MAX_BRIDGE_SPAN = 6'", "'MAX_SHORT_BRIDGE_SPAN = 6'")
        source = source.replace("'data.beginRoadConstruction(chosen.centers(), chosen.profile())'",
                                "'data.beginRoadConstruction(chosen.centers(), chosen.profile(), chosen.supports())'")
    return source
Path.read_text = _alpha52_audit_read_text
try:
    namespace = {'__file__': str(ALPHA51), '__name__': '__main__'}
    exec(compile(alpha51, str(ALPHA51), 'exec'), namespace, namespace)
finally:
    Path.read_text = _original_read_text

road_state = text(JAVA / 'settlement/RoadConstructionState.java')
road = text(JAVA / 'settlement/SettlementRoadService.java')
data = text(JAVA / 'settlement/SettlementData.java')
props = text(ROOT / 'gradle.properties')
lock = text(ROOT / 'COMPANION_LOCK.json')

must(road_state, ('optionalFieldOf("bridge_supports", List.of())', 'bridgeSupportPositions()',
                  'bridgeSupportCount()', 'fromPath(List<BlockPos> centers, List<Integer> profile,',
                  'step + 1, path, profile, bridgeSupports', 'encoded, path, profile, bridgeSupports'),
     'alpha.52 persisted bridge supports')
must(data, ('beginRoadConstruction(List<BlockPos> centers, List<Integer> profile, List<BlockPos> bridgeSupports)',
            'RoadConstructionState.fromPath(centers, profile, bridgeSupports)'), 'alpha.52 road state authority')
must(road, ('MAX_SHORT_BRIDGE_SPAN = 6', 'MAX_LONG_BRIDGE_SPAN = 24', 'MIN_RAVINE_DEPTH = 4',
            'MAX_LONG_BRIDGE_PIER_DEPTH = 12', 'LONG_BRIDGE_PIER_INTERVAL = 6',
            'boolean legacyPrepaidRepair = road.legacyPrepaidPaving()',
            '!legacyPrepaidRepair && !ensurePavingMaterial(server, data, builder, 1L, 1L)',
            '!legacyPrepaidRepair && !consumeCarriedStone(builder, 1L)',
            'planBridgeSupports(', 'planPierColumn(', 'isNaturalSupportGround(',
            'chosen.supports()', 'road.bridgeSupportCount()', 'bridgeSupportPositions()',
            '교각이 필요한 장교량은 현재 직선 구간에서만',
            'level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE)',
            'consumeCarriedStone(builder, stoneDelta)', 'data.advanceRoadConstruction()',
            'if (changed) level.setBlock(target, current, DIRECT_BLOCK_UPDATE)',
            'ensurePavingMaterial(server, data, builder, 1L, 1L)',
            'consumeCarriedStone(builder, 1L)'), 'alpha.52 long bridge physical authority')
place = road.find('if (!level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE)) return false;')
consume = road.find('if (!consumeCarriedStone(builder, stoneDelta))', place)
advance = road.find('data.advanceRoadConstruction();', consume)
if min(place, consume, advance) < 0 or not (place < consume < advance):
    raise SystemExit('alpha.52 road paving must place successfully before stone consume/state advance')
finish = road.find('private static boolean finishIfValid')
repair_material = road.find('ensurePavingMaterial(server, data, builder, 1L, 1L)', finish)
repair_place = road.find('level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)', repair_material)
repair_consume = road.find('consumeCarriedStone(builder, 1L)', repair_place)
if min(finish, repair_material, repair_place, repair_consume) < 0 or not (finish < repair_material < repair_place < repair_consume):
    raise SystemExit('alpha.52 final repair must physically fetch/place/consume stone; no free repair')
forbid(road, ('forceChunk', 'setChunkForced', 'teleportTo(', 'destroyBlock(', 'dropResources('),
       'alpha.52 road safety')
must(props, ('mod_version=0.1.0-alpha.52', 'bounded long-bridge/ravine crossings with persisted physical stone piers'),
     'alpha.52 build properties')
must(lock, ('"frontier_settlement": "0.1.0-alpha.52"', 'Alpha.52 extends the existing road authority',
            'up to 24 centerline cells', 'support within 12 blocks', 'same road builder physically hauls stone',
            '"status": "candidate_runtime_lock"'), 'alpha.52 companion lock')

print('Frontier Settlement alpha.23-52 cumulative source audit: PASS')
