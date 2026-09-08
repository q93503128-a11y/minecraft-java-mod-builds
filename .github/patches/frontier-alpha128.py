from pathlib import Path
import re
import json

ROOT = Path('projects/frontier-settlement')
CONSTRUCTION_PATH = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java'
CONTEXT_PATH = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementContextService.java'
TEST_PATH = ROOT / 'tools/test_current_source.py'
GRADLE_PATH = ROOT / 'gradle.properties'
README_PATH = ROOT / 'README.md'


def read(path):
    return path.read_text(encoding='utf-8')


def write(path, content):
    path.write_text(content, encoding='utf-8')


def sub_once(text, pattern, replacement, label):
    out, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f'{label}: expected one match, got {count}')
    return out


construction = read(CONSTRUCTION_PATH)
construction = sub_once(
    construction,
    r'    private static boolean moveBuilderTowardGradeCell\(ServerLevel level, FrontierWorkerEntity builder, BlockPos target\) \{.*?    private static List<GradeCell> createGradePlan',
    '''    private static boolean moveBuilderTowardGradeCell(ServerLevel level, FrontierWorkerEntity builder, BlockPos target) {
        for (BlockPos candidate : gradeApproachPositions(level, builder, target)) {
            double distance = builder.distanceToSqr(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
            if (distance <= 4.0D) {
                builder.getNavigation().stop();
                return true;
            }
            if (moveToReachable(builder, candidate, 1.05D)) return true;
        }
        builder.getNavigation().stop();
        return false;
    }

    private static boolean hasReachableGradeWorkPosition(ServerLevel level, FrontierWorkerEntity builder, BlockPos target) {
        for (BlockPos candidate : gradeApproachPositions(level, builder, target)) {
            if (builder.distanceToSqr(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D) <= 4.0D
                    || createReachablePath(builder, candidate) != null) return true;
        }
        return false;
    }

    /**
     * Placement intentionally accepts natural trees and soft vegetation inside a lot. Approach the
     * first pending grade cell from nearby real ground so one tree-covered corner cannot pin a
     * valid project at zero percent.
     */
    private static List<BlockPos> gradeApproachPositions(ServerLevel level, FrontierWorkerEntity builder, BlockPos target) {
        Set<BlockPos> unique = new HashSet<>();
        for (int radius = 0; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = target.getX() + dx;
                    int z = target.getZ() + dz;
                    int y = terrainSurfaceHeight(level, x, z);
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (isWalkableApproachCell(level, candidate)) unique.add(candidate);
                }
            }
        }
        List<BlockPos> result = new ArrayList<>(unique);
        result.sort(Comparator.comparingDouble(pos -> {
            double tx = pos.getX() - target.getX();
            double tz = pos.getZ() - target.getZ();
            return (tx * tx + tz * tz) * 4.0D + builder.distanceToSqr(
                    pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        }));
        return List.copyOf(result);
    }

    private static List<GradeCell> createGradePlan''',
    'grading approach replacement')

construction = sub_once(
    construction,
    r'    private static void recoverBuilderFromBlockedCell\(ServerLevel level, SettlementData data, FrontierWorkerEntity builder\) \{.*?    private static BlockPos findSafeBuilderHome\(ServerLevel level, SettlementData data\) \{',
    '''    private static void recoverBuilderFromBlockedCell(ServerLevel level, SettlementData data, FrontierWorkerEntity builder) {
        BlockPos feet = builder.blockPosition();
        BlockPos head = feet.above();
        if (!level.hasChunkAt(feet) || !level.hasChunkAt(head)) return;
        boolean physicallyBlocked = blocksCurrentPathCell(level, feet, level.getBlockState(feet))
                || blocksCurrentPathCell(level, head, level.getBlockState(head));
        boolean elevatedStranded = !physicallyBlocked && builderStrandedOnArtificialElevation(level, data, builder);
        if (!physicallyBlocked && !elevatedStranded) return;
        BlockPos safe = findSafeBuilderHome(level, data);
        if (safe == null) return;
        if (elevatedStranded && createReachablePath(builder, safe) != null) return;
        builder.getNavigation().stop();
        builder.setPos(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
    }

    /** Accessible balconies/bridges stay physical; only a disconnected elevated perch is recovered. */
    private static boolean builderStrandedOnArtificialElevation(ServerLevel level, SettlementData data,
                                                                 FrontierWorkerEntity builder) {
        BlockPos feet = builder.blockPosition();
        int naturalGroundY = nearestNaturalGroundBelow(level, feet, 16);
        if (naturalGroundY == Integer.MIN_VALUE) return false;
        int artificialRise = (feet.getY() - 1) - naturalGroundY;
        if (artificialRise < 3) return false;
        BlockPos safe = findSafeBuilderHome(level, data);
        return safe != null && createReachablePath(builder, safe) == null;
    }

    private static int nearestNaturalGroundBelow(ServerLevel level, BlockPos feet, int maxDepth) {
        for (int depth = 1; depth <= maxDepth; depth++) {
            BlockPos probe = feet.below(depth);
            if (!level.hasChunkAt(probe)) return Integer.MIN_VALUE;
            if (isNaturalGround(level.getBlockState(probe))) return probe.getY();
        }
        return Integer.MIN_VALUE;
    }

    private static BlockPos findSafeBuilderHome(ServerLevel level, SettlementData data) {''',
    'builder recovery replacement')

