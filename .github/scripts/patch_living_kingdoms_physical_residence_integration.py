from pathlib import Path

builder_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java")
builder = builder_path.read_text(encoding="utf-8")

old = """        ChunkPos chunk = event.getChunk().getPos();
        if (!intersectsExterior(chunk)) return;
        long packed = pack(chunk.x(), chunk.z());
        if (isCi() && !CI_REQUIRED.contains(packed)) return;
        enqueue(level, packed, false);
"""
new = """        ChunkPos chunk = event.getChunk().getPos();
        boolean exteriorChunk = intersectsExterior(chunk);
        boolean residenceChunk = ErdenExteriorResidenceCatalog.residenceChunk(
                chunk.x(), chunk.z());
        if (!exteriorChunk && !residenceChunk) return;
        long packed = pack(chunk.x(), chunk.z());
        if (isCi() && !CI_REQUIRED.contains(packed)) return;
        enqueue(level, packed, false);
"""
if old in builder:
    builder = builder.replace(old, new, 1)
elif new not in builder:
    raise SystemExit("onChunkLoad residence integration point missing")

old = """        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        data.markChunk(active.packed, EXTERIOR_REVISION, active.plan.appliedWrites());
        markCompletedNodeAnchors(data);
        QUEUED.remove(active.packed);
"""
new = """        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        if (active.buildExterior) {
            data.markChunk(active.packed, EXTERIOR_REVISION, active.plan.appliedWrites());
            markCompletedNodeAnchors(data);
        }
        if (active.buildResidences) {
            List<ErdenExteriorResidenceCatalog.ResidencePlot> plots =
                    ErdenExteriorResidenceCatalog.forChunk(active.chunkX, active.chunkZ);
            for (ErdenExteriorResidenceCatalog.ResidencePlot plot : plots) {
                if (!ErdenExteriorResidenceBuilder.validateLoadedResidence(level, plot)) {
                    throw new IllegalStateException(
                            "Invalid Erden exterior residence " + plot.householdId());
                }
            }
            ErdenExteriorResidenceSavedData residences = level.getDataStorage()
                    .computeIfAbsent(ErdenExteriorResidenceSavedData.TYPE);
            residences.markChunk(
                    active.chunkX, active.chunkZ,
                    ErdenExteriorResidenceBuilder.RESIDENCE_REVISION,
                    plots, active.plan.appliedWrites());
        }
        QUEUED.remove(active.packed);
"""
if old in builder:
    builder = builder.replace(old, new, 1)
elif new not in builder:
    raise SystemExit("active completion residence integration point missing")

anchor_method = """    public static boolean anchorBuilt(
            ServerLevel level,
            ErdenKingdomSupplyCatalog.SupplyNode node) {
        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        return data.nodeComplete(node.id, EXTERIOR_REVISION);
    }

"""
anchor_plus = anchor_method + """    public static boolean residenceBuilt(ServerLevel level, String householdId) {
        return level.getDataStorage().computeIfAbsent(ErdenExteriorResidenceSavedData.TYPE)
                .householdBuilt(
                        householdId,
                        ErdenExteriorResidenceBuilder.RESIDENCE_REVISION);
    }

"""
if "public static boolean residenceBuilt(ServerLevel level, String householdId)" not in builder:
    if builder.count(anchor_method) != 1:
        raise SystemExit("residenceBuilt insertion point missing")
    builder = builder.replace(anchor_method, anchor_plus, 1)

old = """        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        for (int forced = 0; forced < CI_FORCE_BUDGET
                && !CI_REQUESTS.isEmpty()
                && RETAINED.size() < CI_MAX_IN_FLIGHT; forced++) {
            long packed = CI_REQUESTS.removeFirst();
            if (!data.needs(packed, EXTERIOR_REVISION)) continue;
"""
new = """        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        ErdenExteriorResidenceSavedData residences = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorResidenceSavedData.TYPE);
        for (int forced = 0; forced < CI_FORCE_BUDGET
                && !CI_REQUESTS.isEmpty()
                && RETAINED.size() < CI_MAX_IN_FLIGHT; forced++) {
            long packed = CI_REQUESTS.removeFirst();
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            if (!data.needs(packed, EXTERIOR_REVISION)
                    && !residences.needsChunk(
                    chunkX, chunkZ,
                    ErdenExteriorResidenceBuilder.RESIDENCE_REVISION)) continue;
"""
if old in builder:
    builder = builder.replace(old, new, 1)
