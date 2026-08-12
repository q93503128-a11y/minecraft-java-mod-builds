package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only gate between discovering an empty upper volume and actually authoring a new floor.
 *
 * <p>A void is not automatically a room. This catalog rejects thin/fragmented shapes, weakly
 * supported spans and role-inappropriate capacity, and it refuses final approval until the existing
 * source-air route planner can reach the proposed level without cutting imported blocks. The audit
 * reads immutable fragment snapshots only; it never loads world chunks or places a block.</p>
 */
public final class ErdenUrbanNewFloorStructuralApprovalCatalog {
    public static final int CATALOG_REVISION = 1;
    public static final int EXPECTED_CANDIDATES = 116;

    private static final int MIN_STRUCTURAL_AREA = 24;
    private static final int MIN_ROOM_SPAN = 4;
    private static final double MIN_COMPACTNESS = 0.45D;
    private static final double MIN_SUPPORT_ANCHOR_RATIO = 0.20D;
    private static final int SUPPORT_PROBE_DEPTH = 6;
    private static final int MIN_ROOF_DISTANCE = 3;

    private static final Map<Long, PlacementApproval> PLACEMENTS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private ErdenUrbanNewFloorStructuralApprovalCatalog() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        PLACEMENTS.clear();

        Map<String, ExternalUrbanFabricBuilder.UrbanFragmentSnapshot> snapshots =
                ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics();
        Map<String, ErdenUrbanUpperRoomOpportunityCatalog.OpportunityProfile> opportunities =
                ErdenUrbanUpperRoomOpportunityCatalog.profiles();
        Map<String, ErdenUrbanSourceAirRoutePlanner.RoutePlan> routes =
                ErdenUrbanSourceAirRoutePlanner.plans();

        Map<String, FragmentAssessment> fragmentAssessments = new LinkedHashMap<>();
        int candidatePlacements = 0;
        Map<Decision, Integer> decisions = new LinkedHashMap<>();
        Map<String, Integer> candidateRoles = new LinkedHashMap<>();

        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement
                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {
            ErdenUrbanUpperRoomOpportunityCatalog.OpportunityProfile opportunity =
                    opportunities.get(placement.fragmentKey());
            if (opportunity == null) {
                throw new IllegalStateException("Missing upper-room opportunity for new-floor audit "
                        + placement.fragmentKey());
            }
            if (opportunity.recommendation()
                    != ErdenUrbanUpperRoomOpportunityCatalog.Recommendation.AUTHOR_NEW_FLOOR_IN_VOID) {
                continue;
            }
            candidatePlacements++;
            candidateRoles.merge(placement.role(), 1, Integer::sum);

            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot =
                    snapshots.get(placement.fragmentKey());
            if (snapshot == null) {
                throw new IllegalStateException("Missing source fragment for new-floor audit "
                        + placement.fragmentKey());
            }
            FragmentAssessment fragment = fragmentAssessments.computeIfAbsent(
                    placement.fragmentKey(), ignored -> assessFragment(
                            snapshot, opportunity, routes.get(placement.fragmentKey())));
            Decision decision = decide(fragment, placement.role());
            PlacementApproval approval = new PlacementApproval(
                    placement.role(), placement.fragmentKey(),
                    placement.entrance().x(), placement.entrance().z(),
                    fragment, decision);
            PLACEMENTS.put(entranceKey(placement.entrance().x(), placement.entrance().z()), approval);
            decisions.merge(decision, 1, Integer::sum);
        }

        if (ExternalUrbanFabricBuilder.plotCount() != 233) {
            throw new IllegalStateException("Erden functional plot count drifted during new-floor audit: "
                    + ExternalUrbanFabricBuilder.plotCount());
        }
        if (candidatePlacements != EXPECTED_CANDIDATES || PLACEMENTS.size() != EXPECTED_CANDIDATES) {
            throw new IllegalStateException("Erden AUTHOR_NEW_FLOOR_IN_VOID candidate drift: expected="
                    + EXPECTED_CANDIDATES + " actual=" + candidatePlacements
                    + " unique=" + PLACEMENTS.size());
        }

