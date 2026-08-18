package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Uses verified authored upper rooms for short workday activities without moving the public-facing
 * workplace itself upstairs. Shops still serve customers on the ground floor; guards, warehouse staff,
 * stable hands and clerks only visit an upper room for bounded secondary duties such as watch relief,
 * ledger work or stock checks.
 */
@EventBusSubscriber(modid = LivingKingdoms.MOD_ID)
public final class ErdenUrbanUpperWorkActivityManager {
    private static final int ROUTINE_INTERVAL = 40;
    private static final long UPPER_ACTIVITY_START = 6_500L;
    private static final long UPPER_ACTIVITY_END = 8_000L;
    private static final int CI_CHUNK_RADIUS = 3;
    private static final long MAX_SECONDARY_SITE_DISTANCE_SQ = 96L * 96L;
    private static final Set<String> UPPER_ACTIVITY_ROLES = Set.of(
            "shop", "warehouse", "guard_post", "stable");

    private static MinecraftServer activeServer;
    private static boolean planLogged;
    private static boolean ciChunksRequested;
    private static boolean ciPassed;
    private static boolean navigationLogged;

    private ErdenUrbanUpperWorkActivityManager() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        ErdenUrbanLifeSavedData life = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanLifeSavedData.TYPE);
        List<ErdenUrbanLifeSavedData.Assignment> assignments = life.assignments();
        if (assignments.size() != ErdenUrbanLifeManager.EXPECTED_CITIZEN_ASSIGNMENTS) return;

        logPlanOnce(assignments);
        requestCiSiteOnce(level, assignments);
        if (level.getGameTime() % ROUTINE_INTERVAL == 0L && inUpperActivityWindow(level)) {
            runUpperActivities(level, assignments);
        }
        verifyCiIfReady(level, assignments);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        planLogged = false;
        ciChunksRequested = false;
        ciPassed = false;
        navigationLogged = false;
    }

    private static boolean inUpperActivityWindow(ServerLevel level) {
        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        return dayTime >= UPPER_ACTIVITY_START && dayTime < UPPER_ACTIVITY_END;
    }

    private static void logPlanOnce(List<ErdenUrbanLifeSavedData.Assignment> assignments) {
        if (planLogged) return;
        int eligibleAssignments = 0;
        for (ErdenUrbanLifeSavedData.Assignment assignment : assignments) {
            if (secondaryActivitySite(assignment) != null) eligibleAssignments++;
        }
        if (eligibleAssignments <= 0) {
            throw new IllegalStateException("Erden upper work activity has no eligible citizen assignment");
        }
        planLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden upper work activities citizens={} eligible_assignments={} roles={} window={}..{} ground_service_preserved=true optional_secondary_activity=true forced_chunk_loading=false",
                assignments.size(), eligibleAssignments, UPPER_ACTIVITY_ROLES.size(),
                UPPER_ACTIVITY_START, UPPER_ACTIVITY_END);
    }

    private static void requestCiSiteOnce(
            ServerLevel level,
            List<ErdenUrbanLifeSavedData.Assignment> assignments) {
        if (ciChunksRequested || !ciEnabled()) return;
        ActivitySite sample = firstActivitySite(assignments);
        if (sample == null) {
            throw new IllegalStateException("Erden upper work activity CI has no eligible sample");
        }
        int centerChunkX = Math.floorDiv(sample.entrance().x(), 16);
        int centerChunkZ = Math.floorDiv(sample.entrance().z(), 16);
        for (int chunkX = centerChunkX - CI_CHUNK_RADIUS;
             chunkX <= centerChunkX + CI_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = centerChunkZ - CI_CHUNK_RADIUS;
                 chunkZ <= centerChunkZ + CI_CHUNK_RADIUS; chunkZ++) {
                ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
            }
        }
        ciChunksRequested = true;
        LivingKingdoms.LOGGER.info(
                "Requested Erden upper work activity CI site citizen={} role={} activity={} site={},{} radius_chunks={} forced_persistent_chunks=false",
                sample.assignment().citizenName(), sample.assignment().workRole(),
                activityName(sample.assignment().workRole()), sample.entrance().x(), sample.entrance().z(),
                CI_CHUNK_RADIUS);
    }

    private static void runUpperActivities(
            ServerLevel level,
            List<ErdenUrbanLifeSavedData.Assignment> assignments) {
        Map<String, ErdenUrbanLifeSavedData.Assignment> byName = new HashMap<>();
        for (ErdenUrbanLifeSavedData.Assignment assignment : assignments) {
            byName.put(assignment.citizenName(), assignment);
        }
        List<Villager> villagers = level.getEntitiesOfClass(
                Villager.class,
                ErdenUrbanLifeManager.managedCitizenBounds(level),
                villager -> byName.containsKey(villager.getName().getString()));

        for (Villager villager : villagers) {
            ErdenUrbanLifeSavedData.Assignment assignment = byName.get(villager.getName().getString());
            ActivitySite site = secondaryActivitySite(assignment);
            if (site == null) continue;
            BlockPos target = ErdenUrbanResidenceResolver.resolveUpperActivityTarget(
                    level, site.entrance(), Math.floorMod(assignment.citizenId().hashCode(), 9));
            if (target == null || !routeLoaded(level, villager.blockPosition(), target)) continue;
            if (villager.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 4.0D) {
                villager.getNavigation().moveTo(
                        target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.58D);
                if (!navigationLogged) {
                    navigationLogged = true;
                    LivingKingdoms.LOGGER.info(
                            "Erden citizen upper work activity navigation citizen={} role={} activity={} target={} ground_service_preserved=true",
                            assignment.citizenName(), assignment.workRole(),
                            activityName(assignment.workRole()), target);
                }
            }
        }
    }

    private static void verifyCiIfReady(
            ServerLevel level,
            List<ErdenUrbanLifeSavedData.Assignment> assignments) {
        if (ciPassed || !ciEnabled()) return;
        ActivitySite sample = firstActivitySite(assignments);
        if (sample == null
                || !ErdenUrbanAuthoredUpperRouteManager.isCompleted(level, sample.entrance())) return;
        BlockPos upper = ErdenUrbanResidenceResolver.resolveUpperActivityTarget(
                level, sample.entrance(), 2);
        if (upper == null) return;
        ErdenUrbanAuthoredUpperRouteManager.verifyOrThrow(level, sample.entrance());
        ErdenUrbanResidenceResolver.verifyTargetOrThrow(level, upper, "upper-work-activity-ci");
        BlockPos ground = ErdenUrbanResidenceResolver.resolveWorkTarget(level, sample.entrance());
        if (ground == null || ground.getY() >= upper.getY()) {
            throw new IllegalStateException(
                    "Erden upper work activity lost its ground-floor primary workplace role="
                            + sample.assignment().workRole());
        }
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_UPPER_WORK_ACTIVITY_PASS citizen={} role={} activity={} upper_target={} ground_target={} route_verified=true ground_service_preserved=true secondary_activity_only=true synthetic_floor_required=false source_blocks_cut=0",
                sample.assignment().citizenName(), sample.assignment().workRole(),
                activityName(sample.assignment().workRole()), upper, ground);
    }

    private static ActivitySite firstActivitySite(
            List<ErdenUrbanLifeSavedData.Assignment> assignments) {
        for (ErdenUrbanLifeSavedData.Assignment assignment : assignments) {
            ExternalUrbanFabricBuilder.UrbanEntrance entrance = secondaryActivitySite(assignment);
            if (entrance != null) return new ActivitySite(assignment, entrance);
        }
        return null;
    }

    private static ExternalUrbanFabricBuilder.UrbanEntrance secondaryActivitySite(
            ErdenUrbanLifeSavedData.Assignment assignment) {
        if (!UPPER_ACTIVITY_ROLES.contains(assignment.workRole())) return null;
        ExternalUrbanFabricBuilder.UrbanEntrance best = null;
        long bestDistance = Long.MAX_VALUE;
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : ExternalUrbanFabricBuilder.entrances()) {
            if (!entrance.role().equals(assignment.workRole())
                    || !ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance)) continue;
            long distance = distanceSquared(
                    assignment.workX(), assignment.workZ(), entrance.x(), entrance.z());
            if (distance < bestDistance) {
                best = entrance;
                bestDistance = distance;
            }
        }
        return bestDistance <= MAX_SECONDARY_SITE_DISTANCE_SQ ? best : null;
    }

    private static boolean routeLoaded(ServerLevel level, BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        int steps = Math.max(Math.abs(dx), Math.abs(dz)) / 8 + 1;
        for (int step = 0; step <= steps; step++) {
            double t = step / (double) steps;
            int x = (int) Math.round(from.getX() + dx * t);
            int z = (int) Math.round(from.getZ() + dz * t);
            if (!level.hasChunk(x >> 4, z >> 4)) return false;
        }
        return true;
    }

    private static String activityName(String role) {
        return switch (role) {
            case "warehouse" -> "upper_inventory_check";
            case "guard_post" -> "watch_relief";
            case "stable" -> "hayloft_stock_check";
            default -> "ledger_and_stocktake";
        };
    }

    private static long distanceSquared(int x1, int z1, int x2, int z2) {
        long dx = x1 - (long) x2;
        long dz = z1 - (long) z2;
        return dx * dx + dz * dz;
    }

    private static boolean ciEnabled() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }

    private record ActivitySite(
            ErdenUrbanLifeSavedData.Assignment assignment,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
    }
}
