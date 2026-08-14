package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
 * Vacancy, independent-household, rent and inward-migration market for Erden's fixed 77 homes.
 *
 * <p>No synthetic 78th tenement or wallet is created. When a lineage leaves a real tenement empty,
 * title reverts to the crown. A married descendant couple may lease that exact residence and become
 * the new household occupying the existing civic wallet slot. The previous wallet's cash is swept
 * into the unclaimed-estate reserve; at most ten coins are returned as a relocation grant, so no
 * money is duplicated. Rent is then collected from that same wallet after the city economy runs.
 * Homes vacant for at least two years may admit one migrant couple per year, keeping the capital
 * populated without force-spawning a permanent citywide entity population.</p>
 */
public final class ErdenCapitalHousingManager {
    public static final int HOUSING_REVISION = 1;
    private static final int EXPECTED_RESIDENCES = ErdenPopulationManager.EXPECTED_HOUSEHOLDS;
    private static final int MAX_CATCH_UP_DAYS = 30;
    private static final int MIN_INDEPENDENCE_AGE = 22;
    private static final int MIN_MARRIAGE_YEARS_BEFORE_INDEPENDENCE = 1;
    private static final int MAX_INDEPENDENT_LETTINGS_PER_YEAR = 3;
    private static final int MAX_MIGRANT_LETTINGS_PER_YEAR = 1;
    private static final int MIGRATION_VACANCY_YEARS = 2;
    private static final long MAX_RELOCATION_GRANT = 10L;

    private static final List<String> MIGRANT_HEADS = List.of(
            "알", "베", "카", "델", "에", "파", "가", "헤", "이", "조", "라", "메", "노", "오", "사", "테"
    );
    private static final List<String> MIGRANT_TAILS = List.of(
            "렌", "리아", "몬", "델", "나", "빈", "로", "엘", "안", "리스", "아", "온", "르", "엔"
    );

    private static MinecraftServer activeServer;
    private static boolean planLogged;
    private static boolean ciPassed;

