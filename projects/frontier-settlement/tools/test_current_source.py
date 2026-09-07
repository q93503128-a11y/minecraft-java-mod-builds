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
require("mod_version=0.1.0-alpha.125" in gradle, "current verifier/version drift")

inventory = text(SETTLEMENT / "SettlementInventory.java")
storage = text(SETTLEMENT / "SettlementStorageService.java")
require("if (stack.is(Items.DIAMOND)) return 6;" in inventory, "diamond metal value missing or drifted")
require(inventory.count("stack.is(Items.DIAMOND)") >= 2, "diamond is not included in settlement metal classification")
require("return SettlementInventory.metalValue(stack) > 0;" in storage, "storage bypasses canonical metal authority")
require("consumeMetalAndFood" in inventory, "local metal value consumption missing")
require("for (int unit = 1; unit <= 24" in storage, "low-value-first shared-resource consumption priority regressed")
require("record ResourceCounts" in inventory and "countResources(Container container)" in inventory,
        "single-pass settlement resource aggregation missing")
require("SettlementInventory.ResourceCounts counts = SettlementInventory.countResources(container);" in storage,
        "shared storage ledger returned to four independent container scans")

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
require("MAX_BUILDER_CREW = 14" in construction and "CIVIC_HALL_BUILDER_BONUS = 2" in construction,
        "civic-hall bounded construction crew bonus missing")
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
require("BASE_WORKER_ATTRACTION_INTERVAL_TICKS = 600" in worker
        and "CIVIC_HALL_WORKER_ATTRACTION_INTERVAL_TICKS = 400" in worker
        and "workerAttractionIntervalTicks(data)" in worker,
        "civic-hall civilian attraction cadence missing")
require("WORKSITE_STORAGE_INTERACTION_REACH_SQR = 36.0D" in worker, "close worksite deposit reach missing")
require("deliverIfCargoFull" in worker, "full-stack immediate deposit handoff missing")
require("tryExportWorksiteBuffer(" not in worker, "retired worksite re-extraction loop returned")
require("LEGACY_WORKSITE_EXPORT_TAG" in worker and "worker.removeTag(LEGACY_WORKSITE_EXPORT_TAG)" in worker, "legacy export-tag migration missing")
require("Profession barrels are already part of SettlementStorageService's authoritative physical" in worker, "worksite barrel authority rationale missing")
require("matchWorkersToBuildings" in worker and "WorkerBuildingAssignment" in worker,
        "same-profession workers returned to UUID/list-index workplace assignment")
require("minimum-total-distance bipartite assignment" in worker
        and "rowPotential" in worker and "columnPotential" in worker and "assignmentCost(" in worker,
        "same-profession worker matching regressed to greedy nearest-pair selection")
require("BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME, lumber)" in worker
        and "BuildingType.FARM, FARM_WORKER_NAME, farm)" in worker
        and "BuildingType.QUARRY, QUARRY_WORKER_NAME, quarry)" in worker
        and "BuildingType.MINE, MINE_WORKER_NAME, mine)" in worker,
        "vacancy recruitment no longer uses physical worker/workplace matching")
production_efficiency = text(SETTLEMENT / "SettlementProductionEfficiencyService.java")
production_upgrade = text(SETTLEMENT / "SettlementProductionUpgradeService.java")
building_record = text(SETTLEMENT / "BuildingRecord.java")
require("upgrade_grade" in building_record and "optionalFieldOf(\"upgrade_grade\", 0)" in building_record and "withUpgradeGrade" in building_record,
        "save-compatible per-building production grade missing")
require("maxGrade(SettlementData data)" in production_efficiency and "grade(SettlementData data, BuildingRecord building)" in production_efficiency,
        "tier-ceiling/per-building production authority missing")
require("building.upgradeGrade() <= 0" in production_efficiency and "return maxGrade(data)" in production_efficiency,
        "legacy grade fallback missing before one-way migration")
require("migrateLegacyGrades" in production_upgrade and "building.withUpgradeGrade(inherited)" in production_upgrade,
        "pre-Alpha.122 free-grade inheritance migration missing")