elif new not in builder:
    raise SystemExit("advance CI combined need point missing")
# Remove now-duplicated declarations in the modified block.
builder = builder.replace(
    """            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            if (level.hasChunk(chunkX, chunkZ)) {
""",
    """            if (level.hasChunk(chunkX, chunkZ)) {
""",
    1)

old = """    private static void enqueue(ServerLevel level, long packed, boolean priority) {
        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        if (!data.needs(packed, EXTERIOR_REVISION)) {
            release(level, packed);
            return;
        }
"""
new = """    private static void enqueue(ServerLevel level, long packed, boolean priority) {
        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        ErdenExteriorResidenceSavedData residences = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorResidenceSavedData.TYPE);
        int chunkX = unpackX(packed);
        int chunkZ = unpackZ(packed);
        if (!data.needs(packed, EXTERIOR_REVISION)
                && !residences.needsChunk(
                chunkX, chunkZ,
                ErdenExteriorResidenceBuilder.RESIDENCE_REVISION)) {
            release(level, packed);
            return;
        }
"""
if old in builder:
    builder = builder.replace(old, new, 1)
elif new not in builder:
    raise SystemExit("enqueue combined need point missing")

old = """    private static void startNext(ServerLevel level) {
        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        while (!PENDING.isEmpty()) {
            long packed = PENDING.removeFirst();
            if (!data.needs(packed, EXTERIOR_REVISION)) {
                QUEUED.remove(packed);
                release(level, packed);
                continue;
            }
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
"""
new = """    private static void startNext(ServerLevel level) {
        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        ErdenExteriorResidenceSavedData residences = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorResidenceSavedData.TYPE);
        while (!PENDING.isEmpty()) {
            long packed = PENDING.removeFirst();
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            boolean buildExterior = data.needs(packed, EXTERIOR_REVISION);
            boolean buildResidences = residences.needsChunk(
                    chunkX, chunkZ,
                    ErdenExteriorResidenceBuilder.RESIDENCE_REVISION);
            if (!buildExterior && !buildResidences) {
                QUEUED.remove(packed);
                release(level, packed);
                continue;
            }
"""
if old in builder:
    builder = builder.replace(old, new, 1)
elif new not in builder:
    raise SystemExit("startNext combined need point missing")

old = """            ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
            IncrementalWorldEditPlan plan = createChunkPlan(level, chunk);
            active = new ActiveChunk(packed, chunkX, chunkZ, plan);
            LivingKingdoms.LOGGER.debug(
                    "Prepared Erden exterior chunk {},{} writes={} operations={}",
                    chunkX, chunkZ, plan.estimatedWrites(), plan.operationCount());
"""
new = """            ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
            IncrementalWorldEditPlan plan = createChunkPlan(
                    level, chunk, buildExterior, buildResidences);
            active = new ActiveChunk(
                    packed, chunkX, chunkZ,
                    buildExterior, buildResidences, plan);
            LivingKingdoms.LOGGER.debug(
                    "Prepared Erden exterior chunk {},{} writes={} operations={} exterior={} residences={}",
                    chunkX, chunkZ, plan.estimatedWrites(), plan.operationCount(),
                    buildExterior, buildResidences);
"""
if old in builder:
    builder = builder.replace(old, new, 1)
elif new not in builder:
    raise SystemExit("startNext plan creation point missing")

