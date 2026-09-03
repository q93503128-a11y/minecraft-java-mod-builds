from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement"
OFFICE = JAVA / "SettlementConstructionOfficeService.java"
WORKER = JAVA / "SettlementWorkerService.java"
AUDIT = ROOT / "tools/test_alpha91_source.py"

office = OFFICE.read_text(encoding="utf-8")
worker = WORKER.read_text(encoding="utf-8")
audit = AUDIT.read_text(encoding="utf-8")

old_tick_head = '''        ServerLevel level = server.overworld();\n        boolean activeProject = data.construction().active();\n        boolean rest = SettlementResidentRoutineService.isRestTime(level);\n\n        for (BuildingRecord office : offices(data)) {\n'''
new_tick_head = '''        ServerLevel level = server.overworld();\n        boolean activeProject = data.construction().active();\n\n        for (BuildingRecord office : offices(data)) {\n'''
if new_tick_head not in office:
    if office.count(old_tick_head) != 1:
        raise SystemExit(f"office tick head anchor count={office.count(old_tick_head)}")
    office = office.replace(old_tick_head, new_tick_head, 1)

old_tick_state = '''            FrontierWorkerEntity runner = ensureSingleRunner(level, data, office, home);\n            if (runner == null) continue;\n            runner.setInvulnerable(true);\n\n            if (!runner.getMainHandItem().isEmpty()) {\n                if (deliverCarried(level, data, office, runner)) continue;\n            }\n            if (rest || !activeProject) {\n                moveOrStop(runner, home, 0.72D);\n                continue;\n            }\n'''
new_tick_state = '''            FrontierWorkerEntity runner = ensureSingleRunner(level, data, office, home);\n            if (runner == null) continue;\n            runner.setNoAi(false);\n            runner.setInvulnerable(false);\n\n            if (!runner.getMainHandItem().isEmpty()) {\n                if (deliverCarried(level, data, office, runner)) continue;\n            }\n            if (!activeProject) {\n                moveOrStop(runner, home, 0.72D);\n                continue;\n            }\n'''
if new_tick_state not in office:
    if office.count(old_tick_state) != 1:
        raise SystemExit(f"office tick state anchor count={office.count(old_tick_state)}")
    office = office.replace(old_tick_state, new_tick_state, 1)

old_keep = '''            FrontierWorkerEntity keep = existing.getFirst();\n            keep.setNoAi(false);\n            keep.setInvulnerable(true);\n'''
new_keep = '''            FrontierWorkerEntity keep = existing.getFirst();\n            keep.setNoAi(false);\n            keep.setInvulnerable(false);\n'''
if new_keep not in office:
    if office.count(old_keep) != 1:
        raise SystemExit(f"office survivor anchor count={office.count(old_keep)}")
    office = office.replace(old_keep, new_keep, 1)

old_spawn = '''        runner.setCustomNameVisible(true);\n        runner.setPersistenceRequired();\n        runner.setInvulnerable(true);\n        runner.addTag(SUPPLY_RUNNER_TAG);\n'''
new_spawn = '''        runner.setCustomNameVisible(true);\n        runner.setPersistenceRequired();\n        runner.setNoAi(false);\n        runner.setInvulnerable(false);\n        runner.addTag(SUPPLY_RUNNER_TAG);\n'''
if new_spawn not in office:
    if office.count(old_spawn) != 1:
        raise SystemExit(f"office spawn anchor count={office.count(old_spawn)}")
    office = office.replace(old_spawn, new_spawn, 1)

old_managed = '''                || worker.entityTags().contains(SettlementFishingOutpostService.FISHING_WORKER_TAG)\n                || worker.entityTags().contains(SettlementWorkshopService.WORKSHOP_WORKER_TAG)\n'''
new_managed = '''                || worker.entityTags().contains(SettlementFishingOutpostService.FISHING_WORKER_TAG)\n                || worker.entityTags().contains(SettlementConstructionOfficeService.SUPPLY_RUNNER_TAG)\n                || worker.entityTags().contains(SettlementWorkshopService.WORKSHOP_WORKER_TAG)\n'''
if new_managed not in worker:
    if worker.count(old_managed) != 1:
        raise SystemExit(f"managed cargo anchor count={worker.count(old_managed)}")
    worker = worker.replace(old_managed, new_managed, 1)

audit_anchor = '''must(office, (\n    "removeDuplicateRunnerPreservingCargo", "canReachInteraction", "moveToInteraction",\n    "createInteractionPath", "path.canReach()", "canReachInteraction(level, runner, pos)"\n), "construction office runner hardening")\n'''
audit_block = '''must(office, (\n    "removeDuplicateRunnerPreservingCargo", "canReachInteraction", "moveToInteraction",\n    "createInteractionPath", "path.canReach()", "canReachInteraction(level, runner, pos)"\n), "construction office runner hardening")\nmust(office, (\n    "runner.setNoAi(false);", "runner.setInvulnerable(false);",\n    "keep.setInvulnerable(false);"\n), "construction office runner runtime state")\nforbid(office, (\n    "SettlementResidentRoutineService.isRestTime(level)",\n    "runner.setInvulnerable(true);",\n    "keep.setInvulnerable(true);",\n    "if (rest || !activeProject)"\n), "construction office orphan rest/invulnerability")\nmust(worker, (\n    "SettlementConstructionOfficeService.SUPPLY_RUNNER_TAG",\n), "construction office death cargo preservation")\n'''
if "construction office runner runtime state" not in audit:
    if audit.count(audit_anchor) != 1:
        raise SystemExit(f"audit office runtime anchor count={audit.count(audit_anchor)}")
    audit = audit.replace(audit_anchor, audit_block, 1)

OFFICE.write_text(office, encoding="utf-8")
WORKER.write_text(worker, encoding="utf-8")
AUDIT.write_text(audit, encoding="utf-8")

for token in ("runner.setInvulnerable(false);", "keep.setInvulnerable(false);"):
    if token not in office:
        raise SystemExit(f"office runtime invariant missing: {token}")
for forbidden in ("SettlementResidentRoutineService.isRestTime(level)", "runner.setInvulnerable(true);", "keep.setInvulnerable(true);", "if (rest || !activeProject)"):
    if forbidden in office:
        raise SystemExit(f"office legacy runtime invariant remains: {forbidden}")
if "SettlementConstructionOfficeService.SUPPLY_RUNNER_TAG" not in worker:
    raise SystemExit("supply runner managed cargo tag missing")
if "construction office runner runtime state" not in audit:
    raise SystemExit("office runtime audit missing")

print("CONSTRUCTION RUNNER RUNTIME STATE PATCH PASS")
