package kr.moonseungjun.campfiresessions.world;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.moonseungjun.campfiresessions.CampfireSessions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Authored first-pass plaza and walk network for the canonical village review.
 *
 * <p>The palette is deliberately derived from the selected Kogtyv Greece shells:
 * gravel/coarse dirt for soft village walks, and tuff/polished diorite for the
 * civic plaza. This class is review-profile-only and never runs in ordinary
 * saves.</p>
 */
public final class VillageReviewLandscape {
    private static final int MAX_PATH_GRADE_DELTA = 6;
    private static final int MAX_PLAZA_GRADE_DELTA = 5;

    private static final int PLAZA_MIN_X = -311;
    private static final int PLAZA_MAX_X = -299;
    private static final int PLAZA_MIN_Z = -37;
    private static final int PLAZA_MAX_Z = -27;
    private static final int PLAZA_SURFACE_Y = 71;
    private static final int PLAZA_CENTER_X = (PLAZA_MIN_X + PLAZA_MAX_X) / 2;
    private static final int PLAZA_CENTER_Z = (PLAZA_MIN_Z + PLAZA_MAX_Z) / 2;

    private static final List<PathSpec> PATHS = List.of(
            path("harbor_to_plaza",
                    node(-328, 64, -48),
                    node(-318, 68, -40),
                    node(-312, 71, -32)),
            path("general_store",
                    node(-305, 71, -38),
                    node(-298, 71, -42),
                    node(-293, 71, -42)),
            path("cafe",
                    node(-299, 71, -30),
                    node(-298, 72, -24)),
            path("resident_services",
                    node(-306, 71, -26),
                    node(-306, 70, -21)),
            path("museum_forecourt",
                    node(-299, 71, -34),
                    node(-290, 73, -35),
                    node(-286, 74, -34)),
            path("clinic",
                    node(-312, 71, -32),
                    node(-324, 67, -24),
                    node(-323, 66, -14)),
            path("clothing",
                    node(-306, 70, -23),
                    node(-298, 71, -23),
                    node(-298, 72, -20),
                    node(-297, 73, -14),
                    node(-297, 73, -7),
                    node(-293, 73, -7)),
            path("north_house",
                    node(-297, 73, -7),
                    node(-297, 72, -2),
                    node(-297, 71, 4)),
            path("south_housing",
                    node(-318, 68, -40),
                    node(-314, 68, -52),
                    node(-314, 67, -65),
                    node(-314, 66, -70)),
            path("east_house",
                    node(-293, 71, -42),
                    node(-293, 70, -52),
                    node(-286, 69, -60),
                    node(-285, 69, -60))
    );

    private VillageReviewLandscape() {}

    public static void preflight(ServerLevel level, List<BoundingBox> protectedBounds) {
        Map<BlockPos, Integer> roads = collectRoadTargets();
        for (Map.Entry<BlockPos, Integer> entry : roads.entrySet()) {
            BlockPos cell = entry.getKey();
            if (isProtected(cell.getX(), cell.getZ(), protectedBounds)) {
                continue;
            }
            checkGrade(level, cell.getX(), cell.getZ(), entry.getValue(), MAX_PATH_GRADE_DELTA, "path");
        }

        for (int x = PLAZA_MIN_X; x <= PLAZA_MAX_X; x++) {
            for (int z = PLAZA_MIN_Z; z <= PLAZA_MAX_Z; z++) {
                if (isProtected(x, z, protectedBounds)) {
                    throw new IllegalStateException("Campfire plaza intersects a protected building footprint at " + x + "," + z);
                }
                checkGrade(level, x, z, PLAZA_SURFACE_Y, MAX_PLAZA_GRADE_DELTA, "plaza");
            }
        }
    }

    public static void place(ServerLevel level, List<BoundingBox> protectedBounds) {
        Map<BlockPos, Integer> roads = collectRoadTargets();
        int roadCells = 0;
        for (Map.Entry<BlockPos, Integer> entry : roads.entrySet()) {
            BlockPos cell = entry.getKey();
            if (isProtected(cell.getX(), cell.getZ(), protectedBounds)) {
                continue;
            }
            gradeSurface(
                    level,
                    cell.getX(),
                    cell.getZ(),
                    entry.getValue(),
                    roadMaterial(cell.getX(), cell.getZ())
            );
            roadCells++;
        }

        int plazaCells = 0;
        for (int x = PLAZA_MIN_X; x <= PLAZA_MAX_X; x++) {
            for (int z = PLAZA_MIN_Z; z <= PLAZA_MAX_Z; z++) {
                gradeSurface(level, x, z, PLAZA_SURFACE_Y, plazaMaterial(x, z));
                plazaCells++;
            }
        }

        CampfireSessions.LOGGER.info(
                "Campfire village review landscape applied: {} road cells, {} plaza cells",
                roadCells,
                plazaCells
        );
    }

    private static void checkGrade(
            ServerLevel level,
            int x,
            int z,
            int targetY,
            int maxDelta,
            String kind
    ) {
        level.getChunkAt(new BlockPos(x, targetY, z));
        int currentSurface = surfaceY(level, x, z);
        if (Math.abs(currentSurface - targetY) > maxDelta) {
            throw new IllegalStateException(
                    "Campfire village " + kind + " grading limit exceeded at " + x + "," + z
                            + ": surface=" + currentSurface + " target=" + targetY
            );
        }
    }

    private static void gradeSurface(
            ServerLevel level,
            int x,
            int z,
            int targetY,
            BlockState top
    ) {
        level.getChunkAt(new BlockPos(x, targetY, z));
        int currentSurface = surfaceY(level, x, z);

        if (currentSurface < targetY) {
            for (int y = currentSurface + 1; y < targetY; y++) {
                level.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        } else if (currentSurface > targetY) {
            for (int y = targetY + 1; y <= currentSurface + 3; y++) {
                level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        BlockPos support = new BlockPos(x, targetY - 1, z);
        if (level.getBlockState(support).isAir() || !level.getFluidState(support).isEmpty()) {
            level.setBlock(support, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        }

        BlockPos surface = new BlockPos(x, targetY, z);
        level.setBlock(surface, top, Block.UPDATE_ALL);
        level.setBlock(surface.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(surface.above(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private static int surfaceY(ServerLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    private static BlockState roadMaterial(int x, int z) {
        int pattern = Math.floorMod(x * 31 + z * 17, 11);
        return pattern < 2 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
    }

    private static BlockState plazaMaterial(int x, int z) {
        boolean border = x == PLAZA_MIN_X || x == PLAZA_MAX_X || z == PLAZA_MIN_Z || z == PLAZA_MAX_Z;
        if (border) {
            return Blocks.POLISHED_TUFF.defaultBlockState();
        }
        if (x == PLAZA_CENTER_X || z == PLAZA_CENTER_Z) {
            return Blocks.POLISHED_DIORITE.defaultBlockState();
        }
        return Blocks.TUFF_BRICKS.defaultBlockState();
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
        putRoadCell(cells, pathName, x, z, y);
        putRoadCell(cells, pathName, x - 1, z, y);
        putRoadCell(cells, pathName, x + 1, z, y);
        putRoadCell(cells, pathName, x, z - 1, y);
        putRoadCell(cells, pathName, x, z + 1, y);
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

    private static PathNode node(int x, int y, int z) {
        return new PathNode(x, y, z);
    }

    private static PathSpec path(String name, PathNode... nodes) {
        return new PathSpec(name, List.of(nodes));
    }

    private record PathNode(int x, int y, int z) {}

    private record PathSpec(String name, List<PathNode> nodes) {}
}
