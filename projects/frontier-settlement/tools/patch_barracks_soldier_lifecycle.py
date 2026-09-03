from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
BARRACKS = JAVA / "settlement/SettlementBarracksService.java"
AUDIT = ROOT / "tools/test_alpha91_source.py"

barracks = BARRACKS.read_text(encoding="utf-8")
audit = AUDIT.read_text(encoding="utf-8")

old_import = '''import net.minecraft.server.level.ServerLevel;\nimport net.minecraft.world.entity.animal.golem.IronGolem;\n'''
new_import = '''import net.minecraft.server.level.ServerLevel;\nimport net.minecraft.world.entity.EquipmentSlot;\nimport net.minecraft.world.entity.animal.golem.IronGolem;\n'''
if new_import not in barracks:
    if barracks.count(old_import) != 1:
        raise SystemExit(f"barracks equipment import anchor count={barracks.count(old_import)}")
    barracks = barracks.replace(old_import, new_import, 1)

old_find = '''    private static FrontierSoldierEntity findSoldier(ServerLevel level, SettlementData data, BuildingRecord barracks, int slot) {\n        String assignment = barracksAssignment(barracks);\n        String slotTag = SOLDIER_SLOT_PREFIX + slot;\n        AABB search = soldierRouteBounds(data, barracks);\n        List<FrontierSoldierEntity> soldiers = level.getEntitiesOfClass(FrontierSoldierEntity.class, search,\n                soldier -> soldier.entityTags().contains(SOLDIER_TAG) && soldier.entityTags().contains(assignment) && soldier.entityTags().contains(slotTag));\n        soldiers.sort(Comparator.comparing(soldier -> soldier.getUUID().toString()));\n        if (!soldiers.isEmpty()) {\n            FrontierSoldierEntity active = soldiers.getFirst();\n            active.setNoAi(false);\n            for (int i = 1; i < soldiers.size(); i++) {\n                FrontierSoldierEntity duplicate = soldiers.get(i);\n                if (duplicate.getTarget() != null) duplicate.setTarget(null);\n                duplicate.getNavigation().stop();\n                duplicate.setNoAi(true);\n            }\n            return active;\n        }\n\n        // Missing/migration is authority: a partial route view never converts or recruits.\n        if (!soldierAssignmentEvidenceLoaded(level, data, barracks)) return null;\n        List<IronGolem> legacy = level.getEntitiesOfClass(IronGolem.class, search,\n                soldier -> !(soldier instanceof FrontierSoldierEntity)\n                        && soldier.entityTags().contains(SOLDIER_TAG)\n                        && soldier.entityTags().contains(assignment)\n                        && soldier.entityTags().contains(slotTag));\n        legacy.sort(Comparator.comparing(soldier -> soldier.getUUID().toString()));\n        return legacy.isEmpty() ? null : migrateLegacySoldier(level, legacy.getFirst());\n    }\n'''
new_find = '''    private static FrontierSoldierEntity findSoldier(ServerLevel level, SettlementData data, BuildingRecord barracks, int slot) {\n        String assignment = barracksAssignment(barracks);\n        String slotTag = SOLDIER_SLOT_PREFIX + slot;\n        AABB search = soldierRouteBounds(data, barracks);\n        List<FrontierSoldierEntity> soldiers = level.getEntitiesOfClass(FrontierSoldierEntity.class, search,\n                soldier -> soldier.entityTags().contains(SOLDIER_TAG) && soldier.entityTags().contains(assignment) && soldier.entityTags().contains(slotTag));\n        soldiers.sort(Comparator\n                .comparingInt((FrontierSoldierEntity soldier) ->\n                        SettlementExternalContentService.isExternalWeapon(soldier.getMainHandItem()) ? 0 : 1)\n                .thenComparing(soldier -> soldier.getUUID().toString()));\n\n        // Loaded legacy bodies are still useful duplicate evidence when a new authoritative body exists.\n        // If no new body exists, the full route evidence gate below remains mandatory before migration.\n        List<IronGolem> legacy = level.getEntitiesOfClass(IronGolem.class, search,\n                soldier -> !(soldier instanceof FrontierSoldierEntity)\n                        && soldier.entityTags().contains(SOLDIER_TAG)\n                        && soldier.entityTags().contains(assignment)\n                        && soldier.entityTags().contains(slotTag));\n        legacy.sort(Comparator\n                .comparingInt((IronGolem soldier) ->\n                        SettlementExternalContentService.isExternalWeapon(soldier.getMainHandItem()) ? 0 : 1)\n                .thenComparing(soldier -> soldier.getUUID().toString()));\n\n        if (!soldiers.isEmpty()) {\n            FrontierSoldierEntity active = soldiers.getFirst();\n            active.setNoAi(false);\n            active.setInvulnerable(false);\n            for (int i = 1; i < soldiers.size(); i++) {\n                removeDuplicateBarracksSoldierPreservingWeapon(level, soldiers.get(i));\n            }\n            for (IronGolem duplicate : legacy) {\n                removeDuplicateBarracksSoldierPreservingWeapon(level, duplicate);\n            }\n            return active;\n        }\n\n        // Missing/migration is authority: a partial route view never converts or recruits.\n        if (!soldierAssignmentEvidenceLoaded(level, data, barracks)) return null;\n        if (legacy.isEmpty()) return null;\n        FrontierSoldierEntity migrated = migrateLegacySoldier(level, legacy.getFirst());\n        if (migrated == null) return null;\n        migrated.setNoAi(false);\n        migrated.setInvulnerable(false);\n        for (int i = 1; i < legacy.size(); i++) {\n            removeDuplicateBarracksSoldierPreservingWeapon(level, legacy.get(i));\n        }\n        return migrated;\n    }\n'''
if new_find not in barracks:
    if barracks.count(old_find) != 1:
        raise SystemExit(f"barracks soldier lifecycle anchor count={barracks.count(old_find)}")
    barracks = barracks.replace(old_find, new_find, 1)

