from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
OFFICE = JAVA / "settlement/SettlementConstructionOfficeService.java"
AUDIT = ROOT / "tools/test_alpha91_source.py"

office = OFFICE.read_text(encoding="utf-8")
audit = AUDIT.read_text(encoding="utf-8")

old_imports = '''import net.minecraft.world.Container;\nimport net.minecraft.world.entity.EquipmentSlot;\nimport kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;\nimport net.minecraft.world.item.ItemStack;\nimport net.minecraft.world.level.block.Blocks;\nimport net.minecraft.world.phys.AABB;\n'''
new_imports = '''import net.minecraft.world.Container;\nimport net.minecraft.world.entity.EquipmentSlot;\nimport net.minecraft.world.entity.item.ItemEntity;\nimport kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;\nimport net.minecraft.world.item.ItemStack;\nimport net.minecraft.world.level.block.Blocks;\nimport net.minecraft.world.level.block.state.BlockState;\nimport net.minecraft.world.level.pathfinder.Path;\nimport net.minecraft.world.phys.AABB;\n'''
if new_imports not in office:
    if office.count(old_imports) != 1:
        raise SystemExit(f"office imports anchor count={office.count(old_imports)}")
    office = office.replace(old_imports, new_imports, 1)

old_refill_source = '''        BlockPos source = nearestOrdinarySource(level, data, office.workCenter(), wanted);\n        if (source == null) {\n            wantWood = !wantWood;\n            wanted = wantWood ? SettlementInventory::isWood : SettlementInventory::isStone;\n            missing = wantWood ? missingWood : missingStone;\n            if (missing <= 0) {\n                moveOrStop(runner, home, 0.72D);\n                return;\n            }\n            source = nearestOrdinarySource(level, data, office.workCenter(), wanted);\n            if (source == null) {\n                moveOrStop(runner, home, 0.72D);\n                return;\n            }\n        }\n\n        if (runner.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)\n                > INTERACTION_RANGE_SQR) {\n            runner.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, 0.86D);\n            return;\n        }\n'''
new_refill_source = '''        BlockPos source = nearestOrdinarySource(level, data, office.workCenter(), runner, wanted);\n        if (source == null) {\n            wantWood = !wantWood;\n            wanted = wantWood ? SettlementInventory::isWood : SettlementInventory::isStone;\n            missing = wantWood ? missingWood : missingStone;\n            if (missing <= 0) {\n                moveOrStop(runner, home, 0.72D);\n                return;\n            }\n            source = nearestOrdinarySource(level, data, office.workCenter(), runner, wanted);\n            if (source == null) {\n                moveOrStop(runner, home, 0.72D);\n                return;\n            }\n        }\n\n        if (runner.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)\n                > INTERACTION_RANGE_SQR) {\n            moveToInteraction(level, runner, source, 0.86D);\n            return;\n        }\n'''
if new_refill_source not in office:
    if office.count(old_refill_source) != 1:
        raise SystemExit(f"refill source anchor count={office.count(old_refill_source)}")
    office = office.replace(old_refill_source, new_refill_source, 1)