old = """    private static IncrementalWorldEditPlan createChunkPlan(ServerLevel level, ChunkPos chunk) {
        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan();
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
"""
new = """    private static IncrementalWorldEditPlan createChunkPlan(
            ServerLevel level,
            ChunkPos chunk,
            boolean buildExterior,
            boolean buildResidences) {
        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan();
        if (buildExterior) {
            for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
"""
if old in builder:
    builder = builder.replace(old, new, 1)
elif new not in builder:
    raise SystemExit("createChunkPlan signature point missing")

old = """            addStorageYard(plan, chunk, node);
        }
        return plan;
    }
"""
new = """                addStorageYard(plan, chunk, node);
            }
        }
        if (buildResidences) {
            for (ErdenExteriorResidenceCatalog.ResidencePlot plot :
                    ErdenExteriorResidenceCatalog.forChunk(chunk.x(), chunk.z())) {
                ErdenExteriorResidenceBuilder.addChunk(plan, level, chunk, plot);
            }
        }
        return plan;
    }
"""
if old in builder:
    builder = builder.replace(old, new, 1)
elif new not in builder:
    raise SystemExit("createChunkPlan residence tail point missing")

old = """        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        if (data.completedNodeCount(EXTERIOR_REVISION) != ErdenKingdomSupplyCatalog.nodes().size()
                || data.builtChunkCount(EXTERIOR_REVISION) < 70
                || data.totalWrites(EXTERIOR_REVISION) <= 0L) return;
"""
new = """        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        ErdenExteriorResidenceSavedData residences = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorResidenceSavedData.TYPE);
        if (data.completedNodeCount(EXTERIOR_REVISION) != ErdenKingdomSupplyCatalog.nodes().size()
                || data.builtChunkCount(EXTERIOR_REVISION) < 70
                || data.totalWrites(EXTERIOR_REVISION) <= 0L
                || residences.builtChunkCount(
                ErdenExteriorResidenceBuilder.RESIDENCE_REVISION)
                != ErdenExteriorResidenceCatalog.EXPECTED_RESIDENCES
                || residences.builtHouseholdCount(
                ErdenExteriorResidenceBuilder.RESIDENCE_REVISION)
                != ErdenExteriorResidenceCatalog.EXPECTED_RESIDENCES
                || residences.totalWrites(
                ErdenExteriorResidenceBuilder.RESIDENCE_REVISION) <= 0L
                || !residences.missingHouseholds(
                ErdenExteriorResidenceBuilder.RESIDENCE_REVISION).isEmpty()) return;
"""
if old in builder:
    builder = builder.replace(old, new, 1)
elif new not in builder:
    raise SystemExit("verify CI residence condition point missing")

old = """        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_KINGDOM_EXTERIOR_PASS revision={} nodes={} producers={} wharves={} anchor_chunks={} writes={} metre_scale=true streamed=true external_buildings=true fields=true paddocks=true mines=true mills=true docks=true roads=true storage_yards=true debris_zero=true",
                EXTERIOR_REVISION, ErdenKingdomSupplyCatalog.nodes().size(),
                ErdenKingdomSupplyCatalog.producerCount(), ErdenKingdomSupplyCatalog.wharfCount(),
                data.builtChunkCount(EXTERIOR_REVISION), data.totalWrites(EXTERIOR_REVISION));
"""
new = """        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_KINGDOM_EXTERIOR_PASS revision={} nodes={} producers={} wharves={} anchor_chunks={} writes={} residences={} attached_quarters={} detached_cottages={} residence_chunks={} doors={} beds={} storage={} hearths={} metre_scale=true streamed=true external_buildings=true fields=true paddocks=true mines=true mills=true docks=true roads=true storage_yards=true physical_residences=true access_paths=true debris_zero=true",
                EXTERIOR_REVISION, ErdenKingdomSupplyCatalog.nodes().size(),
                ErdenKingdomSupplyCatalog.producerCount(), ErdenKingdomSupplyCatalog.wharfCount(),
                data.builtChunkCount(EXTERIOR_REVISION), data.totalWrites(EXTERIOR_REVISION),
                ErdenExteriorResidenceCatalog.EXPECTED_RESIDENCES,
                ErdenExteriorResidenceCatalog.EXPECTED_ATTACHED_QUARTERS,
                ErdenExteriorResidenceCatalog.EXPECTED_DETACHED_COTTAGES,
                residences.builtChunkCount(ErdenExteriorResidenceBuilder.RESIDENCE_REVISION),
                ErdenExteriorResidenceCatalog.EXPECTED_RESIDENCES,
                ErdenExteriorResidenceCatalog.EXPECTED_RESIDENCES
                        * ErdenExteriorResidenceBuilder.BEDS_PER_RESIDENCE,
                ErdenExteriorResidenceCatalog.EXPECTED_RESIDENCES,
                ErdenExteriorResidenceCatalog.EXPECTED_RESIDENCES);
"""
if old in builder:
    builder = builder.replace(old, new, 1)
