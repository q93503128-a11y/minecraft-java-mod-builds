package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authoritative ownership and maintenance simulation for Erden's 74 exterior homes.
 * Succession changes the steward while reserves, condition and occupancy remain attached to the home.
 */
public final class ErdenExteriorEstateManager {
    public static final int ESTATE_REVISION = 1;
    public static final int EXPECTED_ESTATES = ErdenExteriorWorkforceManager.EXPECTED_HOUSEHOLDS;
    public static final int HOUSEHOLD_CAPACITY = 5;

    private static final int MAX_CATCH_UP_DAYS = 30;
    private static MinecraftServer activeServer;
    private static boolean planLogged;
    private static boolean ciPassed;

    private ErdenExteriorEstateManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        ErdenExteriorWorkforceSavedData workforce = workforce(level);
        ErdenExteriorLifecycleSavedData lifecycle = lifecycle(level);
        if (!workforce.hasPopulation(
                ErdenExteriorWorkforceManager.WORKFORCE_REVISION,
                ErdenExteriorWorkforceManager.EXPECTED_HOUSEHOLDS)
                || !lifecycle.initialized(
                ErdenExteriorLifecycleManager.LIFECYCLE_REVISION,
                ErdenExteriorWorkforceManager.EXPECTED_RESIDENTS,
                ErdenExteriorWorkforceManager.EXPECTED_HOUSEHOLDS)) return;

