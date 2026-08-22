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
    private static final long WAIT_LOG_INTERVAL = 200L;
    private static final long PROBE_TIMEOUT_TICKS = 12_000L;

    private static final Set<Long> RETAINED = new LinkedHashSet<>();
    private static MinecraftServer activeServer;
    private static ActiveBuild activeBuild;
    private static boolean requested;
    private static boolean passed;
    private static boolean failed;
    private static long probeRequestedAt;
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
        // Wait for that authoritative stream before this focused probe expands only around the
        // four destination neighborhoods needed by the community acceptance check.
        if (!construction.isBuilt(homeKey, ErdenRegionalSettlementCatalog.REVISION)
                || !construction.isBuilt(marketKey, ErdenRegionalSettlementCatalog.REVISION)) return;

        if (!requested) requestProbe(level, settlement, household);
        if (timedOut(level)) {
            failAndRelease(level, "timeout_before_physical_acceptance");
            return;
        }
        if (!allLoaded(level)) return;
        if (!buildProbe(level, construction)) return;
        verifyAndFinish(level, society, economy, settlement, household);
    }

    private static void requestProbe(
            ServerLevel level,
            ErdenRegionalSettlementCatalog.Settlement settlement,
            ErdenRegionalSocietySavedData.Household household) {
        requested = true;
        probeRequestedAt = level.getGameTime();
        lastWaitLogTick = Long.MIN_VALUE;

        addProbeArea(level, household.homeX(), household.homeZ(), DESTINATION_PROBE_RADIUS);
        addProbeArea(level, settlement.x(), settlement.z(), DESTINATION_PROBE_RADIUS);
        ErdenRegionalSettlementCatalog.BuildingLot inn =
                ErdenRegionalCommunityManager.requireLot(settlement, "village_inn");
        addProbeArea(
                level, settlement.x() + inn.dx(), settlement.z() + inn.dz(), DESTINATION_PROBE_RADIUS);
        BlockPos market = ErdenRegionalEconomyManager.storagePosition(settlement);
        addProbeArea(level, market.getX(), market.getZ(), DESTINATION_PROBE_RADIUS);
        LivingKingdoms.LOGGER.info(
                "Requested Erden regional community physical CI probe settlement={} chunks={} destination_radius={} transient_ticket=portal persistent_forced_chunks=false",
                REPRESENTATIVE_ID, RETAINED.size(), DESTINATION_PROBE_RADIUS);
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
        level.getChunkSource().addTicketAndLoadWithRadius(
                TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
    }

    private static boolean allLoaded(ServerLevel level) {
        if (RETAINED.isEmpty()) return false;
        for (long key : RETAINED) {
            if (!level.hasChunk(unpackX(key), unpackZ(key))) return false;
        }
        return true;
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

    private static void verifyAndFinish(
            ServerLevel level,
            ErdenRegionalSocietySavedData society,
            ErdenRegionalEconomySavedData economy,
            ErdenRegionalSettlementCatalog.Settlement settlement,
            ErdenRegionalSocietySavedData.Household household) {
        ErdenRegionalSettlementSavedData construction = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSettlementSavedData.TYPE);
        for (long key : RETAINED) {
            if (!construction.isBuilt(key, ErdenRegionalSettlementCatalog.REVISION)) return;
        }

        Set<String> expectedNames = new LinkedHashSet<>();
        for (ErdenRegionalSocietySavedData.Resident resident : household.residents()) {
            if (!society.isDead(resident.id())) expectedNames.add(resident.name());
        }
        List<Villager> residents = level.getEntitiesOfClass(
                Villager.class, settlementBounds(level, settlement),
                candidate -> expectedNames.contains(candidate.getName().getString()));
        Set<String> present = new LinkedHashSet<>();
        for (Villager resident : residents) present.add(resident.getName().getString());
        boolean residentIdentity = expectedNames.size() == EXPECTED_RESIDENTS
                && present.size() == EXPECTED_RESIDENTS;

        int baseY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(settlement.x(), settlement.z()));
        boolean physicalSquare = level.getBlockState(
                new BlockPos(settlement.x(), baseY, settlement.z())).is(Blocks.WATER)
                && level.getBlockState(
                new BlockPos(settlement.x() + 2, baseY, settlement.z() + 2)).is(Blocks.STONE_BRICKS);
        ErdenRegionalSettlementCatalog.BuildingLot inn =
                ErdenRegionalCommunityManager.requireLot(settlement, "village_inn");
        int innX = settlement.x() + inn.dx();
        int innZ = settlement.z() + inn.dz();
        boolean physicalInn = hasStructureNear(level, innX, innZ, 12, 24);
        boolean physicalHome = hasStructureNear(level, household.homeX(), household.homeZ(), 12, 24);
        BlockPos market = ErdenRegionalEconomyManager.storagePosition(settlement);
        boolean physicalMarket = level.getBlockState(market).is(Blocks.BARREL)
                && economy.storageMaterialized(REPRESENTATIVE_ID);

        ErdenRegionalCommunityManager.ResidentContext context =
                new ErdenRegionalCommunityManager.ResidentContext(
                        household, household.residents().getFirst());
        BlockPos homeTarget = ErdenRegionalCommunityManager.activityTarget(
                level, society, context, ErdenRegionalCommunityManager.Activity.HOME, 0L);
        BlockPos squareTarget = ErdenRegionalCommunityManager.activityTarget(
                level, society, context, ErdenRegionalCommunityManager.Activity.SQUARE, 0L);
        BlockPos innTarget = ErdenRegionalCommunityManager.activityTarget(
                level, society, context, ErdenRegionalCommunityManager.Activity.INN, 0L);
        BlockPos marketTarget = ErdenRegionalCommunityManager.activityTarget(
                level, society, context, ErdenRegionalCommunityManager.Activity.MARKET, 0L);
        boolean destinationsWalkable = !homeTarget.equals(BlockPos.ZERO)
                && !squareTarget.equals(BlockPos.ZERO)
                && !innTarget.equals(BlockPos.ZERO)
                && !marketTarget.equals(BlockPos.ZERO);

        // Do not make the route-guard proof depend on whether the four physical destination probes
        // happen to load every chunk between home and inn. A deliberately distant, never-retained
        // endpoint tests the runtime contract directly: routeLoaded must reject a route that leaves
        // the already-loaded world without itself creating a ticket.
        BlockPos guardTarget = homeTarget.offset(ROUTE_GUARD_DISTANCE, 0, ROUTE_GUARD_DISTANCE);
        boolean loadedRouteGuard = destinationsWalkable
                && !ErdenRegionalCommunityManager.routeLoaded(level, homeTarget, guardTarget);

        if (!residentIdentity || !physicalSquare || !physicalInn || !physicalHome || !physicalMarket
                || !destinationsWalkable || !loadedRouteGuard) {
            logWait(
                    level, present.size(), physicalHome, physicalSquare, physicalInn, physicalMarket,
                    destinationsWalkable, loadedRouteGuard,
                    homeTarget, squareTarget, innTarget, marketTarget);
            return;
        }

        int released = releaseProbe(level);
        passed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_REGIONAL_COMMUNITY_PHYSICAL_PASS revision={} settlement={} residents={} destinations=4 probe_chunks={} physical_home=true physical_square=true physical_inn=true physical_market=true resident_identity=true destinations_walkable=true transient_probe_released=true navigation_only=true loaded_route_guard=true persistent_forced_chunks=false",
                ErdenRegionalCommunityManager.COMMUNITY_REVISION,
                REPRESENTATIVE_ID, present.size(), released);
    }

    private static void logWait(
            ServerLevel level,
            int residentCount,
            boolean physicalHome,
            boolean physicalSquare,
            boolean physicalInn,
            boolean physicalMarket,
            boolean destinationsWalkable,
            boolean loadedRouteGuard,
            BlockPos homeTarget,
            BlockPos squareTarget,
            BlockPos innTarget,
            BlockPos marketTarget) {
        long now = level.getGameTime();
        if (lastWaitLogTick != Long.MIN_VALUE && now - lastWaitLogTick < WAIT_LOG_INTERVAL) return;
        lastWaitLogTick = now;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_REGIONAL_COMMUNITY_PHYSICAL_WAIT residents={}/{} home={} square={} inn={} market={} destinations_walkable={} loaded_route_guard={} home_target={} square_target={} inn_target={} market_target={} retained_chunks={} elapsed_ticks={}",
                residentCount, EXPECTED_RESIDENTS,
                physicalHome, physicalSquare, physicalInn, physicalMarket,
                destinationsWalkable, loadedRouteGuard,
                homeTarget, squareTarget, innTarget, marketTarget,
                RETAINED.size(), now - probeRequestedAt);
    }

    private static boolean timedOut(ServerLevel level) {
        return requested && level.getGameTime() - probeRequestedAt > PROBE_TIMEOUT_TICKS;
    }

    private static void failAndRelease(ServerLevel level, String reason) {
        int released = releaseProbe(level);
        failed = true;
        LivingKingdoms.LOGGER.error(
                "LK_ERDEN_REGIONAL_COMMUNITY_PHYSICAL_FAIL revision={} settlement={} reason={} released_chunks={} transient_probe_released=true persistent_forced_chunks=false",
                ErdenRegionalCommunityManager.COMMUNITY_REVISION,
                REPRESENTATIVE_ID, reason, released);
    }

    private static int releaseProbe(ServerLevel level) {
        int released = RETAINED.size();
        for (long key : Set.copyOf(RETAINED)) {
            level.getChunkSource().removeTicketWithRadius(
                    TicketType.PORTAL, new ChunkPos(unpackX(key), unpackZ(key)), 0);
            RETAINED.remove(key);
        }
        activeBuild = null;
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
        requested = false;
        passed = false;
        failed = false;
        probeRequestedAt = 0L;
        lastWaitLogTick = Long.MIN_VALUE;
        RETAINED.clear();
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

    private record ActiveBuild(
            long key,
            int chunkX,
            int chunkZ,
            IncrementalWorldEditPlan plan) {
    }
}
