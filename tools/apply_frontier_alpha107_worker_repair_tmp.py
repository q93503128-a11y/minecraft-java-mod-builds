#!/usr/bin/env python3
from pathlib import Path
import json

REPO = Path(__file__).resolve().parents[1]
ROOT = REPO / "projects/frontier-settlement"
SET = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement"


def read(p): return p.read_text(encoding="utf-8")
def write(p, s): p.write_text(s, encoding="utf-8")
def replace_once(s, old, new, label):
    count = s.count(old)
    if count != 1:
        raise AssertionError(f"{label}: expected 1 match, got {count}")
    return s.replace(old, new, 1)

# Version / lock.
gradle = ROOT / "gradle.properties"
s = read(gradle)
s = replace_once(s, "mod_version=0.1.0-alpha.106", "mod_version=0.1.0-alpha.107", "gradle version")
if "# Alpha.107 production handoff" not in s:
    s += "\n# Alpha.107 production handoff: full-stack local deposit no longer re-extracts into a haul loop; completed houses auto-repair missing blueprint cells from real settlement wood/stone/metal before ruin retirement.\n"
write(gradle, s)

lockp = ROOT / "COMPANION_LOCK.json"
lock = json.loads(read(lockp))
lock["target"]["frontier_settlement"] = "0.1.0-alpha.107"
note = "Alpha.107 keeps completed production worksite barrels as authoritative physical buffers instead of immediately re-extracting each produced stack, and repairs only missing/replaceable completed-HOUSE blueprint cells from real loaded settlement wood/stone/metal before existing ruin retirement."
if note not in lock.get("notes", []):
    lock.setdefault("notes", []).append(note)
write(lockp, json.dumps(lock, ensure_ascii=False, indent=2) + "\n")

# Worker state machine / deposit loop.
workerp = SET / "SettlementWorkerService.java"
w = read(workerp)
w = replace_once(w,
    '    private static final String WORKSITE_EXPORT_TAG = "frontier_settlement_worksite_export";\n',
    '    // Alpha.107 no longer re-extracts every produced stack from the profession barrel.\n'
    '    // Keep the old entity tag only as a one-way save migration marker.\n'
    '    private static final String LEGACY_WORKSITE_EXPORT_TAG = "frontier_settlement_worksite_export";\n',
    "legacy export tag")
w = replace_once(w,
    '    private static final double QUARRY_REMOTE_WORK_REACH_SQR = 25.0D; // 5 blocks\n',
    '    private static final double QUARRY_REMOTE_WORK_REACH_SQR = 25.0D; // 5 blocks\n'
    '    // A worker already beside its own profession barrel should interact directly instead of\n'
    '    // depending on a fragile final path cell inside fences/walls.\n'
    '    private static final double WORKSITE_STORAGE_INTERACTION_REACH_SQR = 36.0D; // 6 blocks\n',
    "worksite storage reach")

old_run = '''            worker.setNoAi(false);\n            worker.setInvulnerable(false);\n            work.run(level, data, worker, building);\n'''
new_run = '''            worker.setNoAi(false);\n            worker.setInvulnerable(false);\n            // Old saves can contain a worker that was carrying a worksite-export stack. That old\n            // state caused a local-barrel -> MAINHAND -> town-storage retry loop. Retire it once and\n            // let the ordinary cargo state machine decide where the physical stack belongs.\n            if (worker.entityTags().contains(LEGACY_WORKSITE_EXPORT_TAG)) {\n                worker.removeTag(LEGACY_WORKSITE_EXPORT_TAG);\n                worker.getNavigation().stop();\n                MOVEMENT_WATCHES.remove(worker.getUUID());\n            }\n            work.run(level, data, worker, building);\n'''
w = replace_once(w, old_run, new_run, "worker legacy tag migration")

for method_name in ("workLumber", "workFarm", "workQuarry", "workMine"):
    old = f"        if (tryExportWorksiteBuffer(level, data, worker, { {'workLumber':'camp','workFarm':'farm','workQuarry':'quarry','workMine':'mine'}[method_name] })) return;\n"
    if old not in w:
        raise AssertionError(f"missing old export call in {method_name}")
    w = w.replace(old, "", 1)

