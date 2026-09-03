from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
BASIC = JAVA / "settlement/SettlementWorkshopService.java"
ADVANCED = JAVA / "settlement/SettlementAdvancedWorkshopService.java"
NAV = JAVA / "settlement/SettlementWorkerStorageNavigation.java"
AUDIT = ROOT / "tools/test_alpha91_source.py"

basic = BASIC.read_text(encoding="utf-8")
advanced = ADVANCED.read_text(encoding="utf-8")
audit = AUDIT.read_text(encoding="utf-8")

nav_source = '''package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Shared physical-container navigation for service workers.
 *
 * A storage block itself is normally solid, so coordinate-only navigation can return a partial path
 * or repeatedly target an impossible cell. Service workers instead prove a path to a walkable
 * adjacent standing cell. Selection APIs exclude unreachable containers and try another real loaded
 * storage target; if none exists, callers keep their exact physical cargo in hand.
 */
final class SettlementWorkerStorageNavigation {
    private SettlementWorkerStorageNavigation() {}

    static BlockPos findReachableExtractionTarget(ServerLevel level, SettlementData data,
                                                  FrontierWorkerEntity worker,
                                                  Predicate<ItemStack> predicate,
                                                  double interactionRangeSqr) {
        Set<BlockPos> excluded = new HashSet<>();
        while (true) {
            BlockPos target = SettlementStorageService.findExtractionTargetExcluding(level, data, predicate, excluded);
            if (target == null) return null;
            if (canReachInteraction(level, worker, target, interactionRangeSqr)) return target;
            excluded.add(target);
        }
    }

    static BlockPos findReachableDepositTarget(ServerLevel level, SettlementData data,
                                               FrontierWorkerEntity worker, ItemStack stack,
                                               double interactionRangeSqr) {
        Set<BlockPos> excluded = new HashSet<>();
        while (true) {
            BlockPos target = SettlementStorageService.findDepositTargetExcluding(level, data, stack, excluded);
            if (target == null) return null;
            if (canReachInteraction(level, worker, target, interactionRangeSqr)) return target;
            excluded.add(target);
        }
    }

    static boolean canReachInteraction(ServerLevel level, FrontierWorkerEntity worker,
                                       BlockPos target, double interactionRangeSqr) {
        if (!level.hasChunkAt(target)) return false;
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                <= interactionRangeSqr) return true;
        for (BlockPos approach : approachPositions(level, worker, target)) {
            if (createExactPath(worker, approach) != null) return true;
        }
        return false;
    }

    static boolean moveToInteraction(ServerLevel level, FrontierWorkerEntity worker,
                                     BlockPos target, double speed, double interactionRangeSqr) {
        if (!level.hasChunkAt(target)) {
            worker.getNavigation().stop();
            return false;
        }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                <= interactionRangeSqr) return true;
        for (BlockPos approach : approachPositions(level, worker, target)) {
            Path path = createExactPath(worker, approach);
            if (path != null && worker.getNavigation().moveTo(path, speed)) return true;
        }
        worker.getNavigation().stop();
        return false;
    }

    private static List<BlockPos> approachPositions(ServerLevel level, FrontierWorkerEntity worker, BlockPos target) {
        int[][] offsets = { {0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1} };
        List<BlockPos> result = new ArrayList<>();
        for (int dy = -1; dy <= 1; dy++) {
            for (int[] offset : offsets) {
                BlockPos approach = target.offset(offset[0], dy, offset[1]);
                if (isWalkable(level, approach)) result.add(approach);
            }
        }
        result.sort(Comparator.comparingDouble(pos -> worker.distanceToSqr(
                pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D)));
        return List.copyOf(result);
    }

    private static Path createExactPath(FrontierWorkerEntity worker, BlockPos target) {
        Path path = worker.getNavigation().createPath(target, 0);
        if (path == null || !path.canReach() || path.getEndNode() == null
                || !path.getEndNode().asBlockPos().equals(target)) return null;
        return path;
    }

    private static boolean isWalkable(ServerLevel level, BlockPos feet) {
        BlockPos head = feet.above();
        BlockPos below = feet.below();
        if (!level.hasChunkAt(feet) || !level.hasChunkAt(head) || !level.hasChunkAt(below)) return false;
        if (level.getBlockEntity(feet) != null || level.getBlockEntity(head) != null) return false;
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(head);
        BlockState belowState = level.getBlockState(below);
        if (!feetState.getFluidState().isEmpty() || !headState.getFluidState().isEmpty()
                || !belowState.getFluidState().isEmpty()) return false;
        if ((!feetState.isAir() && !feetState.canBeReplaced())
                || (!headState.isAir() && !headState.canBeReplaced())) return false;
        return !belowState.isAir() && !belowState.canBeReplaced();
    }
}
'''
if NAV.exists():
    current_nav = NAV.read_text(encoding="utf-8")
    if current_nav != nav_source:
        raise SystemExit("existing storage-navigation helper differs from expected hardening source")
else:
    NAV.write_text(nav_source, encoding="utf-8")

