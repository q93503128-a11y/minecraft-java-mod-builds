package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic household-scale population simulation for Erden's streamed capital. Every small
 * tenement owns one household, every adult has a unique workplace and shift, and only residents
 * whose home chunks are loaded are materialised as villagers. Aggregate production and household
 * consumption continue through saved daily civic ledgers without loading the entire city.
 */
public final class ErdenPopulationManager {
    public static final int POPULATION_REVISION = 1;
    public static final int EXPECTED_HOUSEHOLDS = 77;
    public static final int EXPECTED_RESIDENTS = 231;
    public static final int EXPECTED_WORKERS = 154;
    public static final int EXPECTED_DEPENDENTS = 77;
    public static final int EXPECTED_VACANCIES = 2;

    private static final Identifier VILLAGER_ID =
            Identifier.fromNamespaceAndPath("minecraft", "villager");
    private static final int MEMBERS_PER_HOUSEHOLD = 3;
    private static final int SPAWN_INTERVAL = 20;
    private static final int SPAWN_BUDGET = 2;
    private static final int ROUTINE_INTERVAL = 40;
    private static final int MAX_CATCH_UP_DAYS = 30;

    private static final List<String> FAMILY_PREFIXES = List.of(
            "로", "아르", "벨", "카", "세르", "미르", "테", "도르", "엘"
    );
    private static final List<String> FAMILY_SUFFIXES = List.of(
            "벤", "딘", "라", "렌", "몬", "실", "하임", "바르", "노"
    );
    private static final List<String> GIVEN_NAMES = List.of(
            "카엘", "리안", "마라", "세린", "토르", "엘린",
            "로웬", "베라", "니엘", "다린", "오르", "메이아",
            "라스", "헤나", "유렌", "사엘", "브란", "이리스"
    );

    private static MinecraftServer activeServer;
    private static boolean planLogged;
    private static boolean ciChunksRequested;
    private static boolean ciPassed;

