package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Set;

/**
 * Adds a second, physically reachable interior storey to streamed Erden urban buildings when the
 * retained external shell is tall enough. The pass deliberately stays inside the narrow functional
 * room carved by {@link ErdenUrbanInteriorBuilder}; it never redraws the attributed facade or roof.
 */
public final class ErdenUrbanUpperFloorBuilder {
    public static final int VERTICAL_REVISION = 1;

    private static final int HALF_WIDTH = 3;
    private static final int DEPTH = 9;
    private static final int UPPER_FLOOR_OFFSET = 5;
    private static final int UPPER_CLEAR_HEIGHT = 3;
    private static final int STAIR_STEPS = 5;
    private static final int PROCESS_BUDGET = 1;
    private static final int MAX_SHELL_SCAN = 32;
    private static final int MIN_TALL_PROBES = 2;
    private static final Set<String> SUPPORTED_ROLES = Set.of(
            "tenement", "shop", "bakery", "inn",
            "stable", "guard_post", "bathhouse", "warehouse"
    );

    private static MinecraftServer activeServer;
    private static boolean diagnosticsLogged;
    private static boolean completionLogged;
    private static boolean ciSamplePassed;

    private ErdenUrbanUpperFloorBuilder() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances = ExternalUrbanFabricBuilder.entrances();
        logDiagnosticsOnce(entrances);

