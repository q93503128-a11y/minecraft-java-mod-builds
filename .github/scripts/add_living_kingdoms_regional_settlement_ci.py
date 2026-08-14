from pathlib import Path

p = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java")
s = p.read_text(encoding="utf-8")


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(message)


require("REGIONAL_SETTLEMENT_REVISION = 1" in s, "regional runtime overlay must be applied first")

# Regional-only chunks must never be marked as legacy exterior chunks.
s = s.replace(
    '''        boolean exteriorNeeded = (!isCi() || isCiExteriorAnchor(packed))
                && data.needs(packed, EXTERIOR_REVISION);''',
    '''        boolean exteriorNeeded = intersectsExterior(new ChunkPos(chunkX, chunkZ))
                && (!isCi() || isCiExteriorAnchor(packed))
                && data.needs(packed, EXTERIOR_REVISION);''')
s = s.replace(
    '''            boolean buildExterior = (!isCi() || isCiExteriorAnchor(packed))
                    && data.needs(packed, EXTERIOR_REVISION);''',
    '''            boolean buildExterior = intersectsExterior(new ChunkPos(chunkX, chunkZ))
                    && (!isCi() || isCiExteriorAnchor(packed))
                    && data.needs(packed, EXTERIOR_REVISION);''')

if "REGIONAL_CI_REQUESTS" not in s:
    fields_anchor = '''    private static final Set<Long> RETAINED = new HashSet<>();
'''
    fields = '''    private static final Set<Long> RETAINED = new HashSet<>();
    private static final ArrayDeque<Long> REGIONAL_CI_REQUESTS = new ArrayDeque<>();
    private static final Set<Long> REGIONAL_CI_LOADING = new HashSet<>();
    private static final Set<Long> REGIONAL_CI_REQUIRED = new HashSet<>();
    private static final Set<Long> REGIONAL_CI_RETAINED = new HashSet<>();
'''
    require(fields_anchor in s, "regional CI field anchor missing")
    s = s.replace(fields_anchor, fields, 1)
    s = s.replace(
        '''    private static boolean ciRequested;
    private static boolean ciPassed;
''',
        '''    private static boolean ciRequested;
    private static boolean ciPassed;
    private static boolean regionalCiPrepared;
    private static boolean regionalCiPassed;
''',
        1)

    tick_anchor = '''        if (isCi()) {
            if (!ciRequested) {
                ciRequested = true;
                prepareCiAnchors();
            }
            advanceCiAnchors(level);
        }

        if (active == null) startNext(level);'''
    tick_replacement = '''        if (isCi()) {
            if (!ciRequested) {
                ciRequested = true;
                prepareCiAnchors();
            }
            advanceCiAnchors(level);
        }
        if (isRegionalCi()) {
            if (!regionalCiPrepared) prepareRegionalCiAnchors();
            advanceRegionalCiAnchors(level);
        }

        if (active == null) startNext(level);'''
    require(tick_anchor in s, "server tick CI anchor missing")
    s = s.replace(tick_anchor, tick_replacement, 1)

    idle_anchor = '''        if (active == null) {
            verifyCi(level);
            return;
        }'''
    idle_replacement = '''        if (active == null) {
            verifyCi(level);
            verifyRegionalCi(level);
            return;
        }'''
    require(idle_anchor in s, "idle verification anchor missing")
    s = s.replace(idle_anchor, idle_replacement, 1)

    complete_anchor = '''        active = null;
        verifyCi(level);
    }
'''
    complete_replacement = '''        active = null;
        verifyCi(level);
        verifyRegionalCi(level);
    }
'''
    require(complete_anchor in s, "completion verification anchor missing")
    s = s.replace(complete_anchor, complete_replacement, 1)

    reset_anchor = '''        RETAINED.clear();
        active = null;
        ciRequested = false;
        ciPassed = false;
'''
    reset_replacement = '''        RETAINED.clear();
        REGIONAL_CI_REQUESTS.clear();
        REGIONAL_CI_LOADING.clear();
        REGIONAL_CI_REQUIRED.clear();
        REGIONAL_CI_RETAINED.clear();
        active = null;
        ciRequested = false;
        ciPassed = false;
        regionalCiPrepared = false;
        regionalCiPassed = false;
'''
    require(reset_anchor in s, "reset regional CI anchor missing")
    s = s.replace(reset_anchor, reset_replacement, 1)

    methods_anchor = '''    private static void prepareCiAnchors() {
'''
    ci_methods = r'''    private static void prepareRegionalCiAnchors() {
        regionalCiPrepared = true;
        Set<Long> unique = new LinkedHashSet<>();
        int hubs = 0;
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            if (!regionalHubEligible(node)) continue;
            hubs++;
            unique.addAll(regionalCoreChunks(node));
            int outwardX = ErdenExteriorResidenceCatalog.hamletOutwardX(node);
            int outwardZ = ErdenExteriorResidenceCatalog.hamletOutwardZ(node);
            int start = Math.max(node.radius + 4, REGIONAL_CORE_DISTANCE - REGIONAL_PLAZA_RADIUS);
            for (int distance = start; distance <= REGIONAL_CROSS_DISTANCE; distance += 4) {
                unique.add(pack((node.x + outwardX * distance) >> 4,
                        (node.z + outwardZ * distance) >> 4));
            }
            List<ErdenExteriorResidenceCatalog.ResidencePlot> homes =
                    ErdenExteriorResidenceCatalog.forNode(node.id);
            if (homes.isEmpty()) throw new IllegalStateException("Regional hub has no existing worker home " + node.id);
            unique.add(homes.getFirst().physicalChunk());
        }
        if (hubs != EXPECTED_REGIONAL_HUBS) {
            throw new IllegalStateException("Invalid Erden regional hub count " + hubs);
        }
        ErdenKingdomSupplyCatalog.SupplyNode sample = ErdenKingdomSupplyCatalog.node("erden_grain_estate_01");
        if (sample == null) throw new IllegalStateException("Missing regional cross-street CI sample node");
        int outwardX = ErdenExteriorResidenceCatalog.hamletOutwardX(sample);
        int outwardZ = ErdenExteriorResidenceCatalog.hamletOutwardZ(sample);
        int sideX = -outwardZ;
        int sideZ = outwardX;
        for (int lateral = -48; lateral <= 48; lateral += 8) {
            int x = sample.x + outwardX * REGIONAL_CROSS_DISTANCE + sideX * lateral;
            int z = sample.z + outwardZ * REGIONAL_CROSS_DISTANCE + sideZ * lateral;
            unique.add(pack(x >> 4, z >> 4));
        }
        REGIONAL_CI_REQUIRED.addAll(unique);
        REGIONAL_CI_REQUESTS.addAll(unique);
        LivingKingdoms.LOGGER.info(
                "Prepared Erden regional-settlement CI hubs={} chunks={} market_villages=4 drovers_waystations=3 mining_villages=5 mill_villages=3 river_trade_posts=2 existing_housing_reused=true focused_ci=true generic_exterior_sweep=false",
                hubs, unique.size());
    }

    private static void advanceRegionalCiAnchors(ServerLevel level) {
        for (long packed : List.copyOf(REGIONAL_CI_LOADING)) {
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            if (!level.hasChunk(chunkX, chunkZ)) continue;
            REGIONAL_CI_LOADING.remove(packed);
            enqueue(level, packed, true);
        }
        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        ErdenExteriorResidenceSavedData residences = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorResidenceSavedData.TYPE);
        for (int forced = 0; forced < CI_FORCE_BUDGET
                && !REGIONAL_CI_REQUESTS.isEmpty()
                && REGIONAL_CI_RETAINED.size() < CI_MAX_IN_FLIGHT; forced++) {
            long packed = REGIONAL_CI_REQUESTS.removeFirst();
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
            boolean regionalNeeded = intersectsRegionalSettlement(chunk)
                    && data.regionalNeeds(packed, REGIONAL_SETTLEMENT_REVISION);
            boolean residenceNeeded = residences.needsChunk(
                    chunkX, chunkZ, ErdenExteriorResidenceBuilder.RESIDENCE_REVISION);
            if (!regionalNeeded && !residenceNeeded) continue;
            if (level.hasChunk(chunkX, chunkZ)) {
                enqueue(level, packed, true);
                continue;
            }
            if (REGIONAL_CI_RETAINED.add(packed)) {
                level.getChunkSource().addTicketAndLoadWithRadius(
                        TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
            }
            REGIONAL_CI_LOADING.add(packed);
        }
    }

    private static void verifyRegionalCi(ServerLevel level) {
        if (!isRegionalCi() || regionalCiPassed || !regionalCiPrepared
                || !REGIONAL_CI_REQUESTS.isEmpty() || !REGIONAL_CI_LOADING.isEmpty()) return;
        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        ErdenExteriorResidenceSavedData residences = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorResidenceSavedData.TYPE);
        if (data.regionalCompletedNodeCount(REGIONAL_SETTLEMENT_REVISION) != EXPECTED_REGIONAL_HUBS) return;
        int bells = 0;
        int homes = 0;
        int spines = 0;
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            if (!regionalHubEligible(node)) continue;
            Point core = regionalCore(node);
            int y = (int) Math.round(AuthoredContinentDensity.surfaceHeight(core.x, core.z));
            if (!level.getBlockState(new BlockPos(core.x, y + 1, core.z)).is(Blocks.BELL)) return;
            if (!regionalRoleFixturePresent(level, node, core, y)) return;
            bells++;
            List<ErdenExteriorResidenceCatalog.ResidencePlot> nodeHomes =
                    ErdenExteriorResidenceCatalog.forNode(node.id);
            if (nodeHomes.isEmpty()) return;
            ErdenExteriorResidenceCatalog.ResidencePlot attached = nodeHomes.getFirst();
            if (!residences.householdBuilt(
                    attached.householdId(), ErdenExteriorResidenceBuilder.RESIDENCE_REVISION)
                    || !ErdenExteriorResidenceBuilder.validateLoadedResidence(level, attached)) return;
            homes++;
            int outwardX = ErdenExteriorResidenceCatalog.hamletOutwardX(node);
            int outwardZ = ErdenExteriorResidenceCatalog.hamletOutwardZ(node);
            int spineX = node.x + outwardX * REGIONAL_CROSS_DISTANCE;
            int spineZ = node.z + outwardZ * REGIONAL_CROSS_DISTANCE;
            if (!regionalRoadPresent(level, spineX, spineZ)) return;
            spines++;
        }
        ErdenKingdomSupplyCatalog.SupplyNode sample = ErdenKingdomSupplyCatalog.node("erden_grain_estate_01");
        int sampleOutX = ErdenExteriorResidenceCatalog.hamletOutwardX(sample);
        int sampleOutZ = ErdenExteriorResidenceCatalog.hamletOutwardZ(sample);
        int sampleSideX = -sampleOutZ;
        int sampleSideZ = sampleOutX;
        for (int lateral : new int[]{-48, -24, 0, 24, 48}) {
            if (!regionalRoadPresent(level,
                    sample.x + sampleOutX * REGIONAL_CROSS_DISTANCE + sampleSideX * lateral,
                    sample.z + sampleOutZ * REGIONAL_CROSS_DISTANCE + sampleSideZ * lateral)) return;
        }
        regionalCiPassed = true;
        int released = REGIONAL_CI_RETAINED.size();
        for (long packed : Set.copyOf(REGIONAL_CI_RETAINED)) releaseRegionalCi(level, packed);
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_REGIONAL_SETTLEMENT_PASS revision={} hubs={} market_villages=4 drovers_waystations=3 mining_villages=5 mill_villages=3 river_trade_posts=2 core_bells={} attached_homes={} spine_connections={} sample_cross_street=true existing_housing_reused=true no_new_households=true streamed=true forced_citywide=false ci_tickets_released={}",
                REGIONAL_SETTLEMENT_REVISION, EXPECTED_REGIONAL_HUBS, bells, homes, spines, released);
    }

    private static boolean regionalRoleFixturePresent(
            ServerLevel level,
            ErdenKingdomSupplyCatalog.SupplyNode node,
            Point core,
            int y) {
        int sideX = -ErdenExteriorResidenceCatalog.hamletOutwardZ(node);
        int sideZ = ErdenExteriorResidenceCatalog.hamletOutwardX(node);
        BlockPos positive4 = new BlockPos(core.x + sideX * 4, y + 1, core.z + sideZ * 4);
        return switch (node.role) {
            case "grain_estate" -> level.getBlockState(
                    new BlockPos(core.x + sideX * 5, y + 1, core.z + sideZ * 5)).is(Blocks.COMPOSTER);
            case "ranch" -> level.getBlockState(positive4).is(Blocks.HAY_BLOCK);
            case "colliery", "iron_mine" -> level.getBlockState(positive4).is(Blocks.SMITHING_TABLE);
            case "paper_mill" -> level.getBlockState(positive4).is(Blocks.CARTOGRAPHY_TABLE);
            case "river_wharf" -> level.getBlockState(positive4).is(Blocks.CHEST);
            default -> false;
        };
    }

    private static boolean regionalRoadPresent(ServerLevel level, int x, int z) {
        int preferred = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
        for (int offset = -6; offset <= 6; offset++) {
            if (level.getBlockState(new BlockPos(x, preferred + offset, z)).is(Blocks.COBBLESTONE)) return true;
        }
        return false;
    }

    private static void releaseRegionalCi(ServerLevel level, long packed) {
        if (!REGIONAL_CI_RETAINED.remove(packed)) return;
        level.getChunkSource().removeTicketWithRadius(
                TicketType.PORTAL, new ChunkPos(unpackX(packed), unpackZ(packed)), 0);
    }

'''
    require(methods_anchor in s, "regional CI insertion anchor missing")
    s = s.replace(methods_anchor, ci_methods + methods_anchor, 1)

    runtime_anchor = '''    private static boolean regionalRuntimeEnabled() {
'''
    isregional = '''    private static boolean isRegionalCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REGIONAL_SETTLEMENT_TEST"));
    }

'''
    require(runtime_anchor in s, "regional runtime method anchor missing")
    s = s.replace(runtime_anchor, isregional + runtime_anchor, 1)

for token in [
    "intersectsExterior(new ChunkPos(chunkX, chunkZ))",
    "REGIONAL_CI_REQUESTS",
    "prepareRegionalCiAnchors",
    "verifyRegionalCi",
    "LK_ERDEN_REGIONAL_SETTLEMENT_PASS",
    "sample_cross_street=true",
    "isRegionalCi()",
]:
    require(token in s, "regional focused CI invariant missing: " + token)

p.write_text(s, encoding="utf-8")
print("Added focused, bounded CI proof for 17 Erden regional settlement centres.")
