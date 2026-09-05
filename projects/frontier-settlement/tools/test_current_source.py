from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
SETTLEMENT = JAVA / "settlement"


def text(path):
    return path.read_text(encoding="utf-8")


def require(condition, message):
    if not condition:
        raise AssertionError(message)


gradle = text(ROOT / "gradle.properties")
require("mod_version=0.1.0-alpha.108" in gradle, "current verifier/version drift")

inventory = text(SETTLEMENT / "SettlementInventory.java")
storage = text(SETTLEMENT / "SettlementStorageService.java")
require("if (stack.is(Items.DIAMOND)) return 6;" in inventory, "diamond metal value missing or drifted")
require(inventory.count("stack.is(Items.DIAMOND)") >= 2, "diamond is not included in settlement metal classification")
require("return SettlementInventory.metalValue(stack) > 0;" in storage, "storage bypasses canonical metal authority")
require("consumeMetalAndFood" in inventory, "local metal value consumption missing")
require("for (int unit = 1; unit <= 24" in storage, "low-value-first shared-resource consumption priority regressed")

construction = text(SETTLEMENT / "SettlementConstructionService.java")
for retired in (
    "DIRECT_HIGH_WORK_RANGE_SQR",
    "HIGH_WORK_RANGE_SQR",
    "SCAFFOLD_POSITION_REACHED_SQR",
    "hasFreshScaffoldCoverage(",
    "ensureConstructionScaffolds(",
    "canClaimFreshTower(",
    "placeClaimedTower(",
    "repairClaimedTower(",
    "hasWalkableScaffoldEntry(",
):
    require(retired not in construction, f"retired scaffold authority returned: {retired}")
require("retireLegacyConstructionScaffolds(" in construction, "legacy scaffold teardown compatibility was removed")
require("ensureProjectBuilder(" in construction, "shared project-builder authority missing")
require("MAX_BUILDER_CREW = 3" in construction, "bounded construction crew cap missing")
require("ensureProjectBuilders" in construction and "desiredBuilderCount" in construction, "multi-builder crew authority missing")
require("i == 0" in construction and "tickConstructionBuilder" in construction, "builder crew is not serialized through one scheduler")
require("terrainSurfaceHeight(level, worldX, worldZ)" in construction, "placement height still treats natural trunks as terrain peaks")
require("isNaturalTreeLog" in construction and "BlockTags.LOGS" in construction and "BlockTags.LEAVES" in construction, "tree-aware natural vegetation evidence missing")
require("isClearableSiteVegetation(level, pos, state)" in construction, "grading does not clear verified natural tree vegetation")
require("isClearableSiteVegetation(level, supply, current)" in construction, "site supply position still rejects natural vegetation")
require("TREE_CANOPY_SEARCH_HEIGHT = 10" in construction and "TREE_CANOPY_SEARCH_RADIUS = 2" in construction, "bounded tree evidence envelope drifted")

road = text(SETTLEMENT / "SettlementRoadService.java")
outpost = text(SETTLEMENT / "SettlementOutpostService.java")
civil = text(SETTLEMENT / "SettlementCivilWorkService.java")
require("ensureProjectBuilder" in road and "clearRoadConstruction" in road, "road start is not transactional")
require("ensureProjectBuilder" in outpost and "clearOutpostConstruction" in outpost, "outpost start is not transactional")
require("ensureProjectBuilders" in civil and "data.clear();" in civil, "civil start/crew acquisition is not transactional")
require("WORK_INTERVAL_TICKS = 5" in civil, "civil scheduler cadence drifted from five-tick service cadence")
require("Heightmap.Types.WORLD_SURFACE" in civil, "full flatten does not clear leaves/ordinary surface blocks")
require("safeDemolitionTarget" in civil and "isReusableCut" in civil, "bulldoze demolition/reusable-earth split missing")
require("MAX_CUT_DEPTH = 32" in civil and "MAX_FILL_DEPTH = 16" in civil, "bounded full-flatten vertical envelope missing")
require("builderInsideCivilEnvelope" in civil, "civil work still depends on exact per-cell pathing")
require("SettlementCivilRetainingService.checkPlan" not in civil, "retired automatic retaining gate still blocks full flatten planning")

