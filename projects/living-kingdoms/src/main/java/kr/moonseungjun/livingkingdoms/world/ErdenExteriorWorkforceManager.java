package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persistent rural households and attendance-driven production for Erden's farms, ranches, mines,
 * paper mills and wharves. Unloaded sites continue as aggregate residents; loaded sites materialise
 * the same saved people as villagers and never create replacement labour after a permanent death.
 */
public final class ErdenExteriorWorkforceManager {
    public static final int WORKFORCE_REVISION = 1;
    public static final int EXPECTED_NODES = 18;
    public static final int EXPECTED_HOUSEHOLDS = 74;
    public static final int EXPECTED_RESIDENTS = 216;
    public static final int EXPECTED_WORKERS = 142;
    public static final int EXPECTED_DEPENDENTS = 74;
    public static final int EXPECTED_PRODUCER_WORKERS = 124;
    public static final int EXPECTED_WHARF_WORKERS = 18;

    private static final Identifier VILLAGER_ID =
            Identifier.fromNamespaceAndPath("minecraft", "villager");
    private static final int MAX_CATCH_UP_DAYS = 30;
    private static final int SPAWN_INTERVAL = 20;
    private static final int SPAWN_BUDGET = 3;
    private static final int ROUTINE_INTERVAL = 60;
    private static final int[][] HOME_OFFSETS = {
            {0, 0}, {28, 0}, {-28, 0}, {0, 28}, {0, -28}
    };
    private static final List<String> FAMILY_PREFIXES = List.of(
            "아르", "벨", "카르", "델", "에른", "파르", "그렌", "하르", "이르"
    );
    private static final List<String> FAMILY_SUFFIXES = List.of(
            "렌", "바르", "딘", "몬", "실", "하임", "베른", "로", "테르"
    );
    private static final List<String> GIVEN_NAMES = List.of(
            "라엔", "세라", "도란", "미엘", "카인", "로아", "브렌", "티아",
            "오렌", "니아", "가렌", "헤르", "유나", "마엘", "리오", "베인"
    );

    private static MinecraftServer activeServer;
    private static boolean planLogged;
    private static boolean ciPassed;

    private ErdenExteriorWorkforceManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        long currentDay = Math.floorDiv(level.getGameTime(), 24_000L);
        ErdenExteriorWorkforceSavedData workforce = data(level);
        ensurePopulation(workforce);
        processThroughDay(level, workforce, currentDay);
        logPlanOnce(workforce);
        ensureLoadedResidents(level, workforce);
        runRoutines(level, workforce, currentDay);
        verifyCi(level, workforce, currentDay);
    }

    public static void prepareBeforeSupply(ServerLevel level, long currentDay) {
        ErdenExteriorWorkforceSavedData workforce = data(level);
        ensurePopulation(workforce);
        processThroughDay(level, workforce, currentDay);
        logPlanOnce(workforce);
    }

    public static boolean isReady(ServerLevel level, long day) {
        ErdenExteriorWorkforceSavedData workforce = data(level);
        return workforce.hasPopulation(WORKFORCE_REVISION, EXPECTED_HOUSEHOLDS)
                && workforce.householdCount() == EXPECTED_HOUSEHOLDS
                && workforce.residentCount() == EXPECTED_RESIDENTS
                && workforce.workerCount() == EXPECTED_WORKERS
                && workforce.dependentCount() == EXPECTED_DEPENDENTS
                && workforce.lastProcessedDay() >= day
                && workforce.nodeLabor().size() == EXPECTED_NODES;
    }

    public static int productionPercent(ServerLevel level, String nodeId, long day) {
        ErdenExteriorWorkforceSavedData workforce = data(level);
        ensurePopulation(workforce);
        ErdenKingdomSupplyCatalog.SupplyNode node = ErdenKingdomSupplyCatalog.node(nodeId);
        return node == null ? 0 : laborState(level, workforce, node, day).productionPercent();
    }

    public static boolean nodeOperational(ServerLevel level, String nodeId, long day) {
        return productionPercent(level, nodeId, day) >= 50;
    }

    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof Villager villager)
                || !(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)) return;
        ResidentRef reference = findByName(data(level), villager.getName().getString());
        if (reference == null) return;

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        ErdenExteriorWorkforceSavedData.Resident resident = reference.resident();
        String message;
        if (resident.worker()) {
            long day = Math.floorDiv(level.getGameTime(), 24_000L);
            boolean absent = absentOnDay(resident, reference.household().nodeRole(), day);
            message = roleName(reference.household().nodeRole()) + " 소속 "
                    + shiftName(resident.shiftStart(), resident.shiftEnd()) + " 근로자입니다. 오늘은 "
                    + (absent ? "휴무 또는 결근일입니다." : "정상 근무일입니다.");
        } else if (resident.lifeStage().equals("child")) {
            message = roleName(reference.household().nodeRole())
                    + " 정착지에서 가족을 돕고 일을 배우는 아이입니다.";
        } else {
            message = roleName(reference.household().nodeRole())
                    + " 정착지의 살림과 공동체 물품을 돌보는 어른입니다.";
        }
        player.sendSystemMessage(Component.literal(
                "§6[" + resident.name() + "] §f" + message));
    }

    public static void markDeadIfWorker(ServerLevel level, Villager villager) {
        if (!level.dimension().equals(StarterRealmManager.REALM_KEY)) return;
        ErdenExteriorWorkforceSavedData workforce = data(level);
        ResidentRef reference = findByName(workforce, villager.getName().getString());
        if (reference == null || workforce.isDead(reference.resident().id())) return;
        workforce.markDead(reference.resident().id());
        LivingKingdoms.LOGGER.info(
                "Erden exterior resident {} of {} died permanently and no replacement labour was created",
                reference.resident().id(), reference.household().nodeId());
    }

    private static ErdenExteriorWorkforceSavedData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenExteriorWorkforceSavedData.TYPE);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        planLogged = false;
        ciPassed = false;
    }

    private static void ensurePopulation(ErdenExteriorWorkforceSavedData workforce) {
        if (workforce.hasPopulation(WORKFORCE_REVISION, EXPECTED_HOUSEHOLDS)) return;
        List<ErdenExteriorWorkforceSavedData.Household> households = new ArrayList<>();
        int globalHousehold = 0;
        int globalWorker = 0;
        int globalDependent = 0;

        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            int requiredWorkers = requiredWorkers(node.role);
            int householdCount = (requiredWorkers + 1) / 2;
            int assignedWorkers = 0;
            for (int localHousehold = 0; localHousehold < householdCount; localHousehold++) {
                int[] offset = HOME_OFFSETS[localHousehold];
                String householdId = "erden_exterior_household_%03d".formatted(globalHousehold + 1);
                String familyName = familyName(globalHousehold);
                List<ErdenExteriorWorkforceSavedData.Resident> residents = new ArrayList<>();
                for (int adultSlot = 0; adultSlot < 2 && assignedWorkers < requiredWorkers; adultSlot++) {
                    Shift shift = shiftFor(node.role, assignedWorkers);
                    String id = node.id + "_worker_%02d".formatted(assignedWorkers + 1);
                    String name = familyName + " " + GIVEN_NAMES.get(
                            Math.floorMod(globalHousehold * 3 + adultSlot, GIVEN_NAMES.size()));
                    residents.add(new ErdenExteriorWorkforceSavedData.Resident(
                            id, name,
                            adultSlot == 0 ? "adult_head" : "adult_partner",
                            adultSlot,
                            node.role,
                            shift.start(), shift.end(),
                            Math.floorMod(globalWorker, 7)));
                    assignedWorkers++;
                    globalWorker++;
                }
                String dependentId = node.id + "_dependent_%02d".formatted(localHousehold + 1);
                String dependentName = familyName + " " + GIVEN_NAMES.get(
                        Math.floorMod(globalHousehold * 3 + 2, GIVEN_NAMES.size()));
                residents.add(new ErdenExteriorWorkforceSavedData.Resident(
                        dependentId, dependentName,
                        (globalHousehold & 1) == 0 ? "child" : "elder",
                        2, "", 0, 0, -1));
                globalDependent++;
                households.add(new ErdenExteriorWorkforceSavedData.Household(
                        householdId, familyName, node.id, node.role,
                        node.x + offset[0], node.z + offset[1], residents));
                globalHousehold++;
            }
        }
        workforce.replacePopulation(WORKFORCE_REVISION, households);
        validatePlan(workforce);
    }

    private static void validatePlan(ErdenExteriorWorkforceSavedData workforce) {
        int producerWorkers = 0;
        int wharfWorkers = 0;
        Set<String> ids = new HashSet<>();
        Set<Long> homes = new HashSet<>();
        for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {
            homes.add(positionKey(household.homeX(), household.homeZ()));
            for (ErdenExteriorWorkforceSavedData.Resident resident : household.residents()) {
                if (!ids.add(resident.id())) {
                    throw new IllegalStateException("Duplicate Erden exterior resident id " + resident.id());
                }
                if (!resident.worker()) continue;
                if (household.nodeRole().equals("river_wharf")) wharfWorkers++;
                else producerWorkers++;
            }
        }
        if (workforce.householdCount() != EXPECTED_HOUSEHOLDS
                || workforce.residentCount() != EXPECTED_RESIDENTS
                || workforce.workerCount() != EXPECTED_WORKERS
                || workforce.dependentCount() != EXPECTED_DEPENDENTS
                || producerWorkers != EXPECTED_PRODUCER_WORKERS
                || wharfWorkers != EXPECTED_WHARF_WORKERS
                || homes.size() != EXPECTED_HOUSEHOLDS) {
            throw new IllegalStateException(
                    "Invalid Erden exterior workforce households=" + workforce.householdCount()
                            + " residents=" + workforce.residentCount()
                            + " workers=" + workforce.workerCount()
                            + " dependents=" + workforce.dependentCount()
                            + " producer_workers=" + producerWorkers
                            + " wharf_workers=" + wharfWorkers
                            + " unique_homes=" + homes.size());
        }
    }

    private static void processThroughDay(
            ServerLevel level,
            ErdenExteriorWorkforceSavedData workforce,
            long currentDay) {
        long previousDay = workforce.lastProcessedDay();
        if (previousDay >= currentDay) return;
        long firstDay = previousDay < 0L
                ? currentDay
                : Math.max(previousDay + 1L, currentDay - MAX_CATCH_UP_DAYS + 1L);
        for (long day = firstDay; day <= currentDay; day++) {
            List<ErdenExteriorWorkforceSavedData.NodeLabor> states = new ArrayList<>();
            for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
                states.add(laborState(level, workforce, node, day));
            }
            if (workforce.recordDay(day, states)
                    && (previousDay < 0L || day % 7L == 0L || day == currentDay)) {
                LivingKingdoms.LOGGER.info(
                        "Processed Erden exterior workforce day={} alive_workers={} attended={} absent={} dead={} production_linked=true",
                        day, workforce.aliveWorkerCount(), workforce.attendedWorkerCount(),
                        workforce.absentWorkerCount(), workforce.deadWorkerCount());
            }
        }
    }

    private static ErdenExteriorWorkforceSavedData.NodeLabor laborState(
            ServerLevel level,
            ErdenExteriorWorkforceSavedData workforce,
            ErdenKingdomSupplyCatalog.SupplyNode node,
            long day) {
        int required = requiredWorkers(node.role);
        int alive = 0;
        int attended = 0;
        int absent = 0;
        int dead = 0;
        for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {
            if (!household.nodeId().equals(node.id)) continue;
            for (ErdenExteriorWorkforceSavedData.Resident resident : household.residents()) {
                if (!resident.worker()) continue;
                if (workforce.isDead(resident.id())) {
                    dead++;
                    continue;
                }
                if (!ErdenExteriorLifecycleManager.foundingWorkerAvailable(
                        level, resident.id(), day)) continue;
                alive++;
                if (absentOnDay(resident, node.role, day)) absent++;
                else attended++;
            }
        }
        ErdenExteriorLifecycleManager.LaborContribution lifecycleLabor =
                ErdenExteriorLifecycleManager.additionalLabor(level, node.id, node.role, day);
        alive += lifecycleLabor.alive();
        attended += lifecycleLabor.attended();
        absent += lifecycleLabor.absent();
        dead += lifecycleLabor.dead();
        ErdenExteriorLifecycleManager.LaborContribution lifecycleLabor =
                ErdenExteriorLifecycleManager.additionalLabor(level, node.id, node.role, day);
        alive += lifecycleLabor.alive();
        attended += lifecycleLabor.attended();
        absent += lifecycleLabor.absent();
        dead += lifecycleLabor.dead();
        int percent = required <= 0 ? 100
                : Math.clamp(attended * 100 / required, 0, 100);
        return new ErdenExteriorWorkforceSavedData.NodeLabor(
                node.id, day, required, alive, attended, absent, dead,
                percent, 0L, 0L);
    }

    private static boolean absentOnDay(
            ErdenExteriorWorkforceSavedData.Resident resident,
            String nodeRole,
            long day) {
        if (resident.restDay() == Math.floorMod(day, 7L)) return true;
        long seed = (long) resident.id().hashCode() * 31L + day * 97L;
        if (Math.floorMod(seed, 53L) == 0L) return true;
        return (nodeRole.equals("colliery") || nodeRole.equals("iron_mine"))
                && Math.floorMod(seed, 89L) == 0L;
    }

    private static void logPlanOnce(ErdenExteriorWorkforceSavedData workforce) {
        if (planLogged) return;
        validatePlan(workforce);
        planLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden exterior workforce nodes={} households={} residents={} workers={} dependents={} producer_workers={} wharf_workers={} shifts=true permanent_deaths=true",
                EXPECTED_NODES, EXPECTED_HOUSEHOLDS, EXPECTED_RESIDENTS,
                EXPECTED_WORKERS, EXPECTED_DEPENDENTS,
                EXPECTED_PRODUCER_WORKERS, EXPECTED_WHARF_WORKERS);
    }

    private static void ensureLoadedResidents(
            ServerLevel level,
            ErdenExteriorWorkforceSavedData workforce) {
        if (level.getGameTime() % SPAWN_INTERVAL != 0L) return;
        Map<String, Villager> existing = loadedResidentsByName(level, workforce);
        int spawned = 0;
        for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {
            if (spawned >= SPAWN_BUDGET) break;
            ErdenKingdomSupplyCatalog.SupplyNode node = ErdenKingdomSupplyCatalog.node(household.nodeId());
            if (node == null
                    || !ErdenKingdomExteriorBuilder.anchorBuilt(level, node)
                    || !level.hasChunk(household.homeX() >> 4, household.homeZ() >> 4)) continue;
            for (ErdenExteriorWorkforceSavedData.Resident resident : household.residents()) {
                if (spawned >= SPAWN_BUDGET) break;
                if (workforce.isDead(resident.id()) || existing.containsKey(resident.name())) continue;
                if (spawnResident(level, household, resident)) spawned++;
            }
        }
    }

    private static boolean spawnResident(
            ServerLevel level,
            ErdenExteriorWorkforceSavedData.Household household,
            ErdenExteriorWorkforceSavedData.Resident resident) {
        EntityType<?> villagerType = BuiltInRegistries.ENTITY_TYPE
                .getOptional(VILLAGER_ID).orElse(null);
        if (villagerType == null) return false;
        Entity created = villagerType.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Villager villager)) return false;
        int x = household.homeX() + resident.bedSlot() - 1;
        int z = household.homeZ() + 2 + resident.bedSlot();
        int preferredY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z)) + 2;
        int standingY = safeStandingY(level, x, preferredY, z);
        villager.setPos(x + 0.5D, standingY, z + 0.5D);
        villager.setCustomName(Component.literal(resident.name()));
        villager.setCustomNameVisible(false);
        villager.setPersistenceRequired();
        villager.setInvulnerable(false);
        return level.addFreshEntity(villager);
    }

    private static void runRoutines(
            ServerLevel level,
            ErdenExteriorWorkforceSavedData workforce,
            long day) {
        if (level.getGameTime() % ROUTINE_INTERVAL != 0L) return;
        Map<String, ResidentRef> references = references(workforce);
        if (references.isEmpty()) return;
        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class, exteriorBounds(level),
                candidate -> references.containsKey(candidate.getName().getString()))) {
            ResidentRef reference = references.get(villager.getName().getString());
            if (reference == null || workforce.isDead(reference.resident().id())) continue;
            boolean working = reference.resident().worker()
                    && !absentOnDay(reference.resident(), reference.household().nodeRole(), day)
                    && inShift(dayTime, reference.resident().shiftStart(), reference.resident().shiftEnd());
            Target target = target(level, reference, working);
            if (target == null) continue;
            villager.setPersistenceRequired();
            if (villager.distanceToSqr(target.x() + 0.5D, target.y(), target.z() + 0.5D) > 4.0D) {
                villager.getNavigation().moveTo(
                        target.x() + 0.5D, target.y(), target.z() + 0.5D, 0.58D);
            }
        }
    }

    private static Target target(ServerLevel level, ResidentRef reference, boolean workplace) {
        ErdenKingdomSupplyCatalog.SupplyNode node =
                ErdenKingdomSupplyCatalog.node(reference.household().nodeId());
        if (node == null) return null;
        int x = workplace ? node.x : reference.household().homeX();
        int z = workplace ? node.z : reference.household().homeZ();
        if (!level.hasChunk(x >> 4, z >> 4)) return null;
        int preferredY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z)) + 2;
        return new Target(x, safeStandingY(level, x, preferredY, z), z);
    }

    private static void verifyCi(
            ServerLevel level,
            ErdenExteriorWorkforceSavedData workforce,
            long day) {
        if (ciPassed
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || !isReady(level, day)
                || workforce.attendedWorkerCount() <= 0
                || workforce.absentWorkerCount() <= 0) return;
        ErdenKingdomSupplyCatalog.SupplyNode sampleNode = ErdenKingdomSupplyCatalog.nodes().getFirst();
        if (!ErdenKingdomExteriorBuilder.anchorBuilt(level, sampleNode)) return;
        ErdenExteriorWorkforceSavedData.Household sample = workforce.households().stream()
                .filter(household -> household.nodeId().equals(sampleNode.id))
                .findFirst().orElse(null);
        if (sample == null) return;
        Set<String> sampleNames = new HashSet<>();
        for (ErdenExteriorWorkforceSavedData.Resident resident : sample.residents()) {
            sampleNames.add(resident.name());
        }
        int spawned = level.getEntitiesOfClass(
                Villager.class, exteriorBounds(level),
                villager -> sampleNames.contains(villager.getName().getString())).size();
        if (spawned != sample.residents().size()) return;
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_EXTERIOR_WORKFORCE_PASS revision={} nodes={} households={} residents={} workers={} dependents={} producer_workers={} wharf_workers={} attended={} absent={} loaded_sample={} attendance=true scheduled_rest=true death_ledger=true production_linked=true",
                WORKFORCE_REVISION, EXPECTED_NODES, EXPECTED_HOUSEHOLDS,
                EXPECTED_RESIDENTS, EXPECTED_WORKERS, EXPECTED_DEPENDENTS,
                EXPECTED_PRODUCER_WORKERS, EXPECTED_WHARF_WORKERS,
                workforce.attendedWorkerCount(), workforce.absentWorkerCount(), spawned);
    }

    private static Map<String, Villager> loadedResidentsByName(
            ServerLevel level,
            ErdenExteriorWorkforceSavedData workforce) {
        Set<String> names = references(workforce).keySet();
        Map<String, Villager> result = new HashMap<>();
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class, exteriorBounds(level),
                candidate -> names.contains(candidate.getName().getString()))) {
            result.putIfAbsent(villager.getName().getString(), villager);
        }
        return result;
    }

    private static Map<String, ResidentRef> references(
            ErdenExteriorWorkforceSavedData workforce) {
        Map<String, ResidentRef> result = new HashMap<>();
        for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {
            for (ErdenExteriorWorkforceSavedData.Resident resident : household.residents()) {
                result.put(resident.name(), new ResidentRef(household, resident));
            }
        }
        return result;
    }

    private static ResidentRef findByName(
            ErdenExteriorWorkforceSavedData workforce,
            String name) {
        for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {
            for (ErdenExteriorWorkforceSavedData.Resident resident : household.residents()) {
                if (resident.name().equals(name)) return new ResidentRef(household, resident);
            }
        }
        return null;
    }

    private static AABB exteriorBounds(ServerLevel level) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            minX = Math.min(minX, node.x - 160);
            minZ = Math.min(minZ, node.z - 160);
            maxX = Math.max(maxX, node.x + 160);
            maxZ = Math.max(maxZ, node.z + 160);
        }
        return new AABB(minX, level.getMinY(), minZ, maxX, level.getMaxY(), maxZ);
    }

    private static int safeStandingY(ServerLevel level, int x, int preferredY, int z) {
        for (int offset = 0; offset <= 8; offset++) {
            int[] candidates = offset == 0
                    ? new int[]{preferredY}
                    : new int[]{preferredY + offset, preferredY - offset};
            for (int standingY : candidates) {
                BlockPos feet = new BlockPos(x, standingY, z);
                if (!level.getBlockState(feet.below()).isAir()
                        && level.getBlockState(feet).isAir()
                        && level.getBlockState(feet.above()).isAir()) return standingY;
            }
        }
        return preferredY;
    }

    private static int requiredWorkers(String role) {
        return switch (role) {
            case "grain_estate" -> 8;
            case "ranch" -> 7;
            case "colliery" -> 9;
            case "iron_mine" -> 10;
            case "paper_mill" -> 8;
            case "river_wharf" -> 6;
            default -> throw new IllegalStateException("Unknown Erden workforce role " + role);
        };
    }

    private static Shift shiftFor(String role, int workerIndex) {
        return switch (role) {
            case "grain_estate", "ranch" -> (workerIndex & 1) == 0
                    ? new Shift(1_000, 7_000) : new Shift(5_000, 11_000);
            case "colliery", "iron_mine" -> (workerIndex & 1) == 0
                    ? new Shift(1_000, 7_000) : new Shift(7_000, 13_000);
            case "paper_mill" -> (workerIndex & 1) == 0
                    ? new Shift(2_000, 8_000) : new Shift(6_000, 12_000);
            case "river_wharf" -> workerIndex % 3 == 0
                    ? new Shift(1_000, 7_000)
                    : workerIndex % 3 == 1
                    ? new Shift(7_000, 13_000)
                    : new Shift(13_000, 1_000);
            default -> new Shift(2_000, 8_000);
        };
    }

    private static boolean inShift(long dayTime, int start, int end) {
        if (start <= end) return dayTime >= start && dayTime < end;
        return dayTime >= start || dayTime < end;
    }

    private static String familyName(int index) {
        return FAMILY_PREFIXES.get(index / FAMILY_SUFFIXES.size())
                + FAMILY_SUFFIXES.get(index % FAMILY_SUFFIXES.size());
    }

    private static String roleName(String role) {
        return switch (role) {
            case "grain_estate" -> "곡물 농장";
            case "ranch" -> "목장";
            case "colliery" -> "탄광";
            case "iron_mine" -> "철광산";
            case "paper_mill" -> "제지소";
            case "river_wharf" -> "강변 부두";
            default -> "외곽 작업장";
        };
    }

    private static String shiftName(int start, int end) {
        if (start > end) return "야간 교대";
        if (start < 2_000) return "이른 교대";
        if (start >= 7_000) return "늦은 교대";
        return "주간 교대";
    }

    private static long positionKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private record Shift(int start, int end) {
    }

    private record ResidentRef(
            ErdenExteriorWorkforceSavedData.Household household,
            ErdenExteriorWorkforceSavedData.Resident resident) {
    }

    private record Target(int x, int y, int z) {
    }
}
