package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

final class VillageFortressTerrain {
    private static final int TERRAFORM_RADIUS = 70;

    private VillageFortressTerrain() {}

    static void buildBase(ServerLevel level, BlockPos center) {
        int groundY = center.getY() - 1;
        terraform(level, center, groundY);
        buildRoads(level, center, groundY);
        buildWalls(level, center, groundY);
        buildTower(level, center.offset(-58, 0, -58), groundY);
        buildTower(level, center.offset(58, 0, -58), groundY);
        buildTower(level, center.offset(-58, 0, 58), groundY);
        buildTower(level, center.offset(58, 0, 58), groundY);
        buildMarketAndBell(level, center, groundY);
        buildLamps(level, center, groundY);
    }

    static void rebuildNorthGate(ServerLevel level, BlockPos center) {
        buildNorthGate(level, center, center.getY() - 1);
    }

    static void destroyNorthGate(ServerLevel level, BlockPos center) {
        int groundY = center.getY() - 1;
        for (int x = center.getX() - 7; x <= center.getX() + 7; x++) {
            for (int z = center.getZ() - 60; z <= center.getZ() - 53; z++) {
                for (int y = groundY + 1; y <= groundY + 11; y++) {
                    set(level, new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    private static void terraform(ServerLevel level, BlockPos center, int groundY) {
        for (int dx = -TERRAFORM_RADIUS; dx <= TERRAFORM_RADIUS; dx++) {
            for (int dz = -TERRAFORM_RADIUS; dz <= TERRAFORM_RADIUS; dz++) {
                set(level, new BlockPos(center.getX() + dx, groundY - 2, center.getZ() + dz), Blocks.DIRT);
                set(level, new BlockPos(center.getX() + dx, groundY - 1, center.getZ() + dz), Blocks.DIRT);
                set(level, new BlockPos(center.getX() + dx, groundY, center.getZ() + dz), Blocks.GRASS_BLOCK);
                for (int y = 1; y <= 28; y++) {
                    set(level, new BlockPos(center.getX() + dx, groundY + y, center.getZ() + dz), Blocks.AIR);
                }
            }
        }
    }

    private static void buildRoads(ServerLevel level, BlockPos center, int groundY) {
        for (int z = -58; z <= 49; z++) {
            for (int width = -3; width <= 3; width++) {
                set(level, new BlockPos(center.getX() + width, groundY, center.getZ() + z), Blocks.PACKED_MUD);
            }
        }
        for (int x = -53; x <= 53; x++) {
            for (int width = -2; width <= 2; width++) {
                set(level, new BlockPos(center.getX() + x, groundY, center.getZ() + width), Blocks.PACKED_MUD);
            }
        }
        for (int dx = -16; dx <= 16; dx++) {
            for (int dz = -16; dz <= 16; dz++) {
                if (dx * dx + dz * dz <= 256) {
                    set(level, new BlockPos(center.getX() + dx, groundY, center.getZ() + dz),
                            ((dx + dz) & 3) == 0 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS);
                }
            }
        }
    }

    private static void buildWalls(ServerLevel level, BlockPos center, int groundY) {
        for (int dx = -58; dx <= 58; dx++) {
            for (int dz = -58; dz <= 58; dz++) {
                boolean edge = Math.abs(dx) >= 56 || Math.abs(dz) >= 56;
                boolean northGate = dz <= -56 && Math.abs(dx) <= 6;
                if (!edge || northGate) continue;
                for (int y = 1; y <= 8; y++) {
                    set(level, new BlockPos(center.getX() + dx, groundY + y, center.getZ() + dz),
                            y <= 2 || y == 8 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE);
                }
                if (((dx + dz) & 1) == 0) {
                    set(level, new BlockPos(center.getX() + dx, groundY + 9, center.getZ() + dz), Blocks.STONE_BRICKS);
                }
            }
        }
        buildNorthGate(level, center, groundY);
        set(level, center.offset(8, 2, -49), Blocks.STONECUTTER);
    }

    private static void buildNorthGate(ServerLevel level, BlockPos center, int groundY) {
        for (int side : new int[]{-10, 10}) {
            for (int xOffset = -3; xOffset <= 3; xOffset++) {
                for (int zOffset = -2; zOffset <= 2; zOffset++) {
                    for (int y = 1; y <= 12; y++) {
                        set(level,
                                new BlockPos(center.getX() + side + xOffset, groundY + y, center.getZ() - 58 + zOffset),
                                y >= 9 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE);
                    }
                }
            }
        }
        for (int x = -10; x <= 10; x++) {
            for (int y = 8; y <= 11; y++) {
                set(level, new BlockPos(center.getX() + x, groundY + y, center.getZ() - 58), Blocks.STONE_BRICKS);
            }
        }
        for (int x = -5; x <= 5; x++) {
            for (int y = 1; y <= 7; y++) {
                set(level, new BlockPos(center.getX() + x, groundY + y, center.getZ() - 57), Blocks.DARK_OAK_PLANKS);
            }
        }
    }

    private static void buildTower(ServerLevel level, BlockPos corner, int groundY) {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                boolean shell = Math.abs(dx) >= 5 || Math.abs(dz) >= 5;
                set(level, new BlockPos(corner.getX() + dx, groundY, corner.getZ() + dz), Blocks.STONE_BRICKS);
                if (!shell) continue;
                for (int y = 1; y <= 14; y++) {
                    set(level, new BlockPos(corner.getX() + dx, groundY + y, corner.getZ() + dz),
                            y <= 3 || y >= 12 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE);
                }
            }
        }
    }

    private static void buildMarketAndBell(ServerLevel level, BlockPos center, int groundY) {
        set(level, center.offset(0, 1, 0), Blocks.STONE_BRICKS);
        set(level, center.offset(0, 2, 0), Blocks.BELL);
        for (int side : new int[]{-1, 1}) {
            int x = side * 20;
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    set(level, center.offset(x + dx, 1, dz), Blocks.SPRUCE_PLANKS);
                }
            }
            set(level, center.offset(x, 2, 0), side < 0 ? Blocks.BARREL : Blocks.CRAFTING_TABLE);
        }
    }

    private static void buildLamps(ServerLevel level, BlockPos center, int groundY) {
        int[][] offsets = {{-14,-14},{14,-14},{-14,14},{14,14},{0,-35},{0,35},{-38,0},{38,0}};
        for (int[] offset : offsets) {
            BlockPos base = new BlockPos(center.getX() + offset[0], groundY + 1, center.getZ() + offset[1]);
            for (int y = 0; y < 4; y++) set(level, base.above(y), Blocks.STRIPPED_SPRUCE_WOOD);
            set(level, base.above(4), Blocks.LANTERN);
        }
    }

    static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlockAndUpdate(pos, block.defaultBlockState());
    }
}