old_delivery = '''    private static boolean deliverCarried(ServerLevel level, SettlementData data, BuildingRecord office, FrontierWorkerEntity runner) {\n        ItemStack carried = runner.getMainHandItem();\n        BlockPos target = nearestOfficeRoom(level, office, carried, runner.blockPosition());\n        if (target == null) target = nearestOrdinaryDeposit(level, data, office, carried, runner.blockPosition());\n        if (target == null) {\n            runner.getNavigation().stop();\n            return true;\n        }\n        if (runner.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)\n                > INTERACTION_RANGE_SQR) {\n            runner.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.88D);\n            return true;\n        }\n        ItemStack remaining = SettlementStorageService.insertAt(level, target, carried);\n        runner.setItemSlot(EquipmentSlot.MAINHAND, remaining);\n        return true;\n    }\n\n    private static BlockPos nearestOfficeRoom(ServerLevel level, BuildingRecord office, ItemStack carried, BlockPos from) {\n        return ConstructionOfficeLayout.materialPositions(office).stream()\n                .filter(pos -> SettlementStorageService.hasRoomAt(level, pos, carried))\n                .min(Comparator.comparingDouble(pos -> pos.distSqr(from)))\n                .orElse(null);\n    }\n\n    private static BlockPos nearestOrdinarySource(ServerLevel level, SettlementData data, BlockPos office,\n                                                   Predicate<ItemStack> wanted) {\n'''
new_delivery = '''    private static boolean deliverCarried(ServerLevel level, SettlementData data, BuildingRecord office, FrontierWorkerEntity runner) {\n        ItemStack carried = runner.getMainHandItem();\n        BlockPos target = nearestOfficeRoom(level, office, runner, carried, runner.blockPosition());\n        if (target == null) target = nearestOrdinaryDeposit(level, data, office, runner, carried, runner.blockPosition());\n        if (target == null) {\n            // No reachable destination: keep the exact physical cargo in MAINHAND instead of freezing\n            // it inside a failed navigation order or discarding it.\n            runner.getNavigation().stop();\n            return true;\n        }\n        if (runner.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)\n                > INTERACTION_RANGE_SQR) {\n            moveToInteraction(level, runner, target, 0.88D);\n            return true;\n        }\n        ItemStack remaining = SettlementStorageService.insertAt(level, target, carried);\n        runner.setItemSlot(EquipmentSlot.MAINHAND, remaining);\n        return true;\n    }\n\n    private static BlockPos nearestOfficeRoom(ServerLevel level, BuildingRecord office, FrontierWorkerEntity runner,\n                                              ItemStack carried, BlockPos from) {\n        return ConstructionOfficeLayout.materialPositions(office).stream()\n                .filter(pos -> SettlementStorageService.hasRoomAt(level, pos, carried))\n                .filter(pos -> canReachInteraction(level, runner, pos))\n                .min(Comparator.comparingDouble(pos -> pos.distSqr(from)))\n                .orElse(null);\n    }\n\n    private static BlockPos nearestOrdinarySource(ServerLevel level, SettlementData data, BlockPos office,\n                                                   FrontierWorkerEntity runner, Predicate<ItemStack> wanted) {\n'''
if new_delivery not in office:
    if office.count(old_delivery) != 1:
        raise SystemExit(f"delivery/source signature anchor count={office.count(old_delivery)}")
    office = office.replace(old_delivery, new_delivery, 1)

old_source_found = '''            if (found) { best = pos; bestDistance = distance; }\n'''
new_source_found = '''            if (found && canReachInteraction(level, runner, pos)) { best = pos; bestDistance = distance; }\n'''
if new_source_found not in office:
    if office.count(old_source_found) != 1:
        raise SystemExit(f"source reachability anchor count={office.count(old_source_found)}")
    office = office.replace(old_source_found, new_source_found, 1)

old_deposit = '''    private static BlockPos nearestOrdinaryDeposit(ServerLevel level, SettlementData data, BuildingRecord officeRecord, ItemStack carried, BlockPos from) {\n        BlockPos office = officeRecord.workCenter();\n        double maxDistance = (double) SOURCE_RADIUS * SOURCE_RADIUS;\n        return SettlementStorageService.ordinaryStoragePositions(data).stream()\n                .filter(pos -> office == null || pos.distSqr(office) <= maxDistance)\n                .filter(pos -> corridorLoaded(level, from, pos))\n                .filter(pos -> SettlementStorageService.hasRoomAt(level, pos, carried))\n                .min(Comparator.comparingDouble(pos -> pos.distSqr(from)))\n                .orElse(null);\n    }\n'''
new_deposit = '''    private static BlockPos nearestOrdinaryDeposit(ServerLevel level, SettlementData data, BuildingRecord officeRecord,\n                                                   FrontierWorkerEntity runner, ItemStack carried, BlockPos from) {\n        BlockPos office = officeRecord.workCenter();\n        double maxDistance = (double) SOURCE_RADIUS * SOURCE_RADIUS;\n        return SettlementStorageService.ordinaryStoragePositions(data).stream()\n                .filter(pos -> office == null || pos.distSqr(office) <= maxDistance)\n                .filter(pos -> corridorLoaded(level, from, pos))\n                .filter(pos -> SettlementStorageService.hasRoomAt(level, pos, carried))\n                .filter(pos -> canReachInteraction(level, runner, pos))\n                .min(Comparator.comparingDouble(pos -> pos.distSqr(from)))\n                .orElse(null);\n    }\n'''
if new_deposit not in office:
    if office.count(old_deposit) != 1:
        raise SystemExit(f"ordinary deposit anchor count={office.count(old_deposit)}")
    office = office.replace(old_deposit, new_deposit, 1)

