package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

final class VillageSimpleBuildingBuilder {
    private VillageSimpleBuildingBuilder() {
    }

    static void build(ServerLevel level, BlockPos origin, int groundY, VillageBuildingCatalog.Spec spec) {
        if (spec.width() >= 40) {
            buildTownHall(level, origin, groundY, spec);
        } else {
            buildFacility(level, origin, groundY, spec);
        }
    }

    private static void buildTownHall(
            ServerLevel level,
            BlockPos origin,
            int groundY,
            VillageBuildingCatalog.Spec spec) {
        int x0 = origin.getX();
        int z0 = origin.getZ();
        int x1 = x0 + spec.width() - 1;
        int z1 = z0 + spec.depth() - 1;
        int centerX = (x0 + x1) / 2;

        clearVolume(level, x0 - 4, groundY + 1, z0 - 5,
                x1 + 4, groundY + 26, z1 + 4);
        fill(level, x0 - 1, groundY, z0 - 1, x1 + 1, groundY, z1 + 1, Blocks.STONE_BRICKS);
        fill(level, x0, groundY + 1, z0, x1, groundY + 1, z1, Blocks.POLISHED_ANDESITE);

        buildFramedShell(level, x0, z0, x1, z1, groundY, 13, spec.panel(), true);
        fill(level, x0 + 1, groundY + 7, z0 + 1, x1 - 1, groundY + 7, z1 - 1, Blocks.SPRUCE_PLANKS);
        clearVolume(level, centerX - 8, groundY + 7, z0 + 6,
                centerX + 8, groundY + 7, z1 - 5);

        openDoubleDoor(level, centerX, groundY, z0, Direction.NORTH);
        buildGrandPorch(level, centerX, groundY, z0);
        buildTallWindows(level, x0, z0, x1, z1, groundY);
        buildSteppedRoof(level, x0, z0, x1, z1, groundY + 14, spec.roof(), 2);

        for (int x = centerX - 11; x <= centerX + 11; x++) {
            put(level, x, groundY + 2, z0 + 19, Blocks.DARK_OAK_PLANKS);
        }
        for (int x : new int[]{centerX - 12, centerX + 12}) {
            for (int y = groundY + 2; y <= groundY + 12; y++) {
                put(level, x, y, z0 + 19, Blocks.STRIPPED_DARK_OAK_WOOD);
            }
        }

        for (int side : new int[]{-1, 1}) {
            int stairX = side < 0 ? x0 + 5 : x1 - 5;
            for (int step = 0; step < 6; step++) {
                fill(level,
                        stairX - 1, groundY + 2 + step, z1 - 5 - step,
                        stairX + 1, groundY + 2 + step, z1 - 5 - step,
                        Blocks.STONE_BRICKS);
            }
        }
    }

    private static void buildFacility(
            ServerLevel level,
            BlockPos origin,
            int groundY,
            VillageBuildingCatalog.Spec spec) {
        int x0 = origin.getX();
        int z0 = origin.getZ();
        int x1 = x0 + spec.width() - 1;
        int z1 = z0 + spec.depth() - 1;

        clearVolume(level, x0 - 3, groundY + 1, z0 - 3,
                x1 + 3, groundY + 18, z1 + 3);
        fill(level, x0 - 1, groundY, z0 - 1, x1 + 1, groundY, z1 + 1, Blocks.STONE_BRICKS);
        fill(level, x0, groundY + 1, z0, x1, groundY + 1, z1, Blocks.SPRUCE_PLANKS);
        buildFramedShell(level, x0, z0, x1, z1, groundY, 8, spec.panel(), false);
        openDoubleDoor(level, x0, groundY, z0, x1, z1, spec.entranceFacing());
        buildFacilityWindows(level, x0, z0, x1, z1, groundY, spec.entranceFacing());
        buildSteppedRoof(level, x0, z0, x1, z1, groundY + 9, spec.roof(), 1);

        Direction front = spec.entranceFacing();
        Direction side = front.getClockWise();
        BlockPos door = switch (front) {
            case NORTH -> new BlockPos((x0 + x1) / 2, groundY + 1, z0);
            case SOUTH -> new BlockPos((x0 + x1) / 2, groundY + 1, z1);
            case WEST -> new BlockPos(x0, groundY + 1, (z0 + z1) / 2);
            case EAST -> new BlockPos(x1, groundY + 1, (z0 + z1) / 2);
            default -> new BlockPos((x0 + x1) / 2, groundY + 1, (z0 + z1) / 2);
        };
        for (int offset : new int[]{-4, 4}) {
            BlockPos post = door.relative(front, 2).relative(side, offset);
            for (int y = 0; y < 4; y++) {
                put(level, post.getX(), post.getY() + y, post.getZ(), Blocks.STRIPPED_DARK_OAK_WOOD);
            }
            put(level, post.getX(), post.getY() + 4, post.getZ(), Blocks.LANTERN);
        }
    }

