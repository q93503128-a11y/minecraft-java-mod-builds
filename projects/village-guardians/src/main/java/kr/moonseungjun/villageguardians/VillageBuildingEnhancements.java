package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

final class VillageBuildingEnhancements {
    private static final int WALL_RADIUS = VillageWorldSystem.FORTRESS_RADIUS;
    private static final int WALL_TOP_Y = 9;
    private static final int WALL_EMPLACEMENT_LANE = 34;
    private static final int WALL_EMPLACEMENT_INSET = 7;
    private static final int WALL_EMPLACEMENT_HALF = 2;

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
            return origin.offset(spec.width() / 2, 1, spec.depth() / 2 - 3);
        }
        BlockPos entrance = VillageBuildingCatalog.entrance(level, origin, spec);
        Direction inward = spec.entranceFacing().getOpposite();
        Direction sideways = spec.entranceFacing().getClockWise();
        return entrance.relative(inward, 4).relative(sideways, 3);
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
                placeRailing(level, new BlockPos(x, railY, northOuter));
                if (!isNorthStairOpening(offset)) {
                    placeRailing(level, new BlockPos(x, railY, northInner));
                }
            }
            placeRailing(level, new BlockPos(x, railY, southOuter));
            if (!isSideRearStairOpening(offset)) {
                placeRailing(level, new BlockPos(x, railY, southInner));
            }

            int z = center.getZ() + offset;
            placeRailing(level, new BlockPos(westOuter, railY, z));
            if (!isSideRearStairOpening(offset)) {
                placeRailing(level, new BlockPos(westInner, railY, z));
            }
            placeRailing(level, new BlockPos(eastOuter, railY, z));
            if (!isSideRearStairOpening(offset)) {
                placeRailing(level, new BlockPos(eastInner, railY, z));
            }
        }
        buildWallTopEmplacements(level, center);
    }

    /** True only for the authored 3x3 placement cores inside the eight wall-top pads. */
    static boolean isWallTopEmplacement(BlockPos center, BlockPos candidate) {
        if (center == null || candidate == null || candidate.getY() != center.getY() + WALL_TOP_Y) return false;
        int dx = candidate.getX() - center.getX();
        int dz = candidate.getZ() - center.getZ();
        int inset = WALL_RADIUS - WALL_EMPLACEMENT_INSET;
        boolean northSouth = Math.abs(Math.abs(dx) - WALL_EMPLACEMENT_LANE) <= 1
                && Math.abs(Math.abs(dz) - inset) <= 1;
        boolean eastWest = Math.abs(Math.abs(dz) - WALL_EMPLACEMENT_LANE) <= 1
                && Math.abs(Math.abs(dx) - inset) <= 1;
        return northSouth || eastWest;
    }

    private static void buildWallTopEmplacements(ServerLevel level, BlockPos center) {
        int floorY = center.getY() - 1 + WALL_TOP_Y;
        int inset = WALL_RADIUS - WALL_EMPLACEMENT_INSET;
        for (int lane : new int[]{-WALL_EMPLACEMENT_LANE, WALL_EMPLACEMENT_LANE}) {
            buildEmplacementPad(level,
                    new BlockPos(center.getX() + lane, floorY, center.getZ() - inset), Direction.NORTH);
            buildEmplacementPad(level,
                    new BlockPos(center.getX() + lane, floorY, center.getZ() + inset), Direction.SOUTH);
            buildEmplacementPad(level,
                    new BlockPos(center.getX() - inset, floorY, center.getZ() + lane), Direction.WEST);
            buildEmplacementPad(level,
                    new BlockPos(center.getX() + inset, floorY, center.getZ() + lane), Direction.EAST);
        }
    }

    /**
     * Five-by-five inward platform with a three-wide opening to the wall walk.
     * The centre chiseled brick is the obvious turret anchor; the 3x3 core is placement-valid.
     */
    private static void buildEmplacementPad(ServerLevel level, BlockPos padCenter, Direction outward) {
        Direction sideways = outward.getClockWise();
        for (int forward = -WALL_EMPLACEMENT_HALF; forward <= WALL_EMPLACEMENT_HALF; forward++) {
            for (int side = -WALL_EMPLACEMENT_HALF; side <= WALL_EMPLACEMENT_HALF; side++) {
                BlockPos floor = padCenter.relative(outward, forward).relative(sideways, side);
                set(level, floor, forward == 0 && side == 0 ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICKS);
                for (int clear = 1; clear <= 3; clear++) set(level, floor.above(clear), Blocks.AIR);
            }
        }

        // U-shaped guard rail on the village-facing and side edges; the wall-facing edge stays open.
        for (int side = -WALL_EMPLACEMENT_HALF; side <= WALL_EMPLACEMENT_HALF; side++) {
            placeRailing(level, padCenter.relative(outward, -WALL_EMPLACEMENT_HALF)
                    .relative(sideways, side).above());
        }
        for (int forward = -WALL_EMPLACEMENT_HALF + 1; forward <= WALL_EMPLACEMENT_HALF - 1; forward++) {
            placeRailing(level, padCenter.relative(outward, forward)
                    .relative(sideways, -WALL_EMPLACEMENT_HALF).above());
            placeRailing(level, padCenter.relative(outward, forward)
                    .relative(sideways, WALL_EMPLACEMENT_HALF).above());
        }

        // The reinforced inner parapet sits exactly three blocks outward from the pad centre.
        // Clear three cells so players can walk directly between the gallery and the emplacement.
        BlockPos galleryOpening = padCenter.relative(outward, WALL_EMPLACEMENT_HALF + 1);
        for (int side = -1; side <= 1; side++) {
            set(level, galleryOpening.relative(sideways, side).above(), Blocks.AIR);
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
            int floorY = forward == 0 ? groundY + 1 : groundY;
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
        int centerZ = (z0 + z1) / 2;

        for (int z = z0 + 7; z <= z1 - 8; z += 4) {
            for (int x : new int[]{centerX - 8, centerX + 8}) {
                set(level, new BlockPos(x, groundY + 2, z), Blocks.DARK_OAK_SLAB);
                set(level, new BlockPos(x + (x < centerX ? -1 : 1), groundY + 2, z), Blocks.DARK_OAK_SLAB);
            }
        }
        for (int x = centerX - 8; x <= centerX + 8; x++) {
            if (Math.abs(x - centerX) > 1) {
                set(level, new BlockPos(x, groundY + 2, z1 - 6), Blocks.DARK_OAK_SLAB);
            }
        }

        set(level, new BlockPos(centerX - 7, groundY + 2, z0 + 5), Blocks.BOOKSHELF);
        set(level, new BlockPos(centerX + 7, groundY + 2, z0 + 5), Blocks.BOOKSHELF);
        set(level, new BlockPos(centerX - 7, groundY + 3, z0 + 5), Blocks.LANTERN);
        set(level, new BlockPos(centerX + 7, groundY + 3, z0 + 5), Blocks.LANTERN);

        int openingWest = centerX - 6;
        int openingEast = centerX + 6;
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

        for (int x : new int[]{x0 + 6, x1 - 6}) {
            for (int z = z0 + 6; z <= z1 - 6; z += 4) {
                set(level, new BlockPos(x, groundY + 8, z), Blocks.SPRUCE_SLAB);
            }
        }
        set(level, new BlockPos(centerX, groundY + 8, centerZ - 8), Blocks.CARTOGRAPHY_TABLE);
        set(level, new BlockPos(centerX, groundY + 9, centerZ - 8), Blocks.LANTERN);
    }

    private static void decorateSmithy(
            ServerLevel level,
            BlockPos origin,
            int groundY,
            VillageBuildingCatalog.Spec spec) {
        int rearX = origin.getX() + 3;
        int rearZ = origin.getZ() + spec.depth() - 3;
        for (int y = groundY + 2; y <= groundY + 11; y++) {
            set(level, new BlockPos(rearX, y, rearZ), y >= groundY + 9 ? Blocks.BRICKS : Blocks.STONE_BRICKS);
        }
        set(level, origin.offset(7, 1, 4), Blocks.POLISHED_ANDESITE);
        set(level, origin.offset(7, 2, 4), Blocks.LANTERN);
        set(level, origin.offset(spec.width() - 7, 1, spec.depth() - 5), Blocks.IRON_BARS);
        set(level, origin.offset(spec.width() - 6, 1, spec.depth() - 5), Blocks.IRON_BARS);
        set(level, origin.offset(spec.width() / 2, 1, spec.depth() / 2), Blocks.MAGMA_BLOCK);
    }

    private static void decorateSkillHall(
            ServerLevel level,
            BlockPos origin,
            int groundY,
            VillageBuildingCatalog.Spec spec) {
        int centerX = origin.getX() + spec.width() / 2;
        int centerZ = origin.getZ() + spec.depth() / 2;
        for (int offset = -5; offset <= 5; offset += 2) {
            set(level, new BlockPos(centerX + offset, groundY + 2, centerZ + 4), Blocks.BOOKSHELF);
            set(level, new BlockPos(centerX + offset, groundY + 3, centerZ + 4), Blocks.BOOKSHELF);
        }
        set(level, new BlockPos(centerX, groundY + 2, centerZ - 3), Blocks.AMETHYST_BLOCK);
        set(level, new BlockPos(centerX, groundY + 3, centerZ - 3), Blocks.LANTERN);
        set(level, new BlockPos(centerX - 5, groundY + 2, centerZ - 3), Blocks.LECTERN);
        set(level, new BlockPos(centerX + 5, groundY + 2, centerZ - 3), Blocks.LECTERN);
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
        for (int z : new int[]{origin.getZ() + 4, origin.getZ() + spec.depth() - 5}) {
            set(level, new BlockPos(origin.getX() + 11, groundY + 2, z), Blocks.QUARTZ_BLOCK);
            set(level, new BlockPos(origin.getX() + 12, groundY + 2, z), Blocks.QUARTZ_BLOCK);
            set(level, new BlockPos(origin.getX() + 11, groundY + 3, z), Blocks.LANTERN);
        }
    }

    private static void decorateStorehouse(
            ServerLevel level,
            BlockPos origin,
            int groundY,
            VillageBuildingCatalog.Spec spec) {
        for (int x = origin.getX() + 4; x <= origin.getX() + 10; x += 3) {
            set(level, new BlockPos(x, groundY + 2, origin.getZ() + spec.depth() - 4), Blocks.HAY_BLOCK);
        }
        for (int z = origin.getZ() + 4; z <= origin.getZ() + spec.depth() - 5; z += 3) {
            set(level, new BlockPos(origin.getX() + spec.width() - 6, groundY + 2, z), Blocks.SPRUCE_PLANKS);
            set(level, new BlockPos(origin.getX() + spec.width() - 6, groundY + 3, z), Blocks.LANTERN);
        }
    }

    private static void decorateBarracks(
            ServerLevel level,
            BlockPos origin,
            int groundY,
            VillageBuildingCatalog.Spec spec) {
        for (int z = origin.getZ() + 4; z <= origin.getZ() + spec.depth() - 4; z += 4) {
            for (int x : new int[]{origin.getX() + 6, origin.getX() + spec.width() - 7}) {
                set(level, new BlockPos(x, groundY + 2, z), Blocks.STRIPPED_SPRUCE_WOOD);
                set(level, new BlockPos(x, groundY + 3, z), Blocks.IRON_BARS);
            }
        }
        for (int x = origin.getX() + 8; x <= origin.getX() + spec.width() - 9; x += 3) {
            set(level, new BlockPos(x, groundY + 2, origin.getZ() + spec.depth() - 4), Blocks.DARK_OAK_SLAB);
        }
        set(level, origin.offset(spec.width() / 2, 1, spec.depth() / 2), Blocks.TARGET);
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

    private static boolean isSideRearStairOpening(int offset) {
        return Math.abs(Math.abs(offset) - WALL_EMPLACEMENT_LANE) <= 3;
    }

    private static void placeRailing(ServerLevel level, BlockPos pos) {
        set(level, pos, Blocks.STONE_BRICK_WALL);
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        VillageFortressTerrain.set(level, pos, block);
    }
}
