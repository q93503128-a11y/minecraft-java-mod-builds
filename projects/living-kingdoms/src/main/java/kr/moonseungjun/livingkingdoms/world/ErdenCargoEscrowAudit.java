package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Fresh-world CI regression for authoritative cargo conservation. It is completely inert outside
 * the dedicated Living Kingdoms audit environment.
 */
public final class ErdenCargoEscrowAudit {
    public static final int AUDIT_REVISION = 1;

    private static final long AUDIT_TRAVEL_TICKS = 40L;
    private static final long AUDIT_AMOUNT = 1L;

    private static MinecraftServer activeServer;
    private static String jobId = "";
    private static String sourceId = "";
    private static String targetId = "";
    private static long expectedBreadTotal;
    private static boolean passed;

    private ErdenCargoEscrowAudit() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        if (passed) return;

        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        ErdenPhysicalEconomySavedData economy = level.getDataStorage()
                .computeIfAbsent(ErdenPhysicalEconomySavedData.TYPE);
        ErdenTransportSavedData transport = level.getDataStorage()
                .computeIfAbsent(ErdenTransportSavedData.TYPE);
        if (economy.lastProcessedDay() < 0L
                || economy.sites().size() != ErdenAuthoritativeEconomyManager.EXPECTED_SITES) {
            return;
        }

        if (jobId.isBlank()) {
            dispatchAuditCargo(level, economy, transport);
        } else {
            verifySettlement(economy, transport);
        }
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        jobId = "";
        sourceId = "";
        targetId = "";
        expectedBreadTotal = 0L;
        passed = false;
    }

    private static void dispatchAuditCargo(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy,
            ErdenTransportSavedData transport) {
        ErdenPhysicalEconomySavedData.SiteState source = economy.sites().stream()
                .filter(site -> site.role().equals("shop") && site.stock("bread") >= AUDIT_AMOUNT)
                .findFirst().orElse(null);
        ErdenPhysicalEconomySavedData.SiteState target = economy.sites().stream()
                .filter(site -> site.role().equals("bakery"))
                .findFirst().orElse(null);
        if (source == null || target == null) return;

        expectedBreadTotal = source.stock("bread") + target.stock("bread");
        sourceId = source.id();
        targetId = target.id();
        source = source
                .addStock("bread", -AUDIT_AMOUNT)
                .addMetric(ErdenCargoEscrowManager.inTransitMetric("bread"), AUDIT_AMOUNT)
                .addMetric("sent", AUDIT_AMOUNT);
        target = target.addMetric(
                ErdenCargoEscrowManager.pendingMetric("bread"), AUDIT_AMOUNT);
        economy.replaceSite(source);
        economy.replaceSite(target);

        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        List<ErdenTransportSavedData.RoutePoint> route = List.of(
                new ErdenTransportSavedData.RoutePoint(source.x(), source.z()),
                new ErdenTransportSavedData.RoutePoint(target.x(), target.z()));
        jobId = transport.nextJobId(day);
        transport.recordManifest(day, AUDIT_TRAVEL_TICKS);
        transport.addJob(new ErdenTransportSavedData.DeliveryJob(
                jobId, source.id(), target.id(), "bread", AUDIT_AMOUNT,
                level.getGameTime(), level.getGameTime(), "aggregate_moving",
                route, 0, 0, false, "", "", AUDIT_TRAVEL_TICKS, true));
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_ESCROW_AUDIT_DEPARTURE revision={} job={} source={} target={} resource=bread amount={} source_debited=true pending_recorded=true",
                AUDIT_REVISION, jobId, sourceId, targetId, AUDIT_AMOUNT);
    }

    private static void verifySettlement(
            ErdenPhysicalEconomySavedData economy,
            ErdenTransportSavedData transport) {
        ErdenTransportSavedData.DeliveryJob job = transport.jobs().stream()
                .filter(candidate -> candidate.id().equals(jobId))
                .findFirst().orElse(null);
        if (job == null || !job.status().equals("settled")) return;
        ErdenPhysicalEconomySavedData.SiteState source = findSite(economy, sourceId);
        ErdenPhysicalEconomySavedData.SiteState target = findSite(economy, targetId);
        if (source == null || target == null) {
            throw new IllegalStateException("Erden escrow audit sites disappeared");
        }
        long actualBreadTotal = source.stock("bread") + target.stock("bread");
        long inTransit = source.metric(ErdenCargoEscrowManager.inTransitMetric("bread"));
        long pending = target.metric(ErdenCargoEscrowManager.pendingMetric("bread"));
        if (actualBreadTotal != expectedBreadTotal
                || inTransit != 0L
                || pending != 0L
                || target.metric("transport_received") < AUDIT_AMOUNT) {
            throw new IllegalStateException(
                    "Erden escrow audit failed expected_bread=" + expectedBreadTotal
                            + " actual_bread=" + actualBreadTotal
                            + " in_transit=" + inTransit
                            + " pending=" + pending
                            + " received=" + target.metric("transport_received"));
        }
        passed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_ESCROW_AUDIT_PASS revision={} job={} departure=true unloading=true destination_credit=true cargo_conserved=true resource=bread amount={} source={} target={}",
                AUDIT_REVISION, jobId, AUDIT_AMOUNT, sourceId, targetId);
    }

    private static ErdenPhysicalEconomySavedData.SiteState findSite(
            ErdenPhysicalEconomySavedData economy,
            String id) {
        for (ErdenPhysicalEconomySavedData.SiteState site : economy.sites()) {
            if (site.id().equals(id)) return site;
        }
        return null;
    }
}
