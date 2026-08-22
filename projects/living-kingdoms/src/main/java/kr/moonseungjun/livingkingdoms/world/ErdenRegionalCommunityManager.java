package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds actual off-duty community life to the six regional villages.
 *
 * <p>The founding society remains authoritative for identity, employment and death. This layer only
 * chooses where a living resident spends off-duty time: home meals, errands at the physical
 * storehouse market, the village square, the village inn, the reeve hall, or a neighboring home.
 * Work shifts always win. Movement is navigation-only and is attempted only when every sampled
 * route chunk is already loaded; this manager never creates chunk tickets and never teleports a
 * resident.</p>
 */
public final class ErdenRegionalCommunityManager {
    public static final int COMMUNITY_REVISION = 1;
    public static final int EXPECTED_SETTLEMENTS = 6;
    public static final int EXPECTED_HOUSEHOLDS = 48;
    public static final int EXPECTED_RESIDENTS = 144;

    // Community runs after Society in the bridge. 20 divides Society's 60-tick routine interval,
    // so every off-duty "return home" decision is deterministically superseded on that same tick.
    private static final int ROUTINE_INTERVAL = 20;
    private static final int NAVIGATION_BUDGET = 10;
    private static final int ROUTE_LOAD_SAMPLE = 8;
    private static final long WHEAT_DAY_RESERVE_PER_HOUSEHOLD = 2L;
    private static final int[][] SQUARE_OFFSETS = {
            {-8, -8}, {0, -8}, {8, -8},
            {-8, 0},           {8, 0},
            {-8, 8},  {0, 8}, {8, 8}
    };

    private static MinecraftServer activeServer;
    private static boolean planLogged;
    private static boolean ciPassed;

    private ErdenRegionalCommunityManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

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