# Basic repair workshop: commission barrel and storage both use real adjacent path authority.
basic = basic.replace(
'''                worker.getNavigation().moveTo(cratePos.getX() + 0.5D, cratePos.getY(), cratePos.getZ() + 0.5D, 0.78D);\n                return;\n''',
'''                SettlementWorkerStorageNavigation.moveToInteraction(\n                        level, worker, cratePos, 0.78D, INTERACTION_RANGE_SQR);\n                return;\n''')
basic = basic.replace(
'''        BlockPos source = SettlementStorageService.findExtractionTarget(level, data, SettlementStorageService::isMetalStack);\n        if (source == null) return;\n''',
'''        BlockPos source = SettlementWorkerStorageNavigation.findReachableExtractionTarget(\n                level, data, worker, SettlementStorageService::isMetalStack, INTERACTION_RANGE_SQR);\n        if (source == null) {\n            worker.getNavigation().stop();\n            return;\n        }\n''')
basic = basic.replace(
'''            worker.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, 0.82D);\n            return;\n''',
'''            SettlementWorkerStorageNavigation.moveToInteraction(\n                    level, worker, source, 0.82D, INTERACTION_RANGE_SQR);\n            return;\n''')
old_basic_return = '''    private static void returnCarriedItem(ServerLevel level, SettlementData data, FrontierWorkerEntity worker, ItemStack carried) {\n        BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);\n        if (!level.hasChunkAt(target)) {\n            worker.getNavigation().stop();\n            return;\n        }\n        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)\n                > INTERACTION_RANGE_SQR) {\n            worker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.8D);\n            return;\n        }\n        if (!(level.getBlockEntity(target) instanceof Container container)) {\n            worker.getNavigation().stop();\n            return;\n        }\n        ItemStack remaining = SettlementInventory.insert(container, carried);\n        worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);\n        if (remaining.isEmpty()) worker.getNavigation().stop();\n    }\n'''
new_basic_return = '''    private static void returnCarriedItem(ServerLevel level, SettlementData data, FrontierWorkerEntity worker, ItemStack carried) {\n        BlockPos target = SettlementWorkerStorageNavigation.findReachableDepositTarget(\n                level, data, worker, carried, INTERACTION_RANGE_SQR);\n        if (target == null) {\n            worker.getNavigation().stop();\n            return;\n        }\n        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)\n                > INTERACTION_RANGE_SQR) {\n            SettlementWorkerStorageNavigation.moveToInteraction(\n                    level, worker, target, 0.8D, INTERACTION_RANGE_SQR);\n            return;\n        }\n        if (!(level.getBlockEntity(target) instanceof Container container)) {\n            worker.getNavigation().stop();\n            return;\n        }\n        ItemStack remaining = SettlementInventory.insert(container, carried);\n        worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);\n        if (remaining.isEmpty()) worker.getNavigation().stop();\n    }\n'''
if new_basic_return not in basic:
    if basic.count(old_basic_return) != 1:
        raise SystemExit(f"basic return anchor count={basic.count(old_basic_return)}")
    basic = basic.replace(old_basic_return, new_basic_return, 1)

# Advanced forge workshop. Its metal predicate may already have been hardened to isForgeMetal by the
# previous validated patch; preserve that exact predicate while adding reachability.
advanced = advanced.replace(
'''                worker.getNavigation().moveTo(cratePos.getX() + 0.5D, cratePos.getY(), cratePos.getZ() + 0.5D, 0.78D);\n                return;\n''',
'''                SettlementWorkerStorageNavigation.moveToInteraction(\n                        level, worker, cratePos, 0.78D, INTERACTION_RANGE_SQR);\n                return;\n''')
old_advanced_source_variants = (
'''        BlockPos source = SettlementStorageService.findExtractionTarget(level, data, SettlementAdvancedWorkshopService::isForgeMetal);\n        if (source == null) return;\n''',
'''        BlockPos source = SettlementStorageService.findExtractionTarget(level, data, SettlementStorageService::isMetalStack);\n        if (source == null) return;\n''',
)
new_advanced_source = '''        BlockPos source = SettlementWorkerStorageNavigation.findReachableExtractionTarget(\n                level, data, worker, SettlementAdvancedWorkshopService::isForgeMetal, INTERACTION_RANGE_SQR);\n        if (source == null) {\n            worker.getNavigation().stop();\n            return;\n        }\n'''
if new_advanced_source not in advanced:
    matched = [old for old in old_advanced_source_variants if old in advanced]
    if len(matched) != 1:
        raise SystemExit(f"advanced source variants matched={len(matched)}")
    advanced = advanced.replace(matched[0], new_advanced_source, 1)
