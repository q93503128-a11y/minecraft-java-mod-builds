package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * Reconciles the complete street-to-door route after imported geometry and terrain are both real.
 *
 * <p>The static builders deliberately preserve authored facade blocks and only know design-time
 * terrain. The threshold manager then proves the real door and its first two metres of porch. This
 * pass owns the remaining generated access corridor: it continues from that proved porch to the
 * actual street with a maximum one-block grade, without flattening the imported porch or replacing
 * the road itself. The work is loaded-chunk-only and idempotent.</p>
 */
public final class ErdenEntryPathReconciler {
    private static final int EXPECTED_ENTRANCES = 273;
    private static final int PROCESS_INTERVAL = 5;
    private static final int PROCESS_BUDGET = 4;
    private static final int PORCH_STEPS = 2;
    private static final int UPDATE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private static MinecraftServer activeServer;
    private static List<Entry> cachedEntries;
    private static final Set<Long> COMPLETE = new HashSet<>();
    private static final Set<Long> REPORTED_STALLS = new HashSet<>();
    private static boolean completionLogged;

    private ErdenEntryPathReconciler() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        if (level.getGameTime() % PROCESS_INTERVAL != 0L) return;

        int processed = 0;
        for (Entry entry : entries()) {
            if (processed >= PROCESS_BUDGET) break;
            long key = key(entry.x, entry.z);
            if (COMPLETE.contains(key)) continue;
            if (!ErdenEntranceThresholdManager.isComplete(level, entry.x, entry.z)) continue;

            Result result = reconcile(level, entry);
            if (result == Result.WAITING) continue;
            if (result == Result.STALLED) {
                reportStall(entry);
                continue;
            }
            COMPLETE.add(key);
            REPORTED_STALLS.remove(key);
            processed++;
        }

