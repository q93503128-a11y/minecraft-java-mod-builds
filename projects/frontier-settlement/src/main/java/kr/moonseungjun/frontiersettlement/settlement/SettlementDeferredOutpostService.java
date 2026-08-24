package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Bounded coarse unloaded-work accounting.
 *
 * Alpha.42 never stores wood, stone, ore, fish, food or cargo as a virtual number. While an eligible
 * outpost or route is unloaded during normal work hours, only elapsed work-time debt is persisted.
 * When the relevant chunks load again, existing physical workers may redeem that debt by harvesting
 * real world blocks/crops/water catches or by carrying a larger real ItemStack on the existing road.
 */
public final class SettlementDeferredOutpostService {
    public static final int SAMPLE_TICKS = 200;
    public static final long MAX_PRODUCTION_TICKS = 24_000L;
    public static final long MAX_LOGISTICS_TICKS = 24_000L;
    public static final long LOGISTICS_CREDIT_TICKS = 1_200L;
    public static final String OVERLAY_GENERAL = "general";
    public static final String OVERLAY_FISHING = "fishing";
    public static final String OVERLAY_MILITARY = "military";

    private SettlementDeferredOutpostService() {}

    public record Snapshot(int productionBacklogOutposts,
                           int logisticsBacklogOutposts,
                           long productionTicks,
                           long logisticsTicks) {}

    public static void tick(MinecraftServer server, SettlementData settlement) {
        if (server.getTickCount() % SAMPLE_TICKS != 0) return;
        ServerLevel level = server.overworld();
        SettlementDeferredOutpostData deferred = SettlementDeferredOutpostData.get(server);

        for (OutpostRecord outpost : settlement.outposts()) {
            OutpostDeferredState state = deferred.stateFor(outpost.id());
            boolean localLoaded = level.hasChunkAt(outpost.center()) && level.hasChunkAt(outpost.stockpile());
            boolean routeLoaded = SettlementOutpostLogisticsService.routeFullyLoaded(level, settlement, outpost);
            OutpostDeferredState next = state;

            if (!localLoaded && productionEligible(outpost, state)) {
                next = next.withProductionTicks(Math.min(MAX_PRODUCTION_TICKS,
                        next.productionTicks() + SAMPLE_TICKS));
            }
            if (state.transportObserved() && !routeLoaded) {
                next = next.withLogisticsTicks(Math.min(MAX_LOGISTICS_TICKS,
                        next.logisticsTicks() + SAMPLE_TICKS));
            }
            deferred.replace(next);
        }
    }

    public static void observeGeneralOverlay(MinecraftServer server, OutpostRecord outpost, String overlay) {
        if (!"general".equals(outpost.specialization())) return;
        String normalized = normalizeOverlay(overlay);
        SettlementDeferredOutpostData data = SettlementDeferredOutpostData.get(server);
        OutpostDeferredState state = data.stateFor(outpost.id());
        if (!normalized.equals(state.observedOverlay())) data.replace(state.withObservedOverlay(normalized));
    }

    public static void observeTransportReady(MinecraftServer server, OutpostRecord outpost) {
        SettlementDeferredOutpostData data = SettlementDeferredOutpostData.get(server);
        OutpostDeferredState state = data.stateFor(outpost.id());
        if (!state.transportObserved()) data.replace(state.withTransportObserved(true));
    }

    public static boolean hasProductionCredit(MinecraftServer server, OutpostRecord outpost, int workPeriodTicks) {
        if (workPeriodTicks <= 0) return false;
        return SettlementDeferredOutpostData.get(server).stateFor(outpost.id()).productionTicks() >= workPeriodTicks;
    }

    /** Consume only after a real loaded harvest/catch actually produced a physical ItemStack. */
    public static boolean consumeProductionCredit(MinecraftServer server, OutpostRecord outpost, int workPeriodTicks) {
        if (workPeriodTicks <= 0) return false;
        SettlementDeferredOutpostData data = SettlementDeferredOutpostData.get(server);
        OutpostDeferredState state = data.stateFor(outpost.id());
        if (state.productionTicks() < workPeriodTicks) return false;
        data.replace(state.withProductionTicks(state.productionTicks() - workPeriodTicks));
        return true;
    }

    /**
     * A logistics credit never is cargo. It can only raise the next physical pickup's batch cap;
     * actual source/container extraction and road travel remain owned by SettlementOutpostLogisticsService.
     */
    public static int adjustedTransportBatch(MinecraftServer server, OutpostRecord outpost, int normalBatch) {
        int base = Math.max(1, normalBatch);
        OutpostDeferredState state = SettlementDeferredOutpostData.get(server).stateFor(outpost.id());
        if (state.logisticsTicks() < LOGISTICS_CREDIT_TICKS) return base;
        return Math.min(64, base * 2);
    }

    /** Consume only when a physical pickup actually exceeded the normal loaded batch. */
    public static boolean consumeLogisticsCredit(MinecraftServer server, OutpostRecord outpost) {
        SettlementDeferredOutpostData data = SettlementDeferredOutpostData.get(server);
        OutpostDeferredState state = data.stateFor(outpost.id());
        if (state.logisticsTicks() < LOGISTICS_CREDIT_TICKS) return false;
        data.replace(state.withLogisticsTicks(state.logisticsTicks() - LOGISTICS_CREDIT_TICKS));
        return true;
    }

    public static Snapshot snapshot(MinecraftServer server, SettlementData settlement) {
        SettlementDeferredOutpostData data = SettlementDeferredOutpostData.get(server);
        int productionOutposts = 0;
        int logisticsOutposts = 0;
        long productionTicks = 0L;
        long logisticsTicks = 0L;
        for (OutpostRecord outpost : settlement.outposts()) {
            OutpostDeferredState state = data.stateFor(outpost.id());
            if (state.productionTicks() > 0L) productionOutposts++;
            if (state.logisticsTicks() > 0L) logisticsOutposts++;
            productionTicks += state.productionTicks();
            logisticsTicks += state.logisticsTicks();
        }
        return new Snapshot(productionOutposts, logisticsOutposts, productionTicks, logisticsTicks);
    }

    private static boolean productionEligible(OutpostRecord outpost, OutpostDeferredState state) {
        return switch (outpost.specialization()) {
            case "lumber", "quarry", "mining", "agriculture" -> true;
            case "general" -> OVERLAY_FISHING.equals(state.observedOverlay());
            default -> false;
        };
    }

    private static String normalizeOverlay(String overlay) {
        if (OVERLAY_FISHING.equals(overlay)) return OVERLAY_FISHING;
        if (OVERLAY_MILITARY.equals(overlay)) return OVERLAY_MILITARY;
        return OVERLAY_GENERAL;
    }
}
