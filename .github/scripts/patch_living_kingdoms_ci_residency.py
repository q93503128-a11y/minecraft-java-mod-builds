from pathlib import Path

ROOT = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world')


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding='utf-8')
    if new in text:
        return
    if old not in text:
        raise SystemExit(f'{label}: expected source block not found in {path}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')


capital = ROOT / 'ErdenCapitalStreamingBuilder.java'
replace_once(
    capital,
    '''    public static boolean isChunkBuilt(ServerLevel level, int chunkX, int chunkZ) {\n        return level.getDataStorage().computeIfAbsent(ErdenCapitalChunkSavedData.TYPE)\n                .isBuilt(pack(chunkX, chunkZ), CAPITAL_REVISION);\n    }\n''',
    '''    /**\n     * Keeps one already-built capital cell resident for a bounded diagnostic window. Unlike\n     * {@link #requestChunk(ServerLevel, int, int)}, this intentionally still adds a transient\n     * ticket after construction is complete. Callers must be diagnostic-only and refresh only\n     * while their real world-state proof is incomplete; the PORTAL ticket expires after refreshes\n     * stop and never creates a persistent forced chunk.\n     */\n    static void retainDiagnosticChunk(ServerLevel level, int chunkX, int chunkZ) {\n        ChunkPos chunk = new ChunkPos(chunkX, chunkZ);\n        if (!intersectsCapital(chunk)) {\n            throw new IllegalArgumentException(\n                    "Diagnostic chunk is outside the Erden capital: " + chunkX + "," + chunkZ);\n        }\n        level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, chunk, 0);\n    }\n\n    public static boolean isChunkBuilt(ServerLevel level, int chunkX, int chunkZ) {\n        return level.getDataStorage().computeIfAbsent(ErdenCapitalChunkSavedData.TYPE)\n                .isBuilt(pack(chunkX, chunkZ), CAPITAL_REVISION);\n    }\n''',
    'capital diagnostic loaded lease')

economy = ROOT / 'ErdenAuthoritativeEconomyManager.java'
replace_once(
    economy,
    '''    private static MinecraftServer activeServer;\n    private static boolean planLogged;\n    private static boolean ciPassed;\n    private static int lastFulfilledHouseholds;\n''',
    '''    private static MinecraftServer activeServer;\n    private static boolean planLogged;\n    private static boolean ciPassed;\n    private static long lastCiChunkRefreshTick = Long.MIN_VALUE;\n    private static int lastCiPendingSamples = -1;\n    private static int lastFulfilledHouseholds;\n''',
    'economy ci field')
replace_once(
    economy,
    '''        ensureEconomy(economy, population);\n        logPlanOnce(economy);\n\n        if (level.getGameTime() % SYNC_INTERVAL == 0L) {\n''',
    '''        ensureEconomy(economy, population);\n        logPlanOnce(economy);\n        refreshCiSampleChunks(level, economy);\n\n        if (level.getGameTime() % SYNC_INTERVAL == 0L) {\n''',
    'economy ci request call')
replace_once(
    economy,
    '''        planLogged = false;\n        ciPassed = false;\n        lastFulfilledHouseholds = 0;\n''',
    '''        planLogged = false;\n        ciPassed = false;\n        lastCiChunkRefreshTick = Long.MIN_VALUE;\n        lastCiPendingSamples = -1;\n        lastFulfilledHouseholds = 0;\n''',
    'economy reset')
replace_once(
    economy,
    '''    private static void verifyCiIfReady(\n            ServerLevel level,\n            ErdenPhysicalEconomySavedData economy) {\n''',
    '''    private static void refreshCiSampleChunks(\n            ServerLevel level,\n            ErdenPhysicalEconomySavedData economy) {\n        if (ciPassed\n                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;\n        long tick = level.getGameTime();\n        if (lastCiChunkRefreshTick != Long.MIN_VALUE\n                && tick - lastCiChunkRefreshTick < 40L) return;\n        lastCiChunkRefreshTick = tick;\n\n        List<ExternalUrbanFabricBuilder.UrbanEntrance> samples = ciEntrances();\n        if (samples.size() != 3) {\n            throw new IllegalStateException(\n                    "Erden physical-economy CI expected three authored sample sites, found "\n                            + samples.size());\n        }\n\n        boolean firstRefresh = lastCiPendingSamples < 0;\n        int pending = 0;\n        List<String> statuses = new ArrayList<>();\n        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : samples) {\n            ErdenPhysicalEconomySavedData.SiteState site = findSite(\n                    economy.sites(), entrance.x(), entrance.z());\n            boolean ready = site != null && isSiteReady(level, site);\n            Container container = ready ? primaryContainer(level, site) : null;\n            long visible = 0L;\n            if (container != null) {\n                for (ResourceItem resource : PHYSICAL_RESOURCES) {\n                    visible += countItem(container, resource.item);\n                }\n            }\n            boolean materialized = site != null && site.materialized();\n            boolean complete = materialized && container != null && visible > 0L;\n            if (!complete) {\n                ErdenUrbanInteriorBuilder.requestPlanChunksForCi(level, entrance);\n                pending++;\n            }\n            statuses.add(\n                    "%s@%d,%d:ready=%s,materialized=%s,container=%s,visible=%d".formatted(\n                            entrance.role(), entrance.x(), entrance.z(), ready, materialized,\n                            container != null, visible));\n        }\n\n        if (firstRefresh) {\n            LivingKingdoms.LOGGER.info(\n                    "Requested Erden physical-economy authored interior CI samples sites={} roles={} bounded_plan_chunks=true refresh_ticks=40 loaded_lease=true persistent_forced_chunks=false",\n                    samples.size(),\n                    samples.stream().map(ExternalUrbanFabricBuilder.UrbanEntrance::role).toList());\n        }\n        if (pending != lastCiPendingSamples) {\n            LivingKingdoms.LOGGER.info(\n                    "Refreshed Erden physical-economy authored interior CI samples pending={} sites={} bounded_plan_chunks=true refresh_ticks=40 loaded_lease=true persistent_forced_chunks=false",\n                    pending, statuses);\n            lastCiPendingSamples = pending;\n        }\n    }\n\n    private static void verifyCiIfReady(\n            ServerLevel level,\n            ErdenPhysicalEconomySavedData economy) {\n''',
    'economy ci method')