old_duplicate = '''        if (!existing.isEmpty()) {\n            FrontierWorkerEntity keep = existing.getFirst();\n            keep.setNoAi(false);\n            keep.setInvulnerable(true);\n            for (int i = 1; i < existing.size(); i++) {\n                FrontierWorkerEntity duplicate = existing.get(i);\n                duplicate.getNavigation().stop();\n                duplicate.setNoAi(true);\n                duplicate.setInvulnerable(true);\n            }\n            return keep;\n        }\n'''
new_duplicate = '''        if (!existing.isEmpty()) {\n            FrontierWorkerEntity keep = existing.getFirst();\n            keep.setNoAi(false);\n            keep.setInvulnerable(true);\n            // More than one loaded body for one office is definitive duplicate evidence even if a\n            // wider route chunk is unloaded. Preserve each duplicate's physical cargo, then discard\n            // the excess entity instead of leaving immortal NoAI statues in old saves forever.\n            for (int i = 1; i < existing.size(); i++) {\n                removeDuplicateRunnerPreservingCargo(level, existing.get(i));\n            }\n            return keep;\n        }\n'''
if new_duplicate not in office:
    if office.count(old_duplicate) != 1:
        raise SystemExit(f"duplicate runner anchor count={office.count(old_duplicate)}")
    office = office.replace(old_duplicate, new_duplicate, 1)

insert_before_assignment = '''    private static String assignmentTag(BuildingRecord office) {\n'''
helper_block = '''    private static boolean removeDuplicateRunnerPreservingCargo(ServerLevel level, FrontierWorkerEntity duplicate) {\n        duplicate.getNavigation().stop();\n        ItemStack carried = duplicate.getMainHandItem();\n        if (!carried.isEmpty()) {\n            ItemEntity physical = new ItemEntity(level, duplicate.getX(), duplicate.getY(), duplicate.getZ(), carried.copy());\n            if (!level.addFreshEntity(physical)) return false;\n            duplicate.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);\n        }\n        duplicate.setNoAi(false);\n        duplicate.setInvulnerable(false);\n        duplicate.discard();\n        return true;\n    }\n\n    private static boolean canReachInteraction(ServerLevel level, FrontierWorkerEntity runner, BlockPos target) {\n        if (runner.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)\n                <= INTERACTION_RANGE_SQR) return true;\n        for (BlockPos approach : interactionApproaches(level, runner, target)) {\n            if (createInteractionPath(runner, approach) != null) return true;\n        }\n        return false;\n    }\n\n    private static boolean moveToInteraction(ServerLevel level, FrontierWorkerEntity runner,\n                                             BlockPos target, double speed) {\n        if (runner.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)\n                <= INTERACTION_RANGE_SQR) return true;\n        for (BlockPos approach : interactionApproaches(level, runner, target)) {\n            Path path = createInteractionPath(runner, approach);\n            if (path != null && runner.getNavigation().moveTo(path, speed)) return true;\n        }\n        runner.getNavigation().stop();\n        return false;\n    }\n\n    private static List<BlockPos> interactionApproaches(ServerLevel level, FrontierWorkerEntity runner, BlockPos target) {\n        int[][] offsets = { {0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1} };\n        java.util.ArrayList<BlockPos> result = new java.util.ArrayList<>();\n        for (int dy = -1; dy <= 1; dy++) {\n            for (int[] offset : offsets) {\n                BlockPos approach = target.offset(offset[0], dy, offset[1]);\n                if (isWalkableInteractionCell(level, approach)) result.add(approach);\n            }\n        }\n        result.sort(Comparator.comparingDouble(pos -> runner.distanceToSqr(\n                pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D)));\n        return List.copyOf(result);\n    }\n\n    private static Path createInteractionPath(FrontierWorkerEntity runner, BlockPos target) {\n        Path path = runner.getNavigation().createPath(target, 0);\n        if (path == null || !path.canReach() || path.getEndNode() == null\n                || !path.getEndNode().asBlockPos().equals(target)) return null;\n        return path;\n    }\n\n    private static boolean isWalkableInteractionCell(ServerLevel level, BlockPos feet) {\n        BlockPos head = feet.above();\n        BlockPos below = feet.below();\n        if (!level.hasChunkAt(feet) || !level.hasChunkAt(head) || !level.hasChunkAt(below)) return false;\n        if (level.getBlockEntity(feet) != null || level.getBlockEntity(head) != null) return false;\n        BlockState feetState = level.getBlockState(feet);\n        BlockState headState = level.getBlockState(head);\n        BlockState belowState = level.getBlockState(below);\n        if (!feetState.getFluidState().isEmpty() || !headState.getFluidState().isEmpty()\n                || !belowState.getFluidState().isEmpty()) return false;\n        if ((!feetState.isAir() && !feetState.canBeReplaced())\n                || (!headState.isAir() && !headState.canBeReplaced())) return false;\n        return !belowState.isAir() && !belowState.canBeReplaced();\n    }\n\n'''
if "private static boolean removeDuplicateRunnerPreservingCargo" not in office:
    if office.count(insert_before_assignment) != 1:
        raise SystemExit(f"helper insertion anchor count={office.count(insert_before_assignment)}")
    office = office.replace(insert_before_assignment, helper_block + insert_before_assignment, 1)

