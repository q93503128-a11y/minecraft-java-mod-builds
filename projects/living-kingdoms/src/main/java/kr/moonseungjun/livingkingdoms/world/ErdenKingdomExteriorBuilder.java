package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Streams the farms, ranches, mines, mills, wharves and their approach roads as exterior chunks load.
 * All geometry uses one Minecraft block as one metre and is retained per 16 x 16 metre cell.
 */
public final class ErdenKingdomExteriorBuilder {
    public static final int EXTERIOR_REVISION = 2;

    private static final int TICK_BUDGET = 2_000;
    private static final int CI_TICK_BUDGET = 4_000;
    private static final int CI_FORCE_BUDGET = 1;
    private static final int CI_MAX_IN_FLIGHT = 2;
    public static final int EXPECTED_CI_ANCHORS = 178;
    private static final int ROAD_HALF_WIDTH = 2;
    private static final int[][] NODE_ANCHOR_OFFSETS = {
            {0, 0}, {28, 0}, {-28, 0}, {0, 28}, {0, -28}
    };

    private static final ArrayDeque<Long> PENDING = new ArrayDeque<>();
    private static final ArrayDeque<Long> CI_REQUESTS = new ArrayDeque<>();
    private static final Set<Long> CI_LOADING = new HashSet<>();
    private static final Set<Long> CI_REQUIRED = new HashSet<>();
    private static final Set<Long> QUEUED = new HashSet<>();
    private static final Set<Long> RETAINED = new HashSet<>();
    private static MinecraftServer activeServer;
    private static ActiveChunk active;
    private static boolean ciRequested;
    private static boolean ciPassed;

    private ErdenKingdomExteriorBuilder() {
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)
                || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        ChunkPos chunk = event.getChunk().getPos();
        boolean exteriorChunk = intersectsExterior(chunk);
        boolean residenceChunk = ErdenExteriorResidenceCatalog.residenceChunk(
                chunk.x(), chunk.z());
        if (!exteriorChunk && !residenceChunk) return;
        long packed = pack(chunk.x(), chunk.z());
        if (isCi() && !CI_REQUIRED.contains(packed)) return;
        enqueue(level, packed, false);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        if (activeServer != server) reset(server);

        if (isCi()) {
            if (!ciRequested) {
                ciRequested = true;
                prepareCiAnchors();
            }
            advanceCiAnchors(level);
        }

        if (active == null) startNext(level);
        if (active == null) {
            verifyCi(level);
            return;
        }
        if (!level.hasChunk(active.chunkX, active.chunkZ)) {
            QUEUED.remove(active.packed);
            release(level, active.packed);
            active = null;
            return;
        }

        active.plan.apply(level, isCi() ? CI_TICK_BUDGET : TICK_BUDGET);
        if (!active.plan.done()) return;