    private ErdenPopulationManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances =
                ExternalUrbanFabricBuilder.entrances();
        ErdenPopulationSavedData population = level.getDataStorage()
                .computeIfAbsent(ErdenPopulationSavedData.TYPE);
        ensurePopulation(population, entrances);
        logPlanOnce(population, entrances);
        requestCiChunks(level, population);
        ErdenCapitalLifecycleManager.prepare(level, population);
        processDailyEconomy(level, population);
        ensureLoadedResidents(level, population);
        runResidentRoutines(level, population);
        verifyCiIfReady(level, population);
    }

    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof Villager villager)
                || !(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)) {
            return;
        }
        ResidentRef reference = findResidentByName(
                level.getDataStorage().computeIfAbsent(ErdenPopulationSavedData.TYPE),
                villager.getName().getString());
        if (reference == null) return;

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        ErdenPopulationSavedData.Resident resident = reference.resident;
        String message;
        if (resident.worker()) {
            message = reference.household.familyName() + " 가구의 구성원입니다. "
                    + roleName(resident.workRole()) + "에서 "
                    + shiftName(resident.shiftStart(), resident.shiftEnd())
                    + " 근무를 맡고 있습니다.";
        } else if (resident.lifeStage().equals("child")) {
            message = reference.household.familyName()
                    + " 가구의 아이입니다. 집 근처에서 심부름과 기초 일을 배우고 있습니다.";
        } else {
            message = reference.household.familyName()
                    + " 가구의 어른입니다. 집안 물품과 가족의 생활을 돌보고 있습니다.";
        }
        player.sendSystemMessage(Component.literal(
                "§6[" + resident.name() + "] §f" + message));
    }

    public static void markDeadIfResident(ServerLevel level, Villager villager) {
        if (!level.dimension().equals(StarterRealmManager.REALM_KEY)) return;
        ErdenPopulationSavedData population = level.getDataStorage()
                .computeIfAbsent(ErdenPopulationSavedData.TYPE);
        ResidentRef reference = findResidentByName(population, villager.getName().getString());
        if (reference == null || population.isDead(reference.resident.id())) return;
        population.markDead(reference.resident.id());
        LivingKingdoms.LOGGER.info(
                "Erden resident {} of household {} died and was removed from production and consumption",
                reference.resident.id(), reference.household.id());
    }

    public static int householdCount(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenPopulationSavedData.TYPE)
                .households().size();
    }

    public static int aliveResidentCount(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenPopulationSavedData.TYPE)
                .aliveResidentCount();
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        planLogged = false;
        ciChunksRequested = false;
        ciPassed = false;
    }

    private static void ensurePopulation(
            ErdenPopulationSavedData population,
            List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances) {
        if (population.hasPopulation(POPULATION_REVISION, EXPECTED_HOUSEHOLDS)) return;
        List<ExternalUrbanFabricBuilder.UrbanEntrance> homes = entrances.stream()
                .filter(entrance -> entrance.role().equals("tenement"))
                .sorted(Comparator.comparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::z)
                        .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::x))
                .toList();
        List<ExternalUrbanFabricBuilder.UrbanEntrance> availableWorkplaces =
                new ArrayList<>(entrances.stream()
                        .filter(entrance -> !entrance.role().equals("tenement"))
                        .sorted(Comparator.comparing(ExternalUrbanFabricBuilder.UrbanEntrance::role)
                                .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::z)
                                .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::x))
                        .toList());
        if (homes.size() != EXPECTED_HOUSEHOLDS) {
            throw new IllegalStateException(
                    "Expected " + EXPECTED_HOUSEHOLDS + " Erden household homes, found " + homes.size());
        }
        if (availableWorkplaces.size() < EXPECTED_WORKERS) {
            throw new IllegalStateException(
                    "Erden has only " + availableWorkplaces.size()
                            + " workplaces for " + EXPECTED_WORKERS + " household workers");
        }

        List<ErdenPopulationSavedData.Household> households = new ArrayList<>();
        for (int householdIndex = 0; householdIndex < homes.size(); householdIndex++) {
            ExternalUrbanFabricBuilder.UrbanEntrance home = homes.get(householdIndex);
            String familyName = familyName(householdIndex);
            List<ErdenPopulationSavedData.Resident> residents = new ArrayList<>();
            for (int memberIndex = 0; memberIndex < MEMBERS_PER_HOUSEHOLD; memberIndex++) {
                String id = "erden_household_%03d_member_%d".formatted(
                        householdIndex + 1, memberIndex + 1);
                String name = familyName + " " + GIVEN_NAMES.get(
                        Math.floorMod(householdIndex * MEMBERS_PER_HOUSEHOLD + memberIndex,
                                GIVEN_NAMES.size()));
                if (memberIndex < 2) {
                    ExternalUrbanFabricBuilder.UrbanEntrance workplace = nearestWorkplace(
                            availableWorkplaces, home.x(), home.z());
                    availableWorkplaces.remove(workplace);
                    Shift shift = shiftFor(workplace.role(), memberIndex);
                    residents.add(new ErdenPopulationSavedData.Resident(
                            id, name,
                            memberIndex == 0 ? "adult_head" : "adult_partner",
                            memberIndex,
                            workplace.x(), workplace.z(), workplace.role(),
                            shift.start, shift.end));
                } else {
                    residents.add(new ErdenPopulationSavedData.Resident(
                            id, name,
                            (householdIndex & 1) == 0 ? "child" : "elder",
                            memberIndex,
                            home.x(), home.z(), "", 0, 0));
                }
            }
            households.add(new ErdenPopulationSavedData.Household(
                    "erden_household_%03d".formatted(householdIndex + 1),
                    familyName, home.x(), home.z(), residents));
        }
        population.replacePopulation(POPULATION_REVISION, List.copyOf(households));
    }

    private static ExternalUrbanFabricBuilder.UrbanEntrance nearestWorkplace(
            List<ExternalUrbanFabricBuilder.UrbanEntrance> available,
            int homeX,
            int homeZ) {
        return available.stream()
                .min(Comparator.<ExternalUrbanFabricBuilder.UrbanEntrance>comparingLong(entrance -> distanceSquared(
                                homeX, homeZ, entrance.x(), entrance.z()))
                        .thenComparing(ExternalUrbanFabricBuilder.UrbanEntrance::role)
                        .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::z)
                        .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::x))
                .orElseThrow(() -> new IllegalStateException("No Erden workplace remains"));
    }

    private static void logPlanOnce(
            ErdenPopulationSavedData population,
            List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances) {
        if (planLogged) return;
        int households = population.households().size();
        int residents = 0;
        int workers = 0;
        int dependents = 0;
        Set<Long> homes = new HashSet<>();
        Set<Long> workplaces = new HashSet<>();
        for (ErdenPopulationSavedData.Household household : population.households()) {
            homes.add(positionKey(household.homeX(), household.homeZ()));
            residents += household.residents().size();
            for (ErdenPopulationSavedData.Resident resident : household.residents()) {
                if (resident.worker()) {
                    workers++;
                    workplaces.add(positionKey(resident.workX(), resident.workZ()));
                } else {
                    dependents++;
                }
            }
        }
        int availableWorkplaces = (int) entrances.stream()
                .filter(entrance -> !entrance.role().equals("tenement"))
                .count();
        int vacancies = availableWorkplaces - workers;
        if (households != EXPECTED_HOUSEHOLDS
                || residents != EXPECTED_RESIDENTS
                || workers != EXPECTED_WORKERS
                || dependents != EXPECTED_DEPENDENTS
                || homes.size() != EXPECTED_HOUSEHOLDS
                || workplaces.size() != EXPECTED_WORKERS
                || vacancies != EXPECTED_VACANCIES) {
            throw new IllegalStateException(
                    "Invalid Erden population plan households=" + households
                            + " residents=" + residents
                            + " workers=" + workers
                            + " dependents=" + dependents
                            + " unique_homes=" + homes.size()
                            + " unique_workplaces=" + workplaces.size()
                            + " vacancies=" + vacancies);
        }
        planLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden household population households={} residents={} workers={} dependents={} owned_homes={} assigned_workplaces={} vacancies={} shifts=early,late,night",
                households, residents, workers, dependents,
                homes.size(), workplaces.size(), vacancies);
    }

    private static void processDailyEconomy(
            ServerLevel level,
            ErdenPopulationSavedData population) {
        long currentDay = Math.floorDiv(level.getGameTime(), 24_000L);
        long previousDay = population.lastProcessedDay();
        if (previousDay >= currentDay) return;
        long firstDay = previousDay < 0L
                ? currentDay
                : Math.max(previousDay + 1L, currentDay - MAX_CATCH_UP_DAYS + 1L);
        for (long day = firstDay; day <= currentDay; day++) {
            Map<String, Long> production = new LinkedHashMap<>();
            Map<String, Long> consumption = new LinkedHashMap<>();
            List<ErdenCapitalLifecycleManager.WorkerSnapshot> activeWorkers =
                    ErdenCapitalLifecycleManager.activeWorkers(level, population, day);
            int livingHouseholds = ErdenCapitalLifecycleManager.livingHouseholdCount(level, population, day);
            int livingResidents = ErdenCapitalLifecycleManager.livingCount(level, population, day);
            for (ErdenCapitalLifecycleManager.WorkerSnapshot worker : activeWorkers) {
                addProduction(production, worker.workRole());
            }
            add(consumption, "food", livingResidents);
            add(consumption, "goods", livingHouseholds);
            add(consumption, "sanitation", livingHouseholds);
            add(consumption, "security", livingHouseholds);
            add(consumption, "logistics", livingHouseholds);
            add(consumption, "transport", (livingHouseholds + 1L) / 2L);

            if (population.applyDay(day, production, consumption)
                    && (previousDay < 0L || day % 7L == 0L)) {
                LivingKingdoms.LOGGER.info(
                        "Processed Erden household economy day={} alive_residents={} alive_workers={} stocks={} cumulative_shortage={}",
                        day, livingResidents, activeWorkers.size(),
                        population.stocks(), population.totalShortage());
            }
        }
    }

    private static void addProduction(Map<String, Long> production, String role) {
        switch (role) {
            case "bakery" -> add(production, "food", 8L);
            case "inn" -> {
                add(production, "food", 3L);
                add(production, "lodging", 4L);
            }
            case "shop" -> add(production, "goods", 5L);
            case "stable" -> add(production, "transport", 6L);
            case "guard_post" -> add(production, "security", 8L);
            case "bathhouse" -> add(production, "sanitation", 9L);
            case "warehouse" -> add(production, "logistics", 7L);
            default -> throw new IllegalStateException("Unknown Erden production role " + role);
        }
    }

    private static void add(Map<String, Long> values, String resource, long amount) {
        values.merge(resource, amount, Long::sum);
    }

    private static void ensureLoadedResidents(
            ServerLevel level,
            ErdenPopulationSavedData population) {
        if (level.getGameTime() % SPAWN_INTERVAL != 0L) return;
        Map<String, Villager> existing = loadedResidentsByName(level, population);
        ErdenUrbanLifeSavedData urbanLife = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanLifeSavedData.TYPE);
        int spawned = 0;
        for (ErdenPopulationSavedData.Household household : population.households()) {
            if (spawned >= SPAWN_BUDGET) break;
            if (!level.hasChunk(household.homeX() >> 4, household.homeZ() >> 4)) continue;
            long homeKey = positionKey(household.homeX(), household.homeZ());
            if (!urbanLife.isUpperFloorComplete(
                    homeKey, ErdenUrbanLifeManager.UPPER_FLOOR_REVISION)) continue;
            ExternalUrbanFabricBuilder.UrbanEntrance entrance = findEntrance(
                    household.homeX(), household.homeZ());
            if (entrance == null) continue;
            int doorY = findLowestDoorY(level, household.homeX(), household.homeZ());
            if (doorY == Integer.MIN_VALUE) continue;
            Room room = room(entrance, doorY - 1);
            for (ErdenPopulationSavedData.Resident resident : household.residents()) {
                if (spawned >= SPAWN_BUDGET) break;
                if (population.isDead(resident.id()) || existing.containsKey(resident.name())) continue;
                if (spawnResident(level, room, resident)) {
                    spawned++;
                }
            }
        }
    }

    private static boolean spawnResident(
            ServerLevel level,
            Room home,
            ErdenPopulationSavedData.Resident resident) {
        EntityType<?> villagerType = BuiltInRegistries.ENTITY_TYPE
                .getOptional(VILLAGER_ID).orElse(null);
        if (villagerType == null) {
            LivingKingdoms.LOGGER.error("Minecraft villager entity type is unavailable");
            return false;
        }
        Entity created = villagerType.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Villager villager)) return false;
        Point point = home.point(resident.bedSlot() - 1, 4 + resident.bedSlot());
        int preferredY = home.floorY + 6;
        int standingY = safeStandingY(level, point.x, preferredY, point.z);
        villager.setPos(point.x + 0.5D, standingY, point.z + 0.5D);
        villager.setCustomName(Component.literal(resident.name()));
        villager.setCustomNameVisible(false);
        villager.setPersistenceRequired();
        villager.setInvulnerable(false);
        if (!level.addFreshEntity(villager)) {
            LivingKingdoms.LOGGER.error("Failed to spawn Erden resident {}", resident.id());
            return false;
        }
        return true;
    }

    private static void runResidentRoutines(
            ServerLevel level,
            ErdenPopulationSavedData population) {
        if (level.getGameTime() % ROUTINE_INTERVAL != 0L) return;
        Map<String, ResidentRef> residents = residentReferences(population);
        if (residents.isEmpty()) return;
        AABB capital = capitalBounds(level);
        List<Villager> villagers = level.getEntitiesOfClass(
                Villager.class, capital,
                villager -> residents.containsKey(villager.getName().getString()));
        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        for (Villager villager : villagers) {
            ResidentRef reference = residents.get(villager.getName().getString());
            if (reference == null || population.isDead(reference.resident.id())) continue;
            boolean working = reference.resident.worker()
                    && inShift(dayTime,
                    reference.resident.shiftStart(), reference.resident.shiftEnd());
            Target target = resolveTarget(level, reference, working);
            if (target == null) continue;
            villager.setPersistenceRequired();
            if (villager.distanceToSqr(
                    target.x + 0.5D, target.y, target.z + 0.5D) > 4.0D) {
                villager.getNavigation().moveTo(
                        target.x + 0.5D, target.y, target.z + 0.5D, 0.60D);
            }
        }
    }

    private static Target resolveTarget(
            ServerLevel level,
            ResidentRef reference,
            boolean workplace) {
        int x = workplace ? reference.resident.workX() : reference.household.homeX();
        int z = workplace ? reference.resident.workZ() : reference.household.homeZ();
        if (!level.hasChunk(x >> 4, z >> 4)) return null;
        ExternalUrbanFabricBuilder.UrbanEntrance entrance = findEntrance(x, z);
        if (entrance == null) return null;
        long key = positionKey(x, z);
        if (workplace) {
            ErdenUrbanInteriorSavedData interiors = level.getDataStorage()
                    .computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE);
            if (!interiors.isComplete(key, ErdenUrbanInteriorBuilder.INTERIOR_REVISION)) return null;
        } else {
            ErdenUrbanLifeSavedData urbanLife = level.getDataStorage()
                    .computeIfAbsent(ErdenUrbanLifeSavedData.TYPE);
            if (!urbanLife.isUpperFloorComplete(
                    key, ErdenUrbanLifeManager.UPPER_FLOOR_REVISION)) return null;
        }
        int doorY = findLowestDoorY(level, x, z);
        if (doorY == Integer.MIN_VALUE) return null;
        Room room = room(entrance, doorY - 1);
        Point point = workplace
                ? room.point(0, 3)
                : room.point(0, 4 + reference.resident.bedSlot());
        return new Target(
                point.x,
                workplace ? room.floorY + 1 : room.floorY + 6,
                point.z);
    }

    private static void requestCiChunks(
            ServerLevel level,
            ErdenPopulationSavedData population) {
        if (ciChunksRequested
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || population.households().isEmpty()) return;
        ErdenPopulationSavedData.Household sample = population.households().getFirst();
        requestBuildingChunks(level, sample.homeX(), sample.homeZ());
        for (ErdenPopulationSavedData.Resident resident : sample.residents()) {
            if (resident.worker()) requestBuildingChunks(level, resident.workX(), resident.workZ());
        }
        ciChunksRequested = true;
    }

    private static void requestBuildingChunks(ServerLevel level, int x, int z) {
        ExternalUrbanFabricBuilder.UrbanEntrance entrance = findEntrance(x, z);
        if (entrance == null) return;
        Bounds bounds = room(entrance, 0).bounds();
        for (int chunkX = Math.floorDiv(bounds.minX, 16);
             chunkX <= Math.floorDiv(bounds.maxX, 16); chunkX++) {
            for (int chunkZ = Math.floorDiv(bounds.minZ, 16);
                 chunkZ <= Math.floorDiv(bounds.maxZ, 16); chunkZ++) {
                ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
            }
        }
    }

    private static void verifyCiIfReady(
            ServerLevel level,
            ErdenPopulationSavedData population) {
        if (ciPassed
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || population.households().size() != EXPECTED_HOUSEHOLDS
                || population.aliveResidentCount() != EXPECTED_RESIDENTS
                || population.aliveWorkerCount() != EXPECTED_WORKERS
                || population.lastProcessedDay() < 0L
                || population.households().isEmpty()) return;
        ErdenPopulationSavedData.Household sample = population.households().getFirst();
        ErdenUrbanLifeSavedData urbanLife = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanLifeSavedData.TYPE);
        if (!urbanLife.isUpperFloorComplete(
                positionKey(sample.homeX(), sample.homeZ()),
                ErdenUrbanLifeManager.UPPER_FLOOR_REVISION)) return;
        Set<String> sampleNames = new HashSet<>();
        for (ErdenPopulationSavedData.Resident resident : sample.residents()) {
            sampleNames.add(resident.name());
        }
        int spawned = level.getEntitiesOfClass(
                Villager.class, capitalBounds(level),
                villager -> sampleNames.contains(villager.getName().getString())).size();
        if (spawned != MEMBERS_PER_HOUSEHOLD) return;
        if (population.totalShortage() != 0L) {
            throw new IllegalStateException(
                    "Initial Erden household economy has shortages=" + population.totalShortage());
        }
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_POPULATION_DIAGNOSTIC_PASS households={} residents={} workers={} dependents={} spawned_sample={} ledger=true shortages={} shifts=true ownership=true",
                EXPECTED_HOUSEHOLDS, EXPECTED_RESIDENTS, EXPECTED_WORKERS,
                EXPECTED_DEPENDENTS, spawned, population.totalShortage());
    }

    private static Map<String, Villager> loadedResidentsByName(
            ServerLevel level,
            ErdenPopulationSavedData population) {
        Set<String> names = residentReferences(population).keySet();
        Map<String, Villager> result = new HashMap<>();
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class, capitalBounds(level),
                villager -> names.contains(villager.getName().getString()))) {
            result.putIfAbsent(villager.getName().getString(), villager);
        }
        return result;
    }

    private static Map<String, ResidentRef> residentReferences(
            ErdenPopulationSavedData population) {
        Map<String, ResidentRef> result = new HashMap<>();
        for (ErdenPopulationSavedData.Household household : population.households()) {
            for (ErdenPopulationSavedData.Resident resident : household.residents()) {
                result.put(resident.name(), new ResidentRef(household, resident));
            }
        }
        return result;
    }

    private static ResidentRef findResidentByName(
            ErdenPopulationSavedData population,
            String name) {
        for (ErdenPopulationSavedData.Household household : population.households()) {
            for (ErdenPopulationSavedData.Resident resident : household.residents()) {
                if (resident.name().equals(name)) return new ResidentRef(household, resident);
            }
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

    private static ExternalUrbanFabricBuilder.UrbanEntrance findEntrance(int x, int z) {
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance
                : ExternalUrbanFabricBuilder.entrances()) {
            if (entrance.x() == x && entrance.z() == z) return entrance;
        }
        return null;
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

    private static int safeStandingY(
            ServerLevel level, int x, int preferredY, int z) {
        for (int offset = 0; offset <= 8; offset++) {
            int[] candidates = offset == 0
                    ? new int[]{preferredY}
                    : new int[]{preferredY + offset, preferredY - offset};
            for (int standingY : candidates) {
                BlockPos feet = new BlockPos(x, standingY, z);
                if (!level.getBlockState(feet.below()).isAir()
                        && level.getBlockState(feet).isAir()
                        && level.getBlockState(feet.above()).isAir()) {
                    return standingY;
                }
            }
        }
        return preferredY;
    }

    private static Room room(
            ExternalUrbanFabricBuilder.UrbanEntrance entrance,
            int floorY) {
        int deltaX = entrance.roadX() - entrance.x();
        int deltaZ = entrance.roadZ() - entrance.z();
        int inwardX;
        int inwardZ;
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            inwardX = deltaX >= 0 ? -1 : 1;
            inwardZ = 0;
        } else {
            inwardX = 0;
            inwardZ = deltaZ >= 0 ? -1 : 1;
        }
        Direction inwardDirection = inwardX > 0 ? Direction.EAST
                : inwardX < 0 ? Direction.WEST
                : inwardZ > 0 ? Direction.SOUTH : Direction.NORTH;
        return new Room(
                entrance.role(), floorY,
                entrance.x(), entrance.z(),
                inwardX, inwardZ,
                -inwardZ, inwardX,
                inwardDirection);
    }

    private static Shift shiftFor(String role, int workerIndex) {
        return switch (role) {
            case "bakery", "stable", "warehouse" -> workerIndex == 0
                    ? new Shift(1_000, 7_000) : new Shift(5_000, 11_000);
            case "shop", "bathhouse" -> workerIndex == 0
                    ? new Shift(3_000, 9_000) : new Shift(7_000, 13_000);
            case "inn" -> workerIndex == 0
                    ? new Shift(4_000, 10_000) : new Shift(12_000, 2_000);
            case "guard_post" -> workerIndex == 0
                    ? new Shift(2_000, 8_000) : new Shift(10_000, 18_000);
            default -> new Shift(2_000, 8_000);
        };
    }

    private static boolean inShift(long dayTime, int start, int end) {
        if (start <= end) return dayTime >= start && dayTime < end;
        return dayTime >= start || dayTime < end;
    }

    private static String familyName(int householdIndex) {
        int prefixIndex = householdIndex / FAMILY_SUFFIXES.size();
        int suffixIndex = householdIndex % FAMILY_SUFFIXES.size();
        return FAMILY_PREFIXES.get(prefixIndex) + FAMILY_SUFFIXES.get(suffixIndex);
    }

    private static String roleName(String role) {
        return switch (role) {
            case "shop" -> "상점";
            case "bakery" -> "제빵소";
            case "inn" -> "여관";
            case "stable" -> "마구간";
            case "guard_post" -> "경비초소";
            case "bathhouse" -> "목욕시설";
            case "warehouse" -> "창고";
            default -> "작업장";
        };
    }

    private static String shiftName(int start, int end) {
        if (start > end) return "야간 교대";
        if (start < 2_000) return "이른 교대";
        if (start >= 9_000) return "늦은 교대";
        return "주간 교대";
    }

    private static long positionKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static long distanceSquared(int x1, int z1, int x2, int z2) {
        long dx = x1 - (long) x2;
        long dz = z1 - (long) z2;
        return dx * dx + dz * dz;
    }

    private record Shift(int start, int end) {
    }

    private record ResidentRef(
            ErdenPopulationSavedData.Household household,
            ErdenPopulationSavedData.Resident resident) {
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
            Point a = point(-3, 1);
            Point b = point(3, 1);
            Point c = point(-3, 9);
            Point d = point(3, 9);
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