old_audit_var = '''outpost = text(JAVA / "settlement/SettlementOutpostLogisticsService.java")\nentity = text(JAVA / "content/FrontierWorkerEntity.java")\n'''
new_audit_var = '''outpost = text(JAVA / "settlement/SettlementOutpostLogisticsService.java")\noffice = text(JAVA / "settlement/SettlementConstructionOfficeService.java")\nentity = text(JAVA / "content/FrontierWorkerEntity.java")\n'''
if new_audit_var not in audit:
    if audit.count(old_audit_var) != 1:
        raise SystemExit(f"audit office var anchor count={audit.count(old_audit_var)}")
    audit = audit.replace(old_audit_var, new_audit_var, 1)

old_audit_outpost = '''forbid(outpost, (\n    "BlockPos target = SettlementStorageService.findLogisticsDepositTarget(level, data, carried);",\n), "outpost forced storage fallback")\nmust(service, (\n'''
new_audit_outpost = '''forbid(outpost, (\n    "BlockPos target = SettlementStorageService.findLogisticsDepositTarget(level, data, carried);",\n), "outpost forced storage fallback")\nmust(office, (\n    "removeDuplicateRunnerPreservingCargo", "canReachInteraction", "moveToInteraction",\n    "createInteractionPath", "path.canReach()", "canReachInteraction(level, runner, pos)"\n), "construction office runner hardening")\nforbid(office, (\n    "duplicate.setNoAi(true);",\n    "runner.getNavigation().moveTo(source.getX() + 0.5D",\n), "construction office legacy runner freeze/source pathing")\nmust(service, (\n'''
if new_audit_outpost not in audit:
    if audit.count(old_audit_outpost) != 1:
        raise SystemExit(f"audit office block anchor count={audit.count(old_audit_outpost)}")
    audit = audit.replace(old_audit_outpost, new_audit_outpost, 1)

OFFICE.write_text(office, encoding="utf-8")
AUDIT.write_text(audit, encoding="utf-8")

final_office = OFFICE.read_text(encoding="utf-8")
final_audit = AUDIT.read_text(encoding="utf-8")
for token in (
    "removeDuplicateRunnerPreservingCargo",
    "canReachInteraction",
    "moveToInteraction",
    "createInteractionPath",
    "path.canReach()",
    "No reachable destination",
):
    if token not in final_office:
        raise SystemExit(f"office invariant missing: {token}")
for forbidden in (
    "duplicate.setNoAi(true);",
    "runner.getNavigation().moveTo(source.getX() + 0.5D",
):
    if forbidden in final_office:
        raise SystemExit(f"legacy office invariant remains: {forbidden}")
if "construction office runner hardening" not in final_audit:
    raise SystemExit("persistent office audit missing")

print("CONSTRUCTION OFFICE RUNNER HARDENING PATCH PASS")
