package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;

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
 * Plans zero-cut staircase branches between already accepted Erden interior levels.
 *
 * <p>The legacy route/new-floor systems remain authoritative for the first upper level. This catalog
 * only handles additional levels selected by {@link ErdenUrbanFullInteriorPlanCatalog}. Every route
 * node keeps player feet/head in raw source AIR or in a crop seal independently proven to correspond
 * to raw source AIR. Routes remain inside the retained fragment and underneath retained structure;
 * imported architecture is never cut and no world chunk is read.</p>
 */
public final class ErdenUrbanFullInteriorRouteCatalog {
    public static final int CATALOG_REVISION = 2;

    private static final int MIN_HEADROOM = 2;
    private static final int MAX_EXPLORED_NODES = 24_000;
    private static final int[] STEP_HEIGHTS = {1, 0};
    private static final int[][] DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    private static final Map<String, List<LevelRoutePlan>> PLANS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private ErdenUrbanFullInteriorRouteCatalog() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        PLANS.clear();
        ErdenUrbanFullInteriorPlanCatalog.bootstrap();
        ErdenUrbanSyntheticSealProvenance.bootstrap();

        Map<String, ExternalUrbanFabricBuilder.UrbanFragmentSnapshot> snapshots =
                ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics();
        Map<String, ErdenUrbanFullInteriorPlanCatalog.InteriorPlan> interiors =
                ErdenUrbanFullInteriorPlanCatalog.plans();
        Map<String, ErdenUrbanUpperRoomOpportunityCatalog.OpportunityProfile> opportunities =
                ErdenUrbanUpperRoomOpportunityCatalog.profiles();

        int fragmentLevels = 0;
        int fragmentRooms = 0;
        int routeNodes = 0;
        for (ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot : snapshots.values()) {
            ErdenUrbanFullInteriorPlanCatalog.InteriorPlan interior = interiors.get(snapshot.fragmentKey());
            ErdenUrbanUpperRoomOpportunityCatalog.OpportunityProfile opportunity =
                    opportunities.get(snapshot.fragmentKey());
            if (interior == null || opportunity == null) {
                throw new IllegalStateException("Missing Erden full-interior route input "
                        + snapshot.fragmentKey());
            }
            List<LevelRoutePlan> plans = plan(snapshot, interior, opportunity);
            PLANS.put(snapshot.fragmentKey(), plans);
            fragmentLevels += plans.size();
            for (LevelRoutePlan level : plans) {
                fragmentRooms += level.regionRoutes().size();
                routeNodes += level.regionRoutes().stream().mapToInt(route -> route.path().size()).sum();
                LivingKingdoms.LOGGER.info(
                        "LK_ERDEN_FULL_INTERIOR_ROUTE fragment={} lower_y={} target_y={} room_routes={} floor_cells={} route_nodes={} turns={} explored_nodes={} source_blocks_cut=0 source_air_only=true source_only=true world_reads=false mutations=0",
                        snapshot.fragmentKey(), level.lowerFeetY(), level.targetFeetY(),
                        level.regionRoutes().size(), level.floorCells().size(),
                        level.regionRoutes().stream().mapToInt(route -> route.path().size()).sum(),
                        level.regionRoutes().stream().mapToInt(RegionRoute::turns).sum(),
                        level.regionRoutes().stream().mapToInt(RegionRoute::exploredNodes).sum());
            }
        }

