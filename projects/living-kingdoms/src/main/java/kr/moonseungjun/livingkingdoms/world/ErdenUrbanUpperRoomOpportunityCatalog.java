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
 * Explains why the strict source-shell planner found no safe upper floor and looks for a safer pivot.
 *
 * <p>Two source-only opportunities are measured independently: an existing supported upper room
 * whose body space is already empty, and a genuinely empty enclosed volume where a new floor could
 * later be authored. The catalog never edits blocks or reads world chunks. Rejection counters are
 * retained so a zero result is actionable evidence rather than an invitation to relax constraints
 * blindly.</p>
 */
public final class ErdenUrbanUpperRoomOpportunityCatalog {
    public static final int CATALOG_REVISION = 3;

    private static final int EDGE_MARGIN = 2;
    private static final int MIN_UPPER_RISE = 4;
    private static final int MAX_UPPER_RISE = 16;
    private static final int MIN_REGION_CELLS = 12;
    private static final int MIN_USABLE_CELLS = 12;
    private static final int MAX_WALL_RAY = 18;
    private static final int MAX_ROOF_RAY = 20;

    private static final Map<String, OpportunityProfile> PROFILES = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private ErdenUrbanUpperRoomOpportunityCatalog() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        PROFILES.clear();
        Map<String, ExternalUrbanFabricBuilder.UrbanFragmentSnapshot> snapshots =
                ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics();
        Map<String, ErdenUrbanPlacedTopologyCatalog.FragmentProfile> topology =
                ErdenUrbanPlacedTopologyCatalog.fragments();