elif new not in builder:
    raise SystemExit("exterior pass marker residence point missing")

old = """    private record ActiveChunk(
            long packed,
            int chunkX,
            int chunkZ,
            IncrementalWorldEditPlan plan) {
    }
"""
new = """    private record ActiveChunk(
            long packed,
            int chunkX,
            int chunkZ,
            boolean buildExterior,
            boolean buildResidences,
            IncrementalWorldEditPlan plan) {
    }
"""
if old in builder:
    builder = builder.replace(old, new, 1)
elif new not in builder:
    raise SystemExit("ActiveChunk residence fields point missing")

builder_path.write_text(builder, encoding="utf-8")

workforce_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorWorkforceManager.java")
workforce = workforce_path.read_text(encoding="utf-8")

old = """            int requiredWorkers = requiredWorkers(node.role);
            int householdCount = (requiredWorkers + 1) / 2;
            int assignedWorkers = 0;
            for (int localHousehold = 0; localHousehold < householdCount; localHousehold++) {
                int[] offset = HOME_OFFSETS[localHousehold];
                String householdId = "erden_exterior_household_%03d".formatted(globalHousehold + 1);
"""
new = """            int requiredWorkers = requiredWorkers(node.role);
            List<ErdenExteriorResidenceCatalog.ResidencePlot> residencePlots =
                    ErdenExteriorResidenceCatalog.forNode(node.id);
            int householdCount = residencePlots.size();
            int assignedWorkers = 0;
            for (int localHousehold = 0; localHousehold < householdCount; localHousehold++) {
                ErdenExteriorResidenceCatalog.ResidencePlot residence =
                        residencePlots.get(localHousehold);
                String householdId = residence.householdId();
"""
if old in workforce:
    workforce = workforce.replace(old, new, 1)
elif new not in workforce:
    raise SystemExit("workforce residence catalog point missing")

old = """                        householdId, familyName, node.id, node.role,
                        node.x + offset[0], node.z + offset[1], residents));
"""
new = """                        householdId, familyName, node.id, node.role,
                        residence.parcelX(), residence.parcelZ(), residents));
"""
if old in workforce:
    workforce = workforce.replace(old, new, 1)
elif new not in workforce:
    raise SystemExit("workforce residence coordinates point missing")

old = """            if (node == null
                    || !ErdenKingdomExteriorBuilder.anchorBuilt(level, node)
                    || !level.hasChunk(household.homeX() >> 4, household.homeZ() >> 4)) continue;
"""
new = """            if (node == null
                    || !ErdenKingdomExteriorBuilder.anchorBuilt(level, node)
                    || !ErdenKingdomExteriorBuilder.residenceBuilt(level, household.id())
                    || !level.hasChunk(household.homeX() >> 4, household.homeZ() >> 4)) continue;
"""
if old in workforce:
    workforce = workforce.replace(old, new, 1)
elif new not in workforce:
    raise SystemExit("workforce residence-built spawn gate missing")

