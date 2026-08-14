package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
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
 * Classifies the exact cropped source fragment behind every one of Erden's deterministic urban lots.
 *
 * <p>This catalog is intentionally source-only. It consumes immutable snapshots exposed by
 * {@link ExternalUrbanFabricBuilder}; it never reads or loads a world chunk. A building is promoted
 * to an authored multilevel candidate only when the retained doorway reaches a meaningful upper
 * floor through real one-block vertical transitions and the fragment contains stairs. Ambiguous
 * layouts remain on the compatibility fallback.</p>
 */
public final class ErdenUrbanPlacedTopologyCatalog {
    public static final int CATALOG_REVISION = 2;

    private static final int EXPECTED_BUILDINGS = 233;
    private static final int MIN_FLOOR_CELLS = 12;
    private static final int MIN_GROUND_REACHABLE = 18;
    private static final int MIN_LEVEL_SEPARATION = 3;

    private static final Map<String, FragmentProfile> FRAGMENTS = new LinkedHashMap<>();
    private static final Map<Long, PlacementProfile> PLACEMENTS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private ErdenUrbanPlacedTopologyCatalog() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        FRAGMENTS.clear();
        PLACEMENTS.clear();

        Map<String, ExternalUrbanFabricBuilder.UrbanFragmentSnapshot> snapshots =
                ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics();
        for (ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot : snapshots.values()) {
            FragmentProfile profile = analyze(snapshot);
            FRAGMENTS.put(snapshot.fragmentKey(), profile);
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_FRAGMENT_TOPOLOGY fragment={} resource={} dimensions={}x{}x{} entrance_local={},{} reachable={} vertical_span={} vertical_transitions={} stairs={} doors={} fixtures={} floor_bands={} classification={} source_only=true world_reads=false",
                    snapshot.fragmentKey(), snapshot.resource(),
                    snapshot.width(), snapshot.height(), snapshot.length(),
                    snapshot.entranceX(), snapshot.entranceZ(),
                    profile.reachableCells(), profile.verticalSpan(),
                    profile.verticalTransitions(), profile.stairBlocks(), profile.doors(),
                    profile.functionalFixtures(), profile.floorBands(), profile.classification());
        }

        List<ExternalUrbanFabricBuilder.UrbanBuildingPlacement> placements =
                ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics();
        validatePlacements(placements, snapshots);

        Map<String, Integer> roles = new LinkedHashMap<>();
        Map<Classification, Integer> classifications = new LinkedHashMap<>();
        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement : placements) {
            FragmentProfile fragment = FRAGMENTS.get(placement.fragmentKey());
            if (fragment == null) {
                throw new IllegalStateException("Missing exact Erden fragment profile "
                        + placement.fragmentKey());
            }
            long key = entranceKey(placement.entrance().x(), placement.entrance().z());
            PlacementProfile previous = PLACEMENTS.put(key, new PlacementProfile(
                    placement.role(), placement.resource(), placement.fragmentKey(),
                    placement.rotation(), placement.entrance(),
                    placement.minX(), placement.maxX(), placement.minZ(), placement.maxZ(),
                    placement.baseY(), placement.height(), placement.width(), placement.length(),
                    fragment.classification(), fragment));
            if (previous != null) {
                throw new IllegalStateException("Duplicate Erden urban entrance at "
                        + placement.entrance().x() + "," + placement.entrance().z());
            }
            roles.merge(placement.role(), 1, Integer::sum);
            classifications.merge(fragment.classification(), 1, Integer::sum);
        }

        Map<String, Integer> expectedRoles = ExternalUrbanFabricBuilder.roleCountsForDiagnostics();
        if (!roles.equals(expectedRoles)) {
            throw new IllegalStateException("Erden exact-fragment role counts drifted: actual="
                    + roles + " expected=" + expectedRoles);
        }
        if (PLACEMENTS.size() != EXPECTED_BUILDINGS
                || PLACEMENTS.size() != ExternalUrbanFabricBuilder.plotCount()) {
            throw new IllegalStateException("Erden exact-fragment catalog size drifted: placements="
                    + PLACEMENTS.size() + " expected=" + EXPECTED_BUILDINGS
                    + " builder=" + ExternalUrbanFabricBuilder.plotCount());
        }

        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "Prepared exact Erden placed-fragment topology catalog buildings={} fragments={} classifications={} roles={} source_only=true world_reads=false chunks_loaded=0 placement_counts_unchanged=true revision={}",
                PLACEMENTS.size(), FRAGMENTS.size(), classifications, roles, CATALOG_REVISION);
    }

    public static PlacementProfile profile(int entranceX, int entranceZ) {
        bootstrap();
        return PLACEMENTS.get(entranceKey(entranceX, entranceZ));
    }

    public static Map<Long, PlacementProfile> placements() {
        bootstrap();
        return Map.copyOf(PLACEMENTS);
    }

    public static Map<String, FragmentProfile> fragments() {
        bootstrap();
        return Map.copyOf(FRAGMENTS);
    }

    private static void validatePlacements(
            List<ExternalUrbanFabricBuilder.UrbanBuildingPlacement> placements,
            Map<String, ExternalUrbanFabricBuilder.UrbanFragmentSnapshot> snapshots) {
        if (placements.size() != EXPECTED_BUILDINGS) {
            throw new IllegalStateException("Expected exactly " + EXPECTED_BUILDINGS
                    + " Erden urban placements but found " + placements.size());
        }
        Set<Long> entrances = new HashSet<>();
        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement : placements) {
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot =
                    snapshots.get(placement.fragmentKey());
            if (snapshot == null) {
                throw new IllegalStateException("Placement references unknown Erden fragment "
                        + placement.fragmentKey());
            }
            int expectedWidth = quarterTurn(placement.rotation())
                    ? snapshot.length() : snapshot.width();
            int expectedLength = quarterTurn(placement.rotation())
                    ? snapshot.width() : snapshot.length();
            if (placement.width() != expectedWidth || placement.length() != expectedLength) {
                throw new IllegalStateException("Rotated Erden fragment dimensions drifted for "
                        + placement.fragmentKey() + ": actual=" + placement.width() + "x"
                        + placement.length() + " expected=" + expectedWidth + "x" + expectedLength);
            }
            ExternalUrbanFabricBuilder.UrbanEntrance entrance = placement.entrance();
            if (entrance.x() < placement.minX() || entrance.x() > placement.maxX()
                    || entrance.z() < placement.minZ() || entrance.z() > placement.maxZ()) {
                throw new IllegalStateException("Erden entrance lies outside retained fragment footprint: "
                        + entrance.x() + "," + entrance.z() + " fragment=" + placement.fragmentKey());
            }
            if (!entrances.add(entranceKey(entrance.x(), entrance.z()))) {
                throw new IllegalStateException("Duplicate Erden entrance metadata: "
                        + entrance.x() + "," + entrance.z());
            }
        }
    }

    private static boolean quarterTurn(Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
    }

    private static FragmentProfile analyze(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot) {
        Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks = new HashMap<>();
        int stairs = 0;
        int doors = 0;
        int fixtures = 0;
        int doorY = Integer.MAX_VALUE;
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            blocks.put(localKey(block.x(), block.y(), block.z()), block);
            Block sourceBlock = block.state().getBlock();
            if (sourceBlock instanceof DoorBlock) {
                doors++;
                if (block.x() == snapshot.entranceX() && block.z() == snapshot.entranceZ()) {
                    doorY = Math.min(doorY, block.y());
                }
            }
            // Stair counting is repeated after the retained entrance height is known.
            // Do not impose an arbitrary upper-floor cap here: tall licensed source buildings
            // keep their complete authored Y span in the cropped urban fragment.
            if (functionalFixture(sourceBlock)) fixtures++;
        }

        if (doorY == Integer.MAX_VALUE) {
            return new FragmentProfile(
                    snapshot.fragmentKey(), snapshot.resource(), 0, 0, 0,
                    stairs, doors, fixtures, List.of(), Classification.FALLBACK);
        }

        // Recount stairs now that the exact retained entrance height is known. The first pass above
        // deliberately cannot trust doorY until all door blocks have been observed.
        stairs = 0;
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            if (block.state().getBlock() instanceof StairBlock
                    && interiorSide(snapshot, block.x(), block.z())) {
                stairs++;
            }
        }

        Node seed = findInteriorSeed(snapshot, blocks, doorY);
        if (seed == null) {
            return new FragmentProfile(
                    snapshot.fragmentKey(), snapshot.resource(), 0, 0, 0,
                    stairs, doors, fixtures, List.of(), Classification.FALLBACK);
        }

        ArrayDeque<Node> pending = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        Map<Integer, Integer> reachableByY = new HashMap<>();
        pending.add(seed);
        visited.add(nodeKey(seed.x(), seed.y(), seed.z()));
        int minY = seed.y();
        int maxY = seed.y();
        int verticalTransitions = 0;

        while (!pending.isEmpty()) {
            Node current = pending.removeFirst();
            minY = Math.min(minY, current.y());
            maxY = Math.max(maxY, current.y());
            reachableByY.merge(current.y(), 1, Integer::sum);
            for (int[] offset : DIRECTIONS) {
                int x = current.x() + offset[0];
                int z = current.z() + offset[1];
                if (x < 0 || x >= snapshot.width() || z < 0 || z >= snapshot.length()) continue;
                if (!interiorSide(snapshot, x, z)) continue;
                int feetY = resolveFeetY(snapshot, blocks, x, z, current.y());
                if (feetY == Integer.MIN_VALUE || Math.abs(feetY - current.y()) > 1) continue;
                // The fragment retains the complete source height. Reachability itself is the
                // safety gate, so a valid authored staircase may continue to any retained floor.
                long key = nodeKey(x, feetY, z);
                if (visited.add(key)) {
                    if (feetY != current.y()) verticalTransitions++;
                    pending.addLast(new Node(x, feetY, z));
                }
            }
        }

        List<FloorBand> bands = selectFloorBands(reachableByY, seed.y());
        int verticalSpan = maxY - minY;
        FloorBand ground = bands.stream()
                .filter(band -> Math.abs(band.feetY() - seed.y()) <= 2)
                .max(Comparator.comparingInt(FloorBand::reachableCells))
                .orElse(null);
        final int maximumAuthoredFeetY = snapshot.height() - 2;
        FloorBand upper = ground == null ? null : bands.stream()
                .filter(band -> band.feetY() >= ground.feetY() + MIN_LEVEL_SEPARATION)
                .filter(band -> band.feetY() <= maximumAuthoredFeetY)
                .max(Comparator.comparingInt(FloorBand::reachableCells))
                .orElse(null);

        boolean groundCandidate = ground != null
                && visited.size() >= MIN_GROUND_REACHABLE
                && ground.reachableCells() >= MIN_FLOOR_CELLS;
        boolean multilevel = groundCandidate
                && upper != null
                && stairs > 0
                && verticalSpan >= MIN_LEVEL_SEPARATION
                && verticalTransitions > 0;
        Classification classification = multilevel
                ? Classification.AUTHORED_MULTILEVEL
                : groundCandidate ? Classification.AUTHORED_GROUND_ONLY : Classification.FALLBACK;
        return new FragmentProfile(
                snapshot.fragmentKey(), snapshot.resource(), visited.size(), verticalSpan,
                verticalTransitions, stairs, doors, fixtures, List.copyOf(bands), classification);
    }

    private static Node findInteriorSeed(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int doorY) {
        int[] inward = inward(snapshot.exteriorSide());
        for (int depth = 1; depth <= 3; depth++) {
            int x = snapshot.entranceX() + inward[0] * depth;
            int z = snapshot.entranceZ() + inward[1] * depth;
            if (x < 0 || x >= snapshot.width() || z < 0 || z >= snapshot.length()) continue;
            int feetY = resolveFeetY(snapshot, blocks, x, z, doorY);
            if (feetY != Integer.MIN_VALUE) return new Node(x, feetY, z);
        }
        int feetY = resolveFeetY(
                snapshot, blocks, snapshot.entranceX(), snapshot.entranceZ(), doorY);
        return feetY == Integer.MIN_VALUE
                ? null : new Node(snapshot.entranceX(), feetY, snapshot.entranceZ());
    }

    private static int resolveFeetY(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int x, int z, int preferredFeetY) {
        for (int offset : FEET_OFFSETS) {
            int feetY = preferredFeetY + offset;
            if (walkable(snapshot, blocks, x, feetY, z)) return feetY;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean walkable(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int x, int feetY, int z) {
        if (x < 0 || x >= snapshot.width() || z < 0 || z >= snapshot.length()) return false;
        if (feetY <= 0 || feetY + 1 >= snapshot.height()) return false;
        ExternalUrbanFabricBuilder.UrbanSourceBlock feet = blocks.get(localKey(x, feetY, z));
        ExternalUrbanFabricBuilder.UrbanSourceBlock head = blocks.get(localKey(x, feetY + 1, z));
        ExternalUrbanFabricBuilder.UrbanSourceBlock floor = blocks.get(localKey(x, feetY - 1, z));
        return bodyPassable(feet) && bodyPassable(head) && supportsBody(floor);
    }

    private static boolean bodyPassable(ExternalUrbanFabricBuilder.UrbanSourceBlock block) {
        if (block == null || block.state().isAir()) return true;
        if (block.state().getBlock() instanceof DoorBlock) return true;
        String id = blockId(block.state());
        return id.contains("torch")
                || id.contains("button")
                || id.contains("pressure_plate")
                || id.contains("carpet")
                || id.contains("lantern")
                || id.endsWith("_sign")
                || id.endsWith("_wall_sign");
    }

    private static boolean supportsBody(ExternalUrbanFabricBuilder.UrbanSourceBlock block) {
        if (block == null || block.state().isAir()) return false;
        Block source = block.state().getBlock();
        if (source instanceof DoorBlock) return false;
        String id = blockId(block.state());
        return !(id.equals("minecraft:water") || id.equals("minecraft:lava")
                || id.contains("torch") || id.contains("button")
                || id.contains("pressure_plate") || id.contains("carpet")
                || id.contains("lantern") || id.contains("chain")
                || id.contains("fence") || id.contains("iron_bars")
                || id.contains("glass_pane") || id.endsWith("_sign")
                || id.endsWith("_wall_sign") || id.endsWith("_trapdoor")
                || id.endsWith("_leaves") || id.endsWith("_sapling")
                || id.contains("grass") || id.contains("flower")
                || id.contains("fern") || id.contains("vine"));
    }

    private static List<FloorBand> selectFloorBands(
            Map<Integer, Integer> reachableByY, int seedY) {
        List<Map.Entry<Integer, Integer>> candidates = reachableByY.entrySet().stream()
                .filter(entry -> entry.getValue() >= MIN_FLOOR_CELLS)
                .sorted((left, right) -> {
                    int byCells = Integer.compare(right.getValue(), left.getValue());
                    if (byCells != 0) return byCells;
                    return Integer.compare(
                            Math.abs(left.getKey() - seedY),
                            Math.abs(right.getKey() - seedY));
                })
                .toList();
        List<FloorBand> selected = new ArrayList<>();
        for (Map.Entry<Integer, Integer> candidate : candidates) {
            boolean separated = selected.stream().allMatch(existing ->
                    Math.abs(existing.feetY() - candidate.getKey()) >= MIN_LEVEL_SEPARATION);
            if (separated) selected.add(new FloorBand(candidate.getKey(), candidate.getValue()));
        }
        selected.sort(Comparator.comparingInt(FloorBand::feetY));
        return selected;
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

    private static boolean functionalFixture(Block block) {
        String id = BuiltInRegistries.BLOCK.getKey(block).toString();
        return id.endsWith("_bed")
                || id.equals("minecraft:chest") || id.equals("minecraft:barrel")
                || id.equals("minecraft:crafting_table") || id.equals("minecraft:furnace")
                || id.equals("minecraft:smoker") || id.equals("minecraft:blast_furnace")
                || id.endsWith("_anvil") || id.equals("minecraft:anvil")
                || id.equals("minecraft:lectern") || id.equals("minecraft:bookshelf")
                || id.equals("minecraft:stonecutter") || id.equals("minecraft:smithing_table")
                || id.equals("minecraft:loom") || id.equals("minecraft:cartography_table")
                || id.equals("minecraft:fletching_table") || id.equals("minecraft:grindstone")
                || id.equals("minecraft:composter") || id.endsWith("cauldron");
    }

    private static String blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static long localKey(int x, int y, int z) {
        return ((long) (x & 0x1fffff) << 42)
                ^ ((long) (y & 0x3fffff) << 20)
                ^ (z & 0xfffffL);
    }

    private static long nodeKey(int x, int y, int z) {
        long a = ((long) x & 0x1fffffL) << 43;
        long b = ((long) y & 0x3fffffL) << 21;
        long c = (long) z & 0x1fffffL;
        return a ^ b ^ c;
    }

    private static long entranceKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static final int[][] DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };
    private static final int[] FEET_OFFSETS = {0, 1, -1};

    public enum Classification {
        AUTHORED_MULTILEVEL,
        AUTHORED_GROUND_ONLY,
        FALLBACK
    }

    public record FloorBand(int feetY, int reachableCells) {
    }

    public record FragmentProfile(
            String fragmentKey,
            String resource,
            int reachableCells,
            int verticalSpan,
            int verticalTransitions,
            int stairBlocks,
            int doors,
            int functionalFixtures,
            List<FloorBand> floorBands,
            Classification classification) {
    }

    public record PlacementProfile(
            String role,
            String resource,
            String fragmentKey,
            Rotation rotation,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            int baseY,
            int height,
            int width,
            int length,
            Classification classification,
            FragmentProfile fragment) {
    }

    private record Node(int x, int y, int z) {
    }
}
