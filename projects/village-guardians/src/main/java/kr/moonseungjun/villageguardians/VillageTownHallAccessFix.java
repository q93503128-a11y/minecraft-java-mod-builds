package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

final class VillageTownHallAccessFix {
    private VillageTownHallAccessFix() {}

    static void apply(ServerLevel level, BlockPos villageCenter) {
        VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(VillageProgressionSystem.Building.TOWN_HALL);
        BlockPos origin = villageCenter.offset(spec.dx(), 0, spec.dz());
        int groundY = villageCenter.getY() - 1;
        int x0 = origin.getX();
        int x1 = x0 + spec.width() - 1;
        int z1 = origin.getZ() + spec.depth() - 1;

        for (int stairX : new int[]{x0 + 5, x1 - 5}) {
            for (int x = stairX - 2; x <= stairX + 2; x++) {
                for (int z = z1 - 9; z <= z1 - 3; z++) {
                    for (int y = groundY + 2; y <= groundY + 10; y++) {
                        level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                }
            }
            for (int step = 0; step < 6; step++) {
                int y = groundY + 2 + step;
                int z = z1 - 4 - step;
                for (int x = stairX - 1; x <= stairX + 1; x++) {
                    level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState());
                }
                level.setBlockAndUpdate(new BlockPos(stairX - 2, y + 1, z), Blocks.DARK_OAK_FENCE.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(stairX + 2, y + 1, z), Blocks.DARK_OAK_FENCE.defaultBlockState());
            }
            for (int x = stairX - 2; x <= stairX + 2; x++) {
                level.setBlockAndUpdate(new BlockPos(x, groundY + 7, z1 - 10), Blocks.SPRUCE_PLANKS.defaultBlockState());
            }
            for (int z = z1 - 10; z <= z1 - 3; z++) {
                level.setBlockAndUpdate(new BlockPos(stairX - 2, groundY + 8, z), Blocks.DARK_OAK_FENCE.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(stairX + 2, groundY + 8, z), Blocks.DARK_OAK_FENCE.defaultBlockState());
            }
        }
    }
}
