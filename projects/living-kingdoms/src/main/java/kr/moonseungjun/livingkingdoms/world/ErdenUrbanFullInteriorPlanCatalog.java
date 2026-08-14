package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Source-only plan for turning Erden's retained decorative building shells into complete playable
 * interiors without cutting imported blocks.
 *
 * <p>The licensed schematics are intentionally treated as immutable architecture. A planned authored
 * floor is accepted only where its floor plane, feet and head cells are source air, the body volume is
 * enclosed by retained structural walls, and a retained roof exists above. A source-air floor already
 * proved by the upper-room opportunity audit is retained as a seed. Further floors must form a real
 * vertical stack: a normal storey gap and substantial X/Z overlap with an already accepted lower level.
 * This avoids both the former false rejection of free-spanning timber floors and arbitrary floating
 * plates in towers or courtyards. Existing supported source floors remain first-class levels. This
 * catalog never reads or mutates a world chunk; it is geometry input for the later multi-floor
 * route/materializer.</p>
 */
public final class ErdenUrbanFullInteriorPlanCatalog {
    public static final int CATALOG_REVISION = 2;

    private static final int EDGE_MARGIN = 2;
    private static final int MIN_UPPER_RISE = 4;
    private static final int MIN_FLOOR_SEPARATION = 5;
    private static final int MIN_REGION_CELLS = 20;
    private static final int MIN_LEVEL_CELLS = 28;
    private static final int MIN_VERTICAL_OVERLAP_CELLS = 20;
    private static final double MIN_VERTICAL_OVERLAP_RATIO = 0.35D;
    private static final int MIN_VERTICAL_LEVEL_GAP = 5;
    private static final int MAX_VERTICAL_LEVEL_GAP = 9;
    private static final int MAX_WALL_RAY = 18;

    private static final Map<String, InteriorPlan> PLANS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private ErdenUrbanFullInteriorPlanCatalog() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        PLANS.clear();

        Map<String, ExternalUrbanFabricBuilder.UrbanFragmentSnapshot> snapshots =
                ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics();
        Map<String, ErdenUrbanUpperRoomOpportunityCatalog.OpportunityProfile> opportunities =
                ErdenUrbanUpperRoomOpportunityCatalog.profiles();

