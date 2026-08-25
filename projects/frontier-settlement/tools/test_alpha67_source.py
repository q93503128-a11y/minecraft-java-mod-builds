#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A66=ROOT/'tools/test_alpha66_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
# Preserve every Alpha.23-66 invariant while evaluating the current Alpha.67 version/lock.
a=text(A66).replace("print('Frontier Settlement alpha.23-66 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.66','0.1.0-alpha.67'); ns={'__file__':str(A66),'__name__':'__main__'}; exec(compile(a,str(A66),'exec'),ns,ns)
outpost=text(JAVA/'settlement/SettlementOutpostLogisticsService.java'); military=text(JAVA/'settlement/SettlementMilitaryOutpostService.java'); props=text(ROOT/'gradle.properties')
must(outpost,(
    'private static boolean assignmentEvidenceLoaded(ServerLevel level, SettlementData data,',
    'AABB bounds = routeBounds(data, outpost, route)',
    'Math.floor(Math.nextDown(bounds.maxX))',
    'Math.floor(Math.nextDown(bounds.maxZ))',
    'BlockPos probe = new BlockPos(chunkX * 16 + 8, probeY, chunkZ * 16 + 8)',
    'if (!level.hasChunkAt(probe)) return false',
    'if (!assignmentEvidenceLoaded(level, data, outpost)) continue',
    'if (outpost == null || !assignmentEvidenceLoaded(level, data, outpost)',
),'alpha.67 transporter assignment evidence')
# The exact entity lookup envelope must remain the evidence envelope source; otherwise absence can drift again.
if outpost.count('routeBounds(data, outpost, route)') < 4:
    raise SystemExit('alpha.67 routeBounds no longer shared by lookup/evidence')
# Population reconciliation consumes allRoutesLoaded; that method must now be assignment-evidence strong.
all_routes=outpost[outpost.index('public static boolean allRoutesLoaded'):outpost.index('public static OutpostRecord firstMissingLoadedAssignment')]
if 'assignmentEvidenceLoaded(level, data, outpost)' not in all_routes or 'routeFullyLoaded(level, data, outpost)' in all_routes:
    raise SystemExit('alpha.67 allRoutesLoaded is not assignment-evidence authoritative')
loaded_count=outpost[outpost.index('public static int loadedAssignedWorkerCount'):outpost.index('public static boolean allRoutesLoaded')]
if 'assignmentEvidenceLoaded(level, data, outpost)' not in loaded_count:
    raise SystemExit('alpha.67 loaded transporter count can still reconcile from partial evidence')
missing=outpost[outpost.index('public static OutpostRecord firstMissingLoadedAssignment'):outpost.index('public static void onLivingDrops')]
if 'assignmentEvidenceLoaded(level, data, outpost)' not in missing:
    raise SystemExit('alpha.67 missing transporter inference can still run from partial evidence')
# Normal physical movement deliberately retains routeFullyLoaded/hasChunkAt boundary semantics; do not turn this into force loading.
tick=outpost[outpost.index('public static void tick'):outpost.index('public static int transportBatchSize')]
if 'assignmentEvidenceLoaded' in tick:
    raise SystemExit('alpha.67 assignment evidence leaked into normal transporter movement loop')
must(outpost,('public static boolean routeFullyLoaded(ServerLevel level, SettlementData data, OutpostRecord outpost)','if (!level.hasChunkAt(pos)) return false','if (!level.hasChunkAt(nextPos)) {','worker.getNavigation().stop()'),'alpha.67 unloaded route pause preserved')
forbid(outpost,('setChunkForced','forceChunk','teleportTo('),'alpha.67 no force-load/teleport authority')
# Sentry already uses a 32-block loaded-area proof matching its 32-block lookup radius; keep that independent authority intact.
must(military,('SENTRY_SEARCH_RADIUS = 32.0D','LOADED_MARGIN = 32','militaryAreaLoaded(ServerLevel level, OutpostRecord outpost)','level.hasChunkAt(center.offset(dx, 0, dz))'),'alpha.67 sentry loaded-area regression')
must(props,('mod_version=0.1.0-alpha.67','fail-closed outpost transporter assignment evidence'),'alpha.67 props')
print('Frontier Settlement alpha.23-67 cumulative source audit: PASS')