construction = sub_once(
    construction,
    r'    private static BlockPos findSafeBuilderHome\(ServerLevel level, SettlementData data, Set<BlockPos> occupied\) \{.*?    private static BlockPos safeSurfaceCell\(ServerLevel level, int x, int z\) \{',
    '''    private static BlockPos findSafeBuilderHome(ServerLevel level, SettlementData data, Set<BlockPos> occupied) {
        BlockPos center = data.centerPos();
        int referenceY = center.getY();
        BlockPos preferred = safeBuilderHomeCell(level, center.getX() + 1, center.getZ() + 1, referenceY);
        if (preferred != null && !occupied.contains(preferred)) return preferred;
        // Spawn/recovery is rare: a wider bounded real-ground search is cheaper than a roof-stranded project.
        for (int radius = 1; radius <= 24; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BlockPos candidate = safeBuilderHomeCell(level, center.getX() + dx, center.getZ() + dz, referenceY);
                    if (candidate != null && !occupied.contains(candidate)) return candidate;
                }
            }
        }
        return null;
    }

    /** Never choose an arbitrary highest collision surface such as a roof or tall log pillar as home. */
    private static BlockPos safeBuilderHomeCell(ServerLevel level, int x, int z, int referenceY) {
        BlockPos candidate = safeSurfaceCell(level, x, z);
        if (candidate == null) return null;
        BlockState support = level.getBlockState(candidate.below());
        if (isNaturalGround(support) || support.is(Blocks.DIRT_PATH)) return candidate;
        return null;
    }

    private static BlockPos safeSurfaceCell(ServerLevel level, int x, int z) {''',
    'safe builder home replacement')
write(CONSTRUCTION_PATH, construction)

context = read(CONTEXT_PATH)
old_label = 'projectLabel = type.displayName() + " 공사" + (constructionIssue.isBlank() ? "" : " · 막힘");'
new_label = 'projectLabel = type.displayName() + " 공사" + (constructionIssue.isBlank() ? "" : " · " + constructionIssueSummary(constructionIssue));'
if context.count(old_label) != 1:
    raise SystemExit(f'context label anchor count={context.count(old_label)}')
context = context.replace(old_label, new_label, 1)
helper_anchor = '    private static int percent(int worked, int total) {\n'
helper = '''    private static String constructionIssueSummary(String issue) {
        if (issue == null || issue.isBlank()) return "";
        if (issue.contains("접근 불가")) return issue.contains("부지 정리") ? "부지 접근 불가" : "자재·현장 접근 불가";
        if (issue.contains("청크 미로드")) return "청크 미로드";
        if (issue.contains("목재 대기")) return "목재 대기";
        if (issue.contains("석재 대기")) return "석재 대기";
        if (issue.contains("자재통")) return "자재통 문제";
        if (issue.contains("위치 막힘") || issue.contains("정리 막힘")) return "부지 막힘";
        if (issue.contains("건설 주민")) return "건설 주민 대기";
        return "막힘";
    }

'''
if context.count(helper_anchor) != 1:
    raise SystemExit('context helper anchor drift')
write(CONTEXT_PATH, context.replace(helper_anchor, helper + helper_anchor, 1))

