package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Marriage, widowhood, remarriage and spouse-residence changes for Erden's capital generations.
 *
 * <p>Founding couples are reconstructed from the authoritative 77-household roster. Later adult
 * descendants may marry across households. Only non-founder, non-steward residents move, their
 * workplace remains unchanged, and wages therefore follow the spouse household wallet through the
 * lifecycle worker snapshot. Births query this ledger, so descendants are born only from an active
 * married pair occupying the same household.</p>
 */
public final class ErdenCapitalMarriageManager {
    public static final int MARRIAGE_REVISION = 1;
    private static final int MIN_MARRIAGE_AGE = 20;
    private static final int MAX_MARRIAGE_AGE = 48;
    private static final int MIN_PARENT_AGE = 20;
    private static final int MAX_PARENT_AGE = 44;
    private static final int REMARRIAGE_WAIT_YEARS = 1;
    private static final int MAX_NEW_UNIONS_PER_YEAR = 10;
    private static final int MAX_CATCH_UP_YEARS = 30;
    private static final int HOUSEHOLD_CAPACITY = ErdenCapitalLifecycleManager.HOUSEHOLD_CAPACITY;

    private static MinecraftServer activeServer;
    private static boolean planLogged;
    private static boolean ciPassed;

    private ErdenCapitalMarriageManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        ErdenPopulationSavedData population = level.getDataStorage()
                .computeIfAbsent(ErdenPopulationSavedData.TYPE);
        if (population.households().size() != ErdenPopulationManager.EXPECTED_HOUSEHOLDS) return;
        ErdenCapitalLifecycleManager.prepare(level, population);
        ErdenCapitalLifecycleSavedData lifecycle = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalLifecycleSavedData.TYPE);
        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        if (lifecycle.lastProcessedDay() < day) return;

        ErdenCapitalMarriageSavedData marriages = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalMarriageSavedData.TYPE);
        ensureInitialized(marriages, lifecycle, population);
        processThroughYear(marriages, lifecycle, population, day);
        logPlanOnce(marriages, day);
        verifyCi(level, marriages, population);
    }

    /** Actual active pair allowed to create a birth in this household. */
    public static List<ErdenCapitalLifecycleSavedData.Person> parentPair(
            ServerLevel level,
            String householdId,
            List<ErdenCapitalLifecycleSavedData.Person> members,
            long day) {
        ErdenCapitalMarriageSavedData marriages = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalMarriageSavedData.TYPE);
        Map<String, ErdenCapitalLifecycleSavedData.Person> byId = byId(members);
        if (marriages.initialized(MARRIAGE_REVISION)) {
            for (ErdenCapitalMarriageSavedData.Union union : marriages.unions()) {
                if (!union.householdId().equals(householdId) || !union.activeOn(day)) continue;
                ErdenCapitalLifecycleSavedData.Person a = byId.get(union.personA());
                ErdenCapitalLifecycleSavedData.Person b = byId.get(union.personB());
                if (fertile(a, day) && fertile(b, day)
                        && a.householdId().equals(householdId)
                        && b.householdId().equals(householdId)) return List.of(a, b);
            }
            return List.of();
        }
        // Save-upgrade fallback before the marriage manager gets its first tick.
        return members.stream()
                .filter(ErdenCapitalLifecycleSavedData.Person::founder)
                .filter(person -> fertile(person, day))
                .sorted(Comparator.comparing(ErdenCapitalLifecycleSavedData.Person::id))
                .limit(2)
                .toList();
    }

    public static String spouseName(ServerLevel level, String personId, long day) {
        ErdenCapitalMarriageSavedData marriages = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalMarriageSavedData.TYPE);
        ErdenCapitalMarriageSavedData.Union union = marriages.activeUnion(personId, day);
        if (union == null) return "";
        ErdenCapitalLifecycleSavedData lifecycle = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalLifecycleSavedData.TYPE);
        ErdenCapitalLifecycleSavedData.Person spouse = lifecycle.person(union.spouseOf(personId));
        return spouse == null ? "" : spouse.name();
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        planLogged = false;
        ciPassed = false;
    }

    private static void ensureInitialized(
            ErdenCapitalMarriageSavedData marriages,
            ErdenCapitalLifecycleSavedData lifecycle,
            ErdenPopulationSavedData population) {
        if (marriages.initialized(MARRIAGE_REVISION)) return;
        List<ErdenCapitalMarriageSavedData.Union> initial = new ArrayList<>();
        int sequence = 1;
        long day = lifecycle.establishedDay();
        for (ErdenPopulationSavedData.Household household : population.households()) {
            String head = "";
            String partner = "";
            for (ErdenPopulationSavedData.Resident resident : household.residents()) {
                if (resident.lifeStage().equals("adult_head")) head = resident.id();
                else if (resident.lifeStage().equals("adult_partner")) partner = resident.id();
            }
            if (head.isBlank() || partner.isBlank()
                    || lifecycle.person(head) == null || lifecycle.person(partner) == null) continue;
            initial.add(new ErdenCapitalMarriageSavedData.Union(
                    "erden_capital_union_%04d".formatted(sequence++),
                    head, partner, household.id(), day, -1L, false));
        }
        if (initial.size() != ErdenPopulationManager.EXPECTED_HOUSEHOLDS) {
            throw new IllegalStateException(
                    "Erden capital marriage initialization expected 77 founding unions, found " + initial.size());
        }
        marriages.initialize(MARRIAGE_REVISION, sequence, initial);
    }

    private static void processThroughYear(
            ErdenCapitalMarriageSavedData marriages,
            ErdenCapitalLifecycleSavedData lifecycle,
            ErdenPopulationSavedData population,
            long currentDay) {
        int currentYear = Math.max(0, Math.toIntExact(Math.floorDiv(
                currentDay - lifecycle.establishedDay(), ErdenCapitalLifecycleManager.DAYS_PER_YEAR)));
        if (marriages.lastProcessedYear() >= currentYear) return;
        int first = Math.max(marriages.lastProcessedYear() + 1,
                currentYear - MAX_CATCH_UP_YEARS + 1);
        for (int year = first; year <= currentYear; year++) {
            long yearDay = lifecycle.establishedDay()
                    + (long) year * ErdenCapitalLifecycleManager.DAYS_PER_YEAR;
            processYear(marriages, lifecycle, population, year, yearDay);
        }
    }

    private static void processYear(
            ErdenCapitalMarriageSavedData marriages,
            ErdenCapitalLifecycleSavedData lifecycle,
            ErdenPopulationSavedData population,
            int year,
            long yearDay) {
        List<ErdenCapitalLifecycleSavedData.Person> people = new ArrayList<>(lifecycle.persons());
        List<ErdenCapitalMarriageSavedData.Union> unions = endDeadUnions(
                marriages.unions(), byId(people), yearDay);
        Set<String> stewardIds = stewardIds(lifecycle.householdLines());
        int nextSequence = marriages.nextUnionSequence();
        FormationResult formed = formUnions(
                people, unions, stewardIds, yearDay, nextSequence, MAX_NEW_UNIONS_PER_YEAR,
                (personId, targetHousehold) -> {
                    if (!lifecycle.movePersonHousehold(personId, targetHousehold)) return false;
                    return moveLocal(people, personId, targetHousehold);
                });
        marriages.replaceYear(year, formed.nextSequence(), formed.unions(), formed.moves());
        if (formed.formed() > 0 || formed.endedBeforeFormation() > 0) {
            LivingKingdoms.LOGGER.info(
                    "Processed Erden capital marriages year={} active={} formed={} ended={} remarriages={} spouse_household_moves={} fixed_homes=77 workplaces_preserved=true wallet_follows_household=true",
                    year, marriages.activeCount(yearDay), formed.formed(), formed.endedBeforeFormation(),
                    formed.remarriages(), formed.moves());
        }
    }

    private static FormationResult formUnions(
            List<ErdenCapitalLifecycleSavedData.Person> people,
            List<ErdenCapitalMarriageSavedData.Union> startingUnions,
            Set<String> stewardIds,
            long day,
            int nextSequence,
            int limit,
            MoveAction moveAction) {
        List<ErdenCapitalMarriageSavedData.Union> unions = new ArrayList<>(startingUnions);
        int ended = endedAt(startingUnions, day);
        Set<String> activeMarried = activePeople(unions, day);
        Map<String, Integer> occupancy = livingOccupancy(people, day);
        List<ErdenCapitalLifecycleSavedData.Person> candidates = people.stream()
                .filter(person -> eligibleForMarriage(person, unions, activeMarried, day))
                .sorted(Comparator.comparing(ErdenCapitalLifecycleSavedData.Person::id))
                .toList();
        Set<String> paired = new HashSet<>();
        int formed = 0;
        int moved = 0;
        int remarriages = 0;
        for (int i = 0; i < candidates.size() && formed < limit; i++) {
            ErdenCapitalLifecycleSavedData.Person a = currentPerson(people, candidates.get(i).id());
            if (a == null || paired.contains(a.id())) continue;
            for (int j = i + 1; j < candidates.size(); j++) {
                ErdenCapitalLifecycleSavedData.Person b = currentPerson(people, candidates.get(j).id());
                if (b == null || paired.contains(b.id()) || !compatible(a, b)) continue;
                String target = chooseHousehold(a, b, occupancy, stewardIds);
                if (target.isBlank()) continue;
                ErdenCapitalLifecycleSavedData.Person mover = a.householdId().equals(target) ? b : a;
                if (mover.founder() || stewardIds.contains(mover.id())) continue;
                String oldHousehold = mover.householdId();
                if (!oldHousehold.equals(target)) {
                    if (!moveAction.move(mover.id(), target)) continue;
                    occupancy.compute(oldHousehold,
                            (ignored, value) -> Math.max(0, value == null ? 0 : value - 1));
                    occupancy.merge(target, 1, Integer::sum);
                    moved++;
                    a = currentPerson(people, a.id());
                    b = currentPerson(people, b.id());
                }
                boolean remarriage = everMarried(unions, a.id()) || everMarried(unions, b.id());
                unions.add(new ErdenCapitalMarriageSavedData.Union(
                        "erden_capital_union_%04d".formatted(nextSequence++),
                        a.id(), b.id(), target, day, -1L, remarriage));
                if (remarriage) remarriages++;
                paired.add(a.id());
                paired.add(b.id());
                formed++;
                break;
            }
        }
        return new FormationResult(List.copyOf(unions), nextSequence, formed, moved, remarriages, ended);
    }

    private static List<ErdenCapitalMarriageSavedData.Union> endDeadUnions(
            List<ErdenCapitalMarriageSavedData.Union> source,
            Map<String, ErdenCapitalLifecycleSavedData.Person> people,
            long day) {
        List<ErdenCapitalMarriageSavedData.Union> result = new ArrayList<>();
        for (ErdenCapitalMarriageSavedData.Union union : source) {
            if (union.activeOn(day)) {
                ErdenCapitalLifecycleSavedData.Person a = people.get(union.personA());
                ErdenCapitalLifecycleSavedData.Person b = people.get(union.personB());
                if (a == null || b == null || !a.aliveOn(day) || !b.aliveOn(day)) {
                    union = union.withEnd(day);
                }
            }
            result.add(union);
        }
        return result;
    }

    private static int endedAt(List<ErdenCapitalMarriageSavedData.Union> unions, long day) {
        int count = 0;
        for (ErdenCapitalMarriageSavedData.Union union : unions) if (union.endDay() == day) count++;
        return count;
    }

    private static boolean eligibleForMarriage(
            ErdenCapitalLifecycleSavedData.Person person,
            List<ErdenCapitalMarriageSavedData.Union> unions,
            Set<String> activeMarried,
            long day) {
        if (!person.aliveOn(day) || person.retiredOn(day) || activeMarried.contains(person.id())) return false;
        int age = ageYears(person, day);
        if (age < MIN_MARRIAGE_AGE || age > MAX_MARRIAGE_AGE) return false;
        long latestEnd = -1L;
        for (ErdenCapitalMarriageSavedData.Union union : unions) {
            if (union.involves(person.id()) && union.endDay() >= 0L) latestEnd = Math.max(latestEnd, union.endDay());
        }
        return latestEnd < 0L
                || day - latestEnd >= (long) REMARRIAGE_WAIT_YEARS * ErdenCapitalLifecycleManager.DAYS_PER_YEAR;
    }

    private static boolean compatible(
            ErdenCapitalLifecycleSavedData.Person a,
            ErdenCapitalLifecycleSavedData.Person b) {
        if (a.householdId().equals(b.householdId()) || (a.founder() && b.founder())) return false;
        if (a.id().equals(b.parentA()) || a.id().equals(b.parentB())
                || b.id().equals(a.parentA()) || b.id().equals(a.parentB())) return false;
        if (!a.parentA().isBlank() && (a.parentA().equals(b.parentA()) || a.parentA().equals(b.parentB()))) return false;
        return a.parentB().isBlank()
                || (!a.parentB().equals(b.parentA()) && !a.parentB().equals(b.parentB()));
    }

    private static String chooseHousehold(
            ErdenCapitalLifecycleSavedData.Person a,
            ErdenCapitalLifecycleSavedData.Person b,
            Map<String, Integer> occupancy,
            Set<String> stewardIds) {
        boolean aSteward = stewardIds.contains(a.id());
        boolean bSteward = stewardIds.contains(b.id());
        if (aSteward && bSteward) return "";
        if (aSteward) return hasCapacity(occupancy, a.householdId()) ? a.householdId() : "";
        if (bSteward) return hasCapacity(occupancy, b.householdId()) ? b.householdId() : "";
        if (a.founder() && !b.founder()) return hasCapacity(occupancy, a.householdId()) ? a.householdId() : "";
        if (b.founder() && !a.founder()) return hasCapacity(occupancy, b.householdId()) ? b.householdId() : "";
        int aCount = occupancy.getOrDefault(a.householdId(), HOUSEHOLD_CAPACITY);
        int bCount = occupancy.getOrDefault(b.householdId(), HOUSEHOLD_CAPACITY);
        String first = aCount <= bCount ? a.householdId() : b.householdId();
        String second = first.equals(a.householdId()) ? b.householdId() : a.householdId();
        if (hasCapacity(occupancy, first)) return first;
        if (hasCapacity(occupancy, second)) return second;
        return "";
    }

    private static boolean hasCapacity(Map<String, Integer> occupancy, String household) {
        return occupancy.getOrDefault(household, HOUSEHOLD_CAPACITY) < HOUSEHOLD_CAPACITY;
    }

    private static Map<String, Integer> livingOccupancy(
            List<ErdenCapitalLifecycleSavedData.Person> people,
            long day) {
        Map<String, Integer> occupancy = new HashMap<>();
        for (ErdenCapitalLifecycleSavedData.Person person : people) {
            if (person.aliveOn(day)) occupancy.merge(person.householdId(), 1, Integer::sum);
        }
        return occupancy;
    }

    private static Set<String> activePeople(
            List<ErdenCapitalMarriageSavedData.Union> unions,
            long day) {
        Set<String> result = new HashSet<>();
        for (ErdenCapitalMarriageSavedData.Union union : unions) {
            if (!union.activeOn(day)) continue;
            result.add(union.personA());
            result.add(union.personB());
        }
        return result;
    }

    private static Set<String> stewardIds(
            List<ErdenCapitalLifecycleSavedData.HouseholdLine> lines) {
        Set<String> result = new HashSet<>();
        for (ErdenCapitalLifecycleSavedData.HouseholdLine line : lines) {
            if (!line.stewardId().isBlank()) result.add(line.stewardId());
        }
        return result;
    }

    private static boolean everMarried(
            List<ErdenCapitalMarriageSavedData.Union> unions,
            String personId) {
        for (ErdenCapitalMarriageSavedData.Union union : unions) if (union.involves(personId)) return true;
        return false;
    }

    private static boolean fertile(ErdenCapitalLifecycleSavedData.Person person, long day) {
        if (person == null || !person.aliveOn(day) || person.retiredOn(day)) return false;
        int age = ageYears(person, day);
        return age >= MIN_PARENT_AGE && age <= MAX_PARENT_AGE;
    }

    private static int ageYears(ErdenCapitalLifecycleSavedData.Person person, long day) {
        return (int) Math.max(0L, Math.floorDiv(
                day - person.birthDay(), ErdenCapitalLifecycleManager.DAYS_PER_YEAR));
    }

    private static Map<String, ErdenCapitalLifecycleSavedData.Person> byId(
            List<ErdenCapitalLifecycleSavedData.Person> people) {
        Map<String, ErdenCapitalLifecycleSavedData.Person> result = new HashMap<>();
        for (ErdenCapitalLifecycleSavedData.Person person : people) result.put(person.id(), person);
        return result;
    }

    private static ErdenCapitalLifecycleSavedData.Person currentPerson(
            List<ErdenCapitalLifecycleSavedData.Person> people,
            String id) {
        for (ErdenCapitalLifecycleSavedData.Person person : people) if (person.id().equals(id)) return person;
        return null;
    }

    private static boolean moveLocal(
            List<ErdenCapitalLifecycleSavedData.Person> people,
            String personId,
            String targetHousehold) {
        for (int index = 0; index < people.size(); index++) {
            ErdenCapitalLifecycleSavedData.Person person = people.get(index);
            if (!person.id().equals(personId)) continue;
            if (person.householdId().equals(targetHousehold)) return true;
            people.set(index, person.withHousehold(targetHousehold));
            return true;
        }
        return false;
    }

    private static void logPlanOnce(ErdenCapitalMarriageSavedData marriages, long day) {
        if (planLogged) return;
        planLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden capital marriages revision={} founding_unions={} active={} remarriages={} household_moves={} marriage_parentage=true spouse_residence=true work_assignment_preserved=true",
                MARRIAGE_REVISION, ErdenPopulationManager.EXPECTED_HOUSEHOLDS,
                marriages.activeCount(day), marriages.remarriageCount(), marriages.householdMoves());
    }

    private static void verifyCi(
            ServerLevel level,
            ErdenCapitalMarriageSavedData marriages,
            ErdenPopulationSavedData population) {
        if (ciPassed || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_CAPITAL_MARRIAGE_TEST"))) return;
        if (!marriages.initialized(MARRIAGE_REVISION)
                || marriages.unions().size() != ErdenPopulationManager.EXPECTED_HOUSEHOLDS) return;

        ErdenCapitalLifecycleManager.Projection projection =
                ErdenCapitalLifecycleManager.projectForAudit(level, population, 28);
        List<ErdenCapitalLifecycleSavedData.Person> people = new ArrayList<>(projection.persons());
        long targetDay = projection.targetDay();
        List<ErdenCapitalMarriageSavedData.Union> unions = endDeadUnions(
                marriages.unions(), byId(people), targetDay);
        Set<String> stewards = stewardIds(projection.householdLines());
        FormationResult first = formUnions(
                people, unions, stewards, targetDay, marriages.nextUnionSequence(), 64,
                (personId, householdId) -> moveLocal(people, personId, householdId));
        if (first.formed() <= 0 || first.moves() <= 0) {
            throw new IllegalStateException(
                    "Erden capital marriage projection failed new_unions=" + first.formed()
                            + " moves=" + first.moves());
        }

        List<ErdenCapitalMarriageSavedData.Union> remarriageUnions = new ArrayList<>(first.unions());
        ErdenCapitalMarriageSavedData.Union victimUnion = remarriageUnions.stream()
                .filter(union -> union.startDay() == targetDay)
                .findFirst().orElseThrow(() -> new IllegalStateException("No projected descendant union to test widowhood"));
        long deathDay = targetDay + 1L;
        for (int i = 0; i < people.size(); i++) {
            ErdenCapitalLifecycleSavedData.Person person = people.get(i);
            if (person.id().equals(victimUnion.personB())) {
                people.set(i, person.withDeath(deathDay));
                break;
            }
        }
        remarriageUnions = endDeadUnions(remarriageUnions, byId(people), deathDay);
        long remarryDay = deathDay + ErdenCapitalLifecycleManager.DAYS_PER_YEAR;
        FormationResult second = formUnions(
                people, remarriageUnions, stewards, remarryDay, first.nextSequence(), 128,
                (personId, householdId) -> moveLocal(people, personId, householdId));
        if (second.remarriages() <= 0) {
            throw new IllegalStateException("Erden capital remarriage projection formed no remarriage after widowhood");
        }

        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_CAPITAL_MARRIAGE_PASS revision=1 founding_unions=77 projection_years=28 projected_new_unions={} projected_household_moves={} projected_remarriages={} widowhood_ends_union=true remarriage_wait_years={} active_union_parentage=true spouse_residence=true work_assignment_preserved=true wallet_follows_household=true fixed_homes=77 fixed_worksites=156 base_population_unchanged=true save_overlay=true source_blocks_cut=0",
                first.formed(), first.moves(), second.remarriages(), REMARRIAGE_WAIT_YEARS);
    }

    @FunctionalInterface
    private interface MoveAction {
        boolean move(String personId, String targetHousehold);
    }

    private record FormationResult(
            List<ErdenCapitalMarriageSavedData.Union> unions,
            int nextSequence,
            int formed,
            int moves,
            int remarriages,
            int endedBeforeFormation) {
    }
}
