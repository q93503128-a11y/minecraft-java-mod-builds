package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persistent marriage/remarriage simulation for Erden's exterior families.
 *
 * <p>Founding {@code adult_head + adult_partner} pairs become the initial unions without rewriting
 * the workforce save. Later unions may move only non-founder lifecycle residents into a spouse's
 * already-authored home; their workplace/node stays unchanged. This keeps a marriage from silently
 * changing employment while still making residence, estate occupancy and descendant spawning
 * follow the new household.</p>
 */
@EventBusSubscriber(modid = LivingKingdoms.MOD_ID)
public final class ErdenExteriorMarriageManager {
    public static final int MARRIAGE_REVISION = 1;
    private static final int MIN_MARRIAGE_AGE = 20;
    private static final int MAX_MARRIAGE_AGE = 48;
    private static final int MIN_PARENT_AGE = 20;
    private static final int MAX_PARENT_AGE = 44;
    private static final int REMARRIAGE_WAIT_YEARS = 1;
    private static final int MAX_NEW_UNIONS_PER_YEAR = 8;
    private static final int MAX_CATCH_UP_YEARS = 20;
    private static final int HOUSEHOLD_CAPACITY = ErdenExteriorEstateManager.HOUSEHOLD_CAPACITY;

    private static MinecraftServer activeServer;
    private static boolean planLogged;
    private static boolean ciPassed;