        ErdenUrbanInteriorSavedData interiors = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE);
        ErdenUrbanUpperFloorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanUpperFloorSavedData.TYPE);

        int processedThisTick = 0;
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : entrances) {
            if (processedThisTick >= PROCESS_BUDGET) break;
            long key = entranceKey(entrance);
            if (data.isComplete(key, VERTICAL_REVISION)) continue;
            if (!interiors.isComplete(key, ErdenUrbanInteriorBuilder.INTERIOR_REVISION)) continue;

            try {
                BuildResult result = tryComplete(level, entrance);
                if (result == BuildResult.WAITING) continue;
                data.markComplete(key, VERTICAL_REVISION, result == BuildResult.BUILT);
                processedThisTick++;
                verifyCiSampleIfNeeded(level, entrance, result);
            } catch (Throwable throwable) {
                LivingKingdoms.LOGGER.error(
                        "Unable to complete Erden upper-floor interior role={} entrance={},{}",
                        entrance.role(), entrance.x(), entrance.z(), throwable);
            }
        }

        int complete = data.completedCount(VERTICAL_REVISION);
        if (!completionLogged && complete == entrances.size()) {
            completionLogged = true;
            int built = data.builtCount(VERTICAL_REVISION);
            LivingKingdoms.LOGGER.info(
                    "Completed Erden vertical urban interior pass plots={} upper_floors={} shell_skips={} stair_steps={} revision={}",
                    complete, built, complete - built, STAIR_STEPS, VERTICAL_REVISION);
        }
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        diagnosticsLogged = false;
        completionLogged = false;
        ciSamplePassed = false;
    }

    private static void logDiagnosticsOnce(List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances) {
        if (diagnosticsLogged) return;
        int supported = 0;
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : entrances) {
            if (SUPPORTED_ROLES.contains(entrance.role())) supported++;
        }
        if (supported != entrances.size()) {
            throw new IllegalStateException(
                    "Unsupported Erden vertical interior roles supported=" + supported
                            + " entrances=" + entrances.size());
        }
        diagnosticsLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden vertical urban interiors plots={} upper_floor_offset={} clear_height={} stair_steps={}",
                entrances.size(), UPPER_FLOOR_OFFSET, UPPER_CLEAR_HEIGHT, STAIR_STEPS);
    }

    private static BuildResult tryComplete(
            ServerLevel level,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        Room geometry = room(entrance, 0);
        if (!chunksReady(level, geometry.bounds())) return BuildResult.WAITING;

        int doorY = findLowestDoorY(level, entrance.x(), entrance.z());
        if (doorY == Integer.MIN_VALUE) return BuildResult.WAITING;
        Room ground = room(entrance, doorY - 1);
        if (!hasVerticalEnvelope(level, ground)) return BuildResult.SKIPPED_SHORT_SHELL;

        Room upper = ground.withFloorY(ground.floorY + UPPER_FLOOR_OFFSET);
        carveUpperRoom(level, upper);
        int stairSide = stairSide(entrance);
        buildStaircase(level, ground, upper, stairSide);
        furnishUpperFloor(level, upper);
        verifyVerticalRoom(level, ground, upper, stairSide, doorY);
        return BuildResult.BUILT;
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
            if (level.getBlockState(cursor).getBlock() instanceof DoorBlock) lowest = Math.min(lowest, y);
        }
        return lowest == Integer.MAX_VALUE ? Integer.MIN_VALUE : lowest;
    }

    private static boolean hasVerticalEnvelope(ServerLevel level, Room ground) {
        int upperFloorY = ground.floorY + UPPER_FLOOR_OFFSET;
        int minimumShellY = upperFloorY + UPPER_CLEAR_HEIGHT + 1;
        int maximumShellY = Math.min(level.getMaxY() - 1, ground.floorY + MAX_SHELL_SCAN);
        Point[] probes = new Point[]{
                new Point(ground.doorX, ground.doorZ),
                ground.point(0, 3), ground.point(0, 8),
                ground.point(-HALF_WIDTH, 3), ground.point(HALF_WIDTH, 3),
                ground.point(-HALF_WIDTH, 8), ground.point(HALF_WIDTH, 8)
        };
        int tallProbes = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Point probe : probes) {
            boolean shellAbove = false;
            for (int y = minimumShellY; y <= maximumShellY; y++) {
                cursor.set(probe.x, y, probe.z);
                if (!level.getBlockState(cursor).isAir()) {
                    shellAbove = true;
                    break;
                }
            }
            if (shellAbove && ++tallProbes >= MIN_TALL_PROBES) return true;
        }
        return false;
    }

    private static void carveUpperRoom(ServerLevel level, Room upper) {
        Block floor = upperFloorBlock(upper.role);
        for (int depth = 1; depth <= DEPTH; depth++) {
            for (int lateral = -HALF_WIDTH; lateral <= HALF_WIDTH; lateral++) {
                Point point = upper.point(lateral, depth);
                set(level, point.x, upper.floorY, point.z, floor.defaultBlockState());
                for (int yOffset = 1; yOffset <= UPPER_CLEAR_HEIGHT; yOffset++) {
                    set(level, point.x, upper.floorY + yOffset, point.z, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static Block upperFloorBlock(String role) {
        return switch (role) {
            case "guard_post", "bathhouse" -> Blocks.STONE_BRICKS;
            case "stable", "warehouse" -> Blocks.SPRUCE_PLANKS;
            default -> Blocks.OAK_PLANKS;
        };
    }

    private static int stairSide(ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        return ((entrance.x() * 31 + entrance.z()) & 1) == 0 ? -2 : 2;
    }

    private static void buildStaircase(ServerLevel level, Room ground, Room upper, int stairSide) {
        BlockState stair = Blocks.SPRUCE_STAIRS.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, ground.inwardDirection);
        for (int step = 0; step < STAIR_STEPS; step++) {
            int depth = 2 + step;
            int y = ground.floorY + 1 + step;
            Point point = ground.point(stairSide, depth);
            set(level, point.x, y, point.z, stair);
            set(level, point.x, y + 1, point.z, Blocks.AIR.defaultBlockState());
            set(level, point.x, y + 2, point.z, Blocks.AIR.defaultBlockState());
        }
        for (int depth = 4; depth <= 5; depth++) {
            Point opening = upper.point(stairSide, depth);
            set(level, opening.x, upper.floorY, opening.z, Blocks.AIR.defaultBlockState());
        }
    }

    private static void furnishUpperFloor(ServerLevel level, Room room) {
        switch (room.role) {
            case "tenement" -> {
                placeBed(level, room, -2, 7, bed("white_bed"));
                placeBed(level, room, 2, 7, bed("light_gray_bed"));
                place(level, room, -3, 3, 1, Blocks.BARREL);
                place(level, room, 3, 3, 1, Blocks.BOOKSHELF);
            }
            case "shop" -> {
                place(level, room, -3, 4, 1, Blocks.BARREL);
                place(level, room, 3, 4, 1, Blocks.CHEST);
                place(level, room, -3, 8, 1, Blocks.BOOKSHELF);
                place(level, room, 3, 8, 1, Blocks.BOOKSHELF);
            }
            case "bakery" -> {
                place(level, room, -3, 4, 1, Blocks.BARREL);
                place(level, room, 3, 4, 1, Blocks.HAY_BLOCK);
                place(level, room, -3, 8, 1, Blocks.CHEST);
                place(level, room, 3, 8, 1, Blocks.BARREL);
            }
            case "inn" -> {
                placeBed(level, room, -2, 7, bed("red_bed"));
                placeBed(level, room, 2, 7, bed("blue_bed"));
                place(level, room, -3, 3, 1, Blocks.BARREL);
                place(level, room, 3, 3, 1, Blocks.CHEST);
            }
            case "stable" -> {
                place(level, room, -3, 4, 1, Blocks.HAY_BLOCK);
                place(level, room, 3, 4, 1, Blocks.HAY_BLOCK);
                place(level, room, -3, 8, 1, Blocks.BARREL);
                place(level, room, 3, 8, 1, Blocks.HAY_BLOCK);
            }
            case "guard_post" -> {
                placeBed(level, room, -2, 7, bed("gray_bed"));
                placeBed(level, room, 2, 7, bed("gray_bed"));
                place(level, room, -3, 3, 1, Blocks.BARREL);
                place(level, room, 3, 3, 1, Blocks.TARGET);
            }
            case "bathhouse" -> {
                place(level, room, -3, 4, 1, Blocks.SMOOTH_STONE_SLAB);
                place(level, room, 3, 4, 1, Blocks.SMOOTH_STONE_SLAB);
                place(level, room, -3, 8, 1, Blocks.BARREL);
                place(level, room, 3, 8, 1, Blocks.CHEST);
            }
            case "warehouse" -> {
                for (int depth = 3; depth <= DEPTH; depth += 2) {
                    place(level, room, -3, depth, 1, Blocks.BARREL);
                    place(level, room, 3, depth, 1, Blocks.BARREL);
                }
                place(level, room, -2, DEPTH, 1, Blocks.CHEST);
                place(level, room, 2, DEPTH, 1, Blocks.CHEST);
            }
            default -> throw new IllegalStateException("Unhandled Erden upper-floor role " + room.role);
        }
    }

    private static BedBlock bed(String path) {
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath("minecraft", path));
        if (!(block instanceof BedBlock bed)) {
            throw new IllegalStateException("Missing Minecraft bed block minecraft:" + path);
        }
        return bed;
    }

    private static void placeBed(ServerLevel level, Room room, int lateral, int depth, BedBlock bed) {
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

    private static void place(ServerLevel level, Room room, int lateral, int depth, int yOffset, Block block) {
        Point point = room.point(lateral, depth);
        set(level, point.x, room.floorY + yOffset, point.z, block.defaultBlockState());
    }

    private static void verifyVerticalRoom(
            ServerLevel level,
            Room ground,
            Room upper,
            int stairSide,
            int doorY) {
        if (!(level.getBlockState(new BlockPos(ground.doorX, doorY, ground.doorZ))
                .getBlock() instanceof DoorBlock)) {
            throw new IllegalStateException("Upper-floor pass removed its entrance door");
        }
        for (int depth = 1; depth <= 3; depth++) {
            Point aisle = upper.point(0, depth);
            if (!level.getBlockState(new BlockPos(aisle.x, upper.floorY + 1, aisle.z)).isAir()) {
                throw new IllegalStateException(
                        "Upper-floor aisle is obstructed role=" + upper.role + " depth=" + depth);
            }
        }
        int stairs = 0;
        for (int step = 0; step < STAIR_STEPS; step++) {
            Point point = ground.point(stairSide, 2 + step);
            if (level.getBlockState(new BlockPos(point.x, ground.floorY + 1 + step, point.z)).getBlock()
                    == Blocks.SPRUCE_STAIRS) {
                stairs++;
            }
        }
        if (stairs != STAIR_STEPS) {
            throw new IllegalStateException(
                    "Upper-floor stair connection incomplete role=" + upper.role + " stairs=" + stairs);
        }
        if (!containsRoleFixture(level, upper)) {
            throw new IllegalStateException("Upper-floor interior has no role fixture role=" + upper.role);
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
            case "tenement", "inn", "guard_post" -> block instanceof BedBlock;
            case "shop" -> block == Blocks.BOOKSHELF || block == Blocks.CHEST;
            case "bakery", "stable" -> block == Blocks.HAY_BLOCK || block == Blocks.BARREL;
            case "bathhouse" -> block == Blocks.SMOOTH_STONE_SLAB || block == Blocks.BARREL;
            case "warehouse" -> block == Blocks.BARREL;
            default -> false;
        };
    }

    private static void verifyCiSampleIfNeeded(
            ServerLevel level,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance,
            BuildResult result) {
        if (ciSamplePassed || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        ExternalUrbanFabricBuilder.UrbanEntrance sample = ExternalUrbanFabricBuilder.diagnosticEntrance();
        if (entrance.x() != sample.x() || entrance.z() != sample.z()) return;
        ciSamplePassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_URBAN_VERTICAL_DIAGNOSTIC_PASS role={} entrance={},{} result={} stair_steps={} facade_replaced=false",
                entrance.role(), entrance.x(), entrance.z(), result, STAIR_STEPS);
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlockAndUpdate(new BlockPos(x, y, z), state);
    }

    private static Room room(ExternalUrbanFabricBuilder.UrbanEntrance entrance, int floorY) {
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

    private static long entranceKey(ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        return ((long) entrance.x() << 32) ^ (entrance.z() & 0xffffffffL);
    }

    private enum BuildResult {
        WAITING,
        BUILT,
        SKIPPED_SHORT_SHELL
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

        Room withFloorY(int y) {
            return new Room(
                    role, y, doorX, doorZ,
                    inwardX, inwardZ, rightX, rightZ, inwardDirection);
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
