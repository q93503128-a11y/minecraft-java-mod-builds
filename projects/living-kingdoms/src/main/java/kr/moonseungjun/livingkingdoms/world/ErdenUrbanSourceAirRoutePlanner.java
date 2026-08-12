package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.world.level.block.DoorBlock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds staircase-shaped routes through source air only, from the retained entrance floor to the
 * upper-room opportunities already proven by {@link ErdenUrbanUpperRoomOpportunityCatalog}.
 *
 * <p>This planner never cuts imported blocks and never reads a world chunk. A route node represents
 * player feet space; feet and head must already be source air. Horizontal moves and one-block rises
 * or drops are allowed only while moving to an adjacent horizontal cell, matching a staircase rather
 * than permitting vertical ladders. Support may be authored later underneath the route, but source
 * facade/roof/interior blocks remain immutable.</p>
 */
public final class ErdenUrbanSourceAirRoutePlanner {
    public static final int PLANNER_REVISION = 1;

    private static final int EDGE_MARGIN = 1;
    private static final int MAX_ROUTE_NODES = 32_000;
    private static final int MIN_HEADROOM = 2;

    private static final Map<String, RoutePlan> PLANS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private ErdenUrbanSourceAirRoutePlanner() {
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
                throw new IllegalStateException("Missing upper-room opportunity before Erden route planning: "
                        + snapshot.fragmentKey());
            }
            RoutePlan plan = plan(snapshot, opportunity);
            PLANS.put(snapshot.fragmentKey(), plan);
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_SOURCE_AIR_ROUTE fragment={} recommendation={} route_classification={} target_mode={} ground_y={} target_y={} path_nodes={} rise={} turns={} explored_nodes={} source_blocks_cut=0 source_only=true world_reads=false mutations=0",
                    snapshot.fragmentKey(), opportunity.recommendation(), plan.classification(),
                    plan.targetMode(), opportunity.groundFeetY(), plan.targetFeetY(),
                    plan.path().size(), plan.rise(), plan.turns(), plan.exploredNodes());
        }

        Map<RouteClassification, Integer> placementCounts = new LinkedHashMap<>();
        int mapped = 0;
        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement
                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {
            RoutePlan plan = PLANS.get(placement.fragmentKey());
            if (plan == null) {
                throw new IllegalStateException("Missing Erden air-route plan for placed fragment "
                        + placement.fragmentKey());
            }
            placementCounts.merge(plan.classification(), 1, Integer::sum);
            mapped++;
        }
        if (mapped != 233 || mapped != ExternalUrbanFabricBuilder.plotCount()) {
            throw new IllegalStateException("Erden air-route placement mapping drifted: " + mapped);
        }

        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden zero-cut source-air route plans fragments={} buildings={} classifications={} source_blocks_cut=0 source_only=true world_reads=false mutations=0 placement_counts_unchanged=true revision={}",
                PLANS.size(), mapped, placementCounts, PLANNER_REVISION);
    }

    public static RoutePlan plan(String fragmentKey) {
        bootstrap();
        return PLANS.get(fragmentKey);
    }

    public static Map<String, RoutePlan> plans() {
        bootstrap();
        return Map.copyOf(PLANS);
    }

    private static RoutePlan plan(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            ErdenUrbanUpperRoomOpportunityCatalog.OpportunityProfile opportunity) {
        Target target = target(opportunity);
        if (target == null || opportunity.groundFeetY() == Integer.MIN_VALUE) {
            return RoutePlan.none(RouteClassification.NO_TARGET);
        }

        Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks = new HashMap<>();
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            blocks.put(blockKey(block.x(), block.y(), block.z()), block);
        }

        Set<Long> ground = reachableGround(snapshot, blocks, opportunity.groundFeetY());
        if (ground.isEmpty()) {
            return RoutePlan.none(RouteClassification.NO_GROUND_ROUTE);
        }

        Set<Long> targetCells = new HashSet<>();
        for (ErdenUrbanUpperRoomOpportunityCatalog.Region region : target.regions()) {
            targetCells.addAll(region.cells());
        }
        if (targetCells.isEmpty()) {
            return RoutePlan.none(RouteClassification.NO_TARGET);
        }

        ArrayDeque<Node> pending = new ArrayDeque<>();
        Map<Long, Long> previous = new HashMap<>();
        Map<Long, Node> nodes = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        for (long cell : ground) {
            int x = cellX(cell);
            int z = cellZ(cell);
            Node seed = new Node(x, opportunity.groundFeetY(), z);
            long key = nodeKey(seed.x(), seed.y(), seed.z());
            if (visited.add(key)) {
                pending.addLast(seed);
                nodes.put(key, seed);
                previous.put(key, Long.MIN_VALUE);
            }
        }

        Node found = null;
        int minimumY = Math.max(1, opportunity.groundFeetY() - 1);
        int maximumY = Math.min(snapshot.height() - MIN_HEADROOM - 1, target.feetY() + 2);
        while (!pending.isEmpty() && visited.size() <= MAX_ROUTE_NODES) {
            Node current = pending.removeFirst();
            if (current.y() == target.feetY()
                    && targetCells.contains(cellKey(current.x(), current.z()))) {
                found = current;
                break;
            }
            for (int[] direction : DIRECTIONS) {
                for (int deltaY : STEP_HEIGHTS) {
                    int x = current.x() + direction[0];
                    int y = current.y() + deltaY;
                    int z = current.z() + direction[1];
                    if (y < minimumY || y > maximumY) continue;
                    if (!routeBodyClear(snapshot, blocks, x, y, z)) continue;
                    if (!routeColumnCovered(snapshot, blocks, x, y, z)) continue;
                    Node next = new Node(x, y, z);
                    long nextKey = nodeKey(x, y, z);
                    if (!visited.add(nextKey)) continue;
                    long currentKey = nodeKey(current.x(), current.y(), current.z());
                    previous.put(nextKey, currentKey);
                    nodes.put(nextKey, next);
                    pending.addLast(next);
                }
            }
        }

        if (found == null) {
            return new RoutePlan(
                    RouteClassification.NO_ZERO_CUT_ROUTE, target.mode(), target.feetY(),
                    List.of(), target.feetY() - opportunity.groundFeetY(), 0, visited.size());
        }

        List<Node> path = reconstruct(found, previous, nodes);
        int turns = countTurns(path);
        return new RoutePlan(
                RouteClassification.ZERO_CUT_ROUTE, target.mode(), target.feetY(),
                List.copyOf(path), target.feetY() - opportunity.groundFeetY(), turns, visited.size());
    }

    private static Target target(ErdenUrbanUpperRoomOpportunityCatalog.OpportunityProfile profile) {
        return switch (profile.recommendation()) {
            case ROUTE_TO_EXISTING_ROOM -> new Target(
                    ErdenUrbanUpperRoomOpportunityCatalog.FloorMode.EXISTING_SOURCE_FLOOR,
                    profile.existingFloor().feetY(), profile.existingFloor().regions());
            case AUTHOR_NEW_FLOOR_IN_VOID -> new Target(
                    ErdenUrbanUpperRoomOpportunityCatalog.FloorMode.NEW_AUTHORED_FLOOR,
                    profile.newFloorVoid().feetY(), profile.newFloorVoid().regions());
            case NO_SAFE_ROOM -> null;
        };
    }

    private static Set<Long> reachableGround(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int feetY) {
        int[] inward = inward(snapshot.exteriorSide());
        Node seed = null;
        for (int depth = 1; depth <= 4; depth++) {
            int x = snapshot.entranceX() + inward[0] * depth;
            int z = snapshot.entranceZ() + inward[1] * depth;
            if (groundWalkable(snapshot, blocks, x, feetY, z)) {
                seed = new Node(x, feetY, z);
                break;
            }
        }
        if (seed == null && groundWalkable(
                snapshot, blocks, snapshot.entranceX(), feetY, snapshot.entranceZ())) {
            seed = new Node(snapshot.entranceX(), feetY, snapshot.entranceZ());
        }
        if (seed == null) return Set.of();

        Set<Long> result = new HashSet<>();
        ArrayDeque<Node> pending = new ArrayDeque<>();
        result.add(cellKey(seed.x(), seed.z()));
        pending.add(seed);
        while (!pending.isEmpty()) {
            Node current = pending.removeFirst();
            for (int[] direction : DIRECTIONS) {
                int x = current.x() + direction[0];
                int z = current.z() + direction[1];
                long cell = cellKey(x, z);
                if (result.contains(cell)) continue;
                if (!groundWalkable(snapshot, blocks, x, feetY, z)) continue;
                result.add(cell);
                pending.addLast(new Node(x, feetY, z));
            }
        }
        return Set.copyOf(result);
    }

    private static boolean groundWalkable(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int x, int feetY, int z) {
        if (!routeBodyClear(snapshot, blocks, x, feetY, z)) return false;
        ExternalUrbanFabricBuilder.UrbanSourceBlock floor = blocks.get(blockKey(x, feetY - 1, z));
        return floor != null && !floor.state().isAir()
                && !(floor.state().getBlock() instanceof DoorBlock);
    }

    private static boolean routeBodyClear(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int x, int feetY, int z) {
        if (x < EDGE_MARGIN || x >= snapshot.width() - EDGE_MARGIN
                || z < EDGE_MARGIN || z >= snapshot.length() - EDGE_MARGIN) return false;
        if (!interiorSide(snapshot, x, z)) return false;
        if (feetY <= 0 || feetY + MIN_HEADROOM - 1 >= snapshot.height()) return false;
        for (int dy = 0; dy < MIN_HEADROOM; dy++) {
            ExternalUrbanFabricBuilder.UrbanSourceBlock block =
                    blocks.get(blockKey(x, feetY + dy, z));
            if (block != null && !block.state().isAir()) return false;
        }
        return true;
    }

    /**
     * A new stair may pass through authored source air that is not a four-wall room at every
     * intermediate height (for example an open stair hall, balcony edge or vaulted transition),
     * but it must stay beneath the immutable source roof. Together with the fragment bounds,
     * entrance-facing interior half-plane and source-air body checks this keeps the route attached
     * to the imported building while allowing a genuine zero-cut connection to an upper room.
     */
    private static boolean routeColumnCovered(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int x, int feetY, int z) {
        return roofAbove(snapshot, blocks, x, feetY + MIN_HEADROOM, z);
    }

    private static boolean roofAbove(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int x, int startY, int z) {
        for (int y = startY; y < snapshot.height(); y++) {
            ExternalUrbanFabricBuilder.UrbanSourceBlock block = blocks.get(blockKey(x, y, z));
            if (structuralBarrier(block)) return true;
        }
        return false;
    }

    private static boolean structuralBarrier(ExternalUrbanFabricBuilder.UrbanSourceBlock block) {
        if (block == null || block.state().isAir()) return false;
        if (block.state().getBlock() instanceof DoorBlock) return false;
        String id = block.state().getBlock().toString();
        return !(id.contains("torch") || id.contains("button") || id.contains("pressure_plate")
                || id.contains("carpet") || id.contains("lantern") || id.contains("chain")
                || id.contains("sign") || id.contains("leaves") || id.contains("sapling")
                || id.contains("grass") || id.contains("flower") || id.contains("fern")
                || id.contains("vine"));
    }

    private static List<Node> reconstruct(
            Node found, Map<Long, Long> previous, Map<Long, Node> nodes) {
        List<Node> reversed = new ArrayList<>();
        long current = nodeKey(found.x(), found.y(), found.z());
        while (current != Long.MIN_VALUE) {
            Node node = nodes.get(current);
            if (node == null) break;
            reversed.add(node);
            current = previous.getOrDefault(current, Long.MIN_VALUE);
        }
        Collections.reverse(reversed);
        return reversed;
    }

    private static int countTurns(List<Node> path) {
        if (path.size() < 3) return 0;
        int turns = 0;
        int lastDx = path.get(1).x() - path.get(0).x();
        int lastDz = path.get(1).z() - path.get(0).z();
        for (int i = 2; i < path.size(); i++) {
            int dx = path.get(i).x() - path.get(i - 1).x();
            int dz = path.get(i).z() - path.get(i - 1).z();
            if (dx != lastDx || dz != lastDz) turns++;
            lastDx = dx;
            lastDz = dz;
        }
        return turns;
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

    private static long nodeKey(int x, int y, int z) {
        long a = ((long) x & 0x1fffffL) << 43;
        long b = ((long) y & 0x3fffffL) << 21;
        long c = (long) z & 0x1fffffL;
        return a ^ b ^ c;
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
    private static final int[] STEP_HEIGHTS = {0, 1, -1};

    public enum RouteClassification {
        ZERO_CUT_ROUTE,
        NO_ZERO_CUT_ROUTE,
        NO_GROUND_ROUTE,
        NO_TARGET
    }

    public record Node(int x, int y, int z) {
    }

    public record RoutePlan(
            RouteClassification classification,
            ErdenUrbanUpperRoomOpportunityCatalog.FloorMode targetMode,
            int targetFeetY,
            List<Node> path,
            int rise,
            int turns,
            int exploredNodes) {
        static RoutePlan none(RouteClassification classification) {
            return new RoutePlan(classification, null, Integer.MIN_VALUE, List.of(), 0, 0, 0);
        }
    }

    private record Target(
            ErdenUrbanUpperRoomOpportunityCatalog.FloorMode mode,
            int feetY,
            List<ErdenUrbanUpperRoomOpportunityCatalog.Region> regions) {
    }
}
