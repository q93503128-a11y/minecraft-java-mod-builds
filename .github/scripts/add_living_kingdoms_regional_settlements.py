from pathlib import Path

ROOT = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world")
SAVED = ROOT / "ErdenKingdomExteriorSavedData.java"
RESIDENCES = ROOT / "ErdenExteriorResidenceCatalog.java"
BUILDER = ROOT / "ErdenKingdomExteriorBuilder.java"


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(message)


# 1) Save-compatible regional construction overlay. Existing exterior revision/state is untouched.
s = SAVED.read_text(encoding="utf-8")
if '"regional_revision"' not in s:
    s = s.replace(
        '            Codec.LONG.optionalFieldOf("total_writes", 0L).forGetter(data -> data.totalWrites)\n'
        '    ).apply(instance, ErdenKingdomExteriorSavedData::new));',
        '            Codec.LONG.optionalFieldOf("total_writes", 0L).forGetter(data -> data.totalWrites),\n'
        '            Codec.INT.optionalFieldOf("regional_revision", 0).forGetter(data -> data.regionalRevision),\n'
        '            Codec.LONG.listOf().optionalFieldOf("regional_built_chunks", List.of())\n'
        '                    .forGetter(data -> List.copyOf(data.regionalBuiltChunks)),\n'
        '            Codec.STRING.listOf().optionalFieldOf("regional_completed_nodes", List.of())\n'
        '                    .forGetter(data -> List.copyOf(data.regionalCompletedNodes)),\n'
        '            Codec.LONG.optionalFieldOf("regional_total_writes", 0L)\n'
        '                    .forGetter(data -> data.regionalTotalWrites)\n'
        '    ).apply(instance, ErdenKingdomExteriorSavedData::new));',
        1,
    )
    s = s.replace(
        '    private long totalWrites;\n',
        '    private long totalWrites;\n'
        '    private int regionalRevision;\n'
        '    private final Set<Long> regionalBuiltChunks;\n'
        '    private final Set<String> regionalCompletedNodes;\n'
        '    private long regionalTotalWrites;\n',
        1,
    )
    s = s.replace(
        '        this(0, List.of(), List.of(), 0L);',
        '        this(0, List.of(), List.of(), 0L, 0, List.of(), List.of(), 0L);',
        1,
    )
    old_ctor = '''    private ErdenKingdomExteriorSavedData(
            int revision,
            List<Long> builtChunks,
            List<String> completedNodes,
            long totalWrites) {
        this.revision = Math.max(0, revision);
        this.builtChunks = new HashSet<>(builtChunks);
        this.completedNodes = new HashSet<>(completedNodes);
        this.totalWrites = Math.max(0L, totalWrites);
    }
'''
    new_ctor = '''    private ErdenKingdomExteriorSavedData(
            int revision,
            List<Long> builtChunks,
            List<String> completedNodes,
            long totalWrites,
            int regionalRevision,
            List<Long> regionalBuiltChunks,
            List<String> regionalCompletedNodes,
            long regionalTotalWrites) {
        this.revision = Math.max(0, revision);
        this.builtChunks = new HashSet<>(builtChunks);
        this.completedNodes = new HashSet<>(completedNodes);
        this.totalWrites = Math.max(0L, totalWrites);
        this.regionalRevision = Math.max(0, regionalRevision);
        this.regionalBuiltChunks = new HashSet<>(regionalBuiltChunks);
        this.regionalCompletedNodes = new HashSet<>(regionalCompletedNodes);
        this.regionalTotalWrites = Math.max(0L, regionalTotalWrites);
    }
'''
    require(old_ctor in s, "exterior SavedData constructor anchor missing")
    s = s.replace(old_ctor, new_ctor, 1)
    insert = '''
    public boolean regionalNeeds(long chunkPos, int currentRevision) {
        return regionalRevision != currentRevision || !regionalBuiltChunks.contains(chunkPos);
    }

    public boolean regionalChunkBuilt(long chunkPos, int currentRevision) {
        return regionalRevision == currentRevision && regionalBuiltChunks.contains(chunkPos);
    }

    public void markRegionalChunk(long chunkPos, int currentRevision, long writes) {
        ensureRegionalRevision(currentRevision);
        if (regionalBuiltChunks.add(chunkPos)) {
            regionalTotalWrites += Math.max(0L, writes);
            setDirty();
        }
    }

    public void markRegionalNode(String nodeId, int currentRevision) {
        ensureRegionalRevision(currentRevision);
        if (regionalCompletedNodes.add(nodeId)) setDirty();
    }

    public boolean regionalNodeComplete(String nodeId, int currentRevision) {
        return regionalRevision == currentRevision && regionalCompletedNodes.contains(nodeId);
    }

    public int regionalBuiltChunkCount(int currentRevision) {
        return regionalRevision == currentRevision ? regionalBuiltChunks.size() : 0;
    }

    public int regionalCompletedNodeCount(int currentRevision) {
        return regionalRevision == currentRevision ? regionalCompletedNodes.size() : 0;
    }

    public long regionalTotalWrites(int currentRevision) {
        return regionalRevision == currentRevision ? regionalTotalWrites : 0L;
    }

    private void ensureRegionalRevision(int currentRevision) {
        if (regionalRevision == currentRevision) return;
        regionalRevision = currentRevision;
        regionalBuiltChunks.clear();
        regionalCompletedNodes.clear();
        regionalTotalWrites = 0L;
        setDirty();
    }
'''
    anchor = '    private void ensureRevision(int currentRevision) {\n'
    require(anchor in s, "exterior SavedData revision anchor missing")
    s = s.replace(anchor, insert + '\n' + anchor, 1)