        if (!completionLogged && COMPLETE.size() == EXPECTED_ENTRANCES) {
            completionLogged = true;
            LivingKingdoms.LOGGER.info(
                    "Completed Erden full entry path reconciliation entrances={} porch_preserved=true full_grade=true road_preserved=true loaded_only=true",
                    COMPLETE.size());
        }
    }

    static boolean isComplete(int x, int z) {
        return COMPLETE.contains(key(x, z));
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        COMPLETE.clear();
        REPORTED_STALLS.clear();
        completionLogged = false;
    }

    private static List<Entry> entries() {
        List<Entry> result = cachedEntries;
        if (result != null) return result;
        synchronized (ErdenEntryPathReconciler.class) {
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
                            "Erden entry path count mismatch " + built.size()
                                    + " != " + EXPECTED_ENTRANCES);
                }
                result = List.copyOf(built);
                cachedEntries = result;
                LivingKingdoms.LOGGER.info(
                        "Prepared Erden full entry path reconciler entrances={} full_grade=true real_doors=true",
                        result.size());
            }
            return result;
        }
    }

    private static Result reconcile(ServerLevel level, Entry entry) {
        int doorY = findLowestDoorY(level, entry.x, entry.z);
        if (doorY == Integer.MIN_VALUE) return Result.WAITING;
        if (!walkable(level, entry.x, doorY, entry.z)) return Result.STALLED;

        Vector outward = outward(level, entry, doorY);
        List<Point> route = route(entry, outward);
        if (route.isEmpty()) return Result.STALLED;
        if (!routeReady(level, route)) return Result.WAITING;

        int previousFeetY = doorY;
        int porchSteps = Math.min(PORCH_STEPS, route.size());
        for (int step = 0; step < porchSteps; step++) {
            Point point = route.get(step);
            int feetY = findWalkableFeetY(level, point.x, point.z, previousFeetY);
            if (feetY == Integer.MIN_VALUE || Math.abs(feetY - previousFeetY) > 1) {
                return Result.STALLED;
            }
            previousFeetY = feetY;
        }
        int porchFloor = previousFeetY - 1;
        int roadFloor = roadFloor(level, entry.roadX, entry.roadZ);
        int generatedSteps = route.size() - porchSteps;
        if (generatedSteps <= 0) {
            return verify(level, entry, doorY, route) ? Result.COMPLETE : Result.STALLED;
        }
        if (Math.abs(roadFloor - porchFloor) > generatedSteps) return Result.STALLED;

        Block material = entry.urbanOrResidential ? Blocks.PACKED_MUD : Blocks.STONE_BRICKS;
        Point previous = porchSteps == 0
                ? new Point(entry.x, entry.z)
                : route.get(porchSteps - 1);
        for (int index = porchSteps; index < route.size(); index++) {
            Point center = route.get(index);
            boolean roadEndpoint = index == route.size() - 1;
            int generatedIndex = index - porchSteps + 1;
            int targetFloor = gradedFloor(
                    porchFloor, roadFloor, generatedIndex, generatedSteps);
            if (!roadEndpoint) {
                int moveX = center.x - previous.x;
                int moveZ = center.z - previous.z;
                boolean eastWest = Math.abs(moveX) >= Math.abs(moveZ);
                for (int width = -1; width <= 1; width++) {
                    int x = eastWest ? center.x : center.x + width;
                    int z = eastWest ? center.z + width : center.z;
                    if (!chunkReady(level, x, z)) return Result.WAITING;
                    normalizeColumn(level, x, z, targetFloor, material);
                }
            }
            previous = center;
        }

        return verify(level, entry, doorY, route) ? Result.COMPLETE : Result.STALLED;
    }

    private static void normalizeColumn(
            ServerLevel level, int x, int z, int targetFloor, Block material) {
        int naturalFloor = designedFloor(x, z);
        if (targetFloor >= naturalFloor) {
            for (int y = naturalFloor; y <= targetFloor; y++) {
                set(level, x, y, z, material);
            }
        } else {
            set(level, x, targetFloor, z, material);
        }
        int clearTop = Math.max(targetFloor + 3, naturalFloor + 3);
        for (int y = targetFloor + 1; y <= clearTop; y++) {
            set(level, x, y, z, Blocks.AIR);
        }
    }

    private static boolean verify(
            ServerLevel level, Entry entry, int doorY, List<Point> route) {
        int previousFeetY = doorY;
        for (Point point : route) {
            int feetY = findWalkableFeetY(level, point.x, point.z, previousFeetY);
            if (feetY == Integer.MIN_VALUE || Math.abs(feetY - previousFeetY) > 1) return false;
            previousFeetY = feetY;
        }
        return pointNear(entry.roadX, entry.roadZ, route.getLast())
                && walkable(level, entry.x, doorY, entry.z);
    }

    private static List<Point> route(Entry entry, Vector outward) {
        int deltaX = entry.roadX - entry.x;
        int deltaZ = entry.roadZ - entry.z;
        int directSteps = Math.max(Math.abs(deltaX), Math.abs(deltaZ));
        if (directSteps <= 0) return List.of();

        int porchSteps = Math.min(PORCH_STEPS, directSteps);
        List<Point> points = new ArrayList<>(directSteps + PORCH_STEPS + 1);
        for (int step = 1; step <= porchSteps; step++) {
            points.add(new Point(
                    entry.x + outward.x * step,
                    entry.z + outward.z * step));
        }
        Point porch = points.getLast();
        int remainingX = entry.roadX - porch.x;
        int remainingZ = entry.roadZ - porch.z;
        int remainingSteps = Math.max(Math.abs(remainingX), Math.abs(remainingZ));
        for (int step = 1; step <= remainingSteps; step++) {
            float progress = step / (float) remainingSteps;
            Point point = new Point(
                    porch.x + Math.round(remainingX * progress),
                    porch.z + Math.round(remainingZ * progress));
            if (!point.equals(points.getLast())) points.add(point);
        }
        return List.copyOf(points);
    }

    /** Mirrors the threshold manager: walkable authored geometry wins before road proximity. */
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

    private static int distanceToRoad(Entry entry, Vector direction) {
        int x = entry.x + direction.x;
        int z = entry.z + direction.z;
        return Math.abs(entry.roadX - x) + Math.abs(entry.roadZ - z);
    }

    private static Vector roadDominantOutward(Entry entry) {
        int deltaX = entry.roadX - entry.x;
        int deltaZ = entry.roadZ - entry.z;
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            return new Vector(deltaX >= 0 ? 1 : -1, 0);
        }
        return new Vector(0, deltaZ >= 0 ? 1 : -1);
    }

    private static boolean routeReady(ServerLevel level, List<Point> route) {
        for (Point point : route) {
            if (!chunkReady(level, point.x, point.z)) return false;
        }
        return true;
    }

    private static boolean chunkReady(ServerLevel level, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        return level.hasChunk(chunkX, chunkZ)
                && ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ);
    }

    private static int roadFloor(ServerLevel level, int x, int z) {
        int surface = RealmSitePlanner.surfaceY(level, x, z);
        int resolved = findWalkableFeetY(level, x, z, surface + 1);
        return resolved == Integer.MIN_VALUE ? designedFloor(x, z) : resolved - 1;
    }

    private static int gradedFloor(int from, int to, int step, int totalSteps) {
        if (totalSteps <= 0) return to;
        float progress = step / (float) totalSteps;
        return Math.round(from + (to - from) * progress);
    }

    private static int findLowestDoorY(ServerLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) return Integer.MIN_VALUE;
        int designed = designedFloor(x, z);
        int minimum = Math.max(level.getMinY(), designed - 18);
        int maximum = Math.min(level.getMaxY() - 1, designed + 82);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int lowest = Integer.MAX_VALUE;
        for (int y = minimum; y <= maximum; y++) {
            cursor.set(x, y, z);
            if (level.getBlockState(cursor).getBlock() instanceof DoorBlock) lowest = Math.min(lowest, y);
        }
        return lowest == Integer.MAX_VALUE ? Integer.MIN_VALUE : lowest;
    }

    private static int findWalkableFeetY(
            ServerLevel level, int x, int z, int preferredFeetY) {
        int[] offsets = {0, 1, -1};
        for (int offset : offsets) {
            int feetY = preferredFeetY + offset;
            if (walkable(level, x, feetY, z)) return feetY;
        }
        return Integer.MIN_VALUE;
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

    private static int designedFloor(int x, int z) {
        return (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        if (y < level.getMinY() || y >= level.getMaxY()) return;
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).is(block)) return;
        level.setBlock(pos, block.defaultBlockState(), UPDATE_FLAGS);
    }

    private static boolean pointNear(int x, int z, Point point) {
        return Math.abs(x - point.x) <= 1 && Math.abs(z - point.z) <= 1;
    }

    private static void reportStall(Entry entry) {
        if (!"1".equals(System.getenv("LIVING_KINGDOMS_CI_ENTRY_TRAVERSAL"))) return;
        long key = key(entry.x, entry.z);
        if (!REPORTED_STALLS.add(key)) return;
        LivingKingdoms.LOGGER.warn(
                "LK_ERDEN_ENTRY_PATH_STALL kind={} role={} entrance={},{} road={},{}",
                entry.kind, entry.role, entry.x, entry.z, entry.roadX, entry.roadZ);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private enum Result {
        WAITING,
        STALLED,
        COMPLETE
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
}
