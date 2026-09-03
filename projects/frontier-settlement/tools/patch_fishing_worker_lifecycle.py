from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement"
FISHING = JAVA / "SettlementFishingOutpostService.java"
AUDIT = ROOT / "tools/test_alpha91_source.py"

fishing = FISHING.read_text(encoding="utf-8")
audit = AUDIT.read_text(encoding="utf-8")

old_find = '''    private static FrontierWorkerEntity findAssignedWorker(ServerLevel level, OutpostRecord outpost) {\n        List<FrontierWorkerEntity> workers = findAssignedWorkers(level, outpost);\n        if (workers.isEmpty()) return null;\n        FrontierWorkerEntity active = workers.getFirst();\n        active.setNoAi(false);\n        for (int i = 1; i < workers.size(); i++) {\n            FrontierWorkerEntity duplicate = workers.get(i);\n            duplicate.getNavigation().stop();\n            duplicate.setNoAi(true);\n            duplicate.setInvulnerable(true);\n        }\n        return active;\n    }\n'''
new_find = '''    private static FrontierWorkerEntity findAssignedWorker(ServerLevel level, OutpostRecord outpost) {\n        List<FrontierWorkerEntity> workers = findAssignedWorkers(level, outpost);\n        if (workers.isEmpty()) return null;\n        FrontierWorkerEntity active = workers.getFirst();\n        active.setNoAi(false);\n        active.setInvulnerable(false);\n        // One fishing overlay owns one physical worker. More than one loaded body with the exact\n        // assignment tag is conclusive duplicate evidence; preserve any real caught fish in MAINHAND\n        // and discard the excess body instead of leaving immortal NoAI statues in old saves.\n        for (int i = 1; i < workers.size(); i++) {\n            SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, workers.get(i));\n        }\n        return active;\n    }\n'''
if new_find not in fishing:
    if fishing.count(old_find) != 1:
        raise SystemExit(f"fishing duplicate anchor count={fishing.count(old_find)}")
    fishing = fishing.replace(old_find, new_find, 1)

old_deliver = '''        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D) > WORK_RANGE_SQR) {\n            worker.getNavigation().moveTo(stock.getX() + 0.5D, stock.getY(), stock.getZ() + 0.5D, 0.82D);\n            return;\n        }\n'''
new_deliver = '''        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D) > WORK_RANGE_SQR) {\n            SettlementWorkerStorageNavigation.moveToInteraction(level, worker, stock, 0.82D, WORK_RANGE_SQR);\n            return;\n        }\n'''
if new_deliver not in fishing:
    if fishing.count(old_deliver) != 1:
        raise SystemExit(f"fishing storage path anchor count={fishing.count(old_deliver)}")
    fishing = fishing.replace(old_deliver, new_deliver, 1)

old_audit_vars = '''outpost = text(JAVA / "settlement/SettlementOutpostLogisticsService.java")\noffice = text(JAVA / "settlement/SettlementConstructionOfficeService.java")\n'''
new_audit_vars = '''outpost = text(JAVA / "settlement/SettlementOutpostLogisticsService.java")\nfishing = text(JAVA / "settlement/SettlementFishingOutpostService.java")\noffice = text(JAVA / "settlement/SettlementConstructionOfficeService.java")\n'''
if new_audit_vars not in audit:
    if audit.count(old_audit_vars) != 1:
        raise SystemExit(f"audit fishing var anchor count={audit.count(old_audit_vars)}")
    audit = audit.replace(old_audit_vars, new_audit_vars, 1)

audit_anchor = '''forbid(outpost, (\n    "BlockPos target = SettlementStorageService.findLogisticsDepositTarget(level, data, carried);",\n), "outpost forced storage fallback")\n'''
audit_block = '''forbid(outpost, (\n    "BlockPos target = SettlementStorageService.findLogisticsDepositTarget(level, data, carried);",\n), "outpost forced storage fallback")\nmust(fishing, (\n    "active.setInvulnerable(false);",\n    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, workers.get(i))",\n    "SettlementWorkerStorageNavigation.moveToInteraction(level, worker, stock, 0.82D, WORK_RANGE_SQR)"\n), "fishing worker lifecycle/storage reachability")\nforbid(fishing, (\n    "duplicate.setNoAi(true);",\n    "duplicate.setInvulnerable(true);",\n    "worker.getNavigation().moveTo(stock.getX() + 0.5D"\n), "legacy fishing duplicate/storage pathing")\n'''
if "fishing worker lifecycle/storage reachability" not in audit:
    if audit.count(audit_anchor) != 1:
        raise SystemExit(f"audit fishing block anchor count={audit.count(audit_anchor)}")
    audit = audit.replace(audit_anchor, audit_block, 1)

FISHING.write_text(fishing, encoding="utf-8")
AUDIT.write_text(audit, encoding="utf-8")

for token in (
    "active.setInvulnerable(false);",
    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, workers.get(i))",
    "SettlementWorkerStorageNavigation.moveToInteraction(level, worker, stock, 0.82D, WORK_RANGE_SQR)",
):
    if token not in fishing:
        raise SystemExit(f"fishing invariant missing: {token}")
for forbidden in ("duplicate.setNoAi(true);", "duplicate.setInvulnerable(true);", "worker.getNavigation().moveTo(stock.getX() + 0.5D"):
    if forbidden in fishing:
        raise SystemExit(f"legacy fishing invariant remains: {forbidden}")
if "fishing worker lifecycle/storage reachability" not in audit:
    raise SystemExit("fishing persistent audit missing")

print("FISHING WORKER LIFECYCLE PATCH PASS")