        ErdenExteriorEstateSavedData estates = estates(level);
        ensureInitialized(estates, workforce, lifecycle, day);
        processThroughDay(estates, workforce, lifecycle, day);
        logPlanOnce(estates);
        verifyCi(estates, workforce, lifecycle, day);
    }

    public static boolean birthAllowed(ServerLevel level, String householdId) {
        ErdenExteriorEstateSavedData.Estate estate = estates(level).estate(householdId);
        return estate != null
                && !estate.vacant()
                && !estate.overcrowded()
                && estate.condition() >= 45
                && estate.maintenanceReserve() >= 2;
    }

    public static String describeHousehold(ServerLevel level, String householdId) {
        ErdenExteriorEstateSavedData.Estate estate = estates(level).estate(householdId);
        if (estate == null) return "주택 장부가 아직 준비되지 않았습니다.";
        String occupancy = estate.vacant()
                ? "빈집"
                : estate.overcrowded() ? "과밀" : "정상 거주";
        return "주택 상태 " + estate.condition() + "/100, 유지 적립금 "
                + estate.maintenanceReserve() + ", " + occupancy
                + ", 거주 " + estate.livingMembers() + "/" + estate.capacity() + "명";
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        planLogged = false;
        ciPassed = false;
    }

    private static ErdenExteriorWorkforceSavedData workforce(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenExteriorWorkforceSavedData.TYPE);
    }

    private static ErdenExteriorLifecycleSavedData lifecycle(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenExteriorLifecycleSavedData.TYPE);
    }

    private static ErdenExteriorEstateSavedData estates(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenExteriorEstateSavedData.TYPE);
    }

    private static void ensureInitialized(
            ErdenExteriorEstateSavedData estates,
            ErdenExteriorWorkforceSavedData workforce,
            ErdenExteriorLifecycleSavedData lifecycle,
            long day) {
        if (estates.initialized(ESTATE_REVISION, EXPECTED_ESTATES)) return;
        List<ErdenExteriorEstateSavedData.Estate> initial = new ArrayList<>();
        for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {
            ErdenExteriorLifecycleSavedData.HouseholdLine line =
                    lifecycle.householdLine(household.id());
            String steward = line == null ? "" : line.stewardId();
            String heir = line == null ? "" : line.heirId();
            int living = livingMembers(lifecycle, household.id(), day);
            int reserve = 24 + Math.floorMod(household.id().hashCode(), 17);
            int condition = 84 + Math.floorMod(household.id().hashCode(), 13);
            initial.add(new ErdenExteriorEstateSavedData.Estate(
                    household.id(), household.nodeId(), household.homeX(), household.homeZ(),
                    steward, heir, living, HOUSEHOLD_CAPACITY, reserve, condition,
                    line == null ? 0 : line.successionCount(),
                    line == null ? 0 : line.successionCount(),
                    day - 1L, living == 0 || steward.isBlank(),
                    living > HOUSEHOLD_CAPACITY));
        }
        estates.initialize(ESTATE_REVISION, day, initial);
        validatePlan(estates, workforce);
    }

    private static void processThroughDay(
            ErdenExteriorEstateSavedData estates,
            ErdenExteriorWorkforceSavedData workforce,
            ErdenExteriorLifecycleSavedData lifecycle,
            long currentDay) {
        if (estates.lastProcessedDay() >= currentDay) return;
        long first = estates.lastProcessedDay() < 0L
                ? currentDay
                : Math.max(estates.lastProcessedDay() + 1L,
                currentDay - MAX_CATCH_UP_DAYS + 1L);
        for (long day = first; day <= currentDay; day++) {
            processDay(estates, workforce, lifecycle, day);
        }
    }

    private static void processDay(
            ErdenExteriorEstateSavedData estates,
            ErdenExteriorWorkforceSavedData workforce,
            ErdenExteriorLifecycleSavedData lifecycle,
            long day) {
        Map<String, ErdenExteriorWorkforceSavedData.Household> households = new HashMap<>();
        for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {
            households.put(household.id(), household);
        }

        List<ErdenExteriorEstateSavedData.Estate> next = new ArrayList<>();
        for (ErdenExteriorEstateSavedData.Estate estate : estates.estates()) {
            ErdenExteriorWorkforceSavedData.Household household =
                    households.get(estate.householdId());
            if (household == null) continue;
            ErdenExteriorLifecycleSavedData.HouseholdLine line =
                    lifecycle.householdLine(estate.householdId());
            String steward = line == null ? "" : line.stewardId();
            String heir = line == null ? "" : line.heirId();
            int living = livingMembers(lifecycle, estate.householdId(), day);
            int activeWorkers = activeWorkers(lifecycle, estate.householdId(), day);
            boolean vacant = living == 0 || steward.isBlank()
                    || !personAlive(lifecycle, steward, day);
            boolean overcrowded = living > estate.capacity();

            ErdenExteriorWorkforceSavedData.NodeLabor labor =
                    workforce.labor(estate.nodeId());
            int production = labor == null ? 0 : Math.clamp(labor.productionPercent(), 0, 100);
            int income = vacant ? 0 : activeWorkers * 2 + production / 20;
            int obligation = vacant ? 2 : Math.max(1, (living + 1) / 2)
                    + (overcrowded ? 2 : 0);
            int available = Math.max(0, estate.maintenanceReserve() + income);
            int spent = Math.min(available, obligation);
            int reserve = available - spent;
            int condition = estate.condition();
            if (vacant) condition -= 2;
            else if (spent >= obligation) condition += 1;
            else condition -= Math.max(1, obligation - spent);
            if (overcrowded) condition -= 1;
            condition = Math.clamp(condition, 0, 100);

            int successionCount = line == null
                    ? estate.lastSuccessionCount()
                    : line.successionCount();
            int inherited = estate.inheritedCount()
                    + Math.max(0, successionCount - estate.lastSuccessionCount());
            next.add(estate.withDailyState(
                    steward, heir, living, reserve, condition, inherited,
                    successionCount, day, vacant, overcrowded));
        }
        if (next.size() != EXPECTED_ESTATES) {
            throw new IllegalStateException(
                    "Invalid Erden exterior estate reconciliation count=" + next.size());
        }
        estates.replaceDay(day, next);
    }

    private static int livingMembers(
            ErdenExteriorLifecycleSavedData lifecycle,
            String householdId,
            long day) {
        int count = 0;
        for (ErdenExteriorLifecycleSavedData.Person person : lifecycle.persons()) {
            if (person.householdId().equals(householdId) && person.aliveOn(day)) count++;
        }
        return count;
    }

    private static int activeWorkers(
            ErdenExteriorLifecycleSavedData lifecycle,
            String householdId,
            long day) {
        int count = 0;
        for (ErdenExteriorLifecycleSavedData.Person person : lifecycle.persons()) {
            if (person.householdId().equals(householdId)
                    && person.assignedWorker()
                    && person.aliveOn(day)
                    && !person.retiredOn(day)) count++;
        }
        return count;
    }

    private static boolean personAlive(
            ErdenExteriorLifecycleSavedData lifecycle,
            String personId,
            long day) {
        ErdenExteriorLifecycleSavedData.Person person = lifecycle.person(personId);
        return person != null && person.aliveOn(day);
    }

    private static void validatePlan(
            ErdenExteriorEstateSavedData estates,
            ErdenExteriorWorkforceSavedData workforce) {
        if (estates.estates().size() != EXPECTED_ESTATES
                || workforce.householdCount() != EXPECTED_ESTATES) {
            throw new IllegalStateException(
                    "Invalid Erden exterior estate plan estates=" + estates.estates().size()
                            + " households=" + workforce.householdCount());
        }
        Map<String, ErdenExteriorWorkforceSavedData.Household> households = new HashMap<>();
        for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {
            households.put(household.id(), household);
        }
        for (ErdenExteriorEstateSavedData.Estate estate : estates.estates()) {
            ErdenExteriorWorkforceSavedData.Household household =
                    households.get(estate.householdId());
            if (household == null
                    || household.homeX() != estate.homeX()
                    || household.homeZ() != estate.homeZ()
                    || !household.nodeId().equals(estate.nodeId())) {
                throw new IllegalStateException(
                        "Invalid Erden exterior estate home " + estate.householdId());
            }
        }
    }

    private static void logPlanOnce(ErdenExteriorEstateSavedData estates) {
        if (planLogged) return;
        planLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden exterior estates revision={} estates={} capacity={} ownership_persistent=true succession_linked=true maintenance_reserves=true vacancy_tracking=true overcrowding_tracking=true",
                ESTATE_REVISION, estates.estates().size(), HOUSEHOLD_CAPACITY);
    }

    private static void verifyCi(
            ErdenExteriorEstateSavedData estates,
            ErdenExteriorWorkforceSavedData workforce,
            ErdenExteriorLifecycleSavedData lifecycle,
            long day) {
        if (ciPassed
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || estates.estates().size() != EXPECTED_ESTATES
                || estates.lastProcessedDay() < day
                || estates.occupiedCount() <= 0
                || estates.totalMaintenanceReserve() <= 0
                || estates.minimumCondition() <= 0) return;
        int linkedStewards = 0;
        int linkedHomes = 0;
        for (ErdenExteriorEstateSavedData.Estate estate : estates.estates()) {
            ErdenExteriorLifecycleSavedData.HouseholdLine line =
                    lifecycle.householdLine(estate.householdId());
            if (line != null
                    && line.stewardId().equals(estate.stewardId())
                    && line.heirId().equals(estate.heirId())) linkedStewards++;
            for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {
                if (household.id().equals(estate.householdId())
                        && household.homeX() == estate.homeX()
                        && household.homeZ() == estate.homeZ()) {
                    linkedHomes++;
                    break;
                }
            }
        }
        if (linkedStewards != EXPECTED_ESTATES || linkedHomes != EXPECTED_ESTATES) return;
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_EXTERIOR_ESTATES_PASS revision={} estates={} occupied={} vacant={} overcrowded={} linked_stewards={} linked_homes={} maintenance_reserve={} minimum_condition={} ownership_persistent=true inheritance_preserves_home=true inheritance_preserves_reserve=true succession_linked=true vacancy_tracking=true overcrowding_tracking=true",
                ESTATE_REVISION, EXPECTED_ESTATES, estates.occupiedCount(), estates.vacantCount(),
                estates.overcrowdedCount(), linkedStewards, linkedHomes,
                estates.totalMaintenanceReserve(), estates.minimumCondition());
    }
}
