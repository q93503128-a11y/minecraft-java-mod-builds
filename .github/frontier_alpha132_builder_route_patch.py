from pathlib import Path

ROOT = Path('projects/frontier-settlement')
CONSTRUCTION = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java'
PROPS = ROOT / 'gradle.properties'
README = ROOT / 'README.md'
SOURCE_TEST = ROOT / 'tools/test_current_source.py'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f'missing patch anchor: {label}')
    if text.count(old) != 1:
        raise SystemExit(f'non-unique patch anchor: {label} ({text.count(old)})')
    return text.replace(old, new, 1)


props = PROPS.read_text(encoding='utf-8')
props = replace_once(props, 'mod_version=0.1.0-alpha.131', 'mod_version=0.1.0-alpha.132', 'version')
PROPS.write_text(props, encoding='utf-8')

construction = CONSTRUCTION.read_text(encoding='utf-8')
construction = replace_once(
    construction,
    '    private static final long SITE_RESERVE_TARGET_PER_CATEGORY = 64L;\n'
    '    private static final long SITE_RESERVE_LOW_WATER = 8L;\n',
    '    private static final long SITE_RESERVE_TARGET_PER_CATEGORY = 64L;\n',
    'retire low-water turnaround threshold',
)

old_material = '''        // Alpha.85 accidentally treated every item consumed from a full reserve as an immediate
        // refill request. A 32 -> 31 transition therefore sent the same builder back to town for
        // exactly one item before another blueprint step could run. Keep physical hauling, but use
        // a low-water mark: initial staging is large, construction continues locally, and another
        // town trip is requested only when the crate is actually running low (or cannot fund the
        // very next transactional placement).
        boolean needsWood = currentWood < nextWoodDelta
                || (remainingWood > currentWood && currentWood <= SITE_RESERVE_LOW_WATER);
        boolean needsStone = currentStone < nextStoneDelta
                || (remainingStone > currentStone && currentStone <= SITE_RESERVE_LOW_WATER);
        long targetWood = Math.min(SITE_RESERVE_TARGET_PER_CATEGORY, remainingWood);
        long targetStone = Math.min(SITE_RESERVE_TARGET_PER_CATEGORY, remainingStone);
'''
new_material = '''        // Alpha.132 keeps the large physical reserve but removes mid-approach low-water turnarounds.
        // Stage a useful reserve once at project start. After visible construction begins, a builder
        // returns to town only when the *next* transactional placement cannot be funded. A refill still
        // targets up to 64 items, so this does not regress to the old one-item shuttle loop.
        long targetWood = Math.min(SITE_RESERVE_TARGET_PER_CATEGORY, remainingWood);
        long targetStone = Math.min(SITE_RESERVE_TARGET_PER_CATEGORY, remainingStone);
        boolean initialStaging = step <= 0;
        boolean needsWood = currentWood < nextWoodDelta
                || (initialStaging && currentWood < targetWood);
        boolean needsStone = currentStone < nextStoneDelta
                || (initialStaging && currentStone < targetStone);
'''
construction = replace_once(construction, old_material, new_material, 'material route hysteresis')

old_grade = '''    private static boolean moveBuilderTowardGradeCell(ServerLevel level, FrontierWorkerEntity builder, BlockPos target) {
        for (BlockPos candidate : gradeApproachPositions(level, builder, target)) {
'''
new_grade = '''    private static boolean moveBuilderTowardGradeCell(ServerLevel level, FrontierWorkerEntity builder, BlockPos target) {
        // Multiple builders can advance the shared grade step while another builder is still walking.
        // Do not replace a valid in-flight vanilla path every tick just because the shared next cell moved.
        if (!builder.getNavigation().isDone()) return true;
        for (BlockPos candidate : gradeApproachPositions(level, builder, target)) {
'''
construction = replace_once(construction, old_grade, new_grade, 'grading route commitment')

