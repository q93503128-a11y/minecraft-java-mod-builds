package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

final class VillageSimpleBuildingBuilder {
    private VillageSimpleBuildingBuilder() {}

    static void build(ServerLevel level, BlockPos origin, int groundY, VillageBuildingCatalog.Spec spec) {
        int x0 = origin.getX();
        int z0 = origin.getZ();
        int x1 = x0 + spec.width() - 1;
        int z1 = z0 + spec.depth() - 1;
        fill(level, x0, groundY, z0, x1, groundY, z1, Blocks.STONE_BRICKS);
        fill(level, x0, groundY + 1, z0, x1, groundY + 1, z1, Blocks.SPRUCE_PLANKS);
        for (int y = 2; y <= spec.height(); y++) {
            for (int x = x0; x <= x1; x++) {
                put(level, x, groundY + y, z0, wall(spec, x - x0, y));
                put(level, x, groundY + y, z1, wall(spec, x - x0, y));
            }
            for (int z = z0; z <= z1; z++) {
                put(level, x0, groundY + y, z, wall(spec, z - z0, y));
                put(level, x1, groundY + y, z, wall(spec, z - z0, y));
            }
        }
        int door = x0 + spec.width() / 2;
        fill(level, door, groundY + 2, z0, door + 1, groundY + 4, z0, Blocks.AIR);
        for (int x = x0 + 4; x <= x1 - 4; x += 5) {
            put(level, x, groundY + 4, z0, Blocks.GLASS_PANE);
            put(level, x, groundY + 4, z1, Blocks.GLASS_PANE);
        }
        int half = (spec.depth() + 1) / 2;
        for (int step = 0; step <= half; step++) {
            int left = z0 - 2 + step;
            int right = z1 + 2 - step;
            int y = groundY + spec.height() + 1 + step;
            if (left > right) break;
            fill(level, x0 - 2, y, left, x1 + 2, y, left, spec.roof());
            fill(level, x0 - 2, y, right, x1 + 2, y, right, spec.roof());
            for (int z = left + 1; z < right; z++) {
                put(level, x0, y - 1, z, spec.panel());
                put(level, x1, y - 1, z, spec.panel());
            }
        }
    }

    static void clear(ServerLevel level, BlockPos center, VillageBuildingCatalog.Spec spec) {
        int groundY = center.getY() - 1;
        fill(level,
                center.getX() + spec.dx() - 2, groundY + 1, center.getZ() + spec.dz() - 2,
                center.getX() + spec.dx() + spec.width() + 1,
                groundY + spec.height() + spec.depth() / 2 + 6,
                center.getZ() + spec.dz() + spec.depth() + 1,
                Blocks.AIR);
    }

    private static Block wall(VillageBuildingCatalog.Spec spec, int index, int y) {
        return index % 5 == 0 || y == 2 || y == 6 ? Blocks.STRIPPED_SPRUCE_WOOD : spec.panel();
    }

    private static void fill(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                    put(level, x, y, z, block);
    }

    private static void put(ServerLevel level, int x, int y, int z, Block block) {
        VillageFortressTerrain.set(level, new BlockPos(x, y, z), block);
    }
}
