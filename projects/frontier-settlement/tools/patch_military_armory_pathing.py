from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
NAV = JAVA / "settlement/SettlementWorkerStorageNavigation.java"
ARMORY = JAVA / "settlement/SettlementMilitaryArmoryService.java"
AUDIT = ROOT / "tools/test_alpha91_source.py"

nav = NAV.read_text(encoding="utf-8")
armory = ARMORY.read_text(encoding="utf-8")
audit = AUDIT.read_text(encoding="utf-8")

def replace_once(src, old, new, label):
    if new in src:
        return src
    count = src.count(old)
    if count != 1:
        raise SystemExit(f"{label} anchor count={count}")
    return src.replace(old, new, 1)

nav = replace_once(nav,
    "import kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;\n",
    "import net.minecraft.world.entity.PathfinderMob;\n",
    "storage nav mob import")
nav = nav.replace("FrontierWorkerEntity worker", "PathfinderMob worker")
if "FrontierWorkerEntity" in nav:
    raise SystemExit("storage nav still worker-specific")

old_move = '''        if (distance > STORAGE_INTERACTION_RANGE_SQR) {
            soldier.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, ARMORY_WALK_SPEED);
            return true;
        }
'''
new_move = '''        if (distance > STORAGE_INTERACTION_RANGE_SQR) {
            return SettlementWorkerStorageNavigation.moveToInteraction(
                    level, soldier, source, ARMORY_WALK_SPEED, STORAGE_INTERACTION_RANGE_SQR);
        }
'''
count = armory.count(old_move)
if count != 2:
    raise SystemExit(f"armory direct move anchor count={count}")
armory = armory.replace(old_move, new_move)

armory = replace_once(armory,
'''            if (!(level.getBlockEntity(pos) instanceof Container container) || !containsExternalWeapon(container)) continue;
            double distance = soldier.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
''',
'''            if (!(level.getBlockEntity(pos) instanceof Container container) || !containsExternalWeapon(container)) continue;
            if (!SettlementWorkerStorageNavigation.canReachInteraction(
                    level, soldier, pos, STORAGE_INTERACTION_RANGE_SQR)) continue;
            double distance = soldier.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
''', "armory reachable source filter")

armory = replace_once(armory,
'''        if (!(level.getBlockEntity(source) instanceof Container container) || !containsExternalWeapon(container)) {
            return false;
        }
        double distance = soldier.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D);
''',
'''        if (!(level.getBlockEntity(source) instanceof Container container) || !containsExternalWeapon(container)) {
            return false;
        }
        if (!SettlementWorkerStorageNavigation.canReachInteraction(
                level, soldier, source, STORAGE_INTERACTION_RANGE_SQR)) {
            soldier.getNavigation().stop();
            return false;
        }
        double distance = soldier.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D);
''', "outpost armory reachable source guard")

old_vars = '''military = text(JAVA / "settlement/SettlementMilitaryOutpostService.java")
barracks = text(JAVA / "settlement/SettlementBarracksService.java")
'''
new_vars = '''military = text(JAVA / "settlement/SettlementMilitaryOutpostService.java")
armory = text(JAVA / "settlement/SettlementMilitaryArmoryService.java")
barracks = text(JAVA / "settlement/SettlementBarracksService.java")
'''
audit = replace_once(audit, old_vars, new_vars, "audit armory var")

old_anchor = '''forbid(military, (
    "duplicate.setNoAi(true);",
    "Historical duplicate bodies are contained rather than deleted"
), "legacy military duplicate containment")
'''
new_block = old_anchor + '''must(armory, (
    "SettlementWorkerStorageNavigation.canReachInteraction(",
    "SettlementWorkerStorageNavigation.moveToInteraction(",
    "soldier.getNavigation().stop();"
), "military armory reachable storage pathing")
forbid(armory, (
    "soldier.getNavigation().moveTo(source.getX() + 0.5D",
), "legacy military armory solid-container pathing")
'''
audit = replace_once(audit, old_anchor, new_block, "audit armory invariants")

audit = replace_once(audit,
'''must(storage_nav, (
    "findReachableExtractionTarget", "findReachableDepositTarget",
    "moveToInteraction", "createExactPath", "path.canReach()"
), "workshop storage navigation")
''',
'''must(storage_nav, (
    "PathfinderMob worker", "findReachableExtractionTarget", "findReachableDepositTarget",
    "moveToInteraction", "createExactPath", "path.canReach()"
), "shared worker/soldier storage navigation")
''', "audit generic storage navigation")

NAV.write_text(nav, encoding="utf-8")
ARMORY.write_text(armory, encoding="utf-8")
AUDIT.write_text(audit, encoding="utf-8")

for token in (
    "PathfinderMob worker",
    "static boolean canReachInteraction",
    "static boolean moveToInteraction",
):
    if token not in nav:
        raise SystemExit(f"generic nav invariant missing: {token}")
for token in (
    "SettlementWorkerStorageNavigation.canReachInteraction(",
    "SettlementWorkerStorageNavigation.moveToInteraction(",
):
    if token not in armory:
        raise SystemExit(f"armory path invariant missing: {token}")
if "soldier.getNavigation().moveTo(source.getX() + 0.5D" in armory:
    raise SystemExit("legacy armory solid-container move remains")
if "military armory reachable storage pathing" not in audit:
    raise SystemExit("armory persistent audit missing")

print("MILITARY ARMORY PATHING PATCH PASS")
