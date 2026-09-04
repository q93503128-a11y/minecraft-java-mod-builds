from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"

def text(path): return path.read_text(encoding="utf-8")
def must(src, tokens, label):
    for token in tokens:
        if token not in src: raise SystemExit(f"{label} missing: {token}")
def forbid(src, tokens, label):
    for token in tokens:
        if token in src: raise SystemExit(f"{label} forbidden: {token}")

props = text(ROOT / "gradle.properties")
lock = json.loads(text(ROOT / "COMPANION_LOCK.json"))
worker = text(JAVA / "settlement/SettlementWorkerService.java")
storage = text(JAVA / "settlement/SettlementStorageService.java")
outpost = text(JAVA / "settlement/SettlementOutpostLogisticsService.java")
fishing = text(JAVA / "settlement/SettlementFishingOutpostService.java")
military = text(JAVA / "settlement/SettlementMilitaryOutpostService.java")
armory = text(JAVA / "settlement/SettlementMilitaryArmoryService.java")
barracks = text(JAVA / "settlement/SettlementBarracksService.java")
waterfront = text(JAVA / "settlement/SettlementWaterfrontService.java")
market = text(JAVA / "settlement/SettlementMarketService.java")
outpost_production = text(JAVA / "settlement/SettlementOutpostProductionService.java")
civil = text(JAVA / "settlement/SettlementCivilWorkService.java")
civil_fill = text(JAVA / "settlement/SettlementCivilFillSupplyService.java")
civil_retain = text(JAVA / "settlement/SettlementCivilRetainingService.java")
road_build = text(JAVA / "settlement/SettlementRoadService.java")
outpost_build = text(JAVA / "settlement/SettlementOutpostService.java")
office = text(JAVA / "settlement/SettlementConstructionOfficeService.java")
advanced_workshop = text(JAVA / "settlement/SettlementAdvancedWorkshopService.java")
workshop = text(JAVA / "settlement/SettlementWorkshopService.java")
storage_nav = text(JAVA / "settlement/SettlementWorkerStorageNavigation.java")
entity = text(JAVA / "content/FrontierWorkerEntity.java")
blueprints = text(JAVA / "settlement/BuildingBlueprints.java")
service = text(JAVA / "settlement/SettlementService.java")
core = text(JAVA / "settlement/SettlementCoreService.java")
content = text(JAVA / "content/FrontierContent.java")

