from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms"
LIFE = BASE / "world/ErdenCapitalLifecycleManager.java"
POP = BASE / "world/ErdenPopulationManager.java"
FIRE = BASE / "world/ErdenFireResponseManager.java"
JUSTICE = BASE / "crime/ErdenJusticeManager.java"


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(message)


# Lifecycle API + physical disappearance when a simulated death occurs.
s = LIFE.read_text(encoding="utf-8")
if "public static boolean isActiveFounderWorker(" not in s:
    anchor = '''    private static boolean isActiveWorker(ErdenCapitalLifecycleSavedData.Person person, long day) {
'''
    helper = '''    public static boolean isActiveFounderWorker(
            ServerLevel level,
            ErdenPopulationSavedData population,
            String personId,
            long day) {
        prepare(level, population);
        ErdenCapitalLifecycleSavedData.Person person = level.getDataStorage()
                .computeIfAbsent(ErdenCapitalLifecycleSavedData.TYPE).person(personId);
        return person != null && person.founder() && isActiveWorker(person, day);
    }

'''
    require(anchor in s, "lifecycle active-worker anchor missing")
    s = s.replace(anchor, helper + anchor, 1)

old_death = '''            if (day >= naturalDeathDay(person)) {
                person = person.withDeath(day);
                model.persons.set(i, person);
                if (persistFounderDeaths && person.founder() && !population.isDead(person.id())) {
                    population.markDead(person.id());
                }
            }'''
new_death = '''            if (day >= naturalDeathDay(person)) {
                person = person.withDeath(day);
                model.persons.set(i, person);
                if (persistFounderDeaths) {
                    if (person.founder() && !population.isDead(person.id())) {
                        population.markDead(person.id());
                    }
                    discardLoadedPerson(level, person.name());
                }
            }'''
if "discardLoadedPerson(level, person.name());" not in s:
    require(old_death in s, "lifecycle natural-death anchor missing")
    s = s.replace(old_death, new_death, 1)

if "private static void discardLoadedPerson(" not in s:
    anchor = '''    private static AABB capitalBounds(ServerLevel level) {
'''
    helper = '''    private static void discardLoadedPerson(ServerLevel level, String name) {
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class, capitalBounds(level),
                candidate -> candidate.getName().getString().equals(name))) {
            villager.discard();
        }
    }

'''
    require(anchor in s, "lifecycle capital-bounds anchor missing")
    s = s.replace(anchor, helper + anchor, 1)
LIFE.write_text(s, encoding="utf-8")

# Founding residents stop commuting and report retirement instead of pretending to work forever.
p = POP.read_text(encoding="utf-8")
old_working = '''        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        for (Villager villager : villagers) {
            ResidentRef reference = residents.get(villager.getName().getString());
            if (reference == null || population.isDead(reference.resident.id())) continue;
            boolean working = reference.resident.worker()
                    && inShift(dayTime,
                    reference.resident.shiftStart(), reference.resident.shiftEnd());'''
new_working = '''        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        long lifecycleDay = Math.floorDiv(level.getGameTime(), 24_000L);
        for (Villager villager : villagers) {
            ResidentRef reference = residents.get(villager.getName().getString());
            if (reference == null || population.isDead(reference.resident.id())) continue;
            boolean working = reference.resident.worker()
                    && ErdenCapitalLifecycleManager.isActiveFounderWorker(
                    level, population, reference.resident.id(), lifecycleDay)
                    && inShift(dayTime,
                    reference.resident.shiftStart(), reference.resident.shiftEnd());'''
if "long lifecycleDay = Math.floorDiv(level.getGameTime(), 24_000L);" not in p:
    require(old_working in p, "population routine working anchor missing")
    p = p.replace(old_working, new_working, 1)

old_interaction = '''        String message;
        if (resident.worker()) {
            message = reference.household.familyName() + " 가구의 구성원입니다. "
                    + roleName(resident.workRole()) + "에서 "
                    + shiftName(resident.shiftStart(), resident.shiftEnd())
                    + " 근무를 맡고 있습니다.";
        } else if (resident.lifeStage().equals("child")) {'''
