package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;

final class VillageFortressTerrain {
    private static final int TERRAFORM_RADIUS = 86;
    private static final int WALL_RADIUS = VillageWorldSystem.FORTRESS_RADIUS;
    private static final int ROAD_HALF_WIDTH = 4;
    private static final int WALL_THICKNESS = 5;
    private static final int WALL_TOP_Y = 9;
    private static final int GATE_HALF_WIDTH = 9;
    private static final int GATE_HEIGHT = 8;

    private VillageFortressTerrain() {
    }

    static void buildBase(ServerLevel level, BlockPos center) {
        int groundY = center.getY() - 1;
        terraform(level, center, groundY);
        buildRoads(level, center, groundY);
        buildWalls(level, center, groundY);
        buildDefenderGalleries(level, center, groundY);
        buildTower(level, center.offset(-WALL_RADIUS, 0, -WALL_RADIUS), groundY);
        buildTower(level, center.offset(WALL_RADIUS, 0, -WALL_RADIUS), groundY);
        buildTower(level, center.offset(-WALL_RADIUS, 0, WALL_RADIUS), groundY);
        buildTower(level, center.offset(WALL_RADIUS, 0, WALL_RADIUS), groundY);
        clearMainAvenue(level, center, groundY);
        buildWallAccess(level, center, groundY);
        buildGateControl(level, center, groundY);
        buildCentralBell(level, center, groundY);
        buildLamps(level, center, groundY);
    }

    static void rebuildNorthGate(ServerLevel level, BlockPos center) {
        int groundY = center.getY() - 1;
        buildNorthGate(level, center, groundY);
        buildDefenderGalleries(level, center, groundY);
        clearMainAvenue(level, center, groundY);
        buildWallAccess(level, center, groundY);
        buildGateControl(level, center, groundY);
    }

    static void restoreCentralBell(ServerLevel level, BlockPos center) {
        buildCentralBell(level, center, center.getY() - 1);
    }

    static BlockPos centralBellPosition(BlockPos center) {
        return center.above();
    }

    static boolean isCentralBell(BlockPos center, BlockPos clicked) {
        return centralBellPosition(center).equals(clicked);
    }

