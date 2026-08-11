package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Second-pass capital infill for the deliberately non-functional spaces between authored buildings.
 *
 * The primary urban fabric owns the canonical homes/workplaces. This pass never creates another
 * functional plot or resident assignment. Instead it waits until a streamed capital chunk is fully
 * built, then turns genuinely empty rear yards and alley corners into small courtyards, storage
 * nooks, wash corners, garden pockets and lit rest spaces. Candidate footprints are checked against
 * roads, door access corridors and the already-built world before any write is scheduled.
 */
public final class ErdenUrbanMicroInfillManager {
    public static final int MICRO_INFILL_REVISION = 1;

    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final int PLAYER_SCAN_RADIUS_CHUNKS = 5;
    private static final int TICK_BUDGET = 320;
    private static final int PARCEL_HALF_SIZE = 2;
    private static final int ACCESS_CLEARANCE = 3;

    private static final ArrayDeque<Long> PENDING = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();
    private static MinecraftServer queuedServer;
    private static ActiveChunk active;

    private ErdenUrbanMicroInfillManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        if (queuedServer != null && queuedServer != server) clearQueue();
        queuedServer = server;

        if (level.getGameTime() % SCAN_INTERVAL_TICKS == 0L) {
            queueVisibleBuiltChunks(level);
        }

        if (active == null) startNext(level);
        if (active == null) return;

        if (!level.hasChunk(active.chunkX(), active.chunkZ())) {
            QUEUED.remove(active.chunkPos());
            active = null;
            return;
        }

        active.plan().apply(level, TICK_BUDGET);
        if (!active.plan().done()) return;

        ErdenUrbanMicroInfillSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanMicroInfillSavedData.TYPE);
        data.mark(active.chunkPos(), MICRO_INFILL_REVISION);
        QUEUED.remove(active.chunkPos());
        LivingKingdoms.LOGGER.debug(
                "Completed Erden micro-infill chunk {},{} writes={} completed_chunks={}",
                active.chunkX(), active.chunkZ(), active.plan().estimatedWrites(),
                data.completedCount(MICRO_INFILL_REVISION));
        active = null;
    }

    public static int completedChunkCount(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenUrbanMicroInfillSavedData.TYPE)
                .completedCount(MICRO_INFILL_REVISION);
    }

    private static void queueVisibleBuiltChunks(ServerLevel level) {
        ErdenUrbanMicroInfillSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanMicroInfillSavedData.TYPE);

        for (ServerPlayer player : level.players()) {
            ChunkPos center = player.chunkPosition();
            for (int dz = -PLAYER_SCAN_RADIUS_CHUNKS; dz <= PLAYER_SCAN_RADIUS_CHUNKS; dz++) {
                for (int dx = -PLAYER_SCAN_RADIUS_CHUNKS; dx <= PLAYER_SCAN_RADIUS_CHUNKS; dx++) {
                    int chunkX = center.x() + dx;
                    int chunkZ = center.z() + dz;
                    if (!insideCapitalChunk(chunkX, chunkZ) || !level.hasChunk(chunkX, chunkZ)) continue;
                    if (!ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) continue;

                    long packed = pack(chunkX, chunkZ);
                    if (!data.needs(packed, MICRO_INFILL_REVISION)) continue;
                    if (QUEUED.add(packed)) PENDING.addLast(packed);
                }
            }
        }
    }

    private static void startNext(ServerLevel level) {
        ErdenUrbanMicroInfillSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanMicroInfillSavedData.TYPE);

        while (!PENDING.isEmpty()) {
            long packed = PENDING.removeFirst();
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);

            if (!data.needs(packed, MICRO_INFILL_REVISION)) {
                QUEUED.remove(packed);
                continue;
            }
            if (!level.hasChunk(chunkX, chunkZ)) {
                QUEUED.remove(packed);
                continue;
            }
            if (!ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) {
                QUEUED.remove(packed);
                continue;
            }

            ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
            IncrementalWorldEditPlan plan = createPlan(level, chunk);
            if (plan.operationCount() == 0) {
                data.mark(packed, MICRO_INFILL_REVISION);
                QUEUED.remove(packed);
                continue;
            }

            active = new ActiveChunk(packed, chunkX, chunkZ, plan);
            return;
        }
    }

    private static IncrementalWorldEditPlan createPlan(ServerLevel level, ChunkPos chunk) {
        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan(chunk);
        int chunkHash = mix(chunk.x(), chunk.z(), 0x6D2B79F5);

        // Roughly one third of built cells receive an infill pocket. This is dense enough to break
        // empty-lot repetition without turning every 16 m cell into visual clutter.
        if (Math.floorMod(chunkHash, 3) != 0) return plan;

        int[][] candidates = {
                {4, 4}, {11, 4}, {4, 11}, {11, 11}, {8, 8}
        };
        int start = Math.floorMod(chunkHash >>> 5, candidates.length);
        int targetParcels = Math.floorMod(chunkHash >>> 11, 7) == 0 ? 2 : 1;
        int placed = 0;

        for (int i = 0; i < candidates.length && placed < targetParcels; i++) {
            int[] candidate = candidates[(start + i) % candidates.length];
            int x = chunk.getMinBlockX() + candidate[0];
            int z = chunk.getMinBlockZ() + candidate[1];
            int y = validatedParcelY(level, x, z);
            if (y == Integer.MIN_VALUE) continue;

            int styleHash = mix(x, z, chunkHash);
            addParcel(plan, x, y, z, styleHash);
            placed++;
        }
        return plan;
    }

    private static int validatedParcelY(ServerLevel level, int centerX, int centerZ) {
        if (!insideCapitalPoint(centerX, centerZ)) return Integer.MIN_VALUE;
        if (Math.abs(centerX) <= 175 && Math.abs(centerZ) <= 175) return Integer.MIN_VALUE;
        if (nearFunctionalAccess(centerX, centerZ)) return Integer.MIN_VALUE;

        int centerY = RealmSitePlanner.surfaceY(level, centerX, centerZ);
        for (int dz = -PARCEL_HALF_SIZE; dz <= PARCEL_HALF_SIZE; dz++) {
            for (int dx = -PARCEL_HALF_SIZE; dx <= PARCEL_HALF_SIZE; dx++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if (!insideCapitalPoint(x, z)) return Integer.MIN_VALUE;
                if (nearRoad(x, z, ACCESS_CLEARANCE)) return Integer.MIN_VALUE;
                if (nearFunctionalAccess(x, z)) return Integer.MIN_VALUE;

                int surfaceY = RealmSitePlanner.surfaceY(level, x, z);
                if (surfaceY != centerY) return Integer.MIN_VALUE;

                BlockPos groundPos = new BlockPos(x, surfaceY, z);
                BlockState ground = level.getBlockState(groundPos);
                if (!naturalOrYardGround(ground)) return Integer.MIN_VALUE;
                if (!level.getFluidState(groundPos).isEmpty()) return Integer.MIN_VALUE;
                if (!level.getBlockState(groundPos.above()).isAir()
                        || !level.getBlockState(groundPos.above(2)).isAir()
                        || !level.getBlockState(groundPos.above(3)).isAir()) {
                    return Integer.MIN_VALUE;
                }
            }
        }
        return centerY;
    }

    private static boolean naturalOrYardGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.STONE)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND);
    }

    private static boolean nearRoad(int x, int z, int radius) {
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (ErdenCapitalStreamingBuilder.roadClassAt(x + dx, z + dz)
                        != ErdenCapitalStreamingBuilder.RoadClass.NONE) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean nearFunctionalAccess(int x, int z) {
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : ExternalUrbanFabricBuilder.entrances()) {
            int minX = Math.min(entrance.x(), entrance.roadX()) - ACCESS_CLEARANCE;
            int maxX = Math.max(entrance.x(), entrance.roadX()) + ACCESS_CLEARANCE;
            int minZ = Math.min(entrance.z(), entrance.roadZ()) - ACCESS_CLEARANCE;
            int maxZ = Math.max(entrance.z(), entrance.roadZ()) + ACCESS_CLEARANCE;
            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) return true;
        }
        return false;
    }

    private static void addParcel(IncrementalWorldEditPlan plan, int x, int y, int z, int hash) {
        int style = Math.floorMod(hash, 5);
        switch (style) {
            case 0 -> addCargoNook(plan, x, y, z);
            case 1 -> addRestCorner(plan, x, y, z);
            case 2 -> addWashCorner(plan, x, y, z);
            case 3 -> addGardenPocket(plan, x, y, z);
            default -> addWoodYard(plan, x, y, z);
        }
    }

    private static void addCargoNook(IncrementalWorldEditPlan plan, int x, int y, int z) {
        patchGround(plan, x, y, z, Blocks.GRAVEL.defaultBlockState());
        plan.addSet(x - 1, y + 1, z, Blocks.BARREL);
        plan.addSet(x, y + 1, z, Blocks.BARREL);
        plan.addSet(x + 1, y + 1, z + 1, Blocks.HAY_BLOCK);
        addLampPost(plan, x - 2, y, z + 2);
    }

    private static void addRestCorner(IncrementalWorldEditPlan plan, int x, int y, int z) {
        patchGround(plan, x, y, z, Blocks.COARSE_DIRT.defaultBlockState());
        plan.addSet(x - 1, y + 1, z, Blocks.STONE_BRICK_SLAB);
        plan.addSet(x, y + 1, z, Blocks.STONE_BRICK_SLAB);
        plan.addSet(x + 1, y + 1, z, Blocks.STONE_BRICK_SLAB);
        plan.addSet(x, y + 1, z + 2, Blocks.FLOWER_POT);
        addLampPost(plan, x + 2, y, z - 2);
    }

    private static void addWashCorner(IncrementalWorldEditPlan plan, int x, int y, int z) {
        patchGround(plan, x, y, z, Blocks.GRAVEL.defaultBlockState());
        plan.addSet(x, y + 1, z, Blocks.CAULDRON);
        plan.addSet(x - 1, y + 1, z + 1, Blocks.BARREL);
        plan.addSet(x + 1, y + 1, z + 1, Blocks.BARREL);
        plan.addSet(x - 2, y + 1, z - 1, Blocks.OAK_FENCE);
        plan.addSet(x + 2, y + 1, z - 1, Blocks.OAK_FENCE);
    }

    private static void addGardenPocket(IncrementalWorldEditPlan plan, int x, int y, int z) {
        patchGround(plan, x, y, z, Blocks.COARSE_DIRT.defaultBlockState());
        plan.addSet(x, y + 1, z, Blocks.COMPOSTER);
        plan.addSet(x - 1, y + 1, z + 1, Blocks.FLOWER_POT);
        plan.addSet(x + 1, y + 1, z + 1, Blocks.FLOWER_POT);
        plan.addSet(x - 1, y + 1, z - 1, Blocks.OAK_LEAVES);
        plan.addSet(x + 1, y + 1, z - 1, Blocks.OAK_LEAVES);
        addLampPost(plan, x + 2, y, z + 2);
    }

    private static void addWoodYard(IncrementalWorldEditPlan plan, int x, int y, int z) {
        patchGround(plan, x, y, z, Blocks.COARSE_DIRT.defaultBlockState());
        plan.addSet(x - 1, y + 1, z, Blocks.OAK_LOG);
        plan.addSet(x, y + 1, z, Blocks.OAK_LOG);
        plan.addSet(x + 1, y + 1, z, Blocks.OAK_LOG);
        plan.addSet(x, y + 2, z, Blocks.OAK_LOG);
        plan.addSet(x + 2, y + 1, z + 1, Blocks.BARREL);
        addLampPost(plan, x - 2, y, z - 2);
    }

    private static void patchGround(IncrementalWorldEditPlan plan, int x, int y, int z, BlockState state) {
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                plan.addSet(x + dx, y, z + dz, state);
            }
        }
    }

    private static void addLampPost(IncrementalWorldEditPlan plan, int x, int y, int z) {
        plan.addSet(x, y + 1, z, Blocks.COBBLESTONE_WALL);
        plan.addSet(x, y + 2, z, Blocks.OAK_FENCE);
        plan.addSet(x, y + 3, z, Blocks.LANTERN);
    }

    private static boolean insideCapitalChunk(int chunkX, int chunkZ) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        return maxX > ErdenCapitalStreamingBuilder.WEST_WALL_X + 18
                && minX < ErdenCapitalStreamingBuilder.EAST_WALL_X - 18
                && maxZ > ErdenCapitalStreamingBuilder.NORTH_WALL_Z + 18
                && minZ < ErdenCapitalStreamingBuilder.SOUTH_WALL_Z - 18;
    }

    private static boolean insideCapitalPoint(int x, int z) {
        return x > ErdenCapitalStreamingBuilder.WEST_WALL_X + 22
                && x < ErdenCapitalStreamingBuilder.EAST_WALL_X - 22
                && z > ErdenCapitalStreamingBuilder.NORTH_WALL_Z + 22
                && z < ErdenCapitalStreamingBuilder.SOUTH_WALL_Z - 22;
    }

    private static int mix(int x, int z, int salt) {
        int value = x * 0x1F123BB5 ^ z * 0x5F356495 ^ salt;
        value ^= value >>> 16;
        value *= 0x7FEB352D;
        value ^= value >>> 15;
        value *= 0x846CA68B;
        return value ^ value >>> 16;
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

    private static void clearQueue() {
        PENDING.clear();
        QUEUED.clear();
        active = null;
        queuedServer = null;
    }

    private record ActiveChunk(long chunkPos, int chunkX, int chunkZ,
                               IncrementalWorldEditPlan plan) {
    }
}
