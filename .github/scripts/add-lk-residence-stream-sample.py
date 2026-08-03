from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/StarterRealmDiagnostics.java")
text = path.read_text(encoding="utf-8")
old = """    private static final StreamSample[] STREAM_SAMPLES = {\n            new StreamSample(\"royal_avenue\", 0, 200, false),\n"""
new = """    private static final StreamSample[] STREAM_SAMPLES = {\n            new StreamSample(\"origin_residence\", 320, 180, false),\n            new StreamSample(\"royal_avenue\", 0, 200, false),\n"""
assert text.count(old) == 1, "stream sample anchor changed"
path.write_text(text.replace(old, new), encoding="utf-8")