old = """        int x = household.homeX() + resident.bedSlot() - 1;
        int z = household.homeZ() + 2 + resident.bedSlot();
        int preferredY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z)) + 2;
        int standingY = safeStandingY(level, x, preferredY, z);
"""
new = """        BlockPos spawn = ErdenExteriorResidenceBuilder.residentSpawnPosition(
                household.id(), resident.bedSlot());
        int x = spawn.getX();
        int z = spawn.getZ();
        int standingY = safeStandingY(level, x, spawn.getY(), z);
"""
if old in workforce:
    workforce = workforce.replace(old, new, 1)
elif new not in workforce:
    raise SystemExit("workforce physical residence spawn point missing")

old = """        int x = workplace ? node.x : reference.household().homeX();
        int z = workplace ? node.z : reference.household().homeZ();
        if (!level.hasChunk(x >> 4, z >> 4)) return null;
        int preferredY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z)) + 2;
        return new Target(x, safeStandingY(level, x, preferredY, z), z);
"""
new = """        BlockPos destination = workplace
                ? new BlockPos(
                node.x,
                (int) Math.round(AuthoredContinentDensity.surfaceHeight(node.x, node.z)) + 2,
                node.z)
                : ErdenExteriorResidenceBuilder.homeTarget(reference.household().id());
        int x = destination.getX();
        int z = destination.getZ();
        if (!level.hasChunk(x >> 4, z >> 4)) return null;
        return new Target(x, safeStandingY(level, x, destination.getY(), z), z);
"""
if old in workforce:
    workforce = workforce.replace(old, new, 1)
elif new not in workforce:
    raise SystemExit("workforce physical home target point missing")

workforce_path.write_text(workforce, encoding="utf-8")

lifecycle_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorLifecycleManager.java")
lifecycle = lifecycle_path.read_text(encoding="utf-8")

old = """            if (household == null
                    || !level.hasChunk(household.homeX() >> 4, household.homeZ() >> 4)) continue;
"""
new = """            if (household == null
                    || !ErdenKingdomExteriorBuilder.residenceBuilt(level, household.id())
                    || !level.hasChunk(household.homeX() >> 4, household.homeZ() >> 4)) continue;
"""
if old in lifecycle:
    lifecycle = lifecycle.replace(old, new, 1)
elif new not in lifecycle:
    raise SystemExit("descendant residence-built spawn gate missing")

old = """        int slot = Math.floorMod(person.id().hashCode(), 4);
        int x = household.homeX() + slot - 1;
        int z = household.homeZ() + 4 + (slot & 1);
        int preferredY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z)) + 2;
        villager.setPos(x + 0.5D, safeStandingY(level, x, preferredY, z), z + 0.5D);
"""
new = """        int slot = Math.floorMod(person.id().hashCode(), 4);
        BlockPos spawn = ErdenExteriorResidenceBuilder.residentSpawnPosition(
                household.id(), slot);
        int x = spawn.getX();
        int z = spawn.getZ();
        villager.setPos(
                x + 0.5D,
                safeStandingY(level, x, spawn.getY(), z),
                z + 0.5D);
"""
if old in lifecycle:
    lifecycle = lifecycle.replace(old, new, 1)
elif new not in lifecycle:
    raise SystemExit("descendant physical residence spawn point missing")

old = """            int x = working ? node.x : household.homeX();
            int z = working ? node.z : household.homeZ();
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            int preferredY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z)) + 2;
            int y = safeStandingY(level, x, preferredY, z);
"""
new = """            BlockPos destination = working
                    ? new BlockPos(
                    node.x,
                    (int) Math.round(AuthoredContinentDensity.surfaceHeight(node.x, node.z)) + 2,
                    node.z)
                    : ErdenExteriorResidenceBuilder.homeTarget(household.id());
            int x = destination.getX();
            int z = destination.getZ();
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            int y = safeStandingY(level, x, destination.getY(), z);
"""
if old in lifecycle:
    lifecycle = lifecycle.replace(old, new, 1)
elif new not in lifecycle:
    raise SystemExit("lifecycle physical home target point missing")

lifecycle_path.write_text(lifecycle, encoding="utf-8")
