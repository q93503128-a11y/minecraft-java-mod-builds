package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Replaces fixed daily warehouse imports with persistent rural production and shipment escrow.
 * Capital warehouses receive resources only after a recorded convoy reaches its destination.
 */
public final class ErdenKingdomSupplyManager {
    public static final int SUPPLY_REVISION = 1;
    public static final int EXPECTED_NODES = 18;
    public static final int EXPECTED_PRODUCERS = 15;
    public static final int EXPECTED_WHARVES = 3;
    public static final int EXPECTED_RESOURCES = 6;
    public static final int EXPECTED_OPENING_CONVOYS = 18;

    private static final int MAX_CATCH_UP_DAYS = 30;
    private static final long SETTLED_RETENTION_TICKS = 7L * 24_000L;
    private static final int MIN_TRAVEL_TICKS = 4_000;
    private static final int TICKS_PER_METRE = 6;

    private static final List<ErdenKingdomSupplyCatalog.SupplyNode> NODES =
            ErdenKingdomSupplyCatalog.nodes();

    private static final List<OutputRate> OUTPUTS = List.of(
            new OutputRate("grain_estate", "wheat", 48L, 96L),
            new OutputRate("ranch", "leather", 40L, 80L),
            new OutputRate("ranch", "hay", 24L, 48L),
            new OutputRate("colliery", "coal", 28L, 72L),
            new OutputRate("iron_mine", "iron", 16L, 24L),
            new OutputRate("paper_mill", "paper", 40L, 80L)
    );

    private static final List<Point> CAPITAL_GATES = List.of(
            new Point(-1_200, 0),
            new Point(1_200, 0),
            new Point(0, -900),
            new Point(0, 900)
    );

    private static boolean ciPassed;
    private static ServerLevel activeLevel;

    private ErdenKingdomSupplyManager() {
    }

    /** Runs before the capital economy copies worksite inventories for the current day. */
    public static void prepareBeforeCityEconomy(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy) {
        if (activeLevel != level) {
            activeLevel = level;
            ciPassed = false;
        }
        if (economy.sites().size() != ErdenAuthoritativeEconomyManager.EXPECTED_SITES) return;

        ErdenKingdomSupplySavedData supply = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomSupplySavedData.TYPE);
        long currentDay = Math.floorDiv(level.getGameTime(), 24_000L);
        if (!supply.hasSupply(SUPPLY_REVISION, EXPECTED_NODES)) {
            initializeSupply(level, economy, supply, currentDay);
        }

