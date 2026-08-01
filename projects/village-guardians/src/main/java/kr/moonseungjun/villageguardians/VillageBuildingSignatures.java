package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Flat, front-facing facility marks. Nothing is placed on a building roof. */
final class VillageBuildingSignatures {
    private static final Block BACKDROP = Blocks.POLISHED_DEEPSLATE;

    private VillageBuildingSignatures() {}

    static void buildAll(ServerLevel level, BlockPos villageCenter) {
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            if (building == VillageProgressionSystem.Building.INFIRMARY) continue;
            if (VillageProgressionSystem.isOperational(building)) build(level, villageCenter, building);
            else remove(level, villageCenter, building);
        }
    }

    static void build(ServerLevel level, BlockPos villageCenter, VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.INFIRMARY) return;
        if (building == VillageProgressionSystem.Building.WALLS) {
            buildGateShield(level, villageCenter);
            return;
        }

        VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(building);
        BlockPos origin = villageCenter.offset(spec.dx(), 0, spec.dz());
        BlockPos entrance = VillageBuildingCatalog.entrance(level, origin, spec);
        BlockPos anchor = entrance.relative(spec.entranceFacing().getOpposite()).above(4);
        Direction sideways = spec.entranceFacing().getClockWise();

        buildBackdrop(level, anchor, sideways, 2, -1, 2);
        switch (building) {
            case TOWN_HALL -> buildCrown(level, anchor, sideways);
            case SMITHY -> buildHammer(level, anchor, sideways);
            case SKILL_HALL -> buildRune(level, anchor, sideways);
            case STOREHOUSE -> buildSupplyCrate(level, anchor, sideways);
            case BARRACKS -> buildCrossedBlades(level, anchor, sideways);
            case INFIRMARY, WALLS -> {
            }
        }
    }

    static void remove(ServerLevel level, BlockPos villageCenter, VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.INFIRMARY) return;
        if (building == VillageProgressionSystem.Building.WALLS) {
            BlockPos anchor = gateAnchor(villageCenter);
            clearPlane(level, anchor, Direction.EAST, 3, -2, 2);
            return;
        }
        VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(building);
        BlockPos origin = villageCenter.offset(spec.dx(), 0, spec.dz());
        BlockPos entrance = VillageBuildingCatalog.entrance(level, origin, spec);
        BlockPos anchor = entrance.relative(spec.entranceFacing().getOpposite()).above(4);
        clearPlane(level, anchor, spec.entranceFacing().getClockWise(), 2, -1, 2);
    }

    private static void buildCrown(ServerLevel level, BlockPos anchor, Direction sideways) {
        line(level, anchor, sideways, -2, 2, 0, Blocks.GOLD_BLOCK);
        mark(level, anchor, sideways, -2, 1, Blocks.GOLD_BLOCK);
        mark(level, anchor, sideways, 0, 2, Blocks.SEA_LANTERN);
        mark(level, anchor, sideways, 2, 1, Blocks.GOLD_BLOCK);
        mark(level, anchor, sideways, 0, 1, Blocks.GOLD_BLOCK);
    }

    private static void buildHammer(ServerLevel level, BlockPos anchor, Direction sideways) {
        mark(level, anchor, sideways, 0, -1, Blocks.IRON_BLOCK);
        mark(level, anchor, sideways, 0, 0, Blocks.IRON_BLOCK);
        mark(level, anchor, sideways, 0, 1, Blocks.IRON_BLOCK);
        line(level, anchor, sideways, -2, 2, 2, Blocks.BRICKS);
        mark(level, anchor, sideways, -2, 1, Blocks.MAGMA_BLOCK);
        mark(level, anchor, sideways, 2, 1, Blocks.MAGMA_BLOCK);
    }

    private static void buildRune(ServerLevel level, BlockPos anchor, Direction sideways) {
        mark(level, anchor, sideways, 0, -1, Blocks.PURPUR_BLOCK);
        mark(level, anchor, sideways, -1, 0, Blocks.PURPUR_BLOCK);
        mark(level, anchor, sideways, 1, 0, Blocks.PURPUR_BLOCK);
        mark(level, anchor, sideways, -2, 1, Blocks.PURPUR_BLOCK);
        mark(level, anchor, sideways, 2, 1, Blocks.PURPUR_BLOCK);
        mark(level, anchor, sideways, -1, 2, Blocks.PURPUR_BLOCK);
        mark(level, anchor, sideways, 1, 2, Blocks.PURPUR_BLOCK);
        mark(level, anchor, sideways, 0, 1, Blocks.AMETHYST_BLOCK);
        mark(level, anchor, sideways, 0, 2, Blocks.SEA_LANTERN);
    }

    private static void buildSupplyCrate(ServerLevel level, BlockPos anchor, Direction sideways) {
        for (int lateral = -2; lateral <= 2; lateral++) {
            for (int vertical = -1; vertical <= 1; vertical++) {
                Block material = Math.abs(lateral) == 2 || vertical == -1 || vertical == 1
                        ? Blocks.GOLD_BLOCK : Blocks.HAY_BLOCK;
                mark(level, anchor, sideways, lateral, vertical, material);
            }
        }
        mark(level, anchor, sideways, 0, 0, Blocks.SEA_LANTERN);
        mark(level, anchor, sideways, 0, 2, Blocks.GOLD_BLOCK);
    }

    private static void buildCrossedBlades(ServerLevel level, BlockPos anchor, Direction sideways) {
        for (int step = -2; step <= 2; step++) {
            int verticalA = step + 1;
            int verticalB = 1 - step;
            if (verticalA >= -1 && verticalA <= 2) {
                mark(level, anchor, sideways, step, verticalA, Blocks.IRON_BLOCK);
            }
            if (verticalB >= -1 && verticalB <= 2) {
                mark(level, anchor, sideways, step, verticalB, Blocks.NETHER_BRICKS);
            }
        }
        mark(level, anchor, sideways, 0, 1, Blocks.SEA_LANTERN);
    }

    private static void buildGateShield(ServerLevel level, BlockPos villageCenter) {
        BlockPos anchor = gateAnchor(villageCenter);
        Direction sideways = Direction.EAST;
        buildBackdrop(level, anchor, sideways, 3, -2, 2);
        for (int lateral = -3; lateral <= 3; lateral++) {
            int lower = Math.abs(lateral) <= 1 ? -2 : Math.abs(lateral) <= 2 ? -1 : 0;
            for (int vertical = lower; vertical <= 2; vertical++) {
                Block material = Math.abs(lateral) == 3 || vertical == 2 || vertical == lower
                        ? Blocks.IRON_BLOCK : Blocks.NETHER_WART_BLOCK;
                mark(level, anchor, sideways, lateral, vertical, material);
            }
        }
        line(level, anchor, sideways, -1, 1, 0, Blocks.GOLD_BLOCK);
        mark(level, anchor, sideways, 0, -1, Blocks.GOLD_BLOCK);
        mark(level, anchor, sideways, 0, 1, Blocks.GOLD_BLOCK);
        mark(level, anchor, sideways, 0, 0, Blocks.SEA_LANTERN);
    }

    private static BlockPos gateAnchor(BlockPos villageCenter) {
        return villageCenter.offset(0, 9, -VillageWorldSystem.FORTRESS_RADIUS + 3);
    }

    private static void buildBackdrop(ServerLevel level, BlockPos anchor, Direction sideways,
                                      int halfWidth, int minVertical, int maxVertical) {
        for (int lateral = -halfWidth; lateral <= halfWidth; lateral++) {
            for (int vertical = minVertical; vertical <= maxVertical; vertical++) {
                mark(level, anchor, sideways, lateral, vertical, BACKDROP);
            }
        }
    }

    private static void clearPlane(ServerLevel level, BlockPos anchor, Direction sideways,
                                   int halfWidth, int minVertical, int maxVertical) {
        for (int lateral = -halfWidth; lateral <= halfWidth; lateral++) {
            for (int vertical = minVertical; vertical <= maxVertical; vertical++) {
                mark(level, anchor, sideways, lateral, vertical, Blocks.AIR);
            }
        }
    }

    private static void line(ServerLevel level, BlockPos anchor, Direction sideways,
                             int from, int to, int vertical, Block block) {
        for (int lateral = from; lateral <= to; lateral++) {
            mark(level, anchor, sideways, lateral, vertical, block);
        }
    }

    private static void mark(ServerLevel level, BlockPos anchor, Direction sideways,
                             int lateral, int vertical, Block block) {
        level.setBlockAndUpdate(anchor.relative(sideways, lateral).above(vertical), block.defaultBlockState());
    }
}
