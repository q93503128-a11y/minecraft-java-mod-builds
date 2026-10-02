package kr.moonseungjun.campfiresessions.world;

import java.util.ArrayList;
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
 * Review-only authored terrain pass for the entrance-aware Campfire village slice.
 *
 * <p>The second client review rejected the dense civic block and oversized
 * continuous terrace. V6 keeps one compact public hub, gives later facilities
 * their own satellite parcels, and distributes the shared housing pool across
 * actual 25x25 reserved lots selected from the canonical-world terrain probe. The initial pass only grades a 17x17 inner pad so yards keep natural edges and future upgrades still have reserved space.</p>
 */
public final class VillageReviewLandscape {
    private static final int AREA_FEATHER = 4;
    private static final int MAX_AREA_DELTA = 12;
    private static final int MAX_PATH_DELTA = 6;

    private static final List<AreaSpec> AREAS = List.of(
            area("civic_west", -323, -292, -63, -21, 72),
            area("civic_southeast", -293, -270, -63, -29, 72),
            area("upper_civic", -317, -282, -21, 18, 75),

            area("clinic_green", -192, -168, -144, -120, 65),
            area("museum_green", -248, -224, 48, 72, 64),

            home("home_01", -308, -76, 68),
            home("home_02", -164, -164, 63),
            home("home_03", -156, 76, 67),
            home("home_04", -12, 76, 65),
            home("home_05", 20, 68, 65),
            home("home_06", 44, 44, 67),
            home("home_07", 76, 36, 65),
            home("home_08", 116, -108, 65),
            home("home_09", 164, -100, 64),
            home("home_10", 100, -44, 91),
            home("home_11", 172, -28, 72)
    );

    private static final List<PathSpec> PATHS = List.of(
            path("harbor_arrival",
                    node(-326, 64, -49),
                    node(-322, 67, -47),
                    node(-316, 70, -44),
                    node(-306, 72, -39)),
            path("plaza_to_services",
                    node(-306, 72, -36),
                    node(-311, 72, -35),
                    node(-315, 72, -34)),
            path("plaza_to_store",
                    node(-301, 72, -42),
                    node(-295, 72, -47),
                    node(-288, 72, -53)),
            path("plaza_to_cafe",
                    node(-299, 72, -31),
                    node(-298, 73, -24),
                    node(-298, 75, -15)),
            path("upper_clothing_branch",
                    node(-299, 75, -16),
                    node(-302, 75, -8),
                    node(-304, 75, 2)),

            path("clinic_front",
                    node(-180, 65, -126),
                    node(-180, 65, -122)),
            path("museum_front",
                    node(-236, 64, 54),
                    node(-236, 64, 50)),

            path("player_house_front",
                    node(-308, 68, -72),
                    node(-308, 68, -68)),
            path("resident_house_1_front",
                    node(-164, 63, -160),
                    node(-164, 63, -156)),
            path("resident_house_2_front",
                    node(-156, 67, 72),
                    node(-156, 67, 68)),
            path("resident_house_3_front",
                    node(-12, 65, 72),
                    node(-12, 65, 68)),
            path("resident_house_4_front",
                    node(15, 65, 68),
                    node(11, 65, 68)),
            path("resident_house_5_front",
                    node(40, 67, 44),
                    node(36, 67, 44)),
            path("resident_house_6_front",
                    node(75, 65, 31),
                    node(75, 65, 27)),
            path("resident_house_7_front",
                    node(117, 65, -104),
                    node(117, 65, -100)),
            path("resident_house_8_front",
                    node(164, 64, -96),
                    node(164, 64, -92)),
            path("resident_house_9_front",
                    node(96, 91, -44),
                    node(92, 91, -44)),
            path("resident_house_10_front",
                    node(178, 72, -30),
                    node(182, 72, -30))
    );

    private VillageReviewLandscape() {}