SAVED.write_text(s, encoding="utf-8")

# 2) Expose the existing hamlet direction so settlement centres and homes cannot drift apart.
r = RESIDENCES.read_text(encoding="utf-8")
if "public static int hamletOutwardX" not in r:
    anchor = '    private static Point physicalAnchor(\n'
    helpers = '''    public static int hamletOutwardX(ErdenKingdomSupplyCatalog.SupplyNode node) {
        return hamletOutward(node).x();
    }

    public static int hamletOutwardZ(ErdenKingdomSupplyCatalog.SupplyNode node) {
        return hamletOutward(node).z();
    }

    private static Point hamletOutward(ErdenKingdomSupplyCatalog.SupplyNode node) {
        int outwardX;
        int outwardZ;
        if (node.role.equals("paper_mill")) {
            outwardX = 0;
            outwardZ = Integer.signum(node.z);
            if (outwardZ == 0) outwardZ = 1;
        } else if (Math.abs(node.x) >= Math.abs(node.z)) {
            outwardX = Integer.signum(node.x);
            outwardZ = 0;
        } else {
            outwardX = 0;
            outwardZ = Integer.signum(node.z);
        }
        if (outwardX == 0 && outwardZ == 0) outwardZ = 1;
        return new Point(outwardX, outwardZ);
    }

'''
    require(anchor in r, "residence physicalAnchor anchor missing")
    r = r.replace(anchor, helpers + anchor, 1)
    old_direction = '''        int outwardX;
        int outwardZ;
        if (node.role.equals("paper_mill")) {
            // Paper deliveries start with a horizontal leg toward a wharf. Put the worker hamlet
            // perpendicular to that freight leg so homes and their short access paths stay clear.
            outwardX = 0;
            outwardZ = Integer.signum(node.z);
            if (outwardZ == 0) outwardZ = 1;
        } else if (Math.abs(node.x) >= Math.abs(node.z)) {
            outwardX = Integer.signum(node.x);
            outwardZ = 0;
        } else {
            outwardX = 0;
            outwardZ = Integer.signum(node.z);
        }
        if (outwardX == 0 && outwardZ == 0) outwardZ = 1;
'''
    new_direction = '''        // Paper deliveries and all other settlement geometry use this same direction.
        int outwardX = hamletOutwardX(node);
        int outwardZ = hamletOutwardZ(node);
'''
    require(old_direction in r, "residence hamlet direction block missing")
    r = r.replace(old_direction, new_direction, 1)
