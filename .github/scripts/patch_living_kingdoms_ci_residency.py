from pathlib import Path

ROOT = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world')


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding='utf-8')
    if new in text:
        return
    if old not in text:
        raise SystemExit(f'{label}: expected source block not found in {path}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')


economy = ROOT / 'ErdenAuthoritativeEconomyManager.java'
replace_once(
    economy,
    '''    private static MinecraftServer activeServer;\n    private static boolean planLogged;\n    private static boolean ciPassed;\n    private static int lastFulfilledHouseholds;\n''',
    '''    private static MinecraftServer activeServer;\n    private static boolean planLogged;\n    private static boolean ciPassed;\n    private static boolean ciChunksRequested;\n    private static int lastFulfilledHouseholds;\n''',
    'economy ci field')
replace_once(
    economy,
    '''        ensureEconomy(economy, population);\n        logPlanOnce(economy);\n\n        if (level.getGameTime() % SYNC_INTERVAL == 0L) {\n''',
    '''        ensureEconomy(economy, population);\n        logPlanOnce(economy);\n        requestCiSampleChunks(level);\n\n        if (level.getGameTime() % SYNC_INTERVAL == 0L) {\n''',
    'economy ci request call')
replace_once(
    economy,
    '''        planLogged = false;\n        ciPassed = false;\n        lastFulfilledHouseholds = 0;\n''',
    '''        planLogged = false;\n        ciPassed = false;\n        ciChunksRequested = false;\n        lastFulfilledHouseholds = 0;\n''',
    'economy reset')
replace_once(
    economy,
    '''    private static void verifyCiIfReady(\n            ServerLevel level,\n            ErdenPhysicalEconomySavedData economy) {\n''',
    '''    private static void requestCiSampleChunks(ServerLevel level) {\n        if (ciChunksRequested\n                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;\n        List<ExternalUrbanFabricBuilder.UrbanEntrance> samples = ciEntrances();\n        if (samples.size() != 3) {\n            throw new IllegalStateException(\n                    "Erden physical-economy CI expected three authored sample sites, found "\n                            + samples.size());\n        }\n        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : samples) {\n            ErdenUrbanInteriorBuilder.requestPlanChunksForCi(level, entrance);\n        }\n        ciChunksRequested = true;\n        LivingKingdoms.LOGGER.info(\n                "Requested Erden physical-economy authored interior CI samples sites={} roles={} bounded_plan_chunks=true persistent_forced_chunks=false",\n                samples.size(), samples.stream().map(ExternalUrbanFabricBuilder.UrbanEntrance::role).toList());\n    }\n\n    private static void verifyCiIfReady(\n            ServerLevel level,\n            ErdenPhysicalEconomySavedData economy) {\n''',
    'economy ci method')

interior = ROOT / 'ErdenUrbanInteriorBuilder.java'
replace_once(
    interior,
    '''    private static void requestCiSampleChunks(ServerLevel level) {\n        if (ciChunksRequested || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;\n        ExternalUrbanFabricBuilder.UrbanEntrance entrance = ExternalUrbanFabricBuilder.diagnosticEntrance();\n        ErdenUrbanAuthoredGroundPlanCatalog.PlacementPlan plan =\n                ErdenUrbanAuthoredGroundPlanCatalog.plan(entrance);\n        if (plan == null) throw new IllegalStateException("Missing authored urban CI plan");\n        for (BlockPos pos : planPositions(plan)) {\n            int centerChunkX = pos.getX() >> 4;\n            int centerChunkZ = pos.getZ() >> 4;\n            for (int chunkX = centerChunkX - 1; chunkX <= centerChunkX + 1; chunkX++) {\n                for (int chunkZ = centerChunkZ - 1; chunkZ <= centerChunkZ + 1; chunkZ++) {\n                    ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);\n                }\n            }\n        }\n        ciChunksRequested = true;\n    }\n''',
    '''    private static void requestCiSampleChunks(ServerLevel level) {\n        if (ciChunksRequested || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;\n        requestPlanChunksForCi(level, ExternalUrbanFabricBuilder.diagnosticEntrance());\n        ciChunksRequested = true;\n    }\n\n    static void requestPlanChunksForCi(\n            ServerLevel level,\n            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {\n        if (!"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;\n        ErdenUrbanAuthoredGroundPlanCatalog.PlacementPlan plan =\n                ErdenUrbanAuthoredGroundPlanCatalog.plan(entrance);\n        if (plan == null) {\n            throw new IllegalStateException(\n                    "Missing authored urban CI plan role=" + entrance.role()\n                            + " entrance=" + entrance.x() + "," + entrance.z());\n        }\n        for (BlockPos pos : planPositions(plan)) {\n            int centerChunkX = pos.getX() >> 4;\n            int centerChunkZ = pos.getZ() >> 4;\n            for (int chunkX = centerChunkX - 1; chunkX <= centerChunkX + 1; chunkX++) {\n                for (int chunkZ = centerChunkZ - 1; chunkZ <= centerChunkZ + 1; chunkZ++) {\n                    ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);\n                }\n            }\n        }\n    }\n''',
    'interior reusable ci plan request')

citadel = ROOT / 'ErdenCitadelInteriorManager.java'
replace_once(
    citadel,
    '''    private static void retainCitadelForCi(\n            ServerLevel level,\n            RealmSiteLayoutSavedData.RealmSite site) {\n        if (ciTicketHeld || !ciMode()) return;\n        ciTicketCenter = new ChunkPos(site.centerX() >> 4, site.centerZ() >> 4);\n        level.getChunkSource().addTicketAndLoadWithRadius(\n                TicketType.PORTAL, ciTicketCenter, CI_CHUNK_RADIUS);\n        ciTicketHeld = true;\n        LivingKingdoms.LOGGER.info(\n                "Retained Erden citadel for CI zoning audit centre_chunk={},{} radius={} transient_ticket=portal forced_chunks=false synchronous_get_chunk=false",\n                ciTicketCenter.x(), ciTicketCenter.z(), CI_CHUNK_RADIUS);\n    }\n''',
    '''    private static void retainCitadelForCi(\n            ServerLevel level,\n            RealmSiteLayoutSavedData.RealmSite site) {\n        if (!ciMode()) return;\n        ChunkPos requestedCenter = new ChunkPos(site.centerX() >> 4, site.centerZ() >> 4);\n        if (ciTicketHeld && ciTicketCenter != null && !ciTicketCenter.equals(requestedCenter)) {\n            releaseCiTicket(level);\n        }\n        ciTicketCenter = requestedCenter;\n        // PORTAL tickets are transient. Refresh the same bounded ticket every zoning pass so a\n        // long fresh-world audit cannot lose the final zone after the ticket timeout expires.\n        level.getChunkSource().addTicketAndLoadWithRadius(\n                TicketType.PORTAL, ciTicketCenter, CI_CHUNK_RADIUS);\n        if (!ciTicketHeld) {\n            LivingKingdoms.LOGGER.info(\n                    "Retained Erden citadel for CI zoning audit centre_chunk={},{} radius={} transient_ticket=portal refreshed_until_verification=true forced_chunks=false synchronous_get_chunk=false",\n                    ciTicketCenter.x(), ciTicketCenter.z(), CI_CHUNK_RADIUS);\n        }\n        ciTicketHeld = true;\n    }\n''',
    'citadel transient ticket refresh')

print('Living Kingdoms CI residency patch applied')
