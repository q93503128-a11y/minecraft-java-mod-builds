from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenCapitalStreamingBuilder.java")
text = path.read_text(encoding="utf-8")

replacements = [
    (
        "import net.minecraft.server.level.ServerLevel;\nimport net.minecraft.world.level.ChunkPos;",
        "import net.minecraft.server.level.ServerLevel;\nimport net.minecraft.server.level.TicketType;\nimport net.minecraft.world.level.ChunkPos;",
    ),
    (
        "     * The temporary force-load is removed as soon as that cell is marked complete.\n",
        "     * The transient chunk ticket is removed as soon as that cell is marked complete.\n",
    ),
    (
        "        if (!data.needs(packed, CAPITAL_REVISION)) return;\n        if (RETAINED_REQUESTS.add(packed)) level.setChunkForced(chunkX, chunkZ, true);\n        level.getChunk(chunkX, chunkZ);\n        enqueue(level, packed, true);",
        "        if (!data.needs(packed, CAPITAL_REVISION)) return;\n        if (RETAINED_REQUESTS.add(packed)) {\n            level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, chunk, 0);\n        }\n        if (level.hasChunk(chunkX, chunkZ)) enqueue(level, packed, true);",
    ),
    (
        "        if (!RETAINED_REQUESTS.remove(packed)) return;\n        level.setChunkForced(unpackX(packed), unpackZ(packed), false);",
        "        if (!RETAINED_REQUESTS.remove(packed)) return;\n        level.getChunkSource().removeTicketWithRadius(\n                TicketType.PORTAL, new ChunkPos(unpackX(packed), unpackZ(packed)), 0);",
    ),
    (
        "                    previous.setChunkForced(unpackX(packed), unpackZ(packed), false);",
        "                    previous.getChunkSource().removeTicketWithRadius(\n                            TicketType.PORTAL,\n                            new ChunkPos(unpackX(packed), unpackZ(packed)),\n                            0);",
    ),
]

for old, new in replacements:
    if old in text:
        text = text.replace(old, new, 1)
    elif new not in text:
        raise SystemExit("missing capital async patch anchor:\n" + old[:240])

if "setChunkForced" in text:
    raise SystemExit("setChunkForced remains in capital streaming builder")
if "level.getChunk(chunkX, chunkZ)" in text:
    raise SystemExit("synchronous level.getChunk remains in capital streaming builder")

path.write_text(text, encoding="utf-8")