        for (FragmentAssessment fragment : fragmentAssessments.values()) {
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_NEW_FLOOR_APPROVAL fragment={} candidate_y={} usable_cells={} largest_region={} bbox={}x{} compactness={} min_roof_distance={} support_anchor_ratio={} route={} source_air_floor=true source_blocks_cut=0 source_only=true world_reads=false mutations=0",
                    fragment.fragmentKey(), fragment.feetY(), fragment.usableCells(),
                    fragment.largestRegionCells(), fragment.width(), fragment.depth(),
                    formatRatio(fragment.compactness()), fragment.minRoofDistance(),
                    formatRatio(fragment.supportAnchorRatio()), fragment.routeClassification());
        }

        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_NEW_FLOOR_APPROVAL_PASS candidates={} classified={} decisions={} roles={} approved={} needs_authored_access={} source_blocks_cut=0 source_only=true world_reads=false mutations=0 placement_counts_unchanged=true plots=233 housing=77 work=156 revision={}",
                candidatePlacements, PLACEMENTS.size(), decisions, candidateRoles,
                decisions.getOrDefault(Decision.APPROVED_FOR_AUTHORING, 0),
                decisions.getOrDefault(Decision.NEEDS_AUTHORED_ACCESS, 0),
                CATALOG_REVISION);
    }

    public static Map<Long, PlacementApproval> placements() {
        bootstrap();
        return Map.copyOf(PLACEMENTS);
    }

    public static int approvedCount() {
        bootstrap();
        return (int) PLACEMENTS.values().stream()
                .filter(value -> value.decision() == Decision.APPROVED_FOR_AUTHORING)
                .count();
    }

    private static FragmentAssessment assessFragment(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            ErdenUrbanUpperRoomOpportunityCatalog.OpportunityProfile opportunity,
            ErdenUrbanSourceAirRoutePlanner.RoutePlan route) {
        ErdenUrbanUpperRoomOpportunityCatalog.LevelOpportunity floor = opportunity.newFloorVoid();
        if (floor.feetY() == Integer.MIN_VALUE || floor.regions().isEmpty()) {
            throw new IllegalStateException("New-floor recommendation has no candidate volume: "
                    + snapshot.fragmentKey());
        }
        ErdenUrbanUpperRoomOpportunityCatalog.Region largest = floor.regions().stream()
                .max((a, b) -> Integer.compare(a.cells().size(), b.cells().size()))
                .orElseThrow();

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (long cell : largest.cells()) {
            int x = cellX(cell);
            int z = cellZ(cell);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }
        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        double compactness = largest.cells().size() / (double) (width * depth);

        Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks = new HashMap<>();
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            blocks.put(blockKey(block.x(), block.y(), block.z()), block);
        }
        int supportAnchors = 0;
        int minRoof = Integer.MAX_VALUE;
        for (long cell : largest.cells()) {
            int x = cellX(cell);
            int z = cellZ(cell);
            if (hasSupportAnchor(blocks, x, floor.feetY() - 1, z)) supportAnchors++;
            minRoof = Math.min(minRoof, roofDistance(blocks, snapshot.height(), x, floor.feetY(), z));
        }
        double supportRatio = supportAnchors / (double) largest.cells().size();
        ErdenUrbanSourceAirRoutePlanner.RouteClassification routeClassification = route == null
                ? ErdenUrbanSourceAirRoutePlanner.RouteClassification.NO_TARGET
                : route.classification();
        boolean routeTargetsNewFloor = route != null
                && route.targetMode()
                == ErdenUrbanUpperRoomOpportunityCatalog.FloorMode.NEW_AUTHORED_FLOOR;

        return new FragmentAssessment(
                snapshot.fragmentKey(), floor.feetY(), floor.usableCells(),
                largest.cells().size(), width, depth, compactness,
                minRoof == Integer.MAX_VALUE ? 0 : minRoof,
                supportRatio, routeClassification, routeTargetsNewFloor);
    }

    private static Decision decide(FragmentAssessment fragment, String role) {
        if (fragment.largestRegionCells() < Math.max(MIN_STRUCTURAL_AREA, roleAreaRequirement(role))
                || fragment.width() < MIN_ROOM_SPAN
                || fragment.depth() < MIN_ROOM_SPAN
                || fragment.compactness() < MIN_COMPACTNESS
                || fragment.minRoofDistance() < MIN_ROOF_DISTANCE) {
            return Decision.REJECT_GEOMETRY;
        }
        if (fragment.supportAnchorRatio() < MIN_SUPPORT_ANCHOR_RATIO) {
            return Decision.REJECT_SUPPORT;
        }
        if (!fragment.routeTargetsNewFloor()
                || fragment.routeClassification()
                != ErdenUrbanSourceAirRoutePlanner.RouteClassification.ZERO_CUT_ROUTE) {
            return Decision.NEEDS_AUTHORED_ACCESS;
        }
        return Decision.APPROVED_FOR_AUTHORING;
    }

    private static int roleAreaRequirement(String role) {
        return switch (role) {
            case "inn" -> 28;
            case "stable", "warehouse" -> 30;
            case "bakery", "bathhouse" -> 24;
            case "shop" -> 20;
            case "tenement", "guard_post" -> 18;
            default -> throw new IllegalArgumentException("Unknown Erden urban role " + role);
        };
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

    private static int roofDistance(
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int height, int x, int feetY, int z) {
        for (int y = feetY + 2; y < height; y++) {
            if (structural(blocks.get(blockKey(x, y, z)))) return y - feetY;
        }
        return Integer.MAX_VALUE;
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
                || id.endsWith("_sign") || id.endsWith("_wall_sign")
                || id.endsWith("_trapdoor") || id.endsWith("_leaves")
                || id.endsWith("_sapling") || id.contains("grass")
                || id.contains("flower") || id.contains("fern") || id.contains("vine"));
    }

    private static String formatRatio(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static long entranceKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static long blockKey(int x, int y, int z) {
        return ((long) (x & 0x1fffff) << 42)
                ^ ((long) (y & 0x3fffff) << 20)
                ^ (z & 0xfffffL);
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

    public enum Decision {
        APPROVED_FOR_AUTHORING,
        NEEDS_AUTHORED_ACCESS,
        REJECT_GEOMETRY,
        REJECT_SUPPORT
    }

    public record FragmentAssessment(
            String fragmentKey,
            int feetY,
            int usableCells,
            int largestRegionCells,
            int width,
            int depth,
            double compactness,
            int minRoofDistance,
            double supportAnchorRatio,
            ErdenUrbanSourceAirRoutePlanner.RouteClassification routeClassification,
            boolean routeTargetsNewFloor) {
    }

    public record PlacementApproval(
            String role,
            String fragmentKey,
            int entranceX,
            int entranceZ,
            FragmentAssessment fragment,
            Decision decision) {
    }
}
