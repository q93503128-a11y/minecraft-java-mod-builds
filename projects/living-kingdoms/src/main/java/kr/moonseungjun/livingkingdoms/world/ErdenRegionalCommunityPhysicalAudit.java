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

    private static final Set<Long> RETAINED = new LinkedHashSet<>();
    private static MinecraftServer activeServer;
    private static ActiveBuild activeBuild;
    private static boolean requested;
    private static boolean passed;

    private ErdenRegionalCommunityPhysicalAudit() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!isCi()) return;
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom") || passed) return;

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
        // Wait for that authoritative stream to finish before this focused probe adds square/inn chunks.
        if (!construction.isBuilt(homeKey, ErdenRegionalSettlementCatalog.REVISION)
                || !construction.isBuilt(marketKey, ErdenRegionalSettlementCatalog.REVISION)) return;

        if (!requested) requestProbe(level, settlement, household);
        if (!allLoaded(level)) return;
        if (!buildProbe(level, construction)) return;
        verifyAndFinish(level, society, economy, settlement, household);
    }

    private static void requestProbe(
            ServerLevel level,
            ErdenRegionalSettlementCatalog.Settlement settlement,
            ErdenRegionalSocietySavedData.Household household) {
        requested = true;
        addProbe(level, household.homeX() >> 4, household.homeZ() >> 4);
        addProbe(level, settlement.x() >> 4, settlement.z() >> 4);
        ErdenRegionalSettlementCatalog.BuildingLot inn =
                ErdenRegionalCommunityManager.requireLot(settlement, "village_inn");
        addProbe(level, (settlement.x() + inn.dx()) >> 4, (settlement.z() + inn.dz()) >> 4);
        BlockPos market = ErdenRegionalEconomyManager.storagePosition(settlement);
        addProbe(level, market.getX() >> 4, market.getZ() >> 4);
        LivingKingdoms.LOGGER.info(
                "Requested Erden regional community physical CI probe settlement={} chunks={} transient_ticket=portal persistent_forced_chunks=false",
                REPRESENTATIVE_ID, RETAINED.size());
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
        if (expectedNames.size() != EXPECTED_RESIDENTS) return;
        List<Villager> residents = level.getEntitiesOfClass(
                Villager.class, settlementBounds(level, settlement),
                candidate -> expectedNames.contains(candidate.getName().getString()));
        Set<String> present = new LinkedHashSet<>();
        for (Villager resident : residents) present.add(resident.getName().getString());
        if (present.size() != EXPECTED_RESIDENTS) return;

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
        boolean loadedRouteGuard = !ErdenRegionalCommunityManager.routeLoaded(level, homeTarget, innTarget);
        if (!physicalSquare || !physicalInn || !physicalHome || !physicalMarket
                || !destinationsWalkable || !loadedRouteGuard) return;

        int released = RETAINED.size();
        for (long key : Set.copyOf(RETAINED)) {
            level.getChunkSource().removeTicketWithRadius(
                    TicketType.PORTAL, new ChunkPos(unpackX(key), unpackZ(key)), 0);
            RETAINED.remove(key);
        }
        passed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_REGIONAL_COMMUNITY_PHYSICAL_PASS revision={} settlement={} residents={} destinations=4 probe_chunks={} physical_home=true physical_square=true physical_inn=true physical_market=true resident_identity=true destinations_walkable=true transient_probe_released=true navigation_only=true loaded_route_guard=true persistent_forced_chunks=false",
                ErdenRegionalCommunityManager.COMMUNITY_REVISION,
                REPRESENTATIVE_ID, present.size(), released);
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
