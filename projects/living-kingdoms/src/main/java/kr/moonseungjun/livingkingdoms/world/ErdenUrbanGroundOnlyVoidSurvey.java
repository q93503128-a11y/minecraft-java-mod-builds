package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Read-only survey for the 77 Erden buildings that intentionally remain ground-only.
 *
 * <p>The legacy interior converter clears a fixed 7x9x4 box. Before replacing that behavior we need
 * to know how much usable room can be created without deleting a single imported source block. This
 * survey therefore treats immutable schematic AIR as the only buildable volume. It measures cells at
 * the retained entrance level whose body/head are source AIR and whose floor either already exists or
 * can be authored into source AIR with nearby structural support. A roof requirement keeps candidate
 * cells inside the retained building shell. No world chunk is read or loaded and no mutation occurs.</p>
 */
public final class ErdenUrbanGroundOnlyVoidSurvey {
    public static final int SURVEY_REVISION = 1;
    public static final int EXPECTED_PLACEMENTS = 77;

    private static final int SUPPORT_PROBE_DEPTH = 6;
    private static final int MAX_ROOF_DISTANCE = 16;
    private static final Map<String, FragmentSurvey> FRAGMENTS = new LinkedHashMap<>();
    private static final Map<Long, PlacementSurvey> PLACEMENTS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private ErdenUrbanGroundOnlyVoidSurvey() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        FRAGMENTS.clear();
        PLACEMENTS.clear();

        Map<String, ExternalUrbanFabricBuilder.UrbanFragmentSnapshot> snapshots =
                ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics();
        Map<String, Integer> roles = new LinkedHashMap<>();

        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement
                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {
            if (ErdenUrbanAuthoredUpperRouteManager.isEligible(placement.entrance())) continue;
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot =
                    snapshots.get(placement.fragmentKey());
            if (snapshot == null) {
                throw new IllegalStateException("Missing ground-only Erden source fragment "
                        + placement.fragmentKey());
            }
            FragmentSurvey fragment = FRAGMENTS.computeIfAbsent(
                    placement.fragmentKey(), ignored -> analyze(snapshot));
            long key = entranceKey(placement.entrance().x(), placement.entrance().z());
            PlacementSurvey previous = PLACEMENTS.put(key, new PlacementSurvey(
                    placement.role(), placement.fragmentKey(), placement.entrance(), fragment));
            if (previous != null) {
                throw new IllegalStateException("Duplicate ground-only Erden entrance "
                        + placement.entrance().x() + "," + placement.entrance().z());
            }
            roles.merge(placement.role(), 1, Integer::sum);
        }

        if (PLACEMENTS.size() != EXPECTED_PLACEMENTS) {
            throw new IllegalStateException("Erden ground-only placement count drifted: expected="
                    + EXPECTED_PLACEMENTS + " actual=" + PLACEMENTS.size());
        }
        int roleTotal = roles.values().stream().mapToInt(Integer::intValue).sum();
        if (roleTotal != EXPECTED_PLACEMENTS) {
            throw new IllegalStateException("Erden ground-only role count drifted: " + roles);
        }

