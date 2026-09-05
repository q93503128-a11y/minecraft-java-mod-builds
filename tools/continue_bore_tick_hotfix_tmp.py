#!/usr/bin/env python3
from pathlib import Path
import json, runpy

ROOT = Path(__file__).resolve().parents[1]
SA = ROOT / 'projects/survival-ascension'
FR = ROOT / 'projects/frontier-settlement'
SET = FR / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement'
OLD = ROOT / 'tools/apply_bore_tick_hotfix_tmp.py'


def read(p): return p.read_text(encoding='utf-8')
def write(p, s): p.write_text(s, encoding='utf-8')
def rep(p, old, new):
    s = read(p)
    if old not in s: raise SystemExit(f'missing continuation anchor in {p}: {old[:100]!r}')
    write(p, s.replace(old, new, 1))

def insert_in_break_method(path, anchor, insertion):
    s = read(path)
    start = s.find('public static void onBreakBlock(BreakBlockEvent event)')
    if start < 0: raise SystemExit(f'onBreakBlock missing: {path}')
    idx = s.find(anchor, start)
    if idx < 0: raise SystemExit(f'break-method anchor missing in {path}: {anchor!r}')
    idx += len(anchor)
    write(path, s[:idx] + insertion + s[idx:])

# The first-stage script intentionally failed on one stale RoadService whitespace anchor after
# applying the Survival scheduler/profiler and Construction guard. Preserve those successful edits,
# then continue with source-structure-aware patches.
try:
    runpy.run_path(str(OLD), run_name='__main__')
except SystemExit as e:
    if 'SettlementRoadService.java' not in str(e) or 'RoadConstructionState road' not in str(e):
        raise
    print('EXPECTED FIRST-STAGE STALE ROAD ANCHOR:', e)

# Road: raw encoded path/support envelope before tunnelExcavationPlan/createPlan allocations.
road = SET / 'SettlementRoadService.java'
s = read(road)
method = '    public static void onBreakBlock(BreakBlockEvent event) {'
helper = '''    private static boolean withinActiveRoadProtectionEnvelope(RoadConstructionState road, BlockPos pos) {
        List<Integer> path = road.path();
        if (path != null && path.size() >= 3) {
            for (int i = 0; i + 2 < path.size(); i += 3) {
                if (Math.abs(pos.getX() - path.get(i)) <= 4
                        && Math.abs(pos.getZ() - path.get(i + 2)) <= 4
                        && Math.abs(pos.getY() - path.get(i + 1)) <= 12) return true;
            }
        } else {
            for (int i = 0; i < road.length(); i++) {
                int x = road.startX() + road.directionX() * i;
                int z = road.startZ() + road.directionZ() * i;
                if (Math.abs(pos.getX() - x) <= 4 && Math.abs(pos.getZ() - z) <= 4
                        && Math.abs(pos.getY() - road.startY()) <= 12) return true;
            }
        }
        List<Integer> supports = road.bridgeSupports();
        if (supports != null) {
            for (int i = 0; i + 2 < supports.size(); i += 3) {
                if (Math.abs(pos.getX() - supports.get(i)) <= 2
                        && Math.abs(pos.getZ() - supports.get(i + 2)) <= 2
                        && Math.abs(pos.getY() - supports.get(i + 1)) <= 16) return true;
            }
        }
        return false;
    }

'''
if helper.strip() not in s:
    if method not in s: raise SystemExit('Road onBreak method anchor missing')
    s = s.replace(method, helper + method, 1)
old = '        BlockPos pos = event.getPos();\n        BlockState current = level.getBlockState(pos);'
new = '        BlockPos pos = event.getPos();\n        if (!withinActiveRoadProtectionEnvelope(road, pos)) return;\n        BlockState current = level.getBlockState(pos);'
if old not in s: raise SystemExit('Road pos/current anchor missing')
s = s.replace(old, new, 1)
write(road, s)

