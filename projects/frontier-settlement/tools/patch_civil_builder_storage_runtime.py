from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement"
CIVIL = JAVA / "SettlementCivilWorkService.java"
FILL = JAVA / "SettlementCivilFillSupplyService.java"
RETAIN = JAVA / "SettlementCivilRetainingService.java"
AUDIT = ROOT / "tools/test_alpha91_source.py"

civil = CIVIL.read_text(encoding="utf-8")
fill = FILL.read_text(encoding="utf-8")
retain = RETAIN.read_text(encoding="utf-8")
audit = AUDIT.read_text(encoding="utf-8")

def replace_once(src, old, new, label):
    if new in src:
        return src
    count = src.count(old)
    if count != 1:
        raise SystemExit(f"{label} anchor count={count}")
    return src.replace(old, new, 1)

civil = replace_once(civil,
'''        if (builder != null) {
            builder.setInvulnerable(true);
            builder.setCustomName(Component.literal("건설 주민 · 토목"));
        }
''',
'''        if (builder != null) {
            builder.setNoAi(false);
            builder.setInvulnerable(false);
            builder.setCustomName(Component.literal("건설 주민 · 토목"));
        }
''', "civil start runtime")

civil = replace_once(civil,
'''        if (builder.isNoAi()) builder.setNoAi(false);
        builder.setInvulnerable(true);
        builder.setCustomName(Component.literal("건설 주민 · 토목"));
''',
'''        if (builder.isNoAi()) builder.setNoAi(false);
        builder.setInvulnerable(false);
        builder.setCustomName(Component.literal("건설 주민 · 토목"));
''', "civil tick runtime")

fill = replace_once(fill,
'''        BlockPos source = SettlementStorageService.findExtractionTarget(level, data,
                SettlementCivilFillSupplyService::isFillStack);
        if (source == null) return false;
        if (builder.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > STORAGE_REACHED_SQR) {
            builder.getNavigation().moveTo(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D, 0.86D);
            return false;
        }
''',
'''        BlockPos source = SettlementWorkerStorageNavigation.findReachableExtractionTarget(
                level, data, builder, SettlementCivilFillSupplyService::isFillStack, STORAGE_REACHED_SQR);
        if (source == null) {
            builder.getNavigation().stop();
            return false;
        }
        if (builder.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > STORAGE_REACHED_SQR) {
            SettlementWorkerStorageNavigation.moveToInteraction(level, builder, source, 0.86D, STORAGE_REACHED_SQR);
            return false;
        }
''', "civil fill extraction reachability")

fill = replace_once(fill,
'''        BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);
        if (!level.hasChunkAt(target) || !SettlementStorageService.hasRoomAt(level, target, carried)) return false;
        if (builder.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                > STORAGE_REACHED_SQR) {
            builder.getNavigation().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 0.86D);
            return false;
        }
''',
'''        BlockPos target = SettlementWorkerStorageNavigation.findReachableDepositTarget(
                level, data, builder, carried, STORAGE_REACHED_SQR);
        if (target == null) {
            builder.getNavigation().stop();
            return false;
        }
        if (builder.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                > STORAGE_REACHED_SQR) {
            SettlementWorkerStorageNavigation.moveToInteraction(level, builder, target, 0.86D, STORAGE_REACHED_SQR);
            return false;
        }
''', "civil fill deposit reachability")

retain = replace_once(retain,
'''        BlockPos source = SettlementStorageService.findExtractionTarget(level, data,
                SettlementCivilRetainingService::isRetainingStack);
        if (source == null) return false;
        if (builder.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > STORAGE_REACHED_SQR) {
            builder.getNavigation().moveTo(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D, 0.86D);
            return false;
        }
''',
'''        BlockPos source = SettlementWorkerStorageNavigation.findReachableExtractionTarget(
                level, data, builder, SettlementCivilRetainingService::isRetainingStack, STORAGE_REACHED_SQR);
        if (source == null) {
            builder.getNavigation().stop();
            return false;
        }
        if (builder.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > STORAGE_REACHED_SQR) {
            SettlementWorkerStorageNavigation.moveToInteraction(level, builder, source, 0.86D, STORAGE_REACHED_SQR);
            return false;
        }
''', "civil retaining extraction reachability")

old_vars = '''outpost_production = text(JAVA / "settlement/SettlementOutpostProductionService.java")
office = text(JAVA / "settlement/SettlementConstructionOfficeService.java")
'''
new_vars = '''outpost_production = text(JAVA / "settlement/SettlementOutpostProductionService.java")
civil = text(JAVA / "settlement/SettlementCivilWorkService.java")
civil_fill = text(JAVA / "settlement/SettlementCivilFillSupplyService.java")
civil_retain = text(JAVA / "settlement/SettlementCivilRetainingService.java")
office = text(JAVA / "settlement/SettlementConstructionOfficeService.java")
'''
audit = replace_once(audit, old_vars, new_vars, "audit civil vars")

old_anchor = '''must(worker, (
    "SettlementOutpostProductionService.PRODUCTION_WORKER_TAG",
), "outpost production death cargo preservation")
'''
new_block = '''must(worker, (
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
'''
audit = replace_once(audit, old_anchor, new_block, "audit civil invariants")

CIVIL.write_text(civil, encoding="utf-8")
FILL.write_text(fill, encoding="utf-8")
RETAIN.write_text(retain, encoding="utf-8")
AUDIT.write_text(audit, encoding="utf-8")

if "builder.setInvulnerable(true);" in civil:
    raise SystemExit("civil builder invulnerability remains")
for token in (
    "SettlementWorkerStorageNavigation.findReachableExtractionTarget(",
    "SettlementWorkerStorageNavigation.findReachableDepositTarget(",
    "if (target == null) {",
):
    if token not in fill:
        raise SystemExit(f"civil fill invariant missing: {token}")
for token in (
    "SettlementWorkerStorageNavigation.findReachableExtractionTarget(",
    "SettlementWorkerStorageNavigation.moveToInteraction(level, builder, source, 0.86D, STORAGE_REACHED_SQR)",
):
    if token not in retain:
        raise SystemExit(f"civil retaining invariant missing: {token}")
if "civil fill storage reachability/null safety" not in audit:
    raise SystemExit("civil persistent audit missing")

print("CIVIL BUILDER STORAGE RUNTIME PATCH PASS")
