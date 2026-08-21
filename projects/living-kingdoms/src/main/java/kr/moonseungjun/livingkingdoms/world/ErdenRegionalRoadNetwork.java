package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Source-only national road graph connecting Erden's capital gates and six second-ring villages.
 * Geometry is authored in metres (one block == one metre) and is streamed by
 * {@link ErdenRegionalRoadManager}; this class never loads chunks or mutates the world.
 */
public final class ErdenRegionalRoadNetwork {
    public static final int REVISION = 1;
    public static final int ROAD_HALF_WIDTH = 3;
    public static final int CORRIDOR_COUNT = 8;
    public static final int WAYSTATION_COUNT = 4;
    public static final int ROUTE_SAMPLE_METRES = 8;

    public static final String SOUTH_GATE = "capital_south_gate";
    public static final String NORTH_GATE = "capital_north_gate";
    public static final String WEST_GATE = "capital_west_gate";

    private static final Map<String, Point> HUBS = new LinkedHashMap<>();
    private static final List<Corridor> CORRIDORS;
    private static final List<Waystation> WAYSTATIONS;

    static {
        HUBS.put(SOUTH_GATE, new Point(0, 916));
        HUBS.put(NORTH_GATE, new Point(0, -916));
        HUBS.put(WEST_GATE, new Point(-1_216, 0));
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            HUBS.put(settlement.id(), loadingBay(settlement));
        }

        CORRIDORS = List.of(
                corridor("south_silver", SOUTH_GATE, "silvermead",
                        point(0, 916), point(0, 2_600), point(-250, 4_200),
                        point(-650, 5_600), hub("silvermead")),
                corridor("silver_harvest", "silvermead", "harvest_crossing",
                        hub("silvermead"), point(-1_400, 6_750), point(-2_050, 7_050),
                        hub("harvest_crossing")),
                corridor("silver_sunfield", "silvermead", "sunfield",
                        hub("silvermead"), point(300, 6_600), point(1_400, 7_000),
                        point(2_200, 7_400), hub("sunfield")),
                corridor("north_pine", NORTH_GATE, "pinewatch",
                        point(0, -916), point(-350, -2_300), point(-850, -3_900),
                        point(-1_500, -5_500), point(-2_200, -6_700), hub("pinewatch")),
                corridor("west_black", WEST_GATE, "blackstone",
                        point(-1_216, 0), point(-3_000, -350), point(-4_700, -800),
                        point(-6_000, -1_300), hub("blackstone")),
                corridor("black_iron", "blackstone", "ironvale",
                        hub("blackstone"), point(-8_200, -900), point(-9_000, 0),
                        point(-9_500, 900), hub("ironvale")),
                corridor("north_west_ring", NORTH_GATE, WEST_GATE,
                        point(0, -916), point(-700, -930), point(-1_216, -500), point(-1_216, 0)),
                corridor("west_south_ring", WEST_GATE, SOUTH_GATE,
                        point(-1_216, 0), point(-1_220, 500), point(-700, 920), point(0, 916))
        );

