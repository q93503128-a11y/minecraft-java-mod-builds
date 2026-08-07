from pathlib import Path

root = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world')
plan_path = root / 'IncrementalWorldEditPlan.java'
builder_path = root / 'ErdenKingdomExteriorBuilder.java'

text = plan_path.read_text(encoding='utf-8')

if 'import net.minecraft.world.level.ChunkPos;\n' not in text:
    text = text.replace(
        'import net.minecraft.server.level.ServerLevel;\n',
        'import net.minecraft.server.level.ServerLevel;\nimport net.minecraft.world.level.ChunkPos;\n', 1)

field_anchor = '''    private static final long MAX_APPLY_NANOS = 40_000_000L;\n\n'''
field_insert = '''    private static final long MAX_APPLY_NANOS = 40_000_000L;\n\n    private final boolean chunkBounded;\n    private final int boundMinX;\n    private final int boundMaxX;\n    private final int boundMinZ;\n    private final int boundMaxZ;\n\n'''
if 'private final boolean chunkBounded;' not in text:
    if field_anchor not in text:
        raise SystemExit('plan field insertion point missing')
    text = text.replace(field_anchor, field_insert, 1)

counter_anchor = '''    private long suppressedTerrainWrites;\n\n'''
counter_insert = '''    private long suppressedTerrainWrites;\n    private long suppressedOutOfBoundsWrites;\n\n    public IncrementalWorldEditPlan() {\n        this.chunkBounded = false;\n        this.boundMinX = Integer.MIN_VALUE;\n        this.boundMaxX = Integer.MAX_VALUE;\n        this.boundMinZ = Integer.MIN_VALUE;\n        this.boundMaxZ = Integer.MAX_VALUE;\n    }\n\n    public IncrementalWorldEditPlan(ChunkPos chunk) {\n        this.chunkBounded = true;\n        this.boundMinX = chunk.getMinBlockX();\n        this.boundMaxX = this.boundMinX + 15;\n        this.boundMinZ = chunk.getMinBlockZ();\n        this.boundMaxZ = this.boundMinZ + 15;\n    }\n\n'''
if 'public IncrementalWorldEditPlan(ChunkPos chunk)' not in text:
    if counter_anchor not in text:
        raise SystemExit('plan constructor insertion point missing')
    text = text.replace(counter_anchor, counter_insert, 1)

getter_anchor = '''    public int sampledColumnCount() { return originalSurfaceHeights.size(); }\n    public long suppressedTerrainWrites() { return suppressedTerrainWrites; }\n'''
getter_insert = '''    public int sampledColumnCount() { return originalSurfaceHeights.size(); }\n    public long suppressedTerrainWrites() { return suppressedTerrainWrites; }\n    public long suppressedOutOfBoundsWrites() { return suppressedOutOfBoundsWrites; }\n'''
if 'suppressedOutOfBoundsWrites()' not in text:
    if getter_anchor not in text:
        raise SystemExit('plan getter insertion point missing')
    text = text.replace(getter_anchor, getter_insert, 1)

set_anchor = '''    public void addSet(int x, int y, int z, BlockState state) {\n        long key = columnKey(x, z);\n'''
set_insert = '''    public void addSet(int x, int y, int z, BlockState state) {\n        if (!insideScope(x, z)) {\n            suppressedOutOfBoundsWrites++;\n            return;\n        }\n        long key = columnKey(x, z);\n'''
if 'if (!insideScope(x, z)) {' not in text:
    if set_anchor not in text:
        raise SystemExit('addSet scope insertion point missing')
    text = text.replace(set_anchor, set_insert, 1)

fill_old = '''        int minX = Math.min(x1, x2);\n        int maxX = Math.max(x1, x2);\n        int minY = Math.min(y1, y2);\n        int maxY = Math.max(y1, y2);\n        int minZ = Math.min(z1, z2);\n        int maxZ = Math.max(z1, z2);\n        long writes = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);\n\n'''
fill_new = '''        int minX = Math.min(x1, x2);\n        int maxX = Math.max(x1, x2);\n        int minY = Math.min(y1, y2);\n        int maxY = Math.max(y1, y2);\n        int minZ = Math.min(z1, z2);\n        int maxZ = Math.max(z1, z2);\n        long requestedWrites = (long) (maxX - minX + 1)\n                * (maxY - minY + 1) * (maxZ - minZ + 1);\n        if (chunkBounded) {\n            minX = Math.max(minX, boundMinX);\n            maxX = Math.min(maxX, boundMaxX);\n            minZ = Math.max(minZ, boundMinZ);\n            maxZ = Math.min(maxZ, boundMaxZ);\n            if (minX > maxX || minZ > maxZ) {\n                suppressedOutOfBoundsWrites += requestedWrites;\n                return;\n            }\n        }\n        long writes = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);\n        suppressedOutOfBoundsWrites += requestedWrites - writes;\n\n'''
if 'long requestedWrites =' not in text:
    if fill_old not in text:
        raise SystemExit('addFill scope replacement point missing')
    text = text.replace(fill_old, fill_new, 1)