    private static void buildFramedShell(
            ServerLevel level,
            int x0,
            int z0,
            int x1,
            int z1,
            int groundY,
            int wallHeight,
            Block panel,
            boolean secondFloor) {
        for (int y = 2; y <= wallHeight; y++) {
            for (int x = x0; x <= x1; x++) {
                put(level, x, groundY + y, z0, framedWall(panel, x - x0, y, secondFloor));
                put(level, x, groundY + y, z1, framedWall(panel, x - x0, y, secondFloor));
            }
            for (int z = z0; z <= z1; z++) {
                put(level, x0, groundY + y, z, framedWall(panel, z - z0, y, secondFloor));
                put(level, x1, groundY + y, z, framedWall(panel, z - z0, y, secondFloor));
            }
        }

        for (int x : new int[]{x0, x1}) {
            for (int z : new int[]{z0, z1}) {
                for (int y = 2; y <= wallHeight; y++) {
                    put(level, x, groundY + y, z, Blocks.STRIPPED_DARK_OAK_WOOD);
                }
            }
        }
    }

    private static Block framedWall(Block panel, int index, int y, boolean secondFloor) {
        if (index % 6 == 0 || y == 2 || (secondFloor && y == 7)) {
            return Blocks.STRIPPED_DARK_OAK_WOOD;
        }
        if (y <= 3) {
            return Blocks.STONE_BRICKS;
        }
        return panel;
    }

    private static void buildGrandPorch(ServerLevel level, int centerX, int groundY, int frontZ) {
        for (int z = frontZ - 4; z <= frontZ - 1; z++) {
            fill(level, centerX - 8, groundY + 1, z, centerX + 8, groundY + 1, z, Blocks.STONE_BRICKS);
        }
        for (int x : new int[]{centerX - 7, centerX - 3, centerX + 3, centerX + 7}) {
            for (int y = groundY + 2; y <= groundY + 8; y++) {
                put(level, x, y, frontZ - 3, Blocks.STRIPPED_DARK_OAK_WOOD);
            }
        }
        fill(level, centerX - 9, groundY + 9, frontZ - 4,
                centerX + 9, groundY + 9, frontZ, Blocks.DEEPSLATE_TILES);
    }

    private static void buildTallWindows(
            ServerLevel level,
            int x0,
            int z0,
            int x1,
            int z1,
            int groundY) {
        for (int x = x0 + 5; x <= x1 - 5; x += 6) {
            for (int y : new int[]{4, 5, 9, 10}) {
                put(level, x, groundY + y, z0, Blocks.GLASS);
                put(level, x, groundY + y, z1, Blocks.GLASS);
            }
        }
        for (int z = z0 + 5; z <= z1 - 5; z += 6) {
            for (int y : new int[]{4, 5, 9, 10}) {
                put(level, x0, groundY + y, z, Blocks.GLASS);
                put(level, x1, groundY + y, z, Blocks.GLASS);
            }
        }
    }

    private static void buildFacilityWindows(
            ServerLevel level,
            int x0,
            int z0,
            int x1,
            int z1,
            int groundY,
            Direction entranceFacing) {
        for (int x = x0 + 4; x <= x1 - 4; x += 6) {
            if (entranceFacing != Direction.NORTH || Math.abs(x - (x0 + x1) / 2) > 3) {
                put(level, x, groundY + 5, z0, Blocks.GLASS);
            }
            if (entranceFacing != Direction.SOUTH || Math.abs(x - (x0 + x1) / 2) > 3) {
                put(level, x, groundY + 5, z1, Blocks.GLASS);
            }
        }
        for (int z = z0 + 4; z <= z1 - 4; z += 6) {
            if (entranceFacing != Direction.WEST || Math.abs(z - (z0 + z1) / 2) > 3) {
                put(level, x0, groundY + 5, z, Blocks.GLASS);
            }
            if (entranceFacing != Direction.EAST || Math.abs(z - (z0 + z1) / 2) > 3) {
                put(level, x1, groundY + 5, z, Blocks.GLASS);
            }
        }
    }