    static void destroyNorthGate(ServerLevel level, BlockPos center) {
        int groundY = center.getY() - 1;
        int gateZ = center.getZ() - WALL_RADIUS;
        for (int x = center.getX() - 12; x <= center.getX() + 12; x++) {
            for (int z = gateZ - 2; z <= gateZ + 4; z++) {
                for (int y = groundY + 1; y <= groundY + 13; y++) {
                    set(level, new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        for (int side : new int[]{-11, 11}) {
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
        setGateControlPowered(level, center, false);
        clearPassageFloor(level, center, groundY);
    }

    static void openNorthGate(ServerLevel level, BlockPos center) {
        int groundY = center.getY() - 1;
        int gateZ = center.getZ() - WALL_RADIUS + 1;
        for (int x = -GATE_HALF_WIDTH; x <= GATE_HALF_WIDTH; x++) {
            for (int y = 1; y <= GATE_HEIGHT; y++) {
                set(level, new BlockPos(center.getX() + x, groundY + y, gateZ), Blocks.AIR);
            }
        }
        setGateControlPowered(level, center, true);
    }

    static void closeNorthGate(ServerLevel level, BlockPos center) {
        int groundY = center.getY() - 1;
        int gateZ = center.getZ() - WALL_RADIUS + 1;
        for (int x = -GATE_HALF_WIDTH; x <= GATE_HALF_WIDTH; x++) {
            for (int y = 1; y <= GATE_HEIGHT; y++) {
                boolean frame = x == -GATE_HALF_WIDTH
                        || x == 0
                        || x == GATE_HALF_WIDTH
                        || y == 1
                        || y == GATE_HEIGHT;
                Block material = frame ? Blocks.STRIPPED_DARK_OAK_WOOD : Blocks.DARK_OAK_PLANKS;
                set(level, new BlockPos(center.getX() + x, groundY + y, gateZ), material);
            }
        }
        setGateControlPowered(level, center, false);
    }

    static BlockPos gateControlPosition(BlockPos center) {
        return center.offset(13, 1, -WALL_RADIUS + 11);
    }

    static boolean isGateControl(BlockPos center, BlockPos clicked) {
        return gateControlPosition(center).equals(clicked);
    }

    static boolean isNorthGatePassable(ServerLevel level, BlockPos center) {
        int groundY = center.getY() - 1;
        int gateZ = center.getZ() - WALL_RADIUS + 1;
        int openColumns = 0;
        for (int x = center.getX() - GATE_HALF_WIDTH; x <= center.getX() + GATE_HALF_WIDTH; x++) {
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
        return openColumns >= 5;
    }

    private static void terraform(ServerLevel level, BlockPos center, int groundY) {
        for (int dx = -TERRAFORM_RADIUS; dx <= TERRAFORM_RADIUS; dx++) {
            for (int dz = -TERRAFORM_RADIUS; dz <= TERRAFORM_RADIUS; dz++) {
                BlockPos column = new BlockPos(center.getX() + dx, groundY, center.getZ() + dz);
                set(level, column.below(2), Blocks.DIRT);
                set(level, column.below(), Blocks.DIRT);
                set(level, column, Blocks.GRASS_BLOCK);
                for (int y = 1; y <= 30; y++) {
                    set(level, column.above(y), Blocks.AIR);
                }
            }
        }
    }

    private static void buildRoads(ServerLevel level, BlockPos center, int groundY) {
        for (int z = -WALL_RADIUS - 8; z <= 34; z++) {
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
        buildHorizontalWall(level, center, groundY, -WALL_RADIUS, true);
        buildHorizontalWall(level, center, groundY, WALL_RADIUS - WALL_THICKNESS + 1, false);
        buildVerticalWall(level, center, groundY, -WALL_RADIUS);
        buildVerticalWall(level, center, groundY, WALL_RADIUS - WALL_THICKNESS + 1);
        buildNorthGate(level, center, groundY);
    }

    private static void buildHorizontalWall(
            ServerLevel level,
            BlockPos center,
            int groundY,
            int startZ,
            boolean north) {
        for (int dx = -WALL_RADIUS; dx <= WALL_RADIUS; dx++) {
            if (north && Math.abs(dx) <= 15) {
                continue;
            }
            for (int offset = 0; offset < WALL_THICKNESS; offset++) {
                int z = center.getZ() + startZ + offset;
                for (int y = 1; y <= WALL_TOP_Y; y++) {
                    Block material = y <= 2 || y >= 8 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE;
                    set(level, new BlockPos(center.getX() + dx, groundY + y, z), material);
                }
            }
            if (Math.floorMod(dx, 6) == 0) {
                for (int offset = 0; offset < WALL_THICKNESS; offset++) {
                    int z = center.getZ() + startZ + offset;
                    set(level, new BlockPos(center.getX() + dx, groundY + 3, z), Blocks.AIR);
                }
                int stepZ = center.getZ() + (north ? startZ + WALL_THICKNESS : startZ - 1);
                set(level, new BlockPos(center.getX() + dx, groundY + 1, stepZ), Blocks.STONE_BRICKS);
            }
            if (Math.floorMod(dx, 3) != 1) {
                int outerZ = center.getZ() + (north ? startZ : startZ + WALL_THICKNESS - 1);
                int innerZ = center.getZ() + (north ? startZ + WALL_THICKNESS - 1 : startZ);
                set(level, new BlockPos(center.getX() + dx, groundY + WALL_TOP_Y + 1, outerZ), Blocks.STONE_BRICKS);
                set(level, new BlockPos(center.getX() + dx, groundY + WALL_TOP_Y + 1, innerZ), Blocks.STONE_BRICKS);
            }
        }
    }

    private static void buildVerticalWall(ServerLevel level, BlockPos center, int groundY, int startX) {
        for (int dz = -WALL_RADIUS; dz <= WALL_RADIUS; dz++) {
            for (int offset = 0; offset < WALL_THICKNESS; offset++) {
                int x = center.getX() + startX + offset;
                for (int y = 1; y <= WALL_TOP_Y; y++) {
                    Block material = y <= 2 || y >= 8 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE;
                    set(level, new BlockPos(x, groundY + y, center.getZ() + dz), material);
                }
            }
            if (Math.floorMod(dz, 6) == 0) {
                for (int offset = 0; offset < WALL_THICKNESS; offset++) {
                    int x = center.getX() + startX + offset;
                    set(level, new BlockPos(x, groundY + 3, center.getZ() + dz), Blocks.AIR);
                }
                int stepX = center.getX() + (startX < 0 ? startX + WALL_THICKNESS : startX - 1);
                set(level, new BlockPos(stepX, groundY + 1, center.getZ() + dz), Blocks.STONE_BRICKS);
            }
            if (Math.floorMod(dz, 3) != 1) {
                int outerX = center.getX() + (startX < 0 ? startX : startX + WALL_THICKNESS - 1);
                int innerX = center.getX() + (startX < 0 ? startX + WALL_THICKNESS - 1 : startX);
                set(level, new BlockPos(outerX, groundY + WALL_TOP_Y + 1, center.getZ() + dz), Blocks.STONE_BRICKS);
                set(level, new BlockPos(innerX, groundY + WALL_TOP_Y + 1, center.getZ() + dz), Blocks.STONE_BRICKS);
            }
        }
    }

    private static void buildDefenderGalleries(ServerLevel level, BlockPos center, int groundY) {
        int floorY = groundY + WALL_TOP_Y;
        for (int offset = -WALL_RADIUS; offset <= WALL_RADIUS; offset++) {
            boolean murderHole = Math.floorMod(offset, 4) == 0;
            for (int outward = 0; outward <= 2; outward++) {
                if (outward == 2 && murderHole) continue;
                set(level, new BlockPos(center.getX() + offset, floorY,
                        center.getZ() - WALL_RADIUS - outward), Blocks.STONE_BRICKS);
                set(level, new BlockPos(center.getX() + offset, floorY,
                        center.getZ() + WALL_RADIUS + outward), Blocks.STONE_BRICKS);
                set(level, new BlockPos(center.getX() - WALL_RADIUS - outward, floorY,
                        center.getZ() + offset), Blocks.STONE_BRICKS);
                set(level, new BlockPos(center.getX() + WALL_RADIUS + outward, floorY,
                        center.getZ() + offset), Blocks.STONE_BRICKS);
            }
            if (!murderHole) {
                set(level, new BlockPos(center.getX() + offset, floorY + 1,
                        center.getZ() - WALL_RADIUS - 2), Blocks.STONE_BRICK_WALL);
                set(level, new BlockPos(center.getX() + offset, floorY + 1,
                        center.getZ() + WALL_RADIUS + 2), Blocks.STONE_BRICK_WALL);
                set(level, new BlockPos(center.getX() - WALL_RADIUS - 2, floorY + 1,
                        center.getZ() + offset), Blocks.STONE_BRICK_WALL);
                set(level, new BlockPos(center.getX() + WALL_RADIUS + 2, floorY + 1,
                        center.getZ() + offset), Blocks.STONE_BRICK_WALL);
            }
        }
    }

    private static void buildNorthGate(ServerLevel level, BlockPos center, int groundY) {
        int gateZ = center.getZ() - WALL_RADIUS;
        for (int side : new int[]{-13, 13}) {
            for (int xOffset = -3; xOffset <= 3; xOffset++) {
                for (int zOffset = 0; zOffset < WALL_THICKNESS + 2; zOffset++) {
                    for (int y = 1; y <= 14; y++) {
                        Block material = y >= 10 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE;
                        set(level,
                                new BlockPos(center.getX() + side + xOffset, groundY + y, gateZ + zOffset),
                                material);
                    }
                }
            }
        }
        for (int x = -13; x <= 13; x++) {
            for (int y = 9; y <= 12; y++) {
                set(level, new BlockPos(center.getX() + x, groundY + y, gateZ + 1), Blocks.STONE_BRICKS);
            }
        }
        for (int x : new int[]{-8, 8}) {
            set(level, new BlockPos(center.getX() + x, groundY + 10, gateZ + 3), Blocks.GLOWSTONE);
        }
        closeNorthGate(level, center);
        clearPassageFloor(level, center, groundY);
    }

    private static void buildGateControl(ServerLevel level, BlockPos center, int groundY) {
        BlockPos control = gateControlPosition(center);
        set(level, control.below(2), Blocks.STONE_BRICKS);
        set(level, control.below(), Blocks.CHISELED_STONE_BRICKS);
        level.setBlockAndUpdate(
                control,
                Blocks.LEVER.defaultBlockState()
                        .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                        .setValue(LeverBlock.FACING, Direction.NORTH)
                        .setValue(LeverBlock.POWERED, false));
        set(level, control.above(), Blocks.AIR);
    }

    private static void setGateControlPowered(ServerLevel level, BlockPos center, boolean powered) {
        BlockPos control = gateControlPosition(center);
        if (level.getBlockState(control).is(Blocks.LEVER)) {
            level.setBlockAndUpdate(
                    control,
                    level.getBlockState(control).setValue(LeverBlock.POWERED, powered));
        }
    }

    private static void buildWallAccess(ServerLevel level, BlockPos center, int groundY) {
        // Preserve the original north-gate access lanes while giving the other three walls
        // their own direct routes. Side/rear lanes align with the authored wall-top defense zones.
        for (int lane : new int[]{-25, 25}) {
            buildWallAccessRamp(level, center, groundY, Direction.NORTH, lane);
        }
        for (int lane : new int[]{-34, 34}) {
            buildWallAccessRamp(level, center, groundY, Direction.SOUTH, lane);
            buildWallAccessRamp(level, center, groundY, Direction.WEST, lane);
            buildWallAccessRamp(level, center, groundY, Direction.EAST, lane);
        }
    }

    private static void buildWallAccessRamp(
            ServerLevel level, BlockPos center, int groundY, Direction outward, int lane) {
        Direction sideways = outward.getClockWise();
        int stairStart = WALL_RADIUS - 14;
        for (int step = 0; step < WALL_TOP_Y; step++) {
            BlockPos row = center.relative(outward, stairStart + step).relative(sideways, lane);
            int y = groundY + 1 + step;
            for (int width = -2; width <= 2; width++) {
                BlockPos column = row.relative(sideways, width);
                BlockPos stairPos = new BlockPos(column.getX(), y, column.getZ());
                for (int supportY = groundY + 1; supportY < y; supportY++) {
                    set(level, new BlockPos(stairPos.getX(), supportY, stairPos.getZ()), Blocks.STONE_BRICKS);
                }
                level.setBlockAndUpdate(
                        stairPos,
                        Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, outward));
                for (int clearY = 1; clearY <= 3; clearY++) {
                    set(level, stairPos.above(clearY), Blocks.AIR);
                }
            }
        }

        int landingStart = WALL_RADIUS - 6;
        int landingEnd = WALL_RADIUS - 1;
        for (int distance = landingStart; distance <= landingEnd; distance++) {
            BlockPos row = center.relative(outward, distance).relative(sideways, lane);
            for (int width = -3; width <= 3; width++) {
                BlockPos column = row.relative(sideways, width);
                BlockPos landing = new BlockPos(column.getX(), groundY + WALL_TOP_Y, column.getZ());
                set(level, landing, Blocks.STONE_BRICKS);
                for (int clearY = 1; clearY <= 3; clearY++) {
                    set(level, landing.above(clearY), Blocks.AIR);
                }
            }
        }
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
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                set(level, new BlockPos(corner.getX() + dx, groundY + 13, corner.getZ() + dz), Blocks.STONE_BRICKS);
            }
        }
        set(level, corner.offset(0, 14, 0), Blocks.GLOWSTONE);
    }

    private static void buildCentralBell(ServerLevel level, BlockPos center, int groundY) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                set(level, new BlockPos(center.getX() + x, groundY, center.getZ() + z), Blocks.CHISELED_STONE_BRICKS);
            }
        }
        set(level, new BlockPos(center.getX(), groundY + 1, center.getZ()), Blocks.STONE_BRICKS);
        set(level, centralBellPosition(center), Blocks.BELL);
    }

    private static void buildLamps(ServerLevel level, BlockPos center, int groundY) {
        int[][] offsets = {
                {-16, -22}, {16, -22}, {-16, 22}, {16, 22},
                {-8, -48}, {8, -48}, {-8, 30}, {8, 30},
                {-36, -7}, {36, -7}, {-36, 9}, {36, 9},
                {-52, -34}, {52, -34}, {-52, 34}, {52, 34}
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
        for (int z = -WALL_RADIUS + 4; z <= 35; z++) {
            for (int x = -ROAD_HALF_WIDTH; x <= ROAD_HALF_WIDTH; x++) {
                for (int y = 1; y <= 12; y++) {
                    set(level, center.offset(x, y, z), Blocks.AIR);
                }
            }
        }
        clearPassageFloor(level, center, groundY);
    }

    private static void clearPassageFloor(ServerLevel level, BlockPos center, int groundY) {
        for (int z = -WALL_RADIUS - 6; z <= -WALL_RADIUS + 12; z++) {
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