w = replace_once(w,
    '        if (!harvested.isEmpty() && appendCargo(worker, harvested)) worker.swing(InteractionHand.MAIN_HAND);\n',
    '        if (!harvested.isEmpty() && appendCargo(worker, harvested)) {\n'
    '            worker.swing(InteractionHand.MAIN_HAND);\n'
    '            deliverIfCargoFull(level, data, worker, camp);\n'
    '        }\n',
    "lumber full-stack immediate handoff")
w = replace_once(w,
    '            if (appendCargo(worker, new ItemStack(Items.WHEAT, harvested))) worker.swing(InteractionHand.MAIN_HAND);\n            return;\n',
    '            if (appendCargo(worker, new ItemStack(Items.WHEAT, harvested))) {\n'
    '                worker.swing(InteractionHand.MAIN_HAND);\n'
    '                deliverIfCargoFull(level, data, worker, farm);\n'
    '            }\n'
    '            return;\n',
    "farm full-stack immediate handoff")
w = replace_once(w,
    '        if (!stone.isEmpty() && appendCargo(worker, stone)) worker.swing(InteractionHand.MAIN_HAND);\n',
    '        if (!stone.isEmpty() && appendCargo(worker, stone)) {\n'
    '            worker.swing(InteractionHand.MAIN_HAND);\n'
    '            deliverIfCargoFull(level, data, worker, quarry);\n'
    '        }\n',
    "quarry full-stack immediate handoff")
w = replace_once(w,
    '        if (!mined.isEmpty() && appendCargo(worker, mined)) worker.swing(InteractionHand.MAIN_HAND);\n',
    '        if (!mined.isEmpty() && appendCargo(worker, mined)) {\n'
    '            worker.swing(InteractionHand.MAIN_HAND);\n'
    '            deliverIfCargoFull(level, data, worker, mine);\n'
    '        }\n',
    "mine full-stack immediate handoff")

start_marker = '''    /**\n     * Profession barrels remain visible local buffers, but they are not dead-end economy silos.\n'''
end_marker = '    private static boolean isQuarryOutputItem(ItemStack stack) {'
start = w.find(start_marker)
end = w.find(end_marker)
if start < 0 or end < 0 or end <= start:
    raise AssertionError("could not locate retired worksite export block")
w = w[:start] + '''    // Profession barrels are already part of SettlementStorageService's authoritative physical\n    // resource ledger. Alpha.107 therefore leaves deposited output in that local buffer and lets\n    // the producer resume work. When the barrel is actually full, the ordinary delivery path\n    // naturally falls back to another loaded town storage target.\n\n''' + w[end:]

cargo_marker = '    private static int cargoLimit(ItemStack stack) {'
idx = w.find(cargo_marker)
if idx < 0: raise AssertionError("cargoLimit marker missing")
helper = '''    private static void deliverIfCargoFull(ServerLevel level, SettlementData data,\n                                               FrontierWorkerEntity worker, BuildingRecord building) {\n        ItemStack carried = worker.getMainHandItem();\n        if (carried.isEmpty() || carried.getCount() < cargoLimit(carried)) return;\n        deliverToWorksiteStorage(level, data, worker, building, carried);\n    }\n\n'''
w = w[:idx] + helper + w[idx:]

method_start = w.find('    private static void deliverToWorksiteStorage(ServerLevel level, SettlementData data,')
method_end = w.find('    private static void deliverToTownStorage(ServerLevel level, SettlementData data,', method_start)
if method_start < 0 or method_end < 0: raise AssertionError("delivery method markers missing")
new_delivery = '''    private static void deliverToWorksiteStorage(ServerLevel level, SettlementData data,\n                                                 FrontierWorkerEntity worker, BuildingRecord building,\n                                                 ItemStack carried) {\n        if (carried.isEmpty()) return;\n        BlockPos local = SettlementStorageService.worksiteStoragePosition(building);\n        if (local != null && level.hasChunkAt(local) && level.getBlockState(local).is(Blocks.BARREL)\n                && SettlementStorageService.hasRoomAt(level, local, carried)) {\n            double distance = worker.distanceToSqr(\n                    local.getX() + 0.5D, local.getY() + 0.5D, local.getZ() + 0.5D);\n            if (distance <= WORKSITE_STORAGE_INTERACTION_REACH_SQR) {\n                // Full-stack handoff is authoritative as soon as the worker is beside its own jobsite.\n                // Do not wait for a final path node that can be invalidated by fences, doors or knockback.\n                worker.getNavigation().stop();\n                ItemStack remaining = SettlementStorageService.insertAt(level, local, carried);\n                worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);\n                clearTargetIfEmpty(worker);\n                return;\n            }\n            if (!isTargetBlocked(level, worker, local) && moveNear(level, worker, local, 0.86D)) return;\n        }\n        deliverToTownStorage(level, data, worker, carried);\n    }\n\n'''
w = w[:method_start] + new_delivery + w[method_end:]