require("new UpgradeCost(128L, 96L, 0L)" in production_upgrade
        and "new UpgradeCost(256L, 192L, 32L)" in production_upgrade
        and "new UpgradeCost(512L, 384L, 96L)" in production_upgrade,
        "RTS production investment cost ladder drifted")
require("countProductionUpgradeMetal" in production_upgrade and "consumeProductionUpgrade" in production_upgrade,
        "facility improvement does not use the atomic common-metal payment path")
require("player.isShiftKeyDown()" in production_upgrade and "event.getItemStack().isEmpty()" in production_upgrade
        and "productionBuildingAt" in production_upgrade, "player-directed local-barrel improvement interaction missing")
require("farmBatch" in production_efficiency and "case 1 -> 12" in production_efficiency and "default -> 24" in production_efficiency,
        "farm harvest batch ladder missing or drifted")
require("case CAMP, HAMLET -> 1" in production_efficiency and "case DOMAIN, FRONTIER_CAPITAL -> 4" in production_efficiency, "production improvement ceiling ladder drifted")
require("farmGrowthModulo" in production_efficiency and "mineWorkPeriod" in production_efficiency, "production efficiency parameters incomplete")
require("worksiteBufferCount" in production_efficiency and "case 1 -> 1; case 2 -> 2; default -> 3" in production_efficiency,
        "per-building physical worksite buffer ladder missing")
require("MAX_WORKSITE_BUFFER_BARRELS = 3" in storage
        and "desiredWorksiteStoragePositions" in storage
        and "worksiteStoragePositions(BuildingRecord building)" in storage,
        "bounded physical profession-buffer storage authority missing")
require("SettlementStorageService.desiredWorksiteStoragePositions(building, data)" in worker,
        "production workers can bypass their paid local-buffer grade")
require("SettlementProductionEfficiencyService.grade(data, camp)" in worker
        and "SettlementProductionEfficiencyService.grade(data, farm)" in worker
        and "SettlementProductionEfficiencyService.grade(data, quarry)" in worker
        and "SettlementProductionEfficiencyService.grade(data, mine)" in worker,
        "production loops are not reading their own persisted building grade")
require("SettlementProductionEfficiencyService.grade(data, building)" in storage and "canProvisionWorksiteBuffers" in storage,
        "worksite buffer capacity does not follow safe per-building improvement")
require("consumeProductionUpgrade" in storage and "countProductionUpgradeMetal" in storage
        and "SettlementEquipmentUpgradeService::isBlacksmithMetal" in storage,
        "production investment physical payment is not atomic/common-metal-only")
require("desiredWorksiteStoragePositions(building, data)" in storage,
        "locked local barrels can still join the settlement resource ledger")
service = text(SETTLEMENT / "SettlementService.java")
require("SettlementProductionUpgradeService.tick(server, data)" in service, "production-grade migration is not wired into settlement runtime")
entry = text(JAVA / "FrontierSettlement.java")
require("SettlementProductionUpgradeService::onRightClickBlock" in entry, "production improvement interaction is not registered")

military_upgrade = text(SETTLEMENT / "SettlementMilitaryUpgradeService.java")
require("new UpgradeCost(256L, 192L, 32L)" in military_upgrade
        and "new UpgradeCost(512L, 384L, 96L)" in military_upgrade
        and "new UpgradeCost(1024L, 768L, 192L)" in military_upgrade,
        "military RTS investment ladder drifted")
require("barracksSlots" in military_upgrade and "return 2 + grade(barracks)" in military_upgrade,
        "paid barracks capacity ladder missing")
require("remoteFoodReserve" in military_upgrade and "remoteMetalReserve" in military_upgrade
        and "remotePatrolRadius" in military_upgrade, "citadel territory-command benefits missing")
require("SettlementMilitaryUpgradeService.tick(server, data)" in service, "military grade migration not wired")
require("SettlementMilitaryUpgradeService::onRightClickBlock" in entry, "military improvement interaction not registered")
armory = text(SETTLEMENT / "SettlementMilitaryArmoryService.java")
require("tickRecovery" in armory and "tickOutpostRecovery" in armory
        and "consumeCommonMilitarySupply" in armory, "physical post-combat recovery missing")
