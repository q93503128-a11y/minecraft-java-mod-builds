package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/** Adds terrain-following access paths, drainage and civic water infrastructure to streamed capital cells. */
public final class ErdenUrbanInfrastructureBuilder {
    public static final int DIAGNOSTIC_WELL_X = -280;
    public static final int DIAGNOSTIC_WELL_Z = -280;
    public static final int DIAGNOSTIC_CISTERN_X = -580;
    public static final int DIAGNOSTIC_CISTERN_Z = -280;

    private static final int FINALIZE_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;
    private static final List<ServiceNode> SERVICE_NODES = List.of(
            new ServiceNode(-280, -280, ServiceType.WELL),
            new ServiceNode(280, -280, ServiceType.WELL),
            new ServiceNode(-280, 280, ServiceType.WELL),
            new ServiceNode(280, 280, ServiceType.WELL),
            new ServiceNode(-880, -280, ServiceType.WELL),
            new ServiceNode(880, -280, ServiceType.WELL),
            new ServiceNode(-880, 280, ServiceType.WELL),
            new ServiceNode(880, 280, ServiceType.WELL),
            new ServiceNode(-280, -680, ServiceType.WELL),
            new ServiceNode(280, -680, ServiceType.WELL),
            new ServiceNode(-280, 680, ServiceType.WELL),
            new ServiceNode(280, 680, ServiceType.WELL),

            new ServiceNode(-580, -280, ServiceType.FIRE_CISTERN),
            new ServiceNode(580, -280, ServiceType.FIRE_CISTERN),
            new ServiceNode(-580, 280, ServiceType.FIRE_CISTERN),
            new ServiceNode(580, 280, ServiceType.FIRE_CISTERN),
            new ServiceNode(-1080, -280, ServiceType.FIRE_CISTERN),
            new ServiceNode(1080, -280, ServiceType.FIRE_CISTERN),
            new ServiceNode(-1080, 280, ServiceType.FIRE_CISTERN),
            new ServiceNode(1080, 280, ServiceType.FIRE_CISTERN)
    );

    private ErdenUrbanInfrastructureBuilder() {
    }

    public static void addChunk(IncrementalWorldEditPlan plan, ServerLevel level, ChunkPos chunk) {
        addRoadDrainage(plan, level, chunk);
        addBuildingAccess(plan, level, chunk);
        for (ServiceNode node : SERVICE_NODES) {
            if (!node.intersects(chunk)) continue;
            if (node.type == ServiceType.WELL) placeWell(plan, chunk, node);
            else placeFireCistern(plan, chunk, node);
        }
    }

    /**
     * Reasserts the stable centre of civic water facilities after every other edit in this cell has
     * completed. This uses the authored terrain height rather than a heightmap that may already
     * include a neighbouring external facade.
     */
    public static void finalizeChunk(ServerLevel level, ChunkPos chunk) {
        for (ServiceNode node : SERVICE_NODES) {
            if (!contains(chunk, node.x, node.z)) continue;
            int baseY = serviceBaseY(node);
            setNow(level, node.x, baseY - 1, node.z, Blocks.STONE_BRICKS);
            setNow(level, node.x, baseY, node.z, Blocks.WATER);
            setNow(level, node.x, baseY + 1, node.z, Blocks.AIR);
            setNow(level, node.x, baseY + 2, node.z, Blocks.AIR);
        }
    }

    public static int serviceNodeCount() {
        return SERVICE_NODES.size();
    }

    private static void addRoadDrainage(IncrementalWorldEditPlan plan, ServerLevel level, ChunkPos chunk) {
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        for (int x = minX; x <= minX + 15; x++) {
            for (int z = minZ; z <= minZ + 15; z++) {
                ErdenCapitalStreamingBuilder.RoadClass road =
                        ErdenCapitalStreamingBuilder.roadClassAt(x, z);
                if (road == ErdenCapitalStreamingBuilder.RoadClass.NONE) continue;
                int surfaceY = RealmSitePlanner.surfaceY(level, x, z);

                if (isRoadEdge(x, z)) {
                    plan.addSet(x, surfaceY, z, Blocks.STONE_BRICK_SLAB);
                    if (Math.floorMod(x * 31 + z * 17, 13) == 0) {
                        plan.addSet(x, surfaceY, z, Blocks.IRON_TRAPDOOR);
                        plan.addSet(x, surfaceY - 1, z, Blocks.AIR);
                        plan.addSet(x, surfaceY - 2, z, Blocks.WATER);
                        plan.addSet(x, surfaceY - 3, z, Blocks.STONE_BRICKS);
                    }
                }

                if (z == 0) addRoyalCulvertCell(plan, level, chunk, x, z, true);
                if (x == 0) addRoyalCulvertCell(plan, level, chunk, x, z, false);
            }
        }
    }