old_clear = '''    private static void clearTargetIfEmpty(FrontierWorkerEntity worker) {\n        if (worker.getMainHandItem().isEmpty()) clearResourceTarget(worker);\n        MOVEMENT_WATCHES.remove(worker.getUUID());\n    }\n'''
new_clear = '''    private static void clearTargetIfEmpty(FrontierWorkerEntity worker) {\n        if (!worker.getMainHandItem().isEmpty()) {\n            MOVEMENT_WATCHES.remove(worker.getUUID());\n            return;\n        }\n        // A completed physical deposit is a hard state-machine boundary: stale target, blocked-path\n        // and navigation state must not survive into the next production cycle.\n        worker.getNavigation().stop();\n        clearTransientWorkerState(worker);\n    }\n'''
w = replace_once(w, old_clear, new_clear, "deposit reset")
old_transient = '''    private static void clearTransientWorkerState(FrontierWorkerEntity worker) {\n        clearResourceTarget(worker);\n        MOVEMENT_WATCHES.remove(worker.getUUID());\n        BLOCKED_TARGETS.remove(worker.getUUID());\n    }\n'''
new_transient = '''    private static void clearTransientWorkerState(FrontierWorkerEntity worker) {\n        clearResourceTarget(worker);\n        MOVEMENT_WATCHES.remove(worker.getUUID());\n        BLOCKED_TARGETS.remove(worker.getUUID());\n        worker.removeTag(LEGACY_WORKSITE_EXPORT_TAG);\n    }\n'''
w = replace_once(w, old_transient, new_transient, "legacy transient cleanup")
if "tryExportWorksiteBuffer(" in w or "WORKSITE_EXPORT_TAG" in w.replace("LEGACY_WORKSITE_EXPORT_TAG", ""):
    raise AssertionError("retired export authority remains")
write(workerp, w)

