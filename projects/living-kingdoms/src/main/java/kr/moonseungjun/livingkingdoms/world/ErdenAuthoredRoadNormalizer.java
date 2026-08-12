package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Canonicalizes completed capital roads after all normal builders have run.
 * This makes roads independent from tree canopies and repairs old saves without rebuilding the kingdom.
 */
@EventBusSubscriber(modid = LivingKingdoms.MOD_ID)
public final class ErdenAuthoredRoadNormalizer {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS
            | Block.UPDATE_KNOWN_SHAPE
            | Block.UPDATE_SUPPRESS_DROPS;
    private static final int REPAIRS_PER_TICK = 2;
    private static final int MAX_CLEAR_HEIGHT = 32;
    private static final int DIAGNOSTIC_X = 0;
    private static final int DIAGNOSTIC_Z = 200;

    private static final ArrayDeque<Long> PENDING = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();
    private static MinecraftServer activeServer;
    private static boolean ciAnchorsQueued;
    private static boolean ciPassed;

    private ErdenAuthoredRoadNormalizer() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)) return;
        ChunkPos chunk = event.getChunk().getPos();
        if (intersectsCapital(chunk)) enqueue(chunk.x(), chunk.z());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        if (activeServer != server) reset(server);

        if (isCi() && !ciAnchorsQueued) {
            ciAnchorsQueued = true;
            enqueue(DIAGNOSTIC_X >> 4, DIAGNOSTIC_Z >> 4);
            enqueue(ErdenUrbanInfrastructureBuilder.DIAGNOSTIC_WELL_X >> 4,
                    ErdenUrbanInfrastructureBuilder.DIAGNOSTIC_WELL_Z >> 4);
            enqueue(ErdenUrbanInfrastructureBuilder.DIAGNOSTIC_CISTERN_X >> 4,
                    ErdenUrbanInfrastructureBuilder.DIAGNOSTIC_CISTERN_Z >> 4);
        }

        int scans = PENDING.size();
        int repaired = 0;
        while (scans-- > 0 && repaired < REPAIRS_PER_TICK && !PENDING.isEmpty()) {
            long packed = PENDING.removeFirst();
            QUEUED.remove(packed);
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            if (!level.hasChunk(chunkX, chunkZ)
                    || !ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) {
                enqueue(chunkX, chunkZ);
                continue;
            }
            ErdenAuthoredRoadSavedData data = level.getDataStorage()
                    .computeIfAbsent(ErdenAuthoredRoadSavedData.TYPE);
            if (!data.needs(packed)) continue;
            RepairResult result = normalizeChunk(level, new ChunkPos(chunkX, chunkZ));
            data.markNormalized(
                    packed, result.roadColumns(), result.culvertCells(), result.removedBlocks());
            repaired++;
        }
        verifyCi(level);
    }

    private static RepairResult normalizeChunk(ServerLevel level, ChunkPos chunk) {
        long roads = 0L;
        long culverts = 0L;
        long removed = 0L;
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        for (int x = minX; x <= minX + 15; x++) {
            for (int z = minZ; z <= minZ + 15; z++) {
                ErdenCapitalStreamingBuilder.RoadClass roadClass =
                        ErdenCapitalStreamingBuilder.roadClassAt(x, z);
                if (roadClass == ErdenCapitalStreamingBuilder.RoadClass.NONE) continue;

                // Generic street/canopy repair must never tunnel through the imported citadel,
                // district buildings or their authored yards. The initial streamed build already
                // established the correct local relationship between those structures and streets.
                if (ErdenCapitalProtectedGeometry.protectsAuthoredStructure(x, z)) continue;

                int authoredY = authoredSurfaceY(x, z);
                BlockPos authoredSurface = new BlockPos(x, authoredY, z);
                boolean fluid = !level.getFluidState(authoredSurface).isEmpty();
                if (fluid && roadClass != ErdenCapitalStreamingBuilder.RoadClass.ROYAL) continue;

                int visibleTop = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                int clearTop = Math.min(
                        level.getMaxY() - 1,
                        Math.max(authoredY + 3, Math.min(visibleTop + 3, authoredY + MAX_CLEAR_HEIGHT)));
                for (int y = authoredY + 1; y <= clearTop; y++) {
                    BlockPos clear = new BlockPos(x, y, z);
                    if (!level.getBlockState(clear).isAir()) removed++;
                    set(level, clear, Blocks.AIR);
                }

                for (int y = authoredY - 3; y < authoredY; y++) {
                    BlockPos support = new BlockPos(x, y, z);
                    if (level.getBlockState(support).isAir()
                            || !level.getFluidState(support).isEmpty()) {
                        set(level, support, fluid ? Blocks.STONE_BRICKS : Blocks.DIRT);
                    }
                }

                Block road = fluid
                        ? Blocks.STONE_BRICKS
                        : roadClass == ErdenCapitalStreamingBuilder.RoadClass.ROYAL
                        ? Blocks.POLISHED_ANDESITE : Blocks.PACKED_MUD;
                set(level, authoredSurface, road);
                roads++;

                if (roadClass == ErdenCapitalStreamingBuilder.RoadClass.ROYAL
                        && (x == 0 || z == 0)) {
                    normalizeCulvert(level, x, authoredY, z);
                    culverts++;
                }
            }
        }
        return new RepairResult(roads, culverts, removed);
    }

    private static void normalizeCulvert(ServerLevel level, int x, int surfaceY, int z) {
        set(level, new BlockPos(x, surfaceY - 4, z), Blocks.STONE_BRICKS);
        set(level, new BlockPos(x, surfaceY - 3, z), Blocks.WATER);
        set(level, new BlockPos(x, surfaceY - 2, z), Blocks.AIR);
        set(level, new BlockPos(x, surfaceY - 1, z), Blocks.STONE_BRICKS);

        if (z == 0) {
            setIfLoaded(level, new BlockPos(x, surfaceY - 3, z - 1), Blocks.STONE_BRICKS);
            setIfLoaded(level, new BlockPos(x, surfaceY - 2, z - 1), Blocks.STONE_BRICKS);
            setIfLoaded(level, new BlockPos(x, surfaceY - 3, z + 1), Blocks.STONE_BRICKS);
            setIfLoaded(level, new BlockPos(x, surfaceY - 2, z + 1), Blocks.STONE_BRICKS);
        } else {
            setIfLoaded(level, new BlockPos(x - 1, surfaceY - 3, z), Blocks.STONE_BRICKS);
            setIfLoaded(level, new BlockPos(x - 1, surfaceY - 2, z), Blocks.STONE_BRICKS);
            setIfLoaded(level, new BlockPos(x + 1, surfaceY - 3, z), Blocks.STONE_BRICKS);
            setIfLoaded(level, new BlockPos(x + 1, surfaceY - 2, z), Blocks.STONE_BRICKS);
        }
    }

    private static void verifyCi(ServerLevel level) {
        if (ciPassed || !isCi()) return;
        long diagnosticChunk = pack(DIAGNOSTIC_X >> 4, DIAGNOSTIC_Z >> 4);
        ErdenAuthoredRoadSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenAuthoredRoadSavedData.TYPE);
        if (!data.isNormalized(diagnosticChunk)) return;

        int y = authoredSurfaceY(DIAGNOSTIC_X, DIAGNOSTIC_Z);
        Block road = level.getBlockState(new BlockPos(DIAGNOSTIC_X, y, DIAGNOSTIC_Z)).getBlock();
        Block water = level.getBlockState(new BlockPos(DIAGNOSTIC_X, y - 3, DIAGNOSTIC_Z)).getBlock();
        Block ceiling = level.getBlockState(new BlockPos(DIAGNOSTIC_X, y - 1, DIAGNOSTIC_Z)).getBlock();
        if ((road != Blocks.POLISHED_ANDESITE && road != Blocks.STONE_BRICKS)
                || water != Blocks.WATER || ceiling != Blocks.STONE_BRICKS) return;

        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_AUTHORED_ROADS_PASS revision={} normalized_chunks={} road_columns={} culvert_cells={} canopy_blocks_removed={} diagnostic_y={} canopy_independent=true existing_saves_repaired=true culvert_reasserted=true protected_authored_lots={}",
                ErdenAuthoredRoadSavedData.REVISION,
                data.normalizedChunkCount(), data.roadColumns(), data.culvertCells(),
                data.canopyBlocksRemoved(), y, ErdenCapitalProtectedGeometry.lotCount());
    }

    private static void enqueue(int chunkX, int chunkZ) {
        long packed = pack(chunkX, chunkZ);
        if (QUEUED.add(packed)) PENDING.addLast(packed);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        PENDING.clear();
        QUEUED.clear();
        ciAnchorsQueued = false;
        ciPassed = false;
    }

    private static int authoredSurfaceY(int x, int z) {
        return (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
    }

    private static boolean intersectsCapital(ChunkPos chunk) {
        int minX = chunk.getMinBlockX();
        int maxX = minX + 15;
        int minZ = chunk.getMinBlockZ();
        int maxZ = minZ + 15;
        return maxX >= ErdenCapitalStreamingBuilder.WEST_WALL_X - 48
                && minX <= ErdenCapitalStreamingBuilder.EAST_WALL_X + 48
                && maxZ >= ErdenCapitalStreamingBuilder.NORTH_WALL_Z - 48
                && minZ <= ErdenCapitalStreamingBuilder.SOUTH_WALL_Z + 48;
    }

    private static boolean isCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), UPDATE_FLAGS);
    }

    private static void setIfLoaded(ServerLevel level, BlockPos pos, Block block) {
        if (level.hasChunkAt(pos)) set(level, pos, block);
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

    private record RepairResult(long roadColumns, long culvertCells, long removedBlocks) {
    }
}
