package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.world.entity.PathfinderMob;
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
                                                  PathfinderMob worker,
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
                                               PathfinderMob worker, ItemStack stack,
                                               double interactionRangeSqr) {
        Set<BlockPos> excluded = new HashSet<>();
        while (true) {
            BlockPos target = SettlementStorageService.findDepositTargetExcluding(level, data, stack, excluded);
            if (target == null) return null;
            if (canReachInteraction(level, worker, target, interactionRangeSqr)) return target;
            excluded.add(target);
        }
    }

    static boolean canReachInteraction(ServerLevel level, PathfinderMob worker,
                                       BlockPos target, double interactionRangeSqr) {
        if (!level.hasChunkAt(target)) return false;
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                <= interactionRangeSqr) return true;
        for (BlockPos approach : approachPositions(level, worker, target)) {
            if (createExactPath(worker, approach) != null) return true;
        }
        return false;
    }

    static boolean moveToInteraction(ServerLevel level, PathfinderMob worker,
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

    private static List<BlockPos> approachPositions(ServerLevel level, PathfinderMob worker, BlockPos target) {
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

    private static Path createExactPath(PathfinderMob worker, BlockPos target) {
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
