package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Alpha.65 shared ground contract. Ground-target magic must resolve against the actual world
 * surface at the requested X/Z instead of inheriting the caster/aim sample Y coordinate.
 */
public final class GroundTargetResolver {
    private GroundTargetResolver() {}

    /** Returns the top collision/fluid surface at the requested X/Z, preserving exact X/Z. */
    public static Vec3 surface(ServerLevel level, Vec3 desired) {
        if (level == null || desired == null) return desired == null ? Vec3.ZERO : desired;
        int x = (int) Math.floor(desired.x);
        int z = (int) Math.floor(desired.z);
        BlockPos probe = new BlockPos(x,
                Math.max(level.getMinY() + 1, Math.min(level.getMaxY() - 2, (int) Math.floor(desired.y))), z);
        if (!level.hasChunkAt(probe)) return desired;
        for (int y = level.getMaxY() - 2; y > level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!state.blocksMotion() && state.getFluidState().isEmpty()) continue;
            return new Vec3(desired.x, y + 1.01, desired.z);
        }
        return desired;
    }

    /** Finds a two-block-high safe standing cell near a desired point, including terrain far below it. */
    public static Optional<BlockPos> safeStanding(ServerLevel level, Vec3 desired, int horizontalRadius) {
        if (level == null || desired == null) return Optional.empty();
        int baseX = (int) Math.floor(desired.x);
        int baseZ = (int) Math.floor(desired.z);
        int radiusLimit = Math.max(0, horizontalRadius);
        for (int radius = 0; radius <= radiusLimit; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    Optional<BlockPos> found = safeColumn(level, baseX + dx, baseZ + dz, desired.y);
                    if (found.isPresent()) return found;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> safeColumn(ServerLevel level, int x, int z, double preferredY) {
        int preferred = (int) Math.floor(Math.max(level.getMinY() + 2,
                Math.min(level.getMaxY() - 3, preferredY)));
        BlockPos probe = new BlockPos(x, preferred, z);
        if (!level.hasChunkAt(probe)) return Optional.empty();

        // First prefer a standing cell close to the requested elevation.
        for (int d = 0; d <= 16; d++) {
            int[] ys = d == 0 ? new int[]{preferred} : new int[]{preferred + d, preferred - d};
            for (int y : ys) {
                if (validStanding(level, x, y, z)) return Optional.of(new BlockPos(x, y, z));
            }
        }
        // Then search the whole loaded column so high-altitude casts still find the real ground.
        for (int y = level.getMaxY() - 3; y > level.getMinY() + 1; y--) {
            if (validStanding(level, x, y, z)) return Optional.of(new BlockPos(x, y, z));
        }
        return Optional.empty();
    }

    private static boolean validStanding(ServerLevel level, int x, int y, int z) {
        if (y <= level.getMinY() + 1 || y >= level.getMaxY() - 2) return false;
        BlockPos feet = new BlockPos(x, y, z);
        return level.getBlockState(feet.below()).blocksMotion()
                && level.getBlockState(feet).isAir()
                && level.getBlockState(feet.above()).isAir();
    }

    public static Vec3 standing(BlockPos pos) {
        return new Vec3(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
    }

    public static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }
}