        settleArrivals(level, economy, supply);
        processMissingDays(level, economy, supply, currentDay);
        settleArrivals(level, economy, supply);
        supply.pruneSettled(Math.max(0L, level.getGameTime() - SETTLED_RETENTION_TICKS));
        verifyCi(level, economy, supply);
    }

    public static boolean isReady(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy) {
        ErdenKingdomSupplySavedData supply = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomSupplySavedData.TYPE);
        return supply.hasSupply(SUPPLY_REVISION, EXPECTED_NODES)
                && supply.openingConvoys() == EXPECTED_OPENING_CONVOYS
                && supply.totalProduced() > 0L
                && supply.totalDispatched() > 0L
                && supply.totalReceived() > 0L
                && receivedResourceCount(economy) == EXPECTED_RESOURCES;
    }

    private static void initializeSupply(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy,
            ErdenKingdomSupplySavedData supply,
            long currentDay) {
        List<ErdenKingdomSupplySavedData.NodeState> nodes = new ArrayList<>();
        for (NodeTemplate template : NODES) {
            ErdenKingdomSupplySavedData.NodeState node = new ErdenKingdomSupplySavedData.NodeState(
                    template.id, template.x, template.z, template.role,
                    List.of(), currentDay - 1L, 0L, 0L);
            for (OutputRate output : outputsFor(template.role)) {
                node = node.produce(output.resource, output.openingStock, currentDay - 1L);
            }
            nodes.add(node);
        }
        supply.initialize(SUPPLY_REVISION, currentDay - 1L, nodes);
        dispatchOpeningConvoys(level, economy, supply, currentDay);
        settleArrivals(level, economy, supply);
        LivingKingdoms.LOGGER.info(
                "Prepared Erden kingdom supply nodes={} producers={} wharves={} opening_convoys={} fixed_daily_imports=false shipment_escrow=true",
                EXPECTED_NODES, EXPECTED_PRODUCERS, EXPECTED_WHARVES,
                supply.openingConvoys());
    }

    private static void dispatchOpeningConvoys(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy,
            ErdenKingdomSupplySavedData supply,
            long day) {
        for (ErdenKingdomSupplySavedData.NodeState snapshot : supply.nodes()) {
            ErdenKingdomSupplySavedData.NodeState node = snapshot;
            for (ErdenKingdomSupplySavedData.ResourceStock stock : List.copyOf(node.stocks())) {
                if (stock.amount() <= 0L) continue;
                ErdenPhysicalEconomySavedData.SiteState warehouse = nearestWarehouse(economy, node);
                if (warehouse == null) continue;
                int routeMetres = routeMetres(node, warehouse);
                String mode = transportMode(node.role());
                ErdenKingdomSupplySavedData.ShipmentState shipment =
                        new ErdenKingdomSupplySavedData.ShipmentState(
                                supply.nextShipmentId(day),
                                node.id(), warehouse.id(), stock.resource(), stock.amount(),
                                level.getGameTime(), level.getGameTime(),
                                "in_transit", mode, routeMetres, true);
                node = node.addStock(stock.resource(), -stock.amount());
                supply.addShipment(shipment);
            }
            supply.replaceNode(node);
        }
    }

    private static void processMissingDays(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy,
            ErdenKingdomSupplySavedData supply,
            long currentDay) {
        long firstDay = Math.max(
                supply.lastProcessedDay() + 1L,
                currentDay - MAX_CATCH_UP_DAYS + 1L);
        for (long day = firstDay; day <= currentDay; day++) {
            long produced = produceDay(supply, day);
            long dispatched = dispatchDay(level, economy, supply, day);
            supply.markProcessedDay(day, produced);
            if (day == currentDay || day % 7L == 0L) {
                LivingKingdoms.LOGGER.info(
                        "Processed Erden kingdom supply day={} produced={} dispatched={} in_transit={} blocked={} fixed_daily_imports=false",
                        day, produced, dispatched, inTransitCount(supply), supply.totalBlocked());
            }
        }
    }

    private static long produceDay(
            ErdenKingdomSupplySavedData supply,
            long day) {
        long produced = 0L;
        int percentage = productionPercentage(day);
        for (ErdenKingdomSupplySavedData.NodeState snapshot : supply.nodes()) {
            if (snapshot.role().equals("river_wharf") || snapshot.lastProducedDay() >= day) continue;
            ErdenKingdomSupplySavedData.NodeState node = snapshot;
            for (OutputRate output : outputsFor(node.role())) {
                long amount = Math.max(1L, output.dailyAmount * percentage / 100L);
                node = node.produce(output.resource, amount, day);
                produced += amount;
            }
            supply.replaceNode(node);
        }
        return produced;
    }

    private static long dispatchDay(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy,
            ErdenKingdomSupplySavedData supply,
            long day) {
        long dispatched = 0L;
        for (ErdenKingdomSupplySavedData.NodeState snapshot : supply.nodes()) {
            if (snapshot.role().equals("river_wharf")) continue;
            ErdenKingdomSupplySavedData.NodeState node = snapshot;
            if (routeBlocked(node, day)) {
                supply.replaceNode(node.markBlocked());
                supply.recordBlocked();
                continue;
            }
            ErdenPhysicalEconomySavedData.SiteState warehouse = nearestWarehouse(economy, node);
            if (warehouse == null) continue;
            for (ErdenKingdomSupplySavedData.ResourceStock stock : List.copyOf(node.stocks())) {
                if (stock.amount() <= 0L) continue;
                int routeMetres = routeMetres(node, warehouse);
                long departureTick = day * 24_000L + 2_000L;
                long arrivalTick = departureTick + Math.max(
                        MIN_TRAVEL_TICKS,
                        (long) routeMetres * TICKS_PER_METRE);
                ErdenKingdomSupplySavedData.ShipmentState shipment =
                        new ErdenKingdomSupplySavedData.ShipmentState(
                                supply.nextShipmentId(day),
                                node.id(), warehouse.id(), stock.resource(), stock.amount(),
                                departureTick, arrivalTick,
                                "in_transit", transportMode(node.role()), routeMetres, false);
                node = node.addStock(stock.resource(), -stock.amount());
                supply.addShipment(shipment);
                dispatched += stock.amount();
            }
            supply.replaceNode(node);
        }
        return dispatched;
    }

    private static void settleArrivals(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy,
            ErdenKingdomSupplySavedData supply) {
        long now = level.getGameTime();
        for (ErdenKingdomSupplySavedData.ShipmentState shipment : supply.shipments()) {
            if (shipment.terminal()
                    || !shipment.status().equals("in_transit")
                    || shipment.arrivalTick() > now) continue;
            ErdenPhysicalEconomySavedData.SiteState warehouse = findSite(
                    economy.sites(), shipment.warehouseId());
            if (warehouse == null || !warehouse.role().equals("warehouse")) {
                supply.replaceShipment(shipment.withStatus("failed"));
                continue;
            }
            warehouse = warehouse
                    .addStock(shipment.resource(), shipment.amount())
                    .addMetric("kingdom_supply_received", shipment.amount())
                    .addMetric("kingdom_supply_shipments", 1L)
                    .addMetric("kingdom_supply_received_" + shipment.resource(), shipment.amount())
                    .addMetric("kingdom_supply_route_metres", shipment.routeMetres());
            economy.replaceSite(warehouse);
            supply.recordArrival(shipment.amount());
            supply.replaceShipment(shipment.withStatus("arrived"));
        }
    }

    private static ErdenPhysicalEconomySavedData.SiteState nearestWarehouse(
            ErdenPhysicalEconomySavedData economy,
            ErdenKingdomSupplySavedData.NodeState node) {
        return economy.sites().stream()
                .filter(site -> site.role().equals("warehouse"))
                .min(Comparator.<ErdenPhysicalEconomySavedData.SiteState>comparingLong(site ->
                                distanceSquared(node.x(), node.z(), site.x(), site.z())
                                        + site.metric("kingdom_supply_received") * 16L)
                        .thenComparing(ErdenPhysicalEconomySavedData.SiteState::id))
                .orElse(null);
    }

    private static int routeMetres(
            ErdenKingdomSupplySavedData.NodeState node,
            ErdenPhysicalEconomySavedData.SiteState warehouse) {
        Point transfer = node.role().equals("paper_mill")
                ? nearestWharf(node.x(), node.z())
                : nearestGate(node.x(), node.z());
        long firstLeg = manhattan(node.x(), node.z(), transfer.x, transfer.z);
        long secondLeg = manhattan(transfer.x, transfer.z, warehouse.x(), warehouse.z());
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, firstLeg + secondLeg));
    }

    private static Point nearestGate(int x, int z) {
        return CAPITAL_GATES.stream()
                .min(Comparator.comparingLong(point -> manhattan(x, z, point.x, point.z)))
                .orElse(CAPITAL_GATES.getFirst());
    }

    private static Point nearestWharf(int x, int z) {
        return NODES.stream()
                .filter(node -> node.role.equals("river_wharf"))
                .map(node -> new Point(node.x, node.z))
                .min(Comparator.comparingLong(point -> manhattan(x, z, point.x, point.z)))
                .orElseGet(() -> nearestGate(x, z));
    }

    private static List<OutputRate> outputsFor(String role) {
        return OUTPUTS.stream().filter(output -> output.role.equals(role)).toList();
    }

    private static String transportMode(String role) {
        return role.equals("paper_mill") ? "barge" : "wagon";
    }

    private static int productionPercentage(long day) {
        return switch ((int) Math.floorMod(day, 7L)) {
            case 0 -> 100;
            case 1 -> 95;
            case 2 -> 105;
            case 3 -> 90;
            case 4 -> 110;
            case 5 -> 100;
            default -> 85;
        };
    }

    private static boolean routeBlocked(
            ErdenKingdomSupplySavedData.NodeState node,
            long day) {
        long seed = (long) node.id().hashCode() * 31L + day * 17L;
        return Math.floorMod(seed, 19L) == 0L;
    }

    private static int inTransitCount(ErdenKingdomSupplySavedData supply) {
        int count = 0;
        for (ErdenKingdomSupplySavedData.ShipmentState shipment : supply.shipments()) {
            if (shipment.status().equals("in_transit")) count++;
        }
        return count;
    }

    private static int receivedWarehouseCount(ErdenPhysicalEconomySavedData economy) {
        int count = 0;
        for (ErdenPhysicalEconomySavedData.SiteState site : economy.sites()) {
            if (site.role().equals("warehouse") && site.metric("kingdom_supply_received") > 0L) count++;
        }
        return count;
    }

    private static int receivedResourceCount(ErdenPhysicalEconomySavedData economy) {
        Set<String> resources = new HashSet<>();
        for (ErdenPhysicalEconomySavedData.SiteState site : economy.sites()) {
            if (!site.role().equals("warehouse")) continue;
            for (OutputRate output : OUTPUTS) {
                if (site.metric("kingdom_supply_received_" + output.resource) > 0L) {
                    resources.add(output.resource);
                }
            }
        }
        return resources.size();
    }

    private static void verifyCi(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy,
            ErdenKingdomSupplySavedData supply) {
        if (ciPassed
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || !isReady(level, economy)
                || supply.lastProcessedDay() < 0L
                || receivedWarehouseCount(economy) < 4
                || inTransitCount(supply) <= 0) return;
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_KINGDOM_SUPPLY_PASS revision={} nodes={} producers={} wharves={} resources={} opening_convoys={} produced={} dispatched={} received={} blocked={} warehouses_supplied={} active_shipments={} fixed_daily_imports=false shipment_escrow=true route_modes=wagon,barge",
                SUPPLY_REVISION, EXPECTED_NODES, EXPECTED_PRODUCERS, EXPECTED_WHARVES,
                EXPECTED_RESOURCES, supply.openingConvoys(), supply.totalProduced(),
                supply.totalDispatched(), supply.totalReceived(), supply.totalBlocked(),
                receivedWarehouseCount(economy), inTransitCount(supply));
    }

    private static ErdenPhysicalEconomySavedData.SiteState findSite(
            List<ErdenPhysicalEconomySavedData.SiteState> sites,
            String id) {
        for (ErdenPhysicalEconomySavedData.SiteState site : sites) {
            if (site.id().equals(id)) return site;
        }
        return null;
    }

    private static long manhattan(int x1, int z1, int x2, int z2) {
        return Math.abs((long) x1 - x2) + Math.abs((long) z1 - z2);
    }

    private static long distanceSquared(int x1, int z1, int x2, int z2) {
        long dx = (long) x1 - x2;
        long dz = (long) z1 - z2;
        return dx * dx + dz * dz;
    }


    private record OutputRate(String role, String resource, long dailyAmount, long openingStock) {
    }

    private record Point(int x, int z) {
    }
}
