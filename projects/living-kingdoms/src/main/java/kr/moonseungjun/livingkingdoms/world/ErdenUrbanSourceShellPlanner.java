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
 * Read-only planner for richer interiors that stay inside the exact imported urban shell.
 *
 * <p>The current cropped facade kits do not prove a source-native upper-floor route. Rather than
 * guessing another fixed box, this planner treats every retained source block as immutable and looks
 * only for source-air volumes that are demonstrably enclosed by the real fragment. It can therefore
 * tell a later streamed converter where a new floor and stairwell could be authored without touching
 * facade, roof or any imported architectural block.</p>
 */
public final class ErdenUrbanSourceShellPlanner {
    public static final int PLANNER_REVISION = 1;

    private static final int EDGE_MARGIN = 2;
    private static final int MIN_UPPER_REGION_CELLS = 24;
    private static final int MIN_ROOM_REGION_CELLS = 12;
    private static final int MAX_UPPER_RISE = 12;
    private static final int MIN_UPPER_RISE = 4;
    private static final int MAX_WALL_RAY = 16;
    private static final int MAX_ROOF_RAY = 18;
    private static final int SHAFT_HALF_WIDTH = 1;

    private static final Map<String, ShellPlan> PLANS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private ErdenUrbanSourceShellPlanner() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        PLANS.clear();

        Map<String, ExternalUrbanFabricBuilder.UrbanFragmentSnapshot> snapshots =
                ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics();
        Map<String, ErdenUrbanPlacedTopologyCatalog.FragmentProfile> topology =
                ErdenUrbanPlacedTopologyCatalog.fragments();

