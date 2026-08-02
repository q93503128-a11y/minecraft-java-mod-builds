package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Completes a compact, walkable ground floor after the streamed facade cells are present. The
 * retained real door is used as the only anchor, so terrain height, source rotation and schematic
 * offsets are read from the finished world instead of being guessed during template decoding.
 */
public final class ErdenUrbanInteriorBuilder {
    public static final int INTERIOR_REVISION = 1;

    private static final int HALF_WIDTH = 3;
    private static final int DEPTH = 9;
    private static final int CLEAR_HEIGHT = 4;
    private static final int PROCESS_BUDGET = 2;
    private static final Set<String> SUPPORTED_ROLES = Set.of(
            "tenement", "shop", "bakery", "inn",
            "stable", "guard_post", "bathhouse", "warehouse"
    );

    private static MinecraftServer activeServer;
    private static boolean diagnosticsLogged;
    private static boolean completionLogged;
    private static boolean ciChunksRequested;
    private static boolean ciSamplePassed;

    private ErdenUrbanInteriorBuilder() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances =
                ExternalUrbanFabricBuilder.entrances();
        logDiagnosticsOnce(entrances);
        requestCiSampleChunks(level);

        ErdenUrbanInteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE);
        int builtThisTick = 0;
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : entrances) {
            if (builtThisTick >= PROCESS_BUDGET) break;
            long key = entranceKey(entrance);
            if (data.isComplete(key, INTERIOR_REVISION)) continue;
            try {
                if (!tryComplete(level, entrance)) continue;
                data.markComplete(key, INTERIOR_REVISION);
                builtThisTick++;
                verifyCiSampleIfNeeded(level, entrance);
            } catch (Throwable throwable) {
                LivingKingdoms.LOGGER.error(
                        "Unable to complete Erden urban interior role={} entrance={},{}",
                        entrance.role(), entrance.x(), entrance.z(), throwable);
            }
        }

        int complete = data.completedCount(INTERIOR_REVISION);
        if (!completionLogged && complete == entrances.size()) {
            completionLogged = true;
            LivingKingdoms.LOGGER.info(
                    "Completed Erden functional urban interiors plots={} fixture_families={} clear_aisles=true revision={}",
                    complete, SUPPORTED_ROLES.size(), INTERIOR_REVISION);
        }
    }

    public static int fixtureFamilyCount() {
        return SUPPORTED_ROLES.size();
    }

    public static Map<String, Integer> plannedInteriorCounts() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String role : SUPPORTED_ROLES) {
            result.put(role, ExternalUrbanFabricBuilder.roleCount(role));
        }
        return Map.copyOf(result);
    }

    public static int completedCount(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE)
                .completedCount(INTERIOR_REVISION);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        diagnosticsLogged = false;
        completionLogged = false;
        ciChunksRequested = false;
        ciSamplePassed = false;
    }

    private static void logDiagnosticsOnce(
            List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances) {
        if (diagnosticsLogged) return;
        Map<String, Integer> counts = plannedInteriorCounts();
        int total = 0;
        for (String role : SUPPORTED_ROLES) {
            int count = counts.getOrDefault(role, 0);
            if (count <= 0) {
                throw new IllegalStateException(
                        "Missing functional Erden urban interior role " + role);
            }
            total += count;
        }
        if (total != entrances.size()) {
            throw new IllegalStateException(
                    "Urban interior count mismatch roles=" + total
                            + " entrances=" + entrances.size());
        }
        diagnosticsLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden functional urban interiors plots={} fixture_families={} clear_aisles=true roles={}",
                total, SUPPORTED_ROLES.size(), counts);
    }

    private static void requestCiSampleChunks(ServerLevel level) {
        if (ciChunksRequested
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        ExternalUrbanFabricBuilder.UrbanEntrance entrance =
                ExternalUrbanFabricBuilder.diagnosticEntrance();
        Room room = room(entrance, 0);
        Bounds bounds = room.bounds();
        for (int chunkX = Math.floorDiv(bounds.minX, 16);
             chunkX <= Math.floorDiv(bounds.maxX, 16); chunkX++) {
            for (int chunkZ = Math.floorDiv(bounds.minZ, 16);
                 chunkZ <= Math.floorDiv(bounds.maxZ, 16); chunkZ++) {
                ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
            }
        }
        ciChunksRequested = true;
    }

    private static boolean tryComplete(
            ServerLevel level,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        Room geometry = room(entrance, 0);
        if (!chunksReady(level, geometry.bounds())) return false;
        int doorY = findLowestDoorY(level, entrance.x(), entrance.z());
        if (doorY == Integer.MIN_VALUE) return false;

        Room room = room(entrance, doorY - 1);
        carveWalkableRoom(level, room);
        switch (entrance.role()) {
            case "tenement" -> furnishTenement(level, room);
            case "shop" -> furnishShop(level, room);
            case "bakery" -> furnishBakery(level, room);
            case "inn" -> furnishInn(level, room);
            case "stable" -> furnishStable(level, room);
            case "guard_post" -> furnishGuardPost(level, room);
            case "bathhouse" -> furnishBathhouse(level, room);
            case "warehouse" -> furnishWarehouse(level, room);
            default -> throw new IllegalStateException(
                    "Unhandled Erden urban role " + entrance.role());
        }
        verifyFunctionalRoom(level, room, doorY);
        return true;
    }

    private static Room room(
            ExternalUrbanFabricBuilder.UrbanEntrance entrance,
            int floorY) {
        int deltaX = entrance.roadX() - entrance.x();
        int deltaZ = entrance.roadZ() - entrance.z();
        int inwardX;
        int inwardZ;
        Direction inwardDirection;
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            inwardX = deltaX >= 0 ? -1 : 1;
            inwardZ = 0;
            inwardDirection = inwardX > 0 ? Direction.EAST : Direction.WEST;
        } else {
            inwardX = 0;
            inwardZ = deltaZ >= 0 ? -1 : 1;
            inwardDirection = inwardZ > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return new Room(
                entrance.role(), floorY,
                entrance.x(), entrance.z(),
                inwardX, inwardZ,
                -inwardZ, inwardX,
                inwardDirection);
    }

    private static boolean chunksReady(ServerLevel level, Bounds bounds) {
        for (int chunkX = Math.floorDiv(bounds.minX, 16);
             chunkX <= Math.floorDiv(bounds.maxX, 16); chunkX++) {
            for (int chunkZ = Math.floorDiv(bounds.minZ, 16);
                 chunkZ <= Math.floorDiv(bounds.maxZ, 16); chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)
                        || !ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int findLowestDoorY(ServerLevel level, int x, int z) {
        int designed = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
        int minimum = Math.max(level.getMinY(), designed - 8);
        int maximum = Math.min(level.getMaxY() - 1, designed + 64);
        int lowest = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = minimum; y <= maximum; y++) {
            cursor.set(x, y, z);
            if (level.getBlockState(cursor).getBlock() instanceof DoorBlock) {
                lowest = Math.min(lowest, y);
            }
        }
        return lowest == Integer.MAX_VALUE ? Integer.MIN_VALUE : lowest;
    }

    private static void carveWalkableRoom(ServerLevel level, Room room) {
        Block floor = switch (room.role) {
            case "stable" -> Blocks.COARSE_DIRT;
            case "guard_post" -> Blocks.STONE_BRICKS;
            case "bathhouse" -> Blocks.SMOOTH_STONE;
            case "warehouse" -> Blocks.SPRUCE_PLANKS;
            default -> Blocks.OAK_PLANKS;
        };
        for (int depth = 1; depth <= DEPTH; depth++) {
            for (int lateral = -HALF_WIDTH; lateral <= HALF_WIDTH; lateral++) {
                Point point = room.point(lateral, depth);
                set(level, point.x, room.floorY, point.z, floor.defaultBlockState());
                for (int y = 1; y <= CLEAR_HEIGHT; y++) {
                    set(level, point.x, room.floorY + y, point.z,
                            Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void furnishTenement(ServerLevel level, Room room) {
        placeBed(level, room, -2, 5, bed("white_bed"));
        placeBed(level, room, 2, 5, bed("light_gray_bed"));
        place(level, room, -3, 3, 1, Blocks.BARREL);
        place(level, room, 3, 3, 1, Blocks.BARREL);
        place(level, room, 0, DEPTH, 1, Blocks.CRAFTING_TABLE);
    }

    private static void furnishShop(ServerLevel level, Room room) {
        for (int lateral : new int[]{-3, -2, 2, 3}) {
            place(level, room, lateral, 4, 1, Blocks.OAK_SLAB);
        }
        place(level, room, -3, 7, 1, Blocks.BARREL);
        place(level, room, 3, 7, 1, Blocks.CHEST);
        place(level, room, -3, DEPTH, 1, Blocks.BOOKSHELF);
        place(level, room, 3, DEPTH, 1, Blocks.BOOKSHELF);
        place(level, room, 0, DEPTH, 1, Blocks.CRAFTING_TABLE);
    }

    private static void furnishBakery(ServerLevel level, Room room) {
        place(level, room, -2, DEPTH, 1, Blocks.FURNACE);
        place(level, room, 0, DEPTH, 1, Blocks.SMOKER);
        place(level, room, 2, DEPTH, 1, Blocks.FURNACE);
        place(level, room, -3, 5, 1, Blocks.BARREL);
        place(level, room, 3, 5, 1, Blocks.HAY_BLOCK);
        place(level, room, -3, 8, 1, Blocks.CRAFTING_TABLE);
        place(level, room, 3, 8, 1, Blocks.CHEST);
    }

    private static void furnishInn(ServerLevel level, Room room) {
        placeTable(level, room, -2, 4);
        placeTable(level, room, 2, 4);
        placeBed(level, room, -2, 7, bed("red_bed"));
        placeBed(level, room, 2, 7, bed("blue_bed"));
        place(level, room, -3, DEPTH, 1, Blocks.BARREL);
        place(level, room, 3, DEPTH, 1, Blocks.CHEST);
    }

    private static void furnishStable(ServerLevel level, Room room) {
        for (int depth = 4; depth <= DEPTH; depth++) {
            place(level, room, -2, depth, 1, Blocks.OAK_FENCE);
            place(level, room, 2, depth, 1, Blocks.OAK_FENCE);
        }
        place(level, room, -3, DEPTH, 1, Blocks.HAY_BLOCK);
        place(level, room, 3, DEPTH, 1, Blocks.HAY_BLOCK);
        place(level, room, -3, 5, 1, Blocks.WATER_CAULDRON);
        place(level, room, 3, 5, 1, Blocks.BARREL);
    }

    private static void furnishGuardPost(ServerLevel level, Room room) {
        for (int depth = 5; depth <= DEPTH; depth++) {
            place(level, room, -2, depth, 1, Blocks.IRON_BARS);
            place(level, room, 2, depth, 1, Blocks.IRON_BARS);
        }
        place(level, room, -2, DEPTH, 1, Blocks.ANVIL);
        place(level, room, 0, DEPTH, 1, Blocks.STONECUTTER);
        place(level, room, 2, DEPTH, 1, Blocks.TARGET);
        place(level, room, -3, 3, 1, Blocks.BARREL);
        place(level, room, 3, 3, 1, Blocks.CHEST);
    }

    private static void furnishBathhouse(ServerLevel level, Room room) {
        for (int depth = 5; depth <= 8; depth++) {
            for (int lateral = -3; lateral <= 3; lateral++) {
                boolean border = depth == 5 || depth == 8
                        || lateral == -3 || lateral == 3;
                place(level, room, lateral, depth, 0, Blocks.SMOOTH_STONE);
                place(level, room, lateral, depth, 1,
                        border ? Blocks.SMOOTH_STONE : Blocks.WATER);
            }
        }
        place(level, room, -3, 3, 1, Blocks.SMOOTH_STONE_SLAB);
        place(level, room, 3, 3, 1, Blocks.SMOOTH_STONE_SLAB);
        place(level, room, -3, DEPTH, 1, Blocks.WATER_CAULDRON);
        place(level, room, 3, DEPTH, 1, Blocks.WATER_CAULDRON);
    }

    private static void furnishWarehouse(ServerLevel level, Room room) {
        for (int depth = 3; depth <= DEPTH; depth += 2) {
            place(level, room, -3, depth, 1, Blocks.BARREL);
            place(level, room, 3, depth, 1, Blocks.BARREL);
            if (depth >= 5) {
                place(level, room, -3, depth, 2, Blocks.BARREL);
                place(level, room, 3, depth, 2, Blocks.BARREL);
            }
        }
        place(level, room, -2, DEPTH, 1, Blocks.CHEST);
        place(level, room, 2, DEPTH, 1, Blocks.CHEST);
        place(level, room, 0, DEPTH, 1, Blocks.CRAFTING_TABLE);
    }

    private static void placeTable(ServerLevel level, Room room, int lateral, int depth) {
        place(level, room, lateral, depth, 1, Blocks.OAK_FENCE);
        place(level, room, lateral, depth, 2, Blocks.OAK_PRESSURE_PLATE);
    }

    private static BedBlock bed(String path) {
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath("minecraft", path));
        if (!(block instanceof BedBlock bed)) {
            throw new IllegalStateException("Missing Minecraft bed block minecraft:" + path);
        }
        return bed;
    }

    private static void placeBed(
            ServerLevel level, Room room,
            int lateral, int depth, BedBlock bed) {
        Point footPoint = room.point(lateral, depth);
        Point headPoint = room.point(lateral, depth + 1);
        BlockState foot = bed.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.FOOT)
                .setValue(HorizontalDirectionalBlock.FACING, room.inwardDirection);
        BlockState head = bed.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.HEAD)
                .setValue(HorizontalDirectionalBlock.FACING, room.inwardDirection);
        set(level, footPoint.x, room.floorY + 1, footPoint.z, foot);
        set(level, headPoint.x, room.floorY + 1, headPoint.z, head);
    }

    private static void place(
            ServerLevel level, Room room,
            int lateral, int depth, int yOffset, Block block) {
        Point point = room.point(lateral, depth);
        set(level, point.x, room.floorY + yOffset, point.z,
                block.defaultBlockState());
    }

    private static void set(
            ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlockAndUpdate(new BlockPos(x, y, z), state);
    }

    private static void verifyFunctionalRoom(ServerLevel level, Room room, int doorY) {
        if (!(level.getBlockState(new BlockPos(room.doorX, doorY, room.doorZ))
                .getBlock() instanceof DoorBlock)) {
            throw new IllegalStateException("Urban interior removed its entrance door");
        }
        for (int depth = 1; depth <= 3; depth++) {
            Point aisle = room.point(0, depth);
            if (!level.getBlockState(new BlockPos(
                    aisle.x, room.floorY + 1, aisle.z)).isAir()) {
                throw new IllegalStateException(
                        "Urban interior aisle is obstructed role=" + room.role
                                + " depth=" + depth);
            }
        }
        if (!containsRoleFixture(level, room)) {
            throw new IllegalStateException(
                    "Urban interior has no role fixture role=" + room.role);
        }
    }

    private static boolean containsRoleFixture(ServerLevel level, Room room) {
        for (int depth = 1; depth <= DEPTH; depth++) {
            for (int lateral = -HALF_WIDTH; lateral <= HALF_WIDTH; lateral++) {
                Point point = room.point(lateral, depth);
                for (int yOffset = 1; yOffset <= 2; yOffset++) {
                    Block block = level.getBlockState(new BlockPos(
                            point.x, room.floorY + yOffset, point.z)).getBlock();
                    if (isRoleFixture(room.role, block)) return true;
                }
            }
        }
        return false;
    }

    private static boolean isRoleFixture(String role, Block block) {
        return switch (role) {
            case "tenement", "inn" -> block instanceof BedBlock;
            case "shop" -> block == Blocks.BOOKSHELF || block == Blocks.CHEST;
            case "bakery" -> block == Blocks.SMOKER;
            case "stable" -> block == Blocks.HAY_BLOCK || block == Blocks.OAK_FENCE;
            case "guard_post" -> block == Blocks.ANVIL || block == Blocks.TARGET;
            case "bathhouse" -> block == Blocks.WATER || block == Blocks.WATER_CAULDRON;
            case "warehouse" -> block == Blocks.BARREL;
            default -> false;
        };
    }

    private static void verifyCiSampleIfNeeded(
            ServerLevel level,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        if (ciSamplePassed
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        ExternalUrbanFabricBuilder.UrbanEntrance sample =
                ExternalUrbanFabricBuilder.diagnosticEntrance();
        if (entrance.x() != sample.x() || entrance.z() != sample.z()) return;
        ciSamplePassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_URBAN_INTERIOR_DIAGNOSTIC_PASS role={} entrance={},{} fixture_families={} clear_aisle=true",
                entrance.role(), entrance.x(), entrance.z(), SUPPORTED_ROLES.size());
    }

    private static long entranceKey(
            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        return ((long) entrance.x() << 32) ^ (entrance.z() & 0xffffffffL);
    }

    private record Room(
            String role,
            int floorY,
            int doorX,
            int doorZ,
            int inwardX,
            int inwardZ,
            int rightX,
            int rightZ,
            Direction inwardDirection) {
        Point point(int lateral, int forward) {
            return new Point(
                    doorX + inwardX * forward + rightX * lateral,
                    doorZ + inwardZ * forward + rightZ * lateral);
        }

        Bounds bounds() {
            Point a = point(-HALF_WIDTH, 1);
            Point b = point(HALF_WIDTH, 1);
            Point c = point(-HALF_WIDTH, DEPTH);
            Point d = point(HALF_WIDTH, DEPTH);
            return new Bounds(
                    Math.min(doorX, Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x))),
                    Math.max(doorX, Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x))),
                    Math.min(doorZ, Math.min(Math.min(a.z, b.z), Math.min(c.z, d.z))),
                    Math.max(doorZ, Math.max(Math.max(a.z, b.z), Math.max(c.z, d.z))));
        }
    }

    private record Point(int x, int z) {
    }

    private record Bounds(int minX, int maxX, int minZ, int maxZ) {
    }
}