gradle = read(GRADLE_PATH)
if gradle.count('mod_version=0.1.0-alpha.127') != 1:
    raise SystemExit('gradle version drift')
write(GRADLE_PATH, gradle.replace('mod_version=0.1.0-alpha.127', 'mod_version=0.1.0-alpha.128', 1))

readme = read(README_PATH)
if readme.count('## Current version: 0.1.0-alpha.127') != 1:
    raise SystemExit('README version drift')
write(README_PATH, readme.replace('## Current version: 0.1.0-alpha.127', '## Current version: 0.1.0-alpha.128', 1))

test = read(TEST_PATH)
if test.count('mod_version=0.1.0-alpha.127') != 1:
    raise SystemExit('current verifier version drift')
test = test.replace('mod_version=0.1.0-alpha.127', 'mod_version=0.1.0-alpha.128', 1)
marker = '''require("현장 자재통 위치가 막혀 있습니다" in construction and "실제 건물 부지 안의 정리 칸이 막혀 있습니다" in construction,
        "placement blocker diagnostics missing")
'''
addition = marker + '''require("gradeApproachPositions" in construction and "radius <= 3" in construction
        and "terrainSurfaceHeight(level, x, z)" in construction,
        "tree-aware bounded grading approach recovery missing")
require("safeBuilderHomeCell" in construction and "radius <= 24" in construction
        and "support.is(Blocks.DIRT_PATH)" in construction,
        "builder home can regress to arbitrary roof-height surfaces")
require("builderStrandedOnArtificialElevation" in construction and "nearestNaturalGroundBelow" in construction
        and "artificialRise < 3" in construction,
        "disconnected elevated builder recovery missing")
context_service = text(SETTLEMENT / "SettlementContextService.java")
require("constructionIssueSummary" in context_service and "부지 접근 불가" in context_service
        and "자재·현장 접근 불가" in context_service,
        "construction HUD returned to generic blocked-only diagnosis")
'''
if test.count(marker) != 1:
    raise SystemExit('current source construction audit anchor drift')
write(TEST_PATH, test.replace(marker, addition, 1))

for rel in ['COMPANION_LOCK.json', 'companion-testpack/resolved-lock.client.json', 'companion-testpack/resolved-lock.server.json']:
    path = ROOT / rel
    data = json.loads(read(path))
    current = data.get('target', {}).get('frontier_settlement')
    if current != '0.1.0-alpha.127':
        raise SystemExit(f'{rel}: target drift {current}')
    data['target']['frontier_settlement'] = '0.1.0-alpha.128'
    if rel == 'COMPANION_LOCK.json':
        data.setdefault('notes', []).append(
            'Frontier Alpha.128 hardens real-play construction recovery: builders no longer choose arbitrary roofs/log pillars as home positions, grading uses bounded ground-aware approach candidates, and the HUD exposes the immediate stall category. Third-party companion pins/hashes remain unchanged.')
    write(path, json.dumps(data, ensure_ascii=False, indent=2) + '\n')

(ROOT / 'CONSTRUCTION_STALL_RECOVERY_ALPHA128.md').write_text('''# Alpha.128 — Construction stall recovery

Real play showed a warehouse project remaining at 0% while settlement resources were available. The visible construction worker was elevated on settlement architecture.

## Root cause

Builder spawn/recovery selected the highest walkable collision surface around the settlement. In a dense settlement that can be a roof or log pillar rather than ground. A builder on a disconnected elevated surface can exist successfully, allowing construction to start, but have no descending navigation path to the first grading cell. Placement also deliberately accepts natural trees, while the old grading approach fallback checked only a very narrow neighbor ring.

## Fix

- builder home/spawn recovery accepts natural ground or dirt-path support and searches a bounded 24-block ring;
- an existing builder at least three blocks above natural ground is relocated only when no real path back to safe ground exists; carried ItemStacks are preserved;
- grading uses a bounded three-block terrain-ground-aware approach set;
- construction HUD exposes the immediate stall class instead of only generic `막힘`;
- no force loading, virtual materials, repeated teleport loop, or new save field is introduced.

## Validation

Source/docs regression audits and Java 25 compile run before commit. Canonical build/JAR and companion validation run from committed Alpha.128. Real graphical play remains separately required.
''', encoding='utf-8')
