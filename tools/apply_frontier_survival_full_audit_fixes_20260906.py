from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one replacement anchor, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def append_once(path: Path, marker: str, extra: str) -> None:
    text = path.read_text(encoding="utf-8")
    if marker in text:
        return
    path.write_text(text.rstrip() + "\n\n" + extra.rstrip() + "\n", encoding="utf-8")


frontier = ROOT / "projects/frontier-settlement"
worker = frontier / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java"
frontier_props = frontier / "gradle.properties"
frontier_check = frontier / "tools/test_current_source.py"

replace_once(
    worker,
'''    private static void runBuildingWorkers(ServerLevel level, SettlementData data, BuildingType type,
                                           String workerName, BuildingWork work) {
        List<BuildingRecord> buildings = buildings(data, type);
        List<FrontierWorkerEntity> workers = workersByName(level, data, type, workerName);
        int count = Math.min(buildings.size(), workers.size());
        for (int i = 0; i < count; i++) {
            BuildingRecord building = buildings.get(i);
            if (!level.hasChunkAt(building.workCenter())) continue;
            FrontierWorkerEntity worker = workers.get(i);
            // Frontier owns the work order, but workers remain ordinary damageable mobs.
            // Clear stale Alpha.84-87 quarantine/active-project flags on every ordinary work tick.
            worker.setNoAi(false);
            worker.setInvulnerable(false);
            // Old saves can contain a worker that was carrying a worksite-export stack. That old
            // state caused a local-barrel -> MAINHAND -> town-storage retry loop. Retire it once and
            // let the ordinary cargo state machine decide where the physical stack belongs.
            if (worker.entityTags().contains(LEGACY_WORKSITE_EXPORT_TAG)) {
                worker.removeTag(LEGACY_WORKSITE_EXPORT_TAG);
                worker.getNavigation().stop();
                MOVEMENT_WATCHES.remove(worker.getUUID());
            }
            work.run(level, data, worker, building);
        }
    }
''',
'''    private record WorkerBuildingAssignment(BuildingRecord building, FrontierWorkerEntity worker) {}

    private static void runBuildingWorkers(ServerLevel level, SettlementData data, BuildingType type,
                                           String workerName, BuildingWork work) {
        List<BuildingRecord> loadedBuildings = new ArrayList<>();
        for (BuildingRecord building : buildings(data, type)) {
            if (level.hasChunkAt(building.workCenter())) loadedBuildings.add(building);
        }
        List<FrontierWorkerEntity> workers = workersByName(level, data, type, workerName);
        for (WorkerBuildingAssignment assignment : matchWorkersToBuildings(loadedBuildings, workers)) {
            BuildingRecord building = assignment.building();
            FrontierWorkerEntity worker = assignment.worker();
            // Same-profession workers are physical civilians, not list-index slots. Match the closest
            // remaining worker to the closest remaining completed workplace so a later build, death,
            // relog or save migration cannot swap jobs merely because UUID lexical order changed.
            worker.setNoAi(false);
            worker.setInvulnerable(false);
            // Old saves can contain a worker that was carrying a worksite-export stack. That old
            // state caused a local-barrel -> MAINHAND -> town-storage retry loop. Retire it once and
            // let the ordinary cargo state machine decide where the physical stack belongs.
            if (worker.entityTags().contains(LEGACY_WORKSITE_EXPORT_TAG)) {
                worker.removeTag(LEGACY_WORKSITE_EXPORT_TAG);
                worker.getNavigation().stop();
                MOVEMENT_WATCHES.remove(worker.getUUID());
            }
            work.run(level, data, worker, building);
        }
    }

    private static List<WorkerBuildingAssignment> matchWorkersToBuildings(List<BuildingRecord> buildings,
                                                                           List<FrontierWorkerEntity> workers) {
        List<BuildingRecord> remainingBuildings = new ArrayList<>(buildings);
        List<FrontierWorkerEntity> remainingWorkers = new ArrayList<>(workers);
        List<WorkerBuildingAssignment> result = new ArrayList<>();
        while (!remainingBuildings.isEmpty() && !remainingWorkers.isEmpty()) {
            BuildingRecord bestBuilding = null;
            FrontierWorkerEntity bestWorker = null;
            double bestDistance = Double.MAX_VALUE;
            long bestBuildingKey = Long.MAX_VALUE;
            String bestWorkerKey = "";
            for (BuildingRecord building : remainingBuildings) {
                BlockPos work = building.workCenter();
                long buildingKey = work.asLong();
                for (FrontierWorkerEntity candidate : remainingWorkers) {
                    double distance = candidate.distanceToSqr(
                            work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D);
                    String workerKey = candidate.getUUID().toString();
                    if (distance < bestDistance
                            || (Double.compare(distance, bestDistance) == 0
                            && (buildingKey < bestBuildingKey
                            || (buildingKey == bestBuildingKey && (bestWorker == null || workerKey.compareTo(bestWorkerKey) < 0))))) {
                        bestBuilding = building;
                        bestWorker = candidate;
                        bestDistance = distance;
                        bestBuildingKey = buildingKey;
                        bestWorkerKey = workerKey;
                    }
                }
            }
            if (bestBuilding == null || bestWorker == null) break;
            result.add(new WorkerBuildingAssignment(bestBuilding, bestWorker));
            remainingBuildings.remove(bestBuilding);
            remainingWorkers.remove(bestWorker);
        }
        return result;
    }
''')

