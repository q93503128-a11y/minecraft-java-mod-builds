package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ordered, compact block-state edit plan that can be applied over many server ticks. */
public final class IncrementalWorldEditPlan {
    private static final ThreadLocal<IncrementalWorldEditPlan> ACTIVE = new ThreadLocal<>();
    private static final int CONSTRUCTION_UPDATE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final long MAX_APPLY_NANOS = 12_000_000L;

    private final boolean chunkBounded;
    private final int boundMinX;
    private final int boundMaxX;
    private final int boundMinZ;
    private final int boundMaxZ;

    private final List<Operation> operations = new ArrayList<>();
    private final Set<Long> retainedConstructionChunks = new HashSet<>();
    private final Map<Long, Integer> originalSurfaceHeights = new HashMap<>();
    private final Map<Long, Integer> plannedSurfaceHeights = new HashMap<>();
    private PendingTerrainColumn pendingTerrainColumn;
    private int operationIndex;
    private long estimatedWrites;
    private long appliedWrites;
    private long suppressedTerrainWrites;
    private long suppressedOutOfBoundsWrites;

    public IncrementalWorldEditPlan() {
        this.chunkBounded = false;
        this.boundMinX = Integer.MIN_VALUE;
        this.boundMaxX = Integer.MAX_VALUE;
        this.boundMinZ = Integer.MIN_VALUE;
        this.boundMaxZ = Integer.MAX_VALUE;
    }

