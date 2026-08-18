from pathlib import Path
from runpy import run_path

path = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenLandmarkInteriorManager.java')
text = path.read_text(encoding='utf-8')


def replace_once(old: str, new: str, label: str) -> None:
    global text
    if new in text:
        return
    if old not in text:
        raise SystemExit(f'{label}: expected source block not found')
    text = text.replace(old, new, 1)


replace_once(
    '''    private static boolean ciSamplePassed;\n    private static boolean ciSampleTicketHeld;\n    private static ChunkPos ciSampleTicketCenter;\n''',
    '''    private static boolean ciSamplePassed;\n    private static boolean ciSampleTicketHeld;\n    private static ChunkPos ciSampleTicketCenter;\n    private static long lastCiChunkRefreshTick = Long.MIN_VALUE;\n''',
    'landmark refresh field')

replace_once(
    '''        ciSamplePassed = false;\n        ciSampleTicketHeld = false;\n        ciSampleTicketCenter = null;\n''',
    '''        ciSamplePassed = false;\n        ciSampleTicketHeld = false;\n        ciSampleTicketCenter = null;\n        lastCiChunkRefreshTick = Long.MIN_VALUE;\n''',
    'landmark reset')

replace_once(
    '''    private static void requestCiSampleChunks(\n            ServerLevel level,\n            List<ExternalDistrictBuildingBuilder.BuildingEntrance> landmarks) {\n        if (ciChunksRequested\n                || landmarks.isEmpty()\n                || !ciMode()) return;\n        ExternalDistrictBuildingBuilder.BuildingEntrance sample = landmarks.getFirst();\n        ciSampleTicketCenter = new ChunkPos(sample.x() >> 4, sample.z() >> 4);\n        level.getChunkSource().addTicketAndLoadWithRadius(\n                TicketType.PORTAL, ciSampleTicketCenter, CI_SAMPLE_TICKET_RADIUS);\n        ciSampleTicketHeld = true;\n\n        Frame frame = frame(sample);\n        for (int lateral : new int[]{-7, 0, 7}) {\n            for (int forward : new int[]{0, 8, 16}) {\n                Point point = frame.point(lateral, forward);\n                int chunkX = point.x >> 4;\n                int chunkZ = point.z >> 4;\n                if (chunkX * 16 >= ErdenCapitalStreamingBuilder.WEST_WALL_X - 16\n                        && chunkX * 16 <= ErdenCapitalStreamingBuilder.EAST_WALL_X + 16\n                        && chunkZ * 16 >= ErdenCapitalStreamingBuilder.NORTH_WALL_Z - 16\n                        && chunkZ * 16 <= ErdenCapitalStreamingBuilder.SOUTH_WALL_Z + 16) {\n                    ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);\n                }\n            }\n        }\n        ciChunksRequested = true;\n        LivingKingdoms.LOGGER.info(\n                "Retained Erden landmark CI sample role={} chunk={},{} radius={} transient_ticket=portal synchronous_get_chunk=false",\n                sample.role(), ciSampleTicketCenter.x(), ciSampleTicketCenter.z(),\n                CI_SAMPLE_TICKET_RADIUS);\n    }\n''',
    '''    private static void requestCiSampleChunks(\n            ServerLevel level,\n            List<ExternalDistrictBuildingBuilder.BuildingEntrance> landmarks) {\n        if (landmarks.isEmpty() || !ciMode() || ciSamplePassed) return;\n        long tick = level.getGameTime();\n        if (lastCiChunkRefreshTick != Long.MIN_VALUE\n                && tick - lastCiChunkRefreshTick < 40L) return;\n        lastCiChunkRefreshTick = tick;\n\n        ExternalDistrictBuildingBuilder.BuildingEntrance sample = landmarks.getFirst();\n        ChunkPos requestedCenter = new ChunkPos(sample.x() >> 4, sample.z() >> 4);\n        if (ciSampleTicketHeld && ciSampleTicketCenter != null\n                && !ciSampleTicketCenter.equals(requestedCenter)) {\n            releaseCiSampleTicket(level);\n        }\n        ciSampleTicketCenter = requestedCenter;\n        level.getChunkSource().addTicketAndLoadWithRadius(\n                TicketType.PORTAL, ciSampleTicketCenter, CI_SAMPLE_TICKET_RADIUS);\n        ciSampleTicketHeld = true;\n\n        Frame frame = frame(sample);\n        for (int lateral : new int[]{-7, 0, 7}) {\n            for (int forward : new int[]{0, 8, 16}) {\n                Point point = frame.point(lateral, forward);\n                int chunkX = point.x >> 4;\n                int chunkZ = point.z >> 4;\n                if (chunkX * 16 >= ErdenCapitalStreamingBuilder.WEST_WALL_X - 16\n                        && chunkX * 16 <= ErdenCapitalStreamingBuilder.EAST_WALL_X + 16\n                        && chunkZ * 16 >= ErdenCapitalStreamingBuilder.NORTH_WALL_Z - 16\n                        && chunkZ * 16 <= ErdenCapitalStreamingBuilder.SOUTH_WALL_Z + 16) {\n                    ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);\n                    ErdenCapitalStreamingBuilder.retainDiagnosticChunk(level, chunkX, chunkZ);\n                }\n            }\n        }\n        if (!ciChunksRequested) {\n            LivingKingdoms.LOGGER.info(\n                    "Retained Erden landmark CI sample role={} chunk={},{} radius={} transient_ticket=portal refreshed_until_verification=true loaded_lease=true refresh_ticks=40 persistent_forced_chunks=false synchronous_get_chunk=false",\n                    sample.role(), ciSampleTicketCenter.x(), ciSampleTicketCenter.z(),\n                    CI_SAMPLE_TICKET_RADIUS);\n        }\n        ciChunksRequested = true;\n    }\n''',
    'landmark bounded ci residency')

path.write_text(text, encoding='utf-8')
run_path('.github/scripts/patch_living_kingdoms_upper_route_ground_ci.py')
print('Living Kingdoms landmark and upper-route predecessor CI residency patches applied')
