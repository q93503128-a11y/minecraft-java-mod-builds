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
require("mod_version=0.1.0-alpha.113" in gradle, "current verifier/version drift")

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
require("MAX_BUILDER_CREW = 12" in construction, "expanded bounded construction crew cap missing")
require("BASE_BUILDER_CREW = 2" in construction and "BUILDERS_PER_CONSTRUCTION_OFFICE = 2" in construction,
        "builder workforce no longer scales from base crew through construction offices")
require("data.outposts().size()" in construction and "OUTPOST_BUILDER_BONUS_CAP = 6" in construction,
        "completed outposts no longer expand builder workforce")
require("buildingProjectBuilders" in construction and "infrastructureProjectBuilder" in construction,
        "dedicated building/road/outpost builder routing missing")
require("ensureProjectBuilders" in construction and "desiredBuilderCount" in construction, "multi-builder crew authority missing")
require("i == 0" in construction and "tickConstructionBuilder" in construction, "builder crew is not serialized through one scheduler")
require("terrainSurfaceHeight(level, worldX, worldZ)" in construction, "placement height still treats natural trunks as terrain peaks")
require("isNaturalTreeLog" in construction and "BlockTags.LOGS" in construction and "BlockTags.LEAVES" in construction, "tree-aware natural vegetation evidence missing")
require("isClearableSiteVegetation(level, pos, state)" in construction, "grading does not clear verified natural tree vegetation")
require("isClearableSiteVegetation(level, supply, current)" in construction, "site supply position still rejects natural vegetation")
require("TREE_CANOPY_SEARCH_HEIGHT = 10" in construction and "TREE_CANOPY_SEARCH_RADIUS = 2" in construction, "bounded tree evidence envelope drifted")
require("for (int x = -1; x <= width; x++)" not in construction and "for (int z = -1; z <= depth; z++)" not in construction,
        "hidden exterior site/grading veto ring returned")
require("List<GradeCell> result = new ArrayList<>(width * depth);" in construction, "grading plan is not footprint-only")
require("현장 자재통 위치가 막혀 있습니다" in construction and "실제 건물 부지 안의 정리 칸이 막혀 있습니다" in construction,
        "placement blocker diagnostics missing")

require("withinConstructionProtectionEnvelope" in construction, "construction bulk-break coarse guard missing")
core_break = text(SETTLEMENT / "SettlementCoreService.java")
require("Math.abs(pos.getX() - center.getX()) > 6" in core_break, "civic core still rebuilds all tier plans for remote breaks")
waterfront_break = text(SETTLEMENT / "SettlementWaterfrontService.java")
require("brokenState.is(Blocks.SPRUCE_SLAB)" in waterfront_break and "brokenState.is(Blocks.BARREL)" in waterfront_break, "waterfront type gate missing")

road = text(SETTLEMENT / "SettlementRoadService.java")
outpost = text(SETTLEMENT / "SettlementOutpostService.java")
civil = text(SETTLEMENT / "SettlementCivilWorkService.java")
require("withinActiveRoadProtectionEnvelope" in road and "road.path()" in road, "road bulk-break coarse guard missing")
require("Math.abs(pos.getX() - state.gateX()) > 16" in outpost, "outpost bulk-break coarse guard missing")
require("infrastructureProjectBuilder" in road and "ProjectLane.ROAD" in road and "clearRoadConstruction" in road,
        "road start is not transactional through its dedicated builder lane")
require("infrastructureProjectBuilder" in outpost and "ProjectLane.OUTPOST" in outpost and "clearOutpostConstruction" in outpost,
        "outpost start is not transactional through its dedicated builder lane")
project_authority = text(SETTLEMENT / "SettlementProjectAuthority.java")
require("MAX_PARALLEL_MANAGED_PROJECTS = 3" in project_authority and "parallelProjectLimit" in project_authority,
        "managed parallel-project capacity missing")
require("MIN_PARALLEL_SEPARATION = 24" in project_authority and "routeSeparatedFromOtherActive" in project_authority,
        "parallel project physical-separation guard missing")
require("SettlementCivilWorkData.get(server).project().active()" in project_authority,
        "civil work is no longer exclusive against managed parallel projects")
require("startBlockReason" in construction and "ProjectLane.BUILDING" in construction,
        "building path bypasses centralized lane capacity")
require("startBlockReason" in road and "ProjectLane.ROAD" in road,
        "road path bypasses centralized lane capacity")
require("startBlockReason" in outpost and "ProjectLane.OUTPOST" in outpost,
        "outpost path bypasses centralized lane capacity")
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
context_ui = text(SETTLEMENT / "SettlementContextService.java")
require('"settlement", "settlement"' in context_ui and '"본진"' in context_ui and "data.centerPos()" in context_ui,
        "main settlement navigation target missing")
location_screen = text(JAVA / "client/SettlementLocationScreen.java")
require("거점 위치   · 본진/전초 좌표·방향" in palette and "new SettlementLocationScreen(this)" in palette,
        "explicit settlement-location button missing from infrastructure menu")
require('"settlement".equals(target.kind())' in location_screen and '"outpost".equals(target.kind())' in location_screen,
        "dedicated location screen does not enumerate main settlement and outposts")
require("markerX()" in location_screen and "markerY()" in location_screen and "markerZ()" in location_screen,
        "dedicated location screen does not expose saved coordinates")
require("distanceSq" in location_screen and "directionName" in location_screen and "오버월드" in location_screen,
        "dedicated location screen distance/direction/dimension behavior missing")

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

print("CURRENT SOURCE CHECK PASS: alpha113 bulk-break event guards + alpha112 footprint-only placement + prior invariants")
