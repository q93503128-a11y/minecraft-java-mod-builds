package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Map;

/**
 * Gives the 16 attributed district landmarks a physical civic purpose without replacing their
 * licensed architecture. Only empty, supported interior cells are furnished; facade, doors, roof
 * and existing authored fixtures are left untouched.
 */
public final class ErdenLandmarkInteriorManager {
    public static final int INTERIOR_REVISION = 1;

    private static final int PROCESS_INTERVAL = 20;
    private static final int PROCESS_BUDGET = 1;
    private static final int MIN_FUNCTIONAL_FIXTURES = 4;
    private static final int SEARCH_RADIUS = 2;
    private static final int CI_SAMPLE_TICKET_RADIUS = 2;

    private static final Map<String, List<Fixture>> FIXTURES = Map.ofEntries(
            Map.entry("royal_chancery", civic(Blocks.LECTERN, Blocks.BOOKSHELF, Blocks.CARTOGRAPHY_TABLE, Blocks.BARREL, Blocks.CHEST, Blocks.BOOKSHELF)),
            Map.entry("treasury_court", civic(Blocks.IRON_BARS, Blocks.CHEST, Blocks.BARREL, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.CHEST, Blocks.IRON_BARS)),
            Map.entry("western_noble_estate", civic(Blocks.BOOKSHELF, Blocks.BARREL, Blocks.CRAFTING_TABLE, Blocks.CHEST, Blocks.BOOKSHELF, Blocks.BARREL)),
            Map.entry("magistrates_estate", civic(Blocks.LECTERN, Blocks.BOOKSHELF, Blocks.CARTOGRAPHY_TABLE, Blocks.CHEST, Blocks.LECTERN, Blocks.BARREL)),
            Map.entry("great_temple", civic(Blocks.LECTERN, Blocks.BELL, Blocks.CHISELED_STONE_BRICKS, Blocks.BOOKSHELF, Blocks.LECTERN, Blocks.BARREL)),
            Map.entry("pilgrim_hospital", civic(Blocks.BREWING_STAND, Blocks.CAULDRON, Blocks.BARREL, Blocks.CHEST, Blocks.BREWING_STAND, Blocks.BOOKSHELF)),
            Map.entry("western_barracks", military()),
            Map.entry("royal_guard_academy", military()),
            Map.entry("eastern_watch_barracks", military()),
            Map.entry("merchant_guildhall", civic(Blocks.LECTERN, Blocks.BARREL, Blocks.CHEST, Blocks.CRAFTING_TABLE, Blocks.CARTOGRAPHY_TABLE, Blocks.BOOKSHELF)),
            Map.entry("covered_craft_hall", civic(Blocks.LOOM, Blocks.STONECUTTER, Blocks.CRAFTING_TABLE, Blocks.BARREL, Blocks.GRINDSTONE, Blocks.CHEST)),
            Map.entry("artisan_compound", civic(Blocks.SMITHING_TABLE, Blocks.GRINDSTONE, Blocks.CRAFTING_TABLE, Blocks.BARREL, Blocks.STONECUTTER, Blocks.CHEST)),
            Map.entry("citizen_court_west", court()),
            Map.entry("citizen_court_east", court()),
            Map.entry("north_river_warehouse", warehouse()),
            Map.entry("south_river_warehouse", warehouse())
    );

    private static MinecraftServer activeServer;
    private static boolean diagnosticsLogged;
    private static boolean completionLogged;
    private static boolean ciChunksRequested;
    private static boolean ciSamplePassed;
    private static boolean ciSampleTicketHeld;
    private static ChunkPos ciSampleTicketCenter;
    private static long lastCiChunkRefreshTick = Long.MIN_VALUE;

    private ErdenLandmarkInteriorManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        if (level.getGameTime() % PROCESS_INTERVAL != 0L) return;

        List<ExternalDistrictBuildingBuilder.BuildingEntrance> landmarks = landmarkEntrances();
        logDiagnosticsOnce(landmarks);
        requestCiSampleChunks(level, landmarks);

        ErdenLandmarkInteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenLandmarkInteriorSavedData.TYPE);
        int processed = 0;
        for (ExternalDistrictBuildingBuilder.BuildingEntrance entrance : landmarks) {
            if (processed >= PROCESS_BUDGET) break;
            long key = entranceKey(entrance);
            if (data.isComplete(key, INTERIOR_REVISION)) continue;
            try {
                int fixtures = tryFurnish(level, entrance);
                if (fixtures < MIN_FUNCTIONAL_FIXTURES) continue;
                data.markComplete(key, INTERIOR_REVISION);
                processed++;
                verifyCiSampleIfNeeded(level, landmarks, entrance, fixtures);
            } catch (Throwable throwable) {
                LivingKingdoms.LOGGER.error(
                        "Unable to furnish Erden landmark role={} entrance={},{}",
                        entrance.role(), entrance.x(), entrance.z(), throwable);
            }
        }

        int complete = data.completedCount(INTERIOR_REVISION);
        if (!completionLogged && complete == landmarks.size()) {
            completionLogged = true;
            LivingKingdoms.LOGGER.info(
                    "Completed Erden functional landmark interiors landmarks={} minimum_fixtures={} facade_replaced=false revision={}",
                    complete, MIN_FUNCTIONAL_FIXTURES, INTERIOR_REVISION);
        }
    }

    private static void reset(MinecraftServer server) {
        if (activeServer != null && ciSampleTicketHeld && ciSampleTicketCenter != null) {
            ServerLevel oldLevel = activeServer.getLevel(StarterRealmManager.REALM_KEY);
            if (oldLevel != null) {
                oldLevel.getChunkSource().removeTicketWithRadius(
                        TicketType.PORTAL, ciSampleTicketCenter, CI_SAMPLE_TICKET_RADIUS);
            }
        }
        activeServer = server;
        diagnosticsLogged = false;
        completionLogged = false;
        ciChunksRequested = false;
        ciSamplePassed = false;
        ciSampleTicketHeld = false;
        ciSampleTicketCenter = null;
        lastCiChunkRefreshTick = Long.MIN_VALUE;
    }

    private static List<ExternalDistrictBuildingBuilder.BuildingEntrance> landmarkEntrances() {
        return ExternalDistrictBuildingBuilder.entrances().stream()
                .filter(entrance -> !entrance.residential())
                .toList();
    }

    private static void logDiagnosticsOnce(
            List<ExternalDistrictBuildingBuilder.BuildingEntrance> landmarks) {
        if (diagnosticsLogged) return;
        if (landmarks.size() != ExternalDistrictBuildingBuilder.landmarkCount()) {
            throw new IllegalStateException(
                    "Erden landmark entrance mismatch entrances=" + landmarks.size()
                            + " placements=" + ExternalDistrictBuildingBuilder.landmarkCount());
        }
        for (ExternalDistrictBuildingBuilder.BuildingEntrance entrance : landmarks) {
            if (!FIXTURES.containsKey(entrance.role())) {
                throw new IllegalStateException("Missing Erden landmark interior role " + entrance.role());
            }
        }
        diagnosticsLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden landmark interior roles={} landmarks={} non_destructive=true",
                FIXTURES.size(), landmarks.size());
    }

    private static int tryFurnish(
            ServerLevel level,
            ExternalDistrictBuildingBuilder.BuildingEntrance entrance) {
        if (!chunkReady(level, entrance.x(), entrance.z())) return 0;
        int doorY = findLowestDoorY(level, entrance.x(), entrance.z());
        if (doorY == Integer.MIN_VALUE) return 0;

        Frame frame = frame(entrance);
        int floorY = doorY - 1;
        int placed = 0;
        for (Fixture fixture : FIXTURES.get(entrance.role())) {
            if (ensureFixture(level, frame, floorY, fixture)) placed++;
        }
        return placed;
    }

    private static boolean ensureFixture(
            ServerLevel level,
            Frame frame,
            int baseFloorY,
            Fixture fixture) {
        for (int forwardOffset = -SEARCH_RADIUS; forwardOffset <= SEARCH_RADIUS; forwardOffset++) {
            for (int lateralOffset = -SEARCH_RADIUS; lateralOffset <= SEARCH_RADIUS; lateralOffset++) {
                int lateral = fixture.lateral + lateralOffset;
                int forward = Math.max(2, fixture.forward + forwardOffset);
                Point point = frame.point(lateral, forward);
                if (!chunkReady(level, point.x, point.z)) continue;
                int floorY = findUsableFloor(level, point.x, point.z, baseFloorY);
                if (floorY == Integer.MIN_VALUE) continue;
                BlockPos pos = new BlockPos(point.x, floorY + 1, point.z);
                BlockStateCheck check = blockCheck(level, pos, fixture.block);
                if (check == BlockStateCheck.MATCH) return true;
                if (check != BlockStateCheck.EMPTY) continue;
                level.setBlockAndUpdate(pos, fixture.block.defaultBlockState());
                return true;
            }
        }
        return false;
    }

    private static BlockStateCheck blockCheck(ServerLevel level, BlockPos pos, Block expected) {
        Block block = level.getBlockState(pos).getBlock();
        if (block == expected) return BlockStateCheck.MATCH;
        return level.getBlockState(pos).isAir() ? BlockStateCheck.EMPTY : BlockStateCheck.OCCUPIED;
    }

    private static int findUsableFloor(ServerLevel level, int x, int z, int baseFloorY) {
        for (int delta = 0; delta <= 3; delta++) {
            for (int sign : delta == 0 ? new int[]{1} : new int[]{1, -1}) {
                int floorY = baseFloorY + delta * sign;
                if (floorY < level.getMinY() || floorY + 2 >= level.getMaxY()) continue;
                BlockPos floor = new BlockPos(x, floorY, z);
                BlockPos feet = floor.above();
                BlockPos head = feet.above();
                if (!level.getBlockState(floor).isAir()
                        && level.getBlockState(feet).isAir()
                        && level.getBlockState(head).isAir()) {
                    return floorY;
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    private static int findLowestDoorY(ServerLevel level, int x, int z) {
        int designed = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
        int minimum = Math.max(level.getMinY(), designed - 8);
        int maximum = Math.min(level.getMaxY() - 1, designed + 64);
        int lowest = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = minimum; y <= maximum; y++) {
            cursor.set(x, y, z);
            if (level.getBlockState(cursor).getBlock() instanceof DoorBlock) lowest = Math.min(lowest, y);
        }
        return lowest == Integer.MAX_VALUE ? Integer.MIN_VALUE : lowest;
    }

    private static boolean chunkReady(ServerLevel level, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        return level.hasChunk(chunkX, chunkZ)
                && ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ);
    }

    private static void requestCiSampleChunks(
            ServerLevel level,
            List<ExternalDistrictBuildingBuilder.BuildingEntrance> landmarks) {
        if (landmarks.isEmpty() || !ciMode() || ciSamplePassed) return;
        long tick = level.getGameTime();
        if (lastCiChunkRefreshTick != Long.MIN_VALUE
                && tick - lastCiChunkRefreshTick < 40L) return;
        lastCiChunkRefreshTick = tick;

        ExternalDistrictBuildingBuilder.BuildingEntrance sample = landmarks.getFirst();
        ChunkPos requestedCenter = new ChunkPos(sample.x() >> 4, sample.z() >> 4);
        if (ciSampleTicketHeld && ciSampleTicketCenter != null
                && !ciSampleTicketCenter.equals(requestedCenter)) {
            releaseCiSampleTicket(level);
        }
        ciSampleTicketCenter = requestedCenter;
        level.getChunkSource().addTicketAndLoadWithRadius(
                TicketType.PORTAL, ciSampleTicketCenter, CI_SAMPLE_TICKET_RADIUS);
        ciSampleTicketHeld = true;

        Frame frame = frame(sample);
        for (int lateral : new int[]{-7, 0, 7}) {
            for (int forward : new int[]{0, 8, 16}) {
                Point point = frame.point(lateral, forward);
                int chunkX = point.x >> 4;
                int chunkZ = point.z >> 4;
                if (chunkX * 16 >= ErdenCapitalStreamingBuilder.WEST_WALL_X - 16
                        && chunkX * 16 <= ErdenCapitalStreamingBuilder.EAST_WALL_X + 16
                        && chunkZ * 16 >= ErdenCapitalStreamingBuilder.NORTH_WALL_Z - 16
                        && chunkZ * 16 <= ErdenCapitalStreamingBuilder.SOUTH_WALL_Z + 16) {
                    ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
                    ErdenCapitalStreamingBuilder.retainDiagnosticChunk(level, chunkX, chunkZ);
                }
            }
        }
        if (!ciChunksRequested) {
            LivingKingdoms.LOGGER.info(
                    "Retained Erden landmark CI sample role={} chunk={},{} radius={} transient_ticket=portal refreshed_until_verification=true loaded_lease=true refresh_ticks=40 persistent_forced_chunks=false synchronous_get_chunk=false",
                    sample.role(), ciSampleTicketCenter.x(), ciSampleTicketCenter.z(),
                    CI_SAMPLE_TICKET_RADIUS);
        }
        ciChunksRequested = true;
    }

    private static void verifyCiSampleIfNeeded(
            ServerLevel level,
            List<ExternalDistrictBuildingBuilder.BuildingEntrance> landmarks,
            ExternalDistrictBuildingBuilder.BuildingEntrance entrance,
            int fixtures) {
        if (ciSamplePassed || landmarks.isEmpty() || !ciMode()) return;
        ExternalDistrictBuildingBuilder.BuildingEntrance sample = landmarks.getFirst();
        if (entrance.x() != sample.x() || entrance.z() != sample.z()) return;
        ciSamplePassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_LANDMARK_INTERIOR_PASS role={} entrance={},{} fixtures={} facade_replaced=false loaded_only=true",
                entrance.role(), entrance.x(), entrance.z(), fixtures);
        releaseCiSampleTicket(level);
    }

    private static void releaseCiSampleTicket(ServerLevel level) {
        if (!ciSampleTicketHeld || ciSampleTicketCenter == null) return;
        level.getChunkSource().removeTicketWithRadius(
                TicketType.PORTAL, ciSampleTicketCenter, CI_SAMPLE_TICKET_RADIUS);
        LivingKingdoms.LOGGER.info(
                "Released Erden landmark CI sample ticket chunk={},{} radius={} transient_ticket=portal",
                ciSampleTicketCenter.x(), ciSampleTicketCenter.z(), CI_SAMPLE_TICKET_RADIUS);
        ciSampleTicketHeld = false;
        ciSampleTicketCenter = null;
    }

    private static boolean ciMode() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }

    private static Frame frame(ExternalDistrictBuildingBuilder.BuildingEntrance entrance) {
        int deltaX = entrance.roadX() - entrance.x();
        int deltaZ = entrance.roadZ() - entrance.z();
        int inwardX;
        int inwardZ;
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            inwardX = deltaX >= 0 ? -1 : 1;
            inwardZ = 0;
        } else {
            inwardX = 0;
            inwardZ = deltaZ >= 0 ? -1 : 1;
        }
        return new Frame(
                entrance.x(), entrance.z(), inwardX, inwardZ, -inwardZ, inwardX);
    }

    private static long entranceKey(ExternalDistrictBuildingBuilder.BuildingEntrance entrance) {
        return ((long) entrance.x() << 32) ^ (entrance.z() & 0xffffffffL);
    }

    private static List<Fixture> civic(Block a, Block b, Block c, Block d, Block e, Block f) {
        return List.of(
                new Fixture(-2, 4, a), new Fixture(2, 4, b),
                new Fixture(-4, 8, c), new Fixture(4, 8, d),
                new Fixture(-3, 12, e), new Fixture(3, 12, f));
    }

    private static List<Fixture> military() {
        return civic(Blocks.BARREL, Blocks.TARGET, Blocks.ANVIL,
                Blocks.GRINDSTONE, Blocks.CHEST, Blocks.IRON_BARS);
    }

    private static List<Fixture> court() {
        return civic(Blocks.LECTERN, Blocks.BOOKSHELF, Blocks.CARTOGRAPHY_TABLE,
                Blocks.CHEST, Blocks.LECTERN, Blocks.BARREL);
    }

    private static List<Fixture> warehouse() {
        return civic(Blocks.BARREL, Blocks.BARREL, Blocks.CHEST,
                Blocks.BARREL, Blocks.CHEST, Blocks.BARREL);
    }

    private enum BlockStateCheck {
        MATCH,
        EMPTY,
        OCCUPIED
    }

    private record Fixture(int lateral, int forward, Block block) {
    }

    private record Frame(int originX, int originZ, int inwardX, int inwardZ, int rightX, int rightZ) {
        Point point(int lateral, int forward) {
            return new Point(
                    originX + inwardX * forward + rightX * lateral,
                    originZ + inwardZ * forward + rightZ * lateral);
        }
    }

    private record Point(int x, int z) {
    }
}