# Completed-house repair before ruin retirement.
integrityp = SET / "SettlementBuildingIntegrityService.java"
integrity = '''package kr.moonseungjun.frontiersettlement.settlement;\n\nimport net.minecraft.core.BlockPos;\nimport net.minecraft.server.MinecraftServer;\nimport net.minecraft.server.level.ServerLevel;\nimport net.minecraft.world.level.block.Blocks;\nimport net.minecraft.world.level.block.state.BlockState;\nimport net.neoforged.neoforge.event.tick.ServerTickEvent;\n\nimport java.util.ArrayList;\nimport java.util.List;\nimport java.util.Set;\n\n/** Keeps completed-building authority tied to the physical world. */\npublic final class SettlementBuildingIntegrityService {\n    private static final int REPAIR_INTERVAL_TICKS = 20;\n    private static final int CHECK_INTERVAL_TICKS = 100;\n    private static final int MAX_REPAIR_BLOCKS_PER_PASS = 12;\n    private static final int RUIN_INTACT_PERCENT = 45;\n    private static final Set<BuildingType> PRODUCTION_TYPES = Set.of(\n            BuildingType.LUMBER_CAMP, BuildingType.FARM, BuildingType.QUARRY, BuildingType.MINE);\n\n    private enum RepairMaterial { WOOD, STONE, METAL }\n    private record RepairCandidate(BlockPos pos, BlockState expected, RepairMaterial material) {}\n\n    private SettlementBuildingIntegrityService() {}\n\n    public static void onServerTick(ServerTickEvent.Post event) {\n        MinecraftServer server = event.getServer();\n        int tick = server.getTickCount();\n        boolean repairPass = tick % REPAIR_INTERVAL_TICKS == 0;\n        boolean integrityPass = tick % CHECK_INTERVAL_TICKS == 0;\n        if (!repairPass && !integrityPass) return;\n\n        SettlementData data = SettlementData.get(server);\n        if (!data.founded()) return;\n        ServerLevel level = server.overworld();\n\n        // Repair comes first on the shared 100-tick boundary. A creeper-damaged house with real\n        // settlement materials should be restored instead of being retired from one stale snapshot.\n        if (repairPass) {\n            int repaired = repairDamagedHouses(level, data);\n            if (repaired > 0) {\n                SettlementService.refreshResources(server, data);\n                SettlementService.broadcast(server, data);\n            }\n        }\n        if (!integrityPass) return;\n\n        for (BuildingRecord building : List.copyOf(data.buildings())) {\n            BuildingType type = BuildingType.fromId(building.type());\n            if (!tracksIntegrity(type) || !fullyLoaded(level, type, building)) continue;\n            List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(\n                    type, building.origin(), building.rotation());\n            int intact = 0;\n            for (BuildingBlueprints.Placement placement : plan) {\n                if (level.getBlockState(placement.pos()).is(placement.state().getBlock())) intact++;\n            }\n            if ((long) intact * 100L >= (long) plan.size() * RUIN_INTACT_PERCENT) continue;\n\n            // Retire authority first. Production remnants/containers become ordinary recoverable world blocks;\n            // only houses keep the Alpha.98 matching non-container remnant cleanup.\n            if (data.removeCompletedBuilding(building)) {\n                if (type == BuildingType.HOUSE) clearKnownHouseRemnants(level, plan);\n                SettlementService.refreshResources(server, data);\n                SettlementService.broadcast(server, data);\n            }\n            break;\n        }\n    }\n\n    private static int repairDamagedHouses(ServerLevel level, SettlementData data) {\n        // Repair is a real physical resource transaction. If any authoritative settlement storage\n        // is unloaded, fail closed rather than repairing from a stale/partial ledger.\n        if (!SettlementStorageService.storageAvailable(level, data)) return 0;\n        SettlementResources available = SettlementStorageService.scan(level, data);\n        long woodBudget = available.wood();\n        long stoneBudget = available.stone();\n        long metalBudget = available.metal();\n        long woodCost = 0L;\n        long stoneCost = 0L;\n        long metalCost = 0L;\n        List<RepairCandidate> selected = new ArrayList<>();\n\n        outer:\n        for (BuildingRecord building : List.copyOf(data.buildings())) {\n            if (building.buildingType() != BuildingType.HOUSE\n                    || !fullyLoaded(level, BuildingType.HOUSE, building)) continue;\n            for (BuildingBlueprints.Placement placement : RotatedBlueprints.create(\n                    BuildingType.HOUSE, building.origin(), building.rotation())) {\n                if (selected.size() >= MAX_REPAIR_BLOCKS_PER_PASS) break outer;\n                BlockPos pos = placement.pos();\n                BlockState current = level.getBlockState(pos);\n                if (current.is(placement.state().getBlock()) || !canRepairVacancy(level, pos, current)) continue;\n                RepairMaterial material = repairMaterial(placement.state());\n                switch (material) {\n                    case WOOD -> {\n                        if (woodCost >= woodBudget) continue;\n                        woodCost++;\n                    }\n                    case STONE -> {\n                        if (stoneCost >= stoneBudget) continue;\n                        stoneCost++;\n                    }\n                    case METAL -> {\n                        if (metalCost >= metalBudget) continue;\n                        metalCost++;\n                    }\n                }\n                selected.add(new RepairCandidate(pos.immutable(), placement.state(), material));\n            }\n        }\n        if (selected.isEmpty()) return 0;\n\n        if ((woodCost > 0L || stoneCost > 0L)\n                && !SettlementStorageService.consume(level, data, woodCost, stoneCost, 0L)) return 0;\n        if (metalCost > 0L && !SettlementStorageService.consumeMetal(level, data, metalCost)) return 0;\n\n        int repaired = 0;\n        for (RepairCandidate candidate : selected) {\n            BlockState current = level.getBlockState(candidate.pos());\n            if (!canRepairVacancy(level, candidate.pos(), current)) continue;\n            if (level.setBlock(candidate.pos(), candidate.expected(), 3)) repaired++;\n        }\n        return repaired;\n    }\n\n    private static RepairMaterial repairMaterial(BlockState expected) {\n        // Houses are overwhelmingly timber. Glass spends one stone unit as the existing mineral\n        // abstraction; lanterns spend one canonical metal unit. Everything else in the house\n        // blueprint (planks/logs/stairs/slabs/door/crafting table) spends one wood unit.\n        if (expected.is(Blocks.LANTERN)) return RepairMaterial.METAL;\n        if (expected.is(Blocks.GLASS)) return RepairMaterial.STONE;\n        return RepairMaterial.WOOD;\n    }\n\n    private static boolean canRepairVacancy(ServerLevel level, BlockPos pos, BlockState current) {\n        if (level.getBlockEntity(pos) != null || !current.getFluidState().isEmpty()) return false;\n        return current.isAir() || current.canBeReplaced();\n    }\n\n    private static boolean tracksIntegrity(BuildingType type) {\n        return type == BuildingType.HOUSE || PRODUCTION_TYPES.contains(type);\n    }\n\n    private static boolean fullyLoaded(ServerLevel level, BuildingType type, BuildingRecord building) {\n        for (BuildingBlueprints.Placement placement : RotatedBlueprints.create(\n                type, building.origin(), building.rotation())) {\n            if (!level.hasChunkAt(placement.pos())) return false;\n        }\n        return true;\n    }\n\n    private static void clearKnownHouseRemnants(ServerLevel level, List<BuildingBlueprints.Placement> plan) {\n        for (BuildingBlueprints.Placement placement : plan) {\n            BlockPos pos = placement.pos();\n            if (level.getBlockEntity(pos) != null) continue;\n            BlockState current = level.getBlockState(pos);\n            if (!current.is(placement.state().getBlock())) continue;\n            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);\n        }\n    }\n}\n'''
write(integrityp, integrity)

