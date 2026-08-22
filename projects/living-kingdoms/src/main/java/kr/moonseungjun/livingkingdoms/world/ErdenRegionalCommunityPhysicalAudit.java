package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** CI-only proof that community schedules point at physical village places and real residents. */
@EventBusSubscriber(modid = LivingKingdoms.MOD_ID)
public final class ErdenRegionalCommunityPhysicalAudit {
    private static final String REPRESENTATIVE_ID = "harvest_crossing";
    private static final String SAMPLE_HOME_ROLE = "farmstead_east";
    private static final int BUILD_BUDGET = 4_000;
    private static final int EXPECTED_RESIDENTS = 3;
    private static final int DESTINATION_PROBE_RADIUS = 16;
    private static final int ROUTE_GUARD_DISTANCE = 32_768;
    private static final long TICKET_REFRESH_INTERVAL = 40L;
    private static final long WAIT_LOG_INTERVAL = 200L;
    private static final long PROBE_TIMEOUT_TICKS = 12_000L;

    // Only one destination neighborhood is retained at a time. PROBED records the union for the
    // final acceptance marker; it never represents live tickets.
    private static final Set<Long> RETAINED = new LinkedHashSet<>();
    private static final Set<Long> PROBED = new LinkedHashSet<>();
    private static MinecraftServer activeServer;
    private static ActiveBuild activeBuild;
    private static ProbeStage stage = ProbeStage.HOME;
    private static boolean requested;
    private static boolean stageRequested;
    private static boolean passed;
    private static boolean failed;
    private static boolean physicalHomeEvidence;
    private static boolean physicalSquareEvidence;
    private static boolean physicalInnEvidence;
    private static boolean physicalMarketEvidence;
    private static boolean homeWalkableEvidence;
    private static boolean squareWalkableEvidence;
    private static boolean innWalkableEvidence;
    private static boolean marketWalkableEvidence;
    private static boolean residentIdentityEvidence;
    private static boolean loadedRouteGuardEvidence;
    private static int residentCountEvidence;
    private static int releasedChunks;
    private static long probeRequestedAt;
    private static long lastTicketRefreshTick;
    private static long lastWaitLogTick;

