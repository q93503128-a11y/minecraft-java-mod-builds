from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
POP = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenPopulationManager.java"
ECO = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenAuthoritativeEconomyManager.java"
MOD = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/LivingKingdoms.java"


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(message)


# Population ledger: lifecycle must run before daily aggregate production/consumption.
p = POP.read_text(encoding="utf-8")
old = '''        requestCiChunks(level, population);
        processDailyEconomy(level, population);'''
new = '''        requestCiChunks(level, population);
        ErdenCapitalLifecycleManager.prepare(level, population);
        processDailyEconomy(level, population);'''
if "ErdenCapitalLifecycleManager.prepare(level, population);" not in p:
    require(old in p, "population lifecycle ordering anchor missing")
    p = p.replace(old, new, 1)

old_daily = '''            Map<String, Long> production = new LinkedHashMap<>();
            Map<String, Long> consumption = new LinkedHashMap<>();
            int livingHouseholds = 0;
            int livingResidents = 0;
            for (ErdenPopulationSavedData.Household household : population.households()) {
                int aliveHere = 0;
                for (ErdenPopulationSavedData.Resident resident : household.residents()) {
                    if (population.isDead(resident.id())) continue;
                    aliveHere++;
                    if (resident.worker()) addProduction(production, resident.workRole());
                }
                if (aliveHere <= 0) continue;
                livingHouseholds++;
                livingResidents += aliveHere;
            }
            add(consumption, "food", livingResidents);'''
new_daily = '''            Map<String, Long> production = new LinkedHashMap<>();
            Map<String, Long> consumption = new LinkedHashMap<>();
            List<ErdenCapitalLifecycleManager.WorkerSnapshot> activeWorkers =
                    ErdenCapitalLifecycleManager.activeWorkers(level, population, day);
            int livingHouseholds = ErdenCapitalLifecycleManager.livingHouseholdCount(level, population, day);
            int livingResidents = ErdenCapitalLifecycleManager.livingCount(level, population, day);
            for (ErdenCapitalLifecycleManager.WorkerSnapshot worker : activeWorkers) {
                addProduction(production, worker.workRole());
            }
            add(consumption, "food", livingResidents);'''
if "List<ErdenCapitalLifecycleManager.WorkerSnapshot> activeWorkers" not in p:
    require(old_daily in p, "population daily economy anchor missing")
    p = p.replace(old_daily, new_daily, 1)

old_log = '''                        day, population.aliveResidentCount(), population.aliveWorkerCount(),
                        population.stocks(), population.totalShortage());'''
new_log = '''                        day, livingResidents, activeWorkers.size(),
                        population.stocks(), population.totalShortage());'''
if "day, livingResidents, activeWorkers.size()," not in p:
    require(old_log in p, "population daily log anchor missing")
    p = p.replace(old_log, new_log, 1)
POP.write_text(p, encoding="utf-8")

# Authoritative economy: production and wages use lifecycle-active workers, including descendants.
e = ECO.read_text(encoding="utf-8")
old_call = '''                result = processDay(day, population, economy.sites(), economy.wallets(), livingEconomy);'''
new_call = '''                result = processDay(level, day, population, economy.sites(), economy.wallets(), livingEconomy);'''
if "result = processDay(level, day, population" not in e:
    require(old_call in e, "economy processDay call anchor missing")
    e = e.replace(old_call, new_call, 1)

old_sig = '''    private static DayResult processDay(
            long day,
            ErdenPopulationSavedData population,'''
new_sig = '''    private static DayResult processDay(
            ServerLevel level,
            long day,
            ErdenPopulationSavedData population,'''
if "private static DayResult processDay(\n            ServerLevel level," not in e:
    require(old_sig in e, "economy processDay signature anchor missing")
    e = e.replace(old_sig, new_sig, 1)

old_workers = '''        Map<Long, WorkerRef> workers = livingWorkers(population);'''
new_workers = '''        Map<Long, WorkerRef> workers = livingWorkers(level, population, day);'''
if "livingWorkers(level, population, day)" not in e:
    require(old_workers in e, "economy worker map anchor missing")
    e = e.replace(old_workers, new_workers, 1)

old_pay_call = '''        payWages(population, sites, wallets, counters);'''
new_pay_call = '''        payWages(workers, sites, wallets, counters);'''
if "payWages(workers, sites, wallets, counters);" not in e:
    require(old_pay_call in e, "economy wage call anchor missing")
    e = e.replace(old_pay_call, new_pay_call, 1)

old_pay = '''    private static void payWages(
            ErdenPopulationSavedData population,
            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites,
            Map<String, ErdenPhysicalEconomySavedData.WalletState> wallets,
            DayCounters counters) {
        Map<Long, String> siteIds = new HashMap<>();
        for (ErdenPhysicalEconomySavedData.SiteState site : sites.values()) {
            siteIds.put(positionKey(site.x(), site.z()), site.id());
        }
        for (ErdenPopulationSavedData.Household household : population.households()) {
            ErdenPhysicalEconomySavedData.WalletState wallet = wallets.get(household.id());
            if (wallet == null) continue;
            for (ErdenPopulationSavedData.Resident resident : household.residents()) {
                if (!resident.worker() || population.isDead(resident.id())) continue;
                String siteId = siteIds.get(positionKey(resident.workX(), resident.workZ()));
                if (siteId == null) continue;
                ErdenPhysicalEconomySavedData.SiteState site = sites.get(siteId);
                long available = site.metric("coins");
                if (available < DAILY_WAGE) {
                    site = site.addMetric("treasury_subsidy", DAILY_WAGE - available)
                            .withMetric("coins", DAILY_WAGE);
                }
                site = site.addMetric("coins", -DAILY_WAGE)
                        .addMetric("wages_paid", DAILY_WAGE);
                wallet = wallet.earn(DAILY_WAGE);
                counters.wages += DAILY_WAGE;
                sites.put(site.id(), site);
            }
            wallets.put(household.id(), wallet);
        }
    }'''
