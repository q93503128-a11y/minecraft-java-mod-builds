package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Sixth-circle precision terrain engineering.
 *
 * Move Earth must not be a miniature Earthquake. The spell relocates existing surface material:
 * a shallow central trench is cut along the locked cast direction and the removed topsoil becomes
 * two flanking berms. Nothing is duplicated, block entities/fluids/unbreakable material are never
 * moved, unloaded chunks are skipped, and a strict per-cast relocation budget caps server cost.
 */
public final class MoveEarthService {
    public static final int MAX_MOVED_BLOCKS = 144;

    private MoveEarthService() {}

    public static double length(double range) {
        return Math.max(20.0, Math.min(36.0, Math.max(0.0, range) * .64));
    }

    public static double trenchHalfWidth(double range) {
        return Math.max(1.6, Math.min(2.4, 1.35 + Math.max(0.0, range) * .018));
    }

    public static double bermOffset(double range) {
        return trenchHalfWidth(range) + 2.6;
    }

    /** Returns the number of source blocks actually relocated. */
    public static int execute(ServerPlayer player, Vec3 center, Vec3 direction, double range) {
        if (player == null || center == null) return 0;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 forward = horizontal(direction);
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        double length = length(range);
        double trench = trenchHalfWidth(range);
        double berm = bermOffset(range);
        int steps = Math.max(12, Math.min(30, (int) Math.ceil(length / 1.35)));
        Set<BlockPos> touched = new HashSet<>();
        int moved = 0;

        for (int step = 0; step < steps && moved < MAX_MOVED_BLOCKS; step++) {
            double along = steps <= 1 ? 0.0 : -length * .5 + length * step / (steps - 1.0);
            Vec3 axis = center.add(forward.scale(along));
            for (int lane = -1; lane <= 1 && moved < MAX_MOVED_BLOCKS; lane++) {
                double lateral = lane * trench * .72;
                Vec3 sourcePoint = axis.add(right.scale(lateral));
                BlockPos source = surfaceBlock(level, sourcePoint);
                if (source == null || touched.contains(source)) continue;
                BlockState state = level.getBlockState(source);
                if (!movable(level, source, state)) continue;

                int side = lane < 0 ? -1 : lane > 0 ? 1 : ((step & 1) == 0 ? -1 : 1);
                double stagger = ((step / 2) & 1) == 0 ? 0.0 : .72;
                Vec3 destinationPoint = axis.add(right.scale(side * (berm + stagger)));
                BlockPos destination = standingCell(level, destinationPoint);
                if (destination == null || touched.contains(destination) || destination.equals(source)) continue;
                if (!level.getBlockState(destination).isAir()) continue;

                // Transaction-like move: remove first, restore if destination placement fails.
                if (!level.setBlock(source, Blocks.AIR.defaultBlockState(), 3)) continue;
                if (!level.setBlock(destination, state, 3)) {
                    level.setBlock(source, state, 3);
                    continue;
                }
                touched.add(source.immutable());
                touched.add(destination.immutable());
                moved++;
            }
        }
        return moved;
    }

    private static BlockPos surfaceBlock(ServerLevel level, Vec3 desired) {
        Vec3 standing = GroundTargetResolver.surface(level, desired);
        BlockPos feet = BlockPos.containing(standing);
        BlockPos ground = feet.below();
        if (!level.hasChunkAt(ground)) return null;
        return ground;
    }

    private static BlockPos standingCell(ServerLevel level, Vec3 desired) {
        Vec3 standing = GroundTargetResolver.surface(level, desired);
        BlockPos feet = BlockPos.containing(standing);
        if (!level.hasChunkAt(feet)) return null;
        if (!level.getBlockState(feet).isAir()) return null;
        return feet;
    }

    private static boolean movable(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity() || !state.blocksMotion()) return false;
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F) return false;
        return state.getBlock().getExplosionResistance() < 1000.0F;
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 flat = value == null ? Vec3.ZERO : new Vec3(value.x, 0.0, value.z);
        return flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }
}
