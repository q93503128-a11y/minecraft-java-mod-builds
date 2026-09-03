from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement"
WORKER = JAVA / "SettlementWorkerService.java"
OUTPOST = JAVA / "SettlementOutpostLogisticsService.java"
WORKSHOP = JAVA / "SettlementWorkshopService.java"
ADVANCED = JAVA / "SettlementAdvancedWorkshopService.java"
AUDIT = ROOT / "tools/test_alpha91_source.py"

worker = WORKER.read_text(encoding="utf-8")
outpost = OUTPOST.read_text(encoding="utf-8")
workshop = WORKSHOP.read_text(encoding="utf-8")
advanced = ADVANCED.read_text(encoding="utf-8")
audit = AUDIT.read_text(encoding="utf-8")

old_tick = '''        if (server.getTickCount() % 10 == 0) {\n            SettlementOutpostLogisticsService.migrateLegacyWorkers(level, data);\n            SettlementConstructionService.reconcileBuilderDuplicates(level, data);\n            reconcileProductionDuplicates(server, level, data);\n        }\n'''
new_tick = '''        if (server.getTickCount() % 10 == 0) {\n            SettlementOutpostLogisticsService.migrateLegacyWorkers(level, data);\n            SettlementConstructionService.reconcileBuilderDuplicates(level, data);\n            int removedDuplicates = reconcileProductionDuplicates(level, data);\n            removedDuplicates += SettlementOutpostLogisticsService.reconcileLoadedAssignmentDuplicates(level, data);\n            removedDuplicates += SettlementWorkshopService.reconcileLoadedAssignmentDuplicates(level, data);\n            removedDuplicates += SettlementAdvancedWorkshopService.reconcileLoadedAssignmentDuplicates(level, data);\n            if (removedDuplicates > 0) {\n                // Recompute only after every civilian duplicate authority has been normalized.\n                // If some assignment evidence is unloaded, repairPopulationAfterDuplicateCleanup()\n                // deliberately keeps the saved count conservative until a complete view is available.\n                repairPopulationAfterDuplicateCleanup(level, data);\n                SettlementService.refreshResources(server, data);\n                SettlementService.broadcast(server, data);\n            }\n        }\n'''
if new_tick not in worker:
    if worker.count(old_tick) != 1:
        raise SystemExit(f"worker tick anchor count={worker.count(old_tick)}")
    worker = worker.replace(old_tick, new_tick, 1)

old_reconcile = '''    private static int reconcileProductionDuplicates(MinecraftServer server, ServerLevel level, SettlementData data) {\n        // Seeing more loaded physical workers than completed jobs is already sufficient proof of an\n        // excess entity. No unloaded resident can make N+1 loaded bodies legal for N completed jobs,\n        // so duplicate removal itself must not be blocked by the much wider recruitment evidence gate.\n        int removed = 0;\n        removed += trimExcessProductionWorkers(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME);\n        removed += trimExcessProductionWorkers(level, data, BuildingType.FARM, FARM_WORKER_NAME);\n        removed += trimExcessProductionWorkers(level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME);\n        removed += trimExcessProductionWorkers(level, data, BuildingType.MINE, MINE_WORKER_NAME);\n        if (removed > 0) {\n            repairPopulationAfterDuplicateCleanup(level, data);\n            SettlementService.refreshResources(server, data);\n            SettlementService.broadcast(server, data);\n        }\n        return removed;\n    }\n'''
new_reconcile = '''    private static int reconcileProductionDuplicates(ServerLevel level, SettlementData data) {\n        // Seeing more loaded physical workers than completed jobs is already sufficient proof of an\n        // excess entity. No unloaded resident can make N+1 loaded bodies legal for N completed jobs,\n        // so duplicate removal itself must not be blocked by the much wider recruitment evidence gate.\n        int removed = 0;\n        removed += trimExcessProductionWorkers(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME);\n        removed += trimExcessProductionWorkers(level, data, BuildingType.FARM, FARM_WORKER_NAME);\n        removed += trimExcessProductionWorkers(level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME);\n        removed += trimExcessProductionWorkers(level, data, BuildingType.MINE, MINE_WORKER_NAME);\n        return removed;\n    }\n'''
if new_reconcile not in worker:
    if worker.count(old_reconcile) != 1:
        raise SystemExit(f"worker reconcile anchor count={worker.count(old_reconcile)}")
    worker = worker.replace(old_reconcile, new_reconcile, 1)