require("countCommonMilitaryMetal" in inventory and "countMilitaryFood" in inventory
        and "GOLDEN_APPLE" in inventory and "ENCHANTED_GOLDEN_APPLE" in inventory,
        "military recovery can consume protected high-value supplies")
require("soldierSlots(barracks)" in text(SETTLEMENT / "SettlementBarracksService.java")
        and "case 3 -> 3; case 4 -> 11; default -> 13" in text(SETTLEMENT / "SettlementBarracksService.java"),
        "expanded barracks slots are not bounded inside the drill yard")

guide = text(JAVA / "client/SettlementGuideScreen.java")
require("생산시설 현장 저장통을 우클릭" in guide, "in-game guide does not teach production investment")
production_status = text(SETTLEMENT / "SettlementProductionStatusService.java")
require("STALE_AFTER_TICKS = 200L" in production_status and "statusFor" in production_status, "production status cache missing")
require("주민 없음" in worker and "주변 벌목 대상 없음" in worker and "접근 가능한 채석면 없음" in worker and "광맥 고갈" in worker and "현장·공동 저장고 가득 참" in worker, "production status coverage missing")
context = text(SETTLEMENT / "SettlementContextService.java")
require("SettlementProductionStatusService.statusFor(level, building)" in context, "production context cache reader missing")
require("SettlementProductionEfficiencyService.farmWorkPeriod" in worker, "farm still uses fixed work cadence")
require("SettlementProductionEfficiencyService.farmBatch" in worker and "harvestLimit" in worker,
        "staffed farm harvest is not bounded against full-stack-per-pass runaway")
require("state.setValue(BlockStateProperties.AGE_7, Math.min(7, age + 1))" in worker, "staffed farm does not actively tend crop growth")
require("tryReplantHarvestedTree" in worker and "saplingForNaturalLog" in worker,
        "town lumber worker no longer restores a physical managed forestry cycle")
require("findManagedQuarryStone" in worker and "MANAGED_QUARRY_MAX_OVERBURDEN = 4" in worker
        and "clearTopQuarryOverburden" in worker,
        "town quarry still requires player-pre-exposed stone")
for stale in ("FARM_WORK_PERIOD_TICKS", "LUMBER_WORK_PERIOD_TICKS", "QUARRY_WORK_PERIOD_TICKS", "MINING_WORK_PERIOD_TICKS"):
    require(stale not in worker, f"stale fixed production pacing authority returned: {stale}")
outpost_production = text(SETTLEMENT / "SettlementOutpostProductionService.java")
require("MAX_LOGS = 8" in outpost_production and "MAX_STONE = 8" in outpost_production,
        "specialized outpost lumber/quarry batches did not receive the physical throughput correction")
require("findManagedQuarryStone" in outpost_production and "clearTopQuarryOverburden" in outpost_production,
        "specialized quarry outpost still requires pre-exposed stone")
require("tryReplantHarvestedTree" in outpost_production,
        "specialized lumber outpost lacks managed physical replanting")
require("WORKER_MAINTENANCE_INTERVAL_TICKS = 200" in outpost_production
        and "WORKER_UUID_CACHE" in outpost_production
        and "resolveCachedWorker" in outpost_production
        and "level.getEntity(uuid)" in outpost_production,
        "specialized outpost worker lookup returned to wide AABB scans on the hot path")
require("TARGET_RESCAN_DELAY_TICKS = 100" in outpost_production
        and "LUMBER_TARGET_CACHE" in outpost_production
        and "QUARRY_TARGET_CACHE" in outpost_production
        and "resolveLumberTarget" in outpost_production
        and "resolveQuarryTarget" in outpost_production,
        "specialized outpost physical target caching/regression backoff missing")
require("validCachedLumberTarget" in outpost_production and "validCachedQuarryTarget" in outpost_production
        and "withinTargetEnvelope" in outpost_production,
        "cached specialized target validation missing")

fishing = text(SETTLEMENT / "SettlementFishingOutpostService.java")
require("shorelineHits" in fishing and "shorelineMisses" in fishing and "prepareShorelineCache" in fishing,
        "fishing shoreline scan cache missing")
