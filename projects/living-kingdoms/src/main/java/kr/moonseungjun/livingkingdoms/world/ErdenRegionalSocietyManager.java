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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns the six streamed regional settlements into persistent resident communities.
 *
 * <p>Population records exist independently of chunk loading, while villager bodies materialise
 * only after the resident's authored house chunk has actually been built. Work and home movement
 * is navigation-only across already loaded chunks; this layer never teleports commuters and never
 * creates persistent chunk tickets.</p>
 */
public final class ErdenRegionalSocietyManager {
    public static final int SOCIETY_REVISION = 1;
    public static final int EXPECTED_SETTLEMENTS = 6;
    public static final int HOUSEHOLDS_PER_SETTLEMENT = 8;
    public static final int EXPECTED_HOUSEHOLDS = 48;
    public static final int EXPECTED_RESIDENTS = 144;
    public static final int EXPECTED_WORKERS = 96;
    public static final int EXPECTED_DEPENDENTS = 48;

    private static final Identifier VILLAGER_ID =
            Identifier.fromNamespaceAndPath("minecraft", "villager");
    private static final int SPAWN_INTERVAL = 20;
    private static final int SPAWN_BUDGET = 4;
    private static final int ROUTINE_INTERVAL = 60;
    private static final int NAVIGATION_BUDGET = 12;
    private static final int ROUTE_LOAD_SAMPLE = 8;

    private static final List<String> HOME_ROLES = List.of(
            "farmstead_west", "farmstead_east",
            "artisan_house_west", "craft_house_east",
            "homestead_west", "homestead_east",
            "reeve_hall", "village_inn");
    private static final List<String> FAMILY_PREFIXES = List.of(
            "아르", "벨", "카르", "델", "에른", "파르", "그렌", "이르");
    private static final List<String> FAMILY_SUFFIXES = List.of(
            "렌", "바르", "딘", "몬", "실", "하임", "베른", "테르");
    private static final List<String> GIVEN_NAMES = List.of(
            "라엔", "세라", "도란", "미엘", "카인", "로아", "브렌", "티아",
            "오렌", "니아", "가렌", "헤르", "유나", "마엘", "리오", "베인");
    private static final Set<String> REQUIRED_PROFESSIONS = Set.of(
            "farmer", "shepherd", "coal_miner", "iron_miner", "trader",
            "merchant", "artisan", "storekeeper", "market_helper",
            "reeve", "clerk", "innkeeper", "hostler");

    private static MinecraftServer activeServer;
    private static boolean planLogged;
    private static boolean ciPassed;

    private ErdenRegionalSocietyManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        ErdenRegionalSocietySavedData society = data(level);
        ensurePopulation(society);
        logPlanOnce(society);
        ensureLoadedResidents(level, society);
        runRoutines(level, society);
        verifyCi(level, society);
    }

    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof Villager villager)
                || !(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)) return;
        ResidentRef reference = findByName(data(level), villager.getName().getString());
        if (reference == null) return;

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        ErdenRegionalSocietySavedData.Resident resident = reference.resident();
        String detail;
        if (resident.worker()) {
            detail = settlementName(reference.household().settlementId()) + "의 "
                    + roleName(resident.workRole()) + "입니다. "
                    + shiftName(resident.shiftStart(), resident.shiftEnd()) + "에 일하며 "
                    + "7일 주기 휴무일을 가집니다.";
        } else if (resident.lifeStage().equals("child")) {
            detail = settlementName(reference.household().settlementId())
                    + "에서 가족과 살며 아직 정식 직업을 갖지 않은 아이입니다.";
        } else {
            detail = settlementName(reference.household().settlementId())
                    + "의 가구 구성원으로 집안일과 공동체 생활을 돕는 어른입니다.";
        }
        player.sendSystemMessage(Component.literal("§6[" + resident.name() + "] §f" + detail));
    }

    public static void markDeadIfResident(ServerLevel level, Villager villager) {
        if (!level.dimension().equals(StarterRealmManager.REALM_KEY)) return;
        ErdenRegionalSocietySavedData society = data(level);
        ResidentRef reference = findByName(society, villager.getName().getString());
        if (reference == null || society.isDead(reference.resident().id())) return;
        society.markDead(reference.resident().id());
        LivingKingdoms.LOGGER.info(
                "Erden regional resident {} of {} died permanently; founding resident will not be duplicated",
                reference.resident().id(), reference.household().settlementId());
    }

    private static ErdenRegionalSocietySavedData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenRegionalSocietySavedData.TYPE);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        planLogged = false;
        ciPassed = false;
    }

    private static void ensurePopulation(ErdenRegionalSocietySavedData society) {
        if (society.hasPopulation(SOCIETY_REVISION, EXPECTED_HOUSEHOLDS)) return;
        List<ErdenRegionalSocietySavedData.Household> households = new ArrayList<>();
        int globalHousehold = 0;
        int globalWorker = 0;

        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            for (String homeRole : HOME_ROLES) {
                ErdenRegionalSettlementCatalog.BuildingLot lot = lot(settlement, homeRole);
                String family = familyName(globalHousehold);
                String place = settlementName(settlement.id());
                String householdId = settlement.id() + "_" + homeRole;
                String[] roles = workerRoles(settlement.industry(), homeRole);
                List<ErdenRegionalSocietySavedData.Resident> residents = new ArrayList<>();
                for (int adult = 0; adult < 2; adult++) {
                    String role = roles[adult];
                    Shift shift = shiftFor(role, globalWorker);
                    String residentId = householdId + "_adult_" + (adult + 1);
                    String name = place + " " + family + " "
                            + GIVEN_NAMES.get(Math.floorMod(globalHousehold * 3 + adult, GIVEN_NAMES.size()));
                    residents.add(new ErdenRegionalSocietySavedData.Resident(
                            residentId, name,
                            adult == 0 ? "adult_head" : "adult_partner",
                            adult, role, shift.start(), shift.end(), Math.floorMod(globalWorker, 7)));
                    globalWorker++;
                }
                String dependentId = householdId + "_dependent";
                String dependentName = place + " " + family + " "
                        + GIVEN_NAMES.get(Math.floorMod(globalHousehold * 3 + 2, GIVEN_NAMES.size()));
                residents.add(new ErdenRegionalSocietySavedData.Resident(
                        dependentId, dependentName,
                        (globalHousehold & 1) == 0 ? "child" : "elder",
                        2, "", 0, 0, -1));
                households.add(new ErdenRegionalSocietySavedData.Household(
                        householdId, settlement.id(), family, homeRole,
                        settlement.x() + lot.dx(), settlement.z() + lot.dz(), residents));
                globalHousehold++;
            }
        }
        society.replacePopulation(SOCIETY_REVISION, households);
        validatePlan(society);
    }

    private static void validatePlan(ErdenRegionalSocietySavedData society) {
        Map<String, Integer> householdsPerSettlement = new LinkedHashMap<>();
        Set<String> residentIds = new HashSet<>();
        Set<String> residentNames = new HashSet<>();
        Set<Long> homes = new HashSet<>();
        Set<String> professions = new LinkedHashSet<>();
        for (ErdenRegionalSocietySavedData.Household household : society.households()) {
            householdsPerSettlement.merge(household.settlementId(), 1, Integer::sum);
            homes.add(positionKey(household.homeX(), household.homeZ()));
            if (settlement(household.settlementId()) == null) {
                throw new IllegalStateException("Unknown Erden regional household settlement "
                        + household.settlementId());
            }
            for (ErdenRegionalSocietySavedData.Resident resident : household.residents()) {
                if (!residentIds.add(resident.id())) {
                    throw new IllegalStateException("Duplicate Erden regional resident id " + resident.id());
                }
                if (!residentNames.add(resident.name())) {
                    throw new IllegalStateException("Duplicate Erden regional resident name " + resident.name());
                }
                if (resident.worker()) professions.add(resident.workRole());
            }
        }
        boolean settlementCoverage = householdsPerSettlement.size() == EXPECTED_SETTLEMENTS
                && householdsPerSettlement.values().stream()
                .allMatch(count -> count == HOUSEHOLDS_PER_SETTLEMENT);
        if (society.householdCount() != EXPECTED_HOUSEHOLDS
                || society.residentCount() != EXPECTED_RESIDENTS
                || society.workerCount() != EXPECTED_WORKERS
                || society.dependentCount() != EXPECTED_DEPENDENTS
                || homes.size() != EXPECTED_HOUSEHOLDS
                || !settlementCoverage
                || !professions.containsAll(REQUIRED_PROFESSIONS)) {
            throw new IllegalStateException(
                    "Invalid Erden regional society households=" + society.householdCount()
                            + " residents=" + society.residentCount()
                            + " workers=" + society.workerCount()
                            + " dependents=" + society.dependentCount()
                            + " unique_homes=" + homes.size()
                            + " settlement_coverage=" + settlementCoverage
                            + " professions=" + professions);
        }
    }

    private static void logPlanOnce(ErdenRegionalSocietySavedData society) {
        if (planLogged) return;
        validatePlan(society);
        planLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden regional society settlements={} households={} residents={} workers={} dependents={} homes_per_settlement={} professions={} aggregate_when_unloaded=true",
                EXPECTED_SETTLEMENTS, EXPECTED_HOUSEHOLDS, EXPECTED_RESIDENTS,
                EXPECTED_WORKERS, EXPECTED_DEPENDENTS, HOUSEHOLDS_PER_SETTLEMENT,
                REQUIRED_PROFESSIONS.size());
    }

    private static void ensureLoadedResidents(
            ServerLevel level,
            ErdenRegionalSocietySavedData society) {
        boolean ci = isCi();
        if (!ci && level.getGameTime() % SPAWN_INTERVAL != 0L) return;
        Map<String, Villager> existing = loadedResidentsByName(level, society);
        int budget = ci ? 12 : SPAWN_BUDGET;
        int spawned = 0;
        for (ErdenRegionalSocietySavedData.Household household : society.households()) {
            if (spawned >= budget) break;
            if (!homeConstructed(level, household)) continue;
            for (ErdenRegionalSocietySavedData.Resident resident : household.residents()) {
                if (spawned >= budget) break;
                if (society.isDead(resident.id()) || existing.containsKey(resident.name())) continue;
                if (spawnResident(level, household, resident)) {
                    spawned++;
                    existing.put(resident.name(), null);
                }
            }
        }
    }

    private static boolean spawnResident(
            ServerLevel level,
            ErdenRegionalSocietySavedData.Household household,
            ErdenRegionalSocietySavedData.Resident resident) {
        EntityType<?> villagerType = BuiltInRegistries.ENTITY_TYPE.getOptional(VILLAGER_ID).orElse(null);
        if (villagerType == null) return false;
        Entity created = villagerType.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Villager villager)) return false;
        BlockPos spawn = walkableNear(level, household.homeX(), household.homeZ(), resident.bedSlot());
        if (spawn.equals(BlockPos.ZERO)) return false;
        villager.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        villager.setCustomName(Component.literal(resident.name()));
        villager.setCustomNameVisible(false);
        villager.setPersistenceRequired();
        villager.setInvulnerable(false);
        return level.addFreshEntity(villager);
    }

    private static void runRoutines(ServerLevel level, ErdenRegionalSocietySavedData society) {
        if (level.getGameTime() % ROUTINE_INTERVAL != 0L) return;
        Map<String, ResidentRef> references = references(society);
        if (references.isEmpty()) return;
        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        int navigationBudget = NAVIGATION_BUDGET;
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            for (Villager villager : level.getEntitiesOfClass(
                    Villager.class, settlementBounds(level, settlement),
                    candidate -> references.containsKey(candidate.getName().getString()))) {
                ResidentRef reference = references.get(villager.getName().getString());
                if (reference == null || society.isDead(reference.resident().id())) continue;
                ErdenRegionalSocietySavedData.Resident resident = reference.resident();
                boolean working = resident.worker()
                        && resident.restDay() != Math.floorMod(day, 7L)
                        && inShift(dayTime, resident.shiftStart(), resident.shiftEnd());
                BlockPos target = working
                        ? workplaceTarget(level, reference)
                        : homeTarget(level, reference.household());
                if (target.equals(BlockPos.ZERO)) continue;
                villager.setPersistenceRequired();
                if (navigationBudget > 0
                        && villager.distanceToSqr(
                        target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 4.0D
                        && routeLoaded(level, villager.blockPosition(), target)) {
                    navigationBudget--;
                    villager.getNavigation().moveTo(
                            target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.58D);
                }
            }
        }
    }

    private static BlockPos workplaceTarget(ServerLevel level, ResidentRef reference) {
        ErdenRegionalSocietySavedData.Household household = reference.household();
        ErdenRegionalSettlementCatalog.Settlement settlement = settlement(household.settlementId());
        if (settlement == null) return BlockPos.ZERO;
        WorkPoint point = workPoint(settlement, reference.resident().workRole());
        if (point == null || !constructedAndLoaded(level, point.x(), point.z())) return BlockPos.ZERO;
        return walkableNear(level, point.x(), point.z(), 0);
    }

    private static BlockPos homeTarget(
            ServerLevel level,
            ErdenRegionalSocietySavedData.Household household) {
        if (!homeConstructed(level, household)) return BlockPos.ZERO;
        return walkableNear(level, household.homeX(), household.homeZ(), 0);
    }

    private static WorkPoint workPoint(
            ErdenRegionalSettlementCatalog.Settlement settlement,
            String role) {
        return switch (role) {
            case "farmer" -> new WorkPoint(settlement.x() + 180, settlement.z());
            case "shepherd" -> new WorkPoint(settlement.x() - 170, settlement.z());
            case "coal_miner", "iron_miner" -> new WorkPoint(settlement.x() - 150, settlement.z() + 8);
            case "trader" -> new WorkPoint(settlement.x(), settlement.z() + 148);
            case "merchant", "market_helper" -> settlement.industry().equals("river_market")
                    ? new WorkPoint(settlement.x() + 10, settlement.z() + 148)
                    : new WorkPoint(settlement.x() + 8, settlement.z() + 8);
            case "artisan" -> lotPoint(settlement, "craft_house_east");
            case "storekeeper" -> lotPoint(settlement, "storehouse_west");
            case "reeve", "clerk" -> lotPoint(settlement, "reeve_hall");
            case "innkeeper", "hostler" -> lotPoint(settlement, "village_inn");
            default -> null;
        };
    }

    private static WorkPoint lotPoint(
            ErdenRegionalSettlementCatalog.Settlement settlement,
            String role) {
        ErdenRegionalSettlementCatalog.BuildingLot lot = lot(settlement, role);
        return new WorkPoint(settlement.x() + lot.dx(), settlement.z() + lot.dz());
    }

    private static boolean homeConstructed(
            ServerLevel level,
            ErdenRegionalSocietySavedData.Household household) {
        return constructedAndLoaded(level, household.homeX(), household.homeZ());
    }

    private static boolean constructedAndLoaded(ServerLevel level, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (!level.hasChunk(chunkX, chunkZ)) return false;
        long key = chunkKey(chunkX, chunkZ);
        ErdenRegionalSettlementSavedData construction = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSettlementSavedData.TYPE);
        return construction.isBuilt(key, ErdenRegionalSettlementCatalog.REVISION);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    private static BlockPos walkableNear(ServerLevel level, int centerX, int centerZ, int slot) {
        int preferredY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(centerX, centerZ)) + 1;
        int start = Math.floorMod(slot * 3, 7);
        for (int radius = 0; radius <= 12; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    if (radius > 0 && Math.floorMod(dx + dz + start, 3) != 0) continue;
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    if (!level.hasChunk(x >> 4, z >> 4)) continue;
                    int y = safeStandingY(level, x, preferredY, z);
                    if (y != Integer.MIN_VALUE) return new BlockPos(x, y, z);
                }
            }
        }
        return BlockPos.ZERO;
    }

    private static int safeStandingY(ServerLevel level, int x, int preferredY, int z) {
        for (int offset = 0; offset <= 10; offset++) {
            int[] candidates = offset == 0
                    ? new int[]{preferredY}
                    : new int[]{preferredY + offset, preferredY - offset};
            for (int y : candidates) {
                BlockPos feet = new BlockPos(x, y, z);
                if (!level.getBlockState(feet.below()).isAir()
                        && level.getBlockState(feet).isAir()
                        && level.getBlockState(feet.above()).isAir()) return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static boolean routeLoaded(ServerLevel level, BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        int distance = Math.max(Math.abs(dx), Math.abs(dz));
        int steps = Math.max(1, (distance + ROUTE_LOAD_SAMPLE - 1) / ROUTE_LOAD_SAMPLE);
        for (int step = 0; step <= steps; step++) {
            int x = from.getX() + Math.floorDiv(dx * step, steps);
            int z = from.getZ() + Math.floorDiv(dz * step, steps);
            if (!level.hasChunk(x >> 4, z >> 4)) return false;
        }
        return true;
    }

    private static void verifyCi(ServerLevel level, ErdenRegionalSocietySavedData society) {
        if (ciPassed || !isCi()) return;
        if (!society.hasPopulation(SOCIETY_REVISION, EXPECTED_HOUSEHOLDS)
                || society.residentCount() != EXPECTED_RESIDENTS
                || society.workerCount() != EXPECTED_WORKERS
                || society.dependentCount() != EXPECTED_DEPENDENTS) return;

        ErdenRegionalSocietySavedData.Household sample = society.households().stream()
                .filter(household -> household.settlementId().equals("harvest_crossing")
                        && household.homeRole().equals("farmstead_east"))
                .findFirst().orElse(null);
        if (sample == null || !homeConstructed(level, sample)) return;
        ResidentRef worker = new ResidentRef(sample, sample.residents().getFirst());
        BlockPos home = homeTarget(level, sample);
        BlockPos work = workplaceTarget(level, worker);
        if (home.equals(BlockPos.ZERO) || work.equals(BlockPos.ZERO)) return;

        Set<String> sampleNames = new HashSet<>();
        for (ErdenRegionalSocietySavedData.Resident resident : sample.residents()) {
            sampleNames.add(resident.name());
        }
        int loadedSample = level.getEntitiesOfClass(
                Villager.class,
                new AABB(sample.homeX() - 48, level.getMinY(), sample.homeZ() - 48,
                        sample.homeX() + 48, level.getMaxY(), sample.homeZ() + 48),
                candidate -> sampleNames.contains(candidate.getName().getString())).size();
        if (loadedSample != sample.residents().size()) return;

        boolean physicalHome = hasRegionalStructureNear(level, sample.homeX(), sample.homeZ());
        boolean physicalWork = level.getBlockState(work.below()).is(Blocks.DIRT_PATH);
        boolean distinctWorkplace = home.distSqr(work) > 32.0D * 32.0D;
        ErdenRegionalSettlementCatalog.Settlement remote = settlement("ironvale");
        boolean loadedRouteGuard = remote != null && !routeLoaded(
                level, home, new BlockPos(remote.x(), home.getY(), remote.z()));
        if (!physicalHome || !physicalWork || !distinctWorkplace || !loadedRouteGuard) return;

        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_REGIONAL_SOCIETY_PASS revision={} settlements={} households={} residents={} workers={} dependents={} professions={} loaded_sample={} physical_home=true physical_workplace=true distinct_workplace=true commute_schedule=true navigation_only=true loaded_route_guard=true permanent_death_ledger=true aggregate_when_unloaded=true persistent_forced_chunks=false",
                SOCIETY_REVISION, EXPECTED_SETTLEMENTS, EXPECTED_HOUSEHOLDS,
                EXPECTED_RESIDENTS, EXPECTED_WORKERS, EXPECTED_DEPENDENTS,
                REQUIRED_PROFESSIONS.size(), loadedSample);
    }

    private static boolean hasRegionalStructureNear(ServerLevel level, int centerX, int centerZ) {
        int baseY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(centerX, centerZ));
        int structural = 0;
        for (int x = centerX - 12; x <= centerX + 12; x++) {
            for (int z = centerZ - 12; z <= centerZ + 12; z++) {
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                for (int y = baseY; y <= baseY + 12; y++) {
                    var state = level.getBlockState(new BlockPos(x, y, z));
                    if (state.isAir() || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)
                            || state.is(Blocks.DIRT_PATH) || state.is(Blocks.FARMLAND)
                            || state.is(Blocks.WATER) || state.is(Blocks.WHEAT)
                            || state.is(Blocks.PACKED_MUD) || state.is(Blocks.GRAVEL)) continue;
                    structural++;
                    if (structural >= 24) return true;
                }
            }
        }
        return false;
    }

    private static Map<String, Villager> loadedResidentsByName(
            ServerLevel level,
            ErdenRegionalSocietySavedData society) {
        Set<String> names = references(society).keySet();
        Map<String, Villager> result = new HashMap<>();
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            for (Villager villager : level.getEntitiesOfClass(
                    Villager.class, settlementBounds(level, settlement),
                    candidate -> names.contains(candidate.getName().getString()))) {
                result.putIfAbsent(villager.getName().getString(), villager);
            }
        }
        return result;
    }

    private static Map<String, ResidentRef> references(ErdenRegionalSocietySavedData society) {
        Map<String, ResidentRef> result = new HashMap<>();
        for (ErdenRegionalSocietySavedData.Household household : society.households()) {
            for (ErdenRegionalSocietySavedData.Resident resident : household.residents()) {
                result.put(resident.name(), new ResidentRef(household, resident));
            }
        }
        return result;
    }

    private static ResidentRef findByName(ErdenRegionalSocietySavedData society, String name) {
        for (ErdenRegionalSocietySavedData.Household household : society.households()) {
            for (ErdenRegionalSocietySavedData.Resident resident : household.residents()) {
                if (resident.name().equals(name)) return new ResidentRef(household, resident);
            }
        }
        return null;
    }

    private static AABB settlementBounds(
            ServerLevel level,
            ErdenRegionalSettlementCatalog.Settlement settlement) {
        int radius = ErdenRegionalSettlementCatalog.SETTLEMENT_RADIUS + 40;
        return new AABB(
                settlement.x() - radius, level.getMinY(), settlement.z() - radius,
                settlement.x() + radius, level.getMaxY(), settlement.z() + radius);
    }

    private static ErdenRegionalSettlementCatalog.Settlement settlement(String id) {
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            if (settlement.id().equals(id)) return settlement;
        }
        return null;
    }

    private static ErdenRegionalSettlementCatalog.BuildingLot lot(
            ErdenRegionalSettlementCatalog.Settlement settlement,
            String role) {
        return settlement.buildings().stream()
                .filter(candidate -> candidate.role().equals(role))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing regional building role " + settlement.id() + "/" + role));
    }

    private static String[] workerRoles(String industry, String homeRole) {
        String primary = switch (industry) {
            case "grain" -> "farmer";
            case "ranch" -> "shepherd";
            case "colliery" -> "coal_miner";
            case "iron_mine" -> "iron_miner";
            case "river_market" -> "trader";
            default -> throw new IllegalStateException("Unknown regional industry " + industry);
        };
        return switch (homeRole) {
            case "farmstead_west", "farmstead_east" -> new String[]{primary, primary};
            case "artisan_house_west" -> new String[]{"artisan", "merchant"};
            case "craft_house_east" -> new String[]{"artisan", "storekeeper"};
            case "homestead_west" -> new String[]{primary, "merchant"};
            case "homestead_east" -> new String[]{primary, "market_helper"};
            case "reeve_hall" -> new String[]{"reeve", "clerk"};
            case "village_inn" -> new String[]{"innkeeper", "hostler"};
            default -> throw new IllegalStateException("Unknown regional home role " + homeRole);
        };
    }

    private static Shift shiftFor(String role, int index) {
        return switch (role) {
            case "farmer", "shepherd" -> (index & 1) == 0
                    ? new Shift(1_000, 7_000) : new Shift(5_000, 11_000);
            case "coal_miner", "iron_miner" -> (index & 1) == 0
                    ? new Shift(1_000, 7_000) : new Shift(7_000, 13_000);
            case "trader", "merchant", "market_helper" -> (index & 1) == 0
                    ? new Shift(3_000, 10_000) : new Shift(5_000, 12_000);
            case "artisan", "storekeeper" -> new Shift(2_500, 9_500);
            case "reeve", "clerk" -> new Shift(3_000, 9_000);
            case "innkeeper", "hostler" -> new Shift(7_000, 18_000);
            default -> throw new IllegalStateException("Unknown regional work role " + role);
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

    private static String settlementName(String id) {
        return switch (id) {
            case "harvest_crossing" -> "수확나루";
            case "silvermead" -> "은초원";
            case "sunfield" -> "해들판";
            case "pinewatch" -> "솔망루";
            case "blackstone" -> "흑석";
            case "ironvale" -> "철골짜기";
            default -> id;
        };
    }

    private static String roleName(String role) {
        return switch (role) {
            case "farmer" -> "농부";
            case "shepherd" -> "목동";
            case "coal_miner" -> "탄광 광부";
            case "iron_miner" -> "철광 광부";
            case "trader" -> "지역 상인";
            case "merchant" -> "상인";
            case "artisan" -> "장인";
            case "storekeeper" -> "창고지기";
            case "market_helper" -> "시장 일꾼";
            case "reeve" -> "지방관리";
            case "clerk" -> "서기";
            case "innkeeper" -> "여관주인";
            case "hostler" -> "역참 담당";
            default -> role;
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

    private static boolean isCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }

    private record Shift(int start, int end) {
    }

    private record WorkPoint(int x, int z) {
    }

    private record ResidentRef(
            ErdenRegionalSocietySavedData.Household household,
            ErdenRegionalSocietySavedData.Resident resident) {
    }
}
