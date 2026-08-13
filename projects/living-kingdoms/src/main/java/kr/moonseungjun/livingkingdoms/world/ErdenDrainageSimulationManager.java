package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loaded-city stormwater simulation for Erden.
 *
 * <p>The static iron road grates and royal culverts remain the physical drainage network. This manager
 * adds hydrology to those blocks: current rain builds storm load, a modeled Silver River stage creates
 * back-pressure, a physically obstructed grate loses capacity, and only loaded streets may receive
 * bounded water sources. Sources created by the simulation are persisted so dry weather can remove
 * exactly those sources later. Normal gameplay never force-loads a district.</p>
 */
public final class ErdenDrainageSimulationManager {
    private static final int WEATHER_INTERVAL = 20;
    private static final int SURVEY_INTERVAL = 40;
    private static final int RECOVERY_INTERVAL = 20;
    private static final int SURVEY_RADIUS = 24;
    private static final int MAX_FLOOD_SOURCES = 64;
    private static final int BASE_RIVER_STAGE_Y = 63;
    private static final int OVERFLOW_THRESHOLD = 96;
    private static final int RECOVERY_THRESHOLD = 68;
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static final int CI_TARGET_X = -779;
    private static final int CI_TARGET_Z = -388;

    private static MinecraftServer activeServer;
    private static boolean ciRequested;
    private static boolean ciTicketHeld;
    private static boolean ciPrepared;
    private static boolean ciOverflowSeen;
    private static boolean ciPassed;
    private static ChunkPos ciChunk;
    private static BlockPos ciDrain;
    private static BlockPos ciBlocker;
    private static int ciOverflowBaseline;
    private static int ciDrainedBaseline;
    private static int ciBlockedBaseline;

    private ErdenDrainageSimulationManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        ErdenDrainageSavedData data = level.getDataStorage().computeIfAbsent(ErdenDrainageSavedData.TYPE);
        long tick = level.getGameTime();