require("WORKER_MAINTENANCE_INTERVAL_TICKS = 200" in fishing
        and "WORKER_UUID_CACHE" in fishing
        and "resolveCachedWorker" in fishing
        and "level.getEntity(uuid)" in fishing,
        "fishing worker lookup returned to full assignment AABB scans every second")

service = text(SETTLEMENT / "SettlementService.java")
require("SettlementGuidanceService.nextGoal(player.level().getServer(), data)" in service, "guidance is missing server authority")
require("8-tick grading gate" not in service, "obsolete construction cadence prose remains")
require("tick % 20 == 0) SettlementConstructionService.settleIdleBuilders" in service, "idle builder path maintenance cadence regressed")
require("List<FrontierWorkerEntity> existing = new ArrayList<>(findBuilders(level, data));" in construction, "active builder discovery path missing")

palette = text(JAVA / "client/BuildingPaletteScreen.java")
require("civilUnlocked" not in palette, "client still owns partial civil unlock logic")
require("마을 등급" in palette and "SETTLEMENT_TIER_COUNT = 6" in palette,
        "settlement tier hierarchy is not explicit in the M palette")
require("다음 성장" in palette and "data.nextGoal()" in palette,
        "server-authored next growth goal is missing from the M palette")
require("FOUNDATION(" in palette and "PRODUCTION(" in palette and "SERVICES(" in palette
        and "DEFENSE(" in palette and "LANDMARKS(" in palette and "INFRA(" in palette,
        "construction-family navigation regressed")
require("잠김" in palette and "건설 가능" in palette and "자원 부족" in palette,
        "building availability states are not explicit")
require("FrontierUiTheme" in palette, "M palette bypasses shared Frontier UI tokens")
require("주민 유입 20초 · 건설 인력 +2" in palette and "현장 버퍼" in palette,
        "M palette hides Alpha.119 civic/production infrastructure effects")

for client_name in ("BuildingPlacementClient.java", "RoadPlacementClient.java", "OutpostPlacementClient.java", "CivilWorkPlacementClient.java"):
    placement_client = text(JAVA / "client" / client_name)
    require("STATIONARY_REFRESH_TICKS = 20" in placement_client,
            f"stationary preview throttle missing: {client_name}")
    require("refreshTicks = 5" not in placement_client,
            f"five-tick stationary preview polling returned: {client_name}")

context_ui = text(SETTLEMENT / "SettlementContextService.java")
require('"settlement", "settlement"' in context_ui and '"본진"' in context_ui and "data.centerPos()" in context_ui,
        "main settlement navigation target missing")
location_screen = text(JAVA / "client/SettlementLocationScreen.java")
require("거점 위치" in palette and "new SettlementLocationScreen(this)" in palette,
        "explicit settlement-location button missing from infrastructure menu")
require('"settlement".equals(target.kind())' in location_screen and '"outpost".equals(target.kind())' in location_screen,
        "dedicated location screen does not enumerate main settlement and outposts")
require("markerX()" in location_screen and "markerY()" in location_screen and "markerZ()" in location_screen,
        "dedicated location screen does not expose saved coordinates")
require("distanceSq" in location_screen and "directionName" in location_screen and "오버월드" in location_screen,
        "dedicated location screen distance/direction/dimension behavior missing")
require("FrontierUiTheme" in location_screen, "location screen bypasses shared Frontier UI tokens")

hud = text(JAVA / "client/SettlementHudOverlay.java")
require("FrontierUiTheme" in hud and "projectProgress()" in hud,
        "settlement HUD does not use shared hierarchy/progress tokens")
require("data.nextGoal()" not in hud, "full growth guidance returned to the permanent idle HUD")
start_screen = text(JAVA / "client/SettlementStartScreen.java")
guide_screen = text(JAVA / "client/SettlementGuideScreen.java")
require("FrontierUiTheme" in start_screen and "54칸 공동 보급고" in start_screen,
        "founding screen is not aligned with current storage/UI authority")
require("FrontierUiTheme" in guide_screen and "M 화면의 마을 등급" in guide_screen,
        "guide screen is not aligned with current M-centered UX")

