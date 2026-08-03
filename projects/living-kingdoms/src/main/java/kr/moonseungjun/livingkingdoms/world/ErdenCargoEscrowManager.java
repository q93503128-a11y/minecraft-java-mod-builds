package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

/**
 * Authoritative cargo escrow wrapped around the proven phase-one porter runtime. It creates only
 * player-near road jobs, settles completed cargo into the target, returns failed cargo to the
 * source, and lets unloaded districts use the aggregate economy without force-loading chunks.
 */
public final class ErdenCargoEscrowManager {
    public static final int TRANSPORT_REVISION = 2;

    private static final int PHYSICAL_RADIUS = 224;
    private static final int DESPAWN_RADIUS = 288;
    private static final int MAX_PHYSICAL_JOBS = 18;
    private static final int MAX_ROUTE_SEARCH = 120_000;
    private static final int MAX_ROUTE_POINTS = 320;
    private static final int LOADING_TICKS = 60;
    private static final int UNLOADING_TICKS = 60;
    private static final double PORTER_SPEED = 0.68D;
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private static MinecraftServer activeServer;
    private static boolean ciPassed;
    private static DispatchContext dispatchContext;

    private ErdenCargoEscrowManager() {
    }

    public enum DispatchResult {
        IMMEDIATE,
        DEFERRED,
        BLOCKED
    }

    public static void beginEconomyDay(ServerLevel level, long day) {
        if (dispatchContext != null) {
            throw new IllegalStateException("Erden cargo dispatch context was not closed");
        }
        dispatchContext = new DispatchContext(level, day);
    }

