package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reconciles each imported door's real vertical position with its terrain-following access path.
 *
 * <p>The first two metres are treated as authored porch geometry. Slabs, stairs and full-block
 * doorstep rises are preserved and followed at their actual walkable height. Only after that porch
 * has been left does the manager grade and clear the terrain route toward the street. This keeps
 * imported facade details intact while still guaranteeing a climbable road connection.</p>
 */
public final class ErdenEntranceThresholdManager {
    public static final int THRESHOLD_REVISION = 6;

    private static final int EXPECTED_ENTRANCES = 273;
    private static final int PROCESS_INTERVAL = 5;
    private static final int PROCESS_BUDGET = 6;
    private static final int PORCH_STEPS = 2;
    private static final int BASE_APPROACH_STEPS = 8;
    private static final int MAX_APPROACH_STEPS = 16;
    private static final int UPDATE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private static MinecraftServer activeServer;
    private static List<Entry> cachedEntries;
    private static boolean completionLogged;
    private static final Set<Long> REPORTED_PREFLIGHT_STALLS = new HashSet<>();

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
            Approach approach = approach(level, entry, doorY);
            PreflightResult preflight = preflight(level, entry, doorY, approach);
            if (!preflight.ok) {
                reportPreflightStall(level, entry, doorY, approach, preflight);
                continue;
            }
            if (!normalize(level, entry, doorY, approach)) continue;