must(props, ("mod_version=0.1.0-alpha.91", "Alpha.91 navigation/storage hardening"), "alpha91 props")
if lock["target"]["frontier_settlement"] != "0.1.0-alpha.91": raise SystemExit("alpha91 lock target")
if not any(str(x).startswith("Alpha.91 ") for x in lock.get("notes", [])): raise SystemExit("alpha91 lock note")
must(worker, (
    "TREE_SEARCH_RADIUS = 128", "QUARRY_SEARCH_RADIUS = 96", "MINE_HORIZONTAL_SEARCH_RADIUS = 48",
    "MINE_SEARCH_DEPTH = 80", "RESOURCE_TARGET_CACHE_TICKS", "RESOURCE_SEARCH_RETRY_TICKS",
    "STUCK_PROGRESS_TIMEOUT_TICKS", "path.canReach()", "createPath(approach, 0)",
    "deliverToWorksiteStorage", "worksiteStoragePosition", "farm.localToWorld(x, 1, z)",
    "resourceRouteMargin(type)", "findTreeForWorker", "findQuarryTargetForWorker"
), "worker hardening")
must(storage, (
    "LEGACY_PUBLIC_STOCKPILE_OFFSETS", "return List.of(data.stockpilePos())", "worksiteStoragePosition",
    "ensureManagedStorage", "generalStoragePositions", "positions.addAll(worksiteStoragePositions(data))",
    "replaceBarrelWithSupplyDepot", "preserved.add(oldContainer.getItem(slot).copy())",
    "SupplyDepotRegistryService.tryRegister(level, pos)",
    "boolean legacyPublicStorage = level.hasChunkAt(stockpile)",
    "if (legacyPublicStorage) upgradeLegacyPublicBarrels(level, data);",
    "ensureStarterSupplyDepot(level, stockpile);",
    "findLogisticsDepositTargetExcluding"
), "shared storage hardening")
must(outpost, (
    "findReachableExtractionTarget", "findReachableLogisticsDepositTarget",
    "findLogisticsDepositTargetExcluding", "canReachStorageInteraction",
    "moveToStorageInteraction", "createStoragePath", "path.canReach()"
), "outpost storage reachability")
must(worker, (
    "SettlementOutpostLogisticsService.reconcileLoadedAssignmentDuplicates(level, data)",
    "SettlementWorkshopService.reconcileLoadedAssignmentDuplicates(level, data)",
    "SettlementAdvancedWorkshopService.reconcileLoadedAssignmentDuplicates(level, data)",
    "static boolean removeDuplicateWorkerPreservingCargo"
), "assignment duplicate orchestration")
forbid(worker, (
    "private static boolean removeDuplicateWorkerPreservingCargo",
    "reconcileProductionDuplicates(server, level, data)"
), "legacy duplicate orchestration")
must(outpost, (
    "public static int reconcileLoadedAssignmentDuplicates",
    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))"
), "transport assignment duplicate cleanup")
must(outpost, ("survivor.setNoAi(false);", "survivor.setInvulnerable(false);"), "transport survivor normalization")
must(workshop, ("survivor.setNoAi(false);", "survivor.setInvulnerable(false);"), "workshop survivor normalization")
must(advanced_workshop, ("survivor.setNoAi(false);", "survivor.setInvulnerable(false);"), "advanced survivor normalization")
forbid(advanced_workshop, (
    "SettlementResidentRoutineService.isRestTime(level)",
    "if (rest) {"
), "orphan advanced workshop night pause")
must(workshop, (
    "public static int reconcileLoadedAssignmentDuplicates",
    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))"
), "workshop assignment duplicate cleanup")
must(advanced_workshop, (
    "public static int reconcileLoadedAssignmentDuplicates",
    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))"
), "advanced workshop assignment duplicate cleanup")
forbid(outpost, (
    "BlockPos target = SettlementStorageService.findLogisticsDepositTarget(level, data, carried);",
), "outpost forced storage fallback")
must(fishing, (
    "active.setInvulnerable(false);",
    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, workers.get(i))",
    "SettlementWorkerStorageNavigation.moveToInteraction(level, worker, stock, 0.82D, WORK_RANGE_SQR)"
), "fishing worker lifecycle/storage reachability")
forbid(fishing, (
    "duplicate.setNoAi(true);",
    "duplicate.setInvulnerable(true);",
    "worker.getNavigation().moveTo(stock.getX() + 0.5D"
), "legacy fishing duplicate/storage pathing")
must(military, (
    "active.setInvulnerable(false);",
    "removeDuplicateSentryPreservingWeapon(level, sentries.get(i))",
    "for (IronGolem duplicate : legacy)",
    "removeDuplicateSentryPreservingWeapon(level, duplicate)",
    "ItemStack carried = legacy.getMainHandItem().copy();",
    "replacement.setItemSlot(EquipmentSlot.MAINHAND, carried);",
    "legacy.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);",
    "removeDuplicateSentryPreservingWeapon(ServerLevel level, IronGolem duplicate)"
), "military sentry lifecycle/migration")
forbid(military, (
    "duplicate.setNoAi(true);",
    "Historical duplicate bodies are contained rather than deleted"
), "legacy military duplicate containment")
must(armory, (
    "SettlementWorkerStorageNavigation.canReachInteraction(",
    "SettlementWorkerStorageNavigation.moveToInteraction(",
    "soldier.getNavigation().stop();"
), "military armory reachable storage pathing")
forbid(armory, (
    "soldier.getNavigation().moveTo(source.getX() + 0.5D",
), "legacy military armory solid-container pathing")
must(barracks, (
    "active.setInvulnerable(false);",
    "removeDuplicateBarracksSoldierPreservingWeapon(level, soldiers.get(i))",
    "for (IronGolem duplicate : legacy)",
    "removeDuplicateBarracksSoldierPreservingWeapon(level, duplicate)",
    "if (!soldierAssignmentEvidenceLoaded(level, data, barracks)) return null;",
    "ItemStack carried = legacy.getMainHandItem().copy();",
    "replacement.setItemSlot(EquipmentSlot.MAINHAND, carried);",
    "legacy.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);",
    "removeDuplicateBarracksSoldierPreservingWeapon(ServerLevel level, IronGolem duplicate)"
), "barracks soldier lifecycle/migration")
forbid(barracks, (
    "duplicate.setNoAi(true);",
    "return legacy.isEmpty() ? null : migrateLegacySoldier(level, legacy.getFirst());"
), "legacy barracks duplicate containment")
must(waterfront, (
    "active.setInvulnerable(false);",
    "trader.setInvulnerable(false);",
    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, traders.get(i))",
    "level, worker, placement.pos(), 0.78D, INTERACTION_RANGE_SQR",
    "level, worker, stock, 0.8D, INTERACTION_RANGE_SQR"
), "waterfront trader/build path hardening")
forbid(waterfront, (
    "duplicate.setNoAi(true);",
    "duplicate.setInvulnerable(true);",
    "trader.setInvulnerable(true);",
    "worker.getNavigation().moveTo(placement.pos().getX() + 0.5D",
    "worker.getNavigation().moveTo(stock.getX() + 0.5D"
), "legacy waterfront lifecycle/pathing")
must(market, (
    "active.setInvulnerable(false);",
    "trader.setInvulnerable(false);",
    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))",
    "level, trader, crate, 0.7D, CRATE_REACHED_SQR"
), "market trader/path hardening")
forbid(market, (
    "duplicate.setNoAi(true);",
    "duplicate.setInvulnerable(true);",
    "trader.getNavigation().moveTo(crate.getX() + 0.5D"
), "legacy market lifecycle/pathing")
must(outpost_production, (
    "active.setInvulnerable(false);",
    "findLegacyWorkers(level, outpost, name, assignmentTag)",
    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))",
    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, duplicate)",
    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, legacy.get(i))",
    "if (!assignmentEvidenceLoaded(level, outpost)) return null;",
    "level, worker, stock, 0.82D, 9.0D",
    "level, worker, target, 0.8D, 8.0D",
    "level, worker, target, 0.78D, 9.0D"
), "outpost production lifecycle/pathing")
forbid(outpost_production, (
    "if (!assigned.isEmpty()) return assigned.getFirst();",
    "move(worker, stock, 0.82D);",
    "move(worker, target, 0.8D);",
    "move(worker, target, 0.78D);"
), "legacy outpost production duplicate/direct target")
must(worker, (
    "SettlementOutpostProductionService.PRODUCTION_WORKER_TAG",
), "outpost production death cargo preservation")
must(civil, (
    "builder.setInvulnerable(false);",
    "SettlementConstructionService.ensureBuilder(level, settlement)",
    "SettlementCivilFillSupplyService.returnCarriedToStorage(level, settlement, builder)"
), "civil builder runtime normalization")
forbid(civil, (
    "builder.setInvulnerable(true);",
), "civil builder orphan invulnerability")
must(civil_fill, (
    "SettlementWorkerStorageNavigation.findReachableExtractionTarget(",
    "SettlementWorkerStorageNavigation.findReachableDepositTarget(",
    "SettlementWorkerStorageNavigation.moveToInteraction(level, builder, source, 0.86D, STORAGE_REACHED_SQR)",
    "SettlementWorkerStorageNavigation.moveToInteraction(level, builder, target, 0.86D, STORAGE_REACHED_SQR)",
    "if (target == null) {",
    "builder.getNavigation().stop();"
), "civil fill storage reachability/null safety")
forbid(civil_fill, (
    "BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);",
    "builder.getNavigation().moveTo(source.getX() + 0.5D",
    "builder.getNavigation().moveTo(target.getX() + 0.5D"
), "legacy civil fill direct storage pathing")
must(civil_retain, (
    "SettlementWorkerStorageNavigation.findReachableExtractionTarget(",
    "SettlementWorkerStorageNavigation.moveToInteraction(level, builder, source, 0.86D, STORAGE_REACHED_SQR)",
    "builder.getNavigation().stop();"
), "civil retaining storage reachability")
forbid(civil_retain, (
    "SettlementStorageService.findExtractionTarget(level, data,",
    "builder.getNavigation().moveTo(source.getX() + 0.5D"
), "legacy civil retaining direct storage pathing")
must(worker, (
    "SettlementConstructionService.BUILDER_TAG",
), "civil builder death cargo preservation")
for construction_src, construction_label in ((road_build, "road construction"), (outpost_build, "outpost construction")):
    must(construction_src, (
        "builder.setInvulnerable(false);",
        "SettlementWorkerStorageNavigation.findReachableExtractionTarget(",
        "SettlementWorkerStorageNavigation.findReachableDepositTarget(",
        "SettlementWorkerStorageNavigation.moveToInteraction(",
        "if (target == null) {",
        "builder.getNavigation().stop();"
    ), construction_label + " runtime/storage hardening")
    forbid(construction_src, (
        "builder.setInvulnerable(true);",
        "SettlementStorageService.findDepositTarget(level, data, carried);",
        "builder.getNavigation().moveTo(source.getX() + 0.5D",
        "builder.getNavigation().moveTo(target.getX() + 0.5D"
    ), "legacy " + construction_label + " runtime/storage pathing")
