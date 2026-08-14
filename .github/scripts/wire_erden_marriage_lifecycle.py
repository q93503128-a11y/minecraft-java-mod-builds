from pathlib import Path

root = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world")
saved = root / "ErdenExteriorLifecycleSavedData.java"
manager = root / "ErdenExteriorLifecycleManager.java"

s = saved.read_text(encoding="utf-8")
if "public Person withHousehold(String newHouseholdId)" not in s:
    anchor = '''        public Person withDeath(long day) {
            return new Person(id, name, householdId, nodeId, birthDay, parentA, parentB,
                    generation, founder, foundingWorker, workRole, shiftStart, shiftEnd, restDay,
                    retirementDay, day);
        }
'''
    addition = anchor + '''
        public Person withHousehold(String newHouseholdId) {
            return new Person(id, name, newHouseholdId, nodeId, birthDay, parentA, parentB,
                    generation, founder, foundingWorker, workRole, shiftStart, shiftEnd, restDay,
                    retirementDay, deathDay);
        }
'''
    if anchor not in s:
        raise SystemExit("Person anchor missing")
    s = s.replace(anchor, addition, 1)
if "public boolean movePersonHousehold(String personId, String newHouseholdId)" not in s:
    anchor = "    public void markDeath(String personId, long day) {\n"
    addition = '''    public boolean movePersonHousehold(String personId, String newHouseholdId) {
        for (int i = 0; i < persons.size(); i++) {
            Person person = persons.get(i);
            if (!person.id().equals(personId)) continue;
            if (person.householdId().equals(newHouseholdId)) return false;
            persons.set(i, person.withHousehold(newHouseholdId));
            setDirty();
            return true;
        }
        return false;
    }

''' + anchor
    if anchor not in s:
        raise SystemExit("markDeath anchor missing")
    s = s.replace(anchor, addition, 1)
saved.write_text(s, encoding="utf-8")

m = manager.read_text(encoding="utf-8")
old = '''            int living = 0;
            List<ErdenExteriorLifecycleSavedData.Person> parents = new ArrayList<>();
            for (ErdenExteriorLifecycleSavedData.Person member : members) {
                if (!member.aliveOn(day)) continue;
                living++;
                int age = ageYears(member, day);
                if (age >= MIN_PARENT_AGE && age <= MAX_PARENT_AGE
                        && !member.retiredOn(day)) parents.add(member);
            }
            if (living >= MAX_HOUSEHOLD_SIZE || parents.size() < 2) continue;'''
new = '''            int living = 0;
            List<ErdenExteriorLifecycleSavedData.Person> parents = level == null
                    ? new ArrayList<>()
                    : new ArrayList<>(ErdenExteriorMarriageManager.parentPair(
                    level, household.id(), members, day));
            for (ErdenExteriorLifecycleSavedData.Person member : members) {
                if (!member.aliveOn(day)) continue;
                living++;
                if (level == null) {
                    int age = ageYears(member, day);
                    if (age >= MIN_PARENT_AGE && age <= MAX_PARENT_AGE
                            && !member.retiredOn(day)) parents.add(member);
                }
            }
            if (living >= MAX_HOUSEHOLD_SIZE || parents.size() != 2) continue;'''
if "ErdenExteriorMarriageManager.parentPair(" not in m:
    if old not in m:
        raise SystemExit("birth parent-selection anchor missing")
    m = m.replace(old, new, 1)
manager.write_text(m, encoding="utf-8")

for path, token in [
    (saved, "movePersonHousehold(String personId, String newHouseholdId)"),
    (manager, "ErdenExteriorMarriageManager.parentPair(")]:
    if token not in path.read_text(encoding="utf-8"):
        raise SystemExit("missing invariant " + token)