worker = text(SETTLEMENT / "SettlementWorkerService.java")
guidance = text(SETTLEMENT / "SettlementGuidanceService.java")
require("arrivalFoodCost()" in worker, "canonical worker arrival-food accessor missing")
require("SettlementWorkerService.arrivalFoodCost()" in guidance, "guidance duplicates arrival-food balance")
require("data.resources().food() < 8L" not in guidance, "stale arrival-food value returned")
require("SettlementCivilWorkData.get(server).project().active()" in guidance, "active civil work absent from next-goal authority")
require("LUMBER_REMOTE_WORK_REACH_SQR = 36.0D" in worker, "bounded six-block lumber remote work missing")
require("QUARRY_REMOTE_WORK_REACH_SQR = 25.0D" in worker, "bounded five-block quarry remote work missing")
require(worker.count("withinResourceWorkReach(worker, target") >= 2, "resource workers still require point-blank target contact")
require("canWorkOrApproach(level, worker, pos, LUMBER_REMOTE_WORK_REACH_SQR)" in worker, "near lumber target still requires a walkable final cell")
require("isBlockedOutsideWorkReach" in worker, "blocked-target retry still suppresses already-reachable remote work")
require("DUPLICATE_MAINTENANCE_INTERVAL_TICKS = 200" in worker, "maintenance duplicate scans regressed to hot-path cadence")
require("WORKSITE_STORAGE_INTERACTION_REACH_SQR = 36.0D" in worker, "close worksite deposit reach missing")
require("deliverIfCargoFull" in worker, "full-stack immediate deposit handoff missing")
require("tryExportWorksiteBuffer(" not in worker, "retired worksite re-extraction loop returned")
require("LEGACY_WORKSITE_EXPORT_TAG" in worker and "worker.removeTag(LEGACY_WORKSITE_EXPORT_TAG)" in worker, "legacy export-tag migration missing")
require("Profession barrels are already part of SettlementStorageService's authoritative physical" in worker, "worksite barrel authority rationale missing")
production_efficiency = text(SETTLEMENT / "SettlementProductionEfficiencyService.java")
require("SettlementTier.current(data)" in production_efficiency, "production upgrades are not derived from canonical settlement tier")
require("case CAMP, HAMLET -> 1" in production_efficiency and "case DOMAIN, FRONTIER_CAPITAL -> 4" in production_efficiency, "production efficiency grade ladder drifted")
require("farmGrowthModulo" in production_efficiency and "mineWorkPeriod" in production_efficiency, "production efficiency parameters incomplete")
require("SettlementProductionEfficiencyService.farmWorkPeriod" in worker, "farm still uses fixed work cadence")
require("state.setValue(BlockStateProperties.AGE_7, Math.min(7, age + 1))" in worker, "staffed farm does not actively tend crop growth")
for stale in ("FARM_WORK_PERIOD_TICKS", "LUMBER_WORK_PERIOD_TICKS", "QUARRY_WORK_PERIOD_TICKS", "MINING_WORK_PERIOD_TICKS"):
    require(stale not in worker, f"stale fixed production pacing authority returned: {stale}")

service = text(SETTLEMENT / "SettlementService.java")
require("SettlementGuidanceService.nextGoal(player.level().getServer(), data)" in service, "guidance is missing server authority")
require("8-tick grading gate" not in service, "obsolete construction cadence prose remains")
require("tick % 20 == 0) SettlementConstructionService.settleIdleBuilders" in service, "idle builder path maintenance cadence regressed")
require("List<FrontierWorkerEntity> existing = new ArrayList<>(findBuilders(level, data));" in construction, "active builder discovery path missing")

palette = text(JAVA / "client/BuildingPaletteScreen.java")
require("civilUnlocked" not in palette, "client still owns partial civil unlock logic")
require("기존 시설 자동 개량" in palette, "production vertical progression is hidden from the build palette")

commands = text(JAVA / "command/SettlementCommands.java")
require("SettlementExplorationBenefitService.barracksRecruitFoodCost(server)" in commands, "status shows stale barracks food cost")
require("SettlementExplorationBenefitService.forgePower(data)" in commands, "status shows stale forge power")
require("SettlementExplorationBenefitService.reforgePower(data)" in commands, "status shows stale reforge power")