old_helper = '''    private static boolean removeDuplicateWorkerPreservingCargo(ServerLevel level, FrontierWorkerEntity worker) {\n'''
new_helper = '''    static boolean removeDuplicateWorkerPreservingCargo(ServerLevel level, FrontierWorkerEntity worker) {\n'''
if new_helper not in worker:
    if worker.count(old_helper) != 1:
        raise SystemExit(f"worker helper anchor count={worker.count(old_helper)}")
    worker = worker.replace(old_helper, new_helper, 1)

outpost_anchor = '''    public static int loadedAssignedWorkerCount(ServerLevel level, SettlementData data) {\n'''
outpost_method = '''    /**\n     * One outpost assignment owns exactly one physical transporter. Two loaded bodies carrying the\n     * same assignment tag are conclusive duplicate evidence even when some other route chunk is not\n     * loaded. Keep the deterministic lowest-UUID body and preserve every excess MAINHAND cargo stack\n     * as a physical item before discarding the duplicate.\n     */\n    public static int reconcileLoadedAssignmentDuplicates(ServerLevel level, SettlementData data) {\n        int removed = 0;\n        for (OutpostRecord outpost : data.outposts()) {\n            List<FrontierWorkerEntity> assigned = findAssignedWorkers(level, data, outpost);\n            for (int i = 1; i < assigned.size(); i++) {\n                if (SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))) removed++;\n            }\n        }\n        return removed;\n    }\n\n'''
if "public static int reconcileLoadedAssignmentDuplicates" not in outpost:
    if outpost.count(outpost_anchor) != 1:
        raise SystemExit(f"outpost insertion anchor count={outpost.count(outpost_anchor)}")
    outpost = outpost.replace(outpost_anchor, outpost_method + outpost_anchor, 1)

workshop_anchor = '''    public static int loadedAssignedWorkerCount(ServerLevel level, SettlementData data) {\n'''
workshop_method = '''    /** One completed workshop owns exactly one assigned artisan. */\n    public static int reconcileLoadedAssignmentDuplicates(ServerLevel level, SettlementData data) {\n        int removed = 0;\n        for (BuildingRecord workshop : data.buildings()) {\n            if (workshop.buildingType() != BuildingType.WORKSHOP) continue;\n            List<FrontierWorkerEntity> assigned = findAssignedWorkers(level, data, workshop);\n            for (int i = 1; i < assigned.size(); i++) {\n                if (SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))) removed++;\n            }\n        }\n        return removed;\n    }\n\n'''
if "public static int reconcileLoadedAssignmentDuplicates" not in workshop:
    if workshop.count(workshop_anchor) != 1:
        raise SystemExit(f"workshop insertion anchor count={workshop.count(workshop_anchor)}")
    workshop = workshop.replace(workshop_anchor, workshop_method + workshop_anchor, 1)

advanced_anchor = '''    public static int loadedAssignedWorkerCount(ServerLevel level, SettlementData data) {\n'''
advanced_method = '''    /** One completed advanced workshop owns exactly one assigned artisan. */\n    public static int reconcileLoadedAssignmentDuplicates(ServerLevel level, SettlementData data) {\n        int removed = 0;\n        for (BuildingRecord workshop : data.buildings()) {\n            if (workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP) continue;\n            List<FrontierWorkerEntity> assigned = findAssignedWorkers(level, data, workshop);\n            for (int i = 1; i < assigned.size(); i++) {\n                if (SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))) removed++;\n            }\n        }\n        return removed;\n    }\n\n'''
if "public static int reconcileLoadedAssignmentDuplicates" not in advanced:
    if advanced.count(advanced_anchor) != 1:
        raise SystemExit(f"advanced insertion anchor count={advanced.count(advanced_anchor)}")
    advanced = advanced.replace(advanced_anchor, advanced_method + advanced_anchor, 1)