        int minConnected = Integer.MAX_VALUE;
        int minDeepest = Integer.MAX_VALUE;
        int totalPotentialInfills = 0;
        for (FragmentSurvey fragment : FRAGMENTS.values()) {
            minConnected = Math.min(minConnected, fragment.connectedCells());
            minDeepest = Math.min(minDeepest, fragment.deepestInteriorDepth());
            totalPotentialInfills += fragment.connectedFloorInfillCells();
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_GROUND_ONLY_VOID_SURVEY fragment={} door_y={} body_air_cells={} supportable_cells={} roofed_supportable_cells={} connected_cells={} connected_existing_floor_cells={} connected_floor_infill_cells={} bbox={}x{} deepest_interior_depth={} support_anchor_cells={} source_air_body=true source_air_floor_only=true source_blocks_cut=0 source_only=true world_reads=false chunks_loaded=0 mutations=0",
                    fragment.fragmentKey(), fragment.doorY(), fragment.bodyAirCells(),
                    fragment.supportableCells(), fragment.roofedSupportableCells(),
                    fragment.connectedCells(), fragment.connectedExistingFloorCells(),
                    fragment.connectedFloorInfillCells(), fragment.connectedWidth(),
                    fragment.connectedDepth(), fragment.deepestInteriorDepth(),
                    fragment.supportAnchorCells());
        }

        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_GROUND_ONLY_VOID_SURVEY_PASS placements={} fragments={} roles={} minimum_connected_cells={} minimum_deepest_depth={} potential_floor_infills={} source_blocks_cut=0 source_only=true world_reads=false chunks_loaded=0 mutations=0 plots=233 housing=77 work=156 revision={}",
                PLACEMENTS.size(), FRAGMENTS.size(), roles,
                minConnected == Integer.MAX_VALUE ? 0 : minConnected,
                minDeepest == Integer.MAX_VALUE ? 0 : minDeepest,
                totalPotentialInfills, SURVEY_REVISION);
    }

    public static Map<Long, PlacementSurvey> placements() {
        bootstrap();
        return Map.copyOf(PLACEMENTS);
    }

    public static PlacementSurvey placement(int entranceX, int entranceZ) {
        bootstrap();
        return PLACEMENTS.get(entranceKey(entranceX, entranceZ));
    }

    public static Map<String, FragmentSurvey> fragments() {
        bootstrap();
        return Map.copyOf(FRAGMENTS);
    }

    private static FragmentSurvey analyze(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot) {
        Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks = new HashMap<>();
        int doorY = Integer.MAX_VALUE;
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            blocks.put(blockKey(block.x(), block.y(), block.z()), block);
            if (block.x() == snapshot.entranceX()
                    && block.z() == snapshot.entranceZ()
                    && block.state().getBlock() instanceof DoorBlock) {
                doorY = Math.min(doorY, block.y());
            }
        }
        if (doorY == Integer.MAX_VALUE) {
            throw new IllegalStateException("Ground-only source fragment has no retained entrance door "
                    + snapshot.fragmentKey());
        }

        Set<Long> roofedCandidates = new HashSet<>();
        int bodyAirCells = 0;
        int supportableCells = 0;
        int roofedSupportableCells = 0;
        int supportAnchorCells = 0;

        for (int x = 0; x < snapshot.width(); x++) {
            for (int z = 0; z < snapshot.length(); z++) {
                if (!interiorSide(snapshot, x, z)) continue;
                if (!sourceAir(blocks.get(blockKey(x, doorY, z)))
                        || !sourceAir(blocks.get(blockKey(x, doorY + 1, z)))) {
                    continue;
                }
                bodyAirCells++;
                ExternalUrbanFabricBuilder.UrbanSourceBlock floor =
                        blocks.get(blockKey(x, doorY - 1, z));
                boolean existingFloor = structural(floor);
                boolean sourceAirFloor = sourceAir(floor);
                boolean anchored = sourceAirFloor
                        && hasSupportAnchor(blocks, x, doorY - 1, z);
                if (anchored) supportAnchorCells++;
                if (!existingFloor && !anchored) continue;
                supportableCells++;
                if (!roofed(blocks, snapshot.height(), x, doorY, z)) continue;
                roofedSupportableCells++;
                roofedCandidates.add(cellKey(x, z));
            }
        }

        int[] inward = inward(snapshot.exteriorSide());
        Cell seed = null;
        for (int depth = 1; depth <= 4 && seed == null; depth++) {
            int x = snapshot.entranceX() + inward[0] * depth;
            int z = snapshot.entranceZ() + inward[1] * depth;
            if (roofedCandidates.contains(cellKey(x, z))) seed = new Cell(x, z);
        }
        if (seed == null) {
            throw new IllegalStateException("Ground-only source fragment has no zero-cut interior seed "
                    + snapshot.fragmentKey());
        }

        ArrayDeque<Cell> pending = new ArrayDeque<>();
        Set<Long> connected = new HashSet<>();
        pending.add(seed);
        connected.add(cellKey(seed.x(), seed.z()));
        while (!pending.isEmpty()) {
            Cell current = pending.removeFirst();
            for (int[] direction : DIRECTIONS) {
                int x = current.x() + direction[0];
                int z = current.z() + direction[1];
                long key = cellKey(x, z);
                if (!roofedCandidates.contains(key) || !connected.add(key)) continue;
                pending.addLast(new Cell(x, z));
            }
        }

        int existingFloorCells = 0;
        int infillFloorCells = 0;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int deepest = 0;
        for (long key : connected) {
            int x = cellX(key);
            int z = cellZ(key);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
            deepest = Math.max(deepest,
                    (x - snapshot.entranceX()) * inward[0]
                            + (z - snapshot.entranceZ()) * inward[1]);
            ExternalUrbanFabricBuilder.UrbanSourceBlock floor =
                    blocks.get(blockKey(x, doorY - 1, z));
            if (structural(floor)) existingFloorCells++;
            else infillFloorCells++;
        }

        return new FragmentSurvey(
                snapshot.fragmentKey(), doorY, bodyAirCells, supportableCells,
                roofedSupportableCells, connected.size(), existingFloorCells,
                infillFloorCells, connected.isEmpty() ? 0 : maxX - minX + 1,
                connected.isEmpty() ? 0 : maxZ - minZ + 1,
                deepest, supportAnchorCells, Set.copyOf(connected));
    }

    private static boolean sourceAir(ExternalUrbanFabricBuilder.UrbanSourceBlock block) {
        return block == null || block.state().isAir();
    }

    private static boolean roofed(
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int height, int x, int feetY, int z) {
        int maximum = Math.min(height - 1, feetY + MAX_ROOF_DISTANCE);
        for (int y = feetY + 2; y <= maximum; y++) {
            if (structural(blocks.get(blockKey(x, y, z)))) return true;
        }
        return false;
    }

    private static boolean hasSupportAnchor(
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int x, int floorY, int z) {
        for (int depth = 1; depth <= SUPPORT_PROBE_DEPTH; depth++) {
            if (structural(blocks.get(blockKey(x, floorY - depth, z)))) return true;
        }
        for (int[] direction : DIRECTIONS) {
            if (structural(blocks.get(blockKey(
                    x + direction[0], floorY, z + direction[1])))) return true;
        }
        return false;
    }

    private static boolean structural(ExternalUrbanFabricBuilder.UrbanSourceBlock block) {
        if (block == null || block.state().isAir()) return false;
        Block source = block.state().getBlock();
        if (source instanceof DoorBlock) return false;
        String id = BuiltInRegistries.BLOCK.getKey(source).toString();
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

    private static boolean interiorSide(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            int x, int z) {
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

    private static long entranceKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
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

    public record FragmentSurvey(
            String fragmentKey,
            int doorY,
            int bodyAirCells,
            int supportableCells,
            int roofedSupportableCells,
            int connectedCells,
            int connectedExistingFloorCells,
            int connectedFloorInfillCells,
            int connectedWidth,
            int connectedDepth,
            int deepestInteriorDepth,
            int supportAnchorCells,
            Set<Long> connectedLocalCells) {
    }

    public record PlacementSurvey(
            String role,
            String fragmentKey,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance,
            FragmentSurvey fragment) {
    }

    private record Cell(int x, int z) {
    }
}