new_interaction = '''        String message;
        long lifecycleDay = Math.floorDiv(level.getGameTime(), 24_000L);
        boolean activeWorker = resident.worker()
                && ErdenCapitalLifecycleManager.isActiveFounderWorker(
                level, level.getDataStorage().computeIfAbsent(ErdenPopulationSavedData.TYPE),
                resident.id(), lifecycleDay);
        if (activeWorker) {
            message = reference.household.familyName() + " 가구의 구성원입니다. "
                    + roleName(resident.workRole()) + "에서 "
                    + shiftName(resident.shiftStart(), resident.shiftEnd())
                    + " 근무를 맡고 있습니다.";
        } else if (resident.worker()) {
            message = reference.household.familyName()
                    + " 가구의 어른입니다. 생업에서는 은퇴했고 집안과 이웃 일을 돕고 있습니다.";
        } else if (resident.lifeStage().equals("child")) {'''
if "boolean activeWorker = resident.worker()" not in p:
    require(old_interaction in p, "population interaction anchor missing")
    p = p.replace(old_interaction, new_interaction, 1)
POP.write_text(p, encoding="utf-8")

# Retired guard-post founders remain citizens/witnesses, but cannot be selected as active guards.
f = FIRE.read_text(encoding="utf-8")
old_fire = '''                if (!resident.workRole().equals("guard_post") || population.isDead(resident.id())) continue;
                guardNames.add(resident.name());'''
new_fire = '''                long day = Math.floorDiv(level.getGameTime(), 24_000L);
                if (!resident.workRole().equals("guard_post")
                        || population.isDead(resident.id())
                        || !ErdenCapitalLifecycleManager.isActiveFounderWorker(
                        level, population, resident.id(), day)) continue;
                guardNames.add(resident.name());'''
if "!ErdenCapitalLifecycleManager.isActiveFounderWorker" not in f:
    require(old_fire in f, "fire guard roster anchor missing")
    f = f.replace(old_fire, new_fire, 1)
FIRE.write_text(f, encoding="utf-8")

j = JUSTICE.read_text(encoding="utf-8")
old_guard_loop = '''        for (ErdenPopulationSavedData.Resident resident : roster.values()) {
            if (resident.workRole().equals("guard_post")) guards.add(resident.name());
        }'''
new_guard_loop = '''        long lifecycleDay = Math.floorDiv(level.getGameTime(), 24_000L);
        for (ErdenPopulationSavedData.Resident resident : roster.values()) {
            if (resident.workRole().equals("guard_post")
                    && ErdenCapitalLifecycleManager.isActiveFounderWorker(
                    level, population, resident.id(), lifecycleDay)) {
                guards.add(resident.name());
            }
        }'''
count = j.count(old_guard_loop)
require(count >= 1 or "ErdenCapitalLifecycleManager.isActiveFounderWorker" in j,
        "justice guard roster anchor missing")
if "ErdenCapitalLifecycleManager.isActiveFounderWorker" not in j:
    j = j.replace(old_guard_loop, new_guard_loop)
    if "import kr.moonseungjun.livingkingdoms.world.ErdenCapitalLifecycleManager;" not in j:
        anchor = "import kr.moonseungjun.livingkingdoms.world.ErdenCapitalStreamingBuilder;\n"
        require(anchor in j, "justice import anchor missing")
        j = j.replace(anchor, anchor + "import kr.moonseungjun.livingkingdoms.world.ErdenCapitalLifecycleManager;\n", 1)
JUSTICE.write_text(j, encoding="utf-8")

for path, token in [
    (LIFE, "discardLoadedPerson(level, person.name())"),
    (POP, "isActiveFounderWorker"),
    (FIRE, "isActiveFounderWorker"),
    (JUSTICE, "isActiveFounderWorker"),
]:
    require(token in path.read_text(encoding="utf-8"), f"runtime lifecycle token missing in {path.name}")

print("Aligned founder retirement/death with physical routines, fire response and civic guard selection.")
