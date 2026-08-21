package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.world.ExternalUrbanFabricBuilder.UrbanFragmentSnapshot;
import kr.moonseungjun.livingkingdoms.world.ExternalUrbanFabricBuilder.UrbanSourceBlock;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Streams terrain-following rural settlement fabric using the same attributed, source-native
 * facade fragments already proven in the capital. No synthetic box-house shell is authored here.
 */
public final class ErdenRegionalSettlementBuilder {
    private static final int ROAD_HALF_WIDTH = 2;
    private static final int RING = 72;
    private static final int ACCESS_LIMIT = 46;
    private static volatile Map<String, UrbanFragmentSnapshot> fragmentsByStyle;

    private ErdenRegionalSettlementBuilder() {
    }

    public static void addChunk(IncrementalWorldEditPlan plan, ServerLevel level, ChunkPos chunk) {
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            if (!intersects(settlement, chunk)) continue;
            addTerrainFabric(plan, level, chunk, settlement);
            addIndustryLandscape(plan, level, chunk, settlement);
            addVillageSquare(plan, level, chunk, settlement);
            for (ErdenRegionalSettlementCatalog.BuildingLot lot : settlement.buildings()) {
                addBuilding(plan, level, chunk, settlement, lot);
            }
        }
    }

    public static int sourceStyleCount() {
        return fragments().size();
    }

    private static void addTerrainFabric(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenRegionalSettlementCatalog.Settlement settlement) {
        for (int x = chunk.getMinBlockX(); x <= chunk.getMinBlockX() + 15; x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMinBlockZ() + 15; z++) {
                int dx = x - settlement.x();
                int dz = z - settlement.z();
                if (!isVillageRoad(dx, dz)) continue;
                surfacePath(plan, level, x, z, Blocks.PACKED_MUD);
            }
        }
        lanternPost(plan, level, chunk, settlement.x() - 15, settlement.z() - 15);
        lanternPost(plan, level, chunk, settlement.x() + 15, settlement.z() - 15);
        lanternPost(plan, level, chunk, settlement.x() - 15, settlement.z() + 15);
        lanternPost(plan, level, chunk, settlement.x() + 15, settlement.z() + 15);
    }

    private static boolean isVillageRoad(int dx, int dz) {
        if (Math.abs(dx) <= ROAD_HALF_WIDTH && Math.abs(dz) <= RING + 34) return true;
        if (Math.abs(dz) <= ROAD_HALF_WIDTH && Math.abs(dx) <= RING + 38) return true;
        boolean northSouthRing = Math.abs(Math.abs(dx) - RING) <= ROAD_HALF_WIDTH
                && Math.abs(dz) <= RING;
        boolean eastWestRing = Math.abs(Math.abs(dz) - RING) <= ROAD_HALF_WIDTH
                && Math.abs(dx) <= RING;
        return northSouthRing || eastWestRing;
    }

    private static void addIndustryLandscape(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenRegionalSettlementCatalog.Settlement settlement) {
        switch (settlement.industry()) {
            case "grain" -> addGrainFields(plan, level, chunk, settlement);
            case "ranch" -> addRanchPaddocks(plan, level, chunk, settlement);
            case "colliery" -> addMineYard(plan, level, chunk, settlement, false);
            case "iron_mine" -> addMineYard(plan, level, chunk, settlement, true);
            case "river_market" -> addRiverMarket(plan, level, chunk, settlement);
            default -> throw new IllegalStateException("Unknown regional industry " + settlement.industry());
        }
    }

    private static void addGrainFields(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenRegionalSettlementCatalog.Settlement settlement) {
        for (int x = chunk.getMinBlockX(); x <= chunk.getMinBlockX() + 15; x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMinBlockZ() + 15; z++) {
                int dx = x - settlement.x();
                int dz = z - settlement.z();
                boolean westField = dx >= -205 && dx <= -145 && Math.abs(dz) <= 118;
                boolean eastField = dx >= 145 && dx <= 205 && Math.abs(dz) <= 118;
                boolean southField = dz >= 145 && dz <= 205 && Math.abs(dx) <= 118;
                if (!westField && !eastField && !southField) continue;
                int y = surfaceY(x, z);
                boolean boundary = Math.floorMod(dx, 60) == 0 || Math.floorMod(dz, 60) == 0;
                plan.addFill(x, y + 1, z, x, y + 3, z, Blocks.AIR);
                if (boundary) {
                    plan.addSet(x, y, z, Blocks.DIRT_PATH);
                    plan.addFill(x, y + 1, z, x, y + 2, z, Blocks.AIR);
                } else if (Math.floorMod(dx + dz, 9) == 0) {
                    plan.addSet(x, y, z, Blocks.WATER);
                    plan.addSet(x, y + 1, z, Blocks.AIR);
                } else {
                    plan.addSet(x, y, z, Blocks.FARMLAND);
                    plan.addSet(x, y + 1, z, Blocks.WHEAT);
                }
            }
        }
        hayStack(plan, level, chunk, settlement.x() + 124, settlement.z() + 150);
        hayStack(plan, level, chunk, settlement.x() - 124, settlement.z() + 150);
    }

    private static void addRanchPaddocks(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenRegionalSettlementCatalog.Settlement settlement) {
        addPaddock(plan, level, chunk, settlement.x() - 170, settlement.z(), 70, 150);
        addPaddock(plan, level, chunk, settlement.x() + 170, settlement.z(), 70, 150);
        hayStack(plan, level, chunk, settlement.x() - 140, settlement.z() + 55);
        hayStack(plan, level, chunk, settlement.x() + 140, settlement.z() - 55);
        waterTrough(plan, level, chunk, settlement.x() - 150, settlement.z() - 48);
        waterTrough(plan, level, chunk, settlement.x() + 150, settlement.z() + 48);
    }

    private static void addPaddock(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            int centerX,
            int centerZ,
            int width,
            int length) {
        int minX = centerX - width / 2;
        int maxX = centerX + width / 2;
        int minZ = centerZ - length / 2;
        int maxZ = centerZ + length / 2;
        for (int x = Math.max(minX, chunk.getMinBlockX()); x <= Math.min(maxX, chunk.getMinBlockX() + 15); x++) {
            for (int z = Math.max(minZ, chunk.getMinBlockZ()); z <= Math.min(maxZ, chunk.getMinBlockZ() + 15); z++) {
                int y = surfaceY(x, z);
                plan.addSet(x, y, z, Blocks.GRASS_BLOCK);
                plan.addFill(x, y + 1, z, x, y + 3, z, Blocks.AIR);
                boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                if (edge) plan.addSet(x, y + 1, z, Blocks.OAK_FENCE);
            }
        }
    }

    private static void addMineYard(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenRegionalSettlementCatalog.Settlement settlement,
            boolean iron) {
        int yardX = settlement.x() - 150;
        int yardZ = settlement.z();
        for (int x = Math.max(yardX - 42, chunk.getMinBlockX()); x <= Math.min(yardX + 42, chunk.getMinBlockX() + 15); x++) {
            for (int z = Math.max(yardZ - 52, chunk.getMinBlockZ()); z <= Math.min(yardZ + 52, chunk.getMinBlockZ() + 15); z++) {
                int y = surfaceY(x, z);
                plan.addSet(x, y, z, Blocks.GRAVEL);
                plan.addFill(x, y + 1, z, x, y + 3, z, Blocks.AIR);
                if (Math.abs(z - yardZ) <= 1) plan.addSet(x, y + 1, z, Blocks.RAIL);
            }
        }
        Block stock = iron ? Blocks.RAW_IRON_BLOCK : Blocks.COAL_BLOCK;
        stockPile(plan, chunk, yardX + 24, surfaceY(yardX + 24, yardZ + 28), yardZ + 28, stock);
        stockPile(plan, chunk, yardX + 24, surfaceY(yardX + 24, yardZ - 28), yardZ - 28, stock);
        addAdit(plan, level, chunk, yardX - 34, yardZ, iron);
    }

    private static void addAdit(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            int x,
            int z,
            boolean iron) {
        int y = surfaceY(x, z);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = 0; dy <= 6; dy++) {
                boolean frame = Math.abs(dx) == 4 || dy == 0 || dy == 6;
                setIfInChunk(plan, chunk, x + dx, y + dy, z, frame ? Blocks.DEEPSLATE_BRICKS : Blocks.AIR);
                for (int depth = 1; depth <= 5; depth++) {
                    setIfInChunk(plan, chunk, x + dx, y + dy, z - depth,
                            frame ? Blocks.DEEPSLATE_BRICKS : Blocks.AIR);
                }
            }
        }
        for (int depth = 0; depth <= 5; depth++) {
            setIfInChunk(plan, chunk, x, y, z - depth, Blocks.STONE);
            setIfInChunk(plan, chunk, x, y + 1, z - depth, Blocks.RAIL);
        }
        setIfInChunk(plan, chunk, x - 3, y + 4, z - 1, Blocks.LANTERN);
        setIfInChunk(plan, chunk, x + 3, y + 4, z - 1, Blocks.LANTERN);
        setIfInChunk(plan, chunk, x + 3, y + 1, z + 4, iron ? Blocks.RAW_IRON_BLOCK : Blocks.COAL_BLOCK);
    }

    private static void addRiverMarket(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenRegionalSettlementCatalog.Settlement settlement) {
        for (int x = Math.max(settlement.x() - 38, chunk.getMinBlockX());
             x <= Math.min(settlement.x() + 38, chunk.getMinBlockX() + 15); x++) {
            for (int z = Math.max(settlement.z() + 126, chunk.getMinBlockZ());
                 z <= Math.min(settlement.z() + 182, chunk.getMinBlockZ() + 15); z++) {
                surfacePath(plan, level, x, z, Blocks.GRAVEL);
            }
        }
        for (int offset : new int[]{-28, -9, 10, 29}) {
            marketStall(plan, level, chunk, settlement.x() + offset, settlement.z() + 148);
        }
    }

    private static void addVillageSquare(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenRegionalSettlementCatalog.Settlement settlement) {
        for (int x = Math.max(settlement.x() - 12, chunk.getMinBlockX());
             x <= Math.min(settlement.x() + 12, chunk.getMinBlockX() + 15); x++) {
            for (int z = Math.max(settlement.z() - 12, chunk.getMinBlockZ());
                 z <= Math.min(settlement.z() + 12, chunk.getMinBlockZ() + 15); z++) {
                int y = surfaceY(x, z);
                Block surface = ((x + z) & 3) == 0 ? Blocks.STONE_BRICKS : Blocks.PACKED_MUD;
                plan.addSet(x, y, z, surface);
                plan.addFill(x, y + 1, z, x, y + 2, z, Blocks.AIR);
            }
        }
        int cx = settlement.x();
        int cz = settlement.z();
        int y = surfaceY(cx, cz);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            setIfInChunk(plan, chunk, cx + dx, y, cz + dz, rim ? Blocks.STONE_BRICKS : Blocks.WATER);
        }
        for (int[] offset : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            for (int dy = 1; dy <= 4; dy++) {
                setIfInChunk(plan, chunk, cx + offset[0], y + dy, cz + offset[1], Blocks.OAK_FENCE);
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            setIfInChunk(plan, chunk, cx + dx, y + 5, cz - 3, Blocks.SPRUCE_SLAB);
            setIfInChunk(plan, chunk, cx + dx, y + 5, cz + 3, Blocks.SPRUCE_SLAB);
        }
        setIfInChunk(plan, chunk, cx + 6, y + 1, cz, Blocks.STONE_BRICKS);
        setIfInChunk(plan, chunk, cx + 6, y + 2, cz, Blocks.STONE_BRICKS);
        setIfInChunk(plan, chunk, cx + 6, y + 3, cz, Blocks.STONE_BRICKS);
        setIfInChunk(plan, chunk, cx + 6, y + 4, cz, Blocks.BELL);
    }

    private static void addBuilding(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenRegionalSettlementCatalog.Settlement settlement,
            ErdenRegionalSettlementCatalog.BuildingLot lot) {
        UrbanFragmentSnapshot fragment = fragment(lot.style());
        Rotation rotation = rotationFor(fragment.exteriorSide(), lot.desiredFront());
        int width = rotatedWidth(fragment, rotation);
        int length = rotatedLength(fragment, rotation);
        int centerX = settlement.x() + lot.dx();
        int centerZ = settlement.z() + lot.dz();
        int originX = centerX - width / 2;
        int originZ = centerZ - length / 2;
        int baseY = surfaceY(centerX, centerZ);

        RotatedPoint entrance = rotate(fragment.entranceX(), fragment.entranceZ(),
                fragment.width(), fragment.length(), rotation);
        int doorX = originX + entrance.x;
        int doorZ = originZ + entrance.z;
        Side front = rotateSide(Side.valueOf(fragment.exteriorSide()), rotation);
        addAccessPath(plan, level, chunk, doorX, doorZ, front);

        Map<Long, Span> spans = new LinkedHashMap<>();
        List<PlacedBlock> placed = new ArrayList<>();
        for (UrbanSourceBlock block : fragment.blocks()) {
            RotatedPoint point = rotate(block.x(), block.z(), fragment.width(), fragment.length(), rotation);
            int x = originX + point.x;
            int z = originZ + point.z;
            if (!contains(chunk, x, z)) continue;
            int y = baseY + block.y();
            spans.merge(columnKey(x, z), new Span(x, z, y, y), Span::merge);
            placed.add(new PlacedBlock(x, y, z, block.state().rotate(rotation)));
        }
        if (placed.isEmpty()) return;

        for (Span span : spans.values()) {
            int originalSurface = plan.plannedSurfaceY(level, span.x, span.z);
            plan.addFill(span.x, span.minY, span.z,
                    span.x, Math.max(originalSurface, span.maxY + 2), span.z, Blocks.AIR);
            if (originalSurface < span.minY - 1) {
                plan.addFill(span.x, originalSurface + 1, span.z,
                        span.x, span.minY - 1, span.z, Blocks.STONE_BRICKS);
            }
            plan.addSet(span.x, span.minY - 1, span.z, Blocks.STONE_BRICKS);
        }
        for (PlacedBlock block : placed) plan.addSet(block.x, block.y, block.z, block.state);
    }

    private static void addAccessPath(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            int doorX,
            int doorZ,
            Side front) {
        int stepX = front == Side.EAST ? 1 : front == Side.WEST ? -1 : 0;
        int stepZ = front == Side.SOUTH ? 1 : front == Side.NORTH ? -1 : 0;
        int lateralX = stepZ;
        int lateralZ = stepX;
        for (int depth = 1; depth <= ACCESS_LIMIT; depth++) {
            int x = doorX + stepX * depth;
            int z = doorZ + stepZ * depth;
            for (int lane = -1; lane <= 1; lane++) {
                int px = x + lateralX * lane;
                int pz = z + lateralZ * lane;
                if (!contains(chunk, px, pz)) continue;
                surfacePath(plan, level, px, pz, Blocks.PACKED_MUD);
            }
        }
    }

    private static void surfacePath(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            int x,
            int z,
            Block surface) {
        int y = surfaceY(x, z);
        plan.addSet(x, y, z, surface);
        plan.addFill(x, y + 1, z, x, y + 2, z, Blocks.AIR);
    }

    private static void lanternPost(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            int x,
            int z) {
        if (!contains(chunk, x, z)) return;
        int y = surfaceY(x, z);
        plan.addSet(x, y, z, Blocks.STONE_BRICKS);
        plan.addSet(x, y + 1, z, Blocks.OAK_FENCE);
        plan.addSet(x, y + 2, z, Blocks.OAK_FENCE);
        plan.addSet(x, y + 3, z, Blocks.LANTERN);
    }

    private static void hayStack(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            int x,
            int z) {
        int y = surfaceY(x, z);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) + Math.abs(dz) > 3) continue;
            setIfInChunk(plan, chunk, x + dx, y + 1, z + dz, Blocks.HAY_BLOCK);
        }
        setIfInChunk(plan, chunk, x, y + 2, z, Blocks.HAY_BLOCK);
    }

    private static void waterTrough(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            int x,
            int z) {
        int y = surfaceY(x, z);
        for (int dx = -3; dx <= 3; dx++) {
            setIfInChunk(plan, chunk, x + dx, y + 1, z, Blocks.CAULDRON);
        }
    }

    private static void stockPile(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            int x,
            int y,
            int z,
            Block stock) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) + Math.abs(dz) > 3) continue;
            setIfInChunk(plan, chunk, x + dx, y + 1, z + dz, stock);
        }
        setIfInChunk(plan, chunk, x, y + 2, z, stock);
    }

    private static void marketStall(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            int x,
            int z) {
        int y = surfaceY(x, z);
        for (int dx = -4; dx <= 4; dx++) {
            setIfInChunk(plan, chunk, x + dx, y + 1, z, Blocks.SPRUCE_PLANKS);
            setIfInChunk(plan, chunk, x + dx, y + 5, z, Blocks.DARK_OAK_SLAB);
        }
        for (int dx : new int[]{-4, 4}) {
            for (int dy = 2; dy <= 4; dy++) {
                setIfInChunk(plan, chunk, x + dx, y + dy, z, Blocks.OAK_FENCE);
            }
        }
        setIfInChunk(plan, chunk, x, y + 2, z, Blocks.BARREL);
    }

    private static void setIfInChunk(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            int x,
            int y,
            int z,
            Block block) {
        if (contains(chunk, x, z)) plan.addSet(x, y, z, block);
    }

    private static Map<String, UrbanFragmentSnapshot> fragments() {
        Map<String, UrbanFragmentSnapshot> result = fragmentsByStyle;
        if (result != null) return result;
        synchronized (ErdenRegionalSettlementBuilder.class) {
            result = fragmentsByStyle;
            if (result != null) return result;
            List<UrbanFragmentSnapshot> snapshots = new ArrayList<>(
                    ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics().values());
            snapshots.sort(Comparator.comparing(UrbanFragmentSnapshot::fragmentKey));
            Map<String, UrbanFragmentSnapshot> selected = new HashMap<>();
            for (UrbanFragmentSnapshot snapshot : snapshots) {
                String resource = snapshot.resource();
                if (resource.endsWith("/all_in_one_house.schem")) selected.putIfAbsent("house", snapshot);
                else if (resource.endsWith("/medieval_manor.schem")) selected.putIfAbsent("manor", snapshot);
                else if (resource.endsWith("/player_castle_house.schem")) selected.putIfAbsent("player_castle", snapshot);
                else if (resource.endsWith("/medieval_tavern_inn.schem")) selected.putIfAbsent("tavern", snapshot);
                else if (resource.endsWith("/fantasy_castle_house.schem")) selected.putIfAbsent("castle", snapshot);
            }
            for (String style : List.of("house", "manor", "castle", "player_castle", "tavern")) {
                if (!selected.containsKey(style)) {
                    throw new IllegalStateException("Missing regional source fragment style " + style);
                }
            }
            result = Map.copyOf(selected);
            fragmentsByStyle = result;
            return result;
        }
    }

    private static UrbanFragmentSnapshot fragment(String style) {
        UrbanFragmentSnapshot result = fragments().get(style);
        if (result == null) throw new IllegalArgumentException("Unknown regional fragment style " + style);
        return result;
    }

    private static Rotation rotationFor(String sourceSide, String desiredSide) {
        Side source = Side.valueOf(sourceSide);
        Side desired = Side.valueOf(desiredSide.toUpperCase());
        for (Rotation rotation : Rotation.values()) {
            if (rotateSide(source, rotation) == desired) return rotation;
        }
        throw new IllegalStateException("Unable to rotate " + sourceSide + " to " + desiredSide);
    }

    private static Side rotateSide(Side side, Rotation rotation) {
        return switch (rotation) {
            case NONE -> side;
            case CLOCKWISE_90 -> switch (side) {
                case NORTH -> Side.EAST;
                case EAST -> Side.SOUTH;
                case SOUTH -> Side.WEST;
                case WEST -> Side.NORTH;
            };
            case CLOCKWISE_180 -> switch (side) {
                case NORTH -> Side.SOUTH;
                case SOUTH -> Side.NORTH;
                case EAST -> Side.WEST;
                case WEST -> Side.EAST;
            };
            case COUNTERCLOCKWISE_90 -> switch (side) {
                case NORTH -> Side.WEST;
                case WEST -> Side.SOUTH;
                case SOUTH -> Side.EAST;
                case EAST -> Side.NORTH;
            };
        };
    }

    private static RotatedPoint rotate(int x, int z, int width, int length, Rotation rotation) {
        return switch (rotation) {
            case NONE -> new RotatedPoint(x, z);
            case CLOCKWISE_90 -> new RotatedPoint(length - 1 - z, x);
            case CLOCKWISE_180 -> new RotatedPoint(width - 1 - x, length - 1 - z);
            case COUNTERCLOCKWISE_90 -> new RotatedPoint(z, width - 1 - x);
        };
    }

    private static int rotatedWidth(UrbanFragmentSnapshot fragment, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                ? fragment.length() : fragment.width();
    }

    private static int rotatedLength(UrbanFragmentSnapshot fragment, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                ? fragment.width() : fragment.length();
    }

    private static int surfaceY(int x, int z) {
        return (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
    }

    private static boolean intersects(
            ErdenRegionalSettlementCatalog.Settlement settlement,
            ChunkPos chunk) {
        int radius = ErdenRegionalSettlementCatalog.SETTLEMENT_RADIUS;
        return settlement.x() + radius >= chunk.getMinBlockX()
                && settlement.x() - radius <= chunk.getMinBlockX() + 15
                && settlement.z() + radius >= chunk.getMinBlockZ()
                && settlement.z() - radius <= chunk.getMinBlockZ() + 15;
    }

    private static boolean contains(ChunkPos chunk, int x, int z) {
        return x >= chunk.getMinBlockX() && x <= chunk.getMinBlockX() + 15
                && z >= chunk.getMinBlockZ() && z <= chunk.getMinBlockZ() + 15;
    }

    private static long columnKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private enum Side { NORTH, SOUTH, WEST, EAST }

    private record RotatedPoint(int x, int z) {
    }

    private record PlacedBlock(int x, int y, int z, BlockState state) {
    }

    private record Span(int x, int z, int minY, int maxY) {
        Span merge(Span other) {
            return new Span(x, z, Math.min(minY, other.minY), Math.max(maxY, other.maxY));
        }
    }
}