    private static void buildSteppedRoof(
            ServerLevel level,
            int x0,
            int z0,
            int x1,
            int z1,
            int roofBase,
            Block roof,
            int overhang) {
        int north = z0 - overhang;
        int south = z1 + overhang;
        int step = 0;
        while (north + step <= south - step) {
            int y = roofBase + step;
            fill(level, x0 - overhang, y, north + step,
                    x1 + overhang, y, north + step, roof);
            fill(level, x0 - overhang, y, south - step,
                    x1 + overhang, y, south - step, roof);
            step++;
        }
        int ridgeZ0 = north + step - 1;
        int ridgeZ1 = south - step + 1;
        fill(level, x0 - overhang, roofBase + step, ridgeZ0,
                x1 + overhang, roofBase + step, ridgeZ1, roof);
    }

    private static void openDoubleDoor(
            ServerLevel level,
            int centerX,
            int groundY,
            int z,
            Direction facing) {
        placeDoor(level, new BlockPos(centerX - 1, groundY + 2, z), facing, DoorHingeSide.LEFT);
        placeDoor(level, new BlockPos(centerX, groundY + 2, z), facing, DoorHingeSide.RIGHT);
        clearVolume(level, centerX - 1, groundY + 4, z, centerX, groundY + 5, z);
    }

    private static void openDoubleDoor(
            ServerLevel level,
            int x0,
            int groundY,
            int z0,
            int x1,
            int z1,
            Direction facing) {
        switch (facing) {
            case NORTH -> openDoubleDoor(level, (x0 + x1) / 2, groundY, z0, Direction.NORTH);
            case SOUTH -> openDoubleDoor(level, (x0 + x1) / 2, groundY, z1, Direction.SOUTH);
            case WEST -> {
                int centerZ = (z0 + z1) / 2;
                placeDoor(level, new BlockPos(x0, groundY + 2, centerZ - 1), Direction.WEST, DoorHingeSide.LEFT);
                placeDoor(level, new BlockPos(x0, groundY + 2, centerZ), Direction.WEST, DoorHingeSide.RIGHT);
            }
            case EAST -> {
                int centerZ = (z0 + z1) / 2;
                placeDoor(level, new BlockPos(x1, groundY + 2, centerZ - 1), Direction.EAST, DoorHingeSide.RIGHT);
                placeDoor(level, new BlockPos(x1, groundY + 2, centerZ), Direction.EAST, DoorHingeSide.LEFT);
            }
            default -> {
            }
        }
    }

    private static void placeDoor(
            ServerLevel level,
            BlockPos lower,
            Direction facing,
            DoorHingeSide hinge) {
        level.setBlockAndUpdate(
                lower,
                Blocks.DARK_OAK_DOOR.defaultBlockState()
                        .setValue(DoorBlock.FACING, facing)
                        .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                        .setValue(DoorBlock.HINGE, hinge));
        level.setBlockAndUpdate(
                lower.above(),
                Blocks.DARK_OAK_DOOR.defaultBlockState()
                        .setValue(DoorBlock.FACING, facing)
                        .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)
                        .setValue(DoorBlock.HINGE, hinge));
    }

    static void clear(ServerLevel level, BlockPos center, VillageBuildingCatalog.Spec spec) {
        int groundY = center.getY() - 1;
        clearVolume(level,
                center.getX() + spec.dx() - 4, groundY + 1, center.getZ() + spec.dz() - 5,
                center.getX() + spec.dx() + spec.width() + 4,
                groundY + 28,
                center.getZ() + spec.dz() + spec.depth() + 4);
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
                int hash = Math.floorMod(x * 31 + z * 17, 29);
                if (hash == 0 || hash == 8) {
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
    }

    private static void buildBrokenCorner(ServerLevel level, int x, int groundY, int z, int height) {
        for (int y = 1; y <= height; y++) {
            put(level, x, groundY + y, z,
                    y == height ? Blocks.STRIPPED_SPRUCE_WOOD : Blocks.COBBLESTONE);
        }
    }

    private static void clearVolume(
            ServerLevel level,
            int x0,
            int y0,
            int z0,
            int x1,
            int y1,
            int z1) {
        fill(level, x0, y0, z0, x1, y1, z1, Blocks.AIR);
    }

    private static void fill(
            ServerLevel level,
            int x0,
            int y0,
            int z0,
            int x1,
            int y1,
            int z1,
            Block block) {
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
