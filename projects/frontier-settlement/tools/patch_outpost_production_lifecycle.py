from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement"
PRODUCTION = JAVA / "SettlementOutpostProductionService.java"
AUDIT = ROOT / "tools/test_alpha91_source.py"

production = PRODUCTION.read_text(encoding="utf-8")
audit = AUDIT.read_text(encoding="utf-8")

def replace_once(src, old, new, label):
    if new in src:
        return src
    count = src.count(old)
    if count != 1:
        raise SystemExit(f"{label} anchor count={count}")
    return src.replace(old, new, 1)

old_ensure = '''    private static FrontierWorkerEntity ensureWorker(ServerLevel level, OutpostRecord outpost) {
        if (!outpostLoaded(level, outpost)) return null;
        List<FrontierWorkerEntity> assigned = findAssignedWorkers(level, outpost);
        if (!assigned.isEmpty()) return assigned.getFirst();

        // Missing is authority. Do not migrate or spawn from a partial entity view.
        if (!assignmentEvidenceLoaded(level, outpost)) return null;

        String assignmentTag = productionTag(outpost.id());
        String name = workerName(outpost);
        List<FrontierWorkerEntity> legacy = level.getEntitiesOfClass(FrontierWorkerEntity.class, assignmentBounds(outpost),
                villager -> villager.getCustomName() != null && name.equals(villager.getCustomName().getString()));
        legacy.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        if (!legacy.isEmpty()) {
            FrontierWorkerEntity worker = legacy.getFirst();
            worker.addTag(PRODUCTION_WORKER_TAG);
            worker.addTag(assignmentTag);
            return worker;
        }

        FrontierWorkerEntity worker = new FrontierWorkerEntity(FrontierContent.FRONTIER_WORKER.get(), level);
        BlockPos spawn = outpost.center().above();
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(name));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.addTag(PRODUCTION_WORKER_TAG);
        worker.addTag(assignmentTag);
        if (!level.addFreshEntity(worker)) return null;
        return worker;
    }
'''
new_ensure = '''    private static FrontierWorkerEntity ensureWorker(ServerLevel level, OutpostRecord outpost) {
        if (!outpostLoaded(level, outpost)) return null;
        String assignmentTag = productionTag(outpost.id());
        String name = workerName(outpost);
        List<FrontierWorkerEntity> assigned = findAssignedWorkers(level, outpost);
        List<FrontierWorkerEntity> legacy = findLegacyWorkers(level, outpost, name, assignmentTag);

        if (!assigned.isEmpty()) {
            FrontierWorkerEntity active = assigned.getFirst();
            active.setNoAi(false);
            active.setInvulnerable(false);
            // One specialized outpost owns one local production authority. More than one loaded body
            // with the same assignment is conclusive duplicate evidence even when the wider envelope
            // is not fully loaded. Preserve physical cargo, then discard every excess body.
            for (int i = 1; i < assigned.size(); i++) {
                SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i));
            }
            // Pre-tag migration remnants use a unique name containing this outpost id. Once a tagged
            // authority is visible, any loaded same-name unassigned body is also definitively excess.
            for (FrontierWorkerEntity duplicate : legacy) {
                SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, duplicate);
            }
            return active;
        }

        // Missing is authority. Do not migrate or spawn from a partial entity view.
        if (!assignmentEvidenceLoaded(level, outpost)) return null;

        if (!legacy.isEmpty()) {
            FrontierWorkerEntity active = legacy.getFirst();
            active.setNoAi(false);
            active.setInvulnerable(false);
            active.addTag(PRODUCTION_WORKER_TAG);
            active.addTag(assignmentTag);
            for (int i = 1; i < legacy.size(); i++) {
                SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, legacy.get(i));
            }
            return active;
        }

        FrontierWorkerEntity worker = new FrontierWorkerEntity(FrontierContent.FRONTIER_WORKER.get(), level);
        BlockPos spawn = outpost.center().above();
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(name));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.setInvulnerable(false);
        worker.addTag(PRODUCTION_WORKER_TAG);
        worker.addTag(assignmentTag);
        if (!level.addFreshEntity(worker)) return null;
        return worker;
    }

    private static List<FrontierWorkerEntity> findLegacyWorkers(ServerLevel level, OutpostRecord outpost,
                                                                 String name, String assignmentTag) {
        List<FrontierWorkerEntity> legacy = level.getEntitiesOfClass(FrontierWorkerEntity.class, assignmentBounds(outpost),
                villager -> villager.getCustomName() != null
                        && name.equals(villager.getCustomName().getString())
                        && !villager.entityTags().contains(assignmentTag));
        legacy.sort(Comparator
                .comparingInt((FrontierWorkerEntity worker) -> worker.getMainHandItem().isEmpty() ? 1 : 0)
                .thenComparing(worker -> worker.getUUID().toString()));
        return legacy;
    }
'''
production = replace_once(production, old_ensure, new_ensure, "outpost production ensureWorker")