    public static void preflightTerraces(ServerLevel level) {
        List<String> failures = new ArrayList<>();
        for (AreaSpec area : AREAS) {
            int worstDelta = 0;
            int worstX = area.minX();
            int worstZ = area.minZ();
            int worstSurface = area.surfaceY();

            for (int x = area.minX(); x <= area.maxX(); x++) {
                for (int z = area.minZ(); z <= area.maxZ(); z++) {
                    if (isOceanColumn(level, x, z)) {
                        continue;
                    }
                    int surface = surfaceY(level, x, z);
                    int distance = edgeDistance(area, x, z);
                    int target = blendedTarget(surface, area.surfaceY(), distance);
                    int delta = Math.abs(surface - target);
                    if (delta > worstDelta) {
                        worstDelta = delta;
                        worstX = x;
                        worstZ = z;
                        worstSurface = surface;
                    }
                }
            }

            CampfireSessions.LOGGER.info(
                    "Campfire village area preflight {}: targetY={} worstDelta={} worst=({},{} surface={})",
                    area.name(),
                    area.surfaceY(),
                    worstDelta,
                    worstX,
                    worstZ,
                    worstSurface
            );
            if (worstDelta > MAX_AREA_DELTA) {
                failures.add(
                        area.name()
                                + " worst=(" + worstX + "," + worstZ + ")"
                                + " surface=" + worstSurface
                                + " target=" + area.surfaceY()
                                + " blendedDelta=" + worstDelta
                );
            }
        }
        if (!failures.isEmpty()) {
            throw new IllegalStateException(
                    "Campfire village parcel grading failed: " + String.join("; ", failures)
            );
        }
    }

    public static void placeTerraces(ServerLevel level) {
        int cells = 0;
        for (AreaSpec area : AREAS) {
            for (int x = area.minX(); x <= area.maxX(); x++) {
                for (int z = area.minZ(); z <= area.maxZ(); z++) {
                    if (isOceanColumn(level, x, z)) {
                        continue;
                    }
                    int surface = surfaceY(level, x, z);
                    int distance = edgeDistance(area, x, z);
                    int target = blendedTarget(surface, area.surfaceY(), distance);
                    boolean clearVegetation = distance >= 2;
                    gradeSurface(
                            level,
                            x,
                            z,
                            target,
                            Blocks.GRASS_BLOCK.defaultBlockState(),
                            clearVegetation
                    );
                    cells++;
                }
            }
        }
        CampfireSessions.LOGGER.info(
                "Campfire village review parcels applied: {} cells across {} areas",
                cells,
                AREAS.size()
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

    private static int edgeDistance(AreaSpec area, int x, int z) {
        return Math.min(
                Math.min(x - area.minX(), area.maxX() - x),
                Math.min(z - area.minZ(), area.maxZ() - z)
        );
    }

    private static int blendedTarget(int current, int target, int edgeDistance) {
        if (edgeDistance >= AREA_FEATHER) {
            return target;
        }
        double t = (edgeDistance + 1.0) / (AREA_FEATHER + 1.0);
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
        int pattern = Math.floorMod(x * 31 + z * 17, 19);
        if (pattern == 0 || pattern == 7) {
            return Blocks.COARSE_DIRT.defaultBlockState();
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
        putRoadCell(cells, pathName, x, z, y);
        putRoadCell(cells, pathName, x + 1, z, y);
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
        if (Math.abs(previous - y) > 2) {
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

    private static boolean isOceanColumn(ServerLevel level, int x, int z) {
        BlockPos sea = new BlockPos(x, 62, z);
        level.getChunkAt(sea);
        return !level.getFluidState(sea).isEmpty() || surfaceY(level, x, z) < 55;
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

    private static AreaSpec area(String name, int minX, int maxX, int minZ, int maxZ, int y) {
        return new AreaSpec(name, minX, maxX, minZ, maxZ, y);
    }

    private static AreaSpec home(String name, int centerX, int centerZ, int y) {
        return area(name, centerX - 8, centerX + 8, centerZ - 8, centerZ + 8, y);
    }

    private static PathNode node(int x, int y, int z) {
        return new PathNode(x, y, z);
    }

    private static PathSpec path(String name, PathNode... nodes) {
        return new PathSpec(name, List.of(nodes));
    }

    private record AreaSpec(String name, int minX, int maxX, int minZ, int maxZ, int surfaceY) {}
    private record PathNode(int x, int y, int z) {}
    private record PathSpec(String name, List<PathNode> nodes) {}
}
