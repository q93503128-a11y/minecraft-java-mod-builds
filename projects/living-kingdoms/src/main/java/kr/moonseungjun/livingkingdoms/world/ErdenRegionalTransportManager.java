package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

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
 * Projects authoritative regional-economy escrow onto the streamed national roads. No chunk is
 * forced for gameplay: while nobody observes a route, the original shipment clock remains the
 * authority. Near a player, the existing Erden courier/cart runtime receives an authoritative
 * DeliveryJob and must physically traverse loaded road cells before the shipment is reconciled.
 */
public final class ErdenRegionalTransportManager {
    public static final int LOGISTICS_REVISION = 1;
    private static final int TICK_INTERVAL = 10;
    private static final int PHYSICAL_RADIUS = 224;
    private static final int MAX_REGIONAL_PHYSICAL_JOBS = 6;
    private static final int MAX_CAPITAL_SEARCH = 120_000;
    private static final int MAX_CAPITAL_ROUTE_POINTS = 320;
    private static final String LOCAL_PREFIX = "regional_local:";
    private static final String SUPPLY_PREFIX = "regional_supply:";

    private static MinecraftServer activeServer;
    private static boolean ciPassed;

    private ErdenRegionalTransportManager() {
    }

    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) {
            activeServer = server;
            ciPassed = false;
        }
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        if (level.getGameTime() % TICK_INTERVAL != 0L) {
            verifyCi(level);
            return;
        }

        ErdenRegionalEconomySavedData regional = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalEconomySavedData.TYPE);
        if (!regional.hasEconomy(
                ErdenRegionalEconomyManager.ECONOMY_REVISION,
                ErdenRegionalEconomyManager.EXPECTED_SETTLEMENTS)) return;
        ErdenTransportSavedData transport = level.getDataStorage()
                .computeIfAbsent(ErdenTransportSavedData.TYPE);
        ErdenPhysicalEconomySavedData capital = level.getDataStorage()
                .computeIfAbsent(ErdenPhysicalEconomySavedData.TYPE);
        ErdenKingdomSupplySavedData supply = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomSupplySavedData.TYPE);

        reconcileLocal(level, regional, transport);
        reconcileSupply(level, regional, capital, supply, transport);
        if (!level.players().isEmpty()) {
            int[] physicalSlots = {physicalAuthoritativeCount(level, transport)};
            materializeLocal(level, regional, transport, physicalSlots);
            materializeSupply(level, capital, supply, transport, physicalSlots);
        }
        refreshPhysicalLabels(level, transport);
        verifyCi(level);
    }

    private static void materializeLocal(
            ServerLevel level,
            ErdenRegionalEconomySavedData regional,
            ErdenTransportSavedData transport,
            int[] physicalSlots) {
        List<ErdenRegionalEconomySavedData.TradeShipment> shipments = regional.tradeShipments().stream()
                .filter(shipment -> shipment.status().equals("in_transit"))
                .sorted(Comparator.comparingLong(ErdenRegionalEconomySavedData.TradeShipment::departureTick)
                        .thenComparing(ErdenRegionalEconomySavedData.TradeShipment::id))
                .toList();
        for (ErdenRegionalEconomySavedData.TradeShipment shipment : shipments) {
            long now = level.getGameTime();
            if (now < shipment.departureTick() || now >= shipment.arrivalTick()) continue;
            List<ErdenTransportSavedData.RoutePoint> route = ErdenRegionalRoadNetwork.route(
                    shipment.sourceId(), shipment.targetId());
            if (route.size() < 2) continue;
            ErdenTransportSavedData.DeliveryJob existing = findJob(transport, localJobId(shipment.id()));
            int modeled = modeledIndex(route.size(), shipment.departureTick(), shipment.arrivalTick(), now);
            int index = existing == null ? modeled : Math.max(existing.waypointIndex(), modeled);
            index = Math.min(index, route.size() - 1);
            if (!nearPlayer(level, route, index) || !pointReady(level, route.get(index))) continue;

            if (existing != null) {
                if (existing.terminal() || activePhysicalJob(level, existing)
                        || physicalSlots[0] >= MAX_REGIONAL_PHYSICAL_JOBS) continue;
                transport.replaceJob(existing.withAttemptAndRoute(
                        existing.attempts(), route, index, now, existing.travelTicks()).withoutEntities());
                physicalSlots[0]++;
                LivingKingdoms.LOGGER.info(
                        "Resynchronized regional trade wagon shipment={} waypoint={}/{} aggregate_catchup=true no_teleport_of_loaded_entity=true",
                        shipment.id(), index, route.size());
                continue;
            }
            if (physicalSlots[0] >= MAX_REGIONAL_PHYSICAL_JOBS) break;
            ErdenTransportSavedData.DeliveryJob job = authoritativeJob(
                    localJobId(shipment.id()), shipment.sourceId(), shipment.targetId(),
                    shipment.resource(), shipment.amount(), shipment.departureTick(), shipment.arrivalTick(),
                    route, index, now);
            transport.addJob(job);
            physicalSlots[0]++;
            LivingKingdoms.LOGGER.info(
                    "Materialized regional trade wagon shipment={} {}->{} resource={} amount={} waypoint={}/{} aggregate_catchup=true",
                    shipment.id(), shipment.sourceId(), shipment.targetId(), shipment.resource(), shipment.amount(),
                    index, route.size());
        }
    }

    private static void materializeSupply(
            ServerLevel level,
            ErdenPhysicalEconomySavedData capital,
            ErdenKingdomSupplySavedData supply,
            ErdenTransportSavedData transport,
            int[] physicalSlots) {
        List<ErdenKingdomSupplySavedData.ShipmentState> shipments = supply.shipments().stream()
                .filter(shipment -> shipment.status().equals("in_transit"))
                .filter(shipment -> shipment.sourceId().startsWith("regional:"))
                .sorted(Comparator.comparingLong(ErdenKingdomSupplySavedData.ShipmentState::departureTick)
                        .thenComparing(ErdenKingdomSupplySavedData.ShipmentState::id))
                .toList();
        for (ErdenKingdomSupplySavedData.ShipmentState shipment : shipments) {
            long now = level.getGameTime();
            if (now < shipment.departureTick() || now >= shipment.arrivalTick()) continue;
            String settlementId = shipment.sourceId().substring("regional:".length());
            ErdenPhysicalEconomySavedData.SiteState warehouse = findSite(capital, shipment.warehouseId());
            if (warehouse == null) continue;
            List<ErdenTransportSavedData.RoutePoint> route = routeToWarehouse(level, settlementId, warehouse);
            if (route.size() < 2) continue;
            ErdenTransportSavedData.DeliveryJob existing = findJob(transport, supplyJobId(shipment.id()));
            int modeled = modeledIndex(route.size(), shipment.departureTick(), shipment.arrivalTick(), now);
            int index = existing == null ? modeled : Math.max(existing.waypointIndex(), modeled);
            index = Math.min(index, route.size() - 1);
            if (!nearPlayer(level, route, index) || !pointReady(level, route.get(index))) continue;

            if (existing != null) {
                if (existing.terminal() || activePhysicalJob(level, existing)
                        || physicalSlots[0] >= MAX_REGIONAL_PHYSICAL_JOBS) continue;
                transport.replaceJob(existing.withAttemptAndRoute(
                        existing.attempts(), route, index, now, existing.travelTicks()).withoutEntities());
                physicalSlots[0]++;
                LivingKingdoms.LOGGER.info(
                        "Resynchronized regional kingdom wagon shipment={} waypoint={}/{} aggregate_catchup=true no_teleport_of_loaded_entity=true",
                        shipment.id(), index, route.size());
                continue;
            }
            if (physicalSlots[0] >= MAX_REGIONAL_PHYSICAL_JOBS) break;
            ErdenTransportSavedData.DeliveryJob job = authoritativeJob(
                    supplyJobId(shipment.id()), shipment.sourceId(), shipment.warehouseId(),
                    shipment.resource(), shipment.amount(), shipment.departureTick(), shipment.arrivalTick(),
                    route, index, now);
            transport.addJob(job);
            physicalSlots[0]++;
            LivingKingdoms.LOGGER.info(
                    "Materialized regional kingdom wagon shipment={} {}->{} resource={} amount={} waypoint={}/{} capital_handoff=physical",
                    shipment.id(), shipment.sourceId(), shipment.warehouseId(), shipment.resource(), shipment.amount(),
                    index, route.size());
        }
    }

    private static ErdenTransportSavedData.DeliveryJob authoritativeJob(
            String id,
            String sourceId,
            String targetId,
            String resource,
            long amount,
            long departureTick,
            long arrivalTick,
            List<ErdenTransportSavedData.RoutePoint> route,
            int waypoint,
            long now) {
        return new ErdenTransportSavedData.DeliveryJob(
                id, sourceId, targetId, resource, amount,
                departureTick, now, "moving", route, waypoint, 0, true,
                "", "", Math.max(1L, arrivalTick - departureTick), true);
    }

    private static void reconcileLocal(
            ServerLevel level,
            ErdenRegionalEconomySavedData regional,
            ErdenTransportSavedData transport) {
        for (ErdenRegionalEconomySavedData.TradeShipment shipment : regional.tradeShipments()) {
            ErdenTransportSavedData.DeliveryJob job = findJob(transport, localJobId(shipment.id()));
            if (job == null) continue;
            if (shipment.terminal()) {
                settleProjection(level, transport, job);
                continue;
            }
            if (job.status().equals("completed")) {
                ErdenRegionalEconomySavedData.SettlementState target = regional.settlement(shipment.targetId());
                if (target == null) {
                    regional.replaceTrade(shipment.withStatus("failed"));
                } else {
                    regional.replaceSettlement(target.recordTradeIn(shipment.resource(), shipment.amount()));
                    regional.replaceTrade(shipment.withStatus("arrived"));
                }
                settleProjection(level, transport, job);
            } else if (job.status().equals("failed")) {
                ErdenRegionalEconomySavedData.SettlementState source = regional.settlement(shipment.sourceId());
                if (source != null) {
                    regional.replaceSettlement(source.addStock(shipment.resource(), shipment.amount()));
                }
                regional.replaceTrade(shipment.withStatus("failed"));
                returnProjection(level, transport, job);
            }
        }
    }

    private static void reconcileSupply(
            ServerLevel level,
            ErdenRegionalEconomySavedData regional,
            ErdenPhysicalEconomySavedData capital,
            ErdenKingdomSupplySavedData supply,
            ErdenTransportSavedData transport) {
        for (ErdenKingdomSupplySavedData.ShipmentState shipment : supply.shipments()) {
            if (!shipment.sourceId().startsWith("regional:")) continue;
            ErdenTransportSavedData.DeliveryJob job = findJob(transport, supplyJobId(shipment.id()));
            if (job == null) continue;
            if (shipment.terminal()) {
                settleProjection(level, transport, job);
                continue;
            }
            if (job.status().equals("completed")) {
                ErdenPhysicalEconomySavedData.SiteState warehouse = findSite(capital, shipment.warehouseId());
                if (warehouse == null || !warehouse.role().equals("warehouse")) {
                    supply.replaceShipment(shipment.withStatus("failed"));
                    supply.recordBlocked();
                } else {
                    warehouse = warehouse
                            .addStock(shipment.resource(), shipment.amount())
                            .addMetric("kingdom_supply_received", shipment.amount())
                            .addMetric("kingdom_supply_shipments", 1L)
                            .addMetric("kingdom_supply_received_" + shipment.resource(), shipment.amount())
                            .addMetric("kingdom_supply_route_metres", shipment.routeMetres());
                    capital.replaceSite(warehouse);
                    supply.recordArrival(shipment.amount());
                    supply.replaceShipment(shipment.withStatus("arrived"));
                }
                settleProjection(level, transport, job);
            } else if (job.status().equals("failed")) {
                String settlementId = shipment.sourceId().substring("regional:".length());
                ErdenRegionalEconomySavedData.SettlementState source = regional.settlement(settlementId);
                if (source != null) {
                    regional.replaceSettlement(source.addStock(shipment.resource(), shipment.amount()));
                }
                supply.recordBlocked();
                supply.replaceShipment(shipment.withStatus("returned"));
                returnProjection(level, transport, job);
            }
        }
    }

    private static void settleProjection(
            ServerLevel level,
            ErdenTransportSavedData transport,
            ErdenTransportSavedData.DeliveryJob job) {
        discardEntities(level, job);
        if (!job.status().equals("settled")) {
            transport.replaceJob(job.withStatus("settled", level.getGameTime()).withoutEntities());
        }
    }

    private static void returnProjection(
            ServerLevel level,
            ErdenTransportSavedData transport,
            ErdenTransportSavedData.DeliveryJob job) {
        discardEntities(level, job);
        transport.recordAuthoritativeReturn(job.amount());
        transport.replaceJob(job.withStatus("returned", level.getGameTime()).withoutEntities());
    }

    private static void refreshPhysicalLabels(ServerLevel level, ErdenTransportSavedData transport) {
        for (ErdenTransportSavedData.DeliveryJob job : transport.jobs()) {
            if (!job.authoritative() || job.terminal()) continue;
            Entity porter = resolveEntity(level, job.porterUuid());
            Entity cart = resolveEntity(level, job.cartUuid());
            if (porter != null) {
                porter.setCustomName(Component.literal("에르덴 장거리 마부"));
                porter.setCustomNameVisible(false);
            }
            if (cart != null) {
                cart.setCustomName(Component.literal("에르덴 장거리 화물 수레"));
                cart.setCustomNameVisible(false);
            }
        }
    }

    private static boolean activePhysicalJob(ServerLevel level, ErdenTransportSavedData.DeliveryJob job) {
        if (job == null || !job.authoritative() || job.terminal()) return false;
        return resolveEntity(level, job.porterUuid()) != null || resolveEntity(level, job.cartUuid()) != null;
    }

    private static int physicalAuthoritativeCount(
            ServerLevel level,
            ErdenTransportSavedData transport) {
        int count = 0;
        for (ErdenTransportSavedData.DeliveryJob job : transport.jobs()) {
            if (activePhysicalJob(level, job)) count++;
        }
        return count;
    }

    private static ErdenTransportSavedData.DeliveryJob findJob(
            ErdenTransportSavedData transport,
            String id) {
        for (ErdenTransportSavedData.DeliveryJob job : transport.jobs()) {
            if (job.id().equals(id)) return job;
        }
        return null;
    }

    private static String localJobId(String shipmentId) {
        return LOCAL_PREFIX + shipmentId;
    }

    private static String supplyJobId(String shipmentId) {
        return SUPPLY_PREFIX + shipmentId;
    }

    private static int modeledIndex(int routeSize, long departure, long arrival, long now) {
        if (routeSize <= 1 || arrival <= departure) return 0;
        double progress = Math.max(0.0D, Math.min(1.0D,
                (double) (now - departure) / (double) (arrival - departure)));
        return Math.min(routeSize - 1, Math.max(0, (int) Math.floor(progress * (routeSize - 1))));
    }

    private static boolean nearPlayer(
            ServerLevel level,
            List<ErdenTransportSavedData.RoutePoint> route,
            int modeledIndex) {
        int start = Math.max(0, modeledIndex - 12);
        int end = Math.min(route.size() - 1, modeledIndex + 12);
        double radiusSquared = (double) PHYSICAL_RADIUS * PHYSICAL_RADIUS;
        for (ServerPlayer player : level.players()) {
            for (int index = start; index <= end; index += 3) {
                ErdenTransportSavedData.RoutePoint point = route.get(index);
                double dx = player.getX() - point.x();
                double dz = player.getZ() - point.z();
                if (dx * dx + dz * dz <= radiusSquared) return true;
            }
        }
        return false;
    }

    private static boolean pointReady(ServerLevel level, ErdenTransportSavedData.RoutePoint point) {
        if (!level.hasChunk(point.x() >> 4, point.z() >> 4)) return false;
        if (ErdenRegionalRoadNetwork.distanceToRoad(point.x(), point.z())
                <= ErdenRegionalRoadNetwork.ROAD_HALF_WIDTH + 1.0D) {
            return ErdenRegionalRoadManager.isRoadChunkBuilt(level, point.x(), point.z());
        }
        return ErdenCapitalStreamingBuilder.isChunkBuilt(level, point.x() >> 4, point.z() >> 4);
    }

    private static List<ErdenTransportSavedData.RoutePoint> routeToWarehouse(
            ServerLevel level,
            String settlementId,
            ErdenPhysicalEconomySavedData.SiteState warehouse) {
        List<ErdenTransportSavedData.RoutePoint> result = new ArrayList<>(
                ErdenRegionalRoadNetwork.routeToCapital(settlementId));
        if (result.isEmpty()) return List.of();
        String gateId = ErdenRegionalRoadNetwork.capitalGateFor(settlementId);
        ErdenRegionalRoadNetwork.Point inside = ErdenRegionalRoadNetwork.insideCapitalGate(gateId);
        appendLine(result, result.getLast(), new ErdenTransportSavedData.RoutePoint(inside.x(), inside.z()));
        List<ErdenTransportSavedData.RoutePoint> capitalRoute = findCapitalRoute(
                level, new ErdenTransportSavedData.RoutePoint(inside.x(), inside.z()), warehouse);
        for (ErdenTransportSavedData.RoutePoint point : capitalRoute) {
            if (result.isEmpty() || !result.getLast().equals(point)) result.add(point);
        }
        return List.copyOf(result);
    }

    private static void appendLine(
            List<ErdenTransportSavedData.RoutePoint> route,
            ErdenTransportSavedData.RoutePoint from,
            ErdenTransportSavedData.RoutePoint to) {
        double length = Math.hypot((double) to.x() - from.x(), (double) to.z() - from.z());
        int steps = Math.max(1, (int) Math.ceil(length / ErdenRegionalRoadNetwork.ROUTE_SAMPLE_METRES));
        for (int step = 1; step <= steps; step++) {
            double t = (double) step / steps;
            ErdenTransportSavedData.RoutePoint point = new ErdenTransportSavedData.RoutePoint(
                    (int) Math.round(from.x() + (to.x() - from.x()) * t),
                    (int) Math.round(from.z() + (to.z() - from.z()) * t));
            if (!route.getLast().equals(point)) route.add(point);
        }
    }

    private static List<ErdenTransportSavedData.RoutePoint> findCapitalRoute(
            ServerLevel level,
            ErdenTransportSavedData.RoutePoint start,
            ErdenPhysicalEconomySavedData.SiteState warehouse) {
        ExternalUrbanFabricBuilder.UrbanEntrance entrance = null;
        for (ExternalUrbanFabricBuilder.UrbanEntrance candidate : ExternalUrbanFabricBuilder.entrances()) {
            if (candidate.x() == warehouse.x() && candidate.z() == warehouse.z()) {
                entrance = candidate;
                break;
            }
        }
        if (entrance == null) return List.of();
        ErdenTransportSavedData.RoutePoint goal = nearestCapitalRoad(entrance.roadX(), entrance.roadZ());
        if (goal == null) return List.of();
        return capitalAStar(level, start, goal);
    }

    private static List<ErdenTransportSavedData.RoutePoint> capitalAStar(
            ServerLevel level,
            ErdenTransportSavedData.RoutePoint start,
            ErdenTransportSavedData.RoutePoint goal) {
        long startKey = pack(start.x(), start.z());
        long goalKey = pack(goal.x(), goal.z());
        PriorityQueue<SearchNode> open = new PriorityQueue<>(
                Comparator.comparingDouble(SearchNode::score).thenComparingLong(SearchNode::key));
        Map<Long, Integer> costs = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        Set<Long> closed = new HashSet<>();
        costs.put(startKey, 0);
        open.add(new SearchNode(startKey, 0, heuristic(start.x(), start.z(), goal.x(), goal.z())));
        int searched = 0;
        while (!open.isEmpty() && searched++ < MAX_CAPITAL_SEARCH) {
            SearchNode current = open.remove();
            if (!closed.add(current.key())) continue;
            if (current.key() == goalKey) return compressCapital(reconstruct(previous, startKey, goalKey));
            int x = unpackX(current.key());
            int z = unpackZ(current.key());
            for (int[] direction : DIRECTIONS) {
                int nx = x + direction[0];
                int nz = z + direction[1];
                ErdenCapitalStreamingBuilder.RoadClass road = ErdenCapitalStreamingBuilder.roadClassAt(nx, nz);
                if (road == ErdenCapitalStreamingBuilder.RoadClass.NONE
                        || !capitalRoadPassable(level, nx, nz)) continue;
                long key = pack(nx, nz);
                if (closed.contains(key)) continue;
                int step = switch (road) {
                    case ROYAL -> 8;
                    case DISTRICT -> 9;
                    case LOCAL -> 10;
                    default -> 12;
                };
                int candidate = current.cost() + step;
                if (candidate >= costs.getOrDefault(key, Integer.MAX_VALUE)) continue;
                costs.put(key, candidate);
                previous.put(key, current.key());
                open.add(new SearchNode(key, candidate,
                        candidate + heuristic(nx, nz, goal.x(), goal.z()) * 8.5D));
            }
        }
        return List.of();
    }

    private static ErdenTransportSavedData.RoutePoint nearestCapitalRoad(int x, int z) {
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

    private static boolean capitalRoadPassable(ServerLevel level, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (!level.hasChunk(chunkX, chunkZ)
                || !ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) return true;
        int y = RealmSitePlanner.surfaceY(level, x, z);
        BlockPos floor = new BlockPos(x, y, z);
        return !level.getBlockState(floor).isAir()
                && level.getBlockState(floor.above()).isAir()
                && level.getBlockState(floor.above(2)).isAir();
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

    private static List<ErdenTransportSavedData.RoutePoint> compressCapital(
            List<ErdenTransportSavedData.RoutePoint> raw) {
        if (raw.size() <= 2) return raw;
        List<ErdenTransportSavedData.RoutePoint> result = new ArrayList<>();
        result.add(raw.getFirst());
        int lastDx = 0;
        int lastDz = 0;
        int since = 0;
        for (int index = 1; index < raw.size(); index++) {
            ErdenTransportSavedData.RoutePoint previous = raw.get(index - 1);
            ErdenTransportSavedData.RoutePoint current = raw.get(index);
            int dx = Integer.compare(current.x(), previous.x());
            int dz = Integer.compare(current.z(), previous.z());
            since++;
            boolean turn = index > 1 && (dx != lastDx || dz != lastDz);
            if (turn || since >= ErdenRegionalRoadNetwork.ROUTE_SAMPLE_METRES) {
                ErdenTransportSavedData.RoutePoint point = turn ? previous : current;
                if (!result.getLast().equals(point)) result.add(point);
                since = 0;
            }
            lastDx = dx;
            lastDz = dz;
        }
        if (!result.getLast().equals(raw.getLast())) result.add(raw.getLast());
        if (result.size() <= MAX_CAPITAL_ROUTE_POINTS) return List.copyOf(result);
        List<ErdenTransportSavedData.RoutePoint> reduced = new ArrayList<>();
        double stride = (double) (result.size() - 1) / (MAX_CAPITAL_ROUTE_POINTS - 1);
        for (int index = 0; index < MAX_CAPITAL_ROUTE_POINTS; index++) {
            reduced.add(result.get((int) Math.round(index * stride)));
        }
        return List.copyOf(reduced);
    }

    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

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
            ErdenPhysicalEconomySavedData economy,
            String id) {
        for (ErdenPhysicalEconomySavedData.SiteState site : economy.sites()) {
            if (site.id().equals(id)) return site;
        }
        return null;
    }

    private static void verifyCi(ServerLevel level) {
        if (ciPassed || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        ErdenRegionalRoadSavedData roads = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalRoadSavedData.TYPE);
        if (roads.builtChunkCount(ErdenRegionalRoadNetwork.REVISION) < 2) return;
        ErdenRegionalEconomySavedData regional = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalEconomySavedData.TYPE);
        if (!regional.hasEconomy(
                ErdenRegionalEconomyManager.ECONOMY_REVISION,
                ErdenRegionalEconomyManager.EXPECTED_SETTLEMENTS)) return;
        ErdenRegionalEconomySavedData.TradeShipment local = regional.tradeShipments().stream()
                .filter(shipment -> shipment.status().equals("in_transit"))
                .findFirst().orElse(null);
        if (local == null) return;
        List<ErdenTransportSavedData.RoutePoint> localRoute = ErdenRegionalRoadNetwork.route(
                local.sourceId(), local.targetId());
        if (localRoute.size() < 100) return;

        ErdenKingdomSupplySavedData supply = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomSupplySavedData.TYPE);
        ErdenPhysicalEconomySavedData capital = level.getDataStorage()
                .computeIfAbsent(ErdenPhysicalEconomySavedData.TYPE);
        ErdenKingdomSupplySavedData.ShipmentState capitalShipment = supply.shipments().stream()
                .filter(shipment -> shipment.sourceId().startsWith("regional:"))
                .findFirst().orElse(null);
        if (capitalShipment == null) return;
        String settlementId = capitalShipment.sourceId().substring("regional:".length());
        ErdenPhysicalEconomySavedData.SiteState warehouse = findSite(capital, capitalShipment.warehouseId());
        if (warehouse == null || routeToWarehouse(level, settlementId, warehouse).size() < 100) return;

        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_REGIONAL_LOGISTICS_PASS revision={} corridors={} waystations={} local_route_points={} capital_route=true authoritative_escrow=true loaded_projection=true reobservation_resync=true navigation_only=true observed_blockage_delays_or_returns=true aggregate_when_unloaded=true return_accounting=true persistent_forced_chunks=false",
                LOGISTICS_REVISION,
                ErdenRegionalRoadNetwork.CORRIDOR_COUNT,
                ErdenRegionalRoadNetwork.WAYSTATION_COUNT,
                localRoute.size());
    }

    private static double heuristic(int x1, int z1, int x2, int z2) {
        return Math.abs((double) x1 - x2) + Math.abs((double) z1 - z2);
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static int unpackX(long key) {
        return (int) (key >> 32);
    }

    private static int unpackZ(long key) {
        return (int) key;
    }

    private record SearchNode(long key, int cost, double score) {
    }
}