replace_once(
    worker,
'''            if (tryFillJob(server, level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME, lumber.size())) return;
            if (tryFillJob(server, level, data, BuildingType.FARM, FARM_WORKER_NAME, farm.size())) return;
            if (tryFillJob(server, level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME, quarry.size())) return;
            if (tryFillJob(server, level, data, BuildingType.MINE, MINE_WORKER_NAME, mine.size())) return;
''',
'''            if (tryFillJob(server, level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME, lumber)) return;
            if (tryFillJob(server, level, data, BuildingType.FARM, FARM_WORKER_NAME, farm)) return;
            if (tryFillJob(server, level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME, quarry)) return;
            if (tryFillJob(server, level, data, BuildingType.MINE, MINE_WORKER_NAME, mine)) return;
''')

replace_once(
    worker,
'''    private static boolean tryFillJob(MinecraftServer server, ServerLevel level, SettlementData data,
                                      BuildingType type, String workerName, int existingWorkers) {
        List<BuildingRecord> available = buildings(data, type);
        if (existingWorkers >= available.size()) return false;
        BuildingRecord target = available.get(existingWorkers);
        if (!level.hasChunkAt(target.workCenter())) return true;
        if (!arrivalFoodAvailable(level, data)) return true;
        FrontierWorkerEntity arrival = spawnWorker(level, target.workCenter(), workerName);
        commitArrival(server, level, data, arrival);
        return true;
    }
''',
'''    private static boolean tryFillJob(MinecraftServer server, ServerLevel level, SettlementData data,
                                      BuildingType type, String workerName, List<FrontierWorkerEntity> existingWorkers) {
        List<BuildingRecord> missing = new ArrayList<>(buildings(data, type));
        if (missing.isEmpty()) return false;
        for (WorkerBuildingAssignment assignment : matchWorkersToBuildings(missing, existingWorkers)) {
            missing.remove(assignment.building());
        }
        if (missing.isEmpty()) return false;
        // Evidence is complete before this method is called, so the nearest-worker matching above
        // distinguishes a genuinely vacant workplace from a worker that merely belongs to a later build.
        BuildingRecord target = missing.getFirst();
        if (!level.hasChunkAt(target.workCenter())) return true;
        if (!arrivalFoodAvailable(level, data)) return true;
        FrontierWorkerEntity arrival = spawnWorker(level, target.workCenter(), workerName);
        commitArrival(server, level, data, arrival);
        return true;
    }
''')

replace_once(frontier_props, "mod_version=0.1.0-alpha.114", "mod_version=0.1.0-alpha.115")
append_once(
    frontier_props,
    "# Alpha.115 worker/workplace authority",
    "# Alpha.115 worker/workplace authority: same-profession production civilians are matched to completed workplaces by deterministic nearest physical distance instead of unrelated UUID/list indexes; vacancy recruitment uses the same matching so later-building survivors cannot cause duplicate arrivals at the wrong workplace.")
replace_once(frontier_check, 'require("mod_version=0.1.0-alpha.114" in gradle, "current verifier/version drift")',
             'require("mod_version=0.1.0-alpha.115" in gradle, "current verifier/version drift")')
replace_once(
    frontier_check,
'''require("Profession barrels are already part of SettlementStorageService's authoritative physical" in worker, "worksite barrel authority rationale missing")
''',
'''require("Profession barrels are already part of SettlementStorageService's authoritative physical" in worker, "worksite barrel authority rationale missing")
require("matchWorkersToBuildings" in worker and "WorkerBuildingAssignment" in worker,
        "same-profession workers returned to UUID/list-index workplace assignment")
require("BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME, lumber)" in worker
        and "BuildingType.FARM, FARM_WORKER_NAME, farm)" in worker
        and "BuildingType.QUARRY, QUARRY_WORKER_NAME, quarry)" in worker
        and "BuildingType.MINE, MINE_WORKER_NAME, mine)" in worker,
        "vacancy recruitment no longer uses physical worker/workplace matching")
''')
replace_once(frontier_check,
             'print("CURRENT SOURCE CHECK PASS: Frontier Settlement 0.1.0-alpha.114 physical production ecology/balance + prior invariants")',
             'print("CURRENT SOURCE CHECK PASS: Frontier Settlement 0.1.0-alpha.115 worker/workplace authority + physical production ecology/balance + prior invariants")')

survival = ROOT / "projects/survival-ascension"
survival_props = survival / "gradle.properties"
survival_main = survival / "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java"
survival_commands = survival / "src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java"
survival_guide = survival / "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java"
survival_check = survival / "tools/test_current_source.py"