    public static void endEconomyDay() {
        DispatchContext context = dispatchContext;
        dispatchContext = null;
        if (context == null) return;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden road manifests day={} manifests={} modeled_travel_ticks={} physicalized={} blocked={} aggregate={} active_jobs={} authoritative_escrow=true unloaded_routes=aggregate",
                context.day, context.manifests, context.modeledTravelTicks,
                context.physicalized, context.blocked, context.aggregate,
                context.transport.activeJobCount());
    }

    public static DispatchResult dispatchTransfer(
            ErdenPhysicalEconomySavedData.SiteState source,
            ErdenPhysicalEconomySavedData.SiteState target,
            String resource,
            long amount) {
        DispatchContext context = dispatchContext;
        if (context == null || amount <= 0L || source.id().equals(target.id())) {
            return DispatchResult.IMMEDIATE;
        }

        long modeledTicks = estimatedTravelTicks(source, target);
        context.transport.recordManifest(context.day, modeledTicks);
        context.manifests++;
        context.modeledTravelTicks += modeledTicks;

        long currentDay = Math.floorDiv(context.level.getGameTime(), 24_000L);
        if (context.day != currentDay
                || context.transport.activeJobCount() >= MAX_PHYSICAL_JOBS
                || !nearAnyPlayer(context.level, source, target, PHYSICAL_RADIUS)) {
            context.aggregate++;
            return DispatchResult.IMMEDIATE;
        }

        List<ErdenTransportSavedData.RoutePoint> route = findRoute(context.level, source, target);
        if (route.isEmpty()) {
            context.blocked++;
            context.transport.markBlocked();
            return DispatchResult.BLOCKED;
        }

        long routeTicks = estimatedTravelTicks(route);
        ErdenTransportSavedData.DeliveryJob job = new ErdenTransportSavedData.DeliveryJob(
                context.transport.nextJobId(context.day),
                source.id(), target.id(), resource, amount,
                context.level.getGameTime(), context.level.getGameTime(),
                "loading", route, 0, 0, requiresCart(resource, amount),
                "", "", routeTicks, true);
        context.transport.addJob(job);
        context.physicalized++;
        return DispatchResult.DEFERRED;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) {
            activeServer = server;
            ciPassed = false;
            dispatchContext = null;
        }
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        ErdenPhysicalEconomySavedData economy = level.getDataStorage()
                .computeIfAbsent(ErdenPhysicalEconomySavedData.TYPE);
        ErdenTransportSavedData transport = level.getDataStorage()
                .computeIfAbsent(ErdenTransportSavedData.TYPE);
        if (economy.lastProcessedDay() < 0L) return;

        settleTerminalJobs(level, economy, transport);
        completeUnloadedJobs(level, transport);
        settleTerminalJobs(level, economy, transport);
        verifyCi(level, economy, transport);
    }

    private static void completeUnloadedJobs(
            ServerLevel level,
            ErdenTransportSavedData transport) {
        for (ErdenTransportSavedData.DeliveryJob job : transport.jobs()) {
            if (!job.authoritative() || job.terminal()) continue;
            int index = Math.min(job.waypointIndex(), Math.max(0, job.route().size() - 1));
            ErdenTransportSavedData.RoutePoint point = job.route().isEmpty()
                    ? null : job.route().get(index);
            if (point != null && nearAnyPlayer(level, point.x(), point.z(), DESPAWN_RADIUS)) continue;
            discardEntities(level, job);
            ErdenTransportSavedData.DeliveryJob aggregate =
                    job.withStatus("aggregate_moving", level.getGameTime()).withoutEntities();
            if (level.getGameTime() >= aggregate.dueTick()) {
                aggregate = aggregate.withStatus("completed", level.getGameTime());
            }
            transport.replaceJob(aggregate);
        }
    }

    private static void settleTerminalJobs(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy,
            ErdenTransportSavedData transport) {
        for (ErdenTransportSavedData.DeliveryJob job : transport.jobs()) {
            if (!job.authoritative()) continue;
            if (job.status().equals("completed")) {
                settleCompleted(level, economy, transport, job);
            } else if (job.status().equals("failed")) {
                returnFailed(level, economy, transport, job);
            }
        }
    }

    private static void settleCompleted(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy,
            ErdenTransportSavedData transport,
            ErdenTransportSavedData.DeliveryJob job) {
        ErdenPhysicalEconomySavedData.SiteState source = findSite(economy.sites(), job.sourceId());
        ErdenPhysicalEconomySavedData.SiteState target = findSite(economy.sites(), job.targetId());
        if (source == null || target == null) {
            transport.replaceJob(job.withStatus("settlement_failed", level.getGameTime()));
            return;
        }
        source = source
                .addMetric(inTransitMetric(job.resource()), -job.amount())
                .addMetric("transport_completed", 1L);
        target = target
                .addStock(job.resource(), job.amount())
                .addMetric(pendingMetric(job.resource()), -job.amount())
                .addMetric("received", job.amount())
                .addMetric("transport_received", job.amount())
                .addMetric("transport_ticks", Math.max(0L, level.getGameTime() - job.createdTick()));
        economy.replaceSite(source);
        economy.replaceSite(target);
        transport.replaceJob(job.withStatus("settled", level.getGameTime()).withoutEntities());
    }

    private static void returnFailed(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy,
            ErdenTransportSavedData transport,
            ErdenTransportSavedData.DeliveryJob job) {
        ErdenPhysicalEconomySavedData.SiteState source = findSite(economy.sites(), job.sourceId());
        ErdenPhysicalEconomySavedData.SiteState target = findSite(economy.sites(), job.targetId());
        if (source == null || target == null) {
            transport.replaceJob(job.withStatus("return_failed", level.getGameTime()));
            return;
        }
        source = source
                .addStock(job.resource(), job.amount())
                .addMetric(inTransitMetric(job.resource()), -job.amount())
                .addMetric("returned_cargo", job.amount())
                .addMetric("blocked_shipments", 1L);
        target = target
                .addMetric(pendingMetric(job.resource()), -job.amount())
                .addMetric("delivery_failures", 1L);
        economy.replaceSite(source);
        economy.replaceSite(target);
        transport.replaceJob(job.withStatus("returned", level.getGameTime()).withoutEntities());
    }

    private static List<ErdenTransportSavedData.RoutePoint> findRoute(
            ServerLevel level,
            ErdenPhysicalEconomySavedData.SiteState source,
            ErdenPhysicalEconomySavedData.SiteState target) {
        ExternalUrbanFabricBuilder.UrbanEntrance sourceEntrance = findEntrance(source.x(), source.z());
        ExternalUrbanFabricBuilder.UrbanEntrance targetEntrance = findEntrance(target.x(), target.z());
        if (sourceEntrance == null || targetEntrance == null) return List.of();
        ErdenTransportSavedData.RoutePoint start = nearestRoad(sourceEntrance.roadX(), sourceEntrance.roadZ());
        ErdenTransportSavedData.RoutePoint goal = nearestRoad(targetEntrance.roadX(), targetEntrance.roadZ());
        if (start == null || goal == null) return List.of();
        return findRoute(level, start, goal);
    }

    private static List<ErdenTransportSavedData.RoutePoint> findRoute(
            ServerLevel level,
            ErdenTransportSavedData.RoutePoint start,
            ErdenTransportSavedData.RoutePoint goal) {
        if (start.equals(goal)) return List.of(start, goal);
        long startKey = positionKey(start.x(), start.z());
        long goalKey = positionKey(goal.x(), goal.z());
        PriorityQueue<SearchNode> open = new PriorityQueue<>(
                Comparator.comparingDouble(SearchNode::score).thenComparingLong(SearchNode::key));
        Map<Long, Integer> costs = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        Set<Long> closed = new HashSet<>();
        costs.put(startKey, 0);
        open.add(new SearchNode(startKey, 0, heuristic(start.x(), start.z(), goal.x(), goal.z())));

        int searched = 0;
        while (!open.isEmpty() && searched++ < MAX_ROUTE_SEARCH) {
            SearchNode current = open.remove();
            if (!closed.add(current.key())) continue;
            if (current.key() == goalKey) {
                return compressRoute(reconstruct(previous, startKey, goalKey));
            }
            int x = unpackX(current.key());
            int z = unpackZ(current.key());
            for (int[] direction : DIRECTIONS) {
                int nextX = x + direction[0];
                int nextZ = z + direction[1];
                ErdenCapitalStreamingBuilder.RoadClass road =
                        ErdenCapitalStreamingBuilder.roadClassAt(nextX, nextZ);
                if (road == ErdenCapitalStreamingBuilder.RoadClass.NONE) continue;
                long nextKey = positionKey(nextX, nextZ);
                if (closed.contains(nextKey)) continue;
                int step = switch (road) {
                    case ROYAL -> 8;
                    case DISTRICT -> 9;
                    case LOCAL -> 10;
                    default -> 12;
                };
                int candidate = current.cost() + step;
                if (candidate >= costs.getOrDefault(nextKey, Integer.MAX_VALUE)) continue;
                costs.put(nextKey, candidate);
                previous.put(nextKey, current.key());
                double score = candidate + heuristic(nextX, nextZ, goal.x(), goal.z()) * 8.5D;
                open.add(new SearchNode(nextKey, candidate, score));
            }
        }
        return List.of();
    }

    private static ErdenTransportSavedData.RoutePoint nearestRoad(int x, int z) {
        if (ErdenCapitalStreamingBuilder.roadClassAt(x, z)
                != ErdenCapitalStreamingBuilder.RoadClass.NONE) {
            return new ErdenTransportSavedData.RoutePoint(x, z);
        }
        for (int radius = 1; radius <= 12; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz : new int[]{-radius, radius}) {
                    if (ErdenCapitalStreamingBuilder.roadClassAt(x + dx, z + dz)
                            != ErdenCapitalStreamingBuilder.RoadClass.NONE) {
                        return new ErdenTransportSavedData.RoutePoint(x + dx, z + dz);
                    }
                }
            }
            for (int dz = -radius + 1; dz < radius; dz++) {
                for (int dx : new int[]{-radius, radius}) {
                    if (ErdenCapitalStreamingBuilder.roadClassAt(x + dx, z + dz)
                            != ErdenCapitalStreamingBuilder.RoadClass.NONE) {
                        return new ErdenTransportSavedData.RoutePoint(x + dx, z + dz);
                    }
                }
            }
        }
        return null;
    }

    private static List<ErdenTransportSavedData.RoutePoint> reconstruct(
            Map<Long, Long> previous,
            long start,
            long goal) {
        List<ErdenTransportSavedData.RoutePoint> reversed = new ArrayList<>();
        long cursor = goal;
        reversed.add(new ErdenTransportSavedData.RoutePoint(unpackX(cursor), unpackZ(cursor)));
        while (cursor != start) {
            Long parent = previous.get(cursor);
            if (parent == null) return List.of();
            cursor = parent;
            reversed.add(new ErdenTransportSavedData.RoutePoint(unpackX(cursor), unpackZ(cursor)));
        }
        List<ErdenTransportSavedData.RoutePoint> route = new ArrayList<>(reversed.size());
        for (int index = reversed.size() - 1; index >= 0; index--) route.add(reversed.get(index));
        return route;
    }

    private static List<ErdenTransportSavedData.RoutePoint> compressRoute(
            List<ErdenTransportSavedData.RoutePoint> raw) {
        if (raw.size() <= 2) return raw;
        List<ErdenTransportSavedData.RoutePoint> result = new ArrayList<>();
        result.add(raw.getFirst());
        int lastDx = 0;
        int lastDz = 0;
        int sincePoint = 0;
        for (int index = 1; index < raw.size(); index++) {
            ErdenTransportSavedData.RoutePoint previous = raw.get(index - 1);
            ErdenTransportSavedData.RoutePoint current = raw.get(index);
            int dx = Integer.compare(current.x(), previous.x());
            int dz = Integer.compare(current.z(), previous.z());
            sincePoint++;
            boolean turn = index > 1 && (dx != lastDx || dz != lastDz);
            if (turn || sincePoint >= 8) {
                ErdenTransportSavedData.RoutePoint point = turn ? previous : current;
                if (!result.getLast().equals(point)) result.add(point);
                sincePoint = 0;
            }
            lastDx = dx;
            lastDz = dz;
        }
        if (!result.getLast().equals(raw.getLast())) result.add(raw.getLast());
        if (result.size() <= MAX_ROUTE_POINTS) return List.copyOf(result);
        List<ErdenTransportSavedData.RoutePoint> reduced = new ArrayList<>();
        double stride = (double) (result.size() - 1) / (MAX_ROUTE_POINTS - 1);
        for (int index = 0; index < MAX_ROUTE_POINTS; index++) {
            reduced.add(result.get((int) Math.round(index * stride)));
        }
        return List.copyOf(reduced);
    }

    private static void verifyCi(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy,
            ErdenTransportSavedData transport) {
        if (ciPassed
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || transport.lastManifestDay() < 0L
                || transport.totalManifests() <= 0L
                || transport.modeledTravelTicks() <= 0L) return;
        List<ExternalUrbanFabricBuilder.UrbanEntrance> samples =
                ErdenPhysicalEconomyManager.ciEntrances();
        if (samples.size() < 2) return;
        ErdenPhysicalEconomySavedData.SiteState source =
                findSite(economy.sites(), samples.get(0).x(), samples.get(0).z());
        ErdenPhysicalEconomySavedData.SiteState target =
                findSite(economy.sites(), samples.get(1).x(), samples.get(1).z());
        if (source == null || target == null || findRoute(level, source, target).isEmpty()) return;
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_TRANSPORT_PASS revision={} manifests={} modeled_travel_ticks={} route_planning=true loaded_obstacle_checks=true persistent_jobs=true authoritative_escrow=true aggregate_fallback=true physical_radius={} max_jobs={}",
                TRANSPORT_REVISION, transport.totalManifests(),
                transport.modeledTravelTicks(), PHYSICAL_RADIUS, MAX_PHYSICAL_JOBS);
    }

    private static boolean nearAnyPlayer(
            ServerLevel level,
            ErdenPhysicalEconomySavedData.SiteState source,
            ErdenPhysicalEconomySavedData.SiteState target,
            int radius) {
        int midX = (source.x() + target.x()) / 2;
        int midZ = (source.z() + target.z()) / 2;
        return nearAnyPlayer(level, source.x(), source.z(), radius)
                || nearAnyPlayer(level, target.x(), target.z(), radius)
                || nearAnyPlayer(level, midX, midZ, radius);
    }

    private static boolean nearAnyPlayer(ServerLevel level, int x, int z, int radius) {
        double radiusSquared = (double) radius * radius;
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - x;
            double dz = player.getZ() - z;
            if (dx * dx + dz * dz <= radiusSquared) return true;
        }
        return false;
    }

    private static boolean requiresCart(String resource, long amount) {
        return amount >= 4L || resource.equals("wheat") || resource.equals("iron")
                || resource.equals("hay") || resource.equals("bread");
    }

    private static long estimatedTravelTicks(
            ErdenPhysicalEconomySavedData.SiteState source,
            ErdenPhysicalEconomySavedData.SiteState target) {
        double distance = Math.sqrt(distanceSquared(
                source.x(), source.z(), target.x(), target.z()));
        return LOADING_TICKS + UNLOADING_TICKS
                + Math.max(40L, Math.round(distance / PORTER_SPEED));
    }

    private static long estimatedTravelTicks(List<ErdenTransportSavedData.RoutePoint> route) {
        if (route.size() < 2) return LOADING_TICKS + UNLOADING_TICKS + 40L;
        double distance = 0.0D;
        for (int index = 1; index < route.size(); index++) {
            ErdenTransportSavedData.RoutePoint previous = route.get(index - 1);
            ErdenTransportSavedData.RoutePoint current = route.get(index);
            distance += Math.sqrt(distanceSquared(
                    previous.x(), previous.z(), current.x(), current.z()));
        }
        return LOADING_TICKS + UNLOADING_TICKS
                + Math.max(40L, Math.round(distance / PORTER_SPEED));
    }

    private static void discardEntities(ServerLevel level, ErdenTransportSavedData.DeliveryJob job) {
        Entity porter = resolveEntity(level, job.porterUuid());
        Entity cart = resolveEntity(level, job.cartUuid());
        if (porter != null) porter.discard();
        if (cart != null) cart.discard();
    }

    private static Entity resolveEntity(ServerLevel level, String uuid) {
        if (uuid == null || uuid.isBlank()) return null;
        try {
            return level.getEntity(UUID.fromString(uuid));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static ErdenPhysicalEconomySavedData.SiteState findSite(
            List<ErdenPhysicalEconomySavedData.SiteState> sites,
            String id) {
        for (ErdenPhysicalEconomySavedData.SiteState site : sites) {
            if (site.id().equals(id)) return site;
        }
        return null;
    }

    private static ErdenPhysicalEconomySavedData.SiteState findSite(
            List<ErdenPhysicalEconomySavedData.SiteState> sites,
            int x,
            int z) {
        for (ErdenPhysicalEconomySavedData.SiteState site : sites) {
            if (site.x() == x && site.z() == z) return site;
        }
        return null;
    }

    private static ExternalUrbanFabricBuilder.UrbanEntrance findEntrance(int x, int z) {
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : ExternalUrbanFabricBuilder.entrances()) {
            if (entrance.x() == x && entrance.z() == z) return entrance;
        }
        return null;
    }

    public static String pendingMetric(String resource) {
        return "pending_" + resource;
    }

    public static String inTransitMetric(String resource) {
        return "in_transit_" + resource;
    }

    private static double heuristic(int x1, int z1, int x2, int z2) {
        return Math.abs((double) x1 - x2) + Math.abs((double) z1 - z2);
    }

    private static long positionKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) packed;
    }

    private static long distanceSquared(int x1, int z1, int x2, int z2) {
        long dx = (long) x1 - x2;
        long dz = (long) z1 - z2;
        return dx * dx + dz * dz;
    }

    private static final class DispatchContext {
        final ServerLevel level;
        final long day;
        final ErdenTransportSavedData transport;
        long manifests;
        long modeledTravelTicks;
        int physicalized;
        int blocked;
        int aggregate;

        DispatchContext(ServerLevel level, long day) {
            this.level = level;
            this.day = day;
            this.transport = level.getDataStorage()
                    .computeIfAbsent(ErdenTransportSavedData.TYPE);
        }
    }

    private record SearchNode(long key, int cost, double score) {
    }
}
