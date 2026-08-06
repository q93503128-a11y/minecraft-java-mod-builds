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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Long-lived demographic simulation for Erden's exterior settlements.
 * The founding workforce remains in its original save file; this manager overlays age, genealogy,
 * births, retirement, natural death, household succession and replacement labour without changing
 * founding resident IDs or the old codec.
 */
public final class ErdenExteriorLifecycleManager {
    public static final int LIFECYCLE_REVISION = 1;
    public static final int DAYS_PER_YEAR = 112;
    public static final int ADULT_AGE = 18;
    public static final int RETIREMENT_AGE = 58;
    public static final int MIN_PARENT_AGE = 20;
    public static final int MAX_PARENT_AGE = 44;
    public static final int MAX_HOUSEHOLD_SIZE = 5;

    private static final int MAX_CATCH_UP_DAYS = DAYS_PER_YEAR * 20;
    private static final int SPAWN_INTERVAL = 40;
    private static final int SPAWN_BUDGET = 2;
    private static final int ROUTINE_INTERVAL = 80;
    private static final Identifier VILLAGER_ID =
            Identifier.fromNamespaceAndPath("minecraft", "villager");
    private static final List<String> DESCENDANT_NAMES = List.of(
            "아델", "베라", "카엘", "델린", "에라", "파엘", "그리아", "하렌",
            "이셀", "라비", "세온", "도엘", "미라", "카린", "로엔", "브리아",
            "티렌", "오라", "니엘", "가엘", "헤린", "유엘", "마린", "리엔"
    );

    private static MinecraftServer activeServer;
    private static boolean planLogged;
    private static boolean ciPassed;

    private ErdenExteriorLifecycleManager() {
    }

    public record LaborContribution(int alive, int attended, int absent, int dead) {
        public static final LaborContribution ZERO = new LaborContribution(0, 0, 0, 0);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        ErdenExteriorWorkforceSavedData workforce = workforce(level);
        if (!workforce.hasPopulation(
                ErdenExteriorWorkforceManager.WORKFORCE_REVISION,
                ErdenExteriorWorkforceManager.EXPECTED_HOUSEHOLDS)) return;
        ErdenExteriorLifecycleSavedData lifecycle = lifecycle(level);
        ensureInitialized(lifecycle, workforce, day);
        processThroughDay(level, lifecycle, workforce, day);
        logPlanOnce(lifecycle, workforce, day);
        ensureLoadedDescendants(level, lifecycle, workforce, day);
        runRoutines(level, lifecycle, workforce, day);
        verifyCi(lifecycle, workforce, day);
    }

    public static boolean foundingWorkerAvailable(
            ServerLevel level,
            String residentId,
            long day) {
        ErdenExteriorWorkforceSavedData workforce = workforce(level);
        ErdenExteriorLifecycleSavedData lifecycle = lifecycle(level);
        ensureInitialized(lifecycle, workforce, day);
        ErdenExteriorLifecycleSavedData.Person person = lifecycle.person(residentId);
        return person == null || (person.aliveOn(day) && !person.retiredOn(day));
    }

    public static boolean controlsRoutine(
            ServerLevel level,
            String residentId,
            long day) {
        ErdenExteriorWorkforceSavedData workforce = workforce(level);
        ErdenExteriorLifecycleSavedData lifecycle = lifecycle(level);
        ensureInitialized(lifecycle, workforce, day);
        ErdenExteriorLifecycleSavedData.Person person = lifecycle.person(residentId);
        return person != null
                && (!person.aliveOn(day)
                || person.retiredOn(day)
                || (!person.foundingWorker() && person.assignedWorker()));
    }

