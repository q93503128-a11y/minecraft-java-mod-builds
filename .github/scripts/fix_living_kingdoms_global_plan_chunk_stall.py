from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/IncrementalWorldEditPlan.java")
text = path.read_text(encoding="utf-8")

replacements = [
    (
        "import net.minecraft.server.level.ServerLevel;\nimport net.minecraft.world.level.ChunkPos;",
        "import net.minecraft.server.level.ServerLevel;\nimport net.minecraft.server.level.TicketType;\nimport net.minecraft.world.level.ChunkPos;",
    ),
    (
        "import java.util.ArrayList;\nimport java.util.HashMap;\nimport java.util.List;\nimport java.util.Map;",
        "import java.util.ArrayList;\nimport java.util.HashMap;\nimport java.util.HashSet;\nimport java.util.List;\nimport java.util.Map;\nimport java.util.Set;",
    ),
    (
        "    private final List<Operation> operations = new ArrayList<>();\n    private final Map<Long, Integer> originalSurfaceHeights = new HashMap<>();",
        "    private final List<Operation> operations = new ArrayList<>();\n    private final Set<Long> retainedConstructionChunks = new HashSet<>();\n    private final Map<Long, Integer> originalSurfaceHeights = new HashMap<>();",
    ),
    (
        "    public int apply(ServerLevel level, int budget) {\n        flushPendingTerrainColumn();\n        int used = 0;\n        long deadline = System.nanoTime() + MAX_APPLY_NANOS;\n        while (operationIndex < operations.size() && used < budget) {\n            if (used > 0 && System.nanoTime() >= deadline) break;\n            Operation operation = operations.get(operationIndex);\n            int consumed = operation.apply(level, budget - used, deadline);\n            if (consumed <= 0) break;\n            used += consumed;\n            appliedWrites += consumed;\n            if (operation.done()) operationIndex++;\n            else break;\n        }\n        return used;\n    }",
        "    public int apply(ServerLevel level, int budget) {\n        flushPendingTerrainColumn();\n        int used = 0;\n        long deadline = System.nanoTime() + MAX_APPLY_NANOS;\n        try {\n            while (operationIndex < operations.size() && used < budget) {\n                if (used > 0 && System.nanoTime() >= deadline) break;\n                Operation operation = operations.get(operationIndex);\n                int consumed = operation.apply(this, level, budget - used, deadline);\n                if (consumed <= 0) break;\n                used += consumed;\n                appliedWrites += consumed;\n                if (operation.done()) operationIndex++;\n                else break;\n            }\n            if (operationIndex >= operations.size()) releaseRetainedConstructionChunks(level);\n            return used;\n        } catch (RuntimeException | Error failure) {\n            releaseRetainedConstructionChunks(level);\n            throw failure;\n        }\n    }",
    ),
    (
        "    private interface Operation {\n        int apply(ServerLevel level, int budget, long deadline);\n        boolean done();\n    }",
        "    private interface Operation {\n        int apply(IncrementalWorldEditPlan owner, ServerLevel level, int budget, long deadline);\n        boolean done();\n    }",
    ),
    (
        "        @Override public int apply(ServerLevel level, int budget, long deadline) {\n            if (done || budget <= 0 || System.nanoTime() >= deadline) return 0;\n            if (!write(level, x, y, z, state)) return 0;",
        "        @Override public int apply(IncrementalWorldEditPlan owner, ServerLevel level, int budget, long deadline) {\n            if (done || budget <= 0 || System.nanoTime() >= deadline) return 0;\n            if (!owner.write(level, x, y, z, state)) return 0;",
    ),
    (
        "        @Override public int apply(ServerLevel level, int budget, long deadline) {\n            int used = 0;\n            while (!done && used < budget\n                    && (used == 0 || System.nanoTime() < deadline)) {\n                if (!write(level, x, y, z, state)) break;",
        "        @Override public int apply(IncrementalWorldEditPlan owner, ServerLevel level, int budget, long deadline) {\n            int used = 0;\n            while (!done && used < budget\n                    && (used == 0 || System.nanoTime() < deadline)) {\n                if (!owner.write(level, x, y, z, state)) break;",
    ),
    (
        "    private static boolean write(ServerLevel level, int x, int y, int z, BlockState state) {\n        if (y < level.getMinY() || y >= level.getMaxY()) return true;\n        int chunkX = x >> 4;\n        int chunkZ = z >> 4;\n        if (!level.hasChunk(chunkX, chunkZ)) return false;",
        "    private boolean write(ServerLevel level, int x, int y, int z, BlockState state) {\n        if (y < level.getMinY() || y >= level.getMaxY()) return true;\n        int chunkX = x >> 4;\n        int chunkZ = z >> 4;\n        if (!level.hasChunk(chunkX, chunkZ)) {\n            if (!chunkBounded) retainConstructionChunk(level, chunkX, chunkZ);\n            return false;\n        }",
    ),
    (
        "        level.setBlock(pos, state, CONSTRUCTION_UPDATE_FLAGS);\n        return true;\n    }\n\n    private record PendingTerrainColumn",
        "        level.setBlock(pos, state, CONSTRUCTION_UPDATE_FLAGS);\n        return true;\n    }\n\n    private void retainConstructionChunk(ServerLevel level, int chunkX, int chunkZ) {\n        long packed = columnKey(chunkX, chunkZ);\n        if (!retainedConstructionChunks.add(packed)) return;\n        level.getChunkSource().addTicketAndLoadWithRadius(\n                TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);\n    }\n\n    private void releaseRetainedConstructionChunks(ServerLevel level) {\n        if (retainedConstructionChunks.isEmpty()) return;\n        for (long packed : Set.copyOf(retainedConstructionChunks)) {\n            level.getChunkSource().removeTicketWithRadius(\n                    TicketType.PORTAL,\n                    new ChunkPos((int) (packed >> 32), (int) packed),\n                    0);\n        }\n        retainedConstructionChunks.clear();\n    }\n\n    private record PendingTerrainColumn",
    ),
]

for old, new in replacements:
    if old in text:
        text = text.replace(old, new, 1)
    elif new not in text:
        raise SystemExit("missing patch anchor:\n" + old[:240])

path.write_text(text, encoding="utf-8")
