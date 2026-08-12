package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
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
 * Gives named Erden citizens persistent homes and workplaces while providing a synthetic upper-floor
 * fallback only for buildings that do not have a verified authored route to a real source upper room.
 * Authored-eligible buildings remain non-destructive: legacy synthetic floors are never removed, but
 * fresh worlds do not create or falsely complete a synthetic upper before the authored route exists.
 */
public final class ErdenUrbanLifeManager {
    public static final int UPPER_FLOOR_REVISION = 1;
    public static final int EXPECTED_CITIZEN_ASSIGNMENTS = 8;

    private static final int HALF_WIDTH = 3;
    private static final int DEPTH = 9;
    private static final int UPPER_FLOOR_OFFSET = 5;
    private static final int UPPER_CLEAR_HEIGHT = 3;
    private static final int PROCESS_BUDGET = 1;
    private static final int ROUTINE_INTERVAL = 40;
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
    private static boolean ciPassed;
    private static boolean authoredCiPassed;

    private ErdenUrbanLifeManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances =
                ExternalUrbanFabricBuilder.entrances();
        logPlanOnce(entrances);
        requestCiSampleChunks(level);

        ErdenUrbanLifeSavedData life = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanLifeSavedData.TYPE);
        ensureCitizenAssignments(level, life, entrances);
        completeOneUpperFloor(level, life, entrances);
        runCitizenRoutines(level, life);
        verifyCiIfReady(level, life);

        int complete = life.completedUpperFloorCount(UPPER_FLOOR_REVISION);
        int authoredEligible = ErdenUrbanAuthoredUpperRouteManager.eligibleCount();
        int syntheticRequired = entrances.size() - authoredEligible;
        if (!completionLogged && complete >= syntheticRequired) {
            completionLogged = true;
            LivingKingdoms.LOGGER.info(
                    "Completed Erden walkable upper floors buildings={} synthetic_completed={} synthetic_required={} authored_upper_buildings={} stairs=true role_spaces={} revision={}",
                    entrances.size(), complete, syntheticRequired, authoredEligible,
                    HABITABLE_ROLES.size(), UPPER_FLOOR_REVISION);
        }
    }

    public static int completedUpperFloorCount(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenUrbanLifeSavedData.TYPE)
                .completedUpperFloorCount(UPPER_FLOOR_REVISION);
    }

    public static int assignmentCount(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenUrbanLifeSavedData.TYPE)
                .assignments().size();
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        planLogged = false;
        completionLogged = false;
        assignmentsLogged = false;
        ciChunksRequested = false;
        ciPassed = false;
        authoredCiPassed = false;
    }

    private static void logPlanOnce(List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances) {
        if (planLogged) return;
        if (entrances.isEmpty()) {
            throw new IllegalStateException("Erden upper-floor plan has no urban entrances");
        }
        for (String role : HABITABLE_ROLES) {
            if (ExternalUrbanFabricBuilder.roleCount(role) <= 0) {
                throw new IllegalStateException("Erden upper-floor plan is missing role " + role);
            }
        }
        int authoredEligible = ErdenUrbanAuthoredUpperRouteManager.eligibleCount();
        planLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden upper-floor conversions buildings={} synthetic_candidates={} authored_candidates={} stair_steps=4 role_spaces={} citizen_assignments={}",
                entrances.size(), entrances.size() - authoredEligible, authoredEligible,
                HABITABLE_ROLES.size(), CITIZENS.size());
    }

    private static void requestCiSampleChunks(ServerLevel level) {
        if (ciChunksRequested
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        ExternalUrbanFabricBuilder.UrbanEntrance sample = syntheticCiSample();
        if (sample == null) {
            throw new IllegalStateException("Erden urban-life CI has no synthetic fallback sample");
        }
        Bounds bounds = room(sample, 0).bounds();
        for (int chunkX = Math.floorDiv(bounds.minX, 16);
             chunkX <= Math.floorDiv(bounds.maxX, 16); chunkX++) {
            for (int chunkZ = Math.floorDiv(bounds.minZ, 16);
                 chunkZ <= Math.floorDiv(bounds.maxZ, 16); chunkZ++) {
                ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
            }
        }
        ciChunksRequested = true;
    }

    private static ExternalUrbanFabricBuilder.UrbanEntrance syntheticCiSample() {
        ExternalUrbanFabricBuilder.UrbanEntrance diagnostic =
                ExternalUrbanFabricBuilder.diagnosticEntrance();
        if (diagnostic != null
                && !ErdenUrbanAuthoredUpperRouteManager.isEligible(diagnostic)) {
            return diagnostic;
        }
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance
                : ExternalUrbanFabricBuilder.entrances()) {
            if (!ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance)) return entrance;
        }
        return null;
    }

    private static void completeOneUpperFloor(
            ServerLevel level,
            ErdenUrbanLifeSavedData life,
            List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances) {
        ErdenUrbanInteriorSavedData groundFloors = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE);
        int built = 0;
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : entrances) {
            if (built >= PROCESS_BUDGET) return;
            long key = entranceKey(entrance.x(), entrance.z());
            // Preserve a legacy synthetic floor if it already exists, but never create a new one for
            // a building whose real authored upper room has an approved source-air route.
            if (life.isUpperFloorComplete(key, UPPER_FLOOR_REVISION)) continue;
            if (ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance)) continue;
            if (!groundFloors.isComplete(key, ErdenUrbanInteriorBuilder.INTERIOR_REVISION)) continue;
            Room geometry = room(entrance, 0);
            if (!chunksReady(level, geometry.bounds())) continue;
            int doorY = findLowestDoorY(level, entrance.x(), entrance.z());
            if (doorY == Integer.MIN_VALUE) continue;
            Room room = room(entrance, doorY - 1);
            buildUpperFloor(level, room);
            verifyUpperFloor(level, room);
            life.markUpperFloorComplete(key, UPPER_FLOOR_REVISION);
            built++;
        }
    }

    private static void buildUpperFloor(ServerLevel level, Room room) {
        int upperFloorY = room.floorY + UPPER_FLOOR_OFFSET;
        Block floor = upperFloorBlock(room.role);
        for (int depth = 1; depth <= DEPTH; depth++) {
            for (int lateral = -HALF_WIDTH; lateral <= HALF_WIDTH; lateral++) {
                Point point = room.point(lateral, depth);
                if (!isStairwell(lateral, depth)) {
                    set(level, point.x, upperFloorY, point.z, floor.defaultBlockState());
                }
                for (int y = 1; y <= UPPER_CLEAR_HEIGHT; y++) {
                    set(level, point.x, upperFloorY + y, point.z,
                            Blocks.AIR.defaultBlockState());
                }
            }
        }

        for (int step = 0; step < 4; step++) {
            Point point = room.point(2, 2 + step);
            int stairY = room.floorY + 1 + step;
            BlockState stair = Blocks.OAK_STAIRS.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, room.inwardDirection);
            set(level, point.x, stairY, point.z, stair);
            set(level, point.x, stairY + 1, point.z, Blocks.AIR.defaultBlockState());
            set(level, point.x, stairY + 2, point.z, Blocks.AIR.defaultBlockState());
        }
        Point landing = room.point(2, 6);
        set(level, landing.x, upperFloorY, landing.z, floor.defaultBlockState());
        set(level, landing.x, upperFloorY + 1, landing.z, Blocks.AIR.defaultBlockState());
        placeUpperFixtures(level, room);
    }

    private static Block upperFloorBlock(String role) {
        return switch (role) {
            case "guard_post", "bathhouse" -> Blocks.SMOOTH_STONE;
            case "stable", "warehouse" -> Blocks.SPRUCE_PLANKS;
            default -> Blocks.OAK_PLANKS;
        };
    }

    private static void placeUpperFixtures(ServerLevel level, Room room) {
        switch (room.role) {
            case "tenement" -> {
                placeUpperBed(level, room, -2, 3, bed("white_bed"));
                placeUpperBed(level, room, -2, 7, bed("brown_bed"));
                placeUpperBed(level, room, 1, 7, bed("light_gray_bed"));
                placeUpper(level, room, 3, 8, 1, Blocks.BARREL);
                placeUpper(level, room, -3, 8, 1, Blocks.BARREL);
            }
            case "inn" -> {
                placeUpperBed(level, room, -2, 3, bed("red_bed"));
                placeUpperBed(level, room, -2, 7, bed("blue_bed"));
                placeUpperBed(level, room, 1, 7, bed("green_bed"));
                placeUpper(level, room, 3, 8, 1, Blocks.CHEST);
                placeUpper(level, room, -3, 8, 1, Blocks.BARREL);
            }
            case "shop" -> {
                placeUpperBed(level, room, -2, 7, bed("yellow_bed"));
                placeUpper(level, room, -3, 3, 1, Blocks.BOOKSHELF);
                placeUpper(level, room, -3, 5, 1, Blocks.BOOKSHELF);
                placeUpper(level, room, 3, 7, 1, Blocks.CHEST);
                placeUpper(level, room, 0, 9, 1, Blocks.LECTERN);
            }
            case "bakery" -> {
                placeUpperBed(level, room, -2, 7, bed("orange_bed"));
                placeUpper(level, room, -3, 4, 1, Blocks.BARREL);
                placeUpper(level, room, 3, 7, 1, Blocks.CHEST);
                placeUpper(level, room, 0, 9, 1, Blocks.CRAFTING_TABLE);
            }
            case "stable" -> {
                for (int depth : new int[]{3, 5, 7, 9}) {
                    placeUpper(level, room, -3, depth, 1, Blocks.HAY_BLOCK);
                    placeUpper(level, room, 3, depth, 1, Blocks.HAY_BLOCK);
                }
                placeUpper(level, room, 0, 9, 1, Blocks.BARREL);
            }
            case "guard_post" -> {
                placeUpperBed(level, room, -2, 3, bed("gray_bed"));
                placeUpperBed(level, room, -2, 7, bed("gray_bed"));
                placeUpper(level, room, 3, 7, 1, Blocks.CHEST);
                placeUpper(level, room, 0, 9, 1, Blocks.SMITHING_TABLE);
            }
            case "bathhouse" -> {
                for (int depth : new int[]{3, 5, 7}) {
                    placeUpper(level, room, -3, depth, 1, Blocks.SMOOTH_STONE_SLAB);
                    placeUpper(level, room, 3, depth, 1, Blocks.SMOOTH_STONE_SLAB);
                }
                placeUpper(level, room, -2, 9, 1, Blocks.BARREL);
                placeUpper(level, room, 2, 9, 1, Blocks.BARREL);
            }
            case "warehouse" -> {
                for (int depth : new int[]{3, 5, 7, 9}) {
                    placeUpper(level, room, -3, depth, 1, Blocks.BARREL);
                    placeUpper(level, room, 3, depth, 1, Blocks.BARREL);
                }
                placeUpper(level, room, 0, 9, 1, Blocks.CHEST);
            }
            default -> throw new IllegalStateException("Unknown upper-floor role " + room.role);
        }
        Point light = room.point(0, 5);
        set(level, light.x, room.floorY + UPPER_FLOOR_OFFSET + 3, light.z,
                Blocks.LANTERN.defaultBlockState());
    }

    private static void ensureCitizenAssignments(
            ServerLevel level,
            ErdenUrbanLifeSavedData life,
            List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances) {
        if (life.assignments().size() == EXPECTED_CITIZEN_ASSIGNMENTS) {
            logAssignmentsOnce(life.assignments());
            return;
        }
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(level, "erden_kingdom");
        if (site == null) return;

        Set<Long> usedHomes = new HashSet<>();
        Set<Long> usedWorkplaces = new HashSet<>();
        List<ErdenUrbanLifeSavedData.Assignment> assignments = new ArrayList<>();
        for (CitizenPlan citizen : CITIZENS) {
            int anchorX = site.centerX() + citizen.anchorOffsetX;
            int anchorZ = site.centerZ() + citizen.anchorOffsetZ;
            ExternalUrbanFabricBuilder.UrbanEntrance workplace = nearestEntrance(
                    entrances, citizen.workRole, anchorX, anchorZ, usedWorkplaces);
            if (workplace == null) {
                throw new IllegalStateException(
                        "No Erden workplace available for " + citizen.id + " role=" + citizen.workRole);
            }
            usedWorkplaces.add(entranceKey(workplace.x(), workplace.z()));
            ExternalUrbanFabricBuilder.UrbanEntrance home = nearestEntrance(
                    entrances, "tenement", workplace.x(), workplace.z(), usedHomes);
            if (home == null) {
                throw new IllegalStateException("No Erden home available for " + citizen.id);
            }
            usedHomes.add(entranceKey(home.x(), home.z()));
            assignments.add(new ErdenUrbanLifeSavedData.Assignment(
                    citizen.id, citizen.name,
                    home.x(), home.z(),
                    workplace.x(), workplace.z(), citizen.workRole));
        }
        life.replaceAssignments(List.copyOf(assignments));
        logAssignmentsOnce(assignments);
    }

    private static ExternalUrbanFabricBuilder.UrbanEntrance nearestEntrance(
            List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances,
            String role,
            int x,
            int z,
            Set<Long> excluded) {
        return entrances.stream()
                .filter(entrance -> entrance.role().equals(role))
                .filter(entrance -> !excluded.contains(entranceKey(entrance.x(), entrance.z())))
                .min(Comparator.comparingLong(entrance -> distanceSquared(
                        x, z, entrance.x(), entrance.z())))
                .orElse(null);
    }

    private static void logAssignmentsOnce(List<ErdenUrbanLifeSavedData.Assignment> assignments) {
        if (assignmentsLogged) return;
        if (assignments.size() != EXPECTED_CITIZEN_ASSIGNMENTS) {
            throw new IllegalStateException(
                    "Expected " + EXPECTED_CITIZEN_ASSIGNMENTS
                            + " Erden citizen assignments, found " + assignments.size());
        }
        assignmentsLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden citizen home-work assignments citizens={} unique_homes=true unique_workplaces=true routines=home,work authored_target_priority=true",
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
            ErdenUrbanLifeSavedData.Assignment assignment =
                    byName.get(villager.getName().getString());
            Target target = resolveTarget(level, life, assignment, working);
            if (target == null) continue;
            villager.setPersistenceRequired();
            if (villager.distanceToSqr(
                    target.x + 0.5D, target.y, target.z + 0.5D) > 4.0D) {
                villager.getNavigation().moveTo(
                        target.x + 0.5D, target.y, target.z + 0.5D, 0.62D);
            }
        }
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

    private static Target resolveTarget(
            ServerLevel level,
            ErdenUrbanLifeSavedData life,
            ErdenUrbanLifeSavedData.Assignment assignment,
            boolean workplace) {
        int x = workplace ? assignment.workX() : assignment.homeX();
        int z = workplace ? assignment.workZ() : assignment.homeZ();
        ExternalUrbanFabricBuilder.UrbanEntrance entrance = findEntrance(x, z);
        if (entrance == null) return null;

        // A completed authored route always wins. This is also the non-destructive migration path:
        // old synthetic blocks may remain in a legacy save, while citizen destinations immediately
        // switch to the real source upper room once its route verifies.
        BlockPos authoredTarget =
                ErdenUrbanAuthoredUpperRouteManager.verifiedUpperTarget(level, entrance);
        if (authoredTarget != null) {
            return new Target(authoredTarget.getX(), authoredTarget.getY(), authoredTarget.getZ());
        }

        long key = entranceKey(x, z);
        if (!life.isUpperFloorComplete(key, UPPER_FLOOR_REVISION)) return null;
        int doorY = findLowestDoorY(level, x, z);
        if (doorY == Integer.MIN_VALUE) return null;
        Room room = room(entrance, doorY - 1);
        Point point = workplace ? room.point(0, 3) : room.point(0, 7);
        int y = workplace
                ? room.floorY + 1
                : room.floorY + UPPER_FLOOR_OFFSET + 1;
        return new Target(point.x, y, point.z);
    }

    private static ExternalUrbanFabricBuilder.UrbanEntrance findEntrance(int x, int z) {
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance
                : ExternalUrbanFabricBuilder.entrances()) {
            if (entrance.x() == x && entrance.z() == z) return entrance;
        }
        return null;
    }

    private static void verifyCiIfReady(
            ServerLevel level,
            ErdenUrbanLifeSavedData life) {
        if (!"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        verifySyntheticCiIfReady(level, life);
        verifyAuthoredRoutineCiIfReady(level, life);
    }

    private static void verifySyntheticCiIfReady(
            ServerLevel level,
            ErdenUrbanLifeSavedData life) {
        if (ciPassed) return;
        ExternalUrbanFabricBuilder.UrbanEntrance sample = syntheticCiSample();
        if (sample == null) return;
        if (!life.isUpperFloorComplete(
                entranceKey(sample.x(), sample.z()), UPPER_FLOOR_REVISION)) return;
        if (life.assignments().size() != EXPECTED_CITIZEN_ASSIGNMENTS) return;
        int doorY = findLowestDoorY(level, sample.x(), sample.z());
        if (doorY == Integer.MIN_VALUE) return;
        verifyUpperFloor(level, room(sample, doorY - 1));
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_URBAN_LIFE_DIAGNOSTIC_PASS upper_floor=true stairs=true upper_role={} assignments={} routines=true synthetic_fallback=true",
                sample.role(), life.assignments().size());
    }

    private static void verifyAuthoredRoutineCiIfReady(
            ServerLevel level,
            ErdenUrbanLifeSavedData life) {
        if (authoredCiPassed
                || life.assignments().size() != EXPECTED_CITIZEN_ASSIGNMENTS) return;
        for (ErdenUrbanLifeSavedData.Assignment assignment : life.assignments()) {
            ExternalUrbanFabricBuilder.UrbanEntrance workplace =
                    findEntrance(assignment.workX(), assignment.workZ());
            if (workplace == null
                    || !ErdenUrbanAuthoredUpperRouteManager.isEligible(workplace)
                    || !ErdenUrbanAuthoredUpperRouteManager.isCompleted(level, workplace)) {
                continue;
            }
            BlockPos authored =
                    ErdenUrbanAuthoredUpperRouteManager.verifiedUpperTarget(level, workplace);
            if (authored == null) continue;

            ErdenUrbanAuthoredUpperRouteManager.verifyOrThrow(level, workplace);
            Target resolved = resolveTarget(level, life, assignment, true);
            if (resolved == null
                    || resolved.x != authored.getX()
                    || resolved.y != authored.getY()
                    || resolved.z != authored.getZ()) {
                throw new IllegalStateException(
                        "Erden authored resident routine did not resolve to verified upper target citizen="
                                + assignment.citizenName());
            }
            if (life.isUpperFloorComplete(
                    entranceKey(workplace.x(), workplace.z()), UPPER_FLOOR_REVISION)) {
                throw new IllegalStateException(
                        "Fresh Erden authored-route workplace was falsely marked synthetic-complete role="
                                + workplace.role());
            }
            verifyRuntimeTargetGeometry(level, authored);
            authoredCiPassed = true;
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_URBAN_AUTHORED_HOME_PASS citizen={} role={} authored_upper=true synthetic_upper_required=false route_verified=true resident_target_verified=true runtime_geometry_verified=true source_blocks_cut=0 routine=work",
                    assignment.citizenName(), workplace.role());
            return;
        }
    }

    private static void verifyRuntimeTargetGeometry(ServerLevel level, BlockPos target) {
        BlockState feet = level.getBlockState(target);
        BlockState head = level.getBlockState(target.above());
        BlockState floor = level.getBlockState(target.below());
        if (!feet.isAir() || !head.isAir()
                || floor.isAir() || !floor.getFluidState().isEmpty()) {
            throw new IllegalStateException(
                    "Erden authored resident target lacks real walkable world geometry target=" + target);
        }
    }

    private static void verifyUpperFloor(ServerLevel level, Room room) {
        int stairCount = 0;
        for (int step = 0; step < 4; step++) {
            Point point = room.point(2, 2 + step);
            if (level.getBlockState(new BlockPos(
                    point.x, room.floorY + 1 + step, point.z)).getBlock()
                    == Blocks.OAK_STAIRS) {
                stairCount++;
            }
        }
        if (stairCount != 4) {
            throw new IllegalStateException(
                    "Erden upper floor has incomplete stairs role=" + room.role
                            + " steps=" + stairCount);
        }
        Point landing = room.point(2, 6);
        if (level.getBlockState(new BlockPos(
                landing.x, room.floorY + UPPER_FLOOR_OFFSET, landing.z)).isAir()) {
            throw new IllegalStateException("Erden upper-floor landing is missing role=" + room.role);
        }
        if (!containsUpperFixture(level, room)) {
            throw new IllegalStateException("Erden upper floor has no role fixture role=" + room.role);
        }
        Point aisle = room.point(0, 5);
        if (!level.getBlockState(new BlockPos(
                aisle.x, room.floorY + UPPER_FLOOR_OFFSET + 1, aisle.z)).isAir()) {
            throw new IllegalStateException("Erden upper-floor aisle is obstructed role=" + room.role);
        }
    }

    private static boolean containsUpperFixture(ServerLevel level, Room room) {
        int baseY = room.floorY + UPPER_FLOOR_OFFSET;
        for (int depth = 1; depth <= DEPTH; depth++) {
            for (int lateral = -HALF_WIDTH; lateral <= HALF_WIDTH; lateral++) {
                Point point = room.point(lateral, depth);
                for (int y = 1; y <= 2; y++) {
                    Block block = level.getBlockState(new BlockPos(
                            point.x, baseY + y, point.z)).getBlock();
                    if (switch (room.role) {
                        case "tenement", "inn", "shop", "bakery", "guard_post" ->
                                block instanceof BedBlock;
                        case "stable" -> block == Blocks.HAY_BLOCK;
                        case "bathhouse" -> block == Blocks.BARREL;
                        case "warehouse" -> block == Blocks.BARREL || block == Blocks.CHEST;
                        default -> false;
                    }) return true;
                }
            }
        }
        return false;
    }

    private static Room room(
            ExternalUrbanFabricBuilder.UrbanEntrance entrance,
            int floorY) {
        int deltaX = entrance.roadX() - entrance.x();
        int deltaZ = entrance.roadZ() - entrance.z();
        int inwardX;
        int inwardZ;
        Direction inwardDirection;
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            inwardX = deltaX >= 0 ? -1 : 1;
            inwardZ = 0;
            inwardDirection = inwardX > 0 ? Direction.EAST : Direction.WEST;
        } else {
            inwardX = 0;
            inwardZ = deltaZ >= 0 ? -1 : 1;
            inwardDirection = inwardZ > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return new Room(
                entrance.role(), floorY,
                entrance.x(), entrance.z(),
                inwardX, inwardZ,
                -inwardZ, inwardX,
                inwardDirection);
    }

    private static boolean chunksReady(ServerLevel level, Bounds bounds) {
        for (int chunkX = Math.floorDiv(bounds.minX, 16);
             chunkX <= Math.floorDiv(bounds.maxX, 16); chunkX++) {
            for (int chunkZ = Math.floorDiv(bounds.minZ, 16);
                 chunkZ <= Math.floorDiv(bounds.maxZ, 16); chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)
                        || !ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int findLowestDoorY(ServerLevel level, int x, int z) {
        int designed = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
        int minimum = Math.max(level.getMinY(), designed - 8);
        int maximum = Math.min(level.getMaxY() - 1, designed + 64);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = minimum; y <= maximum; y++) {
            cursor.set(x, y, z);
            if (level.getBlockState(cursor).getBlock() instanceof DoorBlock) return y;
        }
        return Integer.MIN_VALUE;
    }

    private static BedBlock bed(String path) {
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath("minecraft", path));
        if (!(block instanceof BedBlock bed)) {
            throw new IllegalStateException("Missing Minecraft bed block minecraft:" + path);
        }
        return bed;
    }

    private static void placeUpperBed(
            ServerLevel level,
            Room room,
            int lateral,
            int depth,
            BedBlock bed) {
        Point foot = room.point(lateral, depth);
        Point head = room.point(lateral, depth + 1);
        int y = room.floorY + UPPER_FLOOR_OFFSET + 1;
        BlockState footState = bed.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.FOOT)
                .setValue(HorizontalDirectionalBlock.FACING, room.inwardDirection);
        BlockState headState = bed.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.HEAD)
                .setValue(HorizontalDirectionalBlock.FACING, room.inwardDirection);
        set(level, foot.x, y, foot.z, footState);
        set(level, head.x, y, head.z, headState);
    }

    private static void placeUpper(
            ServerLevel level,
            Room room,
            int lateral,
            int depth,
            int yOffset,
            Block block) {
        Point point = room.point(lateral, depth);
        set(level, point.x,
                room.floorY + UPPER_FLOOR_OFFSET + yOffset,
                point.z, block.defaultBlockState());
    }

    private static void set(
            ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlockAndUpdate(new BlockPos(x, y, z), state);
    }

    private static boolean isStairwell(int lateral, int depth) {
        return lateral == 2 && depth >= 2 && depth <= 5;
    }

    private static long entranceKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static long distanceSquared(int x1, int z1, int x2, int z2) {
        long dx = x1 - (long) x2;
        long dz = z1 - (long) z2;
        return dx * dx + dz * dz;
    }

    private record CitizenPlan(
            String id,
            String name,
            int anchorOffsetX,
            int anchorOffsetZ,
            String workRole) {
    }

    private record Target(int x, int y, int z) {
    }

    private record Room(
            String role,
            int floorY,
            int doorX,
            int doorZ,
            int inwardX,
            int inwardZ,
            int rightX,
            int rightZ,
            Direction inwardDirection) {
        Point point(int lateral, int forward) {
            return new Point(
                    doorX + inwardX * forward + rightX * lateral,
                    doorZ + inwardZ * forward + rightZ * lateral);
        }

        Bounds bounds() {
            Point a = point(-HALF_WIDTH, 1);
            Point b = point(HALF_WIDTH, 1);
            Point c = point(-HALF_WIDTH, DEPTH);
            Point d = point(HALF_WIDTH, DEPTH);
            return new Bounds(
                    Math.min(doorX, Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x))),
                    Math.max(doorX, Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x))),
                    Math.min(doorZ, Math.min(Math.min(a.z, b.z), Math.min(c.z, d.z))),
                    Math.max(doorZ, Math.max(Math.max(a.z, b.z), Math.max(c.z, d.z))));
        }
    }

    private record Point(int x, int z) {
    }

    private record Bounds(int minX, int maxX, int minZ, int maxZ) {
    }
}