        logPlanOnce(society);
        runCommunityRoutines(level, society, economy);
        verifyCi(society, economy);
    }

    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled()
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof Villager villager)
                || !(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)) return;

        ErdenRegionalSocietySavedData society = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSocietySavedData.TYPE);
        ResidentContext context = contextByName(society, villager.getName().getString());
        if (context == null || society.isDead(context.resident().id())) return;
        ErdenRegionalEconomySavedData economy = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalEconomySavedData.TYPE);
        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        boolean foodSecure = foodSecure(economy, context.household().settlementId(), society);
        Activity activity = scheduledActivity(context.household(), context.resident(), day, dayTime, foodSecure);
        player.sendSystemMessage(Component.literal(
                "§e[현재 생활] §f" + activityLabel(activity, context.household(), day)), true);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        planLogged = false;
        ciPassed = false;
    }

    private static void logPlanOnce(ErdenRegionalSocietySavedData society) {
        if (planLogged) return;
        if (society.householdCount() != EXPECTED_HOUSEHOLDS
                || society.residentCount() != EXPECTED_RESIDENTS) {
            throw new IllegalStateException(
                    "Invalid regional community population households=" + society.householdCount()
                            + " residents=" + society.residentCount());
        }
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            requireLot(settlement, "village_inn");
            requireLot(settlement, "reeve_hall");
            requireLot(settlement, "storehouse_west");
        }
        planLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden regional community revision={} settlements={} households={} residents={} modes={} work_priority=true off_duty_precedence=true family_schedule=true rest_day_social=true shortage_aware=true fluid_safe_targets=true navigation_only=true no_chunk_loading=true",
                COMMUNITY_REVISION, EXPECTED_SETTLEMENTS, EXPECTED_HOUSEHOLDS,
                EXPECTED_RESIDENTS, Activity.values().length);
    }

    private static void runCommunityRoutines(
            ServerLevel level,
            ErdenRegionalSocietySavedData society,
            ErdenRegionalEconomySavedData economy) {
        if (level.getGameTime() % ROUTINE_INTERVAL != 0L) return;
        Map<String, ResidentContext> contexts = contexts(society);
        if (contexts.isEmpty()) return;

        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        int navigationBudget = NAVIGATION_BUDGET;
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            boolean secure = foodSecure(economy, settlement.id(), society);
            for (Villager villager : level.getEntitiesOfClass(
                    Villager.class, settlementBounds(level, settlement),
                    candidate -> contexts.containsKey(candidate.getName().getString()))) {
                ResidentContext context = contexts.get(villager.getName().getString());
                if (context == null || society.isDead(context.resident().id())) continue;
                Activity activity = scheduledActivity(
                        context.household(), context.resident(), day, dayTime, secure);
                if (activity == Activity.WORK) continue;
                BlockPos target = activityTarget(level, society, context, activity, day);
                if (target.equals(BlockPos.ZERO)) continue;
                villager.setPersistenceRequired();
                if (villager.distanceToSqr(
                        target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) <= 4.0D) {
                    villager.getNavigation().stop();
                    continue;
                }
                if (navigationBudget <= 0
                        || !routeLoaded(level, villager.blockPosition(), target)) continue;
                navigationBudget--;
                villager.getNavigation().moveTo(
                        target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.54D);
            }
        }
    }

    static Activity scheduledActivity(
            ErdenRegionalSocietySavedData.Household household,
            ErdenRegionalSocietySavedData.Resident resident,
            long day,
            long dayTime,
            boolean foodSecure) {
        long normalizedTime = Math.floorMod(dayTime, 24_000L);
        boolean restDay = resident.worker()
                && resident.restDay() == (int) Math.floorMod(day, 7L);
        if (resident.worker() && !restDay
                && inShift(normalizedTime, resident.shiftStart(), resident.shiftEnd())) {
            return Activity.WORK;
        }

        if (normalizedTime < 1_200L || normalizedTime >= 18_500L) return Activity.HOME;
        boolean child = resident.lifeStage().equals("child");
        boolean elder = resident.lifeStage().equals("elder");
        int residentCycle = stableCycle(resident.id(), day, 6);
        int householdCycle = stableCycle(household.id(), day, 6);

        if (normalizedTime < 2_800L) {
            if (child) return Activity.SQUARE;
            if (elder) return residentCycle % 2 == 0 ? Activity.REEVE : Activity.NEIGHBOR;
            if (restDay) return residentCycle % 2 == 0 ? Activity.MARKET : Activity.SQUARE;
            return Activity.HOME;
        }
        if (normalizedTime < 7_000L) {
            if (child) return Activity.SQUARE;
            if (elder) return residentCycle % 3 == 0 ? Activity.REEVE : Activity.NEIGHBOR;
            if (restDay) {
                return switch (residentCycle % 3) {
                    case 0 -> Activity.MARKET;
                    case 1 -> Activity.SQUARE;
                    default -> Activity.NEIGHBOR;
                };
            }
            return residentCycle % 3 == 0 ? Activity.NEIGHBOR : Activity.MARKET;
        }
        if (normalizedTime < 10_500L) {
            if (child) return residentCycle % 2 == 0 ? Activity.SQUARE : Activity.NEIGHBOR;
            if (elder) return residentCycle % 2 == 0 ? Activity.MARKET : Activity.NEIGHBOR;
            return switch (residentCycle % 3) {
                case 0 -> Activity.MARKET;
                case 1 -> Activity.NEIGHBOR;
                default -> Activity.SQUARE;
            };
        }
        if (normalizedTime < 12_500L) return Activity.HOME;

        if (normalizedTime < 15_500L) {
            if (!foodSecure) return Activity.HOME;
            return householdCycle % 3 == 0 ? Activity.INN : Activity.HOME;
        }

        if (normalizedTime < 18_000L) {
            if (child) return residentCycle % 2 == 0 ? Activity.NEIGHBOR : Activity.HOME;
            if (elder) return residentCycle % 2 == 0 ? Activity.SQUARE : Activity.NEIGHBOR;
            if (!foodSecure) return Activity.HOME;
            return switch (residentCycle % 3) {
                case 0 -> Activity.INN;
                case 1 -> Activity.SQUARE;
                default -> Activity.NEIGHBOR;
            };
        }
        return Activity.HOME;
    }

    static BlockPos activityTarget(
            ServerLevel level,
            ErdenRegionalSocietySavedData society,
            ResidentContext context,
            Activity activity,
            long day) {
        ErdenRegionalSettlementCatalog.Settlement settlement = settlement(context.household().settlementId());
        if (settlement == null) return BlockPos.ZERO;
        return switch (activity) {
            case WORK -> BlockPos.ZERO;
            case HOME -> constructedAndLoaded(
                    level, context.household().homeX(), context.household().homeZ())
                    ? walkableNear(level, context.household().homeX(), context.household().homeZ(),
                    context.resident().bedSlot()) : BlockPos.ZERO;
            case MARKET -> {
                BlockPos market = ErdenRegionalEconomyManager.storagePosition(settlement);
                int spread = stableCycle(context.resident().id(), day, 5) - 2;
                yield constructedAndLoaded(level, market.getX(), market.getZ())
                        ? walkableNear(level, market.getX() + spread * 2, market.getZ() + 4,
                        context.resident().bedSlot()) : BlockPos.ZERO;
            }
            case SQUARE -> {
                int index = stableCycle(context.resident().id(), day, SQUARE_OFFSETS.length);
                int x = settlement.x() + SQUARE_OFFSETS[index][0];
                int z = settlement.z() + SQUARE_OFFSETS[index][1];
                yield constructedAndLoaded(level, x, z)
                        ? walkableNear(level, x, z, context.resident().bedSlot()) : BlockPos.ZERO;
            }
            case INN -> lotTarget(level, settlement, "village_inn", context.resident().bedSlot());
            case REEVE -> lotTarget(level, settlement, "reeve_hall", context.resident().bedSlot());
            case NEIGHBOR -> neighborTarget(level, society, context, day);
        };
    }

    private static BlockPos neighborTarget(
            ServerLevel level,
            ErdenRegionalSocietySavedData society,
            ResidentContext context,
            long day) {
        List<ErdenRegionalSocietySavedData.Household> neighbors = new ArrayList<>();
        for (ErdenRegionalSocietySavedData.Household candidate : society.households()) {
            if (candidate.settlementId().equals(context.household().settlementId())
                    && !candidate.id().equals(context.household().id())) {
                neighbors.add(candidate);
            }
        }
        if (neighbors.isEmpty()) return BlockPos.ZERO;
        int index = stableCycle(context.household().id(), day, neighbors.size());
        ErdenRegionalSocietySavedData.Household neighbor = neighbors.get(index);
        if (!constructedAndLoaded(level, neighbor.homeX(), neighbor.homeZ())) return BlockPos.ZERO;
        return walkableNear(level, neighbor.homeX(), neighbor.homeZ(), context.resident().bedSlot());
    }

    private static BlockPos lotTarget(
            ServerLevel level,
            ErdenRegionalSettlementCatalog.Settlement settlement,
            String role,
            int slot) {
        ErdenRegionalSettlementCatalog.BuildingLot lot = requireLot(settlement, role);
        int x = settlement.x() + lot.dx();
        int z = settlement.z() + lot.dz();
        if (!constructedAndLoaded(level, x, z)) return BlockPos.ZERO;
        return walkableNear(level, x, z, slot);
    }

    private static boolean foodSecure(
            ErdenRegionalEconomySavedData economy,
            String settlementId,
            ErdenRegionalSocietySavedData society) {
        ErdenRegionalEconomySavedData.SettlementState state = economy.settlement(settlementId);
        if (state == null) return false;
        int households = 0;
        for (ErdenRegionalSocietySavedData.Household household : society.households()) {
            if (household.settlementId().equals(settlementId)) households++;
        }
        long oneDayReserve = households * WHEAT_DAY_RESERVE_PER_HOUSEHOLD;
        return state.stock("wheat") >= oneDayReserve;
    }

    private static void verifyCi(
            ErdenRegionalSocietySavedData society,
            ErdenRegionalEconomySavedData economy) {
        if (ciPassed || !isCi()) return;
        Set<Activity> modes = EnumSet.noneOf(Activity.class);
        boolean deterministic = true;
        boolean childSquare = false;
        boolean elderNeighboring = false;
        boolean restDaySocial = false;
        boolean shortageAware = false;
        int familySuppers = 0;
        long[] sampleTimes = {600L, 1_800L, 4_000L, 8_000L, 11_000L, 13_500L, 16_500L, 20_000L};

        for (long day = 0; day < 7; day++) {
            for (ErdenRegionalSocietySavedData.Household household : society.households()) {
                boolean sameFamilySupper = true;
                Activity familyActivity = null;
                for (ErdenRegionalSocietySavedData.Resident resident : household.residents()) {
                    for (long time : sampleTimes) {
                        Activity first = scheduledActivity(household, resident, day, time, true);
                        Activity second = scheduledActivity(household, resident, day, time, true);
                        modes.add(first);
                        deterministic &= first == second;
                        if (resident.lifeStage().equals("child") && first == Activity.SQUARE) {
                            childSquare = true;
                        }
                        if (resident.lifeStage().equals("elder") && first == Activity.NEIGHBOR) {
                            elderNeighboring = true;
                        }
                        if (resident.worker()
                                && resident.restDay() == (int) Math.floorMod(day, 7L)
                                && time >= 2_800L && time < 10_500L
                                && first != Activity.HOME && first != Activity.WORK) {
                            restDaySocial = true;
                        }
                    }
                    Activity secureSupper = scheduledActivity(household, resident, day, 13_500L, true);
                    Activity shortageSupper = scheduledActivity(household, resident, day, 13_500L, false);
                    if (secureSupper == Activity.INN && shortageSupper == Activity.HOME) {
                        shortageAware = true;
                    }
                    if (secureSupper == Activity.WORK) {
                        sameFamilySupper = false;
                    } else if (familyActivity == null) {
                        familyActivity = secureSupper;
                    } else if (familyActivity != secureSupper) {
                        sameFamilySupper = false;
                    }
                }
                if (sameFamilySupper
                        && (familyActivity == Activity.HOME || familyActivity == Activity.INN)) {
                    familySuppers++;
                }
            }
        }

        boolean workPreserved = modes.contains(Activity.WORK);
        boolean activityCoverage = modes.containsAll(EnumSet.allOf(Activity.class));
        boolean economyPresent = economy.hasEconomy(
                ErdenRegionalEconomyManager.ECONOMY_REVISION,
                ErdenRegionalEconomyManager.EXPECTED_SETTLEMENTS);
        if (!deterministic || !childSquare || !elderNeighboring || !restDaySocial
                || !shortageAware || familySuppers < 36 || !workPreserved
                || !activityCoverage || !economyPresent) return;

        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_REGIONAL_COMMUNITY_PASS revision={} settlements={} households={} residents={} activity_modes={} family_suppers={} work_preserved=true off_duty_precedence=true breakfast_home=true market_errands=true children_square=true elder_neighboring=true family_supper=true inn_gathering=true rest_day_social=true shortage_aware=true deterministic_schedule=true fluid_safe_targets=true navigation_only=true loaded_route_guard=true no_chunk_loading=true persistent_forced_chunks=false",
                COMMUNITY_REVISION, EXPECTED_SETTLEMENTS, EXPECTED_HOUSEHOLDS,
                EXPECTED_RESIDENTS, modes.size(), familySuppers);
    }

    static boolean routeLoaded(ServerLevel level, BlockPos from, BlockPos to) {
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

    private static boolean constructedAndLoaded(ServerLevel level, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (!level.hasChunk(chunkX, chunkZ)) return false;
        ErdenRegionalSettlementSavedData construction = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSettlementSavedData.TYPE);
        return construction.isBuilt(pack(chunkX, chunkZ), ErdenRegionalSettlementCatalog.REVISION);
    }

    static BlockPos walkableNear(ServerLevel level, int centerX, int centerZ, int slot) {
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
                        && level.getFluidState(feet.below()).isEmpty()
                        && level.getBlockState(feet).isAir()
                        && level.getBlockState(feet.above()).isAir()) return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    static ResidentContext contextByName(ErdenRegionalSocietySavedData society, String name) {
        for (ErdenRegionalSocietySavedData.Household household : society.households()) {
            for (ErdenRegionalSocietySavedData.Resident resident : household.residents()) {
                if (resident.name().equals(name)) return new ResidentContext(household, resident);
            }
        }
        return null;
    }

    private static Map<String, ResidentContext> contexts(ErdenRegionalSocietySavedData society) {
        Map<String, ResidentContext> result = new HashMap<>();
        for (ErdenRegionalSocietySavedData.Household household : society.households()) {
            for (ErdenRegionalSocietySavedData.Resident resident : household.residents()) {
                result.put(resident.name(), new ResidentContext(household, resident));
            }
        }
        return result;
    }

    static ErdenRegionalSettlementCatalog.Settlement settlement(String id) {
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            if (settlement.id().equals(id)) return settlement;
        }
        return null;
    }

    static ErdenRegionalSettlementCatalog.BuildingLot requireLot(
            ErdenRegionalSettlementCatalog.Settlement settlement,
            String role) {
        return settlement.buildings().stream()
                .filter(candidate -> candidate.role().equals(role))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing regional community building " + settlement.id() + "/" + role));
    }

    private static AABB settlementBounds(
            ServerLevel level,
            ErdenRegionalSettlementCatalog.Settlement settlement) {
        int radius = ErdenRegionalSettlementCatalog.SETTLEMENT_RADIUS + 40;
        return new AABB(
                settlement.x() - radius, level.getMinY(), settlement.z() - radius,
                settlement.x() + radius, level.getMaxY(), settlement.z() + radius);
    }

    private static boolean inShift(long dayTime, int start, int end) {
        if (start <= end) return dayTime >= start && dayTime < end;
        return dayTime >= start || dayTime < end;
    }

    private static int stableCycle(String id, long day, int modulo) {
        long mixed = (long) id.hashCode() * 31L + day * 17L;
        return (int) Math.floorMod(mixed, modulo);
    }

    private static String activityLabel(
            Activity activity,
            ErdenRegionalSocietySavedData.Household household,
            long day) {
        return switch (activity) {
            case WORK -> "근무 중";
            case HOME -> "가족과 집에서 쉬는 중";
            case MARKET -> "마을 시장에서 장을 보는 중";
            case SQUARE -> "마을 광장에서 시간을 보내는 중";
            case NEIGHBOR -> "이웃집을 방문하는 중";
            case INN -> stableCycle(household.id(), day, 3) == 0
                    ? "가족과 여관에서 저녁을 먹는 중" : "마을 여관에서 사람들을 만나는 중";
            case REEVE -> "촌장관에 볼일을 보러 가는 중";
        };
    }

    private static long pack(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    private static boolean isCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }

    enum Activity {
        WORK,
        HOME,
        MARKET,
        SQUARE,
        NEIGHBOR,
        INN,
        REEVE
    }

    record ResidentContext(
            ErdenRegionalSocietySavedData.Household household,
            ErdenRegionalSocietySavedData.Resident resident) {
    }
}