replace_once(survival_props, "mod_version=0.61.17-alpha.1", "mod_version=0.61.18-alpha.1")
replace_once(survival_main, 'VERSION = "0.61.17-alpha.1"', 'VERSION = "0.61.18-alpha.1"')
replace_once(
    survival_commands,
'''                .then(skillSetLevelNode("harvesting", SkillType.HARVESTING))
                .then(skillSetLevelNode("combat", SkillType.COMBAT))
''',
'''                .then(skillSetLevelNode("harvesting", SkillType.HARVESTING))
                .then(skillSetLevelNode("fishing", SkillType.FISHING))
                .then(skillSetLevelNode("combat", SkillType.COMBAT))
''')
replace_once(survival_commands, ' + " | V " + (level < 30 ? "잠김"', ' + " | X " + (level < 30 ? "잠김"')
replace_once(survival_guide, ' / V %s", SkillTuning.mobilitySpeedMultiplier(level)', ' / X %s", SkillTuning.mobilitySpeedMultiplier(level)')

replace_once(survival_check, 'require("mod_version=0.61.17-alpha.1" in props, "Survival Ascension version drift")',
             'require("mod_version=0.61.18-alpha.1" in props, "Survival Ascension version drift")')
replace_once(survival_check, 'require(\'VERSION = "0.61.17-alpha.1"\' in main, "source version drift")',
             'require(\'VERSION = "0.61.18-alpha.1"\' in main, "source version drift")')
replace_once(
    survival_check,
'''client = text(JAVA / "client/SurvivalAscensionClient.java")
require("InputConstants.KEY_X" in client, "dash default key must be X")
require("mobility_action\\\", InputConstants.KEY_V" not in client, "old V dash default returned")
''',
'''client = text(JAVA / "client/SurvivalAscensionClient.java")
require("InputConstants.KEY_X" in client, "dash default key must be X")
require("mobility_action\\\", InputConstants.KEY_V" not in client, "old V dash default returned")
commands_for_controls = text(JAVA / "command/AscensionCommands.java")
guide_for_controls = text(JAVA / "client/GuideScreen.java")
require('" | X "' in commands_for_controls and '" / X %s"' in guide_for_controls,
        "mobility stats/help no longer match the actual X key")
require('" | V "' not in commands_for_controls and '" / V %s"' not in guide_for_controls,
        "stale V mobility label returned")
''')
replace_once(
    survival_check,
'''commands = text(JAVA / "command/AscensionCommands.java")
require("GLOBAL_SOFT_TIME_BUDGET_NANOS = 6_000_000L" in bore''',
'''commands = text(JAVA / "command/AscensionCommands.java")
for skill_literal, skill_enum in (
    ("mining", "MINING"),
    ("woodcutting", "WOODCUTTING"),
    ("harvesting", "HARVESTING"),
    ("fishing", "FISHING"),
    ("combat", "COMBAT"),
    ("construction", "CONSTRUCTION"),
    ("mobility", "MOBILITY"),
):
    require(f'skillSetLevelNode("{skill_literal}", SkillType.{skill_enum})' in commands,
            f"GM skill playtest command missing: {skill_literal}")
require("GLOBAL_SOFT_TIME_BUDGET_NANOS = 6_000_000L" in bore''')
replace_once(
    survival_check,
'''require("borestats" in commands and "BoreMiningService.profileLines" in commands, "bore runtime profile command missing")
''',
'''require("borestats" in commands and "BoreMiningService.profileLines" in commands, "bore runtime profile command missing")
construction = text(JAVA / "construction/ConstructionProgression.java")
harvesting = text(JAVA / "harvesting/HarvestingProgression.java")
woodcutting = text(JAVA / "woodcutting/WoodcuttingProgression.java")
irrigation = text(JAVA / "harvesting/IrrigationReplantService.java")
require("level.getBlockEntity(target) != null" in mining, "bulk mining no longer protects block entities")
require("AREA_BREAK_GUARD" in mining and "player.isShiftKeyDown()" in mining, "mining recursion/precision guard missing")
require("CHAIN_GUARD" in woodcutting and "JOBS.clear()" in woodcutting, "woodcutting queue/recursion cleanup missing")
require("AREA_GUARD" in harvesting and "MAX_PENDING_PER_PLAYER" in harvesting and "JOBS.clear()" in harvesting,
        "harvesting queue bounds/cleanup missing")
require("FieldDepotService.hasMaterial" in irrigation and "FieldDepotService.consumeOne" in irrigation,
        "irrigation replant no longer consumes physical seed material")
require("EventHooks.onBlockPlace" in construction and "FieldDepotService.consumeOne" in construction,
        "construction placement/material transaction guard missing")
''')
replace_once(survival_check,
             'print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.17 adaptive bore budget/profiling + protocol15 + prior runtime invariants")',
             'print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.18 full skill command/control regression + adaptive bore budget/profiling + protocol15 + prior runtime invariants")')

print("Applied Frontier Alpha.115 + Survival Ascension 0.61.18 full-audit fixes")