# Outpost: exact blueprint/footprint protection only matters near its persisted gate.
outpost = SET / 'SettlementOutpostService.java'
insert_in_break_method(outpost, '        BlockPos pos = event.getPos();\n',
'''        if (Math.abs(pos.getX() - state.gateX()) > 16 || Math.abs(pos.getZ() - state.gateZ()) > 16
                || Math.abs(pos.getY() - state.gateY()) > 20) return;
''')

# Core: starter storage remains globally checked first; civic plan reconstruction is center-local.
core = SET / 'SettlementCoreService.java'
rep(core,
'''            return;
        }

        // Protect the matching civic state from every tier, not only the current tier.''',
'''            return;
        }

        BlockPos center = data.centerPos();
        if (Math.abs(pos.getX() - center.getX()) > 6 || Math.abs(pos.getZ() - center.getZ()) > 6
                || pos.getY() < center.getY() - 1 || pos.getY() > center.getY() + 5) return;

        // Protect the matching civic state from every tier, not only the current tier.''')

# Waterfront managed plan is only spruce slabs / barrels / oak fences. Reject stone before SavedData.
waterfront = SET / 'SettlementWaterfrontService.java'
insert_in_break_method(waterfront, '        if (level != server.overworld()) return;\n',
'''        BlockState brokenState = event.getState();
        if (!brokenState.is(Blocks.SPRUCE_SLAB) && !brokenState.is(Blocks.BARREL)
                && !brokenState.is(Blocks.OAK_FENCE)) return;
''')

# Barrel-only managed services: reject ordinary tunnel blocks before SettlementData lookups/scans.
for name in ('SettlementMarketService.java', 'SettlementWorkshopService.java',
             'SettlementAdvancedWorkshopService.java', 'SettlementCartStationService.java'):
    p = SET / name
    insert_in_break_method(p, '        if (level != server.overworld()) return;\n',
                           '        if (!event.getState().is(Blocks.BARREL)) return;\n')

# Road-lamp service can reject every ordinary stone/deepslate break before SavedData/road scans.
tier = SET / 'SettlementTierInfrastructureService.java'
insert_in_break_method(tier, '        if(level!=server.overworld())return;\n',
'''        Block eventBlock=event.getState().getBlock();
        if(eventBlock!=Blocks.OAK_FENCE&&eventBlock!=Blocks.LANTERN)return;
''')

# Frontier version/lock/docs.
rep(FR / 'gradle.properties', 'mod_version=0.1.0-alpha.112', 'mod_version=0.1.0-alpha.113')
with (FR / 'gradle.properties').open('a', encoding='utf-8') as f:
    f.write('\n# Alpha.113 bulk-break event cost: unrelated break events are rejected by cheap physical envelopes/type gates before rebuilding settlement protection plans.\n')
lock_path = FR / 'COMPANION_LOCK.json'
lock = json.loads(read(lock_path))
if lock.get('target', {}).get('frontier_settlement') != '0.1.0-alpha.112':
    raise SystemExit('unexpected Frontier lock version')
lock['target']['frontier_settlement'] = '0.1.0-alpha.113'
lock.setdefault('notes', []).append('Alpha.113 preserves exact infrastructure break protection but rejects unrelated BreakBlockEvents by cheap block-type/geometric gates before expensive plan reconstruction, reducing cross-mod amplification during Survival bulk mining.')
write(lock_path, json.dumps(lock, ensure_ascii=False, indent=2) + '\n')

