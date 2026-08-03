from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java")
text = path.read_text(encoding="utf-8")

old = """import java.util.ArrayDeque;\nimport java.util.HashSet;\nimport java.util.List;\nimport java.util.Set;\n"""
new = """import java.util.ArrayDeque;\nimport java.util.HashSet;\nimport java.util.LinkedHashSet;\nimport java.util.List;\nimport java.util.Set;\n"""
assert text.count(old) == 1, "import anchor changed"
text = text.replace(old, new)

old = """    private static final int TICK_BUDGET = 2_000;\n    private static final int ROAD_HALF_WIDTH = 2;\n"""
new = """    private static final int TICK_BUDGET = 2_000;\n    private static final int CI_FORCE_BUDGET = 1;\n    private static final int ROAD_HALF_WIDTH = 2;\n"""
assert text.count(old) == 1, "constant anchor changed"
text = text.replace(old, new)

old = """    private static final ArrayDeque<Long> PENDING = new ArrayDeque<>();\n    private static final Set<Long> QUEUED = new HashSet<>();\n    private static final Set<Long> RETAINED = new HashSet<>();\n"""
new = """    private static final ArrayDeque<Long> PENDING = new ArrayDeque<>();\n    private static final ArrayDeque<Long> CI_REQUESTS = new ArrayDeque<>();\n    private static final Set<Long> CI_LOADING = new HashSet<>();\n    private static final Set<Long> QUEUED = new HashSet<>();\n    private static final Set<Long> RETAINED = new HashSet<>();\n"""
assert text.count(old) == 1, "queue anchor changed"
text = text.replace(old, new)

old = """        if (isCi() && !ciRequested) {\n            ciRequested = true;\n            requestCiAnchors(level);\n        }\n\n"""
new = """        if (isCi()) {\n            if (!ciRequested) {\n                ciRequested = true;\n                prepareCiAnchors();\n            }\n            advanceCiAnchors(level);\n        }\n\n"""
assert text.count(old) == 1, "CI tick anchor changed"
text = text.replace(old, new)

old = """        PENDING.clear();\n        QUEUED.clear();\n        RETAINED.clear();\n"""
new = """        PENDING.clear();\n        CI_REQUESTS.clear();\n        CI_LOADING.clear();\n        QUEUED.clear();\n        RETAINED.clear();\n"""
assert text.count(old) == 1, "reset anchor changed"
text = text.replace(old, new)

old = """    private static void requestCiAnchors(ServerLevel level) {\n        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {\n            for (int[] offset : NODE_ANCHOR_OFFSETS) {\n                requestChunk(level, (node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4);\n            }\n        }\n        LivingKingdoms.LOGGER.info(\n                \"Requested Erden exterior CI anchors nodes={} retained_chunks={} metre_scale=true streamed=true\",\n                ErdenKingdomSupplyCatalog.nodes().size(), RETAINED.size());\n    }\n\n    private static void requestChunk(ServerLevel level, int chunkX, int chunkZ) {\n        long packed = pack(chunkX, chunkZ);\n        ErdenKingdomExteriorSavedData data = level.getDataStorage()\n                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);\n        if (!data.needs(packed, EXTERIOR_REVISION)) return;\n        if (RETAINED.add(packed)) level.setChunkForced(chunkX, chunkZ, true);\n        level.getChunk(chunkX, chunkZ);\n        enqueue(level, packed, true);\n    }\n\n"""
new = """    private static void prepareCiAnchors() {\n        Set<Long> unique = new LinkedHashSet<>();\n        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {\n            for (int[] offset : NODE_ANCHOR_OFFSETS) {\n                unique.add(pack((node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4));\n            }\n        }\n        CI_REQUESTS.addAll(unique);\n        LivingKingdoms.LOGGER.info(\n                \"Requested Erden exterior CI anchors nodes={} request_queue={} metre_scale=true streamed=true staggered=true synchronous_get_chunk=false\",\n                ErdenKingdomSupplyCatalog.nodes().size(), CI_REQUESTS.size());\n    }\n\n    private static void advanceCiAnchors(ServerLevel level) {\n        for (long packed : List.copyOf(CI_LOADING)) {\n            int chunkX = unpackX(packed);\n            int chunkZ = unpackZ(packed);\n            if (!level.hasChunk(chunkX, chunkZ)) continue;\n            CI_LOADING.remove(packed);\n            enqueue(level, packed, true);\n        }\n\n        ErdenKingdomExteriorSavedData data = level.getDataStorage()\n                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);\n        for (int forced = 0; forced < CI_FORCE_BUDGET && !CI_REQUESTS.isEmpty(); forced++) {\n            long packed = CI_REQUESTS.removeFirst();\n            if (!data.needs(packed, EXTERIOR_REVISION)) continue;\n            int chunkX = unpackX(packed);\n            int chunkZ = unpackZ(packed);\n            if (level.hasChunk(chunkX, chunkZ)) {\n                enqueue(level, packed, true);\n                continue;\n            }\n            if (RETAINED.add(packed)) {\n                level.setChunkForced(chunkX, chunkZ, true);\n            }\n            CI_LOADING.add(packed);\n        }\n    }\n\n"""
assert text.count(old) == 1, "CI request implementation anchor changed"
text = text.replace(old, new)

old = """    private static void release(ServerLevel level, long packed) {\n        if (!RETAINED.remove(packed)) return;\n        level.setChunkForced(unpackX(packed), unpackZ(packed), false);\n    }\n"""
new = """    private static void release(ServerLevel level, long packed) {\n        CI_LOADING.remove(packed);\n        if (!RETAINED.remove(packed)) return;\n        level.setChunkForced(unpackX(packed), unpackZ(packed), false);\n    }\n"""
assert text.count(old) == 1, "release anchor changed"
text = text.replace(old, new)

path.write_text(text, encoding="utf-8")
