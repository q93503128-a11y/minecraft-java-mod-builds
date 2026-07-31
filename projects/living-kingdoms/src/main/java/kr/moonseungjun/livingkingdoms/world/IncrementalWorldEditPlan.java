package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/** Ordered, compact block-edit plan that can be applied over many server ticks. */
public final class IncrementalWorldEditPlan {
    private static final ThreadLocal<IncrementalWorldEditPlan> ACTIVE = new ThreadLocal<>();

    private final List<Operation> operations = new ArrayList<>();
    private int operationIndex;
    private long estimatedWrites;
    private long appliedWrites;

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

    public void addSet(int x, int y, int z, Block block) {
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
        operations.add(new BoxOperation(minX, minY, minZ, maxX, maxY, maxZ, block));
        estimatedWrites += (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
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
        level.setBlock(pos, block.defaultBlockState(), 2);
    }
}
