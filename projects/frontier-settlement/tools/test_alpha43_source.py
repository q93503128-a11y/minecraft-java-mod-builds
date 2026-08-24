#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
ALPHA42 = ROOT / 'tools/test_alpha42_source.py'


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


# Preserve every Alpha.23-42 contract; only advance the canonical version and PASS banner.
alpha42_source = text(ALPHA42)
alpha42_source = alpha42_source.replace("print('Frontier Settlement alpha.42 source audit: PASS')", 'pass')
alpha42_source = alpha42_source.replace('0.1.0-alpha.42', '0.1.0-alpha.43')
namespace = {'__file__': str(ALPHA42), '__name__': '__main__'}
exec(compile(alpha42_source, str(ALPHA42), 'exec'), namespace, namespace)

required = [
    JAVA / 'network/SettlementContextTarget.java',
    JAVA / 'network/SettlementContextPayload.java',
    JAVA / 'settlement/SettlementContextService.java',
    JAVA / 'client/SettlementNoticeQueue.java',
    JAVA / 'client/ClientCompanionLayout.java',
    JAVA / 'compat/jade/FrontierJadePlugin.java',
    JAVA / 'compat/jade/FrontierJadeBlockProvider.java',
]
missing = [str(path.relative_to(ROOT)) for path in required if not path.is_file()]
if missing:
    raise SystemExit('alpha.43 missing required files: ' + ', '.join(missing))

context_target = text(JAVA / 'network/SettlementContextTarget.java')
must(context_target, (
    'presentation/Jade', 'boolean contains(BlockPos pos)', 'String title', 'String detail', 'int progress',
), 'alpha.43 compact spatial context target')

context_payload = text(JAVA / 'network/SettlementContextPayload.java')
must(context_payload, (
    'MAX_TARGETS = 256', 'List.copyOf(targets)', 'SettlementContextTarget.encode',
    'SettlementContextTarget.decode', 'targetAt(net.minecraft.core.BlockPos pos)',
), 'alpha.43 bounded context payload')

context_service = text(JAVA / 'settlement/SettlementContextService.java')
must(context_service, (
    'presentation-only context',
    '"stockpile", "stockpile"',
    '"building:" + type.id()',
    '"outpost:" + outpost.id()',
    'SettlementFishingOutpostService.specializationDisplayName(level, outpost)',
    '"공동 창고"',
    '"실물 권위 · 목재 "',
    'construction.grading() ? "부지 정리 중" : "자재 운반·시공 중"',
), 'alpha.43 authoritative presentation snapshot')
forbid(context_service, (
    'new ItemStack', 'Container', 'level.setBlock(', 'forceChunk', 'setChunkForced', 'getChunk(',
    'teleportTo(', 'destroyBlock(', 'dropResources(', 'data.addPopulation(', 'data.setPopulation(',
), 'alpha.43 context must never become gameplay authority')

snapshot = text(JAVA / 'network/SettlementSnapshotPayload.java')
must(snapshot, (
    'SettlementContextPayload context',
    'SettlementContextPayload.CODEC.encode(buf, payload.context())',
    'SettlementContextPayload.CODEC.decode(buf)',
), 'alpha.43 context sync on existing settlement snapshot')

service = text(JAVA / 'settlement/SettlementService.java')
must(service, (
    'SettlementContextService.snapshot(player.level().getServer(), data)',
    'boolean activeProject = data.construction().active() || data.roadConstruction().active() || data.outpostConstruction().active()',
    'if (changed || activeProject) broadcast(server, data)',
), 'alpha.43 project-progress synchronization')

client_state = text(JAVA / 'client/ClientSettlementState.java')
must(client_state, (
    'SettlementNoticeQueue.push("마을 성장 · " + next.tier())',
    'SettlementNoticeQueue.push("완공 · " + target.title())',
    'SettlementNoticeQueue.push("영토 확장 · " + target.title())',
    'SettlementNoticeQueue.push("공사 시작 · " + next.projectLabel())',
), 'alpha.43 meaningful client transitions')

notices = text(JAVA / 'client/SettlementNoticeQueue.java')
must(notices, (
    'LIFETIME_MS = 6_000L', 'MAX_VISIBLE = 3',
    'screenWidth - width - 8', '0xB0000000',
    'no modal popup and no new key',
), 'alpha.43 compact side notifications')

layout = text(JAVA / 'client/ClientCompanionLayout.java')
must(layout, (
    'ModList.get().isLoaded("xaerominimap") ? 154 : 8',
), 'alpha.43 Xaero-aware HUD collision avoidance')
forbid(layout, ('xaero.common.', 'xaero.hud.', 'Class.forName('), 'alpha.43 no brittle Xaero internal API link yet')

hud = text(JAVA / 'client/SettlementHudOverlay.java')
must(hud, (
    'ClientCompanionLayout.resourceHudY()',
    'context.projectLabel()',
    'context.projectProgress()',
    'SettlementNoticeQueue.render(graphics, minecraft)',
), 'alpha.43 compact HUD progress/notices')

jade_plugin = text(JAVA / 'compat/jade/FrontierJadePlugin.java')
jade_provider = text(JAVA / 'compat/jade/FrontierJadeBlockProvider.java')
must(jade_plugin, (
    '@WailaPlugin', 'implements IWailaPlugin',
    'registration.registerBlockComponent(FrontierJadeBlockProvider.INSTANCE, Block.class)',
), 'alpha.43 optional Jade plugin')
must(jade_provider, (
    'implements IBlockComponentProvider',
    'ClientSettlementState.context().targetAt(accessor.getPosition())',
    'tooltip.add(Component.literal(target.title()))',
    'tooltip.add(Component.literal(detail))',
    'return FrontierJadePlugin.SETTLEMENT_STATUS',
    'return 2500',
), 'alpha.43 Jade minimal infrastructure status')

build = text(ROOT / 'build.gradle')
props = text(ROOT / 'gradle.properties')
must(build, (
    "url = 'https://api.modrinth.com/maven'",
    'compileOnly "maven.modrinth:nvQzSEkH:${project.jade_version_id}"',
), 'alpha.43 optional Jade compile seam')
must(props, ('jade_version_id=HLYMycSr', 'mod_version=0.1.0-alpha.43'), 'alpha.43 locked build properties')
if 'implementation "maven.modrinth:nvQzSEkH:' in build:
    raise SystemExit('alpha.43 Jade must not become runtime implementation dependency')

# Jade references are quarantined to the optional compat package. Frontier core/client must boot without Jade.
for path in JAVA.rglob('*.java'):
    if 'compat/jade' in path.as_posix():
        continue
    source = text(path)
    if 'snownee.jade' in source:
        raise SystemExit(f'alpha.43 hard Jade reference outside compat seam: {path.relative_to(ROOT)}')

keys = text(JAVA / 'client/BuildingPlacementClient.java')
for token in ('key.frontier_settlement.build_mode', 'key.frontier_settlement.rotate_building',
              'key.frontier_settlement.confirm_building', 'key.frontier_settlement.road_reset'):
    if token not in keys:
        raise SystemExit('alpha.43 fixed key contract changed: ' + token)

print('Frontier Settlement alpha.43 source audit: PASS')