old_assigned_sort = '''        assigned.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return assigned;
'''
new_assigned_sort = '''        assigned.sort(Comparator
                .comparingInt((FrontierWorkerEntity worker) -> worker.getMainHandItem().isEmpty() ? 1 : 0)
                .thenComparing(worker -> worker.getUUID().toString()));
        return assigned;
'''
production = replace_once(production, old_assigned_sort, new_assigned_sort, "outpost production assigned sort")

old_deliver = '''        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D) > 9.0D) {
            move(worker, stock, 0.82D);
            return;
        }
'''
new_deliver = '''        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D) > 9.0D) {
            SettlementWorkerStorageNavigation.moveToInteraction(level, worker, stock, 0.82D, 9.0D);
            return;
        }
'''
production = replace_once(production, old_deliver, new_deliver, "outpost production stock path")

old_lumber_move = '''        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 8.0D) {
            move(worker, target, 0.8D);
            return;
        }
'''
new_lumber_move = '''        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 8.0D) {
            SettlementWorkerStorageNavigation.moveToInteraction(level, worker, target, 0.8D, 8.0D);
            return;
        }
'''
production = replace_once(production, old_lumber_move, new_lumber_move, "outpost lumber path")

old_quarry_move = '''        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 9.0D) {
            move(worker, target, 0.78D);
            return;
        }
'''
new_quarry_move = '''        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 9.0D) {
            SettlementWorkerStorageNavigation.moveToInteraction(level, worker, target, 0.78D, 9.0D);
            return;
        }
'''
production = replace_once(production, old_quarry_move, new_quarry_move, "outpost quarry path")

old_vars = '''market = text(JAVA / "settlement/SettlementMarketService.java")
office = text(JAVA / "settlement/SettlementConstructionOfficeService.java")
'''
new_vars = '''market = text(JAVA / "settlement/SettlementMarketService.java")
outpost_production = text(JAVA / "settlement/SettlementOutpostProductionService.java")
office = text(JAVA / "settlement/SettlementConstructionOfficeService.java")
'''
audit = replace_once(audit, old_vars, new_vars, "audit outpost production var")

old_audit_anchor = '''forbid(market, (
    "duplicate.setNoAi(true);",
    "duplicate.setInvulnerable(true);",
    "trader.getNavigation().moveTo(crate.getX() + 0.5D"
), "legacy market lifecycle/pathing")
'''
new_audit_block = '''forbid(market, (
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
'''
audit = replace_once(audit, old_audit_anchor, new_audit_block, "audit outpost production invariants")

PRODUCTION.write_text(production, encoding="utf-8")
AUDIT.write_text(audit, encoding="utf-8")

for token in (
    "active.setInvulnerable(false);",
    "findLegacyWorkers(level, outpost, name, assignmentTag)",
    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))",
    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, duplicate)",
    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, legacy.get(i))",
    "if (!assignmentEvidenceLoaded(level, outpost)) return null;",
    "level, worker, stock, 0.82D, 9.0D",
    "level, worker, target, 0.8D, 8.0D",
    "level, worker, target, 0.78D, 9.0D",
):
    if token not in production:
        raise SystemExit(f"outpost production invariant missing: {token}")
for forbidden in (
    "if (!assigned.isEmpty()) return assigned.getFirst();",
    "move(worker, stock, 0.82D);",
    "move(worker, target, 0.8D);",
    "move(worker, target, 0.78D);",
):
    if forbidden in production:
        raise SystemExit(f"legacy outpost production invariant remains: {forbidden}")
if "outpost production lifecycle/pathing" not in audit:
    raise SystemExit("outpost production persistent audit missing")

print("OUTPOST PRODUCTION LIFECYCLE PATCH PASS")
