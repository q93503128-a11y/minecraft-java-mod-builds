from pathlib import Path

path = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenPopulationCiChunkRetainer.java')
text = path.read_text(encoding='utf-8')

start_marker = '    private static void retainBuilding(ServerLevel level, int x, int z) {'
start = text.find(start_marker)
if start < 0:
    raise SystemExit('population CI retainBuilding method not found')
end = text.rfind('\n}')
if end <= start:
    raise SystemExit('population CI class closing brace not found')

replacement = '''    private static void retainBuilding(ServerLevel level, int x, int z) {\n        ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement = null;\n        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement candidate\n                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {\n            if (candidate.entrance().x() == x && candidate.entrance().z() == z) {\n                placement = candidate;\n                break;\n            }\n        }\n        if (placement == null) return;\n\n        // Population readiness is defined by the authored ground plan plus its verified upper\n        // residence. Retaining only the old 7x9 doorway room can leave most of a 34x38 source\n        // fragment unloaded forever in headless CI. Refresh the actual placement footprint with\n        // one chunk of halo, and explicitly refresh all authored-ground plan chunks. These are\n        // transient PORTAL leases only; no persistent forced chunk state is written.\n        ErdenUrbanInteriorBuilder.requestPlanChunksForCi(level, placement.entrance());\n        int minChunkX = Math.floorDiv(placement.minX(), 16) - 1;\n        int maxChunkX = Math.floorDiv(placement.maxX(), 16) + 1;\n        int minChunkZ = Math.floorDiv(placement.minZ(), 16) - 1;\n        int maxChunkZ = Math.floorDiv(placement.maxZ(), 16) + 1;\n        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {\n            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {\n                if (!ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) {\n                    ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);\n                }\n                ErdenCapitalStreamingBuilder.retainDiagnosticChunk(level, chunkX, chunkZ);\n            }\n        }\n    }\n'''

text = text[:start] + replacement + text[end:]

if 'setChunkForced' in text:
    raise SystemExit('population full-residence patch still contains persistent forced chunks')
if 'buildingPlacementsForDiagnostics()' not in text:
    raise SystemExit('population full-residence patch did not bind to actual authored placement')
if 'ErdenUrbanInteriorBuilder.requestPlanChunksForCi(level, placement.entrance());' not in text:
    raise SystemExit('population full-residence patch did not refresh authored-ground plan')
if 'retainDiagnosticChunk(level, chunkX, chunkZ)' not in text:
    raise SystemExit('population full-residence patch did not use transient diagnostic leases')

path.write_text(text, encoding='utf-8')
print('Living Kingdoms population CI now retains complete authored residence footprints transiently')