# Survival changelog and profiler/acceptance runbook.
changelog = SA / 'CHANGELOG.md'
text = read(changelog)
entry = '''## 0.61.17-alpha.1
- Replaced the fixed-count-only tunnel scheduler with a 6 ms global / 4 ms per-job soft server-thread time budget plus EWMA prediction before starting another full vanilla break pipeline. The existing 12-target local hard cap remains only a secondary safety ceiling.
- Kept `ServerPlayerGameMode.destroyBlock` authoritative for every eligible tunnel block, preserving NeoForge break cancellation, Silk Touch/Fortune/loot, item/XP drops, durability policy, stats/advancements, normal neighbor/light/fluid behavior and client synchronization. No chunk force-loading was added.
- Added per-job runtime profiling for target generation, validation, reduced-wear bookkeeping, the complete vanilla/NeoForge destroy pipeline, and scheduler-slice p95/p99/max. `/ascension borestats` reports the latest completed job.
- Frontier Settlement Alpha.113 adds cheap physical envelope/type gates before expensive settlement break-protection plan reconstruction, removing a confirmed cross-mod amplification path triggered once per automatically mined block.
- Network protocol remains 15. Tunnel geometry, hardness gate, drops, enchantment semantics and the one-normal-wear-per-four-successful-extra-block policy are unchanged.

'''
if '## 0.61.17-alpha.1' not in text:
    if not text.startswith('# Changelog\n\n'): raise SystemExit('changelog header drift')
    write(changelog, text.replace('# Changelog\n\n', '# Changelog\n\n' + entry, 1))

write(SA / 'TUNNEL_PERFORMANCE.md', '''# Tunnel bulk-mining server-tick performance

## Reported integrated-server baseline

Minecraft Java 26.2 / NeoForge 26.2 / Java 25, shaders off: ordinary play averaged about 8 ms with a roughly 16 ms max tick. A 7x7x10 tunnel job averaged about 62 ms and showed a roughly 790 ms max server tick while client FPS stayed near 60. These are human F3 measurements and are the acceptance baseline, not CI-generated numbers.

## Current-main root cause audit

The target queue was already a deque and geometry was generated once. The defect was cost control: one job could still start twelve `ServerPlayerGameMode.destroyBlock` pipelines in one tick regardless of the time spent by preceding calls. Each pipeline deliberately includes vanilla/NeoForge break event, loot/enchantment, drop/entity, block/neighbor/light/fluid, stat/advancement and client-sync work.

Frontier Settlement multiplied that cost because its global BreakBlockEvent listeners could rebuild complete active building/road/outpost plans, and the civic-core listener rebuilt all tier plans, even for unrelated tunnel blocks. This was avoidable CPU/allocation/GC pressure layered on top of the necessary break pipeline.

## 0.61.17 / Alpha.113 controls

- 6 ms global and 4 ms per-job soft bore budgets measured with `System.nanoTime()`.
- EWMA prediction avoids knowingly starting another full destroy pipeline when the remaining slice is smaller than recent pipeline cost; one target is always allowed to prevent starvation.
- Existing loaded-chunk admission remains fail-closed. No force-load/generation path was added.
- The manual-equivalent `gameMode.destroyBlock` path remains intact. There is no raw AIR fast path and no loot/enchantment/event bypass.
- Pending-count bookkeeping is batched once per scheduler slice rather than once per target.
- Frontier listeners use block-type or conservative physical envelopes before exact plan protection. Exact cancellation still runs for candidate positions.

## Runtime profiler

After a job completes, run `/ascension borestats`; the log also emits `[bore-profile]`. Buckets: target generation, validation, reduced-wear bookkeeping, complete vanilla destroy pipeline p95/p99/max, and scheduler-slice p95/p99/max. The destroy bucket deliberately includes downstream NeoForge subscribers and Minecraft internals; splitting it further would require invasive instrumentation and is not used as a production shortcut.

## Required real-play acceptance

The original integrated-server pack must be re-run because build/CI success cannot establish F3 MSPT. Exercise ordinary stone, ores/mixed blocks, fluid and gravity adjacency, block entities, chunk boundaries, enchanted/Silk Touch/Fortune tools, full inventory, LAN, player movement/logout/death, save/rejoin, and overlapping requests. Compare F3 avg/max and `/ascension borestats` against the reported ~8/16 ms idle and ~62/790 ms bore baseline. Human performance acceptance is pending until repeated >50 ms and hundreds-of-ms spikes are absent in that environment.
''')