    private static boolean isRoadEdge(int x, int z) {
        return ErdenCapitalStreamingBuilder.roadClassAt(x + 1, z)
                        == ErdenCapitalStreamingBuilder.RoadClass.NONE
                || ErdenCapitalStreamingBuilder.roadClassAt(x - 1, z)
                        == ErdenCapitalStreamingBuilder.RoadClass.NONE
                || ErdenCapitalStreamingBuilder.roadClassAt(x, z + 1)
                        == ErdenCapitalStreamingBuilder.RoadClass.NONE
                || ErdenCapitalStreamingBuilder.roadClassAt(x, z - 1)
                        == ErdenCapitalStreamingBuilder.RoadClass.NONE;
    }

    private static void addRoyalCulvertCell(IncrementalWorldEditPlan plan, ServerLevel level,
                                            ChunkPos chunk, int x, int z, boolean eastWest) {
        int surfaceY = RealmSitePlanner.surfaceY(level, x, z);
        setClipped(plan, chunk, x, surfaceY - 4, z, Blocks.STONE_BRICKS);
        setClipped(plan, chunk, x, surfaceY - 3, z, Blocks.WATER);
        setClipped(plan, chunk, x, surfaceY - 2, z, Blocks.AIR);
        setClipped(plan, chunk, x, surfaceY - 1, z, Blocks.STONE_BRICKS);
        if (eastWest) {
            for (int side : new int[]{-1, 1}) {
                setClipped(plan, chunk, x, surfaceY - 3, z + side, Blocks.STONE_BRICKS);
                setClipped(plan, chunk, x, surfaceY - 2, z + side, Blocks.STONE_BRICKS);
            }
        } else {
            for (int side : new int[]{-1, 1}) {
                setClipped(plan, chunk, x + side, surfaceY - 3, z, Blocks.STONE_BRICKS);
                setClipped(plan, chunk, x + side, surfaceY - 2, z, Blocks.STONE_BRICKS);
            }
        }
    }

    private static void addBuildingAccess(IncrementalWorldEditPlan plan, ServerLevel level,
                                          ChunkPos chunk) {
        for (ExternalDistrictBuildingBuilder.BuildingEntrance entrance
                : ExternalDistrictBuildingBuilder.entrances()) {
            if (!segmentIntersects(chunk, entrance.x(), entrance.z(), entrance.roadX(), entrance.roadZ(), 2)) {
                continue;
            }
            addAccessPath(plan, level, chunk, entrance);
        }
    }

    private static void addAccessPath(IncrementalWorldEditPlan plan, ServerLevel level,
                                      ChunkPos chunk,
                                      ExternalDistrictBuildingBuilder.BuildingEntrance entrance) {
        int deltaX = entrance.roadX() - entrance.x();
        int deltaZ = entrance.roadZ() - entrance.z();
        int steps = Math.max(Math.abs(deltaX), Math.abs(deltaZ));
        if (steps == 0) return;
        boolean eastWest = Math.abs(deltaX) >= Math.abs(deltaZ);
        Block material = entrance.residential() ? Blocks.PACKED_MUD : Blocks.STONE_BRICKS;
        for (int step = 0; step <= steps; step++) {
            // The threshold reconciler owns the doorway itself and the first two metres of authored
            // porch. Never pave those cells from terrain height: some imported doors begin at the
            // template's minimum Y, so a step-0 surface write can literally replace the door block.
            if (step <= 2) continue;
            int centerX = entrance.x() + Math.round(deltaX * (step / (float) steps));
            int centerZ = entrance.z() + Math.round(deltaZ * (step / (float) steps));
            for (int width = -1; width <= 1; width++) {
                int x = eastWest ? centerX : centerX + width;
                int z = eastWest ? centerZ + width : centerZ;
                if (!contains(chunk, x, z)) continue;
                int surfaceY = RealmSitePlanner.surfaceY(level, x, z);
                plan.addSet(x, surfaceY, z, material);
                plan.addFill(x, surfaceY + 1, z, x, surfaceY + 3, z, Blocks.AIR);
            }
        }
    }