new_pay = '''    private static void payWages(
            Map<Long, WorkerRef> workers,
            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites,
            Map<String, ErdenPhysicalEconomySavedData.WalletState> wallets,
            DayCounters counters) {
        Map<Long, String> siteIds = new HashMap<>();
        for (ErdenPhysicalEconomySavedData.SiteState site : sites.values()) {
            siteIds.put(positionKey(site.x(), site.z()), site.id());
        }
        for (Map.Entry<Long, WorkerRef> entry : workers.entrySet()) {
            WorkerRef worker = entry.getValue();
            ErdenPhysicalEconomySavedData.WalletState wallet = wallets.get(worker.householdId());
            String siteId = siteIds.get(entry.getKey());
            if (wallet == null || siteId == null) continue;
            ErdenPhysicalEconomySavedData.SiteState site = sites.get(siteId);
            long available = site.metric("coins");
            if (available < DAILY_WAGE) {
                site = site.addMetric("treasury_subsidy", DAILY_WAGE - available)
                        .withMetric("coins", DAILY_WAGE);
            }
            site = site.addMetric("coins", -DAILY_WAGE)
                    .addMetric("wages_paid", DAILY_WAGE);
            wallet = wallet.earn(DAILY_WAGE);
            counters.wages += DAILY_WAGE;
            sites.put(site.id(), site);
            wallets.put(worker.householdId(), wallet);
        }
    }'''
if "for (Map.Entry<Long, WorkerRef> entry : workers.entrySet())" not in e:
    require(old_pay in e, "economy payWages block anchor missing")
    e = e.replace(old_pay, new_pay, 1)

old_living = '''    private static Map<Long, WorkerRef> livingWorkers(ErdenPopulationSavedData population) {
        Map<Long, WorkerRef> result = new HashMap<>();
        for (ErdenPopulationSavedData.Household household : population.households()) {
            for (ErdenPopulationSavedData.Resident resident : household.residents()) {
                if (!resident.worker() || population.isDead(resident.id())) continue;
                result.put(positionKey(resident.workX(), resident.workZ()),
                        new WorkerRef(household.id(), resident.id()));
            }
        }
        return result;
    }'''
new_living = '''    private static Map<Long, WorkerRef> livingWorkers(
            ServerLevel level,
            ErdenPopulationSavedData population,
            long day) {
        Map<Long, WorkerRef> result = new HashMap<>();
        for (ErdenCapitalLifecycleManager.WorkerSnapshot worker
                : ErdenCapitalLifecycleManager.activeWorkers(level, population, day)) {
            result.put(positionKey(worker.workX(), worker.workZ()),
                    new WorkerRef(worker.householdId(), worker.personId()));
        }
        return result;
    }'''
if "ErdenCapitalLifecycleManager.activeWorkers(level, population, day)" not in e:
    require(old_living in e, "economy livingWorkers block anchor missing")
    e = e.replace(old_living, new_living, 1)
ECO.write_text(e, encoding="utf-8")

# Main event wiring: descendant materialisation/routines, death and interaction.
m = MOD.read_text(encoding="utf-8")
if "import kr.moonseungjun.livingkingdoms.world.ErdenCapitalLifecycleManager;" not in m:
    anchor = "import kr.moonseungjun.livingkingdoms.world.ErdenCapitalStreamingBuilder;\n"
    require(anchor in m, "LivingKingdoms import anchor missing")
    m = m.replace(anchor, anchor + "import kr.moonseungjun.livingkingdoms.world.ErdenCapitalLifecycleManager;\n", 1)
if "ErdenCapitalLifecycleManager.onServerTick(event);" not in m:
    anchor = "        ErdenPopulationManager.onServerTick(event);\n"
    require(anchor in m, "LivingKingdoms population tick anchor missing")
    m = m.replace(anchor, anchor + "        ErdenCapitalLifecycleManager.onServerTick(event);\n", 1)
if "ErdenCapitalLifecycleManager.markDeadIfLifecycleResident(level, villager);" not in m:
    anchor = "            ErdenPopulationManager.markDeadIfResident(level, villager);\n"
    require(anchor in m, "LivingKingdoms death anchor missing")
    m = m.replace(anchor, anchor + "            ErdenCapitalLifecycleManager.markDeadIfLifecycleResident(level, villager);\n", 1)
if "ErdenCapitalLifecycleManager.handleInteraction(event);" not in m:
    anchor = "        ErdenPopulationManager.handleInteraction(event);\n"
    require(anchor in m, "LivingKingdoms interaction anchor missing")
    m = m.replace(anchor, anchor + "        ErdenCapitalLifecycleManager.handleInteraction(event);\n", 1)
MOD.write_text(m, encoding="utf-8")

for path, tokens in {
    POP: ["ErdenCapitalLifecycleManager.prepare(level, population)", "activeWorkers.size()"],
    ECO: ["livingWorkers(level, population, day)", "payWages(workers, sites, wallets, counters)"],
    MOD: ["ErdenCapitalLifecycleManager.onServerTick(event)", "markDeadIfLifecycleResident", "ErdenCapitalLifecycleManager.handleInteraction(event)"],
}.items():
    text = path.read_text(encoding="utf-8")
    for token in tokens:
        require(token in text, f"missing lifecycle integration token {token} in {path.name}")

print("Wired capital lifecycle into population, physical economy, wages, death hooks and resident interaction.")
