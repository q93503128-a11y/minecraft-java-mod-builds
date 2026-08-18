package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persistent home/work routines for Erden's named citizens.
 *
 * <p>Every urban building now has an authored ground floor and a verified zero-cut upper route.
 * This manager therefore contains no synthetic-floor constructor and no ground-only fallback.
 * Legacy synthetic completion records remain readable only through the saved-data compatibility
 * accessor; no new record or synthetic block is ever created.</p>
 */
public final class ErdenUrbanLifeManager {
    public static final int UPPER_FLOOR_REVISION = 1;
    public static final int EXPECTED_CITIZEN_ASSIGNMENTS = 8;

    private static final int ROUTINE_INTERVAL = 40;
    private static final int CI_AUTHORED_HOME_CHUNK_RADIUS = 3;
    private static final Set<String> HABITABLE_ROLES = Set.of(
            "tenement", "shop", "bakery", "inn",
            "stable", "guard_post", "bathhouse", "warehouse"
    );
    private static final List<CitizenPlan> CITIZENS = List.of(
            new CitizenPlan("erden_guide", "기록관 마렌", 4, 8, "shop"),
            new CitizenPlan("erden_fisher", "어업조합원 로안", -165, 68, "warehouse"),
            new CitizenPlan("erden_neighbor", "석공 엘라", 30, 35, "warehouse"),
            new CitizenPlan("erden_clerk", "시장서기 페른", -70, 18, "shop"),
            new CitizenPlan("erden_smith", "철공조합장 하벨", 78, -35, "guard_post"),
            new CitizenPlan("erden_apothecary", "약제사 미라", -72, -76, "shop"),
            new CitizenPlan("erden_sergeant", "성문부사관 토렌", 0, -94, "guard_post"),
            new CitizenPlan("erden_carter", "마차조합원 베아", -96, 70, "stable")
    );

    private static MinecraftServer activeServer;
    private static boolean planLogged;
    private static boolean completionLogged;
    private static boolean assignmentsLogged;
    private static boolean ciChunksRequested;
    private static boolean authoredCiPassed;

    private ErdenUrbanLifeManager() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances = ExternalUrbanFabricBuilder.entrances();
        logPlanOnce(entrances);

        ErdenUrbanLifeSavedData life = level.getDataStorage().computeIfAbsent(ErdenUrbanLifeSavedData.TYPE);
        ensureCitizenAssignments(level, life, entrances);
        requestCiSampleChunks(level, life);
        runCitizenRoutines(level, life);
        verifyAuthoredRoutineCiIfReady(level, life);

