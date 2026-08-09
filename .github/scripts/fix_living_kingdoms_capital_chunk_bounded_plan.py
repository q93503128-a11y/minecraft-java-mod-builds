from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenCapitalStreamingBuilder.java")
text = path.read_text(encoding="utf-8")
old = "    private static IncrementalWorldEditPlan createChunkPlan(ServerLevel level, ChunkPos chunk) {\n        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan();"
new = "    private static IncrementalWorldEditPlan createChunkPlan(ServerLevel level, ChunkPos chunk) {\n        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan(chunk);"
if old in text:
    text = text.replace(old, new, 1)
elif new not in text:
    raise SystemExit("capital chunk-bounded plan anchor missing")
path.write_text(text, encoding="utf-8")
