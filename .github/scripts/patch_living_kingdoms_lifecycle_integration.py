from pathlib import Path

living_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/LivingKingdoms.java")
living = living_path.read_text(encoding="utf-8")

if "import kr.moonseungjun.livingkingdoms.world.ErdenExteriorLifecycleManager;" not in living:
    needle = "import kr.moonseungjun.livingkingdoms.world.ErdenExteriorTicketReaper;\n"
    if needle not in living:
        raise SystemExit("LivingKingdoms lifecycle import insertion point missing")
    living = living.replace(needle, needle + "import kr.moonseungjun.livingkingdoms.world.ErdenExteriorLifecycleManager;\n")

if "ErdenExteriorLifecycleManager.onServerTick(event);" not in living:
    needle = "        ErdenExteriorTicketReaper.onServerTick(event);\n        ErdenExteriorWorkforceManager.onServerTick(event);\n"
    replacement = "        ErdenExteriorTicketReaper.onServerTick(event);\n        ErdenExteriorLifecycleManager.onServerTick(event);\n        ErdenExteriorWorkforceManager.onServerTick(event);\n"
    if needle not in living:
        raise SystemExit("LivingKingdoms lifecycle tick insertion point missing")
    living = living.replace(needle, replacement)

if "ErdenExteriorLifecycleManager.markDeadIfLifecycleResident(level, villager);" not in living:
    needle = "            ErdenExteriorWorkforceManager.markDeadIfWorker(level, villager);\n"
    if needle not in living:
        raise SystemExit("LivingKingdoms lifecycle death insertion point missing")
    living = living.replace(needle, needle + "            ErdenExteriorLifecycleManager.markDeadIfLifecycleResident(level, villager);\n")

if "ErdenExteriorLifecycleManager.handleInteraction(event);" not in living:
    needle = "        ErdenExteriorWorkforceManager.handleInteraction(event);\n"
    if needle not in living:
        raise SystemExit("LivingKingdoms lifecycle interaction insertion point missing")
    living = living.replace(needle, "        ErdenExteriorLifecycleManager.handleInteraction(event);\n" + needle)

living_path.write_text(living, encoding="utf-8")

manager_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorWorkforceManager.java")
manager = manager_path.read_text(encoding="utf-8")
manager = manager.replace(
    "        processThroughDay(workforce, currentDay);",
    "        processThroughDay(level, workforce, currentDay);"
)
manager = manager.replace(
    "        return node == null ? 0 : laborState(workforce, node, day).productionPercent();",
    "        return node == null ? 0 : laborState(level, workforce, node, day).productionPercent();"
)
old_signature = """    private static void processThroughDay(
            ErdenExteriorWorkforceSavedData workforce,
            long currentDay) {
"""
new_signature = """    private static void processThroughDay(
            ServerLevel level,
            ErdenExteriorWorkforceSavedData workforce,
            long currentDay) {
"""
if old_signature in manager:
    manager = manager.replace(old_signature, new_signature)
elif new_signature not in manager:
    raise SystemExit("workforce processThroughDay signature missing")
manager = manager.replace(
    "                states.add(laborState(workforce, node, day));",
    "                states.add(laborState(level, workforce, node, day));"
)
old_labor_signature = """    private static ErdenExteriorWorkforceSavedData.NodeLabor laborState(
            ErdenExteriorWorkforceSavedData workforce,
            ErdenKingdomSupplyCatalog.SupplyNode node,
            long day) {
"""
new_labor_signature = """    private static ErdenExteriorWorkforceSavedData.NodeLabor laborState(
            ServerLevel level,
            ErdenExteriorWorkforceSavedData workforce,
            ErdenKingdomSupplyCatalog.SupplyNode node,
            long day) {
"""
if old_labor_signature in manager:
    manager = manager.replace(old_labor_signature, new_labor_signature)
elif new_labor_signature not in manager:
    raise SystemExit("workforce laborState signature missing")
old_worker_block = """                if (workforce.isDead(resident.id())) {
                    dead++;
                    continue;
                }
                alive++;
                if (absentOnDay(resident, node.role, day)) absent++;
                else attended++;
"""
new_worker_block = """                if (workforce.isDead(resident.id())) {
                    dead++;
                    continue;
                }
                if (!ErdenExteriorLifecycleManager.foundingWorkerAvailable(
                        level, resident.id(), day)) continue;
                alive++;
                if (absentOnDay(resident, node.role, day)) absent++;
                else attended++;
"""
if old_worker_block in manager:
    manager = manager.replace(old_worker_block, new_worker_block)
elif new_worker_block not in manager:
    raise SystemExit("workforce founding worker block missing")
old_percent = """        int percent = required <= 0 ? 100
                : Math.clamp(attended * 100 / required, 0, 100);
"""
new_percent = """        ErdenExteriorLifecycleManager.LaborContribution lifecycleLabor =
                ErdenExteriorLifecycleManager.additionalLabor(level, node.id, node.role, day);
        alive += lifecycleLabor.alive();
        attended += lifecycleLabor.attended();
        absent += lifecycleLabor.absent();
        dead += lifecycleLabor.dead();
        int percent = required <= 0 ? 100
                : Math.clamp(attended * 100 / required, 0, 100);
"""
if old_percent in manager:
    manager = manager.replace(old_percent, new_percent)
elif new_percent not in manager:
    raise SystemExit("workforce lifecycle labor insertion point missing")

manager_path.write_text(manager, encoding="utf-8")