    public IncrementalWorldEditPlan(ChunkPos chunk) {
        this.chunkBounded = true;
        this.boundMinX = chunk.getMinBlockX();
        this.boundMaxX = this.boundMinX + 15;
        this.boundMinZ = chunk.getMinBlockZ();
        this.boundMaxZ = this.boundMinZ + 15;
    }

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
        return originalSurfaceHeights.computeIfAbsent(
                key,
                ignored -> (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z)));
    }

    public int plannedSurfaceY(ServerLevel level, int x, int z) {
        long key = columnKey(x, z);
        return plannedSurfaceHeights.computeIfAbsent(key, ignored -> originalSurfaceY(level, x, z));
    }

    public void setPlannedSurfaceY(int x, int z, int y) {
        plannedSurfaceHeights.put(columnKey(x, z), y);
    }

    public int sampledColumnCount() { return originalSurfaceHeights.size(); }
    public long suppressedTerrainWrites() { return suppressedTerrainWrites; }
    public long suppressedOutOfBoundsWrites() { return suppressedOutOfBoundsWrites; }

    public void addSet(int x, int y, int z, Block block) {
        addSet(x, y, z, block.defaultBlockState());
    }

    public void addSet(int x, int y, int z, BlockState state) {
        if (!insideScope(x, z)) {
            suppressedOutOfBoundsWrites++;
            return;
        }
        long key = columnKey(x, z);
        if (pendingTerrainColumn != null) {
            if (pendingTerrainColumn.key == key
                    && pendingTerrainColumn.surfaceY == y
                    && isTerrainSurface(state.getBlock())) {
                suppressedTerrainWrites += pendingTerrainColumn.writes + 1L;
                pendingTerrainColumn = null;
                return;
            }
            flushPendingTerrainColumn();
        }
        operations.add(new SetOperation(x, y, z, state));
        estimatedWrites++;
    }

    public void addFill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        addFill(x1, y1, z1, x2, y2, z2, block.defaultBlockState());
    }

    public void addFill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        if (y2 < y1) return;
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        long requestedWrites = (long) (maxX - minX + 1)
                * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (chunkBounded) {
            minX = Math.max(minX, boundMinX);
            maxX = Math.min(maxX, boundMaxX);
            minZ = Math.max(minZ, boundMinZ);
            maxZ = Math.min(maxZ, boundMaxZ);
            if (minX > maxX || minZ > maxZ) {
                suppressedOutOfBoundsWrites += requestedWrites;
                return;
            }
        }
        long writes = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        suppressedOutOfBoundsWrites += requestedWrites - writes;

        flushPendingTerrainColumn();
        Block block = state.getBlock();
        long key = columnKey(minX, minZ);
        if (state == block.defaultBlockState()
                && minX == maxX && minZ == maxZ
                && originalSurfaceHeights.containsKey(key)
                && isLegacyTerrainColumnMaterial(block)) {
            int surfaceY = block == Blocks.AIR ? minY - 1 : maxY + 1;
            pendingTerrainColumn = new PendingTerrainColumn(
                    key, surfaceY, minX, minY, minZ, maxX, maxY, maxZ, state, writes
            );
            return;
        }
        addBoxOperation(minX, minY, minZ, maxX, maxY, maxZ, state, writes);
    }

    public int apply(ServerLevel level, int budget) {
        flushPendingTerrainColumn();
        int used = 0;
        long deadline = System.nanoTime() + MAX_APPLY_NANOS;
        try {
            while (operationIndex < operations.size() && used < budget) {
                if (used > 0 && System.nanoTime() >= deadline) break;
                Operation operation = operations.get(operationIndex);
                int consumed = operation.apply(this, level, budget - used, deadline);
                if (consumed <= 0) break;
                used += consumed;
                appliedWrites += consumed;
                if (operation.done()) operationIndex++;
                else break;
            }
            if (operationIndex >= operations.size()) releaseRetainedConstructionChunks(level);
            return used;
        } catch (RuntimeException | Error failure) {
            releaseRetainedConstructionChunks(level);
            throw failure;
        }
    }

    public boolean done() {
        flushPendingTerrainColumn();
        return operationIndex >= operations.size();
    }

    public long estimatedWrites() { flushPendingTerrainColumn(); return estimatedWrites; }
    public long appliedWrites() { return appliedWrites; }
    public int operationCount() { flushPendingTerrainColumn(); return operations.size(); }
    public float progress() {
        flushPendingTerrainColumn();
        return estimatedWrites == 0L ? 1.0F : Math.min(1.0F, appliedWrites / (float) estimatedWrites);
    }

    private void flushPendingTerrainColumn() {
        if (pendingTerrainColumn == null) return;
        PendingTerrainColumn pending = pendingTerrainColumn;
        pendingTerrainColumn = null;
        addBoxOperation(pending.minX, pending.minY, pending.minZ,
                pending.maxX, pending.maxY, pending.maxZ, pending.state, pending.writes);
    }

    private void addBoxOperation(int minX, int minY, int minZ,
                                 int maxX, int maxY, int maxZ,
                                 BlockState state, long writes) {
        operations.add(new BoxOperation(minX, minY, minZ, maxX, maxY, maxZ, state));
        estimatedWrites += writes;
    }

    private static boolean isLegacyTerrainColumnMaterial(Block block) {
        return block == Blocks.AIR || block == Blocks.DIRT || block == Blocks.COARSE_DIRT
                || block == Blocks.ROOTED_DIRT || block == Blocks.STONE || block == Blocks.MOSS_BLOCK;
    }

    private static boolean isTerrainSurface(Block block) {
        return block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == Blocks.COARSE_DIRT
                || block == Blocks.ROOTED_DIRT || block == Blocks.STONE || block == Blocks.MOSS_BLOCK
                || block == Blocks.PODZOL || block == Blocks.MYCELIUM || block == Blocks.SAND;
    }

    private boolean insideScope(int x, int z) {
        return !chunkBounded
                || (x >= boundMinX && x <= boundMaxX && z >= boundMinZ && z <= boundMaxZ);
    }

    private static long columnKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    public static final class Scope implements AutoCloseable {
        private boolean closed;
        private Scope() {}
        @Override public void close() {
            if (closed) return;
            closed = true;
            ACTIVE.remove();
        }
    }

    private interface Operation {
        int apply(IncrementalWorldEditPlan owner, ServerLevel level, int budget, long deadline);
        boolean done();
    }

    private static final class SetOperation implements Operation {
        private final int x, y, z;
        private final BlockState state;
        private boolean done;
        private SetOperation(int x, int y, int z, BlockState state) {
            this.x = x; this.y = y; this.z = z; this.state = state;
        }
        @Override public int apply(IncrementalWorldEditPlan owner, ServerLevel level, int budget, long deadline) {
            if (done || budget <= 0 || System.nanoTime() >= deadline) return 0;
            if (!owner.write(level, x, y, z, state)) return 0;
            done = true;
            return 1;
        }
        @Override public boolean done() { return done; }
    }

    private static final class BoxOperation implements Operation {
        private final int minX, minY, minZ, maxX, maxY, maxZ;
        private final BlockState state;
        private int x, y, z;
        private boolean done;
        private BoxOperation(int minX, int minY, int minZ,
                             int maxX, int maxY, int maxZ, BlockState state) {
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
            this.state = state; this.x = minX; this.y = minY; this.z = minZ;
        }
        @Override public int apply(IncrementalWorldEditPlan owner, ServerLevel level, int budget, long deadline) {
            int used = 0;
            while (!done && used < budget
                    && (used == 0 || System.nanoTime() < deadline)) {
                if (!owner.write(level, x, y, z, state)) break;
                used++;
                advance();
            }
            return used;
        }
        private void advance() {
            z++;
            if (z <= maxZ) return;
            z = minZ; y++;
            if (y <= maxY) return;
            y = minY; x++;
            if (x > maxX) done = true;
        }
        @Override public boolean done() { return done; }
    }

    private boolean write(ServerLevel level, int x, int y, int z, BlockState state) {
        if (y < level.getMinY() || y >= level.getMaxY()) return true;
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (!level.hasChunk(chunkX, chunkZ)) {
            if (!chunkBounded) retainConstructionChunk(level, chunkX, chunkZ);
            return false;
        }
        // hasChunk() can be true while the FULL LevelChunk is still being promoted/generated.
        // Never let streamed construction synchronously wait for that promotion on the server tick.
        LevelChunk loadedChunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (loadedChunk == null) {
            if (!chunkBounded) retainConstructionChunk(level, chunkX, chunkZ);
            return false;
        }
        BlockPos pos = new BlockPos(x, y, z);
        if (loadedChunk.getBlockState(pos).equals(state)) return true;
        // External structures can carry stale pending block-entity NBT after cleanup changed
        // the corresponding block to air. Remove that pending/ticking entry before replacing
        // the state so LevelChunk never tries to instantiate (for example) a beehive on air.
        loadedChunk.removeBlockEntity(pos);
        level.setBlock(pos, state, CONSTRUCTION_UPDATE_FLAGS);
        return true;
    }

    private void retainConstructionChunk(ServerLevel level, int chunkX, int chunkZ) {
        long packed = columnKey(chunkX, chunkZ);
        if (!retainedConstructionChunks.add(packed)) return;
        level.getChunkSource().addTicketAndLoadWithRadius(
                TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
    }

    private void releaseRetainedConstructionChunks(ServerLevel level) {
        if (retainedConstructionChunks.isEmpty()) return;
        for (long packed : Set.copyOf(retainedConstructionChunks)) {
            level.getChunkSource().removeTicketWithRadius(
                    TicketType.PORTAL,
                    new ChunkPos((int) (packed >> 32), (int) packed),
                    0);
        }
        retainedConstructionChunks.clear();
    }

    private record PendingTerrainColumn(long key, int surfaceY,
                                        int minX, int minY, int minZ,
                                        int maxX, int maxY, int maxZ,
                                        BlockState state, long writes) {}
}