benefit = text(SETTLEMENT / "SettlementBenefitService.java")
require("onRightClickBlock" in benefit and "player.isShiftKeyDown()" in benefit
        and "SettlementEquipmentUpgradeService.consumeBlacksmithMetalItems" in benefit,
        "blacksmith repair is not explicit player-directed copper/iron maintenance")
require("SettlementStorageService.consumeMetal(level, data, metalCost)" not in benefit,
        "blacksmith repair can still implicitly consume gold/diamond-valued storage")
require("repairNearbyEquipment" not in benefit, "automatic proximity blacksmith repair returned")

equipment_upgrade = text(SETTLEMENT / "SettlementEquipmentUpgradeService.java")
require("MAX_REINFORCEMENT_LEVEL = 5" in equipment_upgrade
        and "ATTACK_DAMAGE_PER_LEVEL = 0.5D" in equipment_upgrade
        and "ARMOR_PER_LEVEL = 0.25D" in equipment_upgrade,
        "deterministic blacksmith reinforcement stat ladder missing")
require("case FRONTIER_TOWN -> 2" in equipment_upgrade
        and "case DOMAIN -> 4" in equipment_upgrade
        and "case FRONTIER_CAPITAL -> 5" in equipment_upgrade,
        "settlement-tier reinforcement caps missing")
require("case 1 -> 4" in equipment_upgrade and "case 2 -> 8" in equipment_upgrade
        and "case 3 -> 12" in equipment_upgrade and "case 4 -> 18" in equipment_upgrade
        and "default -> 26" in equipment_upgrade,
        "physical reinforcement cost ladder missing")
require("DataComponents.CUSTOM_DATA" in equipment_upgrade and "CustomData.update" in equipment_upgrade,
        "reinforcement level is not stored as bounded per-stack custom data")
require("ItemAttributeModifierEvent" in equipment_upgrade
        and "event.addModifier(Attributes.ATTACK_DAMAGE" in equipment_upgrade
        and "event.addModifier(Attributes.ARMOR" in equipment_upgrade,
        "reinforcement bypasses additive NeoForge item-attribute authority")
require("Items.COPPER_INGOT" in equipment_upgrade and "Items.RAW_COPPER" in equipment_upgrade
        and "Items.IRON_INGOT" in equipment_upgrade and "Items.RAW_IRON" in equipment_upgrade
        and "Items.GOLD_INGOT" not in equipment_upgrade and "Items.DIAMOND" not in equipment_upgrade,
        "blacksmith payment is not restricted to common copper/iron items")
require("SettlementEquipmentUpgradeService::onItemAttributeModifiers" in text(JAVA / "FrontierSettlement.java"),
        "dynamic reinforcement attribute listener is not registered on the NeoForge event bus")

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

print("CURRENT SOURCE CHECK PASS: Frontier Settlement 0.1.0-alpha.117 UI/runtime hardening + prior invariants")


barracks_runtime = text(SETTLEMENT / "SettlementBarracksService.java")
require("SettlementMilitaryUpgradeService.barracksPatrolRadius(data, barracks)" in barracks_runtime and "SettlementMilitaryUpgradeService.barracksThreatRadiusBonus(data, barracks)" in barracks_runtime, "paid barracks/citadel response bonus missing")
require("Monster threat = nearestThreat(level, data, barracks, barracks.workCenter());" in barracks_runtime and "patrol(level, data, barracks, slot, soldier, threat)" in barracks_runtime, "barracks shared threat scan missing")
require("patrolAreaLoaded(ServerLevel level, SettlementData data, BuildingRecord barracks)" in barracks_runtime, "citadel patrol loaded-area gate missing")


logistics_upgrade = text(SETTLEMENT / "SettlementLogisticsUpgradeService.java")
warehouse_layout = text(SETTLEMENT / "WarehouseLayout.java")
cart_layout = text(SETTLEMENT / "CartStationLayout.java")
require("case CAMP, HAMLET, VILLAGE -> 1" in logistics_upgrade
        and "case FRONTIER_TOWN -> 2" in logistics_upgrade
        and "case DOMAIN, FRONTIER_CAPITAL -> 3" in logistics_upgrade,
        "logistics tier ceiling drifted")
