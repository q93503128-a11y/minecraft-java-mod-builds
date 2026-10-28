package dev.moonseungjun.augmentationsurvival.augment.active;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/** Shared landing-contact check used by both vanilla fall and common damage boundaries. */
public final class TemporaryPlatformLandingProtection {
    private TemporaryPlatformLandingProtection() { }

    public static boolean protects(ServerPlayer player) {
        if (TemporaryPlatformService.cancelsFallDamage(player)) return true;

        AABB box = player.getBoundingBox();
        int minX = (int) Math.floor(box.minX + 1.0E-4D);
        int maxX = (int) Math.floor(box.maxX - 1.0E-4D);
        int minZ = (int) Math.floor(box.minZ + 1.0E-4D);
        int maxZ = (int) Math.floor(box.maxZ - 1.0E-4D);
        int footY = (int) Math.floor(box.minY - 1.0E-3D);
        int fallbackY = (int) Math.floor(box.minY) - 1;

        for (int y : new int[]{footY, fallbackY}) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (player.level().getBlockState(new BlockPos(x, y, z))
                            .is(TemporaryPlatformBlocks.TEMPORARY_PLATFORM)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
