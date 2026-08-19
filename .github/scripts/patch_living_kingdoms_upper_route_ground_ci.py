from pathlib import Path

path = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenUrbanAuthoredUpperRouteManager.java')
text = path.read_text(encoding='utf-8')


def replace_once(old: str, new: str, label: str) -> None:
    global text
    if new in text:
        return
    if old not in text:
        raise SystemExit(f'{label}: expected source block not found')
    text = text.replace(old, new, 1)


replace_once(
    '''    private static boolean ciPassed;\n    private static long ciRouteKey = Long.MIN_VALUE;\n''',
    '''    private static boolean ciPassed;\n    private static long ciRouteKey = Long.MIN_VALUE;\n    private static long lastCiChunkRefreshTick = Long.MIN_VALUE;\n''',
    'upper-route refresh field')

replace_once(
    '''        ciPassed = false;\n        ciRouteKey = Long.MIN_VALUE;\n''',
    '''        ciPassed = false;\n        ciRouteKey = Long.MIN_VALUE;\n        lastCiChunkRefreshTick = Long.MIN_VALUE;\n''',
    'upper-route refresh reset')

start_marker = '    private static void requestCiSampleChunks(ServerLevel level) {'
end_marker = '    private static void verifyCiIfReady('
start = text.find(start_marker)
end = text.find(end_marker, start)
if start < 0 or end < 0:
    raise SystemExit('upper-route CI request method boundaries not found')

replacement = '''    private static void requestCiSampleChunks(ServerLevel level) {\n        if (ciPassed\n                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;\n        long tick = level.getGameTime();\n        if (lastCiChunkRefreshTick != Long.MIN_VALUE\n                && tick - lastCiChunkRefreshTick < 40L) return;\n        lastCiChunkRefreshTick = tick;\n\n        PlacementRoute sample = ROUTES.values().stream()\n                .filter(route -> {\n                    ExternalUrbanFabricBuilder.UrbanEntrance diagnostic =\n                            ExternalUrbanFabricBuilder.diagnosticEntrance();\n                    return route.entranceX() == diagnostic.x()\n                            && route.entranceZ() == diagnostic.z();\n                })\n                .findFirst()\n                .orElseGet(() -> ROUTES.values().iterator().next());\n        ExternalUrbanFabricBuilder.UrbanEntrance sampleEntrance =\n                ExternalUrbanFabricBuilder.entrances().stream()\n                        .filter(entrance -> entrance.x() == sample.entranceX()\n                                && entrance.z() == sample.entranceZ())\n                        .findFirst()\n                        .orElseThrow(() -> new IllegalStateException(\n                                "Missing Erden authored upper-route CI entrance "\n                                        + sample.entranceX() + "," + sample.entranceZ()));\n\n        // Ground materialization is a hard predecessor for the upper route. Refresh the complete\n        // authored-ground placement lease and the route body together until the real-world route\n        // proof succeeds. These are transient PORTAL leases only; once ciPassed becomes true no\n        // further refresh occurs and the chunks naturally unload.\n        ErdenUrbanInteriorBuilder.requestPlanChunksForCi(level, sampleEntrance);\n        for (int chunkX = Math.floorDiv(sample.bounds().minX(), 16);\n             chunkX <= Math.floorDiv(sample.bounds().maxX(), 16); chunkX++) {\n            for (int chunkZ = Math.floorDiv(sample.bounds().minZ(), 16);\n                 chunkZ <= Math.floorDiv(sample.bounds().maxZ(), 16); chunkZ++) {\n                ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);\n                ErdenCapitalStreamingBuilder.retainDiagnosticChunk(level, chunkX, chunkZ);\n            }\n        }\n        ciRouteKey = sample.entranceKey();\n        if (!ciChunksRequested) {\n            LivingKingdoms.LOGGER.info(\n                    "Requested Erden authored upper-route CI sample role={} entrance={},{} bounded_route_chunks=true authored_ground_plan=true refreshed_until_verification=true refresh_ticks=40 loaded_lease=true persistent_forced_chunks=false",\n                    sample.role(), sample.entranceX(), sample.entranceZ());\n        }\n        ciChunksRequested = true;\n    }\n\n'''
text = text[:start] + replacement + text[end:]

if 'lastCiChunkRefreshTick' not in text:
    raise SystemExit('upper-route refresh clock was not installed')
if 'refreshed_until_verification=true' not in text:
    raise SystemExit('upper-route refreshed lease marker was not installed')
if 'ErdenUrbanInteriorBuilder.requestPlanChunksForCi(level, sampleEntrance);' not in text:
    raise SystemExit('upper-route ground predecessor lease was not installed')
if 'ErdenCapitalStreamingBuilder.retainDiagnosticChunk(level, chunkX, chunkZ);' not in text:
    raise SystemExit('upper-route route-body diagnostic lease was not installed')

path.write_text(text, encoding='utf-8')
print('Living Kingdoms upper-route CI ground predecessor and route leases refresh until verification')
