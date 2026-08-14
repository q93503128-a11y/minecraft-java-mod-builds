from pathlib import Path

p = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenUrbanFullInteriorPlanCatalog.java')
s = p.read_text(encoding='utf-8')


def once(old: str, new: str, label: str) -> None:
    global s
    count = s.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected one match, found {count}')
    s = s.replace(old, new, 1)

once('public static final int CATALOG_REVISION = 1;',
     'public static final int CATALOG_REVISION = 2;', 'revision')
once(
'''    private static final int MIN_REGION_CELLS = 20;
    private static final int MIN_LEVEL_CELLS = 28;
    private static final int MIN_SHELL_ANCHORS = 6;
    private static final int MAX_WALL_RAY = 18;
''',
'''    private static final int MIN_REGION_CELLS = 20;
    private static final int MIN_LEVEL_CELLS = 28;
    private static final int MIN_VERTICAL_OVERLAP_CELLS = 20;
    private static final double MIN_VERTICAL_OVERLAP_RATIO = 0.35D;
    private static final int MIN_VERTICAL_LEVEL_GAP = 5;
    private static final int MAX_VERTICAL_LEVEL_GAP = 9;
    private static final int MAX_WALL_RAY = 18;
''', 'continuity constants')
once(
''' * floor is accepted only where its floor plane, feet and head cells are source air, the body volume is
 * enclosed by retained structural walls, and a retained roof exists above. Large connected regions
 * must also touch the real shell at several boundary points so floating plates in towers or courtyards
 * are rejected. Existing supported source floors are kept as first-class levels. This catalog never
 * reads or mutates a world chunk; it is geometry input for the later multi-floor route/materializer.</p>
''',
''' * floor is accepted only where its floor plane, feet and head cells are source air, the body volume is
 * enclosed by retained structural walls, and a retained roof exists above. A source-air floor already
 * proved by the upper-room opportunity audit is retained as a seed. Further floors must form a real
 * vertical stack: a normal storey gap and substantial X/Z overlap with an already accepted lower level.
 * This avoids both the former false rejection of free-spanning timber floors and arbitrary floating
 * plates in towers or courtyards. Existing supported source floors remain first-class levels. This
 * catalog never reads or mutates a world chunk; it is geometry input for the later multi-floor
 * route/materializer.</p>
''', 'documentation')
old_analyze = '''        List<PlannedLevel> candidates = new ArrayList<>();
        int maximumY = snapshot.height() - 4;
        for (int feetY = groundY + MIN_UPPER_RISE; feetY <= maximumY; feetY++) {
            if (tooCloseToExisting(feetY, existing)) continue;
            Set<Long> cells = new HashSet<>();
            for (int x = EDGE_MARGIN; x < snapshot.width() - EDGE_MARGIN; x++) {
                for (int z = EDGE_MARGIN; z < snapshot.length() - EDGE_MARGIN; z++) {
                    if (!interiorSide(snapshot, x, z)) continue;
                    if (!sourceAir(blocks, x, feetY - 1, z)
                            || !sourceAir(blocks, x, feetY, z)
                            || !sourceAir(blocks, x, feetY + 1, z)) {
                        continue;
                    }
                    if (!retainedRoofAbove(blocks, snapshot, x, feetY + 2, z)) continue;
                    if (!enclosedAtBody(blocks, snapshot, x, feetY, z)) continue;
                    cells.add(cellKey(x, z));
                }
            }

            List<PlannedRegion> regions = regions(cells, snapshot, blocks, feetY).stream()
                    .filter(region -> region.cells().size() >= MIN_REGION_CELLS)
                    .filter(region -> region.shellAnchors() >= MIN_SHELL_ANCHORS)
                    .sorted(Comparator.comparingInt((PlannedRegion region) -> region.cells().size()).reversed())
                    .toList();
            int usable = regions.stream().mapToInt(region -> region.cells().size()).sum();
            int anchors = regions.stream().mapToInt(PlannedRegion::shellAnchors).sum();
            if (usable >= MIN_LEVEL_CELLS) {
                candidates.add(new PlannedLevel(
                        LevelKind.NEW_AUTHORED_FLOOR, feetY, usable, List.copyOf(regions), anchors));
            }
        }

        List<PlannedLevel> selected = selectIndependentLevels(candidates, existing, groundY);
'''
new_analyze = '''        List<PlannedLevel> seededNew = new ArrayList<>();
        ErdenUrbanUpperRoomOpportunityCatalog.LevelOpportunity provenNew = opportunity.newFloorVoid();
        if (opportunity.recommendation()
                == ErdenUrbanUpperRoomOpportunityCatalog.Recommendation.AUTHOR_NEW_FLOOR_IN_VOID
                && provenNew.feetY() != Integer.MIN_VALUE
                && provenNew.usableCells() >= MIN_LEVEL_CELLS) {
            List<PlannedRegion> seedRegions = provenNew.regions().stream()
                    .filter(region -> region.cells().size() >= MIN_REGION_CELLS)
                    .map(region -> new PlannedRegion(
                            region.cells(), shellAnchors(snapshot, blocks, provenNew.feetY(), region.cells())))
                    .toList();
            int seedCells = seedRegions.stream().mapToInt(region -> region.cells().size()).sum();
            if (seedCells >= MIN_LEVEL_CELLS) {
                int seedAnchors = seedRegions.stream().mapToInt(PlannedRegion::shellAnchors).sum();
                seededNew.add(new PlannedLevel(
                        LevelKind.NEW_AUTHORED_FLOOR, provenNew.feetY(), seedCells,
                        List.copyOf(seedRegions), seedAnchors));
            }
        }

        List<PlannedLevel> fixedLevels = new ArrayList<>(existing);
        fixedLevels.addAll(seededNew);

        List<PlannedLevel> candidates = new ArrayList<>();
        int maximumY = snapshot.height() - 4;
        for (int feetY = groundY + MIN_UPPER_RISE; feetY <= maximumY; feetY++) {
            if (tooCloseToExisting(feetY, fixedLevels)) continue;
            Set<Long> cells = new HashSet<>();
            for (int x = EDGE_MARGIN; x < snapshot.width() - EDGE_MARGIN; x++) {
                for (int z = EDGE_MARGIN; z < snapshot.length() - EDGE_MARGIN; z++) {
                    if (!interiorSide(snapshot, x, z)) continue;
                    if (!sourceAir(blocks, x, feetY - 1, z)
                            || !sourceAir(blocks, x, feetY, z)
                            || !sourceAir(blocks, x, feetY + 1, z)) {
                        continue;
                    }
                    if (!retainedRoofAbove(blocks, snapshot, x, feetY + 2, z)) continue;
                    if (!enclosedAtBody(blocks, snapshot, x, feetY, z)) continue;
                    cells.add(cellKey(x, z));
                }
            }

            List<PlannedRegion> regions = regions(cells, snapshot, blocks, feetY).stream()
                    .filter(region -> region.cells().size() >= MIN_REGION_CELLS)
                    .sorted(Comparator.comparingInt((PlannedRegion region) -> region.cells().size()).reversed())
                    .toList();
            int usable = regions.stream().mapToInt(region -> region.cells().size()).sum();
            int anchors = regions.stream().mapToInt(PlannedRegion::shellAnchors).sum();
            if (usable >= MIN_LEVEL_CELLS) {
                candidates.add(new PlannedLevel(
                        LevelKind.NEW_AUTHORED_FLOOR, feetY, usable, List.copyOf(regions), anchors));
            }
        }

        List<PlannedLevel> selectedExtensions = selectVerticallyContinuousLevels(candidates, fixedLevels);
        List<PlannedLevel> selected = new ArrayList<>(seededNew);
        selected.addAll(selectedExtensions);
        selected.sort(Comparator.comparingInt(PlannedLevel::feetY));
'''
once(old_analyze, new_analyze, 'analysis and seeded continuity')
start = s.index('    private static List<PlannedLevel> selectIndependentLevels(')
end = s.index('    private static boolean tooCloseToExisting(', start)
replacement = '''    private static List<PlannedLevel> selectVerticallyContinuousLevels(
            List<PlannedLevel> candidates,
            List<PlannedLevel> fixedLevels) {
        List<PlannedLevel> accepted = new ArrayList<>(fixedLevels);
        List<PlannedLevel> selected = new ArrayList<>();
        Set<Integer> consumedY = new HashSet<>();

        while (true) {
            CandidateContinuity best = null;
            for (PlannedLevel candidate : candidates) {
                if (consumedY.contains(candidate.feetY())
                        || tooCloseToExisting(candidate.feetY(), accepted)) {
                    continue;
                }
                CandidateContinuity continuity = bestContinuity(candidate, accepted);
                if (continuity == null) continue;
                if (best == null || betterContinuity(continuity, best)) best = continuity;
            }
            if (best == null) break;
            accepted.add(best.candidate());
            selected.add(best.candidate());
            consumedY.add(best.candidate().feetY());
        }
        selected.sort(Comparator.comparingInt(PlannedLevel::feetY));
        return List.copyOf(selected);
    }

    private static CandidateContinuity bestContinuity(
            PlannedLevel candidate, List<PlannedLevel> accepted) {
        CandidateContinuity best = null;
        for (PlannedLevel lower : accepted) {
            int gap = candidate.feetY() - lower.feetY();
            if (gap < MIN_VERTICAL_LEVEL_GAP || gap > MAX_VERTICAL_LEVEL_GAP) continue;
            int overlap = overlapCells(candidate, lower);
            int denominator = Math.max(1, Math.min(candidate.usableCells(), lower.usableCells()));
            double ratio = overlap / (double) denominator;
            if (overlap < MIN_VERTICAL_OVERLAP_CELLS || ratio < MIN_VERTICAL_OVERLAP_RATIO) continue;
            CandidateContinuity continuity = new CandidateContinuity(candidate, lower, gap, overlap, ratio);
            if (best == null || betterForSameCandidate(continuity, best)) best = continuity;
        }
        return best;
    }

    private static boolean betterContinuity(CandidateContinuity candidate, CandidateContinuity current) {
        if (candidate.candidate().feetY() != current.candidate().feetY()) {
            return candidate.candidate().feetY() < current.candidate().feetY();
        }
        if (candidate.overlapCells() != current.overlapCells()) {
            return candidate.overlapCells() > current.overlapCells();
        }
        return candidate.candidate().usableCells() > current.candidate().usableCells();
    }

    private static boolean betterForSameCandidate(
            CandidateContinuity candidate, CandidateContinuity current) {
        if (candidate.overlapRatio() != current.overlapRatio()) {
            return candidate.overlapRatio() > current.overlapRatio();
        }
        if (candidate.overlapCells() != current.overlapCells()) {
            return candidate.overlapCells() > current.overlapCells();
        }
        return candidate.gap() < current.gap();
    }

    private static int overlapCells(PlannedLevel first, PlannedLevel second) {
        Set<Long> cells = new HashSet<>();
        for (PlannedRegion region : first.regions()) cells.addAll(region.cells());
        int overlap = 0;
        Set<Long> counted = new HashSet<>();
        for (PlannedRegion region : second.regions()) {
            for (long cell : region.cells()) {
                if (cells.contains(cell) && counted.add(cell)) overlap++;
            }
        }
        return overlap;
    }

'''
s = s[:start] + replacement + s[end:]
record_anchor = '''    public record PlannedRegion(List<Long> cells, int shellAnchors) {
    }
'''
record_insert = '''    private record CandidateContinuity(
            PlannedLevel candidate,
            PlannedLevel lower,
            int gap,
            int overlapCells,
            double overlapRatio) {
    }

    public record PlannedRegion(List<Long> cells, int shellAnchors) {
    }
'''
once(record_anchor, record_insert, 'continuity record')

p.write_text(s, encoding='utf-8')
print('Patched full interior planning with proven seeds and vertical continuity')
