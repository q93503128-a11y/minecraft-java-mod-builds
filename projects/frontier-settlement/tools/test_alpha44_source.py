#!/usr/bin/env python3
from pathlib import Path

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


# Preserve every Alpha.23-43 contract. Alpha.44 only advances version and adds the safe Xaero marker seam.
alpha43_source = text(ALPHA43)
alpha43_source = alpha43_source.replace("print('Frontier Settlement alpha.43 source audit: PASS')", 'pass')
alpha43_source = alpha43_source.replace('0.1.0-alpha.43', '0.1.0-alpha.44')
namespace = {'__file__': str(ALPHA43), '__name__': '__main__'}
exec(compile(alpha43_source, str(ALPHA43), 'exec'), namespace, namespace)

xaero_path = JAVA / 'compat/xaero/FrontierXaeroWaypoints.java'
if not xaero_path.is_file():
    raise SystemExit('alpha.44 missing Xaero custom-waypoint compat seam')
xaero = text(xaero_path)
must(xaero, (
    'private static final String MOD_KEY = "frontier_settlement"',
    'WaypointsManager.getCustomWaypoints(MOD_KEY)',
    'customWaypoints.clear()',
    '"stockpile".equals(target.kind())',
    '"outpost".equals(target.kind())',
    'String name = settlement ? "개척마을" : target.title()',
    'String symbol = settlement ? "M" : "O"',
    'new Waypoint(',
    'WaypointColor.GRAY',
    'catch (LinkageError error)',
    'disabled = true',
    'Xaero 마커 연동 비활성 · 버전 확인 필요',
    'clearOwnedWaypoints()',
    'RESYNC_TICKS = 20',
), 'alpha.44 bounded Xaero settlement/outpost markers')
forbid(xaero, (
    'BuiltInHudModules', 'currentSession', 'worldManager', 'getWaypointSet(',
    'Mixin', 'org.spongepowered.asm.mixin', 'Class.forName(', 'java.lang.reflect',
    'setBlock(', 'ItemStack', 'Container', 'teleportTo(', 'setChunkForced', 'forceChunk',
), 'alpha.44 Xaero seam must avoid internal/mixin/gameplay authority')

# Do not turn every building or road point into map clutter.
if '"building".equals(target.kind())' in xaero or '"construction".equals(target.kind())' in xaero:
    raise SystemExit('alpha.44 Xaero marker scope expanded beyond settlement/outpost location awareness')
if 'RoadSegment' in xaero or 'road.points' in xaero or 'waypoints()' in xaero:
    raise SystemExit('alpha.44 Xaero marker seam must not mirror road centerline points')

client = text(JAVA / 'client/FrontierSettlementClient.java')
must(client, (
    'ModList.get().isLoaded("xaerominimap")',
    'NeoForge.EVENT_BUS.addListener(FrontierXaeroWaypoints::tick)',
), 'alpha.44 optional client registration')

build = text(ROOT / 'build.gradle')
props = text(ROOT / 'gradle.properties')
lock = text(ROOT / 'COMPANION_LOCK.json')
must(build, (
    "url = 'https://chocolateminecraft.com/maven'",
    "includeGroup 'xaero.minimap'",
    "includeGroup 'xaero.lib'",
    'compileOnly "xaero.minimap:xaerominimap-neoforge-${project.minecraft_version}:${project.xaero_minimap_version}"',
), 'alpha.44 official Xaero compile seam')
must(props, (
    'xaero_minimap_version=26.4.2',
    'mod_version=0.1.0-alpha.44',
), 'alpha.44 locked Xaero/version properties')
must(lock, (
    '"frontier_settlement": "0.1.0-alpha.44"',
    '"version_id":"IqOn6XCo"',
    '"version":"26.4.2"',
    '"status": "candidate_runtime_lock"',
), 'alpha.44 companion lock')
if 'implementation "xaero.minimap:' in build:
    raise SystemExit('alpha.44 Xaero must not become a runtime implementation dependency')

# All direct Xaero classes stay quarantined under compat/xaero. Core/client may only use the mod-id string and compat class.
for path in JAVA.rglob('*.java'):
    if 'compat/xaero' in path.as_posix():
        continue
    source = text(path)
    if 'import xaero.' in source or 'xaero.common.' in source or 'xaero.hud.' in source:
        raise SystemExit(f'alpha.44 hard Xaero reference outside compat seam: {path.relative_to(ROOT)}')

print('Frontier Settlement alpha.44 source audit: PASS')
