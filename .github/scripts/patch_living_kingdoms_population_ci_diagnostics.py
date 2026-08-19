from pathlib import Path

path = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenPopulationManager.java')
text = path.read_text(encoding='utf-8')


def replace_once(old: str, new: str, label: str) -> None:
    global text
    if new in text:
        return
    if old not in text:
        raise SystemExit(f'{label}: expected source block not found')
    text = text.replace(old, new, 1)

replace_once(
    '''    private static boolean ciChunksRequested;\n    private static boolean ciPassed;\n''',
    '''    private static boolean ciChunksRequested;\n    private static boolean ciPassed;\n    private static long lastCiDiagnosticTick = Long.MIN_VALUE;\n''',
    'population CI diagnostic field')
replace_once(
    '''        ciChunksRequested = false;\n        ciPassed = false;\n''',
    '''        ciChunksRequested = false;\n        ciPassed = false;\n        lastCiDiagnosticTick = Long.MIN_VALUE;\n''',
    'population CI diagnostic reset')
replace_once(
    '''        runResidentRoutines(level, population);\n        verifyCiIfReady(level, population);\n''',
    '''        runResidentRoutines(level, population);\n        logCiStateIfNeeded(level, population);\n        verifyCiIfReady(level, population);\n''',
    'population CI diagnostic hook')

marker = '''    private static void verifyCiIfReady(\n            ServerLevel level,\n            ErdenPopulationSavedData population) {\n'''
helper = r'''    private static void logCiStateIfNeeded(
            ServerLevel level,
            ErdenPopulationSavedData population) {
        if (ciPassed
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || population.households().isEmpty()) return;
        long tick = level.getGameTime();
        if (lastCiDiagnosticTick != Long.MIN_VALUE && tick - lastCiDiagnosticTick < 40L) return;
        lastCiDiagnosticTick = tick;

        ErdenPopulationSavedData.Household sample = population.households().getFirst();
        ExternalUrbanFabricBuilder.UrbanEntrance home = findEntrance(sample.homeX(), sample.homeZ());
        if (home == null) {
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_POPULATION_CI_STATE home={},{} stage=home_entrance_missing tick={}",
                    sample.homeX(), sample.homeZ(), tick);
            return;
        }
        long homeKey = positionKey(home.x(), home.z());
        boolean homeChunkLoaded = level.hasChunk(home.x() >> 4, home.z() >> 4);
        boolean groundPlanned = ErdenUrbanAuthoredGroundPlanCatalog.plan(home) != null;
        boolean groundComplete = groundPlanned
                && level.getDataStorage().computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE)
                        .isComplete(homeKey, ErdenUrbanInteriorBuilder.INTERIOR_REVISION);
        boolean upperEligible = ErdenUrbanAuthoredUpperRouteManager.isEligible(home);
        boolean upperPrepared = upperEligible
                && ErdenUrbanAuthoredUpperRouteManager.isPrepared(level, home);
        boolean upperComplete = upperEligible
                && ErdenUrbanAuthoredUpperRouteManager.isCompleted(level, home);
        BlockPos upperTarget = upperComplete
                ? ErdenUrbanAuthoredUpperRouteManager.verifiedUpperTarget(level, home) : null;
        boolean residenceReady = ErdenUrbanResidenceResolver.isResidenceReady(level, home);

        java.util.ArrayList<String> targets = new java.util.ArrayList<>();
        java.util.HashSet<String> sampleNames = new java.util.HashSet<>();
        for (ErdenPopulationSavedData.Resident resident : sample.residents()) {
            sampleNames.add(resident.name());
            BlockPos target = ErdenUrbanResidenceResolver.resolveHomeTarget(
                    level, home, resident.bedSlot());
            targets.add(resident.id() + "=" + target);
        }
        int spawned = level.getEntitiesOfClass(
                Villager.class, capitalBounds(level),
                villager -> sampleNames.contains(villager.getName().getString())).size();

        ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement = null;
        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement candidate
                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {
            if (candidate.entrance().x() == home.x() && candidate.entrance().z() == home.z()) {
                placement = candidate;
                break;
            }
        }
        java.util.ArrayList<String> chunkStates = new java.util.ArrayList<>();
        String fragment = "missing";
        if (placement != null) {
            fragment = placement.fragmentKey();
            int minChunkX = Math.floorDiv(placement.minX(), 16) - 1;
            int maxChunkX = Math.floorDiv(placement.maxX(), 16) + 1;
            int minChunkZ = Math.floorDiv(placement.minZ(), 16) - 1;
            int maxChunkZ = Math.floorDiv(placement.maxZ(), 16) + 1;
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    chunkStates.add(ErdenCapitalStreamingBuilder.diagnosticChunkState(
                            level, chunkX, chunkZ));
                }
            }
        }

        String stage = !homeChunkLoaded ? "home_chunk_unloaded"
                : !groundPlanned ? "ground_plan_missing"
                : !groundComplete ? "ground_incomplete"
                : !upperEligible ? "upper_not_eligible"
                : !upperPrepared ? "upper_not_prepared"
                : !upperComplete ? "upper_incomplete"
                : upperTarget == null ? "upper_target_missing"
                : !residenceReady ? "residence_not_ready"
                : spawned < MEMBERS_PER_HOUSEHOLD ? "residents_not_spawned"
                : population.totalShortage() != 0L ? "shortage"
                : "ready";
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_POPULATION_CI_STATE home={},{} role={} fragment={} ground_complete={} upper_eligible={} upper_prepared={} upper_complete={} upper_target={} residence_ready={} spawned={} targets={} shortages={} stage={} chunks={} tick={} persistent_forced_chunks=false",
                home.x(), home.z(), home.role(), fragment, groundComplete, upperEligible,
                upperPrepared, upperComplete, upperTarget, residenceReady, spawned, targets,
                population.totalShortage(), stage, chunkStates, tick);
    }

'''
if helper not in text:
    if marker not in text:
        raise SystemExit('population verify marker not found')
    text = text.replace(marker, helper + marker, 1)

if 'LK_ERDEN_POPULATION_CI_STATE' not in text:
    raise SystemExit('population CI state marker missing')
if 'diagnosticChunkState' not in text:
    raise SystemExit('population placement chunk-state diagnostics missing')

path.write_text(text, encoding='utf-8')
print('Living Kingdoms population CI stage diagnostics installed')