scope_anchor = '''    private static long columnKey(int x, int z) {\n        return ((long) x << 32) ^ (z & 0xffffffffL);\n    }\n\n'''
scope_insert = '''    private boolean insideScope(int x, int z) {\n        return !chunkBounded\n                || (x >= boundMinX && x <= boundMaxX && z >= boundMinZ && z <= boundMaxZ);\n    }\n\n    private static long columnKey(int x, int z) {\n        return ((long) x << 32) ^ (z & 0xffffffffL);\n    }\n\n'''
if 'private boolean insideScope(int x, int z)' not in text:
    if scope_anchor not in text:
        raise SystemExit('insideScope insertion point missing')
    text = text.replace(scope_anchor, scope_insert, 1)

set_apply_old = '''            write(level, x, y, z, state);\n            done = true;\n            return 1;\n'''
set_apply_new = '''            if (!write(level, x, y, z, state)) return 0;\n            done = true;\n            return 1;\n'''
if set_apply_new not in text:
    if set_apply_old not in text:
        raise SystemExit('SetOperation write point missing')
    text = text.replace(set_apply_old, set_apply_new, 1)

box_apply_old = '''                write(level, x, y, z, state);\n                used++;\n                advance();\n'''
box_apply_new = '''                if (!write(level, x, y, z, state)) break;\n                used++;\n                advance();\n'''
if box_apply_new not in text:
    if box_apply_old not in text:
        raise SystemExit('BoxOperation write point missing')
    text = text.replace(box_apply_old, box_apply_new, 1)

write_old = '''    private static void write(ServerLevel level, int x, int y, int z, BlockState state) {\n        if (y < level.getMinY() || y >= level.getMaxY()) return;\n        BlockPos pos = new BlockPos(x, y, z);\n        if (level.getBlockState(pos).equals(state)) return;\n        // External structures can carry stale pending block-entity NBT after cleanup changed\n        // the corresponding block to air. Remove that pending/ticking entry before replacing\n        // the state so LevelChunk never tries to instantiate (for example) a beehive on air.\n        level.getChunkAt(pos).removeBlockEntity(pos);\n        level.setBlock(pos, state, CONSTRUCTION_UPDATE_FLAGS);\n    }\n'''
write_new = '''    private static boolean write(ServerLevel level, int x, int y, int z, BlockState state) {\n        if (y < level.getMinY() || y >= level.getMaxY()) return true;\n        int chunkX = x >> 4;\n        int chunkZ = z >> 4;\n        if (!level.hasChunk(chunkX, chunkZ)) return false;\n        BlockPos pos = new BlockPos(x, y, z);\n        if (level.getBlockState(pos).equals(state)) return true;\n        // External structures can carry stale pending block-entity NBT after cleanup changed\n        // the corresponding block to air. Remove that pending/ticking entry before replacing\n        // the state so LevelChunk never tries to instantiate (for example) a beehive on air.\n        level.getChunkAt(pos).removeBlockEntity(pos);\n        level.setBlock(pos, state, CONSTRUCTION_UPDATE_FLAGS);\n        return true;\n    }\n'''
if 'private static boolean write(ServerLevel level' not in text:
    if write_old not in text:
        raise SystemExit('write method replacement point missing')
    text = text.replace(write_old, write_new, 1)

plan_path.write_text(text, encoding='utf-8')

builder = builder_path.read_text(encoding='utf-8')
method_anchor = '''    private static IncrementalWorldEditPlan createChunkPlan(\n            ServerLevel level,\n            ChunkPos chunk,\n            boolean buildExterior,\n            boolean buildResidences) {\n        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan();\n'''
method_new = '''    private static IncrementalWorldEditPlan createChunkPlan(\n            ServerLevel level,\n            ChunkPos chunk,\n            boolean buildExterior,\n            boolean buildResidences) {\n        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan(chunk);\n'''
if 'IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan(chunk);' not in builder:
    if method_anchor not in builder:
        raise SystemExit('exterior chunk-bound plan insertion point missing')
    builder = builder.replace(method_anchor, method_new, 1)

log_old = '''                    "Prepared Erden exterior chunk {},{} writes={} operations={} exterior={} residences={}",\n                    chunkX, chunkZ, plan.estimatedWrites(), plan.operationCount(),\n                    buildExterior, buildResidences);\n'''
log_new = '''                    "Prepared Erden exterior chunk {},{} writes={} operations={} exterior={} residences={} clipped_out_of_chunk_writes={}",\n                    chunkX, chunkZ, plan.estimatedWrites(), plan.operationCount(),\n                    buildExterior, buildResidences, plan.suppressedOutOfBoundsWrites());\n'''
if 'clipped_out_of_chunk_writes={}' not in builder:
    if log_old not in builder:
        raise SystemExit('exterior plan debug marker insertion point missing')
    builder = builder.replace(log_old, log_new, 1)

builder_path.write_text(builder, encoding='utf-8')