# Current-source verifier follows the new runtime authority.
verifier = ROOT / "tools/test_current_source.py"
v = read(verifier)
v = replace_once(v, 'require("mod_version=0.1.0-alpha.106" in gradle, "current verifier/version drift")',
                 'require("mod_version=0.1.0-alpha.107" in gradle, "current verifier/version drift")', "verifier version")
anchor = 'require("DUPLICATE_MAINTENANCE_INTERVAL_TICKS = 200" in worker, "maintenance duplicate scans regressed to hot-path cadence")\n'
addition = '''require("WORKSITE_STORAGE_INTERACTION_REACH_SQR = 36.0D" in worker, "close worksite deposit reach missing")\nrequire("deliverIfCargoFull" in worker, "full-stack immediate deposit handoff missing")\nrequire("tryExportWorksiteBuffer(" not in worker, "retired worksite re-extraction loop returned")\nrequire("LEGACY_WORKSITE_EXPORT_TAG" in worker and "worker.removeTag(LEGACY_WORKSITE_EXPORT_TAG)" in worker, "legacy export-tag migration missing")\nrequire("Profession barrels are already part of SettlementStorageService's authoritative physical" in worker, "worksite barrel authority rationale missing")\n'''
v = replace_once(v, anchor, anchor + addition, "worker verifier additions")
old_integrity = '''require("if (type == BuildingType.HOUSE) clearKnownHouseRemnants" in integrity, "production retirement may clear player/container remnants")\n'''
new_integrity = old_integrity + '''require("REPAIR_INTERVAL_TICKS = 20" in integrity and "MAX_REPAIR_BLOCKS_PER_PASS = 12" in integrity, "bounded immediate house repair cadence missing")\nrequire("repairDamagedHouses" in integrity and "repairPass" in integrity and "integrityPass" in integrity, "house repair is not ordered before ruin retirement")\nrequire("SettlementStorageService.consume(level, data, woodCost, stoneCost, 0L)" in integrity, "house repair bypasses physical wood/stone authority")\nrequire("SettlementStorageService.consumeMetal(level, data, metalCost)" in integrity, "house lantern repair bypasses canonical metal authority")\nrequire("canRepairVacancy" in integrity and "level.getBlockEntity(pos) != null" in integrity, "house repair may overwrite protected/player container cells")\n'''
v = replace_once(v, old_integrity, new_integrity, "integrity verifier additions")
v = replace_once(v,
    'print("CURRENT SOURCE CHECK PASS: alpha106 production density + active farm tending + prior authority invariants")',
    'print("CURRENT SOURCE CHECK PASS: alpha107 worker handoff + physical house repair + prior authority invariants")',
    "verifier message")
write(verifier, v)

print("Alpha107 patch applied")
