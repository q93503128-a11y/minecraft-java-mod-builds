package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

final class VillageStructureShell {
    private VillageStructureShell() {
    }

    static void build(ServerLevel level, BlockPos origin, int groundY, VillageBuildingCatalog.Spec spec) {
        VillageSimpleBuildingBuilder.build(level, origin, groundY, spec);
    }

    static void clear(ServerLevel level, BlockPos center, VillageBuildingCatalog.Spec spec) {
        VillageSimpleBuildingBuilder.clear(level, center, spec);
        if (spec.width() < 40) {
            return;
        }
        int groundY = center.getY() - 1;
        for (int x = center.getX() + spec.dx() - 4;
             x <= center.getX() + spec.dx() + spec.width() + 4;
             x++) {
            for (int z = center.getZ() + spec.dz() - 5;
                 z <= center.getZ() + spec.dz() + spec.depth() + 4;
                 z++) {
                for (int y = groundY + 28; y <= groundY + 40; y++) {
                    VillageFortressTerrain.set(level, new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    static void ruin(ServerLevel level, BlockPos center, VillageBuildingCatalog.Spec spec) {
        VillageSimpleBuildingBuilder.ruin(level, center, spec);
    }
}
