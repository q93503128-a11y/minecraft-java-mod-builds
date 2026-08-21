package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.server.level.ServerLevel;

import java.util.Comparator;
import java.util.List;

/** Inserts regional-village surplus into the existing kingdom-supply shipment escrow. */
public final class ErdenRegionalSupplyBridge {
    private static final int MIN_TRAVEL_TICKS = 4_000;
    private static final int TICKS_PER_METRE = 6;
    private static final List<Point> CAPITAL_GATES = List.of(
            new Point(-1_200, 0),
            new Point(1_200, 0),
            new Point(0, -900),
            new Point(0, 900)
    );

    private ErdenRegionalSupplyBridge() {
    }

    public static long enqueue(
            ServerLevel level,
            ErdenPhysicalEconomySavedData capital,
            String sourceId,
            int sourceX,
            int sourceZ,
            String resource,
            long requested,
            long day) {
        long amount = Math.max(0L, requested);
        if (amount <= 0L
                || capital.sites().size() != ErdenAuthoritativeEconomyManager.EXPECTED_SITES) return 0L;
        ErdenKingdomSupplySavedData supply = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomSupplySavedData.TYPE);
        if (!supply.hasSupply(
                ErdenKingdomSupplyManager.SUPPLY_REVISION,
                ErdenKingdomSupplyManager.EXPECTED_NODES)) return 0L;

        ErdenPhysicalEconomySavedData.SiteState warehouse = capital.sites().stream()
                .filter(site -> site.role().equals("warehouse"))
                .min(Comparator.<ErdenPhysicalEconomySavedData.SiteState>comparingLong(site ->
                                distanceSquared(sourceX, sourceZ, site.x(), site.z())
                                        + site.metric("kingdom_supply_received") * 16L)
                        .thenComparing(ErdenPhysicalEconomySavedData.SiteState::id))
                .orElse(null);
        if (warehouse == null) return 0L;

        Point gate = CAPITAL_GATES.stream()
                .min(Comparator.comparingLong(point -> manhattan(sourceX, sourceZ, point.x(), point.z())))
                .orElse(CAPITAL_GATES.getFirst());
        long firstLeg = manhattan(sourceX, sourceZ, gate.x(), gate.z());
        long secondLeg = manhattan(gate.x(), gate.z(), warehouse.x(), warehouse.z());
        int routeMetres = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, firstLeg + secondLeg));
        long departureTick = level.getGameTime();
        long arrivalTick = departureTick + Math.max(
                MIN_TRAVEL_TICKS, (long) routeMetres * TICKS_PER_METRE);
        supply.addShipment(new ErdenKingdomSupplySavedData.ShipmentState(
                supply.nextShipmentId(day), sourceId, warehouse.id(), resource, amount,
                departureTick, arrivalTick, "in_transit", "wagon", routeMetres, false));
        return amount;
    }

    private static long manhattan(int x1, int z1, int x2, int z2) {
        return Math.abs((long) x1 - x2) + Math.abs((long) z1 - z2);
    }

    private static long distanceSquared(int x1, int z1, int x2, int z2) {
        long dx = (long) x1 - x2;
        long dz = (long) z1 - z2;
        return dx * dx + dz * dz;
    }

    private record Point(int x, int z) {
    }
}
