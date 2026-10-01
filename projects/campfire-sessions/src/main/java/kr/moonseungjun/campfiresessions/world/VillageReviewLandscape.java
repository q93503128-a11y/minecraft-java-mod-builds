package kr.moonseungjun.campfiresessions.world;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.moonseungjun.campfiresessions.CampfireSessions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Review-only authored terrain pass for the second Campfire village slice.
 *
 * <p>The first client review rejected isolated per-building pads. This pass
 * shapes three coherent village levels first, then lays a connected soft path
 * network around the externally authored structures. The outer mountain and
 * coastline remain outside these bounded work zones.</p>
 */
public final class VillageReviewLandscape {
    private static final int TERRACE_FEATHER = 3;
    private static final int MAX_TERRACE_DELTA = 12;
    private static final int MAX_PATH_DELTA = 4;

    private static final List<TerraceSpec> TERRACES = List.of(
            terrace("waterfront", -338, -316, -61, -40, 64),
            terrace("civic", -323, -270, -63, -21, 72),
            terrace("upper", -317, -282, -21, 18, 75)
    );

    private static final List<PathSpec> PATHS = List.of(
            path("harbor_arrival",
                    node(-326, 64, -49),
                    node(-321, 67, -46),
                    node(-318, 72, -42),
                    node(-307, 72, -36)),
            path("civic_spine",
                    node(-307, 72, -36),
                    node(-299, 72, -35),
                    node(-289, 72, -35),
                    node(-278, 72, -35)),
            path("south_shops",
                    node(-307, 72, -44),
                    node(-296, 72, -47),
                    node(-285, 72, -47)),
            path("resident_services",
                    node(-307, 72, -36),
                    node(-306, 72, -31)),
            path("upper_steps",
                    node(-299, 72, -27),
                    node(-299, 73, -23),
                    node(-299, 74, -20),
                    node(-299, 75, -17)),
            path("upper_lane",
                    node(-311, 75, -5),
                    node(-302, 75, -5),
                    node(-293, 75, -5),
                    node(-287, 75, -5)),
            path("upper_south",
                    node(-308, 75, 2),
                    node(-300, 75, 8),
                    node(-290, 75, 8))
    );

    private VillageReviewLandscape() {}

    public static void preflightTerraces(ServerLevel level) {
        for (TerraceSpec terrace : TERRACES) {
            int worstDelta = 0;
            int worstX = terrace.minX();
            int worstZ = terrace.minZ();
            int worstSurface = terrace.surfaceY();
            for (int x = terrace.minX(); x <= terrace.maxX(); x++) {
                for (int z = terrace.minZ(); z <= terrace.maxZ(); z++) {
                    int surface = surfaceY(level, x, z);
                    int distance = edgeDistance(terrace, x, z);
                    int target = blendedTarget(surface, terrace.surfaceY(), distance);
                    int delta = Math.abs(surface - target);
                    if (delta > worstDelta) {
                        worstDelta = delta;
                        worstX = x;
                        worstZ = z;
                        worstSurface = surface;
                    }
                }
            }
            if (worstDelta > MAX_TERRACE_DELTA) {
                throw new IllegalStateException(
                        "Campfire village terrace " + terrace.name() + " exceeds grading limit"
                                + " at " + worstX + "," + worstZ
                                + ": surface=" + worstSurface
                                + " target=" + terrace.surfaceY()
                                + " blendedDelta=" + worstDelta
                );
            }
            CampfireSessions.LOGGER.info(
                    "Campfire village terrace preflight {}: targetY={} worstDelta={}",
                    terrace.name(),
                    terrace.surfaceY(),
                    worstDelta
            );
        }
    }

    public static void placeTerraces(ServerLevel level) {
        int cells = 0;
        for (TerraceSpec terrace : TERRACES) {
            for (int x = terrace.minX(); x <= terrace.maxX(); x++) {
                for (int z = terrace.minZ(); z <= terrace.maxZ(); z++) {
                    int surface = surfaceY(level, x, z);
                    int distance = edgeDistance(terrace, x, z);
                    int target = blendedTarget(surface, terrace.surfaceY(), distance);
                    gradeSurface(level, x, z, target, Blocks.GRASS_BLOCK.defaultBlockState(), true);
                    cells++;
                }
            }
        }
        CampfireSessions.LOGGER.info(
                "Campfire village review terraces applied: {} cells across {} levels",
                cells,
                TERRACES.size()
        );
    }

    public static void preflightPaths(ServerLevel level, List<BoundingBox> protectedBounds) {
        for (Map.Entry<BlockPos, Integer> entry : collectRoadTargets().entrySet()) {
            BlockPos cell = entry.getKey();
            if (isProtected(cell.getX(), cell.getZ(), protectedBounds)) {
                continue;
            }
            int surface = surfaceY(level, cell.getX(), cell.getZ());
            if (Math.abs(surface - entry.getValue()) > MAX_PATH_DELTA) {
                throw new IllegalStateException(
                        "Campfire village path grading limit exceeded at "
                                + cell.getX() + "," + cell.getZ()
                                + ": surface=" + surface + " target=" + entry.getValue()
                );
            }
        }
    }

