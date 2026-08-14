from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms"
SAVED = BASE / "world/ErdenCapitalLifecycleSavedData.java"
LIFE = BASE / "world/ErdenCapitalLifecycleManager.java"
MOD = BASE / "LivingKingdoms.java"


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(message)


s = SAVED.read_text(encoding="utf-8")
old_work = '''        public Person withWork(int x, int z, String role, int start, int end) {
            return new Person(id, name, householdId, birthDay, parentA, parentB,
                    generation, founder, foundingWorker, x, z, role, start, end,
                    retirementDay, deathDay);
        }
'''
new_work = old_work + '''
        public Person withHousehold(String household) {
            return new Person(id, name, household, birthDay, parentA, parentB,
                    generation, founder, foundingWorker, workX, workZ, workRole,
                    shiftStart, shiftEnd, retirementDay, deathDay);
        }
'''
if "public Person withHousehold(String household)" not in s:
    require(old_work in s, "capital lifecycle Person.withWork anchor missing")
    s = s.replace(old_work, new_work, 1)

old_mark = '''    public void markDeath(String personId, long day) {
        for (int i = 0; i < persons.size(); i++) {
            Person person = persons.get(i);
            if (!person.id().equals(personId) || !person.aliveOn(day)) continue;
            persons.set(i, person.withDeath(day));
            setDirty();
            return;
        }
    }
'''
new_mark = old_mark + '''
    public boolean movePersonHousehold(String personId, String targetHousehold) {
        if (targetHousehold == null || targetHousehold.isBlank()) return false;
        for (int i = 0; i < persons.size(); i++) {
            Person person = persons.get(i);
            if (!person.id().equals(personId) || person.founder()) continue;
            if (person.householdId().equals(targetHousehold)) return true;
            persons.set(i, person.withHousehold(targetHousehold));
            setDirty();
            return true;
        }
        return false;
    }
'''
if "public boolean movePersonHousehold(" not in s:
    require(old_mark in s, "capital lifecycle markDeath anchor missing")
    s = s.replace(old_mark, new_mark, 1)
SAVED.write_text(s, encoding="utf-8")

l = LIFE.read_text(encoding="utf-8")
old_call = '''        reconcileSuccessions(model, day);
        maybeBirthChildren(population, model, establishedDay, day);
        assignVacantWorkplaces(level, population, model, day);'''
new_call = '''        reconcileSuccessions(model, day);
        maybeBirthChildren(level, population, model, establishedDay, day);
        assignVacantWorkplaces(level, population, model, day);'''
if "maybeBirthChildren(level, population, model, establishedDay, day);" not in l:
    require(old_call in l, "capital lifecycle birth call anchor missing")
    l = l.replace(old_call, new_call, 1)

old_sig = '''    private static void maybeBirthChildren(
            ErdenPopulationSavedData population,
            Model model,
            long establishedDay,
            long day) {'''
new_sig = '''    private static void maybeBirthChildren(
            ServerLevel level,
            ErdenPopulationSavedData population,
            Model model,
            long establishedDay,
            long day) {'''
if "private static void maybeBirthChildren(\n            ServerLevel level," not in l:
    require(old_sig in l, "capital lifecycle birth signature anchor missing")
    l = l.replace(old_sig, new_sig, 1)

old_parents = '''            List<ErdenCapitalLifecycleSavedData.Person> parents = members.stream()
                    .filter(ErdenCapitalLifecycleSavedData.Person::founder)
                    .filter(person -> {
                        int age = ageYears(person, day);
                        return age >= MIN_PARENT_AGE && age <= MAX_PARENT_AGE;
                    })
                    .sorted(Comparator.comparing(ErdenCapitalLifecycleSavedData.Person::id))
                    .limit(2)
                    .toList();'''
new_parents = '''            List<ErdenCapitalLifecycleSavedData.Person> parents =
                    ErdenCapitalMarriageManager.parentPair(level, line.householdId(), members, day);'''
if "ErdenCapitalMarriageManager.parentPair" not in l:
    require(old_parents in l, "capital lifecycle founder-parent block missing")
    l = l.replace(old_parents, new_parents, 1)

if "public static Projection projectForAudit(" not in l:
    anchor = '''    public static boolean isActiveFounderWorker(
'''
    helper = '''    public static Projection projectForAudit(
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

'''
    require(anchor in l, "capital lifecycle audit projection anchor missing")
    l = l.replace(anchor, helper + anchor, 1)

if "public record Projection(" not in l:
    anchor = '''    public record WorkerSnapshot(
'''
    record = '''    public record Projection(
            List<ErdenCapitalLifecycleSavedData.Person> persons,
            List<ErdenCapitalLifecycleSavedData.HouseholdLine> householdLines,
            long targetDay) {
    }

'''
    require(anchor in l, "capital lifecycle WorkerSnapshot anchor missing")
    l = l.replace(anchor, record + anchor, 1)
LIFE.write_text(l, encoding="utf-8")

m = MOD.read_text(encoding="utf-8")
if "import kr.moonseungjun.livingkingdoms.world.ErdenCapitalMarriageManager;" not in m:
    anchor = "import kr.moonseungjun.livingkingdoms.world.ErdenCapitalLifecycleManager;\n"
    require(anchor in m, "LivingKingdoms lifecycle import anchor missing")
    m = m.replace(anchor, anchor + "import kr.moonseungjun.livingkingdoms.world.ErdenCapitalMarriageManager;\n", 1)
if "ErdenCapitalMarriageManager.onServerTick(event);" not in m:
    anchor = "        ErdenCapitalLifecycleManager.onServerTick(event);\n"
    require(anchor in m, "LivingKingdoms lifecycle tick anchor missing")
    m = m.replace(anchor, anchor + "        ErdenCapitalMarriageManager.onServerTick(event);\n", 1)
MOD.write_text(m, encoding="utf-8")

for path, tokens in {
    SAVED: ["withHousehold", "movePersonHousehold"],
    LIFE: ["ErdenCapitalMarriageManager.parentPair", "public static Projection projectForAudit", "public record Projection"],
    MOD: ["ErdenCapitalMarriageManager.onServerTick(event)"],
}.items():
    text = path.read_text(encoding="utf-8")
    for token in tokens:
        require(token in text, f"missing capital marriage integration token {token} in {path.name}")

print("Wired capital marriage residence, married parentage and lifecycle projection into the runtime.")
