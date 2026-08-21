package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Streams second-ring Erden villages only when their chunks are visited, plus a bounded CI sample. */
public final class ErdenRegionalSettlementManager {
    private static final int TICK_BUDGET = 1_600;
    private static final int CI_TICK_BUDGET = 4_000;
    private static final int CI_FORCE_BUDGET = 1;
    private static final int CI_MAX_IN_FLIGHT = 6;
    private static final long CI_ECONOMY_PROBE_LEASE_TICKS = 400L;

    private static final ArrayDeque<Long> PENDING = new ArrayDeque<>();
    private static final ArrayDeque<Long> CI_REQUESTS = new ArrayDeque<>();
    private static final Set<Long> CI_REQUIRED = new HashSet<>();
    private static final Set<Long> CI_LOADING = new HashSet<>();
    private static final Set<Long> QUEUED = new HashSet<>();
    private static final Set<Long> RETAINED = new HashSet<>();
    private static MinecraftServer activeServer;
    private static ActiveChunk active;
    private static boolean ciRequested;
    private static boolean ciPassed;
    private static long economyProbeReleaseTick = Long.MIN_VALUE;

    private ErdenRegionalSettlementManager() {
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)
                || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        ChunkPos chunk = event.getChunk().getPos();
        if (!ErdenRegionalSettlementCatalog.intersects(chunk)) return;
        long packed = pack(chunk.x(), chunk.z());
        if (isCi() && !CI_REQUIRED.contains(packed)) return;
        enqueue(level, packed, false);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        if (activeServer != server) reset(server);

        if (isCi()) {
            if (!ciRequested) prepareCi(level);
            advanceCi(level);
            releaseExpiredEconomyProbe(level);
        }

        if (active == null) startNext(level);
        if (active == null) {
            verifyCi(level);
            return;
        }
        if (!level.hasChunk(active.chunkX, active.chunkZ)) {
            QUEUED.remove(active.packed);
            if (!isCi()) release(level, active.packed);
            active = null;
            return;
        }

        active.plan.apply(level, isCi() ? CI_TICK_BUDGET : TICK_BUDGET);
        if (!active.plan.done()) return;