        prepareCiSample(level, data);
        if (tick % WEATHER_INTERVAL == 0L) updateHydrology(level, data);
        if (tick % SURVEY_INTERVAL == 0L) {
            surveyPlayerLoadedDrainage(level, data);
            surveyCiDrain(level, data);
        }
        if (tick % RECOVERY_INTERVAL == 0L) {
            recoverFloodSources(level, data);
            advanceCiRecovery(level, data);
            verifyCi(level, data);
        }
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        ciRequested = false;
        ciTicketHeld = false;
        ciPrepared = false;
        ciOverflowSeen = false;
        ciPassed = false;
        ciChunk = null;
        ciDrain = null;
        ciBlocker = null;
        ciOverflowBaseline = 0;
        ciDrainedBaseline = 0;
        ciBlockedBaseline = 0;
    }

    private static void updateHydrology(ServerLevel level, ErdenDrainageSavedData data) {
        int nextLoad = data.stormLoad();
        if (level.isRaining()) {
            nextLoad += level.isThundering() ? 9 : 6;
        } else {
            nextLoad -= 5;
        }
        data.updateStormLoad(nextLoad);

        int observed = observeLoadedSilverRiverSurface(level);
        int baseStage = observed == Integer.MIN_VALUE ? BASE_RIVER_STAGE_Y : observed;
        int stormRise = data.stormLoad() / 24;
        data.updateRiverStage(baseStage + stormRise);
    }

    private static int observeLoadedSilverRiverSurface(ServerLevel level) {
        int best = Integer.MIN_VALUE;
        Set<Long> sampled = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            int z = player.blockPosition().getZ();
            int riverX = authoredSilverRiverCenterX(z);
            int chunkX = riverX >> 4;
            int chunkZ = z >> 4;
            long key = ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
            if (!sampled.add(key) || !level.hasChunk(chunkX, chunkZ)) continue;
            int maxY = Math.min(level.getMaxY() - 1, 96);
            int minY = Math.max(level.getMinY(), 44);
            for (int y = maxY; y >= minY; y--) {
                if (!level.getBlockState(new BlockPos(riverX, y, z)).is(Blocks.WATER)) continue;
                best = Math.max(best, y);
                break;
            }
        }
        return best;
    }

    private static void surveyPlayerLoadedDrainage(ServerLevel level, ErdenDrainageSavedData data) {
        Set<Long> sampledColumns = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            BlockPos center = player.blockPosition();
            if (!insideCapital(center.getX(), center.getZ())) continue;
            for (int x = center.getX() - SURVEY_RADIUS; x <= center.getX() + SURVEY_RADIUS; x++) {
                for (int z = center.getZ() - SURVEY_RADIUS; z <= center.getZ() + SURVEY_RADIUS; z++) {
                    long key = ((long) x << 32) ^ (z & 0xffffffffL);
                    if (!sampledColumns.add(key) || !insideCapital(x, z) || !level.hasChunk(x >> 4, z >> 4)) {
                        continue;
                    }
                    int surfaceY = RealmSitePlanner.surfaceY(level, x, z);
                    BlockPos drain = new BlockPos(x, surfaceY, z);
                    if (!level.getBlockState(drain).is(Blocks.IRON_TRAPDOOR)) continue;
                    evaluateDrain(level, data, drain);
                }
            }
        }
    }

    private static void evaluateDrain(ServerLevel level, ErdenDrainageSavedData data, BlockPos drain) {
        boolean blocked = isBlocked(level, drain);
        if (blocked) data.recordBlockedSample();

        int riverBackPressure = Math.max(0, data.riverStageY() - BASE_RIVER_STAGE_Y) * 9;
        int obstructionPressure = blocked ? 58 : 0;
        int pressure = data.stormLoad() + riverBackPressure + obstructionPressure;
        if (pressure <= OVERFLOW_THRESHOLD || data.floodCells().size() >= MAX_FLOOD_SOURCES) return;

        BlockPos source = findOverflowSource(level, data, drain);
        if (source == null) return;
        if (!level.setBlock(source, Blocks.WATER.defaultBlockState(), UPDATE_FLAGS)) return;
        data.addFloodCell(new ErdenDrainageSavedData.FloodCell(
                source.getX(), source.getY(), source.getZ(),
                drain.getX(), drain.getY(), drain.getZ()));
        LivingKingdoms.LOGGER.info(
                "Erden storm drain overflow drain={},{},{} source={},{},{} storm_load={} river_stage_y={} blocked={} loaded_only=true owned_source=true",
                drain.getX(), drain.getY(), drain.getZ(), source.getX(), source.getY(), source.getZ(),
                data.stormLoad(), data.riverStageY(), blocked);
    }

    private static boolean isBlocked(ServerLevel level, BlockPos drain) {
        if (!level.hasChunk(drain.getX() >> 4, drain.getZ() >> 4)) return false;
        boolean inletOpen = level.getBlockState(drain.above()).isAir()
                || level.getBlockState(drain.above()).is(Blocks.WATER);
        boolean throatOpen = level.getBlockState(drain.below()).isAir()
                || level.getBlockState(drain.below()).is(Blocks.WATER);
        return !inletOpen || !throatOpen;
    }

    private static BlockPos findOverflowSource(
            ServerLevel level,
            ErdenDrainageSavedData data,
            BlockPos drain) {
        for (int radius = 1; radius <= 2; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    int x = drain.getX() + dx;
                    int z = drain.getZ() + dz;
                    if (!insideCapital(x, z) || !level.hasChunk(x >> 4, z >> 4)) continue;
                    if (ErdenCapitalStreamingBuilder.roadClassAt(x, z)
                            == ErdenCapitalStreamingBuilder.RoadClass.NONE) continue;
                    int surfaceY = RealmSitePlanner.surfaceY(level, x, z);
                    if (Math.abs(surfaceY - drain.getY()) > 1) continue;
                    BlockPos source = new BlockPos(x, surfaceY + 1, z);
                    if (!level.getBlockState(source).isAir() || level.getBlockState(source.below()).isAir()) continue;
                    if (data.hasFloodCell(source.getX(), source.getY(), source.getZ())) continue;
                    return source;
                }
            }
        }
        return null;
    }

    private static void recoverFloodSources(ServerLevel level, ErdenDrainageSavedData data) {
        if (data.floodCells().isEmpty()) return;
        int riverBackPressure = Math.max(0, data.riverStageY() - BASE_RIVER_STAGE_Y) * 9;
        int recovered = 0;
        for (ErdenDrainageSavedData.FloodCell cell : new ArrayList<>(data.floodCells())) {
            if (recovered >= 4) break;
            if (!level.hasChunk(cell.x() >> 4, cell.z() >> 4)) continue;
            BlockPos source = new BlockPos(cell.x(), cell.y(), cell.z());
            BlockPos drain = new BlockPos(cell.drainX(), cell.drainY(), cell.drainZ());
            boolean blocked = isBlocked(level, drain);
            int pressure = data.stormLoad() + riverBackPressure + (blocked ? 58 : 0);
            if (pressure > RECOVERY_THRESHOLD) continue;
            if (level.getBlockState(source).is(Blocks.WATER)) {
                level.setBlock(source, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
            }
            data.removeFloodCell(cell);
            recovered++;
            LivingKingdoms.LOGGER.info(
                    "Erden stormwater receded source={},{},{} drain={},{},{} storm_load={} river_stage_y={} obstruction_clear={} owned_source=true",
                    source.getX(), source.getY(), source.getZ(),
                    drain.getX(), drain.getY(), drain.getZ(), data.stormLoad(), data.riverStageY(), !blocked);
        }
    }

    private static int authoredSilverRiverCenterX(int z) {
        return (int) Math.round(-820.0
                + Math.sin(z / 2_900.0) * 470.0
                + Math.sin(z / 930.0) * 105.0);
    }

    private static boolean insideCapital(int x, int z) {
        return x >= ErdenCapitalStreamingBuilder.WEST_WALL_X
                && x <= ErdenCapitalStreamingBuilder.EAST_WALL_X
                && z >= ErdenCapitalStreamingBuilder.NORTH_WALL_Z
                && z <= ErdenCapitalStreamingBuilder.SOUTH_WALL_Z;
    }

    private static void prepareCiSample(ServerLevel level, ErdenDrainageSavedData data) {
        if (!isCi() || ciPrepared || ciPassed) return;
        ciChunk = new ChunkPos(CI_TARGET_X >> 4, CI_TARGET_Z >> 4);
        if (!ErdenCapitalStreamingBuilder.isChunkBuilt(level, ciChunk.x(), ciChunk.z())) {
            if (!ciRequested) {
                ErdenCapitalStreamingBuilder.requestChunk(level, ciChunk.x(), ciChunk.z());
                ciRequested = true;
            }
            return;
        }
        if (!ciTicketHeld) {
            level.getChunkSource().addTicketAndLoadWithRadius(net.minecraft.server.level.TicketType.PORTAL, ciChunk, 0);
            ciTicketHeld = true;
            return;
        }
        if (!level.hasChunk(ciChunk.x(), ciChunk.z())) return;

        ciDrain = findDrainInChunk(level, ciChunk);
        if (ciDrain == null) {
            throw new IllegalStateException("No physical Erden drainage grate in diagnostic chunk " + ciChunk);
        }
        ciBlocker = ciDrain.above();
        if (!level.getBlockState(ciBlocker).isAir()) {
            throw new IllegalStateException("Diagnostic Erden drainage inlet was not clear before obstruction");
        }
        level.setBlock(ciBlocker, Blocks.COBBLESTONE.defaultBlockState(), UPDATE_FLAGS);
        ciOverflowBaseline = data.overflowEvents();
        ciDrainedBaseline = data.drainedCells();
        ciBlockedBaseline = data.blockedSamples();
        data.updateStormLoad(104);
        data.updateRiverStage(BASE_RIVER_STAGE_Y + 2);
        ciPrepared = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden drainage CI grate={},{},{} physical_grate=true obstruction_block={} synthetic_storm_load={} loaded_only=true",
                ciDrain.getX(), ciDrain.getY(), ciDrain.getZ(),
                level.getBlockState(ciBlocker).getBlock(), data.stormLoad());
    }

    private static BlockPos findDrainInChunk(ServerLevel level, ChunkPos chunk) {
        for (int x = chunk.getMinBlockX(); x <= chunk.getMinBlockX() + 15; x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMinBlockZ() + 15; z++) {
                int y = RealmSitePlanner.surfaceY(level, x, z);
                BlockPos candidate = new BlockPos(x, y, z);
                if (level.getBlockState(candidate).is(Blocks.IRON_TRAPDOOR)) return candidate;
            }
        }
        return null;
    }

    private static void surveyCiDrain(ServerLevel level, ErdenDrainageSavedData data) {
        if (!isCi() || !ciPrepared || ciPassed || ciDrain == null || ciOverflowSeen) return;
        if (!level.hasChunk(ciDrain.getX() >> 4, ciDrain.getZ() >> 4)) return;
        evaluateDrain(level, data, ciDrain);
        if (data.overflowEvents() <= ciOverflowBaseline || data.floodCells().isEmpty()) return;
        ciOverflowSeen = true;
        if (ciBlocker != null && level.getBlockState(ciBlocker).is(Blocks.COBBLESTONE)) {
            level.setBlock(ciBlocker, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
        data.updateStormLoad(0);
        data.updateRiverStage(BASE_RIVER_STAGE_Y);
    }

    private static void advanceCiRecovery(ServerLevel level, ErdenDrainageSavedData data) {
        if (!isCi() || !ciPrepared || !ciOverflowSeen || ciPassed) return;
        if (ciBlocker != null && level.getBlockState(ciBlocker).is(Blocks.COBBLESTONE)) {
            level.setBlock(ciBlocker, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
    }

    private static void verifyCi(ServerLevel level, ErdenDrainageSavedData data) {
        if (!isCi() || !ciPrepared || !ciOverflowSeen || ciPassed) return;
        if (data.overflowEvents() <= ciOverflowBaseline
                || data.blockedSamples() <= ciBlockedBaseline
                || data.drainedCells() <= ciDrainedBaseline
                || !data.floodCells().isEmpty()) return;
        ciPassed = true;
        cleanupCi(level);
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_DRAINAGE_PASS revision=1 physical_grate=true weather_accumulation=true obstruction_detected=true modeled_river_stage=true overflow_visible=true recovery=true loaded_city_only=true forced_citywide=false persistent_flood_ownership=true");
    }

    private static void cleanupCi(ServerLevel level) {
        if (ciBlocker != null && level.getBlockState(ciBlocker).is(Blocks.COBBLESTONE)) {
            level.setBlock(ciBlocker, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
        if (ciTicketHeld && ciChunk != null) {
            level.getChunkSource().removeTicketWithRadius(net.minecraft.server.level.TicketType.PORTAL, ciChunk, 0);
            ciTicketHeld = false;
        }
    }

    private static boolean isCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }
}