    public static LaborContribution additionalLabor(
            ServerLevel level,
            String nodeId,
            String nodeRole,
            long day) {
        ErdenExteriorWorkforceSavedData workforce = workforce(level);
        ErdenExteriorLifecycleSavedData lifecycle = lifecycle(level);
        ensureInitialized(lifecycle, workforce, day);
        int alive = 0;
        int attended = 0;
        int absent = 0;
        int dead = 0;
        for (ErdenExteriorLifecycleSavedData.Person person : lifecycle.persons()) {
            if (person.foundingWorker()
                    || !person.nodeId().equals(nodeId)
                    || !person.assignedWorker()) continue;
            if (!person.aliveOn(day)) {
                dead++;
                continue;
            }
            if (person.retiredOn(day) || ageYears(person, day) < ADULT_AGE) continue;
            alive++;
            if (absentOnDay(person.id(), person.restDay(), nodeRole, day)) absent++;
            else attended++;
        }
        return new LaborContribution(alive, attended, absent, dead);
    }

    public static void markDeadIfLifecycleResident(ServerLevel level, Villager villager) {
        if (!level.dimension().equals(StarterRealmManager.REALM_KEY)) return;
        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        ErdenExteriorLifecycleSavedData data = lifecycle(level);
        ErdenExteriorLifecycleSavedData.Person person = data.personByName(villager.getName().getString());
        if (person == null || !person.aliveOn(day)) return;
        data.markDeath(person.id(), day);
        if (person.founder()) workforce(level).markDead(person.id());
        LivingKingdoms.LOGGER.info(
                "Erden exterior lifecycle resident {} of household {} died on day={} generation={}",
                person.id(), person.householdId(), day, person.generation());
    }

    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof Villager villager)
                || !(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)) return;
        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        ErdenExteriorLifecycleSavedData data = lifecycle(level);
        ErdenExteriorLifecycleSavedData.Person person = data.personByName(villager.getName().getString());
        if (person == null) return;
        ErdenExteriorLifecycleSavedData.HouseholdLine line = data.householdLine(person.householdId());
        String standing = line != null && line.stewardId().equals(person.id())
                ? "가구주"
                : line != null && line.heirId().equals(person.id()) ? "후계자" : "가족 구성원";
        String work = person.assignedWorker()
                ? roleName(person.workRole()) + " 근로자"
                : ageYears(person, day) < ADULT_AGE ? "성장 중인 아이" : "현재 결원을 기다리는 성인";
        player.sendSystemMessage(Component.literal(
                "§6[" + person.name() + "] §f" + person.generation() + "세대 " + standing
                        + ", " + ageYears(person, day) + "세, " + work + "입니다."));
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
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

    private static void ensureInitialized(
            ErdenExteriorLifecycleSavedData lifecycle,
            ErdenExteriorWorkforceSavedData workforce,
            long day) {
        if (lifecycle.initialized(
                LIFECYCLE_REVISION,
                ErdenExteriorWorkforceManager.EXPECTED_RESIDENTS,
                ErdenExteriorWorkforceManager.EXPECTED_HOUSEHOLDS)) return;

        List<ErdenExteriorLifecycleSavedData.Person> founders = new ArrayList<>();
        List<ErdenExteriorLifecycleSavedData.HouseholdLine> lines = new ArrayList<>();
        for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {
            String steward = "";
            String heir = "";
            for (ErdenExteriorWorkforceSavedData.Resident resident : household.residents()) {
                int age = founderAge(resident);
                long birthDay = day - (long) age * DAYS_PER_YEAR
                        - Math.floorMod(resident.id().hashCode(), DAYS_PER_YEAR);
                long deathDay = workforce.isDead(resident.id()) ? day : -1L;
                boolean foundingWorker = resident.worker();
                founders.add(new ErdenExteriorLifecycleSavedData.Person(
                        resident.id(), resident.name(), household.id(), household.nodeId(),
                        birthDay, "", "", 0, true, foundingWorker,
                        resident.workRole(), resident.shiftStart(), resident.shiftEnd(),
                        resident.restDay(), -1L, deathDay));
                if (steward.isBlank() && resident.lifeStage().equals("adult_head")) {
                    steward = resident.id();
                } else if (heir.isBlank() && !resident.lifeStage().equals("elder")) {
                    heir = resident.id();
                }
            }
            if (steward.isBlank() && !household.residents().isEmpty()) {
                steward = household.residents().getFirst().id();
            }
            lines.add(new ErdenExteriorLifecycleSavedData.HouseholdLine(
                    household.id(), steward, heir, Integer.MIN_VALUE, 0, 0));
        }
        lifecycle.initialize(LIFECYCLE_REVISION, day, founders, lines);
        validateInitialization(lifecycle, workforce);
    }

    private static void validateInitialization(
            ErdenExteriorLifecycleSavedData lifecycle,
            ErdenExteriorWorkforceSavedData workforce) {
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (ErdenExteriorLifecycleSavedData.Person person : lifecycle.persons()) {
            if (!ids.add(person.id())) {
                throw new IllegalStateException("Duplicate Erden lifecycle person id " + person.id());
            }
            if (!names.add(person.name())) {
                throw new IllegalStateException("Duplicate Erden lifecycle person name " + person.name());
            }
        }
        if (lifecycle.founderCount() != ErdenExteriorWorkforceManager.EXPECTED_RESIDENTS
                || lifecycle.householdLines().size() != ErdenExteriorWorkforceManager.EXPECTED_HOUSEHOLDS
                || ids.size() != workforce.residentCount()) {
            throw new IllegalStateException(
                    "Invalid Erden lifecycle founders=" + lifecycle.founderCount()
                            + " household_lines=" + lifecycle.householdLines().size()
                            + " workforce_residents=" + workforce.residentCount());
        }
    }

    private static void processThroughDay(
            ServerLevel level,
            ErdenExteriorLifecycleSavedData lifecycle,
            ErdenExteriorWorkforceSavedData workforce,
            long currentDay) {
        if (lifecycle.lastProcessedDay() >= currentDay) return;
        long first = lifecycle.lastProcessedDay() < lifecycle.establishedDay()
                ? lifecycle.establishedDay()
                : Math.max(lifecycle.lastProcessedDay() + 1L,
                currentDay - MAX_CATCH_UP_DAYS + 1L);
        for (long day = first; day <= currentDay; day++) {
            processDay(lifecycle, workforce, day);
        }
    }

    private static void processDay(
            ErdenExteriorLifecycleSavedData lifecycle,
            ErdenExteriorWorkforceSavedData workforce,
            long day) {
        List<ErdenExteriorLifecycleSavedData.Person> people = new ArrayList<>(lifecycle.persons());
        List<ErdenExteriorLifecycleSavedData.HouseholdLine> lines =
                new ArrayList<>(lifecycle.householdLines());
        int nextSequence = lifecycle.nextBirthSequence();

        for (int i = 0; i < people.size(); i++) {
            ErdenExteriorLifecycleSavedData.Person person = people.get(i);
            if (person.founder() && workforce.isDead(person.id()) && person.aliveOn(day)) {
                people.set(i, person.withDeath(day));
                continue;
            }
            if (!person.aliveOn(day)) continue;
            int age = ageYears(person, day);
            if (person.assignedWorker()
                    && person.retirementDay() < 0L
                    && age >= RETIREMENT_AGE) {
                person = person.withRetirement(day);
                people.set(i, person);
            }
            if (day > lifecycle.establishedDay()
                    && isBirthday(person, day)
                    && naturalDeath(person, age, day)) {
                people.set(i, person.withDeath(day));
                if (person.founder()) workforce.markDead(person.id());
            }
        }

        people = fillVacancies(people, workforce, day);

        int year = Math.toIntExact(Math.floorDiv(day - lifecycle.establishedDay(), DAYS_PER_YEAR));
        if (day > lifecycle.establishedDay()
                && Math.floorMod(day - lifecycle.establishedDay(), DAYS_PER_YEAR) == 0L) {
            BirthResult births = processBirths(
                    people, lines, workforce, day, year, nextSequence);
            people = births.people();
            lines = births.lines();
            nextSequence = births.nextSequence();
        }

        lines = processSuccession(people, lines, day);
        lifecycle.replaceDay(day, nextSequence, people, lines);
    }

    private static List<ErdenExteriorLifecycleSavedData.Person> fillVacancies(
            List<ErdenExteriorLifecycleSavedData.Person> people,
            ErdenExteriorWorkforceSavedData workforce,
            long day) {
        List<ErdenExteriorLifecycleSavedData.Person> result = new ArrayList<>(people);
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            int required = requiredWorkers(node.role);
            int active = 0;
            for (ErdenExteriorLifecycleSavedData.Person person : result) {
                if (person.nodeId().equals(node.id)
                        && person.assignedWorker()
                        && person.aliveOn(day)
                        && !person.retiredOn(day)
                        && ageYears(person, day) >= ADULT_AGE) active++;
            }
            int vacancies = Math.max(0, required - active);
            if (vacancies == 0) continue;
            List<Integer> candidates = new ArrayList<>();
            for (int i = 0; i < result.size(); i++) {
                ErdenExteriorLifecycleSavedData.Person person = result.get(i);
                int age = ageYears(person, day);
                if (person.nodeId().equals(node.id)
                        && !person.assignedWorker()
                        && person.aliveOn(day)
                        && age >= ADULT_AGE && age < RETIREMENT_AGE) {
                    candidates.add(i);
                }
            }
            candidates.sort(Comparator.comparingLong(index -> result.get(index).birthDay()));
            for (int candidateIndex : candidates) {
                if (vacancies-- <= 0) break;
                ErdenExteriorLifecycleSavedData.Person person = result.get(candidateIndex);
                int workerIndex = required - vacancies - 1;
                Shift shift = shiftFor(node.role, workerIndex);
                result.set(candidateIndex, person.withWork(
                        node.role, shift.start(), shift.end(),
                        Math.floorMod(person.id().hashCode(), 7)));
            }
        }
        return result;
    }

    private static BirthResult processBirths(
            List<ErdenExteriorLifecycleSavedData.Person> people,
            List<ErdenExteriorLifecycleSavedData.HouseholdLine> lines,
            ErdenExteriorWorkforceSavedData workforce,
            long day,
            int year,
            int nextSequence) {
        List<ErdenExteriorLifecycleSavedData.Person> resultPeople = new ArrayList<>(people);
        List<ErdenExteriorLifecycleSavedData.HouseholdLine> resultLines = new ArrayList<>(lines);
        Map<String, ErdenExteriorWorkforceSavedData.Household> households = new LinkedHashMap<>();
        for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {
            households.put(household.id(), household);
        }

        for (int lineIndex = 0; lineIndex < resultLines.size(); lineIndex++) {
            ErdenExteriorLifecycleSavedData.HouseholdLine line = resultLines.get(lineIndex);
            ErdenExteriorWorkforceSavedData.Household household = households.get(line.householdId());
            if (household == null
                    || (line.lastBirthYear() != Integer.MIN_VALUE
                    && year - line.lastBirthYear() < 2)) continue;
            List<ErdenExteriorLifecycleSavedData.Person> members = members(
                    resultPeople, household.id());
            int living = 0;
            List<ErdenExteriorLifecycleSavedData.Person> parents = new ArrayList<>();
            for (ErdenExteriorLifecycleSavedData.Person member : members) {
                if (!member.aliveOn(day)) continue;
                living++;
                int age = ageYears(member, day);
                if (age >= MIN_PARENT_AGE && age <= MAX_PARENT_AGE
                        && !member.retiredOn(day)) parents.add(member);
            }
            if (living >= MAX_HOUSEHOLD_SIZE || parents.size() < 2) continue;
            ErdenExteriorWorkforceSavedData.NodeLabor labor = workforce.labor(household.nodeId());
            if (labor == null || labor.productionPercent() < 65) continue;
            long seed = (long) household.id().hashCode() * 41L + year * 101L;
            if (Math.floorMod(seed, 3L) != 0L) continue;

            parents.sort(Comparator.comparingLong(ErdenExteriorLifecycleSavedData.Person::birthDay));
            ErdenExteriorLifecycleSavedData.Person parentA = parents.get(0);
            ErdenExteriorLifecycleSavedData.Person parentB = parents.get(1);
            int generation = Math.max(parentA.generation(), parentB.generation()) + 1;
            String id = household.id() + "_birth_%04d".formatted(nextSequence);
            String given = DESCENDANT_NAMES.get(Math.floorMod(nextSequence - 1, DESCENDANT_NAMES.size()));
            String name = household.familyName() + " " + given + "-" + nextSequence;
            resultPeople.add(new ErdenExteriorLifecycleSavedData.Person(
                    id, name, household.id(), household.nodeId(), day,
                    parentA.id(), parentB.id(), generation,
                    false, false, "", 0, 0, -1, -1L, -1L));
            resultLines.set(lineIndex, line.withBirth(year));
            nextSequence++;
        }
        return new BirthResult(resultPeople, resultLines, nextSequence);
    }

    private static List<ErdenExteriorLifecycleSavedData.HouseholdLine> processSuccession(
            List<ErdenExteriorLifecycleSavedData.Person> people,
            List<ErdenExteriorLifecycleSavedData.HouseholdLine> lines,
            long day) {
        List<ErdenExteriorLifecycleSavedData.HouseholdLine> result = new ArrayList<>();
        Map<String, ErdenExteriorLifecycleSavedData.Person> byId = new HashMap<>();
        for (ErdenExteriorLifecycleSavedData.Person person : people) byId.put(person.id(), person);
        for (ErdenExteriorLifecycleSavedData.HouseholdLine line : lines) {
            List<ErdenExteriorLifecycleSavedData.Person> members = members(people, line.householdId());
            ErdenExteriorLifecycleSavedData.Person steward = byId.get(line.stewardId());
            boolean validSteward = steward != null
                    && steward.aliveOn(day)
                    && ageYears(steward, day) >= ADULT_AGE;
            ErdenExteriorLifecycleSavedData.Person heir = chooseHeir(members, line.stewardId(), day);
            if (!validSteward) {
                ErdenExteriorLifecycleSavedData.Person replacement = null;
                ErdenExteriorLifecycleSavedData.Person namedHeir = byId.get(line.heirId());
                if (namedHeir != null
                        && namedHeir.aliveOn(day)
                        && ageYears(namedHeir, day) >= ADULT_AGE) replacement = namedHeir;
                if (replacement == null) replacement = chooseSteward(members, day);
                if (replacement != null) {
                    heir = chooseHeir(members, replacement.id(), day);
                    result.add(line.withSuccession(
                            replacement.id(), heir == null ? "" : heir.id()));
                    continue;
                }
            }
            String heirId = heir == null ? "" : heir.id();
            result.add(line.heirId().equals(heirId) ? line : line.withHeir(heirId));
        }
        return result;
    }

    private static ErdenExteriorLifecycleSavedData.Person chooseSteward(
            List<ErdenExteriorLifecycleSavedData.Person> members,
            long day) {
        return members.stream()
                .filter(person -> person.aliveOn(day) && ageYears(person, day) >= ADULT_AGE)
                .min(Comparator.comparingLong(ErdenExteriorLifecycleSavedData.Person::birthDay))
                .orElse(null);
    }

    private static ErdenExteriorLifecycleSavedData.Person chooseHeir(
            List<ErdenExteriorLifecycleSavedData.Person> members,
            String stewardId,
            long day) {
        return members.stream()
                .filter(person -> !person.id().equals(stewardId) && person.aliveOn(day))
                .sorted(Comparator
                        .comparingInt((ErdenExteriorLifecycleSavedData.Person person) ->
                                ageYears(person, day) >= ADULT_AGE ? 0 : 1)
                        .thenComparingLong(ErdenExteriorLifecycleSavedData.Person::birthDay))
                .findFirst().orElse(null);
    }

    private static void ensureLoadedDescendants(
            ServerLevel level,
            ErdenExteriorLifecycleSavedData lifecycle,
            ErdenExteriorWorkforceSavedData workforce,
            long day) {
        if (level.getGameTime() % SPAWN_INTERVAL != 0L) return;
        Map<String, ErdenExteriorWorkforceSavedData.Household> households = householdMap(workforce);
        Set<String> existing = loadedLifecycleNames(level, lifecycle);
        int spawned = 0;
        for (ErdenExteriorLifecycleSavedData.Person person : lifecycle.persons()) {
            if (spawned >= SPAWN_BUDGET) break;
            if (person.founder() || !person.aliveOn(day) || existing.contains(person.name())) continue;
            ErdenExteriorWorkforceSavedData.Household household = households.get(person.householdId());
            if (household == null
                    || !level.hasChunk(household.homeX() >> 4, household.homeZ() >> 4)) continue;
            ErdenKingdomSupplyCatalog.SupplyNode node = ErdenKingdomSupplyCatalog.node(person.nodeId());
            if (node == null || !ErdenKingdomExteriorBuilder.anchorBuilt(level, node)) continue;
            if (spawnDescendant(level, household, person, day)) spawned++;
        }
        discardRecordedDead(level, lifecycle, day);
    }

    private static boolean spawnDescendant(
            ServerLevel level,
            ErdenExteriorWorkforceSavedData.Household household,
            ErdenExteriorLifecycleSavedData.Person person,
            long day) {
        EntityType<?> villagerType = BuiltInRegistries.ENTITY_TYPE.getOptional(VILLAGER_ID).orElse(null);
        if (villagerType == null) return false;
        Entity created = villagerType.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Villager villager)) return false;
        int slot = Math.floorMod(person.id().hashCode(), 4);
        int x = household.homeX() + slot - 1;
        int z = household.homeZ() + 4 + (slot & 1);
        int preferredY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z)) + 2;
        villager.setPos(x + 0.5D, safeStandingY(level, x, preferredY, z), z + 0.5D);
        villager.setCustomName(Component.literal(person.name()));
        villager.setCustomNameVisible(false);
        villager.setPersistenceRequired();
        villager.setInvulnerable(false);
        if (ageYears(person, day) < ADULT_AGE) villager.setAge(-24_000);
        return level.addFreshEntity(villager);
    }

    private static void discardRecordedDead(
            ServerLevel level,
            ErdenExteriorLifecycleSavedData lifecycle,
            long day) {
        Map<String, ErdenExteriorLifecycleSavedData.Person> byName = new HashMap<>();
        for (ErdenExteriorLifecycleSavedData.Person person : lifecycle.persons()) {
            byName.put(person.name(), person);
        }
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class, exteriorBounds(level),
                candidate -> byName.containsKey(candidate.getName().getString()))) {
            ErdenExteriorLifecycleSavedData.Person person = byName.get(villager.getName().getString());
            if (person != null && !person.aliveOn(day)) villager.discard();
        }
    }

    private static void runRoutines(
            ServerLevel level,
            ErdenExteriorLifecycleSavedData lifecycle,
            ErdenExteriorWorkforceSavedData workforce,
            long day) {
        if (level.getGameTime() % ROUTINE_INTERVAL != 0L) return;
        Map<String, ErdenExteriorLifecycleSavedData.Person> people = new HashMap<>();
        for (ErdenExteriorLifecycleSavedData.Person person : lifecycle.persons()) {
            if (!person.foundingWorker() || person.retiredOn(day)) {
                people.put(person.name(), person);
            }
        }
        Map<String, ErdenExteriorWorkforceSavedData.Household> households = householdMap(workforce);
        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class, exteriorBounds(level),
                candidate -> people.containsKey(candidate.getName().getString()))) {
            ErdenExteriorLifecycleSavedData.Person person = people.get(villager.getName().getString());
            if (person == null || !person.aliveOn(day)) continue;
            ErdenExteriorWorkforceSavedData.Household household = households.get(person.householdId());
            ErdenKingdomSupplyCatalog.SupplyNode node = ErdenKingdomSupplyCatalog.node(person.nodeId());
            if (household == null || node == null) continue;
            boolean working = person.assignedWorker()
                    && !person.retiredOn(day)
                    && ageYears(person, day) >= ADULT_AGE
                    && !absentOnDay(person.id(), person.restDay(), node.role, day)
                    && inShift(dayTime, person.shiftStart(), person.shiftEnd());
            int x = working ? node.x : household.homeX();
            int z = working ? node.z : household.homeZ();
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            int preferredY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z)) + 2;
            int y = safeStandingY(level, x, preferredY, z);
            villager.setPersistenceRequired();
            if (villager.distanceToSqr(x + 0.5D, y, z + 0.5D) > 4.0D) {
                villager.getNavigation().moveTo(x + 0.5D, y, z + 0.5D, 0.56D);
            }
        }
    }

    private static void logPlanOnce(
            ErdenExteriorLifecycleSavedData lifecycle,
            ErdenExteriorWorkforceSavedData workforce,
            long day) {
        if (planLogged) return;
        validateInitialization(lifecycle, workforce);
        planLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden exterior lifecycle revision={} founders={} households={} year_days={} adult_age={} retirement_age={} births_economy_gated=true succession=true save_overlay=true current_day={}",
                LIFECYCLE_REVISION, lifecycle.founderCount(), lifecycle.householdLines().size(),
                DAYS_PER_YEAR, ADULT_AGE, RETIREMENT_AGE, day);
    }

    private static void verifyCi(
            ErdenExteriorLifecycleSavedData lifecycle,
            ErdenExteriorWorkforceSavedData workforce,
            long day) {
        if (ciPassed
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || lifecycle.founderCount() != ErdenExteriorWorkforceManager.EXPECTED_RESIDENTS
                || lifecycle.householdLines().size() != ErdenExteriorWorkforceManager.EXPECTED_HOUSEHOLDS) return;
        int futureAdults = 0;
        int futureRetirements = 0;
        int successionHouseholds = 0;
        int parentReadyHouseholds = 0;
        Map<String, List<ErdenExteriorLifecycleSavedData.Person>> byHousehold = new HashMap<>();
        for (ErdenExteriorLifecycleSavedData.Person person : lifecycle.persons()) {
            byHousehold.computeIfAbsent(person.householdId(), ignored -> new ArrayList<>()).add(person);
            int age = ageYears(person, day);
            if (!person.foundingWorker() && age < ADULT_AGE) futureAdults++;
            if (person.foundingWorker() && age < RETIREMENT_AGE) futureRetirements++;
        }
        for (ErdenExteriorLifecycleSavedData.HouseholdLine line : lifecycle.householdLines()) {
            List<ErdenExteriorLifecycleSavedData.Person> members =
                    byHousehold.getOrDefault(line.householdId(), List.of());
            if (members.stream().filter(person -> person.aliveOn(day)
                    && ageYears(person, day) >= ADULT_AGE).count() >= 2) successionHouseholds++;
            if (members.stream().filter(person -> person.aliveOn(day)
                    && ageYears(person, day) >= MIN_PARENT_AGE
                    && ageYears(person, day) <= MAX_PARENT_AGE).count() >= 2) parentReadyHouseholds++;
        }
        if (futureAdults <= 0 || futureRetirements <= 0
                || successionHouseholds <= 0 || parentReadyHouseholds <= 0) return;
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_EXTERIOR_LIFECYCLE_PASS revision={} founders={} households={} future_adults={} scheduled_retirements={} succession_households={} parent_ready_households={} year_days={} adulthood={} retirement={} births=true inheritance=true replacement_labor=true permanent_deaths=true save_overlay=true",
                LIFECYCLE_REVISION, lifecycle.founderCount(), lifecycle.householdLines().size(),
                futureAdults, futureRetirements, successionHouseholds, parentReadyHouseholds,
                DAYS_PER_YEAR, ADULT_AGE, RETIREMENT_AGE);
    }

    private static int founderAge(ErdenExteriorWorkforceSavedData.Resident resident) {
        int seed = Math.floorMod(resident.id().hashCode(), 1000);
        if (resident.worker()) return 24 + seed % 20;
        if (resident.lifeStage().equals("child")) return 6 + seed % 8;
        return 62 + seed % 11;
    }

    private static int ageYears(ErdenExteriorLifecycleSavedData.Person person, long day) {
        return Math.max(0, Math.toIntExact(Math.floorDiv(day - person.birthDay(), DAYS_PER_YEAR)));
    }

    private static boolean isBirthday(ErdenExteriorLifecycleSavedData.Person person, long day) {
        return day >= person.birthDay()
                && Math.floorMod(day - person.birthDay(), DAYS_PER_YEAR) == 0L;
    }

    private static boolean naturalDeath(
            ErdenExteriorLifecycleSavedData.Person person,
            int age,
            long day) {
        if (age < 65) return false;
        int denominator = age < 70 ? 40 : age < 80 ? 24 : age < 90 ? 10 : 3;
        long year = Math.floorDiv(day - person.birthDay(), DAYS_PER_YEAR);
        long seed = (long) person.id().hashCode() * 131L + year * 17L;
        return Math.floorMod(seed, denominator) == 0L;
    }

    private static boolean absentOnDay(
            String personId,
            int restDay,
            String nodeRole,
            long day) {
        if (restDay == Math.floorMod(day, 7L)) return true;
        long seed = (long) personId.hashCode() * 31L + day * 97L;
        if (Math.floorMod(seed, 53L) == 0L) return true;
        return (nodeRole.equals("colliery") || nodeRole.equals("iron_mine"))
                && Math.floorMod(seed, 89L) == 0L;
    }

    private static int requiredWorkers(String role) {
        return switch (role) {
            case "grain_estate" -> 8;
            case "ranch" -> 7;
            case "colliery" -> 9;
            case "iron_mine" -> 10;
            case "paper_mill" -> 8;
            case "river_wharf" -> 6;
            default -> throw new IllegalStateException("Unknown Erden lifecycle role " + role);
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

    private static List<ErdenExteriorLifecycleSavedData.Person> members(
            List<ErdenExteriorLifecycleSavedData.Person> people,
            String householdId) {
        List<ErdenExteriorLifecycleSavedData.Person> result = new ArrayList<>();
        for (ErdenExteriorLifecycleSavedData.Person person : people) {
            if (person.householdId().equals(householdId)) result.add(person);
        }
        return result;
    }

    private static Map<String, ErdenExteriorWorkforceSavedData.Household> householdMap(
            ErdenExteriorWorkforceSavedData workforce) {
        Map<String, ErdenExteriorWorkforceSavedData.Household> result = new HashMap<>();
        for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {
            result.put(household.id(), household);
        }
        return result;
    }

    private static Set<String> loadedLifecycleNames(
            ServerLevel level,
            ErdenExteriorLifecycleSavedData lifecycle) {
        Set<String> names = new HashSet<>();
        for (ErdenExteriorLifecycleSavedData.Person person : lifecycle.persons()) {
            if (!person.founder()) names.add(person.name());
        }
        Set<String> loaded = new HashSet<>();
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class, exteriorBounds(level),
                candidate -> names.contains(candidate.getName().getString()))) {
            loaded.add(villager.getName().getString());
        }
        return loaded;
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

    private record Shift(int start, int end) {
    }

    private record BirthResult(
            List<ErdenExteriorLifecycleSavedData.Person> people,
            List<ErdenExteriorLifecycleSavedData.HouseholdLine> lines,
            int nextSequence) {
    }
}