        ChunkPos chunk = new ChunkPos(active.chunkX, active.chunkZ);
        ConstructionDebrisCleaner.cleanStreamedChunkCompletion(level, chunk);
        ErdenKingdomExteriorSavedData data = level.getDataStorage()
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
        if (isCi()) {
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_EXTERIOR_CHUNK_COMPLETE chunk={},{} applied_writes={} exterior={} residences={}",
                    active.chunkX, active.chunkZ, active.plan.appliedWrites(),
                    active.buildExterior, active.buildResidences);
        }
        QUEUED.remove(active.packed);
        release(level, active.packed);
        active = null;
        verifyCi(level);
    }

    public static long storageAnchorChunk(ErdenKingdomSupplyCatalog.SupplyNode node) {
        int offsetX = switch (node.facingQuarterTurns) {
            case 1 -> 18;
            case 3 -> -18;
            default -> 0;
        };
        int offsetZ = switch (node.facingQuarterTurns) {
            case 0 -> 18;
            case 2 -> -18;
            default -> 0;
        };
        return pack((node.x + offsetX) >> 4, (node.z + offsetZ) >> 4);
    }

    public static BlockPos storagePosition(
            ServerLevel level,
            ErdenKingdomSupplyCatalog.SupplyNode node) {
        int offsetX = switch (node.facingQuarterTurns) {
            case 1 -> 18;
            case 3 -> -18;
            default -> 0;
        };
        int offsetZ = switch (node.facingQuarterTurns) {
            case 0 -> 18;
            case 2 -> -18;
            default -> 0;
        };
        int x = node.x + offsetX;
        int z = node.z + offsetZ;
        int y = (int) Math.round(AuthoredContinentDensity.surfaceHeight(node.x, node.z)) + 1;
        return new BlockPos(x, y, z);
    }

    public static boolean anchorBuilt(
            ServerLevel level,
            ErdenKingdomSupplyCatalog.SupplyNode node) {
        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        return data.nodeComplete(node.id, EXTERIOR_REVISION);
    }

    public static boolean residenceBuilt(ServerLevel level, String householdId) {
        return level.getDataStorage().computeIfAbsent(ErdenExteriorResidenceSavedData.TYPE)
                .householdBuilt(
                        householdId,
                        ErdenExteriorResidenceBuilder.RESIDENCE_REVISION);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        PENDING.clear();
        CI_REQUESTS.clear();
        CI_LOADING.clear();
        CI_REQUIRED.clear();
        QUEUED.clear();
        RETAINED.clear();
        active = null;
        ciRequested = false;
        ciPassed = false;
    }

    private static void prepareCiAnchors() {
        Set<Long> unique = new LinkedHashSet<>();
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            for (int[] offset : NODE_ANCHOR_OFFSETS) {
                unique.add(pack((node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4));
            }
            unique.add(storageAnchorChunk(node));
        }
        for (ErdenExteriorResidenceCatalog.ResidencePlot plot :
                ErdenExteriorResidenceCatalog.plots()) {
            unique.add(plot.physicalChunk());
        }
        if (unique.size() != EXPECTED_CI_ANCHORS) {
            throw new IllegalStateException("Invalid Erden exterior CI anchor count " + unique.size());
        }
        CI_REQUIRED.addAll(unique);
        CI_REQUESTS.addAll(unique);
        LivingKingdoms.LOGGER.info(
                "Requested Erden exterior CI anchors nodes={} request_queue={} metre_scale=true streamed=true staggered=true synchronous_get_chunk=false forced_chunks=false transient_ticket=portal max_in_flight={}",
                ErdenKingdomSupplyCatalog.nodes().size(), CI_REQUESTS.size(), CI_MAX_IN_FLIGHT);
    }

    private static void advanceCiAnchors(ServerLevel level) {
        for (long packed : List.copyOf(CI_LOADING)) {
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            if (!level.hasChunk(chunkX, chunkZ)) continue;
            CI_LOADING.remove(packed);
            enqueue(level, packed, true);
        }

        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        ErdenExteriorResidenceSavedData residences = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorResidenceSavedData.TYPE);
        for (int forced = 0; forced < CI_FORCE_BUDGET
                && !CI_REQUESTS.isEmpty()
                && RETAINED.size() < CI_MAX_IN_FLIGHT; forced++) {
            long packed = CI_REQUESTS.removeFirst();
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            boolean exteriorNeeded = isCiExteriorAnchor(packed)
                    && data.needs(packed, EXTERIOR_REVISION);
            if (!exteriorNeeded
                    && !residences.needsChunk(
                    chunkX, chunkZ,
                    ErdenExteriorResidenceBuilder.RESIDENCE_REVISION)) continue;
            if (level.hasChunk(chunkX, chunkZ)) {
                enqueue(level, packed, true);
                continue;
            }
            if (RETAINED.add(packed)) {
                LivingKingdoms.LOGGER.info(
                        "LK_ERDEN_EXTERIOR_CHUNK_REQUEST chunk={},{} exterior_needed={} residence_needed={} retained={} queue_remaining={}",
                        chunkX, chunkZ, exteriorNeeded,
                        residences.needsChunk(chunkX, chunkZ,
                                ErdenExteriorResidenceBuilder.RESIDENCE_REVISION),
                        RETAINED.size(), CI_REQUESTS.size());
                level.getChunkSource().addTicketAndLoadWithRadius(
                        TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
            }
            CI_LOADING.add(packed);
        }
    }

    private static void enqueue(ServerLevel level, long packed, boolean priority) {
        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        ErdenExteriorResidenceSavedData residences = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorResidenceSavedData.TYPE);
        int chunkX = unpackX(packed);
        int chunkZ = unpackZ(packed);
        boolean exteriorNeeded = (!isCi() || isCiExteriorAnchor(packed))
                && data.needs(packed, EXTERIOR_REVISION);
        if (!exteriorNeeded
                && !residences.needsChunk(
                chunkX, chunkZ,
                ErdenExteriorResidenceBuilder.RESIDENCE_REVISION)) {
            release(level, packed);
            return;
        }
        if (QUEUED.add(packed)) {
            if (priority) PENDING.addFirst(packed);
            else PENDING.addLast(packed);
        } else if (priority && PENDING.remove(packed)) {
            PENDING.addFirst(packed);
        }
    }

    private static void startNext(ServerLevel level) {
        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        ErdenExteriorResidenceSavedData residences = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorResidenceSavedData.TYPE);
        while (!PENDING.isEmpty()) {
            long packed = PENDING.removeFirst();
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            boolean buildExterior = (!isCi() || isCiExteriorAnchor(packed))
                    && data.needs(packed, EXTERIOR_REVISION);
            boolean buildResidences = residences.needsChunk(
                    chunkX, chunkZ,
                    ErdenExteriorResidenceBuilder.RESIDENCE_REVISION);
            if (!buildExterior && !buildResidences) {
                QUEUED.remove(packed);
                release(level, packed);
                continue;
            }
            if (!level.hasChunk(chunkX, chunkZ)) {
                QUEUED.remove(packed);
                release(level, packed);
                continue;
            }
            ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
            IncrementalWorldEditPlan plan = createChunkPlan(
                    level, chunk, buildExterior, buildResidences);
            active = new ActiveChunk(
                    packed, chunkX, chunkZ,
                    buildExterior, buildResidences, plan);
            if (isCi()) {
                LivingKingdoms.LOGGER.info(
                        "LK_ERDEN_EXTERIOR_CHUNK_START chunk={},{} writes={} operations={} exterior={} residences={} plots={} clipped={}",
                        chunkX, chunkZ, plan.estimatedWrites(), plan.operationCount(),
                        buildExterior, buildResidences,
                        ErdenExteriorResidenceCatalog.forChunk(chunkX, chunkZ).size(),
                        plan.suppressedOutOfBoundsWrites());
            }
            LivingKingdoms.LOGGER.debug(
                    "Prepared Erden exterior chunk {},{} writes={} operations={} exterior={} residences={} clipped_out_of_chunk_writes={}",
                    chunkX, chunkZ, plan.estimatedWrites(), plan.operationCount(),
                    buildExterior, buildResidences, plan.suppressedOutOfBoundsWrites());
            return;
        }
    }

    private static IncrementalWorldEditPlan createChunkPlan(
            ServerLevel level,
            ChunkPos chunk,
            boolean buildExterior,
            boolean buildResidences) {
        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan(chunk);
        if (buildExterior) {
            for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            addApproachRoad(plan, level, chunk, node);
            if (!intersectsSite(chunk, node)) continue;
            addSiteTerrain(plan, level, chunk, node);
            ExternalDistrictBuildingBuilder.addSupplyBuildingChunk(
                    plan, level, chunk,
                    node.x, node.z, node.id, node.role,
                    node.buildingStyle, node.facingQuarterTurns);
            switch (node.role) {
                case "grain_estate" -> addGrainEstate(plan, level, chunk, node);
                case "ranch" -> addRanch(plan, level, chunk, node);
                case "colliery" -> addMine(plan, level, chunk, node, false);
                case "iron_mine" -> addMine(plan, level, chunk, node, true);
                case "paper_mill" -> addPaperMill(plan, level, chunk, node);
                case "river_wharf" -> addWharf(plan, level, chunk, node);
                default -> throw new IllegalStateException("Unknown Erden supply role " + node.role);
            }
                addStorageYard(plan, chunk, node);
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

    private static void addSiteTerrain(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenKingdomSupplyCatalog.SupplyNode node) {
        int baseY = baseY(node);
        int flattenRadius = switch (node.role) {
            case "grain_estate" -> 30;
            case "ranch" -> 28;
            case "colliery", "iron_mine" -> 24;
            case "paper_mill" -> 24;
            case "river_wharf" -> 20;
            default -> 20;
        };
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        Block surface = switch (node.role) {
            case "colliery", "iron_mine" -> Blocks.COARSE_DIRT;
            case "paper_mill", "river_wharf" -> Blocks.PACKED_MUD;
            default -> Blocks.GRASS_BLOCK;
        };
        for (int x = minX; x <= minX + 15; x++) {
            for (int z = minZ; z <= minZ + 15; z++) {
                long dx = (long) x - node.x;
                long dz = (long) z - node.z;
                if (dx * dx + dz * dz > (long) flattenRadius * flattenRadius) continue;
                int original = plan.originalSurfaceY(level, x, z);
                if (original < baseY) {
                    plan.addFill(x, original + 1, z, x, baseY - 1, z, Blocks.DIRT);
                } else if (original > baseY) {
                    plan.addFill(x, baseY + 1, z, x, original + 3, z, Blocks.AIR);
                }
                plan.addSet(x, baseY, z, surface);
                plan.setPlannedSurfaceY(x, z, baseY);
            }
        }
    }

    private static void addApproachRoad(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenKingdomSupplyCatalog.SupplyNode node) {
        Point transfer = roadDestination(node);
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        for (int x = minX; x <= minX + 15; x++) {
            for (int z = minZ; z <= minZ + 15; z++) {
                if (!onManhattanRoute(x, z, node.x, node.z, transfer.x, transfer.z, ROAD_HALF_WIDTH)) continue;
                int y = plan.plannedSurfaceY(level, x, z);
                plan.addSet(x, y, z, Blocks.PACKED_MUD);
                plan.addFill(x, y + 1, z, x, y + 3, z, Blocks.AIR);
            }
        }
    }

    private static void addGrainEstate(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenKingdomSupplyCatalog.SupplyNode node) {
        addField(plan, level, chunk, node.x - 58, node.z - 22, node.x - 30, node.z + 22, true);
        addField(plan, level, chunk, node.x + 30, node.z - 22, node.x + 58, node.z + 22, false);
        addFenceRectangle(plan, level, chunk, node.x - 62, node.z - 27, node.x + 62, node.z + 27);
        addBarn(plan, chunk, node.x - 8, baseY(node) + 1, node.z + 34, 16, 11);
    }

    private static void addField(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            int x1,
            int z1,
            int x2,
            int z2,
            boolean waterOnX) {
        int minX = Math.max(Math.min(x1, x2), chunk.getMinBlockX());
        int maxX = Math.min(Math.max(x1, x2), chunk.getMinBlockX() + 15);
        int minZ = Math.max(Math.min(z1, z2), chunk.getMinBlockZ());
        int maxZ = Math.min(Math.max(z1, z2), chunk.getMinBlockZ() + 15);
        if (minX > maxX || minZ > maxZ) return;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int y = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
                boolean channel = waterOnX ? Math.floorMod(x, 9) == 0 : Math.floorMod(z, 9) == 0;
                plan.addSet(x, y, z, channel ? Blocks.WATER : Blocks.FARMLAND);
                if (!channel) {
                    plan.addSet(x, y + 1, z,
                            Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
                }
            }
        }
    }

    private static void addRanch(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenKingdomSupplyCatalog.SupplyNode node) {
        addFenceRectangle(plan, level, chunk, node.x - 56, node.z - 38, node.x - 8, node.z + 38);
        addFenceRectangle(plan, level, chunk, node.x + 8, node.z - 38, node.x + 56, node.z + 38);
        addBarn(plan, chunk, node.x - 10, baseY(node) + 1, node.z + 34, 20, 12);
        addTrough(plan, chunk, node.x - 34, node.z, baseY(node));
        addTrough(plan, chunk, node.x + 34, node.z, baseY(node));
        setIfChunk(plan, chunk, node.x - 18, baseY(node) + 1, node.z + 24, Blocks.HAY_BLOCK);
        setIfChunk(plan, chunk, node.x + 18, baseY(node) + 1, node.z + 24, Blocks.HAY_BLOCK);
    }

    private static void addMine(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenKingdomSupplyCatalog.SupplyNode node,
            boolean iron) {
        int y = baseY(node) + 1;
        int portalX = node.x + 20;
        int portalZ = node.z;
        Block masonry = iron ? Blocks.DEEPSLATE_BRICKS : Blocks.STONE_BRICKS;
        Block ore = iron ? Blocks.DEEPSLATE_IRON_ORE : Blocks.DEEPSLATE_COAL_ORE;
        addBoxClipped(plan, chunk, portalX - 5, y, portalZ - 6, portalX + 5, y + 7, portalZ + 6, masonry);
        addBoxClipped(plan, chunk, portalX - 3, y + 1, portalZ - 4, portalX + 3, y + 5, portalZ + 4, Blocks.AIR);
        for (int step = 0; step < 32; step++) {
            int x = portalX + step;
            int floor = y - Math.min(10, step / 3);
            addBoxClipped(plan, chunk, x, floor + 1, portalZ - 2, x, floor + 4, portalZ + 2, Blocks.AIR);
            setIfChunk(plan, chunk, x, floor, portalZ, Blocks.RAIL);
            if (step % 6 == 0) {
                setIfChunk(plan, chunk, x, floor + 2, portalZ - 3, ore);
                setIfChunk(plan, chunk, x, floor + 2, portalZ + 3, ore);
            }
        }
        addHeadframe(plan, chunk, portalX, y + 7, portalZ);
        addSpoilPile(plan, chunk, node.x - 30, node.z + 24, y, iron ? Blocks.TUFF : Blocks.COAL_ORE);
    }

    private static void addPaperMill(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenKingdomSupplyCatalog.SupplyNode node) {
        int y = baseY(node);
        for (int x = node.x - 46; x <= node.x + 46; x++) {
            for (int z = node.z + 20; z <= node.z + 25; z++) {
                setIfChunk(plan, chunk, x, y, z, Blocks.WATER);
            }
        }
        for (int x = node.x - 42; x <= node.x + 42; x += 3) {
            setIfChunk(plan, chunk, x, y + 1, node.z + 18, Blocks.SUGAR_CANE);
            setIfChunk(plan, chunk, x, y + 1, node.z + 27, Blocks.SUGAR_CANE);
        }
        addMillWheel(plan, chunk, node.x + 22, y + 5, node.z + 22);
        addBarn(plan, chunk, node.x - 34, y + 1, node.z - 4, 14, 10);
    }

    private static void addWharf(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenKingdomSupplyCatalog.SupplyNode node) {
        int y = baseY(node) + 1;
        int directionX = node.facingQuarterTurns == 1 ? -1 : node.facingQuarterTurns == 3 ? 1 : 0;
        int directionZ = node.facingQuarterTurns == 0 ? -1 : node.facingQuarterTurns == 2 ? 1 : 0;
        for (int length = 0; length <= 42; length++) {
            for (int width = -5; width <= 5; width++) {
                int x = node.x + directionX * length + (directionZ != 0 ? width : 0);
                int z = node.z + directionZ * length + (directionX != 0 ? width : 0);
                setIfChunk(plan, chunk, x, y, z, Blocks.OAK_PLANKS);
                if (Math.abs(width) == 5 && length % 7 == 0) {
                    setIfChunk(plan, chunk, x, y - 1, z, Blocks.STRIPPED_OAK_LOG);
                    setIfChunk(plan, chunk, x, y + 1, z, Blocks.OAK_FENCE);
                }
            }
        }
        addCrane(plan, chunk, node.x + directionX * 18, y + 1, node.z + directionZ * 18);
        addBoxClipped(plan, chunk, node.x - 10, y, node.z - 10, node.x + 10, y, node.z + 10, Blocks.STONE_BRICKS);
    }

    private static void addStorageYard(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            ErdenKingdomSupplyCatalog.SupplyNode node) {
        int y = baseY(node);
        BlockPos storage = storagePosition(null, node);
        addBoxClipped(plan, chunk,
                storage.getX() - 3, y, storage.getZ() - 3,
                storage.getX() + 3, y, storage.getZ() + 3,
                Blocks.STONE_BRICKS);
        setIfChunk(plan, chunk, storage.getX(), storage.getY(), storage.getZ(), Blocks.BARREL);
        setIfChunk(plan, chunk, storage.getX() - 2, y + 1, storage.getZ(), Blocks.HAY_BLOCK);
        setIfChunk(plan, chunk, storage.getX() + 2, y + 1, storage.getZ(), Blocks.CHEST);
    }

    private static void addFenceRectangle(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            int x1,
            int z1,
            int x2,
            int z2) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            addFenceAt(plan, level, chunk, x, z1);
            addFenceAt(plan, level, chunk, x, z2);
        }
        for (int z = Math.min(z1, z2) + 1; z < Math.max(z1, z2); z++) {
            addFenceAt(plan, level, chunk, x1, z);
            addFenceAt(plan, level, chunk, x2, z);
        }
    }

    private static void addFenceAt(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            int x,
            int z) {
        if (!inside(chunk, x, z)) return;
        int y = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
        plan.addSet(x, y + 1, z, Blocks.OAK_FENCE);
    }

    private static void addBarn(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            int centerX,
            int y,
            int centerZ,
            int width,
            int length) {
        int halfX = width / 2;
        int halfZ = length / 2;
        addBoxClipped(plan, chunk, centerX - halfX, y, centerZ - halfZ,
                centerX + halfX, y, centerZ + halfZ, Blocks.OAK_PLANKS);
        addBoxClipped(plan, chunk, centerX - halfX + 1, y + 1, centerZ - halfZ + 1,
                centerX + halfX - 1, y + 6, centerZ + halfZ - 1, Blocks.AIR);
        addBoxClipped(plan, chunk, centerX - halfX, y + 7, centerZ - halfZ,
                centerX + halfX, y + 7, centerZ + halfZ, Blocks.DARK_OAK_PLANKS);
        addBoxClipped(plan, chunk, centerX - 2, y + 1, centerZ - halfZ,
                centerX + 2, y + 4, centerZ - halfZ, Blocks.AIR);
    }

    private static void addTrough(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            int x,
            int z,
            int baseY) {
        addBoxClipped(plan, chunk, x - 4, baseY + 1, z - 1, x + 4, baseY + 1, z + 1, Blocks.OAK_PLANKS);
        addBoxClipped(plan, chunk, x - 3, baseY + 2, z, x + 3, baseY + 2, z, Blocks.WATER);
    }

    private static void addHeadframe(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            int x,
            int y,
            int z) {
        addBoxClipped(plan, chunk, x - 4, y, z - 4, x - 3, y + 10, z - 3, Blocks.STRIPPED_SPRUCE_LOG);
        addBoxClipped(plan, chunk, x + 3, y, z - 4, x + 4, y + 10, z - 3, Blocks.STRIPPED_SPRUCE_LOG);
        addBoxClipped(plan, chunk, x - 4, y + 9, z - 4, x + 4, y + 10, z - 3, Blocks.SPRUCE_PLANKS);
        setIfChunk(plan, chunk, x, y + 7, z - 3, Blocks.IRON_BARS);
    }

    private static void addSpoilPile(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            int centerX,
            int centerZ,
            int baseY,
            Block block) {
        for (int layer = 0; layer < 6; layer++) {
            int radius = 8 - layer;
            addBoxClipped(plan, chunk,
                    centerX - radius, baseY + layer, centerZ - radius,
                    centerX + radius, baseY + layer, centerZ + radius,
                    block);
        }
    }

    private static void addMillWheel(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            int centerX,
            int centerY,
            int centerZ) {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dy = -6; dy <= 6; dy++) {
                int distance = dx * dx + dy * dy;
                if (distance < 25 || distance > 42) continue;
                setIfChunk(plan, chunk, centerX + dx, centerY + dy, centerZ, Blocks.DARK_OAK_PLANKS);
            }
        }
        addBoxClipped(plan, chunk, centerX - 6, centerY, centerZ, centerX + 6, centerY, centerZ, Blocks.STRIPPED_OAK_LOG);
        addBoxClipped(plan, chunk, centerX, centerY - 6, centerZ, centerX, centerY + 6, centerZ, Blocks.STRIPPED_OAK_LOG);
    }

    private static void addCrane(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            int x,
            int y,
            int z) {
        addBoxClipped(plan, chunk, x, y, z, x, y + 9, z, Blocks.STRIPPED_SPRUCE_LOG);
        addBoxClipped(plan, chunk, x, y + 9, z, x + 8, y + 9, z, Blocks.STRIPPED_SPRUCE_LOG);
        addBoxClipped(plan, chunk, x + 8, y + 5, z, x + 8, y + 8, z, Blocks.IRON_CHAIN);
        setIfChunk(plan, chunk, x + 8, y + 4, z, Blocks.BARREL);
    }

    private static void addBoxClipped(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            int x1,
            int y1,
            int z1,
            int x2,
            int y2,
            int z2,
            Block block) {
        int minX = Math.max(Math.min(x1, x2), chunk.getMinBlockX());
        int maxX = Math.min(Math.max(x1, x2), chunk.getMinBlockX() + 15);
        int minZ = Math.max(Math.min(z1, z2), chunk.getMinBlockZ());
        int maxZ = Math.min(Math.max(z1, z2), chunk.getMinBlockZ() + 15);
        if (minX > maxX || minZ > maxZ || y2 < y1) return;
        plan.addFill(minX, y1, minZ, maxX, y2, maxZ, block);
    }

    private static void setIfChunk(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            int x,
            int y,
            int z,
            Block block) {
        if (inside(chunk, x, z)) plan.addSet(x, y, z, block);
    }

    private static boolean inside(ChunkPos chunk, int x, int z) {
        return x >= chunk.getMinBlockX() && x <= chunk.getMinBlockX() + 15
                && z >= chunk.getMinBlockZ() && z <= chunk.getMinBlockZ() + 15;
    }

    private static boolean intersectsExterior(ChunkPos chunk) {
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            if (intersectsSite(chunk, node) || intersectsRoute(chunk, node)) return true;
        }
        return false;
    }

    private static boolean intersectsSite(
            ChunkPos chunk,
            ErdenKingdomSupplyCatalog.SupplyNode node) {
        int minX = chunk.getMinBlockX();
        int maxX = minX + 15;
        int minZ = chunk.getMinBlockZ();
        int maxZ = minZ + 15;
        int closestX = Math.clamp(node.x, minX, maxX);
        int closestZ = Math.clamp(node.z, minZ, maxZ);
        long dx = (long) closestX - node.x;
        long dz = (long) closestZ - node.z;
        return dx * dx + dz * dz <= (long) node.radius * node.radius;
    }

    private static boolean intersectsRoute(
            ChunkPos chunk,
            ErdenKingdomSupplyCatalog.SupplyNode node) {
        Point destination = roadDestination(node);
        int minX = chunk.getMinBlockX() - ROAD_HALF_WIDTH;
        int maxX = chunk.getMinBlockX() + 15 + ROAD_HALF_WIDTH;
        int minZ = chunk.getMinBlockZ() - ROAD_HALF_WIDTH;
        int maxZ = chunk.getMinBlockZ() + 15 + ROAD_HALF_WIDTH;
        return axisSegmentIntersects(minX, maxX, minZ, maxZ,
                node.x, node.z, destination.x, node.z)
                || axisSegmentIntersects(minX, maxX, minZ, maxZ,
                destination.x, node.z, destination.x, destination.z);
    }

    private static boolean axisSegmentIntersects(
            int boxMinX,
            int boxMaxX,
            int boxMinZ,
            int boxMaxZ,
            int x1,
            int z1,
            int x2,
            int z2) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        return maxX >= boxMinX && minX <= boxMaxX && maxZ >= boxMinZ && minZ <= boxMaxZ;
    }

    private static boolean onManhattanRoute(
            int x,
            int z,
            int startX,
            int startZ,
            int endX,
            int endZ,
            int width) {
        boolean first = x >= Math.min(startX, endX) - width
                && x <= Math.max(startX, endX) + width
                && Math.abs(z - startZ) <= width;
        boolean second = Math.abs(x - endX) <= width
                && z >= Math.min(startZ, endZ) - width
                && z <= Math.max(startZ, endZ) + width;
        return first || second;
    }

    private static Point roadDestination(ErdenKingdomSupplyCatalog.SupplyNode node) {
        if (node.role.equals("paper_mill")) {
            ErdenKingdomSupplyCatalog.SupplyNode nearest = null;
            long best = Long.MAX_VALUE;
            for (ErdenKingdomSupplyCatalog.SupplyNode candidate : ErdenKingdomSupplyCatalog.nodes()) {
                if (!candidate.role.equals("river_wharf")) continue;
                long distance = manhattan(node.x, node.z, candidate.x, candidate.z);
                if (distance < best) {
                    nearest = candidate;
                    best = distance;
                }
            }
            if (nearest != null) return new Point(nearest.x, nearest.z);
        }
        List<Point> gates = List.of(
                new Point(-1_200, 0), new Point(1_200, 0),
                new Point(0, -900), new Point(0, 900));
        Point nearest = gates.getFirst();
        long best = manhattan(node.x, node.z, nearest.x, nearest.z);
        for (Point gate : gates) {
            long distance = manhattan(node.x, node.z, gate.x, gate.z);
            if (distance < best) {
                nearest = gate;
                best = distance;
            }
        }
        return nearest;
    }

    private static void markCompletedNodeAnchors(ErdenKingdomExteriorSavedData data) {
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            if (data.nodeComplete(node.id, EXTERIOR_REVISION)) continue;
            boolean complete = true;
            for (int[] offset : NODE_ANCHOR_OFFSETS) {
                long packed = pack((node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4);
                if (!data.isBuilt(packed, EXTERIOR_REVISION)) {
                    complete = false;
                    break;
                }
            }
            if (complete && !data.isBuilt(storageAnchorChunk(node), EXTERIOR_REVISION)) {
                complete = false;
            }
            if (complete) data.markNode(node.id, EXTERIOR_REVISION);
        }
    }

    private static void verifyCi(ServerLevel level) {
        if (ciPassed || !isCi()) return;
        ErdenKingdomExteriorSavedData data = level.getDataStorage()
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
        if (!ErdenExteriorTicketReaper.storageValidationComplete()) return;
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
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
    }

    private static int baseY(ErdenKingdomSupplyCatalog.SupplyNode node) {
        return (int) Math.round(AuthoredContinentDensity.surfaceHeight(node.x, node.z));
    }

    private static boolean isCiExteriorAnchor(long packed) {
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            for (int[] offset : NODE_ANCHOR_OFFSETS) {
                if (pack((node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4) == packed) {
                    return true;
                }
            }
            if (storageAnchorChunk(node) == packed) return true;
        }
        return false;
    }

    private static boolean isCi() {
        if (!"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return false;
        // Focused subsystem audits still bootstrap the authored realm, but must not also request
        // the 178-chunk exterior regression sweep. Naturally loaded exterior chunks continue to
        // use the normal streaming path, so production behavior is unchanged.
        return !"1".equals(System.getenv("LIVING_KINGDOMS_CI_RIVER_PORT_TEST"))
                && !"1".equals(System.getenv("LIVING_KINGDOMS_CI_FIRE_RESPONSE_TEST"));
    }

    private static void release(ServerLevel level, long packed) {
        CI_LOADING.remove(packed);
        RETAINED.remove(packed);
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) packed;
    }

    private static long manhattan(int x1, int z1, int x2, int z2) {
        return Math.abs((long) x1 - x2) + Math.abs((long) z1 - z2);
    }

    private record Point(int x, int z) {
    }

    private record ActiveChunk(
            long packed,
            int chunkX,
            int chunkZ,
            boolean buildExterior,
            boolean buildResidences,
            IncrementalWorldEditPlan plan) {
    }
}
