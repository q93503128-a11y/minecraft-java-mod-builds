package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Places the player on the authored Radia overlook without re-flattening the surrounding terrain.
 * The hub builder owns topology; this class only guarantees a tiny solid spawn pad and headroom.
 */
public final class RadiaSafeSpawn {
    private RadiaSafeSpawn() {}

    public static void place(ServerLevel level, ServerPlayer player) {
        Vec3 spawn = RadiaHubWorld.spawnPoint();
        int groundY = (int)Math.floor(spawn.y) - 1;
        int cx = (int)Math.floor(spawn.x);
        int cz = (int)Math.floor(spawn.z);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                BlockPos ground = new BlockPos(x, groundY, z);
                var state = level.getBlockState(ground);
                if (state.isAir() || !state.getFluidState().isEmpty()) {
                    level.setBlock(ground, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
                }
                for (int y = groundY + 1; y <= groundY + 4; y++) {
                    BlockPos headroom = new BlockPos(x, y, z);
                    if (!level.getBlockState(headroom).isAir()) {
                        level.setBlock(headroom, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        player.setNoGravity(false);
        player.fallDistance = 0.0F;
        player.setDeltaMovement(Vec3.ZERO);
        player.setPos(spawn.x, spawn.y, spawn.z);
        player.setYRot(180.0F);
        player.setXRot(3.0F);
    }
}