    private ErdenExteriorMarriageManager() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        ErdenExteriorLifecycleSavedData lifecycle = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorLifecycleSavedData.TYPE);
        ErdenExteriorWorkforceSavedData workforce = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorWorkforceSavedData.TYPE);
        if (!lifecycle.initialized(
                ErdenExteriorLifecycleManager.LIFECYCLE_REVISION,
                ErdenExteriorWorkforceManager.EXPECTED_RESIDENTS,
                ErdenExteriorWorkforceManager.EXPECTED_HOUSEHOLDS)
                || lifecycle.lastProcessedDay() < day
                || !workforce.hasPopulation(
                ErdenExteriorWorkforceManager.WORKFORCE_REVISION,
                ErdenExteriorWorkforceManager.EXPECTED_HOUSEHOLDS)) return;

        ErdenExteriorMarriageSavedData marriages = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorMarriageSavedData.TYPE);
        ensureInitialized(marriages, lifecycle, workforce);
        processThroughYear(marriages, lifecycle, workforce, day);
        logPlanOnce(marriages, day);
        verifyCi(marriages, lifecycle, workforce, day);
    }

    /** Return the actual active married pair allowed to create a birth in this household. */
    public static List<ErdenExteriorLifecycleSavedData.Person> parentPair(
            ServerLevel level,
            String householdId,
            List<ErdenExteriorLifecycleSavedData.Person> members,
            long day) {
        ErdenExteriorMarriageSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorMarriageSavedData.TYPE);
        if (!data.initialized(MARRIAGE_REVISION)) return List.of();
        Map<String, ErdenExteriorLifecycleSavedData.Person> byId = new HashMap<>();
        for (ErdenExteriorLifecycleSavedData.Person member : members) byId.put(member.id(), member);
        for (ErdenExteriorMarriageSavedData.Union union : data.unions()) {
            if (!union.householdId().equals(householdId) || !union.activeOn(day)) continue;
            ErdenExteriorLifecycleSavedData.Person a = byId.get(union.personA());
            ErdenExteriorLifecycleSavedData.Person b = byId.get(union.personB());
            if (fertile(a, day) && fertile(b, day)) return List.of(a, b);
        }
        return List.of();
    }

    public static String spouseName(ServerLevel level, String personId, long day) {
        ErdenExteriorMarriageSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorMarriageSavedData.TYPE);
        ErdenExteriorMarriageSavedData.Union union = data.activeUnion(personId, day);
        if (union == null) return "";
        ErdenExteriorLifecycleSavedData lifecycle = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorLifecycleSavedData.TYPE);
        ErdenExteriorLifecycleSavedData.Person spouse = lifecycle.person(union.spouseOf(personId));
        return spouse == null ? "" : spouse.name();
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        planLogged = false;
        ciPassed = false;
    }

    private static void ensureInitialized(
            ErdenExteriorMarriageSavedData marriages,
            ErdenExteriorLifecycleSavedData lifecycle,
            ErdenExteriorWorkforceSavedData workforce) {
        if (marriages.initialized(MARRIAGE_REVISION)) return;
        List<ErdenExteriorMarriageSavedData.Union> initial = new ArrayList<>();
        int sequence = 1;
        long day = lifecycle.establishedDay();
        for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {
            String head = "";
            String partner = "";
            for (ErdenExteriorWorkforceSavedData.Resident resident : household.residents()) {
                if (resident.lifeStage().equals("adult_head")) head = resident.id();
                else if (resident.lifeStage().equals("adult_partner")) partner = resident.id();
            }
            if (head.isBlank() || partner.isBlank()
                    || lifecycle.person(head) == null || lifecycle.person(partner) == null) continue;
            initial.add(new ErdenExteriorMarriageSavedData.Union(
                    "erden_union_%04d".formatted(sequence++),
                    head, partner, household.id(), day, -1L, false));
        }
        marriages.initialize(MARRIAGE_REVISION, 0, sequence, initial);
        if (initial.isEmpty()) {
            throw new IllegalStateException("Erden marriage initialization found no founding couples");
        }
    }

    private static void processThroughYear(
            ErdenExteriorMarriageSavedData marriages,
            ErdenExteriorLifecycleSavedData lifecycle,
            ErdenExteriorWorkforceSavedData workforce,
            long currentDay) {
        int currentYear = Math.max(0, Math.toIntExact(Math.floorDiv(
                currentDay - lifecycle.establishedDay(), ErdenExteriorLifecycleManager.DAYS_PER_YEAR)));
        if (marriages.lastProcessedYear() >= currentYear) return;
        int first = Math.max(marriages.lastProcessedYear() + 1,
                currentYear - MAX_CATCH_UP_YEARS + 1);
        for (int year = first; year <= currentYear; year++) {
            long yearDay = lifecycle.establishedDay()
                    + (long) year * ErdenExteriorLifecycleManager.DAYS_PER_YEAR;
            processYear(marriages, lifecycle, workforce, year, yearDay);
        }
    }

    private static void processYear(
            ErdenExteriorMarriageSavedData marriages,
            ErdenExteriorLifecycleSavedData lifecycle,
            ErdenExteriorWorkforceSavedData workforce,
            int year,
            long yearDay) {
        List<ErdenExteriorMarriageSavedData.Union> unions = new ArrayList<>();
        int ended = 0;
        for (ErdenExteriorMarriageSavedData.Union union : marriages.unions()) {
            if (union.activeOn(yearDay)) {
                ErdenExteriorLifecycleSavedData.Person a = lifecycle.person(union.personA());
                ErdenExteriorLifecycleSavedData.Person b = lifecycle.person(union.personB());
                if (a == null || b == null || !a.aliveOn(yearDay) || !b.aliveOn(yearDay)) {
                    union = union.withEnd(yearDay);
                    ended++;
                }
            }
            unions.add(union);
        }

        Set<String> householdIds = new HashSet<>();
        for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {
            householdIds.add(household.id());
        }
        Map<String, Integer> occupancy = livingOccupancy(lifecycle, householdIds, yearDay);
        Set<String> activeMarried = activePeople(unions, yearDay);
        List<ErdenExteriorLifecycleSavedData.Person> candidates = new ArrayList<>();
        for (ErdenExteriorLifecycleSavedData.Person person : lifecycle.persons()) {
            if (!eligibleForMarriage(person, unions, activeMarried, yearDay)) continue;
            if (!householdIds.contains(person.householdId())) continue;
            candidates.add(person);
        }
        candidates.sort(Comparator.comparing(ErdenExteriorLifecycleSavedData.Person::id));

        Set<String> paired = new HashSet<>();
        int formed = 0;
        int moved = 0;
        int remarriages = 0;
        int nextSequence = marriages.nextUnionSequence();
        for (int i = 0; i < candidates.size() && formed < MAX_NEW_UNIONS_PER_YEAR; i++) {
            ErdenExteriorLifecycleSavedData.Person a = candidates.get(i);
            if (paired.contains(a.id())) continue;
            for (int j = i + 1; j < candidates.size(); j++) {
                ErdenExteriorLifecycleSavedData.Person b = candidates.get(j);
                if (paired.contains(b.id()) || !compatible(a, b)) continue;
                String target = chooseHousehold(a, b, occupancy);
                if (target.isBlank()) continue;
                ErdenExteriorLifecycleSavedData.Person mover =
                        a.householdId().equals(target) ? b : a;
                if (mover.founder()) continue;
                if (!mover.householdId().equals(target)) {
                    if (!lifecycle.movePersonHousehold(mover.id(), target)) continue;
                    occupancy.compute(mover.householdId(), (ignored, value) -> Math.max(0, value == null ? 0 : value - 1));
                    occupancy.merge(target, 1, Integer::sum);
                    moved++;
                }
                boolean remarriage = everMarried(unions, a.id()) || everMarried(unions, b.id());
                unions.add(new ErdenExteriorMarriageSavedData.Union(
                        "erden_union_%04d".formatted(nextSequence++),
                        a.id(), b.id(), target, yearDay, -1L, remarriage));
                if (remarriage) remarriages++;
                paired.add(a.id());
                paired.add(b.id());
                formed++;
                break;
            }
        }

        marriages.replaceYear(year, nextSequence, unions);
        if (formed > 0 || ended > 0) {
            LivingKingdoms.LOGGER.info(
                    "Processed Erden marriages year={} active={} formed={} ended={} remarriages={} spouse_household_moves={} founder_workplaces_preserved=true household_capacity={}",
                    year, marriages.activeCount(yearDay), formed, ended, remarriages, moved, HOUSEHOLD_CAPACITY);
        }
    }

    private static boolean eligibleForMarriage(
            ErdenExteriorLifecycleSavedData.Person person,
            List<ErdenExteriorMarriageSavedData.Union> unions,
            Set<String> activeMarried,
            long day) {
        if (!person.aliveOn(day) || person.retiredOn(day) || activeMarried.contains(person.id())) return false;
        int age = ageYears(person, day);
        if (age < MIN_MARRIAGE_AGE || age > MAX_MARRIAGE_AGE) return false;
        long latestEnd = -1L;
        for (ErdenExteriorMarriageSavedData.Union union : unions) {
            if (union.involves(person.id()) && union.endDay() >= 0L) {
                latestEnd = Math.max(latestEnd, union.endDay());
            }
        }
        return latestEnd < 0L
                || day - latestEnd >= (long) REMARRIAGE_WAIT_YEARS * ErdenExteriorLifecycleManager.DAYS_PER_YEAR;
    }

    private static boolean compatible(
            ErdenExteriorLifecycleSavedData.Person a,
            ErdenExteriorLifecycleSavedData.Person b) {
        if (a.householdId().equals(b.householdId()) || (a.founder() && b.founder())) return false;
        if (a.id().equals(b.parentA()) || a.id().equals(b.parentB())
                || b.id().equals(a.parentA()) || b.id().equals(a.parentB())) return false;
        if (!a.parentA().isBlank() && (a.parentA().equals(b.parentA()) || a.parentA().equals(b.parentB()))) return false;
        return a.parentB().isBlank()
                || (!a.parentB().equals(b.parentA()) && !a.parentB().equals(b.parentB()));
    }

    private static String chooseHousehold(
            ErdenExteriorLifecycleSavedData.Person a,
            ErdenExteriorLifecycleSavedData.Person b,
            Map<String, Integer> occupancy) {
        if (a.founder() && !b.founder()) {
            return occupancy.getOrDefault(a.householdId(), HOUSEHOLD_CAPACITY) < HOUSEHOLD_CAPACITY
                    ? a.householdId() : "";
        }
        if (b.founder() && !a.founder()) {
            return occupancy.getOrDefault(b.householdId(), HOUSEHOLD_CAPACITY) < HOUSEHOLD_CAPACITY
                    ? b.householdId() : "";
        }
        int aCount = occupancy.getOrDefault(a.householdId(), HOUSEHOLD_CAPACITY);
        int bCount = occupancy.getOrDefault(b.householdId(), HOUSEHOLD_CAPACITY);
        String first = aCount <= bCount ? a.householdId() : b.householdId();
        String second = first.equals(a.householdId()) ? b.householdId() : a.householdId();
        if (occupancy.getOrDefault(first, HOUSEHOLD_CAPACITY) < HOUSEHOLD_CAPACITY) return first;
        if (occupancy.getOrDefault(second, HOUSEHOLD_CAPACITY) < HOUSEHOLD_CAPACITY) return second;
        return "";
    }

    private static Map<String, Integer> livingOccupancy(
            ErdenExteriorLifecycleSavedData lifecycle,
            Set<String> householdIds,
            long day) {
        Map<String, Integer> occupancy = new HashMap<>();
        for (String id : householdIds) occupancy.put(id, 0);
        for (ErdenExteriorLifecycleSavedData.Person person : lifecycle.persons()) {
            if (person.aliveOn(day) && householdIds.contains(person.householdId())) {
                occupancy.merge(person.householdId(), 1, Integer::sum);
            }
        }
        return occupancy;
    }

    private static Set<String> activePeople(
            List<ErdenExteriorMarriageSavedData.Union> unions,
            long day) {
        Set<String> result = new HashSet<>();
        for (ErdenExteriorMarriageSavedData.Union union : unions) {
            if (!union.activeOn(day)) continue;
            result.add(union.personA());
            result.add(union.personB());
        }
        return result;
    }

    private static boolean everMarried(
            List<ErdenExteriorMarriageSavedData.Union> unions,
            String personId) {
        for (ErdenExteriorMarriageSavedData.Union union : unions) {
            if (union.involves(personId)) return true;
        }
        return false;
    }

    private static boolean fertile(ErdenExteriorLifecycleSavedData.Person person, long day) {
        if (person == null || !person.aliveOn(day) || person.retiredOn(day)) return false;
        int age = ageYears(person, day);
        return age >= MIN_PARENT_AGE && age <= MAX_PARENT_AGE;
    }

    private static int ageYears(ErdenExteriorLifecycleSavedData.Person person, long day) {
        return Math.max(0, Math.toIntExact(Math.floorDiv(
                day - person.birthDay(), ErdenExteriorLifecycleManager.DAYS_PER_YEAR)));
    }

    private static void logPlanOnce(ErdenExteriorMarriageSavedData marriages, long day) {
        if (planLogged) return;
        planLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden exterior marriages revision={} founding_unions={} active={} persistent=true remarriage=true spouse_household_relocation=true founder_workplaces_preserved=true spouse_based_births=true",
                MARRIAGE_REVISION, marriages.unions().size(), marriages.activeCount(day));
    }

    private static void verifyCi(
            ErdenExteriorMarriageSavedData marriages,
            ErdenExteriorLifecycleSavedData lifecycle,
            ErdenExteriorWorkforceSavedData workforce,
            long day) {
        if (ciPassed
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || marriages.unions().isEmpty()) return;
        int validFounding = 0;
        for (ErdenExteriorMarriageSavedData.Union union : marriages.unions()) {
            if (union.startDay() != lifecycle.establishedDay()) continue;
            ErdenExteriorLifecycleSavedData.Person a = lifecycle.person(union.personA());
            ErdenExteriorLifecycleSavedData.Person b = lifecycle.person(union.personB());
            if (a != null && b != null
                    && a.founder() && b.founder()
                    && a.householdId().equals(union.householdId())
                    && b.householdId().equals(union.householdId())) validFounding++;
        }
        int expectedFounding = 0;
        for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {
            boolean head = false;
            boolean partner = false;
            for (ErdenExteriorWorkforceSavedData.Resident resident : household.residents()) {
                head |= resident.lifeStage().equals("adult_head");
                partner |= resident.lifeStage().equals("adult_partner");
            }
            if (head && partner) expectedFounding++;
        }
        if (expectedFounding <= 0 || validFounding != expectedFounding) return;
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_EXTERIOR_MARRIAGE_PASS revision={} founding_unions={} expected_founders={} persistent_unions=true death_closes_union=true remarriage_wait_years={} nonfounder_household_moves=true founder_workplaces_preserved=true household_capacity={} spouse_based_births=true",
                MARRIAGE_REVISION, validFounding, expectedFounding, REMARRIAGE_WAIT_YEARS, HOUSEHOLD_CAPACITY);
    }
}