audit_anchor = '''must(outpost, (\n    "findReachableExtractionTarget", "findReachableLogisticsDepositTarget",\n    "findLogisticsDepositTargetExcluding", "canReachStorageInteraction",\n    "moveToStorageInteraction", "createStoragePath", "path.canReach()"\n), "outpost storage reachability")\n'''
audit_block = '''must(outpost, (\n    "findReachableExtractionTarget", "findReachableLogisticsDepositTarget",\n    "findLogisticsDepositTargetExcluding", "canReachStorageInteraction",\n    "moveToStorageInteraction", "createStoragePath", "path.canReach()"\n), "outpost storage reachability")\nmust(worker, (\n    "SettlementOutpostLogisticsService.reconcileLoadedAssignmentDuplicates(level, data)",\n    "SettlementWorkshopService.reconcileLoadedAssignmentDuplicates(level, data)",\n    "SettlementAdvancedWorkshopService.reconcileLoadedAssignmentDuplicates(level, data)",\n    "static boolean removeDuplicateWorkerPreservingCargo"\n), "assignment duplicate orchestration")\nforbid(worker, (\n    "private static boolean removeDuplicateWorkerPreservingCargo",\n    "reconcileProductionDuplicates(server, level, data)"\n), "legacy duplicate orchestration")\nmust(outpost, (\n    "public static int reconcileLoadedAssignmentDuplicates",\n    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))"\n), "transport assignment duplicate cleanup")\nmust(workshop, (\n    "public static int reconcileLoadedAssignmentDuplicates",\n    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))"\n), "workshop assignment duplicate cleanup")\nmust(advanced_workshop, (\n    "public static int reconcileLoadedAssignmentDuplicates",\n    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))"\n), "advanced workshop assignment duplicate cleanup")\n'''
if "assignment duplicate orchestration" not in audit:
    if audit.count(audit_anchor) != 1:
        raise SystemExit(f"audit insertion anchor count={audit.count(audit_anchor)}")
    audit = audit.replace(audit_anchor, audit_block, 1)

WORKER.write_text(worker, encoding="utf-8")
OUTPOST.write_text(outpost, encoding="utf-8")
WORKSHOP.write_text(workshop, encoding="utf-8")
ADVANCED.write_text(advanced, encoding="utf-8")
AUDIT.write_text(audit, encoding="utf-8")

for label, src, required in (
    ("worker", worker, (
        "SettlementOutpostLogisticsService.reconcileLoadedAssignmentDuplicates(level, data)",
        "SettlementWorkshopService.reconcileLoadedAssignmentDuplicates(level, data)",
        "SettlementAdvancedWorkshopService.reconcileLoadedAssignmentDuplicates(level, data)",
        "static boolean removeDuplicateWorkerPreservingCargo",
    )),
    ("outpost", outpost, ("public static int reconcileLoadedAssignmentDuplicates",)),
    ("workshop", workshop, ("public static int reconcileLoadedAssignmentDuplicates",)),
    ("advanced", advanced, ("public static int reconcileLoadedAssignmentDuplicates",)),
    ("audit", audit, ("assignment duplicate orchestration",)),
):
    for token in required:
        if token not in src:
            raise SystemExit(f"{label} invariant missing: {token}")

if "private static boolean removeDuplicateWorkerPreservingCargo" in worker:
    raise SystemExit("worker duplicate helper still private")
if "reconcileProductionDuplicates(server, level, data)" in worker:
    raise SystemExit("legacy production duplicate orchestration remains")

print("ASSIGNMENT DUPLICATE LIFECYCLE PATCH PASS")
