package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Builds the 2.4 x 1.8 km capital one loaded 16 x 16 metre cell at a time. */
public final class ErdenCapitalStreamingBuilder {
    public static final int CAPITAL_REVISION = 4;
    public static final int WEST_WALL_X = -1_200;
    public static final int EAST_WALL_X = 1_200;
    public static final int NORTH_WALL_Z = -900;
    public static final int SOUTH_WALL_Z = 900;

    private static final int STREAM_MARGIN = 48;
    private static final int TICK_BUDGET = 2_400;
    private static final ArrayDeque<Long> PENDING = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();
    private static final Set<Long> RETAINED_REQUESTS = new HashSet<>();
    private static MinecraftServer queuedServer;
    private static ActiveChunk active;

    private ErdenCapitalStreamingBuilder() {
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)) return;
        ChunkPos chunk = event.getChunk().getPos();
        if (!intersectsCapital(chunk)) return;
        enqueue(level, pack(chunk.x(), chunk.z()), false);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        if (queuedServer != null && queuedServer != server) clearQueue();
        queuedServer = server;

        // A PORTAL ticket can emit ChunkEvent.Load slightly before hasChunk() becomes observable to
        // this tick loop. Keep every retained request authoritative until its SavedData revision is
        // complete, and keep explicit requests ahead of incidental background chunk loads.
        requeueRetainedLoaded(level);

        if (active == null) startNext(level);
        if (active == null) return;
        if (!level.hasChunk(active.chunkX(), active.chunkZ())) {
            // startNext retains every active capital cell. A brief visibility gap can still happen
            // while the ticket is promoting the chunk, so wait without discarding the half-applied
            // deterministic plan.
            return;
        }

        try {
            active.plan().apply(level, TICK_BUDGET);
        } catch (RuntimeException | Error failure) {
            long failedChunk = active.chunkPos();
            QUEUED.remove(failedChunk);
            releaseRetained(level, failedChunk);
            active = null;
            throw failure;
        }
        if (!active.plan().done()) return;

        ChunkPos completedChunk = new ChunkPos(active.chunkX(), active.chunkZ());
        ErdenUrbanInfrastructureBuilder.finalizeChunk(level, completedChunk);
        ConstructionDebrisCleaner.cleanStreamedChunkCompletion(level, completedChunk);
        ErdenCapitalChunkSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalChunkSavedData.TYPE);
        data.mark(active.chunkPos(), CAPITAL_REVISION);
        QUEUED.remove(active.chunkPos());
        releaseRetained(level, active.chunkPos());
        active = null;
    }

    /**
     * Requests deterministic completion of one capital cell even when no player is keeping it loaded.
     * The transient chunk ticket is removed as soon as that cell is marked complete.
     */
    public static void requestChunk(ServerLevel level, int chunkX, int chunkZ) {
        ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
        if (!intersectsCapital(chunk)) {
            throw new IllegalArgumentException("Requested chunk is outside the Erden capital: "
                    + chunkX + "," + chunkZ);
        }
        long packed = pack(chunkX, chunkZ);
        ErdenCapitalChunkSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalChunkSavedData.TYPE);
        if (!data.needs(packed, CAPITAL_REVISION)) return;
        retain(level, packed, chunk);
        if (level.hasChunk(chunkX, chunkZ)) enqueue(level, packed, true);
    }

    public static boolean isChunkBuilt(ServerLevel level, int chunkX, int chunkZ) {
        return level.getDataStorage().computeIfAbsent(ErdenCapitalChunkSavedData.TYPE)
                .isBuilt(pack(chunkX, chunkZ), CAPITAL_REVISION);
    }

    public static int builtChunkCount(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenCapitalChunkSavedData.TYPE)
                .builtCount(CAPITAL_REVISION);
    }

    /** Package-private CI/runtime diagnostic; it never loads a chunk or mutates queue state. */
    static String diagnosticChunkState(ServerLevel level, int chunkX, int chunkZ) {
        long packed = pack(chunkX, chunkZ);
        boolean activeHere = active != null && active.chunkPos() == packed;
        String activeState = active == null
                ? "none"
                : active.chunkX() + "," + active.chunkZ()
                        + ":done=" + active.plan().done()
                        + ":progress=" + active.plan().progress()
                        + ":applied=" + active.plan().appliedWrites()
                        + ":estimated=" + active.plan().estimatedWrites()
                        + ":operations=" + active.plan().operationCount()
                        + ":loaded=" + level.hasChunk(active.chunkX(), active.chunkZ())
                        + ":retained=" + RETAINED_REQUESTS.contains(active.chunkPos());
        return chunkX + "," + chunkZ
                + "{loaded=" + level.hasChunk(chunkX, chunkZ)
                + ",built=" + isChunkBuilt(level, chunkX, chunkZ)
                + ",queued=" + QUEUED.contains(packed)
                + ",pending=" + PENDING.contains(packed)
                + ",retained=" + RETAINED_REQUESTS.contains(packed)
                + ",active_here=" + activeHere
                + ",pending_size=" + PENDING.size()
                + ",queued_size=" + QUEUED.size()
                + ",retained_size=" + RETAINED_REQUESTS.size()
                + ",active=" + activeState + "}";
    }

    private static void requeueRetainedLoaded(ServerLevel level) {
        ErdenCapitalChunkSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalChunkSavedData.TYPE);
        for (long packed : Set.copyOf(RETAINED_REQUESTS)) {
            if (!data.needs(packed, CAPITAL_REVISION)) {
                QUEUED.remove(packed);
                PENDING.remove(packed);
                releaseRetained(level, packed);
                continue;
            }
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            if (!level.hasChunk(chunkX, chunkZ)) {
                // PORTAL tickets are keyless in this runtime. Another subsystem releasing the same
                // ticket type can therefore drop the physical lease while this builder still owns
                // the logical retained request. Reassert only retained, unfinished cells; adding an
                // already-present ticket is idempotent and keeps construction ownership bounded.
                level.getChunkSource().addTicketAndLoadWithRadius(
                        TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
                continue;
            }

            // ChunkEvent.Load adds ordinary work at the tail. Explicitly retained requests are
            // latency-sensitive (player-visible construction and diagnostics), so promote an
            // already-queued pending cell instead of leaving it behind the whole background
            // stream. enqueue(..., true) safely leaves the currently active cell untouched.
            enqueue(level, packed, true);
        }
    }

    private static void enqueue(ServerLevel level, long chunkPos, boolean visibleFirst) {
        ErdenCapitalChunkSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalChunkSavedData.TYPE);
        if (!data.needs(chunkPos, CAPITAL_REVISION)) {
            releaseRetained(level, chunkPos);
            return;
        }
        if (QUEUED.add(chunkPos)) {
            if (visibleFirst) PENDING.addFirst(chunkPos);
            else PENDING.addLast(chunkPos);
        } else if (visibleFirst && PENDING.remove(chunkPos)) {
            PENDING.addFirst(chunkPos);
        }
    }

    private static void startNext(ServerLevel level) {
        ErdenCapitalChunkSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalChunkSavedData.TYPE);
        while (!PENDING.isEmpty()) {
            long packed = PENDING.removeFirst();
            if (!data.needs(packed, CAPITAL_REVISION)) {
                QUEUED.remove(packed);
                releaseRetained(level, packed);
                continue;
            }
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            if (!level.hasChunk(chunkX, chunkZ)) {
                // ChunkEvent.Load may precede the tick at which hasChunk becomes true. Preserve both
                // the queue membership and transient ticket, then retry on a later server tick.
                PENDING.addLast(packed);
                return;
            }
            ChunkPos chunk = new ChunkPos(chunkX, chunkZ);

            // A background ChunkEvent.Load used to enter the queue without owning a ticket. If the
            // player moved away after we selected that cell, an unfinished active plan could sit at
            // the head forever and starve every explicitly retained request behind it. Retaining
            // only the single active cell bounds the ticket cost while guaranteeing forward progress.
            retain(level, packed, chunk);

            IncrementalWorldEditPlan plan = createChunkPlan(level, chunk);
            active = new ActiveChunk(packed, chunkX, chunkZ, plan);
            LivingKingdoms.LOGGER.debug(
                    "Prepared streamed Erden capital chunk {},{} writes={} operations={} retained_active=true",
                    chunkX, chunkZ, plan.estimatedWrites(), plan.operationCount()
            );
            return;
        }
    }

    private static IncrementalWorldEditPlan createChunkPlan(ServerLevel level, ChunkPos chunk) {
        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan(chunk);
        addRoadNetwork(plan, level, chunk);
        ExternalRealmBuilder.addCapitalWallChunk(plan, level, chunk);
        ExternalDistrictBuildingBuilder.addChunk(plan, level, chunk);
        ExternalUrbanFabricBuilder.addChunk(plan, level, chunk);
        ErdenUrbanInfrastructureBuilder.addChunk(plan, level, chunk);
        return plan;
    }

    private static void addRoadNetwork(IncrementalWorldEditPlan plan, ServerLevel level, ChunkPos chunk) {
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        for (int x = minX; x <= minX + 15; x++) {
            for (int z = minZ; z <= minZ + 15; z++) {
                RoadClass roadClass = roadClassAt(x, z);
                if (roadClass == RoadClass.NONE) continue;

                int surfaceY = RealmSitePlanner.surfaceY(level, x, z);
                BlockPos surface = new BlockPos(x, surfaceY, z);
                boolean fluid = !level.getFluidState(surface).isEmpty();
                if (fluid && roadClass != RoadClass.ROYAL) continue;

                if (fluid && roadClass == RoadClass.ROYAL) {
                    int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
                    if (Math.floorMod(x + z, 6) == 0) {
                        plan.addFill(x, floor + 1, z, x, surfaceY - 1, z, Blocks.STONE_BRICKS);
                    }
                    plan.addSet(x, surfaceY, z, Blocks.STONE_BRICKS);
                } else {
                    plan.addSet(x, surfaceY, z,
                            roadClass == RoadClass.ROYAL ? Blocks.POLISHED_ANDESITE : Blocks.PACKED_MUD);
                }
                plan.addFill(x, surfaceY + 1, z, x, surfaceY + 3, z, Blocks.AIR);
            }
        }
    }

    static RoadClass roadClassAt(int x, int z) {
        if (!insideWalls(x, z) || insideCitadel(x, z)) return RoadClass.NONE;
        if (Math.abs(x) <= 7 || Math.abs(z) <= 6) return RoadClass.ROYAL;
        if (Math.abs(x - 600) <= 4 || Math.abs(x + 600) <= 4
                || Math.abs(z - 300) <= 4 || Math.abs(z + 300) <= 4) {
            return RoadClass.DISTRICT;
        }

        boolean innerRing = (Math.abs(Math.abs(x) - 1_075) <= 4 && Math.abs(z) <= 790)
                || (Math.abs(Math.abs(z) - 775) <= 4 && Math.abs(x) <= 1_075);
        if (innerRing) return RoadClass.DISTRICT;

        int curvedX = x + (int) Math.round(Math.sin(z / 185.0) * 17.0);
        int curvedZ = z + (int) Math.round(Math.sin(x / 210.0) * 15.0);
        boolean laneX = modularDistance(curvedX, 132) <= 2;
        boolean laneZ = modularDistance(curvedZ, 118) <= 2;
        return laneX || laneZ ? RoadClass.LOCAL : RoadClass.NONE;
    }

    private static int modularDistance(int coordinate, int spacing) {
        int remainder = Math.floorMod(coordinate, spacing);
        return Math.min(remainder, spacing - remainder);
    }

    private static boolean insideWalls(int x, int z) {
        return x > WEST_WALL_X + 10 && x < EAST_WALL_X - 10
                && z > NORTH_WALL_Z + 10 && z < SOUTH_WALL_Z - 10;
    }

    private static boolean insideCitadel(int x, int z) {
        return x >= -82 && x <= 82 && z >= -82 && z <= 82;
    }

    private static boolean intersectsCapital(ChunkPos chunk) {
        int minX = chunk.getMinBlockX();
        int maxX = minX + 15;
        int minZ = chunk.getMinBlockZ();
        int maxZ = minZ + 15;
        return maxX >= WEST_WALL_X - STREAM_MARGIN && minX <= EAST_WALL_X + STREAM_MARGIN
                && maxZ >= NORTH_WALL_Z - STREAM_MARGIN && minZ <= SOUTH_WALL_Z + STREAM_MARGIN;
    }

    private static long pack(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) packed;
    }

    private static void retain(ServerLevel level, long packed, ChunkPos chunk) {
        RETAINED_REQUESTS.add(packed);
        // Reassert the physical lease even when the logical set already contains this chunk. A
        // keyless PORTAL remove from a different owner may have erased the shared physical ticket.
        level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, chunk, 0);
    }

    private static void releaseRetained(ServerLevel level, long packed) {
        if (!RETAINED_REQUESTS.remove(packed)) return;
        level.getChunkSource().removeTicketWithRadius(
                TicketType.PORTAL, new ChunkPos(unpackX(packed), unpackZ(packed)), 0);
    }

    private static void clearQueue() {
        if (queuedServer != null) {
            ServerLevel previous = queuedServer.getLevel(StarterRealmManager.REALM_KEY);
            if (previous != null) {
                for (long packed : Set.copyOf(RETAINED_REQUESTS)) {
                    previous.getChunkSource().removeTicketWithRadius(
                            TicketType.PORTAL,
                            new ChunkPos(unpackX(packed), unpackZ(packed)),
                            0);
                }
            }
        }
        PENDING.clear();
        QUEUED.clear();
        RETAINED_REQUESTS.clear();
        active = null;
    }

    enum RoadClass {
        NONE,
        LOCAL,
        DISTRICT,
        ROYAL
    }

    private record ActiveChunk(long chunkPos, int chunkX, int chunkZ,
                               IncrementalWorldEditPlan plan) {
    }
}
