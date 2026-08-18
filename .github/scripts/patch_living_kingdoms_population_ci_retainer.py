from pathlib import Path

path = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenPopulationCiChunkRetainer.java')
text = path.read_text(encoding='utf-8')

text = text.replace('import java.util.HashSet;\nimport java.util.Set;\n\n', '')
text = text.replace('    private static final Set<Long> RETAINED_CHUNKS = new HashSet<>();\n\n', '')
text = text.replace('''        if (activeServer != event.getServer()) {\n            activeServer = event.getServer();\n            RETAINED_CHUNKS.clear();\n        }\n''', '''        if (activeServer != event.getServer()) {\n            activeServer = event.getServer();\n        }\n''')
old = '''                if (!ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) {\n                    ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);\n                }\n                long key = ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);\n                if (RETAINED_CHUNKS.add(key)) {\n                    level.setChunkForced(chunkX, chunkZ, true);\n                }\n'''
new = '''                if (!ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) {\n                    ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);\n                }\n                // CI samples need residency, not a persistent forced-chunk mutation. The retainer\n                // runs every five ticks, so refreshing the bounded diagnostic PORTAL lease keeps\n                // the sample available and naturally releases it after the diagnostic stops.\n                ErdenCapitalStreamingBuilder.retainDiagnosticChunk(level, chunkX, chunkZ);\n'''
if new not in text:
    if old not in text:
        raise SystemExit('population CI forced-chunk block not found')
    text = text.replace(old, new, 1)
if 'setChunkForced' in text or 'RETAINED_CHUNKS' in text:
    raise SystemExit('persistent population CI forced-chunk code remains')
path.write_text(text, encoding='utf-8')
print('Living Kingdoms population CI retainer converted to transient diagnostic leases')