        int buildings = 0;
        int buildingsWithExpansion = 0;
        int mappedLevels = 0;
        int mappedRooms = 0;
        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement
                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {
            buildings++;
            List<LevelRoutePlan> plans = PLANS.get(placement.fragmentKey());
            if (plans == null) {
                throw new IllegalStateException("Missing full-interior route mapping "
                        + placement.fragmentKey());
            }
            if (!plans.isEmpty()) buildingsWithExpansion++;
            mappedLevels += plans.size();
            mappedRooms += plans.stream().mapToInt(level -> level.regionRoutes().size()).sum();
        }
        if (buildings != 233 || buildings != ExternalUrbanFabricBuilder.plotCount()) {
            throw new IllegalStateException("Erden full-interior route placement count drifted: " + buildings);
        }
        if (buildingsWithExpansion <= 0 || mappedLevels <= 0 || mappedRooms <= 0) {
            throw new IllegalStateException("Erden full-interior source plan has no routable expansion");
        }

        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden full interior extension routes fragments={} fragment_levels={} fragment_rooms={} fragment_route_nodes={} buildings=233 buildings_with_expansion={} mapped_levels={} mapped_rooms={} source_blocks_cut=0 source_air_only=true source_only=true world_reads=false mutations=0 plots=233 housing=77 work=156 revision={}",
                PLANS.size(), fragmentLevels, fragmentRooms, routeNodes,
                buildingsWithExpansion, mappedLevels, mappedRooms, CATALOG_REVISION);
    }

    public static List<LevelRoutePlan> plans(String fragmentKey) {
        bootstrap();
        return PLANS.getOrDefault(fragmentKey, List.of());
    }

    public static Map<String, List<LevelRoutePlan>> plans() {
        bootstrap();
        return Map.copyOf(PLANS);
    }

    private static List<LevelRoutePlan> plan(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            ErdenUrbanFullInteriorPlanCatalog.InteriorPlan interior,
            ErdenUrbanUpperRoomOpportunityCatalog.OpportunityProfile opportunity) {
        List<ErdenUrbanFullInteriorPlanCatalog.PlannedLevel> levels = new ArrayList<>();
        levels.addAll(interior.existingLevels());
        levels.addAll(interior.selectedAuthoredLevels());
        levels.sort(Comparator.comparingInt(ErdenUrbanFullInteriorPlanCatalog.PlannedLevel::feetY));
        if (levels.size() < 2) return List.of();

        Integer legacyNewY = opportunity.recommendation()
                == ErdenUrbanUpperRoomOpportunityCatalog.Recommendation.AUTHOR_NEW_FLOOR_IN_VOID
                ? opportunity.newFloorVoid().feetY() : null;

        Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks = new HashMap<>();
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            blocks.put(blockKey(block.x(), block.y(), block.z()), block);
        }

        List<LevelRoutePlan> result = new ArrayList<>();
        for (int index = 0; index < levels.size(); index++) {
            ErdenUrbanFullInteriorPlanCatalog.PlannedLevel target = levels.get(index);
            if (target.kind() != ErdenUrbanFullInteriorPlanCatalog.LevelKind.NEW_AUTHORED_FLOOR) continue;
            if (legacyNewY != null && target.feetY() == legacyNewY) continue;
            ErdenUrbanFullInteriorPlanCatalog.PlannedLevel lower = nearestLower(levels, index, target.feetY());
            if (lower == null) {
                throw new IllegalStateException("No accepted lower level for Erden full interior target "
                        + snapshot.fragmentKey() + " y=" + target.feetY());
            }

            Set<Long> lowerCells = levelCells(lower);
            if (lowerCells.isEmpty()) {
                throw new IllegalStateException("Empty lower level for Erden full interior target "
                        + snapshot.fragmentKey() + " y=" + target.feetY());
            }
            List<RegionRoute> regionRoutes = new ArrayList<>();
            Set<Long> floorCells = new HashSet<>();
            // Room branches are planned sequentially. Once one branch has claimed an ascending
            // stair cell, later branches may share it only in the same direction. This turns the
            // independent shortest paths into one coherent staircase network instead of allowing
            // two stair blocks at the same position to demand opposite facings.
            Map<Long, RiseDirection> committedAscents = new HashMap<>();
            for (ErdenUrbanFullInteriorPlanCatalog.PlannedRegion region : target.regions()) {
                Set<Long> regionCells = new HashSet<>(region.cells());
                if (regionCells.isEmpty()) continue;
                RouteSearch search = search(
                        snapshot, blocks, lower.feetY(), lowerCells,
                        target.feetY(), regionCells, committedAscents);
                if (search.path().isEmpty()) {
                    throw new IllegalStateException(
                            "No zero-cut staircase branch to planned Erden room fragment="
                                    + snapshot.fragmentKey() + " lower=" + lower.feetY()
                                    + " target=" + target.feetY()
                                    + " region_cells=" + regionCells.size()
                                    + " explored=" + search.exploredNodes());
                }
                commitAscents(search.path(), committedAscents, snapshot.fragmentKey());
                floorCells.addAll(regionCells);
                regionRoutes.add(new RegionRoute(
                        List.copyOf(region.cells()), search.path(),
                        countTurns(search.path()), search.exploredNodes()));
            }
            if (regionRoutes.isEmpty() || floorCells.size() < 28) {
                throw new IllegalStateException("Unsafe empty Erden full interior extension "
                        + snapshot.fragmentKey() + " y=" + target.feetY());
            }
            result.add(new LevelRoutePlan(
                    lower.feetY(), target.feetY(), List.copyOf(floorCells),
                    List.copyOf(regionRoutes)));
        }
        return List.copyOf(result);
    }

    private static ErdenUrbanFullInteriorPlanCatalog.PlannedLevel nearestLower(
            List<ErdenUrbanFullInteriorPlanCatalog.PlannedLevel> levels,
            int targetIndex, int targetY) {
        ErdenUrbanFullInteriorPlanCatalog.PlannedLevel best = null;
        for (int i = 0; i < targetIndex; i++) {
            ErdenUrbanFullInteriorPlanCatalog.PlannedLevel candidate = levels.get(i);
            if (candidate.feetY() >= targetY) continue;
            if (best == null || candidate.feetY() > best.feetY()) best = candidate;
        }
        return best;
    }

    private static Set<Long> levelCells(ErdenUrbanFullInteriorPlanCatalog.PlannedLevel level) {
        Set<Long> result = new HashSet<>();
        for (ErdenUrbanFullInteriorPlanCatalog.PlannedRegion region : level.regions()) {
            result.addAll(region.cells());
        }
        return result;
    }

    private static RouteSearch search(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int lowerY,
            Set<Long> lowerCells,
            int targetY,
            Set<Long> targetCells,
            Map<Long, RiseDirection> committedAscents) {
        ArrayDeque<Node> pending = new ArrayDeque<>();
        Map<Long, Long> previous = new HashMap<>();
        Map<Long, Node> nodes = new HashMap<>();
        Set<Long> visited = new HashSet<>();

        for (long cell : lowerCells) {
            int x = cellX(cell);
            int z = cellZ(cell);
            if (!routeBodyClear(snapshot, blocks, x, lowerY, z)) continue;
            Node seed = new Node(x, lowerY, z);
            long key = nodeKey(x, lowerY, z);
            if (visited.add(key)) {
                pending.addLast(seed);
                nodes.put(key, seed);
                previous.put(key, Long.MIN_VALUE);
            }
        }

        Node found = null;
        while (!pending.isEmpty() && visited.size() <= MAX_EXPLORED_NODES) {
            Node current = pending.removeFirst();
            if (current.y() == targetY && targetCells.contains(cellKey(current.x(), current.z()))) {
                found = current;
                break;
            }
            for (int[] direction : DIRECTIONS) {
                for (int dy : STEP_HEIGHTS) {
                    int x = current.x() + direction[0];
                    int y = current.y() + dy;
                    int z = current.z() + direction[1];
                    if (y < lowerY || y > targetY) continue;
                    if (dy == 1) {
                        long riseKey = nodeKey(current.x(), current.y(), current.z());
                        RiseDirection committed = committedAscents.get(riseKey);
                        if (committed != null
                                && (committed.dx() != direction[0] || committed.dz() != direction[1])) {
                            continue;
                        }
                    }
                    if (!routeBodyClear(snapshot, blocks, x, y, z)) continue;
                    if (!routeCellPermitted(snapshot, blocks, x, y, z)) continue;
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
        if (found == null) return new RouteSearch(List.of(), visited.size());
        List<Node> reverse = new ArrayList<>();
        long key = nodeKey(found.x(), found.y(), found.z());
        while (key != Long.MIN_VALUE) {
            Node node = nodes.get(key);
            if (node == null) {
                throw new IllegalStateException("Broken Erden full-interior route predecessor");
            }
            reverse.add(node);
            key = previous.getOrDefault(key, Long.MIN_VALUE);
        }
        java.util.Collections.reverse(reverse);
        return new RouteSearch(List.copyOf(reverse), visited.size());
    }

    private static void commitAscents(
            List<Node> path,
            Map<Long, RiseDirection> committedAscents,
            String fragmentKey) {
        for (int index = 1; index < path.size(); index++) {
            Node previous = path.get(index - 1);
            Node current = path.get(index);
            if (current.y() - previous.y() != 1) continue;
            int dx = current.x() - previous.x();
            int dz = current.z() - previous.z();
            RiseDirection direction = new RiseDirection(dx, dz);
            long key = nodeKey(previous.x(), previous.y(), previous.z());
            RiseDirection old = committedAscents.putIfAbsent(key, direction);
            if (old != null && !old.equals(direction)) {
                throw new IllegalStateException(
                        "Conflicting source-route ascent survived planning fragment="
                                + fragmentKey + " at=" + previous + " old=" + old
                                + " new=" + direction);
            }
        }
    }

    private static boolean routeBodyClear(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int x, int feetY, int z) {
        if (x < 0 || x >= snapshot.width() || z < 0 || z >= snapshot.length()) return false;
        if (feetY <= 0 || feetY + MIN_HEADROOM - 1 >= snapshot.height()) return false;
        for (int dy = 0; dy < MIN_HEADROOM; dy++) {
            ExternalUrbanFabricBuilder.UrbanSourceBlock block =
                    blocks.get(blockKey(x, feetY + dy, z));
            if (block != null && !block.state().isAir()
                    && !ErdenUrbanSyntheticSealProvenance.isClearableSourceAirSeal(
                    snapshot.fragmentKey(), x, feetY + dy, z)) {
                return false;
            }
        }
        return true;
    }

    private static boolean routeCellPermitted(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int x, int feetY, int z) {
        if (!interiorSide(snapshot, x, z)) return false;
        for (int y = feetY + MIN_HEADROOM; y < snapshot.height(); y++) {
            ExternalUrbanFabricBuilder.UrbanSourceBlock roof = blocks.get(blockKey(x, y, z));
            if (roof != null && !roof.state().isAir()
                    && !ErdenUrbanSyntheticSealProvenance.isClearableSourceAirSeal(
                    snapshot.fragmentKey(), x, y, z)) {
                return true;
            }
        }
        return false;
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

    private static int countTurns(List<Node> path) {
        int turns = 0;
        int lastDx = 0;
        int lastDz = 0;
        for (int i = 1; i < path.size(); i++) {
            int dx = Integer.compare(path.get(i).x() - path.get(i - 1).x(), 0);
            int dz = Integer.compare(path.get(i).z() - path.get(i - 1).z(), 0);
            if (i > 1 && (dx != lastDx || dz != lastDz)) turns++;
            lastDx = dx;
            lastDz = dz;
        }
        return turns;
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

    private record RiseDirection(int dx, int dz) {
    }

    private record RouteSearch(List<Node> path, int exploredNodes) {
    }

    public record Node(int x, int y, int z) {
    }

    public record RegionRoute(
            List<Long> regionCells,
            List<Node> path,
            int turns,
            int exploredNodes) {
    }

    public record LevelRoutePlan(
            int lowerFeetY,
            int targetFeetY,
            List<Long> floorCells,
            List<RegionRoute> regionRoutes) {
    }
}
