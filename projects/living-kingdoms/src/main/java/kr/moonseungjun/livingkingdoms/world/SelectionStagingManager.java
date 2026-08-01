package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

/** Keeps first-time players out of the void while origin selection and capital construction begin. */
public final class SelectionStagingManager {
    private static final int CENTER_X = 0;
    private static final int CENTER_Z = 0;
    private static final int FLOOR_Y = 96;
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;
    private static final BlockPos MARKER = new BlockPos(CENTER_X, FLOOR_Y - 5, CENTER_Z);

    private SelectionStagingManager() {
    }

    public static void ensure(ServerPlayer player) {
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) return;
        buildIfMissing(realm);

        boolean wrongDimension = !player.level().dimension().equals(StarterRealmManager.REALM_KEY);
        double dx = player.getX() - (CENTER_X + 0.5D);
        double dz = player.getZ() - (CENTER_Z + 0.5D);
        boolean outside = dx * dx + dz * dz > 121.0D;
        boolean unsafeHeight = player.getY() < FLOOR_Y - 2 || player.getY() > FLOOR_Y + 12;
        if (wrongDimension || outside || unsafeHeight) {
            player.teleportTo(realm, CENTER_X + 0.5D, FLOOR_Y + 1.0D, CENTER_Z + 0.5D,
                    Set.<Relative>of(), 0.0F, 0.0F, true);
        }
        player.fallDistance = 0.0F;
    }

    private static void buildIfMissing(ServerLevel level) {
        if (level.getBlockState(MARKER).is(Blocks.LODESTONE)) return;

        for (int x = CENTER_X - 9; x <= CENTER_X + 9; x++) {
            for (int z = CENTER_Z - 9; z <= CENTER_Z + 9; z++) {
                BlockPos floor = new BlockPos(x, FLOOR_Y, z);
                boolean edge = Math.abs(x - CENTER_X) == 9 || Math.abs(z - CENTER_Z) == 9;
                set(level, floor.below(2), Blocks.STONE_BRICKS);
                set(level, floor.below(), Blocks.STONE_BRICKS);
                set(level, floor, ((x - CENTER_X) % 6 == 0 && (z - CENTER_Z) % 6 == 0)
                        ? Blocks.SEA_LANTERN : Blocks.POLISHED_ANDESITE);
                for (int y = 1; y <= 7; y++) set(level, floor.above(y), Blocks.AIR);
                if (edge) set(level, floor.above(), Blocks.STONE_BRICK_WALL);
            }
        }

        for (int[] corner : new int[][]{{-7, -7}, {7, -7}, {-7, 7}, {7, 7}}) {
            int x = CENTER_X + corner[0];
            int z = CENTER_Z + corner[1];
            for (int y = 1; y <= 4; y++) set(level, new BlockPos(x, FLOOR_Y + y, z), Blocks.SPRUCE_FENCE);
            set(level, new BlockPos(x, FLOOR_Y + 5, z), Blocks.LANTERN);
        }

        set(level, new BlockPos(CENTER_X, FLOOR_Y, CENTER_Z - 7), Blocks.CHISELED_STONE_BRICKS);
        set(level, MARKER, Blocks.LODESTONE);
    }

    private static void set(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block block) {
        if (pos.getY() < level.getMinY() || pos.getY() >= level.getMaxY()) return;
        if (level.getBlockState(pos).getBlock() == block) return;
        level.setBlock(pos, block.defaultBlockState(), UPDATE_FLAGS);
    }
}
