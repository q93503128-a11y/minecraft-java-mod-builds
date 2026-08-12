package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Reconciles each imported door's real vertical position with its terrain-following access path.
 * External schematics can place a door two or three metres above their natural lot surface; the
 * old x/z-only path reached the facade but could leave an unclimbable vertical threshold. The
 * repaired route first leaves the facade on the same cardinal axis used by the functional interior
 * and only then bends toward the street, preventing diagonal paths from clipping building corners.
 */
public final class ErdenEntranceThresholdManager {
    public static final int THRESHOLD_REVISION = 2;

    private static final int EXPECTED_ENTRANCES = 273;
    private static final int PROCESS_INTERVAL = 5;
    private static final int PROCESS_BUDGET = 6;
    private static final int PORCH_STEPS = 3;
    private static final int BASE_APPROACH_STEPS = 8;
    private static final int MAX_APPROACH_STEPS = 16;
    private static final int UPDATE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private static MinecraftServer activeServer;
    private static List<Entry> cachedEntries;
    private static boolean completionLogged;

    private ErdenEntranceThresholdManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        if (level.getGameTime() % PROCESS_INTERVAL != 0L) return;

        List<Entry> entries = entries();
        ErdenEntranceThresholdSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenEntranceThresholdSavedData.TYPE);
        int processed = 0;
        for (Entry entry : entries) {
            if (processed >= PROCESS_BUDGET) break;
            long key = key(entry.x, entry.z);
            if (data.isComplete(key, THRESHOLD_REVISION)) continue;
            if (!entryChunkReady(level, entry)) continue;

            int doorY = findLowestDoorY(level, entry.x, entry.z);
            if (doorY == Integer.MIN_VALUE) continue;
            Approach approach = approach(entry, doorY);
            if (!preflight(level, entry, doorY, approach)) continue;
            if (!normalize(level, entry, doorY, approach)) continue;

            data.markComplete(key, THRESHOLD_REVISION);
            processed++;
            if (diagnosticMode()) {
                LivingKingdoms.LOGGER.debug(
                        "Normalized Erden entrance threshold kind={} role={} entrance={},{} road={},{} door_y={} approach_steps={} porch_steps={} floor_delta={}",
                        entry.kind, entry.role, entry.x, entry.z, entry.roadX, entry.roadZ,
                        doorY, approach.steps, approach.porchSteps,
                        approach.endFloor - (doorY - 1));
            }
        }

        int complete = data.completedCount(THRESHOLD_REVISION);
        if (!completionLogged && complete == entries.size()) {
            completionLogged = true;
            LivingKingdoms.LOGGER.info(
                    "Completed Erden entrance threshold normalization entrances={} graded_landings=true cardinal_porches=true real_door_heights=true loaded_only=true revision={}",
                    complete, THRESHOLD_REVISION);
        }
    }

    public static boolean isComplete(ServerLevel level, int x, int z) {
        return level.getDataStorage().computeIfAbsent(ErdenEntranceThresholdSavedData.TYPE)
                .isComplete(key(x, z), THRESHOLD_REVISION);
    }

    public static int completedCount(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenEntranceThresholdSavedData.TYPE)
                .completedCount(THRESHOLD_REVISION);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        completionLogged = false;
    }

    private static List<Entry> entries() {
        List<Entry> result = cachedEntries;
        if (result != null) return result;
        synchronized (ErdenEntranceThresholdManager.class) {
            result = cachedEntries;
            if (result == null) {
                List<Entry> built = new ArrayList<>(EXPECTED_ENTRANCES);
                for (ExternalUrbanFabricBuilder.UrbanEntrance entrance
                        : ExternalUrbanFabricBuilder.entrances()) {
                    built.add(new Entry(
                            "urban", entrance.role(), entrance.x(), entrance.z(),
                            entrance.roadX(), entrance.roadZ(), true));
                }
                for (ExternalDistrictBuildingBuilder.BuildingEntrance entrance
                        : ExternalDistrictBuildingBuilder.entrances()) {
                    built.add(new Entry(
                            "district", entrance.role(), entrance.x(), entrance.z(),
                            entrance.roadX(), entrance.roadZ(), entrance.residential()));
                }
                if (built.size() != EXPECTED_ENTRANCES) {
                    throw new IllegalStateException(
                            "Erden entrance threshold count mismatch " + built.size()
                                    + " != " + EXPECTED_ENTRANCES);
                }
                result = List.copyOf(built);
                cachedEntries = result;
                LivingKingdoms.LOGGER.info(
                        "Prepared Erden entrance threshold reconciler entrances={} terrain_matched=true cardinal_porches=true",
                        result.size());
            }
            return result;
        }
    }

    private static boolean entryChunkReady(ServerLevel level, Entry entry) {
        int chunkX = entry.x >> 4;
        int chunkZ = entry.z >> 4;
        return level.hasChunk(chunkX, chunkZ)
                && ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ);
    }

    private static int findLowestDoorY(ServerLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) return Integer.MIN_VALUE;
        int designed = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
        int minimum = Math.max(level.getMinY(), designed - 18);
        int maximum = Math.min(level.getMaxY() - 1, designed + 82);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int lowest = Integer.MAX_VALUE;
        for (int y = minimum; y <= maximum; y++) {
            cursor.set(x, y, z);
            if (level.getBlockState(cursor).getBlock() instanceof DoorBlock) {
                lowest = Math.min(lowest, y);
            }
        }
        return lowest == Integer.MAX_VALUE ? Integer.MIN_VALUE : lowest;
    }

    private static Approach approach(Entry entry, int doorY) {
        Route route = route(entry);
        int totalSteps = route.points.size();
        int steps = Math.min(totalSteps, BASE_APPROACH_STEPS);
        Point end = route.points.get(steps - 1);
        int endFloor = designedFloor(end.x, end.z);
        int doorFloor = doorY - 1;
        int required = Math.min(MAX_APPROACH_STEPS, Math.abs(endFloor - doorFloor) + 2);
        if (required > steps && required <= totalSteps) {
            steps = required;
            end = route.points.get(steps - 1);
            endFloor = designedFloor(end.x, end.z);
        }
        return new Approach(
                route.points,
                Math.max(1, steps),
                route.porchSteps,
                doorFloor,
                endFloor);
    }

    /**
     * Validates the whole cardinal doorway run before the first write. We never erase a facade just
     * to make a path pass: every cell from the real door through the short porch must already have
     * player body clearance. Beyond that authored exterior run, the terrain path may be graded and
     * cleared because it is deliberately outside the building envelope.
     */
    private static boolean preflight(
            ServerLevel level,
            Entry entry,
            int doorY,
            Approach approach) {
        if (!walkable(level, entry.x, doorY, entry.z)) return false;
        int previousFloor = approach.doorFloor;
        for (int step = 1; step <= approach.steps; step++) {
            Point center = approach.points.get(step - 1);
            if (!level.hasChunk(center.x >> 4, center.z >> 4)
                    || !ErdenCapitalStreamingBuilder.isChunkBuilt(
                    level, center.x >> 4, center.z >> 4)) {
                return false;
            }
            float progress = step / (float) approach.steps;
            int targetFloor = Math.round(
                    approach.doorFloor
                            + (approach.endFloor - approach.doorFloor) * progress);
            if (Math.abs(targetFloor - previousFloor) > 1) return false;
            if (step <= approach.porchSteps) {
                BlockPos feet = new BlockPos(center.x, targetFloor + 1, center.z);
                if (!bodyPassable(level, feet) || !bodyPassable(level, feet.above())) {
                    return false;
                }
            }
            previousFloor = targetFloor;
        }
        return Math.abs(previousFloor - approach.endFloor) <= 1;
    }

    private static boolean normalize(
            ServerLevel level,
            Entry entry,
            int doorY,
            Approach approach) {
        Block material = entry.urbanOrResidential ? Blocks.PACKED_MUD : Blocks.STONE_BRICKS;
        int previousFloor = approach.doorFloor;
        Point previousPoint = new Point(entry.x, entry.z);

        for (int step = 1; step <= approach.steps; step++) {
            Point center = approach.points.get(step - 1);
            float progress = step / (float) approach.steps;
            int targetFloor = Math.round(
                    approach.doorFloor
                            + (approach.endFloor - approach.doorFloor) * progress);
            if (Math.abs(targetFloor - previousFloor) > 1) return false;

            int movementX = center.x - previousPoint.x;
            int movementZ = center.z - previousPoint.z;
            boolean eastWest = Math.abs(movementX) >= Math.abs(movementZ);
            int halfWidth = step <= approach.porchSteps ? 0 : 1;
            for (int width = -halfWidth; width <= halfWidth; width++) {
                int x = eastWest ? center.x : center.x + width;
                int z = eastWest ? center.z + width : center.z;
                if (!level.hasChunk(x >> 4, z >> 4)
                        || !ErdenCapitalStreamingBuilder.isChunkBuilt(
                        level, x >> 4, z >> 4)) {
                    return false;
                }
                int naturalFloor = designedFloor(x, z);
                if (targetFloor >= naturalFloor) {
                    for (int y = naturalFloor; y <= targetFloor; y++) {
                        set(level, x, y, z, material);
                    }
                } else {
                    set(level, x, targetFloor, z, material);
                }
                if (step > approach.porchSteps) {
                    int clearTop = Math.max(targetFloor + 3, naturalFloor + 2);
                    for (int y = targetFloor + 1; y <= clearTop; y++) {
                        set(level, x, y, z, Blocks.AIR);
                    }
                }
            }
            if (!walkable(level, center.x, targetFloor + 1, center.z)) return false;
            previousFloor = targetFloor;
            previousPoint = center;
        }

        if (Math.abs(previousFloor - approach.endFloor) > 1) return false;
        return walkable(level, entry.x, doorY, entry.z);
    }

    private static Route route(Entry entry) {
        int deltaX = entry.roadX - entry.x;
        int deltaZ = entry.roadZ - entry.z;
        int directSteps = Math.max(Math.abs(deltaX), Math.abs(deltaZ));
        if (directSteps <= 0) {
            return new Route(List.of(new Point(entry.x, entry.z)), 0);
        }

        Vector outward = outward(entry);
        int porchSteps = Math.min(PORCH_STEPS, directSteps);
        List<Point> points = new ArrayList<>(directSteps + PORCH_STEPS);
        for (int step = 1; step <= porchSteps; step++) {
            points.add(new Point(
                    entry.x + outward.x * step,
                    entry.z + outward.z * step));
        }

        Point porch = points.getLast();
        int secondDeltaX = entry.roadX - porch.x;
        int secondDeltaZ = entry.roadZ - porch.z;
        int secondSteps = Math.max(Math.abs(secondDeltaX), Math.abs(secondDeltaZ));
        for (int step = 1; step <= secondSteps; step++) {
            float progress = step / (float) secondSteps;
            Point point = new Point(
                    porch.x + Math.round(secondDeltaX * progress),
                    porch.z + Math.round(secondDeltaZ * progress));
            if (!point.equals(points.getLast())) points.add(point);
        }
        return new Route(List.copyOf(points), porchSteps);
    }

    /** Matches the exterior axis implied by the same road vector used by the urban interior. */
    private static Vector outward(Entry entry) {
        int deltaX = entry.roadX - entry.x;
        int deltaZ = entry.roadZ - entry.z;
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            return new Vector(deltaX >= 0 ? 1 : -1, 0);
        }
        return new Vector(0, deltaZ >= 0 ? 1 : -1);
    }

    private static int designedFloor(int x, int z) {
        return (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
    }

    private static boolean walkable(ServerLevel level, int x, int feetY, int z) {
        if (feetY <= level.getMinY() || feetY + 1 >= level.getMaxY()) return false;
        if (!level.hasChunk(x >> 4, z >> 4)) return false;
        BlockPos feet = new BlockPos(x, feetY, z);
        BlockPos head = feet.above();
        BlockPos floor = feet.below();
        return bodyPassable(level, feet)
                && bodyPassable(level, head)
                && !level.getBlockState(floor).isAir()
                && level.getFluidState(feet).isEmpty()
                && level.getFluidState(head).isEmpty()
                && level.getFluidState(floor).isEmpty();
    }

    private static boolean bodyPassable(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.isAir()
                || state.getBlock() instanceof DoorBlock
                || state.getCollisionShape(level, pos).isEmpty();
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        if (y < level.getMinY() || y >= level.getMaxY()) return;
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).is(block)) return;
        level.setBlock(pos, block.defaultBlockState(), UPDATE_FLAGS);
    }

    private static boolean diagnosticMode() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_ENTRY_TRAVERSAL"));
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private record Entry(
            String kind,
            String role,
            int x,
            int z,
            int roadX,
            int roadZ,
            boolean urbanOrResidential) {
    }

    private record Point(int x, int z) {
    }

    private record Vector(int x, int z) {
    }

    private record Route(List<Point> points, int porchSteps) {
    }

    private record Approach(
            List<Point> points,
            int steps,
            int porchSteps,
            int doorFloor,
            int endFloor) {
    }
}
