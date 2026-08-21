package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Builds only the loaded 16x16 metre cells of Erden's authored national road network. */
public final class ErdenRegionalRoadBuilder {
    private static final double CARRIAGEWAY_RADIUS = 1.55D;
    private static final double SHOULDER_RADIUS = ErdenRegionalRoadNetwork.ROAD_HALF_WIDTH + 0.35D;
    private static final int CLEARANCE = 3;
    private static final int STATION_HALF_X = 9;
    private static final int STATION_HALF_Z = 7;

    private ErdenRegionalRoadBuilder() {
    }

    public static void addChunk(IncrementalWorldEditPlan plan, ServerLevel level, ChunkPos chunk) {
        addRoadSurface(plan, level, chunk);
        for (ErdenRegionalRoadNetwork.Waystation station : ErdenRegionalRoadNetwork.waystations()) {
            if (station.x() + STATION_HALF_X < chunk.getMinBlockX()
                    || station.x() - STATION_HALF_X > chunk.getMinBlockX() + 15
                    || station.z() + STATION_HALF_Z < chunk.getMinBlockZ()
                    || station.z() - STATION_HALF_Z > chunk.getMinBlockZ() + 15) continue;
            addWaystation(plan, level, chunk, station);
        }
    }

    public static int surfaceY(int x, int z) {
        return (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
    }

    private static void addRoadSurface(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk) {
        for (int x = chunk.getMinBlockX(); x <= chunk.getMinBlockX() + 15; x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMinBlockZ() + 15; z++) {
                double distance = ErdenRegionalRoadNetwork.distanceToRoad(x, z);
                if (distance > SHOULDER_RADIUS) continue;
                int y = surfaceY(x, z);
                BlockPos surface = new BlockPos(x, y, z);
                boolean wet = !level.getFluidState(surface).isEmpty()
                        || !level.getFluidState(surface.above()).isEmpty();
                Block block;
                if (wet) block = Blocks.STONE_BRICKS;
                else if (distance <= CARRIAGEWAY_RADIUS) block = Blocks.PACKED_MUD;
                else block = Blocks.GRAVEL;
                plan.addSet(x, y, z, block);
                plan.addFill(x, y + 1, z, x, y + CLEARANCE, z, Blocks.AIR);
                if (wet && Math.floorMod(x + z, 5) == 0) {
                    plan.addFill(x, Math.max(55, y - 5), z, x, y - 1, z, Blocks.COBBLESTONE);
                }
            }
        }
    }

    private static void addWaystation(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenRegionalRoadNetwork.Waystation station) {
        int baseY = surfaceY(station.x(), station.z());
        for (int x = Math.max(chunk.getMinBlockX(), station.x() - STATION_HALF_X);
             x <= Math.min(chunk.getMinBlockX() + 15, station.x() + STATION_HALF_X); x++) {
            for (int z = Math.max(chunk.getMinBlockZ(), station.z() - STATION_HALF_Z);
                 z <= Math.min(chunk.getMinBlockZ() + 15, station.z() + STATION_HALF_Z); z++) {
                int naturalY = surfaceY(x, z);
                int targetY = Math.max(naturalY - 2, Math.min(naturalY + 2, baseY));
                if (targetY > naturalY) {
                    plan.addFill(x, naturalY, z, x, targetY - 1, z, Blocks.DIRT);
                }
                plan.addSet(x, targetY, z,
                        Math.abs(z - station.z()) <= 2 ? Blocks.PACKED_MUD : Blocks.COARSE_DIRT);
                plan.addFill(x, targetY + 1, z, x, Math.max(targetY + 4, naturalY + 3), z, Blocks.AIR);
            }
        }

        // Open roadside shelter: roof + posts, never a sealed box that blocks the carriageway.
        int shelterX = station.x() + 5;
        int shelterZ = station.z() - 4;
        int shelterY = surfaceY(shelterX, shelterZ);
        for (int dx : new int[]{-3, 3}) {
            for (int dz : new int[]{-2, 2}) {
                for (int dy = 1; dy <= 4; dy++) {
                    setIfInChunk(plan, chunk, shelterX + dx, shelterY + dy, shelterZ + dz, Blocks.SPRUCE_LOG);
                }
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                setIfInChunk(plan, chunk, shelterX + dx, shelterY + 5, shelterZ + dz, Blocks.SPRUCE_SLAB);
            }
        }
        setIfInChunk(plan, chunk, shelterX, shelterY + 1, shelterZ, Blocks.BARREL);
        setIfInChunk(plan, chunk, shelterX - 2, shelterY + 1, shelterZ, Blocks.HAY_BLOCK);
        setIfInChunk(plan, chunk, shelterX + 2, shelterY + 1, shelterZ, Blocks.CRAFTING_TABLE);
        setIfInChunk(plan, chunk, shelterX, shelterY + 4, shelterZ, Blocks.LANTERN);

        // Hitching rail on the opposite side keeps the main five-to-seven metre road clear.
        int railX = station.x() - 6;
        int railZ = station.z() + 4;
        int railY = surfaceY(railX, railZ);
        for (int dx = -2; dx <= 2; dx++) {
            setIfInChunk(plan, chunk, railX + dx, railY + 1, railZ, Blocks.OAK_FENCE);
        }
        setIfInChunk(plan, chunk, railX - 2, railY + 2, railZ, Blocks.LANTERN);
        setIfInChunk(plan, chunk, railX + 2, railY + 2, railZ, Blocks.LANTERN);
    }

    private static void setIfInChunk(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            int x,
            int y,
            int z,
            Block block) {
        if (x < chunk.getMinBlockX() || x > chunk.getMinBlockX() + 15
                || z < chunk.getMinBlockZ() || z > chunk.getMinBlockZ() + 15) return;
        plan.addSet(x, y, z, block);
    }
}
