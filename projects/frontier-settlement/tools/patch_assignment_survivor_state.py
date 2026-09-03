from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement"
OUTPOST = JAVA / "SettlementOutpostLogisticsService.java"
WORKSHOP = JAVA / "SettlementWorkshopService.java"
ADVANCED = JAVA / "SettlementAdvancedWorkshopService.java"
AUDIT = ROOT / "tools/test_alpha91_source.py"

outpost = OUTPOST.read_text(encoding="utf-8")
workshop = WORKSHOP.read_text(encoding="utf-8")
advanced = ADVANCED.read_text(encoding="utf-8")
audit = AUDIT.read_text(encoding="utf-8")

old_loop_outpost = '''            List<FrontierWorkerEntity> assigned = findAssignedWorkers(level, data, outpost);\n            for (int i = 1; i < assigned.size(); i++) {\n'''
new_loop_outpost = '''            List<FrontierWorkerEntity> assigned = findAssignedWorkers(level, data, outpost);\n            if (!assigned.isEmpty()) {\n                FrontierWorkerEntity survivor = assigned.getFirst();\n                survivor.setNoAi(false);\n                survivor.setInvulnerable(false);\n            }\n            for (int i = 1; i < assigned.size(); i++) {\n'''
if new_loop_outpost not in outpost:
    if outpost.count(old_loop_outpost) != 1:
        raise SystemExit(f"outpost survivor anchor count={outpost.count(old_loop_outpost)}")
    outpost = outpost.replace(old_loop_outpost, new_loop_outpost, 1)

old_loop_workshop = '''            List<FrontierWorkerEntity> assigned = findAssignedWorkers(level, data, workshop);\n            for (int i = 1; i < assigned.size(); i++) {\n'''
new_loop_workshop = '''            List<FrontierWorkerEntity> assigned = findAssignedWorkers(level, data, workshop);\n            if (!assigned.isEmpty()) {\n                FrontierWorkerEntity survivor = assigned.getFirst();\n                survivor.setNoAi(false);\n                survivor.setInvulnerable(false);\n            }\n            for (int i = 1; i < assigned.size(); i++) {\n'''
if new_loop_workshop not in workshop:
    if workshop.count(old_loop_workshop) != 1:
        raise SystemExit(f"workshop survivor anchor count={workshop.count(old_loop_workshop)}")
    workshop = workshop.replace(old_loop_workshop, new_loop_workshop, 1)

if new_loop_workshop not in advanced:
    if advanced.count(old_loop_workshop) != 1:
        raise SystemExit(f"advanced survivor anchor count={advanced.count(old_loop_workshop)}")
    advanced = advanced.replace(old_loop_workshop, new_loop_workshop, 1)

old_advanced_tick = '''        ServerLevel level = server.overworld();\n        boolean rest = SettlementResidentRoutineService.isRestTime(level);\n        for (BuildingRecord workshop : data.buildings()) {\n'''
new_advanced_tick = '''        ServerLevel level = server.overworld();\n        for (BuildingRecord workshop : data.buildings()) {\n'''
if new_advanced_tick not in advanced:
    if advanced.count(old_advanced_tick) != 1:
        raise SystemExit(f"advanced rest flag anchor count={advanced.count(old_advanced_tick)}")
    advanced = advanced.replace(old_advanced_tick, new_advanced_tick, 1)

old_rest_block = '''            FrontierWorkerEntity worker = findAssignedWorker(level, data, workshop);\n            if (worker == null) continue;\n            ItemStack carried = worker.getMainHandItem();\n            if (rest) {\n                if (!carried.isEmpty()) returnCarriedItem(level, data, worker, carried);\n                else moveOrStop(worker, home, 0.68D);\n                continue;\n            }\n            runService(server, level, data, workshop, cratePos, crate, worker);\n'''
new_rest_block = '''            FrontierWorkerEntity worker = findAssignedWorker(level, data, workshop);\n            if (worker == null) continue;\n            runService(server, level, data, workshop, cratePos, crate, worker);\n'''
if new_rest_block not in advanced:
    if advanced.count(old_rest_block) != 1:
        raise SystemExit(f"advanced rest block anchor count={advanced.count(old_rest_block)}")
    advanced = advanced.replace(old_rest_block, new_rest_block, 1)

audit_anchor = '''must(outpost, (\n    "public static int reconcileLoadedAssignmentDuplicates",\n    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))"\n), "transport assignment duplicate cleanup")\n'''
audit_block = '''must(outpost, (\n    "public static int reconcileLoadedAssignmentDuplicates",\n    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))"\n), "transport assignment duplicate cleanup")\nmust(outpost, ("survivor.setNoAi(false);", "survivor.setInvulnerable(false);"), "transport survivor normalization")\nmust(workshop, ("survivor.setNoAi(false);", "survivor.setInvulnerable(false);"), "workshop survivor normalization")\nmust(advanced_workshop, ("survivor.setNoAi(false);", "survivor.setInvulnerable(false);"), "advanced survivor normalization")\nforbid(advanced_workshop, (\n    "SettlementResidentRoutineService.isRestTime(level)",\n    "if (rest) {"\n), "orphan advanced workshop night pause")\n'''
if "transport survivor normalization" not in audit:
    if audit.count(audit_anchor) != 1:
        raise SystemExit(f"audit survivor anchor count={audit.count(audit_anchor)}")
    audit = audit.replace(audit_anchor, audit_block, 1)

OUTPOST.write_text(outpost, encoding="utf-8")
WORKSHOP.write_text(workshop, encoding="utf-8")
ADVANCED.write_text(advanced, encoding="utf-8")
AUDIT.write_text(audit, encoding="utf-8")

for label, src in (("outpost", outpost), ("workshop", workshop), ("advanced", advanced)):
    if "survivor.setNoAi(false);" not in src or "survivor.setInvulnerable(false);" not in src:
        raise SystemExit(f"{label} survivor normalization missing")
if "SettlementResidentRoutineService.isRestTime(level)" in advanced or "if (rest) {" in advanced:
    raise SystemExit("advanced workshop orphan night pause remains")
if "transport survivor normalization" not in audit:
    raise SystemExit("survivor audit missing")

print("ASSIGNMENT SURVIVOR STATE PATCH PASS")