        WAYSTATIONS = List.of(
                new Waystation("amber_post", -250, 4_200, "남부 곡창대로 역참"),
                new Waystation("northwatch_post", -850, -3_900, "북부 산림대로 역참"),
                new Waystation("westroad_post", -4_700, -800, "서부 광산대로 역참"),
                new Waystation("ironroad_post", -9_000, 0, "철골짜기 고갯길 역참")
        );
        validate();
    }

    private ErdenRegionalRoadNetwork() {
    }

    public static List<Corridor> corridors() {
        return CORRIDORS;
    }

    public static List<Waystation> waystations() {
        return WAYSTATIONS;
    }

    public static Point hub(String id) {
        Point point = HUBS.get(id);
        if (point == null) throw new IllegalArgumentException("Unknown Erden road hub " + id);
        return point;
    }

    public static Point insideCapitalGate(String gateId) {
        return switch (gateId) {
            case SOUTH_GATE -> new Point(0, 880);
            case NORTH_GATE -> new Point(0, -880);
            case WEST_GATE -> new Point(-1_180, 0);
            default -> throw new IllegalArgumentException("Unknown capital gate " + gateId);
        };
    }

    public static String capitalGateFor(String settlementId) {
        return switch (settlementId) {
            case "harvest_crossing", "silvermead", "sunfield" -> SOUTH_GATE;
            case "pinewatch" -> NORTH_GATE;
            case "blackstone", "ironvale" -> WEST_GATE;
            default -> throw new IllegalArgumentException("Unknown regional settlement " + settlementId);
        };
    }

    public static List<ErdenTransportSavedData.RoutePoint> route(String sourceHub, String targetHub) {
        if (sourceHub.equals(targetHub)) {
            Point point = hub(sourceHub);
            return List.of(new ErdenTransportSavedData.RoutePoint(point.x, point.z));
        }
        List<Traversal> traversals = shortestTraversals(sourceHub, targetHub);
        if (traversals.isEmpty()) return List.of();
        List<Point> authored = new ArrayList<>();
        for (Traversal traversal : traversals) {
            List<Point> points = traversal.forward
                    ? traversal.corridor.points
                    : reversed(traversal.corridor.points);
            for (Point point : points) {
                if (authored.isEmpty() || !authored.getLast().equals(point)) authored.add(point);
            }
        }
        return sample(authored);
    }

    public static List<ErdenTransportSavedData.RoutePoint> routeToCapital(String settlementId) {
        return route(settlementId, capitalGateFor(settlementId));
    }

    public static boolean intersects(ChunkPos chunk) {
        double centerX = chunk.getMinBlockX() + 7.5D;
        double centerZ = chunk.getMinBlockZ() + 7.5D;
        double threshold = 15.5D;
        for (Corridor corridor : CORRIDORS) {
            for (int index = 1; index < corridor.points.size(); index++) {
                Point a = corridor.points.get(index - 1);
                Point b = corridor.points.get(index);
                if (distanceToSegment(centerX, centerZ, a.x, a.z, b.x, b.z) <= threshold) return true;
            }
        }
        for (Waystation station : WAYSTATIONS) {
            double dx = centerX - station.x;
            double dz = centerZ - station.z;
            if (dx * dx + dz * dz <= 29.0D * 29.0D) return true;
        }
        return false;
    }

    public static double distanceToRoad(int x, int z) {
        double best = Double.MAX_VALUE;
        for (Corridor corridor : CORRIDORS) {
            for (int index = 1; index < corridor.points.size(); index++) {
                Point a = corridor.points.get(index - 1);
                Point b = corridor.points.get(index);
                best = Math.min(best, distanceToSegment(x, z, a.x, a.z, b.x, b.z));
            }
        }
        return best;
    }

    public static Waystation waystationNear(int x, int z, int radius) {
        long radiusSquared = (long) radius * radius;
        for (Waystation station : WAYSTATIONS) {
            long dx = (long) x - station.x;
            long dz = (long) z - station.z;
            if (dx * dx + dz * dz <= radiusSquared) return station;
        }
        return null;
    }

    public static long totalRoadMetres() {
        long total = 0L;
        for (Corridor corridor : CORRIDORS) total += Math.round(corridor.length());
        return total;
    }

    private static Corridor corridor(String id, String from, String to, Point... points) {
        return new Corridor(id, from, to, List.of(points));
    }

    private static Point point(int x, int z) {
        return new Point(x, z);
    }

    private static Point loadingBay(ErdenRegionalSettlementCatalog.Settlement settlement) {
        ErdenRegionalSettlementCatalog.BuildingLot storehouse = settlement.buildings().stream()
                .filter(lot -> lot.role().equals("storehouse_west"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing storehouse for " + settlement.id()));
        // The market barrel sits at centre+24. Stop five metres east so the streamed road never
        // clears/replaces the authoritative player-editable barrel itself.
        return new Point(settlement.x() + storehouse.dx() + 29, settlement.z() + storehouse.dz());
    }

    private static List<Traversal> shortestTraversals(String source, String target) {
        if (!HUBS.containsKey(source) || !HUBS.containsKey(target)) return List.of();
        Map<String, Double> distances = new HashMap<>();
        Map<String, Traversal> previous = new HashMap<>();
        PriorityQueue<NodeCost> open = new PriorityQueue<>(
                Comparator.comparingDouble(NodeCost::cost).thenComparing(NodeCost::node));
        distances.put(source, 0.0D);
        open.add(new NodeCost(source, 0.0D));
        while (!open.isEmpty()) {
            NodeCost current = open.remove();
            if (current.cost > distances.getOrDefault(current.node, Double.MAX_VALUE)) continue;
            if (current.node.equals(target)) break;
            for (Corridor corridor : CORRIDORS) {
                boolean forward;
                String next;
                if (corridor.from.equals(current.node)) {
                    forward = true;
                    next = corridor.to;
                } else if (corridor.to.equals(current.node)) {
                    forward = false;
                    next = corridor.from;
                } else {
                    continue;
                }
                double candidate = current.cost + corridor.length();
                if (candidate >= distances.getOrDefault(next, Double.MAX_VALUE)) continue;
                distances.put(next, candidate);
                previous.put(next, new Traversal(corridor, forward, current.node, next));
                open.add(new NodeCost(next, candidate));
            }
        }
        if (!previous.containsKey(target)) return List.of();
        List<Traversal> reverse = new ArrayList<>();
        String cursor = target;
        while (!cursor.equals(source)) {
            Traversal traversal = previous.get(cursor);
            if (traversal == null) return List.of();
            reverse.add(traversal);
            cursor = traversal.previousNode;
        }
        List<Traversal> result = new ArrayList<>(reverse.size());
        for (int index = reverse.size() - 1; index >= 0; index--) result.add(reverse.get(index));
        return List.copyOf(result);
    }

    private static List<Point> reversed(List<Point> input) {
        List<Point> result = new ArrayList<>(input.size());
        for (int index = input.size() - 1; index >= 0; index--) result.add(input.get(index));
        return result;
    }

    private static List<ErdenTransportSavedData.RoutePoint> sample(List<Point> authored) {
        if (authored.isEmpty()) return List.of();
        List<ErdenTransportSavedData.RoutePoint> result = new ArrayList<>();
        Point first = authored.getFirst();
        result.add(new ErdenTransportSavedData.RoutePoint(first.x, first.z));
        for (int index = 1; index < authored.size(); index++) {
            Point a = authored.get(index - 1);
            Point b = authored.get(index);
            double length = Math.hypot((double) b.x - a.x, (double) b.z - a.z);
            int steps = Math.max(1, (int) Math.ceil(length / ROUTE_SAMPLE_METRES));
            for (int step = 1; step <= steps; step++) {
                double t = (double) step / steps;
                int x = (int) Math.round(a.x + (b.x - a.x) * t);
                int z = (int) Math.round(a.z + (b.z - a.z) * t);
                ErdenTransportSavedData.RoutePoint point = new ErdenTransportSavedData.RoutePoint(x, z);
                if (!result.getLast().equals(point)) result.add(point);
            }
        }
        return List.copyOf(result);
    }

    private static double distanceToSegment(
            double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax;
        double dz = bz - az;
        double lengthSquared = dx * dx + dz * dz;
        if (lengthSquared <= 0.0001D) return Math.hypot(px - ax, pz - az);
        double t = ((px - ax) * dx + (pz - az) * dz) / lengthSquared;
        t = Math.max(0.0D, Math.min(1.0D, t));
        return Math.hypot(px - (ax + dx * t), pz - (az + dz * t));
    }

    private static void validate() {
        if (CORRIDORS.size() != CORRIDOR_COUNT) {
            throw new IllegalStateException("Regional road corridor count drifted: " + CORRIDORS.size());
        }
        if (WAYSTATIONS.size() != WAYSTATION_COUNT) {
            throw new IllegalStateException("Regional road waystation count drifted: " + WAYSTATIONS.size());
        }
        Set<String> corridorIds = new HashSet<>();
        for (Corridor corridor : CORRIDORS) {
            if (!corridorIds.add(corridor.id)) {
                throw new IllegalStateException("Duplicate regional road corridor " + corridor.id);
            }
            if (!HUBS.containsKey(corridor.from) || !HUBS.containsKey(corridor.to)
                    || corridor.points.size() < 2
                    || !corridor.points.getFirst().equals(HUBS.get(corridor.from))
                    || !corridor.points.getLast().equals(HUBS.get(corridor.to))) {
                throw new IllegalStateException("Invalid regional road corridor geometry " + corridor.id);
            }
        }
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            String gate = capitalGateFor(settlement.id());
            if (route(settlement.id(), gate).size() < 2) {
                throw new IllegalStateException("Regional settlement disconnected from capital: " + settlement.id());
            }
        }
        if (route("pinewatch", "sunfield").size() < 2
                || route("ironvale", "harvest_crossing").size() < 2) {
            throw new IllegalStateException("Regional road graph is not nationally connected");
        }
    }

    public record Point(int x, int z) {
    }

    public record Corridor(String id, String from, String to, List<Point> points) {
        public Corridor {
            points = List.copyOf(points);
        }

        public double length() {
            double total = 0.0D;
            for (int index = 1; index < points.size(); index++) {
                Point a = points.get(index - 1);
                Point b = points.get(index);
                total += Math.hypot((double) b.x - a.x, (double) b.z - a.z);
            }
            return total;
        }
    }

    public record Waystation(String id, int x, int z, String name) {
    }

    private record NodeCost(String node, double cost) {
    }

    private record Traversal(
            Corridor corridor,
            boolean forward,
            String previousNode,
            String nextNode) {
    }
}