        for (ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot : snapshots.values()) {
            ErdenUrbanPlacedTopologyCatalog.FragmentProfile fragment = topology.get(snapshot.fragmentKey());
            if (fragment == null) {
                throw new IllegalStateException("Missing exact topology before Erden shell planning: "
                        + snapshot.fragmentKey());
            }
            ShellPlan plan = plan(snapshot, fragment);
            PLANS.put(snapshot.fragmentKey(), plan);
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_SOURCE_SHELL_PLAN fragment={} source_classification={} ground_y={} candidate_upper_y={} usable_upper_cells={} room_regions={} shaft_candidates={} protected_blocks={} shell_classification={} source_only=true world_reads=false mutations=0",
                    plan.fragmentKey(), fragment.classification(), plan.groundFeetY(),
                    plan.upperFeetY(), plan.usableUpperCells(), plan.roomRegions().size(),
                    plan.stairShaftCandidates().size(), plan.protectedBlocks(), plan.classification());
        }

        Map<ShellClassification, Integer> placementCounts = new LinkedHashMap<>();
        int mapped = 0;
        int usableUpperCells = 0;
        int shaftCandidates = 0;
        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement
                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {
            ShellPlan plan = PLANS.get(placement.fragmentKey());
            if (plan == null) {
                throw new IllegalStateException("Missing Erden shell plan for placed fragment "
                        + placement.fragmentKey());
            }
            placementCounts.merge(plan.classification(), 1, Integer::sum);
            usableUpperCells += plan.usableUpperCells();
            shaftCandidates += plan.stairShaftCandidates().size();
            mapped++;
        }
        if (mapped != ExternalUrbanFabricBuilder.plotCount() || mapped != 233) {
            throw new IllegalStateException("Erden shell planner placement mapping drifted: " + mapped);
        }

        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden source-shell interior plans fragments={} buildings={} classifications={} mapped_upper_cells={} mapped_shaft_candidates={} protected_source_blocks=true source_only=true world_reads=false placement_counts_unchanged=true revision={}",
                PLANS.size(), mapped, placementCounts, usableUpperCells, shaftCandidates,
                PLANNER_REVISION);
    }

    public static ShellPlan plan(String fragmentKey) {
        bootstrap();
        return PLANS.get(fragmentKey);
    }

    public static Map<String, ShellPlan> plans() {
        bootstrap();
        return Map.copyOf(PLANS);
    }

    private static ShellPlan plan(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            ErdenUrbanPlacedTopologyCatalog.FragmentProfile topology) {
        Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks = blockMap(snapshot.blocks());
        int protectedBlocks = blocks.size();
        int doorY = retainedDoorY(snapshot);
        int groundY = groundFeetY(topology, doorY);
        if (groundY == Integer.MIN_VALUE
                || topology.classification() == ErdenUrbanPlacedTopologyCatalog.Classification.FALLBACK) {
            return ShellPlan.none(snapshot.fragmentKey(), protectedBlocks);
        }

        Set<Long> reachableGround = reachableGround(snapshot, blocks, groundY);
        CandidateLevel best = null;
        int maximumUpper = Math.min(snapshot.height() - 3, groundY + MAX_UPPER_RISE);
        for (int upperY = groundY + MIN_UPPER_RISE; upperY <= maximumUpper; upperY++) {
            Set<Long> candidates = new HashSet<>();
            for (int x = EDGE_MARGIN; x < snapshot.width() - EDGE_MARGIN; x++) {
                for (int z = EDGE_MARGIN; z < snapshot.length() - EDGE_MARGIN; z++) {
                    if (!interiorSide(snapshot, x, z)) continue;
                    if (!sourceAir(blocks, x, upperY - 1, z)
                            || !sourceAir(blocks, x, upperY, z)
                            || !sourceAir(blocks, x, upperY + 1, z)) {
                        continue;
                    }
                    if (!roofAbove(blocks, x, upperY + 2, z, snapshot.height())) continue;
                    if (!enclosedAtBody(blocks, snapshot, x, upperY, z)) continue;
                    candidates.add(cellKey(x, z));
                }
            }
            List<RoomRegion> regions = regions(candidates);
            int usable = regions.stream()
                    .filter(region -> region.cells().size() >= MIN_ROOM_REGION_CELLS)
                    .mapToInt(region -> region.cells().size())
                    .sum();
            int largest = regions.stream().mapToInt(region -> region.cells().size()).max().orElse(0);
            if (largest < MIN_UPPER_REGION_CELLS) continue;
            CandidateLevel candidate = new CandidateLevel(upperY, usable, regions, candidates);
            if (best == null || candidate.usableCells() > best.usableCells()
                    || candidate.usableCells() == best.usableCells()
                    && candidate.upperFeetY() < best.upperFeetY()) {
                best = candidate;
            }
        }

        if (best == null) {
            return new ShellPlan(
                    snapshot.fragmentKey(), groundY, Integer.MIN_VALUE, 0,
                    List.of(), List.of(), protectedBlocks, ShellClassification.NO_SAFE_UPPER);
        }

        List<StairShaftCandidate> shafts = stairShafts(
                snapshot, blocks, reachableGround, groundY, best);
        ShellClassification classification = shafts.isEmpty()
                ? ShellClassification.UPPER_ONLY : ShellClassification.UPPER_AND_SHAFT;
        List<RoomRegion> retainedRegions = best.regions().stream()
                .filter(region -> region.cells().size() >= MIN_ROOM_REGION_CELLS)
                .sorted(Comparator.comparingInt((RoomRegion region) -> region.cells().size()).reversed())
                .toList();
        return new ShellPlan(
                snapshot.fragmentKey(), groundY, best.upperFeetY(), best.usableCells(),
                List.copyOf(retainedRegions), List.copyOf(shafts), protectedBlocks, classification);
    }

    private static Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blockMap(
            List<ExternalUrbanFabricBuilder.UrbanSourceBlock> source) {
        Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> result = new HashMap<>();
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : source) {
            result.put(blockKey(block.x(), block.y(), block.z()), block);
        }
        return result;
    }

    private static int retainedDoorY(ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot) {
        int result = Integer.MAX_VALUE;
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            if (block.x() == snapshot.entranceX() && block.z() == snapshot.entranceZ()
                    && block.state().getBlock() instanceof DoorBlock) {
                result = Math.min(result, block.y());
            }
        }
        return result == Integer.MAX_VALUE ? Integer.MIN_VALUE : result;
    }

    private static int groundFeetY(
            ErdenUrbanPlacedTopologyCatalog.FragmentProfile topology, int doorY) {
        if (doorY == Integer.MIN_VALUE || topology.floorBands().isEmpty()) return Integer.MIN_VALUE;
        int bestY = Integer.MIN_VALUE;
        int bestDistance = Integer.MAX_VALUE;
        int bestCells = -1;
        for (ErdenUrbanPlacedTopologyCatalog.FloorBand band : topology.floorBands()) {
            int distance = Math.abs(band.feetY() - doorY);
            if (distance < bestDistance || distance == bestDistance
                    && band.reachableCells() > bestCells) {
                bestY = band.feetY();
                bestDistance = distance;
                bestCells = band.reachableCells();
            }
        }
        return bestY;
    }

    private static Set<Long> reachableGround(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int groundY) {
        int[] inward = inward(snapshot.exteriorSide());
        Cell seed = null;
        for (int depth = 1; depth <= 4; depth++) {
            int x = snapshot.entranceX() + inward[0] * depth;
            int z = snapshot.entranceZ() + inward[1] * depth;
            if (walkableAt(blocks, snapshot, x, groundY, z)) {
                seed = new Cell(x, z);
                break;
            }
        }
        if (seed == null && walkableAt(
                blocks, snapshot, snapshot.entranceX(), groundY, snapshot.entranceZ())) {
            seed = new Cell(snapshot.entranceX(), snapshot.entranceZ());
        }
        if (seed == null) return Set.of();

        Set<Long> visited = new HashSet<>();
        ArrayDeque<Cell> pending = new ArrayDeque<>();
        visited.add(cellKey(seed.x(), seed.z()));
        pending.add(seed);
        while (!pending.isEmpty()) {
            Cell current = pending.removeFirst();
            for (int[] direction : DIRECTIONS) {
                int x = current.x() + direction[0];
                int z = current.z() + direction[1];
                if (x < 0 || x >= snapshot.width() || z < 0 || z >= snapshot.length()) continue;
                if (!interiorSide(snapshot, x, z)) continue;
                if (!walkableAt(blocks, snapshot, x, groundY, z)) continue;
                long key = cellKey(x, z);
                if (visited.add(key)) pending.addLast(new Cell(x, z));
            }
        }
        return Set.copyOf(visited);
    }

    private static boolean walkableAt(
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            int x, int feetY, int z) {
        if (x < 0 || x >= snapshot.width() || z < 0 || z >= snapshot.length()) return false;
        if (feetY <= 0 || feetY + 1 >= snapshot.height()) return false;
        return bodyPassable(blocks.get(blockKey(x, feetY, z)))
                && bodyPassable(blocks.get(blockKey(x, feetY + 1, z)))
                && supportsBody(blocks.get(blockKey(x, feetY - 1, z)));
    }

    private static boolean sourceAir(
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks, int x, int y, int z) {
        ExternalUrbanFabricBuilder.UrbanSourceBlock block = blocks.get(blockKey(x, y, z));
        return block == null || block.state().isAir();
    }

    private static boolean roofAbove(
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int x, int startY, int z, int height) {
        int maximum = Math.min(height - 1, startY + MAX_ROOF_RAY);
        for (int y = startY; y <= maximum; y++) {
            ExternalUrbanFabricBuilder.UrbanSourceBlock block = blocks.get(blockKey(x, y, z));
            if (structuralBarrier(block)) return true;
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
                int probeX = x + direction[0] * distance;
                int probeZ = z + direction[1] * distance;
                if (probeX < 0 || probeX >= snapshot.width()
                        || probeZ < 0 || probeZ >= snapshot.length()) {
                    break;
                }
                ExternalUrbanFabricBuilder.UrbanSourceBlock feet =
                        blocks.get(blockKey(probeX, feetY, probeZ));
                ExternalUrbanFabricBuilder.UrbanSourceBlock head =
                        blocks.get(blockKey(probeX, feetY + 1, probeZ));
                if (structuralBarrier(feet) && structuralBarrier(head)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private static List<RoomRegion> regions(Set<Long> candidates) {
        Set<Long> remaining = new HashSet<>(candidates);
        List<RoomRegion> result = new ArrayList<>();
        while (!remaining.isEmpty()) {
            long seed = remaining.iterator().next();
            remaining.remove(seed);
            Set<Long> cells = new HashSet<>();
            ArrayDeque<Long> pending = new ArrayDeque<>();
            cells.add(seed);
            pending.add(seed);
            while (!pending.isEmpty()) {
                long current = pending.removeFirst();
                int x = cellX(current);
                int z = cellZ(current);
                for (int[] direction : DIRECTIONS) {
                    long next = cellKey(x + direction[0], z + direction[1]);
                    if (remaining.remove(next)) {
                        cells.add(next);
                        pending.addLast(next);
                    }
                }
            }
            result.add(new RoomRegion(List.copyOf(cells)));
        }
        return result;
    }

    private static List<StairShaftCandidate> stairShafts(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            Set<Long> reachableGround,
            int groundY,
            CandidateLevel upper) {
        if (reachableGround.isEmpty()) return List.of();
        List<StairShaftCandidate> result = new ArrayList<>();
        Set<Long> upperCells = upper.candidates();
        for (long cell : upperCells) {
            int x = cellX(cell);
            int z = cellZ(cell);
            if (!reachableGround.contains(cellKey(x, z))) continue;
            if (x - SHAFT_HALF_WIDTH < EDGE_MARGIN || x + SHAFT_HALF_WIDTH >= snapshot.width() - EDGE_MARGIN
                    || z - SHAFT_HALF_WIDTH < EDGE_MARGIN || z + SHAFT_HALF_WIDTH >= snapshot.length() - EDGE_MARGIN) {
                continue;
            }
            if (!supportsBody(blocks.get(blockKey(x, groundY - 1, z)))) continue;
            if (!clearShaft(blocks, x, z, groundY, upper.upperFeetY() + 1)) continue;
            result.add(new StairShaftCandidate(x, z, groundY, upper.upperFeetY()));
            if (result.size() >= 12) break;
        }
        return result;
    }

    private static boolean clearShaft(
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int centerX, int centerZ, int minimumY, int maximumY) {
        for (int x = centerX - SHAFT_HALF_WIDTH; x <= centerX + SHAFT_HALF_WIDTH; x++) {
            for (int z = centerZ - SHAFT_HALF_WIDTH; z <= centerZ + SHAFT_HALF_WIDTH; z++) {
                for (int y = minimumY; y <= maximumY; y++) {
                    if (!sourceAir(blocks, x, y, z)) return false;
                }
            }
        }
        return true;
    }

    private static boolean structuralBarrier(ExternalUrbanFabricBuilder.UrbanSourceBlock block) {
        if (block == null || block.state().isAir()) return false;
        Block source = block.state().getBlock();
        if (source instanceof DoorBlock) return false;
        String id = blockId(block.state());
        return !(id.contains("torch") || id.contains("button")
                || id.contains("pressure_plate") || id.contains("carpet")
                || id.contains("lantern") || id.contains("chain")
                || id.endsWith("_sign") || id.endsWith("_wall_sign")
                || id.endsWith("_trapdoor") || id.endsWith("_leaves")
                || id.endsWith("_sapling") || id.contains("grass")
                || id.contains("flower") || id.contains("fern") || id.contains("vine"));
    }

    private static boolean bodyPassable(ExternalUrbanFabricBuilder.UrbanSourceBlock block) {
        if (block == null || block.state().isAir()) return true;
        if (block.state().getBlock() instanceof DoorBlock) return true;
        String id = blockId(block.state());
        return id.contains("torch") || id.contains("button")
                || id.contains("pressure_plate") || id.contains("carpet")
                || id.contains("lantern") || id.endsWith("_sign")
                || id.endsWith("_wall_sign");
    }

    private static boolean supportsBody(ExternalUrbanFabricBuilder.UrbanSourceBlock block) {
        if (!structuralBarrier(block)) return false;
        String id = blockId(block.state());
        return !(id.contains("fence") || id.contains("iron_bars")
                || id.contains("glass_pane") || id.contains("wall"));
    }

    private static String blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
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

    private static int[] inward(String exteriorSide) {
        return switch (exteriorSide) {
            case "NORTH" -> new int[]{0, 1};
            case "SOUTH" -> new int[]{0, -1};
            case "WEST" -> new int[]{1, 0};
            case "EAST" -> new int[]{-1, 0};
            default -> throw new IllegalArgumentException("Unknown Erden exterior side " + exteriorSide);
        };
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

    public enum ShellClassification {
        UPPER_AND_SHAFT,
        UPPER_ONLY,
        NO_SAFE_UPPER
    }

    public record StairShaftCandidate(int x, int z, int groundFeetY, int upperFeetY) {
    }

    public record RoomRegion(List<Long> cells) {
    }

    public record ShellPlan(
            String fragmentKey,
            int groundFeetY,
            int upperFeetY,
            int usableUpperCells,
            List<RoomRegion> roomRegions,
            List<StairShaftCandidate> stairShaftCandidates,
            int protectedBlocks,
            ShellClassification classification) {
        static ShellPlan none(String fragmentKey, int protectedBlocks) {
            return new ShellPlan(
                    fragmentKey, Integer.MIN_VALUE, Integer.MIN_VALUE, 0,
                    List.of(), List.of(), protectedBlocks, ShellClassification.NO_SAFE_UPPER);
        }
    }

    private record CandidateLevel(
            int upperFeetY,
            int usableCells,
            List<RoomRegion> regions,
            Set<Long> candidates) {
    }

    private record Cell(int x, int z) {
    }
}