advanced = advanced.replace(
'''            worker.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, 0.82D);\n            return;\n''',
'''            SettlementWorkerStorageNavigation.moveToInteraction(\n                    level, worker, source, 0.82D, INTERACTION_RANGE_SQR);\n            return;\n''')
old_advanced_return = '''    private static void returnCarriedItem(ServerLevel level, SettlementData data, FrontierWorkerEntity worker, ItemStack carried) {\n        BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);\n        if (!level.hasChunkAt(target)) { worker.getNavigation().stop(); return; }\n        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)\n                > INTERACTION_RANGE_SQR) {\n            worker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.8D);\n            return;\n        }\n        if (!(level.getBlockEntity(target) instanceof Container container)) { worker.getNavigation().stop(); return; }\n        ItemStack remaining = SettlementInventory.insert(container, carried);\n        worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);\n        if (remaining.isEmpty()) worker.getNavigation().stop();\n    }\n'''
new_advanced_return = '''    private static void returnCarriedItem(ServerLevel level, SettlementData data, FrontierWorkerEntity worker, ItemStack carried) {\n        BlockPos target = SettlementWorkerStorageNavigation.findReachableDepositTarget(\n                level, data, worker, carried, INTERACTION_RANGE_SQR);\n        if (target == null) { worker.getNavigation().stop(); return; }\n        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)\n                > INTERACTION_RANGE_SQR) {\n            SettlementWorkerStorageNavigation.moveToInteraction(\n                    level, worker, target, 0.8D, INTERACTION_RANGE_SQR);\n            return;\n        }\n        if (!(level.getBlockEntity(target) instanceof Container container)) { worker.getNavigation().stop(); return; }\n        ItemStack remaining = SettlementInventory.insert(container, carried);\n        worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);\n        if (remaining.isEmpty()) worker.getNavigation().stop();\n    }\n'''
if new_advanced_return not in advanced:
    if advanced.count(old_advanced_return) != 1:
        raise SystemExit(f"advanced return anchor count={advanced.count(old_advanced_return)}")
    advanced = advanced.replace(old_advanced_return, new_advanced_return, 1)

# Persistent audit additions that are stable regardless of earlier Alpha.91 audit extensions.
workshop_var = 'workshop = text(JAVA / "settlement/SettlementWorkshopService.java")\n'
nav_var = 'storage_nav = text(JAVA / "settlement/SettlementWorkerStorageNavigation.java")\n'
if workshop_var not in audit or nav_var not in audit:
    anchor = 'entity = text(JAVA / "content/FrontierWorkerEntity.java")\n'
    if audit.count(anchor) != 1:
        raise SystemExit(f"workshop audit variable anchor count={audit.count(anchor)}")
    prefix = ""
    if workshop_var not in audit: prefix += workshop_var
    if nav_var not in audit: prefix += nav_var
    audit = audit.replace(anchor, prefix + anchor, 1)

workshop_audit = '''must(storage_nav, (\n    "findReachableExtractionTarget", "findReachableDepositTarget",\n    "moveToInteraction", "createExactPath", "path.canReach()"\n), "workshop storage navigation")\nmust(workshop, (\n    "SettlementWorkerStorageNavigation.findReachableExtractionTarget",\n    "SettlementWorkerStorageNavigation.findReachableDepositTarget",\n    "SettlementWorkerStorageNavigation.moveToInteraction"\n), "basic workshop storage reachability")\nmust(advanced_workshop, (\n    "SettlementWorkerStorageNavigation.findReachableExtractionTarget",\n    "SettlementWorkerStorageNavigation.findReachableDepositTarget",\n    "SettlementWorkerStorageNavigation.moveToInteraction"\n), "advanced workshop storage reachability")\nforbid(workshop, (\n    "SettlementStorageService.findExtractionTarget(level, data, SettlementStorageService::isMetalStack)",\n    "BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);"\n), "basic workshop forced storage target")\nforbid(advanced_workshop, (\n    "BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);"\n), "advanced workshop forced storage target")\n'''
if '"workshop storage navigation"' not in audit:
    anchor = 'must(service, (\n'
    if audit.count(anchor) != 1:
        raise SystemExit(f"workshop audit block anchor count={audit.count(anchor)}")
    audit = audit.replace(anchor, workshop_audit + anchor, 1)

BASIC.write_text(basic, encoding="utf-8")
ADVANCED.write_text(advanced, encoding="utf-8")
AUDIT.write_text(audit, encoding="utf-8")

for path, tokens in (
    (NAV, ("findReachableExtractionTarget", "findReachableDepositTarget", "createExactPath", "path.canReach()")),
    (BASIC, ("SettlementWorkerStorageNavigation.findReachableExtractionTarget", "SettlementWorkerStorageNavigation.findReachableDepositTarget")),
    (ADVANCED, ("SettlementWorkerStorageNavigation.findReachableExtractionTarget", "SettlementWorkerStorageNavigation.findReachableDepositTarget")),
):
    src = path.read_text(encoding="utf-8")
    for token in tokens:
        if token not in src:
            raise SystemExit(f"{path.name} missing: {token}")
if "workshop storage navigation" not in AUDIT.read_text(encoding="utf-8"):
    raise SystemExit("persistent workshop reachability audit missing")

print("WORKSHOP STORAGE REACHABILITY PATCH PASS")