    public static void placePaths(ServerLevel level, List<BoundingBox> protectedBounds) {
        int cells = 0;
        for (Map.Entry<BlockPos, Integer> entry : collectRoadTargets().entrySet()) {
            BlockPos cell = entry.getKey();
            if (isProtected(cell.getX(), cell.getZ(), protectedBounds)) {
                continue;
            }
            gradeSurface(
                    level,
                    cell.getX(),
                    cell.getZ(),
                    entry.getValue(),
                    roadMaterial(cell.getX(), cell.getZ()),
                    false
            );
            cells++;
        }
        CampfireSessions.LOGGER.info("Campfire village review paths applied: {} cells", cells);
    }

    private static int edgeDistance(TerraceSpec terrace, int x, int z) {
        return Math.min(
                Math.min(x - terrace.minX(), terrace.maxX() - x),
                Math.min(z - terrace.minZ(), terrace.maxZ() - z)
        );
    }

    private static int blendedTarget(int current, int target, int edgeDistance) {
        if (edgeDistance >= TERRACE_FEATHER) {
            return target;
        }
        double t = (edgeDistance + 1.0) / (TERRACE_FEATHER + 1.0);
        return (int) Math.round(current + (target - current) * t);
    }

    private static void gradeSurface(
            ServerLevel level,
            int x,
            int z,
            int targetY,
            BlockState top,
            boolean clearVillageVegetation
    ) {
        level.getChunkAt(new BlockPos(x, targetY, z));
        int currentSurface = surfaceY(level, x, z);

        if (currentSurface < targetY) {
            for (int y = currentSurface + 1; y < targetY; y++) {
                level.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        int clearTop = clearVillageVegetation
                ? Math.max(targetY + 24, currentSurface + 3)
                : Math.max(targetY + 3, currentSurface + 2);
        for (int y = targetY + 1; y <= clearTop; y++) {
            level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }

        BlockPos support = new BlockPos(x, targetY - 1, z);
        if (level.getBlockState(support).isAir() || !level.getFluidState(support).isEmpty()) {
            level.setBlock(support, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.setBlock(new BlockPos(x, targetY, z), top, Block.UPDATE_ALL);
    }

    private static BlockState roadMaterial(int x, int z) {
        int pattern = Math.floorMod(x * 31 + z * 17, 13);
        if (pattern == 0) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (pattern <= 2) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        return Blocks.DIRT_PATH.defaultBlockState();
    }

    private static Map<BlockPos, Integer> collectRoadTargets() {
        Map<BlockPos, Integer> cells = new LinkedHashMap<>();
        for (PathSpec path : PATHS) {
            for (int i = 0; i + 1 < path.nodes().size(); i++) {
                rasterSegment(cells, path.name(), path.nodes().get(i), path.nodes().get(i + 1));
            }
        }
        return cells;
    }

    private static void rasterSegment(
            Map<BlockPos, Integer> cells,
            String pathName,
            PathNode start,
            PathNode end
    ) {
        int x = start.x();
        int z = start.z();
        int dx = Math.abs(end.x() - start.x());
        int dz = Math.abs(end.z() - start.z());
        int sx = start.x() < end.x() ? 1 : -1;
        int sz = start.z() < end.z() ? 1 : -1;
        int error = dx - dz;
        int steps = Math.max(dx, dz);
        int step = 0;

        while (true) {
            double t = steps == 0 ? 0.0 : (double) step / (double) steps;
            int y = (int) Math.round(start.y() + (end.y() - start.y()) * t);
            stampRoad(cells, pathName, x, z, y);
            if (x == end.x() && z == end.z()) {
                break;
            }
            int doubled = error * 2;
            if (doubled > -dz) {
                error -= dz;
                x += sx;
            }
            if (doubled < dx) {
                error += dx;
                z += sz;
            }
            step++;
        }
    }

    private static void stampRoad(Map<BlockPos, Integer> cells, String pathName, int x, int z, int y) {
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                putRoadCell(cells, pathName, x + ox, z + oz, y);
            }
        }
    }

    private static void putRoadCell(
            Map<BlockPos, Integer> cells,
            String pathName,
            int x,
            int z,
            int y
    ) {
        BlockPos key = new BlockPos(x, 0, z);
        Integer previous = cells.get(key);
        if (previous == null) {
            cells.put(key, y);
            return;
        }
        if (Math.abs(previous - y) > 1) {
            throw new IllegalStateException(
                    "Campfire village path height conflict at " + x + "," + z
                            + " while adding " + pathName + ": " + previous + " vs " + y
            );
        }
        cells.put(key, (previous + y + 1) / 2);
    }

    private static boolean isProtected(int x, int z, List<BoundingBox> bounds) {
        for (BoundingBox box : bounds) {
            if (x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ()) {
                return true;
            }
        }
        return false;
    }

    private static int surfaceY(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        int floor = Math.max(level.getMinY(), y - 48);
        while (y > floor) {
            var state = level.getBlockState(new BlockPos(x, y, z));
            if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
                return y;
            }
            y--;
        }
        return y;
    }

    private static TerraceSpec terrace(String name, int minX, int maxX, int minZ, int maxZ, int y) {
        return new TerraceSpec(name, minX, maxX, minZ, maxZ, y);
    }

    private static PathNode node(int x, int y, int z) {
        return new PathNode(x, y, z);
    }

    private static PathSpec path(String name, PathNode... nodes) {
        return new PathSpec(name, List.of(nodes));
    }

    private record TerraceSpec(String name, int minX, int maxX, int minZ, int maxZ, int surfaceY) {}
    private record PathNode(int x, int y, int z) {}
    private record PathSpec(String name, List<PathNode> nodes) {}
}