cart_layout = text(SETTLEMENT / "CartStationLayout.java")
cart_service = text(SETTLEMENT / "SettlementCartStationService.java")
require("freightSlotCount()" in cart_layout, "cart freight slot count is not authoritative")
require("MAX_ROAD_DISTANCE = 12" in cart_service and '" + MAX_ROAD_DISTANCE + "' in cart_service, "cart road range display duplicates its balance")
require("최대 17×17" not in text(SETTLEMENT / "SettlementCivilWorkService.java"), "civil size display duplicates current constants")
require("플레이어 44블록" not in text(SETTLEMENT / "SettlementCivilWorkService.java"), "civil player range display duplicates current constant")
require("마을 중심 112블록" not in text(SETTLEMENT / "SettlementCivilWorkService.java"), "civil settlement range display duplicates current constant")
require("절토·성토 높이 차는 최대 7블록" not in text(SETTLEMENT / "SettlementCivilWorkService.java"), "civil height display duplicates current constants")
require("CartStationLayout.freightSlotCount()" in commands, "status duplicates cart-station freight count")
require("SettlementCivilWorkService.MAX_WIDTH" in commands and "SettlementCivilWorkService.MAX_CUT_DEPTH" in commands, "status duplicates civil-work limits")

require(not (SETTLEMENT / "SettlementResidentRoutineService.java").exists(), "retired night navigation authority still exists")
require("__pycache__/" in text(ROOT / ".gitignore"), "Python cache ignore missing")
require(not any((ROOT / "tools").rglob("*.pyc")), "generated pyc committed in tools")

retired_mutators = (
    REPO / ".github/workflows/apply-frontier-alpha89.yml",
    REPO / ".github/workflows/apply-frontier-alpha90.yml",
    REPO / ".github/workflows/apply-frontier-alpha90-retry.yml",
    REPO / ".github/workflows/apply-frontier-alpha91.yml",
    REPO / ".github/workflows/apply-starter-shared-supply-depot.yml",
    ROOT / "tools/apply_alpha89.py",
    ROOT / "tools/apply_alpha91_patch.py",
    ROOT / "tools/patch_production_shared_depot_routing.py",
    ROOT / "tools/patch_worksite_output_provenance.py",
    REPO / "tools/apply_starter_shared_supply_depot.py",
    REPO / "tools/fix_starter_depot_migration_gate.py",
)
for retired in retired_mutators:
    require(not retired.exists(), f"obsolete Frontier mutator returned: {retired.relative_to(REPO)}")

integrity = text(SETTLEMENT / "SettlementBuildingIntegrityService.java")
require("RUIN_INTACT_PERCENT = 45" in integrity and "removeCompletedBuilding" in integrity and "clearKnownHouseRemnants" in integrity, "Alpha98 house integrity authority regressed")
for production_type in ("BuildingType.LUMBER_CAMP", "BuildingType.FARM", "BuildingType.QUARRY", "BuildingType.MINE"):
    require(production_type in integrity, f"ruined production building is not integrity-tracked: {production_type}")
require("if (type == BuildingType.HOUSE) clearKnownHouseRemnants" in integrity, "production retirement may clear player/container remnants")
require("REPAIR_INTERVAL_TICKS = 20" in integrity and "MAX_REPAIR_BLOCKS_PER_PASS = 12" in integrity, "bounded immediate house repair cadence missing")
require("repairDamagedHouses" in integrity and "repairPass" in integrity and "integrityPass" in integrity, "house repair is not ordered before ruin retirement")
require("SettlementStorageService.consume(level, data, woodCost, stoneCost, 0L)" in integrity, "house repair bypasses physical wood/stone authority")
require("SettlementStorageService.consumeMetal(level, data, metalCost)" in integrity, "house lantern repair bypasses canonical metal authority")
require("canRepairVacancy" in integrity and "level.getBlockEntity(pos) != null" in integrity, "house repair may overwrite protected/player container cells")

military = text(SETTLEMENT / "SettlementMilitaryOutpostService.java")
require(military.count("SettlementInventory.countMetal(container)") >= 3, "remote military metal still uses raw item counts")
require("SettlementInventory.consumeMetalAndFood(container, metal, food)" in military, "remote recruitment bypasses canonical metal values")

logistics = text(SETTLEMENT / "SettlementOutpostLogisticsService.java")
require("SettlementInventory.metalValue(stack) == unitValue" in logistics, "remote metal hauling bypasses canonical values")
require("instanceof BlockItem blockItem" in logistics and "Tags.Blocks.ORES" in logistics, "companion ore cargo can strand at outposts")

tier = text(SETTLEMENT / "SettlementTier.java")
require("hasMatureFoodBase" in tier and "BuildingType.WAREHOUSE" in tier, "Domain still forces duplicate farm footprint")

print("CURRENT SOURCE CHECK PASS: alpha108 tree-aware placement + alpha107 worker/repair + prior authority invariants")