must(office, (
    "removeDuplicateRunnerPreservingCargo", "canReachInteraction", "moveToInteraction",
    "createInteractionPath", "path.canReach()", "canReachInteraction(level, runner, pos)"
), "construction office runner hardening")
must(office, (
    "runner.setNoAi(false);", "runner.setInvulnerable(false);",
    "keep.setInvulnerable(false);"
), "construction office runner runtime state")
forbid(office, (
    "SettlementResidentRoutineService.isRestTime(level)",
    "runner.setInvulnerable(true);",
    "keep.setInvulnerable(true);",
    "if (rest || !activeProject)"
), "construction office orphan rest/invulnerability")
must(worker, (
    "SettlementConstructionOfficeService.SUPPLY_RUNNER_TAG",
), "construction office death cargo preservation")
forbid(office, (
    "duplicate.setNoAi(true);",
    "runner.getNavigation().moveTo(source.getX() + 0.5D",
), "construction office legacy runner freeze/source pathing")
must(advanced_workshop, (
    "private static boolean isForgeMetal(ItemStack stack)",
    "SettlementStorageService.isMetalStack(stack)",
    "!stack.is(ExternalContentTags.EXPEDITION_RELICS)",
    "SettlementAdvancedWorkshopService::isForgeMetal"
), "advanced workshop disjoint costs")
forbid(advanced_workshop, (
    "SettlementStorageService::isMetalStack",
    "SettlementStorageService.isMetalStack(carried)"
), "advanced workshop overlapping metal predicate")
must(storage_nav, (
    "PathfinderMob worker", "findReachableExtractionTarget", "findReachableDepositTarget",
    "moveToInteraction", "createExactPath", "path.canReach()"
), "shared worker/soldier storage navigation")
must(workshop, (
    "SettlementWorkerStorageNavigation.findReachableExtractionTarget",
    "SettlementWorkerStorageNavigation.findReachableDepositTarget",
    "SettlementWorkerStorageNavigation.moveToInteraction"
), "basic workshop storage reachability")
must(advanced_workshop, (
    "SettlementWorkerStorageNavigation.findReachableExtractionTarget",
    "SettlementWorkerStorageNavigation.findReachableDepositTarget",
    "SettlementWorkerStorageNavigation.moveToInteraction"
), "advanced workshop storage reachability")
forbid(workshop, (
    "SettlementStorageService.findExtractionTarget(level, data, SettlementStorageService::isMetalStack)",
    "BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);"
), "basic workshop forced storage target")
forbid(advanced_workshop, (
    "BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);",
), "advanced workshop forced storage target")
must(service, (
    "FrontierContent.SUPPLY_DEPOT.get().defaultBlockState()",
    "SupplyDepotRegistryService.tryRegister(level, stockpile)",
    "SettlementStorageService.ensureManagedStorage(server.overworld(), data)",
    "SettlementStorageService.ensureManagedStorage(level, data)"
), "starter shared depot")
must(core, (
    "SettlementStorageService.isManagedStoragePosition(data, pos)",
    "current.is(FrontierContent.SUPPLY_DEPOT.get())"
), "managed storage protection")
must(content, ("SUPPLY_DEPOT_ITEM", "BuildCreativeModeTabContentsEvent"), "shared depot visibility")
must(entity, ("PathType.WATER", "setPathfindingMalus(PathType.WATER, -1.0F)"), "water avoidance")
must(blueprints, ("b.put(5, 1, 6, Blocks.BARREL.defaultBlockState(), Phase.FINISH);",), "lumber barrel")
forbid(storage, ("PUBLIC_STOCKPILE_TARGET_BARRELS",), "superseded public barrel cluster")
forbid(worker, ("farm.origin().offset(x, 1, z)",), "rotated farm bug")
forbid(worker, ("getNavigation().moveTo(\n                    approach.getX() + 0.5D",), "coordinate-only partial path")
print("Alpha.91 source audit: PASS")