interior = ROOT / 'ErdenUrbanInteriorBuilder.java'
replace_once(
    interior,
    '''    private static void requestCiSampleChunks(ServerLevel level) {\n        if (ciChunksRequested || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;\n        ExternalUrbanFabricBuilder.UrbanEntrance entrance = ExternalUrbanFabricBuilder.diagnosticEntrance();\n        ErdenUrbanAuthoredGroundPlanCatalog.PlacementPlan plan =\n                ErdenUrbanAuthoredGroundPlanCatalog.plan(entrance);\n        if (plan == null) throw new IllegalStateException("Missing authored urban CI plan");\n        for (BlockPos pos : planPositions(plan)) {\n            int centerChunkX = pos.getX() >> 4;\n            int centerChunkZ = pos.getZ() >> 4;\n            for (int chunkX = centerChunkX - 1; chunkX <= centerChunkX + 1; chunkX++) {\n                for (int chunkZ = centerChunkZ - 1; chunkZ <= centerChunkZ + 1; chunkZ++) {\n                    ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);\n                }\n            }\n        }\n        ciChunksRequested = true;\n    }\n''',
    '''    private static void requestCiSampleChunks(ServerLevel level) {\n        if (ciChunksRequested || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;\n        requestPlanChunksForCi(level, ExternalUrbanFabricBuilder.diagnosticEntrance());\n        ciChunksRequested = true;\n    }\n\n    static void requestPlanChunksForCi(\n            ServerLevel level,\n            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {\n        if (!"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;\n        ErdenUrbanAuthoredGroundPlanCatalog.PlacementPlan plan =\n                ErdenUrbanAuthoredGroundPlanCatalog.plan(entrance);\n        if (plan == null) {\n            throw new IllegalStateException(\n                    "Missing authored urban CI plan role=" + entrance.role()\n                            + " entrance=" + entrance.x() + "," + entrance.z());\n        }\n        for (BlockPos pos : planPositions(plan)) {\n            int centerChunkX = pos.getX() >> 4;\n            int centerChunkZ = pos.getZ() >> 4;\n            for (int chunkX = centerChunkX - 1; chunkX <= centerChunkX + 1; chunkX++) {\n                for (int chunkZ = centerChunkZ - 1; chunkZ <= centerChunkZ + 1; chunkZ++) {\n                    ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);\n                    ErdenCapitalStreamingBuilder.retainDiagnosticChunk(level, chunkX, chunkZ);\n                }\n            }\n        }\n    }\n''',
    'interior reusable ci plan request')

citadel = ROOT / 'ErdenCitadelInteriorManager.java'
replace_once(
    citadel,
    '''    private static void retainCitadelForCi(\n            ServerLevel level,\n            RealmSiteLayoutSavedData.RealmSite site) {\n        if (ciTicketHeld || !ciMode()) return;\n        ciTicketCenter = new ChunkPos(site.centerX() >> 4, site.centerZ() >> 4);\n        level.getChunkSource().addTicketAndLoadWithRadius(\n                TicketType.PORTAL, ciTicketCenter, CI_CHUNK_RADIUS);\n        ciTicketHeld = true;\n        LivingKingdoms.LOGGER.info(\n                "Retained Erden citadel for CI zoning audit centre_chunk={},{} radius={} transient_ticket=portal forced_chunks=false synchronous_get_chunk=false",\n                ciTicketCenter.x(), ciTicketCenter.z(), CI_CHUNK_RADIUS);\n    }\n''',
    '''    private static void retainCitadelForCi(\n            ServerLevel level,\n            RealmSiteLayoutSavedData.RealmSite site) {\n        if (!ciMode()) return;\n        ChunkPos requestedCenter = new ChunkPos(site.centerX() >> 4, site.centerZ() >> 4);\n        if (ciTicketHeld && ciTicketCenter != null && !ciTicketCenter.equals(requestedCenter)) {\n            releaseCiTicket(level);\n        }\n        ciTicketCenter = requestedCenter;\n        // PORTAL tickets are transient. Refresh the same bounded ticket every zoning pass so a\n        // long fresh-world audit cannot lose the final zone after the ticket timeout expires.\n        level.getChunkSource().addTicketAndLoadWithRadius(\n                TicketType.PORTAL, ciTicketCenter, CI_CHUNK_RADIUS);\n        if (!ciTicketHeld) {\n            LivingKingdoms.LOGGER.info(\n                    "Retained Erden citadel for CI zoning audit centre_chunk={},{} radius={} transient_ticket=portal refreshed_until_verification=true forced_chunks=false synchronous_get_chunk=false",\n                    ciTicketCenter.x(), ciTicketCenter.z(), CI_CHUNK_RADIUS);\n        }\n        ciTicketHeld = true;\n    }\n''',
    'citadel transient ticket refresh')

print('Living Kingdoms CI residency patch applied')