        for (ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot : snapshots.values()) {
            ErdenUrbanPlacedTopologyCatalog.FragmentProfile exact = topology.get(snapshot.fragmentKey());
            if (exact == null) {
                throw new IllegalStateException("Missing exact topology for Erden upper-room audit "
                        + snapshot.fragmentKey());
            }
            OpportunityProfile profile = analyze(snapshot, exact);
            PROFILES.put(snapshot.fragmentKey(), profile);
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_UPPER_ROOM_OPPORTUNITY fragment={} exact_classification={} ground_y={} existing_level={} existing_cells={} existing_regions={} existing_candidates={} new_level={} new_cells={} new_regions={} rejection={} recommendation={} source_only=true world_reads=false mutations=0",
                    snapshot.fragmentKey(), exact.classification(), profile.groundFeetY(),
                    profile.existingFloor().feetY(), profile.existingFloor().usableCells(),
                    profile.existingFloor().regions().size(), profile.existingFloors().size(),
                    profile.newFloorVoid().feetY(), profile.newFloorVoid().usableCells(),
                    profile.newFloorVoid().regions().size(), profile.rejections(),
                    profile.recommendation());
        }

        Map<Recommendation, Integer> placementRecommendations = new LinkedHashMap<>();
        int mapped = 0;
        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement
                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {
            OpportunityProfile profile = PROFILES.get(placement.fragmentKey());
            if (profile == null) {
                throw new IllegalStateException("Missing Erden upper-room profile for placed fragment "
                        + placement.fragmentKey());
            }
            placementRecommendations.merge(profile.recommendation(), 1, Integer::sum);
            mapped++;
        }
        if (mapped != 233 || mapped != ExternalUrbanFabricBuilder.plotCount()) {
            throw new IllegalStateException("Erden upper-room opportunity placement drift: " + mapped);
        }

        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden upper-room opportunity catalog fragments={} buildings={} recommendations={} source_only=true world_reads=false mutations=0 placement_counts_unchanged=true revision={}",
                PROFILES.size(), mapped, placementRecommendations, CATALOG_REVISION);
    }

    public static OpportunityProfile profile(String fragmentKey) {
        bootstrap();
        return PROFILES.get(fragmentKey);
    }

    public static Map<String, OpportunityProfile> profiles() {
        bootstrap();
        return Map.copyOf(PROFILES);
    }

    private static OpportunityProfile analyze(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            ErdenUrbanPlacedTopologyCatalog.FragmentProfile topology) {
        Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks = new HashMap<>();
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            blocks.put(blockKey(block.x(), block.y(), block.z()), block);
        }
        int doorY = retainedDoorY(snapshot);
        int groundY = resolveGroundFeetY(topology, doorY);
        if (groundY == Integer.MIN_VALUE) {
            return new OpportunityProfile(
                    snapshot.fragmentKey(), groundY,
                    LevelOpportunity.none(FloorMode.EXISTING_SOURCE_FLOOR), List.of(),
                    LevelOpportunity.none(FloorMode.NEW_AUTHORED_FLOOR),
                    new RejectionStats(0, 0, 0, 0, 0, 0, 0), Recommendation.NO_SAFE_ROOM);
        }

        MutableRejections rejected = new MutableRejections();
        List<LevelOpportunity> existingFloors = new ArrayList<>();
        LevelOpportunity bestNew = LevelOpportunity.none(FloorMode.NEW_AUTHORED_FLOOR);
        int maximumY = Math.min(snapshot.height() - 2, groundY + MAX_UPPER_RISE);
        for (int feetY = groundY + MIN_UPPER_RISE; feetY <= maximumY; feetY++) {
            Set<Long> existing = new HashSet<>();
            Set<Long> newFloor = new HashSet<>();
            for (int x = EDGE_MARGIN; x < snapshot.width() - EDGE_MARGIN; x++) {
                for (int z = EDGE_MARGIN; z < snapshot.length() - EDGE_MARGIN; z++) {
                    if (!interiorSide(snapshot, x, z)) {
                        rejected.outsideInterior++;
                        continue;
                    }
                    if (!sourceAir(blocks, x, feetY, z)) {
                        rejected.bodyBlocked++;
                        continue;
                    }
                    if (!sourceAir(blocks, x, feetY + 1, z)) {
                        rejected.headBlocked++;
                        continue;
                    }
                    if (!roofAbove(blocks, x, feetY + 2, z, snapshot.height())) {
                        rejected.noRoof++;
                        continue;
                    }
                    if (!enclosedAtBody(blocks, snapshot, x, feetY, z)) {
                        rejected.notEnclosed++;
                        continue;
                    }

                    ExternalUrbanFabricBuilder.UrbanSourceBlock floor =
                            blocks.get(blockKey(x, feetY - 1, z));
                    if (supportsFloor(floor)) {
                        existing.add(cellKey(x, z));
                        rejected.existingSupported++;
                    } else if (floor == null || floor.state().isAir()) {
                        newFloor.add(cellKey(x, z));
                        rejected.newFloorVoid++;
                    } else {
                        rejected.unsuitableFloor++;
                    }
                }
            }
            LevelOpportunity existingLevel = opportunity(
                    FloorMode.EXISTING_SOURCE_FLOOR, feetY, existing);
            LevelOpportunity newLevel = opportunity(
                    FloorMode.NEW_AUTHORED_FLOOR, feetY, newFloor);
            if (existingLevel.usableCells() >= MIN_USABLE_CELLS) {
                existingFloors.add(existingLevel);
            }
            if (better(newLevel, bestNew)) bestNew = newLevel;
        }

        existingFloors.sort(
                Comparator.comparingInt(LevelOpportunity::usableCells).reversed()
                        .thenComparingInt(LevelOpportunity::feetY));
        List<LevelOpportunity> immutableExistingFloors = List.copyOf(existingFloors);
        LevelOpportunity bestExisting = immutableExistingFloors.isEmpty()
                ? LevelOpportunity.none(FloorMode.EXISTING_SOURCE_FLOOR)
                : immutableExistingFloors.get(0);

        Recommendation recommendation;
        if (!immutableExistingFloors.isEmpty()) {
            recommendation = Recommendation.ROUTE_TO_EXISTING_ROOM;
        } else if (bestNew.usableCells() >= MIN_USABLE_CELLS) {
            recommendation = Recommendation.AUTHOR_NEW_FLOOR_IN_VOID;
        } else {
            recommendation = Recommendation.NO_SAFE_ROOM;
        }
        return new OpportunityProfile(
                snapshot.fragmentKey(), groundY, bestExisting, immutableExistingFloors, bestNew,
                rejected.freeze(), recommendation);
    }

    private static LevelOpportunity opportunity(FloorMode mode, int feetY, Set<Long> cells) {
        List<Region> regions = regions(cells).stream()
                .filter(region -> region.cells().size() >= MIN_REGION_CELLS)
                .sorted(Comparator.comparingInt((Region region) -> region.cells().size()).reversed())
                .toList();
        int usable = regions.stream().mapToInt(region -> region.cells().size()).sum();
        return new LevelOpportunity(mode, feetY, usable, List.copyOf(regions));
    }

    private static boolean better(LevelOpportunity candidate, LevelOpportunity current) {
        if (candidate.usableCells() != current.usableCells()) {
            return candidate.usableCells() > current.usableCells();
        }
        if (candidate.usableCells() == 0) return false;
        return current.feetY() == Integer.MIN_VALUE || candidate.feetY() < current.feetY();
    }

    private static List<Region> regions(Set<Long> candidates) {
        Set<Long> remaining = new HashSet<>(candidates);
        List<Region> result = new ArrayList<>();
        while (!remaining.isEmpty()) {
            long seed = remaining.iterator().next();
            remaining.remove(seed);
            List<Long> region = new ArrayList<>();
            ArrayDeque<Long> pending = new ArrayDeque<>();
            pending.add(seed);
            while (!pending.isEmpty()) {
                long current = pending.removeFirst();
                region.add(current);
                int x = cellX(current);
                int z = cellZ(current);
                for (int[] direction : DIRECTIONS) {
                    long next = cellKey(x + direction[0], z + direction[1]);
                    if (remaining.remove(next)) pending.addLast(next);
                }
            }
            result.add(new Region(List.copyOf(region)));
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

    private static int resolveGroundFeetY(
            ErdenUrbanPlacedTopologyCatalog.FragmentProfile topology, int doorY) {
        if (!topology.floorBands().isEmpty()) {
            ErdenUrbanPlacedTopologyCatalog.FloorBand best = topology.floorBands().stream()
                    .min(Comparator.comparingInt(band -> Math.abs(band.feetY() - doorY)))
                    .orElse(null);
            if (best != null) return best.feetY();
        }
        return doorY;
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
            if (structuralBarrier(blocks.get(blockKey(x, y, z)))) return true;
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
                        || probeZ < 0 || probeZ >= snapshot.length()) break;
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

    private static boolean supportsFloor(ExternalUrbanFabricBuilder.UrbanSourceBlock block) {
        if (!structuralBarrier(block)) return false;
        String id = blockId(block.state());
        return !(id.contains("fence") || id.contains("wall") || id.contains("iron_bars")
                || id.contains("glass_pane") || id.contains("chain")
                || id.endsWith("_trapdoor") || id.endsWith("_door"));
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

    public enum FloorMode {
        EXISTING_SOURCE_FLOOR,
        NEW_AUTHORED_FLOOR
    }

    public enum Recommendation {
        ROUTE_TO_EXISTING_ROOM,
        AUTHOR_NEW_FLOOR_IN_VOID,
        NO_SAFE_ROOM
    }

    public record Region(List<Long> cells) {
    }

    public record LevelOpportunity(
            FloorMode mode, int feetY, int usableCells, List<Region> regions) {
        static LevelOpportunity none(FloorMode mode) {
            return new LevelOpportunity(mode, Integer.MIN_VALUE, 0, List.of());
        }
    }

    public record RejectionStats(
            long outsideInterior,
            long bodyBlocked,
            long headBlocked,
            long noRoof,
            long notEnclosed,
            long unsuitableFloor,
            long acceptedFloorCells) {
    }

    public record OpportunityProfile(
            String fragmentKey,
            int groundFeetY,
            LevelOpportunity existingFloor,
            List<LevelOpportunity> existingFloors,
            LevelOpportunity newFloorVoid,
            RejectionStats rejections,
            Recommendation recommendation) {
    }

    private static final class MutableRejections {
        long outsideInterior;
        long bodyBlocked;
        long headBlocked;
        long noRoof;
        long notEnclosed;
        long unsuitableFloor;
        long existingSupported;
        long newFloorVoid;

        RejectionStats freeze() {
            return new RejectionStats(
                    outsideInterior, bodyBlocked, headBlocked, noRoof,
                    notEnclosed, unsuitableFloor, existingSupported + newFloorVoid);
        }
    }
}