old_migrate = '''    private static FrontierSoldierEntity migrateLegacySoldier(ServerLevel level, IronGolem legacy) {\n        FrontierSoldierEntity replacement = new FrontierSoldierEntity(FrontierContent.FRONTIER_SOLDIER.get(), level);\n        replacement.setPos(legacy.getX(), legacy.getY(), legacy.getZ());\n        replacement.setYRot(legacy.getYRot());\n        replacement.setXRot(legacy.getXRot());\n        replacement.setCustomName(legacy.getCustomName());\n        replacement.setCustomNameVisible(legacy.isCustomNameVisible());\n        replacement.setPersistenceRequired();\n        replacement.setPlayerCreated(true);\n        for (String tag : legacy.entityTags()) replacement.addTag(tag);\n        replacement.setHealth(Math.min(replacement.getMaxHealth(), legacy.getHealth()));\n        if (!level.addFreshEntity(replacement)) return null;\n        legacy.discard();\n        return replacement;\n    }\n'''
new_migrate = '''    private static FrontierSoldierEntity migrateLegacySoldier(ServerLevel level, IronGolem legacy) {\n        FrontierSoldierEntity replacement = new FrontierSoldierEntity(FrontierContent.FRONTIER_SOLDIER.get(), level);\n        replacement.setPos(legacy.getX(), legacy.getY(), legacy.getZ());\n        replacement.setYRot(legacy.getYRot());\n        replacement.setXRot(legacy.getXRot());\n        replacement.setCustomName(legacy.getCustomName());\n        replacement.setCustomNameVisible(legacy.isCustomNameVisible());\n        replacement.setPersistenceRequired();\n        replacement.setPlayerCreated(true);\n        for (String tag : legacy.entityTags()) replacement.addTag(tag);\n        replacement.setHealth(Math.min(replacement.getMaxHealth(), legacy.getHealth()));\n        ItemStack carried = legacy.getMainHandItem().copy();\n        if (!carried.isEmpty()) replacement.setItemSlot(EquipmentSlot.MAINHAND, carried);\n        if (!level.addFreshEntity(replacement)) return null;\n        legacy.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);\n        legacy.discard();\n        return replacement;\n    }\n\n    private static boolean removeDuplicateBarracksSoldierPreservingWeapon(ServerLevel level, IronGolem duplicate) {\n        if (duplicate.getTarget() != null) duplicate.setTarget(null);\n        duplicate.getNavigation().stop();\n        ItemStack carried = duplicate.getMainHandItem();\n        if (!carried.isEmpty()) {\n            ItemEntity physical = new ItemEntity(level, duplicate.getX(), duplicate.getY(), duplicate.getZ(), carried.copy());\n            if (!level.addFreshEntity(physical)) return false;\n            duplicate.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);\n        }\n        duplicate.setNoAi(false);\n        duplicate.setInvulnerable(false);\n        duplicate.discard();\n        return true;\n    }\n'''
if new_migrate not in barracks:
    if barracks.count(old_migrate) != 1:
        raise SystemExit(f"barracks migration anchor count={barracks.count(old_migrate)}")
    barracks = barracks.replace(old_migrate, new_migrate, 1)