    private ErdenCapitalHousingManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        ErdenPopulationSavedData population = level.getDataStorage()
                .computeIfAbsent(ErdenPopulationSavedData.TYPE);
        if (population.households().size() != EXPECTED_RESIDENCES) return;
        ErdenCapitalLifecycleManager.prepare(level, population);
        ErdenCapitalLifecycleSavedData lifecycle = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalLifecycleSavedData.TYPE);
        ErdenCapitalMarriageSavedData marriages = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalMarriageSavedData.TYPE);
        ErdenPhysicalEconomySavedData economy = level.getDataStorage()
                .computeIfAbsent(ErdenPhysicalEconomySavedData.TYPE);
        if (!marriages.initialized(ErdenCapitalMarriageManager.MARRIAGE_REVISION)
                || !economy.hasEconomy(
                ErdenAuthoritativeEconomyManager.ECONOMY_REVISION,
                ErdenAuthoritativeEconomyManager.EXPECTED_SITES,
                ErdenAuthoritativeEconomyManager.EXPECTED_WALLETS)) return;

        long currentDay = Math.floorDiv(level.getGameTime(), 24_000L);
        ErdenCapitalHousingSavedData housing = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalHousingSavedData.TYPE);
        if (!housing.initialized(HOUSING_REVISION, EXPECTED_RESIDENCES)) {
            initialize(housing, population, currentDay);
        }
        processThroughDay(level, population, lifecycle, marriages, economy, housing, currentDay);
        logPlanOnce(housing);
        verifyCi(level, population, economy, housing);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        planLogged = false;
        ciPassed = false;
    }

    private static void initialize(
            ErdenCapitalHousingSavedData housing,
            ErdenPopulationSavedData population,
            long day) {
        List<ErdenCapitalHousingSavedData.Residence> residences = population.households().stream()
                .sorted(Comparator.comparing(ErdenPopulationSavedData.Household::id))
                .map(household -> new ErdenCapitalHousingSavedData.Residence(
                        household.id(), household.homeX(), household.homeZ(),
                        "owner_occupied", "", -1L, day - 1L,
                        0, 0L, 0L, 0L, 0))
                .toList();
        housing.initialize(HOUSING_REVISION, day, residences);
    }

    private static void processThroughDay(
            ServerLevel level,
            ErdenPopulationSavedData population,
            ErdenCapitalLifecycleSavedData lifecycle,
            ErdenCapitalMarriageSavedData marriages,
            ErdenPhysicalEconomySavedData economy,
            ErdenCapitalHousingSavedData housing,
            long currentDay) {
        if (housing.lastProcessedDay() >= currentDay) return;
        long first = housing.lastProcessedDay() < 0L
                ? currentDay
                : Math.max(housing.lastProcessedDay() + 1L, currentDay - MAX_CATCH_UP_DAYS + 1L);
        for (long day = first; day <= currentDay; day++) {
            HousingDay result = processDay(
                    population, lifecycle, marriages, economy,
                    housing.residences(), housing.nextMigrantSequence(), day);
            housing.replaceDay(
                    day, result.residences(), result.rentPaid(), result.estateSwept(),
                    result.independentLettings(), result.migrantLettings(), result.nextMigrantSequence());
            if (result.independentLettings() > 0 || result.migrantLettings() > 0 || day % 7L == 0L) {
                LivingKingdoms.LOGGER.info(
                        "Processed Erden capital housing day={} vacant={} leased={} independent_lettings={} migrant_lettings={} rent_paid={} estate_swept={} crown_rent_reserve={} unclaimed_estate_reserve={} fixed_residences=77 fixed_wallet_slots=77",
                        day, housing.vacantCount(), housing.leasedCount(),
                        result.independentLettings(), result.migrantLettings(),
                        result.rentPaid(), result.estateSwept(),
                        housing.crownRentReserve(), housing.unclaimedEstateReserve());
            }
        }
    }

    private static HousingDay processDay(
            ErdenPopulationSavedData population,
            ErdenCapitalLifecycleSavedData lifecycle,
            ErdenCapitalMarriageSavedData marriages,
            ErdenPhysicalEconomySavedData economy,
            List<ErdenCapitalHousingSavedData.Residence> sourceResidences,
            int nextMigrantSequence,
            long day) {
        List<ErdenCapitalHousingSavedData.Residence> residences = new ArrayList<>(sourceResidences);
        Map<String, Integer> occupancy = livingOccupancy(lifecycle.persons(), day);
        reconcileResidenceStates(residences, lifecycle.persons(), occupancy, day);

        long rentPaid = collectRent(residences, economy, occupancy, day);
        int independent = 0;
        int migrants = 0;
        long estateSwept = 0L;

        if (isYearBoundary(lifecycle.establishedDay(), day)) {
            LettingResult local = formIndependentHouseholds(
                    residences, lifecycle, marriages, economy, occupancy, day,
                    MAX_INDEPENDENT_LETTINGS_PER_YEAR);
            independent += local.lettings();
            estateSwept += local.estateSwept();

            MigrationResult migration = admitMigrants(
                    population, residences, lifecycle, marriages, economy, occupancy,
                    day, nextMigrantSequence, MAX_MIGRANT_LETTINGS_PER_YEAR);
            migrants += migration.lettings();
            estateSwept += migration.estateSwept();
            nextMigrantSequence = migration.nextSequence();
        }

        return new HousingDay(
                List.copyOf(residences), rentPaid, estateSwept,
                independent, migrants, nextMigrantSequence);
    }

    private static void reconcileResidenceStates(
            List<ErdenCapitalHousingSavedData.Residence> residences,
            List<ErdenCapitalLifecycleSavedData.Person> people,
            Map<String, Integer> occupancy,
            long day) {
        Map<String, List<ErdenCapitalLifecycleSavedData.Person>> byHousehold = peopleByHousehold(people, day);
        for (int index = 0; index < residences.size(); index++) {
            ErdenCapitalHousingSavedData.Residence residence = residences.get(index);
            int living = occupancy.getOrDefault(residence.slotId(), 0);
            if (living <= 0) {
                if (!residence.vacant()) residence = residence.asVacant(1L);
                else residence = residence.asVacant(1L);
            } else if (residence.leased()) {
                ErdenCapitalLifecycleSavedData.Person representative = byHousehold
                        .getOrDefault(residence.slotId(), List.of()).stream()
                        .filter(person -> !person.founder())
                        .min(Comparator.comparing(ErdenCapitalLifecycleSavedData.Person::id))
                        .orElse(null);
                if (representative != null
                        && !representative.id().equals(residence.tenantRepresentative())) {
                    residence = residence.withTenant(representative.id());
                }
            } else {
                residence = residence.asOwnerOccupied();
            }
            residences.set(index, residence);
        }
    }

    private static long collectRent(
            List<ErdenCapitalHousingSavedData.Residence> residences,
            ErdenPhysicalEconomySavedData economy,
            Map<String, Integer> occupancy,
            long day) {
        int vacancies = 0;
        for (ErdenCapitalHousingSavedData.Residence residence : residences) if (residence.vacant()) vacancies++;
        long totalPaid = 0L;
        for (int index = 0; index < residences.size(); index++) {
            ErdenCapitalHousingSavedData.Residence residence = residences.get(index);
            if (!residence.leased() || occupancy.getOrDefault(residence.slotId(), 0) <= 0
                    || residence.lastRentDay() >= day) continue;
            int rent = residence.arrears() >= 20L ? 1 : marketRent(vacancies);
            ErdenPhysicalEconomySavedData.WalletState wallet = economy.wallet(residence.slotId());
            if (wallet == null) continue;
            long before = wallet.coins();
            ErdenPhysicalEconomySavedData.WalletState charged = wallet.spend(rent);
            long paid = Math.max(0L, before - charged.coins());
            if (economy.replaceWallet(charged)) {
                totalPaid += paid;
                residences.set(index, residence.withRentDay(day, rent, paid));
            }
        }
        return totalPaid;
    }

    private static LettingResult formIndependentHouseholds(
            List<ErdenCapitalHousingSavedData.Residence> residences,
            ErdenCapitalLifecycleSavedData lifecycle,
            ErdenCapitalMarriageSavedData marriages,
            ErdenPhysicalEconomySavedData economy,
            Map<String, Integer> occupancy,
            long day,
            int limit) {
        List<ErdenCapitalHousingSavedData.Residence> vacancies = vacantResidences(residences);
        if (vacancies.isEmpty()) return new LettingResult(0, 0L);
        Set<String> stewards = stewardIds(lifecycle.householdLines());
        Map<String, ErdenCapitalLifecycleSavedData.Person> people = peopleById(lifecycle.persons());
        int lettings = 0;
        long estateSwept = 0L;
        for (ErdenCapitalMarriageSavedData.Union union : marriages.unions()) {
            if (lettings >= limit || vacancies.isEmpty()) break;
            if (!union.activeOn(day)
                    || day - union.startDay() < (long) MIN_MARRIAGE_YEARS_BEFORE_INDEPENDENCE
                    * ErdenCapitalLifecycleManager.DAYS_PER_YEAR) continue;
            ErdenCapitalLifecycleSavedData.Person a = people.get(union.personA());
            ErdenCapitalLifecycleSavedData.Person b = people.get(union.personB());
            if (!independenceCandidate(a, b, union, stewards, occupancy, day)) continue;
            ErdenCapitalHousingSavedData.Residence target = vacancies.removeFirst();
            String sourceHousehold = a.householdId();
            if (!movePair(lifecycle, marriages, union, a, b, target.slotId())) continue;
            WalletRelet relet = reletWallet(economy, target.slotId());
            if (!relet.success()) {
                movePair(lifecycle, marriages, union, lifecycle.person(a.id()), lifecycle.person(b.id()), sourceHousehold);
                continue;
            }
            estateSwept += relet.estateSwept();
            occupancy.compute(sourceHousehold,
                    (ignored, value) -> Math.max(0, (value == null ? 0 : value) - 2));
            occupancy.put(target.slotId(), 2);
            int rent = marketRent(vacancies.size());
            replaceResidence(residences, target.asLease(a.id(), day, rent));
            lettings++;
        }
        return new LettingResult(lettings, estateSwept);
    }

    private static boolean independenceCandidate(
            ErdenCapitalLifecycleSavedData.Person a,
            ErdenCapitalLifecycleSavedData.Person b,
            ErdenCapitalMarriageSavedData.Union union,
            Set<String> stewards,
            Map<String, Integer> occupancy,
            long day) {
        if (a == null || b == null || a.founder() || b.founder()
                || !a.aliveOn(day) || !b.aliveOn(day)
                || !a.householdId().equals(b.householdId())
                || !union.householdId().equals(a.householdId())
                || stewards.contains(a.id()) || stewards.contains(b.id())
                || occupancy.getOrDefault(a.householdId(), 0) < 3) return false;
        return ageYears(a, day) >= MIN_INDEPENDENCE_AGE && ageYears(b, day) >= MIN_INDEPENDENCE_AGE;
    }

    private static MigrationResult admitMigrants(
            ErdenPopulationSavedData population,
            List<ErdenCapitalHousingSavedData.Residence> residences,
            ErdenCapitalLifecycleSavedData lifecycle,
            ErdenCapitalMarriageSavedData marriages,
            ErdenPhysicalEconomySavedData economy,
            Map<String, Integer> occupancy,
            long day,
            int nextSequence,
            int limit) {
        int lettings = 0;
        long estateSwept = 0L;
        long minimumVacancy = (long) MIGRATION_VACANCY_YEARS * ErdenCapitalLifecycleManager.DAYS_PER_YEAR;
        for (ErdenCapitalHousingSavedData.Residence vacancy : vacantResidences(residences)) {
            if (lettings >= limit || vacancy.vacancyDays() < minimumVacancy) continue;
            int firstSequence = nextSequence++;
            int secondSequence = nextSequence++;
            String firstId = "erden_capital_migrant_%05d".formatted(firstSequence);
            String secondId = "erden_capital_migrant_%05d".formatted(secondSequence);
            ErdenCapitalLifecycleSavedData.Person first = migrant(
                    firstId, migrantName(firstSequence), vacancy.slotId(), day, 24 + Math.floorMod(firstSequence, 8));
            ErdenCapitalLifecycleSavedData.Person second = migrant(
                    secondId, migrantName(secondSequence), vacancy.slotId(), day, 22 + Math.floorMod(secondSequence, 9));
            List<String> migrantIds = List.of(first.id(), second.id());
            if (!lifecycle.addPeople(List.of(first, second))) continue;
            ErdenCapitalMarriageSavedData.Union union = marriages.createUnion(
                    first.id(), second.id(), vacancy.slotId(), day, false);
            if (union == null) {
                lifecycle.removeNonFounderPeople(migrantIds);
                continue;
            }
            WalletRelet relet = reletWallet(economy, vacancy.slotId());
            if (!relet.success()) {
                marriages.removeUnion(union.id());
                lifecycle.removeNonFounderPeople(migrantIds);
                continue;
            }
            estateSwept += relet.estateSwept();
            occupancy.put(vacancy.slotId(), 2);
            replaceResidence(residences, vacancy.asLease(first.id(), day, marketRent(vacantResidences(residences).size())));
            lettings++;
        }
        return new MigrationResult(lettings, estateSwept, nextSequence);
    }

    private static ErdenCapitalLifecycleSavedData.Person migrant(
            String id,
            String name,
            String householdId,
            long day,
            int age) {
        return new ErdenCapitalLifecycleSavedData.Person(
                id, name, householdId,
                day - (long) age * ErdenCapitalLifecycleManager.DAYS_PER_YEAR,
                "", "", 1, false, false,
                0, 0, "", 0, 0, -1L, -1L);
    }

    private static boolean movePair(
            ErdenCapitalLifecycleSavedData lifecycle,
            ErdenCapitalMarriageSavedData marriages,
            ErdenCapitalMarriageSavedData.Union union,
            ErdenCapitalLifecycleSavedData.Person a,
            ErdenCapitalLifecycleSavedData.Person b,
            String householdId) {
        if (a == null || b == null || householdId == null || householdId.isBlank()) return false;
        String oldA = a.householdId();
        String oldB = b.householdId();
        if (!lifecycle.movePersonHousehold(a.id(), householdId)) return false;
        if (!lifecycle.movePersonHousehold(b.id(), householdId)) {
            lifecycle.movePersonHousehold(a.id(), oldA);
            return false;
        }
        if (!marriages.moveUnionHousehold(union.id(), householdId)) {
            lifecycle.movePersonHousehold(a.id(), oldA);
            lifecycle.movePersonHousehold(b.id(), oldB);
            return false;
        }
        return true;
    }

    private static WalletRelet reletWallet(
            ErdenPhysicalEconomySavedData economy,
            String slotId) {
        ErdenPhysicalEconomySavedData.WalletState old = economy.wallet(slotId);
        if (old == null) return new WalletRelet(false, 0L, 0L);
        long grant = Math.min(MAX_RELOCATION_GRANT, old.coins());
        long estate = Math.max(0L, old.coins() - grant);
        ErdenPhysicalEconomySavedData.WalletState replacement =
                new ErdenPhysicalEconomySavedData.WalletState(slotId, grant, 0L, 0L);
        return new WalletRelet(economy.replaceWallet(replacement), estate, grant);
    }

    private static List<ErdenCapitalHousingSavedData.Residence> vacantResidences(
            List<ErdenCapitalHousingSavedData.Residence> residences) {
        return residences.stream()
                .filter(ErdenCapitalHousingSavedData.Residence::vacant)
                .sorted(Comparator.comparingLong(ErdenCapitalHousingSavedData.Residence::vacancyDays).reversed()
                        .thenComparing(ErdenCapitalHousingSavedData.Residence::slotId))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static void replaceResidence(
            List<ErdenCapitalHousingSavedData.Residence> residences,
            ErdenCapitalHousingSavedData.Residence replacement) {
        for (int index = 0; index < residences.size(); index++) {
            if (!residences.get(index).slotId().equals(replacement.slotId())) continue;
            residences.set(index, replacement);
            return;
        }
    }

    private static Map<String, Integer> livingOccupancy(
            List<ErdenCapitalLifecycleSavedData.Person> people,
            long day) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (ErdenCapitalLifecycleSavedData.Person person : people) {
            if (person.aliveOn(day)) result.merge(person.householdId(), 1, Integer::sum);
        }
        return result;
    }

    private static Map<String, List<ErdenCapitalLifecycleSavedData.Person>> peopleByHousehold(
            List<ErdenCapitalLifecycleSavedData.Person> people,
            long day) {
        Map<String, List<ErdenCapitalLifecycleSavedData.Person>> result = new HashMap<>();
        for (ErdenCapitalLifecycleSavedData.Person person : people) {
            if (!person.aliveOn(day)) continue;
            result.computeIfAbsent(person.householdId(), ignored -> new ArrayList<>()).add(person);
        }
        return result;
    }

    private static Map<String, ErdenCapitalLifecycleSavedData.Person> peopleById(
            List<ErdenCapitalLifecycleSavedData.Person> people) {
        Map<String, ErdenCapitalLifecycleSavedData.Person> result = new HashMap<>();
        for (ErdenCapitalLifecycleSavedData.Person person : people) result.put(person.id(), person);
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

    private static int ageYears(ErdenCapitalLifecycleSavedData.Person person, long day) {
        return (int) Math.max(0L, Math.floorDiv(
                day - person.birthDay(), ErdenCapitalLifecycleManager.DAYS_PER_YEAR));
    }

    private static boolean isYearBoundary(long establishedDay, long day) {
        long elapsed = day - establishedDay;
        return elapsed > 0L && Math.floorMod(elapsed, ErdenCapitalLifecycleManager.DAYS_PER_YEAR) == 0L;
    }

    private static int marketRent(int vacancies) {
        if (vacancies <= 3) return 2;
        return 1;
    }

    private static String migrantName(int sequence) {
        int value = Math.max(0, sequence - 1);
        int base = MIGRANT_HEADS.size() * MIGRANT_TAILS.size();
        StringBuilder name = new StringBuilder("이주민 ");
        int remaining = value;
        do {
            int digit = Math.floorMod(remaining, base);
            if (name.length() > 4) name.append('·');
            name.append(MIGRANT_HEADS.get(digit % MIGRANT_HEADS.size()));
            name.append(MIGRANT_TAILS.get(digit / MIGRANT_HEADS.size()));
            remaining = Math.floorDiv(remaining, base);
        } while (remaining > 0);
        return name.toString();
    }

    private static void logPlanOnce(ErdenCapitalHousingSavedData housing) {
        if (planLogged) return;
        planLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden capital housing revision={} residences={} fixed_wallet_slots=77 vacancy_market=true crown_reversion=true independent_households=true daily_rent=true inward_migration=true no_synthetic_tenements=true",
                HOUSING_REVISION, housing.residences().size());
    }

    private static void verifyCi(
            ServerLevel level,
            ErdenPopulationSavedData population,
            ErdenPhysicalEconomySavedData economy,
            ErdenCapitalHousingSavedData housing) {
        if (ciPassed || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_CAPITAL_HOUSING_TEST"))) return;
        if (!housing.initialized(HOUSING_REVISION, EXPECTED_RESIDENCES)
                || economy.wallets().size() != ErdenAuthoritativeEconomyManager.EXPECTED_WALLETS) return;
        ErdenCapitalLifecycleManager.Projection projection =
                ErdenCapitalLifecycleManager.projectForAudit(level, population, 72);
        Map<String, Integer> occupancy = livingOccupancy(projection.persons(), projection.targetDay());
        long projectedVacant = population.households().stream()
                .filter(household -> occupancy.getOrDefault(household.id(), 0) == 0)
                .count();
        long projectedOccupied = EXPECTED_RESIDENCES - projectedVacant;
        if (projectedVacant <= 0 || projectedOccupied <= 0) {
            throw new IllegalStateException(
                    "Erden capital housing projection produced no vacancy turnover vacant=" + projectedVacant
                            + " occupied=" + projectedOccupied);
        }
        long sampleEstate = 30L;
        long sampleGrant = Math.min(MAX_RELOCATION_GRANT, sampleEstate);
        long sampleReserve = sampleEstate - sampleGrant;
        long sampleRentPaid = Math.min(sampleGrant, 5L);
        if (sampleGrant + sampleReserve != sampleEstate || sampleRentPaid <= 0L) {
            throw new IllegalStateException("Erden capital housing money-conservation projection failed");
        }
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_CAPITAL_HOUSING_PASS revision=1 residences=77 wallet_slots=77 projection_years=72 projected_vacant={} projected_occupied={} independent_household_path=true spouse_union_retarget=true migrant_household_path=true persistent_vacancy=true dynamic_rent=true rent_relief_on_arrears=true unclaimed_estate_conserved=true relocation_grant={} estate_reserve={} sample_rent_paid={} existing_wallet_slot_reused=true no_synthetic_tenements=true source_blocks_cut=0",
                projectedVacant, projectedOccupied, sampleGrant, sampleReserve, sampleRentPaid);
    }

    private record HousingDay(
            List<ErdenCapitalHousingSavedData.Residence> residences,
            long rentPaid,
            long estateSwept,
            int independentLettings,
            int migrantLettings,
            int nextMigrantSequence) {
    }

    private record LettingResult(int lettings, long estateSwept) {
    }

    private record MigrationResult(int lettings, long estateSwept, int nextSequence) {
    }

    private record WalletRelet(boolean success, long estateSwept, long relocationGrant) {
    }
}
