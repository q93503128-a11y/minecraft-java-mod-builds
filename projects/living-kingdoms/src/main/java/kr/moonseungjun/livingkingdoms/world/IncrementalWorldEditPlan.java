package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ordered, compact block-edit plan that can be applied over many server ticks. */
public final class IncrementalWorldEditPlan {
    private static final ThreadLocal<IncrementalWorldEditPlan> ACTIVE = new ThreadLocal<>();
    private static final int CONSTRUCTION_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;
    private static final int LEGACY_TERRAIN_COLUMN_HEIGHT = 8;

    private final List<Operation> operations = new ArrayList<>();
    /** Original generator surface, sampled once per column while the plan is assembled. */
    private final Map<Long, Integer> originalSurfaceHeights = new HashMap<>();
    /** Surface that earlier operations in this same plan have already promised to create. */
    private final Map<Long, Integer> plannedSurfaceHeights = new HashMap<>();
    /** Surface cap belonging to a discarded legacy cut/fill column. */
    private final Map<Long, Integer> suppressedTerrainCaps = new HashMap<>();
    private int operationIndex;
    private long estimatedWrites;
    private long appliedWrites;
    private long suppressedTerrainWrites;

    public static Scope activate(IncrementalWorldEditPlan plan) {
        if (ACTIVE.get() != null) throw new IllegalStateException("Nested world-edit planning is not supported");
        ACTIVE.set(plan);
        return new Scope();
    }

    public static IncrementalWorldEditPlan active() {
        IncrementalWorldEditPlan plan = ACTIVE.get();
        if (plan == null) throw new IllegalStateException("No active incremental world-edit plan");
        return plan;
    }

    public int originalSurfaceY(ServerLevel level, int x, int z) {
        long key = columnKey(x, z);
        return originalSurfaceHeights.computeIfAbsent(key, ignored -> RealmSitePlanner.surfaceY(level, x, z));
    }

    public int plannedSurfaceY(ServerLevel level, int x, int z) {
        long key = columnKey(x, z);
        return plannedSurfaceHeights.computeIfAbsent(key, ignored -> originalSurfaceY(level, x, z));
    }

    public void setPlannedSurfaceY(int x, int z, int y) {
        plannedSurfaceHeights.put(columnKey(x, z), y);
    }

    public int sampledColumnCount() {
        return originalSurfaceHeights.size();
    }

    public long suppressedTerrainWrites() {
        return suppressedTerrainWrites;
    }

    public void addSet(int x, int y, int z, Block block) {
        long key = columnKey(x, z);
        Integer cap = suppressedTerrainCaps.get(key);
        if (cap != null && cap == y && isTerrainSurface(block)) {
            suppressedTerrainCaps.remove(key);
            suppressedTerrainWrites++;
            return;
        }
        operations.add(new SetOperation(x, y, z, block));
        estimatedWrites++;
    }

    public void addFill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        if (y2 < y1) return;
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        long writes = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);

        // The old capital builder flattened an enormous rectangle by scheduling one tall cut/fill
        // column for every x/z coordinate. The authored generator now creates the capital plateau,
        // so replaying those columns only carves cliffs into correct terrain. Small three-block road
        // clearances, canals, rooms, foundations, trees and all multi-column structures remain intact.
        if (minX == maxX && minZ == maxZ
                && maxY - minY + 1 >= LEGACY_TERRAIN_COLUMN_HEIGHT
                && isLegacyTerrainColumnMaterial(block)) {
            int surfaceCap = block == Blocks.AIR ? minY - 1 : maxY + 1;
            suppressedTerrainCaps.put(columnKey(minX, minZ), surfaceCap);
            suppressedTerrainWrites += writes;
            return;
        }

        operations.add(new BoxOperation(minX, minY, minZ, maxX, maxY, maxZ, block));
        estimatedWrites += writes;
    }

    public int apply(ServerLevel level, int budget) {
        int used = 0;
        while (operationIndex < operations.size() && used < budget) {
            Operation operation = operations.get(operationIndex);
            int consumed = operation.apply(level, budget - used);
            used += consumed;
            appliedWrites += consumed;
            if (operation.done()) operationIndex++;
            else break;
        }
        return used;
    }

    public boolean done() {
        return operationIndex >= operations.size();
    }

    public long estimatedWrites() {
        return estimatedWrites;
    }

    public long appliedWrites() {
        return appliedWrites;
    }

    public int operationCount() {
        return operations.size();
    }

    public float progress() {
        return estimatedWrites == 0L ? 1.0F : Math.min(1.0F, appliedWrites / (float) estimatedWrites);
    }

    private static boolean isLegacyTerrainColumnMaterial(Block block) {
        return block == Blocks.AIR
                || block == Blocks.DIRT
                || block == Blocks.COARSE_DIRT
                || block == Blocks.ROOTED_DIRT
                || block == Blocks.STONE
                || block == Blocks.MOSS_BLOCK;
    }

    private static boolean isTerrainSurface(Block block) {
        return block == Blocks.GRASS_BLOCK
                || block == Blocks.DIRT
                || block == Blocks.COARSE_DIRT
                || block == Blocks.ROOTED_DIRT
                || block == Blocks.STONE
                || block == Blocks.MOSS_BLOCK
                || block == Blocks.PODZOL
                || block == Blocks.MYCELIUM
                || block == Blocks.SAND;
    }

    private static long columnKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    public static final class Scope implements AutoCloseable {
        private boolean closed;

        private Scope() {
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            ACTIVE.remove();
        }
    }

    private interface Operation {
        int apply(ServerLevel level, int budget);
        boolean done();
    }

    private static final class SetOperation implements Operation {
        private final int x;
        private final int y;
        private final int z;
        private final Block block;
        private boolean done;

        private SetOperation(int x, int y, int z, Block block) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
        }

        @Override
        public int apply(ServerLevel level, int budget) {
            if (done || budget <= 0) return 0;
            write(level, x, y, z, block);
            done = true;
            return 1;
        }

        @Override
        public boolean done() {
            return done;
        }
    }

    private static final class BoxOperation implements Operation {
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int maxX;
        private final int maxY;
        private final int maxZ;
        private final Block block;
        private int x;
        private int y;
        private int z;
        private boolean done;

        private BoxOperation(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Block block) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.block = block;
            this.x = minX;
            this.y = minY;
            this.z = minZ;
        }

        @Override
        public int apply(ServerLevel level, int budget) {
            int used = 0;
            while (!done && used < budget) {
                write(level, x, y, z, block);
                used++;
                advance();
            }
            return used;
        }

        private void advance() {
            z++;
            if (z <= maxZ) return;
            z = minZ;
            y++;
            if (y <= maxY) return;
            y = minY;
            x++;
            if (x > maxX) done = true;
        }

        @Override
        public boolean done() {
            return done;
        }
    }

    private static void write(ServerLevel level, int x, int y, int z, Block block) {
        if (y < level.getMinY() || y >= level.getMaxY()) return;
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).getBlock() == block) return;
        level.setBlock(pos, block.defaultBlockState(), CONSTRUCTION_UPDATE_FLAGS);
    }
}
