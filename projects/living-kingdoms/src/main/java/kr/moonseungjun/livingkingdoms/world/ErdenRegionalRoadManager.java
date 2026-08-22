package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Streams the authored national-road cells only when visited; CI uses two bounded transient probes. */
public final class ErdenRegionalRoadManager {
    private static final int TICK_BUDGET = 1_800;
    private static final int CI_TICK_BUDGET = 4_000;
    private static final int CI_MAX_IN_FLIGHT = 2;

    private static final ArrayDeque<Long> PENDING = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();
    private static final Set<Long> CI_REQUIRED = new HashSet<>();
    private static final Set<Long> CI_LOADING = new HashSet<>();
    private static final Set<Long> CI_RETAINED = new HashSet<>();
    private static MinecraftServer activeServer;
    private static ActiveChunk active;
    private static boolean ciPrepared;
    private static boolean ciPassed;

    private ErdenRegionalRoadManager() {
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)
                || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        ChunkPos chunk = event.getChunk().getPos();
        if (!ErdenRegionalRoadNetwork.intersects(chunk)) return;
        long key = pack(chunk.x(), chunk.z());
        if (isCi() && !CI_REQUIRED.contains(key)) return;
        enqueue(level, key, false);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        if (activeServer != server) reset(server);
        if (isCi()) {
            if (!ciPrepared) prepareCi(level);
            advanceCi(level);
        }
        if (active == null) startNext(level);
        if (active == null) {
            verifyCi(level);
            return;
        }
        if (!level.hasChunk(active.chunkX, active.chunkZ)) {
            QUEUED.remove(active.key);
            active = null;
            return;
        }
        active.plan.apply(level, isCi() ? CI_TICK_BUDGET : TICK_BUDGET);
        if (!active.plan.done()) return;

