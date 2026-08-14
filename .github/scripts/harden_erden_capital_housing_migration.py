from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world"
LIFE = BASE / "ErdenCapitalLifecycleSavedData.java"
MARRIAGE = BASE / "ErdenCapitalMarriageSavedData.java"
HOUSING = BASE / "ErdenCapitalHousingManager.java"


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(message)


# Lifecycle: atomic multi-person insertion and rollback for non-founder migration fixtures/runtime.
s = LIFE.read_text(encoding="utf-8")
if "public boolean addPeople(List<Person> additions)" not in s:
    anchor = '''    public boolean addPerson(Person person) {
        if (person == null || person.id().isBlank() || person.name().isBlank()
                || person.householdId().isBlank() || person(person.id()) != null) return false;
        persons.add(person);
        setDirty();
        return true;
    }
'''
    helper = anchor + '''
    public boolean addPeople(List<Person> additions) {
        if (additions == null || additions.isEmpty()) return false;
        java.util.Set<String> ids = new java.util.HashSet<>();
        java.util.Set<String> names = new java.util.HashSet<>();
        for (Person person : additions) {
            if (person == null || person.id().isBlank() || person.name().isBlank()
                    || person.householdId().isBlank()
                    || person(person.id()) != null || personByName(person.name()) != null
                    || !ids.add(person.id()) || !names.add(person.name())) return false;
        }
        persons.addAll(additions);
        setDirty();
        return true;
    }

    public boolean removeNonFounderPeople(List<String> personIds) {
        if (personIds == null || personIds.isEmpty()) return false;
        java.util.Set<String> ids = new java.util.HashSet<>(personIds);
        for (Person person : persons) {
            if (ids.contains(person.id()) && person.founder()) return false;
        }
        boolean changed = persons.removeIf(person -> ids.contains(person.id()) && !person.founder());
        if (changed) setDirty();
        return changed;
    }
'''
    require(anchor in s, "lifecycle addPerson anchor missing")
    s = s.replace(anchor, helper, 1)
LIFE.write_text(s, encoding="utf-8")

# Marriage rollback for a newly created migrant union if downstream wallet re-let cannot commit.
m = MARRIAGE.read_text(encoding="utf-8")
if "public boolean removeUnion(String unionId)" not in m:
    anchor = '''    public Union createUnion(
'''
    helper = '''    public boolean removeUnion(String unionId) {
        if (unionId == null || unionId.isBlank()) return false;
        boolean changed = unions.removeIf(union -> union.id().equals(unionId));
        if (changed) setDirty();
        return changed;
    }

'''
    require(anchor in m, "marriage createUnion anchor missing")
    m = m.replace(anchor, helper + anchor, 1)
MARRIAGE.write_text(m, encoding="utf-8")

h = HOUSING.read_text(encoding="utf-8")
old_add = '''            if (!lifecycle.addPerson(first) || !lifecycle.addPerson(second)) continue;
            ErdenCapitalMarriageSavedData.Union union = marriages.createUnion(
                    first.id(), second.id(), vacancy.slotId(), day, false);
            if (union == null) continue;
            WalletRelet relet = reletWallet(economy, vacancy.slotId());
            if (!relet.success()) continue;'''
new_add = '''            List<String> migrantIds = List.of(first.id(), second.id());
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
            }'''
if "List<String> migrantIds = List.of(first.id(), second.id());" not in h:
    require(old_add in h, "housing migrant insertion anchor missing")
    h = h.replace(old_add, new_add, 1)

old_name = '''    private static String migrantName(int sequence) {
        int value = Math.max(0, sequence - 1);
        return "이주민 " + MIGRANT_HEADS.get(value % MIGRANT_HEADS.size())
                + MIGRANT_TAILS.get((value / MIGRANT_HEADS.size()) % MIGRANT_TAILS.size());
    }'''
new_name = '''    private static String migrantName(int sequence) {
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
    }'''
if "int base = MIGRANT_HEADS.size() * MIGRANT_TAILS.size();" not in h:
    require(old_name in h, "housing migrantName anchor missing")
    h = h.replace(old_name, new_name, 1)
HOUSING.write_text(h, encoding="utf-8")

for path, tokens in {
    LIFE: ["addPeople(List<Person> additions)", "removeNonFounderPeople"],
    MARRIAGE: ["removeUnion(String unionId)"],
    HOUSING: ["migrantIds", "removeNonFounderPeople", "MIGRANT_HEADS.size() * MIGRANT_TAILS.size()", "name.append('·')"],
}.items():
    text = path.read_text(encoding="utf-8")
    for token in tokens:
        require(token in text, f"missing housing migration hardening token {token} in {path.name}")

print("Hardened migrant insertion as an all-or-rollback transaction and made long-run names unboundedly unique.")