old_vars = '''military = text(JAVA / "settlement/SettlementMilitaryOutpostService.java")\noffice = text(JAVA / "settlement/SettlementConstructionOfficeService.java")\n'''
new_vars = '''military = text(JAVA / "settlement/SettlementMilitaryOutpostService.java")\nbarracks = text(JAVA / "settlement/SettlementBarracksService.java")\noffice = text(JAVA / "settlement/SettlementConstructionOfficeService.java")\n'''
if new_vars not in audit:
    if audit.count(old_vars) != 1:
        raise SystemExit(f"audit barracks var anchor count={audit.count(old_vars)}")
    audit = audit.replace(old_vars, new_vars, 1)

audit_anchor = '''forbid(military, (\n    "duplicate.setNoAi(true);",\n    "Historical duplicate bodies are contained rather than deleted"\n), "legacy military duplicate containment")\n'''
audit_block = '''forbid(military, (\n    "duplicate.setNoAi(true);",\n    "Historical duplicate bodies are contained rather than deleted"\n), "legacy military duplicate containment")\nmust(barracks, (\n    "active.setInvulnerable(false);",\n    "removeDuplicateBarracksSoldierPreservingWeapon(level, soldiers.get(i))",\n    "for (IronGolem duplicate : legacy)",\n    "removeDuplicateBarracksSoldierPreservingWeapon(level, duplicate)",\n    "if (!soldierAssignmentEvidenceLoaded(level, data, barracks)) return null;",\n    "ItemStack carried = legacy.getMainHandItem().copy();",\n    "replacement.setItemSlot(EquipmentSlot.MAINHAND, carried);",\n    "legacy.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);",\n    "removeDuplicateBarracksSoldierPreservingWeapon(ServerLevel level, IronGolem duplicate)"\n), "barracks soldier lifecycle/migration")\nforbid(barracks, (\n    "duplicate.setNoAi(true);",\n    "return legacy.isEmpty() ? null : migrateLegacySoldier(level, legacy.getFirst());"\n), "legacy barracks duplicate containment")\n'''
if "barracks soldier lifecycle/migration" not in audit:
    if audit.count(audit_anchor) != 1:
        raise SystemExit(f"audit barracks block anchor count={audit.count(audit_anchor)}")
    audit = audit.replace(audit_anchor, audit_block, 1)

BARRACKS.write_text(barracks, encoding="utf-8")
AUDIT.write_text(audit, encoding="utf-8")

for token in (
    "active.setInvulnerable(false);",
    "removeDuplicateBarracksSoldierPreservingWeapon(level, soldiers.get(i))",
    "for (IronGolem duplicate : legacy)",
    "if (!soldierAssignmentEvidenceLoaded(level, data, barracks)) return null;",
    "ItemStack carried = legacy.getMainHandItem().copy();",
    "replacement.setItemSlot(EquipmentSlot.MAINHAND, carried);",
    "legacy.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);",
    "private static boolean removeDuplicateBarracksSoldierPreservingWeapon(ServerLevel level, IronGolem duplicate)",
):
    if token not in barracks:
        raise SystemExit(f"barracks invariant missing: {token}")
for forbidden in ("duplicate.setNoAi(true);", "return legacy.isEmpty() ? null : migrateLegacySoldier(level, legacy.getFirst());"):
    if forbidden in barracks:
        raise SystemExit(f"legacy barracks invariant remains: {forbidden}")
if "barracks soldier lifecycle/migration" not in audit:
    raise SystemExit("barracks persistent audit missing")

print("BARRACKS SOLDIER LIFECYCLE PATCH PASS")