    private static void placeWell(IncrementalWorldEditPlan plan,
                                  ChunkPos chunk, ServiceNode node) {
        int baseY = serviceBaseY(node);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int x = node.x + dx;
                int z = node.z + dz;
                if (!contains(chunk, x, z)) continue;
                boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                plan.addSet(x, baseY, z, rim ? Blocks.STONE_BRICKS : Blocks.WATER);
                plan.addFill(x, baseY + 1, z, x, baseY + 3, z, Blocks.AIR);
                if (rim) plan.addSet(x, baseY - 1, z, Blocks.STONE_BRICKS);
            }
        }
        for (int dx : new int[]{-2, 2}) {
            for (int dz : new int[]{-2, 2}) {
                for (int y = baseY + 1; y <= baseY + 3; y++) {
                    setClipped(plan, chunk, node.x + dx, y, node.z + dz, Blocks.SPRUCE_FENCE);
                }
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    setClipped(plan, chunk, node.x + dx, baseY + 4, node.z + dz, Blocks.DARK_OAK_SLAB);
                }
            }
        }
        setClipped(plan, chunk, node.x, baseY + 3, node.z, Blocks.LANTERN);
    }

    private static void placeFireCistern(IncrementalWorldEditPlan plan,
                                         ChunkPos chunk, ServiceNode node) {
        int baseY = serviceBaseY(node);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int x = node.x + dx;
                int z = node.z + dz;
                if (!contains(chunk, x, z)) continue;
                boolean wall = Math.abs(dx) == 3 || Math.abs(dz) == 2;
                plan.addSet(x, baseY - 1, z, Blocks.STONE_BRICKS);
                plan.addSet(x, baseY, z, wall ? Blocks.STONE_BRICKS : Blocks.WATER);
                plan.addFill(x, baseY + 1, z, x, baseY + 2, z, Blocks.AIR);
            }
        }
        setClipped(plan, chunk, node.x - 4, baseY, node.z - 1, Blocks.CAULDRON);
        setClipped(plan, chunk, node.x - 4, baseY, node.z + 1, Blocks.CAULDRON);
        setClipped(plan, chunk, node.x + 4, baseY, node.z - 1, Blocks.BARREL);
        setClipped(plan, chunk, node.x + 4, baseY, node.z + 1, Blocks.BARREL);
        setClipped(plan, chunk, node.x, baseY + 1, node.z - 3, Blocks.SPRUCE_FENCE);
        setClipped(plan, chunk, node.x, baseY + 2, node.z - 3, Blocks.BELL);
    }

    private static int serviceBaseY(ServiceNode node) {
        return (int) Math.round(AuthoredContinentDensity.surfaceHeight(node.x, node.z));
    }

    private static boolean segmentIntersects(ChunkPos chunk, int x1, int z1, int x2, int z2, int margin) {
        int minX = Math.min(x1, x2) - margin;
        int maxX = Math.max(x1, x2) + margin;
        int minZ = Math.min(z1, z2) - margin;
        int maxZ = Math.max(z1, z2) + margin;
        return maxX >= chunk.getMinBlockX() && minX <= chunk.getMinBlockX() + 15
                && maxZ >= chunk.getMinBlockZ() && minZ <= chunk.getMinBlockZ() + 15;
    }

    private static void setClipped(IncrementalWorldEditPlan plan, ChunkPos chunk,
                                   int x, int y, int z, Block block) {
        if (contains(chunk, x, z)) plan.addSet(x, y, z, block);
    }

    private static void setNow(ServerLevel level, int x, int y, int z, Block block) {
        if (y < level.getMinY() || y >= level.getMaxY()) return;
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).is(block)) return;
        level.setBlock(pos, block.defaultBlockState(), FINALIZE_UPDATE_FLAGS);
    }

    private static boolean contains(ChunkPos chunk, int x, int z) {
        return x >= chunk.getMinBlockX() && x <= chunk.getMinBlockX() + 15
                && z >= chunk.getMinBlockZ() && z <= chunk.getMinBlockZ() + 15;
    }

    private enum ServiceType {
        WELL,
        FIRE_CISTERN
    }

    private record ServiceNode(int x, int z, ServiceType type) {
        int radius() {
            return type == ServiceType.WELL ? 3 : 5;
        }

        boolean intersects(ChunkPos chunk) {
            int radius = radius();
            return x + radius >= chunk.getMinBlockX() && x - radius <= chunk.getMinBlockX() + 15
                    && z + radius >= chunk.getMinBlockZ() && z - radius <= chunk.getMinBlockZ() + 15;
        }
    }
}