# Survival source verifier version + adaptive/profiler contracts.
sa_test = SA / 'tools/test_current_source.py'
rep(sa_test, 'require("mod_version=0.61.16-alpha.1" in props, "Survival Ascension version drift")', 'require("mod_version=0.61.17-alpha.1" in props, "Survival Ascension version drift")')
rep(sa_test, 'require(\'VERSION = "0.61.16-alpha.1"\' in main, "source version drift")', 'require(\'VERSION = "0.61.17-alpha.1"\' in main, "source version drift")')
rep(sa_test, 'warband = text(JAVA / "elite/WarbandDirector.java")', '''bore = text(JAVA / "mining/BoreMiningService.java")
automated_break = text(JAVA / "progress/AutomatedToolBreak.java")
commands = text(JAVA / "command/AscensionCommands.java")
require("GLOBAL_SOFT_TIME_BUDGET_NANOS = 6_000_000L" in bore and "LOCAL_SOFT_TIME_BUDGET_NANOS = 4_000_000L" in bore, "bore time budget missing")
require("LOCAL_HARD_BLOCK_CAP_PER_TICK = 12" in bore and "now + predicted > localDeadline" in bore, "adaptive predictive stop missing")
require("removePending(job.playerId, removed)" in bore and "removePending(job.playerId, 1)" not in bore, "pending-count batching regressed")
require("TimedBreakResult" in automated_break and "player.gameMode.destroyBlock(target)" in automated_break, "manual-equivalent destroy path/profiler missing")
require("setBlock(target" not in bore and "setChunkForced" not in bore and "addRegionTicket" not in bore, "bore bypass/force-load returned")
require("pipelineP95Nanos" in bore and "sliceP99Nanos" in bore, "bore percentile profiler missing")
require("borestats" in commands and "BoreMiningService.profileLines" in commands, "bore runtime profile command missing")

warband = text(JAVA / "elite/WarbandDirector.java")''')
rep(sa_test, 'print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.16 soft TBOS shrine locator + protocol15 + prior runtime invariants")', 'print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.17 adaptive bore budget/profiling + protocol15 + prior runtime invariants")')

# Frontier source verifier.
fr_test = FR / 'tools/test_current_source.py'
rep(fr_test, 'require("mod_version=0.1.0-alpha.112" in gradle, "current verifier/version drift")', 'require("mod_version=0.1.0-alpha.113" in gradle, "current verifier/version drift")')
rep(fr_test, 'road = text(SETTLEMENT / "SettlementRoadService.java")', '''require("withinConstructionProtectionEnvelope" in construction, "construction bulk-break coarse guard missing")
core_break = text(SETTLEMENT / "SettlementCoreService.java")
require("Math.abs(pos.getX() - center.getX()) > 6" in core_break, "civic core still rebuilds all tier plans for remote breaks")
waterfront_break = text(SETTLEMENT / "SettlementWaterfrontService.java")
require("brokenState.is(Blocks.SPRUCE_SLAB)" in waterfront_break and "brokenState.is(Blocks.BARREL)" in waterfront_break, "waterfront type gate missing")

road = text(SETTLEMENT / "SettlementRoadService.java")''')
rep(fr_test, 'require("infrastructureProjectBuilder" in road and "ProjectLane.ROAD" in road and "clearRoadConstruction" in road,', '''require("withinActiveRoadProtectionEnvelope" in road and "road.path()" in road, "road bulk-break coarse guard missing")
require("Math.abs(pos.getX() - state.gateX()) > 16" in outpost, "outpost bulk-break coarse guard missing")
require("infrastructureProjectBuilder" in road and "ProjectLane.ROAD" in road and "clearRoadConstruction" in road,''')
rep(fr_test, 'print("CURRENT SOURCE CHECK PASS: alpha112 footprint-only placement + blocker diagnostics + alpha111 location UX + prior invariants")', 'print("CURRENT SOURCE CHECK PASS: alpha113 bulk-break event guards + alpha112 footprint-only placement + prior invariants")')

print('CONTINUATION PATCH APPLIED')
