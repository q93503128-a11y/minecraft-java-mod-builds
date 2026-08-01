package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

final class VillageBuildingEnhancements {
    private static final int WALL_RADIUS = VillageWorldSystem.FORTRESS_RADIUS;
    private static final int WALL_TOP_Y = 9;

    private VillageBuildingEnhancements() {
    }

    static void apply(
            ServerLevel level,
            BlockPos villageCenter,
            VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.WALLS) {
            return;
        }
        VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(building);
        BlockPos origin = villageCenter.offset(spec.dx(), 0, spec.dz());
        connectEntranceToRoad(level, villageCenter, origin, spec);
        switch (building) {
            case TOWN_HALL -> decorateTownHall(level, origin, villageCenter.getY() - 1, spec);
            case SMITHY -> decorateSmithy(level, origin, villageCenter.getY() - 1, spec);
            case SKILL_HALL -> decorateSkillHall(level, origin, villageCenter.getY() - 1, spec);
            case INFIRMARY -> decorateInfirmary(level, origin, villageCenter.getY() - 1, spec);
            case STOREHOUSE -> decorateStorehouse(level, origin, villageCenter.getY() - 1, spec);
            case BARRACKS -> decorateBarracks(level, origin, villageCenter.getY() - 1, spec);
            case WALLS -> {
            }
        }
    }

    static BlockPos terminalPosition(
            ServerLevel level,
            BlockPos villageCenter,
            VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.WALLS) {
            return VillageFortressTerrain.gateControlPosition(villageCenter);
        }
        VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(building);
        BlockPos origin = villageCenter.offset(spec.dx(), 0, spec.dz());
        if (building == VillageProgressionSystem.Building.TOWN_HALL) {
            return origin.offset(spec.width() / 2, 1, spec.depth() / 2 - 2);
        }
        BlockPos entrance = VillageBuildingCatalog.entrance(level, origin, spec);
        Direction inward = spec.entranceFacing().getOpposite();
        Direction sideways = spec.entranceFacing().getClockWise();
        return entrance.relative(inward, 5).relative(sideways, 4);
    }

    static VillageProgressionSystem.Building buildingAtTerminal(
            ServerLevel level,
            BlockPos villageCenter,
            BlockPos clicked) {
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            if (building == VillageProgressionSystem.Building.TOWN_HALL
                    || building == VillageProgressionSystem.Building.WALLS) {
                continue;
            }
            if (terminalPosition(level, villageCenter, building).equals(clicked)
                    && level.getBlockState(clicked).is(expectedTerminal(building))) {
                return building;
            }
        }
        return null;
    }

    static void reinforceWallRailings(ServerLevel level, BlockPos center) {
        int railY = center.getY() - 1 + WALL_TOP_Y + 1;
        int northOuter = center.getZ() - WALL_RADIUS;
        int northInner = northOuter + 4;
        int southOuter = center.getZ() + WALL_RADIUS;
        int southInner = southOuter - 4;
        int westOuter = center.getX() - WALL_RADIUS;
        int westInner = westOuter + 4;
        int eastOuter = center.getX() + WALL_RADIUS;
        int eastInner = eastOuter - 4;

        for (int offset = -WALL_RADIUS; offset <= WALL_RADIUS; offset++) {
            int x = center.getX() + offset;
            if (Math.abs(offset) > 15) {
                placeRailing(level, new BlockPos(x, railY, northOuter), offset);
                if (!isNorthStairOpening(offset)) {
                    placeRailing(level, new BlockPos(x, railY, northInner), offset);
                }
            }
            placeRailing(level, new BlockPos(x, railY, southOuter), offset);
            placeRailing(level, new BlockPos(x, railY, southInner), offset);

            int z = center.getZ() + offset;
            placeRailing(level, new BlockPos(westOuter, railY, z), offset);
            placeRailing(level, new BlockPos(westInner, railY, z), offset);
            placeRailing(level, new BlockPos(eastOuter, railY, z), offset);
            placeRailing(level, new BlockPos(eastInner, railY, z), offset);
        }
    }

    private static void connectEntranceToRoad(
            ServerLevel level,
            BlockPos villageCenter,
            BlockPos origin,
            VillageBuildingCatalog.Spec spec) {
        BlockPos entrance = VillageBuildingCatalog.entrance(level, origin, spec);
        Direction outward = spec.entranceFacing();
        Direction sideways = outward.getClockWise();
        int groundY = villageCenter.getY() - 1;

        for (int forward = 0; forward <= 64; forward++) {
            BlockPos row = entrance.relative(outward, forward);
            boolean raisedThreshold = forward <= 1;
            int floorY = raisedThreshold ? groundY + 1 : groundY;
            for (int side = -2; side <= 2; side++) {
                BlockPos floor = new BlockPos(
                        row.getX() + sideways.getStepX() * side,
                        floorY,
                        row.getZ() + sideways.getStepZ() * side);
                set(level, floor, Math.abs(side) == 2 ? Blocks.STONE_BRICKS : Blocks.PACKED_MUD);
                for (int y = 1; y <= 4; y++) {
                    set(level, floor.above(y), Blocks.AIR);
                }
            }
            if (pathReachedRoad(villageCenter, row, outward)) {
                break;
            }
        }
    }

    private static boolean pathReachedRoad(BlockPos center, BlockPos row, Direction outward) {
        int dx = row.getX() - center.getX();
        int dz = row.getZ() - center.getZ();
        if (outward.getAxis() == Direction.Axis.X) {
            return Math.abs(dx) <= 4;
        }
        return dx * dx + dz * dz <= 18 * 18;
    }

    private static void decorateTownHall(
            ServerLevel level,
            BlockPos origin,
            int groundY,
            VillageBuildingCatalog.Spec spec) {
        int x0 = origin.getX();
        int z0 = origin.getZ();
        int x1 = x0 + spec.width() - 1;
        int z1 = z0 + spec.depth() - 1;
        int centerX = (x0 + x1) / 2;

        for (int z = z0 + 9; z <= z0 + 23; z += 4) {
            for (int x : new int[]{centerX - 10, centerX + 10}) {
                set(level, new BlockPos(x, groundY + 2, z), Blocks.DARK_OAK_SLAB);
            }
        }
        for (int x = centerX - 9; x <= centerX + 9; x++) {
            if (Math.abs(x - centerX) > 1) {
                set(level, new BlockPos(x, groundY + 2, z0 + 20), Blocks.DARK_OAK_SLAB);
            }
        }
        set(level, new BlockPos(centerX - 8, groundY + 2, z0 + 7), Blocks.BOOKSHELF);
        set(level, new BlockPos(centerX + 8, groundY + 2, z0 + 7), Blocks.BOOKSHELF);
        set(level, new BlockPos(centerX - 8, groundY + 3, z0 + 7), Blocks.LANTERN);
        set(level, new BlockPos(centerX + 8, groundY + 3, z0 + 7), Blocks.LANTERN);

        int openingWest = centerX - 8;
        int openingEast = centerX + 8;
        int openingNorth = z0 + 6;
        int openingSouth = z1 - 5;
        for (int z = openingNorth; z <= openingSouth; z++) {
            set(level, new BlockPos(openingWest - 1, groundY + 8, z), Blocks.DARK_OAK_FENCE);
            set(level, new BlockPos(openingEast + 1, groundY + 8, z), Blocks.DARK_OAK_FENCE);
        }
        for (int x = openingWest - 1; x <= openingEast + 1; x++) {
            set(level, new BlockPos(x, groundY + 8, openingNorth - 1), Blocks.DARK_OAK_FENCE);
            set(level, new BlockPos(x, groundY + 8, openingSouth + 1), Blocks.DARK_OAK_FENCE);
        }
        for (int x : new int[]{x0 + 7, x1 - 7}) {
            set(level, new BlockPos(x, groundY + 8, z0 + 8), Blocks.SPRUCE_SLAB);
            set(level, new BlockPos(x, groundY + 8, z0 + 12), Blocks.SPRUCE_SLAB);
            set(level, new BlockPos(x, groundY + 8, z0 + 16), Blocks.SPRUCE_SLAB);
        }
    }

    private static void decorateSmithy(
            ServerLevel level,
            BlockPos origin,
            int groundY,
            VillageBuildingCatalog.Spec spec) {
        int rearX = origin.getX() + 3;
        int rearZ = origin.getZ() + spec.depth() - 3;
        for (int y = groundY + 2; y <= groundY + 12; y++) {
            set(level, new BlockPos(rearX, y, rearZ), y >= groundY + 10 ? Blocks.BRICKS : Blocks.STONE_BRICKS);
        }
        set(level, origin.offset(8, 1, 5), Blocks.POLISHED_ANDESITE);
        set(level, origin.offset(8, 2, 5), Blocks.LANTERN);
        set(level, origin.offset(16, 1, 14), Blocks.IRON_BARS);
        set(level, origin.offset(17, 1, 14), Blocks.IRON_BARS);
    }

    private static void decorateSkillHall(
            ServerLevel level,
            BlockPos origin,
            int groundY,
            VillageBuildingCatalog.Spec spec) {
        int centerX = origin.getX() + spec.width() / 2;
        int centerZ = origin.getZ() + spec.depth() / 2;
        for (int offset = -6; offset <= 6; offset += 3) {
            set(level, new BlockPos(centerX + offset, groundY + 2, centerZ + 5), Blocks.BOOKSHELF);
            set(level, new BlockPos(centerX + offset, groundY + 3, centerZ + 5), Blocks.BOOKSHELF);
        }
        set(level, new BlockPos(centerX, groundY + 2, centerZ - 4), Blocks.AMETHYST_BLOCK);
        set(level, new BlockPos(centerX, groundY + 3, centerZ - 4), Blocks.LANTERN);
    }

    private static void decorateInfirmary(
            ServerLevel level,
            BlockPos origin,
            int groundY,
            VillageBuildingCatalog.Spec spec) {
        int x0 = origin.getX();
        int centerZ = origin.getZ() + spec.depth() / 2;
        set(level, new BlockPos(x0 - 1, groundY + 6, centerZ), Blocks.REDSTONE_BLOCK);
        set(level, new BlockPos(x0 - 1, groundY + 5, centerZ), Blocks.REDSTONE_BLOCK);
        set(level, new BlockPos(x0 - 1, groundY + 7, centerZ), Blocks.REDSTONE_BLOCK);
        set(level, new BlockPos(x0 - 1, groundY + 6, centerZ - 1), Blocks.REDSTONE_BLOCK);
        set(level, new BlockPos(x0 - 1, groundY + 6, centerZ + 1), Blocks.REDSTONE_BLOCK);
        for (int z : new int[]{origin.getZ() + 5, origin.getZ() + 14}) {
            set(level, new BlockPos(origin.getX() + 14, groundY + 2, z), Blocks.QUARTZ_BLOCK);
            set(level, new BlockPos(origin.getX() + 15, groundY + 2, z), Blocks.QUARTZ_BLOCK);
        }
    }

    private static void decorateStorehouse(
            ServerLevel level,
            BlockPos origin,
            int groundY,
            VillageBuildingCatalog.Spec spec) {
        for (int x = origin.getX() + 5; x <= origin.getX() + 11; x += 3) {
            set(level, new BlockPos(x, groundY + 2, origin.getZ() + 15), Blocks.HAY_BLOCK);
        }
        for (int z = origin.getZ() + 5; z <= origin.getZ() + 11; z += 3) {
            set(level, new BlockPos(origin.getX() + 18, groundY + 2, z), Blocks.SPRUCE_PLANKS);
            set(level, new BlockPos(origin.getX() + 18, groundY + 3, z), Blocks.LANTERN);
        }
    }

    private static void decorateBarracks(
            ServerLevel level,
            BlockPos origin,
            int groundY,
            VillageBuildingCatalog.Spec spec) {
        for (int z = origin.getZ() + 5; z <= origin.getZ() + 15; z += 5) {
            for (int x : new int[]{origin.getX() + 7, origin.getX() + 19}) {
                set(level, new BlockPos(x, groundY + 2, z), Blocks.STRIPPED_SPRUCE_WOOD);
                set(level, new BlockPos(x, groundY + 3, z), Blocks.IRON_BARS);
            }
        }
        for (int x = origin.getX() + 10; x <= origin.getX() + 16; x += 3) {
            set(level, new BlockPos(x, groundY + 2, origin.getZ() + 16), Blocks.DARK_OAK_SLAB);
        }
    }

    private static Block expectedTerminal(VillageProgressionSystem.Building building) {
        return switch (building) {
            case SMITHY -> Blocks.SMITHING_TABLE;
            case SKILL_HALL -> Blocks.ENCHANTING_TABLE;
            case INFIRMARY -> Blocks.BREWING_STAND;
            case STOREHOUSE -> Blocks.BARREL;
            case BARRACKS -> Blocks.TARGET;
            case TOWN_HALL -> Blocks.LECTERN;
            case WALLS -> Blocks.LEVER;
        };
    }

    private static boolean isNorthStairOpening(int offset) {
        return Math.abs(Math.abs(offset) - 25) <= 3;
    }

    private static void placeRailing(ServerLevel level, BlockPos pos, int pattern) {
        set(level, pos, Blocks.STONE_BRICK_WALL);
        if (Math.floorMod(pattern, 3) == 0) {
            set(level, pos.above(), Blocks.STONE_BRICK_WALL);
        }
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        VillageFortressTerrain.set(level, pos, block);
    }
}
