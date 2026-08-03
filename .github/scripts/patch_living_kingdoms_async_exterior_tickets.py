from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java")
text = path.read_text(encoding="utf-8")

replacements = [
    (
        "import net.minecraft.server.level.ServerLevel;\n",
        "import net.minecraft.server.level.ServerLevel;\nimport net.minecraft.server.level.TicketType;\n",
        "ticket import",
    ),
    (
        "        CI_REQUIRED.addAll(unique);\n        CI_REQUIRED.addAll(unique);\n        CI_REQUESTS.addAll(unique);",
        "        CI_REQUIRED.addAll(unique);\n        CI_REQUESTS.addAll(unique);",
        "duplicate required registration",
    ),
    (
        "            if (RETAINED.add(packed)) {\n                level.setChunkForced(chunkX, chunkZ, true);\n            }\n            CI_LOADING.add(packed);",
        "            if (RETAINED.add(packed)) {\n                level.getChunkSource().addTicketAndLoadWithRadius(\n                        TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);\n            }\n            CI_LOADING.add(packed);",
        "asynchronous loading ticket",
    ),
    (
        "    private static void release(ServerLevel level, long packed) {\n        CI_LOADING.remove(packed);\n        if (!RETAINED.remove(packed)) return;\n        level.setChunkForced(unpackX(packed), unpackZ(packed), false);\n    }",
        "    private static void release(ServerLevel level, long packed) {\n        CI_LOADING.remove(packed);\n        RETAINED.remove(packed);\n    }",
        "release transient tracking",
    ),
    (
        "synchronous_get_chunk=false max_in_flight={}",
        "synchronous_get_chunk=false forced_chunks=false transient_ticket=portal max_in_flight={}",
        "ticket audit marker",
    ),
]

for old, new, label in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, got {count}")
    text = text.replace(old, new)

if "setChunkForced" in text:
    raise SystemExit("setChunkForced remains in exterior builder")

path.write_text(text, encoding="utf-8")
