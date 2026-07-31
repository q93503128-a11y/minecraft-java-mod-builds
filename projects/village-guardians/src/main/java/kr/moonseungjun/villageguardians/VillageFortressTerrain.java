package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

final class VillageFortressTerrain {
    private static final int TERRAFORM_RADIUS = 86;
    private static final int WALL_RADIUS = VillageWorldSystem.FORTRESS_RADIUS;
    private static final int ROAD_HALF_WIDTH = 4;

    private VillageFortressTerrain() {
    }

    static void buildBase(ServerLevel level, BlockPos center) {
        int groundY = center.getY() - 1;
        terraform(level, center, groundY);
        buildRoads(level, center, groundY);
        buildWalls(level, center, groundY);
        buildTower(level, center.offset(-WALL_RADIUS, 0, -WALL_RADIUS), groundY);
        buildTower(level, center.offset(WALL_RADIUS, 0, -WALL_RADIUS), groundY);
        buildTower(level, center.offset(-WALL_RADIUS, 0, WALL_RADIUS), groundY);
        buildTower(level, center.offset(WALL_RADIUS, 0, WALL_RADIUS), groundY);
        buildCentralBell(level, center, groundY);
        buildLamps(level, center, groundY);
        clearMainAvenue(level, center, groundY);
    }

    static void rebuildNorthGate(ServerLevel level, BlockPos center) {
        buildNorthGate(level, center, center.getY() - 1);
        clearMainAvenue(level, center, center.getY() - 1);
    }

    static void destroyNorthGate(ServerLevel level, BlockPos center) {
        int groundY = center.getY() - 1;
        int gateZ = center.getZ() - WALL_RADIUS;
        for (int x = center.getX() - 9; x <= center.getX() + 9; x++) {
            for (int z = gateZ - 3; z <= gateZ + 4; z++) {
                for (int y = groundY + 1; y <= groundY + 13; y++) {
                    set(level, new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }

        for (int side : new int[]{-9, 9}) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int hash = Math.floorMod(side * 17 + dx * 11 + dz * 7, 5);
                    if (hash <= 2) {
                        set(level,
                                new BlockPos(center.getX() + side + dx, groundY + 1, gateZ + dz),
                                hash == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.COBBLESTONE);
                    }
                }
            }
        }
        for (int x : new int[]{-7, -6, 6, 7}) {
            set(level, center.offset(x, 0, -WALL_RADIUS + 2), Blocks.COBBLESTONE);
            if ((x & 1) == 0) {
                set(level, center.offset(x, 1, -WALL_RADIUS + 2), Blocks.DARK_OAK_PLANKS);
            }
        }
        clearPassageFloor(level, center, groundY);
    }

    static boolean isNorthGatePassable(ServerLevel level, BlockPos center) {
        int groundY = center.getY() - 1;
        int gateZ = center.getZ() - WALL_RADIUS + 1;
        int openColumns = 0;
        for (int x = center.getX() - 4; x <= center.getX() + 4; x++) {
            boolean open = true;
            for (int y = groundY + 1; y <= groundY + 3; y++) {
                if (!level.getBlockState(new BlockPos(x, y, gateZ)).isAir()) {
                    open = false;
                    break;
                }
            }
            if (open) {
                openColumns++;
            }
        }
        return openColumns >= 3;
    }

    private static void terraform(ServerLevel level, BlockPos center, int groundY) {
        for (int dx = -TERRAFORM_RADIUS; dx <= TERRAFORM_RADIUS; dx++) {
            for (int dz = -TERRAFORM_RADIUS; dz <= TERRAFORM_RADIUS; dz++) {
                BlockPos column = new BlockPos(center.getX() + dx, groundY, center.getZ() + dz);
                set(level, column.below(2), Blocks.DIRT);
                set(level, column.below(), Blocks.DIRT);
                set(level, column, Blocks.GRASS_BLOCK);
                for (int y = 1; y <= 25; y++) {
                    BlockPos clear = column.above(y);
                    if (!level.getBlockState(clear).isAir()) {
                        set(level, clear, Blocks.AIR);
                    }
                }
            }
        }
    }

    private static void buildRoads(ServerLevel level, BlockPos center, int groundY) {
        for (int z = -WALL_RADIUS - 8; z <= 69; z++) {
            for (int width = -ROAD_HALF_WIDTH; width <= ROAD_HALF_WIDTH; width++) {
                Block road = Math.abs(width) == ROAD_HALF_WIDTH
                        ? Blocks.STONE_BRICKS
                        : ((z + width) & 3) == 0 ? Blocks.POLISHED_ANDESITE : Blocks.PACKED_MUD;
                set(level, new BlockPos(center.getX() + width, groundY, center.getZ() + z), road);
            }
        }

        for (int x = -72; x <= 72; x++) {
            for (int width = -3; width <= 3; width++) {
                Block road = Math.abs(width) == 3 ? Blocks.STONE_BRICKS : Blocks.PACKED_MUD;
                set(level, new BlockPos(center.getX() + x, groundY, center.getZ() + width), road);
            }
        }

        for (int dx = -18; dx <= 18; dx++) {
            for (int dz = -18; dz <= 18; dz++) {
                if (dx * dx + dz * dz <= 324) {
                    set(level,
                            new BlockPos(center.getX() + dx, groundY, center.getZ() + dz),
                            ((dx + dz) & 3) == 0 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS);
                }
            }
        }
    }