        if (!completionLogged && ErdenUrbanInteriorBuilder.completedCount(level) == entrances.size()) {
            completionLogged = true;
            int authoredEligible = ErdenUrbanAuthoredUpperRouteManager.eligibleCount();
            if (authoredEligible != entrances.size() || ErdenUrbanResidenceResolver.groundOnlyBuildingCount() != 0) {
                throw new IllegalStateException("Erden residence mode coverage drifted authored="
                        + authoredEligible + " buildings=" + entrances.size());
            }
            LivingKingdoms.LOGGER.info(
                    "Completed Erden residence modes buildings={} authored_ground_buildings={} authored_upper_buildings={} ground_only_buildings=0 synthetic_upper_created=0 legacy_synthetic_records={} role_spaces={} revision={}",
                    entrances.size(), entrances.size(), authoredEligible,
                    life.completedUpperFloorCount(UPPER_FLOOR_REVISION), HABITABLE_ROLES.size(), UPPER_FLOOR_REVISION);
        }
    }

    /** Compatibility metric for old saves only; current code never creates these records. */
    public static int completedUpperFloorCount(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenUrbanLifeSavedData.TYPE)
                .completedUpperFloorCount(UPPER_FLOOR_REVISION);
    }

    public static int assignmentCount(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenUrbanLifeSavedData.TYPE)
                .assignments().size();
    }

    static boolean managesCitizenId(String citizenId) {
        return CITIZENS.stream().anyMatch(citizen -> citizen.id().equals(citizenId));
    }

    static AABB managedCitizenBounds(ServerLevel level) {
        return capitalBounds(level);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        planLogged = false;
        completionLogged = false;
        assignmentsLogged = false;
        ciChunksRequested = false;
        authoredCiPassed = false;
    }

    private static void logPlanOnce(List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances) {
        if (planLogged) return;
        if (entrances.isEmpty()) throw new IllegalStateException("Erden urban-life plan has no entrances");
        for (String role : HABITABLE_ROLES) {
            if (ExternalUrbanFabricBuilder.roleCount(role) <= 0) {
                throw new IllegalStateException("Erden urban-life plan is missing role " + role);
            }
        }
        int authoredEligible = ErdenUrbanAuthoredUpperRouteManager.eligibleCount();
        if (authoredEligible != entrances.size()) {
            throw new IllegalStateException("Erden urban-life requires authored upper coverage for all buildings: "
                    + authoredEligible + "/" + entrances.size());
        }
        planLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden residence modes buildings={} authored_ground_candidates={} authored_upper_candidates={} ground_only_candidates=0 synthetic_upper_created=0 role_spaces={} citizen_assignments={}",
                entrances.size(), entrances.size(), authoredEligible,
                HABITABLE_ROLES.size(), CITIZENS.size());
    }

    private static void requestCiSampleChunks(
            ServerLevel level,
            ErdenUrbanLifeSavedData life) {
        if (ciChunksRequested || !ciEnabled()) return;
        ExternalUrbanFabricBuilder.UrbanEntrance authoredHome = authoredHomeSample(life.assignments());
        if (authoredHome == null) return;
        int centerChunkX = Math.floorDiv(authoredHome.x(), 16);
        int centerChunkZ = Math.floorDiv(authoredHome.z(), 16);
        for (int chunkX = centerChunkX - CI_AUTHORED_HOME_CHUNK_RADIUS;
             chunkX <= centerChunkX + CI_AUTHORED_HOME_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = centerChunkZ - CI_AUTHORED_HOME_CHUNK_RADIUS;
                 chunkZ <= centerChunkZ + CI_AUTHORED_HOME_CHUNK_RADIUS; chunkZ++) {
                ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
            }
        }
        ciChunksRequested = true;
        LivingKingdoms.LOGGER.info(
                "Requested Erden urban-life CI authored home role={} home={},{} radius_chunks={} deterministic_assignment=true ground_only_sample=false",
                authoredHome.role(), authoredHome.x(), authoredHome.z(), CI_AUTHORED_HOME_CHUNK_RADIUS);
    }

    private static ExternalUrbanFabricBuilder.UrbanEntrance authoredHomeSample(
            List<ErdenUrbanLifeSavedData.Assignment> assignments) {
        for (ErdenUrbanLifeSavedData.Assignment assignment : assignments) {
            ExternalUrbanFabricBuilder.UrbanEntrance home = findEntrance(assignment.homeX(), assignment.homeZ());
            if (home != null && ErdenUrbanAuthoredUpperRouteManager.isEligible(home)) return home;
        }
        return null;
    }

    private static void ensureCitizenAssignments(
            ServerLevel level,
            ErdenUrbanLifeSavedData life,
            List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances) {
        if (assignmentsValid(life.assignments())) {
            logAssignmentsOnce(life.assignments());
            return;
        }
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(level, "erden_kingdom");
        if (site == null) return;

        Set<Long> usedHomes = new HashSet<>();
        Set<Long> usedWorkplaces = new HashSet<>();
        List<ErdenUrbanLifeSavedData.Assignment> assignments = new ArrayList<>();
        for (CitizenPlan citizen : CITIZENS) {
            int anchorX = site.centerX() + citizen.anchorOffsetX();
            int anchorZ = site.centerZ() + citizen.anchorOffsetZ();
            ExternalUrbanFabricBuilder.UrbanEntrance workplace = nearestEntrance(
                    entrances, citizen.workRole(), anchorX, anchorZ, usedWorkplaces);
            if (workplace == null) {
                throw new IllegalStateException("No Erden workplace for " + citizen.id()
                        + " role=" + citizen.workRole());
            }
            usedWorkplaces.add(entranceKey(workplace.x(), workplace.z()));
            ExternalUrbanFabricBuilder.UrbanEntrance home = nearestEntrance(
                    entrances, "tenement", workplace.x(), workplace.z(), usedHomes);
            if (home == null || !ErdenUrbanAuthoredUpperRouteManager.isEligible(home)) {
                throw new IllegalStateException("No authored Erden home for " + citizen.id());
            }
            usedHomes.add(entranceKey(home.x(), home.z()));
            assignments.add(new ErdenUrbanLifeSavedData.Assignment(
                    citizen.id(), citizen.name(), home.x(), home.z(),
                    workplace.x(), workplace.z(), citizen.workRole()));
        }
        if (!assignmentsValid(assignments)) {
            throw new IllegalStateException("Erden citizen assignment validation failed");
        }
        life.replaceAssignments(List.copyOf(assignments));
        logAssignmentsOnce(assignments);
    }

    private static boolean assignmentsValid(List<ErdenUrbanLifeSavedData.Assignment> assignments) {
        if (assignments.size() != EXPECTED_CITIZEN_ASSIGNMENTS) return false;
        Set<Long> homes = new HashSet<>();
        Set<Long> workplaces = new HashSet<>();
        for (ErdenUrbanLifeSavedData.Assignment assignment : assignments) {
            ExternalUrbanFabricBuilder.UrbanEntrance home = findEntrance(assignment.homeX(), assignment.homeZ());
            ExternalUrbanFabricBuilder.UrbanEntrance work = findEntrance(assignment.workX(), assignment.workZ());
            if (home == null || work == null
                    || !home.role().equals("tenement")
                    || !ErdenUrbanAuthoredUpperRouteManager.isEligible(home)
                    || !ErdenUrbanAuthoredUpperRouteManager.isEligible(work)
                    || !homes.add(entranceKey(home.x(), home.z()))
                    || !workplaces.add(entranceKey(work.x(), work.z()))) return false;
        }
        return true;
    }

    private static ExternalUrbanFabricBuilder.UrbanEntrance nearestEntrance(
            List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances,
            String role,
            int x,
            int z,
            Set<Long> excluded) {
        return entrances.stream()
                .filter(entrance -> entrance.role().equals(role))
                .filter(ErdenUrbanAuthoredUpperRouteManager::isEligible)
                .filter(entrance -> !excluded.contains(entranceKey(entrance.x(), entrance.z())))
                .min(Comparator.comparingLong(entrance -> distanceSquared(x, z, entrance.x(), entrance.z())))
                .orElse(null);
    }

    private static void logAssignmentsOnce(List<ErdenUrbanLifeSavedData.Assignment> assignments) {
        if (assignmentsLogged) return;
        if (!assignmentsValid(assignments)) throw new IllegalStateException("Invalid Erden citizen assignments");
        assignmentsLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden citizen home-work assignments citizens={} unique_homes=true unique_workplaces=true routines=home,work authored_ground=true authored_upper=true synthetic_fallback=false",
                assignments.size());
    }

    private static void runCitizenRoutines(
            ServerLevel level,
            ErdenUrbanLifeSavedData life) {
        if (level.getGameTime() % ROUTINE_INTERVAL != 0L) return;
        List<ErdenUrbanLifeSavedData.Assignment> assignments = life.assignments();
        if (assignments.isEmpty()) return;

        Map<String, ErdenUrbanLifeSavedData.Assignment> byName = new HashMap<>();
        for (ErdenUrbanLifeSavedData.Assignment assignment : assignments) {
            byName.put(assignment.citizenName(), assignment);
        }
        List<Villager> villagers = level.getEntitiesOfClass(
                Villager.class, capitalBounds(level),
                villager -> byName.containsKey(villager.getName().getString()));
        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        boolean working = dayTime >= 2_000L && dayTime < 11_000L;
        for (Villager villager : villagers) {
            ErdenUrbanLifeSavedData.Assignment assignment = byName.get(villager.getName().getString());
            BlockPos target = resolveTarget(level, assignment, working);
            if (target == null) continue;
            villager.setPersistenceRequired();
            if (villager.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 4.0D) {
                villager.getNavigation().moveTo(
                        target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.62D);
            }
        }
    }

    private static BlockPos resolveTarget(
            ServerLevel level,
            ErdenUrbanLifeSavedData.Assignment assignment,
            boolean workplace) {
        int x = workplace ? assignment.workX() : assignment.homeX();
        int z = workplace ? assignment.workZ() : assignment.homeZ();
        ExternalUrbanFabricBuilder.UrbanEntrance entrance = findEntrance(x, z);
        if (entrance == null) return null;
        return workplace
                ? ErdenUrbanResidenceResolver.resolveWorkTarget(level, entrance)
                : ErdenUrbanResidenceResolver.resolveHomeTarget(level, entrance, 1);
    }

    private static void verifyAuthoredRoutineCiIfReady(
            ServerLevel level,
            ErdenUrbanLifeSavedData life) {
        if (authoredCiPassed || !ciEnabled()
                || life.assignments().size() != EXPECTED_CITIZEN_ASSIGNMENTS) return;
        for (ErdenUrbanLifeSavedData.Assignment assignment : life.assignments()) {
            ExternalUrbanFabricBuilder.UrbanEntrance home = findEntrance(assignment.homeX(), assignment.homeZ());
            if (home == null || !ErdenUrbanAuthoredUpperRouteManager.isCompleted(level, home)) continue;
            BlockPos authored = ErdenUrbanAuthoredUpperRouteManager.verifiedUpperTarget(level, home);
            if (authored == null) continue;
            ErdenUrbanAuthoredUpperRouteManager.verifyOrThrow(level, home);
            BlockPos resolved = ErdenUrbanResidenceResolver.resolveHomeTarget(level, home, 1);
            if (resolved == null || resolved.getY() != authored.getY()) {
                throw new IllegalStateException("Erden authored home routine did not resolve upper citizen="
                        + assignment.citizenName());
            }
            verifyRuntimeTargetGeometry(level, resolved);
            if (life.isUpperFloorComplete(entranceKey(home.x(), home.z()), UPPER_FLOOR_REVISION)) {
                throw new IllegalStateException("Fresh Erden authored home was marked synthetic-complete role="
                        + home.role());
            }
            authoredCiPassed = true;
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_URBAN_AUTHORED_HOME_PASS citizen={} role={} authored_ground=true authored_upper=true synthetic_upper_required=false route_verified=true resident_target_verified=true runtime_geometry_verified=true source_blocks_cut=0 routine=home",
                    assignment.citizenName(), home.role());
            return;
        }
    }

    private static void verifyRuntimeTargetGeometry(ServerLevel level, BlockPos target) {
        BlockState feet = level.getBlockState(target);
        BlockState head = level.getBlockState(target.above());
        BlockState floor = level.getBlockState(target.below());
        if (!feet.isAir() || !head.isAir() || floor.isAir() || !floor.getFluidState().isEmpty()) {
            throw new IllegalStateException("Erden authored resident target lacks walkable geometry target="
                    + target);
        }
    }

    private static ExternalUrbanFabricBuilder.UrbanEntrance findEntrance(int x, int z) {
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : ExternalUrbanFabricBuilder.entrances()) {
            if (entrance.x() == x && entrance.z() == z) return entrance;
        }
        return null;
    }

    private static AABB capitalBounds(ServerLevel level) {
        return new AABB(
                ErdenCapitalStreamingBuilder.WEST_WALL_X - 64,
                level.getMinY(),
                ErdenCapitalStreamingBuilder.NORTH_WALL_Z - 64,
                ErdenCapitalStreamingBuilder.EAST_WALL_X + 64,
                level.getMaxY(),
                ErdenCapitalStreamingBuilder.SOUTH_WALL_Z + 64);
    }

    private static long entranceKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static long distanceSquared(int x1, int z1, int x2, int z2) {
        long dx = x1 - (long) x2;
        long dz = z1 - (long) z2;
        return dx * dx + dz * dz;
    }

    private static boolean ciEnabled() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }

    private record CitizenPlan(
            String id, String name, int anchorOffsetX, int anchorOffsetZ, String workRole) {}
}