old_move = '''        if (builderWithinSiteWorkEnvelope(level, construction, type, builder)) {
            builder.getNavigation().stop();
            return true;
        }
        for (BlockPos work : workPositionsFor(level, construction, type, placement, builder, supply)) {
'''
new_move = '''        if (builderWithinSiteWorkEnvelope(level, construction, type, builder)) {
            builder.getNavigation().stop();
            return true;
        }
        // The shared build step can advance several times while this builder is still approaching.
        // Preserve an in-flight site path instead of continuously retargeting another wall/corner.
        if (!builder.getNavigation().isDone()) return false;
        for (BlockPos work : workPositionsFor(level, construction, type, placement, builder, supply)) {
'''
construction = replace_once(construction, old_move, new_move, 'building route commitment')

old_sort = '''        List<BlockPos> result = new ArrayList<>(unique);
        result.sort(Comparator.comparingDouble(pos -> {
            double tx = (double) pos.getX() - target.getX();
            double tz = (double) pos.getZ() - target.getZ();
            double targetHorizontal = tx * tx + tz * tz;
            return targetHorizontal * 4.0D + builder.distanceToSqr(
                    pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        }));
        return List.copyOf(result);
'''
new_sort = '''        List<BlockPos> result = new ArrayList<>(unique);
        // Every candidate below belongs to the same complete perimeter set. Prefer the physically
        // nearest stable entry cell, not the latest blueprint block, so another builder advancing the
        // shared step cannot make an approaching resident reverse direction across the site.
        result.sort(Comparator
                .comparingDouble((BlockPos pos) -> builder.distanceToSqr(
                        pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D))
                .thenComparingLong(BlockPos::asLong));
        return List.copyOf(result);
'''
construction = replace_once(construction, old_sort, new_sort, 'stable site entry ordering')
CONSTRUCTION.write_text(construction, encoding='utf-8')

readme = README.read_text(encoding='utf-8')
readme = replace_once(readme, '## Current version: 0.1.0-alpha.131', '## Current version: 0.1.0-alpha.132', 'README version')
anchor = '## Alpha.131 — Deep-work return recovery and performance smoothing\n'
section = '''## Alpha.132 construction-route commitment\n\nReal-play feedback showed construction residents visibly starting toward a building and then reversing. The shared build/grade step can advance while another resident is still walking, and the old approach chooser weighted the newest blueprint cell heavily enough to replace that resident's path with a different side of the site. Low-water reserve refills could also pre-empt an approach even though the site still had enough material for the next placement. Alpha.132 keeps one valid in-flight route until it finishes, uses the nearest stable ground perimeter entry rather than the newest block as the approach priority, stages the initial 64-item reserve once, and after work begins refills only when the next paid placement cannot proceed. Physical hauling, actual ItemStack costs, safe-ground site authority and multiplayer server authority are unchanged.\n\n'''
if section not in readme:
    if anchor in readme:
        readme = readme.replace(anchor, section + anchor, 1)
    else:
        marker = '## Alpha.131'
        idx = readme.find(marker)
        if idx < 0:
            raise SystemExit('README Alpha.131 anchor missing')
        readme = readme[:idx] + section + readme[idx:]
README.write_text(readme, encoding='utf-8')

source_test = SOURCE_TEST.read_text(encoding='utf-8')
source_test = replace_once(source_test, 'mod_version=0.1.0-alpha.131', 'mod_version=0.1.0-alpha.132', 'source audit version')
needle = '''require("builderStrandedOnArtificialElevation" in construction and "builderOnArtificialElevation" in construction
        and "nearestNaturalGroundBelow" in construction and "return artificialRise >= 3;" in construction,
        "disconnected elevated builder recovery missing")
'''
extra = needle + '''require("Preserve an in-flight site path" in construction
        and "thenComparingLong(BlockPos::asLong)" in construction,
        "construction workers can again thrash between changing blueprint-side approach targets")
require("initialStaging = step <= 0" in construction and "SITE_RESERVE_LOW_WATER" not in construction,
        "construction coordinator can again reverse mid-approach for proactive low-water refills")
require("Do not replace a valid in-flight vanilla path every tick" in construction,
        "grading workers can again replace an active path whenever another builder advances the grade step")
'''
source_test = replace_once(source_test, needle, extra, 'source audit route guards')
SOURCE_TEST.write_text(source_test, encoding='utf-8')