    private static void buildWalls(ServerLevel level, BlockPos center, int groundY) {
        for (int dx = -WALL_RADIUS; dx <= WALL_RADIUS; dx++) {
            for (int dz = -WALL_RADIUS; dz <= WALL_RADIUS; dz++) {
                boolean edge = Math.abs(dx) >= WALL_RADIUS - 2 || Math.abs(dz) >= WALL_RADIUS - 2;
                boolean northGate = dz <= -WALL_RADIUS + 2 && Math.abs(dx) <= 8;
                if (!edge || northGate) {
                    continue;
                }
                for (int y = 1; y <= 9; y++) {
                    Block wall = y <= 2 || y >= 8 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE;
                    set(level, center.offset(dx, y - 1, dz), wall);
                }
                if (((dx + dz) & 1) == 0) {
                    set(level, center.offset(dx, 9, dz), Blocks.STONE_BRICKS);
                }
            }
        }
        buildNorthGate(level, center, groundY);
        set(level, center.offset(11, 1, -WALL_RADIUS + 8), Blocks.STONECUTTER);
    }

    private static void buildNorthGate(ServerLevel level, BlockPos center, int groundY) {
        int gateZ = center.getZ() - WALL_RADIUS;
        for (int side : new int[]{-12, 12}) {
            for (int xOffset = -3; xOffset <= 3; xOffset++) {
                for (int zOffset = -3; zOffset <= 3; zOffset++) {
                    for (int y = 1; y <= 14; y++) {
                        Block material = y >= 10 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE;
                        set(level,
                                new BlockPos(center.getX() + side + xOffset, groundY + y, gateZ + zOffset),
                                material);
                    }
                }
            }
        }
        for (int x = -12; x <= 12; x++) {
            for (int y = 9; y <= 12; y++) {
                set(level, new BlockPos(center.getX() + x, groundY + y, gateZ), Blocks.STONE_BRICKS);
            }
        }
        for (int x = -7; x <= 7; x++) {
            for (int y = 1; y <= 8; y++) {
                set(level, new BlockPos(center.getX() + x, groundY + y, gateZ + 1), Blocks.DARK_OAK_PLANKS);
            }
        }
        clearPassageFloor(level, center, groundY);
    }

    private static void buildTower(ServerLevel level, BlockPos corner, int groundY) {
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                boolean shell = Math.abs(dx) >= 6 || Math.abs(dz) >= 6;
                set(level, new BlockPos(corner.getX() + dx, groundY, corner.getZ() + dz), Blocks.STONE_BRICKS);
                if (!shell) {
                    continue;
                }
                for (int y = 1; y <= 16; y++) {
                    set(level,
                            new BlockPos(corner.getX() + dx, groundY + y, corner.getZ() + dz),
                            y <= 3 || y >= 13 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE);
                }
            }
        }
    }

    private static void buildCentralBell(ServerLevel level, BlockPos center, int groundY) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                set(level, new BlockPos(center.getX() + x, groundY, center.getZ() + z), Blocks.CHISELED_STONE_BRICKS);
            }
        }
        set(level, new BlockPos(center.getX(), groundY + 1, center.getZ()), Blocks.STONE_BRICKS);
        set(level, new BlockPos(center.getX(), groundY + 2, center.getZ()), Blocks.BELL);
    }

    private static void buildLamps(ServerLevel level, BlockPos center, int groundY) {
        int[][] offsets = {
                {-16, -22}, {16, -22}, {-16, 22}, {16, 22},
                {-8, -48}, {8, -48}, {-8, 32}, {8, 32},
                {-36, -7}, {36, -7}, {-36, 9}, {36, 9}
        };
        for (int[] offset : offsets) {
            if (Math.abs(offset[0]) <= ROAD_HALF_WIDTH + 1) {
                continue;
            }
            BlockPos base = new BlockPos(center.getX() + offset[0], groundY + 1, center.getZ() + offset[1]);
            for (int y = 0; y < 4; y++) {
                set(level, base.above(y), Blocks.STRIPPED_SPRUCE_WOOD);
            }
            set(level, base.above(4), Blocks.LANTERN);
        }
    }

    private static void clearMainAvenue(ServerLevel level, BlockPos center, int groundY) {
        for (int z = -WALL_RADIUS + 4; z <= 37; z++) {
            for (int x = -ROAD_HALF_WIDTH; x <= ROAD_HALF_WIDTH; x++) {
                for (int y = 1; y <= 8; y++) {
                    set(level, center.offset(x, y - 1, z), Blocks.AIR);
                }
            }
        }
        clearPassageFloor(level, center, groundY);
    }

    private static void clearPassageFloor(ServerLevel level, BlockPos center, int groundY) {
        for (int z = -WALL_RADIUS - 5; z <= -WALL_RADIUS + 7; z++) {
            for (int x = -ROAD_HALF_WIDTH; x <= ROAD_HALF_WIDTH; x++) {
                set(level,
                        new BlockPos(center.getX() + x, groundY, center.getZ() + z),
                        Math.abs(x) == ROAD_HALF_WIDTH ? Blocks.STONE_BRICKS : Blocks.PACKED_MUD);
            }
        }
    }

    static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlockAndUpdate(pos, block.defaultBlockState());
    }
}