        ChunkPos chunk = new ChunkPos(active.chunkX, active.chunkZ);
        ConstructionDebrisCleaner.cleanStreamedChunkCompletion(level, chunk);
        ErdenRegionalSettlementSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSettlementSavedData.TYPE);
        data.markChunk(active.packed, ErdenRegionalSettlementCatalog.REVISION, active.plan.appliedWrites());
        markCentreIfNeeded(data, active.chunkX, active.chunkZ);
        if (isCi()) {
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_REGIONAL_CHUNK_COMPLETE chunk={},{} applied_writes={} operations={} clipped={}",
                    active.chunkX, active.chunkZ, active.plan.appliedWrites(),
                    active.plan.operationCount(), active.plan.suppressedOutOfBoundsWrites());
        }
        QUEUED.remove(active.packed);
        if (!isCi()) release(level, active.packed);
        active = null;
        verifyCi(level);
    }

    private static void prepareCi(ServerLevel level) {
        ciRequested = true;
        CI_REQUIRED.clear();
        CI_REQUIRED.addAll(ErdenRegionalSettlementAudit.requiredChunkKeys());
        addRegionalSocietyHomeProbe();
        addRegionalEconomyStorageProbe();
        CI_REQUESTS.addAll(CI_REQUIRED);
        if (CI_REQUIRED.size() < 3 || CI_REQUIRED.size() > CI_MAX_IN_FLIGHT) {
            throw new IllegalStateException("Invalid regional settlement CI probe count " + CI_REQUIRED.size());
        }
        LivingKingdoms.LOGGER.info(
                "Requested Erden regional settlement CI sample representative={} probe_chunks={} settlements={} buildings={} transient_ticket=portal persistent_forced_chunks=false",
                ErdenRegionalSettlementAudit.representativeId(), CI_REQUIRED.size(),
                ErdenRegionalSettlementCatalog.SETTLEMENT_COUNT,
                ErdenRegionalSettlementCatalog.TOTAL_BUILDINGS);
    }

    private static void addRegionalSocietyHomeProbe() {
        ErdenRegionalSettlementCatalog.Settlement settlement = representativeSettlement();
        ErdenRegionalSettlementCatalog.BuildingLot lot = settlement.buildings().stream()
                .filter(candidate -> candidate.role().equals("farmstead_east"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing regional society representative home"));
        CI_REQUIRED.add(pack((settlement.x() + lot.dx()) >> 4, (settlement.z() + lot.dz()) >> 4));
    }

    private static void addRegionalEconomyStorageProbe() {
        CI_REQUIRED.add(regionalEconomyStorageProbeKey());
    }

    private static long regionalEconomyStorageProbeKey() {
        return ErdenRegionalEconomyManager.storageChunkKey(representativeSettlement());
    }

    private static ErdenRegionalSettlementCatalog.Settlement representativeSettlement() {
        return ErdenRegionalSettlementCatalog.settlements().stream()
                .filter(candidate -> candidate.id().equals("harvest_crossing"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing regional representative settlement"));
    }

    private static void advanceCi(ServerLevel level) {
        for (long packed : List.copyOf(CI_LOADING)) {
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            if (!level.hasChunk(chunkX, chunkZ)) continue;
            CI_LOADING.remove(packed);
            enqueue(level, packed, true);
        }
        for (int forced = 0; forced < CI_FORCE_BUDGET
                && !CI_REQUESTS.isEmpty()
                && RETAINED.size() < CI_MAX_IN_FLIGHT; forced++) {
            long packed = CI_REQUESTS.removeFirst();
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            if (RETAINED.add(packed)) {
                level.getChunkSource().addTicketAndLoadWithRadius(
                        TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
            }
            if (level.hasChunk(chunkX, chunkZ)) enqueue(level, packed, true);
            else CI_LOADING.add(packed);
        }
    }

    private static void enqueue(ServerLevel level, long packed, boolean priority) {
        ErdenRegionalSettlementSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSettlementSavedData.TYPE);
        if (!data.needs(packed, ErdenRegionalSettlementCatalog.REVISION)) return;
        if (QUEUED.add(packed)) {
            if (priority) PENDING.addFirst(packed);
            else PENDING.addLast(packed);
        } else if (priority && PENDING.remove(packed)) {
            PENDING.addFirst(packed);
        }
    }

    private static void startNext(ServerLevel level) {
        ErdenRegionalSettlementSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSettlementSavedData.TYPE);
        while (!PENDING.isEmpty()) {
            long packed = PENDING.removeFirst();
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            if (!data.needs(packed, ErdenRegionalSettlementCatalog.REVISION)) {
                QUEUED.remove(packed);
                continue;
            }
            if (!level.hasChunk(chunkX, chunkZ)) {
                QUEUED.remove(packed);
                if (!isCi()) release(level, packed);
                continue;
            }
            ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
            IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan(chunk);
            ErdenRegionalSettlementBuilder.addChunk(plan, level, chunk);
            active = new ActiveChunk(packed, chunkX, chunkZ, plan);
            if (isCi()) {
                LivingKingdoms.LOGGER.info(
                        "LK_ERDEN_REGIONAL_CHUNK_START chunk={},{} writes={} operations={} clipped={}",
                        chunkX, chunkZ, plan.estimatedWrites(), plan.operationCount(),
                        plan.suppressedOutOfBoundsWrites());
            }
            return;
        }
    }

    private static void markCentreIfNeeded(
            ErdenRegionalSettlementSavedData data,
            int chunkX,
            int chunkZ) {
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            if ((settlement.x() >> 4) == chunkX && (settlement.z() >> 4) == chunkZ) {
                data.markCentre(settlement.id(), ErdenRegionalSettlementCatalog.REVISION);
            }
        }
    }

    private static void verifyCi(ServerLevel level) {
        if (!isCi() || !ciRequested || ciPassed || active != null
                || !PENDING.isEmpty() || !CI_REQUESTS.isEmpty() || !CI_LOADING.isEmpty()) return;
        ErdenRegionalSettlementSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSettlementSavedData.TYPE);
        for (long packed : CI_REQUIRED) {
            if (!data.isBuilt(packed, ErdenRegionalSettlementCatalog.REVISION)
                    || !level.hasChunk(unpackX(packed), unpackZ(packed))) return;
        }
        if (!ErdenRegionalSettlementAudit.verify(level)) {
            throw new IllegalStateException("Representative Erden regional settlement physical audit failed");
        }
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_REGIONAL_SETTLEMENT_PASS revision={} settlements={} buildings={} representative={} source_fragments={} probe_chunks={} physical_square=true physical_road=true physical_field=true physical_building=true terrain_geography=true streamed=true metre_scale=true persistent_forced_chunks=false",
                ErdenRegionalSettlementCatalog.REVISION,
                ErdenRegionalSettlementCatalog.SETTLEMENT_COUNT,
                ErdenRegionalSettlementCatalog.TOTAL_BUILDINGS,
                ErdenRegionalSettlementAudit.representativeId(),
                ErdenRegionalSettlementBuilder.sourceStyleCount(), CI_REQUIRED.size());
        long economyProbe = regionalEconomyStorageProbeKey();
        for (long packed : Set.copyOf(RETAINED)) {
            if (packed != economyProbe) release(level, packed);
        }
        economyProbeReleaseTick = level.getGameTime() + CI_ECONOMY_PROBE_LEASE_TICKS;
        LivingKingdoms.LOGGER.info(
                "Retained Erden regional economy storage CI probe chunk={},{} lease_ticks={} transient_ticket=portal persistent_forced_chunks=false",
                unpackX(economyProbe), unpackZ(economyProbe), CI_ECONOMY_PROBE_LEASE_TICKS);
    }

    private static void releaseExpiredEconomyProbe(ServerLevel level) {
        if (!ciPassed || economyProbeReleaseTick == Long.MIN_VALUE
                || level.getGameTime() < economyProbeReleaseTick) return;
        int released = RETAINED.size();
        for (long packed : Set.copyOf(RETAINED)) release(level, packed);
        economyProbeReleaseTick = Long.MIN_VALUE;
        LivingKingdoms.LOGGER.info(
                "Released Erden regional economy storage CI probe released={} transient_lease_expired=true persistent_forced_chunks=false",
                released);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        PENDING.clear();
        CI_REQUESTS.clear();
        CI_REQUIRED.clear();
        CI_LOADING.clear();
        QUEUED.clear();
        RETAINED.clear();
        active = null;
        ciRequested = false;
        ciPassed = false;
        economyProbeReleaseTick = Long.MIN_VALUE;
    }

    private static void release(ServerLevel level, long packed) {
        if (!RETAINED.remove(packed)) return;
        level.getChunkSource().removeTicketWithRadius(
                TicketType.PORTAL,
                new ChunkPos(unpackX(packed), unpackZ(packed)),
                0);
    }

    private static boolean isCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) packed;
    }

    private record ActiveChunk(long packed, int chunkX, int chunkZ, IncrementalWorldEditPlan plan) {
    }
}