        ChunkPos chunk = new ChunkPos(active.chunkX, active.chunkZ);
        ConstructionDebrisCleaner.cleanStreamedChunkCompletion(level, chunk);
        ErdenRegionalRoadSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalRoadSavedData.TYPE);
        data.markBuilt(active.key, ErdenRegionalRoadNetwork.REVISION, active.plan.appliedWrites());
        if (isCi()) {
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_REGIONAL_ROAD_CHUNK_COMPLETE chunk={},{} writes={} operations={} clipped={}",
                    active.chunkX, active.chunkZ, active.plan.appliedWrites(),
                    active.plan.operationCount(), active.plan.suppressedOutOfBoundsWrites());
        }
        QUEUED.remove(active.key);
        active = null;
        verifyCi(level);
    }

    public static boolean isRoadChunkBuilt(ServerLevel level, int x, int z) {
        long key = pack(x >> 4, z >> 4);
        return level.getDataStorage().computeIfAbsent(ErdenRegionalRoadSavedData.TYPE)
                .isBuilt(key, ErdenRegionalRoadNetwork.REVISION);
    }

    private static void prepareCi(ServerLevel level) {
        ciPrepared = true;
        CI_REQUIRED.clear();
        CI_REQUIRED.add(pack(0 >> 4, 2_600 >> 4));
        ErdenRegionalRoadNetwork.Waystation station = ErdenRegionalRoadNetwork.waystations().getFirst();
        CI_REQUIRED.add(pack(station.x() >> 4, station.z() >> 4));
        if (CI_REQUIRED.size() != CI_MAX_IN_FLIGHT) {
            throw new IllegalStateException("Regional road CI probe count drifted: " + CI_REQUIRED.size());
        }
        for (long key : CI_REQUIRED) {
            int chunkX = unpackX(key);
            int chunkZ = unpackZ(key);
            CI_RETAINED.add(key);
            level.getChunkSource().addTicketAndLoadWithRadius(
                    TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
            if (level.hasChunk(chunkX, chunkZ)) enqueue(level, key, true);
            else CI_LOADING.add(key);
        }
        LivingKingdoms.LOGGER.info(
                "Requested Erden national-road CI sample chunks={} corridors={} waystations={} transient_ticket=portal persistent_forced_chunks=false",
                CI_REQUIRED.size(), ErdenRegionalRoadNetwork.CORRIDOR_COUNT,
                ErdenRegionalRoadNetwork.WAYSTATION_COUNT);
    }

    private static void advanceCi(ServerLevel level) {
        for (long key : Set.copyOf(CI_LOADING)) {
            int chunkX = unpackX(key);
            int chunkZ = unpackZ(key);
            if (!level.hasChunk(chunkX, chunkZ)) continue;
            CI_LOADING.remove(key);
            enqueue(level, key, true);
        }
    }

    private static void enqueue(ServerLevel level, long key, boolean priority) {
        ErdenRegionalRoadSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalRoadSavedData.TYPE);
        if (!data.needs(key, ErdenRegionalRoadNetwork.REVISION)) return;
        if (!QUEUED.add(key)) return;
        if (priority) PENDING.addFirst(key);
        else PENDING.addLast(key);
    }

    private static void startNext(ServerLevel level) {
        ErdenRegionalRoadSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalRoadSavedData.TYPE);
        while (!PENDING.isEmpty()) {
            long key = PENDING.removeFirst();
            if (!data.needs(key, ErdenRegionalRoadNetwork.REVISION)) {
                QUEUED.remove(key);
                continue;
            }
            int chunkX = unpackX(key);
            int chunkZ = unpackZ(key);
            if (!level.hasChunk(chunkX, chunkZ)) {
                QUEUED.remove(key);
                continue;
            }
            ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
            IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan(chunk);
            ErdenRegionalRoadBuilder.addChunk(plan, level, chunk);
            active = new ActiveChunk(key, chunkX, chunkZ, plan);
            return;
        }
    }

    private static void verifyCi(ServerLevel level) {
        if (!isCi() || !ciPrepared || ciPassed || active != null
                || !PENDING.isEmpty() || !CI_LOADING.isEmpty()) return;
        ErdenRegionalRoadSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalRoadSavedData.TYPE);
        for (long key : CI_REQUIRED) {
            if (!level.hasChunk(unpackX(key), unpackZ(key))
                    || !data.isBuilt(key, ErdenRegionalRoadNetwork.REVISION)) return;
        }

        int roadY = ErdenRegionalRoadBuilder.surfaceY(0, 2_600);
        BlockPos road = new BlockPos(0, roadY, 2_600);
        boolean physicalRoad = level.getBlockState(road).is(Blocks.PACKED_MUD)
                || level.getBlockState(road).is(Blocks.STONE_BRICKS);
        ErdenRegionalRoadNetwork.Waystation station = ErdenRegionalRoadNetwork.waystations().getFirst();
        int shelterX = station.x() + 5;
        int shelterZ = station.z() - 4;
        int shelterY = ErdenRegionalRoadBuilder.waystationFloorY(station, shelterX, shelterZ);
        boolean physicalWaystation = level.getBlockState(
                new BlockPos(shelterX, shelterY + 1, shelterZ)).is(Blocks.BARREL)
                && level.getBlockState(new BlockPos(shelterX, shelterY + 4, shelterZ)).is(Blocks.LANTERN);
        if (!physicalRoad || !physicalWaystation) {
            throw new IllegalStateException(
                    "Erden regional road physical audit failed road=" + physicalRoad
                            + " waystation=" + physicalWaystation);
        }
        if (ErdenRegionalRoadNetwork.totalRoadMetres() < 25_000L
                || ErdenRegionalRoadNetwork.route("ironvale", "sunfield").size() < 100) {
            throw new IllegalStateException("Erden regional road graph scale/connectivity regressed");
        }
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_REGIONAL_ROADS_PASS revision={} corridors={} waystations={} road_metres={} network_connected=true streamed=true terrain_following=true physical_road=true physical_waystation=true persistent_forced_chunks=false",
                ErdenRegionalRoadNetwork.REVISION,
                ErdenRegionalRoadNetwork.CORRIDOR_COUNT,
                ErdenRegionalRoadNetwork.WAYSTATION_COUNT,
                ErdenRegionalRoadNetwork.totalRoadMetres());
        releaseCi(level);
    }

    private static void releaseCi(ServerLevel level) {
        for (long key : Set.copyOf(CI_RETAINED)) {
            level.getChunkSource().removeTicketWithRadius(
                    TicketType.PORTAL,
                    new ChunkPos(unpackX(key), unpackZ(key)),
                    0);
        }
        CI_RETAINED.clear();
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        PENDING.clear();
        QUEUED.clear();
        CI_REQUIRED.clear();
        CI_LOADING.clear();
        CI_RETAINED.clear();
        active = null;
        ciPrepared = false;
        ciPassed = false;
    }

    private static boolean isCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }

    private static long pack(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    private static int unpackX(long key) {
        return (int) (key >> 32);
    }

    private static int unpackZ(long key) {
        return (int) key;
    }

    private record ActiveChunk(long key, int chunkX, int chunkZ, IncrementalWorldEditPlan plan) {
    }
}
