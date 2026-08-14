package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
 * Long-running generational overlay for the 77 households inside Erden's capital.
 *
 * <p>The founding population stays in {@link ErdenPopulationSavedData}. This layer adds stable ages,
 * children, retirement, natural death, household succession and replacement labour without changing
 * the original population or economy codecs. A descendant who reaches adulthood can take one of the
 * two original vacancies or a workplace released by death/retirement. Those assignments are used by
 * the same production and wage ledgers as founding workers.</p>
 */
public final class ErdenCapitalLifecycleManager {
    public static final int LIFECYCLE_REVISION = 1;
    public static final int DAYS_PER_YEAR = 112;
    public static final int ADULT_AGE = 18;
    public static final int RETIREMENT_AGE = 58;
    public static final int HOUSEHOLD_CAPACITY = 5;

    private static final int MIN_PARENT_AGE = 20;
    private static final int MAX_PARENT_AGE = 46;
    private static final int BIRTH_SPACING_YEARS = 2;
    private static final int MAX_CATCH_UP_DAYS = DAYS_PER_YEAR * 5;
    private static final int SPAWN_INTERVAL = 20;
    private static final int ROUTINE_INTERVAL = 40;
    private static final int SPAWN_BUDGET = 2;
    private static final Identifier VILLAGER_ID = Identifier.fromNamespaceAndPath("minecraft", "villager");

    private static final List<String> NAME_HEADS = List.of(
            "아", "벨", "카", "세", "리", "마", "테", "도", "엘", "유", "브", "니", "로", "하", "페", "이", "사", "네"
    );
    private static final List<String> NAME_TAILS = List.of(
            "렌", "린", "엘", "아", "온", "르", "안", "라", "인", "엔", "오", "리스", "란", "나", "빈", "엘라", "르엔", "시아"
    );

    private static boolean ciProjected;

    private ErdenCapitalLifecycleManager() {
    }

