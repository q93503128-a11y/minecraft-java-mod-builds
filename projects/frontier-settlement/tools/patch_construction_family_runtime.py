from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement"
ROAD = JAVA / "SettlementRoadService.java"
OUTPOST = JAVA / "SettlementOutpostService.java"
AUDIT = ROOT / "tools/test_alpha91_source.py"

road = ROAD.read_text(encoding="utf-8")
outpost = OUTPOST.read_text(encoding="utf-8")
audit = AUDIT.read_text(encoding="utf-8")

def replace_once(src, old, new, label):
    if new in src:
        return src
    count = src.count(old)
    if count != 1:
        raise SystemExit(f"{label} anchor count={count}")
    return src.replace(old, new, 1)

def harden(src, extraction_predicate, label):
    src = replace_once(src,
'''        if (builder.isNoAi()) builder.setNoAi(false);
        builder.setInvulnerable(true);
''',
'''        if (builder.isNoAi()) builder.setNoAi(false);
        builder.setInvulnerable(false);
''', f"{label} builder runtime")

    old_extract = f'''        BlockPos source = SettlementStorageService.findExtractionTarget(level, data, {extraction_predicate});
        if (source == null) return false;
        if (builder.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > STORAGE_INTERACTION_RANGE_SQR) {{
            builder.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, 0.9D);
            return false;
        }}
'''
    new_extract = f'''        BlockPos source = SettlementWorkerStorageNavigation.findReachableExtractionTarget(
                level, data, builder, {extraction_predicate}, STORAGE_INTERACTION_RANGE_SQR);
        if (source == null) {{
            builder.getNavigation().stop();
            return false;
        }}
        if (builder.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > STORAGE_INTERACTION_RANGE_SQR) {{
            SettlementWorkerStorageNavigation.moveToInteraction(
                    level, builder, source, 0.9D, STORAGE_INTERACTION_RANGE_SQR);
            return false;
        }}
'''
    src = replace_once(src, old_extract, new_extract, f"{label} extraction reachability")

    src = replace_once(src,
'''        BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);
        if (!level.hasChunkAt(target) || !SettlementStorageService.hasRoomAt(level, target, carried)) {
            builder.getNavigation().stop();
            return false;
        }
        if (builder.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                > STORAGE_INTERACTION_RANGE_SQR) {
            builder.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.9D);
            return false;
        }
''',
'''        BlockPos target = SettlementWorkerStorageNavigation.findReachableDepositTarget(
                level, data, builder, carried, STORAGE_INTERACTION_RANGE_SQR);
        if (target == null) {
            builder.getNavigation().stop();
            return false;
        }
        if (builder.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                > STORAGE_INTERACTION_RANGE_SQR) {
            SettlementWorkerStorageNavigation.moveToInteraction(
                    level, builder, target, 0.9D, STORAGE_INTERACTION_RANGE_SQR);
            return false;
        }
''', f"{label} deposit reachability/null safety")
    return src

road = harden(road, "SettlementInventory::isStone", "road")
outpost = harden(outpost, "predicate", "outpost")

old_vars = '''civil_retain = text(JAVA / "settlement/SettlementCivilRetainingService.java")
office = text(JAVA / "settlement/SettlementConstructionOfficeService.java")
'''
new_vars = '''civil_retain = text(JAVA / "settlement/SettlementCivilRetainingService.java")
road_build = text(JAVA / "settlement/SettlementRoadService.java")
outpost_build = text(JAVA / "settlement/SettlementOutpostService.java")
office = text(JAVA / "settlement/SettlementConstructionOfficeService.java")
'''
audit = replace_once(audit, old_vars, new_vars, "audit construction family vars")

old_anchor = '''must(worker, (
    "SettlementConstructionService.BUILDER_TAG",
), "civil builder death cargo preservation")
'''
new_block = old_anchor + '''for construction_src, construction_label in ((road_build, "road construction"), (outpost_build, "outpost construction")):
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
'''
audit = replace_once(audit, old_anchor, new_block, "audit construction family invariants")

ROAD.write_text(road, encoding="utf-8")
OUTPOST.write_text(outpost, encoding="utf-8")
AUDIT.write_text(audit, encoding="utf-8")

for src, label in ((road, "road"), (outpost, "outpost")):
    for token in (
        "builder.setInvulnerable(false);",
        "SettlementWorkerStorageNavigation.findReachableExtractionTarget(",
        "SettlementWorkerStorageNavigation.findReachableDepositTarget(",
        "SettlementWorkerStorageNavigation.moveToInteraction(",
        "if (target == null) {",
    ):
        if token not in src:
            raise SystemExit(f"{label} invariant missing: {token}")
    for token in (
        "builder.setInvulnerable(true);",
        "SettlementStorageService.findDepositTarget(level, data, carried);",
        "builder.getNavigation().moveTo(source.getX() + 0.5D",
        "builder.getNavigation().moveTo(target.getX() + 0.5D",
    ):
        if token in src:
            raise SystemExit(f"{label} legacy invariant remains: {token}")
if 'for construction_src, construction_label in ((road_build, "road construction"), (outpost_build, "outpost construction")):' not in audit:
    raise SystemExit("construction family persistent audit missing")

print("CONSTRUCTION FAMILY RUNTIME PATCH PASS")
