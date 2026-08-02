package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Carves a compact, walkable ground-floor room behind every retained urban entrance and furnishes
 * it according to the plot's assigned civic role. The exterior facade remains authored by the
 * attributed schematic; only the protected interior volume behind the door is normalized.
 */
public final class ErdenUrbanInteriorBuilder {
    private static final int CLEAR_HEIGHT = 4;
    private static final int MAX_HALF_WIDTH = 4;
    private static final int MAX_DEPTH = 11;
    private static final Set<String> SUPPORTED_ROLES = Set.of(
            "tenement", "shop", "bakery", "inn",
            "stable", "guard_post", "bathhouse", "warehouse"
    );
    private static volatile boolean diagnosticsLogged;

    private ErdenUrbanInteriorBuilder() {
    }

    public static void addChunk(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            String role,
            int originX,
            int floorY,
            int originZ,
            int buildingWidth,
            int buildingLength,
            int doorX,
            int doorZ,
            int roadX,
            int roadZ) {
        if (!SUPPORTED_ROLES.contains(role)) {
            throw new IllegalArgumentException("Unsupported Erden urban interior role " + role);
        }
        logDiagnosticsOnce();
        InteriorRoom room = createRoom(
                role, originX, floorY, originZ,
                buildingWidth, buildingLength,
                doorX, doorZ, roadX, roadZ);
        if (room == null || !room.intersects(chunk)) return;

        carveWalkableRoom(plan, chunk, room);
        switch (role) {
            case "tenement" -> furnishTenement(plan, chunk, room);
            case "shop" -> furnishShop(plan, chunk, room);
            case "bakery" -> furnishBakery(plan, chunk, room);
            case "inn" -> furnishInn(plan, chunk, room);
            case "stable" -> furnishStable(plan, chunk, room);
            case "guard_post" -> furnishGuardPost(plan, chunk, room);
            case "bathhouse" -> furnishBathhouse(plan, chunk, room);
            case "warehouse" -> furnishWarehouse(plan, chunk, room);
            default -> throw new IllegalStateException("Unhandled Erden urban role " + role);
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

    private static void logDiagnosticsOnce() {
        if (diagnosticsLogged) return;
        synchronized (ErdenUrbanInteriorBuilder.class) {
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
            LivingKingdoms.LOGGER.info(
                    "Prepared Erden functional urban interiors plots={} fixture_families={} clear_aisles=true roles={}",
                    total, SUPPORTED_ROLES.size(), counts);
            diagnosticsLogged = true;
        }
    }

    private static InteriorRoom createRoom(
            String role,
            int originX,
            int floorY,
            int originZ,
            int buildingWidth,
            int buildingLength,
            int doorX,
            int doorZ,
            int roadX,
            int roadZ) {
        int deltaX = roadX - doorX;
        int deltaZ = roadZ - doorZ;
        int outwardX;
        int outwardZ;
        Direction inwardDirection;
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            outwardX = deltaX >= 0 ? 1 : -1;
            outwardZ = 0;
            inwardDirection = outwardX > 0 ? Direction.WEST : Direction.EAST;
        } else {
            outwardX = 0;
            outwardZ = deltaZ >= 0 ? 1 : -1;
            inwardDirection = outwardZ > 0 ? Direction.NORTH : Direction.SOUTH;
        }
        int inwardX = -outwardX;
        int inwardZ = -outwardZ;
        int rightX = -inwardZ;
        int rightZ = inwardX;

        int minX = originX + 2;
        int maxX = originX + buildingWidth - 3;
        int minZ = originZ + 2;
        int maxZ = originZ + buildingLength - 3;
        int availableDepth = distanceToBoundary(
                doorX, doorZ, inwardX, inwardZ, minX, maxX, minZ, maxZ);
        int positiveWidth = distanceToBoundary(
                doorX, doorZ, rightX, rightZ, minX, maxX, minZ, maxZ);
        int negativeWidth = distanceToBoundary(
                doorX, doorZ, -rightX, -rightZ, minX, maxX, minZ, maxZ);
        int depth = Math.min(MAX_DEPTH, availableDepth);
        int halfWidth = Math.min(MAX_HALF_WIDTH, Math.min(positiveWidth, negativeWidth));
        if (depth < 7 || halfWidth < 3) return null;
        return new InteriorRoom(
                role, floorY, doorX, doorZ,
                inwardX, inwardZ, rightX, rightZ,
                inwardDirection, halfWidth, depth);
    }

    private static int distanceToBoundary(
            int x, int z, int directionX, int directionZ,
            int minX, int maxX, int minZ, int maxZ) {
        if (directionX > 0) return maxX - x;
        if (directionX < 0) return x - minX;
        if (directionZ > 0) return maxZ - z;
        if (directionZ < 0) return z - minZ;
        return 0;
    }

    private static void carveWalkableRoom(
            IncrementalWorldEditPlan plan, ChunkPos chunk, InteriorRoom room) {
        Block floor = switch (room.role) {
            case "stable" -> Blocks.COARSE_DIRT;
            case "guard_post" -> Blocks.STONE_BRICKS;
            case "bathhouse" -> Blocks.SMOOTH_STONE;
            case "warehouse" -> Blocks.SPRUCE_PLANKS;
            default -> Blocks.OAK_PLANKS;
        };
        for (int depth = 1; depth <= room.depth; depth++) {
            for (int lateral = -room.halfWidth; lateral <= room.halfWidth; lateral++) {
                Point point = room.point(lateral, depth);
                if (!contains(chunk, point.x, point.z)) continue;
                plan.addSet(point.x, room.floorY, point.z, floor);
                plan.addFill(
                        point.x, room.floorY + 1, point.z,
                        point.x, room.floorY + CLEAR_HEIGHT, point.z,
                        Blocks.AIR);
            }
        }
    }

    private static void furnishTenement(
            IncrementalWorldEditPlan plan, ChunkPos chunk, InteriorRoom room) {
        placeBed(plan, chunk, room, -room.halfWidth + 1, 5, bed("white_bed"));
        placeBed(plan, chunk, room, room.halfWidth - 1, 5, bed("light_gray_bed"));
        if (room.depth >= 10) {
            placeBed(plan, chunk, room, -room.halfWidth + 1, 9, bed("brown_bed"));
            placeBed(plan, chunk, room, room.halfWidth - 1, 9, bed("gray_bed"));
        }
        place(plan, chunk, room, -room.halfWidth, 3, 1, Blocks.BARREL);
        place(plan, chunk, room, room.halfWidth, 3, 1, Blocks.BARREL);
        place(plan, chunk, room, 0, room.depth, 1, Blocks.CRAFTING_TABLE);
    }

    private static void furnishShop(
            IncrementalWorldEditPlan plan, ChunkPos chunk, InteriorRoom room) {
        for (int lateral = -room.halfWidth; lateral <= room.halfWidth; lateral++) {
            if (Math.abs(lateral) <= 1) continue;
            place(plan, chunk, room, lateral, 4, 1, Blocks.OAK_SLAB);
        }
        place(plan, chunk, room, -room.halfWidth, 7, 1, Blocks.BARREL);
        place(plan, chunk, room, room.halfWidth, 7, 1, Blocks.CHEST);
        place(plan, chunk, room, -room.halfWidth, room.depth, 1, Blocks.BOOKSHELF);
        place(plan, chunk, room, room.halfWidth, room.depth, 1, Blocks.BOOKSHELF);
        place(plan, chunk, room, 0, room.depth, 1, Blocks.CRAFTING_TABLE);
    }

    private static void furnishBakery(
            IncrementalWorldEditPlan plan, ChunkPos chunk, InteriorRoom room) {
        place(plan, chunk, room, -2, room.depth, 1, Blocks.FURNACE);
        place(plan, chunk, room, 0, room.depth, 1, Blocks.SMOKER);
        place(plan, chunk, room, 2, room.depth, 1, Blocks.FURNACE);
        place(plan, chunk, room, -room.halfWidth, 5, 1, Blocks.BARREL);
        place(plan, chunk, room, room.halfWidth, 5, 1, Blocks.HAY_BLOCK);
        place(plan, chunk, room, -room.halfWidth, 8, 1, Blocks.CRAFTING_TABLE);
        place(plan, chunk, room, room.halfWidth, 8, 1, Blocks.CHEST);
    }

    private static void furnishInn(
            IncrementalWorldEditPlan plan, ChunkPos chunk, InteriorRoom room) {
        placeTable(plan, chunk, room, -2, 4);
        placeTable(plan, chunk, room, 2, 4);
        placeBed(plan, chunk, room, -room.halfWidth + 1, 8, bed("red_bed"));
        placeBed(plan, chunk, room, room.halfWidth - 1, 8, bed("blue_bed"));
        place(plan, chunk, room, -room.halfWidth, room.depth, 1, Blocks.BARREL);
        place(plan, chunk, room, room.halfWidth, room.depth, 1, Blocks.CHEST);
    }

    private static void furnishStable(
            IncrementalWorldEditPlan plan, ChunkPos chunk, InteriorRoom room) {
        for (int depth = 4; depth <= room.depth; depth++) {
            place(plan, chunk, room, -2, depth, 1, Blocks.OAK_FENCE);
            place(plan, chunk, room, 2, depth, 1, Blocks.OAK_FENCE);
        }
        place(plan, chunk, room, -room.halfWidth, room.depth, 1, Blocks.HAY_BLOCK);
        place(plan, chunk, room, room.halfWidth, room.depth, 1, Blocks.HAY_BLOCK);
        place(plan, chunk, room, -room.halfWidth, 5, 1, Blocks.WATER_CAULDRON);
        place(plan, chunk, room, room.halfWidth, 5, 1, Blocks.BARREL);
    }

    private static void furnishGuardPost(
            IncrementalWorldEditPlan plan, ChunkPos chunk, InteriorRoom room) {
        for (int depth = 5; depth <= room.depth; depth++) {
            place(plan, chunk, room, -room.halfWidth + 1, depth, 1, Blocks.IRON_BARS);
            place(plan, chunk, room, room.halfWidth - 1, depth, 1, Blocks.IRON_BARS);
        }
        place(plan, chunk, room, -2, room.depth, 1, Blocks.ANVIL);
        place(plan, chunk, room, 0, room.depth, 1, Blocks.STONECUTTER);
        place(plan, chunk, room, 2, room.depth, 1, Blocks.TARGET);
        place(plan, chunk, room, -room.halfWidth, 3, 1, Blocks.BARREL);
        place(plan, chunk, room, room.halfWidth, 3, 1, Blocks.CHEST);
    }

    private static void furnishBathhouse(
            IncrementalWorldEditPlan plan, ChunkPos chunk, InteriorRoom room) {
        int poolStart = 5;
        int poolEnd = Math.max(poolStart + 2, room.depth - 1);
        for (int depth = poolStart; depth <= poolEnd; depth++) {
            for (int lateral = -3; lateral <= 3; lateral++) {
                boolean border = depth == poolStart || depth == poolEnd
                        || lateral == -3 || lateral == 3;
                place(plan, chunk, room, lateral, depth, 0, Blocks.SMOOTH_STONE);
                place(plan, chunk, room, lateral, depth, 1,
                        border ? Blocks.SMOOTH_STONE : Blocks.WATER);
            }
        }
        place(plan, chunk, room, -room.halfWidth, 3, 1, Blocks.SMOOTH_STONE_SLAB);
        place(plan, chunk, room, room.halfWidth, 3, 1, Blocks.SMOOTH_STONE_SLAB);
        place(plan, chunk, room, -room.halfWidth, room.depth, 1, Blocks.WATER_CAULDRON);
        place(plan, chunk, room, room.halfWidth, room.depth, 1, Blocks.WATER_CAULDRON);
    }

    private static void furnishWarehouse(
            IncrementalWorldEditPlan plan, ChunkPos chunk, InteriorRoom room) {
        for (int depth = 3; depth <= room.depth; depth += 2) {
            place(plan, chunk, room, -room.halfWidth, depth, 1, Blocks.BARREL);
            place(plan, chunk, room, room.halfWidth, depth, 1, Blocks.BARREL);
            if (depth >= 5) {
                place(plan, chunk, room, -room.halfWidth, depth, 2, Blocks.BARREL);
                place(plan, chunk, room, room.halfWidth, depth, 2, Blocks.BARREL);
            }
        }
        place(plan, chunk, room, -2, room.depth, 1, Blocks.CHEST);
        place(plan, chunk, room, 2, room.depth, 1, Blocks.CHEST);
        place(plan, chunk, room, 0, room.depth, 1, Blocks.CRAFTING_TABLE);
    }

    private static void placeTable(
            IncrementalWorldEditPlan plan, ChunkPos chunk,
            InteriorRoom room, int lateral, int depth) {
        place(plan, chunk, room, lateral, depth, 1, Blocks.OAK_FENCE);
        place(plan, chunk, room, lateral, depth, 2, Blocks.OAK_PRESSURE_PLATE);
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
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            InteriorRoom room,
            int lateral,
            int depth,
            BedBlock bed) {
        Point footPoint = room.point(lateral, depth);
        Point headPoint = room.point(lateral, depth + 1);
        BlockState foot = bed.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.FOOT)
                .setValue(HorizontalDirectionalBlock.FACING, room.inwardDirection);
        BlockState head = bed.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.HEAD)
                .setValue(HorizontalDirectionalBlock.FACING, room.inwardDirection);
        if (contains(chunk, footPoint.x, footPoint.z)) {
            plan.addSet(footPoint.x, room.floorY + 1, footPoint.z, foot);
        }
        if (contains(chunk, headPoint.x, headPoint.z)) {
            plan.addSet(headPoint.x, room.floorY + 1, headPoint.z, head);
        }
    }

    private static void place(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            InteriorRoom room,
            int lateral,
            int depth,
            int yOffset,
            Block block) {
        Point point = room.point(lateral, depth);
        if (!contains(chunk, point.x, point.z)) return;
        plan.addSet(point.x, room.floorY + yOffset, point.z, block);
    }

    private static boolean contains(ChunkPos chunk, int x, int z) {
        return x >= chunk.getMinBlockX()
                && x <= chunk.getMinBlockX() + 15
                && z >= chunk.getMinBlockZ()
                && z <= chunk.getMinBlockZ() + 15;
    }

    private record InteriorRoom(
            String role,
            int floorY,
            int doorX,
            int doorZ,
            int inwardX,
            int inwardZ,
            int rightX,
            int rightZ,
            Direction inwardDirection,
            int halfWidth,
            int depth) {
        Point point(int lateral, int forward) {
            return new Point(
                    doorX + inwardX * forward + rightX * lateral,
                    doorZ + inwardZ * forward + rightZ * lateral);
        }

        boolean intersects(ChunkPos chunk) {
            Point a = point(-halfWidth, 1);
            Point b = point(halfWidth, depth);
            int minX = Math.min(a.x, b.x);
            int maxX = Math.max(a.x, b.x);
            int minZ = Math.min(a.z, b.z);
            int maxZ = Math.max(a.z, b.z);
            return maxX >= chunk.getMinBlockX()
                    && minX <= chunk.getMinBlockX() + 15
                    && maxZ >= chunk.getMinBlockZ()
                    && minZ <= chunk.getMinBlockZ() + 15;
        }
    }

    private record Point(int x, int z) {
    }
}