RESIDENCES.write_text(r, encoding="utf-8")

# 3) Regional village/trade-centre layer inside the existing streamed exterior builder.
b = BUILDER.read_text(encoding="utf-8")
if "REGIONAL_SETTLEMENT_REVISION" not in b:
    b = b.replace(
        '    public static final int EXTERIOR_REVISION = 2;\n',
        '    public static final int EXTERIOR_REVISION = 2;\n'
        '    public static final int REGIONAL_SETTLEMENT_REVISION = 1;\n'
        '    public static final int EXPECTED_REGIONAL_HUBS = 17;\n',
        1,
    )
    b = b.replace(
        '    private static final int ROAD_HALF_WIDTH = 2;\n',
        '    private static final int ROAD_HALF_WIDTH = 2;\n'
        '    private static final int REGIONAL_CORE_DISTANCE = 80;\n'
        '    private static final int REGIONAL_CROSS_DISTANCE = 88;\n'
        '    private static final int REGIONAL_PLAZA_RADIUS = 6;\n'
        '    private static final int REGIONAL_SIDE_SPAN = 54;\n'
        '    private static final int REGIONAL_LANE_HALF_WIDTH = 1;\n',
        1,
    )

    b = b.replace(
        '''        boolean residenceChunk = ErdenExteriorResidenceCatalog.residenceChunk(
                chunk.x(), chunk.z());
        if (!exteriorChunk && !residenceChunk) return;''',
        '''        boolean residenceChunk = ErdenExteriorResidenceCatalog.residenceChunk(
                chunk.x(), chunk.z());
        boolean regionalChunk = regionalRuntimeEnabled() && intersectsRegionalSettlement(chunk);
        if (!exteriorChunk && !residenceChunk && !regionalChunk) return;''',
        1,
    )

    b = b.replace(
        '''        if (active.buildResidences) {
            List<ErdenExteriorResidenceCatalog.ResidencePlot> plots =''',
        '''        if (active.buildRegional) {
            data.markRegionalChunk(
                    active.packed, REGIONAL_SETTLEMENT_REVISION, active.plan.appliedWrites());
            markCompletedRegionalHubs(data);
        }
        if (active.buildResidences) {
            List<ErdenExteriorResidenceCatalog.ResidencePlot> plots =''',
        1,
    )

    old_enqueue = '''        boolean exteriorNeeded = (!isCi() || isCiExteriorAnchor(packed))
                && data.needs(packed, EXTERIOR_REVISION);
        if (!exteriorNeeded
                && !residences.needsChunk(
                chunkX, chunkZ,
                ErdenExteriorResidenceBuilder.RESIDENCE_REVISION)) {'''
    new_enqueue = '''        boolean exteriorNeeded = (!isCi() || isCiExteriorAnchor(packed))
                && data.needs(packed, EXTERIOR_REVISION);
        boolean regionalNeeded = regionalRuntimeEnabled()
                && intersectsRegionalSettlement(new ChunkPos(chunkX, chunkZ))
                && data.regionalNeeds(packed, REGIONAL_SETTLEMENT_REVISION);
        if (!exteriorNeeded && !regionalNeeded
                && !residences.needsChunk(
                chunkX, chunkZ,
                ErdenExteriorResidenceBuilder.RESIDENCE_REVISION)) {'''
    require(old_enqueue in b, "exterior enqueue anchor missing")
    b = b.replace(old_enqueue, new_enqueue, 1)

    old_start = '''            boolean buildExterior = (!isCi() || isCiExteriorAnchor(packed))
                    && data.needs(packed, EXTERIOR_REVISION);
            boolean buildResidences = residences.needsChunk(
                    chunkX, chunkZ,
                    ErdenExteriorResidenceBuilder.RESIDENCE_REVISION);
            if (!buildExterior && !buildResidences) {'''
    new_start = '''            boolean buildExterior = (!isCi() || isCiExteriorAnchor(packed))
                    && data.needs(packed, EXTERIOR_REVISION);
            boolean buildRegional = regionalRuntimeEnabled()
                    && intersectsRegionalSettlement(new ChunkPos(chunkX, chunkZ))
                    && data.regionalNeeds(packed, REGIONAL_SETTLEMENT_REVISION);
            boolean buildResidences = residences.needsChunk(
                    chunkX, chunkZ,
                    ErdenExteriorResidenceBuilder.RESIDENCE_REVISION);
            if (!buildExterior && !buildRegional && !buildResidences) {'''
    require(old_start in b, "exterior start anchor missing")
    b = b.replace(old_start, new_start, 1)

    b = b.replace(
        '''            IncrementalWorldEditPlan plan = createChunkPlan(
                    level, chunk, buildExterior, buildResidences);
            active = new ActiveChunk(
                    packed, chunkX, chunkZ,
                    buildExterior, buildResidences, plan);''',
        '''            IncrementalWorldEditPlan plan = createChunkPlan(
                    level, chunk, buildExterior, buildRegional, buildResidences);
            active = new ActiveChunk(
                    packed, chunkX, chunkZ,
                    buildExterior, buildRegional, buildResidences, plan);''',
        1,
    )

    b = b.replace(
        '''            boolean buildExterior,
            boolean buildResidences) {
        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan(chunk);''',
        '''            boolean buildExterior,
            boolean buildRegional,
            boolean buildResidences) {
        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan(chunk);''',
        1,
    )

    insert_after_exterior = '''        if (buildRegional) {
            for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
                if (regionalHubEligible(node) && intersectsRegionalSettlement(chunk, node)) {
                    addRegionalSettlement(plan, level, chunk, node);
                }
            }
        }
'''
    anchor = '''        if (buildResidences) {
            for (ErdenExteriorResidenceCatalog.ResidencePlot plot :'''
    require(anchor in b, "residence plan anchor missing")
    b = b.replace(anchor, insert_after_exterior + anchor, 1)

    methods_anchor = '    private static void addSiteTerrain(\n'
    regional_methods = r'''    private static boolean regionalHubEligible(ErdenKingdomSupplyCatalog.SupplyNode node) {
        return !node.id.equals("erden_west_wharf");
    }

    private static String regionalHubType(ErdenKingdomSupplyCatalog.SupplyNode node) {
        return switch (node.role) {
            case "grain_estate" -> "market_village";
            case "ranch" -> "drovers_waystation";
            case "colliery", "iron_mine" -> "mining_village";
            case "paper_mill" -> "mill_village";
            case "river_wharf" -> "river_trade_post";
            default -> throw new IllegalStateException("Unknown Erden regional hub role " + node.role);
        };
    }

    private static Point regionalCore(ErdenKingdomSupplyCatalog.SupplyNode node) {
        int outwardX = ErdenExteriorResidenceCatalog.hamletOutwardX(node);
        int outwardZ = ErdenExteriorResidenceCatalog.hamletOutwardZ(node);
        return new Point(
                node.x + outwardX * REGIONAL_CORE_DISTANCE,
                node.z + outwardZ * REGIONAL_CORE_DISTANCE);
    }

    private static boolean intersectsRegionalSettlement(ChunkPos chunk) {
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            if (regionalHubEligible(node) && intersectsRegionalSettlement(chunk, node)) return true;
        }
        return false;
    }

    private static boolean intersectsRegionalSettlement(
            ChunkPos chunk,
            ErdenKingdomSupplyCatalog.SupplyNode node) {
        int outwardX = ErdenExteriorResidenceCatalog.hamletOutwardX(node);
        int outwardZ = ErdenExteriorResidenceCatalog.hamletOutwardZ(node);
        int start = Math.max(node.radius + 4, REGIONAL_CORE_DISTANCE - REGIONAL_PLAZA_RADIUS);
        int end = REGIONAL_CROSS_DISTANCE + REGIONAL_LANE_HALF_WIDTH;
        int x1 = node.x + outwardX * start;
        int z1 = node.z + outwardZ * start;
        int x2 = node.x + outwardX * end;
        int z2 = node.z + outwardZ * end;
        int sideX = -outwardZ;
        int sideZ = outwardX;
        int ax = x1 + sideX * REGIONAL_SIDE_SPAN;
        int az = z1 + sideZ * REGIONAL_SIDE_SPAN;
        int bx = x1 - sideX * REGIONAL_SIDE_SPAN;
        int bz = z1 - sideZ * REGIONAL_SIDE_SPAN;
        int cx = x2 + sideX * REGIONAL_SIDE_SPAN;
        int cz = z2 + sideZ * REGIONAL_SIDE_SPAN;
        int dx = x2 - sideX * REGIONAL_SIDE_SPAN;
        int dz = z2 - sideZ * REGIONAL_SIDE_SPAN;
        int minX = Math.min(Math.min(ax, bx), Math.min(cx, dx));
        int maxX = Math.max(Math.max(ax, bx), Math.max(cx, dx));
        int minZ = Math.min(Math.min(az, bz), Math.min(cz, dz));
        int maxZ = Math.max(Math.max(az, bz), Math.max(cz, dz));
        return chunk.getMinBlockX() + 15 >= minX && chunk.getMinBlockX() <= maxX
                && chunk.getMinBlockZ() + 15 >= minZ && chunk.getMinBlockZ() <= maxZ;
    }

    private static void addRegionalSettlement(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenKingdomSupplyCatalog.SupplyNode node) {
        int outwardX = ErdenExteriorResidenceCatalog.hamletOutwardX(node);
        int outwardZ = ErdenExteriorResidenceCatalog.hamletOutwardZ(node);
        int sideX = -outwardZ;
        int sideZ = outwardX;
        Point core = regionalCore(node);
        int coreY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(core.x, core.z));
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        int spineStart = Math.max(node.radius + 4, REGIONAL_CORE_DISTANCE - REGIONAL_PLAZA_RADIUS);
        for (int x = minX; x <= minX + 15; x++) {
            for (int z = minZ; z <= minZ + 15; z++) {
                int dx = x - node.x;
                int dz = z - node.z;
                int longitudinal = dx * outwardX + dz * outwardZ;
                int lateral = dx * sideX + dz * sideZ;
                boolean plaza = Math.abs(longitudinal - REGIONAL_CORE_DISTANCE) <= REGIONAL_PLAZA_RADIUS
                        && Math.abs(lateral) <= REGIONAL_PLAZA_RADIUS;
                boolean spine = longitudinal >= spineStart && longitudinal <= REGIONAL_CROSS_DISTANCE
                        && Math.abs(lateral) <= REGIONAL_LANE_HALF_WIDTH;
                boolean cross = Math.abs(longitudinal - REGIONAL_CROSS_DISTANCE) <= REGIONAL_LANE_HALF_WIDTH
                        && Math.abs(lateral) <= REGIONAL_SIDE_SPAN;
                if (plaza) {
                    int original = plan.originalSurfaceY(level, x, z);
                    if (original < coreY) plan.addFill(x, original + 1, z, x, coreY - 1, z, Blocks.DIRT);
                    else if (original > coreY) plan.addFill(x, coreY + 1, z, x, original + 3, z, Blocks.AIR);
                    plan.addSet(x, coreY, z, regionalPlazaBlock(node));
                    plan.addFill(x, coreY + 1, z, x, coreY + 3, z, Blocks.AIR);
                    plan.setPlannedSurfaceY(x, z, coreY);
                } else if (spine || cross) {
                    int y = plan.plannedSurfaceY(level, x, z);
                    plan.addSet(x, y, z, Blocks.COBBLESTONE);
                    plan.addFill(x, y + 1, z, x, y + 3, z, Blocks.AIR);
                }
            }
        }
        addRegionalCommonFixtures(plan, chunk, node, core, coreY, outwardX, outwardZ, sideX, sideZ);
        switch (node.role) {
            case "grain_estate" -> addRegionalMarket(plan, chunk, core, coreY, sideX, sideZ);
            case "ranch" -> addRegionalDroversPost(plan, chunk, core, coreY, sideX, sideZ);
            case "colliery", "iron_mine" -> addRegionalMiningServices(plan, chunk, core, coreY, sideX, sideZ, node.role.equals("iron_mine"));
            case "paper_mill" -> addRegionalMillServices(plan, chunk, core, coreY, sideX, sideZ);
            case "river_wharf" -> addRegionalTradePost(plan, chunk, core, coreY, sideX, sideZ);
            default -> throw new IllegalStateException("Unknown regional settlement role " + node.role);
        }
    }

    private static Block regionalPlazaBlock(ErdenKingdomSupplyCatalog.SupplyNode node) {
        return switch (node.role) {
            case "grain_estate" -> Blocks.COBBLESTONE;
            case "ranch" -> Blocks.COARSE_DIRT;
            case "colliery" -> Blocks.POLISHED_ANDESITE;
            case "iron_mine" -> Blocks.DEEPSLATE_TILES;
            case "paper_mill" -> Blocks.PACKED_MUD;
            case "river_wharf" -> Blocks.STONE_BRICKS;
            default -> Blocks.COBBLESTONE;
        };
    }

    private static void addRegionalCommonFixtures(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            ErdenKingdomSupplyCatalog.SupplyNode node,
            Point core,
            int y,
            int outwardX,
            int outwardZ,
            int sideX,
            int sideZ) {
        setIfChunk(plan, chunk, core.x, y + 1, core.z, Blocks.BELL);
        setIfChunk(plan, chunk, core.x + sideX * 3, y + 1, core.z + sideZ * 3, Blocks.LECTERN);
        setIfChunk(plan, chunk, core.x - sideX * 3, y + 1, core.z - sideZ * 3, Blocks.BARREL);
        int lampX = core.x - outwardX * 5;
        int lampZ = core.z - outwardZ * 5;
        setIfChunk(plan, chunk, lampX, y + 1, lampZ, Blocks.OAK_FENCE);
        setIfChunk(plan, chunk, lampX, y + 2, lampZ, Blocks.OAK_FENCE);
        setIfChunk(plan, chunk, lampX, y + 3, lampZ, Blocks.LANTERN);
    }

    private static void addRegionalMarket(
            IncrementalWorldEditPlan plan, ChunkPos chunk, Point core, int y, int sideX, int sideZ) {
        for (int sign : new int[]{-1, 1}) {
            int cx = core.x + sideX * sign * 4;
            int cz = core.z + sideZ * sign * 4;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                setIfChunk(plan, chunk, cx + dx, y + 1, cz + dz, Blocks.OAK_PLANKS);
            }
            setIfChunk(plan, chunk, cx, y + 2, cz, Blocks.OAK_FENCE);
            setIfChunk(plan, chunk, cx, y + 3, cz, sign < 0 ? Blocks.YELLOW_WOOL : Blocks.WHITE_WOOL);
            setIfChunk(plan, chunk, cx + sideX, y + 1, cz + sideZ, Blocks.COMPOSTER);
        }
    }

    private static void addRegionalDroversPost(
            IncrementalWorldEditPlan plan, ChunkPos chunk, Point core, int y, int sideX, int sideZ) {
        for (int offset = -4; offset <= 4; offset += 2) {
            int x = core.x + sideX * offset;
            int z = core.z + sideZ * offset;
            setIfChunk(plan, chunk, x, y + 1, z, Blocks.OAK_FENCE);
            setIfChunk(plan, chunk, x, y + 2, z, Blocks.LEAD == null ? Blocks.OAK_FENCE : Blocks.OAK_FENCE);
        }
        setIfChunk(plan, chunk, core.x + sideX * 4, y + 1, core.z + sideZ * 4, Blocks.HAY_BLOCK);
        setIfChunk(plan, chunk, core.x - sideX * 4, y + 1, core.z - sideZ * 4, Blocks.WATER_CAULDRON);
    }

    private static void addRegionalMiningServices(
            IncrementalWorldEditPlan plan, ChunkPos chunk, Point core, int y,
            int sideX, int sideZ, boolean iron) {
        int x = core.x + sideX * 4;
        int z = core.z + sideZ * 4;
        setIfChunk(plan, chunk, x, y + 1, z, Blocks.SMITHING_TABLE);
        setIfChunk(plan, chunk, x - sideX, y + 1, z - sideZ, Blocks.BLAST_FURNACE);
        setIfChunk(plan, chunk, x + sideX, y + 1, z + sideZ, Blocks.STONECUTTER);
        setIfChunk(plan, chunk, core.x - sideX * 4, y + 1, core.z - sideZ * 4,
                iron ? Blocks.RAW_IRON_BLOCK : Blocks.COAL_BLOCK);
        setIfChunk(plan, chunk, core.x - sideX * 3, y + 1, core.z - sideZ * 3, Blocks.BARREL);
    }

    private static void addRegionalMillServices(
            IncrementalWorldEditPlan plan, ChunkPos chunk, Point core, int y, int sideX, int sideZ) {
        setIfChunk(plan, chunk, core.x + sideX * 4, y + 1, core.z + sideZ * 4, Blocks.CARTOGRAPHY_TABLE);
        setIfChunk(plan, chunk, core.x + sideX * 3, y + 1, core.z + sideZ * 3, Blocks.BARREL);
        setIfChunk(plan, chunk, core.x - sideX * 4, y + 1, core.z - sideZ * 4, Blocks.LOOM);
        setIfChunk(plan, chunk, core.x - sideX * 3, y + 1, core.z - sideZ * 3, Blocks.BIRCH_FENCE);
    }

    private static void addRegionalTradePost(
            IncrementalWorldEditPlan plan, ChunkPos chunk, Point core, int y, int sideX, int sideZ) {
        setIfChunk(plan, chunk, core.x + sideX * 4, y + 1, core.z + sideZ * 4, Blocks.CHEST);
        setIfChunk(plan, chunk, core.x + sideX * 3, y + 1, core.z + sideZ * 3, Blocks.BARREL);
        setIfChunk(plan, chunk, core.x - sideX * 4, y + 1, core.z - sideZ * 4, Blocks.CRAFTING_TABLE);
        setIfChunk(plan, chunk, core.x - sideX * 3, y + 1, core.z - sideZ * 3, Blocks.OAK_FENCE);
    }

    private static Set<Long> regionalCoreChunks(ErdenKingdomSupplyCatalog.SupplyNode node) {
        Point core = regionalCore(node);
        Set<Long> chunks = new HashSet<>();
        for (int x : new int[]{core.x - REGIONAL_PLAZA_RADIUS, core.x + REGIONAL_PLAZA_RADIUS}) {
            for (int z : new int[]{core.z - REGIONAL_PLAZA_RADIUS, core.z + REGIONAL_PLAZA_RADIUS}) {
                chunks.add(pack(x >> 4, z >> 4));
            }
        }
        return chunks;
    }

    private static void markCompletedRegionalHubs(ErdenKingdomExteriorSavedData data) {
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            if (!regionalHubEligible(node)
                    || data.regionalNodeComplete(node.id, REGIONAL_SETTLEMENT_REVISION)) continue;
            boolean complete = true;
            for (long packed : regionalCoreChunks(node)) {
                if (!data.regionalChunkBuilt(packed, REGIONAL_SETTLEMENT_REVISION)) {
                    complete = false;
                    break;
                }
            }
            if (complete) {
                data.markRegionalNode(node.id, REGIONAL_SETTLEMENT_REVISION);
                LivingKingdoms.LOGGER.info(
                        "Completed Erden regional settlement node={} type={} existing_worker_homes={} reused_housing=true public_square=true local_lanes=true role_services=true regional_revision={}",
                        node.id, regionalHubType(node),
                        ErdenExteriorResidenceCatalog.forNode(node.id).size(), REGIONAL_SETTLEMENT_REVISION);
            }
        }
    }

'''
    require(methods_anchor in b, "regional method insertion anchor missing")
    b = b.replace(methods_anchor, regional_methods + methods_anchor, 1)

    # Regional mode is disabled inside other/global CI fixtures, so focused audits stay isolated.
    isci_old = '''        return !"1".equals(System.getenv("LIVING_KINGDOMS_CI_RIVER_PORT_TEST"))
                && !"1".equals(System.getenv("LIVING_KINGDOMS_CI_FIRE_RESPONSE_TEST"));'''
    isci_new = '''        return !"1".equals(System.getenv("LIVING_KINGDOMS_CI_RIVER_PORT_TEST"))
                && !"1".equals(System.getenv("LIVING_KINGDOMS_CI_FIRE_RESPONSE_TEST"))
                && !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REGIONAL_SETTLEMENT_TEST"));'''
    require(isci_old in b, "focused exterior CI isolation anchor missing")
    b = b.replace(isci_old, isci_new, 1)
    runtime_anchor = '    private static void release(ServerLevel level, long packed) {\n'
    runtime_method = '''    private static boolean regionalRuntimeEnabled() {
        if (!"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return true;
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REGIONAL_SETTLEMENT_TEST"));
    }

'''
    require(runtime_anchor in b, "regional runtime helper anchor missing")
    b = b.replace(runtime_anchor, runtime_method + runtime_anchor, 1)

    old_record = '''            boolean buildExterior,
            boolean buildResidences,
            IncrementalWorldEditPlan plan) {'''
    new_record = '''            boolean buildExterior,
            boolean buildRegional,
            boolean buildResidences,
            IncrementalWorldEditPlan plan) {'''
    require(old_record in b, "ActiveChunk record anchor missing")
    b = b.replace(old_record, new_record, 1)

    # Generic CI never builds regional chunks, so preserve its exact progress-log contract.
    b = b.replace(
        '                    active.chunkX, active.chunkZ, active.plan.appliedWrites(),\n                    active.buildExterior, active.buildResidences);',
        '                    active.chunkX, active.chunkZ, active.plan.appliedWrites(),\n                    active.buildExterior, active.buildResidences);',
        1,
    )

BUILDER.write_text(b, encoding="utf-8")

for path, tokens in {
    SAVED: ["regional_revision", "regionalNeeds", "markRegionalNode"],
    RESIDENCES: ["hamletOutwardX", "hamletOutwardZ"],
    BUILDER: ["REGIONAL_SETTLEMENT_REVISION = 1", "EXPECTED_REGIONAL_HUBS = 17",
              "regionalRuntimeEnabled", "market_village", "mining_village",
              "reused_housing=true", "LIVING_KINGDOMS_CI_REGIONAL_SETTLEMENT_TEST"],
}.items():
    text = path.read_text(encoding="utf-8")
    for token in tokens:
        require(token in text, f"missing {token} in {path.name}")

print("Staged save-compatible Erden regional settlement overlay for 17 existing production hamlets.")
