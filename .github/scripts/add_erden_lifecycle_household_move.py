from pathlib import Path

p = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorLifecycleSavedData.java")
s = p.read_text(encoding="utf-8")

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
        raise SystemExit("Person death-method anchor missing")
    s = s.replace(anchor, addition, 1)

if "public boolean movePersonHousehold(String personId, String newHouseholdId)" not in s:
    anchor = '''    public void markDeath(String personId, long day) {
'''
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

for token in ["withHousehold(String newHouseholdId)", "movePersonHousehold(String personId, String newHouseholdId)"]:
    if token not in s:
        raise SystemExit("missing " + token)
p.write_text(s, encoding="utf-8")
