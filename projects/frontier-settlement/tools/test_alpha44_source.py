#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
ALPHA43 = ROOT / 'tools/test_alpha43_source.py'


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


# Preserve every Alpha.23-43 invariant, then extend the same physical construction authority for medium terrain.
alpha43_source = text(ALPHA43)
alpha43_source = alpha43_source.replace("print('Frontier Settlement alpha.43 source audit: PASS')", 'pass')
alpha43_source = alpha43_source.replace('0.1.0-alpha.43', '0.1.0-alpha.44')
namespace = {'__file__': str(ALPHA43), '__name__': '__main__'}
exec(compile(alpha43_source, str(ALPHA43), 'exec'), namespace, namespace)

construction = text(JAVA / 'settlement/SettlementConstructionService.java')
must(construction, (
    'SMALL_TERRAIN_SPAN = 2',
    'MAX_TERRAIN_WORK_SPAN = 4',
    'MAX_TERRAIN_CUT_HEIGHT = 3',
    'MAX_TERRAIN_RETAINING_STONE = 96',
    'boolean terrainWork, int terrainStoneCost',
    'private record Site(BlockPos origin, int terrainSpan, int terrainStoneCost)',
    'private record GradeCell(BlockPos floor, boolean foundation, int retainingStone)',
    'terrainSpan > MAX_TERRAIN_WORK_SPAN',
    '지형 공사 포함',
    '옹벽/기초 추가 석재',
    'long requiredStone = type.stoneCost() + check.terrainStoneCost()',
    'createGradePlan(level, construction, type)',
    'fillDepthToSupport(level, floor)',
    'edge && fillDepth >= 2 ? fillDepth : 0',
    'stageTerrainStone(server, data, builder, crate, supply, cell.retainingStone())',
    'SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isStone)',
    'SettlementStorageService.extract(level, source, SettlementInventory::isStone, amount)',
    'cell.retainingStone()',
    'Blocks.COBBLESTONE.defaultBlockState()',
    'Blocks.COARSE_DIRT.defaultBlockState()',
    'int minFoundationY = construction.originY() - 1 - MAX_GRADE_FILL_DEPTH',
    'relativeY <= MAX_TERRAIN_CUT_HEIGHT && isNaturalGround(state)',
), 'alpha.44 bounded medium terrain construction')
forbid(construction, (
    'destroyBlock(', 'dropResources(', 'setChunkForced', 'forceChunk', 'teleportTo(',
    'SettlementStorageService.consume(level, data, type.woodCost(), type.stoneCost()',
), 'alpha.44 terrain work must stay physical/bounded')

# Alpha.60 intentionally supersedes Alpha.44's historical consume-before-placement ordering.
# The historical invariant that retaining/foundation stone is real remains, while the current
# transaction is stronger: reversible world mutation succeeds before retaining ItemStack commit.
if 'stageTerrainStone(server, data, builder, crate, supply, cell.retainingStone())' not in construction:
    raise SystemExit('alpha.44 retaining stone is no longer physically staged')
if 'SettlementInventory.consume(terrainCrate, 0L, cell.retainingStone(), 0L)' not in construction:
    raise SystemExit('alpha.44 retaining stone physical consume disappeared')
if 'applyGradeCellTransactional(level, construction, type, cell)' not in construction:
    raise SystemExit('alpha.60 transactional grade placement missing while superseding Alpha.44 ordering')

# The failed historical Xaero custom-waypoint probe is intentionally removed. Keep only Alpha.43 layout collision avoidance.
xaero_compat = JAVA / 'compat/xaero/FrontierXaeroWaypoints.java'
if xaero_compat.exists():
    raise SystemExit('alpha.44 brittle Xaero compat probe must be removed after 26.4.2 API rejection')
client = text(JAVA / 'client/FrontierSettlementClient.java')
build = text(ROOT / 'build.gradle')
forbid(client, ('FrontierXaeroWaypoints', 'import xaero.'), 'alpha.44 no direct Xaero runtime link')
forbid(build, ('chocolateminecraft.com', 'xaero.minimap:', "includeGroup 'xaero.lib'"),
       'alpha.44 Xaero compile probe must not remain in the runtime build')

props = text(ROOT / 'gradle.properties')
lock_text = text(ROOT / 'COMPANION_LOCK.json')
must(props, (
    'jade_version_id=HLYMycSr',
    'mod_version=0.1.0-alpha.44',
    'bounded medium-terrain work using real retaining stone',
), 'alpha.44 build properties')
forbid(props, ('xaero_minimap_version=',), 'alpha.44 removed Xaero compile probe property')
try:
    lock = json.loads(lock_text)
except json.JSONDecodeError as exc:
    raise SystemExit(f'alpha.44 companion lock is not valid JSON: {exc}')
if lock.get('status') != 'candidate_runtime_lock':
    raise SystemExit('alpha.44 companion candidate lock status changed')
if lock.get('target', {}).get('frontier_settlement') != '0.1.0-alpha.44':
    raise SystemExit('alpha.44 Frontier target version missing from companion lock')
xaero = next((entry for entry in lock.get('entries', []) if entry.get('id') == 'xaeros_minimap'), None)
if xaero is None or xaero.get('version') != '26.4.2':
    raise SystemExit('alpha.44 Xaero 26.4.2 candidate entry missing from companion lock')

print('Frontier Settlement alpha.44 source audit: PASS')
