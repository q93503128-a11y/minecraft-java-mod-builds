package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Guarantees that the opening spawn is a clear plaza tile rather than a roof or vanilla heightmap result. */
public final class RadiaSafeSpawn {
    private static final int CX = 0;
    private static final int CZ = 12;
    private static final int GROUND_Y = 65;

    private RadiaSafeSpawn() {}

    public static void place(ServerLevel level, ServerPlayer player) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int x = CX + dx, z = CZ + dz;
                for (int y = GROUND_Y - 3; y < GROUND_Y; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
                }
                level.setBlock(new BlockPos(x, GROUND_Y, z),
                        (Math.abs(dx) + Math.abs(dz) <= 2 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS).defaultBlockState(), 2);
                for (int y = GROUND_Y + 1; y <= GROUND_Y + 7; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        player.setNoGravity(false);
        player.fallDistance = 0.0F;
        player.setDeltaMovement(Vec3.ZERO);
        player.setPos(CX + 0.5, GROUND_Y + 1.0, CZ + 0.5);
        player.setYRot(180.0F);
        player.setXRot(3.0F);
    }
}
