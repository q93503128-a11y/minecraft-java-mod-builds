package io.github.q93503128.turnbound.session;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Finds or validates an open arena so a field encounter never starts inside a wall/tree. */
final class BattleArenaLocator {
    record Arena(Vec3 center, float facingYaw) {}

    private static final double[][] FORMATION = {
            {-3.0, -2.2}, {-1.0, -2.2}, {1.0, -2.2}, {3.0, -2.2},
            {-3.2, 2.8}, {-1.6, 2.8}, {0.0, 2.8}, {1.6, 2.8}, {3.2, 2.8}
    };

    private BattleArenaLocator() {}

    static Arena locate(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        float yaw = player.getYRot();
        Vec3 forward = forward(yaw);
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 preferred = player.position().add(forward.scale(7.0));

        int[] forwardOffsets = {0, 4, -4, 8, -8, 12, -12};
        int[] lateralOffsets = {0, 4, -4, 8, -8, 12, -12, 16, -16};
        Vec3 best = null;
        int bestScore = Integer.MAX_VALUE;

        outer:
        for (int forwardOffset : forwardOffsets) {
            for (int lateralOffset : lateralOffsets) {
                Vec3 raw = preferred.add(forward.scale(forwardOffset)).add(right.scale(lateralOffset));
                Vec3 candidate = groundCenter(level, raw.x, raw.z);
                int score = score(level, candidate, forward, right);
                if (score < bestScore) {
                    bestScore = score;
                    best = candidate;
                }
                if (score == 0) break outer;
            }
        }

        if (best == null) best = groundCenter(level, preferred.x, preferred.z);
        return new Arena(best, yaw);
    }

    /** Canonical field anchors are exact. If the authored space is obstructed, the encounter must not silently relocate. */
    static Arena fixedIfOpen(ServerPlayer player, Vec3 center, float yaw) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 forward = forward(yaw);
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        return score(level, center, forward, right) == 0 ? new Arena(center, yaw) : null;
    }

    static Vec3 forward(float yaw) {
        double radians = Math.toRadians(yaw);
        return new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
    }

    static Vec3 groundPosition(ServerLevel level, Vec3 position) {
        int x = (int) Math.floor(position.x);
        int z = (int) Math.floor(position.z);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new Vec3(position.x, y, position.z);
    }

    private static Vec3 groundCenter(ServerLevel level, double x, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
        return new Vec3(blockX + 0.5, y, blockZ + 0.5);
    }

    private static int score(ServerLevel level, Vec3 center, Vec3 forward, Vec3 right) {
        int score = pointPenalty(level, center, center.y);
        for (double[] local : FORMATION) {
            Vec3 raw = center.add(right.scale(local[0])).add(forward.scale(local[1]));
            Vec3 grounded = groundPosition(level, raw);
            score += pointPenalty(level, grounded, center.y);
        }

        // Keep the default third-person camera arc clear as well as the combatant slots.
        for (int i = 2; i <= 8; i++) {
            Vec3 cameraSample = center.subtract(forward.scale(i)).add(0.0, 1.7 + i * 0.42, 0.0);
            BlockPos pos = BlockPos.containing(cameraSample);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) score += 8;
            if (!level.getFluidState(pos).isEmpty()) score += 12;
        }
        return score;
    }

    private static int pointPenalty(ServerLevel level, Vec3 point, double centerY) {
        int score = 0;
        if (Math.abs(point.y - centerY) > 1.5) score += 30;
        BlockPos feet = BlockPos.containing(point.x, point.y + 0.05, point.z);
        BlockPos body = feet.above();
        BlockPos head = body.above();
        BlockPos below = feet.below();

        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) score += 80;
        if (!level.getBlockState(body).getCollisionShape(level, body).isEmpty()) score += 80;
        if (!level.getBlockState(head).getCollisionShape(level, head).isEmpty()) score += 50;
        if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(body).isEmpty()) score += 100;
        if (level.getBlockState(below).getCollisionShape(level, below).isEmpty()) score += 25;
        return score;
    }
}
