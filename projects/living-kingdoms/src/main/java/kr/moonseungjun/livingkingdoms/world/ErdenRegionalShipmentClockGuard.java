package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.UUID;

/**
 * Prevents a visible wagon from being silently delivered by its aggregate clock while the player
 * is watching it. The due tick is only held a short distance ahead while a real courier/cart
 * entity exists; once the route is no longer observed, aggregate simulation resumes immediately.
 */
public final class ErdenRegionalShipmentClockGuard {
    private static final long HOLD_AHEAD_TICKS = 100L;
    private static final String LOCAL_PREFIX = "regional_local:";
    private static final String SUPPLY_PREFIX = "regional_supply:";

    private static MinecraftServer activeServer;

    private ErdenRegionalShipmentClockGuard() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) activeServer = server;
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        ErdenTransportSavedData transport = level.getDataStorage()
                .computeIfAbsent(ErdenTransportSavedData.TYPE);
        ErdenRegionalEconomySavedData regional = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalEconomySavedData.TYPE);
        ErdenKingdomSupplySavedData supply = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomSupplySavedData.TYPE);
        long holdUntil = level.getGameTime() + HOLD_AHEAD_TICKS;

        for (ErdenTransportSavedData.DeliveryJob job : transport.jobs()) {
            if (!job.authoritative() || job.terminal() || !physicallyPresent(level, job)) continue;
            if (job.id().startsWith(LOCAL_PREFIX)) {
                String shipmentId = job.id().substring(LOCAL_PREFIX.length());
                for (ErdenRegionalEconomySavedData.TradeShipment shipment : regional.tradeShipments()) {
                    if (!shipment.id().equals(shipmentId)
                            || !shipment.status().equals("in_transit")
                            || shipment.arrivalTick() >= holdUntil) continue;
                    regional.replaceTrade(new ErdenRegionalEconomySavedData.TradeShipment(
                            shipment.id(), shipment.sourceId(), shipment.targetId(), shipment.resource(),
                            shipment.amount(), shipment.departureTick(), holdUntil,
                            shipment.status(), shipment.routeMetres()));
                    break;
                }
            } else if (job.id().startsWith(SUPPLY_PREFIX)) {
                String shipmentId = job.id().substring(SUPPLY_PREFIX.length());
                for (ErdenKingdomSupplySavedData.ShipmentState shipment : supply.shipments()) {
                    if (!shipment.id().equals(shipmentId)
                            || !shipment.status().equals("in_transit")
                            || shipment.arrivalTick() >= holdUntil) continue;
                    supply.replaceShipment(new ErdenKingdomSupplySavedData.ShipmentState(
                            shipment.id(), shipment.sourceId(), shipment.warehouseId(), shipment.resource(),
                            shipment.amount(), shipment.departureTick(), holdUntil,
                            shipment.status(), shipment.mode(), shipment.routeMetres(), shipment.openingConvoy()));
                    break;
                }
            }
        }
    }

    private static boolean physicallyPresent(
            ServerLevel level,
            ErdenTransportSavedData.DeliveryJob job) {
        return resolve(level, job.porterUuid()) != null || resolve(level, job.cartUuid()) != null;
    }

    private static Entity resolve(ServerLevel level, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return level.getEntity(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