            data.markComplete(key, THRESHOLD_REVISION);
            processed++;
            if (diagnosticMode()) {
                LivingKingdoms.LOGGER.debug(
                        "Normalized Erden entrance threshold kind={} role={} entrance={},{} road={},{} door_y={} outward={},{} approach_steps={} porch_steps={} porch_end_floor={} end_floor={}",
                        entry.kind, entry.role, entry.x, entry.z, entry.roadX, entry.roadZ,
                        doorY, approach.outward.x, approach.outward.z,
                        approach.steps, approach.porchSteps,
                        approach.porchEndFloor, approach.endFloor);
            }
        }

        int complete = data.completedCount(THRESHOLD_REVISION);
        if (!completionLogged && complete == entries.size()) {
            completionLogged = true;
            LivingKingdoms.LOGGER.info(
                    "Completed Erden entrance threshold normalization entrances={} graded_landings=true authored_porches_preserved=true actual_door_normals=true real_door_heights=true loaded_only=true revision={}",
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
        REPORTED_PREFLIGHT_STALLS.clear();
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
                        "Prepared Erden entrance threshold reconciler entrances={} terrain_matched=true authored_porches_preserved=true actual_door_normals=true",
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

    private static Approach approach(ServerLevel level, Entry entry, int doorY) {
        Vector outward = outward(level, entry, doorY);
        Route route = route(entry, outward);
        int totalSteps = route.points.size();
        int porchEndFloor = resolvePorchEndFloor(level, route, doorY);
        int steps = Math.max(1, Math.min(totalSteps, BASE_APPROACH_STEPS));
        int endFloor = porchEndFloor;

        for (int pass = 0; pass < 3; pass++) {
            Point end = route.points.get(steps - 1);
            endFloor = steps <= route.porchSteps
                    ? porchEndFloor
                    : designedFloor(end.x, end.z);
            int required = Math.min(
                    MAX_APPROACH_STEPS,
                    route.porchSteps + Math.abs(endFloor - porchEndFloor) + 2);
            int nextSteps = Math.min(totalSteps, Math.max(steps, required));
            if (nextSteps == steps) break;
            steps = nextSteps;
        }

        Point end = route.points.get(steps - 1);
        endFloor = steps <= route.porchSteps
                ? porchEndFloor
                : designedFloor(end.x, end.z);
        return new Approach(
                route.points,
                steps,
                Math.min(route.porchSteps, steps),
                doorY - 1,
                porchEndFloor,
                endFloor,
                outward);
    }

    /**
     * Proves the authored doorway and porch without changing it, then verifies that the remaining
     * generated grade can change by no more than one block per horizontal step.
     */
    private static PreflightResult preflight(
            ServerLevel level,
            Entry entry,
            int doorY,
            Approach approach) {
        if (!walkable(level, entry.x, doorY, entry.z)) {
            return new PreflightResult(false, "door_not_walkable", 0,
                    new Point(entry.x, entry.z), doorY - 1);
        }

        int previousFeetY = doorY;
        for (int step = 1; step <= approach.porchSteps; step++) {
            Point center = approach.points.get(step - 1);
            if (!routeChunkReady(level, center)) {
                return new PreflightResult(false, "route_chunk_not_ready", step, center,
                        previousFeetY - 1);
            }
            int feetY = findWalkableFeetY(level, center.x, center.z, previousFeetY);
            if (feetY == Integer.MIN_VALUE) {
                return new PreflightResult(false, "porch_not_walkable", step, center,
                        previousFeetY - 1);
            }
            if (Math.abs(feetY - previousFeetY) > 1) {
                return new PreflightResult(false, "porch_step_too_high", step, center,
                        feetY - 1);
            }
            previousFeetY = feetY;
        }

        int previousFloor = previousFeetY - 1;
        if (previousFloor != approach.porchEndFloor) {
            return new PreflightResult(false, "porch_floor_changed", approach.porchSteps,
                    approach.points.get(Math.max(0, approach.porchSteps - 1)), previousFloor);
        }

        for (int step = approach.porchSteps + 1; step <= approach.steps; step++) {
            Point center = approach.points.get(step - 1);
            if (!routeChunkReady(level, center)) {
                return new PreflightResult(false, "route_chunk_not_ready", step, center,
                        previousFloor);
            }
            int targetFloor = gradedFloor(approach, step);
            if (Math.abs(targetFloor - previousFloor) > 1) {
                return new PreflightResult(false, "grade_too_steep", step, center, targetFloor);
            }
            previousFloor = targetFloor;
        }

        if (Math.abs(previousFloor - approach.endFloor) > 1) {
            return new PreflightResult(false, "grade_end_mismatch", approach.steps,
                    approach.points.get(approach.steps - 1), previousFloor);
        }
        return new PreflightResult(true, "ok", 0,
                new Point(entry.x, entry.z), approach.doorFloor);
    }

    private static boolean normalize(
            ServerLevel level,
            Entry entry,
            int doorY,
            Approach approach) {
        Block material = entry.urbanOrResidential ? Blocks.PACKED_MUD : Blocks.STONE_BRICKS;
        int previousFloor = approach.doorFloor;
        Point previousPoint = new Point(entry.x, entry.z);

        int previousFeetY = doorY;
        for (int step = 1; step <= approach.porchSteps; step++) {
            Point center = approach.points.get(step - 1);
            int feetY = findWalkableFeetY(level, center.x, center.z, previousFeetY);
            if (feetY == Integer.MIN_VALUE || Math.abs(feetY - previousFeetY) > 1) return false;
            previousFeetY = feetY;
            previousFloor = feetY - 1;
            previousPoint = center;
        }
        if (previousFloor != approach.porchEndFloor) return false;

        for (int step = approach.porchSteps + 1; step <= approach.steps; step++) {
            Point center = approach.points.get(step - 1);
            int targetFloor = gradedFloor(approach, step);
            if (Math.abs(targetFloor - previousFloor) > 1) return false;

            int movementX = center.x - previousPoint.x;
            int movementZ = center.z - previousPoint.z;
            boolean eastWest = Math.abs(movementX) >= Math.abs(movementZ);
            for (int width = -1; width <= 1; width++) {
                int x = eastWest ? center.x : center.x + width;
                int z = eastWest ? center.z + width : center.z;
                if (!level.hasChunk(x >> 4, z >> 4)
                        || !ErdenCapitalStreamingBuilder.isChunkBuilt(level, x >> 4, z >> 4)) {
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
                int clearTop = Math.max(targetFloor + 3, naturalFloor + 2);
                for (int y = targetFloor + 1; y <= clearTop; y++) {
                    set(level, x, y, z, Blocks.AIR);
                }
            }
            if (!walkable(level, center.x, targetFloor + 1, center.z)) return false;
            previousFloor = targetFloor;
            previousPoint = center;
        }

        if (Math.abs(previousFloor - approach.endFloor) > 1) return false;
        return walkable(level, entry.x, doorY, entry.z);
    }

    private static int gradedFloor(Approach approach, int step) {
        if (step <= approach.porchSteps || approach.steps <= approach.porchSteps) {
            return approach.porchEndFloor;
        }
        int gradeSteps = approach.steps - approach.porchSteps;
        float progress = (step - approach.porchSteps) / (float) gradeSteps;
        return Math.round(
                approach.porchEndFloor
                        + (approach.endFloor - approach.porchEndFloor) * progress);
    }

    private static int resolvePorchEndFloor(ServerLevel level, Route route, int doorY) {
        int previousFeetY = doorY;
        for (int step = 1; step <= route.porchSteps; step++) {
            Point center = route.points.get(step - 1);
            if (!routeChunkReady(level, center)) return doorY - 1;
            int feetY = findWalkableFeetY(level, center.x, center.z, previousFeetY);
            if (feetY == Integer.MIN_VALUE || Math.abs(feetY - previousFeetY) > 1) return doorY - 1;
            previousFeetY = feetY;
        }
        return previousFeetY - 1;
    }

    private static int findWalkableFeetY(ServerLevel level, int x, int z, int preferredFeetY) {
        int[] offsets = {0, 1, -1};
        for (int offset : offsets) {
            int feetY = preferredFeetY + offset;
            if (walkable(level, x, feetY, z)) return feetY;
        }
        return Integer.MIN_VALUE;
    }

    private static Route route(Entry entry, Vector outward) {
        int deltaX = entry.roadX - entry.x;
        int deltaZ = entry.roadZ - entry.z;
        int directSteps = Math.max(Math.abs(deltaX), Math.abs(deltaZ));
        if (directSteps <= 0) return new Route(List.of(new Point(entry.x, entry.z)), 0);

        int porchSteps = Math.min(PORCH_STEPS, directSteps);
        List<Point> points = new ArrayList<>(directSteps + PORCH_STEPS);
        for (int step = 1; step <= porchSteps; step++) {
            points.add(new Point(entry.x + outward.x * step, entry.z + outward.z * step));
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

    /**
     * Uses the actual door normal. A geometrically walkable authored porch is stronger evidence of
     * the exterior side than a road that happens to be two blocks closer behind a wall; road
     * distance is therefore only a tie-breaker after probing both door-normal directions.
     */
    private static Vector outward(ServerLevel level, Entry entry, int doorY) {
        BlockPos doorPos = new BlockPos(entry.x, doorY, entry.z);
        var state = level.getBlockState(doorPos);
        if (state.getBlock() instanceof DoorBlock && state.hasProperty(DoorBlock.FACING)) {
            Direction facing = state.getValue(DoorBlock.FACING);
            Vector first = new Vector(facing.getStepX(), facing.getStepZ());
            Vector second = new Vector(-first.x, -first.z);
            int firstClear = clearRun(level, entry, doorY, first);
            int secondClear = clearRun(level, entry, doorY, second);
            if (firstClear != secondClear) return firstClear > secondClear ? first : second;
            int firstDistance = distanceToRoad(entry, first);
            int secondDistance = distanceToRoad(entry, second);
            if (firstDistance != secondDistance) return firstDistance < secondDistance ? first : second;
        }
        return roadDominantOutward(entry);
    }

    private static int distanceToRoad(Entry entry, Vector direction) {
        int x = entry.x + direction.x;
        int z = entry.z + direction.z;
        return Math.abs(entry.roadX - x) + Math.abs(entry.roadZ - z);
    }

    private static int clearRun(ServerLevel level, Entry entry, int doorY, Vector direction) {
        int clear = 0;
        int previousFeetY = doorY;
        for (int depth = 1; depth <= PORCH_STEPS; depth++) {
            int x = entry.x + direction.x * depth;
            int z = entry.z + direction.z * depth;
            if (!level.hasChunk(x >> 4, z >> 4)) break;
            int feetY = findWalkableFeetY(level, x, z, previousFeetY);
            if (feetY == Integer.MIN_VALUE || Math.abs(feetY - previousFeetY) > 1) break;
            previousFeetY = feetY;
            clear++;
        }
        return clear;
    }

    private static Vector roadDominantOutward(Entry entry) {
        int deltaX = entry.roadX - entry.x;
        int deltaZ = entry.roadZ - entry.z;
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) return new Vector(deltaX >= 0 ? 1 : -1, 0);
        return new Vector(0, deltaZ >= 0 ? 1 : -1);
    }

    private static boolean routeChunkReady(ServerLevel level, Point point) {
        return level.hasChunk(point.x >> 4, point.z >> 4)
                && ErdenCapitalStreamingBuilder.isChunkBuilt(level, point.x >> 4, point.z >> 4);
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

    /** Generated threshold grading may cut terrain and clutter, but never another authored door. */
    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        if (y < level.getMinY() || y >= level.getMaxY()) return;
        BlockPos pos = new BlockPos(x, y, z);
        var existing = level.getBlockState(pos);
        if (existing.getBlock() instanceof DoorBlock) return;
        if (existing.is(block)) return;
        level.setBlock(pos, block.defaultBlockState(), UPDATE_FLAGS);
    }

    private static void reportPreflightStall(
            ServerLevel level,
            Entry entry,
            int doorY,
            Approach approach,
            PreflightResult result) {
        if (!diagnosticMode() || "route_chunk_not_ready".equals(result.reason)) return;
        long entryKey = key(entry.x, entry.z);
        if (!REPORTED_PREFLIGHT_STALLS.add(entryKey)) return;

        BlockPos pointFeet = new BlockPos(result.point.x, result.targetFloor + 1, result.point.z);
        BlockPos doorPos = new BlockPos(entry.x, doorY, entry.z);
        var doorState = level.getBlockState(doorPos);
        String facing = doorState.getBlock() instanceof DoorBlock && doorState.hasProperty(DoorBlock.FACING)
                ? doorState.getValue(DoorBlock.FACING).getName()
                : "unknown";
        LivingKingdoms.LOGGER.warn(
                "LK_ERDEN_THRESHOLD_PREFLIGHT_STALL kind={} role={} entrance={},{} road={},{} door_y={} door_floor={} porch_end_floor={} designed_floor={} door_facing={} selected_outward={},{} reason={} step={} point={},{} target_floor={} feet_block={} head_block={} floor_block={} east={} west={} south={} north={}",
                entry.kind, entry.role, entry.x, entry.z, entry.roadX, entry.roadZ,
                doorY, approach.doorFloor, approach.porchEndFloor,
                designedFloor(entry.x, entry.z), facing,
                approach.outward.x, approach.outward.z, result.reason, result.step,
                result.point.x, result.point.z, result.targetFloor,
                blockId(level, pointFeet), blockId(level, pointFeet.above()),
                blockId(level, pointFeet.below()),
                probeDirection(level, entry, doorY, new Vector(1, 0)),
                probeDirection(level, entry, doorY, new Vector(-1, 0)),
                probeDirection(level, entry, doorY, new Vector(0, 1)),
                probeDirection(level, entry, doorY, new Vector(0, -1)));
    }

    private static String probeDirection(
            ServerLevel level,
            Entry entry,
            int doorY,
            Vector direction) {
        StringBuilder result = new StringBuilder();
        int previousFeetY = doorY;
        for (int depth = 1; depth <= PORCH_STEPS; depth++) {
            if (depth > 1) result.append('|');
            int x = entry.x + direction.x * depth;
            int z = entry.z + direction.z * depth;
            if (!level.hasChunk(x >> 4, z >> 4)) {
                result.append(depth).append(":unloaded");
                continue;
            }
            int resolvedFeet = findWalkableFeetY(level, x, z, previousFeetY);
            BlockPos rawFeet = new BlockPos(x, previousFeetY, z);
            result.append(depth).append(':')
                    .append(blockId(level, rawFeet)).append('/')
                    .append(blockId(level, rawFeet.above())).append('/')
                    .append(resolvedFeet == Integer.MIN_VALUE ? "blocked" : "feet=" + resolvedFeet);
            if (resolvedFeet != Integer.MIN_VALUE) previousFeetY = resolvedFeet;
        }
        result.append("@road=").append(distanceToRoad(entry, direction));
        return result.toString();
    }

    private static String blockId(ServerLevel level, BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
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
            int porchEndFloor,
            int endFloor,
            Vector outward) {
    }

    private record PreflightResult(
            boolean ok,
            String reason,
            int step,
            Point point,
            int targetFloor) {
    }
}