        for (ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot : snapshots.values()) {
            ErdenUrbanUpperRoomOpportunityCatalog.OpportunityProfile opportunity =
                    opportunities.get(snapshot.fragmentKey());
            if (opportunity == null) {
                throw new IllegalStateException("Missing upper-room evidence for full interior plan "
                        + snapshot.fragmentKey());
            }
            InteriorPlan plan = analyze(snapshot, opportunity);
            PLANS.put(snapshot.fragmentKey(), plan);
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_FULL_INTERIOR_PLAN fragment={} ground_y={} retained_height={} existing_levels={} authored_candidates={} selected_authored_levels={} total_upper_levels={} planned_rooms={} authored_floor_cells={} shell_anchors={} classification={} source_blocks_cut=0 source_only=true world_reads=false mutations=0",
                    snapshot.fragmentKey(), plan.groundFeetY(), snapshot.height(),
                    levelYs(plan.existingLevels()), plan.authoredCandidates().size(),
                    levelYs(plan.selectedAuthoredLevels()), plan.totalUpperLevels(),
                    plan.plannedRooms(), plan.authoredFloorCells(), plan.shellAnchors(),
                    plan.classification());
        }

        int mapped = 0;
        int buildingsWithUpper = 0;
        int buildingsWithTwoOrMoreUpper = 0;
        int plannedUpperLevels = 0;
        int plannedRooms = 0;
        Map<Classification, Integer> classifications = new LinkedHashMap<>();
        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement
                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {
            InteriorPlan plan = PLANS.get(placement.fragmentKey());
            if (plan == null) {
                throw new IllegalStateException("Missing full interior plan for placed fragment "
                        + placement.fragmentKey());
            }
            mapped++;
            classifications.merge(plan.classification(), 1, Integer::sum);
            if (plan.totalUpperLevels() >= 1) buildingsWithUpper++;
            if (plan.totalUpperLevels() >= 2) buildingsWithTwoOrMoreUpper++;
            plannedUpperLevels += plan.totalUpperLevels();
            plannedRooms += plan.plannedRooms();
        }
        if (mapped != 233 || mapped != ExternalUrbanFabricBuilder.plotCount()) {
            throw new IllegalStateException("Erden full-interior placement count drifted: " + mapped);
        }

        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden full interior source plans fragments={} buildings={} buildings_with_upper={} buildings_with_two_or_more_upper={} planned_upper_levels={} planned_rooms={} classifications={} source_blocks_cut=0 source_only=true world_reads=false mutations=0 plots=233 housing=77 work=156 revision={}",
                PLANS.size(), mapped, buildingsWithUpper, buildingsWithTwoOrMoreUpper,
                plannedUpperLevels, plannedRooms, classifications, CATALOG_REVISION);
    }

    public static InteriorPlan plan(String fragmentKey) {
        bootstrap();
        return PLANS.get(fragmentKey);
    }

    public static Map<String, InteriorPlan> plans() {
        bootstrap();
        return Map.copyOf(PLANS);
    }

    private static InteriorPlan analyze(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            ErdenUrbanUpperRoomOpportunityCatalog.OpportunityProfile opportunity) {
        int groundY = opportunity.groundFeetY();
        if (groundY == Integer.MIN_VALUE) {
            return new InteriorPlan(snapshot.fragmentKey(), groundY, List.of(), List.of(), List.of(),
                    0, 0, 0, Classification.NO_SAFE_INTERIOR_PLAN);
        }

        Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks = new HashMap<>();
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            blocks.put(blockKey(block.x(), block.y(), block.z()), block);
        }

        List<PlannedLevel> existing = opportunity.existingFloors().stream()
                .filter(level -> level.usableCells() >= 12)
                .map(level -> new PlannedLevel(
                        LevelKind.EXISTING_SOURCE_FLOOR,
                        level.feetY(),
                        level.usableCells(),
                        level.regions().stream()
                                .map(region -> new PlannedRegion(region.cells(), 0))
                                .toList(),
                        0))
                .sorted(Comparator.comparingInt(PlannedLevel::feetY))
                .toList();

        List<PlannedLevel> seededNew = new ArrayList<>();
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
        int rooms = existing.stream().mapToInt(level -> level.regions().size()).sum()
                + selected.stream().mapToInt(level -> level.regions().size()).sum();
        int authoredCells = selected.stream().mapToInt(PlannedLevel::usableCells).sum();
        int anchors = selected.stream().mapToInt(PlannedLevel::shellAnchors).sum();
        int totalUpper = existing.size() + selected.size();
        Classification classification = totalUpper >= 2
                ? Classification.MULTI_UPPER_PLAN
                : totalUpper == 1 ? Classification.SINGLE_UPPER_PLAN
                : Classification.NO_SAFE_INTERIOR_PLAN;
        return new InteriorPlan(
                snapshot.fragmentKey(), groundY,
                List.copyOf(existing), List.copyOf(candidates), List.copyOf(selected),
                rooms, authoredCells, anchors, classification);
    }

    private static List<PlannedLevel> selectVerticallyContinuousLevels(
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

    private static boolean tooCloseToExisting(int feetY, List<PlannedLevel> existing) {
        for (PlannedLevel level : existing) {
            if (Math.abs(level.feetY() - feetY) < MIN_FLOOR_SEPARATION) return true;
        }
        return false;
    }

    private static List<PlannedRegion> regions(
            Set<Long> cells,
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int feetY) {
        Set<Long> remaining = new HashSet<>(cells);
        List<PlannedRegion> result = new ArrayList<>();
        while (!remaining.isEmpty()) {
            long seed = remaining.iterator().next();
            remaining.remove(seed);
            List<Long> regionCells = new ArrayList<>();
            ArrayDeque<Long> pending = new ArrayDeque<>();
            pending.add(seed);
            while (!pending.isEmpty()) {
                long current = pending.removeFirst();
                regionCells.add(current);
                int x = cellX(current);
                int z = cellZ(current);
                for (int[] direction : DIRECTIONS) {
                    long next = cellKey(x + direction[0], z + direction[1]);
                    if (remaining.remove(next)) pending.addLast(next);
                }
            }
            int anchors = shellAnchors(snapshot, blocks, feetY, regionCells);
            result.add(new PlannedRegion(List.copyOf(regionCells), anchors));
        }
        return result;
    }

    private static int shellAnchors(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int feetY,
            List<Long> regionCells) {
        Set<Long> region = new HashSet<>(regionCells);
        int anchors = 0;
        for (long cell : regionCells) {
            int x = cellX(cell);
            int z = cellZ(cell);
            for (int[] direction : DIRECTIONS) {
                int nx = x + direction[0];
                int nz = z + direction[1];
                if (region.contains(cellKey(nx, nz))) continue;
                if (retainedBarrier(snapshot, blocks, nx, feetY - 1, nz)
                        || retainedBarrier(snapshot, blocks, nx, feetY, nz)
                        || retainedBarrier(snapshot, blocks, nx, feetY + 1, nz)) {
                    anchors++;
                }
            }
        }
        return anchors;
    }

    private static boolean retainedRoofAbove(
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            int x, int startY, int z) {
        for (int y = startY; y < snapshot.height(); y++) {
            if (retainedBarrier(snapshot, blocks, x, y, z)) return true;
        }
        return false;
    }

    private static boolean enclosedAtBody(
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            int x, int feetY, int z) {
        for (int[] direction : DIRECTIONS) {
            boolean found = false;
            for (int distance = 1; distance <= MAX_WALL_RAY; distance++) {
                int nx = x + direction[0] * distance;
                int nz = z + direction[1] * distance;
                if (nx < 0 || nx >= snapshot.width() || nz < 0 || nz >= snapshot.length()) break;
                if (retainedBarrier(snapshot, blocks, nx, feetY, nz)
                        && retainedBarrier(snapshot, blocks, nx, feetY + 1, nz)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private static boolean retainedBarrier(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int x, int y, int z) {
        if (x < 0 || x >= snapshot.width() || z < 0 || z >= snapshot.length()
                || y < 0 || y >= snapshot.height()) {
            return false;
        }
        ExternalUrbanFabricBuilder.UrbanSourceBlock block = blocks.get(blockKey(x, y, z));
        if (!structuralBarrier(block)) return false;
        return !ErdenUrbanSyntheticSealProvenance.isClearableSourceAirSeal(
                snapshot.fragmentKey(), x, y, z);
    }

    private static boolean sourceAir(
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int x, int y, int z) {
        ExternalUrbanFabricBuilder.UrbanSourceBlock block = blocks.get(blockKey(x, y, z));
        return block == null || block.state().isAir();
    }

    private static boolean structuralBarrier(ExternalUrbanFabricBuilder.UrbanSourceBlock block) {
        if (block == null || block.state().isAir()) return false;
        Block source = block.state().getBlock();
        if (source instanceof DoorBlock) return false;
        String id = blockId(block.state());
        return !(id.equals("minecraft:water") || id.equals("minecraft:lava")
                || id.contains("torch") || id.contains("button")
                || id.contains("pressure_plate") || id.contains("carpet")
                || id.contains("lantern") || id.contains("chain")
                || id.endsWith("_sign") || id.endsWith("_wall_sign")
                || id.endsWith("_leaves") || id.endsWith("_sapling")
                || id.contains("grass") || id.contains("flower")
                || id.contains("fern") || id.contains("vine"));
    }

    private static boolean interiorSide(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot, int x, int z) {
        return switch (snapshot.exteriorSide()) {
            case "NORTH" -> z >= snapshot.entranceZ();
            case "SOUTH" -> z <= snapshot.entranceZ();
            case "WEST" -> x >= snapshot.entranceX();
            case "EAST" -> x <= snapshot.entranceX();
            default -> false;
        };
    }

    private static List<Integer> levelYs(List<PlannedLevel> levels) {
        return levels.stream().map(PlannedLevel::feetY).toList();
    }

    private static String blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static long blockKey(int x, int y, int z) {
        return ((long) (x & 0x1fffff) << 42)
                ^ ((long) (y & 0x3fffff) << 20)
                ^ (z & 0xfffffL);
    }

    private static long cellKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static int cellX(long key) {
        return (int) (key >> 32);
    }

    private static int cellZ(long key) {
        return (int) key;
    }

    private static final int[][] DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    public enum LevelKind {
        EXISTING_SOURCE_FLOOR,
        NEW_AUTHORED_FLOOR
    }

    public enum Classification {
        MULTI_UPPER_PLAN,
        SINGLE_UPPER_PLAN,
        NO_SAFE_INTERIOR_PLAN
    }

    private record CandidateContinuity(
            PlannedLevel candidate,
            PlannedLevel lower,
            int gap,
            int overlapCells,
            double overlapRatio) {
    }

    public record PlannedRegion(List<Long> cells, int shellAnchors) {
    }

    public record PlannedLevel(
            LevelKind kind,
            int feetY,
            int usableCells,
            List<PlannedRegion> regions,
            int shellAnchors) {
    }

    public record InteriorPlan(
            String fragmentKey,
            int groundFeetY,
            List<PlannedLevel> existingLevels,
            List<PlannedLevel> authoredCandidates,
            List<PlannedLevel> selectedAuthoredLevels,
            int plannedRooms,
            int authoredFloorCells,
            int shellAnchors,
            Classification classification) {
        public int totalUpperLevels() {
            return existingLevels.size() + selectedAuthoredLevels.size();
        }
    }
}
