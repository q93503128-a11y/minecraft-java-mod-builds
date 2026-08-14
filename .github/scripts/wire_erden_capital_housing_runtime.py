from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms"
LIFE = BASE / "world/ErdenCapitalLifecycleManager.java"
AUTH = BASE / "world/ErdenAuthoritativeEconomyManager.java"
MARKET = BASE / "world/ErdenLivingEconomyManager.java"
MOD = BASE / "LivingKingdoms.java"


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(message)


# Lifecycle exposes the currently occupied civic household slots.
l = LIFE.read_text(encoding="utf-8")
if "public static Set<String> livingHouseholdIds(" not in l:
    anchor = '''    public static int livingHouseholdCount(
'''
    helper = '''    public static Set<String> livingHouseholdIds(
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

'''
    require(anchor in l, "lifecycle livingHouseholdCount anchor missing")
    l = l.replace(anchor, helper + anchor, 1)
LIFE.write_text(l, encoding="utf-8")

# Authoritative market passes lifecycle occupancy, not founder-only death state.
a = AUTH.read_text(encoding="utf-8")
old_market = '''        ErdenLivingEconomyManager.MarketResult market =
                ErdenLivingEconomyManager.runDailyMarket(
                        day, population, sites, wallets, livingEconomy);'''
new_market = '''        Set<String> activeHouseholds =
                ErdenCapitalLifecycleManager.livingHouseholdIds(level, population, day);
        ErdenLivingEconomyManager.MarketResult market =
                ErdenLivingEconomyManager.runDailyMarket(
                        day, population, activeHouseholds, sites, wallets, livingEconomy);'''
if "ErdenCapitalLifecycleManager.livingHouseholdIds(level, population, day)" not in a:
    require(old_market in a, "authoritative market call anchor missing")
    a = a.replace(old_market, new_market, 1)
AUTH.write_text(a, encoding="utf-8")

m = MARKET.read_text(encoding="utf-8")
old_sig = '''    public static MarketResult runDailyMarket(
            long day,
            ErdenPopulationSavedData population,
            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites,'''
new_sig = '''    public static MarketResult runDailyMarket(
            long day,
            ErdenPopulationSavedData population,
            Set<String> activeHouseholds,
            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites,'''
if "Set<String> activeHouseholds" not in m[m.index("public static MarketResult runDailyMarket"):m.index("public static boolean operatesOnDay")]:
    require(old_sig in m, "living market signature anchor missing")
    m = m.replace(old_sig, new_sig, 1)

old_alive = '''        for (ErdenPopulationSavedData.Household household : population.households()) {
            if (aliveResidents(population, household) <= 0) continue;'''
new_alive = '''        for (ErdenPopulationSavedData.Household household : population.households()) {
            if (!activeHouseholds.contains(household.id())) continue;'''
if "if (!activeHouseholds.contains(household.id())) continue;" not in m:
    require(old_alive in m, "living market founder-only household guard missing")
    m = m.replace(old_alive, new_alive, 1)
MARKET.write_text(m, encoding="utf-8")

# Housing runs after wages/market, so daily rent sees the day's household income.
mod = MOD.read_text(encoding="utf-8")
if "import kr.moonseungjun.livingkingdoms.world.ErdenCapitalHousingManager;" not in mod:
    anchor = "import kr.moonseungjun.livingkingdoms.world.ErdenCapitalLifecycleManager;\n"
    require(anchor in mod, "LivingKingdoms lifecycle import anchor missing")
    mod = mod.replace(anchor, anchor + "import kr.moonseungjun.livingkingdoms.world.ErdenCapitalHousingManager;\n", 1)
if "ErdenCapitalHousingManager.onServerTick(event);" not in mod:
    anchor = "        ErdenAuthoritativeEconomyManager.onServerTick(event);\n"
    require(anchor in mod, "LivingKingdoms authoritative economy tick anchor missing")
    mod = mod.replace(anchor, anchor + "        ErdenCapitalHousingManager.onServerTick(event);\n", 1)
MOD.write_text(mod, encoding="utf-8")

for path, tokens in {
    LIFE: ["public static Set<String> livingHouseholdIds("],
    AUTH: ["ErdenCapitalLifecycleManager.livingHouseholdIds(level, population, day)"],
    MARKET: ["Set<String> activeHouseholds", "!activeHouseholds.contains(household.id())"],
    MOD: ["ErdenCapitalHousingManager.onServerTick(event)"],
}.items():
    text = path.read_text(encoding="utf-8")
    for token in tokens:
        require(token in text, f"missing housing runtime token {token} in {path.name}")

print("Wired capital housing into lifecycle-aware consumers and post-wage rent processing.")
