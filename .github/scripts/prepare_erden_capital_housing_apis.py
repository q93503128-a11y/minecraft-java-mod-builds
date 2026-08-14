from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world"
LIFE = BASE / "ErdenCapitalLifecycleSavedData.java"
MARRIAGE = BASE / "ErdenCapitalMarriageSavedData.java"
ECONOMY = BASE / "ErdenPhysicalEconomySavedData.java"


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(message)


# Lifecycle: allow a bounded adult migrant to enter the overlay without changing the founding roster.
s = LIFE.read_text(encoding="utf-8")
if "public boolean addPerson(Person person)" not in s:
    anchor = '''    public boolean movePersonHousehold(String personId, String targetHousehold) {
'''
    helper = '''    public boolean addPerson(Person person) {
        if (person == null || person.id().isBlank() || person.name().isBlank()
                || person.householdId().isBlank() || person(person.id()) != null) return false;
        persons.add(person);
        setDirty();
        return true;
    }

'''
    require(anchor in s, "lifecycle movePersonHousehold anchor missing")
    s = s.replace(anchor, helper + anchor, 1)
LIFE.write_text(s, encoding="utf-8")

# Marriage: a leased independent household can retarget an existing union; migrants can form a union.
m = MARRIAGE.read_text(encoding="utf-8")
old_end = '''        public Union withEnd(long day) {
            if (endDay >= 0L && endDay <= day) return this;
            return new Union(id, personA, personB, householdId, startDay, day, remarriage);
        }
'''
new_end = old_end + '''
        public Union withHousehold(String household) {
            if (household == null || household.isBlank() || household.equals(householdId)) return this;
            return new Union(id, personA, personB, household, startDay, endDay, remarriage);
        }
'''
if "public Union withHousehold(String household)" not in m:
    require(old_end in m, "marriage union withEnd anchor missing")
    m = m.replace(old_end, new_end, 1)

if "public boolean moveUnionHousehold(" not in m:
    anchor = '''    public void replaceYear(
'''
    helper = '''    public boolean moveUnionHousehold(String unionId, String householdId) {
        if (unionId == null || unionId.isBlank() || householdId == null || householdId.isBlank()) return false;
        for (int index = 0; index < unions.size(); index++) {
            Union union = unions.get(index);
            if (!union.id().equals(unionId)) continue;
            Union updated = union.withHousehold(householdId);
            if (!updated.equals(union)) {
                unions.set(index, updated);
                setDirty();
            }
            return true;
        }
        return false;
    }

    public Union createUnion(
            String personA,
            String personB,
            String householdId,
            long startDay,
            boolean remarriage) {
        if (personA == null || personB == null || personA.isBlank() || personB.isBlank()
                || householdId == null || householdId.isBlank() || personA.equals(personB)) return null;
        for (Union union : unions) {
            if (union.activeOn(startDay) && (union.involves(personA) || union.involves(personB))) return null;
        }
        Union created = new Union(
                "erden_capital_union_%04d".formatted(nextUnionSequence++),
                personA, personB, householdId, startDay, -1L, remarriage);
        unions.add(created);
        setDirty();
        return created;
    }

'''
    require(anchor in m, "marriage replaceYear anchor missing")
    m = m.replace(anchor, helper + anchor, 1)
MARRIAGE.write_text(m, encoding="utf-8")

# Economy: keep exactly 77 civic wallet slots while allowing a vacant slot to be re-let.
e = ECONOMY.read_text(encoding="utf-8")
if "public WalletState wallet(String householdId)" not in e:
    anchor = '''    public long lastProcessedDay() {
'''
    helper = '''    public WalletState wallet(String householdId) {
        if (householdId == null || householdId.isBlank()) return null;
        for (WalletState wallet : wallets) {
            if (wallet.householdId().equals(householdId)) return wallet;
        }
        return null;
    }

    public boolean replaceWallet(WalletState replacement) {
        if (replacement == null || replacement.householdId().isBlank()) return false;
        for (int index = 0; index < wallets.size(); index++) {
            WalletState current = wallets.get(index);
            if (!current.householdId().equals(replacement.householdId())) continue;
            if (!current.equals(replacement)) {
                wallets.set(index, replacement);
                setDirty();
            }
            return true;
        }
        return false;
    }

'''
    require(anchor in e, "physical economy lastProcessedDay anchor missing")
    e = e.replace(anchor, helper + anchor, 1)
ECONOMY.write_text(e, encoding="utf-8")

for path, tokens in {
    LIFE: ["public boolean addPerson(Person person)"],
    MARRIAGE: ["withHousehold(String household)", "moveUnionHousehold", "createUnion("],
    ECONOMY: ["WalletState wallet(String householdId)", "replaceWallet(WalletState replacement)"],
}.items():
    text = path.read_text(encoding="utf-8")
    for token in tokens:
        require(token in text, f"missing capital housing API {token} in {path.name}")

print("Prepared save-compatible housing extension APIs without changing the 77-home/77-wallet invariants.")