    private ErdenRegionalCommunityPhysicalAudit() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!isCi()) return;
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom") || passed || failed) return;

        ErdenRegionalSocietySavedData society = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSocietySavedData.TYPE);
        if (!society.hasPopulation(
                ErdenRegionalSocietyManager.SOCIETY_REVISION,
                ErdenRegionalSocietyManager.EXPECTED_HOUSEHOLDS)) return;
        ErdenRegionalEconomySavedData economy = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalEconomySavedData.TYPE);
        if (!economy.hasEconomy(
                ErdenRegionalEconomyManager.ECONOMY_REVISION,
                ErdenRegionalEconomyManager.EXPECTED_SETTLEMENTS)) return;

        ErdenRegionalSettlementCatalog.Settlement settlement = representative();
        ErdenRegionalSocietySavedData.Household household = sampleHousehold(society);
        ErdenRegionalSettlementSavedData construction = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSettlementSavedData.TYPE);
        long homeKey = pack(household.homeX() >> 4, household.homeZ() >> 4);
        long marketKey = ErdenRegionalEconomyManager.storageChunkKey(settlement);

        // The base regional CI already proves/builds the representative home and physical market.
        // Wait for that authoritative stream before this focused proof begins.
        if (!construction.isBuilt(homeKey, ErdenRegionalSettlementCatalog.REVISION)
                || !construction.isBuilt(marketKey, ErdenRegionalSettlementCatalog.REVISION)) return;

        if (!requested) beginProbe(level);
        if (timedOut(level)) {
            failAndRelease(level, "timeout_before_physical_acceptance");
            return;
        }
        if (!stageRequested) requestStage(level, settlement, household);
        refreshStageTickets(level);
        if (!allLoaded(level)) {
            logLoadWait(level);
            return;
        }
        if (!buildProbe(level, construction)) return;
        if (!verifyStage(level, society, economy, settlement, household)) return;

        releasedChunks += releaseCurrentStage(level);
        stageRequested = false;
        lastTicketRefreshTick = Long.MIN_VALUE;
        lastWaitLogTick = Long.MIN_VALUE;
        if (stage == ProbeStage.MARKET) {
            finishAcceptance(level);
            return;
        }
        stage = stage.next();
    }

    private static void beginProbe(ServerLevel level) {
        requested = true;
        probeRequestedAt = level.getGameTime();
        lastTicketRefreshTick = Long.MIN_VALUE;
        lastWaitLogTick = Long.MIN_VALUE;
        LivingKingdoms.LOGGER.info(
                "Requested Erden regional community staged physical CI probe settlement={} destinations=4 destination_radius={} max_live_destination_groups=1 transient_ticket=portal refresh_ticks={} refreshed_until_verification=true persistent_forced_chunks=false",
                REPRESENTATIVE_ID, DESTINATION_PROBE_RADIUS, TICKET_REFRESH_INTERVAL);
    }

    private static void requestStage(
            ServerLevel level,
            ErdenRegionalSettlementCatalog.Settlement settlement,
            ErdenRegionalSocietySavedData.Household household) {
        if (!RETAINED.isEmpty()) {
            throw new IllegalStateException("Regional community CI retained chunks leaked between stages");
        }
        stageRequested = true;
        lastTicketRefreshTick = level.getGameTime();
        switch (stage) {
            case HOME -> addProbeArea(level, household.homeX(), household.homeZ(), DESTINATION_PROBE_RADIUS);
            case SQUARE -> addProbeArea(level, settlement.x(), settlement.z(), DESTINATION_PROBE_RADIUS);
            case INN -> {
                ErdenRegionalSettlementCatalog.BuildingLot inn =
                        ErdenRegionalCommunityManager.requireLot(settlement, "village_inn");
                addProbeArea(
                        level, settlement.x() + inn.dx(), settlement.z() + inn.dz(),
                        DESTINATION_PROBE_RADIUS);
            }
            case MARKET -> {
                BlockPos market = ErdenRegionalEconomyManager.storagePosition(settlement);
                addProbeArea(level, market.getX(), market.getZ(), DESTINATION_PROBE_RADIUS);
            }
        }
        LivingKingdoms.LOGGER.info(
                "Requested Erden regional community physical stage={} live_chunks={} unique_probe_chunks={} transient_ticket=portal refresh_ticks={} refreshed_until_verification=true",
                stage.id(), RETAINED.size(), PROBED.size(), TICKET_REFRESH_INTERVAL);
    }

    private static void addProbeArea(ServerLevel level, int blockX, int blockZ, int radius) {
        int minChunkX = (blockX - radius) >> 4;
        int maxChunkX = (blockX + radius) >> 4;
        int minChunkZ = (blockZ - radius) >> 4;
        int maxChunkZ = (blockZ + radius) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                addProbe(level, chunkX, chunkZ);
            }
        }
    }

    private static void addProbe(ServerLevel level, int chunkX, int chunkZ) {
        long key = pack(chunkX, chunkZ);
        if (!RETAINED.add(key)) return;
        PROBED.add(key);
        level.getChunkSource().addTicketAndLoadWithRadius(
                TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
    }

    private static void refreshStageTickets(ServerLevel level) {
        if (!stageRequested || RETAINED.isEmpty()) return;
        long now = level.getGameTime();
        if (lastTicketRefreshTick != Long.MIN_VALUE
                && now - lastTicketRefreshTick < TICKET_REFRESH_INTERVAL) return;
        lastTicketRefreshTick = now;
        for (long key : RETAINED) {
            level.getChunkSource().addTicketAndLoadWithRadius(
                    TicketType.PORTAL, new ChunkPos(unpackX(key), unpackZ(key)), 0);
        }
    }

    private static boolean allLoaded(ServerLevel level) {
        if (RETAINED.isEmpty()) return false;
        for (long key : RETAINED) {
            if (!level.hasChunk(unpackX(key), unpackZ(key))) return false;
        }
        return true;
    }

    private static void logLoadWait(ServerLevel level) {
        long now = level.getGameTime();
        if (lastWaitLogTick != Long.MIN_VALUE && now - lastWaitLogTick < WAIT_LOG_INTERVAL) return;
        lastWaitLogTick = now;
        int loaded = 0;
        for (long key : RETAINED) {
            if (level.hasChunk(unpackX(key), unpackZ(key))) loaded++;
        }
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_REGIONAL_COMMUNITY_PHYSICAL_LOAD_WAIT stage={} loaded={}/{} unique_probe_chunks={} elapsed_ticks={}",
                stage.id(), loaded, RETAINED.size(), PROBED.size(), now - probeRequestedAt);
    }

    private static boolean buildProbe(
            ServerLevel level,
            ErdenRegionalSettlementSavedData construction) {
        if (activeBuild != null) {
            activeBuild.plan().apply(level, BUILD_BUDGET);
            if (!activeBuild.plan().done()) return false;
            finishBuild(level, construction, activeBuild);
            activeBuild = null;
        }
        for (long key : RETAINED) {
            if (!construction.needs(key, ErdenRegionalSettlementCatalog.REVISION)) continue;
            int chunkX = unpackX(key);
            int chunkZ = unpackZ(key);
            if (!level.hasChunk(chunkX, chunkZ)) return false;
            ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
            IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan(chunk);
            ErdenRegionalSettlementBuilder.addChunk(plan, level, chunk);
            activeBuild = new ActiveBuild(key, chunkX, chunkZ, plan);
            activeBuild.plan().apply(level, BUILD_BUDGET);
            if (!activeBuild.plan().done()) return false;
            finishBuild(level, construction, activeBuild);
            activeBuild = null;
            return false;
        }
        return true;
    }

    private static void finishBuild(
            ServerLevel level,
            ErdenRegionalSettlementSavedData construction,
            ActiveBuild build) {
        ChunkPos chunk = new ChunkPos(build.chunkX(), build.chunkZ());
        ConstructionDebrisCleaner.cleanStreamedChunkCompletion(level, chunk);
        construction.markChunk(
                build.key(), ErdenRegionalSettlementCatalog.REVISION,
                build.plan().appliedWrites());
    }

    private static boolean verifyStage(
            ServerLevel level,
            ErdenRegionalSocietySavedData society,
            ErdenRegionalEconomySavedData economy,
            ErdenRegionalSettlementCatalog.Settlement settlement,
            ErdenRegionalSocietySavedData.Household household) {
        ErdenRegionalSettlementSavedData construction = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSettlementSavedData.TYPE);
        for (long key : RETAINED) {
            if (!construction.isBuilt(key, ErdenRegionalSettlementCatalog.REVISION)) return false;
        }

        ErdenRegionalCommunityManager.ResidentContext context =
                new ErdenRegionalCommunityManager.ResidentContext(
                        household, household.residents().getFirst());
        return switch (stage) {
            case HOME -> verifyHome(level, society, settlement, household, context);
            case SQUARE -> verifySquare(level, society, settlement, context);
            case INN -> verifyInn(level, society, settlement, context);
            case MARKET -> verifyMarket(level, society, economy, settlement, context);
        };
    }

    private static boolean verifyHome(
            ServerLevel level,
            ErdenRegionalSocietySavedData society,
            ErdenRegionalSettlementCatalog.Settlement settlement,
            ErdenRegionalSocietySavedData.Household household,
            ErdenRegionalCommunityManager.ResidentContext context) {
        Set<String> expectedNames = new LinkedHashSet<>();
        for (ErdenRegionalSocietySavedData.Resident resident : household.residents()) {
            if (!society.isDead(resident.id())) expectedNames.add(resident.name());
        }
        List<Villager> residents = level.getEntitiesOfClass(
                Villager.class, settlementBounds(level, settlement),
                candidate -> expectedNames.contains(candidate.getName().getString()));
        Set<String> present = new LinkedHashSet<>();
        for (Villager resident : residents) present.add(resident.getName().getString());

        boolean physical = hasStructureNear(level, household.homeX(), household.homeZ(), 12, 24);
        BlockPos target = ErdenRegionalCommunityManager.activityTarget(
                level, society, context, ErdenRegionalCommunityManager.Activity.HOME, 0L);
        boolean walkable = !target.equals(BlockPos.ZERO);
        BlockPos guardTarget = target.offset(ROUTE_GUARD_DISTANCE, 0, ROUTE_GUARD_DISTANCE);
        boolean routeGuard = walkable
                && !ErdenRegionalCommunityManager.routeLoaded(level, target, guardTarget);
        boolean identity = expectedNames.size() == EXPECTED_RESIDENTS
                && present.size() == EXPECTED_RESIDENTS;
        if (!physical || !walkable || !routeGuard || !identity) {
            logStageWait(level, present.size(), physical, walkable, routeGuard, target);
            return false;
        }

        physicalHomeEvidence = true;
        homeWalkableEvidence = true;
        loadedRouteGuardEvidence = true;
        residentIdentityEvidence = true;
        residentCountEvidence = present.size();
        return true;
    }

    private static boolean verifySquare(
            ServerLevel level,
            ErdenRegionalSocietySavedData society,
            ErdenRegionalSettlementCatalog.Settlement settlement,
            ErdenRegionalCommunityManager.ResidentContext context) {
        int baseY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(settlement.x(), settlement.z()));
        boolean physical = level.getBlockState(
                new BlockPos(settlement.x(), baseY, settlement.z())).is(Blocks.WATER)
                && level.getBlockState(
                new BlockPos(settlement.x() + 2, baseY, settlement.z() + 2)).is(Blocks.STONE_BRICKS);
        BlockPos target = ErdenRegionalCommunityManager.activityTarget(
                level, society, context, ErdenRegionalCommunityManager.Activity.SQUARE, 0L);
        boolean walkable = !target.equals(BlockPos.ZERO);
        if (!physical || !walkable) {
            logStageWait(level, residentCountEvidence, physical, walkable, true, target);
            return false;
        }
        physicalSquareEvidence = true;
        squareWalkableEvidence = true;
        return true;
    }

    private static boolean verifyInn(
            ServerLevel level,
            ErdenRegionalSocietySavedData society,
            ErdenRegionalSettlementCatalog.Settlement settlement,
            ErdenRegionalCommunityManager.ResidentContext context) {
        ErdenRegionalSettlementCatalog.BuildingLot inn =
                ErdenRegionalCommunityManager.requireLot(settlement, "village_inn");
        int innX = settlement.x() + inn.dx();
        int innZ = settlement.z() + inn.dz();
        boolean physical = hasStructureNear(level, innX, innZ, 12, 24);
        BlockPos target = ErdenRegionalCommunityManager.activityTarget(
                level, society, context, ErdenRegionalCommunityManager.Activity.INN, 0L);
        boolean walkable = !target.equals(BlockPos.ZERO);
        if (!physical || !walkable) {
            logStageWait(level, residentCountEvidence, physical, walkable, true, target);
            return false;
        }
        physicalInnEvidence = true;
        innWalkableEvidence = true;
        return true;
    }

    private static boolean verifyMarket(
            ServerLevel level,
            ErdenRegionalSocietySavedData society,
            ErdenRegionalEconomySavedData economy,
            ErdenRegionalSettlementCatalog.Settlement settlement,
            ErdenRegionalCommunityManager.ResidentContext context) {
        BlockPos market = ErdenRegionalEconomyManager.storagePosition(settlement);
        boolean physical = level.getBlockState(market).is(Blocks.BARREL)
                && economy.storageMaterialized(REPRESENTATIVE_ID);
        BlockPos target = ErdenRegionalCommunityManager.activityTarget(
                level, society, context, ErdenRegionalCommunityManager.Activity.MARKET, 0L);
        boolean walkable = !target.equals(BlockPos.ZERO);
        if (!physical || !walkable) {
            logStageWait(level, residentCountEvidence, physical, walkable, true, target);
            return false;
        }
        physicalMarketEvidence = true;
        marketWalkableEvidence = true;
        return true;
    }

    private static void logStageWait(
            ServerLevel level,
            int residentCount,
            boolean physical,
            boolean walkable,
            boolean routeGuard,
            BlockPos target) {
        long now = level.getGameTime();
        if (lastWaitLogTick != Long.MIN_VALUE && now - lastWaitLogTick < WAIT_LOG_INTERVAL) return;
        lastWaitLogTick = now;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_REGIONAL_COMMUNITY_PHYSICAL_WAIT stage={} residents={}/{} physical={} walkable={} loaded_route_guard={} target={} live_chunks={} unique_probe_chunks={} elapsed_ticks={}",
                stage.id(), residentCount, EXPECTED_RESIDENTS,
                physical, walkable, routeGuard, target,
                RETAINED.size(), PROBED.size(), now - probeRequestedAt);
    }

    private static void finishAcceptance(ServerLevel level) {
        boolean destinationsWalkable = homeWalkableEvidence && squareWalkableEvidence
                && innWalkableEvidence && marketWalkableEvidence;
        if (!physicalHomeEvidence || !physicalSquareEvidence || !physicalInnEvidence
                || !physicalMarketEvidence || !residentIdentityEvidence
                || !destinationsWalkable || !loadedRouteGuardEvidence) {
            failAndRelease(level, "staged_evidence_incomplete");
            return;
        }
        passed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_REGIONAL_COMMUNITY_PHYSICAL_PASS revision={} settlement={} residents={} destinations=4 probe_chunks={} physical_home=true physical_square=true physical_inn=true physical_market=true resident_identity=true destinations_walkable=true transient_probe_released=true staged_probe=true refreshed_until_verification=true refresh_ticks={} navigation_only=true loaded_route_guard=true persistent_forced_chunks=false",
                ErdenRegionalCommunityManager.COMMUNITY_REVISION,
                REPRESENTATIVE_ID, residentCountEvidence, PROBED.size(), TICKET_REFRESH_INTERVAL);
    }

    private static boolean timedOut(ServerLevel level) {
        return requested && level.getGameTime() - probeRequestedAt > PROBE_TIMEOUT_TICKS;
    }

    private static void failAndRelease(ServerLevel level, String reason) {
        releasedChunks += releaseCurrentStage(level);
        failed = true;
        LivingKingdoms.LOGGER.error(
                "LK_ERDEN_REGIONAL_COMMUNITY_PHYSICAL_FAIL revision={} settlement={} stage={} reason={} released_chunks={} unique_probe_chunks={} transient_probe_released=true persistent_forced_chunks=false",
                ErdenRegionalCommunityManager.COMMUNITY_REVISION,
                REPRESENTATIVE_ID, stage.id(), reason, releasedChunks, PROBED.size());
    }

    private static int releaseCurrentStage(ServerLevel level) {
        int released = RETAINED.size();
        for (long key : Set.copyOf(RETAINED)) {
            level.getChunkSource().removeTicketWithRadius(
                    TicketType.PORTAL, new ChunkPos(unpackX(key), unpackZ(key)), 0);
            RETAINED.remove(key);
        }
        activeBuild = null;
        lastTicketRefreshTick = Long.MIN_VALUE;
        return released;
    }

    private static boolean hasStructureNear(
            ServerLevel level,
            int centerX,
            int centerZ,
            int radius,
            int minimum) {
        int baseY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(centerX, centerZ));
        int structural = 0;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                for (int y = baseY; y <= baseY + 12; y++) {
                    var state = level.getBlockState(new BlockPos(x, y, z));
                    if (state.isAir() || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)
                            || state.is(Blocks.DIRT_PATH) || state.is(Blocks.PACKED_MUD)
                            || state.is(Blocks.GRAVEL) || state.is(Blocks.FARMLAND)
                            || state.is(Blocks.WATER) || state.is(Blocks.WHEAT)) continue;
                    structural++;
                    if (structural >= minimum) return true;
                }
            }
        }
        return false;
    }

    private static ErdenRegionalSettlementCatalog.Settlement representative() {
        ErdenRegionalSettlementCatalog.Settlement settlement =
                ErdenRegionalCommunityManager.settlement(REPRESENTATIVE_ID);
        if (settlement == null) throw new IllegalStateException("Missing regional community representative");
        return settlement;
    }

    private static ErdenRegionalSocietySavedData.Household sampleHousehold(
            ErdenRegionalSocietySavedData society) {
        return society.households().stream()
                .filter(household -> household.settlementId().equals(REPRESENTATIVE_ID)
                        && household.homeRole().equals(SAMPLE_HOME_ROLE))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing regional community sample household"));
    }

    private static AABB settlementBounds(
            ServerLevel level,
            ErdenRegionalSettlementCatalog.Settlement settlement) {
        int radius = ErdenRegionalSettlementCatalog.SETTLEMENT_RADIUS + 40;
        return new AABB(
                settlement.x() - radius, level.getMinY(), settlement.z() - radius,
                settlement.x() + radius, level.getMaxY(), settlement.z() + radius);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        activeBuild = null;
        stage = ProbeStage.HOME;
        requested = false;
        stageRequested = false;
        passed = false;
        failed = false;
        physicalHomeEvidence = false;
        physicalSquareEvidence = false;
        physicalInnEvidence = false;
        physicalMarketEvidence = false;
        homeWalkableEvidence = false;
        squareWalkableEvidence = false;
        innWalkableEvidence = false;
        marketWalkableEvidence = false;
        residentIdentityEvidence = false;
        loadedRouteGuardEvidence = false;
        residentCountEvidence = 0;
        releasedChunks = 0;
        probeRequestedAt = 0L;
        lastTicketRefreshTick = Long.MIN_VALUE;
        lastWaitLogTick = Long.MIN_VALUE;
        RETAINED.clear();
        PROBED.clear();
    }

    private static long pack(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) packed;
    }

    private static boolean isCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }

    private enum ProbeStage {
        HOME("home"),
        SQUARE("square"),
        INN("inn"),
        MARKET("market");

        private final String id;

        ProbeStage(String id) {
            this.id = id;
        }

        String id() {
            return id;
        }

        ProbeStage next() {
            return switch (this) {
                case HOME -> SQUARE;
                case SQUARE -> INN;
                case INN -> MARKET;
                case MARKET -> MARKET;
            };
        }
    }

    private record ActiveBuild(
            long key,
            int chunkX,
            int chunkZ,
            IncrementalWorldEditPlan plan) {
    }
}
