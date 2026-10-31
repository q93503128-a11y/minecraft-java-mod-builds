package dev.moonseungjun.augmentationsurvival.augment.active;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Prevents Rewind from restoring a historical snapshot inside solid collision. */
public final class RewindSafetyService {
    private static final int[] VERTICAL_OFFSETS = {0, 1, -1, 2, -2, 3, -3, 4, -4};

    private RewindSafetyService() {
    }

    public static TeleportTransition adjust(ServerPlayer player, TeleportTransition transition) {
        ServerLevel level = transition.newLevel();
        Vec3 desired = transition.position();
        if (isSafe(player, level, desired)) return transition;

        for (int radius = 1; radius <= 4; radius++) {
            for (int dy : VERTICAL_OFFSETS) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        Vec3 candidate = desired.add(dx, dy, dz);
                        if (isSafe(player, level, candidate)) return transition.withPosition(candidate);
                    }
                }
            }
        }

        // A rewind that cannot find a safe historical destination must never trap the player.
        return transition.withPosition(player.position());
    }

    public static boolean isSafe(ServerPlayer player, ServerLevel level, Vec3 position) {
        if (position.y < level.getMinY() || position.y >= level.getMaxY() - 1) return false;
        AABB moved = player.getBoundingBox().move(
                position.x - player.getX(),
                position.y - player.getY(),
                position.z - player.getZ()
        );
        return level.noCollision(player, moved);
    }
}