require("new UpgradeCost(256L, 192L, 24L)" in logistics_upgrade
        and "new UpgradeCost(512L, 384L, 64L)" in logistics_upgrade
        and "new UpgradeCost(320L, 224L, 32L)" in logistics_upgrade
        and "new UpgradeCost(640L, 448L, 96L)" in logistics_upgrade,
        "warehouse/cart logistics investment costs drifted")
require("player.isShiftKeyDown()" in logistics_upgrade and "event.getItemStack().isEmpty()" in logistics_upgrade
        and "logisticsBuildingAt" in logistics_upgrade,
        "player-directed logistics upgrade interaction missing")
require("countCommonUpgradeMetal" in logistics_upgrade and "consumeLogisticsUpgrade" in logistics_upgrade,
        "logistics investment bypasses common-metal atomic payment")
require("Grade I/II/III = 6/10/14 real barrels" in warehouse_layout
        and "activeStoragePositions" in warehouse_layout,
        "warehouse physical capacity ladder missing")
require("Grade I/II/III = 4/6/8 physical freight barrels" in cart_layout
        and "activeFreightPositions" in cart_layout,
        "cart-station physical freight capacity ladder missing")
require("WarehouseLayout.activeStoragePositions(building)" in storage
        and "CartStationLayout.activeFreightPositions(building)" in storage,
        "inactive future logistics barrels can join the settlement ledger")
require("SettlementLogisticsUpgradeService.ensureManagedStorage(level, data)" in storage
        and "public static boolean canSafelyCreateManagedBarrel" in storage,
        "safe logistics storage provisioning is not wired")
require("CART_STATION_GRADE_II_TRANSPORT_STACK = 40" in logistics
        and "CART_STATION_GRADE_III_TRANSPORT_STACK = 48" in logistics
        and "MAX_PRODUCTIVE_TRANSPORT_STACK = 64" in logistics
        and "SettlementLogisticsUpgradeService.warehouseFreightBonus(data)" in logistics,
        "productive freight grade/warehouse throughput ladder missing")
require("public static int transportBatchSize(SettlementData data)" in logistics
        and "CART_STATION_TRANSPORT_STACK" in logistics,
        "ordinary reverse-supply transport authority was removed")
require("SettlementLogisticsUpgradeService.tick(server, data)" in service,
        "logistics legacy migration is not wired into settlement runtime")
require("SettlementLogisticsUpgradeService::onRightClickBlock" in entry,
        "logistics upgrade interaction is not registered")
require("SettlementLogisticsUpgradeService.storageSummary(level, building)" in context,
        "warehouse/cart saturation context missing")
require("창고·수레 정거장은 빈손 웅크리기+저장통 우클릭" in guide_screen,
        "in-game guide does not teach logistics investment")

# Alpha.125 RTS operations visibility.
operations_summary = text(JAVA / "client" / "SettlementOperationsSummary.java")
operations_screen = text(JAVA / "client" / "SettlementOperationsScreen.java")
palette_screen = text(JAVA / "client" / "BuildingPaletteScreen.java")
require("Presentation-only RTS summary" in operations_summary and "snapshot.context().targets()" in operations_summary,
        "operations summary stopped reusing the existing presentation snapshot/context")
require("productionUpgradeBacklog" in operations_summary and "logisticsUpgradeBacklog" in operations_summary
        and "militaryUpgradeBacklog" in operations_summary, "RTS paid-upgrade backlog visibility missing")
require("ClientSettlementState.snapshot()" in operations_screen and "SettlementOperationsSummary.from(snapshot)" in operations_screen,
        "operations screen is not driven by synchronized client presentation state")
require("new SettlementOperationsScreen(this)" in palette_screen, "M palette operations entry point missing")
require("신규 개량 I" in palette_screen and "신규 물류 I" in palette_screen and "신규 군사 I" in palette_screen,
        "construction palette returned to misleading free tier-derived facility grades")
require("완공 후 현장 저장통에서 수동 개량" in palette_screen,
        "production investment interaction guidance missing from construction palette")
