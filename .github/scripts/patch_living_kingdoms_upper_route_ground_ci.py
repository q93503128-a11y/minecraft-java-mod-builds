from pathlib import Path

path = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenUrbanAuthoredUpperRouteManager.java')
text = path.read_text(encoding='utf-8')

old = '''        for (int chunkX = Math.floorDiv(sample.bounds().minX(), 16);\n             chunkX <= Math.floorDiv(sample.bounds().maxX(), 16); chunkX++) {\n'''
new = '''        ExternalUrbanFabricBuilder.UrbanEntrance sampleEntrance =\n                ExternalUrbanFabricBuilder.entrances().stream()\n                        .filter(entrance -> entrance.x() == sample.entranceX()\n                                && entrance.z() == sample.entranceZ())\n                        .findFirst()\n                        .orElseThrow(() -> new IllegalStateException(\n                                "Missing Erden authored upper-route CI entrance "\n                                        + sample.entranceX() + "," + sample.entranceZ()));\n        // Ground completion is a hard predecessor for route materialization. Retaining only the\n        // narrow stair path can leave the wider authored-ground placement plan unloaded forever,\n        // so keep the same bounded ground-plan chunks resident until this route is verified.\n        ErdenUrbanInteriorBuilder.requestPlanChunksForCi(level, sampleEntrance);\n\n        for (int chunkX = Math.floorDiv(sample.bounds().minX(), 16);\n             chunkX <= Math.floorDiv(sample.bounds().maxX(), 16); chunkX++) {\n'''
if new not in text:
    if old not in text:
        raise SystemExit('upper-route CI ground-plan insertion point not found')
    text = text.replace(old, new, 1)

old_log = '''                    "Requested Erden authored upper-route CI sample role={} entrance={},{} bounded_route_chunks=true refresh_ticks=40 loaded_lease=true persistent_forced_chunks=false",\n'''
new_log = '''                    "Requested Erden authored upper-route CI sample role={} entrance={},{} bounded_route_chunks=true authored_ground_plan=true refresh_ticks=40 loaded_lease=true persistent_forced_chunks=false",\n'''
if new_log not in text:
    if old_log not in text:
        raise SystemExit('upper-route CI log marker not found')
    text = text.replace(old_log, new_log, 1)

path.write_text(text, encoding='utf-8')
print('Living Kingdoms upper-route CI ground-plan predecessor patch applied')