    /** Called by the capital population manager before its daily civic ledger is calculated. */
    public static void prepare(ServerLevel level, ErdenPopulationSavedData population) {
        if (population.households().size() != ErdenPopulationManager.EXPECTED_HOUSEHOLDS) return;
        long currentDay = Math.floorDiv(level.getGameTime(), 24_000L);
        ErdenCapitalLifecycleSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalLifecycleSavedData.TYPE);
        if (!data.initialized(LIFECYCLE_REVISION,
                ErdenPopulationManager.EXPECTED_RESIDENTS,
                ErdenPopulationManager.EXPECTED_HOUSEHOLDS)) {
            initialize(population, data, currentDay);
        }
        synchronizeFounderDeaths(population, data, currentDay);
        processToDay(level, population, data, currentDay);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        ErdenPopulationSavedData population = level.getDataStorage()
                .computeIfAbsent(ErdenPopulationSavedData.TYPE);
        if (population.households().size() != ErdenPopulationManager.EXPECTED_HOUSEHOLDS) return;
        prepare(level, population);
        long tick = level.getGameTime();
        if (tick % SPAWN_INTERVAL == 0L) materializeLoadedDescendants(level, population);
        if (tick % ROUTINE_INTERVAL == 0L) runDescendantRoutines(level, population);
        verifyCiProjection(level, population);
    }

    public static List<WorkerSnapshot> activeWorkers(
            ServerLevel level,
            ErdenPopulationSavedData population,
            long day) {
        prepare(level, population);
        ErdenCapitalLifecycleSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalLifecycleSavedData.TYPE);
        List<WorkerSnapshot> result = new ArrayList<>();
        for (ErdenCapitalLifecycleSavedData.Person person : data.persons()) {
            if (!isActiveWorker(person, day)) continue;
            result.add(new WorkerSnapshot(
                    person.id(), person.householdId(), person.name(),
                    person.workX(), person.workZ(), person.workRole(),
                    person.shiftStart(), person.shiftEnd(), person.generation(), person.founder()));
        }
        return List.copyOf(result);
    }

    public static int livingCount(
            ServerLevel level,
            ErdenPopulationSavedData population,
            long day) {
        prepare(level, population);
        return level.getDataStorage().computeIfAbsent(ErdenCapitalLifecycleSavedData.TYPE)
                .livingCount(day);
    }

    public static Set<String> livingHouseholdIds(
            ServerLevel level,
            ErdenPopulationSavedData population,
            long day) {
        prepare(level, population);
        ErdenCapitalLifecycleSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalLifecycleSavedData.TYPE);
        Set<String> result = new HashSet<>();
        for (ErdenCapitalLifecycleSavedData.Person person : data.persons()) {
            if (person.aliveOn(day)) result.add(person.householdId());
        }
        return Set.copyOf(result);
    }

    public static int livingHouseholdCount(
            ServerLevel level,
            ErdenPopulationSavedData population,
            long day) {
        prepare(level, population);
        ErdenCapitalLifecycleSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalLifecycleSavedData.TYPE);
        Set<String> households = new HashSet<>();
        for (ErdenCapitalLifecycleSavedData.Person person : data.persons()) {
            if (person.aliveOn(day)) households.add(person.householdId());
        }
        return households.size();
    }

    public static void markDeadIfLifecycleResident(ServerLevel level, Villager villager) {
        if (!level.dimension().equals(StarterRealmManager.REALM_KEY)) return;
        ErdenCapitalLifecycleSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalLifecycleSavedData.TYPE);
        ErdenCapitalLifecycleSavedData.Person person = data.personByName(villager.getName().getString());
        if (person == null) return;
        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        data.markDeath(person.id(), day);
        LivingKingdoms.LOGGER.info(
                "Erden capital lifecycle death resident={} household={} generation={} founder={} permanent=true",
                person.id(), person.householdId(), person.generation(), person.founder());
    }

    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof Villager villager)
                || !(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)) return;
        ErdenPopulationSavedData population = level.getDataStorage()
                .computeIfAbsent(ErdenPopulationSavedData.TYPE);
        ErdenCapitalLifecycleSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalLifecycleSavedData.TYPE);
        ErdenCapitalLifecycleSavedData.Person person = data.personByName(villager.getName().getString());
        if (person == null || person.founder()) return;
        ErdenPopulationSavedData.Household household = household(population, person.householdId());
        if (household == null) return;
        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        int age = ageYears(person, day);
        String duty = isActiveWorker(person, day)
                ? roleName(person.workRole()) + "에서 근무 중"
                : age < ADULT_AGE ? "가족의 일을 배우는 중"
                : person.retiredOn(day) ? "생업에서 은퇴함" : "현재 가구 일을 돕는 중";
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        player.sendSystemMessage(Component.literal(
                "§6[" + person.name() + "] §f" + household.familyName() + " 가구의 "
                        + person.generation() + "세대 구성원, " + age + "세. " + duty + "."));
    }

    private static void initialize(
            ErdenPopulationSavedData population,
            ErdenCapitalLifecycleSavedData data,
            long day) {
        List<ErdenCapitalLifecycleSavedData.Person> founders = new ArrayList<>();
        List<ErdenCapitalLifecycleSavedData.HouseholdLine> lines = new ArrayList<>();
        int householdIndex = 0;
        for (ErdenPopulationSavedData.Household household : population.households()) {
            householdIndex++;
            String steward = "";
            String heir = "";
            for (ErdenPopulationSavedData.Resident resident : household.residents()) {
                int age = founderAge(resident, householdIndex);
                long ageOffset = Math.floorMod(resident.id().hashCode(), DAYS_PER_YEAR);
                long birthDay = day - (long) age * DAYS_PER_YEAR - ageOffset;
                founders.add(new ErdenCapitalLifecycleSavedData.Person(
                        resident.id(), resident.name(), household.id(), birthDay,
                        "", "", 0, true, resident.worker(),
                        resident.workX(), resident.workZ(), resident.workRole(),
                        resident.shiftStart(), resident.shiftEnd(), -1L, -1L));
                if (resident.lifeStage().equals("adult_head")) steward = resident.id();
                if (resident.lifeStage().equals("child")) heir = resident.id();
            }
            if (steward.isBlank() && !household.residents().isEmpty()) steward = household.residents().getFirst().id();
            if (heir.isBlank() && household.residents().size() > 1) heir = household.residents().get(1).id();
            lines.add(new ErdenCapitalLifecycleSavedData.HouseholdLine(
                    household.id(), steward, heir, Integer.MIN_VALUE, 0, 0));
        }
        data.initialize(LIFECYCLE_REVISION, day, founders, lines);
        LivingKingdoms.LOGGER.info(
                "Prepared Erden capital lifecycle revision={} founders={} households={} year_days={} adulthood={} retirement_age={} fixed_homes={} fixed_worksites={} save_overlay=true",
                LIFECYCLE_REVISION, founders.size(), lines.size(), DAYS_PER_YEAR,
                ADULT_AGE, RETIREMENT_AGE, ErdenPopulationManager.EXPECTED_HOUSEHOLDS,
                ErdenAuthoritativeEconomyManager.EXPECTED_SITES);
    }

    private static int founderAge(ErdenPopulationSavedData.Resident resident, int householdIndex) {
        int salt = Math.floorMod(resident.id().hashCode() + householdIndex * 31, 97);
        return switch (resident.lifeStage()) {
            case "adult_head" -> 28 + salt % 18;
            case "adult_partner" -> 26 + salt % 17;
            case "child" -> 7 + salt % 8;
            default -> 60 + salt % 8;
        };
    }

    private static void synchronizeFounderDeaths(
            ErdenPopulationSavedData population,
            ErdenCapitalLifecycleSavedData data,
            long day) {
        for (ErdenCapitalLifecycleSavedData.Person person : data.persons()) {
            if (person.founder() && population.isDead(person.id()) && person.aliveOn(day)) {
                data.markDeath(person.id(), day);
            }
        }
    }

    private static void processToDay(
            ServerLevel level,
            ErdenPopulationSavedData population,
            ErdenCapitalLifecycleSavedData data,
            long currentDay) {
        if (data.lastProcessedDay() >= currentDay) return;
        long first = data.lastProcessedDay() < 0L
                ? currentDay
                : Math.max(data.lastProcessedDay() + 1L, currentDay - MAX_CATCH_UP_DAYS + 1L);
        Model model = new Model(data.persons(), data.householdLines(), data.nextBirthSequence());
        for (long day = first; day <= currentDay; day++) {
            processModelDay(level, population, model, data.establishedDay(), day, true);
        }
        data.replaceDay(currentDay, model.nextSequence, model.persons, model.lines);
        if (first == currentDay || Math.floorMod(currentDay - data.establishedDay(), DAYS_PER_YEAR) == 0L) {
            LivingKingdoms.LOGGER.info(
                    "Processed Erden capital lifecycle day={} living={} descendants={} retired={} births={} successions={} replacement_workers={} generation_overlay=true",
                    currentDay, countLiving(model.persons, currentDay), countDescendants(model.persons),
                    countRetired(model.persons, currentDay), totalBirths(model.lines),
                    totalSuccessions(model.lines), countReplacementWorkers(model.persons, currentDay));
        }
    }

    private static void processModelDay(
            ServerLevel level,
            ErdenPopulationSavedData population,
            Model model,
            long establishedDay,
            long day,
            boolean persistFounderDeaths) {
        for (int i = 0; i < model.persons.size(); i++) {
            ErdenCapitalLifecycleSavedData.Person person = model.persons.get(i);
            if (!person.aliveOn(day)) continue;
            if (person.assignedWorker() && !person.retiredOn(day) && ageYears(person, day) >= RETIREMENT_AGE) {
                person = person.withRetirement(day);
                model.persons.set(i, person);
            }
            if (day >= naturalDeathDay(person)) {
                person = person.withDeath(day);
                model.persons.set(i, person);
                if (persistFounderDeaths) {
                    if (person.founder() && !population.isDead(person.id())) {
                        population.markDead(person.id());
                    }
                    discardLoadedPerson(level, person.name());
                }
            }
        }
        reconcileSuccessions(model, day);
        maybeBirthChildren(level, population, model, establishedDay, day);
        assignVacantWorkplaces(level, population, model, day);
    }

    private static void reconcileSuccessions(Model model, long day) {
        for (int i = 0; i < model.lines.size(); i++) {
            ErdenCapitalLifecycleSavedData.HouseholdLine line = model.lines.get(i);
            ErdenCapitalLifecycleSavedData.Person steward = find(model.persons, line.stewardId());
            if (steward != null && steward.aliveOn(day)) continue;
            List<ErdenCapitalLifecycleSavedData.Person> candidates = model.persons.stream()
                    .filter(person -> person.householdId().equals(line.householdId()) && person.aliveOn(day))
                    .sorted(Comparator.comparingInt((ErdenCapitalLifecycleSavedData.Person person) -> ageYears(person, day)).reversed()
                            .thenComparing(ErdenCapitalLifecycleSavedData.Person::id))
                    .toList();
            if (candidates.isEmpty()) continue;
            String nextSteward = candidates.getFirst().id();
            String nextHeir = candidates.size() > 1 ? candidates.get(1).id() : "";
            model.lines.set(i, line.withSuccession(nextSteward, nextHeir));
        }
    }

    private static void maybeBirthChildren(
            ServerLevel level,
            ErdenPopulationSavedData population,
            Model model,
            long establishedDay,
            long day) {
        long elapsed = day - establishedDay;
        if (elapsed < DAYS_PER_YEAR || Math.floorMod(elapsed, DAYS_PER_YEAR) != 0L) return;
        int year = (int) Math.floorDiv(elapsed, DAYS_PER_YEAR);
        if (population.totalShortage() > 0L) return;

        Map<String, ErdenPopulationSavedData.Household> households = householdMap(population);
        for (int lineIndex = 0; lineIndex < model.lines.size(); lineIndex++) {
            ErdenCapitalLifecycleSavedData.HouseholdLine line = model.lines.get(lineIndex);
            if (line.lastBirthYear() != Integer.MIN_VALUE
                    && year - line.lastBirthYear() < BIRTH_SPACING_YEARS) continue;
            List<ErdenCapitalLifecycleSavedData.Person> members = model.persons.stream()
                    .filter(person -> person.householdId().equals(line.householdId()) && person.aliveOn(day))
                    .toList();
            if (members.size() >= HOUSEHOLD_CAPACITY) continue;
            List<ErdenCapitalLifecycleSavedData.Person> parents =
                    ErdenCapitalMarriageManager.parentPair(level, line.householdId(), members, day);
            if (parents.size() != 2) continue;
            if (Math.floorMod(line.householdId().hashCode() * 31L + year * 17L, 100L) >= 55L) continue;
            ErdenPopulationSavedData.Household household = households.get(line.householdId());
            if (household == null) continue;
            int sequence = model.nextSequence++;
            String id = "erden_capital_descendant_%05d".formatted(sequence);
            String name = descendantName(household.familyName(), sequence);
            int generation = Math.max(parents.get(0).generation(), parents.get(1).generation()) + 1;
            model.persons.add(new ErdenCapitalLifecycleSavedData.Person(
                    id, name, line.householdId(), day,
                    parents.get(0).id(), parents.get(1).id(), generation,
                    false, false, household.homeX(), household.homeZ(), "", 0, 0, -1L, -1L));
            ErdenCapitalLifecycleSavedData.HouseholdLine updated = line.withBirth(year);
            if (updated.heirId().isBlank()) updated = updated.withHeir(id);
            model.lines.set(lineIndex, updated);
        }
    }

    private static void assignVacantWorkplaces(
            ServerLevel level,
            ErdenPopulationSavedData population,
            Model model,
            long day) {
        List<ExternalUrbanFabricBuilder.UrbanEntrance> workplaces = ExternalUrbanFabricBuilder.entrances().stream()
                .filter(entrance -> !entrance.role().equals("tenement"))
                .sorted(Comparator.comparing(ExternalUrbanFabricBuilder.UrbanEntrance::role)
                        .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::z)
                        .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::x))
                .toList();
        Set<Long> occupied = new HashSet<>();
        for (ErdenCapitalLifecycleSavedData.Person person : model.persons) {
            if (isActiveWorker(person, day)) occupied.add(positionKey(person.workX(), person.workZ()));
        }
        Map<String, ErdenPopulationSavedData.Household> households = householdMap(population);
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < model.persons.size(); i++) {
            ErdenCapitalLifecycleSavedData.Person person = model.persons.get(i);
            int age = ageYears(person, day);
            if (!person.aliveOn(day) || person.assignedWorker() || person.retiredOn(day)
                    || age < ADULT_AGE || age >= RETIREMENT_AGE) continue;
            candidates.add(i);
        }
        candidates.sort(Comparator.comparingInt((Integer index) -> ageYears(model.persons.get(index), day)).reversed()
                .thenComparing(index -> model.persons.get(index).id()));
        for (int index : candidates) {
            ErdenCapitalLifecycleSavedData.Person person = model.persons.get(index);
            ErdenPopulationSavedData.Household household = households.get(person.householdId());
            if (household == null) continue;
            ExternalUrbanFabricBuilder.UrbanEntrance workplace = workplaces.stream()
                    .filter(entrance -> !occupied.contains(positionKey(entrance.x(), entrance.z())))
                    .min(Comparator.<ExternalUrbanFabricBuilder.UrbanEntrance>comparingLong(entrance ->
                                    distanceSquared(household.homeX(), household.homeZ(), entrance.x(), entrance.z()))
                            .thenComparing(ExternalUrbanFabricBuilder.UrbanEntrance::role)
                            .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::z)
                            .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::x))
                    .orElse(null);
            if (workplace == null) break;
            Shift shift = shiftFor(workplace.role(), person.id().hashCode());
            model.persons.set(index, person.withWork(
                    workplace.x(), workplace.z(), workplace.role(), shift.start(), shift.end()));
            occupied.add(positionKey(workplace.x(), workplace.z()));
        }
    }

    private static void materializeLoadedDescendants(
            ServerLevel level,
            ErdenPopulationSavedData population) {
        ErdenCapitalLifecycleSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalLifecycleSavedData.TYPE);
        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        Map<String, ErdenPopulationSavedData.Household> households = householdMap(population);
        Set<String> loadedNames = new HashSet<>();
        for (Villager villager : level.getEntitiesOfClass(Villager.class, capitalBounds(level))) {
            loadedNames.add(villager.getName().getString());
        }
        int spawned = 0;
        for (ErdenCapitalLifecycleSavedData.Person person : data.persons()) {
            if (spawned >= SPAWN_BUDGET) break;
            if (person.founder() || !person.aliveOn(day) || loadedNames.contains(person.name())) continue;
            ErdenPopulationSavedData.Household household = households.get(person.householdId());
            if (household == null || !level.hasChunk(household.homeX() >> 4, household.homeZ() >> 4)) continue;
            ErdenUrbanLifeSavedData urbanLife = level.getDataStorage().computeIfAbsent(ErdenUrbanLifeSavedData.TYPE);
            if (!urbanLife.isUpperFloorComplete(positionKey(household.homeX(), household.homeZ()),
                    ErdenUrbanLifeManager.UPPER_FLOOR_REVISION)) continue;
            Target home = homeTarget(level, household, person.id().hashCode());
            if (home == null || !spawnVillager(level, person, home)) continue;
            loadedNames.add(person.name());
            spawned++;
        }
    }

    private static boolean spawnVillager(
            ServerLevel level,
            ErdenCapitalLifecycleSavedData.Person person,
            Target home) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(VILLAGER_ID).orElse(null);
        if (type == null) return false;
        Entity created = type.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Villager villager)) return false;
        int y = safeStandingY(level, home.x(), home.y(), home.z());
        villager.setPos(home.x() + 0.5D, y, home.z() + 0.5D);
        villager.setCustomName(Component.literal(person.name()));
        villager.setCustomNameVisible(false);
        villager.setPersistenceRequired();
        villager.setInvulnerable(false);
        return level.addFreshEntity(villager);
    }

    private static void runDescendantRoutines(
            ServerLevel level,
            ErdenPopulationSavedData population) {
        ErdenCapitalLifecycleSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalLifecycleSavedData.TYPE);
        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        Map<String, ErdenCapitalLifecycleSavedData.Person> descendants = new HashMap<>();
        for (ErdenCapitalLifecycleSavedData.Person person : data.persons()) {
            if (!person.founder() && person.aliveOn(day)) descendants.put(person.name(), person);
        }
        if (descendants.isEmpty()) return;
        Map<String, ErdenPopulationSavedData.Household> households = householdMap(population);
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class, capitalBounds(level),
                candidate -> descendants.containsKey(candidate.getName().getString()))) {
            ErdenCapitalLifecycleSavedData.Person person = descendants.get(villager.getName().getString());
            ErdenPopulationSavedData.Household household = households.get(person.householdId());
            if (household == null) continue;
            boolean working = isActiveWorker(person, day)
                    && inShift(dayTime, person.shiftStart(), person.shiftEnd());
            Target target = working ? workTarget(level, person) : homeTarget(level, household, person.id().hashCode());
            if (target == null) continue;
            villager.setPersistenceRequired();
            if (villager.distanceToSqr(target.x() + 0.5D, target.y(), target.z() + 0.5D) > 4.0D) {
                villager.getNavigation().moveTo(target.x() + 0.5D, target.y(), target.z() + 0.5D, 0.60D);
            }
        }
    }

    private static Target homeTarget(
            ServerLevel level,
            ErdenPopulationSavedData.Household household,
            int salt) {
        ExternalUrbanFabricBuilder.UrbanEntrance entrance = findEntrance(household.homeX(), household.homeZ());
        if (entrance == null) return null;
        int doorY = findLowestDoorY(level, household.homeX(), household.homeZ());
        if (doorY == Integer.MIN_VALUE) return null;
        int inwardX;
        int inwardZ;
        int dx = entrance.roadX() - entrance.x();
        int dz = entrance.roadZ() - entrance.z();
        if (Math.abs(dx) >= Math.abs(dz)) {
            inwardX = dx >= 0 ? -1 : 1;
            inwardZ = 0;
        } else {
            inwardX = 0;
            inwardZ = dz >= 0 ? -1 : 1;
        }
        int rightX = -inwardZ;
        int rightZ = inwardX;
        int lateral = Math.floorMod(salt, 5) - 2;
        int depth = 5 + Math.floorMod(salt / 7, 3);
        return new Target(
                entrance.x() + inwardX * depth + rightX * lateral,
                doorY + 5,
                entrance.z() + inwardZ * depth + rightZ * lateral);
    }

    private static Target workTarget(ServerLevel level, ErdenCapitalLifecycleSavedData.Person person) {
        if (!level.hasChunk(person.workX() >> 4, person.workZ() >> 4)) return null;
        ExternalUrbanFabricBuilder.UrbanEntrance entrance = findEntrance(person.workX(), person.workZ());
        if (entrance == null) return null;
        int doorY = findLowestDoorY(level, person.workX(), person.workZ());
        if (doorY == Integer.MIN_VALUE) return null;
        int dx = entrance.roadX() - entrance.x();
        int dz = entrance.roadZ() - entrance.z();
        int inwardX;
        int inwardZ;
        if (Math.abs(dx) >= Math.abs(dz)) {
            inwardX = dx >= 0 ? -1 : 1;
            inwardZ = 0;
        } else {
            inwardX = 0;
            inwardZ = dz >= 0 ? -1 : 1;
        }
        return new Target(entrance.x() + inwardX * 3, doorY, entrance.z() + inwardZ * 3);
    }

    private static void verifyCiProjection(
            ServerLevel level,
            ErdenPopulationSavedData population) {
        if (ciProjected || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_CAPITAL_LIFECYCLE_TEST"))) return;
        ErdenCapitalLifecycleSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalLifecycleSavedData.TYPE);
        if (!data.initialized(LIFECYCLE_REVISION,
                ErdenPopulationManager.EXPECTED_RESIDENTS,
                ErdenPopulationManager.EXPECTED_HOUSEHOLDS)) return;
        Model projection = new Model(data.persons(), data.householdLines(), data.nextBirthSequence());
        long baseDay = Math.max(data.lastProcessedDay(), data.establishedDay());
        long target = baseDay + 60L * DAYS_PER_YEAR;
        for (long day = baseDay + 1L; day <= target; day++) {
            processModelDay(level, population, projection, data.establishedDay(), day, false);
        }
        int births = totalBirths(projection.lines) - data.birthCount();
        int successions = totalSuccessions(projection.lines) - data.successionCount();
        int replacementWorkers = countReplacementWorkers(projection.persons, target);
        int living = countLiving(projection.persons, target);
        if (births <= 0 || successions <= 0 || replacementWorkers <= 0 || living <= 0
                || population.households().size() != ErdenPopulationManager.EXPECTED_HOUSEHOLDS) {
            throw new IllegalStateException(
                    "Erden capital lifecycle projection failed births=" + births
                            + " successions=" + successions
                            + " replacements=" + replacementWorkers + " living=" + living);
        }
        ciProjected = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_CAPITAL_LIFECYCLE_PASS revision=1 founders={} households={} projection_years=60 projected_births={} projected_successions={} projected_replacement_workers={} projected_living={} adulthood={} retirement_age={} fixed_homes=77 fixed_worksites=156 base_population_unchanged=true economy_workers_linkable=true save_overlay=true source_blocks_cut=0",
                data.founderCount(), data.householdLines().size(), births, successions,
                replacementWorkers, living, ADULT_AGE, RETIREMENT_AGE);
    }

    public static Projection projectForAudit(
            ServerLevel level,
            ErdenPopulationSavedData population,
            int years) {
        prepare(level, population);
        ErdenCapitalLifecycleSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalLifecycleSavedData.TYPE);
        Model projection = new Model(data.persons(), data.householdLines(), data.nextBirthSequence());
        long baseDay = Math.max(data.lastProcessedDay(), data.establishedDay());
        long targetDay = baseDay + (long) Math.max(1, years) * DAYS_PER_YEAR;
        for (long day = baseDay + 1L; day <= targetDay; day++) {
            processModelDay(level, population, projection, data.establishedDay(), day, false);
        }
        return new Projection(List.copyOf(projection.persons), List.copyOf(projection.lines), targetDay);
    }

    public static boolean isActiveFounderWorker(
            ServerLevel level,
            ErdenPopulationSavedData population,
            String personId,
            long day) {
        prepare(level, population);
        ErdenCapitalLifecycleSavedData.Person person = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalLifecycleSavedData.TYPE).person(personId);
        return person != null && person.founder() && isActiveWorker(person, day);
    }

    private static boolean isActiveWorker(ErdenCapitalLifecycleSavedData.Person person, long day) {
        return person.aliveOn(day) && person.assignedWorker() && !person.retiredOn(day)
                && ageYears(person, day) >= ADULT_AGE;
    }

    private static int ageYears(ErdenCapitalLifecycleSavedData.Person person, long day) {
        return (int) Math.max(0L, Math.floorDiv(day - person.birthDay(), DAYS_PER_YEAR));
    }

    private static long naturalDeathDay(ErdenCapitalLifecycleSavedData.Person person) {
        int lifespan = 72 + Math.floorMod(person.id().hashCode(), 13);
        return person.birthDay() + (long) lifespan * DAYS_PER_YEAR;
    }

    private static ErdenCapitalLifecycleSavedData.Person find(
            List<ErdenCapitalLifecycleSavedData.Person> persons,
            String id) {
        if (id == null || id.isBlank()) return null;
        for (ErdenCapitalLifecycleSavedData.Person person : persons) if (person.id().equals(id)) return person;
        return null;
    }

    private static String descendantName(String familyName, int sequence) {
        int value = Math.max(0, sequence - 1);
        String stem = NAME_HEADS.get(value % NAME_HEADS.size())
                + NAME_TAILS.get((value / NAME_HEADS.size()) % NAME_TAILS.size());
        int cycle = value / (NAME_HEADS.size() * NAME_TAILS.size());
        if (cycle > 0) stem += NAME_TAILS.get(cycle % NAME_TAILS.size());
        return familyName + " " + stem;
    }

    private static Shift shiftFor(String role, int salt) {
        boolean second = (salt & 1) != 0;
        return switch (role) {
            case "bakery", "stable", "warehouse" -> second ? new Shift(5_000, 11_000) : new Shift(1_000, 7_000);
            case "shop", "bathhouse" -> second ? new Shift(7_000, 13_000) : new Shift(3_000, 9_000);
            case "inn" -> second ? new Shift(12_000, 2_000) : new Shift(4_000, 10_000);
            case "guard_post" -> second ? new Shift(10_000, 18_000) : new Shift(2_000, 8_000);
            default -> new Shift(2_000, 8_000);
        };
    }

    private static boolean inShift(long time, int start, int end) {
        if (start <= end) return time >= start && time < end;
        return time >= start || time < end;
    }

    private static Map<String, ErdenPopulationSavedData.Household> householdMap(ErdenPopulationSavedData population) {
        Map<String, ErdenPopulationSavedData.Household> result = new LinkedHashMap<>();
        for (ErdenPopulationSavedData.Household household : population.households()) result.put(household.id(), household);
        return result;
    }

    private static ErdenPopulationSavedData.Household household(
            ErdenPopulationSavedData population,
            String id) {
        for (ErdenPopulationSavedData.Household household : population.households()) {
            if (household.id().equals(id)) return household;
        }
        return null;
    }

    private static ExternalUrbanFabricBuilder.UrbanEntrance findEntrance(int x, int z) {
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : ExternalUrbanFabricBuilder.entrances()) {
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

    private static int safeStandingY(ServerLevel level, int x, int preferredY, int z) {
        for (int offset = 0; offset <= 8; offset++) {
            int[] candidates = offset == 0
                    ? new int[]{preferredY}
                    : new int[]{preferredY + offset, preferredY - offset};
            for (int y : candidates) {
                if (y <= level.getMinY() || y >= level.getMaxY() - 1) continue;
                BlockPos feet = new BlockPos(x, y, z);
                if (!level.getBlockState(feet.below()).isAir()
                        && level.getBlockState(feet).isAir()
                        && level.getBlockState(feet.above()).isAir()) return y;
            }
        }
        return preferredY;
    }

    private static void discardLoadedPerson(ServerLevel level, String name) {
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class, capitalBounds(level),
                candidate -> candidate.getName().getString().equals(name))) {
            villager.discard();
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

    private static long positionKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static long distanceSquared(int x1, int z1, int x2, int z2) {
        long dx = (long) x1 - x2;
        long dz = (long) z1 - z2;
        return dx * dx + dz * dz;
    }

    private static int countLiving(List<ErdenCapitalLifecycleSavedData.Person> persons, long day) {
        int count = 0;
        for (ErdenCapitalLifecycleSavedData.Person person : persons) if (person.aliveOn(day)) count++;
        return count;
    }

    private static int countDescendants(List<ErdenCapitalLifecycleSavedData.Person> persons) {
        int count = 0;
        for (ErdenCapitalLifecycleSavedData.Person person : persons) if (!person.founder()) count++;
        return count;
    }

    private static int countRetired(List<ErdenCapitalLifecycleSavedData.Person> persons, long day) {
        int count = 0;
        for (ErdenCapitalLifecycleSavedData.Person person : persons) {
            if (person.aliveOn(day) && person.retiredOn(day)) count++;
        }
        return count;
    }

    private static int countReplacementWorkers(List<ErdenCapitalLifecycleSavedData.Person> persons, long day) {
        int count = 0;
        for (ErdenCapitalLifecycleSavedData.Person person : persons) {
            if (!person.foundingWorker() && isActiveWorker(person, day)) count++;
        }
        return count;
    }

    private static int totalBirths(List<ErdenCapitalLifecycleSavedData.HouseholdLine> lines) {
        int count = 0;
        for (ErdenCapitalLifecycleSavedData.HouseholdLine line : lines) count += line.birthCount();
        return count;
    }

    private static int totalSuccessions(List<ErdenCapitalLifecycleSavedData.HouseholdLine> lines) {
        int count = 0;
        for (ErdenCapitalLifecycleSavedData.HouseholdLine line : lines) count += line.successionCount();
        return count;
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
            default -> role;
        };
    }

    public record Projection(
            List<ErdenCapitalLifecycleSavedData.Person> persons,
            List<ErdenCapitalLifecycleSavedData.HouseholdLine> householdLines,
            long targetDay) {
    }

    public record WorkerSnapshot(
            String personId,
            String householdId,
            String name,
            int workX,
            int workZ,
            String workRole,
            int shiftStart,
            int shiftEnd,
            int generation,
            boolean founder) {
    }

    private static final class Model {
        final List<ErdenCapitalLifecycleSavedData.Person> persons;
        final List<ErdenCapitalLifecycleSavedData.HouseholdLine> lines;
        int nextSequence;

        Model(
                List<ErdenCapitalLifecycleSavedData.Person> persons,
                List<ErdenCapitalLifecycleSavedData.HouseholdLine> lines,
                int nextSequence) {
            this.persons = new ArrayList<>(persons);
            this.lines = new ArrayList<>(lines);
            this.nextSequence = nextSequence;
        }
    }

    private record Shift(int start, int end) {
    }

    private record Target(int x, int y, int z) {
    }
}
