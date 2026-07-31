package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

final class VillageSimpleBuildingBuilder {
    private VillageSimpleBuildingBuilder() {
    }

    static void build(ServerLevel level, BlockPos origin, int groundY, VillageBuildingCatalog.Spec spec) {
        int x0 = origin.getX();
        int z0 = origin.getZ();
        int x1 = x0 + spec.width() - 1;
        int z1 = z0 + spec.depth() - 1;

        fill(level, x0 - 2, groundY + 1, z0 - 2,
                x1 + 2, groundY + spec.height() + spec.depth() / 2 + 5, z1 + 2,
                Blocks.AIR);
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
        for (int x = x0 + 4; x <= x1 - 4; x += 6) {
            put(level, x, groundY + 4, z0, Blocks.GLASS);
            put(level, x, groundY + 4, z1, Blocks.GLASS);
        }
        for (int z = z0 + 4; z <= z1 - 4; z += 6) {
            put(level, x0, groundY + 4, z, Blocks.GLASS);
            put(level, x1, groundY + 4, z, Blocks.GLASS);
        }

        int roofBase = groundY + spec.height() + 1;
        int halfDepth = (spec.depth() + 3) / 2;
        for (int step = 0; step <= halfDepth; step++) {
            int north = z0 - 2 + step;
            int south = z1 + 2 - step;
            if (north > south) {
                break;
            }
            int roofY = roofBase + step;
            fill(level, x0 - 2, roofY, north, x1 + 2, roofY, north, spec.roof());
            fill(level, x0 - 2, roofY, south, x1 + 2, roofY, south, spec.roof());
            if (north == south || north + 1 == south) {
                fill(level, x0 - 2, roofY, north, x1 + 2, roofY, south, spec.roof());
            }
        }

        for (int x : new int[]{x0, x1}) {
            int inset = 0;
            for (int y = roofBase; y <= roofBase + halfDepth; y++) {
                int north = z0 + inset;
                int south = z1 - inset;
                if (north > south) {
                    break;
                }
                fill(level, x, y, north, x, y, south, spec.panel());
                inset++;
            }
        }
    }

    static void clear(ServerLevel level, BlockPos center, VillageBuildingCatalog.Spec spec) {
        int groundY = center.getY() - 1;
        fill(level,
                center.getX() + spec.dx() - 3, groundY + 1, center.getZ() + spec.dz() - 3,
                center.getX() + spec.dx() + spec.width() + 2,
                groundY + spec.height() + spec.depth() / 2 + 7,
                center.getZ() + spec.dz() + spec.depth() + 2,
                Blocks.AIR);
    }

    static void ruin(ServerLevel level, BlockPos center, VillageBuildingCatalog.Spec spec) {
        int groundY = center.getY() - 1;
        int x0 = center.getX() + spec.dx();
        int z0 = center.getZ() + spec.dz();
        int x1 = x0 + spec.width() - 1;
        int z1 = z0 + spec.depth() - 1;

        clear(level, center, spec);
        fill(level, x0, groundY, z0, x1, groundY, z1, Blocks.CRACKED_STONE_BRICKS);

        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int hash = Math.floorMod(x * 31 + z * 17, 23);
                if (hash == 0 || hash == 7) {
                    put(level, x, groundY + 1, z, Blocks.COBBLESTONE);
                } else if (hash == 3) {
                    put(level, x, groundY + 1, z, Blocks.SPRUCE_PLANKS);
                }
            }
        }

        buildBrokenCorner(level, x0, groundY, z0, 4);
        buildBrokenCorner(level, x1, groundY, z0, 3);
        buildBrokenCorner(level, x0, groundY, z1, 2);
        buildBrokenCorner(level, x1, groundY, z1, 4);

        int doorway = x0 + spec.width() / 2;
        for (int x = doorway - 2; x <= doorway + 2; x++) {
            put(level, x, groundY + 1, z0 - 1, Blocks.COBBLESTONE);
        }
    }

    private static void buildBrokenCorner(ServerLevel level, int x, int groundY, int z, int height) {
        for (int y = 1; y <= height; y++) {
            put(level, x, groundY + y, z, y == height ? Blocks.STRIPPED_SPRUCE_WOOD : Blocks.COBBLESTONE);
        }
    }

    private static Block wall(VillageBuildingCatalog.Spec spec, int index, int y) {
        return index % 5 == 0 || y == 2 || y == 6
                ? Blocks.STRIPPED_SPRUCE_WOOD
                : spec.panel();
    }

    private static void fill(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    put(level, x, y, z, block);
                }
            }
        }
    }

    private static void put(ServerLevel level, int x, int y, int z, Block block) {
        VillageFortressTerrain.set(level, new BlockPos(x, y, z), block);
    }
}
